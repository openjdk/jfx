/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;

/**
 * PresentableState is intended to provide for a shadow copy of View/Window
 * state for use off the event thread. It is the task of the invoker of
 * Prism to make sure that the state is consistent for a rendering probably
 * by use of the AbstractPainter.renderLock to ensure consistent state.
 */
public abstract class PresentableState {

    /** The underlying Window and View */
    protected Window window;
    protected View view;

    // Captured state
    protected int nativeFrameBuffer;
    protected int windowX, windowY;
    protected float windowAlpha;
    protected long nativeWindowHandle;
    protected long nativeView;
    protected int viewWidth, viewHeight;
    protected float renderScaleX, renderScaleY;
    protected int renderWidth, renderHeight;
    protected float outputScaleX, outputScaleY;
    protected int outputWidth, outputHeight;
    protected int screenHeight;
    protected int screenWidth;
    protected boolean isWindowVisible;
    protected boolean isWindowMinimized;
    protected static final boolean hasWindowManager =
            Application.GetApplication().hasWindowManager();
    // Between PaintCollector and *Painter, there is a window where
    // the associated View can be closed. This variable allows us
    // to shortcut the queued *Painter task.
    protected boolean isClosed;
    protected final int pixelFormat = Pixels.getNativeFormat();

    /** Create a PresentableState based on a View.
     *
     * Must be called on the event thread.
     */
    public PresentableState() {
    }

    /**
     * The screen relative window X
     * @return The screen relative window X
     *
     * May be called on any thread.
     */
    public int getWindowX() {
        return windowX;
    }

    /**
     * The screen relative window Y
     * @return The screen relative window Y
     *
     * May be called on any thread.
     */
    public int getWindowY() {
        return windowY;
    }

    /**
     * @return the width of the View
     *
     * May be called on any thread.
     */
    public int getWidth() {
        return viewWidth;
    }

    /**
     * @return the height of the View
     *
     * May be called on any thread.
     */
    public int getHeight() {
        return viewHeight;
    }

    public int getRenderWidth() {
        return renderWidth;
    }

    public int getRenderHeight() {
        return renderHeight;
    }

    public int getOutputWidth() {
        return outputWidth;
    }

    public int getOutputHeight() {
        return outputHeight;
    }

    /**
     * @return Screen.getScale
     *
     * May be called on any thread
     */
    public float getRenderScaleX() {
        return renderScaleX;
    }

    /**
     * @return Screen.getScale
     *
     * May be called on any thread
     */
    public float getRenderScaleY() {
        return renderScaleY;
    }

    public float getOutputScaleX() {
        return outputScaleX;
    }

    public float getOutputScaleY() {
        return outputScaleY;
    }

    /**
     * @return the window's alpha level
     *
     * May be called on any thread.
     */
    public float getAlpha() {
        return windowAlpha;
    }

    /**
     * @return the native handle of the window represented by this
     * PresentableState
     *
     * May be called on any thread.
     */
    public long getNativeWindow() {
        return nativeWindowHandle;
    }

    /**
     * @return the native handle of the View represented by this
     * PresentableState
     *
     * May be called on any thread.
     */
    public long getNativeView() {
        return nativeView;
    }

    /**
     * @return the current height of the screen
     *
     * May be called on any thread.
     */
    public int getScreenHeight() {
        return screenHeight;
    }

    /**
     * @return the current width of the screen
     *
     * May be called on any thread.
     */
    public int getScreenWidth() {
        return screenWidth;
    }

    /**
     * @return true if the underlying View is closed, false otherwise
     *
     * May be called on any thread.
     */
    public boolean isViewClosed() {
        return isClosed;
    }

    /**
     * @return true if the underlying Window is minimized, false otherwise
     *
     * May be called on any thread.
     */
    public boolean isWindowMinimized() {
        return isWindowMinimized;
    }

    /**
     * @return true if the underlying Window is Visible, false otherwise
     *
     * May be called on any thread.
     */
    public boolean isWindowVisible() {
        return isWindowVisible;
    }

    /**
     * @return true if the underlying window is managed by a window manager
     * external to JavaFX
     *
     * May be called on any thread.
     */
    public boolean hasWindowManager() {
        return hasWindowManager;
    }

    /**
     * @return the underlying Window
     *
     * May be called on any thread.
     */
    public Window getWindow() {
        return window;
    }

    public boolean isMSAA() { return false; }

    /**
     * @return the underlying View
     *
     * May be called on any thread.
     */
    public View getView() {
        return view;
    }

    /**
     * @return native pixel format
     *
     * May be called on any thread.
     */
    public int getPixelFormat() {
        return pixelFormat;
    }

    /**
     * @return native native frame buffer
     *
     * May be called on any thread.
     */
    public int getNativeFrameBuffer() {
        return nativeFrameBuffer;
    }

    /**
     * Locks the underlying view for rendering
     *
     * Must be called on Prism renderer thread.
     */
    public void lock() {
        if (view != null) {
            view.lock();
            nativeFrameBuffer = view.getNativeFrameBuffer();
        }
    }

    /**
     * Unlocks the underlying view after rendering
     *
     * Must be called on Prism renderer thread.
     */
    public void unlock() {
        if (view != null) view.unlock();
    }

    /**
     * Put the pixels on the screen.
     *
     * @param source - the source for the Pixels object to be uploaded
     */
    public void uploadPixels(PixelSource source) {
        Pixels pixels = source.getLatestPixels();
        if (pixels != null) {
            try {
                view.uploadPixels(pixels);
            } finally {
                source.doneWithPixels(pixels);
            }
        }
    }

    private int scale(int dim, float fromScale, float toScale) {
        return (fromScale == toScale)
               ? dim
               : (int) Math.ceil(dim * toScale / fromScale);
    }

    protected void update(float viewScaleX,   float viewScaleY,
                          float renderScaleX, float renderScaleY,
                          float outputScaleX, float outputScaleY)
    {
        this.renderScaleX = renderScaleX;
        this.renderScaleY = renderScaleY;
        this.outputScaleX = outputScaleX;
        this.outputScaleY = outputScaleY;
        if (renderScaleX == viewScaleX && renderScaleY == viewScaleY) {
            renderWidth = viewWidth;
            renderHeight = viewHeight;
        } else {
            renderWidth = scale(viewWidth, viewScaleX, renderScaleX);
            renderHeight = scale(viewHeight, viewScaleY, renderScaleY);
        }
        if (outputScaleX == viewScaleX && outputScaleY == viewScaleY) {
            outputWidth = viewWidth;
            outputHeight = viewHeight;
        } else if (outputScaleX == renderScaleX && outputScaleY == renderScaleY) {
            outputWidth = renderWidth;
            outputHeight = renderHeight;
        } else {
            outputWidth = scale(viewWidth, viewScaleX, outputScaleX);
            outputHeight = scale(viewHeight, viewScaleY, outputScaleY);
        }
    }

    /** Updates the state of this object based on the current state of its
     * nativeWindow.
     *
     * May only be called from the event thread.
     */
    public void update() {
        // should only be called on the event thread
        if (view != null) {
            viewWidth = view.getWidth();
            viewHeight = view.getHeight();
            window = view.getWindow();
        } else {
            viewWidth = viewHeight = -1;
            window = null;
        }
        if (window != null) {
            windowX = window.getX();
            windowY = window.getY();
            windowAlpha = window.getAlpha();
            nativeView = view.getNativeView();
            nativeWindowHandle = window.getNativeWindow();
            isClosed = view.isClosed();
            isWindowVisible = window.isVisible();
            isWindowMinimized = window.isMinimized();
            update(window.getPlatformScaleX(), window.getPlatformScaleY(),
                   window.getRenderScaleX(),   window.getRenderScaleY(),
                   window.getOutputScaleX(),   window.getOutputScaleY());
            Screen screen = window.getScreen();
            if (screen != null) {
                // note only used by Embedded Z order painting
                // !hasWindowManager so should be safe to ignore
                // when null, most likely because of "In Browswer"
                screenHeight = screen.getHeight();
                screenWidth = screen.getWidth();
            }
        } else {
            //TODO - should other variables be cleared?
            nativeView = -1;
            nativeWindowHandle = -1;
            isClosed = true;
        }
    }
}
