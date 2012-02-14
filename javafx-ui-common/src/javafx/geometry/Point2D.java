/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.geometry;

/**
 * A 2D geometric point that represents the x, y coordinates.
 *
 * @profile common
 */
public class Point2D {
    /**
     * The x coordinate.
     *
     * @profile common
     * @defaultValue 0.0
     */
    private double x;

    /**
     * The x coordinate.
     * @return the x coordinate
     */
    public final double getX() {
        return x;
    }
    
    /**
     * The y coordinate.
     *
     * @profile common
     * @defaultValue 0.0
     */
    private double y;

    /**
     * The y coordinate.
     * @return the y coordinate
     */
    public final double getY() {
        return y;
    }

    /**
     * Cache the hash code to make computing hashes faster.
     */
    private int hash = 0;

    /**
     * Creates a new instance of {@code Point2D}.
     * @param x the x coordinate of the point
     * @param y the y coordinate of the point
     */
    public Point2D(double x, double y) {
        this.x  = x;
        this.y = y;
    }

    /**
     * Computes the distance between this point and point {@code (x1, y1)}.
     *
     * @profile common
     * @param x1 the x coordinate of other point
     * @param y1 the y coordinate of other point
     * @return the distance between this point and point {@code (x1, y1)}.
     */
    public double distance(double x1, double y1) {
        double a = getX() - x1;
        double b = getY() - y1;
        return Math.sqrt(a * a + b * b);
    }

    /**
     * Computes the distance between this point and point {@code p}.
     *
     * @profile common
     * @param p the other point
     * @return the distance between this point and point {@code p}.
     */
    public double distance(Point2D p) {
        return distance(p.getX(), p.getY());
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * 
     * @param obj the reference object with which to compare
     * @return true if this point is the same as the obj argument; false otherwise
      */
    @Override public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Point2D) {
            Point2D other = (Point2D) obj;
            return getX() == other.getX() && getY() == other.getY();
        } else return false;
    }

    /**
     * Returns a hash code value for the point.
     * @return a hash code value for the point.
     */
    @Override public int hashCode() {
        if (hash == 0) {
            long bits = 7L;
            bits = 31L * bits + Double.doubleToLongBits(getX());
            bits = 31L * bits + Double.doubleToLongBits(getY());
            hash = (int) (bits ^ (bits >> 32));
        }
        return hash;
    }

    /**
     * Returns a string representation of this {@code Point2D}.
     * This method is intended to be used only for informational purposes.
     * The content and format of the returned string might vary between
     * implementations.
     * The returned string might be empty but cannot be {@code null}.
     *
     * @profile common
     */
    @Override public String toString() {
        return "Point2D [x = " + getX() + ", y = " + getY() + "]";
    }
}