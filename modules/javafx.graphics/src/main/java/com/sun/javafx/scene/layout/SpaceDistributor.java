package com.sun.javafx.scene.layout;

import java.util.Objects;

/**
 * Algorithm for distributing space given min and max size restrictions.
 */
public class SpaceDistributor {
    private static final double[] EMPTY = new double[0];

    /**
     * Distributes a given amount of space according to the given minimum and maximum sizes.<p>
     *
     * If {@code renderScale} is a positive value, then all input values and the returned spaces
     * will be rounded up to the nearest pixel boundary. If it is 0, then no rounding occurs.<p>
     *
     * If the sum of the given minimum sizes exceeds the space available for distribution,
     * then the returned spaces are a copy of the minimum sizes. If the sum of the given maximum
     * sizes is smaller than the space available for distribution, then the returned spaces are
     * a copy of the maximum sizes.
     *
     * @param space an amount of space to distribute, cannot be negative
     * @param renderScale a render scale, cannot be negative
     * @param minSizes an array with minimum sizes, cannot be {@code null} and will not be modified
     * @param maxSizes an array with maximum sizes, cannot be {@code null} and will not be modified
     * @return a new array with the distributed space result, never {@code null}
     * @throws NullPointerException when any argument is {@code null}
     * @throws IllegalArgumentException when the two arrays are not the same size, a minimum size
     *   exceeds a maximum size, space was negative, or the render scale was negative
     */
    public static double[] distribute(double space, double renderScale, double[] minSizes, double[] maxSizes) {
        if (Objects.requireNonNull(minSizes, "minSizes").length != Objects.requireNonNull(maxSizes, "maxSizes").length) {
            throw new IllegalArgumentException("minSizes and maxSizes arrays must be of the same length");
        }

        if (space < 0) {
            throw new IllegalArgumentException("space cannot be negative: " + space);
        }

        if (renderScale < 0) {
            throw new IllegalArgumentException("renderScale cannot be negative: " + renderScale);
        }

        if (minSizes.length == 0) {
            return EMPTY;
        }

        double[] scaledMinSizes = ceil(minSizes, renderScale);

        double minSpaceRequired = sum(scaledMinSizes);
        double scaledSpace = ceil(space, renderScale);

        if (minSpaceRequired >= scaledSpace) {

            /*
             * Insufficient space was available to accommodate the minimum sizes, and so the
             * best that can be done is to return the (render scale adjusted) minimum sizes.
             * This means that more space is used in the returned array than was strictly
             * allowed by the given input space amount.
             */

            return scaledMinSizes;
        }

        double[] scaledMaxSizes = ceil(maxSizes, renderScale);

        double maxSpaceRequired = sum(scaledMaxSizes);

        if (maxSpaceRequired <= scaledSpace) {

            /*
             * More space was available than could possible be used given the maximum size
             * restrictions. The returned sizes will be the (render scale adjusted) maximum sizes,
             * and less space is used in the returned array than was strictly allowed by the given
             * input space amount.
             */

            return scaledMaxSizes;
        }

        distributeAvailablePreScaled(scaledSpace - minSpaceRequired, renderScale, scaledMinSizes, scaledMaxSizes);

        return scaledMinSizes;
    }

    /*
     * This method modifies the given spaces array to the correct sizes given the available space
     * and taken into account their maximum sizes. The inputs to this call are all pre-scaled.
     * The input maxSizes array is not modified.
     */
    private static void distributeAvailablePreScaled(double available, double renderScale, double[] spaces, double[] maxSizes) {
        int count = spaces.length;
        int resizables = 0;

        /*
         * Determine the amount of spaces that can be resized, while
         * checking the input values make sense:
         */

        for (int i = 0; i < count; i++) {
            if (spaces[i] > maxSizes[i]) {
                throw new IllegalArgumentException("minSizes[" + i + "] exceeded maxSizes[" + i + "]: " + spaces[i] + " > " + maxSizes[i]);
            }

            if (spaces[i] < maxSizes[i]) {
                resizables++;
            }
        }

        /*
         * Distribute available space evenly among resizable spaces:
         */

        for (;;) {
            double idealIncrease = floor(available / resizables, renderScale);
            int initialResizables = resizables;

            for (int i = 0; i < count; i++) {
                if (spaces[i] < maxSizes[i]) {
                    double s = Math.min(maxSizes[i], spaces[i] + idealIncrease);

                    available -= s - spaces[i];
                    spaces[i] = s;

                    if (s == maxSizes[i]) {
                        resizables--;
                    }
                }
            }

            if (initialResizables == resizables) {
                break;
            }
        }

        /*
         * Do a final pass to distribute left over space that could not
         * be distributed evenly to all non-maxed spaces; this left over
         * space should never exceed d * (count - 1), because if it did, the
         * previous loop should have distributed it already.
         */

        double d = 1 / renderScale; // positive infinity if pixelSize is 0, which means it will exit the final loop immediately

        assert renderScale == 0 || available < d * (count - 1) + 0.0001 : "available exceeded threshold: " + available;

        for (int i = 0; i < count; i++) {
            if (available < d) {
                break;
            }

            double s = spaces[i] + d;

            if (s <= maxSizes[i]) {
                spaces[i] = s;
                available -= d;
            }
        }

        assert available < 0.0001 : "available should be 0: " + available;
    }

    private static double[] ceil(double[] array, double s) {
        double[] ceiledArray = new double[array.length];

        for (int i = 0; i < array.length; i++) {
            ceiledArray[i] = ceil(array[i], s);
        }

        return ceiledArray;
    }

    private static double ceil(double x, double s) {
        if (s == 0) {
            return x;
        }

        return Math.ceil(x * s) / s;
    }

    private static double floor(double x, double s) {
        if (s == 0) {
            return x;
        }

        return Math.floor(x * s) / s;
    }

    private static double sum(double[] values) {
        double sum = 0;

        for (int i = 0; i < values.length; i++) {
            sum += values[i];
        }

        return sum;
    }
}
