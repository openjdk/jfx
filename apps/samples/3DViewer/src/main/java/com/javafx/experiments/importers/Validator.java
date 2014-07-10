/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates.
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
package com.javafx.experiments.importers;

import javafx.collections.ObservableIntegerArray;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * Mesh data validator
 */
public class Validator {

    public void validate(Node node) {
        if (node instanceof MeshView) {
            MeshView meshView = (MeshView) node;
            validate(meshView.getMesh());
        } else if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                validate(child);
            }
        }
    }

    public void validate(Mesh mesh) {
        if (!(mesh instanceof TriangleMesh)) {
            throw new AssertionError("Mesh is not TriangleMesh: " + mesh.getClass() + ", mesh = " + mesh);
        }
        TriangleMesh tMesh = (TriangleMesh) mesh;
        int numPoints = tMesh.getPoints().size() / tMesh.getPointElementSize();
        int numTexCoords = tMesh.getTexCoords().size() / tMesh.getTexCoordElementSize();
        int numFaces = tMesh.getFaces().size() / tMesh.getFaceElementSize();
        if (numPoints == 0 || numPoints * tMesh.getPointElementSize() != tMesh.getPoints().size()) {
            throw new AssertionError("Points array size is not correct: " + tMesh.getPoints().size());
        }
        if (numTexCoords == 0 || numTexCoords * tMesh.getTexCoordElementSize() != tMesh.getTexCoords().size()) {
            throw new AssertionError("TexCoords array size is not correct: " + tMesh.getPoints().size());
        }
        if (numFaces == 0 || numFaces * tMesh.getFaceElementSize() != tMesh.getFaces().size()) {
            throw new AssertionError("Faces array size is not correct: " + tMesh.getPoints().size());
        }
        if (numFaces != tMesh.getFaceSmoothingGroups().size() && tMesh.getFaceSmoothingGroups().size() > 0) {
            throw new AssertionError("FaceSmoothingGroups array size is not correct: " + tMesh.getPoints().size() + ", numFaces = " + numFaces);
        }
        ObservableIntegerArray faces = tMesh.getFaces();
        for (int i = 0; i < faces.size(); i += 2) {
            int pIndex = faces.get(i);
            if (pIndex < 0 || pIndex > numPoints) {
                throw new AssertionError("Incorrect point index: " + pIndex + ", numPoints = " + numPoints);
            }
            int tcIndex = faces.get(i + 1);
            if (tcIndex < 0 || tcIndex > numTexCoords) {
                throw new AssertionError("Incorrect texCoord index: " + tcIndex + ", numTexCoords = " + numTexCoords);
            }
        }
//        System.out.println("Validation successfull of " + mesh);
    }

}
