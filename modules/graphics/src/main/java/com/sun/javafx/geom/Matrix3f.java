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

package com.sun.javafx.geom;

/**
 * A single precision floating point 3 by 3 matrix.
 * Primarily to support 3D rotations.
 */
public class Matrix3f {

    /**
     * The first matrix element in the first row.
     */
    public float m00;
    /**
     * The second matrix element in the first row.
     */
    public float m01;
    /**
     * The third matrix element in the first row.
     */
    public float m02;
    /**
     * The first matrix element in the second row.
     */
    public float m10;
    /**
     * The second matrix element in the second row.
     */
    public float m11;
    /**
     * The third matrix element in the second row.
     */
    public float m12;
    /**
     * The first matrix element in the third row.
     */
    public float m20;
    /**
     * The second matrix element in the third row.
     */
    public float m21;
    /**
     * The third matrix element in the third row.
     */
    public float m22;

    /**
     * Constructs and initializes a Matrix3f from the specified nine values.
     * @param m00 the [0][0] element
     * @param m01 the [0][1] element
     * @param m02 the [0][2] element
     * @param m10 the [1][0] element
     * @param m11 the [1][1] element
     * @param m12 the [1][2] element
     * @param m20 the [2][0] element
     * @param m21 the [2][1] element
     * @param m22 the [2][2] element
     */
    public Matrix3f(float m00, float m01, float m02,
                    float m10, float m11, float m12,
                    float m20, float m21, float m22) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;

        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;

        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;

    }

    /**
     * Constructs and initializes a Matrix3f from the specified
     * nine-element array.   this.m00 =v[0], this.m01=v[1], etc.
     * @param v the array of length 9 containing in order
     */
    public Matrix3f(float[] v) {
        this.m00 = v[0];
        this.m01 = v[1];
        this.m02 = v[2];

        this.m10 = v[3];
        this.m11 = v[4];
        this.m12 = v[5];

        this.m20 = v[6];
        this.m21 = v[7];
        this.m22 = v[8];

    }

    /**
     * Constructs and initializes a Matrix3f from the specified
     * nine-element array.   this.m00 =v[0], this.m01=v[1], etc.
     * @param v the array of length 9 containing in order
     */
    public Matrix3f(Vec3f[] v) {
        this.m00 = v[0].x;
        this.m01 = v[0].y;
        this.m02 = v[0].z;

        this.m10 = v[1].x;
        this.m11 = v[1].x;
        this.m12 = v[1].x;

        this.m20 = v[2].x;
        this.m21 = v[2].x;
        this.m22 = v[2].x;

    }

    /**
     *  Constructs a new matrix with the same values as the
     *  Matrix3f parameter.
     *  @param m1  the source matrix
     */
    public Matrix3f(Matrix3f m1) {
        this.m00 = m1.m00;
        this.m01 = m1.m01;
        this.m02 = m1.m02;

        this.m10 = m1.m10;
        this.m11 = m1.m11;
        this.m12 = m1.m12;

        this.m20 = m1.m20;
        this.m21 = m1.m21;
        this.m22 = m1.m22;

    }

    /**
     * Constructs and initializes a Matrix3f to all zeros.
     */
    public Matrix3f() {
        this.m00 = (float) 1.0;
        this.m01 = (float) 0.0;
        this.m02 = (float) 0.0;

        this.m10 = (float) 0.0;
        this.m11 = (float) 1.0;
        this.m12 = (float) 0.0;

        this.m20 = (float) 0.0;
        this.m21 = (float) 0.0;
        this.m22 = (float) 1.0;

    }

    /**
     * Returns a string that contains the values of this Matrix3f.
     * @return the String representation
     */
    @Override
    public String toString() {
        return this.m00 + ", " + this.m01 + ", " + this.m02 + "\n"
                + this.m10 + ", " + this.m11 + ", " + this.m12 + "\n"
                + this.m20 + ", " + this.m21 + ", " + this.m22 + "\n";
    }

    /**
     * Sets this Matrix3f to identity.
     */
    public final void setIdentity() {
        this.m00 = (float) 1.0;
        this.m01 = (float) 0.0;
        this.m02 = (float) 0.0;

        this.m10 = (float) 0.0;
        this.m11 = (float) 1.0;
        this.m12 = (float) 0.0;

        this.m20 = (float) 0.0;
        this.m21 = (float) 0.0;
        this.m22 = (float) 1.0;
    }

    /**
     * Sets the specified row of this matrix3f to the three values provided.
     * @param row the row number to be modified (zero indexed)
     * @param v the replacement row
     */
    public final void setRow(int row, float[] v) {
        switch (row) {
            case 0:
                this.m00 = v[0];
                this.m01 = v[1];
                this.m02 = v[2];
                break;

            case 1:
                this.m10 = v[0];
                this.m11 = v[1];
                this.m12 = v[2];
                break;

            case 2:
                this.m20 = v[0];
                this.m21 = v[1];
                this.m22 = v[2];
                break;

            default:
                throw new ArrayIndexOutOfBoundsException("Matrix3f");
        }
    }

    /**
     * Sets the specified row of this matrix3f to the Vector provided.
     * @param row the row number to be modified (zero indexed)
     * @param v the replacement row
     */
    public final void setRow(int row, Vec3f v) {
        switch (row) {
            case 0:
                this.m00 = v.x;
                this.m01 = v.y;
                this.m02 = v.z;
                break;

            case 1:
                this.m10 = v.x;
                this.m11 = v.y;
                this.m12 = v.z;
                break;

            case 2:
                this.m20 = v.x;
                this.m21 = v.y;
                this.m22 = v.z;
                break;

            default:
                throw new ArrayIndexOutOfBoundsException("Matrix3f");
        }
    }

    /**
     * Copies the matrix values in the specified row into the vector parameter.
     * @param row  the matrix row
     * @param v    the vector into which the matrix row values will be copied
     */
    public final void getRow(int row, Vec3f v) {
        if (row == 0) {
            v.x = m00;
            v.y = m01;
            v.z = m02;
        } else if (row == 1) {
            v.x = m10;
            v.y = m11;
            v.z = m12;
        } else if (row == 2) {
            v.x = m20;
            v.y = m21;
            v.z = m22;
        } else {
            throw new ArrayIndexOutOfBoundsException("Matrix3f");
        }
    }

    /**
     * Copies the matrix values in the specified row into the array parameter.
     * @param row  the matrix row
     * @param v    the array into which the matrix row values will be copied
     */
    public final void getRow(int row, float[] v) {
        if (row == 0) {
            v[0] = m00;
            v[1] = m01;
            v[2] = m02;
        } else if (row == 1) {
            v[0] = m10;
            v[1] = m11;
            v[2] = m12;
        } else if (row == 2) {
            v[0] = m20;
            v[1] = m21;
            v[2] = m22;
        } else {
            throw new ArrayIndexOutOfBoundsException("Matrix3f");
        }
    }
}

