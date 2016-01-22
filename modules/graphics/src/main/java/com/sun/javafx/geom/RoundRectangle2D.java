/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.transform.BaseTransform;

/**
 * The <code>RoundRectangle2D</code> class defines a rectangle with
 * rounded corners defined by a location {@code (x,y)}, a
 * dimension {@code (w x h)}, and the width and height of an arc
 * with which to round the corners.
 *
 * @version 1.29, 05/05/07
 */
public class RoundRectangle2D extends RectangularShape {
    /**
     * The X coordinate of this <code>RoundRectangle2D</code>.
     */
    public float x;

    /**
     * The Y coordinate of this <code>RoundRectangle2D</code>.
     */
    public float y;

    /**
     * The width of this <code>RoundRectangle2D</code>.
     */
    public float width;

    /**
     * The height of this <code>RoundRectangle2D</code>.
     */
    public float height;

    /**
     * The width of the arc that rounds off the corners.
     */
    public float arcWidth;

    /**
     * The height of the arc that rounds off the corners.
     */
    public float arcHeight;

    /**
     * Constructs a new <code>RoundRectangle2D</code>, initialized to
     * location (0.0,&nbsp;0.0), size (0.0,&nbsp;0.0), and corner arcs
     * of radius 0.0.
     */
    public RoundRectangle2D() {
    }

    /**
     * Constructs and initializes a <code>RoundRectangle2D</code>
     * from the specified <code>float</code> coordinates.
     *
     * @param x the X coordinate of the newly
     *          constructed <code>RoundRectangle2D</code>
     * @param y the Y coordinate of the newly
     *          constructed <code>RoundRectangle2D</code>
     * @param w the width to which to set the newly
     *          constructed <code>RoundRectangle2D</code>
     * @param h the height to which to set the newly
     *          constructed <code>RoundRectangle2D</code>
     * @param arcw the width of the arc to use to round off the
     *             corners of the newly constructed
     *             <code>RoundRectangle2D</code>
     * @param arch the height of the arc to use to round off the
     *             corners of the newly constructed
     *             <code>RoundRectangle2D</code>
     */
    public RoundRectangle2D(float x, float y, float w, float h, float arcw, float arch) {
        setRoundRect(x, y, w, h, arcw, arch);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getX() {
        return x;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getY() {
        return y;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getWidth() {
        return width;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getHeight() {
        return height;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return (width <= 0.0f) || (height <= 0.0f);
    }

    /**
     * Sets the location, size, and corner radii of this
     * <code>RoundRectangle2D</code> to the specified
     * <code>float</code> values.
     *
     * @param x the X coordinate to which to set the
     *          location of this <code>RoundRectangle2D</code>
     * @param y the Y coordinate to which to set the
     *          location of this <code>RoundRectangle2D</code>
     * @param w the width to which to set this
     *          <code>RoundRectangle2D</code>
     * @param h the height to which to set this
     *          <code>RoundRectangle2D</code>
     * @param arcw the width to which to set the arc of this
     *             <code>RoundRectangle2D</code>
     * @param arch the height to which to set the arc of this
     *             <code>RoundRectangle2D</code>
     */
    public void setRoundRect(float x, float y, float w, float h,
                                 float arcw, float arch)
    {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
        this.arcWidth = arcw;
        this.arcHeight = arch;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RectBounds getBounds() {
        return new RectBounds(x, y, x + width, y + height);
    }

    /**
     * Sets this <code>RoundRectangle2D</code> to be the same as the
     * specified <code>RoundRectangle2D</code>.
     * @param rr the specified <code>RoundRectangle2D</code>
     */
    public void setRoundRect(RoundRectangle2D rr) {
        setRoundRect(rr.x, rr.y, rr.width, rr.height, rr.arcWidth, rr.arcHeight);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFrame(float x, float y, float w, float h) {
        setRoundRect(x, y, w, h, this.arcWidth, this.arcHeight);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(float x, float y) {
        if (isEmpty()) { return false; }
        float rrx0 = this.x;
        float rry0 = this.y;
        float rrx1 = rrx0 + this.width;
        float rry1 = rry0 + this.height;
        // Check for trivial rejection - point is outside bounding rectangle
        if (x < rrx0 || y < rry0 || x >= rrx1 || y >= rry1) {
            return false;
        }
        float aw = Math.min(this.width, Math.abs(this.arcWidth)) / 2f;
        float ah = Math.min(this.height, Math.abs(this.arcHeight)) / 2f;
        // Check which corner point is in and do circular containment
        // test - otherwise simple acceptance
        if (x >= (rrx0 += aw) && x < (rrx0 = rrx1 - aw)) {
            return true;
        }
        if (y >= (rry0 += ah) && y < (rry0 = rry1 - ah)) {
            return true;
        }
        x = (x - rrx0) / aw;
        y = (y - rry0) / ah;
        return (x * x + y * y <= 1.0);
    }

    private int classify(float coord, float left, float right, float arcsize) {
        if (coord < left) {
            return 0;
        } else if (coord < left + arcsize) {
            return 1;
        } else if (coord < right - arcsize) {
            return 2;
        } else if (coord < right) {
            return 3;
        } else {
            return 4;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean intersects(float x, float y, float w, float h) {
        if (isEmpty() || w <= 0 || h <= 0) {
            return false;
        }
        float rrx0 = this.x;
        float rry0 = this.y;
        float rrx1 = rrx0 + this.width;
        float rry1 = rry0 + this.height;
        // Check for trivial rejection - bounding rectangles do not intersect
        if (x + w <= rrx0 || x >= rrx1 || y + h <= rry0 || y >= rry1) {
            return false;
        }
        float aw = Math.min(this.width, Math.abs(this.arcWidth)) / 2f;
        float ah = Math.min(this.height, Math.abs(this.arcHeight)) / 2f;
        int x0class = classify(x, rrx0, rrx1, aw);
        int x1class = classify(x + w, rrx0, rrx1, aw);
        int y0class = classify(y, rry0, rry1, ah);
        int y1class = classify(y + h, rry0, rry1, ah);
        // Trivially accept if any point is inside inner rectangle
        if (x0class == 2 || x1class == 2 || y0class == 2 || y1class == 2) {
            return true;
        }
        // Trivially accept if either edge spans inner rectangle
        if ((x0class < 2 && x1class > 2) || (y0class < 2 && y1class > 2)) {
            return true;
        }
        // Since neither edge spans the center, then one of the corners
        // must be in one of the rounded edges.  We detect this case if
        // a [xy]0class is 3 or a [xy]1class is 1.  One of those two cases
        // must be true for each direction.
        // We now find a "nearest point" to test for being inside a rounded
        // corner.
        x = (x1class == 1) ? (x = x + w - (rrx0 + aw)) : (x = x - (rrx1 - aw));
        y = (y1class == 1) ? (y = y + h - (rry0 + ah)) : (y = y - (rry1 - ah));
        x = x / aw;
        y = y / ah;
        return (x * x + y * y <= 1f);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(float x, float y, float w, float h) {
        if (isEmpty() || w <= 0 || h <= 0) {
            return false;
        }
        return (contains(x, y) &&
            contains(x + w, y) &&
            contains(x, y + h) &&
            contains(x + w, y + h));
    }

    /**
     * Returns an iteration object that defines the boundary of this
     * <code>RoundRectangle2D</code>.
     * The iterator for this class is multi-threaded safe, which means
     * that this <code>RoundRectangle2D</code> class guarantees that
     * modifications to the geometry of this <code>RoundRectangle2D</code>
     * object do not affect any iterations of that geometry that
     * are already in process.
     * @param tx an optional <code>BaseTransform</code> to be applied to
     * the coordinates as they are returned in the iteration, or
     * <code>null</code> if untransformed coordinates are desired
     * @return    the <code>PathIterator</code> object that returns the
     *          geometry of the outline of this
     *          <code>RoundRectangle2D</code>, one segment at a time.
     */
    @Override
    public PathIterator getPathIterator(BaseTransform tx) {
        return new RoundRectIterator(this, tx);
    }

    @Override
    public RoundRectangle2D copy() {
        return new RoundRectangle2D(x, y, width, height, arcWidth, arcHeight);
    }

    /**
     * Returns the hashcode for this <code>RoundRectangle2D</code>.
     * @return the hashcode for this <code>RoundRectangle2D</code>.
     */
    @Override
    public int hashCode() {
        int bits = java.lang.Float.floatToIntBits(x);
        bits += java.lang.Float.floatToIntBits(y) * 37;
        bits += java.lang.Float.floatToIntBits(width) * 43;
        bits += java.lang.Float.floatToIntBits(height) * 47;
        bits += java.lang.Float.floatToIntBits(arcWidth) * 53;
        bits += java.lang.Float.floatToIntBits(arcHeight) * 59;
        return bits;
    }

    /**
     * Determines whether or not the specified <code>Object</code> is
     * equal to this <code>RoundRectangle2D</code>.  The specified
     * <code>Object</code> is equal to this <code>RoundRectangle2D</code>
     * if it is an instance of <code>RoundRectangle2D</code> and if its
     * location, size, and corner arc dimensions are the same as this
     * <code>RoundRectangle2D</code>.
     * @param obj  an <code>Object</code> to be compared with this
     *             <code>RoundRectangle2D</code>.
     * @return  <code>true</code> if <code>obj</code> is an instance
     *          of <code>RoundRectangle2D</code> and has the same values;
     *          <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof RoundRectangle2D) {
            RoundRectangle2D rr2d = (RoundRectangle2D) obj;
            return ((x == rr2d.x) &&
                    (y == rr2d.y) &&
                    (width == rr2d.width) &&
                    (height == rr2d.height) &&
                    (arcWidth == rr2d.arcWidth) &&
                    (arcHeight == rr2d.arcHeight));
        }
        return false;
    }
}
