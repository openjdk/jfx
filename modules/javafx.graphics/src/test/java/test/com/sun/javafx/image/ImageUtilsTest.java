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
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

public class ImageUtilsTest {

    @Test
    void solidOpaqueReturnsSameColor() {
        var img = new WritableImage(16, 16);
        fillArgb(img, 0xffff0000); // opaque red
        Color resultOnWhite = ImageUtils.computeDominantColor(img, Color.WHITE);
        assertEquals(Color.rgb(255, 0, 0), resultOnWhite);
        Color resultOnBlack = ImageUtils.computeDominantColor(img, Color.BLACK);
        assertEquals(Color.rgb(255, 0, 0), resultOnBlack);
    }

    @Test
    void fullyTransparentReturnsBackground() {
        var img = new WritableImage(16, 16);
        fillArgb(img, 0x00000000); // fully transparent
        Color resultOnWhite = ImageUtils.computeDominantColor(img, Color.WHITE);
        assertEquals(Color.WHITE, resultOnWhite);
        Color resultOnBlack = ImageUtils.computeDominantColor(img, Color.BLACK);
        assertEquals(Color.BLACK, resultOnBlack);
    }

    @Test
    void transparentAreaMakesBackgroundDominant() {
        var img = new WritableImage(20, 10);
        fillArgb(img, 0x00000000); // transparent everywhere
        fillRectArgb(img, 0, 0, 4, 10, 0xffff0000); // 20% opaque red
        Color resultOnWhite = ImageUtils.computeDominantColor(img, Color.WHITE);
        assertEquals(Color.WHITE, resultOnWhite);
        Color resultOnBlack = ImageUtils.computeDominantColor(img, Color.BLACK);
        assertEquals(Color.BLACK, resultOnBlack);
    }

    @Test
    void majorityColorWins() {
        var img = new WritableImage(20, 10);
        fillArgb(img, 0xff00ff00); // green background
        fillRectArgb(img, 0, 0, 4, 10, 0xff0000ff); // 20% blue strip
        Color resultOnWhite = ImageUtils.computeDominantColor(img, Color.WHITE);
        assertEquals(Color.rgb(0, 255, 0), resultOnWhite);
        Color resultOnBlack = ImageUtils.computeDominantColor(img, Color.BLACK);
        assertEquals(Color.rgb(0, 255, 0), resultOnBlack);
    }

    @Test
    void partialAlphaCompositeOverBackground() {
        // 50% alpha red (A=0x80)
        var img = new WritableImage(16, 16);
        fillArgb(img, 0x80ff0000);
        Color resultOnWhite = ImageUtils.computeDominantColor(img, Color.WHITE);
        assertEquals(Color.rgb(255, 187, 187), resultOnWhite);
        Color resultOnBlack = ImageUtils.computeDominantColor(img, Color.BLACK);
        assertEquals(Color.rgb(188, 0, 0), resultOnBlack);
    }

    @Test
    void halfTransparentBlackOverWhite() {
        // 1x1 pixel, 50% alpha black
        var img = new WritableImage(1, 1);
        var pw = img.getPixelWriter();
        pw.setArgb(0, 0, 0x80000000);
        Color result = ImageUtils.computeDominantColor(img, Color.WHITE);
        assertEquals(Color.rgb(187, 187, 187), result);
    }

    @ParameterizedTest
    @MethodSource("testImages_data")
    void testImages(String fileName, Color background, Color expected) {
        Image image = loadImage(fileName);
        Color result = ImageUtils.computeDominantColor(image, background);
        assertEquals(expected, result);
    }

    static Stream<Arguments> testImages_data() {
        return Stream.of(
            Arguments.of("alpha-hole-green-frame-64x64.png", Color.BLACK, Color.rgb(76, 166, 84)),
            Arguments.of("alpha-hole-green-frame-64x64.png", Color.WHITE, Color.rgb(76, 166, 84)),
            Arguments.of("blue-bg-soft-red-circles-64x64.png", Color.WHITE, Color.rgb(75, 128, 215)),
            Arguments.of("layered-landscape-foliage-dominant-96x64.png", Color.WHITE, Color.rgb(76, 122, 62)),
            Arguments.of("near-greens-majority-skew-64x64.png", Color.WHITE, Color.rgb(114, 160, 78)),
            Arguments.of("olive-majority-confetti-noise-1024x1024.png", Color.WHITE, Color.rgb(123, 137, 65)),
            Arguments.of("orange-field-purple-dots-64x64.png", Color.WHITE, Color.rgb(228, 134, 44)),
            Arguments.of("teal-field-black-speckle-64x64.png", Color.WHITE, Color.rgb(45, 157, 157)),
            Arguments.of("transparent-amber-overlay-cutouts-64x64.png", Color.WHITE, Color.rgb(251, 236, 221)),
            Arguments.of("transparent-amber-overlay-cutouts-64x64.png", Color.BLACK, Color.rgb(136, 98, 23)),
            Arguments.of("transparent-teal-overlay-slits-64x64.png", Color.WHITE, Color.rgb(217, 237, 234)),
            Arguments.of("transparent-teal-overlay-slits-64x64.png", Color.BLACK, Color.rgb(14, 112, 103)),
            Arguments.of("transparent-violet-overlay-windows-64x64.png", Color.WHITE, Color.rgb(231, 210, 243)),
            Arguments.of("transparent-violet-overlay-windows-64x64.png", Color.BLACK, Color.rgb(115, 43, 142)),
            Arguments.of("warm-cool-diagonal-gradient-96x96.png", Color.WHITE, Color.rgb(218, 98, 73)),
            Arguments.of("white-bg-red-circle-64x64.png", Color.WHITE, Color.rgb(250, 250, 250))
        );
    }

    static Image loadImage(String fileName) {
        InputStream input = ImageUtilsTest.class.getResourceAsStream(fileName);
        assertNotNull(input, () -> "Missing image: " + fileName);

        try (input) {
            BufferedImage bufferedImage = ImageIO.read(input);
            WritableImage image = new WritableImage(bufferedImage.getWidth(), bufferedImage.getHeight());
            PixelWriter pixelWriter = image.getPixelWriter();

            for (int y = 0; y < bufferedImage.getHeight(); y++) {
                for (int x = 0; x < bufferedImage.getWidth(); x++) {
                    pixelWriter.setArgb(x, y, bufferedImage.getRGB(x, y));
                }
            }

            return image;
        } catch (Exception ex) {
            throw new AssertionError("Failed to read image: " + fileName, ex);
        }
    }

    static void fillArgb(WritableImage img, int argb) {
        PixelWriter pw = img.getPixelWriter();

        for (int y = 0; y < (int) img.getHeight(); y++) {
            for (int x = 0; x < (int) img.getWidth(); x++) {
                pw.setArgb(x, y, argb);
            }
        }
    }

    static void fillRectArgb(WritableImage img, int x0, int y0, int w, int h, int argb) {
        PixelWriter pw = img.getPixelWriter();

        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                pw.setArgb(x, y, argb);
            }
        }
    }
}
