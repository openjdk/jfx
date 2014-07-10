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
