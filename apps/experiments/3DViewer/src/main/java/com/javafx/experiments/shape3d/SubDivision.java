package com.javafx.experiments.shape3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;


/**
 * 
 * Catmull Clark subdivision surface
 */
public class SubDivision {
    
    private PolygonMesh oldMesh;
    private Map<Edge, EdgeInfo> edgeInfos;
    private FaceInfo[] faceInfos;
    private PointInfo[] pointInfos;
    private float[] points;
    private float[] texCoords;
    private int[] reindex;
    private int newPointIndex;
    private int newTexCoordIndex;
    private BoundaryMode boundaryMode;
    private MapBorderMode mapBorderMode;

    /** 
     * Describes whether the edges and points at the boundary are treated as creases
     */
    public enum BoundaryMode {
        /**
         * only edges at the boundary are treated as creases
         */
        CREASE_EDGES, 
        /**
         * edges and points at the boundary are treated as creases
         */
        CREASE_ALL
    }
    
    /**
     * describes how the new texture coordinate for the control point is defined
     */
    public enum MapBorderMode {
        /**
         * keeps the same uvs for all control points
         */
        NOT_SMOOTH, 
        /**
         * smooths uvs of points at corners
         */
        SMOOTH_INTERNAL, 
        /**
         * smooths uvs of points at boundaries (and creases [in the future when creases are defined])
         */
        SMOOTH_ALL
    }
    
    public SubDivision(PolygonMesh oldMesh, BoundaryMode boundaryMode, MapBorderMode mapBorderMode) {
        this.oldMesh = oldMesh;
        this.boundaryMode = boundaryMode;
        this.mapBorderMode = mapBorderMode;
    }
    
    public PolygonMesh subdivide() {
        collectInfo();
        calcEdgePoints();
        
        points = new float[oldMesh.getPoints().size() + faceInfos.length * 3 + edgeInfos.size() * 3];
        texCoords = new float[oldMesh.getNumEdgesInFaces() * 4 * 2];
        int[][] faces = new int[oldMesh.getNumEdgesInFaces()][8];
        newPointIndex = 0;
        newTexCoordIndex = 0;
        reindex = new int[oldMesh.getPoints().size()]; // indexes incremented by 1, 0 reserved for empty
        
        int newFacesInd = 0;
        for (int f = 0; f < oldMesh.faces.length; f++) {
            FaceInfo faceInfo = faceInfos[f];
            int[] oldFaces = oldMesh.faces[f];
            for (int p = 0; p < oldFaces.length; p += 2) {
                faces[newFacesInd][0] = getPointNewIndex(oldFaces[p]);
                faces[newFacesInd][1] = getTexCoordNewIndex(faceInfo, oldFaces[p], oldFaces[p+1]);
                faces[newFacesInd][2] = getPointNewIndex(faceInfo, (p / 2 + 1) % faceInfo.edges.length);
                faces[newFacesInd][3] = getTexCoordNewIndex(faceInfo, (p / 2 + 1) % faceInfo.edges.length);
                faces[newFacesInd][4] = getPointNewIndex(faceInfo);
                faces[newFacesInd][5] = getTexCoordNewIndex(faceInfo);
                faces[newFacesInd][6] = getPointNewIndex(faceInfo, p / 2);
                faces[newFacesInd][7] = getTexCoordNewIndex(faceInfo, p / 2);
                newFacesInd++;
            }
        }
        PolygonMesh newMesh = new PolygonMesh(points, texCoords, faces);
        return newMesh;
    }
    
    public static PolygonMesh subdivide(PolygonMesh oldMesh, BoundaryMode boundaryMode, MapBorderMode mapBorderMode) {
        SubDivision subDivision = new SubDivision(oldMesh, boundaryMode, mapBorderMode);
        return subDivision.subdivide();
    }

    private void addEdge(Edge edge, FaceInfo faceInfo, Point3D midPoint) {
        EdgeInfo edgeInfo = edgeInfos.get(edge);
        if (edgeInfo == null) {
            edgeInfo = new EdgeInfo();
            edgeInfo.midPoint = midPoint;
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
        pointInfos = new PointInfo[oldMesh.getPoints().size() / 3];
        
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
            double fx, fy, fz, fu, fv;
            double tx, ty, tz, tu, tv;
            double x = 0, y = 0, z = 0, u = 0, v = 0;
            fx = oldMesh.getPoints().get(from * 3);
            fy = oldMesh.getPoints().get(from * 3 + 1);
            fz = oldMesh.getPoints().get(from * 3 + 2);
            fu = oldMesh.texCoords[texFrom * 2];
            fv = oldMesh.texCoords[texFrom * 2 + 1];
            for (int i = 0; i < n; i++) {
                int to = face[i * 2];
                int texTo = face[i * 2 + 1];
                tx = oldMesh.getPoints().get(to * 3);
                ty = oldMesh.getPoints().get(to * 3 + 1);
                tz = oldMesh.getPoints().get(to * 3 + 2);
                tu = oldMesh.texCoords[texTo * 2];
                tv = oldMesh.texCoords[texTo * 2 + 1];
                Point3D midPoint = new Point3D((fx + tx) / 2, (fy + ty) / 2, (fz + tz) / 2);
                Point2D midTexCoord = new Point2D((fu + tu) / 2, (fv + tv) / 2);
                Edge edge = new Edge(from, to);
                faceInfo.edges[i] = edge;
                faceInfo.edgeTexCoords[i] = midTexCoord;
                addEdge(edge, faceInfo, midPoint);
                addPoint(to, faceInfo, edge);
                addPoint(from, edge);
                fx = tx; fy = ty; fz = tz; fu = tu; fv = tv;
                x += tx / n; y += ty / n; z += tz / n; u += tu / n; v += tv / n;
                from = to;
                texFrom = texTo;
            }
            faceInfo.point = new Point3D(x, y, z);
            faceInfo.texCoord = new Point2D(u, v);
        }
    }

    private void calcEdgePoints() {
        for (EdgeInfo edgeInfo : edgeInfos.values()) {
            if (edgeInfo.isBoundary()) {
                edgeInfo.edgePoint = edgeInfo.midPoint;
            } else {
                int n = edgeInfo.faces.size() + 2;
                double x = edgeInfo.midPoint.getX() * 2 / n;
                double y = edgeInfo.midPoint.getY() * 2 / n;
                double z = edgeInfo.midPoint.getZ() * 2 / n;
                for (FaceInfo faceInfo : edgeInfo.faces) {
                    Point3D facePoint = faceInfo.point;
                    x += facePoint.getX() / n;
                    y += facePoint.getY() / n;
                    z += facePoint.getZ() / n;
                }
                edgeInfo.edgePoint = new Point3D(x, y, z);
            }
        }
    }

    private void calcControlPoint(int srcPointIndex, int destPointIndex) {
        PointInfo pointInfo = pointInfos[srcPointIndex];
        double x, y, z;
        if (pointInfo.isBoundary()) {
            if ((boundaryMode == BoundaryMode.CREASE_EDGES) || pointInfo.hasInternalEdge()) {
                x = oldMesh.getPoints().get(srcPointIndex * 3) / 2;
                y = oldMesh.getPoints().get(srcPointIndex * 3 + 1) / 2;
                z = oldMesh.getPoints().get(srcPointIndex * 3 + 2) / 2;
                for (Edge edge : pointInfo.edges) {
                    EdgeInfo edgeInfo = edgeInfos.get(edge);
                    if (edgeInfo.isBoundary()) {
                        x += edgeInfo.edgePoint.getX() / 4;
                        y += edgeInfo.edgePoint.getY() / 4;
                        z += edgeInfo.edgePoint.getZ() / 4;
                    }
                }
            } else {
                x = oldMesh.getPoints().get(srcPointIndex * 3);
                y = oldMesh.getPoints().get(srcPointIndex * 3 + 1);
                z = oldMesh.getPoints().get(srcPointIndex * 3 + 2);
            }
        } else {
            int n = pointInfo.faces.size();
            x = oldMesh.getPoints().get(srcPointIndex * 3) * (n - 3.0) / n;
            y = oldMesh.getPoints().get(srcPointIndex * 3 + 1) * (n - 3.0) / n;
            z = oldMesh.getPoints().get(srcPointIndex * 3 + 2) * (n - 3.0) / n;
            for (FaceInfo faceInfo : pointInfo.faces) {
                Point3D point = faceInfo.point;
                x += point.getX() / n / n;
                y += point.getY() / n / n;
                z += point.getZ() / n / n;
            }
            for (Edge edge : pointInfo.edges) {
                EdgeInfo edgeInfo = edgeInfos.get(edge);
                x += edgeInfo.midPoint.getX() * 2 / n / n;
                y += edgeInfo.midPoint.getY() * 2 / n / n;
                z += edgeInfo.midPoint.getZ() * 2 / n / n;
            }
        }
        points[destPointIndex * 3] = (float) x;
        points[destPointIndex * 3 + 1] = (float) y;
        points[destPointIndex * 3 + 2] = (float) z;
    }

    private void calcControlTexCoord(FaceInfo faceInfo, int srcPointIndex, int srcTexCoordIndex, int destTexCoordIndex){
        PointInfo pointInfo = pointInfos[srcPointIndex];
        if ((mapBorderMode == MapBorderMode.SMOOTH_ALL && pointInfo.isBoundary()) || 
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
            destPointIndex = newPointIndex;
            reindex[srcPointIndex] = destPointIndex + 1;
            newPointIndex++;
            calcControlPoint(srcPointIndex, destPointIndex);
        }
        return destPointIndex;
    }
    
    private int getPointNewIndex(FaceInfo faceInfo, int edgeInd) {
        Edge edge = faceInfo.edges[edgeInd];
        EdgeInfo edgeInfo = edgeInfos.get(edge);
        int destPointIndex = edgeInfo.newPointIndex - 1;
        if (destPointIndex == -1) {
            destPointIndex = newPointIndex;
            edgeInfo.newPointIndex = destPointIndex + 1;
            newPointIndex++;
            points[destPointIndex * 3] = (float) edgeInfo.edgePoint.getX();
            points[destPointIndex * 3 + 1] = (float) edgeInfo.edgePoint.getY();
            points[destPointIndex * 3 + 2] = (float) edgeInfo.edgePoint.getZ();
        }
        return destPointIndex;
    }

    private int getPointNewIndex(FaceInfo faceInfo) {
        int destPointIndex = faceInfo.newPointIndex - 1;
        if (destPointIndex == -1) {
            destPointIndex = newPointIndex;
            faceInfo.newPointIndex = destPointIndex + 1;
            newPointIndex++;
            points[destPointIndex * 3] = (float) faceInfo.point.getX();
            points[destPointIndex * 3 + 1] = (float) faceInfo.point.getY();
            points[destPointIndex * 3 + 2] = (float) faceInfo.point.getZ();
        }
        return destPointIndex;
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
        int destTexCoordIndex = newTexCoordIndex;
        newTexCoordIndex++;
        texCoords[destTexCoordIndex * 2] = (float) faceInfo.texCoord.getX();
        texCoords[destTexCoordIndex * 2 + 1] = (float) faceInfo.texCoord.getY();
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
        Point3D midPoint;
        Point3D edgePoint;
        int newPointIndex;
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
        Point3D point;
        Point2D texCoord;
        int newPointIndex;
        Edge[] edges;
        Point2D[] edgeTexCoords;

        public FaceInfo(int n) {
            edges = new Edge[n];
            edgeTexCoords = new Point2D[n];
        }
    }
}