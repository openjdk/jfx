package com.sun.javafx.scene.layout;

/**
 * Math functions which deal with rounding scaled values.
 */
public class ScaledMath {

    /**
     * The value is floored for a given scale using Math.floor.
     * When the absolute value of the given value multiplied by the
     * current scale is less than 10^15, then this method guarantees that:
     *
     * <pre>scaledFloor(scaledFloor(value, scale), scale) == scaledFloor(value, scale)</pre>
     *
     * The limit is about 10^15 because double values will no longer be able to represent
     * larger integers with exact precision beyond this limit.
     *
     * @param value The value that needs to be floored
     * @param scale The scale that will be used
     * @return value floored with scale
     */
    public static double floor(double value, double scale) {
        double d = value * scale;

        if (Double.isInfinite(d)) {  // Avoids returning NaN for high magnitude inputs
            return value;
        }

        return Math.floor(d + Math.ulp(d)) / scale;
    }

    /**
     * The value is ceiled with a given scale using Math.ceil.
     * When the absolute value of the given value multiplied by the
     * current scale is less than 10^15, then this method guarantees that:
     *
     * <pre>scaledCeil(scaledCeil(value, scale), scale) == scaledCeil(value, scale)</pre>
     *
     * The limit is about 10^15 because double values will no longer be able to represent
     * larger integers with exact precision beyond this limit.
     *
     * @param value The value that needs to be ceiled
     * @param scale The scale that will be used
     * @return value ceiled with scale
     */
    public static double ceil(double value, double scale) {
        double d = value * scale;

        if (Double.isInfinite(d)) {  // Avoids returning NaN for high magnitude inputs
            return value;
        }

        return Math.ceil(d - Math.ulp(d)) / scale;
    }

    /**
     * The value is rounded with a given scale using {@link Math#round(double)}.
     *
     * @param value The value that needs to be rounded
     * @param scale The scale that will be used
     * @return value rounded with scale
     * @deprecated uses {@link Math#round(double)} instead of {@link Math#rint(double)}, don't use in new code, use {@link #rint(double, double)}
     */
    @Deprecated
    public static double round(double value, double scale) {
        return Math.round(value * scale) / scale;
    }

    /**
     * The value is rounded with a given scale using {@link Math#rint(double)}.
     *
     * @param value The value that needs to be rounded
     * @param scale The scale that will be used
     * @return value rounded with scale
     */
    public static double rint(double value, double scale) {
        return Math.rint(value * scale) / scale;
    }
}
