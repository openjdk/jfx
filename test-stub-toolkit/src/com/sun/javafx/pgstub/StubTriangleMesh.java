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

package com.sun.javafx.pgstub;

import com.sun.javafx.sg.PGTriangleMesh;
import javafx.scene.shape.TriangleMesh;

public class StubTriangleMesh implements PGTriangleMesh {

    private float[] points;
    private float[] texCoords;
    private int[] faces;
    private int[] smoothingGroups;


    @Override
    public void setPoints(float[] points) {
        if (points == null) {
            this.points = null;
            return;
        }
        this.points = new float[points.length];
        System.arraycopy(points, 0, this.points, 0, points.length);
    }

    @Override
    public void setPoints(float[] points, int index, int length) {
        if (points == null) {
            this.points = null;
            return;
        }

        int indexOffset = index * TriangleMesh.NUM_COMPONENTS_PER_POINT;
        int lengthInFloatUnit = length * TriangleMesh.NUM_COMPONENTS_PER_POINT;
        System.arraycopy(points, indexOffset, this.points, indexOffset, lengthInFloatUnit);
    }

    @Override
    public void setTexCoords(float[] texCoords) {
        if (texCoords == null) {
            this.texCoords = null;
            return;
        }
        this.texCoords = new float[texCoords.length];
        System.arraycopy(texCoords, 0, this.texCoords, 0, texCoords.length);
    }

    @Override
    public void setTexCoords(float[] texCoords, int index, int length) {
        if (texCoords == null) {
            this.texCoords = null;
            return;
        }
        int indexOffset = index * TriangleMesh.NUM_COMPONENTS_PER_TEXCOORD;
        int lengthInFloatUnit = length * TriangleMesh.NUM_COMPONENTS_PER_TEXCOORD;
        System.arraycopy(texCoords, indexOffset, this.texCoords, indexOffset, lengthInFloatUnit);
    }

    @Override
    public void setFaces(int[] faces) {
        if (faces == null) {
            this.faces = null;
            return;
        }
        this.faces = new int[faces.length];
        System.arraycopy(faces, 0, this.faces, 0, faces.length);
    }

    @Override
    public void setFaces(int[] faces, int index, int length) {
        if (faces == null) {
            this.faces = null;
            return;
        }
        int indexOffset = index * TriangleMesh.NUM_COMPONENTS_PER_FACE;
        int lengthInIntUnit = length * TriangleMesh.NUM_COMPONENTS_PER_FACE;
        System.arraycopy(faces, indexOffset, this.faces, indexOffset, lengthInIntUnit);
    }

    @Override
    public void setFaceSmoothingGroups(int[] faceSmoothingGroups) {
        if (faceSmoothingGroups == null) {
            this.smoothingGroups = null;
            return;
        }
        this.smoothingGroups = new int[faceSmoothingGroups.length];
        System.arraycopy(faceSmoothingGroups, 0, this.smoothingGroups, 0,
                faceSmoothingGroups.length);
    }

    @Override
    public void setFaceSmoothingGroups(int[] faceSmoothingGroups, int index, int length) {
        if (faceSmoothingGroups == null) {
            this.smoothingGroups = null;
            return;
        }

        System.arraycopy(faceSmoothingGroups, index, this.smoothingGroups, index, length);
    }
}
