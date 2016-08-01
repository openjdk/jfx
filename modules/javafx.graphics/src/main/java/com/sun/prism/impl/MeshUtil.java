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

import com.sun.javafx.geom.Quat4f;
import com.sun.javafx.geom.Vec2f;
import com.sun.javafx.geom.Vec3f;

/**
 * Utility routines for dealing with mesh computation.
 */
class MeshUtil {

    static final float NORMAL_WELD_COS = 0.9952f; // cos(5.6)
    static final float TANGENT_WELD_COS = 0.866f; // cos(30)
    static final float G_UV_PARALLEL = 0.9988f; // cos(2.8125)
    static final float COS_1_DEGREE = 0.9998477f;
    static final float BIG_ENOUGH_NORMA2 = 0.0625f; // 1.f/16
    static final double PI = 3.1415926535897932384626433832795;
    static final float INV_SQRT2 = 0.7071067812f;
    static final float DEAD_FACE = 9.094947E-13f; // 1.f/1024/1024/1024/1024
    static final float MAGIC_SMALL = 1E-10f; // 0.000001
    static final float COS110 = -0.33333334f; // -1.f / 3

    private MeshUtil() {
    }

    static boolean isDeadFace(float areaSquared) { // one square millimeter
        return areaSquared < DEAD_FACE;
    }

    static boolean isDeadFace(int[] f) {
        return f[0] == f[1] || f[1] == f[2] || f[2] == f[0];
    }

    static boolean isNormalAlmostEqual(Vec3f n1, Vec3f n2) {
        return n1.dot(n2) >= COS_1_DEGREE;
    }

    static boolean isTangentOk(Vec3f[] t1, Vec3f[] t2) {
        return t1[0].dot(t2[0]) >= NORMAL_WELD_COS
                && t1[1].dot(t2[1]) >= TANGENT_WELD_COS
                && t1[2].dot(t2[2]) >= TANGENT_WELD_COS;
    }

    static boolean isNormalOkAfterWeld(Vec3f normalSum) {
        return normalSum.dot(normalSum) > BIG_ENOUGH_NORMA2;
    }

    /*
     * Sum all tangets spaces inside sm group and test is if still ok
     */
    static boolean isTangentOK(Vec3f[] nSum) {
        return isTangentOk(nSum, nSum);
    }

    static boolean isOppositeLookingNormals(Vec3f[] n1, Vec3f[] n2) {
        float cosPhi = n1[0].dot(n2[0]);
        return cosPhi < COS110;
    }

    static float fabs(float x) {
        return x < 0 ? -x : x;
    }

    // Note: b will be modified to return the result and a remains unchanged.
    static void getOrt(Vec3f a, Vec3f b) {
        //return a ^ preOrt ^ a;
        b.cross(a, b);
        b.cross(b, a);
    }

    static void orthogonalizeTB(Vec3f[] norm) {
        // N,T,B:  N preserved, T and B get orthogonalized to N
        // N = norm[0], T = norm[1] and B = norm[2]
        getOrt(norm[0], norm[1]);
        getOrt(norm[0], norm[2]);
        norm[1].normalize();
        norm[2].normalize();
    }

    static void computeTBNNormalized(Vec3f pa, Vec3f pb, Vec3f pc,
            Vec2f ta, Vec2f tb, Vec2f tc, Vec3f[] norm) {
        MeshTempState instance = MeshTempState.getInstance();
        Vec3f n = instance.vec3f1;
        Vec3f v1 = instance.vec3f2;
        Vec3f v2 = instance.vec3f3;

        // compute Normal |(v1-v0)X(v2-v0)|
        v1.sub(pb, pa);
        v2.sub(pc, pa);
        n.cross(v1, v2);
        norm[0].set(n);
        norm[0].normalize(); // TODO: make sure each triangle area (size) will be considered

        v1.set(0, tb.x - ta.x, tb.y - ta.y);
        v2.set(0, tc.x - ta.x, tc.y - ta.y);

        if (v1.y * v2.z == v1.z * v2.y) {
            MeshUtil.generateTB(pa, pb, pc, norm);
            return;
        }

        // compute Tangent and Binomal
        v1.x = pb.x - pa.x;
        v2.x = pc.x - pa.x;
        n.cross(v1, v2);
        norm[1].x = -n.y / n.x;
        norm[2].x = -n.z / n.x;

        v1.x = pb.y - pa.y;
        v2.x = pc.y - pa.y;
        n.cross(v1, v2);
        norm[1].y = -n.y / n.x;
        norm[2].y = -n.z / n.x;

        v1.x = pb.z - pa.z;
        v2.x = pc.z - pa.z;
        n.cross(v1, v2);
        norm[1].z = -n.y / n.x;
        norm[2].z = -n.z / n.x;

        norm[1].normalize();
        norm[2].normalize();
    }

    /*
     * Fix TB if T and B go almost in parralel.
     * If T (ntb[1]) and B (ntb[2]) is almost parallel, invent something
     * artificial in NTB.
     *
     * This method assumes that T and B are normalized.
     */
    static void fixParallelTB(Vec3f[] ntb) {
        MeshTempState instance = MeshTempState.getInstance();
        Vec3f median = instance.vec3f1;
        median.add(ntb[1], ntb[2]);
        Vec3f ort = instance.vec3f2;
        ort.cross(ntb[0], median);
        median.normalize();
        ort.normalize();

        //ntb[1] = (median + ort) * invSqrt2;
        ntb[1].add(median, ort);
        ntb[1].mul(INV_SQRT2);

        //ntb[2] = (median - ort) * invSqrt2;
        ntb[2].sub(median, ort);
        ntb[2].mul(INV_SQRT2);
    }

    /*
     * Generate artificial tangent for un-textured face
     */
    static void generateTB(Vec3f v0, Vec3f v1, Vec3f v2, Vec3f[] ntb) {
        MeshTempState instance = MeshTempState.getInstance();
        Vec3f a = instance.vec3f1;
        a.sub(v1, v0);
        Vec3f b = instance.vec3f2;
        b.sub(v2, v0);

        if (a.dot(a) > b.dot(b)) {
            ntb[1].set(a);
            ntb[1].normalize(); // TODO: make sure each triangle area (size) will be considered
            ntb[2].cross(ntb[0], ntb[1]);
        } else {
            ntb[2].set(b);
            ntb[2].normalize(); // TODO: make sure each triangle area (size) will be considered
            ntb[1].cross(ntb[2], ntb[0]);
        }
    }

    static double clamp(double x, double min, double max) {
        return x < max ? (x > min ? x : min) : max;
    }

    static void fixTSpace(Vec3f[] norm) {
        float nNorm = norm[0].length();

        MeshTempState instance = MeshTempState.getInstance();
        Vec3f n1 = instance.vec3f1;
        n1.set(norm[1]);
        Vec3f n2 = instance.vec3f2;
        n2.set(norm[2]);
        getOrt(norm[0], n1);
        getOrt(norm[0], n2);

        float n1Length = n1.length();
        float n2Length = n2.length();

        double cosPhi = (n1.dot(n2)) / (n1Length * n2Length);
        Vec3f e1 = instance.vec3f3;
        Vec3f e2 = instance.vec3f4;

        if (fabs((float) cosPhi) > 0.998) {
            Vec3f n2fix = instance.vec3f5;
            n2fix.cross(norm[0], n1);
            n2fix.normalize();

            e2.set(n2fix);
            if (n2fix.dot(n2) < 0) {
                e2.mul(-1);
            }
            e1.set(n1);
            e1.mul(1f / n1Length);
        } else {
            double phi = Math.acos(clamp(cosPhi, -1, 1));
            double alpha = (PI * 0.5 - phi) * 0.5;
            Vec2f e1Local = instance.vec2f1;
            e1Local.set((float) Math.sin(alpha), (float) Math.cos(alpha));
            Vec2f e2Local = instance.vec2f2;
            e2Local.set((float) Math.sin(alpha + phi), (float) Math.cos(alpha + phi));

            Vec3f n1T = instance.vec3f5;
            n1T.set(n2);
            getOrt(n1, n1T);
            float n1TLength = n1T.length();

            // e1 = float(e1_local.y/l1) * n1 - float(e1_local.x/l_n1T) * n1T;
            e1.set(n1);
            e1.mul(e1Local.y / n1Length);

            Vec3f n1TT = instance.vec3f6;
            n1TT.set(n1T);
            n1TT.mul(e1Local.x / n1TLength);
            e1.sub(n1TT);

            // e2 = float(e2_local.y/l1) * n1 + float(e2_local.x/l_n1T) * n1T;
            e2.set(n1);
            e2.mul(e2Local.y / n1Length);

            // Recycle n1TT for temp computation
            n1TT.set(n1T);
            n1TT.mul(e2Local.x / n1TLength);
            e2.add(n1TT);

            float e1DotN1 = e1.dot(n1);
            float e2DotN2 = e2.dot(n2);

            // This will interfer with degenerated triangle unit test.
            // assert ((e1DotN1 / n1Length - e2DotN2 / n2Length) < 0.001);
        }

        norm[1].set(e1);
        norm[2].set(e2);
        norm[0].mul(1f / nNorm);
    }

    static void buildQuat(Vec3f[] tm, Quat4f quat) {
        MeshTempState instance = MeshTempState.getInstance();
        float[][] m = instance.matrix;
        float[] tmp = instance.vector;

        for (int i = 0; i < 3; i++) {
            m[i][0] = tm[i].x;
            m[i][1] = tm[i].y;
            m[i][2] = tm[i].z;
        }

        float trace = m[0][0] + m[1][1] + m[2][2];

        if (trace > 0) {
            float s = (float) Math.sqrt(trace + 1.0f);
            float t = 0.5f / s;
            quat.w = 0.5f * s;
            quat.x = (m[1][2] - m[2][1]) * t;
            quat.y = (m[2][0] - m[0][2]) * t;
            quat.z = (m[0][1] - m[1][0]) * t;

        } else {
            int[] next = {1, 2, 0};
            int i = 0;

            if (m[1][1] > m[0][0]) {
                i = 1;
            }
            if (m[2][2] > m[i][i]) {
                i = 2;
            }

            int j = next[i], k = next[j];
            float s = (float) Math.sqrt(m[i][i] - m[j][j] - m[k][k] + 1.0f);

            if (m[j][k] < m[k][j]) {
                s = -s;
            }

            float t = 0.5f / s;

            tmp[i] = 0.5f * s;
            quat.w = (m[j][k] - m[k][j]) * t;
            tmp[j] = (m[i][j] + m[j][i]) * t;
            tmp[k] = (m[i][k] + m[k][i]) * t;
            quat.x = tmp[0];
            quat.y = tmp[1];
            quat.z = tmp[2];
        }
    }
}

