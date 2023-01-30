/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * <code>RectangularShape</code> is the base class for a number of
 * {@link Shape} objects whose geometry is defined by a rectangular frame.
 * This class does not directly specify any specific geometry by
 * itself, but merely provides manipulation methods inherited by
 * a whole category of <code>Shape</code> objects.
 * The manipulation methods provided by this class can be used to
 * query and modify the rectangular frame, which provides a reference
 * for the subclasses to define their geometry.
 *
 * @version     1.26, 05/05/07
 */
public abstract class RectangularShape extends Shape {

    /**
     * This is an abstract class that cannot be instantiated directly.
     *
     * @see Arc2D
     * @see Ellipse2D
     * @see Rectangle2D
     * @see RoundRectangle2D
     */
    protected RectangularShape() { }

    /**
     * Returns the X coordinate of the upper-left corner of
     * the framing rectangle in <code>double</code> precision.
     * @return the X coordinate of the upper-left corner of
     * the framing rectangle.
     */
    public abstract float getX();

    /**
     * Returns the Y coordinate of the upper-left corner of
     * the framing rectangle in <code>double</code> precision.
     * @return the Y coordinate of the upper-left corner of
     * the framing rectangle.
     */
    public abstract float getY();

    /**
     * Returns the width of the framing rectangle in
     * <code>double</code> precision.
     * @return the width of the framing rectangle.
     */
    public abstract float getWidth();

    /**
     * Returns the height of the framing rectangle
     * in <code>double</code> precision.
     * @return the height of the framing rectangle.
     */
    public abstract float getHeight();

    /**
     * Returns the smallest X coordinate of the framing
     * rectangle of the <code>Shape</code> in <code>double</code>
     * precision.
     * @return the smallest X coordinate of the framing
     *      rectangle of the <code>Shape</code>.
     */
    public float getMinX() {
        return getX();
    }

    /**
     * Returns the smallest Y coordinate of the framing
     * rectangle of the <code>Shape</code> in <code>double</code>
     * precision.
     * @return the smallest Y coordinate of the framing
     *      rectangle of the <code>Shape</code>.
     */
    public float getMinY() {
        return getY();
    }

    /**
     * Returns the largest X coordinate of the framing
     * rectangle of the <code>Shape</code> in <code>double</code>
     * precision.
     * @return the largest X coordinate of the framing
     *      rectangle of the <code>Shape</code>.
     */
    public float getMaxX() {
        return getX() + getWidth();
    }

    /**
     * Returns the largest Y coordinate of the framing
     * rectangle of the <code>Shape</code> in <code>double</code>
     * precision.
     * @return the largest Y coordinate of the framing
     *      rectangle of the <code>Shape</code>.
     */
    public float getMaxY() {
        return getY() + getHeight();
    }

    /**
     * Returns the X coordinate of the center of the framing
     * rectangle of the <code>Shape</code> in <code>double</code>
     * precision.
     * @return the X coordinate of the center of the framing rectangle
     *      of the <code>Shape</code>.
     */
    public float getCenterX() {
        return getX() + getWidth() / 2f;
    }

    /**
     * Returns the Y coordinate of the center of the framing
     * rectangle of the <code>Shape</code> in <code>double</code>
     * precision.
     * @return the Y coordinate of the center of the framing rectangle
     *      of the <code>Shape</code>.
     */
    public float getCenterY() {
        return getY() + getHeight() / 2f;
    }

    /**
     * Determines whether the <code>RectangularShape</code> is empty.
     * When the <code>RectangularShape</code> is empty, it encloses no
     * area.
     * @return <code>true</code> if the <code>RectangularShape</code> is empty;
     *      <code>false</code> otherwise.
     */
    public abstract boolean isEmpty();

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
     * @see #getFrame
     */
    public abstract void setFrame(float x, float y, float w, float h);

    /**
     * Sets the location and size of the framing rectangle of this
     * <code>Shape</code> to the specified {@link Point2D} and
     * {@link Dimension2D}, respectively.  The framing rectangle is used
     * by the subclasses of <code>RectangularShape</code> to define
     * their geometry.
     * @param loc the specified <code>Point2D</code>
     * @param size the specified <code>Dimension2D</code>
     * @see #getFrame
     */
    public void setFrame(Point2D loc, Dimension2D size) {
        setFrame(loc.x, loc.y, size.width, size.height);
    }

    /**
     * Sets the diagonal of the framing rectangle of this <code>Shape</code>
     * based on the two specified coordinates.  The framing rectangle is
     * used by the subclasses of <code>RectangularShape</code> to define
     * their geometry.
     *
     * @param x1 the X coordinate of the start point of the specified diagonal
     * @param y1 the Y coordinate of the start point of the specified diagonal
     * @param x2 the X coordinate of the end point of the specified diagonal
     * @param y2 the Y coordinate of the end point of the specified diagonal
     */
    public void setFrameFromDiagonal(float x1, float y1, float x2, float y2) {
        if (x2 < x1) {
            float t = x1;
            x1 = x2;
            x2 = t;
        }
        if (y2 < y1) {
            float t = y1;
            y1 = y2;
            y2 = t;
        }
        setFrame(x1, y1, x2 - x1, y2 - y1);
    }

    /**
     * Sets the diagonal of the framing rectangle of this <code>Shape</code>
     * based on two specified <code>Point2D</code> objects.  The framing
     * rectangle is used by the subclasses of <code>RectangularShape</code>
     * to define their geometry.
     *
     * @param p1 the start <code>Point2D</code> of the specified diagonal
     * @param p2 the end <code>Point2D</code> of the specified diagonal
     */
    public void setFrameFromDiagonal(Point2D p1, Point2D p2) {
        setFrameFromDiagonal(p1.x, p1.y, p2.x, p2.y);
    }

    /**
     * Sets the framing rectangle of this <code>Shape</code>
     * based on the specified center point coordinates and corner point
     * coordinates.  The framing rectangle is used by the subclasses of
     * <code>RectangularShape</code> to define their geometry.
     *
     * @param centerX the X coordinate of the specified center point
     * @param centerY the Y coordinate of the specified center point
     * @param cornerX the X coordinate of the specified corner point
     * @param cornerY the Y coordinate of the specified corner point
     */
    public void setFrameFromCenter(float centerX, float centerY,
                   float cornerX, float cornerY)
    {
        float halfW = Math.abs(cornerX - centerX);
        float halfH = Math.abs(cornerY - centerY);
        setFrame(centerX - halfW, centerY - halfH, halfW * 2f, halfH * 2f);
    }

    /**
     * Sets the framing rectangle of this <code>Shape</code> based on a
     * specified center <code>Point2D</code> and corner
     * <code>Point2D</code>.  The framing rectangle is used by the subclasses
     * of <code>RectangularShape</code> to define their geometry.
     * @param center the specified center <code>Point2D</code>
     * @param corner the specified corner <code>Point2D</code>
     */
    public void setFrameFromCenter(Point2D center, Point2D corner) {
        setFrameFromCenter(center.x, center.y, corner.x, corner.y);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Point2D p) {
        return contains(p.x, p.y);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RectBounds getBounds() {
        float width = getWidth();
        float height = getHeight();
        if (width < 0 || height < 0) {
            return new RectBounds();
        }
        float x = getX();
        float y = getY();
        float x1 = (float)Math.floor(x);
        float y1 = (float)Math.floor(y);
        float x2 = (float)Math.ceil(x + width);
        float y2 = (float)Math.ceil(y + height);
        return new RectBounds(x1, y1, x2, y2);
    }

    /**
     * Returns an iterator object that iterates along the
     * <code>Shape</code> object's boundary and provides access to a
     * flattened view of the outline of the <code>Shape</code>
     * object's geometry.
     * <p>
     * Only SEG_MOVETO, SEG_LINETO, and SEG_CLOSE point types will
     * be returned by the iterator.
     * <p>
     * The amount of subdivision of the curved segments is controlled
     * by the <code>flatness</code> parameter, which specifies the
     * maximum distance that any point on the unflattened transformed
     * curve can deviate from the returned flattened path segments.
     * An optional {@link BaseTransform} can
     * be specified so that the coordinates returned in the iteration are
     * transformed accordingly.
     * @param tx an optional <code>BaseTransform</code> to be applied to the
     *      coordinates as they are returned in the iteration,
     *      or <code>null</code> if untransformed coordinates are desired.
     * @param flatness the maximum distance that the line segments used to
     *          approximate the curved segments are allowed to deviate
     *          from any point on the original curve
     * @return a <code>PathIterator</code> object that provides access to
     *      the <code>Shape</code> object's flattened geometry.
     */
    @Override
    public PathIterator getPathIterator(BaseTransform tx, float flatness) {
        return new FlatteningPathIterator(getPathIterator(tx), flatness);
    }

    @Override
    public String toString() {
        return getClass().getName() +
            "[x=" + getX() +
            ",y=" + getY() +
            ",w=" + getWidth() +
            ",h=" + getHeight() + "]";
    }
}
