/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

import javafx.animation.Interpolatable;
import javafx.beans.NamedArg;


// PENDING_DOC_REVIEW of this whole class
/**
 * A 2D geometric point that usually represents the x, y coordinates.
 * It can also represent a relative magnitude vector's x, y magnitudes.
 * @since JavaFX 2.0
 */
public class Point2D implements Interpolatable<Point2D> {

    /**
     * Point or vector with both coordinates set to 0.
     */
    public static final Point2D ZERO = new Point2D(0.0, 0.0);

    /**
     * The x coordinate.
     *
     * @defaultValue 0.0
     */
    private final double x;

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
    private final double y;

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
    public Point2D(@NamedArg("x") double x, @NamedArg("y") double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Computes the distance between this point and point {@code (x1, y1)}.
     *
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
     * Computes the distance between this point and the specified {@code point}.
     *
     * @param point the other point
     * @return the distance between this point and the specified {@code point}.
     * @throws NullPointerException if the specified {@code point} is null
     */
    public double distance(Point2D point) {
        return distance(point.getX(), point.getY());
    }

    /**
     * Returns a point with the specified coordinates added to the coordinates
     * of this point.
     * @param x the X coordinate addition
     * @param y the Y coordinate addition
     * @return the point with added coordinates
     * @since JavaFX 8.0
     */
    public Point2D add(double x, double y) {
        return new Point2D(
                getX() + x,
                getY() + y);
    }

    /**
     * Returns a point with the coordinates of the specified point added to the
     * coordinates of this point.
     * @param point the point whose coordinates are to be added
     * @return the point with added coordinates
     * @throws NullPointerException if the specified {@code point} is null
     * @since JavaFX 8.0
     */
    public Point2D add(Point2D point) {
        return add(point.getX(), point.getY());
    }

    /**
     * Returns a point with the specified coordinates subtracted from
     * the coordinates of this point.
     * @param x the X coordinate subtraction
     * @param y the Y coordinate subtraction
     * @return the point with subtracted coordinates
     * @since JavaFX 8.0
     */
    public Point2D subtract(double x, double y) {
        return new Point2D(
                getX() - x,
                getY() - y);
    }

    /**
     * Returns a point with the coordinates of this point multiplied
     * by the specified factor
     * @param factor the factor multiplying the coordinates
     * @return the point with multiplied coordinates
     * @since JavaFX 8.0
     */
    public Point2D multiply(double factor) {
        return new Point2D(getX() * factor, getY() * factor);
    }

    /**
     * Returns a point with the coordinates of the specified point subtracted
     * from the coordinates of this point.
     * @param point the point whose coordinates are to be subtracted
     * @return the point with subtracted coordinates
     * @throws NullPointerException if the specified {@code point} is null
     * @since JavaFX 8.0
     */
    public Point2D subtract(Point2D point) {
        return subtract(point.getX(), point.getY());
    }

    /**
     * Normalizes the relative magnitude vector represented by this instance.
     * Returns a vector with the same direction and magnitude equal to 1.
     * If this is a zero vector, a zero vector is returned.
     * @return the normalized vector represented by a {@code Point2D} instance
     * @since JavaFX 8.0
     */
    public Point2D normalize() {
        final double mag = magnitude();

        if (mag == 0.0) {
            return new Point2D(0.0, 0.0);
        }

        return new Point2D(
                getX() / mag,
                getY() / mag);
    }

    /**
     * Returns a point which lies in the middle between this point and the
     * specified coordinates.
     * @param x the X coordinate of the second endpoint
     * @param y the Y coordinate of the second endpoint
     * @return the point in the middle
     * @since JavaFX 8.0
     */
    public Point2D midpoint(double x, double y) {
        return new Point2D(
                x + (getX() - x) / 2.0,
                y + (getY() - y) / 2.0);
    }

    /**
     * Returns a point which lies in the middle between this point and the
     * specified point.
     * @param point the other endpoint
     * @return the point in the middle
     * @throws NullPointerException if the specified {@code point} is null
     * @since JavaFX 8.0
     */
    public Point2D midpoint(Point2D point) {
        return midpoint(point.getX(), point.getY());
    }

    /**
     * Computes the angle (in degrees) between the vector represented
     * by this point and the specified vector.
     * @param x the X magnitude of the other vector
     * @param y the Y magnitude of the other vector
     * @return the angle between the two vectors measured in degrees
     * @since JavaFX 8.0
     */
    public double angle(double x, double y) {
        final double ax = getX();
        final double ay = getY();

        final double delta = (ax * x + ay * y) / Math.sqrt(
                (ax * ax + ay * ay) * (x * x + y * y));

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
    public double angle(Point2D point) {
        return angle(point.getX(), point.getY());
    }

    /**
     * Computes the angle (in degrees) between the three points with this point
     * as a vertex.
     * @param p1 one point
     * @param p2 other point
     * @return angle between the vectors (this, p1) and (this, p2) measured
     *         in degrees, {@code NaN} if the three points are not different
     *         from one another
     * @throws NullPointerException if {@code p1} or {@code p2} is null
     * @since JavaFX 8.0
     */
    public double angle(Point2D p1, Point2D p2) {
        final double x = getX();
        final double y = getY();

        final double ax = p1.getX() - x;
        final double ay = p1.getY() - y;
        final double bx = p2.getX() - x;
        final double by = p2.getY() - y;

        final double delta = (ax * bx + ay * by) / Math.sqrt(
                (ax * ax + ay * ay) * (bx * bx + by * by));

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

        return Math.sqrt(x * x + y * y);
    }

    /**
     * Computes dot (scalar) product of the vector represented by this instance
     * and the specified vector.
     * @param x the X magnitude of the other vector
     * @param y the Y magnitude of the other vector
     * @return the dot product of the two vectors
     * @since JavaFX 8.0
     */
    public double dotProduct(double x, double y) {
        return getX() * x + getY() * y;
    }

    /**
     * Computes dot (scalar) product of the vector represented by this instance
     * and the specified vector.
     * @param vector the other vector
     * @return the dot product of the two vectors
     * @throws NullPointerException if the specified {@code vector} is null
     * @since JavaFX 8.0
     */
    public double dotProduct(Point2D vector) {
        return dotProduct(vector.getX(), vector.getY());
    }

    /**
     * Computes cross product of the vector represented by this instance
     * and the specified vector.
     * @param x the X magnitude of the other vector
     * @param y the Y magnitude of the other vector
     * @return the cross product of the two vectors
     * @since JavaFX 8.0
     */
    public Point3D crossProduct(double x, double y) {
        final double ax = getX();
        final double ay = getY();

        return new Point3D(
                0, 0, ax * y - ay * x);
    }

    /**
     * Computes cross product of the vector represented by this instance
     * and the specified vector.
     * @param vector the other vector
     * @return the cross product of the two vectors
     * @throws NullPointerException if the specified {@code vector} is null
     * @since JavaFX 8.0
     */
    public Point3D crossProduct(Point2D vector) {
        return crossProduct(vector.getX(), vector.getY());
    }

    /**
     * {@inheritDoc}
     *
     * @since 13
     */
    @Override
    public Point2D interpolate(Point2D endValue, double t) {
        if (t <= 0.0) return this;
        if (t >= 1.0) return endValue;
        return new Point2D(
            getX() + (endValue.getX() - getX()) * t,
            getY() + (endValue.getY() - getY()) * t
        );
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
     */
    @Override public String toString() {
        return "Point2D [x = " + getX() + ", y = " + getY() + "]";
    }
}
