/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import static java.util.Arrays.asList;
import com.sun.webkit.WebPage;
import com.sun.webkit.WebPageShim;
import javafx.scene.web.WebEngineShim;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SVGPointerEventsTest extends TestBase {

    /**
     * This test loads a website with SVG shapes "polyline", "path", "rect",
     * "circle", "ellipse", "polygon". In addition a "dashed" path is included.
     * By setting the CSS property "pointer-events" to "stroke", the rendering
     * engine is configured to only react to clicks on the stroke of the shapes.
     *
     * <p>With a javascript event click handler, mouse events on the shapes
     * are recorded.</p>
     *
     * <p>This tests simulates clicks on the stroke, so the shapes should have
     * received clicks and report being activated.</p>
     */
    @Test
    public void testClickOnStrokePointerEventsStroke() throws Exception {
        load(SVGPointerEventsTest.class.getClassLoader().getResource("test/html/pointerevents-stroke.html").toExternalForm());
        submit(() -> {
            final WebPage page = WebEngineShim.getPage(getEngine());
            // Render WebPage so that all of the svg paths also will be rendered.
            WebPageShim.paint(page, 0, 0, 800, 600);

            WebPageShim.click(page, 130, 80);
            WebPageShim.click(page, 330, 80);
            WebPageShim.click(page, 530, 80);
            WebPageShim.click(page, 130, 280);
            WebPageShim.click(page, 330, 280);
            WebPageShim.click(page, 530, 280);
            WebPageShim.click(page, 70, 410);

            for (String s : asList("polyline", "path", "rect", "circle", "ellipse", "polygon", "dashed")) {
                assertTrue("Expected element '" + s + "' to be activated", (boolean) getEngine().executeScript("isActivated('" + s + "')"));
            }
        });
    }


    /**
     * This test loads a website with SVG shapes "polyline", "path", "rect",
     * "circle", "ellipse", "polygon". In addition a "dashed" path is included.
     * By setting the CSS property "pointer-events" to "stroke", the rendering
     * engine is configured to only react to clicks on the stroke of the shapes.
     *
     * <p>With a javascript event click handler, mouse events on the shapes
     * are recorded.</p>
     *
     * <p>This tests simulates clicks on the fill, so the shapes should not
     * receive clicks.</p>
     */
    @Test
    public void testClickOnFillPointerEventsStroke() throws Exception {
        load(SVGPointerEventsTest.class.getClassLoader().getResource("test/html/pointerevents-stroke.html").toExternalForm());
        submit(() -> {
            final WebPage page = WebEngineShim.getPage(getEngine());
            // Render WebPage so that all of the svg paths also will be rendered.
            WebPageShim.paint(page, 0, 0, 800, 600);

            WebPageShim.click(page, 80, 80);
            WebPageShim.click(page, 280, 80);
            WebPageShim.click(page, 480, 80);
            WebPageShim.click(page, 80, 280);
            WebPageShim.click(page, 280, 280);
            WebPageShim.click(page, 480, 280);
            WebPageShim.click(page, 30, 410);

            for (String s : asList("polyline", "path", "rect", "circle", "ellipse", "polygon", "dashed")) {
                assertFalse("Expected element '" + s + "' not to be activated", (boolean) getEngine().executeScript("isActivated('" + s + "')"));
            }
        });
    }
}
