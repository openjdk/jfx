/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.iio;

import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageStorage;
import com.sun.javafx.iio.ImageStorageException;
import com.sun.prism.Image;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test multiple calls to getPixel with both a normal image
 * and a Hi-DPI "@2x" image.
 *
 * @test
 * @bug 8258986
 */
public class ImageHiDPITest {
    private static final String IMAGE_NAME = "checker.png";

    private String imagePath;

    @Before
    public void setup() {
        imagePath = this.getClass().getResource(IMAGE_NAME).toExternalForm();
        assertNotNull(imagePath);
    }

    private ImageFrame loadImage(String path, float pixelScale) {
        try {
            ImageFrame[] imageFrames =
                    ImageStorage.loadAll(path, null, 0, 0, true, pixelScale, true);

            assertNotNull(imageFrames);
            assertEquals(1, imageFrames.length);
            ImageFrame imageFrame = imageFrames[0];
            assertNotNull(imageFrame);

            assertEquals("Unexpected pixel scale",
                    pixelScale, imageFrame.getPixelScale(), 0.0001f);

            int width = imageFrame.getWidth();
            int height = imageFrame.getHeight();
            assertTrue("Image size must be at least 8x8", width >= 8 && height >= 8);

            return imageFrame;
        } catch (ImageStorageException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void testPixelGet(float pixelScale) {
        ImageFrame imageFrame = loadImage(imagePath, pixelScale);
        int width = imageFrame.getWidth();
        int height = imageFrame.getHeight();

        // Convert to a Prism Image
        Image image = Image.convertImageFrame(imageFrame);
        assertNotNull(image);
        assertEquals(width, image.getWidth());
        assertEquals(height, image.getHeight());

        // Test pixel read operations.
        // NOTE: the x, y position passed into getArgb are unscaled,
        // user-space coords in the range [0, (sz/pixelScale)-1]
        int w = (int) (width / pixelScale);
        int h = (int) (height / pixelScale);

        // Test pixel read at the center and all 4 corners.
        // In each of these cases we will read the pixel twice to
        // ensure that the value remains stable, and that we don't
        // get an IOOBE.

        final int[] xvals = {
            w / 2,
            2,
            w - 2,
            2,
            w - 2
        };

        final int[] yvals = {
            h / 2,
            2,
            2,
            h - 2,
            h - 2
        };

        final int[] exColors = {
            0xffff00ff, // CENTER
            0xffff0000, // TOP LEFT
            0xff0000ff, // TOP RIGHT
            0xffff8080, // BOTTOM LEFT
            0xff8ff080  // BOTTOM RIGHT
        };

        for (int i = 0; i < xvals.length; i++) {
            int pix1 = image.getArgb(xvals[i], yvals[i]);
            assertEquals("getArgb returns incorrect color", exColors[i], pix1);
            int pix2 = image.getArgb(xvals[i], yvals[i]);
            assertEquals("second call to getArgb returns different result", pix1, pix2);
        }
    }

    @Test
    public void testNormalPixelGet() {
        // Test image with pixel scale of 1, which will load the unscaled image
        testPixelGet(1.0f);
    }

    @Test
    public void testScaledPixelGet() {
        // Test image with pixel scale of 2, which will load the @2x image file
        testPixelGet(2.0f);
    }

    @Test
    public void testScaledImageSize() {
        // Load both the normal and the @2x images
        ImageFrame imageFrame1 = loadImage(imagePath, 1.0f);
        ImageFrame imageFrame2 = loadImage(imagePath, 2.0f);

        // Check that the size of the @2x image is twice that of the normal image
        int exWidth2 = imageFrame1.getWidth() * 2;
        int exHeight2 = imageFrame1.getHeight()* 2;
        assertEquals("width of @2x image is wrong", exWidth2, imageFrame2.getWidth());
        assertEquals("height of @2x image is wrong", exHeight2, imageFrame2.getHeight());
    }

}
