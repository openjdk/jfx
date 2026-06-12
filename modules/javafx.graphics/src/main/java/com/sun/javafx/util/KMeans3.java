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

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/**
 * K-means clustering for three-dimensional points.
 */
public final class KMeans3 {

    private KMeans3() {}

    /**
     * Clusters the given points using the specified iteration limit and convergence tolerance.
     *
     * @param points interleaved {@code x, y, z} point coordinates
     * @param pointCount number of points to read from {@code points}
     * @param clusterCount requested number of clusters; values larger than {@code pointCount}
     *                     are clamped to {@code pointCount}
     * @param maxIterations maximum number of iterations
     * @param tolerance convergence threshold for the maximum center movement
     * @return final cluster centers and membership counts
     */
    public static ClusterResult cluster(double[] points, int pointCount, int clusterCount,
                                        int maxIterations, double tolerance) {
        Objects.requireNonNull(points, "points");

        if (pointCount < 1) {
            throw new IllegalArgumentException("pointCount must be >= 1");
        }

        if (clusterCount < 1) {
            throw new IllegalArgumentException("clusterCount must be >= 1");
        }

        if (maxIterations < 1) {
            throw new IllegalArgumentException("maxIterations must be >= 1");
        }

        if (tolerance < 0) {
            throw new IllegalArgumentException("tolerance must be >= 0");
        }

        Objects.checkIndex(pointCount * 3 - 1, points.length);

        // We can't have more non-empty clusters than there are input points.
        clusterCount = Math.min(clusterCount, pointCount);

        // Deterministic seeding keeps clustering reproducible for the same input data.
        Random random = new Random(0);
        double[] centers = new double[clusterCount * 3];
        initCenters(points, pointCount, clusterCount, random, centers);

        // Per-cluster sums and compensation terms for the current iteration.
        double[] sumX = new double[clusterCount];
        double[] sumY = new double[clusterCount];
        double[] sumZ = new double[clusterCount];
        double[] compX = new double[clusterCount];
        double[] compY = new double[clusterCount];
        double[] compZ = new double[clusterCount];
        int[] counts = new int[clusterCount];

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            Arrays.fill(sumX, 0);
            Arrays.fill(sumY, 0);
            Arrays.fill(sumZ, 0);
            Arrays.fill(compX, 0);
            Arrays.fill(compY, 0);
            Arrays.fill(compZ, 0);
            Arrays.fill(counts, 0);

            // Assign each point to its nearest center and accumulate per-cluster sums.
            for (int i = 0; i < pointCount; i++) {
                int best = nearestCenter(points, i, centers, clusterCount);
                int base = i * 3;
                accumulate(sumX, compX, best, points[base]);
                accumulate(sumY, compY, best, points[base + 1]);
                accumulate(sumZ, compZ, best, points[base + 2]);
                counts[best] += 1;
            }

            // During each iteration, a new center is computed for every non-empty cluster.
            // We then measure how far each center moved from its previous position, and maxMove tracks
            // the largest movement over all centers in the current iteration. The algorithm has converged
            // when the maximum movement is less than the requested tolerance.
            double maxMove = 0;

            // Recompute centers from the accumulated sums, reseeding empty clusters as needed.
            for (int c = 0; c < clusterCount; c++) {
                if (counts[c] > 0) {
                    int centerBase = c * 3;
                    double newX = (sumX[c] + compX[c]) / counts[c];
                    double newY = (sumY[c] + compY[c]) / counts[c];
                    double newZ = (sumZ[c] + compZ[c]) / counts[c];
                    double dX = newX - centers[centerBase];
                    double dY = newY - centers[centerBase + 1];
                    double dZ = newZ - centers[centerBase + 2];
                    double move = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
                    if (move > maxMove) {
                        maxMove = move;
                    }

                    centers[centerBase] = newX;
                    centers[centerBase + 1] = newY;
                    centers[centerBase + 2] = newZ;
                } else {
                    // If a cluster becomes empty, reseed it from an existing point.
                    copyPointToCenter(points, random.nextInt(pointCount), centers, c);

                    // Force another iteration after reseeding an empty cluster.
                    maxMove = Double.POSITIVE_INFINITY;
                }
            }

            if (maxMove < tolerance) {
                break;
            }
        }

        // Membership counts from the last accumulation pass may be stale if centers moved.
        // Recompute counts against the final converged centers before returning.
        Arrays.fill(counts, 0);
        for (int i = 0; i < pointCount; i++) {
            int best = nearestCenter(points, i, centers, clusterCount);
            counts[best]++;
        }

        return new ClusterResult(centers, counts);
    }

    /**
     * Adds a value to the running sum of a cluster and stores a compensation term using Neumaier's improved
     * Kahan–Babuška summation algorithm, which significantly reduces the floating-point summation error
     * compared to naive summation.
     *
     * @param sums accumulated sums for each cluster
     * @param comp compensation terms for each cluster
     * @param index index of the cluster
     * @param value value to add to the running sum for the specified cluster
     * @see <a href="https://en.wikipedia.org/wiki/Kahan_summation_algorithm">Kahan summation algorithm</a>
     */
    private static void accumulate(double[] sums, double[] comp, int index, double value) {
        double sum = sums[index];
        double t = sum + value;

        if (Math.abs(sum) >= Math.abs(value)) {
            comp[index] += (sum - t) + value;
        } else {
            comp[index] += (value - t) + sum;
        }

        sums[index] = t;
    }

    /**
     * Initializes centers with a k-means++ strategy.
     *
     * @see <a href="https://courses.cs.duke.edu/spring07/cps296.2/papers/kMeansPlusPlus.pdf">k-means++</a>
     */
    private static void initCenters(double[] points, int pointCount, int clusterCount,
                                    Random random, double[] centers) {
        // Choose the first center uniformly at random, then choose later centers
        // with probability proportional to squared distance from the nearest center.
        copyPointToCenter(points, random.nextInt(pointCount), centers, 0);

        // For each point, store the squared distance to its nearest chosen center so far.
        // Squared distance is sufficient for comparisons and avoids sqrt cost.
        double[] minDist2 = new double[pointCount];
        for (int i = 0; i < pointCount; i++) {
            minDist2[i] = distanceToCenterSq(points, i, centers, 0);
        }

        for (int c = 1; c < clusterCount; c++) {
            double total = 0;
            for (int i = 0; i < pointCount; i++) {
                total += minDist2[i];
            }

            int chosen;
            if (total <= 0) {
                // All points are effectively identical to an existing center, so fall back
                // to uniform random selection.
                chosen = random.nextInt(pointCount);
            } else {
                double threshold = random.nextDouble() * total;
                double accumulated = 0;

                // Fallback in case rounding leaves the threshold unmatched before the last point.
                chosen = pointCount - 1;
                for (int i = 0; i < pointCount; i++) {
                    accumulated += minDist2[i];
                    if (accumulated >= threshold) {
                        chosen = i;
                        break;
                    }
                }
            }

            copyPointToCenter(points, chosen, centers, c);

            for (int i = 0; i < pointCount; i++) {
                double candidateDistance = distanceToCenterSq(points, i, centers, c);
                if (candidateDistance < minDist2[i]) {
                    minDist2[i] = candidateDistance;
                }
            }
        }
    }

    /**
     * Returns the index of the nearest center for the given point.
     * <p>
     * Squared distances are used to avoid unnecessary square-root work during comparisons.
     */
    private static int nearestCenter(double[] points, int pointIndex, double[] centers, int clusterCount) {
        int best = 0;
        double bestDistance = distanceSq(points, pointIndex, centers, 0);

        // Ties are resolved in favor of the first center encountered.
        for (int c = 1; c < clusterCount; c++) {
            double distance = distanceSq(points, pointIndex, centers, c);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = c;
            }
        }

        return best;
    }

    private static void copyPointToCenter(double[] points, int pointIndex, double[] centers, int centerIndex) {
        int pointBase = pointIndex * 3;
        int centerBase = centerIndex * 3;
        centers[centerBase] = points[pointBase];
        centers[centerBase + 1] = points[pointBase + 1];
        centers[centerBase + 2] = points[pointBase + 2];
    }

    private static double distanceToCenterSq(double[] points, int pointIndex, double[] centers, int centerIndex) {
        int pointBase = pointIndex * 3;
        int centerBase = centerIndex * 3;
        double dX = points[pointBase] - centers[centerBase];
        double dY = points[pointBase + 1] - centers[centerBase + 1];
        double dZ = points[pointBase + 2] - centers[centerBase + 2];
        return dX * dX + dY * dY + dZ * dZ;
    }

    private static double distanceSq(double[] points, int pointIndex, double[] centers, int centerIndex) {
        int pointBase = pointIndex * 3;
        int centerBase = centerIndex * 3;
        double dX = points[pointBase] - centers[centerBase];
        double dY = points[pointBase + 1] - centers[centerBase + 1];
        double dZ = points[pointBase + 2] - centers[centerBase + 2];
        return dX * dX + dY * dY + dZ * dZ;
    }

    /**
     * The cluster centers and membership counts.
     *
     * @param centers interleaved {@code x, y, z} center coordinates
     * @param counts number of points assigned to each center
     */
    public record ClusterResult(double[] centers, int[] counts) {
        public ClusterResult {
            Objects.requireNonNull(centers, "centers");
            Objects.requireNonNull(counts, "counts");
        }

        public int largestIndex() {
            int largest = 0;
            for (int c = 1; c < counts.length; c++) {
                if (counts[c] > counts[largest]) {
                    largest = c;
                }
            }

            return largest;
        }
    }
}
