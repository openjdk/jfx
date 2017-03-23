/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.animation;

import javafx.util.Duration;

import com.sun.scenario.animation.NumberTangentInterpolator;
import com.sun.scenario.animation.SplineInterpolator;

/**
 * The abstract class defines several {@code interpolate} methods, which are
 * used to calculate interpolated values. Various built-in implementations of
 * this class are offered. Applications may choose to implement their own
 * {@code Interpolator} to get custom interpolation behavior.
 * <p>
 * A custom {@code Interpolator} has to be defined in terms of a "
 * {@link #curve(double) curve()}".
 * @since JavaFX 2.0
 */
public abstract class Interpolator {

    private static final double EPSILON = 1e-12;

    /**
     * The constructor of {@code Interpolator}.
     */
    protected Interpolator() {
    }

    /**
     * Built-in interpolator that provides discrete time interpolation. The
     * return value of {@code interpolate()} is {@code endValue} only when the
     * input {@code fraction} is 1.0, and {@code startValue} otherwise.
     */
    public static final Interpolator DISCRETE = new Interpolator() {
        @Override
        protected double curve(double t) {
            return (Math.abs(t - 1.0) < EPSILON) ? 1.0 : 0.0;
        }

        @Override
        public String toString() {
            return "Interpolator.DISCRETE";
        }
    };

    /**
     * Built-in interpolator that provides linear time interpolation. The return
     * value of {@code interpolate()} is {@code startValue} + ({@code endValue}
     * - {@code startValue}) * {@code fraction}.
     */
    public static final Interpolator LINEAR = new Interpolator() {
        @Override
        protected double curve(double t) {
            return t;
        }

        @Override
        public String toString() {
            return "Interpolator.LINEAR";
        }
    };

    /*
     * Easing is calculated with the following algorithm (taken from SMIL 3.0
     * specs). The result is clamped because of possible rounding errors.
     *
     * double runRate = 1.0 / (1.0 - acceleration/2.0 - deceleration/2.0); if
     * (fraction < acceleration) { double averageRunRate = runRate * (fraction /
     * acceleration) / 2; fraction *= averageRunRate; } else if (fraction > (1.0
     * - deceleration)) { // time spent in deceleration portion double tdec =
     * fraction - (1.0 - deceleration); // proportion of tdec to total
     * deceleration time double pdec = tdec / deceleration; fraction = runRate *
     * (1.0 - ( acceleration / 2) - deceleration + tdec * (2 - pdec) / 2); }
     * else { fraction = runRate * (fraction - (acceleration / 2)); }
     */

    /**
     * Built-in interpolator instance that provides ease in/out behavior.
     * <p>
     * An ease-both interpolator will make an animation start slow, then
     * accelerate and slow down again towards the end, all in a smooth manner.
     * <p>
     * The implementation uses the algorithm for easing defined in SMIL 3.0
     * with an acceleration and deceleration factor of 0.2, respectively.
     */
    public static final Interpolator EASE_BOTH = new Interpolator() {
        @Override
        protected double curve(double t) {
            // See the SMIL 3.1 specification for details on this calculation
            // acceleration = 0.2, deceleration = 0.2
            return Interpolator.clamp((t < 0.2) ? 3.125 * t * t
                    : (t > 0.8) ? -3.125 * t * t + 6.25 * t - 2.125
                            : 1.25 * t - 0.125);
        }

        @Override
        public String toString() {
            return "Interpolator.EASE_BOTH";
        }
    };
    /**
     * Built-in interpolator instance that provides ease in behavior.
     * <p>
     * An ease-in interpolator will make an animation start slow and then
     * accelerate smoothly.
     * <p>
     * The implementation uses the algorithm for easing defined in SMIL 3.0
     * with an acceleration factor of 0.2.
     */
    public static final Interpolator EASE_IN = new Interpolator() {
        private static final double S1 = 25.0 / 9.0;
        private static final double S3 = 10.0 / 9.0;
        private static final double S4 = 1.0 / 9.0;

        @Override
        protected double curve(double t) {
            // See the SMIL 3.1 specification for details on this calculation
            // acceleration = 0.2, deceleration = 0.0
            return Interpolator.clamp((t < 0.2) ? S1 * t * t : S3 * t - S4);
        }

        @Override
        public String toString() {
            return "Interpolator.EASE_IN";
        }
    };

    /**
     * Built-in interpolator instance that provides ease out behavior.
     * <p>
     * An ease-out interpolator will make an animation slow down toward the
     * end smoothly.
     * <p>
     * The implementation uses the algorithm for easing defined in SMIL 3.0
     * with an deceleration factor of 0.2.
     */
    public static final Interpolator EASE_OUT = new Interpolator() {
        private static final double S1 = -25.0 / 9.0;
        private static final double S2 = 50.0 / 9.0;
        private static final double S3 = -16.0 / 9.0;
        private static final double S4 = 10.0 / 9.0;

        @Override
        protected double curve(double t) {
            // See the SMIL 3.1 specification for details on this calculation
            // acceleration = 0.2, deceleration = 0.0
            return Interpolator.clamp((t > 0.8) ? S1 * t * t + S2 * t + S3 : S4
                    * t);
        }

        @Override
        public String toString() {
            return "Interpolator.EASE_OUT";
        }
    };

    /**
     * Creates an {@code Interpolator}, which {@link #curve(double) curve()} is
     * shaped using the spline control points defined by ({@code x1}, {@code y1}
     * ) and ({@code x2}, {@code y2}). The anchor points of the spline are
     * implicitly defined as ({@code 0.0}, {@code 0.0}) and ({@code 1.0},
     * {@code 1.0}).
     *
     * @param x1
     *            x coordinate of the first control point
     * @param y1
     *            y coordinate of the first control point
     * @param x2
     *            x coordinate of the second control point
     * @param y2
     *            y coordinate of the second control point
     * @return A spline interpolator
     */
    public static Interpolator SPLINE(double x1, double y1, double x2, double y2) {
        return new SplineInterpolator(x1, y1, x2, y2);
    }

    /**
     * Create a tangent interpolator. A tangent interpolator allows to define
     * the behavior of an animation curve very precisely by defining the
     * tangents close to a key frame.
     *
     * A tangent interpolator defines the behavior to the left and to the right
     * of a key frame, therefore it is only useful within a {@link Timeline}.
     * If used in a {@link KeyFrame} after a KeyFrame that has different interpolator,
     * it's treated as if the out-tangent of that KeyFrame was equal to the value in the KeyFrame.
     *
     * <p>
     * <img src="doc-files/tangent_interpolator.png" alt="A tangent interpolator
     * defines the behavior to the left and to the right of a key frame,
     * therefore it is only useful within a Timeline">
     *
     * <p>
     * The parameters define the tangent of the animation curve for the in
     * tangent (before a key frame) and out tangent (after a key frame). Each
     * tangent is specified with a pair, the distance to the key frame and the
     * value of the tangent at this moment.
     * <p>
     * The interpolation then follows a bezier curve, with 2 control points defined by the specified tangent and
     * positioned at 1/3 of the duration before the second KeyFrame or after the first KeyFrame. See the picture above.
     *
     * @param t1
     *            The delta time of the in-tangent, relative to the KeyFrame
     * @param v1
     *            The value of the in-tangent
     * @param t2
     *            The delta time of the out-tangent, relative to the KeyFrame
     * @param v2
     *            The value of the out-tangent
     * @return the new tangent interpolator
     */
    public static Interpolator TANGENT(Duration t1, double v1, Duration t2,
            double v2) {
        return new NumberTangentInterpolator(t1, v1, t2, v2);
    }

    /**
     * Creates a tangent interpolator, for which in-tangent and out-tangent are
     * identical. This is especially useful for the first and the last key frame
     * of a {@link Timeline}, because for these key frames only one tangent is
     * used.
     *
     * @see #TANGENT(Duration, double, Duration, double)
     *
     * @param t
     *            The delta time of the tangent
     * @param v
     *            The value of the tangent
     * @return the new Tangent interpolator
     */
    public static Interpolator TANGENT(Duration t, double v) {
        return new NumberTangentInterpolator(t, v);
    }

    /**
     * This method takes two {@code Objects} along with a {@code fraction}
     * between {@code 0.0} and {@code 1.0} and returns the interpolated value.
     * <p>
     * If both {@code Objects} implement {@code Number}, their values are
     * interpolated. If {@code startValue} implements {@link Interpolatable} the
     * calculation defined in {@link Interpolatable#interpolate(Object, double)
     * interpolate()} is used. If neither of these conditions are met, a
     * discrete interpolation is used, i.e. {@code endValue} is returned if and
     * only if {@code fraction} is {@code 1.0}, otherwise {@code startValue} is
     * returned.
     * <p>
     * Before calculating the interpolated value, the fraction is altered
     * according to the function defined in {@link #curve(double) curve()}.
     *
     * @param startValue
     *            start value
     * @param endValue
     *            end value
     * @param fraction
     *            a value between 0.0 and 1.0
     * @return interpolated value
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object interpolate(Object startValue, Object endValue,
            double fraction) {
        if ((startValue instanceof Number) && (endValue instanceof Number)) {
            final double start = ((Number) startValue).doubleValue();
            final double end = ((Number) endValue).doubleValue();
            final double val = start + (end - start) * curve(fraction);
            if ((startValue instanceof Double) || (endValue instanceof Double)) {
                return Double.valueOf(val);
            }
            if ((startValue instanceof Float) || (endValue instanceof Float)) {
                return Float.valueOf((float) val);
            }
            if ((startValue instanceof Long) || (endValue instanceof Long)) {
                return Long.valueOf(Math.round(val));
            }
            return Integer.valueOf((int) Math.round(val));
        } else if ((startValue instanceof Interpolatable) && (endValue instanceof Interpolatable)) {
            return ((Interpolatable) startValue).interpolate(endValue,
                    curve(fraction));
        } else {
            // discrete
            return (curve(fraction) == 1.0) ? endValue : startValue;
        }
    }

    /**
     * This method takes two {@code boolean} values along with a
     * {@code fraction} between {@code 0.0} and {@code 1.0} and returns the
     * interpolated value.
     * <p>
     * Before calculating the interpolated value, the fraction is altered
     * according to the function defined in {@link #curve(double) curve()}.
     *
     * @param startValue
     *            the first data point
     * @param endValue
     *            the second data point
     * @param fraction
     *            the fraction in {@code [0.0...1.0]}
     * @return the interpolated value
     */
    public boolean interpolate(boolean startValue, boolean endValue,
            double fraction) {
        return (Math.abs(curve(fraction) - 1.0) < EPSILON) ? endValue
                : startValue;
    }

    /**
     * This method takes two {@code double} values along with a {@code fraction}
     * between {@code 0.0} and {@code 1.0} and returns the interpolated value.
     * <p>
     * Before calculating the interpolated value, the fraction is altered
     * according to the function defined in {@link #curve(double) curve()}.
     *
     * @param startValue
     *            the first data point
     * @param endValue
     *            the second data point
     * @param fraction
     *            the fraction in {@code [0.0...1.0]}
     * @return the interpolated value
     */
    public double interpolate(double startValue, double endValue,
            double fraction) {
        return startValue + (endValue - startValue) * curve(fraction);
    }

    /**
     * This method takes two {@code int} values along with a {@code fraction}
     * between {@code 0.0} and {@code 1.0} and returns the interpolated value.
     * <p>
     * Before calculating the interpolated value, the fraction is altered
     * according to the function defined in {@link #curve(double) curve()}.
     *
     * @param startValue
     *            the first data point
     * @param endValue
     *            the second data point
     * @param fraction
     *            the fraction in {@code [0.0...1.0]}
     * @return the interpolated value
     */
    public int interpolate(int startValue, int endValue, double fraction) {
        return startValue
                + (int) Math.round((endValue - startValue) * curve(fraction));
    }

    /**
     * This method takes two {@code int} values along with a {@code fraction}
     * between {@code 0.0} and {@code 1.0} and returns the interpolated value.
     * <p>
     * Before calculating the interpolated value, the fraction is altered
     * according to the function defined in {@link #curve(double) curve()}.
     *
     * @param startValue
     *            the first data point
     * @param endValue
     *            the second data point
     * @param fraction
     *            the fraction in {@code [0.0...1.0]}
     * @return the interpolated value
     */
    public long interpolate(long startValue, long endValue,
            double fraction) {
        return startValue
                + Math.round((endValue - startValue) * curve(fraction));
    }

    private static double clamp(double t) {
        return (t < 0.0) ? 0.0 : (t > 1.0) ? 1.0 : t;
    }

    /**
     * Mapping from [0.0..1.0] to itself.
     *
     * @param t
     *            time, but normalized to the range [0.0..1.0], where 0.0 is the
     *            start of the current interval, while 1.0 is the end of the
     *            current interval. Usually a function that increases
     *            monotonically.
     * @return the curved value
     */
    protected abstract double curve(double t);

}
