/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
