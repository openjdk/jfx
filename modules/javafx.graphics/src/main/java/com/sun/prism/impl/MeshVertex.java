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

import com.sun.javafx.geom.Vec3f;

/**
 * TODO: 3D - Need documentation
 * Utility routines for dealing with mesh computation.
 */
class MeshVertex {

    int smGroup;
    int pVert; // vertexBuffer
    int tVert; // vertexBuffer
    int fIdx;
    int index; // indexBuffer
    Vec3f[] norm; // {N, T, B}
    MeshVertex next = null;

    static final int IDX_UNDEFINED = -1;
    static final int IDX_SET_SMOOTH = -2;
    static final int IDX_UNITE = -3;

    MeshVertex() {
        norm = new Vec3f[3];
        for (int i = 0; i < norm.length; i++) {
            norm[i] = new Vec3f();
        }
    }

    static void avgSmNormals(MeshVertex v) {
        Vec3f normalSum = MeshTempState.getInstance().vec3f1;
        for (; v != null; v = v.next) {
            if (v.index == IDX_UNDEFINED) {
                normalSum.set(v.norm[0]);
                int sm = v.smGroup;

                for (MeshVertex i = v.next; i != null; i = i.next) {
                    if (i.smGroup == sm) {
                        assert (i.index == IDX_UNDEFINED);
                        i.index = IDX_SET_SMOOTH;
                        normalSum.add(i.norm[0]);
                    }
                }

                if (MeshUtil.isNormalOkAfterWeld(normalSum)) {
                    normalSum.normalize();
                    for (MeshVertex i = v; i != null; i = i.next) {
                        if (i.smGroup == sm) {
                            i.norm[0].set(normalSum);
                        }
                    }
                }
            }
        }
    }

    static boolean okToWeldVertsTB(MeshVertex a, MeshVertex b) {
        return a.tVert == b.tVert && MeshUtil.isTangentOk(a.norm, b.norm);
    }

    /*
     * Weld points, assign new indexes and calculate new TB
     * return current number of points (last index +1)
     */
    static int weldWithTB(MeshVertex v, int index) {
        Vec3f[] nSum = MeshTempState.getInstance().triNormals;
        for (; v != null; v = v.next) {
            if (v.index < 0) {
                int nuLocal = 0;
                for (int i = 0; i < 3; i++) {
                    nSum[i].set(v.norm[i]);
                }
                for (MeshVertex i = v.next; i != null; i = i.next) {
                    if (i.index < 0) {
                        if (okToWeldVertsTB(v, i)) {
                            i.index = IDX_UNITE;
                            nuLocal++;
                            for (int j = 0; j < 3; ++j) {
                                nSum[j].add(i.norm[j]);
                            }
                        }
                    }
                }

                if (nuLocal != 0) {
                    if (MeshUtil.isTangentOK(nSum)) {
                        MeshUtil.fixTSpace(nSum);
                        v.index = index;
                        for (int i = 0; i < 3; ++i) {
                            v.norm[i].set(nSum[i]);
                        }
                        for (MeshVertex i = v.next; i != null; i = i.next) {
                            if (i.index == IDX_UNITE) {
                                i.index = index;
                                i.norm[0].set(0, 0, 0);
                            }
                        }
                    } else {
                        // roll all back, unite failed
                        nuLocal = 0;
                    }
                }

                if (nuLocal == 0) {
                    // nothing to join, fix in-place
                    MeshUtil.fixTSpace(v.norm);
                    v.index = index;
                }
                index++;
            }
        }
        return index;
    }

    static void mergeSmIndexes(MeshVertex n) {
        for (MeshVertex l = n; l != null;) {
            boolean change = false;
            for (MeshVertex i = l.next; i != null; i = i.next) {
                if (((l.smGroup & i.smGroup) != 0) && (l.smGroup != i.smGroup)) {
                    l.smGroup = i.smGroup | l.smGroup;
                    i.smGroup = l.smGroup;
                    change = true;
                }
            }
            if (!change) {
                l = l.next;
            }
        }
    }

    /*
     * Check for normals in one smoothing group and remove points from the group
     * if they are opposite looking in order to prevent a normal to be zero in
     * one SM group. Opposite looking normals are normals which angle is more
     * than 110 degrees.
     */
    static void correctSmNormals(MeshVertex n) {
        // remove opposite looking normals from one smoothing group
        for (MeshVertex l = n; l != null; l = l.next) {
            if (l.smGroup != 0) {
                for (MeshVertex i = l.next; i != null; i = i.next) {
                    if (((i.smGroup & l.smGroup) != 0)
                            && MeshUtil.isOppositeLookingNormals(i.norm, l.norm)) {
                        l.smGroup = 0;
                        i.smGroup = 0;
                        break;
                    }
                }
            }
        }
    }

    static int processVertices(MeshVertex[] pVerts, int nVertex,
            boolean allHardEdges, boolean allSameSmoothing) {
        int nNewVerts = 0;
        Vec3f normalSum = MeshTempState.getInstance().vec3f1;
        for (int i = 0; i < nVertex; ++i) {
            if (pVerts[i] != null) {
                if (!allHardEdges) {
                    if (allSameSmoothing) {
                        // calculate average normal for one smoothing group
                        normalSum.set(pVerts[i].norm[0]);
                        for (MeshVertex v = pVerts[i].next; v != null; v = v.next) {
                            normalSum.add(v.norm[0]);
                        }

                        if (MeshUtil.isNormalOkAfterWeld(normalSum)) {
                            normalSum.normalize();
                            for (MeshVertex v = pVerts[i]; v != null; v = v.next) {
                                v.norm[0].set(normalSum);
                            }
                        }
                    } else {
                        // various Sm
                        mergeSmIndexes(pVerts[i]);
                        avgSmNormals(pVerts[i]);
                    }
                }
                // weld points based on texture and assign new indexes
                nNewVerts = weldWithTB(pVerts[i], nNewVerts);
            }
        }
        return nNewVerts;
    }

    @Override
    public String toString() {
        return "MeshVertex : " + getClass().getName()
                + "@0x" + Integer.toHexString(hashCode())
                + ":: smGroup = " + smGroup + "\n"
                + "\tnorm[0] = " + norm[0] + "\n"
                + "\tnorm[1] = " + norm[1] + "\n"
                + "\tnorm[2] = " + norm[2] + "\n"
                + "\ttIndex = " + tVert + ", fIndex = " + fIdx + "\n"
                + "\tpIdx = " + index + "\n"
                + "\tnext = " + ((next == null) ? next : next.getClass().getName()
                + "@0x" + Integer.toHexString(next.hashCode())) + "\n";
    }

    static void dumpInfo(MeshVertex v) {
        System.err.println("** dumpInfo: ");
        for (MeshVertex q = v; q != null; q = q.next) {
            System.err.println(q);
        }
        System.err.println("***********************************");
    }
}
