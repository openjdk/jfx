/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.sg.prism.NGGroup;
import com.sun.prism.Graphics;

public final class NGWebView extends NGGroup {
    private volatile float width, height;

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
        if (g == null || width <= 0 || height <= 0) {
            return;
        }
        g.getTransformNoClone().transform(transformedBounds, destBounds);
    }

    @Override public boolean hasOverlappingContents() {
        return false;
    }

    @Override protected boolean hasVisuals() {
        return true;
    }
}
