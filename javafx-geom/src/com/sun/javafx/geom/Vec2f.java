/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * A 2-dimensional, single-precision, floating-point vector.
 *
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

    public Vec2f() { }

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
     * @param v the specified <code>Vec2f</code> to which to set
     * this <code>Vec2f</code>
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
     *           against this <code>Vec2f</code>
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
     *           against this <code>Vec2f</code>
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
     * @return      a hash code for this <code>Vec2f</code>.
     */
    @Override
    public int hashCode() {
        int bits = 7;
        bits = 31 * bits + java.lang.Float.floatToIntBits(x);
        bits = 31 * bits + java.lang.Float.floatToIntBits(y);
        return bits;
    }

    /**
     * Determines whether or not two 2D points or vectors are equal.
     * Two instances of <code>Vec2f</code> are equal if the values of their
     * <code>x</code> and <code>y</code> member fields, representing
     * their position in the coordinate space, are the same.
     * @param obj an object to be compared with this <code>Vec2f</code>
     * @return <code>true</code> if the object to be compared is
     *         an instance of <code>Vec2f</code> and has
     *         the same values; <code>false</code> otherwise.
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
     * @return a string representation of this <code>Vec2f</code>.
     */
    @Override
    public String toString() {
        return "Vec2f[" + x + ", " + y + "]";
    }
}
