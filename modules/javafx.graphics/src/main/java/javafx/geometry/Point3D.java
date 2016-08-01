/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.NamedArg;


// PENDING_DOC_REVIEW of this whole class
/**
 * A 3D geometric point that usually represents the x, y, z coordinates.
 * It can also represent a relative magnitude vector's x, y, z magnitudes.
 *
 * @since JavaFX 2.0
 */
public class Point3D {

    /**
     * Point or vector with all three coordinates set to 0.
     */
    public static final Point3D ZERO = new Point3D(0.0, 0.0, 0.0);

    /**
     * The x coordinate.
     *
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
     * The z coordinate.
     *
     * @defaultValue 0.0
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
    public Point3D(@NamedArg("x") double x, @NamedArg("y") double y, @NamedArg("z") double z) {
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
     */
    public double distance(double x1, double y1, double z1) {
        double a = getX() - x1;
        double b = getY() - y1;
        double c = getZ() - z1;
        return Math.sqrt(a * a + b * b + c * c);
    }

    /**
     * Computes the distance between this point and the specified {@code point}.
     *
     * @param point the other point
     * @return the distance between this point and the specified {@code point}.
     * @throws NullPointerException if the specified {@code point} is null
     */
    public double distance(Point3D  point) {
        return distance(point.getX(), point.getY(), point.getZ());
    }

    /**
     * Returns a point with the specified coordinates added to the coordinates
     * of this point.
     * @param x the X coordinate addition
     * @param y the Y coordinate addition
     * @param z the Z coordinate addition
     * @return the point with added coordinates
     * @since JavaFX 8.0
     */
    public Point3D add(double x, double y, double z) {
        return new Point3D(
                getX() + x,
                getY() + y,
                getZ() + z);
    }

    /**
     * Returns a point with the coordinates of the specified point added to the
     * coordinates of this point.
     * @param point the point whose coordinates are to be added
     * @return the point with added coordinates
     * @throws NullPointerException if the specified {@code point} is null
     * @since JavaFX 8.0
     */
    public Point3D add(Point3D point) {
        return add(point.getX(), point.getY(), point.getZ());
    }

    /**
     * Returns a point with the specified coordinates subtracted from
     * the coordinates of this point.
     * @param x the X coordinate subtraction
     * @param y the Y coordinate subtraction
     * @param z the Z coordinate subtraction
     * @return the point with subtracted coordinates
     * @since JavaFX 8.0
     */
    public Point3D subtract(double x, double y, double z) {
        return new Point3D(
                getX() - x,
                getY() - y,
                getZ() - z);
    }

    /**
     * Returns a point with the coordinates of the specified point subtracted
     * from the coordinates of this point.
     * @param point the point whose coordinates are to be subtracted
     * @return the point with subtracted coordinates
     * @throws NullPointerException if the specified {@code point} is null
     * @since JavaFX 8.0
     */
    public Point3D subtract(Point3D point) {
        return subtract(point.getX(), point.getY(), point.getZ());
    }

    /**
     * Returns a point with the coordinates of this point multiplied
     * by the specified factor
     * @param factor the factor multiplying the coordinates
     * @return the point with multiplied coordinates
     * @since JavaFX 8.0
     */
    public Point3D multiply(double factor) {
        return new Point3D(getX() * factor, getY() * factor, getZ() * factor);
    }

    /**
     * Normalizes the relative magnitude vector represented by this instance.
     * Returns a vector with the same direction and magnitude equal to 1.
     * If this is a zero vector, a zero vector is returned.
     * @return the normalized vector represented by a {@code Point3D} instance
     * @since JavaFX 8.0
     */
    public Point3D normalize() {
        final double mag = magnitude();

        if (mag == 0.0) {
            return new Point3D(0.0, 0.0, 0.0);
        }

        return new Point3D(
                getX() / mag,
                getY() / mag,
                getZ() / mag);
    }

    /**
     * Returns a point which lies in the middle between this point and the
     * specified coordinates.
     * @param x the X coordinate of the second endpoint
     * @param y the Y coordinate of the second endpoint
     * @param z the Z coordinate of the second endpoint
     * @return the point in the middle
     * @since JavaFX 8.0
     */
    public Point3D midpoint(double x, double y, double z) {
        return new Point3D(
                x + (getX() - x) / 2.0,
                y + (getY() - y) / 2.0,
                z + (getZ() - z) / 2.0);
    }

    /**
     * Returns a point which lies in the middle between this point and the
     * specified point.
     * @param point the other endpoint
     * @return the point in the middle
     * @throws NullPointerException if the specified {@code point} is null
     * @since JavaFX 8.0
     */
    public Point3D midpoint(Point3D point) {
        return midpoint(point.getX(), point.getY(), point.getZ());
    }

    /**
     * Computes the angle (in degrees) between the vector represented
     * by this point and the specified vector.
     * @param x the X magnitude of the other vector
     * @param y the Y magnitude of the other vector
     * @param z the Z magnitude of the other vector
     * @return the angle between the two vectors measured in degrees
     * @since JavaFX 8.0
     */
    public double angle(double x, double y, double z) {
        final double ax = getX();
        final double ay = getY();
        final double az = getZ();

        final double delta = (ax * x + ay * y + az * z) / Math.sqrt(
                (ax * ax + ay * ay + az * az) * (x * x + y * y + z * z));

        if (delta > 1.0) {
            return 0.0;
        }
        if (delta < -1.0) {
            return 180.0;
        }

        return Math.toDegrees(Math.acos(delta));
    }

    /**
     * Computes the angle (in degrees) between the vector represented
     * by this point and the vector represented by the specified point.
     * @param point the other vector
     * @return the angle between the two vectors measured in degrees,
     *         {@code NaN} if any of the two vectors is a zero vector
     * @throws NullPointerException if the specified {@code point} is null
     * @since JavaFX 8.0
     */
    public double angle(Point3D point) {
        return angle(point.getX(), point.getY(), point.getZ());
    }

    /**
     * Computes the angle (in degrees) between the three points with this point
     * as a vertex.
     * @param p1 one point
     * @param p2 other point
     * @return angle between the vectors (this, p1) and (this, p2) measured
     *         in degrees, {@code NaN} if the three points are not different
     *         from one another
     * @throws NullPointerException if the {@code p1} or {@code p2} is null
     * @since JavaFX 8.0
     */
    public double angle(Point3D p1, Point3D p2) {
        final double x = getX();
        final double y = getY();
        final double z = getZ();

        final double ax = p1.getX() - x;
        final double ay = p1.getY() - y;
        final double az = p1.getZ() - z;
        final double bx = p2.getX() - x;
        final double by = p2.getY() - y;
        final double bz = p2.getZ() - z;

        final double delta = (ax * bx + ay * by + az * bz) / Math.sqrt(
                (ax * ax + ay * ay + az * az) * (bx * bx + by * by + bz * bz));

        if (delta > 1.0) {
            return 0.0;
        }
        if (delta < -1.0) {
            return 180.0;
        }

        return Math.toDegrees(Math.acos(delta));
    }

    /**
     * Computes magnitude (length) of the relative magnitude vector represented
     * by this instance.
     * @return magnitude of the vector
     * @since JavaFX 8.0
     */
    public double magnitude() {
        final double x = getX();
        final double y = getY();
        final double z = getZ();

        return Math.sqrt(x * x + y * y + z * z);
    }

    /**
     * Computes dot (scalar) product of the vector represented by this instance
     * and the specified vector.
     * @param x the X magnitude of the other vector
     * @param y the Y magnitude of the other vector
     * @param z the Z magnitude of the other vector
     * @return the dot product of the two vectors
     * @since JavaFX 8.0
     */
    public double dotProduct(double x, double y, double z) {
        return getX() * x + getY() * y + getZ() * z;
    }

    /**
     * Computes dot (scalar) product of the vector represented by this instance
     * and the specified vector.
     * @param vector the other vector
     * @return the dot product of the two vectors
     * @throws NullPointerException if the specified {@code vector} is null
     * @since JavaFX 8.0
     */
    public double dotProduct(Point3D vector) {
        return dotProduct(vector.getX(), vector.getY(), vector.getZ());
    }

    /**
     * Computes cross product of the vector represented by this instance
     * and the specified vector.
     * @param x the X magnitude of the other vector
     * @param y the Y magnitude of the other vector
     * @param z the Z magnitude of the other vector
     * @return the cross product of the two vectors
     * @since JavaFX 8.0
     */
    public Point3D crossProduct(double x, double y, double z) {
        final double ax = getX();
        final double ay = getY();
        final double az = getZ();

        return new Point3D(
                ay * z - az * y,
                az * x - ax * z,
                ax * y - ay * x);
    }

    /**
     * Computes cross product of the vector represented by this instance
     * and the specified vector.
     * @param vector the other vector
     * @return the cross product of the two vectors
     * @throws NullPointerException if the specified {@code vector} is null
     * @since JavaFX 8.0
     */
    public Point3D crossProduct(Point3D vector) {
        return crossProduct(vector.getX(), vector.getY(), vector.getZ());
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
     */
    @Override public String toString() {
        return "Point3D [x = " + getX() + ", y = " + getY() + ", z = " + getZ() + "]";
    }
}
