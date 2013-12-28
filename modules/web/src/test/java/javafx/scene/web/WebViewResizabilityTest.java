/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class WebViewResizabilityTest extends TestBase {
    
    public @Test void testWebViewIsResizable() {
        assertTrue(getView().isResizable());
    }

    public @Test void testWebViewContentBiasIsNull() {
        assertNull(getView().getContentBias());
    }

    public @Test void testWebViewMinPrefMaxPropertyDefaults() {
        WebView webview = getView();

        assertEquals(0, webview.getMinWidth(), 0);
        assertEquals(0, webview.getMinHeight(), 0);
        assertEquals(800, webview.getPrefWidth(), 0);
        assertEquals(600, webview.getPrefHeight(), 0);
        assertEquals(Double.MAX_VALUE, webview.getMaxWidth(), 0);
        assertEquals(Double.MAX_VALUE, webview.getMaxHeight(), 0);
    }

    public @Test void testWebViewMinPrefMaxSizeDefaults() {
        WebView webview = getView();

        assertEquals(0, webview.minWidth(-1),0);
        assertEquals(0, webview.minHeight(-1),0);
        assertEquals(800, webview.prefWidth(-1),0);
        assertEquals(600, webview.prefHeight(-1),0);
        assertEquals(Double.MAX_VALUE, webview.maxWidth(-1),0);
        assertEquals(Double.MAX_VALUE, webview.maxHeight(-1),0);
    }

    public @Test void testWebViewSetMinWidth() {
        WebView webview = getView();
        webview.setMinWidth(10);

        assertEquals(10, webview.getMinWidth(),0);
        assertEquals(10, webview.minWidth(-1),0);
    }

    public @Test void testWebViewSetMinHeight() {
        WebView webview = getView();
        webview.setMinHeight(10);

        assertEquals(10, webview.getMinHeight(),0);
        assertEquals(10, webview.minHeight(-1),0);
    }

    public @Test void testWebViewSetPrefWidth() {
        WebView webview = getView();
        webview.setPrefWidth(100);

        assertEquals(100, webview.getPrefWidth(),0);
        assertEquals(100, webview.prefWidth(-1),0);
    }

    public @Test void testWebViewSetPrefHeight() {
        WebView webview = getView();
        webview.setPrefHeight(100);

        assertEquals(100, webview.getPrefHeight(),0);
        assertEquals(100, webview.prefHeight(-1),0);
    }

    public @Test void testWebViewSetMaxWidth() {
        WebView webview = getView();
        webview.setMaxWidth(100);

        assertEquals(100, webview.getMaxWidth(),0);
        assertEquals(100, webview.maxWidth(-1),0);
    }

    public @Test void testWebViewSetMaxHeight() {
        WebView webview = getView();
        webview.setMaxHeight(100);

        assertEquals(100, webview.getMaxHeight(),0);
        assertEquals(100, webview.maxHeight(-1),0);
    }
}
