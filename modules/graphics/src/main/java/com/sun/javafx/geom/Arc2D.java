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
 * <a name="inscribes">
 * The arc is a partial section of a full ellipse which
 * inscribes the framing rectangle of its parent {@link RectangularShape}.
 * </a>
 * <a name="angles">
 * The angles are specified relative to the non-square
 * framing rectangle such that 45 degrees always falls on the line from
 * the center of the ellipse to the upper right corner of the framing
 * rectangle.
 * As a result, if the framing rectangle is noticeably longer along one
 * axis than the other, the angles to the start and end of the arc segment
 * will be skewed farther along the longer axis of the frame.
 * </a>
 *
 * @version 10 Feb 1997
 */
public class Arc2D extends RectangularShape {

    /**
     * The closure type for an open arc with no path segments
     * connecting the two ends of the arc segment.
     */
    public final static int OPEN = 0;

    /**
     * The closure type for an arc closed by drawing a straight
     * line segment from the start of the arc segment to the end of the
     * arc segment.
     */
    public final static int CHORD = 1;

    /**
     * The closure type for an arc closed by drawing straight line
     * segments from the start of the arc segment to the center
     * of the full ellipse and from that point to the end of the arc segment.
     */
    public final static int PIE = 2;

    private int type;

    /**
     * The X coordinate of the upper-left corner of the framing
     * rectangle of the arc.
     * @serial
     */
    public float x;

    /**
     * The Y coordinate of the upper-left corner of the framing
     * rectangle of the arc.
     * @serial
     */
    public float y;

    /**
     * The overall width of the full ellipse of which this arc is
     * a partial section (not considering the
     * angular extents).
     * @serial
     */
    public float width;

    /**
     * The overall height of the full ellipse of which this arc is
     * a partial section (not considering the
     * angular extents).
     * @serial
     */
    public float height;

    /**
     * The starting angle of the arc in degrees.
     * @serial
     */
    public float start;

    /**
     * The angular extent of the arc in degrees.
     * @serial
     */
    public float extent;

    /**
     * Constructs a new OPEN arc, initialized to location (0, 0),
     * size (0, 0), angular extents (start = 0, extent = 0).
     */
    public Arc2D() {
        this(OPEN);
    }

    /**
     * Constructs a new arc, initialized to location (0, 0),
     * size (0, 0), angular extents (start = 0, extent = 0), and
     * the specified closure type.
     *
     * @param type The closure type for the arc:
     * {@link #OPEN}, {@link #CHORD}, or {@link #PIE}.
     */
    public Arc2D(int type) {
        setArcType(type);
    }

    /**
     * Constructs a new arc, initialized to the specified location,
     * size, angular extents, and closure type.
     *
     * @param x The X coordinate of the upper-left corner of
     *          the arc's framing rectangle.
     * @param y The Y coordinate of the upper-left corner of
     *          the arc's framing rectangle.
     * @param w The overall width of the full ellipse of which
     *          this arc is a partial section.
     * @param h The overall height of the full ellipse of which this
     *          arc is a partial section.
     * @param start The starting angle of the arc in degrees.
     * @param extent The angular extent of the arc in degrees.
     * @param type The closure type for the arc:
     * {@link #OPEN}, {@link #CHORD}, or {@link #PIE}.
     */
    public Arc2D(float x, float y, float w, float h,
             float start, float extent, int type) {
        this(type);
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
        this.start = start;
        this.extent = extent;
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
    public boolean isEmpty() {
        return (width <= 0f || height <= 0f);
    }

    /**
     * {@inheritDoc}
     */
    public void setArc(float x, float y, float w, float h,
               float angSt, float angExt, int closure) {
        this.setArcType(closure);
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
        this.start = angSt;
        this.extent = angExt;
    }

    /**
     * Returns the arc closure type of the arc: {@link #OPEN},
     * {@link #CHORD}, or {@link #PIE}.
     * @return One of the integer constant closure types defined
     * in this class.
     * @see #setArcType
     */
    public int getArcType() {
        return type;
    }

    /**
     * Returns the starting point of the arc.  This point is the
     * intersection of the ray from the center defined by the
     * starting angle and the elliptical boundary of the arc.
     *
     * @return A <CODE>Point2D</CODE> object representing the
     * x,y coordinates of the starting point of the arc.
     */
    public Point2D getStartPoint() {
        double angle = Math.toRadians(-start);
        double x = this.x + (Math.cos(angle) * 0.5 + 0.5) * width;
        double y = this.y + (Math.sin(angle) * 0.5 + 0.5) * height;
        return new Point2D((float)x, (float)y);
    }

    /**
     * Returns the ending point of the arc.  This point is the
     * intersection of the ray from the center defined by the
     * starting angle plus the angular extent of the arc and the
     * elliptical boundary of the arc.
     *
     * @return A <CODE>Point2D</CODE> object representing the
     * x,y coordinates  of the ending point of the arc.
     */
    public Point2D getEndPoint() {
        double angle = Math.toRadians(-start - extent);
        double x = this.x + (Math.cos(angle) * 0.5 + 0.5) * width;
        double y = this.y + (Math.sin(angle) * 0.5 + 0.5) * height;
        return new Point2D((float)x, (float)y);
    }

    /**
     * Sets the location, size, angular extents, and closure type of
     * this arc to the specified values.
     *
     * @param loc The <CODE>Point2D</CODE> representing the coordinates of
     * the upper-left corner of the arc.
     * @param size The <CODE>Dimension2D</CODE> representing the width
     * and height of the full ellipse of which this arc is
     * a partial section.
     * @param angSt The starting angle of the arc in degrees.
     * @param angExt The angular extent of the arc in degrees.
     * @param closure The closure type for the arc:
     * {@link #OPEN}, {@link #CHORD}, or {@link #PIE}.
     */
    public void setArc(Point2D loc, Dimension2D size,
               float angSt, float angExt, int closure) {
        setArc(loc.x, loc.y, size.width, size.height, angSt, angExt, closure);
    }

    /**
     * Sets this arc to be the same as the specified arc.
     *
     * @param a The <CODE>Arc2D</CODE> to use to set the arc's values.
     */
    public void setArc(Arc2D a) {
        setArc(a.x, a.y, a.width, a.height, a.start, a.extent, a.type);
    }

    /**
     * Sets the position, bounds, angular extents, and closure type of
     * this arc to the specified values. The arc is defined by a center
     * point and a radius rather than a framing rectangle for the full ellipse.
     *
     * @param x The X coordinate of the center of the arc.
     * @param y The Y coordinate of the center of the arc.
     * @param radius The radius of the arc.
     * @param angSt The starting angle of the arc in degrees.
     * @param angExt The angular extent of the arc in degrees.
     * @param closure The closure type for the arc:
     * {@link #OPEN}, {@link #CHORD}, or {@link #PIE}.
     */
    public void setArcByCenter(float x, float y, float radius,
                   float angSt, float angExt, int closure) {
        setArc(x - radius, y - radius, radius * 2f, radius * 2f,
               angSt, angExt, closure);
    }

    /**
     * Sets the position, bounds, and angular extents of this arc to the
     * specified value. The starting angle of the arc is tangent to the
     * line specified by points (p1, p2), the ending angle is tangent to
     * the line specified by points (p2, p3), and the arc has the
     * specified radius.
     *
     * @param p1 The first point that defines the arc. The starting
     * angle of the arc is tangent to the line specified by points (p1, p2).
     * @param p2 The second point that defines the arc. The starting
     * angle of the arc is tangent to the line specified by points (p1, p2).
     * The ending angle of the arc is tangent to the line specified by
     * points (p2, p3).
     * @param p3 The third point that defines the arc. The ending angle
     * of the arc is tangent to the line specified by points (p2, p3).
     * @param radius The radius of the arc.
     */
    public void setArcByTangent(Point2D p1, Point2D p2, Point2D p3, float radius) {
        double ang1 = Math.atan2(p1.y - p2.y,
                     p1.x - p2.x);
        double ang2 = Math.atan2(p3.y - p2.y,
                     p3.x - p2.x);
        double diff = ang2 - ang1;
        if (diff > Math.PI) {
            ang2 -= Math.PI * 2.0;
        } else if (diff < -Math.PI) {
            ang2 += Math.PI * 2.0;
        }
        double bisect = (ang1 + ang2) / 2.0;
        double theta = Math.abs(ang2 - bisect);
        double dist = radius / Math.sin(theta);
        double x = p2.x + dist * Math.cos(bisect);
        double y = p2.y + dist * Math.sin(bisect);
        // REMIND: This needs some work...
        if (ang1 < ang2) {
            ang1 -= Math.PI / 2.0;
            ang2 += Math.PI / 2.0;
        } else {
            ang1 += Math.PI / 2.0;
            ang2 -= Math.PI / 2.0;
        }
        ang1 = Math.toDegrees(-ang1);
        ang2 = Math.toDegrees(-ang2);
        diff = ang2 - ang1;
        if (diff < 0) {
            diff += 360;
        } else {
            diff -= 360;
        }
        setArcByCenter((float)x, (float)y, (float)radius, (float)ang1, (float)diff, type);
    }

    /**
     * Sets the starting angle of this arc to the angle that the
     * specified point defines relative to the center of this arc.
     * The angular extent of the arc will remain the same.
     *
     * @param p The <CODE>Point2D</CODE> that defines the starting angle.
     * @see #start
     */
    public void setAngleStart(Point2D p) {
        // Bias the dx and dy by the height and width of the oval.
        double dx = this.height * (p.x - getCenterX());
        double dy = this.width * (p.y - getCenterY());
        start = (float)-Math.toDegrees(Math.atan2(dy, dx));
    }

    /**
     * Sets the starting angle and angular extent of this arc using two
     * sets of coordinates. The first set of coordinates is used to
     * determine the angle of the starting point relative to the arc's
     * center. The second set of coordinates is used to determine the
     * angle of the end point relative to the arc's center.
     * The arc will always be non-empty and extend counterclockwise
     * from the first point around to the second point.
     *
     * @param x1 The X coordinate of the arc's starting point.
     * @param y1 The Y coordinate of the arc's starting point.
     * @param x2 The X coordinate of the arc's ending point.
     * @param y2 The Y coordinate of the arc's ending point.
     */
    public void setAngles(float x1, float y1, float x2, float y2) {
        double x = getCenterX();
        double y = getCenterY();
        double w = this.width;
        double h = this.height;;
        // Note: reversing the Y equations negates the angle to adjust
        // for the upside down coordinate system.
        // Also we should bias atans by the height and width of the oval.
        double ang1 = Math.atan2(w * (y - y1), h * (x1 - x));
        double ang2 = Math.atan2(w * (y - y2), h * (x2 - x));
        ang2 -= ang1;
        if (ang2 <= 0.0) {
            ang2 += Math.PI * 2.0;
        }
        start = (float)Math.toDegrees(ang1);
        extent = (float)Math.toDegrees(ang2);
    }

    /**
     * Sets the starting angle and angular extent of this arc using
     * two points. The first point is used to determine the angle of
     * the starting point relative to the arc's center.
     * The second point is used to determine the angle of the end point
     * relative to the arc's center.
     * The arc will always be non-empty and extend counterclockwise
     * from the first point around to the second point.
     *
     * @param p1 The <CODE>Point2D</CODE> that defines the arc's
     * starting point.
     * @param p2 The <CODE>Point2D</CODE> that defines the arc's
     * ending point.
     */
    public void setAngles(Point2D p1, Point2D p2) {
        setAngles(p1.x, p1.y, p2.x, p2.y);
    }

    /**
     * Sets the closure type of this arc to the specified value:
     * <CODE>OPEN</CODE>, <CODE>CHORD</CODE>, or <CODE>PIE</CODE>.
     *
     * @param type The integer constant that represents the closure
     * type of this arc: {@link #OPEN}, {@link #CHORD}, or
     * {@link #PIE}.
     *
     * @throws IllegalArgumentException if <code>type</code> is not
     * 0, 1, or 2.+
     * @see #getArcType
     */
    public void setArcType(int type) {
        if (type < OPEN || type > PIE) {
            throw new IllegalArgumentException("invalid type for Arc: "+type);
        }
        this.type = type;
    }

    /**
     * {@inheritDoc}
     * Note that the arc
     * <a href="Arc2D.html#inscribes">partially inscribes</a>
     * the framing rectangle of this {@code RectangularShape}.
     */
    public void setFrame(float x, float y, float w, float h) {
        setArc(x, y, w, h, start, extent, type);
    }

    /**
     * Returns the high-precision framing rectangle of the arc.  The framing
     * rectangle contains only the part of this <code>Arc2D</code> that is
     * in between the starting and ending angles and contains the pie
     * wedge, if this <code>Arc2D</code> has a <code>PIE</code> closure type.
     * <p>
     * This method differs from the
     * {@link RectangularShape#getBounds() getBounds} in that the
     * <code>getBounds</code> method only returns the bounds of the
     * enclosing ellipse of this <code>Arc2D</code> without considering
     * the starting and ending angles of this <code>Arc2D</code>.
     *
     * @return the <CODE>RectBounds</CODE> that represents the arc's
     * framing rectangle.
     */
    public RectBounds getBounds() {
        if (isEmpty()) {
            return new RectBounds(x, y, x + width, y + height);
        }
        double x1, y1, x2, y2;
        if (getArcType() == PIE) {
            x1 = y1 = x2 = y2 = 0.0;
        } else {
            x1 = y1 = 1.0;
            x2 = y2 = -1.0;
        }
        double angle = 0.0;
        for (int i = 0; i < 6; i++) {
            if (i < 4) {
            // 0-3 are the four quadrants
            angle += 90.0;
            if (!containsAngle((float)angle)) {
                continue;
            }
            } else if (i == 4) {
            // 4 is start angle
            angle = start;
            } else {
            // 5 is end angle
            angle += extent;
            }
            double rads = Math.toRadians(-angle);
            double xe = Math.cos(rads);
            double ye = Math.sin(rads);
            x1 = Math.min(x1, xe);
            y1 = Math.min(y1, ye);
            x2 = Math.max(x2, xe);
            y2 = Math.max(y2, ye);
        }
        double w = this.width;
        double h = this.height;
        x2 = this.x + (x2 * 0.5 + 0.5) * w;
        y2 = this.y + (y2 * 0.5 + 0.5) * h;
        x1 = this.x + (x1 * 0.5 + 0.5) * w;
        y1 = this.y + (y1 * 0.5 + 0.5) * h;
        return new RectBounds((float)x1, (float)y1, (float)x2, (float)y2);
    }

    /*
     * Normalizes the specified angle into the range -180 to 180.
     */
    static float normalizeDegrees(double angle) {
        if (angle > 180.0) {
            if (angle <= (180.0 + 360.0)) {
                angle = angle - 360.0;
            } else {
                angle = Math.IEEEremainder(angle, 360.0);
                // IEEEremainder can return -180 here for some input values...
                if (angle == -180.0) {
                    angle = 180.0;
                }
            }
        } else if (angle <= -180.0) {
            if (angle > (-180.0 - 360.0)) {
                angle = angle + 360.0;
            } else {
                angle = Math.IEEEremainder(angle, 360.0);
                // IEEEremainder can return -180 here for some input values...
                if (angle == -180.0) {
                    angle = 180.0;
                }
            }
        }
        return (float)angle;
    }

    /**
     * Determines whether or not the specified angle is within the
     * angular extents of the arc.
     *
     * @param angle The angle to test.
     *
     * @return <CODE>true</CODE> if the arc contains the angle,
     * <CODE>false</CODE> if the arc doesn't contain the angle.
     */
    public boolean containsAngle(float angle) {
        double angExt = extent;
        boolean backwards = (angExt < 0.0);
        if (backwards) {
            angExt = -angExt;
        }
        if (angExt >= 360.0) {
            return true;
        }
        angle = normalizeDegrees(angle) - normalizeDegrees(start);
        if (backwards) {
            angle = -angle;
        }
        if (angle < 0.0) {
            angle += 360.0;
        }

        return (angle >= 0.0) && (angle < angExt);
    }

    /**
     * Determines whether or not the specified point is inside the boundary
     * of the arc.
     *
     * @param x The X coordinate of the point to test.
     * @param y The Y coordinate of the point to test.
     *
     * @return <CODE>true</CODE> if the point lies within the bound of
     * the arc, <CODE>false</CODE> if the point lies outside of the
     * arc's bounds.
     */
    public boolean contains(float x, float y) {
        // Normalize the coordinates compared to the ellipse
        // having a center at 0,0 and a radius of 0.5.
        double ellw = this.width;
        if (ellw <= 0.0) {
            return false;
        }
        double normx = (x - this.x) / ellw - 0.5;
        double ellh = this.height;
        if (ellh <= 0.0) {
            return false;
        }
        double normy = (y - this.y) / ellh - 0.5;
        double distSq = (normx * normx + normy * normy);
        if (distSq >= 0.25) {
            return false;
        }
        double angExt = Math.abs(extent);
        if (angExt >= 360.0) {
            return true;
        }
        boolean inarc = containsAngle((float)-Math.toDegrees(Math.atan2(normy,
                                     normx)));
        if (type == PIE) {
            return inarc;
        }
        // CHORD and OPEN behave the same way
        if (inarc) {
            if (angExt >= 180.0) {
            return true;
            }
            // point must be outside the "pie triangle"
        } else {
            if (angExt <= 180.0) {
            return false;
            }
            // point must be inside the "pie triangle"
        }
        // The point is inside the pie triangle iff it is on the same
        // side of the line connecting the ends of the arc as the center.
        double angle = Math.toRadians(-start);
        double x1 = Math.cos(angle);
        double y1 = Math.sin(angle);
        angle += Math.toRadians(-extent);
        double x2 = Math.cos(angle);
        double y2 = Math.sin(angle);
        boolean inside = (Line2D.relativeCCW((float)x1, (float)y1, (float)x2, (float)y2, (float)(2*normx), (float)(2*normy)) *
                  Line2D.relativeCCW((float)x1, (float)y1, (float)x2, (float)y2, 0, 0) >= 0);
        return inarc ? !inside : inside;
    }

    /**
     * Determines whether or not the interior of the arc intersects
     * the interior of the specified rectangle.
     *
     * @param x The X coordinate of the rectangle's upper-left corner.
     * @param y The Y coordinate of the rectangle's upper-left corner.
     * @param w The width of the rectangle.
     * @param h The height of the rectangle.
     *
     * @return <CODE>true</CODE> if the arc intersects the rectangle,
     * <CODE>false</CODE> if the arc doesn't intersect the rectangle.
     */
    public boolean intersects(float x, float y, float w, float h) {
        float aw = this.width;
        float ah = this.height;

        if ( w <= 0 || h <= 0 || aw <= 0 || ah <= 0 ) {
            return false;
        }
        float ext = extent;
        if (ext == 0) {
            return false;
        }

        float ax  = this.x;
        float ay  = this.y;
        float axw = ax + aw;
        float ayh = ay + ah;
        float xw  = x + w;
        float yh  = y + h;

        // check bbox
        if (x >= axw || y >= ayh || xw <= ax || yh <= ay) {
            return false;
        }

        // extract necessary data
        float axc = getCenterX();
        float ayc = getCenterY();

        // inlined getStartPoint
        double sangle = Math.toRadians(-start);
        float sx = (float) (this.x + (Math.cos(sangle) * 0.5 + 0.5) * width);
        float sy = (float) (this.y + (Math.sin(sangle) * 0.5 + 0.5) * height);

        // inlined getEndPoint
        double eangle = Math.toRadians(-start - extent);
        float ex = (float) (this.x + (Math.cos(eangle) * 0.5 + 0.5) * width);
        float ey = (float) (this.y + (Math.sin(eangle) * 0.5 + 0.5) * height);

        /*
         * Try to catch rectangles that intersect arc in areas
         * outside of rectagle with left top corner coordinates
         * (min(center x, start point x, end point x),
         *  min(center y, start point y, end point y))
         * and rigth bottom corner coordinates
         * (max(center x, start point x, end point x),
         *  max(center y, start point y, end point y)).
         * So we'll check axis segments outside of rectangle above.
         */
        if (ayc >= y && ayc <= yh) { // 0 and 180
            if ((sx < xw && ex < xw && axc < xw &&
                 axw > x && containsAngle(0)) ||
                (sx > x && ex > x && axc > x &&
                 ax < xw && containsAngle(180))) {
            return true;
            }
        }
        if (axc >= x && axc <= xw) { // 90 and 270
            if ((sy > y && ey > y && ayc > y &&
                 ay < yh && containsAngle(90)) ||
                (sy < yh && ey < yh && ayc < yh &&
                 ayh > y && containsAngle(270))) {
            return true;
            }
        }

        /*
         * For PIE we should check intersection with pie slices;
         * also we should do the same for arcs with extent is greater
         * than 180, because we should cover case of rectangle, which
         * situated between center of arc and chord, but does not
         * intersect the chord.
         */
        if (type == PIE || Math.abs(ext) > 180) {
            // for PIE: try to find intersections with pie slices
            if (Shape.intersectsLine(x, y, w, h, axc, ayc, sx, sy) ||
                Shape.intersectsLine(x, y, w, h, axc, ayc, ex, ey))
            {
                return true;
            }
        } else {
            // for CHORD and OPEN: try to find intersections with chord
            if (Shape.intersectsLine(x, y, w, h, sx, sy, ex, ey)) {
                return true;
            }
        }

        // finally check the rectangle corners inside the arc
        if (contains(x, y) || contains(x + w, y) ||
            contains(x, y + h) || contains(x + w, y + h)) {
            return true;
        }

        return false;
    }

    /**
     * Determines whether or not the interior of the arc entirely contains
     * the specified rectangle.
     *
     * @param x The X coordinate of the rectangle's upper-left corner.
     * @param y The Y coordinate of the rectangle's upper-left corner.
     * @param w The width of the rectangle.
     * @param h The height of the rectangle.
     *
     * @return <CODE>true</CODE> if the arc contains the rectangle,
     * <CODE>false</CODE> if the arc doesn't contain the rectangle.
     */
    public boolean contains(float x, float y, float w, float h) {
        if (!(contains(x, y) &&
              contains(x + w, y) &&
              contains(x, y + h) &&
              contains(x + w, y + h))) {
            return false;
        }
        // If the shape is convex then we have done all the testing
        // we need.  Only PIE arcs can be concave and then only if
        // the angular extents are greater than 180 degrees.
        if (type != PIE || Math.abs(extent) <= 180.0) {
            return true;
        }
        // For a PIE shape we have an additional test for the case where
        // the angular extents are greater than 180 degrees and all four
        // rectangular corners are inside the shape but one of the
        // rectangle edges spans across the "missing wedge" of the arc.
        // We can test for this case by checking if the rectangle intersects
        // either of the pie angle segments.
        float halfW = getWidth() / 2f;
        float halfH = getHeight() / 2f;
        float xc = x + halfW;
        float yc = y + halfH;
        float angle = (float) Math.toRadians(-start);
        float xe = (float) (xc + halfW * Math.cos(angle));
        float ye = (float) (yc + halfH * Math.sin(angle));
        if (Shape.intersectsLine(x, y, w, h, xc, yc, xe, ye)) {
            return false;
        }
        angle += (float) Math.toRadians(-extent);
        xe = (float) (xc + halfW * Math.cos(angle));
        ye = (float) (yc + halfH * Math.sin(angle));
        return !Shape.intersectsLine(x, y, w, h, xc, yc, xe, ye);
    }

    /**
     * Returns an iteration object that defines the boundary of the
     * arc.
     * This iterator is multithread safe.
     * <code>Arc2D</code> guarantees that
     * modifications to the geometry of the arc
     * do not affect any iterations of that geometry that
     * are already in process.
     *
     * @param tx an optional <CODE>BaseTransform</CODE> to be applied
     * to the coordinates as they are returned in the iteration, or null
     * if the untransformed coordinates are desired.
     *
     * @return A <CODE>PathIterator</CODE> that defines the arc's boundary.
     */
    public PathIterator getPathIterator(BaseTransform tx) {
        return new ArcIterator(this, tx);
    }

    @Override
    public Arc2D copy() {
        return new Arc2D(x, y, width, height, start, extent, type);
    }

    /**
     * Returns the hashcode for this <code>Arc2D</code>.
     * @return the hashcode for this <code>Arc2D</code>.
     */
    @Override
    public int hashCode() {
        int bits = java.lang.Float.floatToIntBits(x);
        bits += java.lang.Float.floatToIntBits(y) * 37;
        bits += java.lang.Float.floatToIntBits(width) * 43;
        bits += java.lang.Float.floatToIntBits(height) * 47;
        bits += java.lang.Float.floatToIntBits(start) * 53;
        bits += java.lang.Float.floatToIntBits(extent) * 59;
        bits += getArcType() * 61;
        return bits;
    }

    /**
     * Determines whether or not the specified <code>Object</code> is
     * equal to this <code>Arc2D</code>.  The specified
     * <code>Object</code> is equal to this <code>Arc2D</code>
     * if it is an instance of <code>Arc2D</code> and if its
     * location, size, arc extents and type are the same as this
     * <code>Arc2D</code>.
     * @param obj  an <code>Object</code> to be compared with this
     *             <code>Arc2D</code>.
     * @return  <code>true</code> if <code>obj</code> is an instance
     *          of <code>Arc2D</code> and has the same values;
     *          <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this)  return true;
        if (obj instanceof Arc2D) {
            Arc2D a2d = (Arc2D) obj;
            return ((x == a2d.x) &&
                    (y == a2d.y) &&
                    (width == a2d.width) &&
                    (height == a2d.height) &&
                    (start == a2d.start) &&
                    (extent == a2d.extent) &&
                    (type == a2d.type));
        }
        return false;
    }
}
