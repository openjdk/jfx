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
 * Utility routines for dealing with mesh computation.
 * TODO: 3D - This is a direct port of the 3D Mesh prototype.
 *       Need to rename members and methods.
 *       This code is poorly written and performance badly on Java.
 *       We should replace it with an implementation that is well suit for Java
 *       and is maintainable.
 * JIRA ID: RT-29542 - FX 8 3D: Mesh computation code needs major clean up or redo
 */
class MeshUtil {

    static final float normalWeldCos  = 0.9952f; // cos(5.6)
    static final float tangentWeldCos = 0.866f; // cos(30)
    static final float gUVParralel    = 0.9988f; // cos(2.8125);
    static final float Cos1Degree     = 0.9998477f;
    static final float bigEnoughNorma2 = 1.f/16;
    static final double PI = 3.1415926535897932384626433832795;
    static final float invSqrt2 = 0.7071067812f;
    static final float DEAD_FACE = 1.f/1024/1024/1024/1024;
    static final float magicSmall = (float) 1E-10; // 0.000001;

    private MeshUtil() {
    }

    static boolean isDeadFace(float areaSquared) { // one square millimeter
        return areaSquared < DEAD_FACE;
    }

    static boolean isDeadFace(int f[]) {
        return f[0] == f[1] || f[1] == f[2] || f[2] == f[0];
    }

    static boolean isNormalAlmostEqual(Vec3f n1, Vec3f n2) {
        return n1.dot(n2) >= Cos1Degree;
    }

    static boolean isTangentOkToWeld(Vec3f t1[], Vec3f t2[]) {
        return t1[0].dot(t2[0]) >= normalWeldCos &&
                t1[1].dot(t2[1]) >= tangentWeldCos &&
                t1[2].dot(t2[2]) >= tangentWeldCos;
    }

    static boolean isTangetGoodAfterWeld(Vec3f t1[], Vec3f t2[]) {

        return t1[0].dot(t2[0]) >= normalWeldCos &&
                t1[1].dot(t2[1]) >= tangentWeldCos &&
                t1[2].dot(t2[2]) >= tangentWeldCos;
    }

    static boolean isNormalOkAfterWeld(Vec3f normalSum) {
        //return normalSum.Norma2() > bigEnoughNorma2;
        return normalSum.dot(normalSum) > bigEnoughNorma2;
    }

    // we summ all tangets spaces inside sm group and test is if still ok ...
    static boolean isTangetGoodAfterWeld(Vec3f nSum[]) {
        return isNormalOkAfterWeld(nSum[0])
                && isNormalOkAfterWeld(nSum[1])
                && isNormalOkAfterWeld(nSum[2]);
    }

    // check for normals in one smoothing group,
    // and remove points from the group if they are opposite looking
    // in order to prevent a normal to be zero in one SM group
    // opposite looking = angle more then 110 degrees
    static boolean isOppositeLookingNormals(Vec3f n1[], Vec3f n2[]) {
        float cos110 = -1.f / 3;
        float cosPhi = n1[0].dot(n2[0]);
        return cosPhi < cos110;
    }

    static float fabs(float x) {
        return x >= 0 ? x : -x;
    }

    static boolean _AssertSmall(float x) {
        return fabs(x) < (1 - Cos1Degree);
    }

    // Note: b will be modified to return the result and a remains unchanged.
    static void getOrt(Vec3f a, Vec3f b) {
        //return a ^ preOrt ^ a;
        b.cross(a, b);
        b.cross(b, a);
    }

    static void orthogonalizeTB(Vec3f norm[]) {
        // N,T,B:  N preserved, T and B get orthogonalized to N
        // N = norm[0], T = norm[1] and B = norm[2]
        getOrt(norm[0], norm[1]);
        getOrt(norm[0], norm[2]);
        norm[1].normalize();
        norm[2].normalize();
    }

    static boolean computeUVNormalized( Vec3f pa, Vec3f pb, Vec3f pc,
            Vec2f ta, Vec2f tb, Vec2f tc, Vec3f u, Vec3f v) {
        if (MeshUtil.computeTangBinorm(pa, pb, pc, ta, tb, tc, u, v)) {
            u.normalize();
            v.normalize();
            return true;
        }
        return false;
    }

    static boolean computeTangBinorm(Vec3f pa, Vec3f pb, Vec3f pc,
            Vec2f ta, Vec2f tb, Vec2f tc, Vec3f t, Vec3f b) {

        Vec3f v1 = new Vec3f(0, tb.x - ta.x, tb.y - ta.y);
        Vec3f v2 = new Vec3f(0, tc.x - ta.x, tc.y - ta.y);

        if (v1.y * v2.z == v1.z * v2.y) {
            return false;
        }

        Vec3f n = new Vec3f();
        v1.x = pb.x - pa.x;
        v2.x = pc.x - pa.x;
        n.cross(v1, v2);
        t.x = -n.y / n.x;
        b.x = -n.z / n.x;

        v1.x = pb.y - pa.y;
        v2.x = pc.y - pa.y;
        n.cross(v1, v2);
        t.y = -n.y / n.x;
        b.y = -n.z / n.x;

        v1.x = pb.z - pa.z;
        v2.x = pc.z - pa.z;
        n.cross(v1, v2);
        t.z = -n.y / n.x;
        b.z = -n.z / n.x;

        return true;
    }

    // fix TB if T and B go almost in parralel
    // T (ntb[1]) and B (ntb[2]) is almost parallel
    // lets invent something artificial in NTB
    // this function assumes that T and B are normalized
    static void fixParrallelTB(Vec3f ntb[]) {
        Vec3f median = new Vec3f(ntb[1]);
        median.add(ntb[2]);
        Vec3f ort = new Vec3f();
        ort.cross(ntb[0], median);
        median.normalize();
        ort.normalize();

        //ntb[1] = (median + ort) * invSqrt2;
        ntb[1].set(median);
        ntb[1].add(ort);
        ntb[1].mul(invSqrt2);

        //ntb[2] = (median - ort) * invSqrt2;
        ntb[2].set(median);
        ntb[2].sub(ort);
        ntb[2].mul(invSqrt2);
//        testOrthoNorm(ntb[0], ntb[1], ntb[2]);
    }

    // generate artificial tangent for un-textured face
    static void generateTB(Vec3f v0, Vec3f v1, Vec3f v2, Vec3f ntb[]) {
        // Vec3f a = v1-v0, b = v2-v0;
        Vec3f a = new Vec3f(v1);
        a.sub(v0);
        Vec3f b = new Vec3f(v2);
        b.sub(v0);

        if (a.dot(a) > b.dot(b)) {
            ntb[1] = a;
            ntb[1].normalize();
            ntb[2].cross(ntb[0], ntb[1]);
        } else {
            ntb[2] = b;
            ntb[2].normalize();
            ntb[1].cross(ntb[2], ntb[0]);
        }
    }

    static double clamp(double x, double min, double max) {
        return x < max ? x > min ? x : min : max;
    }

    static void fixTSpace(Vec3f norm[]) {

        float N_norma = norm[0].length();

        Vec3f n1 = new Vec3f(norm[1]);
        Vec3f n2 = new Vec3f(norm[2]);
        getOrt(norm[0], n1);
        getOrt(norm[0], n2);

        float l1 = n1.length();
        float l2 = n2.length();

//        System.err.println("** norm[0] = " + norm[0]);
//        System.err.println("** n1 = " + n1);
//        System.err.println("** n2 = " + n2);

        double cosPhi = (n1.dot(n2)) / (l1 * l2);

        Vec3f e1, e2;

        if (fabs((float) cosPhi) > 0.998) {
            // Vec3f n2fix = (N^n1).Normalize();
            Vec3f n2fix = new Vec3f();
            n2fix.cross(norm[0], n1);
            n2fix.normalize();

            e2 = new Vec3f(n2fix);
            if (n2fix.dot(n2) < 0) {
                e2.mul(-1);
            }
            e1 = new Vec3f(n1);
            e1.mul(1f / l1);
//            System.err.println("(1) e1 = " + e1);
//            System.err.println("(1) e2 = " + e2);
        } else {
            double phi = Math.acos(clamp(cosPhi, -1, 1));
            double alpha = (PI * 0.5 - phi) * 0.5;
            Vec2f e1_local = new Vec2f((float) Math.sin(alpha), (float) Math.cos(alpha));
            Vec2f e2_local = new Vec2f((float) Math.sin(alpha + phi), (float) Math.cos(alpha + phi));

            Vec3f n1T = new Vec3f(n2);
            getOrt(n1, n1T);
            float l_n1T = n1T.length();

            // e1 = float(e1_local.y/l1) * n1 - float(e1_local.x/l_n1T) * n1T;
            e1 = new Vec3f(n1);
            e1.mul(e1_local.y / l1);

            Vec3f n1TT = new Vec3f(n1T);
            n1TT.mul(e1_local.x / l_n1T);
            e1.sub(n1TT);

            // e2 = float(e2_local.y/l1) * n1 + float(e2_local.x/l_n1T) * n1T;
            e2 = new Vec3f(n1);
            e2.mul(e2_local.y / l1);

            // Recycle n1TT for temp computation
            n1TT.set(n1T);
            n1TT.mul(e2_local.x / l_n1T);
            e2.add(n1TT);

            float e1_dot_n1 = e1.dot(n1);
            float e2_dot_n2 = e2.dot(n2);
//            System.err.println("(2) e1 = " + e1);
//            System.err.println("(2) e2 = " + e2);
            assert ((e1_dot_n1 / l1 - e2_dot_n2 / l2) < 0.001);
        }

        norm[1].set(e1);
        norm[2].set(e2);
        norm[0].mul(1.f / N_norma);
    }

}
