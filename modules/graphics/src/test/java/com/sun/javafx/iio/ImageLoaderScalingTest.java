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
import java.io.IOException;
import java.io.InputStream;
import static org.junit.Assert.*;
import org.junit.Ignore;
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
            throws Exception
    {
        ImageFrame[] imgFrames =
            ImageStorage.loadAll(stream, null, width, height, false, 1.0f, false);
        assertNotNull(imgFrames);
        assertTrue(imgFrames.length > 0);
        return Image.convertImageFrame(imgFrames[0]);
    }

    private void compare(Image img, Image expectedImg) {
        assertNotNull(img);
        assertNotNull(expectedImg);
        int w = img.getWidth(), h = img.getHeight();
        double scaleX = (double)expectedImg.getWidth() / w;
        double scaleY = (double)expectedImg.getHeight() / h;
        for (int y = 0; y < h; y++) {
            int srcY = (int) Math.floor((y + 0.5) * scaleY);
            for (int x = 0; x < w; x++) {
                int srcX = (int) Math.floor((x + 0.5) * scaleX);
                int expected = expectedImg.getArgb(srcX, srcY);
                int actual = img.getArgb(x, y);
                if (expected != actual) {
                    if (writeFiles) {
                        writeImages(img, expectedImg);
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

    private void writeImage(Image img, String fileName) {
        int w = img.getWidth();
        int h = img.getHeight();
        int pixels[] = new int[w * h];
        img.getPixels(0, 0, w, h,
                javafx.scene.image.PixelFormat.getIntArgbPreInstance(),
                pixels, 0, w);
        BufferedImage bImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        bImg.setRGB(0, 0, w, h, pixels, 0, w);
        try {
            ImageTestHelper.writeImage(bImg, fileName, "png", null);
        } catch (IOException e) {
            System.err.println("writeImage " + fileName + " failed: " + e);
        }
    }

    private void writeImages(Image img, Image expectedImg) {
        int w = img.getWidth();
        int h = img.getHeight();
        writeImage(expectedImg, "out"+w+"x"+h+"Orig.png");
        writeImage(img, "out"+w+"x"+h+"Scaled.png");
    }

    private void scaleAndCompareImage(BufferedImage bImg, String format,
            int width, int height) throws Exception
    {
        ByteArrayInputStream in = ImageTestHelper.writeImageToStream(bImg, format, null);
        Image expectedImg = loadImage(in, 0, 0);
        in.reset();
        Image img = loadImage(in, width, height);
        compare(img, expectedImg);
    }

    private void testScale(String format, int srcW, int srcH, int dstW, int dstH) throws Exception {
        BufferedImage bImg = createImage(srcW, srcH);
        scaleAndCompareImage(bImg, format, dstW, dstH);
    }

    @Test
    public void testNoScalePNG() throws Exception {
        testScale("png", 100, 100, 0, 0);
        testScale("png", 100, 100, 100, 100);
    }

    @Test
    public void testNoScaleBMP() throws Exception {
        testScale("bmp", 100, 100, 0, 0);
        testScale("bmp", 100, 100, 100, 100);
    }

    @Test
    public void testNoScaleJPG() throws Exception {
        testScale("jpg", 100, 100, 0, 0);
        testScale("jpg", 100, 100, 100, 100);
    }

    @Test
    public void testNoScaleGIF() throws Exception {
        testScale("gif", 100, 100, 0, 0);
        testScale("gif", 100, 100, 100, 100);
    }

    @Test
    public void testAllTheScalesPNG() throws Exception {
        testAllTheScales("png");
    }

    @Test
    public void testAllTheScalesBMP() throws Exception {
        testAllTheScales("bmp");
    }

    @Ignore // libjpeg can scale the image itself and results are unpredictable
    @Test
    public void testAllTheScalesJPG() throws Exception {
        testAllTheScales("jpg");
    }

    @Test
    public void testAllTheScalesGIF() throws Exception {
        testAllTheScales("gif");
    }

    public void testAllTheScales(String format) throws Exception {
        BufferedImage bImg = createImage(10, 10);
        for (int h = 2; h < 20; h++) {
            for (int w = 2; w < 20; w++) {
                scaleAndCompareImage(bImg, format, w, h);
                testScale(format, w, h, 10, 10);
            }
        }
    }

    // (62.0 / 78.0) * 78 != 62
    @Test
    public void testRT20295_PNG() throws Exception {
        testScale("png", 100, 62, 100, 78);
    }

    @Test
    public void testRT20295_BMP() throws Exception {
        testScale("bmp", 100, 62, 100, 78);
    }

    @Test
    public void testRT20295_JPG() throws Exception {
        testScale("jpg", 100, 62, 100, 78);
    }

    @Test
    public void testRT20295_GIF() throws Exception {
        testScale("gif", 100, 62, 100, 78);
    }
}
