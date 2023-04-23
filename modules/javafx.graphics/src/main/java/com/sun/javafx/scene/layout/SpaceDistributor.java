package com.sun.javafx.scene.layout;

import java.util.Objects;

/**
 * Algorithm for distributing space given min and max size restrictions.
 */
// TODO perhaps instantiate this class so render scale can be a field
public class SpaceDistributor {
    private static final double[] EMPTY = new double[0];

    /**
     * Adjusts the given sizes towards the given target sizes to fit a given amount of space,
     * quantizing all sizes according to the given render scale. If the sizes cannot be made
     * to fit, returns a quantized copy of either the given sizes or the target sizes, otherwise
     * returns the fitted quantized sizes.<p>
     *
     * If {@code renderScale} is a positive value, then all input values and the returned spaces
     * will be rounded up to the nearest pixel boundary (quantized). If it is 0, then no
     * rounding occurs.<p>
     *
     * Returns a copy of the current sizes if the given space is smaller and the target sizes
     * are larger that the current sizes; or vice versa, when the given space is larger and the
     * target sizes are smaller than the current sizes. This happens when the direction of
     * change (shrinking or growing) is incompatible with the given target sizes.<p>
     *
     * Returns a copy of the target sizes if both the given space and the target sizes
     * are smaller than the current sizes, and the target sizes are larger than the given space;
     * or vice versa, when both the given space and the target sizes are larger than the current
     * sizes, and the target sizes are smaller than the given space.
     *
     * @param space the size of the space in which to distribute the given sizes, cannot be negative
     * @param renderScale a render scale, cannot be negative
     * @param sizes an array with initial sizes, cannot be {@code null} and will not be modified
     * @param targetSizes an array with target sizes, cannot be {@code null} and will not be modified
     * @return a new array with the adjusted sizes, never {@code null}
     * @throws NullPointerException when any argument is {@code null}
     * @throws IllegalArgumentException when the two arrays are not the same size, a size
     *   exceeds a target size, space was negative, or the render scale was negative
     */
    public static double[] distribute(double space, double renderScale, double[] sizes, double[] targetSizes) {
        if (Objects.requireNonNull(sizes, "sizes").length != Objects.requireNonNull(targetSizes, "targetSizes").length) {
            throw new IllegalArgumentException("sizes and targetSizes arrays must be of the same length");
        }

        if (space < 0) {
            throw new IllegalArgumentException("space cannot be negative: " + space);
        }

        if (renderScale < 0) {
            throw new IllegalArgumentException("renderScale cannot be negative: " + renderScale);
        }

        if (sizes.length == 0) {
            return EMPTY;
        }

        double[] quantizedSizes = ceil(sizes, renderScale);
        double spaceRequired = round(sum(quantizedSizes), renderScale);
        double quantizedSpace = ceil(space, renderScale);
        double available = round(quantizedSpace - spaceRequired, renderScale);

        if (available == 0) {
            return quantizedSizes;
        }

        boolean grow = available > 0;
        double[] quantizedTargetSizes = ceil(targetSizes, renderScale);
        double targetSpaceRequired = round(sum(quantizedTargetSizes), renderScale);

        // if the target sizes can't be reached when growing/shrinking (it's the wrong way), then return current sizes:
        if (grow == (targetSpaceRequired < spaceRequired)) {
            return quantizedSizes;
        }

        // if the target sizes are smaller/larger than the intended space, then return the target sizes:
        if (grow == (targetSpaceRequired < quantizedSpace)) {
            return quantizedTargetSizes;
        }

        distributeAvailablePreScaled(available, renderScale, quantizedSizes, quantizedTargetSizes);

        return quantizedSizes;
    }

    /*
     * This method modifies the given spaces array to the correct sizes given the available space
     * and taken into account their maximum sizes. The input targetSizes array is not modified.
     *
     * Note: when renderScale is not zero, all input values must be on whole pixel boundaries.
     * Furthermore, all newly calculated values in this method are also rounded to pixel boundaries.
     */
    private static void distributeAvailablePreScaled(double available, double renderScale, double[] sizes, double[] targetSizes) {
        int count = sizes.length;
        int resizables = 0;
        boolean grow = available >= 0;

        /*
         * Determine the amount of spaces that can be resized, while
         * checking the input values make sense:
         */

        for (int i = 0; i < count; i++) {
            if (grow && sizes[i] > targetSizes[i]) {
                throw new IllegalArgumentException("growing: sizes[" + i + "] exceeded targetSizes[" + i + "]: " + sizes[i] + " > " + targetSizes[i]);
            }
            else if(!grow && targetSizes[i] > sizes[i]) {
                throw new IllegalArgumentException("shrinking: targetSizes[" + i + "] exceeded sizes[" + i + "]: " + targetSizes[i] + " > " + sizes[i]);
            }

            if (sizes[i] != targetSizes[i]) {
                resizables++;
            }
        }

        /*
         * Distribute available space evenly among resizable spaces:
         */

        for (;;) {
            double idealChange = down(available / resizables, renderScale);

            if (idealChange == 0) {  // exit loop when nothing is going to change
                break;
            }

            int initialResizables = resizables;

            for (int i = 0; i < count; i++) {
                double size = sizes[i];
                double targetSize = targetSizes[i];

                if ((grow && size < targetSize) || (!grow && size > targetSize)) {
                    double x = round(size + idealChange, renderScale);
                    double newSpace = grow ? Math.min(targetSize, x) : Math.max(targetSize, x);

                    available = round(available - newSpace + size, renderScale);
                    sizes[i] = newSpace;

                    if (newSpace == targetSize && --resizables == 0) {  // exit method when nothing left to resize
                        return;
                    }
                }
            }

            if (initialResizables == resizables) {  // exit loop when nothing changed
                break;
            }
        }

        if (renderScale == 0) {  // exit after distribution when not dealing with pixels
            return;
        }

        /*
         * When dealing with pixel sized chunks, a final pass is needed to distribute left over
         * space that could not be distributed evenly to all spaces that did not reach their
         * target, meaning some spaces will get or lose one extra pixel.
         */

        double pixelSize = 1 / renderScale;

        assert Math.abs(available) <= round(pixelSize * resizables, renderScale) : "available exceeded threshold: " + available + "; resizables: = " + resizables;

        for (int i = 0; i < count; i++) {
            if (Math.abs(available) < 0.5 * pixelSize) {  // exit if less than half a pixel is available
                break;
            }

            if (available < 0) {
                double s = round(sizes[i] - pixelSize, renderScale);

                if (s >= targetSizes[i]) {
                    sizes[i] = s;
                    available += pixelSize;
                }
            }
            else {
                double s = round(sizes[i] + pixelSize, renderScale);

                if (s <= targetSizes[i]) {
                    sizes[i] = s;
                    available -= pixelSize;
                }
            }
        }

        assert Math.abs(available) == 0 : "available should be 0: " + available;
    }

    // array functions:

    private static double[] ceil(double[] values, double s) {
        double[] ceiledValues = new double[values.length];

        for (int i = 0; i < values.length; i++) {
            ceiledValues[i] = ceil(values[i], s);
        }

        return ceiledValues;
    }

    private static double sum(double[] values) {
        double sum = 0;

        for (int i = 0; i < values.length; i++) {
            sum += values[i];
        }

        return sum;
    }

    // rounding functions:

    private static double down(double x, double s) {
        return x < 0 ? ceil(x, s) : floor(x, s);
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

    private static double round(double x, double s) {
        if (s == 0) {
            return x;
        }

        return Math.round(x * s) / s;
    }
}
