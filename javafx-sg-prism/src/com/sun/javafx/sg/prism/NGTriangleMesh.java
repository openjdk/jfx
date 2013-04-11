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

package com.sun.javafx.sg.prism;

import com.sun.javafx.sg.PGTriangleMesh;
import com.sun.prism.Mesh;
import com.sun.prism.ResourceFactory;
import javafx.scene.shape.TriangleMesh;

/**
 * TODO: 3D - Need documentation
 */
public class NGTriangleMesh implements PGTriangleMesh {
    private boolean meshDirty = true;
    private Mesh mesh;

    // points is an array of x,y,z interleaved
    private float[] points;

    // texCoords is an array of u,v interleaved
    private float[] texCoords;

    // faces is an array of v1,v2,v3 interleaved (where v = {point, texCoord})
    private int[] faces;

    // faceSmoothingGroups is an array of face smoothing group values
    private int[] faceSmoothingGroups;

    Mesh createMesh(ResourceFactory rf) {
        if (mesh == null) {
            mesh = rf.createMesh();
            meshDirty = true;
        }
        validate();
        return mesh;        
    }

    void validate() {
        if (meshDirty) {
            if (!mesh.buildGeometry(points, texCoords, faces, faceSmoothingGroups)) {
                throw new RuntimeException("NGTriangleMesh: buildGeometry failed");
            }
            meshDirty = false;
        }
    }

    // Note: This method is intentionally made package scope for security
    // reason. It is created for internal use only. 
    // Do not make it a public method without careful consideration.
    void setPointsByRef(float[] points) {
        meshDirty = true;
        this.points = points;
    }

    // Note: This method is intentionally made package scope for security
    // reason. It is created for internal use only. 
    // Do not make it a public method without careful consideration.
    void setTexCoordsByRef(float[] texCoords) {
        meshDirty = true;
        this.texCoords = texCoords;
    }
    
    // Note: This method is intentionally made package scope for security
    // reason. It is created for internal use only. 
    // Do not make it a public method without careful consideration.
    void setFacesByRef(int[] faces) {
        meshDirty = true;
        this.faces = faces;
    }

    // Note: This method is intentionally made package scope for security
    // reason. It is created for internal use only. 
    // Do not make it a public method without careful consideration.
    void setFaceSmoothingGroupsByRef(int[] faceSmoothingGroups) {
        meshDirty = true;
        this.faceSmoothingGroups = faceSmoothingGroups;
    }

    public void setPoints(float[] points) {
        meshDirty = true;
        if (points == null) {
            this.points = null;
            return;
        }
        if ((this.points == null) || (this.points.length != points.length)) {
            this.points = new float[points.length];
        }
        System.arraycopy(points, 0, this.points, 0, this.points.length);      
    }

    public void setPoints(float[] points, int index, int length) {
        meshDirty = true;
        if (points == null) {
            this.points = null;
            return;
        }

        // Range check were done in the FX layer.
        int indexOffset = index * TriangleMesh.NUM_COMPONENTS_PER_POINT;
        int lengthInFloatUnit = length * TriangleMesh.NUM_COMPONENTS_PER_POINT;
        System.arraycopy(points, indexOffset, this.points, indexOffset, lengthInFloatUnit);
    }

    public void setTexCoords(float[] texCoords) {
        meshDirty = true;
        if (texCoords == null) {
            this.texCoords = null;
            return;
        }
        if ((this.texCoords == null) || (this.texCoords.length != texCoords.length)) {
            this.texCoords = new float[texCoords.length];
        }
        System.arraycopy(texCoords, 0, this.texCoords, 0, this.texCoords.length);
    }

    public void setTexCoords(float[] texCoords, int index, int length) {
        meshDirty = true;
        if (texCoords == null) {
            this.texCoords = null;
            return;
        }

        // Range check were done in the FX layer.
        int indexOffset = index * TriangleMesh.NUM_COMPONENTS_PER_TEXCOORD;
        int lengthInFloatUnit = length * TriangleMesh.NUM_COMPONENTS_PER_TEXCOORD;
        System.arraycopy(texCoords, indexOffset, this.texCoords, indexOffset, lengthInFloatUnit);
    }

    public void setFaces(int[] faces) {
        meshDirty = true;
        if (faces == null) {
            this.faces = null;
            return;
        }
        if ((this.faces == null) || (this.faces.length != faces.length)) {
            this.faces = new int[faces.length];
        }
        System.arraycopy(faces, 0, this.faces, 0, this.faces.length);
    }

    public void setFaces(int[] faces, int index, int length) {
        meshDirty = true;
        if (faces == null) {
            this.faces = null;
            return;
        }

        // Range check were done in the FX layer.
        int indexOffset = index * TriangleMesh.NUM_COMPONENTS_PER_FACE;
        int lengthInIntUnit = length * TriangleMesh.NUM_COMPONENTS_PER_FACE;
        System.arraycopy(faces, indexOffset, this.faces, indexOffset, lengthInIntUnit);
    }

    public void setFaceSmoothingGroups(int[] faceSmoothingGroups) {
        meshDirty = true;
        if (faceSmoothingGroups == null) {
            this.faceSmoothingGroups = null;
            return;
        }
        if ((this.faceSmoothingGroups == null) || 
                (this.faceSmoothingGroups.length != faceSmoothingGroups.length)) {
            this.faceSmoothingGroups = new int[faceSmoothingGroups.length];
        }

        for (int i = 0; i < faceSmoothingGroups.length; i++) {
            this.faceSmoothingGroups[i] = 1 << faceSmoothingGroups[i];
        }
    }

    public void setFaceSmoothingGroups(int[] faceSmoothingGroups, int index, int length) {
        meshDirty = true;
        if (faceSmoothingGroups == null) {
            this.faceSmoothingGroups = null;
            return;
        }

        // Range check were done in the FX layer.
        int toIndex = index + length;
        for (int i = index; i < toIndex; i++) {
            this.faceSmoothingGroups[i] = 1 << faceSmoothingGroups[i];
        }
    }

    // NOTE: This method is used for unit test purpose only.
    int[] test_getShiftedFaceSmoothingGroups() {
        return this.faceSmoothingGroups;
    }
}
