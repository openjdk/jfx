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
 * A 3D geometric point that represents the x, y, z coordinates.
 *
 * @profile common conditional scene3d
 * @since JavaFX 1.3
 */

public class Point3D {
    /**
     * The x coordinate.
     *
     * @profile common conditional scene3d
     * @defaultvalue 0.0
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
     * @profile common conditional scene3d
     * @defaultvalue 0.0
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
     * The z coordinate.
     *
     * @profile common conditional scene3d
     * @defaultvalue 0.0
     */
    private double z;

    /**
     * The z coordinate.
     * @return the z coordinate
     */
    public final double getZ() {
        return z;
    }

    /**
     * Cache the hash code to make computing hashes faster.
     */
    private int hash = 0;

    /**
     * Creates a new instance of {@code Point3D}.
     * @param x The X coordinate of the {@code Point3D}
     * @param y The Y coordinate of the {@code Point3D}
     * @param z The Z coordinate of the {@code Point3D}
     */
    public Point3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Computes the distance between this point and point {@code (x1, y1, z1)}.
     *
     * @param x1 the x coordinate of other point
     * @param y1 the y coordinate of other point
     * @param z1 the z coordinate of other point
     * @return the distance between this point and point {@code (x1, y1, z1)}.
     * @profile common conditional scene3d
     */
    public double distance(double x1, double y1, double z1) {
        double a = getX() - x1;
        double b = getY() - y1;
        double c = getZ() - z1;
        return Math.sqrt(a * a + b * b + c * c);
    }

    /**
     * Computes the distance between this point and point {@code p}.
     *
     * @param p the other point
     * @return the distance between this point and point {@code p}.
     * @profile common conditional scene3d
     */
    public double distance(Point3D  p) {
        return distance(p.getX(), p.getY(), p.getZ());
    }

    /**
     * Returns a hash code value for the point.
     * @return a hash code value for the point.
     */
    @Override public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Point3D) {
            Point3D other = (Point3D) obj;
            return getX() == other.getX() && getY() == other.getY() && getZ() == other.getZ();
        } else return false;
    }

    /**
     * Returns a hash code for this {@code Point3D} object.
     * @return a hash code for this {@code Point3D} object.
     */ 
    @Override public int hashCode() {
        if (hash == 0) {
            long bits = 7L;
            bits = 31L * bits + Double.doubleToLongBits(getX());
            bits = 31L * bits + Double.doubleToLongBits(getY());
            bits = 31L * bits + Double.doubleToLongBits(getZ());
            hash = (int) (bits ^ (bits >> 32));
        }
        return hash;
    }

    /**
     * Returns a string representation of this {@code Point3D}.
     * This method is intended to be used only for informational purposes.
     * The content and format of the returned string might vary between
     * implementations.
     * The returned string might be empty but cannot be {@code null}.
     *
     * @profile common conditional scene3d
     */
    @Override public String toString() {
        return "Point3D [x = " + getX() + ", y = " + getY() + ", z = " + getZ() + "]";
    }
}
