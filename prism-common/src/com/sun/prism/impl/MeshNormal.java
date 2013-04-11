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

/**
 * TODO: 3D - Need documentation
 * Utility routines for dealing with mesh computation.
 * TODO: 3D - This is a direct port of the 3D Mesh prototype.
 *       Need to rename members and methods.
 *       This code is poorly written and performance badly on Java.
 *       We should replace it with an implementation that is well suit for Java
 *       and is maintainable.
 * JIRA ID: RT-29542 - FX 8 3D: Mesh computation code needs major clean up or redo
 */
class MeshNormal {
    private BaseMesh mesh;
    private int nFaces, nVerts;
    //Vec3iPtr  faces;
    private int faces[]; // nFaces * 3
    private MeshNTBVertex verts[];

    private static Vec3f tmp1 = new Vec3f();
    private static Vec3f tmp2 = new Vec3f();

    MeshNormal(BaseMesh mesh) {
        this.mesh = mesh;
        compute();
    }

    int  getNumVerts() {
        return nVerts;
    }

    int  getNumFaces() {
        return nFaces;
    }

    MeshNTBVertex getTangent(int i) {
        assert( i < nVerts );
        return verts[i];
    }

    int[] getFace(int i, int[] face) {
        assert( i < nFaces );
        if (face == null) {
            face = new int[3];
        }
        int index = i * 3;
        face[0] = faces[index];
        face[1] = faces[index + 1];
        face[2] = faces[index + 2];
        return face;
    }

    private void compute() {
        int nOldVerts = mesh.getNumVerts();

        nFaces = mesh.getNumFaces();

        // big pool for all possible vertices
        MeshVertex[] pool = new MeshVertex[nFaces * 3];
        for (int i = 0; i < pool.length; i++) {
            pool[i] = new MeshVertex();
        }

        // faces validity map
        boolean[] validFaces = new boolean[nFaces];

        // list of vertices for each point (use as pointer)
        MeshVertex[] pVerts = new MeshVertex[nOldVerts];

//        System.err.println("nOldVerts = " + nOldVerts + ", nFaces = " + nFaces);
//        System.err.println("\n ************** Before ******************\n");
//        for (int i = 0; i < nOldVerts; i++) {
//            System.err.println("pVerts[" + i + "] = " + pVerts[i]);
//        }
//        for(int i = 0; i < nFaces; i++) {
//            System.err.println("validFaces[" + i + "] = " + validFaces[i]);
//        }
//        for(int i = 0; i < (nFaces * 3); i++) {
//            System.err.println("pool[" + i + "] = " + pool[i]);
//        }
//        System.err.println("\n ************** Enter ******************\n");

        collect(pool, pVerts, validFaces);

//        System.err.println("\n ************** After ******************\n");
//        for (int i = 0; i < nOldVerts; i++) {
//            System.err.println("pVerts[" + i + "] = " + pVerts[i]);
//        }
//        for(int i = 0; i < nFaces; i++) {
//            System.err.println("validFaces[" + i + "] = " + validFaces[i]);
//        }
//        for(int i = 0; i < (nFaces * 3); i++) {
//            System.err.println("pool[" + i + "] = " + pool[i]);
//        }

        int nU[] = new int[1];
        int nNewVerts = 0;

        for (int i=0; i!=nOldVerts; ++i) {
            if (pVerts[i] != null) {
//                System.err.println("Before ************ nU[0] = " + nU[0] + ", nNewVerts = " + nNewVerts);
                nNewVerts = MeshVertex.processVertices(pVerts[i], nNewVerts, nU);
//                System.err.println("After ************ nU[0] = " + nU[0] + ", nNewVerts = " + nNewVerts);
            }
        }

        buildVetrices(nOldVerts, nNewVerts, pVerts);
        buildFaces(pVerts, validFaces);

        nVerts = nNewVerts;
    }

    // returns number of good faces
    // does copy faces information into this->faces
    private void collect(MeshVertex pool[], MeshVertex pVerts[],
            boolean validFaces[])
    {
        Vec3f n[] = new Vec3f[3];
        for (int i = 0; i < 3; i++) {
            n[i] = new Vec3f();
        }

        int nEmptyFaces = 0;

        // faces = new Vec3i[nFaces];
        faces = new int[nFaces * 3];
        int smFace[] = new int[BaseMesh.MAX_FACE_MEMBERS];
        int triVerts[] = new int[3];
        Vec3f triPoints[] = new Vec3f[3];
        Vec2f triTexCoords[] = new Vec2f[3];
        Vec3f N = new Vec3f();

        int poolIndex = 0;
        for (int f = 0; f < nFaces; f++) {
            //faces[f].set(0,0,0);
            int index = f * 3;
            for (int i = 0; i < 3; i++) {
                faces[index + i] = 0;
            }
            validFaces[f] = false;

            //SmFace &smFace = mesh.getFace(f);
            smFace = mesh.getFace(f, smFace);

            triVerts[0] = smFace[BaseMesh.FaceMembers.POINT0.ordinal()];
            triVerts[1] = smFace[BaseMesh.FaceMembers.POINT1.ordinal()];
            triVerts[2] = smFace[BaseMesh.FaceMembers.POINT2.ordinal()];
            if (MeshUtil.isDeadFace(triVerts)) {
                nEmptyFaces++;
                continue;
            }
            for (int i = 0; i < 3; i++) {
                triPoints[i] = mesh.getVertex(triVerts[i], triPoints[i]);
            }

            // Vec3f N = (v1-v0)^(v2-v1);
            tmp1.sub(triPoints[1], triPoints[0]);
            tmp2.sub(triPoints[2], triPoints[1]);
            N.cross(tmp1, tmp2);
            float areaSquared = N.dot(N);
//            System.err.println("areaSquared = " + areaSquared);

            if (MeshUtil.isDeadFace(areaSquared)) {
                nEmptyFaces++;
                //LogFace("Wrong", f, mesh.faces[f]);
                continue;
            }

            for (int i = 0; i < 3; i++) {
                faces[index + i] = triVerts[i];
            }
            validFaces[f] = true;

            n[0].set(N);
            n[0].normalize();

            boolean valid;

            // Get tex. coord. index
            triVerts[0] = smFace[BaseMesh.FaceMembers.TEXCOORD0.ordinal()];
            triVerts[1] = smFace[BaseMesh.FaceMembers.TEXCOORD1.ordinal()];
            triVerts[2] = smFace[BaseMesh.FaceMembers.TEXCOORD2.ordinal()];

            triTexCoords[0] = mesh.getTVertex(triVerts[0], triTexCoords[0]);
            triTexCoords[1] = mesh.getTVertex(triVerts[1], triTexCoords[1]);
            triTexCoords[2] = mesh.getTVertex(triVerts[2], triTexCoords[2]);

            if (valid = MeshUtil.computeUVNormalized(triPoints[0], triPoints[1], triPoints[2],
                    triTexCoords[0], triTexCoords[1], triTexCoords[2], n[1], n[2])) {

                if (MeshUtil.fabs(n[1].dot(n[2])) > MeshUtil.gUVParralel) {
                    // U and V go almost in parallel
                    MeshUtil.fixParrallelTB(n);
                }
            } else {
                MeshUtil.generateTB(triPoints[0], triPoints[1], triPoints[2], n);
                // face has no valid mapping
            }

            for (int j=0; j!=3; ++j) {
                MeshVertex vNew = pool[poolIndex++];

                for (int i=0; i!=3; ++i) {
                    vNew.norm[i].set(n[i]);
                }
                vNew.smGroup = smFace[BaseMesh.FaceMembers.SMOOTHING_GROUP.ordinal()];
                vNew.initiazed = true;
                vNew.fIndex = f;
                vNew.valid = valid;
                vNew.pIdx = MeshVertex.idxUndef;
                int ii = j == 0 ? BaseMesh.FaceMembers.TEXCOORD0.ordinal() :
                        j == 1 ? BaseMesh.FaceMembers.TEXCOORD1.ordinal() :
                        BaseMesh.FaceMembers.TEXCOORD2.ordinal();
                vNew.tIndex = smFace[ii];
                ii = j == 0 ? BaseMesh.FaceMembers.POINT0.ordinal() :
                        j == 1 ? BaseMesh.FaceMembers.POINT1.ordinal() :
                        BaseMesh.FaceMembers.POINT2.ordinal();
                int pIdx = smFace[ii];
                vNew.next   = pVerts[pIdx];
                pVerts[pIdx] = vNew;
            }
        }
    }

    private static void buildVSQuat(Vec3f tm[], MeshQuat quat) {

        tmp1.cross(tm[1], tm[2]);
        float d = tm[0].dot(tmp1);
        if (d < 0) {
            tm[2].mul(-1);
        }

        quat.buildQuat(tm);

        assert( quat.w >= 0 );

        if (d<0) {
            if (quat.w == 0) {
                quat.w = MeshUtil.magicSmall;
            }
            quat.scale(-1);
        }
    }

    static int fillNewVertices(MeshVertex v, int idLast, int oldIndex, MeshNTBVertex vertices[]) {
        for (; v != null; v = v.next) {
            if (v.pIdx == idLast) {
                MeshNTBVertex newVert = vertices[idLast];
                newVert.pVert = oldIndex;
                newVert.tVert = v.tIndex;
                buildVSQuat(v.norm, newVert.tbn);
                idLast++;
            }
        }
        return idLast;
    }

    void buildVetrices( int nOldVerts, int nNewVerts, MeshVertex pVerts[]) {
        verts = new MeshNTBVertex[nNewVerts];
        for (int i = 0; i < nNewVerts; i++) {
            verts[i] = new MeshNTBVertex();
        }
        MeshNTBVertex vertices[] = verts;

        int idLast = 0;

        for (int i=0; i != nOldVerts; ++i) {
            MeshVertex v = pVerts[i];
            idLast = fillNewVertices(v, idLast, i, vertices);
            idLast = fillNewVertices(v, idLast, i, vertices);
        }

        assert ( idLast == nNewVerts );
    }

    void buildFaces(MeshVertex pVerts[], boolean validFaces[]) {
        for (int i = 0; i != nFaces; ++i) {
            if (validFaces[i]) {
                int index = i * 3;
                for (int j = 0; j != 3; ++j) {
                    faces[index + j] = MeshVertex.getIndex(pVerts[faces[index + j]], i);
                }
            }
        }
    }

}
