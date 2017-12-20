/*
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism;

import com.sun.javafx.geom.Area;
import com.sun.javafx.geom.GeneralShapePair;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.RoundRectangle2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.ShapePair;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.impl.shape.ShapeUtil;

public final class BasicStroke {

    /** Constant value for end cap style. */
    public static final int CAP_BUTT = 0;
    /** Constant value for end cap style. */
    public static final int CAP_ROUND = 1;
    /** Constant value for end cap style. */
    public static final int CAP_SQUARE = 2;

    /** Constant value for join style. */
    public static final int JOIN_MITER = 0;
    /** Constant value for join style. */
    public static final int JOIN_ROUND = 1;
    /** Constant value for join style. */
    public static final int JOIN_BEVEL = 2;

    public static final int TYPE_CENTERED = 0;
    public static final int TYPE_INNER = 1;
    public static final int TYPE_OUTER = 2;

    float width;
    int type;
    int cap;
    int join;
    float miterLimit;
    float dash[];
    float dashPhase;

    public BasicStroke() {
        set(TYPE_CENTERED, 1.0f, CAP_SQUARE, JOIN_MITER, 10f);
    }

    public BasicStroke(float width, int cap, int join, float miterLimit) {
        set(TYPE_CENTERED, width, cap, join, miterLimit);
    }

    public BasicStroke(int type, float width,
                       int cap, int join, float miterLimit)
    {
        set(type, width, cap, join, miterLimit);
    }

    public BasicStroke(float width, int cap, int join, float miterLimit,
                       float[] dash, float dashPhase)
    {
        set(TYPE_CENTERED, width, cap, join, miterLimit);
        set(dash, dashPhase);
    }

    public BasicStroke(float width, int cap, int join, float miterLimit,
                       double[] dash, float dashPhase)
    {
        set(TYPE_CENTERED, width, cap, join, miterLimit);
        set(dash, dashPhase);
    }

    public BasicStroke(int type, float width, int cap, int join, float miterLimit,
                       float[] dash, float dashPhase)
    {
        set(type, width, cap, join, miterLimit);
        set(dash, dashPhase);
    }

    public BasicStroke(int type, float width, int cap, int join, float miterLimit,
                       double[] dash, float dashPhase)
    {
        set(type, width, cap, join, miterLimit);
        set(dash, dashPhase);
    }

    public void set(int type, float width,
                    int cap, int join, float miterLimit)
    {
        if (type != TYPE_CENTERED && type != TYPE_INNER && type != TYPE_OUTER) {
            throw new IllegalArgumentException("illegal type");
        }
        if (width < 0.0f) {
            throw new IllegalArgumentException("negative width");
        }
        if (cap != CAP_BUTT && cap != CAP_ROUND && cap != CAP_SQUARE) {
            throw new IllegalArgumentException("illegal end cap value");
        }
        if (join == JOIN_MITER) {
            if (miterLimit < 1.0f) {
                throw new IllegalArgumentException("miter limit < 1");
            }
        } else if (join != JOIN_ROUND && join != JOIN_BEVEL) {
            throw new IllegalArgumentException("illegal line join value");
        }
        this.type = type;
        this.width = width;
        this.cap = cap;
        this.join = join;
        this.miterLimit = miterLimit;
    }

    public void set(float dash[], float dashPhase) {
        if (dash != null) {
            boolean allzero = true;
            for (int i = 0; i < dash.length; i++) {
                float d = dash[i];
                if (d > 0.0) {
                    allzero = false;
                } else if (d < 0.0) {
                    throw new IllegalArgumentException("negative dash length");
                }
            }
            if (allzero) {
                throw new IllegalArgumentException("dash lengths all zero");
            }
        }
        this.dash = dash;
        this.dashPhase = dashPhase;
    }

    public void set(double dash[], float dashPhase) {
        if (dash != null) {
            float newdashes[] = new float[dash.length];
            boolean allzero = true;
            for (int i = 0; i < dash.length; i++) {
                float d = (float) dash[i];
                if (d > 0.0) {
                    allzero = false;
                } else if (d < 0.0) {
                    throw new IllegalArgumentException("negative dash length");
                }
                newdashes[i] = d;
            }
            if (allzero) {
                throw new IllegalArgumentException("dash lengths all zero");
            }
            this.dash = newdashes;
        } else {
            this.dash = null;
        }
        this.dashPhase = dashPhase;
    }

    /**
     * Returns the stroke type, one of {@code TYPE_CENTERED},
     * {@code TYPE_INNER}, or {@code TYPE_OUTER}.
     * @return the stroke type
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the line width.  Line width is represented in user space,
     * which is the default-coordinate space used by Java 2D.  See the
     * <code>Graphics2D</code> class comments for more information on
     * the user space coordinate system.
     * @return the line width of this <code>BasicStroke</code>.
     */
    public float getLineWidth() {
        return width;
    }

    /**
     * Returns the end cap style.
     * @return the end cap style of this <code>BasicStroke</code> as one
     * of the static <code>int</code> values that define possible end cap
     * styles.
     */
    public int getEndCap() {
        return cap;
    }

    /**
     * Returns the line join style.
     * @return the line join style of the <code>BasicStroke</code> as one
     * of the static <code>int</code> values that define possible line
     * join styles.
     */
    public int getLineJoin() {
        return join;
    }

    /**
     * Returns the limit of miter joins.
     * @return the limit of miter joins of the <code>BasicStroke</code>.
     */
    public float getMiterLimit() {
        return miterLimit;
    }

    /**
     * Returns true if this stroke object will apply dashing attributes
     * to the path.
     * @return whether the stroke has dashes
     */
    public boolean isDashed() {
        return (dash != null);
    }
    /**
     * Returns the array representing the lengths of the dash segments.
     * Alternate entries in the array represent the user space lengths
     * of the opaque and transparent segments of the dashes.
     * As the pen moves along the outline of the <code>Shape</code>
     * to be stroked, the user space
     * distance that the pen travels is accumulated.  The distance
     * value is used to index into the dash array.
     * The pen is opaque when its current cumulative distance maps
     * to an even element of the dash array and transparent otherwise.
     * @return the dash array.
     */
    public float[] getDashArray() {
        return dash;
    }

    /**
     * Returns the current dash phase.
     * The dash phase is a distance specified in user coordinates that
     * represents an offset into the dashing pattern. In other words, the dash
     * phase defines the point in the dashing pattern that will correspond to
     * the beginning of the stroke.
     * @return the dash phase as a <code>float</code> value.
     */
    public float getDashPhase() {
        return dashPhase;
    }

    public Shape createStrokedShape(Shape s) {
        Shape ret;
        if (s instanceof RoundRectangle2D) {
            ret = strokeRoundRectangle((RoundRectangle2D) s);
        } else {
            ret = null;
        }
        if (ret != null) {
            return ret;
        }

        ret = createCenteredStrokedShape(s);

        if (type == TYPE_INNER) {
            ret = makeIntersectedShape(ret, s);
        } else if (type == TYPE_OUTER) {
            ret = makeSubtractedShape(ret, s);
        }
        return ret;
    }

    private boolean isCW(final float dx1, final float dy1,
                         final float dx2, final float dy2)
    {
        return dx1 * dy2 <= dy1 * dx2;
    }

    private void computeOffset(final float lx, final float ly,
                               final float w, final float[] m, int off) {
        final float len = (float) Math.sqrt(lx * lx + ly * ly);
        if (len == 0) {
            m[off + 0] = m[off + 1] = 0;
        } else {
            m[off + 0] = (ly * w) / len;
            m[off + 1] = -(lx * w) / len;
        }
    }

    private void computeMiter(final float x0, final float y0,
                              final float x1, final float y1,
                              final float x0p, final float y0p,
                              final float x1p, final float y1p,
                              final float[] m, int off)
    {
        float x10 = x1 - x0;
        float y10 = y1 - y0;
        float x10p = x1p - x0p;
        float y10p = y1p - y0p;

        // if this is 0, the lines are parallel. If they go in the
        // same direction, there is no intersection so m[off] and
        // m[off+1] will contain infinity, so no miter will be drawn.
        // If they go in the same direction that means that the start of the
        // current segment and the end of the previous segment have the same
        // tangent, in which case this method won't even be involved in
        // miter drawing because it won't be called by drawMiter (because
        // (mx == omx && my == omy) will be true, and drawMiter will return
        // immediately).
        float den = x10*y10p - x10p*y10;
        float t = x10p*(y0-y0p) - y10p*(x0-x0p);
        t /= den;
        m[off++] = x0 + t*x10;
        m[off] = y0 + t*y10;
    }


    // taken from com.sun.javafx.geom.Shape.accumulateQuad (added the width)
    private void accumulateQuad(float bbox[], int off,
                               float v0, float vc, float v1, float w)
    {
        // Breaking this quad down into a polynomial:
        // eqn[0] = v0;
        // eqn[1] = vc + vc - v0 - v0;
        // eqn[2] = v0 - vc - vc + v1;
        // Deriving the polynomial:
        // eqn'[0] = 1*eqn[1] = 2*(vc-v0)
        // eqn'[1] = 2*eqn[2] = 2*((v1-vc)-(vc-v0))
        // Solving for zeroes on the derivative:
        // e1*t + e0 = 0
        // t = -e0/e1;
        // t = -2(vc-v0) / 2((v1-vc)-(vc-v0))
        // t = (v0-vc) / (v1-vc+v0-vc)
        float num = v0 - vc;
        float den = v1 - vc + num;
        if (den != 0f) {
            float t = num / den;
            if (t > 0 && t < 1) {
                float u = 1f - t;
                float v = v0 * u * u + 2 * vc * t * u + v1 * t * t;
                if (bbox[off] > v - w) bbox[off] = v - w;
                if (bbox[off+2] < v + w) bbox[off+2] = v + w;
            }
        }
    }

    // taken from com.sun.javafx.geom.Shape.accumulateCubic (added the width)
    private void accumulateCubic(float bbox[], int off, float t,
                                float v0, float vc0, float vc1, float v1, float w)
    {
        if (t > 0 && t < 1) {
            float u = 1f - t;
            float v =        v0 * u * u * u
                      + 3 * vc0 * t * u * u
                      + 3 * vc1 * t * t * u
                      +      v1 * t * t * t;
            if (bbox[off] > v - w) bbox[off] = v - w;
            if (bbox[off+2] < v + w) bbox[off+2] = v + w;
        }
    }

    // taken from com.sun.javafx.geom.Shape.accumulateCubic (added the width)
    private void accumulateCubic(float bbox[], int off,
                                float v0, float vc0, float vc1, float v1, float w)
    {
        // Breaking this cubic down into a polynomial:
        // eqn[0] = v0;
        // eqn[1] = (vc0 - v0) * 3f;
        // eqn[2] = (vc1 - vc0 - vc0 + v0) * 3f;
        // eqn[3] = v1 + (vc0 - vc1) * 3f - v0;
        // Deriving the polynomial:
        // eqn'[0] = 1*eqn[1] = 3(vc0-v0)
        // eqn'[1] = 2*eqn[2] = 6((vc1-vc0)-(vc0-v0))
        // eqn'[2] = 3*eqn[3] = 3((v1-vc1)-2(vc1-vc0)+(vc0-v0))
        // Solving for zeroes on the derivative:
        // e2*t*t + e1*t + e0 = a*t*t + b*t + c = 0
        // Note that in solving for 0 we can divide all e0,e1,e2 by 3
        // t = (-b +/- sqrt(b*b-4ac))/2a
        float c = vc0 - v0;
        float b = 2f * ((vc1 - vc0) - c);
        float a = (v1 - vc1) - b - c;
        if (a == 0f) {
            // The quadratic parabola has degenerated to a line.
            if (b == 0f) {
                // The line has degenerated to a constant.
                return;
            }
            accumulateCubic(bbox, off, -c/b, v0, vc0, vc1, v1, w);
        } else {
            // From Numerical Recipes, 5.6, Quadratic and Cubic Equations
            float d = b * b - 4f * a * c;
            if (d < 0f) {
                // If d < 0.0, then there are no roots
                return;
            }
            d = (float) Math.sqrt(d);
            // For accuracy, calculate one root using:
            //     (-b +/- d) / 2a
            // and the other using:
            //     2c / (-b +/- d)
            // Choose the sign of the +/- so that b+d gets larger in magnitude
            if (b < 0f) {
                d = -d;
            }
            float q = (b + d) / -2f;
            // We already tested a for being 0 above
            accumulateCubic(bbox, off, q/a, v0, vc0, vc1, v1, w);
            if (q != 0f) {
                accumulateCubic(bbox, off, c/q, v0, vc0, vc1, v1, w);
            }
        }
    }

    // Basically any type of transform that does not violate a uniform
    // unsheared 2D scale.  We may have to scale the associated line width,
    // but we can accumulate everything in device space with no problems.
    private static final int SAFE_ACCUMULATE_MASK =
        (BaseTransform.TYPE_FLIP |
         BaseTransform.TYPE_GENERAL_ROTATION |
         BaseTransform.TYPE_QUADRANT_ROTATION |
         BaseTransform.TYPE_TRANSLATION |
         BaseTransform.TYPE_UNIFORM_SCALE);

    public void accumulateShapeBounds(float bbox[], Shape shape, BaseTransform tx) {
        if (type == TYPE_INNER) {
            Shape.accumulate(bbox, shape, tx);
            return;
        }
        if ((tx.getType() & ~SAFE_ACCUMULATE_MASK) != 0) {
            // This is a work-around for RT-15648.  That bug still applies here
            // since we should be optimizing that case, but at least with this
            // work-around, someone who calls this method, and is not aware of
            // that bug, will not be bitten by a bad answer.
            Shape.accumulate(bbox, createStrokedShape(shape), tx);
            return;
        }
        PathIterator pi = shape.getPathIterator(tx);
        boolean lastSegmentMove = true;
        float coords[] = new float[6];
        float w = type == TYPE_CENTERED ? getLineWidth() / 2 : getLineWidth();
        // Length(Transform(w, 0)) == w * Length(Transform(1, 0))
        w *= Math.hypot(tx.getMxx(), tx.getMyx());
        // starting x,y; previous x0, y0 and current x1,y1
        float sx = 0f, sy = 0f, x0 = 0f, y0 = 0f, x1, y1;
        // starting delta x,y; delta x,y; previous delta x,y
        float sdx = 0f, sdy = 0f, dx, dy, pdx = 0f, pdy = 0f;
        // current offset
        float o[] = new float[4];
        // previous offset; starting offset
        float pox = 0f, poy = 0f, sox = 0f, soy = 0f;

        while (!pi.isDone()) {
            int cur = pi.currentSegment(coords);
            switch (cur) {
                case PathIterator.SEG_MOVETO:
                    if (!lastSegmentMove) {
                        accumulateCap(pdx, pdy, x0, y0, pox, poy, bbox, w);
                        accumulateCap(-sdx, -sdy, sx, sy, -sox, -soy, bbox, w);
                    }

                    x0 = sx = coords[0];
                    y0 = sy = coords[1];
                    break;
                case PathIterator.SEG_LINETO:
                    x1 = coords[0];
                    y1 = coords[1];
                    dx = x1 - x0;
                    dy = y1 - y0;
                    if (dx == 0f && dy == 0f) {
                        // Ideally these segments should be ignored, but both
                        // Java 2D and OpenPisces treat this case as if we
                        // were joining to a segment that was horizontal.
                        dx = 1f;
                    }

                    computeOffset(dx, dy, w, o, 0);

                    if (!lastSegmentMove) {
                        accumulateJoin(pdx, pdy, dx, dy, x0, y0, pox, poy, o[0], o[1], bbox, w);
                    }

                    x0 = x1;
                    y0 = y1;
                    pdx = dx;
                    pdy = dy;
                    pox = o[0];
                    poy = o[1];
                    if (lastSegmentMove) {
                        sdx = pdx;
                        sdy = pdy;
                        sox = pox;
                        soy = poy;
                    }
                    break;
                case PathIterator.SEG_QUADTO:
                    x1 = coords[2];
                    y1 = coords[3];
                    dx = coords[0] - x0;
                    dy = coords[1] - y0;

                    computeOffset(dx, dy, w, o, 0);
                    if (!lastSegmentMove) {
                        accumulateJoin(pdx, pdy, dx, dy, x0, y0, pox, poy, o[0], o[1], bbox, w);
                    }

                    if (bbox[0] > coords[0] - w || bbox[2] < coords[0] + w) {
                        accumulateQuad(bbox, 0, x0, coords[0], x1, w);
                    }
                    if (bbox[1] > coords[1] - w || bbox[3] < coords[1] + w) {
                        accumulateQuad(bbox, 1, y0, coords[1], y1, w);
                    }
                    x0 = x1;
                    y0 = y1;
                    if (lastSegmentMove) {
                        sdx = dx;
                        sdy = dy;
                        sox = o[0];
                        soy = o[1];
                    }
                    pdx = x1 - coords[0];
                    pdy = y1 - coords[1];
                    computeOffset(pdx, pdy, w, o, 0);
                    pox = o[0];
                    poy = o[1];
                    break;
                case PathIterator.SEG_CUBICTO:
                    x1 = coords[4];
                    y1 = coords[5];
                    dx = coords[0] - x0;
                    dy = coords[1] - y0;

                    computeOffset(dx, dy, w, o, 0);
                    if (!lastSegmentMove) {
                        accumulateJoin(pdx, pdy, dx, dy, x0, y0, pox, poy, o[0], o[1], bbox, w);
                    }

                    if (bbox[0] > coords[0] - w || bbox[2] < coords[0] + w ||
                        bbox[0] > coords[2] - w || bbox[2] < coords[2] + w)
                    {
                        accumulateCubic(bbox, 0, x0, coords[0], coords[2], x1, w);
                    }
                    if (bbox[1] > coords[1] - w|| bbox[3] < coords[1] + w ||
                        bbox[1] > coords[3] - w|| bbox[3] < coords[3] + w)
                    {
                        accumulateCubic(bbox, 1, y0, coords[1], coords[3], y1, w);
                    }
                    x0 = x1;
                    y0 = y1;
                    if (lastSegmentMove) {
                        sdx = dx;
                        sdy = dy;
                        sox = o[0];
                        soy = o[1];
                    }
                    pdx = x1 - coords[2];
                    pdy = y1 - coords[3];
                    computeOffset(pdx, pdy, w, o, 0);
                    pox = o[0];
                    poy = o[1];
                    break;
                case PathIterator.SEG_CLOSE:
                    dx = sx - x0;
                    dy = sy - y0;
                    x1 = sx;
                    y1 = sy;

                    if (!lastSegmentMove) {
                        computeOffset(sdx, sdy, w, o, 2);
                        if (dx == 0 && dy == 0) {
                            accumulateJoin(pdx, pdy, sdx, sdy, sx, sy, pox, poy, o[2], o[3], bbox, w);
                        } else {
                            computeOffset(dx, dy, w, o, 0);
                            accumulateJoin(pdx, pdy, dx, dy, x0, y0, pox, poy, o[0], o[1], bbox, w);
                            accumulateJoin(dx, dy, sdx, sdy, x1, y1, o[0], o[1], o[2], o[3], bbox, w);
                        }
                    }
                    x0 = x1;
                    y0 = y1;
                    break;
            }
            // Close behaves like a move in certain ways - after close, line will draw a cap,
            // like if the close implicitely did move to the original coordinates
            lastSegmentMove = cur == PathIterator.SEG_MOVETO || cur == PathIterator.SEG_CLOSE;
            pi.next();
        }

        if (!lastSegmentMove) {
            accumulateCap(pdx, pdy, x0, y0, pox, poy, bbox, w);
            accumulateCap(-sdx, -sdy, sx, sy, -sox, -soy, bbox, w);
        }
    }

    private void accumulate(float o0, float o1, float o2, float o3, float[] bbox) {
        if (o0 <= o2) {
            if (o0 < bbox[0]) bbox[0] = o0;
            if (o2 > bbox[2]) bbox[2] = o2;
        } else {
            if (o2 < bbox[0]) bbox[0] = o2;
            if (o0 > bbox[2]) bbox[2] = o0;
        }
        if (o1 <= o3) {
            if (o1 < bbox[1]) bbox[1] = o1;
            if (o3 > bbox[3]) bbox[3] = o3;
        } else {
            if (o3 < bbox[1]) bbox[1] = o3;
            if (o1 > bbox[3]) bbox[3] = o1;
        }
    }

    private void accumulateOrdered(float o0, float o1, float o2, float o3, float[] bbox) {
        if (o0 < bbox[0]) bbox[0] = o0;
        if (o2 > bbox[2]) bbox[2] = o2;
        if (o1 < bbox[1]) bbox[1] = o1;
        if (o3 > bbox[3]) bbox[3] = o3;
    }


    private void accumulateJoin(float pdx, float pdy, float dx, float dy, float x0, float y0,
                                float pox, float poy, float ox, float oy, float[] bbox, float w) {

        if (join == JOIN_BEVEL) {
            accumulateBevel(x0, y0, pox, poy, ox, oy, bbox);
        } else if (join == JOIN_MITER) {
            accumulateMiter(pdx, pdy, dx, dy, pox, poy, ox, oy, x0, y0, bbox, w);
        } else { // JOIN_ROUND
            accumulateOrdered(x0 - w, y0 - w, x0 + w, y0 + w, bbox);
        }


    }

    private void accumulateCap(float dx, float dy, float x0, float y0,
                               float ox, float oy, float[] bbox, float w) {
        if (cap == CAP_SQUARE) {
            accumulate(x0 + ox - oy, y0 + oy + ox, x0 - ox - oy, y0 - oy + ox, bbox);
        } else if (cap == CAP_BUTT) {
            accumulate(x0 + ox, y0 + oy, x0 - ox, y0 - oy, bbox);
        } else { //cap == CAP_ROUND
            accumulateOrdered(x0 - w, y0 - w, x0 + w, y0 + w, bbox);
        }

    }

    private float[] tmpMiter = new float[2];

    private void accumulateMiter(float pdx, float pdy, float dx, float dy,
                                    float pox, float poy, float ox, float oy,
                                    float x0, float y0, float[] bbox, float w) {
        // Always accumulate bevel for cases of degenerate miters...
        accumulateBevel(x0, y0, pox, poy, ox, oy, bbox);

        boolean cw = isCW(pdx, pdy, dx, dy);

        if (cw) {
            pox = -pox;
            poy = -poy;
            ox = -ox;
            oy = -oy;
        }

        computeMiter((x0 - pdx) + pox, (y0 - pdy) + poy, x0 + pox, y0 + poy,
                     (x0 + dx) + ox, (y0 + dy) + oy, x0 + ox, y0 + oy,
                     tmpMiter, 0);
        float lenSq = (tmpMiter[0] - x0) * (tmpMiter[0] - x0) + (tmpMiter[1] - y0) * (tmpMiter[1] - y0);

        float miterLimitWidth = miterLimit * w;
        if (lenSq < miterLimitWidth * miterLimitWidth) {
            accumulateOrdered(tmpMiter[0], tmpMiter[1], tmpMiter[0], tmpMiter[1], bbox);
        }
    }


    private void accumulateBevel(float x0, float y0, float pox, float poy, float ox, float oy, float[] bbox) {
        accumulate(x0 + pox, y0 + poy, x0 - pox, y0 - poy, bbox);
        accumulate(x0 + ox, y0 + oy, x0 - ox, y0 - oy, bbox);
    }

    public Shape createCenteredStrokedShape(final Shape s) {
        return ShapeUtil.createCenteredStrokedShape(s, this);
    }

    static final float SQRT_2 = (float) Math.sqrt(2);

    Shape strokeRoundRectangle(RoundRectangle2D rr) {
        if (rr.width < 0 || rr.height < 0) {
            return new Path2D();
        }
        if (isDashed()) {
            return null;
        }
        int j;
        float aw = rr.arcWidth;
        float ah = rr.arcHeight;
        if (aw <= 0f || ah <= 0f) {
            aw = ah = 0f;
            if (type == TYPE_INNER) {
                j = JOIN_MITER;
            } else {
                j = this.join;
                if (j == JOIN_MITER && miterLimit < SQRT_2) {
                    j = JOIN_BEVEL;
                }
            }
        } else {
            if (aw < ah * 0.9f || ah < aw * 0.9f) {
                // RT-27416
                // TODO: Need to check these multipliers and
                // optimize this case...
                return null;
            }
            j = JOIN_ROUND;
        }
        float id, od;
        if (type == TYPE_INNER) {
            od = 0f;
            id = this.width;
        } else if (type == TYPE_OUTER) {
            od = this.width;
            id = 0f;
        } else {
            od = id = this.width/2f;
        }
        Shape outer;
        switch (j) {
            case JOIN_MITER:
                outer = new RoundRectangle2D(rr.x - od, rr.y - od,
                                             rr.width+od*2f, rr.height+od*2f,
                                             0f, 0f);
                break;
            case JOIN_BEVEL:
                outer = makeBeveledRect(rr.x, rr.y, rr.width, rr.height, od);
                break;
            case JOIN_ROUND:
                outer = new RoundRectangle2D(rr.x - od, rr.y - od,
                                             rr.width+od*2f, rr.height+od*2f,
                                             aw+od*2f, ah+od*2f);
                break;
            default:
                throw new InternalError("Unrecognized line join style");
        }
        if (rr.width <= id*2f || rr.height <= id*2f) {
            return outer;
        }
        aw -= id*2f;
        ah -= id*2f;
        if (aw <= 0f || ah <= 0f) {
            aw = ah = 0f;
        }
        Shape inner = new RoundRectangle2D(rr.x + id, rr.y + id,
                                           rr.width-id*2f, rr.height-id*2f,
                                           aw, ah);
        Path2D p2d = (outer instanceof Path2D)
            ? ((Path2D) outer) : new Path2D(outer);
        p2d.setWindingRule(Path2D.WIND_EVEN_ODD);
        p2d.append(inner, false);
        return p2d;
    }

    static Shape makeBeveledRect(float rx, float ry,
                                 float rw, float rh,
                                 float d)
    {
        float rx0 = rx;
        float ry0 = ry;
        float rx1 = rx + rw;
        float ry1 = ry + rh;
        Path2D p = new Path2D();
        p.moveTo(rx0, ry0 - d);
        p.lineTo(rx1, ry0 - d);
        p.lineTo(rx1 + d, ry0);
        p.lineTo(rx1 + d, ry1);
        p.lineTo(rx1, ry1 + d);
        p.lineTo(rx0, ry1 + d);
        p.lineTo(rx0 - d, ry1);
        p.lineTo(rx0 - d, ry0);
        p.closePath();
        return p;
    }

    protected Shape makeIntersectedShape(Shape outer, Shape inner) {
        return new CAGShapePair(outer, inner, ShapePair.TYPE_INTERSECT);
    }

    protected Shape makeSubtractedShape(Shape outer, Shape inner) {
        return new CAGShapePair(outer, inner, ShapePair.TYPE_SUBTRACT);
    }

    static class CAGShapePair extends GeneralShapePair {
        private Shape cagshape;

        public CAGShapePair(Shape outer, Shape inner, int type) {
            super(outer, inner, type);
        }

        @Override
        public PathIterator getPathIterator(BaseTransform tx) {
            if (cagshape == null) {
                Area o = new Area(getOuterShape());
                Area i = new Area(getInnerShape());
                if (getCombinationType() == ShapePair.TYPE_INTERSECT) {
                    o.intersect(i);
                } else {
                    o.subtract(i);
                }
                cagshape = o;
            }
            return cagshape.getPathIterator(tx);
        }
    }

    /**
     * Returns the hashcode for this stroke.
     * @return      a hash code for this stroke.
     */
    @Override
    public int hashCode() {
        int hash = Float.floatToIntBits(width);
        hash = hash * 31 + join;
        hash = hash * 31 + cap;
        hash = hash * 31 + Float.floatToIntBits(miterLimit);
        if (dash != null) {
            hash = hash * 31 + Float.floatToIntBits(dashPhase);
            for (int i = 0; i < dash.length; i++) {
                hash = hash * 31 + Float.floatToIntBits(dash[i]);
            }
        }
        return hash;
    }

    /**
     * Tests if a specified object is equal to this <code>BasicStroke</code>
     * by first testing if it is a <code>BasicStroke</code> and then comparing
     * its width, join, cap, miter limit, dash, and dash phase attributes with
     * those of this <code>BasicStroke</code>.
     * @param  obj the specified object to compare to this
     *              <code>BasicStroke</code>
     * @return <code>true</code> if the width, join, cap, miter limit, dash, and
     *            dash phase are the same for both objects;
     *            <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BasicStroke)) {
            return false;
        }
        BasicStroke bs = (BasicStroke) obj;
        if (width != bs.width) {
            return false;
        }
        if (join != bs.join) {
            return false;
        }
        if (cap != bs.cap) {
            return false;
        }
        if (miterLimit != bs.miterLimit) {
            return false;
        }
        if (dash != null) {
            if (dashPhase != bs.dashPhase) {
                return false;
            }
            if (!java.util.Arrays.equals(dash, bs.dash)) {
                return false;
            }
        }
        else if (bs.dash != null) {
            return false;
        }

        return true;
    }

    public BasicStroke copy() {
        return new BasicStroke(type, width, cap, join, miterLimit, dash, dashPhase);
    }
}
