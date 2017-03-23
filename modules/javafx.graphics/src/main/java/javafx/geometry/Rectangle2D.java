/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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


/**
 * A 2D rectangle used to describe the bounds of an object. It is defined by a
 * location (minX, minY) and dimension (width x height).
 * @since JavaFX 2.0
 */
public class Rectangle2D {
    /**
     * An empty {@code Rectangle2D} instance (with all coordinates equal to zero).
     */
    public static final Rectangle2D EMPTY = new Rectangle2D(0, 0, 0, 0);

    /**
     * The x coordinate of the upper-left corner of this {@code Rectangle2D}.
     *
     * @return the x coordinate of the upper-left corner
     * @defaultValue 0.0
     */
    public double getMinX() { return minX; }
    private double minX;

    /**
     * The y coordinate of the upper-left corner of this {@code Rectangle2D}.
     *
     * @return the y coordinate of the upper-left corner
     * @defaultValue 0.0
     */
    public double getMinY() { return minY; }
    private double minY;

    /**
     * The width of this {@code Rectangle2D}.
     *
     * @return the width
     * @defaultValue 0.0
     */
    public double getWidth() { return width; }
    private double width;

    /**
     * The height of this {@code Rectangle2D}.
     *
     * @return the height
     * @defaultValue 0.0
     */
    public double getHeight() { return height; }
    private double height;

    /**
     * The x coordinate of the lower-right corner of this {@code Rectangle2D}.
     *
     * @return the x coordinate of the lower-right corner
     * @defaultValue {@code minX + width}
     */
    public double getMaxX() { return maxX; }
    private double maxX;

    /**
     * The y coordinate of the lower-right corner of this {@code Rectangle2D}.
     *
     * @return the y coordinate of the lower-right corner
     * @defaultValue {@code minY + height}
     */
    public double getMaxY() { return maxY; }
    private double maxY;

    /**
     * Cache the hash code to make computing hashes faster.
     */
    private int hash = 0;

    /**
     * Creates a new instance of {@code Rectangle2D}.
     * @param minX The x coordinate of the upper-left corner of the {@code Rectangle2D}
     * @param minY The y coordinate of the upper-left corner of the {@code Rectangle2D}
     * @param width The width of the {@code Rectangle2D}
     * @param height The height of the {@code Rectangle2D}
     */
    public Rectangle2D(@NamedArg("minX") double minX, @NamedArg("minY") double minY, @NamedArg("width") double width, @NamedArg("height") double height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Both width and height must be >= 0");
        }

        this.minX = minX;
        this.minY = minY;
        this.width = width;
        this.height = height;
        this.maxX = minX + width;
        this.maxY = minY + height;
    }

   /**
    * Tests if the specified point is inside the boundary of {@code Rectangle2D}.
    *
    * @param p the specified point to be tested
    * @return true if the specified point is inside the boundary of this
    * {@code Rectangle2D}; false otherwise
    */
    public boolean contains(Point2D p) {
        if (p == null) return false;
        return contains(p.getX(), p.getY());
    }

   /**
    * Tests if the specified {@code (x, y)} coordinates are inside the boundary
    * of {@code Rectangle2D}.
    *
    * @param x the specified x coordinate to be tested
    * @param y the specified y coordinate to be tested
    * @return true if the specified {@code (x, y)} coordinates are inside the
    * boundary of this {@code Rectangle2D}; false otherwise
    */
    public boolean contains(double x, double y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

   /**
    * Tests if the interior of this {@code Rectangle2D} entirely contains the
    * specified Rectangle2D, {@code r}.
    *
    * @param r The specified Rectangle2D
    * @return true if the specified Rectangle2D, {@code r}, is inside the
    * boundary of this {@code Rectangle2D}; false otherwise
    */
    public boolean contains(Rectangle2D r) {
        if (r == null) return false;
        return r.minX >= minX && r.minY >= minY && r.maxX <= maxX && r.maxY <= maxY;
    }

   /**
    * Tests if the interior of this {@code Rectangle2D} entirely contains the
    * specified rectangular area.
    *
    * @param x the x coordinate of the upper-left corner of the specified
    * rectangular area
    * @param y the y coordinate of the upper-left corner of the specified
    * rectangular area
    * @param w the width of the specified rectangular area
    * @param h the height of the specified rectangular area
    * @return true if the interior of this {@code Rectangle2D} entirely contains
    * the specified rectangular area; false otherwise
    */
    public boolean contains(double x, double y, double w, double h) {
        return x >= minX && y >= minY && w <= maxX - x && h <= maxY - y;
    }

   /**
    * Tests if the interior of this {@code Rectangle2D} intersects the interior
    * of a specified Rectangle2D, {@code r}.
    *
    * @param r The specified Rectangle2D
    * @return true if the interior of this {@code Rectangle2D} and the interior
    * of the specified Rectangle2D, {@code r}, intersect
    */
    public boolean intersects(Rectangle2D r) {
        if (r == null) return false;
        return r.maxX > minX && r.maxY > minY && r.minX < maxX && r.minY < maxY;
    }

   /**
    * Tests if the interior of this {@code Rectangle2D} intersects the interior
    * of a specified rectangular area.
    *
    * @param x the x coordinate of the upper-left corner of the specified
    * rectangular area
    * @param y the y coordinate of the upper-left corner of the specified
    * rectangular area
    * @param w the width of the specified rectangular area
    * @param h the height of the specified rectangular area
    * @return true if the interior of this {@code Rectangle2D} and the interior
    * of the rectangular area intersect
    */
    public boolean intersects(double x, double y, double w, double h) {
        return x < maxX && y < maxY && x + w > minX && y + h > minY;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is equal to the {@code obj} argument; {@code false} otherwise
     */
    @Override public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Rectangle2D) {
            Rectangle2D other = (Rectangle2D) obj;
            return minX == other.minX
                && minY == other.minY
                && width == other.width
                && height == other.height;
        } else return false;
    }

    /**
     * Returns a hash code for this {@code Rectangle2D} object.
     * @return a hash code for this {@code Rectangle2D} object.
     */
    @Override public int hashCode() {
        if (hash == 0) {
            long bits = 7L;
            bits = 31L * bits + Double.doubleToLongBits(minX);
            bits = 31L * bits + Double.doubleToLongBits(minY);
            bits = 31L * bits + Double.doubleToLongBits(width);
            bits = 31L * bits + Double.doubleToLongBits(height);
            hash = (int) (bits ^ (bits >> 32));
        }
        return hash;
    }

    /**
     * Returns a string representation of this {@code Rectangle2D}.
     * This method is intended to be used only for informational purposes.
     * The content and format of the returned string might vary between
     * implementations.
     * The returned string might be empty but cannot be {@code null}.
     */
    @Override public String toString() {
        return "Rectangle2D [minX = " + minX
                + ", minY=" + minY
                + ", maxX=" + maxX
                + ", maxY=" + maxY
                + ", width=" + width
                + ", height=" + height
                + "]";
    }
}
