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

package com.sun.javafx.geom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class CubicApproximator {
    private float accuracy;
    private float maxCubicSize;

    public CubicApproximator(float accuracy, float maxCubicSize) {
        this.accuracy = accuracy;
        this.maxCubicSize = maxCubicSize;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setMaxCubicSize(float maxCCubicSize) {
        this.maxCubicSize = maxCCubicSize;
    }

    public float getMaxCubicSize() {
        return maxCubicSize;
    }

    public float approximate(List<QuadCurve2D> res, List<CubicCurve2D> tmp,
                              CubicCurve2D curve) {
        float d = getApproxError(curve);
        if (d < accuracy) {
            tmp.add(curve);
            res.add(approximate(curve));
            return d;
        } else {
            SplitCubic(tmp, new float[] {curve.x1, curve.y1,
                                         curve.ctrlx1, curve.ctrly1,
                                         curve.ctrlx2, curve.ctrly2,
                                         curve.x2, curve.y2});
            return approximate(tmp, res);
        }
    }

    public float approximate(List<QuadCurve2D> res, CubicCurve2D curve) {
        List<CubicCurve2D> tmp = new ArrayList<>();
        return approximate(res, tmp, curve);
    }

    private QuadCurve2D approximate(CubicCurve2D c) {
        // Using following approximation of the control point: (3P1-P0+3P2-P3)/4
        return new QuadCurve2D(c.x1, c.y1,
            (3f*c.ctrlx1 - c.x1 + 3f*c.ctrlx2 - c.x2)/4f,
            (3f*c.ctrly1 - c.y1 + 3f*c.ctrly2 - c.y2)/4f,
            c.x2, c.y2);
    }

    private float approximate(List<CubicCurve2D> curves,
                               List<QuadCurve2D> res)
    {
        QuadCurve2D approx = approximate(curves.get(0));
        float dMax = CubicApproximator.compareCPs(
            curves.get(0), CubicApproximator.elevate(approx));

        res.add(approx);

        for (int i = 1; i < curves.size(); i++) {
            approx = approximate(curves.get(i));
            float d = CubicApproximator.compareCPs(
                curves.get(i), CubicApproximator.elevate(approx));
            if (d > dMax) {
                dMax = d;
            }
            res.add(approx);
        }
        return dMax;
    }

    private static CubicCurve2D elevate(QuadCurve2D q) {
        return new CubicCurve2D(q.x1, q.y1,
            (q.x1 + 2f*q.ctrlx)/3f,
            (q.y1 + 2f*q.ctrly)/3f,
            (2f*q.ctrlx + q.x2)/3f,
            (2f*q.ctrly + q.y2)/3f,
            q.x2, q.y2);
    }

    private static float compare(CubicCurve2D c1, CubicCurve2D c2) {
        float res = Math.abs(c1.x1 - c2.x1);
        float d = Math.abs(c1.y1 - c2.y1);
        if (res < d) res = d;
        d = Math.abs(c1.ctrlx1 - c2.ctrlx1);
        if (res < d) res = d;
        d = Math.abs(c1.ctrly1 - c2.ctrly1);
        if (res < d) res = d;
        d = Math.abs(c1.ctrlx2 - c2.ctrlx2);
        if (res < d) res = d;
        d = Math.abs(c1.ctrly2 - c2.ctrly2);
        if (res < d) res = d;
        d = Math.abs(c1.x2 - c2.x2);
        if (res < d) res = d;
        d = Math.abs(c1.y2 - c2.y2);
        if (res < d) res = d;

        return res;
    }

    private static float getApproxError(float [] coords) {
        /* Calculating error for approximation using formula:
         * max(|(-3P1+P0+3P2-P3)/6|, |(3P1-P0-3P2+P3)/6|)
         */
        float res =
            (-3f*coords[2] + coords[0] + 3f*coords[4] - coords[6])/6f;
        float d = (-3f*coords[3] + coords[1] + 3f*coords[5] - coords[7])/6f;
        if (res < d) res = d;
        d = (3f*coords[2] - coords[0] - 3f*coords[4] + coords[6])/6f;
        if (res < d) res = d;
        d = (3f*coords[3] - coords[1] - 3f*coords[5] + coords[7])/6f;
        if (res < d) res = d;
        return res;
    }

    public static float getApproxError(CubicCurve2D curve) {
        return getApproxError(new float[] {curve.x1, curve.y1,
                                      curve.ctrlx1, curve.ctrly1,
                                      curve.ctrlx2, curve.ctrly2,
                                      curve.x2, curve.y2});
    }

    private static float compareCPs(CubicCurve2D c1, CubicCurve2D c2) {
        float res = Math.abs(c1.ctrlx1 - c2.ctrlx1);
        float d = Math.abs(c1.ctrly1 - c2.ctrly1);
        if (res < d) res = d;
        d = Math.abs(c1.ctrlx2 - c2.ctrlx2);
        if (res < d) res = d;
        d = Math.abs(c1.ctrly2 - c2.ctrly2);
        if (res < d) res = d;
        return res;
    }


    /*
     * Checking size of the cubic curves and split them if necessary.
     * Calling DrawMonotonicCubic for the curves of the appropriate size.
     * Note: coords array could be changed
     */
    private void ProcessMonotonicCubic(List<CubicCurve2D> resVect,
                                       float[] coords)
    {

        float[] coords1 = new float[8];
        float tx, ty;
        float xMin, xMax;
        float yMin, yMax;

        xMin = xMax = coords[0];
        yMin = yMax = coords[1];

        for (int i = 2; i < 8; i += 2) {
            xMin = (xMin > coords[i])? coords[i] : xMin;
            xMax = (xMax < coords[i])? coords[i] : xMax;
            yMin = (yMin > coords[i + 1])? coords[i + 1] : yMin;
            yMax = (yMax < coords[i + 1])? coords[i + 1] : yMax;
        }

        if (xMax - xMin > maxCubicSize || yMax - yMin > maxCubicSize ||
            getApproxError(coords) > accuracy) {
            coords1[6] = coords[6];
            coords1[7] = coords[7];
            coords1[4] = (coords[4] + coords[6])/2f;
            coords1[5] = (coords[5] + coords[7])/2f;
            tx = (coords[2] + coords[4])/2f;
            ty = (coords[3] + coords[5])/2f;
            coords1[2] = (tx + coords1[4])/2f;
            coords1[3] = (ty + coords1[5])/2f;
            coords[2] =  (coords[0] + coords[2])/2f;
            coords[3] =  (coords[1] + coords[3])/2f;
            coords[4] = (coords[2] + tx)/2f;
            coords[5] = (coords[3] + ty)/2f;
            coords[6]=coords1[0]=(coords[4] + coords1[2])/2f;
            coords[7]=coords1[1]=(coords[5] + coords1[3])/2f;

            ProcessMonotonicCubic(resVect, coords);

            ProcessMonotonicCubic(resVect, coords1);
        } else {
            resVect.add(new CubicCurve2D(
                coords[0], coords[1], coords[2], coords[3],
                coords[4], coords[5], coords[6], coords[7]));
        }
    }

    /*
     * Split cubic curve into monotonic in X and Y parts. Calling
     *
     * Note: coords array could be changed
     */
    public void SplitCubic(List<CubicCurve2D> resVect,
                                   float[] coords)
    {
        /* Temporary array for holding parameters corresponding to the extreme
         * in X and Y points
         */
        float params[] = new float[4];
        float eqn[] = new float[3];
        float res[] = new float[2];
        int cnt = 0;

        /* Simple check for monotonicity in X before searching for the extreme
         * points of the X(t) function. We first check if the curve is
         * monotonic in X by seeing if all of the X coordinates are strongly
         * ordered.
         */
        if ((coords[0] > coords[2] || coords[2] > coords[4] ||
             coords[4] > coords[6]) &&
            (coords[0] < coords[2] || coords[2] < coords[4] ||
             coords[4] < coords[6]))
        {
            /* Searching for extreme points of the X(t) function  by solving
             * dX(t)
             * ----  = 0 equation
             *  dt
             */
            eqn[2] = -coords[0] + 3*coords[2] - 3*coords[4] + coords[6];
            eqn[1] = 2*(coords[0] - 2*coords[2] + coords[4]);
            eqn[0] = -coords[0] + coords[2];

            int nr = QuadCurve2D.solveQuadratic(eqn, res);

            /* Following code also correctly works in degenerate case of
             * the quadratic equation (nr = -1) because we do not need
             * splitting in this case.
             */
            for (int i = 0; i < nr; i++) {
                if (res[i] > 0 && res[i] < 1) {
                    params[cnt++] = res[i];
                }
            }
        }

        /* Simple check for monotonicity in Y before searching for the extreme
         * points of the Y(t) function. We first check if the curve is
         * monotonic in Y by seeing if all of the Y coordinates are strongly
         * ordered.
         */
        if ((coords[1] > coords[3] || coords[3] > coords[5] ||
             coords[5] > coords[7]) &&
            (coords[1] < coords[3] || coords[3] < coords[5] ||
             coords[5] < coords[7]))
        {
            /* Searching for extreme points of the Y(t) function by solving
             * dY(t)
             * ----- = 0 equation
             *  dt
             */
            eqn[2] = -coords[1] + 3*coords[3] - 3*coords[5] + coords[7];
            eqn[1] = 2*(coords[1] - 2*coords[3] + coords[5]);
            eqn[0] = -coords[1] + coords[3];

            int nr = QuadCurve2D.solveQuadratic(eqn, res);

            /* Following code also correctly works in degenerate case of
             * the quadratic equation (nr = -1) because we do not need
             * splitting in this case.
             */
            for (int i = 0; i < nr; i++) {
                if (res[i] > 0 && res[i] < 1) {
                    params[cnt++] = res[i];
                }
            }
        }

        if (cnt > 0) {
            /* Sorting parameter values corresponding to the extreme points
             * of the curve
             */
            Arrays.sort(params, 0, cnt);

            /* Processing obtained monotonic parts */
            ProcessFirstMonotonicPartOfCubic(resVect, coords, params[0]);
            for (int i = 1; i < cnt; i++) {
                float param = params[i] - params[i-1];
                if (param > 0) {
                    ProcessFirstMonotonicPartOfCubic(resVect, coords,
                        /* Scale parameter to match with rest of the curve */
                        param / (1f - params[i - 1]));
                }
            }
        }

        ProcessMonotonicCubic(resVect,coords);
    }

    /*
     * Bite the piece of the cubic curve from start point till the point
     * corresponding to the specified parameter then call ProcessCubic for the
     * bitten part.
     * Note: coords array will be changed
     */
    private void ProcessFirstMonotonicPartOfCubic(
        List<CubicCurve2D> resVector, float[] coords, float t)
    {
        float[] coords1 = new float[8];
        float tx, ty;

        coords1[0] = coords[0];
        coords1[1] = coords[1];
        tx = coords[2] + t*(coords[4] - coords[2]);
        ty = coords[3] + t*(coords[5] - coords[3]);
        coords1[2] =  coords[0] + t*(coords[2] - coords[0]);
        coords1[3] =  coords[1] + t*(coords[3] - coords[1]);
        coords1[4] = coords1[2] + t*(tx - coords1[2]);
        coords1[5] = coords1[3] + t*(ty - coords1[3]);
        coords[4] = coords[4] + t*(coords[6] - coords[4]);
        coords[5] = coords[5] + t*(coords[7] - coords[5]);
        coords[2] = tx + t*(coords[4] - tx);
        coords[3] = ty + t*(coords[5] - ty);
        coords[0]=coords1[6]=coords1[4] + t*(coords[2] - coords1[4]);
        coords[1]=coords1[7]=coords1[5] + t*(coords[3] - coords1[5]);

        ProcessMonotonicCubic(resVector, coords1);
    }

}
