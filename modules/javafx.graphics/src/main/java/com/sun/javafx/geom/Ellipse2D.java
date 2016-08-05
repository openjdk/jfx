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
 * The <code>Ellipse2D</code> class describes an ellipse that is defined
 * by a framing rectangle.
 *
 * @version 1.26, 05/05/07
 */
public class Ellipse2D extends RectangularShape {
    /**
     * The X coordinate of the upper-left corner of the
     * framing rectangle of this {@code Ellipse2D}.
     */
    public float x;

    /**
     * The Y coordinate of the upper-left corner of the
     * framing rectangle of this {@code Ellipse2D}.
     */
    public float y;

    /**
     * The overall width of this <code>Ellipse2D</code>.
     */
    public float width;

    /**
     * The overall height of this <code>Ellipse2D</code>.
     */
    public float height;

    /**
     * Constructs a new <code>Ellipse2D</code>, initialized to
     * location (0,&nbsp;0) and size (0,&nbsp;0).
     */
    public Ellipse2D() { }

    /**
     * Constructs and initializes an <code>Ellipse2D</code> from the
     * specified coordinates.
     *
     * @param x the X coordinate of the upper-left corner
     *          of the framing rectangle
     * @param y the Y coordinate of the upper-left corner
     *          of the framing rectangle
     * @param w the width of the framing rectangle
     * @param h the height of the framing rectangle
     */
    public Ellipse2D(float x, float y, float w, float h) {
        setFrame(x, y, w, h);
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
        return (width <= 0f || height <= 0f);
    }

    /**
     * Sets the location and size of the framing rectangle of this
     * <code>Shape</code> to the specified rectangular values.
     *
     * @param x the X coordinate of the upper-left corner of the
     *          specified rectangular shape
     * @param y the Y coordinate of the upper-left corner of the
     *          specified rectangular shape
     * @param w the width of the specified rectangular shape
     * @param h the height of the specified rectangular shape
     */
    public void setFrame(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
    }

    /**
     * {@inheritDoc}
     */
    public RectBounds getBounds() {
        return new RectBounds(x, y, x + width, y + height);
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(float x, float y) {
        // Normalize the coordinates compared to the ellipse
        // having a center at 0,0 and a radius of 0.5.
        float ellw = this.width;
        if (ellw <= 0f) {
            return false;
        }
        float normx = (x - this.x) / ellw - 0.5f;
        float ellh = this.height;
        if (ellh <= 0f) {
            return false;
        }
        float normy = (y - this.y) / ellh - 0.5f;
        return (normx * normx + normy * normy) < 0.25f;
    }

    /**
     * {@inheritDoc}
     */
    public boolean intersects(float x, float y, float w, float h) {
        if (w <= 0f || h <= 0f) {
            return false;
        }
        // Normalize the rectangular coordinates compared to the ellipse
        // having a center at 0,0 and a radius of 0.5.
        float ellw = this.width;
        if (ellw <= 0f) {
            return false;
        }
        float normx0 = (x - this.x) / ellw - 0.5f;
        float normx1 = normx0 + w / ellw;
        float ellh = this.height;
        if (ellh <= 0f) {
            return false;
        }
        float normy0 = (y - this.y) / ellh - 0.5f;
        float normy1 = normy0 + h / ellh;
        // find nearest x (left edge, right edge, 0.0)
        // find nearest y (top edge, bottom edge, 0.0)
        // if nearest x,y is inside circle of radius 0.5, then intersects
        float nearx, neary;
        if (normx0 > 0f) {
            // center to left of X extents
            nearx = normx0;
        } else if (normx1 < 0f) {
            // center to right of X extents
            nearx = normx1;
        } else {
            nearx = 0f;
        }
        if (normy0 > 0f) {
            // center above Y extents
            neary = normy0;
        } else if (normy1 < 0f) {
            // center below Y extents
            neary = normy1;
        } else {
            neary = 0f;
        }
        return (nearx * nearx + neary * neary) < 0.25f;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(float x, float y, float w, float h) {
        return (contains(x, y) &&
            contains(x + w, y) &&
            contains(x, y + h) &&
            contains(x + w, y + h));
    }

    /**
     * Returns an iteration object that defines the boundary of this
     * <code>Ellipse2D</code>.
     * The iterator for this class is multi-threaded safe, which means
     * that this <code>Ellipse2D</code> class guarantees that
     * modifications to the geometry of this <code>Ellipse2D</code>
     * object do not affect any iterations of that geometry that
     * are already in process.
     * @param tx an optional <code>BaseTransform</code> to be applied to
     * the coordinates as they are returned in the iteration, or
     * <code>null</code> if untransformed coordinates are desired
     * @return    the <code>PathIterator</code> object that returns the
     *          geometry of the outline of this <code>Ellipse2D</code>,
     *      one segment at a time.
     */
    public PathIterator getPathIterator(BaseTransform tx) {
        return new EllipseIterator(this, tx);
    }

    @Override
    public Ellipse2D copy() {
        return new Ellipse2D(x, y, width, height);
    }

    /**
     * Returns the hashcode for this <code>Ellipse2D</code>.
     * @return the hashcode for this <code>Ellipse2D</code>.
     */
    @Override
    public int hashCode() {
        int bits = java.lang.Float.floatToIntBits(x);
        bits += java.lang.Float.floatToIntBits(y) * 37;
        bits += java.lang.Float.floatToIntBits(width) * 43;
        bits += java.lang.Float.floatToIntBits(height) * 47;
        return bits;
    }

    /**
     * Determines whether or not the specified <code>Object</code> is
     * equal to this <code>Ellipse2D</code>.  The specified
     * <code>Object</code> is equal to this <code>Ellipse2D</code>
     * if it is an instance of <code>Ellipse2D</code> and if its
     * location and size are the same as this <code>Ellipse2D</code>.
     * @param obj  an <code>Object</code> to be compared with this
     *             <code>Ellipse2D</code>.
     * @return  <code>true</code> if <code>obj</code> is an instance
     *          of <code>Ellipse2D</code> and has the same values;
     *          <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Ellipse2D) {
            Ellipse2D e2d = (Ellipse2D) obj;
            return ((x == e2d.x) &&
                    (y == e2d.y) &&
                    (width == e2d.width) &&
                    (height == e2d.height));
        }
        return false;
    }
}
