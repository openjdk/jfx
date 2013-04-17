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
 * TODO: This is a direct port of the 3D Mesh prototype.
 *       Need to change to use proper quaternion class in the future
 * JIRA ID: RT-29542 - FX 8 3D: Mesh computation code needs major clean up or redo
 */
class MeshQuat {


    Vec3f v;
    float w;

    private static float m[][] = new float[3][3];
    private static float tmp[] = new float[3];

    MeshQuat() {
        v = new Vec3f();
        w = 0f;
    }

    MeshQuat(Vec3f v, float w) {
        this.v = new Vec3f(v);
        this.w = w;
    }

    void buildQuat(Vec3f tm[]) {

        for (int i = 0; i < 3; i++) {
            m[i][0] = tm[i].x;
            m[i][1] = tm[i].y;
            m[i][2] = tm[i].z;
        }

        float trace = m[0][0] + m[1][1] + m[2][2];

        if (trace > 0) {

            float s = (float) Math.sqrt(trace + 1.0f);
            float t = 0.5f / s;
            w = 0.5f * s;
            v.x = (m[1][2] - m[2][1]) * t;
            v.y = (m[2][0] - m[0][2]) * t;
            v.z = (m[0][1] - m[1][0]) * t;

        } else {

            int next[] = {1, 2, 0};
            int i = 0;

            if (m[1][1] > m[0][0]) {
                i = 1;
            }
            if (m[2][2] > m[i][i]) {
                i = 2;
            }

            int j = next[i], k = next[j];

            double s = Math.sqrt(m[i][i] - m[j][j] - m[k][k] + 1.0f);

            if (m[j][k] < m[k][j]) {
                s = -s;
            }

            float t = (float) (0.5 / s);

            tmp[i] = (float) (0.5f * s);
            w = (m[j][k] - m[k][j]) * t;
            tmp[j] = (m[i][j] + m[j][i]) * t;
            tmp[k] = (m[i][k] + m[k][i]) * t;
            v.x = tmp[0];
            v.y = tmp[1];
            v.z = tmp[2];
        }
    }

    void scale(float s) {
        v.mul(s);
        w *= s;
    }

}
