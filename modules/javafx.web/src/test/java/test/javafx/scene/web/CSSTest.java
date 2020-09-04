/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static javafx.concurrent.Worker.State.FAILED;
import static javafx.concurrent.Worker.State.SUCCEEDED;

import com.sun.webkit.WebPage;
import com.sun.webkit.WebPageShim;

import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import javafx.concurrent.Worker.State;
import javafx.scene.Scene;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.web.WebEngineShim;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

public class CSSTest extends TestBase {

    private State getLoadState() {
        return submit(() -> getEngine().getLoadWorker().getState());
    }

    private void setStyle(final String style) {
        submit(() -> {
            getView().setStyle(style);
            new Scene(getView()).snapshot(null);
        });
    }


    private void testContextMenuEnabled(boolean expected) {
        assertEquals(expected, getView().isContextMenuEnabled());
    }

    @Test public void testContextMenuEnabledDefault() {
        testContextMenuEnabled(true);
    }

    @Test public void testContextMenuEnabledManual() {
        submit(() -> {
            getView().setContextMenuEnabled(false);
            testContextMenuEnabled(false);
        });
    }

    @Test public void testContextMenuEnabledCSS() {
        setStyle("-fx-context-menu-enabled: false");
        submit(() -> {
            testContextMenuEnabled(false);
        });
    }


    private void testZoom(double expected) {
        assertEquals(expected, getView().getZoom(), 0);
    }

    @Test public void testZoomDefault() {
        testZoom(1);
    }

    @Test public void testZoomManual() {
        submit(() -> {
            getView().setZoom(3);
            testZoom(3);
        });
    }

    @Test public void testZoomCSS() {
        setStyle("-fx-zoom: .3");
        submit(() -> {
            testZoom(0.3);
        });
    }


    private void testFontSmoothingType(FontSmoothingType expected) {
        assertEquals(expected, getView().getFontSmoothingType());
    }

    @Test public void testFontSmoothingTypeDefault() {
        testFontSmoothingType(FontSmoothingType.LCD);
    }

    @Test public void testFontSmoothingTypeManual() {
        submit(() -> {
            getView().setFontSmoothingType(FontSmoothingType.GRAY);
            testFontSmoothingType(FontSmoothingType.GRAY);
        });
    }

    @Test public void testFontSmoothingTypeCSS() {
        setStyle("-fx-font-smoothing-type: gray");
        submit(() -> {
            testFontSmoothingType(FontSmoothingType.GRAY);
        });
    }


    private void testFontScale(double expected) {
        assertEquals(expected, getView().getFontScale(), 0);
    }

    @Test public void testFontScaleDefault() {
        testFontScale(1);
    }

    @Test public void testFontScaleManual() {
        submit(() -> {
            getView().setFontScale(2);
            testFontScale(2);
        });
    }

    @Test public void testFontScaleCSS() {
        setStyle("-fx-font-scale: .2");
        submit(() -> {
            testFontScale(0.2);
        });
    }


    private void testMinWidth(double expected) {
        assertEquals(expected, getView().getMinWidth(), 0);
    }

    @Test public void testMinWidthDefault() {
        testMinWidth(0);
    }

    @Test public void testMinWidthManual() {
        submit(() -> {
            getView().setMinWidth(2);
            testMinWidth(2);
        });
    }

    @Test public void testMinWidthCSS() {
        setStyle("-fx-min-width: 3px");
        submit(() -> {
            testMinWidth(3);
        });
    }


    private void testMinHeight(double expected) {
        assertEquals(expected, getView().getMinHeight(), 0);
    }

    @Test public void testMinHeightDefault() {
        testMinHeight(0);
    }

    @Test public void testMinHeightManual() {
        submit(() -> {
            getView().setMinHeight(2);
            testMinHeight(2);
        });
    }

    @Test public void testMinHeightCSS() {
        setStyle("-fx-min-height: 3px");
        submit(() -> {
            testMinHeight(3);
        });
    }


    private void testPrefWidth(double expected) {
        assertEquals(expected, getView().getPrefWidth(), 0);
    }

    @Test public void testPrefWidthDefault() {
        testPrefWidth(800);
    }

    @Test public void testPrefWidthManual() {
        submit(() -> {
            getView().setPrefWidth(2);
            testPrefWidth(2);
        });
    }

    @Test public void testPrefWidthCSS() {
        setStyle("-fx-pref-width: 3px");
        submit(() -> {
            testPrefWidth(3);
        });
    }


    private void testPrefHeight(double expected) {
        assertEquals(expected, getView().getPrefHeight(), 0);
    }

    @Test public void testPrefHeightDefault() {
        testPrefHeight(600);
    }

    @Test public void testPrefHeightManual() {
        submit(() -> {
            getView().setPrefHeight(2);
            testPrefHeight(2);
        });
    }

    @Test public void testPrefHeightCSS() {
        setStyle("-fx-pref-height: 3px");
        submit(() -> {
            testPrefHeight(3);
        });
    }


    private void testMaxWidth(double expected) {
        assertEquals(expected, getView().getMaxWidth(), 0);
    }

    @Test public void testMaxWidthDefault() {
        testMaxWidth(Double.MAX_VALUE);
    }

    @Test public void testMaxWidthManual() {
        submit(() -> {
            getView().setMaxWidth(2);
            testMaxWidth(2);
        });
    }

    @Test public void testMaxWidthCSS() {
        setStyle("-fx-max-width: 3px");
        submit(() -> {
            testMaxWidth(3);
        });
    }


    private void testMaxHeight(double expected) {
        assertEquals(expected, getView().getMaxHeight(), 0);
    }

    @Test public void testMaxHeightDefault() {
        testMaxHeight(Double.MAX_VALUE);
    }

    @Test public void testMaxHeightManual() {
        submit(() -> {
            getView().setMaxHeight(2);
            testMaxHeight(2);
        });
    }

    @Test public void testMaxHeightCSS() {
        setStyle("-fx-max-height: 3px");
        submit(() -> {
            testMaxHeight(3);
        });
    }

    @Test public void testLongSelectorList() {
        final String FILE = "src/test/resources/test/html/longselectorlist.html";
        load(new File(FILE));
        assertEquals("Loading Long SelectorList completed successfully", SUCCEEDED, getLoadState());
    }

    @Test public void testBorderRadiusPropertyRendering() {
        loadContent(
                "<!DOCTYPE html>\n" +
                "<html>\n" +
                "  <head>\n" +
                "    <style>\n" +
                "      button {\n" +
                "        background-color: black; color: white; display: block; font-size: 32px;\n" +
                "        width: 200px; height: 100px; padding: 0; border: none;\n" +
                "        border-radius: 32px;\n" +
                "      }\n" +
                "      .bad0 {\n" +
                "        background-color: red;\n" +
                "      }\n" +
                "      .bad1 {\n" +
                "        border-bottom-left-radius: 0;\n" +
                "        background-color: blue;\n" +
                "      }\n" +
                "      .bad2 {\n" +
                "        border-bottom-left-radius: 0;\n" +
                "        border-bottom-right-radius: 0;\n" +
                "        background-color: green;\n" +
                "      }\n" +
                "      .bad3 {\n" +
                "        border-bottom-left-radius: 0;\n" +
                "        border-bottom-right-radius: 0;\n" +
                "        border-top-right-radius: 0;\n" +
                "      }\n" +
                "    </style>\n" +
                "  </head>\n" +
                "  <body style='margin: 0px 0px;'>\n" +
                "    <button class=\"bad0\">A</button>\n" +
                "    <button class=\"bad1\">B</button>\n" +
                "    <button class=\"bad2\">C</button>\n" +
                "    <button class=\"bad3\">D</button>\n" +
                "  </body>\n" +
                "</html>"
        );
        submit(() -> {
            final WebPage webPage = WebEngineShim.getPage(getEngine());
            assertNotNull(webPage);
            final BufferedImage img = WebPageShim.paint(webPage, 0, 0, 800, 600);
            assertNotNull(img);

            final Color pixelAt0x0 = new Color(img.getRGB(0, 0), true);
            assertFalse("Color should not be red:" + pixelAt0x0, isColorsSimilar(Color.RED, pixelAt0x0, 1));
            final Color pixelAt199x0 = new Color(img.getRGB(199, 0), true);
            assertFalse("Color should not be red:" + pixelAt199x0, isColorsSimilar(Color.RED, pixelAt199x0, 1));
            final Color pixelAt0x99 = new Color(img.getRGB(0, 99), true);
            assertFalse("Color should not be red:" + pixelAt0x99, isColorsSimilar(Color.RED, pixelAt0x99, 1));
            final Color pixelAt199x99 = new Color(img.getRGB(199, 99), true);
            assertFalse("Color should not be red:" + pixelAt199x99, isColorsSimilar(Color.RED, pixelAt199x99, 1));

            final Color pixelAt0x100 = new Color(img.getRGB(0, 100), true);
            assertFalse("Color should not be blue:" + pixelAt0x100, isColorsSimilar(Color.BLUE, pixelAt0x100, 1));
            final Color pixelAt199x100 = new Color(img.getRGB(199, 100), true);
            assertFalse("Color should not be blue:" + pixelAt199x100, isColorsSimilar(Color.BLUE, pixelAt199x100, 1));
            final Color pixel0x199 = new Color(img.getRGB(0, 199), true);
            assertTrue("Color should be opaque blue:" + pixel0x199, isColorsSimilar(Color.BLUE, pixel0x199, 1));
            final Color pixelAt199x199 = new Color(img.getRGB(199, 199), true);
            assertFalse("Color should not be blue:" + pixelAt199x199, isColorsSimilar(Color.BLUE, pixelAt199x199, 1));
        });
    }
}
