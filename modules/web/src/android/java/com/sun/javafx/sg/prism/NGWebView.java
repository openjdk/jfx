/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.sg.prism;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import java.util.logging.Logger;
import com.sun.prism.Graphics;
import com.sun.prism.paint.Color;
import com.sun.webkit.WebPage;

public final class NGWebView extends NGGroup {

    private final static Logger log =
        Logger.getLogger(NGWebView.class.getName());
    private volatile WebPage page;
    private volatile float width, height;
    private static final Color VERY_LIGHT_RED = new Color(1, 0, 0, .3f);

    public void setPage(WebPage page) {
        this.page = page;
    }

    public void resize(float w, float h) {
        if (width != w || height != h) {
            width = w;
            height = h;
            geometryChanged();
        }
    }

    // Invoked on JavaFX User Thread.
    public void update() {
    }

    public void requestRender() {
        visualsChanged();
    }
    private final float[] src = new float[]{0.0f, 0.0f};
    private final float[] dest = new float[]{0.0f, 0.0f};
    private final RectBounds destBounds = new RectBounds();

    @Override
    protected void doRender(Graphics g) {
        renderContent(g);
    }

    @Override
    public void setTransformedBounds(BaseBounds bounds, boolean byTransformChangeOnly) {
        super.setTransformedBounds(bounds, byTransformChangeOnly);
    }

    // Invoked on Render Thread.
    @Override
    protected void renderContent(Graphics g) {
//        g.setPaint(VERY_LIGHT_RED);
//        g.fillRect(transformedBounds.getMinX(),
//                transformedBounds.getMinY(),
//                transformedBounds.getWidth(),
//                transformedBounds.getHeight());

        g.getTransformNoClone().
                transform(transformedBounds, destBounds);
        page.moveAndResize(
                destBounds.getMinX(),
                destBounds.getMinY(),
                destBounds.getWidth(),
                destBounds.getHeight());
    }

    @Override public boolean hasOverlappingContents() {
        return false;
    }
    
    @Override protected boolean hasVisuals() {
        return true;
    }
}
