/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.sw;

import com.sun.glass.ui.Pixels;
import com.sun.javafx.geom.Rectangle;
import com.sun.prism.Presentable;
import com.sun.prism.PresentableState;
import com.sun.prism.impl.QueuedPixelSource;
import java.nio.IntBuffer;

final class SWPresentable extends SWRTTexture implements Presentable {

    private final PresentableState pState;
    private Pixels pixels;
    private QueuedPixelSource pixelSource = new QueuedPixelSource(false);

    public SWPresentable(PresentableState pState, SWResourceFactory factory) {
        super(factory, pState.getRenderWidth(), pState.getRenderHeight());
        this.pState = pState;
    }

    @Override
    public boolean lockResources(PresentableState pState) {
        return (getPhysicalWidth() != pState.getRenderWidth() ||
                getPhysicalHeight() != pState.getRenderHeight());
    }

    @Override
    public boolean prepare(Rectangle dirtyregion) {
        if (!pState.isViewClosed()) {
            /*
             * RT-27374
             * TODO: make sure the imgrep matches the Pixels.getNativeFormat()
             * TODO: dirty region support
             */
            int w = getPhysicalWidth();
            int h = getPhysicalHeight();
            pixels = pixelSource.getUnusedPixels(w, h, 1.0f, 1.0f);
            IntBuffer pixBuf = (IntBuffer) pixels.getPixels();
            IntBuffer buf = getSurface().getDataIntBuffer();
            assert buf.hasArray();
            System.arraycopy(buf.array(), 0, pixBuf.array(), 0, w*h);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean present() {
        pixelSource.enqueuePixels(pixels);
        pState.uploadPixels(pixelSource);
        return true;
    }

    @Override
    public float getPixelScaleFactorX() {
        return pState.getRenderScaleX();
    }

    @Override
    public float getPixelScaleFactorY() {
        return pState.getRenderScaleY();
    }

    @Override
    public int getContentWidth() {
        return pState.getOutputWidth();
    }

    @Override
    public int getContentHeight() {
        return pState.getOutputHeight();
    }

    @Override public boolean isMSAA() {
        return super.isMSAA();
    }
}
