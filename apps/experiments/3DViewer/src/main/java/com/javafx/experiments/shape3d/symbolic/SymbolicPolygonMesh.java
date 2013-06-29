package com.javafx.experiments.shape3d.symbolic;

import com.javafx.experiments.shape3d.PolygonMesh;

/**
 * Polygon mesh where the points are symbolic. That is, the values of the 
 * points depend on other variables and they can be updated appropriately.
 */
public class SymbolicPolygonMesh {
    public SymbolicPointArray points;
    public float[] texCoords;
    public int[][] faces;
    private int numEdgesInFaces = -1;

    public SymbolicPolygonMesh() { }
    
    public SymbolicPolygonMesh(SymbolicPointArray points, float[] texCoords, int[][] faces) {
        this.points = points;
        this.texCoords = texCoords;
        this.faces = faces;
    }
    
    public SymbolicPolygonMesh(PolygonMesh mesh) {
        this.points = new OriginalPointArray(mesh);
        this.texCoords = mesh.getTexCoords().toArray(this.texCoords);
        this.faces = mesh.faces;
    }
    
    public int getNumEdgesInFaces() {
        if (numEdgesInFaces == -1) {
            numEdgesInFaces = 0;
            for(int[] face : faces) {
                numEdgesInFaces += face.length;
            }
           numEdgesInFaces /= 2;
        }
        return numEdgesInFaces;
    }
}
