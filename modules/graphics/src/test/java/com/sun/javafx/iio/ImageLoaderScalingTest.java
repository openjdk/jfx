/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.iio;

import com.sun.prism.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import static org.junit.Assert.*;
import org.junit.Test;

public class ImageLoaderScalingTest {
    // if true, the test will write original and scaled PNG files to the current directory
    private static final boolean writeFiles = false;

    private BufferedImage createImage(int w, int h) {
        BufferedImage bImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ImageTestHelper.drawImageRandom(bImg);
        return bImg;
    }

    private Image loadImage(InputStream stream, int width, int height)
    {
        try {
            ImageFrame[] imgFrames =
                ImageStorage.loadAll(stream, null, width, height, false, 1.0f, false);
            assertNotNull(imgFrames);
            assertTrue(imgFrames.length > 0);
            return Image.convertImageFrame(imgFrames[0]);
        } catch (ImageStorageException e) {
            fail("unexpected ImageStorageException: " + e);
        } catch (Exception e) {
            fail("unexpected Exception: " + e);
        }
        return null;
    }

    private ByteArrayInputStream writePNGStream(BufferedImage bImg, File file) {
        return ImageTestHelper.writeImageToStream(bImg, "png", null, file);
    }

    private void compare(Image img, BufferedImage bImg) {
        assertNotNull(img);
        assertNotNull(bImg);
        int w = img.getWidth(), h = img.getHeight();
        double scaleX = (double)bImg.getWidth() / w;
        double scaleY = (double)bImg.getHeight() / h;
        for (int y = 0; y < h; y++) {
            int srcY = (int) Math.floor((y + 0.5) * scaleY);
            for (int x = 0; x < w; x++) {
                int srcX = (int) Math.floor((x + 0.5) * scaleX);
                int expected = bImg.getRGB(srcX, srcY);
                int actual = img.getArgb(x, y);
                if (expected != actual) {
                    if (writeFiles) {
                        writeImages(img, bImg);
                    }
                    throw new org.junit.ComparisonFailure(
                        "pixel " + x + ", " + y + " does not match",
                        String.format("0x%08X", expected),
                        String.format("0x%08X", actual)
                    );
                }
            }
        }
    }

    private void writeImages(Image img, BufferedImage bImg) {
        int w = img.getWidth();
        int h = img.getHeight();
        writePNGStream(bImg, new File("out"+w+"x"+h+"OrigJDK.png"));
        int pixels[] = new int[w * h];
        img.getPixels(0, 0, w, h,
                javafx.scene.image.PixelFormat.getIntArgbPreInstance(),
                pixels, 0, w);
        BufferedImage fxImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        fxImg.setRGB(0, 0, w, h, pixels, 0, w);
        writePNGStream(fxImg, new File("out"+w+"x"+h+"ScaledFX.png"));
    }

    private void scaleAndCompareImage(BufferedImage bImg, int width, int height) {
        ByteArrayInputStream in = writePNGStream(bImg, null);
        Image img = loadImage(in, width, height);
        compare(img, bImg);
    }

    private void testScale(int w1, int h1, int w2, int h2) {
        BufferedImage bImg = createImage(w1, h1);
        scaleAndCompareImage(bImg, w2, h2);
    }

    @Test
    public void testNoScale() {
        testScale(100, 100, 100, 100);
    }

    @Test
    public void testAllTheScales() {
        BufferedImage bImg = createImage(10, 10);
        for (int h = 2; h < 20; h++) {
            for (int w = 2; w < 20; w++) {
                scaleAndCompareImage(bImg, w, h);
                testScale(w, h, 10, 10);
            }
        }
    }

    @Test
    public void testRT20295() {
        // (62.0 / 78.0) * 78 != 62
        testScale(100, 62, 100, 78);
    }
}
