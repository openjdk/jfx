/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.javafx.experiments.shape3d.symbolic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.geometry.Point2D;

import static com.javafx.experiments.shape3d.SubdivisionMesh.*;

/**
 *
 * Data structure builder for Catmull Clark subdivision surface
 */
public class SymbolicSubdivisionBuilder {

    private SymbolicPolygonMesh oldMesh;
    private Map<Edge, EdgeInfo> edgeInfos;
    private FaceInfo[] faceInfos;
    private PointInfo[] pointInfos;
    private SubdividedPointArray points;
    private float[] texCoords;
    private int[] reindex;
    private int newTexCoordIndex;
    private BoundaryMode boundaryMode;
    private MapBorderMode mapBorderMode;

    public SymbolicSubdivisionBuilder(SymbolicPolygonMesh oldMesh, BoundaryMode boundaryMode, MapBorderMode mapBorderMode) {
        this.oldMesh = oldMesh;
        this.boundaryMode = boundaryMode;
        this.mapBorderMode = mapBorderMode;
    }

    public SymbolicPolygonMesh subdivide() {
        collectInfo();

        texCoords = new float[(oldMesh.getNumEdgesInFaces() * 3 + oldMesh.faces.length) * 2];
        int[][] faces = new int[oldMesh.getNumEdgesInFaces()][8];
        int[] faceSmoothingGroups = new int[oldMesh.getNumEdgesInFaces()];
        newTexCoordIndex = 0;
        reindex = new int[oldMesh.points.numPoints]; // indexes incremented by 1, 0 reserved for empty

        // face points first
        int newFacesInd = 0;
        for (int f = 0; f < oldMesh.faces.length; f++) {
            FaceInfo faceInfo = faceInfos[f];
            int[] oldFaces = oldMesh.faces[f];
            for (int p = 0; p < oldFaces.length; p += 2) {
                faces[newFacesInd][4] = getPointNewIndex(faceInfo);
                faces[newFacesInd][5] = getTexCoordNewIndex(faceInfo);
                faceSmoothingGroups[newFacesInd] = oldMesh.faceSmoothingGroups[f];
                newFacesInd++;
            }
        }
        // then, add edge points
        newFacesInd = 0;
        for (int f = 0; f < oldMesh.faces.length; f++) {
            FaceInfo faceInfo = faceInfos[f];
            int[] oldFaces = oldMesh.faces[f];
            for (int p = 0; p < oldFaces.length; p += 2) {
                faces[newFacesInd][2] = getPointNewIndex(faceInfo, (p / 2 + 1) % faceInfo.edges.length);
                faces[newFacesInd][3] = getTexCoordNewIndex(faceInfo, (p / 2 + 1) % faceInfo.edges.length);
                faces[newFacesInd][6] = getPointNewIndex(faceInfo, p / 2);
                faces[newFacesInd][7] = getTexCoordNewIndex(faceInfo, p / 2);
                newFacesInd++;
            }
        }
        // finally, add control points
        newFacesInd = 0;
        for (int f = 0; f < oldMesh.faces.length; f++) {
            FaceInfo faceInfo = faceInfos[f];
            int[] oldFaces = oldMesh.faces[f];
            for (int p = 0; p < oldFaces.length; p += 2) {
                faces[newFacesInd][0] = getPointNewIndex(oldFaces[p]);
                faces[newFacesInd][1] = getTexCoordNewIndex(faceInfo, oldFaces[p], oldFaces[p+1]);
                newFacesInd++;
            }
        }

        SymbolicPolygonMesh newMesh = new SymbolicPolygonMesh(points, texCoords, faces, faceSmoothingGroups);
        return newMesh;
    }

    public static SymbolicPolygonMesh subdivide(SymbolicPolygonMesh oldMesh, BoundaryMode boundaryMode, MapBorderMode mapBorderMode) {
        SymbolicSubdivisionBuilder subdivision = new SymbolicSubdivisionBuilder(oldMesh, boundaryMode, mapBorderMode);
        return subdivision.subdivide();
    }

    private void addEdge(Edge edge, FaceInfo faceInfo) {
        EdgeInfo edgeInfo = edgeInfos.get(edge);
        if (edgeInfo == null) {
            edgeInfo = new EdgeInfo();
            edgeInfo.edge = edge;
            edgeInfos.put(edge, edgeInfo);
        }
        edgeInfo.faces.add(faceInfo);
    }

    private void addPoint(int point, FaceInfo faceInfo, Edge edge) {
        PointInfo pointInfo = pointInfos[point];
        if (pointInfo == null) {
            pointInfo = new PointInfo();
            pointInfos[point] = pointInfo;
        }
        pointInfo.edges.add(edge);
        pointInfo.faces.add(faceInfo);
    }

    private void addPoint(int point, Edge edge) {
        PointInfo pointInfo = pointInfos[point];
        if (pointInfo == null) {
            pointInfo = new PointInfo();
            pointInfos[point] = pointInfo;
        }
        pointInfo.edges.add(edge);
    }

    private void collectInfo() {
        edgeInfos = new HashMap<>(oldMesh.faces.length * 2);
        faceInfos = new FaceInfo[oldMesh.faces.length];
        pointInfos = new PointInfo[oldMesh.points.numPoints];

        for (int f = 0; f < oldMesh.faces.length; f++) {
            int[] face = oldMesh.faces[f];
            int n = face.length / 2;
            FaceInfo faceInfo = new FaceInfo(n);
            faceInfos[f] = faceInfo;
            if (n < 3) {
                continue;
            }
            int from = face[(n-1) * 2];
            int texFrom = face[(n-1) * 2 + 1];
            double fu, fv;
            double tu, tv;
            double u = 0, v = 0;
            fu = oldMesh.texCoords[texFrom * 2];
            fv = oldMesh.texCoords[texFrom * 2 + 1];
            for (int i = 0; i < n; i++) {
                int to = face[i * 2];
                int texTo = face[i * 2 + 1];
                tu = oldMesh.texCoords[texTo * 2];
                tv = oldMesh.texCoords[texTo * 2 + 1];
                Point2D midTexCoord = new Point2D((fu + tu) / 2, (fv + tv) / 2);
                Edge edge = new Edge(from, to);
                faceInfo.edges[i] = edge;
                faceInfo.edgeTexCoords[i] = midTexCoord;
                addEdge(edge, faceInfo);
                addPoint(to, faceInfo, edge);
                addPoint(from, edge);
                fu = tu; fv = tv;
                u += tu / n; v += tv / n;
                from = to;
                texFrom = texTo;
            }
            faceInfo.texCoord = new Point2D(u, v);
        }

        points = new SubdividedPointArray(oldMesh.points, oldMesh.points.numPoints + faceInfos.length + edgeInfos.size(), boundaryMode);

        for (int f = 0; f < oldMesh.faces.length; f++) {
            int[] face = oldMesh.faces[f];
            int n = face.length / 2;
            int[] faceVertices = new int[n];
            for (int i = 0; i < n; i++) {
                faceVertices[i] = face[i * 2];
            }
            faceInfos[f].facePoint = points.addFacePoint(faceVertices);
        }

        for(EdgeInfo edgeInfo : edgeInfos.values()) {
            int[] edgeFacePoints = new int[edgeInfo.faces.size()];
            for (int f = 0; f < edgeInfo.faces.size(); f++) {
                edgeFacePoints[f] = edgeInfo.faces.get(f).facePoint;
            }
            edgeInfo.edgePoint = points.addEdgePoint(edgeFacePoints, edgeInfo.edge.from, edgeInfo.edge.to, edgeInfo.isBoundary());
        }
    }

    private int calcControlPoint(int srcPointIndex) {
        PointInfo pointInfo = pointInfos[srcPointIndex];
        int origPoint = srcPointIndex;

        int[] facePoints = new int[pointInfo.faces.size()];
        for (int f = 0; f < facePoints.length; f++) {
            facePoints[f] = pointInfo.faces.get(f).facePoint;
        }
        int[] edgePoints = new int[pointInfo.edges.size()];
        boolean[] isEdgeBoundary = new boolean[pointInfo.edges.size()];
        int[] fromEdgePoints = new int[pointInfo.edges.size()];
        int[] toEdgePoints = new int[pointInfo.edges.size()];
        int i = 0;
        for (Edge edge : pointInfo.edges) {
            EdgeInfo edgeInfo = edgeInfos.get(edge);
            edgePoints[i] = edgeInfo.edgePoint;
            isEdgeBoundary[i] = edgeInfo.isBoundary();
            fromEdgePoints[i] = edgeInfo.edge.from;
            toEdgePoints[i] = edgeInfo.edge.to;
            i++;
        }
        int destPointIndex = points.addControlPoint(facePoints, edgePoints, fromEdgePoints, toEdgePoints, isEdgeBoundary, origPoint, pointInfo.isBoundary(), pointInfo.hasInternalEdge());
        return destPointIndex;
    }

    private void calcControlTexCoord(FaceInfo faceInfo, int srcPointIndex, int srcTexCoordIndex, int destTexCoordIndex){
        PointInfo pointInfo = pointInfos[srcPointIndex];
        boolean pointBelongsToCrease = oldMesh.points instanceof OriginalPointArray;
        if ((mapBorderMode == MapBorderMode.SMOOTH_ALL && (pointInfo.isBoundary() || pointBelongsToCrease)) ||
                (mapBorderMode == MapBorderMode.SMOOTH_INTERNAL && !pointInfo.hasInternalEdge())) {
            double u = oldMesh.texCoords[srcTexCoordIndex * 2] / 2;
            double v = oldMesh.texCoords[srcTexCoordIndex * 2 + 1] / 2;
            for (int i = 0; i < faceInfo.edges.length; i++) {
                if ((faceInfo.edges[i].to == srcPointIndex) || (faceInfo.edges[i].from == srcPointIndex)) {
                    u += faceInfo.edgeTexCoords[i].getX() / 4;
                    v += faceInfo.edgeTexCoords[i].getY() / 4;
                }
            }
            texCoords[destTexCoordIndex * 2] = (float) u;
            texCoords[destTexCoordIndex * 2 + 1] = (float) v;
        } else {
            texCoords[destTexCoordIndex * 2] = oldMesh.texCoords[srcTexCoordIndex * 2];
            texCoords[destTexCoordIndex * 2 + 1] = oldMesh.texCoords[srcTexCoordIndex * 2 + 1];
        }
    }

    private int getPointNewIndex(int srcPointIndex) {
        int destPointIndex = reindex[srcPointIndex] - 1;
        if (destPointIndex == -1) {
            destPointIndex = calcControlPoint(srcPointIndex);
            reindex[srcPointIndex] = destPointIndex + 1;
        }
        return destPointIndex;
    }

    private int getPointNewIndex(FaceInfo faceInfo, int edgeInd) {
        Edge edge = faceInfo.edges[edgeInd];
        EdgeInfo edgeInfo = edgeInfos.get(edge);
        return edgeInfo.edgePoint;
    }

    private int getPointNewIndex(FaceInfo faceInfo) {
        return faceInfo.facePoint;
    }

    private int getTexCoordNewIndex(FaceInfo faceInfo, int srcPointIndex, int srcTexCoordIndex) {
        int destTexCoordIndex = newTexCoordIndex;
        newTexCoordIndex++;
        calcControlTexCoord(faceInfo, srcPointIndex, srcTexCoordIndex, destTexCoordIndex);
        return destTexCoordIndex;
    }

    private int getTexCoordNewIndex(FaceInfo faceInfo, int edgeInd) {
        int destTexCoordIndex = newTexCoordIndex;
        newTexCoordIndex++;
        texCoords[destTexCoordIndex * 2] = (float) faceInfo.edgeTexCoords[edgeInd].getX();
        texCoords[destTexCoordIndex * 2 + 1] = (float) faceInfo.edgeTexCoords[edgeInd].getY();
        return destTexCoordIndex;
    }

    private int getTexCoordNewIndex(FaceInfo faceInfo) {
        int destTexCoordIndex = faceInfo.newTexCoordIndex - 1;
        if (destTexCoordIndex == -1) {
            destTexCoordIndex = newTexCoordIndex;
            faceInfo.newTexCoordIndex = destTexCoordIndex + 1;
            newTexCoordIndex++;
            texCoords[destTexCoordIndex * 2] = (float) faceInfo.texCoord.getX();
            texCoords[destTexCoordIndex * 2 + 1] = (float) faceInfo.texCoord.getY();
        }
        return destTexCoordIndex;
    }

    private static class Edge {
        int from, to;

        public Edge(int from, int to) {
            this.from = Math.min(from, to);
            this.to = Math.max(from, to);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + this.from;
            hash = 41 * hash + this.to;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Edge other = (Edge) obj;
            if (this.from != other.from) {
                return false;
            }
            if (this.to != other.to) {
                return false;
            }
            return true;
        }
    }

    private static class EdgeInfo {
        Edge edge;
        int edgePoint;
        List<FaceInfo> faces = new ArrayList<>(2);

        /**
         * an edge is in the boundary if it has only one adjacent face
         */
        public boolean isBoundary() {
            return faces.size() == 1;
        }
    }

    private class PointInfo {
        List<FaceInfo> faces = new ArrayList<>(4);
        Set<Edge> edges = new HashSet<>(4);

        /**
         * A point is in the boundary if any of its adjacent edges is in the boundary
         */
        public boolean isBoundary() {
            for (Edge edge : edges) {
                EdgeInfo edgeInfo = edgeInfos.get(edge);
                if (edgeInfo.isBoundary())
                    return true;
            }
            return false;
        }

        /**
         * A point is internal if at least one of its adjacent edges is not in the boundary
         */
        public boolean hasInternalEdge() {
            for (Edge edge : edges) {
                EdgeInfo edgeInfo = edgeInfos.get(edge);
                if (!edgeInfo.isBoundary())
                    return true;
            }
            return false;
        }
    }

    private static class FaceInfo {
        int facePoint;
        Point2D texCoord;
        int newTexCoordIndex;
        Edge[] edges;
        Point2D[] edgeTexCoords;

        public FaceInfo(int n) {
            edges = new Edge[n];
            edgeTexCoords = new Point2D[n];
        }
    }
}