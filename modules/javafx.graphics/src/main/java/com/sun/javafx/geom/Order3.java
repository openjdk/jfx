/*
 * Copyright (c) 1998, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Vector;

final class Order3 extends Curve {
    private double x0;
    private double y0;
    private double cx0;
    private double cy0;
    private double cx1;
    private double cy1;
    private double x1;
    private double y1;

    private double xmin;
    private double xmax;

    private double xcoeff0;
    private double xcoeff1;
    private double xcoeff2;
    private double xcoeff3;

    private double ycoeff0;
    private double ycoeff1;
    private double ycoeff2;
    private double ycoeff3;

    public static void insert(Vector curves, double tmp[],
                              double x0, double y0,
                              double cx0, double cy0,
                              double cx1, double cy1,
                              double x1, double y1,
                              int direction)
    {
        int numparams = getHorizontalParams(y0, cy0, cy1, y1, tmp);
        if (numparams == 0) {
            // We are using addInstance here to avoid inserting horisontal
            // segments
            addInstance(curves, x0, y0, cx0, cy0, cx1, cy1, x1, y1, direction);
            return;
        }
        // Store coordinates for splitting at tmp[3..10]
        tmp[3] = x0;  tmp[4]  = y0;
        tmp[5] = cx0; tmp[6]  = cy0;
        tmp[7] = cx1; tmp[8]  = cy1;
        tmp[9] = x1;  tmp[10] = y1;
        double t = tmp[0];
        if (numparams > 1 && t > tmp[1]) {
            // Perform a "2 element sort"...
            tmp[0] = tmp[1];
            tmp[1] = t;
            t = tmp[0];
        }
        split(tmp, 3, t);
        if (numparams > 1) {
            // Recalculate tmp[1] relative to the range [tmp[0]...1]
            t = (tmp[1] - t) / (1 - t);
            split(tmp, 9, t);
        }
        int index = 3;
        if (direction == DECREASING) {
            index += numparams * 6;
        }
        while (numparams >= 0) {
            addInstance(curves,
                        tmp[index + 0], tmp[index + 1],
                        tmp[index + 2], tmp[index + 3],
                        tmp[index + 4], tmp[index + 5],
                        tmp[index + 6], tmp[index + 7],
                        direction);
            numparams--;
            if (direction == INCREASING) {
                index += 6;
            } else {
                index -= 6;
            }
        }
    }

    public static void addInstance(Vector curves,
                                   double x0, double y0,
                                   double cx0, double cy0,
                                   double cx1, double cy1,
                                   double x1, double y1,
                                   int direction) {
        if (y0 > y1) {
            curves.add(new Order3(x1, y1, cx1, cy1, cx0, cy0, x0, y0,
                                  -direction));
        } else if (y1 > y0) {
            curves.add(new Order3(x0, y0, cx0, cy0, cx1, cy1, x1, y1,
                                  direction));
        }
    }

    /**
     * [A double version of what is in QuadCurve2D...]
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
    public static int solveQuadratic(double eqn[], double res[]) {
        double a = eqn[2];
        double b = eqn[1];
        double c = eqn[0];
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
            double d = b * b - 4f * a * c;
            if (d < 0f) {
                // If d < 0.0, then there are no roots
                return 0;
            }
            d = Math.sqrt(d);
            // For accuracy, calculate one root using:
            //     (-b +/- d) / 2a
            // and the other using:
            //     2c / (-b +/- d)
            // Choose the sign of the +/- so that b+d gets larger in magnitude
            if (b < 0f) {
                d = -d;
            }
            double q = (b + d) / -2f;
            // We already tested a for being 0 above
            res[roots++] = q / a;
            if (q != 0f) {
                res[roots++] = c / q;
            }
        }
        return roots;
    }

    /*
     * Return the count of the number of horizontal sections of the
     * specified cubic Bezier curve.  Put the parameters for the
     * horizontal sections into the specified <code>ret</code> array.
     * <p>
     * If we examine the parametric equation in t, we have:
     *   Py(t) = C0(1-t)^3 + 3CP0 t(1-t)^2 + 3CP1 t^2(1-t) + C1 t^3
     *         = C0 - 3C0t + 3C0t^2 - C0t^3 +
     *           3CP0t - 6CP0t^2 + 3CP0t^3 +
     *           3CP1t^2 - 3CP1t^3 +
     *           C1t^3
     *   Py(t) = (C1 - 3CP1 + 3CP0 - C0) t^3 +
     *           (3C0 - 6CP0 + 3CP1) t^2 +
     *           (3CP0 - 3C0) t +
     *           (C0)
     * If we take the derivative, we get:
     *   Py(t) = Dt^3 + At^2 + Bt + C
     *   dPy(t) = 3Dt^2 + 2At + B = 0
     *        0 = 3*(C1 - 3*CP1 + 3*CP0 - C0)t^2
     *          + 2*(3*CP1 - 6*CP0 + 3*C0)t
     *          + (3*CP0 - 3*C0)
     *        0 = 3*(C1 - 3*CP1 + 3*CP0 - C0)t^2
     *          + 3*2*(CP1 - 2*CP0 + C0)t
     *          + 3*(CP0 - C0)
     *        0 = (C1 - CP1 - CP1 - CP1 + CP0 + CP0 + CP0 - C0)t^2
     *          + 2*(CP1 - CP0 - CP0 + C0)t
     *          + (CP0 - C0)
     *        0 = (C1 - CP1 + CP0 - CP1 + CP0 - CP1 + CP0 - C0)t^2
     *          + 2*(CP1 - CP0 - CP0 + C0)t
     *          + (CP0 - C0)
     *        0 = ((C1 - CP1) - (CP1 - CP0) - (CP1 - CP0) + (CP0 - C0))t^2
     *          + 2*((CP1 - CP0) - (CP0 - C0))t
     *          + (CP0 - C0)
     * Note that this method will return 0 if the equation is a line,
     * which is either always horizontal or never horizontal.
     * Completely horizontal curves need to be eliminated by other
     * means outside of this method.
     */
    public static int getHorizontalParams(double c0, double cp0,
                                          double cp1, double c1,
                                          double ret[]) {
        if (c0 <= cp0 && cp0 <= cp1 && cp1 <= c1) {
            return 0;
        }
        c1 -= cp1;
        cp1 -= cp0;
        cp0 -= c0;
        ret[0] = cp0;
        ret[1] = (cp1 - cp0) * 2;
        ret[2] = (c1 - cp1 - cp1 + cp0);
        int numroots = solveQuadratic(ret, ret);
        int j = 0;
        for (int i = 0; i < numroots; i++) {
            double t = ret[i];
            // No splits at t==0 and t==1
            if (t > 0 && t < 1) {
                if (j < i) {
                    ret[j] = t;
                }
                j++;
            }
        }
        return j;
    }

    /*
     * Split the cubic Bezier stored at coords[pos...pos+7] representing
     * the parametric range [0..1] into two subcurves representing the
     * parametric subranges [0..t] and [t..1].  Store the results back
     * into the array at coords[pos...pos+7] and coords[pos+6...pos+13].
     */
    public static void split(double coords[], int pos, double t) {
        double x0, y0, cx0, cy0, cx1, cy1, x1, y1;
        coords[pos+12] = x1 = coords[pos+6];
        coords[pos+13] = y1 = coords[pos+7];
        cx1 = coords[pos+4];
        cy1 = coords[pos+5];
        x1 = cx1 + (x1 - cx1) * t;
        y1 = cy1 + (y1 - cy1) * t;
        x0 = coords[pos+0];
        y0 = coords[pos+1];
        cx0 = coords[pos+2];
        cy0 = coords[pos+3];
        x0 = x0 + (cx0 - x0) * t;
        y0 = y0 + (cy0 - y0) * t;
        cx0 = cx0 + (cx1 - cx0) * t;
        cy0 = cy0 + (cy1 - cy0) * t;
        cx1 = cx0 + (x1 - cx0) * t;
        cy1 = cy0 + (y1 - cy0) * t;
        cx0 = x0 + (cx0 - x0) * t;
        cy0 = y0 + (cy0 - y0) * t;
        coords[pos+2] = x0;
        coords[pos+3] = y0;
        coords[pos+4] = cx0;
        coords[pos+5] = cy0;
        coords[pos+6] = cx0 + (cx1 - cx0) * t;
        coords[pos+7] = cy0 + (cy1 - cy0) * t;
        coords[pos+8] = cx1;
        coords[pos+9] = cy1;
        coords[pos+10] = x1;
        coords[pos+11] = y1;
    }

    public Order3(double x0, double y0,
                  double cx0, double cy0,
                  double cx1, double cy1,
                  double x1, double y1,
                  int direction)
    {
        super(direction);
        // REMIND: Better accuracy in the root finding methods would
        //  ensure that cys are in range.  As it stands, they are never
        //  more than "1 mantissa bit" out of range...
        if (cy0 < y0) cy0 = y0;
        if (cy1 > y1) cy1 = y1;
        this.x0 = x0;
        this.y0 = y0;
        this.cx0 = cx0;
        this.cy0 = cy0;
        this.cx1 = cx1;
        this.cy1 = cy1;
        this.x1 = x1;
        this.y1 = y1;
        xmin = Math.min(Math.min(x0, x1), Math.min(cx0, cx1));
        xmax = Math.max(Math.max(x0, x1), Math.max(cx0, cx1));
        xcoeff0 = x0;
        xcoeff1 = (cx0 - x0) * 3.0;
        xcoeff2 = (cx1 - cx0 - cx0 + x0) * 3.0;
        xcoeff3 = x1 - (cx1 - cx0) * 3.0 - x0;
        ycoeff0 = y0;
        ycoeff1 = (cy0 - y0) * 3.0;
        ycoeff2 = (cy1 - cy0 - cy0 + y0) * 3.0;
        ycoeff3 = y1 - (cy1 - cy0) * 3.0 - y0;
        YforT1 = YforT2 = YforT3 = y0;
    }

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public double getXTop() {
        return x0;
    }

    @Override
    public double getYTop() {
        return y0;
    }

    @Override
    public double getXBot() {
        return x1;
    }

    @Override
    public double getYBot() {
        return y1;
    }

    @Override
    public double getXMin() {
        return xmin;
    }

    @Override
    public double getXMax() {
        return xmax;
    }

    @Override
    public double getX0() {
        return (direction == INCREASING) ? x0 : x1;
    }

    @Override
    public double getY0() {
        return (direction == INCREASING) ? y0 : y1;
    }

    public double getCX0() {
        return (direction == INCREASING) ? cx0 : cx1;
    }

    public double getCY0() {
        return (direction == INCREASING) ? cy0 : cy1;
    }

    public double getCX1() {
        return (direction == DECREASING) ? cx0 : cx1;
    }

    public double getCY1() {
        return (direction == DECREASING) ? cy0 : cy1;
    }

    @Override
    public double getX1() {
        return (direction == DECREASING) ? x0 : x1;
    }

    @Override
    public double getY1() {
        return (direction == DECREASING) ? y0 : y1;
    }

    private double TforY1;
    private double YforT1;
    private double TforY2;
    private double YforT2;
    private double TforY3;
    private double YforT3;

    /*
     * Solve the cubic whose coefficients are in the a,b,c,d fields and
     * return the first root in the range [0, 1].
     * The cubic solved is represented by the equation:
     *     x^3 + (ycoeff2)x^2 + (ycoeff1)x + (ycoeff0) = y
     * @return the first valid root (in the range [0, 1])
     */
    @Override
    public double TforY(double y) {
        if (y <= y0) return 0;
        if (y >= y1) return 1;
        if (y == YforT1) return TforY1;
        if (y == YforT2) return TforY2;
        if (y == YforT3) return TforY3;
        // From Numerical Recipes, 5.6, Quadratic and Cubic Equations
        if (ycoeff3 == 0.0) {
            // The cubic degenerated to quadratic (or line or ...).
            return Order2.TforY(y, ycoeff0, ycoeff1, ycoeff2);
        }
        double a = ycoeff2 / ycoeff3;
        double b = ycoeff1 / ycoeff3;
        double c = (ycoeff0 - y) / ycoeff3;
        int roots = 0;
        double Q = (a * a - 3.0 * b) / 9.0;
        double R = (2.0 * a * a * a - 9.0 * a * b + 27.0 * c) / 54.0;
        double R2 = R * R;
        double Q3 = Q * Q * Q;
        double a_3 = a / 3.0;
        double t;
        if (R2 < Q3) {
            double theta = Math.acos(R / Math.sqrt(Q3));
            Q = -2.0 * Math.sqrt(Q);
            t = refine(a, b, c, y, Q * Math.cos(theta / 3.0) - a_3);
            if (t < 0) {
                t = refine(a, b, c, y,
                           Q * Math.cos((theta + Math.PI * 2.0)/ 3.0) - a_3);
            }
            if (t < 0) {
                t = refine(a, b, c, y,
                           Q * Math.cos((theta - Math.PI * 2.0)/ 3.0) - a_3);
            }
        } else {
            boolean neg = (R < 0.0);
            double S = Math.sqrt(R2 - Q3);
            if (neg) {
                R = -R;
            }
            double A = Math.pow(R + S, 1.0 / 3.0);
            if (!neg) {
                A = -A;
            }
            double B = (A == 0.0) ? 0.0 : (Q / A);
            t = refine(a, b, c, y, (A + B) - a_3);
        }
        if (t < 0) {
            //throw new InternalError("bad t");
            double t0 = 0;
            double t1 = 1;
            while (true) {
                t = (t0 + t1) / 2;
                if (t == t0 || t == t1) {
                    break;
                }
                double yt = YforT(t);
                if (yt < y) {
                    t0 = t;
                } else if (yt > y) {
                    t1 = t;
                } else {
                    break;
                }
            }
        }
        if (t >= 0) {
            TforY3 = TforY2;
            YforT3 = YforT2;
            TforY2 = TforY1;
            YforT2 = YforT1;
            TforY1 = t;
            YforT1 = y;
        }
        return t;
    }

    public double refine(double a, double b, double c,
                         double target, double t)
    {
        if (t < -0.1 || t > 1.1) {
            return -1;
        }
        double y = YforT(t);
        double t0, t1;
        if (y < target) {
            t0 = t;
            t1 = 1;
        } else {
            t0 = 0;
            t1 = t;
        }
        double origt = t;
        double origy = y;
        boolean useslope = true;
        while (y != target) {
            if (!useslope) {
                double t2 = (t0 + t1) / 2;
                if (t2 == t0 || t2 == t1) {
                    break;
                }
                t = t2;
            } else {
                double slope = dYforT(t, 1);
                if (slope == 0) {
                    useslope = false;
                    continue;
                }
                double t2 = t + ((target - y) / slope);
                if (t2 == t || t2 <= t0 || t2 >= t1) {
                    useslope = false;
                    continue;
                }
                t = t2;
            }
            y = YforT(t);
            if (y < target) {
                t0 = t;
            } else if (y > target) {
                t1 = t;
            } else {
                break;
            }
        }
        boolean verbose = false;
        if (false && t >= 0 && t <= 1) {
            y = YforT(t);
            long tdiff = diffbits(t, origt);
            long ydiff = diffbits(y, origy);
            long yerr = diffbits(y, target);
            if (yerr > 0 || (verbose && tdiff > 0)) {
                System.out.println("target was y = "+target);
                System.out.println("original was y = "+origy+", t = "+origt);
                System.out.println("final was y = "+y+", t = "+t);
                System.out.println("t diff is "+tdiff);
                System.out.println("y diff is "+ydiff);
                System.out.println("y error is "+yerr);
                double tlow = prev(t);
                double ylow = YforT(tlow);
                double thi = next(t);
                double yhi = YforT(thi);
                if (Math.abs(target - ylow) < Math.abs(target - y) ||
                    Math.abs(target - yhi) < Math.abs(target - y))
                {
                    System.out.println("adjacent y's = ["+ylow+", "+yhi+"]");
                }
            }
        }
        return (t > 1) ? -1 : t;
    }

    @Override
    public double XforY(double y) {
        if (y <= y0) {
            return x0;
        }
        if (y >= y1) {
            return x1;
        }
        return XforT(TforY(y));
    }

    @Override
    public double XforT(double t) {
        return (((xcoeff3 * t) + xcoeff2) * t + xcoeff1) * t + xcoeff0;
    }

    @Override
    public double YforT(double t) {
        return (((ycoeff3 * t) + ycoeff2) * t + ycoeff1) * t + ycoeff0;
    }

    @Override
    public double dXforT(double t, int deriv) {
        switch (deriv) {
        case 0:
            return (((xcoeff3 * t) + xcoeff2) * t + xcoeff1) * t + xcoeff0;
        case 1:
            return ((3 * xcoeff3 * t) + 2 * xcoeff2) * t + xcoeff1;
        case 2:
            return (6 * xcoeff3 * t) + 2 * xcoeff2;
        case 3:
            return 6 * xcoeff3;
        default:
            return 0;
        }
    }

    @Override
    public double dYforT(double t, int deriv) {
        switch (deriv) {
        case 0:
            return (((ycoeff3 * t) + ycoeff2) * t + ycoeff1) * t + ycoeff0;
        case 1:
            return ((3 * ycoeff3 * t) + 2 * ycoeff2) * t + ycoeff1;
        case 2:
            return (6 * ycoeff3 * t) + 2 * ycoeff2;
        case 3:
            return 6 * ycoeff3;
        default:
            return 0;
        }
    }

    @Override
    public double nextVertical(double t0, double t1) {
        double eqn[] = {xcoeff1, 2 * xcoeff2, 3 * xcoeff3};
        int numroots = solveQuadratic(eqn, eqn);
        for (int i = 0; i < numroots; i++) {
            if (eqn[i] > t0 && eqn[i] < t1) {
                t1 = eqn[i];
            }
        }
        return t1;
    }

    @Override
    public void enlarge(RectBounds r) {
        r.add((float) x0, (float) y0);
        double eqn[] = {xcoeff1, 2 * xcoeff2, 3 * xcoeff3};
        int numroots = solveQuadratic(eqn, eqn);
        for (int i = 0; i < numroots; i++) {
            double t = eqn[i];
            if (t > 0 && t < 1) {
                r.add((float) XforT(t), (float) YforT(t));
            }
        }
        r.add((float) x1, (float) y1);
    }

    @Override
    public Curve getSubCurve(double ystart, double yend, int dir) {
        if (ystart <= y0 && yend >= y1) {
            return getWithDirection(dir);
        }
        double eqn[] = new double[14];
        double t0, t1;
        t0 = TforY(ystart);
        t1 = TforY(yend);
        eqn[0] = x0;
        eqn[1] = y0;
        eqn[2] = cx0;
        eqn[3] = cy0;
        eqn[4] = cx1;
        eqn[5] = cy1;
        eqn[6] = x1;
        eqn[7] = y1;
        if (t0 > t1) {
            /* This happens in only rare cases where ystart is
             * very near yend and solving for the yend root ends
             * up stepping slightly lower in t than solving for
             * the ystart root.
             * Ideally we might want to skip this tiny little
             * segment and just modify the surrounding coordinates
             * to bridge the gap left behind, but there is no way
             * to do that from here.  Higher levels could
             * potentially eliminate these tiny "fixup" segments,
             * but not without a lot of extra work on the code that
             * coalesces chains of curves into subpaths.  The
             * simplest solution for now is to just reorder the t
             * values and chop out a miniscule curve piece.
             */
            double t = t0;
            t0 = t1;
            t1 = t;
        }
        if (t1 < 1) {
            split(eqn, 0, t1);
        }
        int i;
        if (t0 <= 0) {
            i = 0;
        } else {
            split(eqn, 0, t0 / t1);
            i = 6;
        }
        return new Order3(eqn[i+0], ystart,
                          eqn[i+2], eqn[i+3],
                          eqn[i+4], eqn[i+5],
                          eqn[i+6], yend,
                          dir);
    }

    @Override
    public Curve getReversedCurve() {
        return new Order3(x0, y0, cx0, cy0, cx1, cy1, x1, y1, -direction);
    }

    @Override
    public int getSegment(float coords[]) {
        if (direction == INCREASING) {
            coords[0] = (float) cx0;
            coords[1] = (float) cy0;
            coords[2] = (float) cx1;
            coords[3] = (float) cy1;
            coords[4] = (float) x1;
            coords[5] = (float) y1;
        } else {
            coords[0] = (float) cx1;
            coords[1] = (float) cy1;
            coords[2] = (float) cx0;
            coords[3] = (float) cy0;
            coords[4] = (float) x0;
            coords[5] = (float) y0;
        }
        return PathIterator.SEG_CUBICTO;
    }

    @Override
    public String controlPointString() {
        return (("("+round(getCX0())+", "+round(getCY0())+"), ")+
                ("("+round(getCX1())+", "+round(getCY1())+"), "));
    }
}
