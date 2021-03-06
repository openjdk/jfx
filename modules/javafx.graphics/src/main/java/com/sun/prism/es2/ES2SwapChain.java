/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.es2;

import com.sun.glass.ui.Screen;
import com.sun.javafx.geom.Rectangle;
import com.sun.prism.GraphicsResource;
import com.sun.prism.Presentable;
import com.sun.prism.PresentableState;
import com.sun.prism.RTTexture;
import com.sun.prism.CompositeMode;
import com.sun.prism.impl.PrismSettings;
import com.sun.javafx.PlatformUtil;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture.WrapMode;

class ES2SwapChain implements ES2RenderTarget, Presentable, GraphicsResource {

    private final ES2Context context;
    private final PresentableState pState;
    // On screen
    private GLDrawable drawable;
    private boolean needsResize;
    private boolean opaque = false;
    private int w, h;
    private float pixelScaleFactorX;
    private float pixelScaleFactorY;
    // a value of zero corresponds to the windowing system-provided
    // framebuffer object
    int nativeDestHandle = 0;
    private final boolean msaa;
    /**
     * An offscreen surface that acts as a persistent backbuffer, currently
     * only used when dirty region optimizations are enabled in the scenegraph.
     *
     * In OpenGL, the contents of a window's (hardware) backbuffer are
     * undefined after a swapBuffers() operation.  The dirty region
     * optimizations used in the Prism scenegraph require the window's
     * backbuffer to be persistent, so when those optimizations are enabled,
     * we insert this special stableBackbuffer into the swap chain.
     * In createGraphics() we return a Graphics object that points to this
     * stableBackbuffer so that the scenegraph gets rendered into it,
     * and then at present() time we first copy stableBackbuffer into the
     * window's hardware backbuffer prior to calling swapBuffers().
     */
    private RTTexture stableBackbuffer;
    private boolean copyFullBuffer;

    @Override
    public boolean isOpaque() {
        if (stableBackbuffer != null) {
            return stableBackbuffer.isOpaque();
        } else {
            return opaque;
        }
    }

    @Override
    public void setOpaque(boolean isOpaque) {
        if (stableBackbuffer != null) {
            stableBackbuffer.setOpaque(isOpaque);
        } else {
            this.opaque = isOpaque;
        }
    }

    ES2SwapChain(ES2Context context, PresentableState pState) {
        this.context = context;
        this.pState = pState;
        this.pixelScaleFactorX = pState.getRenderScaleX();
        this.pixelScaleFactorY = pState.getRenderScaleY();
        this.msaa = pState.isMSAA();
        long nativeWindow = pState.getNativeWindow();
        drawable = ES2Pipeline.glFactory.createGLDrawable(
                nativeWindow, context.getPixelFormat());
    }

    @Override
    public boolean lockResources(PresentableState pState) {
        if (this.pState != pState ||
            pixelScaleFactorX != pState.getRenderScaleX() ||
            pixelScaleFactorY != pState.getRenderScaleY())
        {
            return true;
        }
        needsResize = (w != pState.getRenderWidth() || h != pState.getRenderHeight());
        // the stableBackbuffer will be used as the render target
        if (stableBackbuffer != null && !needsResize) {
            stableBackbuffer.lock();
            if (stableBackbuffer.isSurfaceLost()) {
                stableBackbuffer = null;
                // For resizes we can keep the back buffer, but if we lose
                // the back buffer then we need the caller to know that a
                // new buffer is coming so that the entire scene can be
                // redrawn.  To force this, we return true and the Presentable
                // is recreated and repainted in its entirety.
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean prepare(Rectangle clip) {
        try {
            ES2Graphics g = ES2Graphics.create(context, this);
            if (stableBackbuffer != null) {
                if (needsResize) {
                    g.forceRenderTarget();
                    needsResize = false;
                }
                // Copy (not blend) the stableBackbuffer into place.
                //TODO: Determine why w/h is needed here
                w = pState.getRenderWidth();
                h = pState.getRenderHeight();
                int sw = w;
                int sh = h;
                int dw = pState.getOutputWidth();
                int dh = pState.getOutputHeight();
                copyFullBuffer = false;
                if (isMSAA()) {
                    context.flushVertexBuffer();
                    // Note must flip the image vertically during blit
                    g.blit(stableBackbuffer, null,
                            0, 0, sw, sh, 0, dh, dw, 0);
                } else {
                    drawTexture(g, stableBackbuffer,
                                0, 0, dw, dh, 0, 0, sw, sh);
                }
                stableBackbuffer.unlock();
            }
            return drawable != null;
        } catch (Throwable th) {
            if (PrismSettings.verbose) {
                th.printStackTrace();
            }
            return false;
        }
    }

    private void drawTexture(ES2Graphics g, RTTexture src,
                             float dx1, float dy1, float dx2, float dy2,
                             float sx1, float sy1, float sx2, float sy2) {

        CompositeMode savedMode = g.getCompositeMode();
        if (!pState.hasWindowManager()) {
            // no window manager - we need to do the blending ourselves
            // pass any window-level alpha setting on to the prism graphics object
            g.setExtraAlpha(pState.getAlpha());
            g.setCompositeMode(CompositeMode.SRC_OVER);
        } else {
            // we have a window manager - copy (not blend) stable backbuffer into place
            g.setCompositeMode(CompositeMode.SRC);
        }
        g.drawTexture(src, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2);
        context.flushVertexBuffer();
        // restore the blend
        g.setCompositeMode(savedMode);
    }

    @Override
    public boolean present() {
        boolean presented = drawable.swapBuffers(context.getGLContext());
        context.makeCurrent(null);
        return presented;
    }

    @Override
    public ES2Graphics createGraphics() {
        if (drawable.getNativeWindow() != pState.getNativeWindow()) {
            drawable = ES2Pipeline.glFactory.createGLDrawable(
                    pState.getNativeWindow(), context.getPixelFormat());
        }
        context.makeCurrent(drawable);

        nativeDestHandle = pState.getNativeFrameBuffer();
        if (nativeDestHandle == 0) {
            GLContext glContext = context.getGLContext();
            nativeDestHandle = glContext.getBoundFBO();
        }

        needsResize = (w != pState.getRenderWidth() || h != pState.getRenderHeight());
        // the stableBackbuffer will be used as the render target
        if (stableBackbuffer == null || needsResize) {
            // note that we will take care of calling
            // forceRenderTarget() for the hardware backbuffer and
            // reset the needsResize flag at present() time...
            if (stableBackbuffer != null) {
                stableBackbuffer.dispose();
                stableBackbuffer = null;
            } else {
                // RT-27554
                // TODO: this implementation was done to make sure there is a
                // context current for the hardware backbuffer before we start
                // attempting to use the FBO associated with the
                // RTTexture "backbuffer"...
                ES2Graphics.create(context, this);
            }
            w = pState.getRenderWidth();
            h = pState.getRenderHeight();
            ResourceFactory factory = context.getResourceFactory();
            stableBackbuffer = factory.createRTTexture(w, h,
                                                       WrapMode.CLAMP_NOT_NEEDED,
                                                       msaa);
            if (PrismSettings.dirtyOptsEnabled) {
                stableBackbuffer.contentsUseful();
            }
            copyFullBuffer = true;
        }
        ES2Graphics g = ES2Graphics.create(context, stableBackbuffer);
        g.scale(pixelScaleFactorX, pixelScaleFactorY);
        return g;
    }

    @Override
    public int getFboID() {
        return nativeDestHandle;
    }

    @Override
    public Screen getAssociatedScreen() {
        return context.getAssociatedScreen();
    }

    @Override
    public int getPhysicalWidth() {
        return pState.getOutputWidth();
    }

    @Override
    public int getPhysicalHeight() {
        return pState.getOutputHeight();
    }

    @Override
    public int getContentX() {
        // EGL doesn't have a window manager, so we need to ask the window for
        // the x/y offset to use
        if (PlatformUtil.useEGL()) {
            return (int) (pState.getWindowX() * pState.getOutputScaleX());
        } else {
            return 0;
        }
    }

    @Override
    public int getContentY() {
        // EGL doesn't have a window manager, so we need to ask the window
        // for the x/y offset to use
        if (PlatformUtil.useEGL()) {
            return ((int) (pState.getScreenHeight() * pState.getOutputScaleY())) -
                    pState.getOutputHeight() - ((int) (pState.getWindowY() * pState.getOutputScaleY()));
        } else {
            return 0;
        }
    }

    @Override
    public int getContentWidth() {
        return pState.getOutputWidth();
    }

    @Override
    public int getContentHeight() {
        return pState.getOutputHeight();
    }

    @Override
    public float getPixelScaleFactorX() {
        return pixelScaleFactorX;
    }

    @Override
    public float getPixelScaleFactorY() {
        return pixelScaleFactorY;
    }

    @Override
    public void dispose() {
        if (stableBackbuffer != null) {
            stableBackbuffer.dispose();
            stableBackbuffer = null;
        }
    }

    @Override
    public boolean isMSAA() {
        return stableBackbuffer != null ? stableBackbuffer.isMSAA() :
                msaa;
    }
}
