/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * The base class for objects that are used to describe the bounds of a node or
 * other scene graph object. One interesting characteristic of a Bounds object
 * is that it may have a negative width, height, or depth. A negative value
 * for any of these indicates that the Bounds are "empty".
 *
 * @since JavaFX 2.0
 */
public abstract class Bounds {

    /**
     * The x coordinate of the upper-left corner of this {@code Bounds}.
     *
     * @return the x coordinate of the upper-left corner
     * @defaultValue 0.0
     */
    public final double getMinX() { return minX; }
    private double minX;

    /**
     * The y coordinate of the upper-left corner of this {@code Bounds}.
     *
     * @return the y coordinate of the upper-left corner
     * @defaultValue 0.0
     */
    public final double getMinY() { return minY; }
    private double minY;

    /**
     * The minimum z coordinate of this {@code Bounds}.
     *
     * @return the minimum z coordinate
     * @defaultValue 0.0
     */
    public final double getMinZ() { return minZ; }
    private double minZ;

    /**
     * The width of this {@code Bounds}.
     *
     * @return the width
     * @defaultValue 0.0
     */
    public final double getWidth() { return width; }
    private double width;

    /**
     * The height of this {@code Bounds}.
     *
     * @return the height
     * @defaultValue 0.0
     */
    public final double getHeight() { return height; }
    private double height;

    /**
     * The depth of this {@code Bounds}.
     *
     * @return the depth
     * @defaultValue 0.0
     */
    public final double getDepth() { return depth; }
    private double depth;

    /**
     * The x coordinate of the lower-right corner of this {@code Bounds}.
     *
     * @return the x coordinate of the lower-right corner
     * @defaultValue {@code minX + width}
     */
    public final double getMaxX() { return maxX; }
    private double maxX;

    /**
     * The y coordinate of the lower-right corner of this {@code Bounds}.
     *
     * @return the y coordinate of the lower-right corner
     * @defaultValue {@code minY + height}
     */
    public final double getMaxY() { return maxY; }
    private double maxY;

    /**
     * The maximum z coordinate of this {@code Bounds}.
     *
     * @return the maximum z coordinate
     * @defaultValue {@code minZ + depth}
     */
    public final double getMaxZ() { return maxZ; }
    private double maxZ;

    /**
     * The central x coordinate of this {@code Bounds}.
     *
     * @return the central x coordinate
     * @implSpec This call is equivalent to {@code (getMaxX() + getMinX())/2.0}.
     * @since 11
     */
    public final double getCenterX() {
        return (getMaxX() + getMinX()) * 0.5;
    }

    /**
     * The central y coordinate of this {@code Bounds}.
     *
     * @return the central y coordinate
     * @implSpec This call is equivalent to {@code (getMaxY() + getMinY())/2.0}.
     * @since 11
     */
    public final double getCenterY() {
        return (getMaxY() + getMinY()) * 0.5;
    }

    /**
     * The central z coordinate of this {@code Bounds}.
     *
     * @return the central z coordinate
     * @implSpec This call is equivalent to {@code (getMaxZ() + getMinZ())/2.0}.
     * @since 11
     */
    public final double getCenterZ() {
        return (getMaxZ() + getMinZ()) * 0.5;
    }

    /**
     * Indicates whether any of the dimensions(width, height or depth) of this bounds
     * is less than zero.
     * @return true if any of the dimensions(width, height or depth) of this bounds
     * is less than zero
     */
    public abstract boolean isEmpty();

    /**
     * Tests if the specified point is inside the boundary of {@code Bounds}.
     *
     * @param p the specified point to be tested
     * @return true if the specified point is inside the boundary of this
     * {@code Bounds}; false otherwise
     */
    public abstract boolean contains(Point2D p);

    /**
     * Tests if the specified point is inside the boundary of {@code Bounds}.
     *
     * @param p the specified 3D point to be tested
     * @return true if the specified point is inside the boundary of this
     * {@code Bounds}; false otherwise
     */
    public abstract boolean contains(Point3D p);

    /**
     * Tests if the specified {@code (x, y)} coordinates are inside the boundary
     * of {@code Bounds}.
     *
     * @param x the specified x coordinate to be tested
     * @param y the specified y coordinate to be tested
     * @return true if the specified {@code (x, y)} coordinates are inside the
     * boundary of this {@code Bounds}; false otherwise
     */
    public abstract boolean contains(double x, double y);

    /**
     * Tests if the specified {@code (x, y, z)} coordinates are inside the boundary
     * of {@code Bounds}.
     *
     * @param x the specified x coordinate to be tested
     * @param y the specified y coordinate to be tested
     * @param z the specified z coordinate to be tested
     * @return true if the specified {@code (x, y)} coordinates are inside the
     * boundary of this {@code Bounds}; false otherwise
     */
    public abstract boolean contains(double x, double y, double z);

    /**
     * Tests if the interior of this {@code Bounds} entirely contains the
     * specified Bounds, {@code b}.
     *
     * @param b The specified Bounds
     * @return true if the specified Bounds, {@code b}, is inside the
     * boundary of this {@code Bounds}; false otherwise
     */
    public abstract boolean contains(Bounds b);

    /**
     * Tests if the interior of this {@code Bounds} entirely contains the
     * specified rectangular area.
     *
     * @param x the x coordinate of the upper-left corner of the specified
     * rectangular area
     * @param y the y coordinate of the upper-left corner of the specified
     * rectangular area
     * @param w the width of the specified rectangular area
     * @param h the height of the specified rectangular area
     * @return true if the interior of this {@code Bounds} entirely contains
     * the specified rectangular area; false otherwise
     */
    public abstract boolean contains(double x, double y, double w, double h);

    /**
     * Tests if the interior of this {@code Bounds} entirely contains the
     * specified rectangular area.
     *
     * @param x the x coordinate of the upper-left corner of the specified
     * rectangular volume
     * @param y the y coordinate of the upper-left corner of the specified
     * rectangular volume
     * @param z the z coordinate of the upper-left corner of the specified
     * rectangular volume
     * @param w the width of the specified rectangular volume
     * @param h the height of the specified rectangular volume
     * @param d the depth of the specified rectangular volume
     * @return true if the interior of this {@code Bounds} entirely contains
     * the specified rectangular area; false otherwise
     */
    public abstract boolean contains(double x, double y, double z,
            double w, double h, double d);

    /**
     * Tests if the interior of this {@code Bounds} intersects the interior
     * of a specified Bounds, {@code b}.
     *
     * @param b The specified Bounds
     * @return true if the interior of this {@code Bounds} and the interior
     * of the specified Bounds, {@code b}, intersect
     */
    public abstract boolean intersects(Bounds b);

    /**
     * Tests if the interior of this {@code Bounds} intersects the interior
     * of a specified rectangular area.
     *
     * @param x the x coordinate of the upper-left corner of the specified
     * rectangular area
     * @param y the y coordinate of the upper-left corner of the specified
     * rectangular area
     * @param w the width of the specified rectangular area
     * @param h the height of the specified rectangular area
     * @return true if the interior of this {@code Bounds} and the interior
     * of the rectangular area intersect
     */
    public abstract boolean intersects(double x, double y, double w, double h);

    /**
     * Tests if the interior of this {@code Bounds} intersects the interior
     * of a specified rectangular area.
     *
     * @param x the x coordinate of the upper-left corner of the specified
     * rectangular volume
     * @param y the y coordinate of the upper-left corner of the specified
     * rectangular volume
     * @param z the z coordinate of the upper-left corner of the specified
     * rectangular volume
     * @param w the width of the specified rectangular volume
     * @param h the height of the specified rectangular volume
     * @param d the depth of the specified rectangular volume
     * @return true if the interior of this {@code Bounds} and the interior
     * of the rectangular area intersect
     */
    public abstract boolean intersects(double x, double y, double z,
            double w, double h, double d);

    /**
     * Creates a new instance of {@code Bounds} class.
     * @param minX the X coordinate of the upper-left corner
     * @param minY the Y coordinate of the upper-left corner
     * @param minZ the minimum z coordinate of the {@code Bounds}
     * @param width the width of the {@code Bounds}
     * @param height the height of the {@code Bounds}
     * @param depth the depth of the {@code Bounds}
     */
    protected Bounds(double minX, double minY, double minZ, double width, double height, double depth) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.maxX = minX + width;
        this.maxY = minY + height;
        this.maxZ = minZ + depth;
    }
}
