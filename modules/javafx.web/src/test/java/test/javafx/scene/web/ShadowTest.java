/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.web.WebEngineShim;
import org.junit.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ShadowTest extends TestBase {
    /**
     * @test
     * @bug 8334124
     * summary
     * Paints an text with a red shadow, and another text without shadow.
     * Checks by red pixel count if the shadow exists and only exists on the first text.
     */
    @Test public void testShadow() {
        loadContent("<html>\n" +
                    "<body style='margin: 0px;'>\n" +
                    "<p style='text-shadow:5px 5px 0 #FF0000;height: 100px;'>text</p>\n" +
                    "<p style='height: 100px'>text</p>\n" +
                    "</body>\n" +
                    "</html>");
        submit(() -> {
                final WebPage webPage = WebEngineShim.getPage(getEngine());
                assertNotNull(webPage);
                final BufferedImage img = WebPageShim.paint(webPage, 0, 0, 800, 600);
                assertNotNull(img);

                int redShadowCnt = 0;
                int noShadowCnt = 0;
                for (int x = 0; x < 100; x++) {
                    for (int y = 0; y < 200; y++) {
                        Color pixelColor = new Color(img.getRGB(x, y), true);
                        if (isColorsSimilar(Color.RED, pixelColor, 1)) {
                            if (y < 100) {
                                redShadowCnt++;
                            } else {
                                noShadowCnt++;
                            }
                        }
                    }
                }

                assertTrue("no shadow found", redShadowCnt > 0);
                assertTrue("wrong shadow found", noShadowCnt == 0);
        });
    }
}
