/*
 * Copyright (c) 2015, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.web;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Base64;
import javax.imageio.ImageIO;

import netscape.javascript.JSObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;

/**
 * Test the Image to DataURL function
 */
public class CanvasTest extends TestBase {

    private static final PrintStream ERR = System.err;

    // JDK-8162922
    @Test public void testCanvasStrokeRect() {
        final String htmlCanvasContent = "\n"
            + "<!DOCTYPE html>\n"
            + "<html>\n"
            + "<body>\n"
            + "<canvas id=\"myCanvas\" width=\"200\" height=\"100\">\n"
            + "</canvas>\n"
            + "<script>\n"
            + "var c = document.getElementById(\"myCanvas\");\n"
            + "var ctx = c.getContext(\"2d\");\n"
            + "ctx.lineWidth = 4;\n"
            + "ctx.setLineDash([4,4]);\n"
            + "ctx.strokeStyle = '#f00';\n"
            + "ctx.strokeRect(10,30,70,70);\n"
            + "var imageData = ctx.getImageData(10, 30, 60, 60);\n"
            + "window.data = imageData.data;\n"
            + "</script>\n"
            + "</body>\n"
            + "</html>\n";

        loadContent(htmlCanvasContent);

        // Without the fix for JDK-8162922, canvas image data will be like below, which is wrong.
        /**
         final int[] wrongPixelArray = {255, 0, 0, 255,
                                        255, 0, 0, 255,
                                        255, 0, 0, 255,
                                        255, 0, 0, 255,
                                        255, 0, 0, 255,
                                        255, 0, 0, 255,
                                        255, 0, 0, 255,
                                        255, 0, 0, 255,};
         */

        // Sample pixel array to test against the canvas image data (with fix for JDK-8162922)
        final int[] expectedPixelArray = {255, 0, 0, 255,
                                          255, 0, 0, 255,
                                          255, 0, 0, 255,
                                          255, 0, 0, 255,
                                          0, 0, 0, 0,
                                          0, 0, 0, 0,
                                          0, 0, 0, 0,
                                          0, 0, 0, 0};

        submit(() -> {
            final JSObject obj = (JSObject) getEngine().executeScript("window.data");
            assertEquals(1, (int) getEngine().executeScript("window.devicePixelRatio"), "Device Pixel Ratio should be 1");
            // Due to mismatch of first pixel(probably a bug), we are skipping first pixel and testing
            // from second pixel onwards (from 16th value) till next 3 pixels (till 47th value)
            for (int i = 16; i < 48; i++) {
                assertEquals(expectedPixelArray[i - 16], (int)obj.getSlot(i), "StrokeRect pixel data is same");
            }
        });
    }

    // JDK-8191035
    @Test public void testCanvasArc() {
        final String htmlCanvasArc =
                "<canvas id='canvas' width='600' height='300'></canvas> <script>" +
                        "var context = document.getElementById('canvas').getContext('2d');" +
                        "context.beginPath();" +
                        "context.arc(300, 150, 75, -1.5707, 2.1362, false);" +
                        "context.strokeStyle = 'red';" +
                        "context.stroke();  </script>";

        loadContent(htmlCanvasArc);
        submit(() -> {
            int redColor = 255;
            assertEquals(redColor, (int) getEngine().executeScript("document.getElementById('canvas').getContext('2d').getImageData(260,213,1,1).data[0]"), "Arc startAngle");
            assertEquals(redColor, (int) getEngine().executeScript("document.getElementById('canvas').getContext('2d').getImageData(300,75,1,1).data[0]"), "Arc endAngle");
        });
    }

    // JDK-8234471
    @Disabled("JDK-8347937")
    @Test public void testCanvasPattern() throws Exception {
        final String htmlCanvasContent = "\n"
            + "<canvas id='canvaspattern' width='100' height='100'></canvas>\n"
            + "<svg id='svgpattern'></svg>\n"
            + "<script>\n"
            + "var patternCanvas = document.createElement('canvas');\n"
            + "var patternCtx = patternCanvas.getContext('2d');\n"
            + "patternCanvas.width = patternCanvas.height = 30;\n"
            + "patternCtx.fillStyle = 'red';\n"
            + "patternCtx.fillRect(0, 0, 20, 20);\n"
            + "\n"
            + "var ctx = document.getElementById('canvaspattern').getContext('2d');\n"
            + "var pattern = ctx.createPattern(patternCanvas, 'repeat');\n"
            + "var matrix = document.getElementById('svgpattern').createSVGMatrix();\n"
            + "pattern.setTransform(matrix.translate(10, 10));\n"
            + "ctx.fillStyle = pattern;\n"
            + "ctx.fillRect(0, 0, 100, 100);\n"
            + "</script>\n";

        loadContent(htmlCanvasContent);
        submit(() -> {
            int redColor = 255;
            assertEquals(
                    0,
                    (int) getEngine().executeScript(
                            "document.getElementById('canvaspattern').getContext('2d').getImageData(1, 1, 1, 1).data[0]"),
                    "Pattern top-left corner");

            assertEquals(
                    redColor,
                    (int) getEngine().executeScript(
                            "document.getElementById('canvaspattern').getContext('2d').getImageData(11, 11, 1, 1).data[0]"),
                    "First rect top-left");

            assertEquals(
                    redColor,
                    (int) getEngine().executeScript(
                            "document.getElementById('canvaspattern').getContext('2d').getImageData(21, 21, 1, 1).data[0]"),
                    "First rect center");
        });
    }

    private BufferedImage htmlCanvasToBufferedImage(final String mime) throws Exception {
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errStream));

        final String html = String.format(""
            + "<body>"
            + "<script>"
            + "canvas = document.createElement('canvas');"
            + "canvas.width = canvas.height = 100;"
            + "var ctx = canvas.getContext('2d');"
            + "ctx.fillStyle = 'red';"
            + "ctx.fillRect(0, 0, 50, 100);"
            + "data = canvas.toDataURL('%s');"
            + "</script>"
            + "</body>"
        , mime);

        loadContent(html);
        System.setErr(ERR);

        // Check whether any exception thrown
        final String exMessage = errStream.toString();
        assertFalse(
                exMessage.contains("Exception") || exMessage.contains("Error"),
                String.format("Test failed with exception:\n%s", exMessage));
        String img = (String) executeScript("window.data");
        assertNotNull("window.data must have base64 encoded image", img);
        // get rid of mime type
        img = img.split(",")[1];
        assertNotNull(img);

        final byte[] imgBytes = Base64.getMimeDecoder().decode(img);
        assertNotNull(imgBytes, "Base64 decoded image data must be valid");
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(imgBytes);
        final BufferedImage decodedImg = ImageIO.read(inputStream);
        assertNotNull(decodedImg);
        return decodedImg;
    }

    @Test
    public void testColorSimilarityAlgorithm() {
        assertTrue(isColorsSimilar(Color.WHITE, Color.WHITE, 0), "Two Color.WHITE must be 100% equal");
        assertTrue(isColorsSimilar(Color.WHITE, Color.BLACK, 100), "Color.BLACK & Color.WHITE must be 100% different");

        assertFalse(isColorsSimilar(Color.WHITE, Color.BLACK, 80), "Color.BLACK & Color.WHITE must be different by at least 80%");
        assertFalse(isColorsSimilar(Color.WHITE, new Color(0, true), 99.99f), "(0, 0, 0, 0) & Color.WHITE must be at least 99.99% different");

        assertTrue(isColorsSimilar(Color.RED, new Color(255, 0, 0, 255), 0), "Color.RED must be 100% equal to (255, 0, 0, 255)");
        assertTrue(isColorsSimilar(Color.RED, new Color(255, 0, 0, 250), 1), "Color.RED must be at least 99% similar to (255, 0, 0, 250)");
        assertTrue(isColorsSimilar(Color.RED, new Color(250, 5, 5, 250), 5), "Color.RED must be at least 95% similar to (250, 5, 5, 250)");

        assertTrue(isColorsSimilar(Color.GREEN, new Color(0, 255, 0, 255), 0), "Color.GREEN must be 100% equal to (0, 255, 0, 255)");
        assertTrue(isColorsSimilar(Color.GREEN, new Color(0, 255, 0, 250), 1), "Color.GREEN must be at least 99% similar to (0, 255, 0, 250)");
        assertTrue(isColorsSimilar(Color.GREEN, new Color(5, 250, 5, 250), 5), "Color.GREEN must be at least 95% similar to (5, 250, 5, 250)");

        assertTrue(isColorsSimilar(Color.BLUE, new Color(0, 0, 255, 255), 0), "Color.BLUE must be 100% equal to (0, 255, 0, 255)");
        assertTrue(isColorsSimilar(Color.BLUE, new Color(0, 0, 255, 250), 1), "Color.BLUE must be at least 99% similar to (0, 0, 255, 250)");
        assertTrue(isColorsSimilar(Color.BLUE, new Color(5, 5, 250, 250), 5), "Color.BLUE must be at least 95% similar to (5, 5, 250, 250)");

        assertTrue(isColorsSimilar(new Color(0, true), new Color(5, 5, 5, 5), 5), "(0, 0, 0, 0) must be at least 95% similar to (5, 5, 5, 5)");
        assertFalse(isColorsSimilar(new Color(0, true), new Color(5, 5, 5, 5), 1), "(0, 0, 0, 0) and (5, 5, 5, 5) must be different by at least 1%");

        assertTrue(isColorsSimilar(Color.RED, Color.GREEN, 75), "Color.RED must be at least 25% similar to Color.GREEN");
        assertFalse(isColorsSimilar(Color.RED, Color.GREEN, 70), "Color.RED and Color.GREEN must be different by at least 70%");
    }

    @Test
    public void testToDataURLWithPNGMimeType() throws Exception {
        final BufferedImage decodedImg = htmlCanvasToBufferedImage("image/png");

        // Pixel at (25 x 25) must be red
        final Color pixelAt25x25 = new Color(decodedImg.getRGB(25, 25), true);
        assertTrue(isColorsSimilar(Color.RED, pixelAt25x25, 1), "Color should be opaque red:" + pixelAt25x25);

        // PNG supports transparency, Pixel at (75 x 25) must be transparent black
        final Color pixelAt75x25 = new Color(decodedImg.getRGB(75, 25), true);
        assertTrue(isColorsSimilar(new Color(0, true), pixelAt75x25, 1), "Color should be transparent black:" + pixelAt75x25);
    }

    @Test
    public void testToDataURLWithJPEGMimeType() throws Exception {
        final BufferedImage decodedImg = htmlCanvasToBufferedImage("image/jpeg");

        // Pixel at (25 x 25) must be red
        final Color pixelAt25x25 = new Color(decodedImg.getRGB(25, 25), true);
        assertTrue(isColorsSimilar(Color.RED, pixelAt25x25, 1), "Color should be opaque red:" + pixelAt25x25);

        // JPEG doesn't supports transparency, Pixel at (75 x 25) must be opaque black
        final Color pixelAt75x25 = new Color(decodedImg.getRGB(75, 25), true);
        assertTrue(isColorsSimilar(Color.BLACK, pixelAt75x25, 1), "Color should be transparent black:" + pixelAt75x25);
    }

    @AfterEach
    public void resetSystemErr() {
        System.setErr(ERR);
    }
}
