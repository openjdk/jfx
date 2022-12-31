/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.webkit.WebPage;
import com.sun.webkit.WebPageShim;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import javafx.scene.web.WebEngineShim;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SVGTest extends TestBase {
    /**
     * @test
     * @bug 8163582
     * summary svg.path.getTotalLength
     * Load a simple SVG, Replace its path and get its path's totalLength using pat.getTotalLength
     */
    @Test(timeout = 30000) public void testSvgGetTotalLength() throws Exception {
        final String svgStub = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 512 512'>" +
                " <path id='pathId' d='M150 0 L75 200 L225 200 Z' /> <svg>";

        // <Path, [Expected, Error Tolerance]>
        final HashMap<String, Double[]> svgPaths = new HashMap<>();
        svgPaths.put("'M 0 0 L 100 0 L 100 100 L 0 100 Z'",
                new Double[] {400.0, 0.000001});
        svgPaths.put("'M 0 0 l 100 0 l 0 100 l -100 0 Z'",
                new Double[] {400.0, 0.000001});
        svgPaths.put("'M 0 0 t 0 100'",
                new Double[] {100.0, 0.1});
        svgPaths.put("'M 0 0 Q 55 50 100 100'",
                new Double[] {141.4803314, 0.001});
        svgPaths.put("'M 778.4191616766467 375.19086364081954 C 781.239563 " +
                        "375.1908569 786.8525244750526 346.60170830052556 786.8802395209582 346.87991373394766'",
                new Double[] {29.86020, 0.1});
        svgPaths.put("'M 0 0 C 0.00001 0.00001 0.00002 0.00001 0.00003 0'",
                new Double[] {0.0000344338, 0.0001});

        loadContent(svgStub);

        svgPaths.forEach((pathData, expected) -> {
            executeScript("document.getElementById('pathId').setAttribute('d' , " + pathData + ");");
            // Get svg path's total length
            Double totalLength = ((Number) executeScript("document.getElementById('pathId').getTotalLength();")).doubleValue();
            final String msg = String.format(
                    "svg.path.getTotalLength() for %s",
                    pathData);
            assertEquals(msg,
                    expected[0], totalLength, expected[1]);
        });
    }

    @Test public void testSVGRenderingWithGradient() {
        loadContent("<html>\n" +
                    "<body style='margin: 0px 0px;'>\n" +
                    "<svg width='400' height='150'>\n" +
                    "<defs>\n" +
                    "<linearGradient id='grad1' x1='0%' y1='0%' x2='100%' y2='100%'>\n" +
                    "<stop offset='0%' style='stop-color:red' />\n" +
                    "<stop offset='100%' style='stop-color:yellow' />\n" +
                    "</linearGradient>\n" +
                    "</defs>\n" +
                    "<rect width='400' height='150' fill='url(#grad1)' />\n" +
                    "</svg>\n" +
                    "</body>\n" +
                    "</html>");
        submit(() -> {
            final WebPage webPage = WebEngineShim.getPage(getEngine());
            assertNotNull(webPage);
            final BufferedImage img = WebPageShim.paint(webPage, 0, 0, 800, 600);
            assertNotNull(img);

            final Color pixelAt0x0 = new Color(img.getRGB(0, 0), true);
            assertTrue("Color should be opaque red:" + pixelAt0x0, isColorsSimilar(Color.RED, pixelAt0x0, 1));

            final Color pixelAt100x36 = new Color(img.getRGB(100, 36), true);
            assertTrue("Color should be almost red:" + pixelAt100x36, isColorsSimilar(Color.RED, pixelAt100x36, 40));
            assertFalse("Color shouldn't be yellow:" + pixelAt100x36, isColorsSimilar(Color.YELLOW, pixelAt100x36, 10));

            final Color pixelAt200x75 = new Color(img.getRGB(200, 75), true);
            assertFalse("Color shouldn't be red:" + pixelAt200x75, isColorsSimilar(Color.RED, pixelAt200x75, 10));
            assertTrue("Color should look like yellow:" + pixelAt200x75, isColorsSimilar(Color.YELLOW, pixelAt200x75, 40));

            final Color pixelAt399x145 = new Color(img.getRGB(399, 149), true);
            assertTrue("Color should be opaque yellow:" + pixelAt399x145, isColorsSimilar(Color.YELLOW, pixelAt399x145, 1));
        });
    }

    @Test public void testCrashOnScrollableSVG() {
        load(SVGTest.class.getClassLoader().getResource("test/html/crash-on-scrollable-svg.html").toExternalForm());
        submit(() -> {
            final WebPage webPage = WebEngineShim.getPage(getEngine());
            assertNotNull(webPage);
            final BufferedImage img = WebPageShim.paint(webPage, 0, 0, 800, 200);
            assertNotNull(img);

            // RED rectangle should be rendered with in 0,0, 100x100.
            final Color pixelAt0x0 = new Color(img.getRGB(0, 0), true);
            assertTrue("Color should be opaque red:" + pixelAt0x0, isColorsSimilar(Color.RED, pixelAt0x0, 1));
            final Color pixelAt50x50 = new Color(img.getRGB(50, 50), true);
            assertTrue("Color should be opaque red:" + pixelAt50x50, isColorsSimilar(Color.RED, pixelAt50x50, 1));
            final Color pixelAt99x99 = new Color(img.getRGB(99, 99), true);
            assertTrue("Color should be opaque red:" + pixelAt99x99, isColorsSimilar(Color.RED, pixelAt99x99, 1));

            // After 100x100, pixel should be WHITE.
            final Color pixelAt100x100 = new Color(img.getRGB(100, 100), true);
            assertTrue("Color should be white:" + pixelAt100x100, isColorsSimilar(Color.WHITE, pixelAt100x100, 1));
        });
    }
}
