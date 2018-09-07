/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.iio.common;

import com.sun.javafx.iio.common.ImageTools;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Random;

public class ImageToolsTest {
    private static final int RANDOM_SEED = 1;  // A random seed
    private static final int MIN_SIZE = 1;  // Smallest dimension to generate
    private static final int MAX_SIZE = 1000;  // Largest dimension to generate
    private static final int RANGE = MAX_SIZE - MIN_SIZE;  // Generate dimension range
    private static final int RANDOM_COUNT = 1000;  // Number of random source and target dimensions to generate and verify

    private final Random rnd = new Random(RANDOM_SEED);

    @Test
    public void testIfComputeDimensionsReturnsValuesInCorrectRangeWhenAspectRatioIsPreserved() {
        // Test a few specific corner cases:
        assertComputeDimensions(1000, 1500, 108, 108);
        assertComputeDimensions(800, 1200, 108, 108);
        assertComputeDimensions(1400, 2100, 108, 108);
        assertComputeDimensions(2000, 3000, 108, 108);
        assertComputeDimensions(98, 97, 40, 50);
        assertComputeDimensions(98, 97, 40, 0);
        assertComputeDimensions(98, 97, 0, 50);
        assertComputeDimensions(98, 97, 0, 0);
        assertComputeDimensions(98, 97, -1, -1);
        assertComputeDimensions(98, 97, 98, 97);
        assertComputeDimensions(98, 6, 3, 3);

        // Test a few random values:
        for (int i = 0; i < RANDOM_COUNT; i++) {
            int sw = rnd.nextInt(RANGE) + MIN_SIZE;
            int sh = rnd.nextInt(RANGE) + MIN_SIZE;
            int tw = rnd.nextInt(RANGE) + MIN_SIZE;
            int th = rnd.nextInt(RANGE) + MIN_SIZE;

            assertComputeDimensions(sw, sh, tw, th);
        }
    }

    @Test
    public void testIfComputeDimensionsReturnsValuesInCorrectRangeWhenAspectRatioIsNotPreserved() {
        assertArrayEquals(new int[] {10, 15}, ImageTools.computeDimensions(100, 101, 10, 15, false));
        assertArrayEquals(new int[] {100, 15}, ImageTools.computeDimensions(100, 101, 0, 15, false));
        assertArrayEquals(new int[] {100, 101}, ImageTools.computeDimensions(100, 101, 0, 0, false));
        assertArrayEquals(new int[] {10, 101}, ImageTools.computeDimensions(100, 101, 10, 0, false));
        assertArrayEquals(new int[] {100, 101}, ImageTools.computeDimensions(100, 101, -1, 0, false));
        assertArrayEquals(new int[] {100, 101}, ImageTools.computeDimensions(100, 101, -1, -1, false));
        assertArrayEquals(new int[] {100, 101}, ImageTools.computeDimensions(100, 101, 0, -1, false));
    }

    private static void assertComputeDimensions(int sw, int sh, int tw, int th) {
        int[] result = ImageTools.computeDimensions(sw, sh, tw, th, true);

        int x = result[0];
        int y = result[1];
        double originalAspect = (double)sw / sh;

        String msg = String.format("src: %dx%d, target: %dx%d, result: %dx%d", sw, sh, tw, th, x, y);

        // When any target dimension is 0 or negative, it defaults to the source dimension size
        tw = tw <= 0 ? sw : tw;
        th = th <= 0 ? sh : th;

        // Result should always return dimensions in the range of 0 < x <= size,
        // and one dimension must be equal to one of the target dimensions.
        assertTrue(msg, x <= tw);
        assertTrue(msg, y <= th);
        assertTrue(msg, x > 0);
        assertTrue(msg, y > 0);
        assertTrue(msg, x == tw || y == th);

        // Check the non-maxed dimension to see if it is within 1 pixel of the expected value
        // when calculated with the original aspect ratio:
        if (x != tw) {
            assertTrue(msg, x == Math.floor(th * originalAspect) || x == Math.ceil(th * originalAspect));
        }
        if (y != th) {
            assertTrue(msg, y == Math.floor(tw / originalAspect) || y == Math.ceil(tw / originalAspect));
        }
    }
}
