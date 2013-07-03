package com.javafx.experiments.shape3d.symbolic;

import java.util.Arrays;
import com.javafx.experiments.shape3d.SubdivisionMesh;
import javafx.scene.shape.TriangleMesh;

public class SubdividedPointArray extends SymbolicPointArray {
    private final float[] controlPoints; // points of the previous subdivision level
    private final int[][] controlInds; // indices corresponding to controlPoints
    private final float[][] controlFactors; // factors corresponding to controlPoints
    private final int[][] inds;
    private final float[][] factors;
    
    private final SubdivisionMesh.BoundaryMode boundaryMode;
    
    private int currPoint = 0;
    
    public SubdividedPointArray(SymbolicPointArray controlPointArray, int numPoints, SubdivisionMesh.BoundaryMode boundaryMode) {
        super(new float[TriangleMesh.NUM_COMPONENTS_PER_POINT*numPoints]);
        
        this.controlPoints = controlPointArray.data;
        this.controlInds = new int[numPoints][];
        this.controlFactors = new float[numPoints][];
        this.inds = new int[numPoints][];
        this.factors = new float[numPoints][];
        
        this.boundaryMode = boundaryMode;
    }
    
    
    public int addFacePoint(int[] vertices) {
        controlInds[currPoint] = vertices;
        controlFactors[currPoint] = new float[vertices.length];
        Arrays.fill(controlFactors[currPoint], 1.0f/vertices.length);
        
        inds[currPoint] = new int[0];
        factors[currPoint] = new float[0];
        
        return currPoint++;
    }
    
    public int addEdgePoint(int[] facePoints, int fromPoint, int toPoint, boolean isBoundary) {
        if (isBoundary) {
            controlInds[currPoint] = new int[] {fromPoint, toPoint};
            controlFactors[currPoint] = new float[] {0.5f, 0.5f};

            inds[currPoint] = new int[0];
            factors[currPoint] = new float[0];
        } else {
            int n = facePoints.length + 2;
            controlInds[currPoint] = new int[] {fromPoint, toPoint};
            controlFactors[currPoint] = new float[] {1.0f/n, 1.0f/n};
            
            inds[currPoint] = facePoints;
            factors[currPoint] = new float[facePoints.length];
            Arrays.fill(factors[currPoint], 1.0f/n);
        }
        return currPoint++;
    }

    public int addControlPoint(int[] facePoints, int[] edgePoints, int[] fromEdgePoints, int[] toEdgePoints, boolean[] isEdgeBoundary, int origPoint, boolean isBoundary, boolean hasInternalEdge) {
        if (isBoundary) {
            if ((boundaryMode == SubdivisionMesh.BoundaryMode.CREASE_EDGES) || hasInternalEdge) {
                controlInds[currPoint] = new int[] {origPoint};
                controlFactors[currPoint] = new float[] {0.5f};
                
                int numBoundaryEdges = 0;
                for (int i = 0; i < edgePoints.length; i++) {
                    if (isEdgeBoundary[i]) {
                        numBoundaryEdges++;
                    }
                }
                inds[currPoint] = new int[numBoundaryEdges];
                factors[currPoint] = new float[numBoundaryEdges];
                int boundaryEdgeInd = 0;
                for (int i = 0; i < edgePoints.length; i++) {
                    if (isEdgeBoundary[i]) {
                        inds[currPoint][boundaryEdgeInd] = edgePoints[i];
                        factors[currPoint][boundaryEdgeInd] = 0.25f;
                        boundaryEdgeInd++;
                    }
                }
            } else {
                controlInds[currPoint] = new int[] {origPoint};
                controlFactors[currPoint] = new float[] {1.0f};

                inds[currPoint] = new int[0];
                factors[currPoint] = new float[0];
            }
        } else {
            int n = facePoints.length;
            
            controlInds[currPoint] = new int[1 + edgePoints.length*2];
            controlFactors[currPoint] = new float[1 + edgePoints.length*2];
            controlInds[currPoint][0] = origPoint;
            controlFactors[currPoint][0] = (n - 3.0f) / n;
            for (int i = 0; i < edgePoints.length; i++) {
                controlInds[currPoint][1+2*i] = fromEdgePoints[i];
                controlFactors[currPoint][1+2*i] = 1.0f/(n * n);
                controlInds[currPoint][1+2*i+1] = toEdgePoints[i];
                controlFactors[currPoint][1+2*i+1] = 1.0f/(n * n);
            }
            
            inds[currPoint] = facePoints;
            factors[currPoint] = new float[facePoints.length];
            Arrays.fill(factors[currPoint], 1.0f/(n * n));
        }
        return currPoint++;
    }
    
    @Override
    public void update() {
        int ci;
        float f;
        float x, y, z;
        for (int i = 0; i < numPoints; i++) {
            x = y = z = 0.0f;
            for (int j = 0; j < controlInds[i].length; j++) {
                ci = 3 * controlInds[i][j];
                f = controlFactors[i][j];
                x += controlPoints[ci] * f;
                y += controlPoints[ci + 1] * f;
                z += controlPoints[ci + 2] * f;
            }
            for (int j = 0; j < inds[i].length; j++) {
                ci = 3 * inds[i][j];
                f = factors[i][j];
                x += data[ci] * f;
                y += data[ci + 1] * f;
                z += data[ci + 2] * f;
            }
            data[3*i] = x;
            data[3*i+1] = y;
            data[3*i+2] = z;
        }
    }
}
