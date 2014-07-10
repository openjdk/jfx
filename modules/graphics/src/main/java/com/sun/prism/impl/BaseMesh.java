/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.Quat4f;
import com.sun.javafx.geom.Vec2f;
import com.sun.javafx.geom.Vec3f;
import com.sun.prism.Mesh;
import java.util.Arrays;
import java.util.HashMap;
import sun.util.logging.PlatformLogger;

/**
 * TODO: 3D - Need documentation
 */
public abstract class BaseMesh extends BaseGraphicsResource implements Mesh {

    private int nVerts;
    private int nTVerts;
    private int nFaces;
    private float[] pos;
    private float[] uv;
    private int[] faces;
    private int[] smoothing;
    private boolean allSameSmoothing;
    private boolean allHardEdges;
    
    //pos (3 floats), tex (2 floats) and norm (4 float)
    protected static final int VERTEX_SIZE = 9;
    protected static final int POINT_SIZE = 3;
    protected static final int TEXCOORD_SIZE = 2;

    // Data members container for a single face
    //    Vec3i pVerts;
    //    Vec3i tVerts;
    //    int  smGroup;
    public static enum FaceMembers {
        POINT0, TEXCOORD0, POINT1, TEXCOORD1, POINT2, TEXCOORD2, SMOOTHING_GROUP
    };
    public static final int FACE_MEMBERS_SIZE = 7;

    protected BaseMesh(Disposer.Record disposerRecord) {
        super(disposerRecord);
    }

    public abstract boolean buildNativeGeometry(float[] vertexBuffer, 
            int vertexBufferLength, int[] indexBufferInt, int indexBufferLength);

    public abstract boolean buildNativeGeometry(float[] vertexBuffer,
            int vertexBufferLength, short[] indexBufferShort, int indexBufferLength);
    
    private boolean updateSkipMeshNormalGeometry(int[] posFromAndLengthIndices, int[] uvFromAndLengthIndices) {

        // Find out the list of modified tex coords.
        int startTexCoord = uvFromAndLengthIndices[0] / TEXCOORD_SIZE;
        int numTexCoords = (uvFromAndLengthIndices[1] / TEXCOORD_SIZE);
        if ((uvFromAndLengthIndices[1] % TEXCOORD_SIZE) > 0) {
            numTexCoords++;
        }

        if (numTexCoords > 0) {
            for (int i = 0; i < numTexCoords; i++) {
                int texCoordOffset = (startTexCoord + i) * TEXCOORD_SIZE;
                MeshGeomComp2VB mt2vb = (MeshGeomComp2VB) texCoord2vbMap.get(texCoordOffset);
                assert mt2vb != null;
                // mt2vb shouldn't be null. We can't have a texCoord referred by 
                // the faces array that isn't in the vertexBuffer.
                if (mt2vb != null) {
                    int[] locs = mt2vb.getLocs();
                    int validLocs = mt2vb.getValidLocs();
                    if (locs != null) {
                        for (int j = 0; j < validLocs; j++) {
                            int vbIndex = (locs[j] * VERTEX_SIZE) + POINT_SIZE;
                            vertexBuffer[vbIndex] = uv[texCoordOffset];
                            vertexBuffer[vbIndex + 1] = uv[texCoordOffset + 1];
                        }
                    } else {
                        int loc = mt2vb.getLoc();
                        int vbIndex = (loc * VERTEX_SIZE) + POINT_SIZE;
                        vertexBuffer[vbIndex] = uv[texCoordOffset];
                        vertexBuffer[vbIndex + 1] = uv[texCoordOffset + 1];
                    }                    
                }
            }
        }

        // Find out the list of modified points
        int startPoint = posFromAndLengthIndices[0] / POINT_SIZE;
        int numPoints = (posFromAndLengthIndices[1] / POINT_SIZE);
        if ((posFromAndLengthIndices[1] % POINT_SIZE) > 0) {
            numPoints++;
        }

        if (numPoints > 0) {
            for (int i = 0; i < numPoints; i++) {
                int pointOffset = (startPoint + i) * POINT_SIZE;
                MeshGeomComp2VB mp2vb = (MeshGeomComp2VB) point2vbMap.get(pointOffset);
                assert mp2vb != null;
                // mp2vb shouldn't be null. We can't have a point referred by
                // the faces array that isn't in the vertexBuffer.
                if (mp2vb != null) {
                    int[] locs = mp2vb.getLocs();
                    int validLocs = mp2vb.getValidLocs();
                    if (locs != null) {
                        for (int j = 0; j < validLocs; j++) {
                            int vbIndex = locs[j] * VERTEX_SIZE;
                            vertexBuffer[vbIndex] = pos[pointOffset];
                            vertexBuffer[vbIndex + 1] = pos[pointOffset + 1];
                            vertexBuffer[vbIndex + 2] = pos[pointOffset + 2];
                        }
                    } else {
                        int loc = mp2vb.getLoc();
                        int vbIndex = loc * VERTEX_SIZE;
                            vertexBuffer[vbIndex] = pos[pointOffset];
                            vertexBuffer[vbIndex + 1] = pos[pointOffset + 1];
                            vertexBuffer[vbIndex + 2] = pos[pointOffset + 2];
                    }                    
                }
            }
        }

        if (indexBuffer != null) {
            return buildNativeGeometry(vertexBuffer,
                    numberOfVertices * VERTEX_SIZE, indexBuffer, nFaces * 3);
        } else {
            return buildNativeGeometry(vertexBuffer,
                    numberOfVertices * VERTEX_SIZE, indexBufferShort, nFaces * 3);
        }        
    }
    
    private float[] vertexBuffer;
    private int[] indexBuffer;
    private short[] indexBufferShort;
    private int numberOfVertices;

    private HashMap<Integer, MeshGeomComp2VB> point2vbMap;
    private HashMap<Integer, MeshGeomComp2VB> texCoord2vbMap;
    
    private boolean buildSkipMeshNormalGeometry() { 
            
        HashMap<Long, Integer> face2vbMap = new HashMap();

        if (point2vbMap == null) {
            point2vbMap = new HashMap();
        } else {
            point2vbMap.clear();
        }
        if (texCoord2vbMap == null) {
            texCoord2vbMap = new HashMap();
        } else {
            texCoord2vbMap.clear();
        }
        
        Integer mf2vb;
        BaseMesh.MeshGeomComp2VB mp2vb;
        BaseMesh.MeshGeomComp2VB mt2vb;
        vertexBuffer = new float[nVerts * VERTEX_SIZE];
        indexBuffer = new int[nFaces * 3];
        int ibCount = 0;
        int vbCount = 0;

        for (int faceCount = 0; faceCount < nFaces; faceCount++) {
            int faceIndex = faceCount * 6;
            for (int i = 0; i < 3; i++) {
                int vertexIndex = i * 2;
                int pointIndex = faceIndex + vertexIndex;
                int texCoordIndex = pointIndex + 1;
                long key = (long) ((long) (faces[pointIndex]) << 32 | faces[texCoordIndex]);
                mf2vb = (Integer) face2vbMap.get(key);
                if (mf2vb == null) {
                    mf2vb = vbCount / VERTEX_SIZE;

                    face2vbMap.put(key, mf2vb);
                    if (vertexBuffer.length <= vbCount) {
                        float[] temp = new float[vbCount + 10 * VERTEX_SIZE]; // Let's increment by 10
                        System.arraycopy(vertexBuffer, 0, temp, 0, vertexBuffer.length);
                        vertexBuffer = temp;
                    }
                    int pointOffset = faces[pointIndex] * POINT_SIZE;
                    int texCoordOffset = faces[texCoordIndex] * TEXCOORD_SIZE;
                    vertexBuffer[vbCount] = pos[pointOffset];
                    vertexBuffer[vbCount + 1] = pos[pointOffset + 1];
                    vertexBuffer[vbCount + 2] = pos[pointOffset + 2];
                    vertexBuffer[vbCount + 3] = uv[texCoordOffset];
                    vertexBuffer[vbCount + 4] = uv[texCoordOffset + 1];
                    vertexBuffer[vbCount + 5] = 0;
                    vertexBuffer[vbCount + 6] = 0;
                    vertexBuffer[vbCount + 7] = 0;
                    vertexBuffer[vbCount + 8] = 0;                   
                    vbCount += VERTEX_SIZE;
 
                    mp2vb = point2vbMap.get(pointOffset);
                    if (mp2vb == null) {
                        // create 
                        mp2vb = new MeshGeomComp2VB(pointOffset, mf2vb);
                        point2vbMap.put(pointOffset, mp2vb);
                    } else {
                        // addLoc
                        mp2vb.addLoc(mf2vb);
                    }
                    
                    mt2vb = texCoord2vbMap.get(texCoordOffset);
                    if (mt2vb == null) {
                        // create 
                        mt2vb = new MeshGeomComp2VB(texCoordOffset, mf2vb);
                        texCoord2vbMap.put(texCoordOffset, mt2vb);
                    } else {
                        // addLoc
                        mt2vb.addLoc(mf2vb);
                    }
                }
                
                // Construct IndexBuffer
                indexBuffer[ibCount++] = mf2vb;
            }
        }

        numberOfVertices = vbCount / VERTEX_SIZE;
        
        if (numberOfVertices > 0x10000) { // > 64K
            return buildNativeGeometry(vertexBuffer,
                    numberOfVertices * VERTEX_SIZE, indexBuffer, nFaces * 3);
        } else {
             
            if (indexBufferShort == null || indexBufferShort.length < nFaces * 3) {
                indexBufferShort = new short[nFaces * 3];
            }
            int ii = 0;
            for (int i = 0; i < nFaces; i++) {
                indexBufferShort[ii] = (short) indexBuffer[ii++]; 
                indexBufferShort[ii] = (short) indexBuffer[ii++]; 
                indexBufferShort[ii] = (short) indexBuffer[ii++]; 
            }
            indexBuffer = null; // free 
            return buildNativeGeometry(vertexBuffer,
                    numberOfVertices * VERTEX_SIZE, indexBufferShort, nFaces * 3);
        }                
    }
    
    @Override
    public boolean buildGeometry(float[] pos, int[] posFromAndLengthIndices,
            float[] uv, int[] uvFromAndLengthIndices,
            int[] faces, int[] facesFromAndLengthIndices,
            int[] smoothing, int[] smoothingFromAndLengthIndices) {
        nVerts = pos.length / 3;
        nTVerts = uv.length / 2;
        nFaces = faces.length / 6;
        assert nVerts > 0 && nFaces > 0 && nTVerts > 0;
        this.pos = pos;
        this.uv = uv;
        this.faces = faces;
        this.smoothing = smoothing.length == nFaces ? smoothing : null;

        if (PrismSettings.skipMeshNormalComputation) {
            boolean updatePoints = posFromAndLengthIndices[1] > 0;
            boolean updateTexCoords = uvFromAndLengthIndices[1] > 0;
            boolean updateFaces = facesFromAndLengthIndices[1] > 0;
            boolean updateSmoothing = smoothingFromAndLengthIndices[1] > 0;

            // First time creation
            boolean buildGeom = !(updatePoints || updateTexCoords || updateFaces || updateSmoothing);

            // We will need to rebuild if there is a change to faces or smoothing
            if (updateFaces || updateSmoothing) {
                buildGeom = true;
            }

            if ((!buildGeom) && (vertexBuffer != null)
                    && ((indexBuffer != null) || (indexBufferShort != null))) {
                return updateSkipMeshNormalGeometry(posFromAndLengthIndices, uvFromAndLengthIndices);
            }

            return buildSkipMeshNormalGeometry();
        }

        MeshTempState instance = MeshTempState.getInstance();
        // big pool for all possible vertices
        if (instance.pool == null || instance.pool.length < nFaces * 3) {            
            instance.pool = new MeshVertex[nFaces * 3];
        }

        if (instance.indexBuffer == null || instance.indexBuffer.length < nFaces * 3) {
            instance.indexBuffer = new int[nFaces * 3];
        }

        if (instance.pVertex == null || instance.pVertex.length < nVerts) {
            instance.pVertex = new MeshVertex[nVerts];
        } else {
            Arrays.fill(instance.pVertex, 0, instance.pVertex.length, null);
        }
             
        // check if all hard edges or all smooth  
        checkSmoothingGroup();

        // compute [N, T, B] for each face
        computeTBNormal(instance.pool, instance.pVertex, instance.indexBuffer);

        // process sm and weld points
        int nNewVerts = MeshVertex.processVertices(instance.pVertex, nVerts,
                allHardEdges, allSameSmoothing);

        if (instance.vertexBuffer == null
                || instance.vertexBuffer.length < nNewVerts * VERTEX_SIZE) {
            instance.vertexBuffer = new float[nNewVerts * VERTEX_SIZE];
        }
        buildVertexBuffer(instance.pVertex, instance.vertexBuffer);
        
        if (nNewVerts > 0x10000) {
            buildIndexBuffer(instance.pool, instance.indexBuffer, null);
            return buildNativeGeometry(instance.vertexBuffer,
                    nNewVerts * VERTEX_SIZE, instance.indexBuffer, nFaces * 3);
        } else {
            if (instance.indexBufferShort == null || instance.indexBufferShort.length < nFaces * 3) {
                instance.indexBufferShort = new short[nFaces * 3];
            }
            buildIndexBuffer(instance.pool, instance.indexBuffer, instance.indexBufferShort);
            return buildNativeGeometry(instance.vertexBuffer,
                    nNewVerts * VERTEX_SIZE, instance.indexBufferShort, nFaces * 3);
        }
    }

    private void computeTBNormal(MeshVertex[] pool, MeshVertex[] pVertex, int[] indexBuffer) {
        MeshTempState instance = MeshTempState.getInstance();
        
        // tmp variables
        int[] smFace = instance.smFace;
        int[] triVerts = instance.triVerts;
        Vec3f[] triPoints = instance.triPoints;
        Vec2f[] triTexCoords = instance.triTexCoords;
        Vec3f[] n = instance.norm;
        final String logname = BaseMesh.class.getName(); 

        for (int f = 0, nDeadFaces = 0, poolIndex = 0; f < nFaces; f++) {
            int index = f * 3;

            smFace = getFace(f, smFace); // copy from mesh to tmp smFace

            // Get tex. point. index
            triVerts[0] = smFace[BaseMesh.FaceMembers.POINT0.ordinal()];
            triVerts[1] = smFace[BaseMesh.FaceMembers.POINT1.ordinal()];
            triVerts[2] = smFace[BaseMesh.FaceMembers.POINT2.ordinal()];

            if (MeshUtil.isDeadFace(triVerts)
                    && PlatformLogger.getLogger(logname).isLoggable(PlatformLogger.Level.FINE)) {
                // Log degenerated triangle
                nDeadFaces++;
                PlatformLogger.getLogger(logname).fine("Dead face ["
                        + triVerts[0] + ", " + triVerts[1] + ", " + triVerts[2]
                        + "] @ face group " + f + "; nEmptyFaces = " + nDeadFaces);
            }

            for (int i = 0; i < 3; i++) {
                triPoints[i] = getVertex(triVerts[i], triPoints[i]);
            }

            // Get tex. coord. index
            triVerts[0] = smFace[BaseMesh.FaceMembers.TEXCOORD0.ordinal()];
            triVerts[1] = smFace[BaseMesh.FaceMembers.TEXCOORD1.ordinal()];
            triVerts[2] = smFace[BaseMesh.FaceMembers.TEXCOORD2.ordinal()];

            for (int i = 0; i < 3; i++) {
                triTexCoords[i] = getTVertex(triVerts[i], triTexCoords[i]);
            }

            MeshUtil.computeTBNNormalized(triPoints[0], triPoints[1], triPoints[2],
                                          triTexCoords[0], triTexCoords[1], triTexCoords[2],
                                          n);

            for (int j = 0; j < 3; ++j) {
                pool[poolIndex] = (pool[poolIndex] == null) ? new MeshVertex() : pool[poolIndex];

                for (int i = 0; i < 3; ++i) {
                    pool[poolIndex].norm[i].set(n[i]);
                }
                pool[poolIndex].smGroup = smFace[BaseMesh.FaceMembers.SMOOTHING_GROUP.ordinal()];
                pool[poolIndex].fIdx = f;
                pool[poolIndex].tVert = triVerts[j];
                pool[poolIndex].index = MeshVertex.IDX_UNDEFINED;
                int ii = j == 0 ? BaseMesh.FaceMembers.POINT0.ordinal()
                        : j == 1 ? BaseMesh.FaceMembers.POINT1.ordinal()
                        : BaseMesh.FaceMembers.POINT2.ordinal();
                int pIdx = smFace[ii];
                pool[poolIndex].pVert = pIdx;
                indexBuffer[index + j] = pIdx;
                pool[poolIndex].next = pVertex[pIdx];
                pVertex[pIdx] = pool[poolIndex];
                poolIndex++;
            }
        }
    }

    private void buildVSQuat(Vec3f[] tm, Quat4f quat) {
        Vec3f v = MeshTempState.getInstance().vec3f1;
        v.cross(tm[1], tm[2]);
        float d = tm[0].dot(v);
        if (d < 0) {
            tm[2].mul(-1);
        }

        MeshUtil.buildQuat(tm, quat);

        // This will interfer with degenerated triangle unit test. 
        // assert (quat.w >= 0);

        if (d < 0) {
            if (quat.w == 0) {
                quat.w = MeshUtil.MAGIC_SMALL;
            }
            quat.scale(-1);
        }
    }

    private void buildVertexBuffer(MeshVertex[] pVerts, float[] vertexBuffer) {
        Quat4f quat = MeshTempState.getInstance().quat;
        int idLast = 0;

        for (int i = 0, index = 0; i < nVerts; ++i) {
            MeshVertex v = pVerts[i];
            for (; v != null; v = v.next) {
                if (v.index == idLast) {
                    int ind = v.pVert * 3;
                    vertexBuffer[index++] = pos[ind];
                    vertexBuffer[index++] = pos[ind + 1];
                    vertexBuffer[index++] = pos[ind + 2];
                    ind = v.tVert * 2;
                    vertexBuffer[index++] = uv[ind];
                    vertexBuffer[index++] = uv[ind + 1];
                    buildVSQuat(v.norm, quat);
                    vertexBuffer[index++] = quat.x;
                    vertexBuffer[index++] = quat.y;
                    vertexBuffer[index++] = quat.z;
                    vertexBuffer[index++] = quat.w;
                    idLast++;
                }
            }
        }
    }

    private void buildIndexBuffer(MeshVertex[] pool, int[] indexBuffer, short[] indexBufferShort) {
        for (int i = 0; i < nFaces; ++i) {
            int index = i * 3;
            if (indexBuffer[index] != MeshVertex.IDX_UNDEFINED) {
                for (int j = 0; j < 3; ++j) {
                    assert (pool[index].fIdx == i);
                    if (indexBufferShort != null) {
                        indexBufferShort[index + j] = (short) pool[index + j].index;
                    } else {
                        indexBuffer[index + j] = pool[index + j].index;
                    }
                    pool[index + j].next = null; // release reference
                }
            } else {
                for (int j = 0; j < 3; ++j) {
                    if (indexBufferShort != null) {
                        indexBufferShort[index + j] = 0;
                    } else {
                        indexBuffer[index + j] = 0;
                    }
                }
            }
        }
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

    private void checkSmoothingGroup() {
        if (smoothing == null || smoothing.length == 0) { // all smooth
            allSameSmoothing = true;
            allHardEdges = false;
            return;
        }

        for (int i = 0; i + 1 < smoothing.length; i++) {
            if (smoothing[i] != smoothing[i + 1]) {
                // various SmGroup
                allSameSmoothing = false;
                allHardEdges = false;
                return;
            }
        }

        if (smoothing[0] == 0) { // all hard edges
            allSameSmoothing = false;
            allHardEdges = true;
        } else { // all belongs to one group == all smooth
            allSameSmoothing = true;
            allHardEdges = false;
        }
    }

    public int[] getFace(int fIdx, int[] face) {
        int index = fIdx * 6;
        if ((face == null) || (face.length < FACE_MEMBERS_SIZE)) {
            face = new int[FACE_MEMBERS_SIZE];
        }
        // Note: Order matter, [0, 5] == FaceMembers' points and texcoords
        for (int i = 0; i < 6; i++) {
            face[i] = faces[index + i];
        }
        // Note: Order matter, 6 == FaceMembers.SMOOTHING_GROUP.ordinal()
        // There is a total of 32 smoothing groups.
        // Assign to 1st smoothing group if smoothing is null.
        face[6] = smoothing != null ? smoothing[fIdx] : 1;
        return face;
    }

    class MeshGeomComp2VB {

        private final int key; // point or texCoord index
        private final int loc; // the first index into vertex buffer
        private int[] locs;
        private int validLocs;

        MeshGeomComp2VB(int key, int loc) {
            assert loc >= 0;
            this.key = key;
            this.loc = loc;
            locs = null;
            validLocs = 0;
        }

        void addLoc(int loc) {
            if (locs == null) {
                locs = new int[3]; // edge of mesh case
                locs[0] = this.loc;
                locs[1] = loc;
                this.validLocs = 2;
            } else if (locs.length > validLocs) {
                locs[validLocs] = loc;
                validLocs++;
            } else {
                int[] temp = new int[validLocs * 2];
                System.arraycopy(locs, 0, temp, 0, locs.length);
                locs = temp;
                locs[validLocs] = loc;
                validLocs++;
            }
        }

        int getKey() {
            return key;
        }

        int getLoc() {
            return loc;
        }

        int[] getLocs() {
            return locs;
        }

        int getValidLocs() {
            return validLocs;
        }
    }

}
