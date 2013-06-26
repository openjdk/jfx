/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.javafx.experiments.importers;

import javafx.collections.ObservableIntegerArray;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import static javafx.scene.shape.TriangleMesh.*;

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
        int numPoints = tMesh.getPoints().size() / NUM_COMPONENTS_PER_POINT;
        int numTexCoords = tMesh.getTexCoords().size() / NUM_COMPONENTS_PER_TEXCOORD;
        int numFaces = tMesh.getFaces().size() / NUM_COMPONENTS_PER_FACE;
        if (numPoints == 0 || numPoints * NUM_COMPONENTS_PER_POINT != tMesh.getPoints().size()) {
            throw new AssertionError("Points array size is not correct: " + tMesh.getPoints().size());
        }
        if (numTexCoords == 0 || numTexCoords * NUM_COMPONENTS_PER_TEXCOORD != tMesh.getTexCoords().size()) {
            throw new AssertionError("TexCoords array size is not correct: " + tMesh.getPoints().size());
        }
        if (numFaces == 0 || numFaces * NUM_COMPONENTS_PER_FACE != tMesh.getFaces().size()) {
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
