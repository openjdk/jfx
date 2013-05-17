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

package com.sun.prism.impl;

import com.sun.javafx.geom.Vec2f;
import com.sun.javafx.geom.Vec3f;
import com.sun.prism.Mesh;

/**
 * TODO: 3D - Need documentation
 *  
 * TODO: 3D - This is a direct port of the 3D Mesh prototype.
 *       Need to rename members and methods.
 *       This code is poorly written and performance badly on Java.
 *       We should replace it with an implementation that is well suit for Java
 *       and is maintainable. 
 * JIRA ID: RT-29542 - FX 8 3D: Mesh computation code needs major clean up or redo
 */
public abstract class BaseMesh extends BaseGraphicsResource implements Mesh {

    private int nVerts;
    private int nTVerts;
    private int nFaces;
    private float pos[];
    private float uv[];
    private int faces[];
    private int smoothing[];
    private MeshNormal meshNormal;

    //pos (3 floats), tex (2 floats) and norm (4 float)
    protected static final int VERTEX_SIZE = 9;

    // Data members container for a single face
    //    Vec3i pVerts;
    //    Vec3i tVerts;
    //    int  smGroup;
    public static enum FaceMembers {
        POINT0, TEXCOORD0, POINT1, TEXCOORD1, POINT2, TEXCOORD2, SMOOTHING_GROUP
    };
    public static final int MAX_FACE_MEMBERS = 7;

    protected BaseMesh(Disposer.Record disposerRecord) {
        super(disposerRecord);
    }

    public abstract boolean buildNativeGeometry();

    @Override
    public boolean buildGeometry(float[] pos, float[] uv, int[] faces, int[] smoothing) {
        nVerts = pos.length / 3;
        nTVerts = uv.length / 2;
        nFaces = faces.length / 6;
        assert nVerts > 0 && nFaces > 0 && nTVerts > 0;
        this.pos = pos;
        this.uv = uv;
        this.faces = faces;
        this.smoothing = smoothing != null && smoothing.length >= nFaces ? smoothing : null;

        // System.err.println("*********** MeshBase.buildGeometry() .....");
        // MeshData.cc
        //(1) MeshNomralsReference tan = computeTangentBNormal(mesh);
        meshNormal = new MeshNormal(this);

        //(2) buildGeometry( FacetMeshWrapper<WORD>(mesh, tan.getPtr()) );
        //    if (ok) setDrawCall();
        //    return ok;
        return buildNativeGeometry();
    }

    private static Vec2f tmp2f = new Vec2f();
    private static Vec3f tmp3f = new Vec3f();
    private int fillOVertex(float pv[], int index, MeshNTBVertex ntb) {
        tmp3f = getVertex(ntb.pVert, tmp3f);
        pv[index++] = tmp3f.x;
        pv[index++] = tmp3f.y;
        pv[index++] = tmp3f.z;
        tmp2f = getTVertex(ntb.tVert, tmp2f);
        pv[index++] = tmp2f.x;
        pv[index++] = tmp2f.y;
        pv[index++] = ntb.tbn.v.x;
        pv[index++] = ntb.tbn.v.y;
        pv[index++] = ntb.tbn.v.z;
        pv[index++] = ntb.tbn.w;
        return index;
    }

    private void copyOVerts(float pv[]) {
        int nv = meshNormal.getNumVerts();
        int index = 0;
        for (int i = 0; i != nv; ++i) {
            index = fillOVertex(pv, index, meshNormal.getTangent(i));
        }
    }

    void copyIndices(int pi[]) {
        int nf = meshNormal.getNumFaces();
        int face[] = new int[3];
        int index = 0;
        for (int i = 0; i != nf; ++i) {
            face = meshNormal.getFace(i, face);
            pi[index++] = face[0];
            pi[index++] = face[1];
            pi[index++] = face[2];
        }
    }

    private int getNVertsGM() {
        return meshNormal.getNumVerts();
    }

    private int getNIndicesGM() {
        return meshNormal.getNumFaces() * 3;
    }

    final protected float[] getVertsGM() {
        int numVerts = getNVertsGM();
        float vertexBuffer[] = new float[numVerts * VERTEX_SIZE];
        copyOVerts(vertexBuffer);
        return vertexBuffer;
    }

    final protected int[] getIndexGM() {
        int numIndices = getNIndicesGM();
        int indexBuffer[] = new int[numIndices];
        copyIndices(indexBuffer);
        return indexBuffer;
    }

    public int getNumVerts() {
        return nVerts;
    }

    public int getNumTVerts() {
        return nTVerts;
    }

    public int getNumFaces() {
        return nFaces;
    }

    public Vec3f getVertex(int pIdx, Vec3f vertex) {
        if (vertex == null) {
            vertex = new Vec3f();
        }
        int index = pIdx * 3;
        vertex.set(pos[index], pos[index + 1], pos[index + 2]);
        return vertex;
    }

    public Vec2f getTVertex(int tIdx, Vec2f texCoord) {
        if (texCoord == null) {
            texCoord = new Vec2f();
        }
        int index = tIdx * 2;
        texCoord.set(uv[index], uv[index + 1]);
        return texCoord;
    }

    public int[] getFace(int fIdx, int face[]) {
        int index = fIdx * 6;
        if ((face == null) && (face.length < MAX_FACE_MEMBERS)) {
            face = new int[MAX_FACE_MEMBERS];
        }
        if (faces[index] < nVerts
                && faces[index + 2] < nVerts
                && faces[index + 4] < nVerts
                && faces[index + 1] < nTVerts
                && faces[index + 3] < nTVerts
                && faces[index + 5] < nTVerts) {
            // Note: Order matter, [0, 5] == FaceMembers' points and texcoords
            for (int i = 0; i < 6; i++) {
                face[i] = faces[index + i];
            }
            // Note: Order matter, 6 == FaceMembers.SMOOTHING_GROUP.ordinal()
            // There is a total of 32 smoothing groups.
            // Assign to 1st smoothing group if smoothing is null.
            face[6] = smoothing != null ? smoothing[fIdx] : 1;
        } else {
            // Note: Order matter, [0, 5] == FaceMembers' points and texcoords
            for (int i = 0; i < 6; i++) {
                face[i] = 0;
            }
            // Note: Order matter, 6 == FaceMembers.SMOOTHING_GROUP.ordinal()
            face[6] = 1;
        }
        return face;
    }

}
