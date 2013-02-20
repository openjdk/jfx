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
import com.sun.prism.MeshFactory;
import javafx.scene.shape.TriangleMesh;

/**
 * TODO: 3D - Need documentation
 */
public class NGTriangleMesh implements PGTriangleMesh {
    private boolean meshDirty = true;
    private Mesh nativeObject;

    private float[] points;     // x,y,z interleaved
    private float[] texCoords;  // u,v interleaved
    private int[] faces;        // v1,v2,v3 interleaved (where v = {point, texCoord})
    private int[] faceSmoothingGroups; // face smoothing group 
    
    protected Mesh getNativeObject(MeshFactory meshFactory) {
        if (nativeObject == null) {
            nativeObject = meshFactory.createMesh();
            meshDirty = true;
        }
        updateNativeIfNeeded();
        return nativeObject;        
    }

    public void updateNativeIfNeeded() {
        if (meshDirty) {
            Mesh.Geometry g = new Mesh.Geometry();
            g.pos = points;
            g.uv = texCoords;
            g.faces = faces;
            g.smoothing = faceSmoothingGroups;
            if (!nativeObject.buildGeometry(g)) {
                throw new RuntimeException("nativeObject.buildGeometry failed");
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

    public void setPoints(int index, float[] points, int start, int length) {
        meshDirty = true;
        if (points == null) {
            this.points = null;
            return;
        }

        // Range check were done in the FX layer.
        int startOffset = start * TriangleMesh.NUM_COMPONENTS_PER_POINT;
        int indexOffset = index * TriangleMesh.NUM_COMPONENTS_PER_POINT;
        int lengthInFloatUnit = length * TriangleMesh.NUM_COMPONENTS_PER_POINT;
        System.arraycopy(points, startOffset, this.points, indexOffset, lengthInFloatUnit);
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

    public void setTexCoords(int index, float[] texCoords, int start, int length) {
        meshDirty = true;
        if (texCoords == null) {
            this.texCoords = null;
            return;
        }

        // Range check were done in the FX layer.
        int startOffset = start * TriangleMesh.NUM_COMPONENTS_PER_TEXCOORD;
        int indexOffset = index * TriangleMesh.NUM_COMPONENTS_PER_TEXCOORD;
        int lengthInFloatUnit = length * TriangleMesh.NUM_COMPONENTS_PER_TEXCOORD;
        System.arraycopy(texCoords, startOffset, this.texCoords, indexOffset, lengthInFloatUnit);
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

    public void setFaces(int index, int[] faces, int start, int length) {
        meshDirty = true;
        if (faces == null) {
            this.faces = null;
            return;
        }

        // Range check were done in the FX layer.
        int startOffset = start * TriangleMesh.NUM_COMPONENTS_PER_FACE;
        int lengthInIntUnit = length * TriangleMesh.NUM_COMPONENTS_PER_FACE;
        int indexOffset = index * TriangleMesh.NUM_COMPONENTS_PER_FACE;
        System.arraycopy(faces, startOffset, this.faces, indexOffset, lengthInIntUnit);
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
        System.arraycopy(faceSmoothingGroups, 0, this.faceSmoothingGroups, 0,
                this.faceSmoothingGroups.length);
    }

    public void setFaceSmoothingGroups(int index, int[] faceSmoothingGroups, int start, int length) {
        meshDirty = true;
        if (faceSmoothingGroups == null) {
            this.faceSmoothingGroups = null;
            return;
        }

        // Range check were done in the FX layer.
        System.arraycopy(faceSmoothingGroups, start, this.faceSmoothingGroups, index, length);
    }
    
}
