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
 * This <code>Line2D</code> represents a line segment in {@code (x,y)}
 * coordinate space.  This class, like all of the Java 2D API, uses a
 * default coordinate system called <i>user space</i> in which the y-axis
 * values increase downward and x-axis values increase to the right.  For
 * more information on the user space coordinate system, see the
 * <a href="http://java.sun.com/j2se/1.3/docs/guide/2d/spec/j2d-intro.fm2.html#61857">
 * Coordinate Systems</a> section of the Java 2D Programmer's Guide.
 *
 * @version     1.37, 05/05/07
 */
public class Line2D extends Shape {
    /**
     * The X coordinate of the start point of the line segment.
     */
    public float x1;

    /**
     * The Y coordinate of the start point of the line segment.
     */
    public float y1;

    /**
     * The X coordinate of the end point of the line segment.
     */
    public float x2;

    /**
     * The Y coordinate of the end point of the line segment.
     */
    public float y2;

    /**
     * Constructs and initializes a Line with coordinates (0, 0) -> (0, 0).
     */
    public Line2D() { }

    /**
     * Constructs and initializes a Line from the specified coordinates.
     * @param x1 the X coordinate of the start point
     * @param y1 the Y coordinate of the start point
     * @param x2 the X coordinate of the end point
     * @param y2 the Y coordinate of the end point
     */
    public Line2D(float x1, float y1, float x2, float y2) {
        setLine(x1, y1, x2, y2);
    }

    /**
     * Constructs and initializes a <code>Line2D</code> from the
     * specified <code>Point2D</code> objects.
     * @param p1 the start <code>Point2D</code> of this line segment
     * @param p2 the end <code>Point2D</code> of this line segment
     */
    public Line2D(Point2D p1, Point2D p2) {
        setLine(p1, p2);
    }

    /**
     * Sets the location of the end points of this <code>Line2D</code>
     * to the specified float coordinates.
     * @param x1 the X coordinate of the start point
     * @param y1 the Y coordinate of the start point
     * @param x2 the X coordinate of the end point
     * @param y2 the Y coordinate of the end point
     */
    public void setLine(float x1, float y1, float x2, float y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    /**
     * Sets the location of the end points of this <code>Line2D</code> to
     * the specified <code>Point2D</code> coordinates.
     * @param p1 the start <code>Point2D</code> of the line segment
     * @param p2 the end <code>Point2D</code> of the line segment
     */
    public void setLine(Point2D p1, Point2D p2) {
        setLine(p1.x, p1.y, p2.x, p2.y);
    }

    /**
     * Sets the location of the end points of this <code>Line2D</code> to
     * the same as those end points of the specified <code>Line2D</code>.
     * @param l the specified <code>Line2D</code>
     */
    public void setLine(Line2D l) {
        setLine(l.x1, l.y1, l.x2, l.y2);
    }

    /**
     * {@inheritDoc}
     */
    public RectBounds getBounds() {
        RectBounds b = new RectBounds();
        b.setBoundsAndSort(x1, y1, x2, y2);
        return b;
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean contains(float x, float y) { return false; }

    /**
     * @inheritDoc
     */
    @Override
    public boolean contains(float x, float y, float w, float h) { return false; }

    /**
     * @inheritDoc
     */
    @Override
    public boolean contains(Point2D p) { return false; }

    /**
     * @inheritDoc
     */
    @Override
    public boolean intersects(float x, float y, float w, float h) {
        int out1, out2;
        if ((out2 = outcode(x, y, w, h, x2, y2)) == 0) {
            return true;
        }
        float px = x1;
        float py = y1;
        while ((out1 = outcode(x, y, w, h, px, py)) != 0) {
            if ((out1 & out2) != 0) {
                return false;
            }
            if ((out1 & (OUT_LEFT | OUT_RIGHT)) != 0) {
                px = x;
                if ((out1 & OUT_RIGHT) != 0) {
                    px += w;
                }
                py = y1 + (px - x1) * (y2 - y1) / (x2 - x1);
            } else {
                py = y;
                if ((out1 & OUT_BOTTOM) != 0) {
                    py += h;
                }
                px = x1 + (py - y1) * (x2 - x1) / (y2 - y1);
            }
        }
        return true;
    }

    /**
     * Returns an indicator of where the specified point
     * {@code (px,py)} lies with respect to the line segment from
     * {@code (x1,y1)} to {@code (x2,y2)}.
     * The return value can be either 1, -1, or 0 and indicates
     * in which direction the specified line must pivot around its
     * first end point, {@code (x1,y1)}, in order to point at the
     * specified point {@code (px,py)}.
     * <p>A return value of 1 indicates that the line segment must
     * turn in the direction that takes the positive X axis towards
     * the negative Y axis.  In the default coordinate system used by
     * Java 2D, this direction is counterclockwise.
     * <p>A return value of -1 indicates that the line segment must
     * turn in the direction that takes the positive X axis towards
     * the positive Y axis.  In the default coordinate system, this
     * direction is clockwise.
     * <p>A return value of 0 indicates that the point lies
     * exactly on the line segment.  Note that an indicator value
     * of 0 is rare and not useful for determining colinearity
     * because of floating point rounding issues.
     * <p>If the point is colinear with the line segment, but
     * not between the end points, then the value will be -1 if the point
     * lies "beyond {@code (x1,y1)}" or 1 if the point lies
     * "beyond {@code (x2,y2)}".
     *
     * @param x1 the X coordinate of the start point of the
     *           specified line segment
     * @param y1 the Y coordinate of the start point of the
     *           specified line segment
     * @param x2 the X coordinate of the end point of the
     *           specified line segment
     * @param y2 the Y coordinate of the end point of the
     *           specified line segment
     * @param px the X coordinate of the specified point to be
     *           compared with the specified line segment
     * @param py the Y coordinate of the specified point to be
     *           compared with the specified line segment
     * @return an integer that indicates the position of the third specified
     *          coordinates with respect to the line segment formed
     *          by the first two specified coordinates.
     */
    public static int relativeCCW(float x1, float y1,
                  float x2, float y2,
                  float px, float py)
    {
        x2 -= x1;
        y2 -= y1;
        px -= x1;
        py -= y1;
        float ccw = px * y2 - py * x2;
        if (ccw == 0.0f) {
            // The point is colinear, classify based on which side of
            // the segment the point falls on.  We can calculate a
            // relative value using the projection of px,py onto the
            // segment - a negative value indicates the point projects
            // outside of the segment in the direction of the particular
            // endpoint used as the origin for the projection.
            ccw = px * x2 + py * y2;
            if (ccw > 0.0f) {
            // Reverse the projection to be relative to the original x2,y2
            // x2 and y2 are simply negated.
            // px and py need to have (x2 - x1) or (y2 - y1) subtracted
            //    from them (based on the original values)
            // Since we really want to get a positive answer when the
            //    point is "beyond (x2,y2)", then we want to calculate
            //    the inverse anyway - thus we leave x2 & y2 negated.
            px -= x2;
            py -= y2;
            ccw = px * x2 + py * y2;
            if (ccw < 0.0f) {
                ccw = 0.0f;
            }
            }
        }
        return (ccw < 0.0f) ? -1 : ((ccw > 0.0f) ? 1 : 0);
    }

    /**
     * Returns an indicator of where the specified point
     * {@code (px,py)} lies with respect to this line segment.
     * See the method comments of
     * {@link #relativeCCW(double, double, double, double, double, double)}
     * to interpret the return value.
     * @param px the X coordinate of the specified point
     *           to be compared with this <code>Line2D</code>
     * @param py the Y coordinate of the specified point
     *           to be compared with this <code>Line2D</code>
     * @return an integer that indicates the position of the specified
     *         coordinates with respect to this <code>Line2D</code>
     * @see #relativeCCW(double, double, double, double, double, double)
     */
    public int relativeCCW(float px, float py) {
        return relativeCCW(x1, y1, x2, y2, px, py);
    }

    /**
     * Returns an indicator of where the specified <code>Point2D</code>
     * lies with respect to this line segment.
     * See the method comments of
     * {@link #relativeCCW(double, double, double, double, double, double)}
     * to interpret the return value.
     * @param p the specified <code>Point2D</code> to be compared
     *          with this <code>Line2D</code>
     * @return an integer that indicates the position of the specified
     *         <code>Point2D</code> with respect to this <code>Line2D</code>
     * @see #relativeCCW(double, double, double, double, double, double)
     */
    public int relativeCCW(Point2D p) {
        return relativeCCW(x1, y1, x2, y2, p.x, p.y);
    }

    /**
     * Tests if the line segment from {@code (x1,y1)} to
     * {@code (x2,y2)} intersects the line segment from {@code (x3,y3)}
     * to {@code (x4,y4)}.
     *
     * @param x1 the X coordinate of the start point of the first
     *           specified line segment
     * @param y1 the Y coordinate of the start point of the first
     *           specified line segment
     * @param x2 the X coordinate of the end point of the first
     *           specified line segment
     * @param y2 the Y coordinate of the end point of the first
     *           specified line segment
     * @param x3 the X coordinate of the start point of the second
     *           specified line segment
     * @param y3 the Y coordinate of the start point of the second
     *           specified line segment
     * @param x4 the X coordinate of the end point of the second
     *           specified line segment
     * @param y4 the Y coordinate of the end point of the second
     *           specified line segment
     * @return <code>true</code> if the first specified line segment
     *          and the second specified line segment intersect
     *          each other; <code>false</code> otherwise.
     */
    public static boolean linesIntersect(float x1, float y1,
                     float x2, float y2,
                     float x3, float y3,
                     float x4, float y4)
    {
    return ((relativeCCW(x1, y1, x2, y2, x3, y3) *
         relativeCCW(x1, y1, x2, y2, x4, y4) <= 0)
        && (relativeCCW(x3, y3, x4, y4, x1, y1) *
            relativeCCW(x3, y3, x4, y4, x2, y2) <= 0));
    }

    /**
     * Tests if the line segment from {@code (x1,y1)} to
     * {@code (x2,y2)} intersects this line segment.
     *
     * @param x1 the X coordinate of the start point of the
     *           specified line segment
     * @param y1 the Y coordinate of the start point of the
     *           specified line segment
     * @param x2 the X coordinate of the end point of the
     *           specified line segment
     * @param y2 the Y coordinate of the end point of the
     *           specified line segment
     * @return <true> if this line segment and the specified line segment
     *          intersect each other; <code>false</code> otherwise.
     */
    public boolean intersectsLine(float x1, float y1, float x2, float y2) {
        return linesIntersect(x1, y1, x2, y2, this.x1, this.y1, this.x2, this.y2);
    }

    /**
     * Tests if the specified line segment intersects this line segment.
     * @param l the specified <code>Line2D</code>
     * @return <code>true</code> if this line segment and the specified line
     *          segment intersect each other;
     *          <code>false</code> otherwise.
     */
    public boolean intersectsLine(Line2D l) {
        return linesIntersect(l.x1, l.y1, l.x2, l.y2, this.x1, this.y1, this.x2, this.y2);
    }

    /**
     * Returns the square of the distance from a point to a line segment.
     * The distance measured is the distance between the specified
     * point and the closest point between the specified end points.
     * If the specified point intersects the line segment in between the
     * end points, this method returns 0.0.
     *
     * @param x1 the X coordinate of the start point of the
     *           specified line segment
     * @param y1 the Y coordinate of the start point of the
     *           specified line segment
     * @param x2 the X coordinate of the end point of the
     *           specified line segment
     * @param y2 the Y coordinate of the end point of the
     *           specified line segment
     * @param px the X coordinate of the specified point being
     *           measured against the specified line segment
     * @param py the Y coordinate of the specified point being
     *           measured against the specified line segment
     * @return a double value that is the square of the distance from the
     *          specified point to the specified line segment.
     * @see #ptLineDistSq(double, double, double, double, double, double)
     */
    public static float ptSegDistSq(float x1, float y1,
                     float x2, float y2,
                     float px, float py)
    {
        // Adjust vectors relative to x1,y1
        // x2,y2 becomes relative vector from x1,y1 to end of segment
        x2 -= x1;
        y2 -= y1;
        // px,py becomes relative vector from x1,y1 to test point
        px -= x1;
        py -= y1;
        float dotprod = px * x2 + py * y2;
        float projlenSq;
        if (dotprod <= 0f) {
            // px,py is on the side of x1,y1 away from x2,y2
            // distance to segment is length of px,py vector
            // "length of its (clipped) projection" is now 0.0
            projlenSq = 0f;
        } else {
            // switch to backwards vectors relative to x2,y2
            // x2,y2 are already the negative of x1,y1=>x2,y2
            // to get px,py to be the negative of px,py=>x2,y2
            // the dot product of two negated vectors is the same
            // as the dot product of the two normal vectors
            px = x2 - px;
            py = y2 - py;
            dotprod = px * x2 + py * y2;
            if (dotprod <= 0f) {
                // px,py is on the side of x2,y2 away from x1,y1
                // distance to segment is length of (backwards) px,py vector
                // "length of its (clipped) projection" is now 0.0
                projlenSq = 0f;
            } else {
                // px,py is between x1,y1 and x2,y2
                // dotprod is the length of the px,py vector
                // projected on the x2,y2=>x1,y1 vector times the
                // length of the x2,y2=>x1,y1 vector
                projlenSq = dotprod * dotprod / (x2 * x2 + y2 * y2);
            }
        }
        // Distance to line is now the length of the relative point
        // vector minus the length of its projection onto the line
        // (which is zero if the projection falls outside the range
        //  of the line segment).
        float lenSq = px * px + py * py - projlenSq;
        if (lenSq < 0f) {
            lenSq = 0f;
        }
        return lenSq;
    }

    /**
     * Returns the distance from a point to a line segment.
     * The distance measured is the distance between the specified
     * point and the closest point between the specified end points.
     * If the specified point intersects the line segment in between the
     * end points, this method returns 0.0.
     *
     * @param x1 the X coordinate of the start point of the
     *           specified line segment
     * @param y1 the Y coordinate of the start point of the
     *           specified line segment
     * @param x2 the X coordinate of the end point of the
     *           specified line segment
     * @param y2 the Y coordinate of the end point of the
     *           specified line segment
     * @param px the X coordinate of the specified point being
     *           measured against the specified line segment
     * @param py the Y coordinate of the specified point being
     *           measured against the specified line segment
     * @return a double value that is the distance from the specified point
     *              to the specified line segment.
     * @see #ptLineDist(double, double, double, double, double, double)
     */
    public static float ptSegDist(float x1, float y1,
                   float x2, float y2,
                   float px, float py)
    {
        return (float) Math.sqrt(ptSegDistSq(x1, y1, x2, y2, px, py));
    }

    /**
     * Returns the square of the distance from a point to this line segment.
     * The distance measured is the distance between the specified
     * point and the closest point between the current line's end points.
     * If the specified point intersects the line segment in between the
     * end points, this method returns 0.0.
     *
     * @param px the X coordinate of the specified point being
     *           measured against this line segment
     * @param py the Y coordinate of the specified point being
     *           measured against this line segment
     * @return a double value that is the square of the distance from the
     *          specified point to the current line segment.
     * @see #ptLineDistSq(double, double)
     */
    public float ptSegDistSq(float px, float py) {
        return ptSegDistSq(x1, y1, x2, y2, px, py);
    }

    /**
     * Returns the square of the distance from a <code>Point2D</code> to
     * this line segment.
     * The distance measured is the distance between the specified
     * point and the closest point between the current line's end points.
     * If the specified point intersects the line segment in between the
     * end points, this method returns 0.0.
     * @param pt the specified <code>Point2D</code> being measured against
     *           this line segment.
     * @return a double value that is the square of the distance from the
     *          specified <code>Point2D</code> to the current
     *          line segment.
     * @see #ptLineDistSq(Point2D)
     */
    public float ptSegDistSq(Point2D pt) {
        return ptSegDistSq(x1, y1, x2, y2, pt.x, pt.y);
    }

    /**
     * Returns the distance from a point to this line segment.
     * The distance measured is the distance between the specified
     * point and the closest point between the current line's end points.
     * If the specified point intersects the line segment in between the
     * end points, this method returns 0.0.
     *
     * @param px the X coordinate of the specified point being
     *           measured against this line segment
     * @param py the Y coordinate of the specified point being
     *           measured against this line segment
     * @return a double value that is the distance from the specified
     *          point to the current line segment.
     * @see #ptLineDist(double, double)
     */
    public double ptSegDist(float px, float py) {
        return ptSegDist(x1, y1, x2, y2, px, py);
    }

    /**
     * Returns the distance from a <code>Point2D</code> to this line
     * segment.
     * The distance measured is the distance between the specified
     * point and the closest point between the current line's end points.
     * If the specified point intersects the line segment in between the
     * end points, this method returns 0.0.
     * @param pt the specified <code>Point2D</code> being measured
     *      against this line segment
     * @return a double value that is the distance from the specified
     *              <code>Point2D</code> to the current line
     *              segment.
     * @see #ptLineDist(Point2D)
     */
    public float ptSegDist(Point2D pt) {
        return ptSegDist(x1, y1, x2, y2, pt.x, pt.y);
    }

    /**
     * Returns the square of the distance from a point to a line.
     * The distance measured is the distance between the specified
     * point and the closest point on the infinitely-extended line
     * defined by the specified coordinates.  If the specified point
     * intersects the line, this method returns 0.0.
     *
     * @param x1 the X coordinate of the start point of the specified line
     * @param y1 the Y coordinate of the start point of the specified line
     * @param x2 the X coordinate of the end point of the specified line
     * @param y2 the Y coordinate of the end point of the specified line
     * @param px the X coordinate of the specified point being
     *           measured against the specified line
     * @param py the Y coordinate of the specified point being
     *           measured against the specified line
     * @return a double value that is the square of the distance from the
     *          specified point to the specified line.
     * @see #ptSegDistSq(double, double, double, double, double, double)
     */
    public static float ptLineDistSq(float x1, float y1,
                      float x2, float y2,
                      float px, float py)
    {
        // Adjust vectors relative to x1,y1
        // x2,y2 becomes relative vector from x1,y1 to end of segment
        x2 -= x1;
        y2 -= y1;
        // px,py becomes relative vector from x1,y1 to test point
        px -= x1;
        py -= y1;
        float dotprod = px * x2 + py * y2;
        // dotprod is the length of the px,py vector
        // projected on the x1,y1=>x2,y2 vector times the
        // length of the x1,y1=>x2,y2 vector
        float projlenSq = dotprod * dotprod / (x2 * x2 + y2 * y2);
        // Distance to line is now the length of the relative point
        // vector minus the length of its projection onto the line
        float lenSq = px * px + py * py - projlenSq;
        if (lenSq < 0f) {
            lenSq = 0f;
        }
        return lenSq;
    }

    /**
     * Returns the distance from a point to a line.
     * The distance measured is the distance between the specified
     * point and the closest point on the infinitely-extended line
     * defined by the specified coordinates.  If the specified point
     * intersects the line, this method returns 0.0.
     *
     * @param x1 the X coordinate of the start point of the specified line
     * @param y1 the Y coordinate of the start point of the specified line
     * @param x2 the X coordinate of the end point of the specified line
     * @param y2 the Y coordinate of the end point of the specified line
     * @param px the X coordinate of the specified point being
     *           measured against the specified line
     * @param py the Y coordinate of the specified point being
     *           measured against the specified line
     * @return a double value that is the distance from the specified
     *           point to the specified line.
     * @see #ptSegDist(double, double, double, double, double, double)
     */
    public static float ptLineDist(float x1, float y1,
                    float x2, float y2,
                    float px, float py)
    {
        return (float) Math.sqrt(ptLineDistSq(x1, y1, x2, y2, px, py));
    }

    /**
     * Returns the square of the distance from a point to this line.
     * The distance measured is the distance between the specified
     * point and the closest point on the infinitely-extended line
     * defined by this <code>Line2D</code>.  If the specified point
     * intersects the line, this method returns 0.0.
     *
     * @param px the X coordinate of the specified point being
     *           measured against this line
     * @param py the Y coordinate of the specified point being
     *           measured against this line
     * @return a double value that is the square of the distance from a
     *          specified point to the current line.
     * @see #ptSegDistSq(double, double)
     */
    public float ptLineDistSq(float px, float py) {
        return ptLineDistSq(x1, y1, x2, y2, px, py);
    }

    /**
     * Returns the square of the distance from a specified
     * <code>Point2D</code> to this line.
     * The distance measured is the distance between the specified
     * point and the closest point on the infinitely-extended line
     * defined by this <code>Line2D</code>.  If the specified point
     * intersects the line, this method returns 0.0.
     * @param pt the specified <code>Point2D</code> being measured
     *           against this line
     * @return a double value that is the square of the distance from a
     *          specified <code>Point2D</code> to the current
     *          line.
     * @see #ptSegDistSq(Point2D)
     */
    public float ptLineDistSq(Point2D pt) {
        return ptLineDistSq(x1, y1, x2, y2, pt.x, pt.y);
    }

    /**
     * Returns the distance from a point to this line.
     * The distance measured is the distance between the specified
     * point and the closest point on the infinitely-extended line
     * defined by this <code>Line2D</code>.  If the specified point
     * intersects the line, this method returns 0.0.
     *
     * @param px the X coordinate of the specified point being
     *           measured against this line
     * @param py the Y coordinate of the specified point being
     *           measured against this line
     * @return a double value that is the distance from a specified point
     *          to the current line.
     * @see #ptSegDist(double, double)
     */
    public float ptLineDist(float px, float py) {
        return ptLineDist(x1, y1, x2, y2, px, py);
    }

    /**
     * Returns the distance from a <code>Point2D</code> to this line.
     * The distance measured is the distance between the specified
     * point and the closest point on the infinitely-extended line
     * defined by this <code>Line2D</code>.  If the specified point
     * intersects the line, this method returns 0.0.
     * @param pt the specified <code>Point2D</code> being measured
     * @return a double value that is the distance from a specified
     *          <code>Point2D</code> to the current line.
     * @see #ptSegDist(Point2D)
     */
    public float ptLineDist(Point2D pt) {
        return ptLineDist(x1, y1, x2, y2, pt.x, pt.y);
    }

    /**
     * Returns an iteration object that defines the boundary of this
     * <code>Line2D</code>.
     * The iterator for this class is not multi-threaded safe,
     * which means that this <code>Line2D</code> class does not
     * guarantee that modifications to the geometry of this
     * <code>Line2D</code> object do not affect any iterations of that
     * geometry that are already in process.
     * @param tx the specified {@link BaseTransform}
     * @return a {@link PathIterator} that defines the boundary of this
     *      <code>Line2D</code>.
     */
    public PathIterator getPathIterator(BaseTransform tx) {
        return new LineIterator(this, tx);
    }

    /**
     * Returns an iteration object that defines the boundary of this
     * flattened <code>Line2D</code>.
     * The iterator for this class is not multi-threaded safe,
     * which means that this <code>Line2D</code> class does not
     * guarantee that modifications to the geometry of this
     * <code>Line2D</code> object do not affect any iterations of that
     * geometry that are already in process.
     * @param tx the specified <code>BaseTransform</code>
     * @param flatness the maximum amount that the control points for a
     *      given curve can vary from colinear before a subdivided
     *      curve is replaced by a straight line connecting the
     *      end points.  Since a <code>Line2D</code> object is
     *          always flat, this parameter is ignored.
     * @return a <code>PathIterator</code> that defines the boundary of the
     *          flattened <code>Line2D</code>
     */
    public PathIterator getPathIterator(BaseTransform tx, float flatness) {
        return new LineIterator(this, tx);
    }

    @Override
    public Line2D copy() {
        return new Line2D(x1, y1, x2, y2);
    }

    @Override
    public int hashCode() {
        int bits = java.lang.Float.floatToIntBits(x1);
        bits += java.lang.Float.floatToIntBits(y1) * 37;
        bits += java.lang.Float.floatToIntBits(x2) * 43;
        bits += java.lang.Float.floatToIntBits(y2) * 47;
        return bits;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Line2D) {
            Line2D line = (Line2D) obj;
            return ((x1 == line.x1) && (y1 == line.y1) &&
                    (x2 == line.x2) && (y2 == line.y2));
        }
        return false;
    }
}
