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
package com.javafx.experiments.shape3d;

import javafx.collections.FXCollections;
import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;

/**
 * A Mesh where each face can be a Polygon
 *
 * can convert to using ObservableIntegerArray
 */
public class PolygonMesh {
    private final ObservableFloatArray points = FXCollections.observableFloatArray();
    private final ObservableFloatArray texCoords = FXCollections.observableFloatArray();
    public int[][] faces = new int[0][0];
    private final ObservableIntegerArray faceSmoothingGroups = FXCollections.observableIntegerArray();
    protected int numEdgesInFaces = -1; // TODO invalidate automatically by listening to faces (whenever it is an observable)

    public PolygonMesh() {}

    public PolygonMesh(float[] points, float[] texCoords, int[][] faces) {
        this.points.addAll(points);
        this.texCoords.addAll(texCoords);
        this.faces = faces;
    }

    public ObservableFloatArray getPoints() {
        return points;
    }

    public ObservableFloatArray getTexCoords() {
        return texCoords;
    }

    public ObservableIntegerArray getFaceSmoothingGroups() {
        return faceSmoothingGroups;
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

    // TODO: Hardcode to constants for FX 8 (only one vertex format)
    private static final int NUM_COMPONENTS_PER_POINT = 3;
    private static final int NUM_COMPONENTS_PER_TEXCOORD = 2;
    private static final int NUM_COMPONENTS_PER_FACE = 6;

    public int getPointElementSize() {
        return NUM_COMPONENTS_PER_POINT;
    }

    public int getTexCoordElementSize() {
        return NUM_COMPONENTS_PER_TEXCOORD;
    }

    public int getFaceElementSize() {
        return NUM_COMPONENTS_PER_FACE;
    }
}
