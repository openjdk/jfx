/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package test.javafx.embed.swing;

import java.awt.image.BufferedImage;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import junit.framework.Assert;

public class FXImageConversionTest {

    private static int width = 100;
    private static int height = 100;

    @Test public void testImageConversionRGBOpaque() {
        WritableImage newimage = new WritableImage(width, height);
        PixelWriter pw = newimage.getPixelWriter();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pw.setArgb(x, y, 0xff000000);
            }
        }
        try {
            BufferedImage b = new BufferedImage(width + 1, height + 1,
                                                BufferedImage.TYPE_INT_RGB);
            BufferedImage bf = SwingFXUtils.fromFXImage(newimage, b);

            assertTrue(bf.getType() == BufferedImage.TYPE_INT_RGB);
        } catch (ClassCastException cex) {
            Assert.fail("FX image conversion wrong cast " + cex);
        }
    }

    @Test public void testImageConversionRGBNotOpaque() {
        WritableImage newimage = new WritableImage(width, height);
        PixelWriter pw = newimage.getPixelWriter();
        for (int x = 0; x < width/2; x++) {
            for (int y = 0; y < height/2; y++) {
                pw.setArgb(x, y, 0xff000000);
            }
        }
        try {
            BufferedImage b = new BufferedImage(width + 1, height + 1,
                                                BufferedImage.TYPE_INT_RGB);
            BufferedImage bf = SwingFXUtils.fromFXImage(newimage, b);

            assertTrue(bf.getType() == BufferedImage.TYPE_INT_ARGB_PRE);
        } catch (ClassCastException cex) {
            Assert.fail("FX image conversion wrong cast " + cex);
        }
    }

    @Test public void testImageConversionGray() {
        WritableImage newimage = new WritableImage(width, height);
        PixelWriter pw = newimage.getPixelWriter();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pw.setArgb(x, y, 0xff000000);
            }
        }
        try {
            BufferedImage b = new BufferedImage(width + 1, height + 1,
                                                BufferedImage.TYPE_BYTE_GRAY);
            BufferedImage bf = SwingFXUtils.fromFXImage(newimage, b);

            assertTrue(bf.getType() == BufferedImage.TYPE_INT_ARGB_PRE);
        } catch (ClassCastException cex) {
            Assert.fail("FX image conversion wrong cast " + cex);
        }
    }
}
