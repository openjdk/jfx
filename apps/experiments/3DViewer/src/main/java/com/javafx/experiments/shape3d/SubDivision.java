package com.javafx.experiments.shape3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.geometry.Point3D;


/**
 * 
 * @author akouznet
 */
public class SubDivision {
    
    private PolygonMesh oldMesh;
    private Map<Edge, EdgeInfo> edgeInfos;
    private FaceInfo[] faceInfos;
    private PointInfo[] pointInfos;
    private float[] points;
    private int[] reindex;
    private int newPointIndex;

    public SubDivision(PolygonMesh oldMesh) {
        this.oldMesh = oldMesh;
    }
    
    public PolygonMesh subdivide() {
        collectInfo();
        calcEdgePoints();
        
        points = new float[oldMesh.points.length + faceInfos.length * 3 + edgeInfos.size() * 3];
        List<int[]> faces = new ArrayList<>(oldMesh.faces.length * 4);
        newPointIndex = 0;
        reindex = new int[oldMesh.points.length]; // indexes incremented by 1, 0 reserved for empty
        
        for (int f = 0; f < oldMesh.faces.length; f++) {
            FaceInfo faceInfo = faceInfos[f];
            int[] oldFaces = oldMesh.faces[f];
            for (int p = 0; p < oldFaces.length; p += 2) {
                int p0 = getPointNewIndex(oldFaces[p]);
                int t0 = oldFaces[p + 1];
                int p1 = getPointNewIndex(faceInfo.edges[(p / 2 + 1) % faceInfo.edges.length]);
                int t1 = t0; // TODO implement texture coordinate subdivision
                int p2 = getPointNewIndex(faceInfo);
                int t2 = t0; // TODO implement texture coordinate subdivision
                int p3 = getPointNewIndex(faceInfo.edges[p / 2]);
                int t3 = t0; // TODO implement texture coordinate subdivision
                faces.add(new int[] { p0, t0, p1, t1, p2, t2, p3, t3 } );
            }
        }
        
        PolygonMesh newMesh = new PolygonMesh(points, oldMesh.texCoords, faces.toArray(new int[0][]));
        return newMesh;
    }
    
    public static PolygonMesh subdivide(PolygonMesh oldMesh) {
        return new SubDivision(oldMesh).subdivide();
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

    private void collectInfo() {
        edgeInfos = new HashMap<>(oldMesh.faces.length * 2);
        faceInfos = new FaceInfo[oldMesh.faces.length];
        pointInfos = new PointInfo[oldMesh.points.length / 3];
        
        for (int f = 0; f < oldMesh.faces.length; f++) {
            int[] face = oldMesh.faces[f];
            int n = face.length / 2;
            FaceInfo faceInfo = new FaceInfo(n);
            faceInfos[f] = faceInfo;
            if (n < 3) {
                continue;
            }
            int from = face[n * 2 - 2];
            double fx, fy, fz;
            double tx, ty, tz;
            double x = 0, y = 0, z = 0;
            fx = oldMesh.points[from * 3];
            fy = oldMesh.points[from * 3 + 1];
            fz = oldMesh.points[from * 3 + 2];
            for (int i = 0; i < n * 2; i += 2) {
                int to = face[i];
                tx = oldMesh.points[to * 3];
                ty = oldMesh.points[to * 3 + 1];
                tz = oldMesh.points[to * 3 + 2];
                Point3D midPoint = new Point3D((fx + tx) / 2, (fy + ty) / 2, (fz + tz) / 2);
                Edge edge = new Edge(from, to);
                faceInfo.edges[i / 2] = edge;
                addEdge(edge, faceInfo, midPoint);
                addPoint(to, faceInfo, edge);
                fx = tx; fy = ty; fz = tz;
                x += tx / n; y += ty / n; z += tz / n;
                from = to;
            }
            faceInfo.point = new Point3D(x, y, z);
        }
    }

    private void calcEdgePoints() {
        for (EdgeInfo edgeInfo : edgeInfos.values()) {
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

    private void calcControlPoint(int srcIndex, int destIndex) {
        double x, y, z;
        PointInfo pointInfo = pointInfos[srcIndex];
        int n = pointInfo.faces.size();
        x = oldMesh.points[srcIndex * 3] * (n - 3.0) / n;
        y = oldMesh.points[srcIndex * 3 + 1] * (n - 3.0) / n;
        z = oldMesh.points[srcIndex * 3 + 2] * (n - 3.0) / n;
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
        points[destIndex * 3] = (float) x;
        points[destIndex * 3 + 1] = (float) y;
        points[destIndex * 3 + 2] = (float) z;
    }

    private int getPointNewIndex(int pointIndex) {
        int index = reindex[pointIndex] - 1;
        if (index == -1) {
            index = newPointIndex;
            reindex[pointIndex] = index + 1;
            newPointIndex++;
            calcControlPoint(pointIndex, index);
        }
        return index;
    }

    private int getPointNewIndex(Edge edge) {
        EdgeInfo edgeInfo = edgeInfos.get(edge);
        int index = edgeInfo.newIndex - 1;
        if (index == -1) {
            index = newPointIndex;
            edgeInfo.newIndex = index + 1;
            newPointIndex++;
            points[index * 3] = (float) edgeInfo.edgePoint.getX();
            points[index * 3 + 1] = (float) edgeInfo.edgePoint.getY();
            points[index * 3 + 2] = (float) edgeInfo.edgePoint.getZ();
        }
        return index;
    }

    private int getPointNewIndex(FaceInfo faceInfo) {
        int index = faceInfo.newIndex - 1;
        if (index == -1) {
            index = newPointIndex;
            faceInfo.newIndex = index + 1;
            newPointIndex++;
            points[index * 3] = (float) faceInfo.point.getX();
            points[index * 3 + 1] = (float) faceInfo.point.getY();
            points[index * 3 + 2] = (float) faceInfo.point.getZ();
        }
        return index;
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
        int newIndex;
        List<FaceInfo> faces = new ArrayList<>(2);
    }
    
    private static class PointInfo {
        List<FaceInfo> faces = new ArrayList<>(4);
        Set<Edge> edges = new HashSet<>(4);
    }
    
    private static class FaceInfo {
        Point3D point;
        int newIndex;
        Edge[] edges;

        public FaceInfo(int n) {
            edges = new Edge[n];
        }
    }
}