package test.javafx.animation;

import org.junit.jupiter.api.Assertions;
import javafx.animation.Interpolator;
import javafx.animation.StepPosition;

import static javafx.animation.Interpolator.SPLINE;

public class InterpolatorUtils {

    // https://www.w3.org/TR/css-easing-1/#the-linear-easing-function
    public static final Interpolator LINEAR = Interpolator.LINEAR;

    // https://www.w3.org/TR/css-easing-1/#cubic-bezier-easing-functions
    public static final Interpolator EASE = SPLINE(0.25, 0.1, 0.25, 1);
    public static final Interpolator EASE_IN = SPLINE(0.42, 0, 1, 1);
    public static final Interpolator EASE_OUT = SPLINE(0, 0, 0.58, 1);
    public static final Interpolator EASE_IN_OUT = SPLINE(0.42, 0, 0.58, 1);
    public static Interpolator CUBIC_BEZIER(double a, double b, double c, double d) {
        return Interpolator.SPLINE(a, b, c, d);
    }

    // https://www.w3.org/TR/css-easing-1/#step-easing-functions
    public static final Interpolator STEP_START = Interpolator.STEP_START;
    public static final Interpolator STEP_END = Interpolator.STEP_END;
    public static Interpolator STEPS(int intervals, StepPosition position) {
        return Interpolator.STEPS(intervals, position);
    }

    /**
     * Asserts that both interpolators are equal by sampling their outputs.
     */
    public static void assertInterpolatorEquals(Interpolator expected, Interpolator actual) {
        Assertions.assertTrue(equals(expected, actual), "Interpolators do not produce equal outputs");
    }

    /**
     * Determines whether two interpolators are equal by sampling their outputs.
     */
    public static boolean equals(Interpolator int1, Interpolator int2) {
        final int numSamples = 16;

        for (int i = 0; i < numSamples; ++i) {
            double d1 = int1.interpolate(0D, 1D, (double)i / numSamples);
            double d2 = int2.interpolate(0D, 1D, (double)i / numSamples);
            if (Math.abs(d2 - d1) > 0.001) {
                return false;
            }
        }

        return true;
    }

}
