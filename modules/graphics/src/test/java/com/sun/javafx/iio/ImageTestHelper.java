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

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import static org.junit.Assert.fail;

public class ImageTestHelper {

    static void writeImage(BufferedImage bImg, String fileName, String format, String compression) {
        if (fileName != null) {
            File file = new File(fileName);
            file.delete();
            writeImage(bImg, file, format, compression);
        }
    }

    static void writeImage(BufferedImage bImg, Object out, String format, String compression) {
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(format);
            ImageWriter writer = iter.next();
            ImageWriteParam iwp = writer.getDefaultWriteParam();
            if (compression != null) {
                iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                iwp.setCompressionType(compression);
            }
            writer.setOutput(ios);
            try {
                writer.write(null, new IIOImage(bImg, null, null), iwp);
            } finally {
                writer.dispose();
                ios.flush();
            }
        } catch (IOException e) {
            fail("unexpected IOException: " + e);
        }
    }

    static ByteArrayInputStream writeImageToStream(BufferedImage bImg,
            String format, String compression, File file)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeImage(bImg, out, format, compression);
        if (file != null) {
            writeImage(bImg, file, format, compression);
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    static void drawImageGradient(BufferedImage bImg) {
        int w = bImg.getWidth();
        int h = bImg.getHeight();
        Graphics2D graphics = bImg.createGraphics();
        GradientPaint g = new GradientPaint(0, 0, Color.RED, w, h, Color.GREEN);
        graphics.setPaint(g);
        graphics.fillRect(0, 0, w, h);
    }

    static void drawImageRandom(BufferedImage bImg) {
        int w = bImg.getWidth();
        int h = bImg.getHeight();
        Random r = new Random(1);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bImg.setRGB(x, y, r.nextInt(1 << 24));
            }
        }
    }

    static void drawImageHue(BufferedImage bImg) {
        int w = bImg.getWidth();
        int h = bImg.getHeight();
        for (int y = 0; y < h; y++) {
            float s = 2.0f * y / h;
            if (s > 1) {
                s = 1;
            }
            float b = 2.0f * (h - y) / h;
            if (b > 1) {
                b = 1;
            }
            for (int x = 0; x < w; x++) {
                float hue = (float) x / w;
                bImg.setRGB(x, y, Color.HSBtoRGB(hue, s, b));
            }
        }
    }

    static void drawImageAll(BufferedImage bImg) {
        int w = bImg.getWidth();
        int h = bImg.getHeight();
        //if (h*w < (1<<24)) return;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bImg.setRGB(x, y, y * h + x);
            }
        }
    }
}
