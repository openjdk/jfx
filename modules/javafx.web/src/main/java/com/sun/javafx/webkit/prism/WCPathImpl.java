/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit.prism;

import com.sun.javafx.geom.Arc2D;
import com.sun.javafx.geom.Ellipse2D;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.RoundRectangle2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.webkit.graphics.WCPath;
import com.sun.webkit.graphics.WCPathIterator;
import com.sun.webkit.graphics.WCRectangle;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;
import com.sun.prism.BasicStroke;
import java.util.Arrays;

final class WCPathImpl extends WCPath<Path2D> {
    private final Path2D path;
    private boolean hasCP = false;

    private final static PlatformLogger log =
            PlatformLogger.getLogger(WCPathImpl.class.getName());

    WCPathImpl() {
        if (log.isLoggable(Level.FINE)) {
            log.fine("Create empty WCPathImpl({0})", getID());
        }
        path = new Path2D();
    }

    WCPathImpl(WCPathImpl wcp) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("Create WCPathImpl({0}) from WCPathImpl({1})",
                    new Object[] { getID(), wcp.getID()});
        }
        path = new Path2D(wcp.path);
        hasCP = wcp.hasCP;
    }

    @Override
    public void addRect(double x, double y, double w, double h) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("WCPathImpl({0}).addRect({1},{2},{3},{4})",
                        new Object[] {getID(), x, y, w, h});
        }
        hasCP = true;
        path.append(new RoundRectangle2D(
                (float)x, (float)y, (float)w, (int)h, 0.0f, 0.0f), false);
    }

    @Override
    public void addEllipse(double x, double y, double w, double h)
    {
        if (log.isLoggable(Level.FINE)) {
            log.fine("WCPathImpl({0}).addEllipse({1},{2},{3},{4})",
                    new Object[] {getID(), x, y, w, h});
        }
        hasCP = true;
        path.append(new Ellipse2D((float)x, (float)y, (float)w, (float)h), false);
    }

    @Override
    public void addArcTo(double x1, double y1, double x2, double y2, double r)
    {
        if (log.isLoggable(Level.FINE)) {
            log.fine("WCPathImpl({0}).addArcTo({1},{2},{3},{4})",
                    new Object[] {getID(), x1, y1, x2, y2});
        }

        Arc2D arc = new Arc2D();
        arc.setArcByTangent(
                path.getCurrentPoint(),
                new Point2D((float) x1, (float) y1),
                new Point2D((float) x2, (float) y2),
                (float) r);

        hasCP = true;
        path.append(arc, true);
    }

    @Override
    public void addArc(double x, double y, double r, double sa,
                       double ea, boolean aclockwise)
    {
        // Use single precision float as native
        final float TWO_PI = 2.0f * (float) Math.PI;
        float startAngle = (float) sa;
        float endAngle = (float) ea;

        if (log.isLoggable(Level.FINE)) {
            log.fine("WCPathImpl({0}).addArc(x={1},y={2},r={3},sa=|{4}|,ea=|{5}|,aclock={6})",
                    new Object[] {getID(), x, y, r, startAngle, endAngle, aclockwise});
        }

        hasCP = true;

        float newEndAngle = endAngle;
        // http://www.whatwg.org/specs/web-apps/current-work/multipage/the-canvas-element.html#dom-context-2d-arc
        // If the anticlockwise argument is false and endAngle-startAngle is equal
        // to or greater than 2pi, or,
        // if the anticlockwise argument is true and startAngle-endAngle is equal to
        // or greater than 2pi,
        // then the arc is the whole circumference of this ellipse, and the point at
        // startAngle along this circle's circumference, measured in radians clockwise
        // from the ellipse's semi-major axis, acts as both the start point and the
        // end point.

        // below condition is already handled in normalizeAngles(), CanvasPath.cpp.
        // if (!anticlockwise && end_angle - start_angle >= twoPiFloat) {
        //   new_end_angle = start_angle + twoPiFloat;
        // } else if (anticlockwise && start_angle - end_angle >= twoPiFloat) {
        //   new_end_angle = start_angle - twoPiFloat;
        // } else

        // Otherwise, the arc is the path along the circumference of this ellipse
        // from the start point to the end point, going anti-clockwise if the
        // anticlockwise argument is true, and clockwise otherwise.
        // Since the points are on the ellipse, as opposed to being simply angles
        // from zero, the arc can never cover an angle greater than 2pi radians.
        //
        // NOTE: When startAngle = 0, endAngle = 2Pi and anticlockwise = true, the
        // spec does not indicate clearly.
        // We draw the entire circle, because some web sites use arc(x, y, r, 0,
        // 2*Math.PI, true) to draw circle.
        // We preserve backward-compatibility.
        if (!aclockwise && startAngle > endAngle) {
            newEndAngle = startAngle + (TWO_PI - ((startAngle - endAngle) % TWO_PI));
        } else if (aclockwise && startAngle < endAngle) {
            newEndAngle = startAngle - (TWO_PI - ((endAngle - startAngle) % TWO_PI));
        }

        path.append(new Arc2D((float) (x - r), (float) (y - r),
                              (float) (2 * r), (float) (2 * r),
                              (float) Math.toDegrees(-startAngle),
                              (float) Math.toDegrees(startAngle - newEndAngle), Arc2D.OPEN), true);
    }

    @Override
    public boolean contains(int rule, double x, double y) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("WCPathImpl({0}).contains({1},{2},{3})",
                    new Object[] {getID(), rule, x, y});
        }
        final int savedRule = path.getWindingRule();
        path.setWindingRule(rule);
        final boolean res = path.contains((float)x, (float)y);
        path.setWindingRule(savedRule);

        return res;
    }

    @Override
    public WCRectangle getBounds() {
        RectBounds b = path.getBounds();
        return new WCRectangle(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
    }

    @Override
    public void clear() {
        if (log.isLoggable(Level.FINE)) {
            log.fine("WCPathImpl({0}).clear()", getID());
        }
        hasCP = false;
        path.reset();
    }

    @Override
    public void moveTo(double x, double y) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("WCPathImpl({0}).moveTo({1},{2})",
                    new Object[] {getID(), x, y});
        }
        hasCP = true;
        path.moveTo((float)x, (float)y);
    }

    @Override
    public void addLineTo(double x, double y) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("WCPathImpl({0}).addLineTo({1},{2})",
                    new Object[] {getID(), x, y});
        }
        hasCP = true;
        path.lineTo((float)x, (float)y);
    }

    @Override
    public void addQuadCurveTo(double x0, double y0, double x1, double y1) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("WCPathImpl({0}).addQuadCurveTo({1},{2},{3},{4})",
                    new Object[] {getID(), x0, y0, x1, y1});
        }
        hasCP = true;
        path.quadTo((float)x0, (float)y0, (float)x1, (float)y1);
    }

    @Override
    public void addBezierCurveTo(double x0, double y0, double x1, double y1,
                                 double x2, double y2) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("WCPathImpl({0}).addBezierCurveTo({1},{2},{3},{4},{5},{6})",
                    new Object[] {getID(), x0, y0, x1, y1, x2, y2});
        }
        hasCP = true;
        path.curveTo((float)x0, (float)y0, (float)x1, (float)y1,
                     (float)x2, (float)y2);
    }

    @Override
    public void addPath(WCPath p) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("WCPathImpl({0}).addPath({1})",
                    new Object[] {getID(), p.getID()});
        }
        hasCP = hasCP || ((WCPathImpl)p).hasCP;
        path.append(((WCPathImpl)p).path, false);
    }

    @Override
    public void closeSubpath() {
        if (log.isLoggable(Level.FINE)) {
            log.fine("WCPathImpl({0}).closeSubpath()", getID());
        }
        path.closePath();
    }

    @Override
    public boolean isEmpty() {
        return !hasCP;
    }

    @Override
    public int getWindingRule() {
        return 1 - this.path.getWindingRule(); // convert prism to webkit
    }

    @Override
    public void setWindingRule(int rule) {
        this.path.setWindingRule(1 - rule); // convert webkit to prism
    }

    @Override
    public Path2D getPlatformPath() {
        if (log.isLoggable(Level.FINE)) {
            log.fine("WCPathImpl({0}).getPath() BEGIN=====", getID());
            PathIterator pi = path.getPathIterator(null);
            float [] coords = new float[6];
            while(!pi.isDone()) {
                switch(pi.currentSegment(coords)) {
                    case PathIterator.SEG_MOVETO:
                        log.fine("SEG_MOVETO ({0},{1})",
                                new Object[] {coords[0], coords[1]});
                        break;
                    case PathIterator.SEG_LINETO:
                        log.fine("SEG_LINETO ({0},{1})",
                                new Object[] {coords[0], coords[1]});
                        break;
                    case PathIterator.SEG_QUADTO:
                        log.fine("SEG_QUADTO ({0},{1},{2},{3})",
                                new Object[] {coords[0], coords[1], coords[2], coords[3]});
                        break;
                    case PathIterator.SEG_CUBICTO:
                        log.fine("SEG_CUBICTO ({0},{1},{2},{3},{4},{5})",
                                new Object[] {coords[0], coords[1], coords[2], coords[3],
                                              coords[4], coords[5]});
                        break;
                    case PathIterator.SEG_CLOSE:
                        log.fine("SEG_CLOSE");
                        break;
                }
                pi.next();
            }
            log.fine("========getPath() END=====");
        }
        return path;
    }

    @Override
    public WCPathIterator getPathIterator() {
        final PathIterator pi = path.getPathIterator(null);
        return new WCPathIterator() {
            @Override public int getWindingRule() {
                return pi.getWindingRule();
            }

            @Override public boolean isDone() {
                return pi.isDone();
            }

            @Override public void next() {
                pi.next();
            }

            @Override public int currentSegment(double[] coords) {
                float [] _coords = new float[6];
                int segmentType = pi.currentSegment(_coords);
                for (int i = 0; i < coords.length; i++) {
                    coords[i] = _coords[i];
                }
                return segmentType;
            }
        };
    }

    @Override
    public void translate(double x, double y)
    {
        if (log.isLoggable(Level.FINE)) {
            log.fine("WCPathImpl({0}).translate({1}, {2})",
                    new Object[] {getID(), x, y});
        }
        path.transform(BaseTransform.getTranslateInstance(x, y));
    }

    @Override
    public void transform(double mxx, double myx,
                          double mxy, double myy,
                          double mxt, double myt)
    {
        if (log.isLoggable(Level.FINE)) {
            log.fine("WCPathImpl({0}).transform({1},{2},{3},{4},{5},{6})",
                    new Object[] {getID(), mxx, myx, mxy, myy, mxt, myt});
        }
        path.transform(BaseTransform.getInstance(mxx, myx, mxy, myy, mxt, myt));
    }

    @Override
    public boolean strokeContains(double x, double y,
                                  double thickness, double miterLimit,
                                  int cap, int join, double dashOffset,
                                  double[] dashArray) {

        BasicStroke stroke = new BasicStroke(
            (float) thickness, cap, join, (float) miterLimit);

        if (dashArray.length > 0) {
            stroke.set(dashArray, (float) dashOffset);
        }

        boolean result = stroke
            .createCenteredStrokedShape(path)
            .contains((float) x, (float) y);

        if (log.isLoggable(Level.FINE)) {
            log.fine(
                "WCPathImpl({0}).strokeContains({1},{2},{3},{4},{5},{6},{7},{8}) = {9}",
                new Object[]{getID(), x, y, thickness, miterLimit, cap, join,
                             dashOffset, Arrays.toString(dashArray), result});
        }

        return result;
    }
}
