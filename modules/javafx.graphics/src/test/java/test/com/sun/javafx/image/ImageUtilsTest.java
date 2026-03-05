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

package test.com.sun.javafx.image;

import com.sun.javafx.image.ImageUtils;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ImageUtilsTest {

    @Test
    void solidOpaqueReturnsSameColor() {
        var img = new WritableImage(16, 16);
        fillArgb(img, 0xffff0000); // opaque red
        var result = ImageUtils.computeDominantColor(img, Color.WHITE, 4, 1, 0);
        assertClose(result, 255, 0, 0, 2);
    }

    @Test
    void fullyTransparentReturnsBackground() {
        var img = new WritableImage(16, 16);
        fillArgb(img, 0x00000000); // fully transparent
        Color result = ImageUtils.computeDominantColor(img, Color.WHITE, 4, 1, 0);
        assertClose(result, 255, 255, 255, 2);
    }

    @Test
    void majorityColorWins() {
        var img = new WritableImage(20, 10);
        fillArgb(img, 0xff00ff00); // green background
        fillRectArgb(img, 0, 0, 4, 10, 0xff0000ff); // 20% blue strip
        var result = ImageUtils.computeDominantColor(img, Color.WHITE, 2, 1, 0);
        assertClose(result, 0, 255, 0, 3); // should be green
    }

    @Test
    void transparentAreaCanMakeBackgroundDominant() {
        var img = new WritableImage(20, 10);
        fillArgb(img, 0x00000000); // transparent everywhere
        fillRectArgb(img, 0, 0, 4, 10, 0xffff0000); // 20% opaque red
        var result = ImageUtils.computeDominantColor(img, Color.WHITE, 2, 1, 0);
        assertClose(result, 255, 255, 255, 3); // background dominates
    }

    @Test
    void partialAlphaCompositesOverBackground() {
        // 50% alpha red (A=0x80) over white produces ~ (255,127,127)
        var img = new WritableImage(16, 16);
        fillArgb(img, 0x80ff0000);
        var result = ImageUtils.computeDominantColor(img, Color.WHITE, 2, 1, 0);
        assertClose(result, 255, 127, 127, 3);
    }

    @Test
    void halfTransparentBlackOverWhite_isMidGray_inSRGBBlending() {
        // 1x1 pixel, 50% alpha black
        var img = new WritableImage(1, 1);
        var pw = img.getPixelWriter();
        pw.setArgb(0, 0, 0x80000000);

        // Compute dominant color as composited over white
        var result = ImageUtils.computeDominantColor(img, Color.WHITE, 1, 1, 0);

        // Since blending is done in sRGB space, 50% black over white is ~0.5 => ~128
        assertClose(result, 128, 128, 128, 3);
    }

    private static void fillArgb(WritableImage img, int argb) {
        PixelWriter pw = img.getPixelWriter();

        for (int y = 0; y < (int) img.getHeight(); y++) {
            for (int x = 0; x < (int) img.getWidth(); x++) {
                pw.setArgb(x, y, argb);
            }
        }
    }

    private static void fillRectArgb(WritableImage img, int x0, int y0, int w, int h, int argb) {
        PixelWriter pw = img.getPixelWriter();

        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                pw.setArgb(x, y, argb);
            }
        }
    }

    private static void assertClose(Color actual, int r, int g, int b, int delta) {
        int r8 = (int)Math.round(actual.getRed() * 255.0);
        int g8 = (int)Math.round(actual.getGreen() * 255.0);
        int b8 = (int)Math.round(actual.getBlue() * 255.0);
        assertTrue(Math.abs(r8 - r) <= delta, "R expected " + r + ", got " + r8);
        assertTrue(Math.abs(g8 - g) <= delta, "G expected " + g + ", got " + g8);
        assertTrue(Math.abs(b8 - b) <= delta, "B expected " + b + ", got " + b8);
    }
}
