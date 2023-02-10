/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.pisces;

public final class Transform6 {

    public int m00, m01, m10, m11;
    public int m02, m12;

    public Transform6() {
        this(1 << 16, 0, 0, 1 << 16, 0, 0);
    }

    public Transform6(int m00, int m01,
                      int m10, int m11,
                      int m02, int m12)
    {
        initialize();

        this.m00 = m00;
        this.m01 = m01;
        this.m10 = m10;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
    }

    public Transform6(Transform6 t) {
        this(t.m00, t.m01, t.m10, t.m11, t.m02, t.m12);
    }

    public void postMultiply(Transform6 t) {
        long _m00 = ((long)m00*t.m00 + (long)m01*t.m10) >> 16;
        long _m01 = ((long)m00*t.m01 + (long)m01*t.m11) >> 16;
        long _m10 = ((long)m10*t.m00 + (long)m11*t.m10) >> 16;
        long _m11 = ((long)m10*t.m01 + (long)m11*t.m11) >> 16;
        long _m02 = (((long)m02 << 16) +
                     (long)m00*t.m02 + (long)m01*t.m12) >> 16;
        long _m12 = (((long)m12 << 16) +
                     (long)m10*t.m02 + (long)m11*t.m12) >> 16;

        this.m00 = (int)_m00;
        this.m01 = (int)_m01;
        this.m02 = (int)_m02;
        this.m10 = (int)_m10;
        this.m11 = (int)_m11;
        this.m12 = (int)_m12;
    }

    public Transform6 inverse() {
        float fm00 = m00/65536.0f;
        float fm01 = m01/65536.0f;
        float fm02 = m02/65536.0f;
        float fm10 = m10/65536.0f;
        float fm11 = m11/65536.0f;
        float fm12 = m12/65536.0f;
        float fdet = fm00*fm11 - fm01*fm10;

        float fa00 =  fm11/fdet;
        float fa01 = -fm01/fdet;
        float fa10 = -fm10/fdet;
        float fa11 =  fm00/fdet;
        float fa02 = (fm01*fm12 - fm02*fm11)/fdet;
        float fa12 = (fm02*fm10 - fm00*fm12)/fdet;

        int a00 = (int)(fa00*65536.0);
        int a01 = (int)(fa01*65536.0f);
        int a10 = (int)(fa10*65536.0f);
        int a11 = (int)(fa11*65536.0f);
        int a02 = (int)(fa02*65536.0f);
        int a12 = (int)(fa12*65536.0f);

        return new Transform6(a00, a01, a10, a11, a02, a12);
    }

    public boolean isIdentity() {
        return (m00 == 1 << 16 && m01 == 0 &&
                m10 == 0       && m11 == 1 << 16 &&
                m02 == 0       && m12 == 0);
    }

    /**
     * Sets this transform to a copy of the transform in the specified
     * <code>Transform6</code> object.
     *
     * @param Tx
     *            the <code>Transform6</code> object from which to copy the
     *            transform
     */
    public Transform6 setTransform(Transform6 Tx) {
        this.m00 = Tx.m00;
        this.m10 = Tx.m10;
        this.m01 = Tx.m01;
        this.m11 = Tx.m11;
        this.m02 = Tx.m02;
        this.m12 = Tx.m12;
        return this;
    }



    @Override
    public String toString() {
        return "Transform6[" +
            "m00=" + (m00/65536.0) + ", " +
            "m01=" + (m01/65536.0) + ", " +
            "m02=" + (m02/65536.0) + ", " +
            "m10=" + (m10/65536.0) + ", " +
            "m11=" + (m11/65536.0) + ", " +
            "m12=" + (m12/65536.0) + "]";
    }

    private native void initialize();
}
