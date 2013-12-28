/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import javafx.scene.Scene;
import javafx.scene.text.FontSmoothingType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CSSTest extends TestBase {

    private void setStyle(final String style) {
        submit(new Runnable() { public void run() {
            getView().setStyle(style);
            new Scene(getView()).snapshot(null);
        }});
    }


    private void testContextMenuEnabled(boolean expected) {
        assertEquals(expected, getView().isContextMenuEnabled());
    }

    @Test public void testContextMenuEnabledDefault() {
        testContextMenuEnabled(true);
    }

    @Test public void testContextMenuEnabledManual() {
        submit(new Runnable() { public void run() {
            getView().setContextMenuEnabled(false);
            testContextMenuEnabled(false);
        }});
    }

    @Test public void testContextMenuEnabledCSS() {
        setStyle("-fx-context-menu-enabled: false");
        submit(new Runnable() { public void run() {
            testContextMenuEnabled(false);
        }});
    }


    private void testZoom(double expected) {
        assertEquals(expected, getView().getZoom(), 0);
    }

    @Test public void testZoomDefault() {
        testZoom(1);
    }

    @Test public void testZoomManual() {
        submit(new Runnable() {
            public void run() {
                getView().setZoom(3);
                testZoom(3);
            }
        });
    }

    @Test public void testZoomCSS() {
        setStyle("-fx-zoom: .3");
        submit(new Runnable() {
            public void run() {
                testZoom(0.3);
            }
        });
    }


    private void testFontSmoothingType(FontSmoothingType expected) {
        assertEquals(expected, getView().getFontSmoothingType());
    }

    @Test public void testFontSmoothingTypeDefault() {
        testFontSmoothingType(FontSmoothingType.LCD);
    }

    @Test public void testFontSmoothingTypeManual() {
        submit(new Runnable() { public void run() {
            getView().setFontSmoothingType(FontSmoothingType.GRAY);
            testFontSmoothingType(FontSmoothingType.GRAY);
        }});
    }

    @Test public void testFontSmoothingTypeCSS() {
        setStyle("-fx-font-smoothing-type: gray");
        submit(new Runnable() { public void run() {
            testFontSmoothingType(FontSmoothingType.GRAY);
        }});
    }


    private void testFontScale(double expected) {
        assertEquals(expected, getView().getFontScale(), 0);
    }

    @Test public void testFontScaleDefault() {
        testFontScale(1);
    }

    @Test public void testFontScaleManual() {
        submit(new Runnable() { public void run() {
            getView().setFontScale(2);
            testFontScale(2);
        }});
    }

    @Test public void testFontScaleCSS() {
        setStyle("-fx-font-scale: .2");
        submit(new Runnable() { public void run() {
            testFontScale(0.2);
        }});
    }


    private void testMinWidth(double expected) {
        assertEquals(expected, getView().getMinWidth(), 0);
    }

    @Test public void testMinWidthDefault() {
        testMinWidth(0);
    }

    @Test public void testMinWidthManual() {
        submit(new Runnable() { public void run() {
            getView().setMinWidth(2);
            testMinWidth(2);
        }});
    }

    @Test public void testMinWidthCSS() {
        setStyle("-fx-min-width: 3px");
        submit(new Runnable() { public void run() {
            testMinWidth(3);
        }});
    }


    private void testMinHeight(double expected) {
        assertEquals(expected, getView().getMinHeight(), 0);
    }

    @Test public void testMinHeightDefault() {
        testMinHeight(0);
    }

    @Test public void testMinHeightManual() {
        submit(new Runnable() { public void run() {
            getView().setMinHeight(2);
            testMinHeight(2);
        }});
    }

    @Test public void testMinHeightCSS() {
        setStyle("-fx-min-height: 3px");
        submit(new Runnable() { public void run() {
            testMinHeight(3);
        }});
    }


    private void testPrefWidth(double expected) {
        assertEquals(expected, getView().getPrefWidth(), 0);
    }

    @Test public void testPrefWidthDefault() {
        testPrefWidth(800);
    }

    @Test public void testPrefWidthManual() {
        submit(new Runnable() { public void run() {
            getView().setPrefWidth(2);
            testPrefWidth(2);
        }});
    }

    @Test public void testPrefWidthCSS() {
        setStyle("-fx-pref-width: 3px");
        submit(new Runnable() { public void run() {
            testPrefWidth(3);
        }});
    }


    private void testPrefHeight(double expected) {
        assertEquals(expected, getView().getPrefHeight(), 0);
    }

    @Test public void testPrefHeightDefault() {
        testPrefHeight(600);
    }

    @Test public void testPrefHeightManual() {
        submit(new Runnable() { public void run() {
            getView().setPrefHeight(2);
            testPrefHeight(2);
        }});
    }

    @Test public void testPrefHeightCSS() {
        setStyle("-fx-pref-height: 3px");
        submit(new Runnable() { public void run() {
            testPrefHeight(3);
        }});
    }


    private void testMaxWidth(double expected) {
        assertEquals(expected, getView().getMaxWidth(), 0);
    }

    @Test public void testMaxWidthDefault() {
        testMaxWidth(Double.MAX_VALUE);
    }

    @Test public void testMaxWidthManual() {
        submit(new Runnable() { public void run() {
            getView().setMaxWidth(2);
            testMaxWidth(2);
        }});
    }

    @Test public void testMaxWidthCSS() {
        setStyle("-fx-max-width: 3px");
        submit(new Runnable() { public void run() {
            testMaxWidth(3);
        }});
    }


    private void testMaxHeight(double expected) {
        assertEquals(expected, getView().getMaxHeight(), 0);
    }

    @Test public void testMaxHeightDefault() {
        testMaxHeight(Double.MAX_VALUE);
    }

    @Test public void testMaxHeightManual() {
        submit(new Runnable() { public void run() {
            getView().setMaxHeight(2);
            testMaxHeight(2);
        }});
    }

    @Test public void testMaxHeightCSS() {
        setStyle("-fx-max-height: 3px");
        submit(new Runnable() { public void run() {
            testMaxHeight(3);
        }});
    }
}
