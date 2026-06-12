/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.util;

import java.util.Objects;

/**
 * Color conversion algorithms.
 */
public final class ColorConversion {

    private ColorConversion() {}

    /**
     * Converts sRGB in the range [0, 1] to linear-light RGB.
     */
    public static void srgbToLinearRgb(double[] source, int sourceOffset,
                                       double[] result, int resultOffset) {
        checkRange(source, sourceOffset, "source");
        checkRange(result, resultOffset, "result");

        result[resultOffset] = srgbChannelToLinear(source[sourceOffset]);
        result[resultOffset + 1] = srgbChannelToLinear(source[sourceOffset + 1]);
        result[resultOffset + 2] = srgbChannelToLinear(source[sourceOffset + 2]);
    }

    /**
     * Converts linear-light RGB to sRGB.
     */
    public static void linearRgbToSrgb(double[] source, int sourceOffset,
                                       double[] result, int resultOffset) {
        checkRange(source, sourceOffset, "source");
        checkRange(result, resultOffset, "result");

        result[resultOffset] = linearChannelToSrgb(source[sourceOffset]);
        result[resultOffset + 1] = linearChannelToSrgb(source[sourceOffset + 1]);
        result[resultOffset + 2] = linearChannelToSrgb(source[sourceOffset + 2]);
    }

    /**
     * Converts linear-light RGB to Lab using a D65 reference white.
     */
    public static void linearRgbToLabD65(double[] source, int sourceOffset,
                                         double[] result, int resultOffset) {
        checkRange(source, sourceOffset, "source");
        checkRange(result, resultOffset, "result");

        LINEAR_TO_XYZ.transform(source, sourceOffset, result, resultOffset);

        double fx = fLab(result[resultOffset] / D65_WHITE_X);
        double fy = fLab(result[resultOffset + 1] / D65_WHITE_Y);
        double fz = fLab(result[resultOffset + 2] / D65_WHITE_Z);

        result[resultOffset] = 116.0 * fy - 16.0;
        result[resultOffset + 1] = 500.0 * (fx - fy);
        result[resultOffset + 2] = 200.0 * (fy - fz);
    }

    /**
     * Converts Lab components using a D65 reference white to linear-light RGB.
     */
    public static void labD65ToLinearRgb(double[] source, int sourceOffset,
                                         double[] result, int resultOffset) {
        checkRange(source, sourceOffset, "source");
        checkRange(result, resultOffset, "result");

        double l = source[sourceOffset];
        double a = source[sourceOffset + 1];
        double b = source[sourceOffset + 2];

        double fy = (l + 16.0) / 116.0;
        double fx = fy + (a / 500.0);
        double fz = fy - (b / 200.0);

        result[resultOffset] = D65_WHITE_X * fInvLab(fx);
        result[resultOffset + 1] = D65_WHITE_Y * fInvLab(fy);
        result[resultOffset + 2] = D65_WHITE_Z * fInvLab(fz);

        XYZ_TO_LINEAR.transform(result, resultOffset, result, resultOffset);
    }

    /**
     * Converts sRGB in the range [0, 1] to Lab using a D65 reference white.
     */
    public static void srgbToLabD65(double[] source, int sourceOffset,
                                    double[] result, int resultOffset) {
        srgbToLinearRgb(source, sourceOffset, result, resultOffset);
        linearRgbToLabD65(result, resultOffset, result, resultOffset);
    }

    /**
     * Converts Lab components using a D65 reference white to sRGB.
     */
    public static void labD65ToSrgb(double[] source, int sourceOffset,
                                    double[] result, int resultOffset) {
        labD65ToLinearRgb(source, sourceOffset, result, resultOffset);
        linearRgbToSrgb(result, resultOffset, result, resultOffset);
    }

    private static void checkRange(double[] values, int offset, String name) {
        Objects.requireNonNull(values, name);
        Objects.checkIndex(offset + 2, values.length);
    }

    private static double srgbChannelToLinear(double value) {
        if (value <= 0.04045) {
            return value / 12.92;
        }

        return Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static double linearChannelToSrgb(double value) {
        if (value <= 0.0) {
            return 0.0;
        }

        if (value >= 1.0) {
            return 1.0;
        }

        if (value <= 0.0031308) {
            return 12.92 * value;
        }

        return 1.055 * Math.pow(value, 1.0 / 2.4) - 0.055;
    }

    private static double fLab(double t) {
        if (t > DELTA3) {
            return Math.cbrt(t);
        }

        return (t / (3.0 * DELTA2)) + (4.0 / 29.0);
    }

    private static double fInvLab(double t) {
        double t3 = t * t * t;
        if (t3 > DELTA3) {
            return t3;
        }

        return 3.0 * DELTA2 * (t - 4.0 / 29.0);
    }

    private static final double D65_WHITE_X = 0.3127 / 0.3290;
    private static final double D65_WHITE_Y = 1.0;
    private static final double D65_WHITE_Z = (1.0 - 0.3127 - 0.3290) / 0.3290;

    private static final double DELTA = 6.0 / 29.0;
    private static final double DELTA2 = DELTA * DELTA;
    private static final double DELTA3 = DELTA2 * DELTA;

    // See XYZ_to_lin_sRGB, https://www.w3.org/TR/css-color-4/#color-conversion-code
    private static final Mat3 XYZ_TO_LINEAR = new Mat3(
        12831.0 / 3959.0, -329.0 / 214.0, -1974.0 / 3959.0,
        -851781.0 / 878810.0, 1648619.0 / 878810.0, 36519.0 / 878810.0,
        705.0 / 12673.0, -2585.0 / 12673.0, 705.0 / 667.0
    );

    // See lin_sRGB_to_XYZ, https://www.w3.org/TR/css-color-4/#color-conversion-code
    private static final Mat3 LINEAR_TO_XYZ = new Mat3(
        506752.0 / 1228815.0, 87881.0 / 245763.0, 12673.0 / 70218.0,
        87098.0 / 409605.0, 175762.0 / 245763.0, 12673.0 / 175545.0,
        7918.0 / 409605.0, 87881.0 / 737289.0, 1001167.0 / 1053270.0
    );

    private record Mat3(double m00, double m01, double m02,
                        double m10, double m11, double m12,
                        double m20, double m21, double m22) {

        void transform(double[] source, int sourceOffset, double[] result, int resultOffset) {
            double x = source[sourceOffset];
            double y = source[sourceOffset + 1];
            double z = source[sourceOffset + 2];
            result[resultOffset] = m00 * x + m01 * y + m02 * z;
            result[resultOffset + 1] = m10 * x + m11 * y + m12 * z;
            result[resultOffset + 2] = m20 * x + m21 * y + m22 * z;
        }
    }
}
