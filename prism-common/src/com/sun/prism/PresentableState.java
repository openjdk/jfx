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

package com.sun.prism;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;

/**
 * PresentableState is intended to provide for a shadow copy of View/Window
 * state for use off the event thread. It is the task of the invoker of 
 * Prism to make sure that the state is consistent for a rendering probably
 * by use of the Abstractpainter.renderLock to ensure consistent state.
 */
public class PresentableState {

    /** The underlying Window and View */
    private Window window;
    protected View view;
    // Captured state
    private int windowX, windowY;
    private float windowAlpha;
    private long nativeWindowHandle;
    private long nativeView;
    private int viewWidth, viewHeight;
    private int screenHeight;
    private boolean isWindowVisible;
    private boolean isWindowMinimized;
    private float screenScale;
    private static boolean hasWindowManager =
            Application.GetApplication().hasWindowManager();
    // Between PaintCollector and *Painter, there is a window where
    // the associated View can be closed. This variable allows us
    // to shortcut the queued *Painter task.
    private boolean isClosed;

    /** Create a PresentableState based on a View.
     *
     * Must be called on the event thread.
     */
    public PresentableState(View view) {
        this.view = view;
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

    /**
     * @return Screen.getScale
     * 
     * May be called on any thread
     */
    public float getScale() {
        return screenScale;
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
     * Updates this object's state to indicate that the underlying View is
     * closed.
     *
     * May be called on any thread.
     */
    public void setViewClosed() {
        this.isClosed = true;
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

    /**
     * @return the underlying View
     *
     * May be called on any thread.
     */
    public View getView() {
        return view;
    }

    /**
     * Locks the underlying view for rendering
     *
     * May be called on any thread.
     */
    public void lock() {
        view.lock();
    }

    /**
     * Unlocks the underlying view after rendering
     *
     * May be called on any thread.
     */
    public void unlock() {
        view.unlock();
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
            Screen screen = window.getScreen();
            if (screen != null) {
                // note only used by Embedded Z order painting 
                // !hasWindowManager so should be safe to ignore
                // when null, most likely because of "In Browswer"
                screenHeight = screen.getHeight();
                screenScale = screen.getScale();
            }
        } else {
            //TODO - should other variables be cleared?
            nativeView = -1;
            nativeWindowHandle = -1;
            isClosed = true;
        }
    }

    public boolean isUpToDate() {
        if (isClosed || view == null) {
            return true; // the dead do not change
        }

        if (viewWidth != view.getWidth()) {
            return false;
        }
        if (viewHeight != view.getHeight()) {
            return false;
        }
        if (!view.getWindow().equals(window)) {
            return false;
        }

        if (window != null) {
            if (!hasWindowManager) {
                if (windowX != window.getX()) {
                    return false;
                }
                if (windowY != window.getY()) {
                    return false;
                }
                if (windowAlpha != window.getAlpha()) {
                    return false;
                }
            }
            if (nativeView != view.getNativeView()) {
                return false;
            }
            if (nativeWindowHandle != window.getNativeWindow()) {
                return false;
            }
            if (isWindowVisible != window.isVisible()) {
                return false;
            }
            if (isWindowMinimized != window.isMinimized()) {
                return false;
            }
            // really needed ? Would think that Glass would send a
            // REPAINT request.....
            if (screenHeight != window.getScreen().getHeight()) {
                return false;
            }
        }

        // we got here, must be current
        return true;
    }
}
