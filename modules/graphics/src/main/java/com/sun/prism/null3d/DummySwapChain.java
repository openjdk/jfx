/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.null3d;

import com.sun.glass.ui.Screen;
import com.sun.javafx.geom.Rectangle;
import com.sun.prism.Graphics;
import com.sun.prism.Presentable;
import com.sun.prism.PresentableState;

class DummySwapChain extends DummyResource implements Presentable {

    private final PresentableState pState;
    private final DummyRTTexture texBackBuffer;
    private int w,h;
    private boolean opaque;

    DummySwapChain(DummyContext context, PresentableState pState, DummyRTTexture rtt) {
        super(context);
        this.pState = pState;
        this.w = pState.getWidth();
        this.h = pState.getHeight();
        texBackBuffer = rtt;
    }

    @Override
    public void dispose() {
        texBackBuffer.dispose();
        super.dispose();
    }

    public boolean lockResources(PresentableState pState) {
        texBackBuffer.lock();
        return false;
    }

    public boolean prepare(Rectangle clip) {
        texBackBuffer.unlock();
        return true;
    }

    public boolean present() {
        return true;
    }

    public int getPhysicalWidth() {
        return w;
    }

    public int getPhysicalHeight() {
        return h;
    }

    public int getContentWidth() {
        return getPhysicalWidth();
    }

    public int getContentHeight() {
        return getPhysicalHeight();
    }

    public int getContentX() {
        return 0;
    }

    public int getContentY() {
        return 0;
    }

    @Override
    public float getPixelScaleFactorX() {
        return 1.0f;
    }

    @Override
    public float getPixelScaleFactorY() {
        return 1.0f;
    }

    public Graphics createGraphics() {
        return DummyGraphics.create(texBackBuffer, context);
    }

    public Screen getAssociatedScreen() {
        return context.getAssociatedScreen();
    }

    public boolean isOpaque() {
        return opaque;
    }

    public void setOpaque(boolean opaque) {
        this.opaque = opaque;
    }

    @Override
    public boolean isMSAA() {
        return false;
    }
}
