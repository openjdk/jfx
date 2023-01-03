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

package com.sun.javafx.webkit.prism;

import com.sun.javafx.logging.PlatformLogger;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.RTTexture;
import com.sun.prism.ResourceFactory;
import com.sun.prism.ResourceFactoryListener;
import com.sun.prism.Texture;
import com.sun.webkit.graphics.WCCamera;
import com.sun.webkit.graphics.WCGraphicsContext;
import com.sun.webkit.graphics.WCGraphicsManager;
import com.sun.webkit.graphics.WCPageBackBuffer;
import java.lang.ref.WeakReference;

final class WCPageBackBufferImpl extends WCPageBackBuffer implements ResourceFactoryListener {
    private RTTexture texture;
    private WeakReference<ResourceFactory> registeredWithFactory = null;
    private boolean firstValidate = true;
    private float pixelScale;

    private final static PlatformLogger log =
            PlatformLogger.getLogger(WCPageBackBufferImpl.class.getName());

    WCPageBackBufferImpl(float pixelScale) {
        this.pixelScale = pixelScale;
    }

    private static RTTexture createTexture(int w, int h) {
        return GraphicsPipeline.getDefaultResourceFactory()
                .createRTTexture(w, h, Texture.WrapMode.CLAMP_NOT_NEEDED);
    }

    @Override
    public WCGraphicsContext createGraphics() {
        Graphics g = texture.createGraphics();
        // Make use of custom camera created for WebKit.
        g.setCamera(WCCamera.INSTANCE);
        g.scale(pixelScale, pixelScale);
        return WCGraphicsManager.getGraphicsManager().createGraphicsContext(g);
    }

    @Override
    public void disposeGraphics(WCGraphicsContext gc) {
        gc.dispose();
    }

    @Override
    public void flush(final WCGraphicsContext gc, int x, int y, final int w, final int h) {
        int x2 = x + w;
        int y2 = y + h;
        ((Graphics) gc.getPlatformGraphics()).drawTexture(texture, x, y, x2, y2,
                x * pixelScale, y * pixelScale, x2 * pixelScale, y2 * pixelScale);
        texture.unlock();
    }

    @Override
    protected void copyArea(int x, int y, int w, int h, int dx, int dy) {
        x *= pixelScale;
        y *= pixelScale;
        w = (int) Math.ceil(w * pixelScale);
        h = (int) Math.ceil(h * pixelScale);
        dx *= pixelScale;
        dy *= pixelScale;
        RTTexture aux = createTexture(w, h);
        aux.createGraphics().drawTexture(texture, 0, 0, w, h, x, y, x + w, y + h);
        texture.createGraphics().drawTexture(aux, x + dx, y + dy, x + w + dx, y + h + dy,
                                             0, 0, w, h);
        aux.dispose();
    }

    @Override
    public boolean validate(int width, int height) {
        ResourceFactory factory = GraphicsPipeline.getDefaultResourceFactory();
        if (factory == null || factory.isDisposed()) {
            log.fine("WCPageBackBufferImpl::validate : device disposed or not ready");

            return false;
        }

        width = (int) Math.ceil(width * pixelScale);
        height = (int) Math.ceil(height * pixelScale);
        if (texture != null) {
            texture.lock();
            if (texture.isSurfaceLost()) {
                texture.dispose();
                texture = null;
            }
        }
        if (texture == null) {
            texture = createTexture(width, height);
            texture.contentsUseful();
            if (registeredWithFactory == null || registeredWithFactory.get() != factory) {
                factory.addFactoryListener(this);
                registeredWithFactory = new WeakReference<>(factory);
            }
            if (firstValidate) {
                // this is the very first time validate() is called. We assume
                // full repaint is already happening, so we don't return false
                firstValidate = false;
            } else {
                // texture must have been nullified in factoryReset() or factoryReleased().
                // Backbuffer is lost, so we request full repaint.
                texture.unlock();
                return false;
            }
        } else {
            int tw = texture.getContentWidth();
            int th = texture.getContentHeight();
            if (tw != width || th != height) {
                // Change the texture size
                RTTexture newTexture = createTexture(width, height);
                newTexture.contentsUseful();
                newTexture.createGraphics().drawTexture(texture, 0, 0,
                        Math.min(width, tw), Math.min(height, th));
                texture.dispose();
                texture = newTexture;
            }
        }
        return true;
    }

    @Override public void factoryReset() {
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
    }

    @Override public void factoryReleased() {
        log.fine("WCPageBackBufferImpl: resource factory released");

        if (texture != null) {
            texture.dispose();
            texture = null;
        }
    }
}
