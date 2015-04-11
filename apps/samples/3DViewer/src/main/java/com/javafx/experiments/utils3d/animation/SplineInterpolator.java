/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.javafx.experiments.utils3d.animation;

import javafx.animation.Interpolator;

/**
 * An implementation of a spline interpolator for temporal interpolation that
 * tries to follow the specification referenced by:
 * http://www.w3.org/TR/SMIL/animation.html#animationNS-OverviewSpline .
 * <p>
 * Basically, a cubic Bezier curve is created with start point (0,0) and
 * endpoint (1,1). The other two control points (px1, py1) and (px2, py2) are
 * given by the user, where px1, py1, px1, and px2 are all in the range [0,1]. A
 * property of this specially constrained Bezier curve is that it is strictly
 * monotonically increasing in both X and Y with t in range [0,1].
 * <p>
 * The interpolator works by giving it a value for X. It then finds what
 * parameter t would generate this X value for the curve. Then this t parameter
 * is applied to the curve to solve for Y. As X increases from 0 to 1, t also
 * increases from 0 to 1, and correspondingly Y increases from 0 to 1. The
 * X-to-Y mapping is not a function of path/curve length.
 */
public class SplineInterpolator extends Interpolator {

    /**
     * The coordinates of the 2 2D control points for a cubic Bezier curve, with
     * implicit start point (0,0) and end point (1,1) -- each individual
     * coordinate value must be in range [0,1].
     */
    private final double x1, y1, x2, y2;

    /**
     * Do the input control points form a line with (0,0) and (1,1), i.e., x1 ==
     * y1 and x2 == y2 -- if so, then all x(t) == y(t) for the curve.
     */
    private final boolean isCurveLinear;

    /**
     * Power of 2 sample size for lookup table of x values.
     */
    private static final int SAMPLE_SIZE = 16;

    /**
     * Difference in t used to calculate each of the xSamples values -- power of
     * 2 sample size should provide exact representation of this value and its
     * integer multiples (integer in range of [0..SAMPLE_SIZE].
     */
    private static final double SAMPLE_INCREMENT = 1.0 / SAMPLE_SIZE;

    /**
     * X values for the bezier curve, sampled at increments of 1/SAMPLE_SIZE --
     * this is used to find the good initial guess for parameter t, given an x.
     */
    private final double[] xSamples = new double[SAMPLE_SIZE + 1];

    /**
     * Creates a new instance with control points (0,0) (px1,py1) (px2,py2)
     * (1,1) -- px1, py1, px2, py2 all in range [0,1].
     *
     * @param px1 X coordinate of first control point, in range [0,1]
     * @param py1 Y coordinate of first control point, in range [0,1]
     * @param px2 X coordinate of second control point, in range [0,1]
     * @param py2 Y coordinate of second control point, in range [0,1]
     */
    public SplineInterpolator(double px1, double py1, double px2, double py2) {
        // check user input for precondition
        if (px1 < 0 || px1 > 1 || py1 < 0 || py1 > 1 || px2 < 0 || px2 > 1
                || py2 < 0 || py2 > 1) {
            throw new IllegalArgumentException(
                    "Control point coordinates must " + "all be in range [0,1]");
        }

        // save control point data
        this.x1 = px1;
        this.y1 = py1;
        this.x2 = px2;
        this.y2 = py2;

        // calc linearity/identity curve
        isCurveLinear = ((x1 == y1) && (x2 == y2));

        // make the array of x value samples
        if (!isCurveLinear) {
            for (int i = 0; i < SAMPLE_SIZE + 1; ++i) {
                xSamples[i] = eval(i * SAMPLE_INCREMENT, x1, x2);
            }
        }
    }

    public double getX1() {
        return x1;
    }

    public double getY1() {
        return y1;
    }

    public double getX2() {
        return x2;
    }

    public double getY2() {
        return y2;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + (int) (Double.doubleToLongBits(this.x1) ^ (Double.doubleToLongBits(this.x1) >>> 32));
        hash = 19 * hash + (int) (Double.doubleToLongBits(this.y1) ^ (Double.doubleToLongBits(this.y1) >>> 32));
        hash = 19 * hash + (int) (Double.doubleToLongBits(this.x2) ^ (Double.doubleToLongBits(this.x2) >>> 32));
        hash = 19 * hash + (int) (Double.doubleToLongBits(this.y2) ^ (Double.doubleToLongBits(this.y2) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SplineInterpolator other = (SplineInterpolator) obj;
        if (Double.doubleToLongBits(this.x1) != Double.doubleToLongBits(other.x1)) {
            return false;
        }
        if (Double.doubleToLongBits(this.y1) != Double.doubleToLongBits(other.y1)) {
            return false;
        }
        if (Double.doubleToLongBits(this.x2) != Double.doubleToLongBits(other.x2)) {
            return false;
        }
        if (Double.doubleToLongBits(this.y2) != Double.doubleToLongBits(other.y2)) {
            return false;
        }
        return true;
    }

    /**
     * Returns the y-value of the cubic bezier curve that corresponds to the x
     * input.
     *
     * @param x is x-value of cubic bezier curve, in range [0,1]
     * @return corresponding y-value of cubic bezier curve -- in range [0,1]
     */
    @Override
    public double curve(double x) {
        // check user input for precondition
        if (x < 0 || x > 1) {
            throw new IllegalArgumentException("x must be in range [0,1]");
        }

        // check quick exit identity cases (linear curve or curve endpoints)
        if (isCurveLinear || x == 0 || x == 1) {
            return x;
        }

        // find the t parameter for a given x value, and use this t to calculate
        // the corresponding y value
        return eval(findTForX(x), y1, y2);
    }

    /**
     * Use Bernstein basis to evaluate 1D cubic Bezier curve (quicker and more
     * numerically stable than power basis) -- 1D control coordinates are (0,
     * p1, p2, 1), where p1 and p2 are in range [0,1], and there is no ordering
     * constraint on p1 and p2, i.e., p1 &lt;= p2 does not have to be true.
     *
     * @param t  is the paramaterized value in range [0,1]
     * @param p1 is 1st control point coordinate in range [0,1]
     * @param p2 is 2nd control point coordinate in range [0,1]
     * @return the value of the Bezier curve at parameter t
     */
    private double eval(double t, double p1, double p2) {
        // Use optimized version of the normal Bernstein basis form of Bezier:
        // (3*(1-t)*(1-t)*t*p1)+(3*(1-t)*t*t*p2)+(t*t*t), since p0=0, p3=1.
        // The above unoptimized version is best using -server, but since we
        // are probably doing client-side animation, this is faster.
        double compT = 1 - t;
        return t * (3 * compT * (compT * p1 + t * p2) + (t * t));
    }

    /**
     * Evaluates Bernstein basis derivative of 1D cubic Bezier curve, where 1D
     * control points are (0, p1, p2, 1), where p1 and p2 are in range [0,1],
     * and there is no ordering constraint on p1 and p2, i.e., p1 &lt;= p2 does
     * not have to be true.
     *
     * @param t  is the paramaterized value in range [0,1]
     * @param p1 is 1st control point coordinate in range [0,1]
     * @param p2 is 2nd control point coordinate in range [0,1]
     * @return the value of the Bezier curve at parameter t
     */
    private double evalDerivative(double t, double p1, double p2) {
        // use optimized version of Berstein basis Bezier derivative:
        // (3*(1-t)*(1-t)*p1)+(6*(1-t)*t*(p2-p1))+(3*t*t*(1-p2)), since
        // p0=0 and p3=1. The above unoptimized version is best using -server,
        // but since we are probably doing client-side animation, this is
        // faster.
        double compT = 1 - t;
        return 3 * (compT * (compT * p1 + 2 * t * (p2 - p1)) + t * t * (1 - p2));
    }

    /**
     * Find an initial good guess for what parameter t might produce the x-value
     * on the Bezier curve -- uses linear interpolation on the x-value sample
     * array that was created on construction.
     *
     * @param x is x-value of cubic bezier curve, in range [0,1]
     * @return a good initial guess for parameter t (in range [0,1]) that gives
     * x
     */
    private double getInitialGuessForT(double x) {
        // find which places in the array that x would be sandwiched between,
        // and then linearly interpolate a reasonable value of t -- array values
        // are ascending (or at least never descending) -- binary search is
        // probably more trouble than it is worth here
        for (int i = 1; i < SAMPLE_SIZE + 1; ++i) {
            if (xSamples[i] >= x) {
                double xRange = xSamples[i] - xSamples[i - 1];
                if (xRange == 0) {
                    // no change in value between samples, so use earlier time
                    return (i - 1) * SAMPLE_INCREMENT;
                } else {
                    // linearly interpolate the time value
                    return ((i - 1) + ((x - xSamples[i - 1]) / xRange))
                            * SAMPLE_INCREMENT;
                }
            }
        }

        // shouldn't get here since 0 <= x <= 1, and xSamples[0] == 0 and
        // xSamples[SAMPLE_SIZE] == 1 (using power of 2 SAMPLE_SIZE for more
        // exact increment arithmetic)
        return 1;
    }

    /**
     * Finds the parameter t that produces the given x-value for the curve --
     * uses Newton-Raphson to refine the value as opposed to subdividing until
     * we are within some tolerance.
     *
     * @param x is x-value of cubic bezier curve, in range [0,1]
     * @return the parameter t (in range [0,1]) that produces x
     */
    private double findTForX(double x) {
        // get an initial good guess for t
        double t = getInitialGuessForT(x);

        // use Newton-Raphson to refine the value for t -- for this constrained
        // Bezier with float accuracy (7 digits), any value not converged by 4
        // iterations is cycling between values, which can minutely affect the
        // accuracy of the last digit
        final int numIterations = 4;
        for (int i = 0; i < numIterations; ++i) {
            // stop if this value of t gives us exactly x
            double xT = (eval(t, x1, x2) - x);
            if (xT == 0) {
                break;
            }

            // stop if derivative is 0
            double dXdT = evalDerivative(t, x1, x2);
            if (dXdT == 0) {
                break;
            }

            // refine t
            t -= xT / dXdT;
        }

        return t;
    }

    @Override
    public String toString() {
        return "SplineInterpolator [x1=" + x1 + ", y1=" + y1 + ", x2=" + x2
                + ", y2=" + y2 + "]";
    }

}
