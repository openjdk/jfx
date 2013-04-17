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

import com.sun.javafx.geom.Vec3f;

/**
 * TODO: 3D - Need documentation
 * Part of MeshBuildTB.cc (see MVertex)
 * Utility routines for dealing with mesh computation.
 * TODO: 3D - This is a direct port of the 3D Mesh prototype.
 *       Need to rename members and methods.
 *       This code is poorly written and performance badly on Java.
 *       We should replace it with an implementation that is well suit for Java
 *       and is maintainable.
 * JIRA ID: RT-29542 - FX 8 3D: Mesh computation code needs major clean up or redo
 */
class MeshVertex {

    // TODO: This is a direct port of the 3D Mesh prototype.
    //       Need to rename members and methods
    static final int idxUndef = -1;
    static final int idxSetSmooth = -2;
    static final int idxUnite = -3;

    boolean initiazed;
    int smGroup;
    Vec3f norm[] = null;
    boolean valid;
    int tIndex;
    int fIndex;
    int pIdx;
    MeshVertex next = null;

    MeshVertex() {
        pIdx = idxUndef;
        norm = new Vec3f[3];  //N T B
        for (int i = 0; i < norm.length; i++) {
            norm[i] = new Vec3f();
        }
        next = null;
    }

    private static boolean areNormalsUnique(MeshVertex v, MeshVertex head) {
        for (int sm = v.smGroup; head != v; head = head.next) {
            if ( head.smGroup == sm
                    && MeshUtil.isNormalAlmostEqual(v.norm[0], head.norm[0])) {
                return false;
            }
        }
        return true;
    }

    // calculate average normal for one smoothing group
    static void makeSmNormal(MeshVertex v) {
        Vec3f normalSum = new Vec3f(v.norm[0]);
        int sm = v.smGroup;

        for (MeshVertex i = v.next; i != null; i = i.next) {
            if (i.smGroup == sm && areNormalsUnique(i, v)) {
                normalSum.add(i.norm[0]);
            }
        }

        boolean normalOk = false;
        if (MeshUtil.isNormalOkAfterWeld(normalSum)) {
            normalSum.normalize();
            normalOk = true;
        } else {
            normalOk = false;
        }

        for (MeshVertex i = v; i != null; i = i.next) {
            if (i.smGroup == sm) {
                assert( i.pIdx == idxUndef );
                i.pIdx = idxSetSmooth;
                if (normalOk) {
                    i.norm[0].set(normalSum);
                    // orthogonalize and normalize after normal ajustment
                    MeshUtil.orthogonalizeTB(i.norm);
                }
            }
        }
    }

    static void makeSmNormals(MeshVertex v) {
        for (; v != null; v = v.next) {
            if (v.pIdx == idxUndef) {
                if (v.smGroup != 0) {
                    makeSmNormal(v);
                } else {
                    v.pIdx = idxSetSmooth;
                }
            }
        }
    }

    static boolean okToWeldVertsTB(MeshVertex a, MeshVertex b) {
        return a.tIndex == b.tIndex &&
            MeshUtil.isTangentOkToWeld(a.norm, b.norm);
    }

    // weld points and assigns new indexes
    // calculate new TB
    // return current number of points (last index +1)
    static int weldWithTB(MeshVertex v, int index, int[] nUnited, boolean valid) {
        int nU = 0;

        for (;v != null; v = v.next) {
            if (v.valid == valid && v.pIdx == idxSetSmooth) {
//                System.err.println("@@@@@ v = ");
//                dumpInfo(v);
                int nuLocal = 0;

                for (MeshVertex q = v; q.next != null; q = q.next) {
                    for (MeshVertex i = q.next; i != null; i = i.next) {
                        if (i.pIdx == idxSetSmooth) {
                            if (okToWeldVertsTB(v, i)) {
                                i.pIdx = idxUnite;
                                nuLocal++;
                            }
                        }
                    }
                }
                if (nuLocal != 0) {
                    v.pIdx = index;
                    Vec3f nSum[] = new Vec3f[3];
                    for (int i = 0; i < 3; i++) {
                        nSum[i] = new Vec3f(v.norm[i]);
                    }

                    for (MeshVertex i = v.next; i != null; i = i.next) {
                        if (i.pIdx == idxUnite) {
                            for (int n = 0; n < 3; ++n) {
                                nSum[n].add(i.norm[n]);
                            }
                        }
                    }

                    boolean tbOk = MeshUtil.isTangetGoodAfterWeld(nSum);

                    if (tbOk) {
//                        System.err.println("** nSum[0] = " + nSum[0]);
//                        System.err.println("** nSum[1] = " + nSum[1]);
//                        System.err.println("** nSum[2] = " + nSum[2]);
//                        System.err.println("X 1 X 1 X 1 X");
                        MeshUtil.fixTSpace(nSum);
//                        System.err.println("** nSum[0] = " + nSum[0]);
//                        System.err.println("** nSum[1] = " + nSum[1]);
//                        System.err.println("** nSum[2] = " + nSum[2]);
                        v.pIdx = index;
//                        System.err.println("^^^^^ (1) v = ");
//                        dumpInfo(v);
                        for (int n = 0; n != 3; ++n) {
                            v.norm[n].set(nSum[n]);
                        }
//                        System.err.println("^^^^^ (2) v = ");
//                        dumpInfo(v);
                        for (MeshVertex i = v.next; i != null; i = i.next) {
                            if (i.pIdx == idxUnite) {
//                                System.err.println("%%%%%%%%%%%%% %%%%%%%% %%%%%%%%%%%%");
                                i.pIdx = index;
                                i.norm[0].set(0, 0, 0);
                            }
                        }
//                        System.err.println("^^^^^ (3) v = ");
//                        dumpInfo(v);
                    } else {
                        // roll all back, unite failed
                        nuLocal = 0;
                        for (MeshVertex i = v.next; i != null; i = i.next) {
                            if (i.pIdx == idxUnite) {
                                i.pIdx = idxSetSmooth;
                            }
                        }
                    }
                }

                if (nuLocal == 0) {
//                    System.err.println("X 2 X 2 X 2 X");
                    // nothing to join, fix in-place
                    MeshUtil.fixTSpace(v.norm);
                    v.pIdx = index;
                }

                index++;
                nU += nuLocal;
            }
        }

        nUnited[0] += nU;
        return index;
    }

    static void correctSmIndex(MeshVertex n) {
        for (MeshVertex l = n; l != null; l = l.next) {
            boolean change = false;

            for (MeshVertex i = l.next; i != null; i = i.next) {
                if (((l.smGroup & i.smGroup) != 0) && (l.smGroup != i.smGroup)) {
                    l.smGroup = i.smGroup | l.smGroup;
                    i.smGroup = l.smGroup;
                    change = true;
                }
                if (!change) {
                    l = l.next;
                }
            }
        }
    }

    static void correctSmGroup(MeshVertex n) {
        correctSmIndex(n);

        // remove opposite looking normals from one smoothing group

        for (MeshVertex l = n; l.next != null; l = l.next) {
            if (l.smGroup == 0) {
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

    // the entry point
    // make all sm groups
    static int processVertices(MeshVertex n, int nVerts,
            int nUnited[]) {
//        System.err.println("In processVertices (1):");
//        dumpInfo(n);
        // assign correct smGroups
        correctSmGroup(n);
//        System.err.println("In processVertices (2):");
//        dumpInfo(n);
        // assign common normals for smGroups
        makeSmNormals(n);
//        System.err.println("In processVertices (3):");
//        dumpInfo(n);

        // process textured faces
        nVerts = weldWithTB(n, nVerts, nUnited, true);
//        System.err.println("In processVertices (4) ************ nUnited[0] = " + nUnited[0] + ", nVerts = " + nVerts);
//        dumpInfo(n);

        // process untextured faces
        nVerts = weldWithTB(n, nVerts, nUnited, false);
//        System.err.println("In processVertices (5) ************ nUnited[0] = " + nUnited[0] + ", nVerts = " + nVerts);
//        dumpInfo(n);
        return nVerts;
    }

    // return new index of vertext for i-th face
    static int getIndex(MeshVertex n, int fi) { // API Call
        MeshVertex v = n;
        while (v != null) {
            if (fi == v.fIndex) {
                return v.pIdx;
            }
            v = v.next;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "MeshVertex : " + getClass().getName()
                + "@0x" + Integer.toHexString(hashCode())
                + ":: smGroup = " + smGroup + "\n"
                + "\tnorm[0] = " + norm[0] + "\n"
                + "\tnorm[1] = " + norm[1] + "\n"
                + "\tnorm[2] = " + norm[2] + "\n"
                + "\tvalid = " + valid + ", tIndex = " + tIndex + ", fIndex = " + fIndex + "\n"
                + "\tpIdx = " + pIdx + ", initized = " + initiazed + "\n"
                + "\tnext = " + ((next == null) ? next : next.getClass().getName()
                + "@0x" + Integer.toHexString(next.hashCode())) + "\n";
    }

    static void dumpInfo(MeshVertex v) {
        System.err.println("** dumpInfo: " );
        for (MeshVertex q = v; q != null; q = q.next) {
            System.err.println(q);
        }
        System.err.println("***********************************");
    }
}
