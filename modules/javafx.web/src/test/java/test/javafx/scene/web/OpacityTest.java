/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.web.WebEngineShim;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OpacityTest extends TestBase {
    /**
     * @test
     * @bug 8298167
     * summary
     * Loads black-background areas with different opacity values: 1.0, 0.5, and 0.0.
     * Checks if the areas are rendered with black, gray, and white colors accordigly.
     * Colors are taken from the center of each area.
     */
    @Test public void testOpacity() {
        loadContent("<html>\n" +
                    "<body style='margin: 0px;'>\n" +
                    "<p style='opacity: 1.0; height: 100px; margin: 0px; background-color:#000; color: #fff;'>text</p>\n" +
                    "<p style='opacity: 0.5; height: 100px; margin: 0px; background-color:#000; color: #fff;'>text</p>\n" +
                    "<p style='opacity: 0.0; height: 100px; margin: 0px; background-color:#000; color: #fff;'>text</p>\n" +
                    "</body>\n" +
                    "</html>");
        submit(() -> {
                final WebPage webPage = WebEngineShim.getPage(getEngine());
                assertNotNull(webPage);
                final BufferedImage img = WebPageShim.paint(webPage, 0, 0, 800, 600);
                assertNotNull(img);

                final Color pixelAt400x50 = new Color(img.getRGB(400, 50), true);
                assertTrue("Color should be black:" + pixelAt400x50, isColorsSimilar(Color.BLACK, pixelAt400x50, 1));
                final Color pixelAt400x150 = new Color(img.getRGB(400, 150), true);
                assertTrue("Color should be gray:" + pixelAt400x150, isColorsSimilar(Color.GRAY, pixelAt400x150, 1));
                final Color pixelAt400x250 = new Color(img.getRGB(400, 250), true);
                assertTrue("Color should be white:" + pixelAt400x250, isColorsSimilar(Color.WHITE, pixelAt400x250, 1));
        });
    }
}
