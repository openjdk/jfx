/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * A 4 element unit quaternion represented by single precision floating point
 * x,y,z,w coordinates. The quaternion is always normalized.
 */
public class Quat4f {

    final static double EPS2 = 1.0e-30;

    /**
     * The x coordinate.
     */
    public float x;
    /**
     * The y coordinate.
     */
    public float y;
    /**
     * The z coordinate.
     */
    public float z;
    /**
     * The w coordinate.
     */
    public float w;

    /**
   * Constructs and initializes a Quat4f to (0,0,0,0).
   */
  public Quat4f()
  {
    this.x = 0.0f;
    this.y = 0.0f;
    this.z = 0.0f;
    this.w = 0.0f;
  }

    /**
     * Constructs and initializes a Quat4f from the specified xyzw coordinates.
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param w the w scalar component
     */
    public Quat4f(float x, float y, float z, float w) {
        float mag;
        mag = (float) (1.0 / Math.sqrt(x * x + y * y + z * z + w * w));
        this.x = x * mag;
        this.y = y * mag;
        this.z = z * mag;
        this.w = w * mag;

    }

    /**
     * Constructs and initializes a Quat4f from the array of length 4.
     * @param q the array of length 4 containing xyzw in order
     */
    public Quat4f(float[] q) {
        float mag;
        mag = (float) (1.0 / Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]));
        x = q[0] * mag;
        y = q[1] * mag;
        z = q[2] * mag;
        w = q[3] * mag;

    }

    /**
     * Constructs and initializes a Quat4f from the specified Quat4f.
     * @param q1 the Quat4f containing the initialization x y z w data
     */
    public Quat4f(Quat4f q1) {
        this.x = q1.x;
        this.y = q1.y;
        this.z = q1.z;
        this.w = q1.w;
    }

    /**
     * Normalizes the value of this quaternion in place.
     */
    public final void normalize() {
        float norm;

        norm = (this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w);

        if (norm > 0.0f) {
            norm = 1.0f / (float) Math.sqrt(norm);
            this.x *= norm;
            this.y *= norm;
            this.z *= norm;
            this.w *= norm;
        } else {
            this.x = (float) 0.0;
            this.y = (float) 0.0;
            this.z = (float) 0.0;
            this.w = (float) 0.0;
        }
    }

    /**
     * Sets the value of this quaternion to the rotational component of
     * the passed matrix.
     * @param m1 the Matrix3f
     */
    public final void set(Matrix3f m1) {
        float ww = 0.25f * (m1.m00 + m1.m11 + m1.m22 + 1.0f);

        if (ww >= 0) {
            if (ww >= EPS2) {
                this.w = (float) Math.sqrt(ww);
                ww = 0.25f / this.w;
                this.x = (m1.m21 - m1.m12) * ww;
                this.y = (m1.m02 - m1.m20) * ww;
                this.z = (m1.m10 - m1.m01) * ww;
                return;
            }
        } else {
            this.w = 0;
            this.x = 0;
            this.y = 0;
            this.z = 1;
            return;
        }

        this.w = 0;
        ww = -0.5f * (m1.m11 + m1.m22);
        if (ww >= 0) {
            if (ww >= EPS2) {
                this.x = (float) Math.sqrt(ww);
                ww = 0.5f / this.x;
                this.y = m1.m10 * ww;
                this.z = m1.m20 * ww;
                return;
            }
        } else {
            this.x = 0;
            this.y = 0;
            this.z = 1;
            return;
        }

        this.x = 0;
        ww = 0.5f * (1.0f - m1.m22);
        if (ww >= EPS2) {
            this.y = (float) Math.sqrt(ww);
            this.z = m1.m21 / (2.0f * this.y);
            return;
        }

        this.y = 0;
        this.z = 1;
    }

    /**
     * Sets the value of this quaternion to the rotational component of
     * the passed float matrix.
     * @param m1 the float[3][3] matrix
     */
    public final void set(float m1[][]) {
        float ww = 0.25f * (m1[0][0] + m1[1][1] + m1[2][2] + 1.0f);

        if (ww >= 0) {
            if (ww >= EPS2) {
                this.w = (float) Math.sqrt(ww);
                ww = 0.25f / this.w;
                this.x = (m1[2][1] - m1[1][2]) * ww;
                this.y = (m1[0][2] - m1[2][0]) * ww;
                this.z = (m1[1][0] - m1[0][1]) * ww;
                return;
            }
        } else {
            this.w = 0;
            this.x = 0;
            this.y = 0;
            this.z = 1;
            return;
        }

        this.w = 0;
        ww = -0.5f * (m1[1][1] + m1[2][2]);
        if (ww >= 0) {
            if (ww >= EPS2) {
                this.x = (float) Math.sqrt(ww);
                ww = 0.5f / this.x;
                this.y = m1[1][0] * ww;
                this.z = m1[2][0] * ww;
                return;
            }
        } else {
            this.x = 0;
            this.y = 0;
            this.z = 1;
            return;
        }

        this.x = 0;
        ww = 0.5f * (1.0f - m1[2][2]);
        if (ww >= EPS2) {
            this.y = (float) Math.sqrt(ww);
            this.z = m1[2][1] / (2.0f * this.y);
            return;
        }

        this.y = 0;
        this.z = 1;
    }

    /**
   * Sets the value of this Quat4f to the scalar multiplication
   * of the scale factor with this.
   * @param s the scalar value
   */
  public final void scale(float s)
  {
    this.x *= s;
    this.y *= s;
    this.z *= s;
    this.w *= s;
  }

    /**
     * Returns a <code>String</code> that represents the value of this
     * <code>Quat4f</code>.
     *
     * @return a string representation of this <code>Quat4f</code>.
     */
    @Override
    public String toString() {
        return "Quat4f[" + x + ", " + y + ", " + z + ", " + w + "]";
    }
}

