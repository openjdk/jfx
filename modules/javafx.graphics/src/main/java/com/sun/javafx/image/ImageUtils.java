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

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import java.util.Arrays;
import java.util.Random;

public final class ImageUtils {

    private ImageUtils() {}

    /**
     * Computes the dominant color of an image when composited onto a solid background by clustering
     * sampled pixels in CIELAB space using k-means and returning the centroid of the largest cluster.
     *
     * @param image the input image
     * @return the dominant color
     */
    public static Color computeDominantColor(Image image, Color background) {
        return computeDominantColor(image, background, 6, 32000);
    }

    /**
     * Computes the dominant color of an image when composited onto a solid background by clustering
     * sampled pixels in CIELAB space using k-means and returning the centroid of the largest cluster.
     * <p>
     * This method invokes {@link #computeDominantColor(Image, Color, int, int, long)} with arguments tuned
     * to limit the runtime cost of the clustering algorithm. It chooses {@code sampleStep} so that the
     * estimated number of sampled pixels is at most {@code maxSamples}, then chooses {@code clusters}
     * based on the resulting sample count and the provided {@code maxClusters}.
     *
     * @param image the input image
     * @param maxClusters maximum number of clusters (>= 1); typical values are 3-8, larger values capture more
     *                    distinct colors but may increase runtime cost and can over-segment noisy images
     * @param maxSamples maximum number of samples (>= 1)
     * @return the dominant color
     */
    public static Color computeDominantColor(Image image, Color background, int maxClusters, int maxSamples) {
        if (maxClusters < 1) {
            throw new IllegalArgumentException("maxClusters must be >= 1");
        }

        if (maxSamples < 1) {
            throw new IllegalArgumentException("maxSamples must be >= 1");
        }

        int w = (int)Math.floor(image.getWidth());
        int h = (int)Math.floor(image.getHeight());
        long numPixels = (long)w * (long)h;

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

        // Estimated number of sampled points from dimensions
        int wS = (w + sampleStep - 1) / sampleStep;
        int hS = (h + sampleStep - 1) / sampleStep;
        int nEst = wS * hS;

        // Choose clusters based on sample budget, smoothly increasing up to maxClusters.
        int minClusters = Math.min(3, maxClusters);
        int clusters;
        if (nEst <= 1) {
            clusters = 1;
        } else if (nEst < 200) {
            clusters = Math.min(minClusters, nEst);
        } else {
            double frac = Math.sqrt(Math.min(1.0, nEst / (double)maxSamples));
            clusters = (int)Math.round(minClusters + frac * (maxClusters - minClusters));
        }

        // Guard against too many clusters for too few points to reduce empty clusters.
        int upper = Math.max(1, nEst / 300);
        clusters = Math.min(clusters, upper);
        clusters = Math.max(1, Math.min(clusters, Math.min(maxClusters, nEst)));

        return computeDominantColor(image, background, clusters, sampleStep, 0);
    }

    /**
     * Computes the dominant color of an image when composited onto a solid background by clustering
     * sampled pixels in CIELAB space using k-means and returning the centroid of the largest cluster.
     * <p>
     * Many images contain a small set of recurring colors. This method approximates that palette by
     * running k-means on pixel colors (in Lab), then selects the cluster that accounts for the greatest
     * total pixel count. The centroid of that cluster is returned as an sRGB color.
     *
     * @param image the input image
     * @param clusters number of clusters (>= 1); typical values are 3-8, larger values capture more
     *                 distinct colors but may increase runtime cost and can over-segment noisy images
     * @param sampleStep subsampling step (>= 1), 2-8 is usually sufficient
     * @param seed random seed for reproducibility
     * @return the dominant color
     */
    public static Color computeDominantColor(Image image, Color background, int clusters, int sampleStep, long seed) {
        PixelReader pixelReader = image.getPixelReader();
        if (pixelReader == null) {
            return Color.TRANSPARENT;
        }

        if (sampleStep < 1) {
            sampleStep = 1;
        }

        int w = (int)Math.floor(image.getWidth());
        int h = (int)Math.floor(image.getHeight());

        // Upper bound on samples when subsampling
        int maxSamples = ((w + sampleStep - 1) / sampleStep) * ((h + sampleStep - 1) / sampleStep);
        double[] labs = new double[3 * maxSamples]; // interleaved L/a/b
        int n = 0;

        double bgR = background.getRed();
        double bgG = background.getGreen();
        double bgB = background.getBlue();

        // Collect samples
        for (int y = 0; y < h; y += sampleStep) {
            for (int x = 0; x < w; x += sampleStep) {
                int argb = pixelReader.getArgb(x, y);
                int a8 = (argb >>> 24) & 0xff;
                int r8 = (argb >>> 16) & 0xff;
                int g8 = (argb >>> 8) & 0xff;
                int b8 = argb & 0xff;

                double a = a8 / 255.0;
                double sr = r8 / 255.0;
                double sg = g8 / 255.0;
                double sb = b8 / 255.0;

                double outR = a * sr + (1.0 - a) * bgR;
                double outG = a * sg + (1.0 - a) * bgG;
                double outB = a * sb + (1.0 - a) * bgB;

                srgbToLab(outR, outG, outB, labs, 3 * n);
                n++;
            }
        }

        if (n == 0) {
            return Color.TRANSPARENT;
        }

        clusters = Math.min(clusters, n);
        var random = new Random(seed);
        double[] centers = new double[3 * clusters];
        initKMeans(labs, n, clusters, random, centers);

        // Lloyd iterations
        int maxIterations = 50;
        double tolerance = 0.0001;
        double[] sumL = new double[clusters];
        double[] sumA = new double[clusters];
        double[] sumB = new double[clusters];
        int[] count = new int[clusters];

        for (int iter = 0; iter < maxIterations; iter++) {
            Arrays.fill(sumL, 0.0);
            Arrays.fill(sumA, 0.0);
            Arrays.fill(sumB, 0.0);
            Arrays.fill(count, 0);

            // Assign and accumulate
            for (int i = 0; i < n; i++) {
                int pBase = 3 * i;
                double pL = labs[pBase];
                double pA = labs[pBase + 1];
                double pBv = labs[pBase + 2];

                int best = 0;
                double bestD = dist2(pL, pA, pBv, centers, 0);

                for (int c = 1; c < clusters; c++) {
                    double d = dist2(pL, pA, pBv, centers, 3 * c);
                    if (d < bestD) {
                        bestD = d;
                        best = c;
                    }
                }

                sumL[best] += pL;
                sumA[best] += pA;
                sumB[best] += pBv;
                count[best] += 1;
            }

            // Update centers
            double maxMove = 0.0;
            for (int c = 0; c < clusters; c++) {
                int cBase = 3 * c;

                if (count[c] > 0) {
                    double newL = sumL[c] / count[c];
                    double newA = sumA[c] / count[c];
                    double newB = sumB[c] / count[c];
                    double dL = newL - centers[cBase];
                    double dA = newA - centers[cBase + 1];
                    double dB = newB - centers[cBase + 2];
                    double move = Math.sqrt(dL * dL + dA * dA + dB * dB);
                    if (move > maxMove) {
                        maxMove = move;
                    }

                    centers[cBase] = newL;
                    centers[cBase + 1] = newA;
                    centers[cBase + 2] = newB;
                } else {
                    // Empty cluster: reseed to a random sample
                    int idx = random.nextInt(n);
                    int pBase = 3 * idx;
                    centers[cBase] = labs[pBase];
                    centers[cBase + 1] = labs[pBase + 1];
                    centers[cBase + 2] = labs[pBase + 2];
                    maxMove = Math.max(maxMove, tolerance + 1.0);
                }
            }

            if (maxMove < tolerance) {
                break;
            }
        }

        Arrays.fill(count, 0);

        // After the k-means loop, recompute counts with final centers
        for (int i = 0; i < n; i++) {
            int pBase = 3 * i;
            double pL = labs[pBase];
            double pA = labs[pBase + 1];
            double pBv = labs[pBase + 2];

            int best = 0;
            double bestD = dist2(pL, pA, pBv, centers, 0);

            for (int c = 1; c < clusters; c++) {
                double d = dist2(pL, pA, pBv, centers, 3 * c);
                if (d < bestD) {
                    bestD = d;
                    best = c;
                }
            }

            count[best]++;
        }

        // Pick largest cluster by count
        int dominant = 0;
        for (int c = 1; c < clusters; c++) {
            if (count[c] > count[dominant]) {
                dominant = c;
            }
        }

        int cBase = 3 * dominant;
        return labToSrgb(centers[cBase], centers[cBase + 1], centers[cBase + 2]);
    }

    private static void initKMeans(double[] labs, int n, int k, Random random, double[] centers) {
        int first = random.nextInt(n);
        copyLab(labs, first, centers, 0);

        double[] minDist2 = new double[n];
        for (int i = 0; i < n; i++) {
            minDist2[i] = dist2Index(labs, i, centers, 0);
        }

        for (int c = 1; c < k; c++) {
            double total = 0.0;
            for (int i = 0; i < n; i++) {
                total += minDist2[i];
            }

            int chosen;
            if (total <= 0.0) {
                chosen = random.nextInt(n);
            } else {
                double r = random.nextDouble() * total;
                double acc = 0.0;
                chosen = n - 1;

                for (int i = 0; i < n; i++) {
                    acc += minDist2[i];
                    if (acc >= r) {
                        chosen = i;
                        break;
                    }
                }
            }

            copyLab(labs, chosen, centers, 3 * c);

            for (int i = 0; i < n; i++) {
                double d = dist2Index(labs, i, centers, 3 * c);
                if (d < minDist2[i]) {
                    minDist2[i] = d;
                }
            }
        }
    }

    private static void copyLab(double[] labs, int srcIndex, double[] dst, int dstOffset) {
        int base = 3 * srcIndex;
        dst[dstOffset] = labs[base];
        dst[dstOffset + 1] = labs[base + 1];
        dst[dstOffset + 2] = labs[base + 2];
    }

    private static double dist2Index(double[] labs, int i, double[] centers, int offset) {
        int base = 3 * i;
        double dL = labs[base] - centers[offset];
        double dA = labs[base + 1] - centers[offset + 1];
        double dB = labs[base + 2] - centers[offset + 2];
        return dL * dL + dA * dA + dB * dB;
    }

    private static double dist2(double pL, double pA, double pB, double[] centers, int offset) {
        double dL = pL - centers[offset];
        double dA = pA - centers[offset + 1];
        double dBv = pB - centers[offset + 2];
        return dL * dL + dA * dA + dBv * dBv;
    }

    private static void srgbToLab(double rs, double gs, double bs, double[] result, int offset) {
        double r = srgbToLinear(rs);
        double g = srgbToLinear(gs);
        double b = srgbToLinear(bs);

        double X = 0.4124564 * r + 0.3575761 * g + 0.1804375 * b;
        double Y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b;
        double Z = 0.0193339 * r + 0.1191920 * g + 0.9503041 * b;

        double fx = fLab(X / XN);
        double fy = fLab(Y / YN);
        double fz = fLab(Z / ZN);

        result[offset] = 116.0 * fy - 16.0;
        result[offset + 1] = 500.0 * (fx - fy);
        result[offset + 2] = 200.0 * (fy - fz);
    }

    private static Color labToSrgb(double L, double A, double B) {
        // Lab -> XYZ
        double fy = (L + 16.0) / 116.0;
        double fx = fy + (A / 500.0);
        double fz = fy - (B / 200.0);

        double X = XN * fInvLab(fx);
        double Y = YN * fInvLab(fy);
        double Z = ZN * fInvLab(fz);

        // XYZ -> linear RGB
        double r = 3.2404542 * X + -1.5371385 * Y + (-0.4985314) * Z;
        double g = -0.9692660 * X + 1.8760108 * Y + 0.0415560 * Z;
        double b = 0.0556434 * X + -0.2040259 * Y + 1.0572252 * Z;

        // linear RGB -> sRGB
        double rs = linearToSrgb(r);
        double gs = linearToSrgb(g);
        double bs = linearToSrgb(b);

        return Color.rgb(clampToByte(rs), clampToByte(gs), clampToByte(bs));
    }

    private static double srgbToLinear(double v) {
        if (v <= 0.04045) {
            return v / 12.92;
        }

        return Math.pow((v + 0.055) / 1.055, 2.4);
    }

    private static double linearToSrgb(double v) {
        if (v <= 0.0) return 0.0;
        if (v >= 1.0) return 1.0;
        if (v <= 0.0031308) return 12.92 * v;
        return 1.055 * Math.pow(v, 1.0 / 2.4) - 0.055;
    }

    private static int clampToByte(double v) {
        if (v <= 0.0) return 0;
        if (v >= 1.0) return 255;
        return (int)Math.round(v * 255.0);
    }

    private static double fLab(double t) {
        if (t > DELTA3){
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

    // D65 reference white (2°), XYZ in [0..1] scale
    private static final double XN = 0.95047;
    private static final double YN = 1.00000;
    private static final double ZN = 1.08883;

    private static final double DELTA = 6.0 / 29.0;
    private static final double DELTA2 = DELTA * DELTA;
    private static final double DELTA3 = DELTA2 * DELTA;
}
