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

package com.sun.javafx.image;

import com.sun.javafx.util.ColorConversion;
import com.sun.javafx.util.KMeans3;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

public final class ImageUtils {

    private ImageUtils() {}

    /**
     * Computes the dominant color of an image when composited onto a solid background by clustering
     * sampled pixels in CIELAB space using k-means and returning the centroid of the largest cluster.
     * Compositing is performed in linear RGB before conversion to Lab.
     *
     * @param image the input image
     * @return the dominant color
     */
    public static Color computeDominantColor(Image image, Color background) {
        int sampleStep = chooseSampleStep((int)image.getWidth(), (int)image.getHeight(), 32000);
        return computeDominantColor(image, background, sampleStep);
    }

    /**
     * Computes the dominant color of an image when composited onto a solid background by clustering
     * sampled pixels in CIELAB space using k-means and returning the centroid of the largest cluster.
     * Compositing is performed in linear RGB before conversion to Lab.
     *
     * @param image the input image
     * @param sampleStep the subsampling stride
     * @return the dominant color
     */
    public static Color computeDominantColor(Image image, Color background, int sampleStep) {
        PixelReader pixelReader = image.getPixelReader();
        if (pixelReader == null) {
            return Color.TRANSPARENT;
        }

        int w = (int)Math.floor(image.getWidth());
        int h = (int)Math.floor(image.getHeight());

        // Upper bound on samples when subsampling
        int maxSamples = ((w + sampleStep - 1) / sampleStep) * ((h + sampleStep - 1) / sampleStep);
        double[] labs = new double[3 * maxSamples]; // interleaved L/a/b
        double[] buffer = new double[3];
        int n = 0;

        // Convert the background color to linear RGB.
        srgbToLinearRgb(background.getRed(), background.getGreen(), background.getBlue(), buffer);
        double bgR = buffer[0],
               bgG = buffer[1],
               bgB = buffer[2];

        // Sample the image in Lab D65
        for (int y = 0; y < h; y += sampleStep) {
            for (int x = 0; x < w; x += sampleStep) {
                // Read the pixel as sRGB + alpha
                int argb = pixelReader.getArgb(x, y);
                int a8 = (argb >>> 24) & 0xff,
                    r8 = (argb >>> 16) & 0xff,
                    g8 = (argb >>> 8) & 0xff,
                    b8 = argb & 0xff;

                // Convert pixel color to linear RGB
                srgbToLinearRgb(r8 / 255.0, g8 / 255.0, b8 / 255.0, buffer);

                // Composite the pixel color over the background in linear RGB
                double a = a8 / 255.0;
                double outR = a * buffer[0] + (1.0 - a) * bgR,
                       outG = a * buffer[1] + (1.0 - a) * bgG,
                       outB = a * buffer[2] + (1.0 - a) * bgB;

                // Convert the composited result to Lab D65 and store it in sample array.
                linearRgbToLabD65(outR, outG, outB, labs, 3 * n++);
            }
        }

        if (n == 0) {
            return Color.TRANSPARENT;
        }

        // Run k-means on the sample array to cluster the collected pixels.
        KMeans3.ClusterResult result = KMeans3.cluster(labs, n, 4, 50, 0.1);

        // Convert the color of the largest cluster back to sRGB.
        ColorConversion.labD65ToSrgb(result.centers(), result.largestIndex() * 3, buffer, 0);

        return Color.rgb(clampToByte(buffer[0]), clampToByte(buffer[1]), clampToByte(buffer[2]));
    }

    private static int chooseSampleStep(int width, int height, int maxSamples) {
        if (maxSamples < 1) {
            throw new IllegalArgumentException("maxSamples must be >= 1");
        }

        long numPixels = (long)width * (long)height;

        // Choose sampleStep so that (w/step)*(h/step) <= maxSamples
        int sampleStep;
        if (numPixels <= (long) maxSamples) {
            sampleStep = 1;
        } else {
            sampleStep = (int)Math.ceil(Math.sqrt(numPixels / (double)maxSamples));
            if (sampleStep < 1) {
                sampleStep = 1;
            }
        }

        return sampleStep;
    }

    private static int clampToByte(double v) {
        if (v <= 0.0) return 0;
        if (v >= 1.0) return 255;
        return (int)Math.round(v * 255.0);
    }

    private static void srgbToLinearRgb(double r, double g, double b, double[] result) {
        result[0] = r;
        result[1] = g;
        result[2] = b;
        ColorConversion.srgbToLinearRgb(result, 0, result, 0);
    }

    public static void linearRgbToLabD65(double r, double g, double b, double[] result, int offset) {
        result[offset] = r;
        result[offset + 1] = g;
        result[offset + 2] = b;
        ColorConversion.linearRgbToLabD65(result, offset, result, offset);
    }
}
