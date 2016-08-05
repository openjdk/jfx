/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.webkit.graphics.WCPath;
import com.sun.webkit.graphics.WCPathIterator;
import com.sun.webkit.graphics.WCRectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

final class WCPathImpl extends WCPath<Path2D> {
    private final Path2D path;
    private boolean hasCP = false;

    private final static Logger log =
        Logger.getLogger(WCPathImpl.class.getName());

    WCPathImpl() {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Create empty WCPathImpl({0})", getID());
        }
        path = new Path2D();
    }

    WCPathImpl(WCPathImpl wcp) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Create WCPathImpl({0}) from WCPathImpl({1})",
                    new Object[] { getID(), wcp.getID()});
        }
        path = new Path2D(wcp.path);
        hasCP = wcp.hasCP;
    }

    public void addRect(double x, double y, double w, double h) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "WCPathImpl({0}).addRect({1},{2},{3},{4})",
                        new Object[] {getID(), x, y, w, h});
        }
        hasCP = true;
        path.append(new RoundRectangle2D(
                (float)x, (float)y, (float)w, (int)h, 0.0f, 0.0f), false);
    }

    public void addEllipse(double x, double y, double w, double h)
    {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "WCPathImpl({0}).addEllipse({1},{2},{3},{4})",
                    new Object[] {getID(), x, y, w, h});
        }
        hasCP = true;
        path.append(new Ellipse2D((float)x, (float)y, (float)w, (float)h), false);
    }

    public void addArcTo(double x1, double y1, double x2, double y2, double r)
    {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "WCPathImpl({0}).addArcTo({1},{2},{3},{4})",
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

    public void addArc(double x, double y, double r, double startAngle,
                       double endAngle, boolean aclockwise)
    {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "WCPathImpl({0}).addArc({1},{2},{3},{4},{5},{6})",
                    new Object[] {getID(), x, y, r, startAngle, endAngle, aclockwise});
        }
        hasCP = true;


        double eps = 0.001;

        if (!aclockwise) {
            if (endAngle < 0.0) {
                if (endAngle < -2.0*Math.PI - eps) {
                    int eMult = (int) (-endAngle / (2*Math.PI)) ;
                    endAngle += eMult*2.0*Math.PI;
                }

                endAngle += 2.0*Math.PI;
            } else {
                if (endAngle > 2.0*Math.PI + eps) {
                    int eMult = (int) (endAngle / (2*Math.PI));
                    endAngle -= eMult*2.0*Math.PI;
                }
            }

            if (startAngle < 0.0) {
                if (startAngle < -2.0*Math.PI - eps) {
                    int sMult = (int) (-startAngle / (2*Math.PI));
                    startAngle += sMult*2.0*Math.PI;
                }

                startAngle += 2.0*Math.PI;
            } else {
                if (startAngle > 2.0*Math.PI + eps) {
                    int sMult = (int) (startAngle / (2*Math.PI));
                    startAngle -= sMult*2.0*Math.PI;
                }
            }

            double d = startAngle - endAngle;

            if (startAngle < endAngle) {
                d = Math.abs(d);
            }

            endAngle = (float) (2.0 * Math.PI - endAngle);

            Shape arc = new Arc2D((float)(x - r), (float)(y - r),
                                  (float)(2*r), (float)(2*r),
                (float) ((endAngle * 180.0) / Math.PI),
                (float) ((d * 180.0) / Math.PI),Arc2D.OPEN);

            PathIterator pi = arc.getPathIterator(null);
            List<Integer> segStack = new ArrayList<Integer>();
            List<Float> valStack = new ArrayList<Float>();
            float [] coords = new float[6];
            while(!pi.isDone()) {
                switch(pi.currentSegment(coords)) {
                    case PathIterator.SEG_MOVETO:
                        valStack.add(coords[1]);
                        valStack.add(coords[0]);
                        break;
                    case PathIterator.SEG_QUADTO:
                        throw new RuntimeException("Unexpected segment: " +
                                                   "SEG_QUADTO");
                    case PathIterator.SEG_CUBICTO:
                        valStack.add(coords[1]);
                        valStack.add(coords[0]);
                        valStack.add(coords[3]);
                        valStack.add(coords[2]);
                        valStack.add(coords[5]);
                        valStack.add(coords[4]);
                        segStack.add(PathIterator.SEG_CUBICTO);
                        break;
                    case PathIterator.SEG_CLOSE:
                        throw new RuntimeException("Unexpected segment: " +
                                                   "SEG_CLOSE");
                }

                pi.next();
            }

            segStack.add(PathIterator.SEG_MOVETO);

            Path2D invArc = new Path2D();
            int segIndex = segStack.size();
            int valIndex = valStack.size();
            while (segIndex > 0) {
                switch(segStack.get(--segIndex)) {
                    case PathIterator.SEG_MOVETO:
                        invArc.moveTo(valStack.get(--valIndex), valStack.get(--valIndex));
                        break;
                    case PathIterator.SEG_LINETO:
                        invArc.lineTo(valStack.get(--valIndex), valStack.get(--valIndex));
                        break;
                    case PathIterator.SEG_QUADTO:
                        invArc.quadTo(valStack.get(--valIndex), valStack.get(--valIndex),
                                      valStack.get(--valIndex), valStack.get(--valIndex));
                        break;
                    case PathIterator.SEG_CUBICTO:
                        invArc.curveTo(valStack.get(--valIndex), valStack.get(--valIndex),
                                       valStack.get(--valIndex), valStack.get(--valIndex),
                                       valStack.get(--valIndex), valStack.get(--valIndex));
                        break;
                }
            }
            path.append(invArc, true);
        } else {

            if (endAngle < 0.0) {
                if (endAngle < -2.0*Math.PI - eps) {
                    int eMult = (int) (-endAngle / (2*Math.PI));
                    endAngle += eMult*2.0*Math.PI;
                }

                endAngle += 2.0*Math.PI;
            } else {
                if (endAngle > 2.0*Math.PI + eps) {
                    int eMult = (int) (endAngle / (2*Math.PI));
                    endAngle -= eMult*2.0*Math.PI;
                }
            }

            if (startAngle < 0.0) {
                if (startAngle < -2.0*Math.PI - eps) {
                    int sMult = (int) (-startAngle / (2*Math.PI));
                    startAngle += sMult*2.0*Math.PI;
                }

                startAngle += 2.0*Math.PI;
            } else {
                if (startAngle > 2.0*Math.PI + eps) {
                    int sMult = (int) (startAngle / (2*Math.PI));
                    startAngle -= sMult*2.0*Math.PI;
                }
            }

            double d = startAngle - endAngle;

            if (startAngle < endAngle) {
                d += 2*Math.PI;
                if (d < eps) {
                    d += 2*Math.PI;
                }
            }

            if (Math.abs(startAngle) > eps) {
                startAngle = (float) (2.0 * Math.PI - startAngle);
            }


            path.append(new Arc2D((float)(x - r), (float)(y - r),
                                  (float)(2*r), (float)(2*r),
                                  (float)((startAngle*180.0f)/Math.PI),
                                  (float)((d*180.0f)/Math.PI),
                                  Arc2D.OPEN), true);
        }
    }

    public boolean contains(int rule, double x, double y) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "WCPathImpl({0}).contains({1},{2},{3})",
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

    public void clear() {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "WCPathImpl({0}).clear()", getID());
        }
        hasCP = false;
        path.reset();
    }

    public void moveTo(double x, double y) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "WCPathImpl({0}).moveTo({1},{2})",
                    new Object[] {getID(), x, y});
        }
        hasCP = true;
        path.moveTo((float)x, (float)y);
    }

    public void addLineTo(double x, double y) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "WCPathImpl({0}).addLineTo({1},{2})",
                    new Object[] {getID(), x, y});
        }
        hasCP = true;
        path.lineTo((float)x, (float)y);
    }

    public void addQuadCurveTo(double x0, double y0, double x1, double y1) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "WCPathImpl({0}).addQuadCurveTo({1},{2},{3},{4})",
                    new Object[] {getID(), x0, y0, x1, y1});
        }
        hasCP = true;
        path.quadTo((float)x0, (float)y0, (float)x1, (float)y1);
    }

    public void addBezierCurveTo(double x0, double y0, double x1, double y1,
                                 double x2, double y2) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "WCPathImpl({0}).addBezierCurveTo({1},{2},{3},{4},{5},{6})",
                    new Object[] {getID(), x0, y0, x1, y1, x2, y2});
        }
        hasCP = true;
        path.curveTo((float)x0, (float)y0, (float)x1, (float)y1,
                     (float)x2, (float)y2);
    }

    public void addPath(WCPath p) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "WCPathImpl({0}).addPath({1})",
                    new Object[] {getID(), p.getID()});
        }
        hasCP = hasCP || ((WCPathImpl)p).hasCP;
        path.append(((WCPathImpl)p).path, false);
    }

    public void closeSubpath() {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "WCPathImpl({0}).closeSubpath()", getID());
        }
        path.closePath();
    }

    public boolean hasCurrentPoint() {
        return hasCP;
    }

    public boolean isEmpty() {
        PathIterator pi = path.getPathIterator(null);
        float [] coords = new float[6];
        while(!pi.isDone()) {
            switch(pi.currentSegment(coords)) {
                case PathIterator.SEG_LINETO:
                case PathIterator.SEG_QUADTO:
                case PathIterator.SEG_CUBICTO:
                    return false;
            }
            pi.next();
        }
        return true;
    }

    public int getWindingRule() {
        return 1 - this.path.getWindingRule(); // convert prism to webkit
    }

    public void setWindingRule(int rule) {
        this.path.setWindingRule(1 - rule); // convert webkit to prism
    }

    public Path2D getPlatformPath() {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "WCPathImpl({0}).getPath() BEGIN=====", getID());
            PathIterator pi = path.getPathIterator(null);
            float [] coords = new float[6];
            while(!pi.isDone()) {
                switch(pi.currentSegment(coords)) {
                    case PathIterator.SEG_MOVETO:
                        log.log(Level.FINE, "SEG_MOVETO ({0},{1})",
                                new Object[] {coords[0], coords[1]});
                        break;
                    case PathIterator.SEG_LINETO:
                        log.log(Level.FINE, "SEG_LINETO ({0},{1})",
                                new Object[] {coords[0], coords[1]});
                        break;
                    case PathIterator.SEG_QUADTO:
                        log.log(Level.FINE, "SEG_QUADTO ({0},{1},{2},{3})",
                                new Object[] {coords[0], coords[1], coords[2], coords[3]});
                        break;
                    case PathIterator.SEG_CUBICTO:
                        log.log(Level.FINE, "SEG_CUBICTO ({0},{1},{2},{3},{4},{5})",
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

    public void translate(double x, double y)
    {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "WCPathImpl({0}).translate({1}, {2})",
                    new Object[] {getID(), x, y});
        }
        path.transform(BaseTransform.getTranslateInstance(x, y));
    }

    public void transform(double mxx, double myx,
                          double mxy, double myy,
                          double mxt, double myt)
    {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "WCPathImpl({0}).transform({1},{2},{3},{4},{5},{6})",
                    new Object[] {getID(), mxx, myx, mxy, myy, mxt, myt});
        }
        path.transform(BaseTransform.getInstance(mxx, myx, mxy, myy, mxt, myt));
    }
}
