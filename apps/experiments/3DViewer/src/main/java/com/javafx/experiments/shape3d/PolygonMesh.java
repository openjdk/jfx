package com.javafx.experiments.shape3d;

import javafx.collections.FXCollections;
import javafx.collections.ObservableFloatArray;

/**
 * A Mesh where each face can be a Polygon
 *
 * TODO convert to using ObservableFloatArray and ObservableIntegerArray
 */
public class PolygonMesh {
    private final ObservableFloatArray points = FXCollections.observableFloatArray();
    public float[] texCoords;
    public int[][] faces;
    private int numEdgesInFaces = -1;

    public PolygonMesh() {}

    public PolygonMesh(float[] points,float[] texCoords, int[][] faces) {
        this.points.addAll(points);
        this.texCoords = texCoords;
        this.faces = faces;
    }

    public ObservableFloatArray getPoints() {
        return points;
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
