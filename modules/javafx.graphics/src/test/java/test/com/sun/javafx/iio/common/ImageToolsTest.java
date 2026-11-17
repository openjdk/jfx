/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
        assertTrue(x <= tw, msg);
        assertTrue(y <= th, msg);
        assertTrue(x > 0, msg);
        assertTrue(y > 0, msg);
        assertTrue(x == tw || y == th, msg);

        // Check the non-maxed dimension to see if it is within 1 pixel of the expected value
        // when calculated with the original aspect ratio:
        if (x != tw) {
            assertTrue(x == Math.floor(th * originalAspect) || x == Math.ceil(th * originalAspect), msg);
        }
        if (y != th) {
            assertTrue(y == Math.floor(tw / originalAspect) || y == Math.ceil(tw / originalAspect), msg);
        }
    }

    @Test
    public void testValidateMaxDimensions() {
        assertDoesNotThrow(() -> ImageTools.validateMaxDimensions(Integer.MAX_VALUE, 1, 1));
        assertDoesNotThrow(() -> ImageTools.validateMaxDimensions(1, Integer.MAX_VALUE, 1));
        assertThrows(IllegalArgumentException.class, () -> ImageTools.validateMaxDimensions((double)Integer.MAX_VALUE + 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> ImageTools.validateMaxDimensions(1, (double)Integer.MAX_VALUE + 1, 1));
        assertDoesNotThrow(() -> ImageTools.validateMaxDimensions(46340, 46341, 1));
        assertThrows(IllegalArgumentException.class, () -> ImageTools.validateMaxDimensions(46340, 46342, 1));
        assertThrows(IllegalArgumentException.class, () -> ImageTools.validateMaxDimensions(46342, 46340, 1));
        assertDoesNotThrow(() -> ImageTools.validateMaxDimensions(37072, 37073, 1.25));
        assertThrows(IllegalArgumentException.class, () -> ImageTools.validateMaxDimensions(37073, 37073, 1.25));
        assertDoesNotThrow(() -> ImageTools.validateMaxDimensions(30893, 30894, 1.5));
        assertThrows(IllegalArgumentException.class, () -> ImageTools.validateMaxDimensions(30894, 30894, 1.5));
        assertDoesNotThrow(() -> ImageTools.validateMaxDimensions(26481, 26480, 1.75));
        assertThrows(IllegalArgumentException.class, () -> ImageTools.validateMaxDimensions(26481, 26481, 1.75));
        assertThrows(IllegalArgumentException.class, () -> ImageTools.validateMaxDimensions(Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1, 1));
        assertThrows(IllegalArgumentException.class, () -> ImageTools.validateMaxDimensions(Integer.MAX_VALUE, Integer.MAX_VALUE, 1));
        assertThrows(IllegalArgumentException.class, () -> ImageTools.validateMaxDimensions(Integer.MAX_VALUE, Integer.MAX_VALUE, 3));
    }

    @Test
    public void testGetScaledImageName() {
        String nameWithoutPath = "image.png";
        String nameWithPath = "/home/path/test/for/testing/image.png";
        String expectedWithoutPath = "image@3x.png";
        String expectedWithPath = "/home/path/test/for/testing/image@4x.png";

        assertEquals(expectedWithoutPath, ImageTools.getScaledImageName(nameWithoutPath, 3));
        assertEquals(expectedWithPath, ImageTools.getScaledImageName(nameWithPath, 4));
    }

    @Test
    public void testHasScaledName() {
        assertTrue(ImageTools.hasScaledName("image@3x.png"));
        assertTrue(ImageTools.hasScaledName("/home/path/test/for/testing/image@2x.png"));
        assertTrue(ImageTools.hasScaledName("image@5x"));
        assertFalse(ImageTools.hasScaledName("image.png"));
        assertFalse(ImageTools.hasScaledName("/home/path/test/for/testing/image.png"));
        assertFalse(ImageTools.hasScaledName("image"));
        assertFalse(ImageTools.hasScaledName("image@somewhere"));
        assertFalse(ImageTools.hasScaledName("image@3ish"));
        assertFalse(ImageTools.hasScaledName("image@4ix"));
        assertFalse(ImageTools.hasScaledName("image@0x"));
        assertFalse(ImageTools.hasScaledName("image@-1x"));
    }
}
