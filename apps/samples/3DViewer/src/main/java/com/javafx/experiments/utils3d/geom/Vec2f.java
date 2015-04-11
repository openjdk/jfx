/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.javafx.experiments.utils3d.geom;

/**
 * A 2-dimensional, single-precision, floating-point vector.
 */
public class Vec2f {
    /**
     * The x coordinate.
     */
    public float x;

    /**
     * The y coordinate.
     */
    public float y;

    public Vec2f() {
    }

    public Vec2f(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vec2f(Vec2f v) {
        this.x = v.x;
        this.y = v.y;
    }


    /**
     * Sets the location of this <code>Vec2f</code> to the same
     * coordinates as the specified <code>Vec2f</code> object.
     *
     * @param v the specified <code>Vec2f</code> to which to set
     *          this <code>Vec2f</code>
     */
    public void set(Vec2f v) {
        this.x = v.x;
        this.y = v.y;
    }

    /**
     * Sets the location of this <code>Vec2f</code> to the
     * specified <code>float</code> coordinates.
     *
     * @param x the new X coordinate of this {@code Vec2f}
     * @param y the new Y coordinate of this {@code Vec2f}
     */
    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the square of the distance between two points.
     *
     * @param x1 the X coordinate of the first specified point
     * @param y1 the Y coordinate of the first specified point
     * @param x2 the X coordinate of the second specified point
     * @param y2 the Y coordinate of the second specified point
     * @return the square of the distance between the two
     * sets of specified coordinates.
     */
    public static float distanceSq(float x1, float y1, float x2, float y2) {
        x1 -= x2;
        y1 -= y2;
        return (x1 * x1 + y1 * y1);
    }

    /**
     * Returns the distance between two points.
     *
     * @param x1 the X coordinate of the first specified point
     * @param y1 the Y coordinate of the first specified point
     * @param x2 the X coordinate of the second specified point
     * @param y2 the Y coordinate of the second specified point
     * @return the distance between the two sets of specified
     * coordinates.
     */
    public static float distance(float x1, float y1, float x2, float y2) {
        x1 -= x2;
        y1 -= y2;
        return (float) Math.sqrt(x1 * x1 + y1 * y1);
    }

    /**
     * Returns the square of the distance from this
     * <code>Vec2f</code> to a specified point.
     *
     * @param vx the X coordinate of the specified point to be measured
     *           against this <code>Vec2f</code>
     * @param vy the Y coordinate of the specified point to be measured
     *           against this <code>Vec2f</code>
     * @return the square of the distance between this
     * <code>Vec2f</code> and the specified point.
     */
    public float distanceSq(float vx, float vy) {
        vx -= x;
        vy -= y;
        return (vx * vx + vy * vy);
    }

    /**
     * Returns the square of the distance from this
     * <code>Vec2f</code> to a specified <code>Vec2f</code>.
     *
     * @param v the specified point to be measured
     *          against this <code>Vec2f</code>
     * @return the square of the distance between this
     * <code>Vec2f</code> to a specified <code>Vec2f</code>.
     */
    public float distanceSq(Vec2f v) {
        float vx = v.x - this.x;
        float vy = v.y - this.y;
        return (vx * vx + vy * vy);
    }

    /**
     * Returns the distance from this <code>Vec2f</code> to
     * a specified point.
     *
     * @param vx the X coordinate of the specified point to be measured
     *           against this <code>Vec2f</code>
     * @param vy the Y coordinate of the specified point to be measured
     *           against this <code>Vec2f</code>
     * @return the distance between this <code>Vec2f</code>
     * and a specified point.
     */
    public float distance(float vx, float vy) {
        vx -= x;
        vy -= y;
        return (float) Math.sqrt(vx * vx + vy * vy);
    }

    /**
     * Returns the distance from this <code>Vec2f</code> to a
     * specified <code>Vec2f</code>.
     *
     * @param v the specified point to be measured
     *          against this <code>Vec2f</code>
     * @return the distance between this <code>Vec2f</code> and
     * the specified <code>Vec2f</code>.
     */
    public float distance(Vec2f v) {
        float vx = v.x - this.x;
        float vy = v.y - this.y;
        return (float) Math.sqrt(vx * vx + vy * vy);
    }

    /**
     * Returns the hashcode for this <code>Vec2f</code>.
     *
     * @return a hash code for this <code>Vec2f</code>.
     */
    @Override
    public int hashCode() {
        int bits = 7;
        bits = 31 * bits + Float.floatToIntBits(x);
        bits = 31 * bits + Float.floatToIntBits(y);
        return bits;
    }

    /**
     * Determines whether or not two 2D points or vectors are equal.
     * Two instances of <code>Vec2f</code> are equal if the values of their
     * <code>x</code> and <code>y</code> member fields, representing
     * their position in the coordinate space, are the same.
     *
     * @param obj an object to be compared with this <code>Vec2f</code>
     * @return <code>true</code> if the object to be compared is
     * an instance of <code>Vec2f</code> and has
     * the same values; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Vec2f) {
            Vec2f v = (Vec2f) obj;
            return (x == v.x) && (y == v.y);
        }
        return false;
    }

    /**
     * Returns a <code>String</code> that represents the value
     * of this <code>Vec2f</code>.
     *
     * @return a string representation of this <code>Vec2f</code>.
     */
    @Override
    public String toString() {
        return "Vec2f[" + x + ", " + y + "]";
    }
}
