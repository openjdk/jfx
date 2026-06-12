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

package test.com.sun.javafx.util;

import com.sun.javafx.util.ColorConversion;
import org.junit.jupiter.api.Test;

import static com.sun.javafx.util.ColorConversion.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ColorConversionTest {

    @Test
    void rejectNullArrays() {
        double[] values = new double[3];
        assertThrows(NullPointerException.class, () -> srgbToLinearRgb(null, 0, values, 0));
        assertThrows(NullPointerException.class, () -> srgbToLinearRgb(values, 0, null, 0));
    }

    @Test
    void rejectInsufficientSourceOrDestinationRange() {
        double[] source = new double[3];
        double[] result = new double[3];
        assertThrows(IndexOutOfBoundsException.class, () -> srgbToLinearRgb(source, 1, result, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> srgbToLinearRgb(source, 0, result, 1));
    }

    @Test
    void srgbRoundTripPreservesColor() {
        double[] srgb = {0.25, 0.5, 0.75};
        double[] linear = new double[3];
        double[] result = new double[3];

        srgbToLinearRgb(srgb, 0, linear, 0);
        linearRgbToSrgb(linear, 0, result, 0);

        assertArrayEquals(srgb, result, 1e-12);
    }

    @Test
    void labRoundTripPreservesColor() {
        double[] srgb = {0.25, 0.5, 0.75};
        double[] lab = new double[3];
        double[] result = new double[3];

        srgbToLabD65(srgb, 0, lab, 0);
        labD65ToSrgb(lab, 0, result, 0);
        assertArrayEquals(srgb, result, 1e-12);
    }

    @Test
    void srgbToLinearRgbMatchesTransferFunctionBreakpoint() {
        double[] srgb = {0.04045, 0.04045, 0.04045};
        double[] result = new double[3];

        srgbToLinearRgb(srgb, 0, result, 0);
        assertArrayEquals(new double[] {0.003130804, 0.003130804, 0.003130804}, result, 1e-9);
    }

    @Test
    void linearRgbToSrgbMatchesTransferFunctionBreakpoint() {
        double[] linear = {0.0031308, 0.0031308, 0.0031308};
        double[] result = new double[3];

        linearRgbToSrgb(linear, 0, result, 0);
        assertArrayEquals(new double[] {0.040449936, 0.040449936, 0.040449936}, result, 1e-9);
    }

    @Test
    void srgbToLabD65MapsBlackToLabOrigin() {
        double[] srgb = {0.0, 0.0, 0.0};
        double[] result = new double[3];

        srgbToLabD65(srgb, 0, result, 0);
        assertArrayEquals(new double[] {0.0, 0.0, 0.0}, result, 1e-12);
    }

    @Test
    void srgbToLabD65MapsWhiteToReferenceWhite() {
        double[] srgb = {1.0, 1.0, 1.0};
        double[] result = new double[3];

        srgbToLabD65(srgb, 0, result, 0);
        assertArrayEquals(new double[] {100.0, 0.0, 0.0}, result, 1e-12);
    }

    @Test
    void srgbToLabD65MapsRedToExpectedLab() {
        double[] srgb = {1.0, 0.0, 0.0};
        double[] result = new double[3];

        srgbToLabD65(srgb, 0, result, 0);
        assertArrayEquals(new double[] {53.23711559542936, 80.09011352310385, 67.20326351172214}, result, 1e-12);
    }

    @Test
    void srgbToLinearRgbSupportsInPlaceConversion() {
        assertInPlaceConversion(new double[] {0.25, 0.5, 0.75}, ColorConversion::srgbToLinearRgb);
    }

    @Test
    void linearRgbToSrgbSupportsInPlaceConversion() {
        assertInPlaceConversion(new double[] {0.25, 0.5, 0.75}, ColorConversion::linearRgbToSrgb);
    }

    @Test
    void linearRgbToLabD65SupportsInPlaceConversion() {
        assertInPlaceConversion(new double[] {0.25, 0.5, 0.75}, ColorConversion::linearRgbToLabD65);
    }

    @Test
    void labD65ToLinearRgbSupportsInPlaceConversion() {
        double[] source = new double[] {0.25, 0.5, 0.75};
        double[] lab = new double[3];
        linearRgbToLabD65(source, 0, lab, 0);
        assertInPlaceConversion(lab, ColorConversion::labD65ToLinearRgb);
    }

    @Test
    void srgbToLabD65SupportsInPlaceConversion() {
        assertInPlaceConversion(new double[] {0.25, 0.5, 0.75}, ColorConversion::srgbToLabD65);
    }

    @Test
    void labD65ToSrgbSupportsInPlaceConversion() {
        double[] srgb = new double[] {0.25, 0.5, 0.75};
        double[] lab = new double[3];
        srgbToLabD65(srgb, 0, lab, 0);
        assertInPlaceConversion(lab, ColorConversion::labD65ToSrgb);
    }

    private static void assertInPlaceConversion(double[] source, Conversion conversion) {
        double[] expected = source.clone();
        conversion.apply(source, 0, expected, 0);

        double[] inPlace = source.clone();
        conversion.apply(inPlace, 0, inPlace, 0);

        assertArrayEquals(expected, inPlace, 1.0E-12);
    }

    @FunctionalInterface
    private interface Conversion {
        void apply(double[] source, int sourceOffset, double[] result, int resultOffset);
    }
}
