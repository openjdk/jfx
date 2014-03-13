/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.concurrent.FutureTask;

import javafx.application.Platform;

import org.junit.Test;

public class WebViewTest extends TestBase {
    final static float SCALE = 1.78f;
    final static float ZOOM = 2.71f;
    final static float DELTA = 1e-3f;

    @Test public void testTextScale() throws Exception {
        WebView view = getView();
        setFontScale(view, SCALE);
        checkFontScale(view, SCALE);
        setZoom(view, ZOOM);
        checkZoom(view, ZOOM);

        load(new File("src/test/resources/html/ipsum.html"));

        checkFontScale(view, SCALE);
        checkZoom(view, ZOOM);
    }
    
    void checkFontScale(WebView view, float scale) {
        assertEquals("WebView.fontScale", scale, view.getFontScale(), DELTA);
        assertEquals("WebPage.zoomFactor",
                scale, view.getEngine().getPage().getZoomFactor(true), DELTA);
    }
    
    private void setFontScale(final WebView view, final float scale) throws Exception {
        submit(() -> {
            view.setFontScale(scale);
        });
    }

    void checkZoom(WebView view, float zoom) {
        assertEquals("WebView.zoom", zoom, view.getZoom(), DELTA);
    }

    private void setZoom(final WebView view, final float zoom) throws Exception {
        submit(() -> {
            view.setZoom(zoom);
        });
    }
}
