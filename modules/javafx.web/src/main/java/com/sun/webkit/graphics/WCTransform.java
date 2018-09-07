/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.graphics;

import java.util.Arrays;

public final class WCTransform extends Ref {
    private final double[] m;
    private final boolean is3D;

    public WCTransform(double m11, double m12, double m13, double m14,
                       double m21, double m22, double m23, double m24,
                       double m31, double m32, double m33, double m34,
                       double m41, double m42, double m43, double m44)
    {
        this.m = new double[16];

        m[0] = m11;
        m[1] = m21;
        m[2] = m31;
        m[3] = m41;

        m[4] = m12;
        m[5] = m22;
        m[6] = m32;
        m[7] = m42;

        m[8] = m13;
        m[9] = m23;
        m[10] = m33;
        m[11] = m43;

        m[12] = m14;
        m[13] = m24;
        m[14] = m34;
        m[15] = m44;

        this.is3D = true;
    }

    public WCTransform(double m00, double m10, double m01, double m11,
                       double m02, double m12) {
        this.m = new double[6];
        m[0] = m00;
        m[1] = m10;
        m[2] = m01;
        m[3] = m11;
        m[4] = m02;
        m[5] = m12;

        this.is3D = false;
    }

    public double [] getMatrix() {
        return Arrays.copyOf(m, m.length);
    }

    @Override
    public String toString() {
         String val = "WCTransform:";
         if (is3D) {
            val += "(" + m[0] + "," + m[1] + "," + m[2] + "," + m[3] + ")" +
                   "(" + m[4] + "," + m[5] + "," + m[6] + "," + m[7] + ")" +
                   "(" + m[8] + "," + m[9] + "," + m[10] + "," + m[11] + ")" +
                   "(" + m[12] + "," + m[13] + "," + m[14] + "," + m[15] + ")";
         } else {
            val += "(" + m[0] + "," + m[1] + "," + m[2] + ")" +
                   "(" + m[3] + "," + m[4] + "," + m[5] + ")";
         }
         return val;
    }
}
