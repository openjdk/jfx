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
 * The <code>QuadCurve2D</code> class defines a quadratic parametric curve
 * segment in {@code (x,y)} coordinate space.
 * <p>
 * This class is only the abstract superclass for all objects that
 * store a 2D quadratic curve segment.
 * The actual storage representation of the coordinates is left to
 * the subclass.
 *
 * @version     1.40, 05/05/07
 */
public class QuadCurve2D extends Shape {
    /**
     * The X coordinate of the start point of the quadratic curve
     * segment.
     */
    public float x1;

    /**
     * The Y coordinate of the start point of the quadratic curve
     * segment.
     */
    public float y1;

    /**
     * The X coordinate of the control point of the quadratic curve
     * segment.
     */
    public float ctrlx;

    /**
     * The Y coordinate of the control point of the quadratic curve
     * segment.
     */
    public float ctrly;

    /**
     * The X coordinate of the end point of the quadratic curve
     * segment.
     */
    public float x2;

    /**
     * The Y coordinate of the end point of the quadratic curve
     * segment.
     */
    public float y2;

    /**
     * Constructs and initializes a <code>QuadCurve2D</code> with
     * coordinates (0, 0, 0, 0, 0, 0).
     */
    public QuadCurve2D() { }

    /**
     * Constructs and initializes a <code>QuadCurve2D</code> from the
     * specified {@code float} coordinates.
     *
     * @param x1 the X coordinate of the start point
     * @param y1 the Y coordinate of the start point
     * @param ctrlx the X coordinate of the control point
     * @param ctrly the Y coordinate of the control point
     * @param x2 the X coordinate of the end point
     * @param y2 the Y coordinate of the end point
     */
    public QuadCurve2D(float x1, float y1,
             float ctrlx, float ctrly,
             float x2, float y2)
        {
        setCurve(x1, y1, ctrlx, ctrly, x2, y2);
    }

    /**
     * Sets the location of the end points and control point of this curve
     * to the specified {@code float} coordinates.
     *
     * @param x1 the X coordinate of the start point
     * @param y1 the Y coordinate of the start point
     * @param ctrlx the X coordinate of the control point
     * @param ctrly the Y coordinate of the control point
     * @param x2 the X coordinate of the end point
     * @param y2 the Y coordinate of the end point
     */
    public void setCurve(float x1, float y1,
                 float ctrlx, float ctrly,
                 float x2, float y2)
    {
        this.x1    = x1;
        this.y1    = y1;
        this.ctrlx = ctrlx;
        this.ctrly = ctrly;
        this.x2    = x2;
        this.y2    = y2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RectBounds getBounds() {
        float left   = Math.min(Math.min(x1, x2), ctrlx);
        float top    = Math.min(Math.min(y1, y2), ctrly);
        float right  = Math.max(Math.max(x1, x2), ctrlx);
        float bottom = Math.max(Math.max(y1, y2), ctrly);
        return new RectBounds(left, top, right, bottom);
    }

    /**
     * {@inheritDoc}
     */
    public CubicCurve2D toCubic() {
        return new CubicCurve2D(x1, y1,
                               (x1 + 2 * ctrlx) / 3, (y1 + 2 * ctrly) / 3,
                               (2 * ctrlx + x2) / 3, (2 * ctrly + y2) / 3,
                               x2, y2);
    }

    /**
     * Sets the location of the end points and control points of this
     * <code>QuadCurve2D</code> to the <code>double</code> coordinates at
     * the specified offset in the specified array.
     * @param coords the array containing coordinate values
     * @param offset the index into the array from which to start
     *      getting the coordinate values and assigning them to this
     *      <code>QuadCurve2D</code>
     */
    public void setCurve(float[] coords, int offset) {
        setCurve(coords[offset + 0], coords[offset + 1],
             coords[offset + 2], coords[offset + 3],
             coords[offset + 4], coords[offset + 5]);
    }

    /**
     * Sets the location of the end points and control point of this
     * <code>QuadCurve2D</code> to the specified <code>Point2D</code>
     * coordinates.
     * @param p1 the start point
     * @param cp the control point
     * @param p2 the end point
     */
    public void setCurve(Point2D p1, Point2D cp, Point2D p2) {
        setCurve(p1.x, p1.y, cp.x, cp.y, p2.x, p2.y);
    }

    /**
     * Sets the location of the end points and control points of this
     * <code>QuadCurve2D</code> to the coordinates of the
     * <code>Point2D</code> objects at the specified offset in
     * the specified array.
     * @param pts an array containing <code>Point2D</code> that define
     *      coordinate values
     * @param offset the index into <code>pts</code> from which to start
     *      getting the coordinate values and assigning them to this
     *      <code>QuadCurve2D</code>
     */
    public void setCurve(Point2D[] pts, int offset) {
        setCurve(pts[offset + 0].x, pts[offset + 0].y,
             pts[offset + 1].x, pts[offset + 1].y,
             pts[offset + 2].x, pts[offset + 2].y);
    }

    /**
     * Sets the location of the end points and control point of this
     * <code>QuadCurve2D</code> to the same as those in the specified
     * <code>QuadCurve2D</code>.
     * @param c the specified <code>QuadCurve2D</code>
     */
    public void setCurve(QuadCurve2D c) {
        setCurve(c.x1, c.y1, c.ctrlx, c.ctrly, c.x2, c.y2);
    }

    /**
     * Returns the square of the flatness, or maximum distance of a
     * control point from the line connecting the end points, of the
     * quadratic curve specified by the indicated control points.
     *
     * @param x1 the X coordinate of the start point
     * @param y1 the Y coordinate of the start point
     * @param ctrlx the X coordinate of the control point
     * @param ctrly the Y coordinate of the control point
     * @param x2 the X coordinate of the end point
     * @param y2 the Y coordinate of the end point
     * @return the square of the flatness of the quadratic curve
     *      defined by the specified coordinates.
     */
    public static float getFlatnessSq(float x1, float y1,
                       float ctrlx, float ctrly,
                       float x2, float y2) {
        return Line2D.ptSegDistSq(x1, y1, x2, y2, ctrlx, ctrly);
    }

    /**
     * Returns the flatness, or maximum distance of a
     * control point from the line connecting the end points, of the
     * quadratic curve specified by the indicated control points.
     *
     * @param x1 the X coordinate of the start point
     * @param y1 the Y coordinate of the start point
     * @param ctrlx the X coordinate of the control point
     * @param ctrly the Y coordinate of the control point
     * @param x2 the X coordinate of the end point
     * @param y2 the Y coordinate of the end point
     * @return the flatness of the quadratic curve defined by the
     *      specified coordinates.
     */
    public static float getFlatness(float x1, float y1,
                     float ctrlx, float ctrly,
                     float x2, float y2) {
        return Line2D.ptSegDist(x1, y1, x2, y2, ctrlx, ctrly);
    }

    /**
     * Returns the square of the flatness, or maximum distance of a
     * control point from the line connecting the end points, of the
     * quadratic curve specified by the control points stored in the
     * indicated array at the indicated index.
     * @param coords an array containing coordinate values
     * @param offset the index into <code>coords</code> from which to
     *      to start getting the values from the array
     * @return the flatness of the quadratic curve that is defined by the
     *      values in the specified array at the specified index.
     */
    public static float getFlatnessSq(float coords[], int offset) {
        return Line2D.ptSegDistSq(coords[offset + 0], coords[offset + 1],
                      coords[offset + 4], coords[offset + 5],
                      coords[offset + 2], coords[offset + 3]);
    }

    /**
     * Returns the flatness, or maximum distance of a
     * control point from the line connecting the end points, of the
     * quadratic curve specified by the control points stored in the
     * indicated array at the indicated index.
     * @param coords an array containing coordinate values
     * @param offset the index into <code>coords</code> from which to
     *      start getting the coordinate values
     * @return the flatness of a quadratic curve defined by the
     *      specified array at the specified offset.
     */
    public static float getFlatness(float coords[], int offset) {
        return Line2D.ptSegDist(coords[offset + 0], coords[offset + 1],
                    coords[offset + 4], coords[offset + 5],
                    coords[offset + 2], coords[offset + 3]);
    }

    /**
     * Returns the square of the flatness, or maximum distance of a
     * control point from the line connecting the end points, of this
     * <code>QuadCurve2D</code>.
     * @return the square of the flatness of this
     *      <code>QuadCurve2D</code>.
     */
    public float getFlatnessSq() {
        return Line2D.ptSegDistSq(x1, y1, x2, y2, ctrlx, ctrly);
    }

    /**
     * Returns the flatness, or maximum distance of a
     * control point from the line connecting the end points, of this
     * <code>QuadCurve2D</code>.
     * @return the flatness of this <code>QuadCurve2D</code>.
     */
    public float getFlatness() {
        return Line2D.ptSegDist(x1, y1, x2, y2, ctrlx, ctrly);
    }

    /**
     * Subdivides this <code>QuadCurve2D</code> and stores the resulting
     * two subdivided curves into the <code>left</code> and
     * <code>right</code> curve parameters.
     * Either or both of the <code>left</code> and <code>right</code>
     * objects can be the same as this <code>QuadCurve2D</code> or
     * <code>null</code>.
     * @param left the <code>QuadCurve2D</code> object for storing the
     * left or first half of the subdivided curve
     * @param right the <code>QuadCurve2D</code> object for storing the
     * right or second half of the subdivided curve
     */
    public void subdivide(QuadCurve2D left, QuadCurve2D right) {
        subdivide(this, left, right);
    }

    /**
     * Subdivides the quadratic curve specified by the <code>src</code>
     * parameter and stores the resulting two subdivided curves into the
     * <code>left</code> and <code>right</code> curve parameters.
     * Either or both of the <code>left</code> and <code>right</code>
     * objects can be the same as the <code>src</code> object or
     * <code>null</code>.
     * @param src the quadratic curve to be subdivided
     * @param left the <code>QuadCurve2D</code> object for storing the
     *      left or first half of the subdivided curve
     * @param right the <code>QuadCurve2D</code> object for storing the
     *      right or second half of the subdivided curve
     */
    public static void subdivide(QuadCurve2D src,
                 QuadCurve2D left,
                 QuadCurve2D right)
    {
        float x1 = src.x1;
        float y1 = src.y1;
        float ctrlx = src.ctrlx;
        float ctrly = src.ctrly;
        float x2 = src.x2;
        float y2 = src.y2;
        float ctrlx1 = (x1 + ctrlx) / 2f;
        float ctrly1 = (y1 + ctrly) / 2f;
        float ctrlx2 = (x2 + ctrlx) / 2f;
        float ctrly2 = (y2 + ctrly) / 2f;
        ctrlx = (ctrlx1 + ctrlx2) / 2f;
        ctrly = (ctrly1 + ctrly2) / 2f;
        if (left != null) {
            left.setCurve(x1, y1, ctrlx1, ctrly1, ctrlx, ctrly);
        }
        if (right != null) {
            right.setCurve(ctrlx, ctrly, ctrlx2, ctrly2, x2, y2);
        }
    }

    /**
     * Subdivides the quadratic curve specified by the coordinates
     * stored in the <code>src</code> array at indices
     * <code>srcoff</code> through <code>srcoff</code>&nbsp;+&nbsp;5
     * and stores the resulting two subdivided curves into the two
     * result arrays at the corresponding indices.
     * Either or both of the <code>left</code> and <code>right</code>
     * arrays can be <code>null</code> or a reference to the same array
     * and offset as the <code>src</code> array.
     * Note that the last point in the first subdivided curve is the
     * same as the first point in the second subdivided curve.  Thus,
     * it is possible to pass the same array for <code>left</code> and
     * <code>right</code> and to use offsets such that
     * <code>rightoff</code> equals <code>leftoff</code> + 4 in order
     * to avoid allocating extra storage for this common point.
     * @param src the array holding the coordinates for the source curve
     * @param srcoff the offset into the array of the beginning of the
     * the 6 source coordinates
     * @param left the array for storing the coordinates for the first
     * half of the subdivided curve
     * @param leftoff the offset into the array of the beginning of the
     * the 6 left coordinates
     * @param right the array for storing the coordinates for the second
     * half of the subdivided curve
     * @param rightoff the offset into the array of the beginning of the
     * the 6 right coordinates
     */
    public static void subdivide(float src[], int srcoff,
                 float left[], int leftoff,
                 float right[], int rightoff)
    {
        float x1 = src[srcoff + 0];
        float y1 = src[srcoff + 1];
        float ctrlx = src[srcoff + 2];
        float ctrly = src[srcoff + 3];
        float x2 = src[srcoff + 4];
        float y2 = src[srcoff + 5];
        if (left != null) {
            left[leftoff + 0] = x1;
            left[leftoff + 1] = y1;
        }
        if (right != null) {
            right[rightoff + 4] = x2;
            right[rightoff + 5] = y2;
        }
        x1 = (x1 + ctrlx) / 2f;
        y1 = (y1 + ctrly) / 2f;
        x2 = (x2 + ctrlx) / 2f;
        y2 = (y2 + ctrly) / 2f;
        ctrlx = (x1 + x2) / 2f;
        ctrly = (y1 + y2) / 2f;
        if (left != null) {
            left[leftoff + 2] = x1;
            left[leftoff + 3] = y1;
            left[leftoff + 4] = ctrlx;
            left[leftoff + 5] = ctrly;
        }
        if (right != null) {
            right[rightoff + 0] = ctrlx;
            right[rightoff + 1] = ctrly;
            right[rightoff + 2] = x2;
            right[rightoff + 3] = y2;
        }
    }

    /**
     * Solves the quadratic whose coefficients are in the <code>eqn</code>
     * array and places the non-complex roots back into the same array,
     * returning the number of roots.  The quadratic solved is represented
     * by the equation:
     * <pre>
     *     eqn = {C, B, A};
     *     ax^2 + bx + c = 0
     * </pre>
     * A return value of <code>-1</code> is used to distinguish a constant
     * equation, which might be always 0 or never 0, from an equation that
     * has no zeroes.
     * @param eqn the array that contains the quadratic coefficients
     * @return the number of roots, or <code>-1</code> if the equation is
     *      a constant
     */
    public static int solveQuadratic(float eqn[]) {
        return solveQuadratic(eqn, eqn);
    }

    /**
     * Solves the quadratic whose coefficients are in the <code>eqn</code>
     * array and places the non-complex roots into the <code>res</code>
     * array, returning the number of roots.
     * The quadratic solved is represented by the equation:
     * <pre>
     *     eqn = {C, B, A};
     *     ax^2 + bx + c = 0
     * </pre>
     * A return value of <code>-1</code> is used to distinguish a constant
     * equation, which might be always 0 or never 0, from an equation that
     * has no zeroes.
     * @param eqn the specified array of coefficients to use to solve
     *        the quadratic equation
     * @param res the array that contains the non-complex roots
     *        resulting from the solution of the quadratic equation
     * @return the number of roots, or <code>-1</code> if the equation is
     *  a constant.
     */
    public static int solveQuadratic(float eqn[], float res[]) {
        float a = eqn[2];
        float b = eqn[1];
        float c = eqn[0];
        int roots = 0;
        if (a == 0f) {
            // The quadratic parabola has degenerated to a line.
            if (b == 0f) {
                // The line has degenerated to a constant.
                return -1;
            }
            res[roots++] = -c / b;
        } else {
            // From Numerical Recipes, 5.6, Quadratic and Cubic Equations
            float d = b * b - 4f * a * c;
            if (d < 0f) {
                // If d < 0.0, then there are no roots
                return 0;
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
            res[roots++] = q / a;
            if (q != 0f) {
                res[roots++] = c / q;
            }
        }
        return roots;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(float x, float y) {
        float x1 = this.x1;
        float y1 = this.y1;
        float xc = this.ctrlx;
        float yc = this.ctrly;
        float x2 = this.x2;
        float y2 = this.y2;

        /*
         * We have a convex shape bounded by quad curve Pc(t)
         * and ine Pl(t).
         *
         *     P1 = (x1, y1) - start point of curve
         *     P2 = (x2, y2) - end point of curve
         *     Pc = (xc, yc) - control point
         *
         *     Pq(t) = P1*(1 - t)^2 + 2*Pc*t*(1 - t) + P2*t^2 =
         *           = (P1 - 2*Pc + P2)*t^2 + 2*(Pc - P1)*t + P1
         *     Pl(t) = P1*(1 - t) + P2*t
         *     t = [0:1]
         *
         *     P = (x, y) - point of interest
         *
         * Let's look at second derivative of quad curve equation:
         *
         *     Pq''(t) = 2 * (P1 - 2 * Pc + P2) = Pq''
         *     It's constant vector.
         *
         * Let's draw a line through P to be parallel to this
         * vector and find the intersection of the quad curve
         * and the line.
         *
         * Pq(t) is point of intersection if system of equations
         * below has the solution.
         *
         *     L(s) = P + Pq''*s == Pq(t)
         *     Pq''*s + (P - Pq(t)) == 0
         *
         *     | xq''*s + (x - xq(t)) == 0
         *     | yq''*s + (y - yq(t)) == 0
         *
         * This system has the solution if rank of its matrix equals to 1.
         * That is, determinant of the matrix should be zero.
         *
         *     (y - yq(t))*xq'' == (x - xq(t))*yq''
         *
         * Let's solve this equation with 't' variable.
         * Also let kx = x1 - 2*xc + x2
         *          ky = y1 - 2*yc + y2
         *
         *     t0q = (1/2)*((x - x1)*ky - (y - y1)*kx) /
         *                 ((xc - x1)*ky - (yc - y1)*kx)
         *
         * Let's do the same for our line Pl(t):
         *
         *     t0l = ((x - x1)*ky - (y - y1)*kx) /
         *           ((x2 - x1)*ky - (y2 - y1)*kx)
         *
         * It's easy to check that t0q == t0l. This fact means
         * we can compute t0 only one time.
         *
         * In case t0 < 0 or t0 > 1, we have an intersections outside
         * of shape bounds. So, P is definitely out of shape.
         *
         * In case t0 is inside [0:1], we should calculate Pq(t0)
         * and Pl(t0). We have three points for now, and all of them
         * lie on one line. So, we just need to detect, is our point
         * of interest between points of intersections or not.
         *
         * If the denominator in the t0q and t0l equations is
         * zero, then the points must be collinear and so the
         * curve is degenerate and encloses no area.  Thus the
         * result is false.
         */
        float kx = x1 - 2 * xc + x2;
        float ky = y1 - 2 * yc + y2;
        float dx = x - x1;
        float dy = y - y1;
        float dxl = x2 - x1;
        float dyl = y2 - y1;

        float t0 = (dx * ky - dy * kx) / (dxl * ky - dyl * kx);
        if (t0 < 0 || t0 > 1 || t0 != t0) {
            return false;
        }

        float xb = kx * t0 * t0 + 2 * (xc - x1) * t0 + x1;
        float yb = ky * t0 * t0 + 2 * (yc - y1) * t0 + y1;
        float xl = dxl * t0 + x1;
        float yl = dyl * t0 + y1;

        return (x >= xb && x < xl) ||
               (x >= xl && x < xb) ||
               (y >= yb && y < yl) ||
               (y >= yl && y < yb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Point2D p) {
        return contains(p.x, p.y);
    }

    /**
     * Fill an array with the coefficients of the parametric equation
     * in t, ready for solving against val with solveQuadratic.
     * We currently have:
     *     val = Py(t) = C1*(1-t)^2 + 2*CP*t*(1-t) + C2*t^2
     *                 = C1 - 2*C1*t + C1*t^2 + 2*CP*t - 2*CP*t^2 + C2*t^2
     *                 = C1 + (2*CP - 2*C1)*t + (C1 - 2*CP + C2)*t^2
     *               0 = (C1 - val) + (2*CP - 2*C1)*t + (C1 - 2*CP + C2)*t^2
     *               0 = C + Bt + At^2
     *     C = C1 - val
     *     B = 2*CP - 2*C1
     *     A = C1 - 2*CP + C2
     */
    private static void fillEqn(float eqn[], float val,
                float c1, float cp, float c2) {
        eqn[0] = c1 - val;
        eqn[1] = cp + cp - c1 - c1;
        eqn[2] = c1 - cp - cp + c2;
    }

    /**
     * Evaluate the t values in the first num slots of the vals[] array
     * and place the evaluated values back into the same array.  Only
     * evaluate t values that are within the range <0, 1>, including
     * the 0 and 1 ends of the range iff the include0 or include1
     * booleans are true.  If an "inflection" equation is handed in,
     * then any points which represent a point of inflection for that
     * quadratic equation are also ignored.
     */
    private static int evalQuadratic(float vals[], int num,
                     boolean include0,
                     boolean include1,
                     float inflect[],
                     float c1, float ctrl, float c2) {
        int j = 0;
        for (int i = 0; i < num; i++) {
            float t = vals[i];
            if ((include0 ? t >= 0 : t > 0) &&
            (include1 ? t <= 1 : t < 1) &&
            (inflect == null ||
             inflect[1] + 2*inflect[2]*t != 0))
            {
                float u = 1 - t;
                vals[j++] = c1*u*u + 2*ctrl*t*u + c2*t*t;
            }
        }
        return j;
    }

    private static final int BELOW = -2;
    private static final int LOWEDGE = -1;
    private static final int INSIDE = 0;
    private static final int HIGHEDGE = 1;
    private static final int ABOVE = 2;

    /**
     * Determine where coord lies with respect to the range from
     * low to high.  It is assumed that low <= high.  The return
     * value is one of the 5 values BELOW, LOWEDGE, INSIDE, HIGHEDGE,
     * or ABOVE.
     */
    private static int getTag(float coord, float low, float high) {
        if (coord <= low) {
            return (coord < low ? BELOW : LOWEDGE);
        }
        if (coord >= high) {
            return (coord > high ? ABOVE : HIGHEDGE);
        }
        return INSIDE;
    }

    /**
     * Determine if the pttag represents a coordinate that is already
     * in its test range, or is on the border with either of the two
     * opttags representing another coordinate that is "towards the
     * inside" of that test range.  In other words, are either of the
     * two "opt" points "drawing the pt inward"?
     */
    private static boolean inwards(int pttag, int opt1tag, int opt2tag) {
        switch (pttag) {
            case BELOW:
            case ABOVE:
            default:
                return false;
            case LOWEDGE:
                return (opt1tag >= INSIDE || opt2tag >= INSIDE);
            case INSIDE:
                return true;
            case HIGHEDGE:
                return (opt1tag <= INSIDE || opt2tag <= INSIDE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean intersects(float x, float y, float w, float h) {
        // Trivially reject non-existant rectangles
        if (w <= 0 || h <= 0) {
            return false;
        }

        // Trivially accept if either endpoint is inside the rectangle
        // (not on its border since it may end there and not go inside)
        // Record where they lie with respect to the rectangle.
        //     -1 => left, 0 => inside, 1 => right
        float x1 = this.x1;
        float y1 = this.y1;
        int x1tag = getTag(x1, x, x + w);
        int y1tag = getTag(y1, y, y + h);
        if (x1tag == INSIDE && y1tag == INSIDE) {
            return true;
        }
        float x2 = this.x2;
        float y2 = this.y2;
        int x2tag = getTag(x2, x, x + w);
        int y2tag = getTag(y2, y, y + h);
        if (x2tag == INSIDE && y2tag == INSIDE) {
            return true;
        }
        float ctrlx = this.ctrlx;
        float ctrly = this.ctrly;
        int ctrlxtag = getTag(ctrlx, x, x + w);
        int ctrlytag = getTag(ctrly, y, y + h);

        // Trivially reject if all points are entirely to one side of
        // the rectangle.
        if (x1tag < INSIDE && x2tag < INSIDE && ctrlxtag < INSIDE) {
            return false;   // All points left
        }
        if (y1tag < INSIDE && y2tag < INSIDE && ctrlytag < INSIDE) {
            return false;   // All points above
        }
        if (x1tag > INSIDE && x2tag > INSIDE && ctrlxtag > INSIDE) {
            return false;   // All points right
        }
        if (y1tag > INSIDE && y2tag > INSIDE && ctrlytag > INSIDE) {
            return false;   // All points below
        }

        // Test for endpoints on the edge where either the segment
        // or the curve is headed "inwards" from them
        // Note: These tests are a superset of the fast endpoint tests
        //       above and thus repeat those tests, but take more time
        //       and cover more cases
        if (inwards(x1tag, x2tag, ctrlxtag) &&
            inwards(y1tag, y2tag, ctrlytag))
        {
            // First endpoint on border with either edge moving inside
            return true;
        }
        if (inwards(x2tag, x1tag, ctrlxtag) &&
            inwards(y2tag, y1tag, ctrlytag))
        {
            // Second endpoint on border with either edge moving inside
            return true;
        }

        // Trivially accept if endpoints span directly across the rectangle
        boolean xoverlap = (x1tag * x2tag <= 0);
        boolean yoverlap = (y1tag * y2tag <= 0);
        if (x1tag == INSIDE && x2tag == INSIDE && yoverlap) {
            return true;
        }
        if (y1tag == INSIDE && y2tag == INSIDE && xoverlap) {
            return true;
        }

        // We now know that both endpoints are outside the rectangle
        // but the 3 points are not all on one side of the rectangle.
        // Therefore the curve cannot be contained inside the rectangle,
        // but the rectangle might be contained inside the curve, or
        // the curve might intersect the boundary of the rectangle.

        float[] eqn = new float[3];
        float[] res = new float[3];
        if (!yoverlap) {
                // Both Y coordinates for the closing segment are above or
            // below the rectangle which means that we can only intersect
            // if the curve crosses the top (or bottom) of the rectangle
            // in more than one place and if those crossing locations
            // span the horizontal range of the rectangle.
            fillEqn(eqn, (y1tag < INSIDE ? y : y+h), y1, ctrly, y2);
            return (solveQuadratic(eqn, res) == 2 &&
                evalQuadratic(res, 2, true, true, null,
                      x1, ctrlx, x2) == 2 &&
                getTag(res[0], x, x+w) * getTag(res[1], x, x+w) <= 0);
        }

        // Y ranges overlap.  Now we examine the X ranges
        if (!xoverlap) {
                // Both X coordinates for the closing segment are left of
            // or right of the rectangle which means that we can only
            // intersect if the curve crosses the left (or right) edge
            // of the rectangle in more than one place and if those
            // crossing locations span the vertical range of the rectangle.
            fillEqn(eqn, (x1tag < INSIDE ? x : x+w), x1, ctrlx, x2);
            return (solveQuadratic(eqn, res) == 2 &&
                evalQuadratic(res, 2, true, true, null,
                      y1, ctrly, y2) == 2 &&
                getTag(res[0], y, y+h) * getTag(res[1], y, y+h) <= 0);
        }

        // The X and Y ranges of the endpoints overlap the X and Y
        // ranges of the rectangle, now find out how the endpoint
        // line segment intersects the Y range of the rectangle
        float dx = x2 - x1;
        float dy = y2 - y1;
        float k = y2 * x1 - x2 * y1;
        int c1tag, c2tag;
        if (y1tag == INSIDE) {
            c1tag = x1tag;
        } else {
            c1tag = getTag((k + dx * (y1tag < INSIDE ? y : y+h)) / dy, x, x+w);
        }
        if (y2tag == INSIDE) {
            c2tag = x2tag;
        } else {
            c2tag = getTag((k + dx * (y2tag < INSIDE ? y : y+h)) / dy, x, x+w);
        }
        // If the part of the line segment that intersects the Y range
        // of the rectangle crosses it horizontally - trivially accept
        if (c1tag * c2tag <= 0) {
            return true;
        }

        // Now we know that both the X and Y ranges intersect and that
        // the endpoint line segment does not directly cross the rectangle.
        //
        // We can almost treat this case like one of the cases above
        // where both endpoints are to one side, except that we will
        // only get one intersection of the curve with the vertical
        // side of the rectangle.  This is because the endpoint segment
        // accounts for the other intersection.
        //
        // (Remember there is overlap in both the X and Y ranges which
        //  means that the segment must cross at least one vertical edge
        //  of the rectangle - in particular, the "near vertical side" -
        //  leaving only one intersection for the curve.)
        //
        // Now we calculate the y tags of the two intersections on the
        // "near vertical side" of the rectangle.  We will have one with
        // the endpoint segment, and one with the curve.  If those two
        // vertical intersections overlap the Y range of the rectangle,
        // we have an intersection.  Otherwise, we don't.

        // c1tag = vertical intersection class of the endpoint segment
        //
        // Choose the y tag of the endpoint that was not on the same
        // side of the rectangle as the subsegment calculated above.
        // Note that we can "steal" the existing Y tag of that endpoint
        // since it will be provably the same as the vertical intersection.
        c1tag = ((c1tag * x1tag <= 0) ? y1tag : y2tag);

        // c2tag = vertical intersection class of the curve
        //
        // We have to calculate this one the straightforward way.
        // Note that the c2tag can still tell us which vertical edge
        // to test against.
        fillEqn(eqn, (c2tag < INSIDE ? x : x+w), x1, ctrlx, x2);
        int num = solveQuadratic(eqn, res);

        // Note: We should be able to assert(num == 2); since the
        // X range "crosses" (not touches) the vertical boundary,
        // but we pass num to evalQuadratic for completeness.
        evalQuadratic(res, num, true, true, null, y1, ctrly, y2);

        // Note: We can assert(num evals == 1); since one of the
        // 2 crossings will be out of the [0,1] range.
        c2tag = getTag(res[0], y, y+h);

        // Finally, we have an intersection if the two crossings
        // overlap the Y range of the rectangle.
        return (c1tag * c2tag <= 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(float x, float y, float w, float h) {
        if (w <= 0 || h <= 0) {
            return false;
        }
        // Assertion: Quadratic curves closed by connecting their
        // endpoints are always convex.
        return (contains(x, y) &&
            contains(x + w, y) &&
            contains(x + w, y + h) &&
            contains(x, y + h));
    }

    /**
     * Returns an iteration object that defines the boundary of the
     * shape of this <code>QuadCurve2D</code>.
     * The iterator for this class is not multi-threaded safe,
     * which means that this <code>QuadCurve2D</code> class does not
     * guarantee that modifications to the geometry of this
     * <code>QuadCurve2D</code> object do not affect any iterations of
     * that geometry that are already in process.
     * @param tx an optional {@link BaseTransform} to apply to the
     *      shape boundary
     * @return a {@link PathIterator} object that defines the boundary
     *      of the shape.
     */
    @Override
    public PathIterator getPathIterator(BaseTransform tx) {
        return new QuadIterator(this, tx);
    }

    /**
     * Returns an iteration object that defines the boundary of the
     * flattened shape of this <code>QuadCurve2D</code>.
     * The iterator for this class is not multi-threaded safe,
     * which means that this <code>QuadCurve2D</code> class does not
     * guarantee that modifications to the geometry of this
     * <code>QuadCurve2D</code> object do not affect any iterations of
     * that geometry that are already in process.
     * @param tx an optional <code>BaseTransform</code> to apply
     *      to the boundary of the shape
     * @param flatness the maximum distance that the control points for a
     *      subdivided curve can be with respect to a line connecting
     *      the end points of this curve before this curve is
     *      replaced by a straight line connecting the end points.
     * @return a <code>PathIterator</code> object that defines the
     *      flattened boundary of the shape.
     */
    @Override
    public PathIterator getPathIterator(BaseTransform tx, float flatness) {
        return new FlatteningPathIterator(getPathIterator(tx), flatness);
    }

    @Override
    public QuadCurve2D copy() {
        return new QuadCurve2D(x1, y1, ctrlx, ctrly, x2, y2);
    }

    @Override
    public int hashCode() {
        int bits = java.lang.Float.floatToIntBits(x1);
        bits += java.lang.Float.floatToIntBits(y1) * 37;
        bits += java.lang.Float.floatToIntBits(x2) * 43;
        bits += java.lang.Float.floatToIntBits(y2) * 47;
        bits += java.lang.Float.floatToIntBits(ctrlx) * 53;
        bits += java.lang.Float.floatToIntBits(ctrly) * 59;
        return bits;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof QuadCurve2D) {
            QuadCurve2D curve = (QuadCurve2D) obj;
            return ((x1 == curve.x1) && (y1 == curve.y1) &&
                    (x2 == curve.x2) && (y2 == curve.y2) &&
                    (ctrlx == curve.ctrlx) && (ctrly == curve.ctrly));
        }
        return false;
    }
}
