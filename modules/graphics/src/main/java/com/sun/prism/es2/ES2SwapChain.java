/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
    private float pixelScaleFactor;
    // a value of zero corresponds to the windowing system-provided
    // framebuffer object
    int nativeDestHandle = 0;
    private final boolean antiAliasing;
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

    public boolean isOpaque() {
        if (stableBackbuffer != null) {
            return stableBackbuffer.isOpaque();
        } else {
            return opaque;
        }
    }

    public void setOpaque(boolean isOpaque) {
        if (stableBackbuffer != null) {
            stableBackbuffer.setOpaque(isOpaque);
        } else {
            this.opaque = isOpaque;
        }
    }

    static float getScale(PresentableState pState) {
        return PrismSettings.allowHiDPIScaling
               ? pState.getScale() //TODO fix getScale
               : 1.0f;
    }

    ES2SwapChain(ES2Context context, PresentableState pState) {
        this.context = context;
        this.pState = pState;
        this.pixelScaleFactor = getScale(pState);
        this.antiAliasing = pState.isAntiAliasing();
        long nativeWindow = pState.getNativeWindow();
        drawable = ES2Pipeline.glFactory.createGLDrawable(
                nativeWindow, context.getPixelFormat());
    }

    public boolean lockResources(PresentableState pState) {
        if (this.pState != pState || pixelScaleFactor != getScale(pState)) {
            return true;
        }
        needsResize = (w != getPhysicalWidth() || h != getPhysicalHeight());
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
                w = getPhysicalWidth();
                h = getPhysicalHeight();
                Rectangle rectDST = new Rectangle(0, 0, w, h);
                if (clip != null && !copyFullBuffer) {
                    rectDST.intersectWith(clip);
                }
                copyFullBuffer = false;
                int x0 = rectDST.x;
                int y0 = rectDST.y;
                int x1 = x0 + rectDST.width;
                int y1 = y0 + rectDST.height;
                if (isAntiAliasing()) {
                    context.flushVertexBuffer();
                    // Note must flip the z axis during blit
                    g.blit(stableBackbuffer, null, x0, y0, x1, y1,
                            x0, y1, x1, y0);
                } else {
                    drawTexture(g, stableBackbuffer, x0, y0, x1, y1,
                            x0, y0, x1, y1);
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

    public boolean present() {
        boolean presented = drawable.swapBuffers(context.getGLContext());
        context.makeCurrent(null);
        return presented;
    }

    public ES2Graphics createGraphics() {
        context.makeCurrent(drawable);

        GLContext glContext = context.getGLContext();
        nativeDestHandle = glContext.getBoundFBO();

        needsResize = (w != getPhysicalWidth() || h != getPhysicalHeight());
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
            w = getPhysicalWidth();
            h = getPhysicalHeight();
            ResourceFactory factory = context.getResourceFactory();
            stableBackbuffer = factory.createRTTexture(w, h,
                                                       WrapMode.CLAMP_NOT_NEEDED,
                                                       antiAliasing);
            if (PrismSettings.dirtyOptsEnabled) {
                stableBackbuffer.contentsUseful();
            }
            copyFullBuffer = true;
        }
        ES2Graphics g = ES2Graphics.create(context, stableBackbuffer);
        g.scale(pixelScaleFactor, pixelScaleFactor);
        return g;
    }

    public int getFboID() {
        return nativeDestHandle;
    }

    public Screen getAssociatedScreen() {
        return context.getAssociatedScreen();
    }

    public int getPhysicalWidth() {
        return (int) (pState.getWidth() * pixelScaleFactor);
    }

    public int getPhysicalHeight() {
        return (int) (pState.getHeight() * pixelScaleFactor);
    }

    public int getContentX() {
        // EGL doesn't have a window manager, so we need to ask the window for
        // the x/y offset to use
        if (PlatformUtil.useEGL()) {
            return pState.getWindowX();
        } else {
            return 0;
        }
    }

    public int getContentY() {
        // EGL doesn't have a window manager, so we need to ask the window
        // for the x/y offset to use
        if (PlatformUtil.useEGL()) {
            return pState.getScreenHeight() -
                   pState.getHeight() - pState.getWindowY();
        } else {
            return 0;
        }
    }

    public int getContentWidth() {
        return (int) (pState.getWidth() * pixelScaleFactor);
    }

    public int getContentHeight() {
        return (int) (pState.getHeight() * pixelScaleFactor);
    }

    public float getPixelScaleFactor() {
        return pixelScaleFactor;
    }

    @Override
    public void dispose() {
        if (stableBackbuffer != null) {
            stableBackbuffer.dispose();
            stableBackbuffer = null;
        }
    }

    public boolean isAntiAliasing() {
        return stableBackbuffer != null ? stableBackbuffer.isAntiAliasing() :
                antiAliasing;
    }
}
