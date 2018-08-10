/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.sg.prism.web;

import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.sg.prism.NGGroup;
import com.sun.prism.Graphics;
import com.sun.prism.PrinterGraphics;
import com.sun.webkit.WebPage;
import com.sun.webkit.graphics.WCGraphicsContext;
import com.sun.webkit.graphics.WCGraphicsManager;
import com.sun.webkit.graphics.WCRectangle;

/**
 * A scene graph node that renders a web resource
 *
 * @author Alexey Ushakov
 */
public final class NGWebView extends NGGroup {

    private final static PlatformLogger log =
            PlatformLogger.getLogger(NGWebView.class.getName());
    private volatile WebPage page;
    private volatile float width, height;

    public void setPage(WebPage page) {
        this.page = page;
    }

    public void resize(float w, float h) {
        if (width != w || height != h) {
            width = w;
            height = h;
            geometryChanged();
            if (page != null) {
                page.setBounds(0, 0, (int)w, (int)h);
            }
        }
    }

    // Invoked on JavaFX User Thread.
    public void update() {
        if (page != null) {
            BaseBounds clip = getClippedBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM);
            if (!clip.isEmpty()) {
                log.finest("updating rectangle: {0}", clip);
                page.updateContent(new WCRectangle(clip.getMinX(), clip.getMinY(),
                                                   clip.getWidth(), clip.getHeight()));
            }
        }
    }

    public void requestRender() {
        visualsChanged();
    }

    // Invoked on Render Thread.
    @Override protected void renderContent(Graphics g) {
        log.finest("rendering into {0}", g);
        if (g == null || page == null || width <= 0 || height <= 0)
            return;

        WCGraphicsContext gc =
                WCGraphicsManager.getGraphicsManager().createGraphicsContext(g);
        try {
            if (g instanceof PrinterGraphics) {
                page.print(gc, 0, 0, (int) width, (int) height);
            } else {
                page.paint(gc, 0, 0, (int) width, (int) height);
            }
            gc.flush();
        } finally {
            gc.dispose();
        }

    }

    @Override public boolean hasOverlappingContents() {
        return false;
    }

    @Override protected boolean hasVisuals() {
        return true;
    }
}
