package com.javafx.experiments.shape3d;

/**
 * A Mesh where each face can be a Polygon
 *
 * TODO convert to using ObservableFloatArray and ObservableIntegerArray
 */
public class PolygonMesh {
    public float[] points;
    public float[] texCoords;
    public int[][] faces;

    public PolygonMesh() {}

    public PolygonMesh(float[] points,float[] texCoords, int[][] faces) {
        this.points = points;
        this.texCoords = texCoords;
        this.faces = faces;
    }
}
