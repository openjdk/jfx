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
import static com.sun.prism.impl.BaseMesh.FACE_MEMBERS_SIZE;

/**************************************************************************
 *                                                                        *
 * Temporary state, used to reduce the occurrence of temporary garbage    *
 * while computing things such as Quat, Normal, Tangent or Bitangent.     *
 * Since these operations happen extremely often and must be very fast,   *
 * we need to reduce the load on the garbage collector.                   *
 *                                                                        *
 *************************************************************************/
final class MeshTempState {
    /**
     * Temporary Vec3fs used by MeshUtil and BaseMesh to compute/adjust normals.
     */
    final Vec3f vec3f1 = new Vec3f();
    final Vec3f vec3f2 = new Vec3f();
    final Vec3f vec3f3 = new Vec3f();
    final Vec3f vec3f4 = new Vec3f();
    final Vec3f vec3f5 = new Vec3f();
    final Vec3f vec3f6 = new Vec3f();

    /**
     * Temporary Vec2fs used by MeshUtil to fix tangent space.
     */
    final Vec2f vec2f1 = new Vec2f();
    final Vec2f vec2f2 = new Vec2f();

    /**
     * Temporary variables used by BaseMesh to compute TBN.
     */
    final int smFace[] = new int[FACE_MEMBERS_SIZE];
    final int triVerts[] = new int[3];
    final Vec3f triPoints[] = new Vec3f[3];
    final Vec2f triTexCoords[] = new Vec2f[3];
    final Vec3f[] triNormals = new Vec3f[3];
    final int triPointIndex[] = new int[3];
    final int triNormalIndex[] = new int[3];
    final int triTexCoordIndex[] = new int[3];

    /**
     * A temporary 3 by 3 float matrix used by BaseMesh to compute quat.
     */
    final float matrix[][] = new float[3][3];
    /**
     * A temporary float array used by BaseMesh to compute quat.
     */
    final float vector[] = new float[3];
    /**
     * A temporary Quat4f used by BaseMesh to build quat.
     */
    final Quat4f quat = new Quat4f();

    /**
     * A temporary MeshVertex array for all possible vertices.
     * Length: nFaces * 3
     */
    MeshVertex[] pool;

    /**
     * A temporary MeshVertex array
     * Length: nVerts
     */
    MeshVertex[] pVertex;

    /**
     * A temporary int indexBuffer array
     * Length: nFaces * 3
     */
    int[] indexBuffer;

    /**
     * A temporary short indexBuffer array
     * Length: nFaces * 3
     */
    short[] indexBufferShort;

    /**
     * A temporary vertexBuffer array
     * Length: nNewVerts * VERTEX_SIZE
     */
    float[] vertexBuffer;

    private static final ThreadLocal<MeshTempState> tempStateRef =
            new ThreadLocal<MeshTempState>() {
                @Override
                protected MeshTempState initialValue() {
                    return new MeshTempState();
                }
            };

    private MeshTempState() {
        for (int i = 0; i < 3; i++) {
            triNormals[i] = new Vec3f();
        }
    }

    static MeshTempState getInstance() {
        return tempStateRef.get();
    }
}

