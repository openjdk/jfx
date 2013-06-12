/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.lens;

import java.security.AccessController;
import java.security.PrivilegedAction;
import com.sun.glass.events.WindowEvent;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;

final class LensWindow extends Window {

    private boolean visible;
    private boolean resizeable = false;
    private int windowHandle;

    private static int nextWindowId = 1;
    private static final boolean restrictWindowToScreen;

    static {
        restrictWindowToScreen =
        AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override public Boolean run() {
                return Boolean.getBoolean("glass.restrictWindowToScreen");
            }
        });
    }

    protected LensWindow(Window owner, Screen screen, int styleMask) {
        super(owner, screen, styleMask);
    }

    protected LensWindow(long parent) {
        super(parent);

    }


    /**
     *
     * w/h is the total window width/height including all its
     * decorations (e.g. title bar). cw/ch is the "client", or
     * interior width/height. Negative values for w/h/cw/ch are
     * treated as "not set". For example: setBounds(x, y, xSet,
     * ySet, 800, 600, -1, -1) will make the window 800x600 pixels.
     * The client area available for drawing will be smaller, e.g.
     * 792x580 - it depends on the window decorations on different
     * platforms. setBounds(x, y, xSet, ySet, -1, -1, 800, 600) will
     * make the window client size to be 800x600 pixels. The area
     * for drawing (FX scene size) will be exactly 800x600, but the
     * total window size including decorations will be slightly
     * bigger. For undecorated windows w/h and cw/ch are obviously
     * the same.
     *
     * As this is a void function the native code should trigger an
     * event to notify the system on actual change
     *
     */
    @Override
    protected void _setBounds(long nativeWindowPointer,
                              int x, int y, boolean xSet, boolean ySet,
                              int w, int h, int cw, int ch,
                              float xGravity, float yGravity) {

        //calculated window dimensions
        int width;
        int height;

        //is new window size is the content size or the window size
        //this required for platforms that support decorations.
        //if isContentSize == true - width & height are
        //the window size w/o decorations
        boolean isContentSize = false;

        //if true window position is also need to be update, else x&y are
        //to be ignored
        boolean  needToUpdatePosition = xSet || ySet;

        //if false, only move window
        boolean needResize = false;

        if (!xSet && !ySet && w < 0 && h < 0 && cw < 0 && ch < 0) {
            //nothing to do, return
            return;
        }

        LensLogger.getLogger().fine("_setBounds x=" + x + " y=" + y + " xSet=" +
                                    xSet + " ySet=" + ySet + " w=" + w + " h=" +
                                    h + " cw=" + cw + " ch=" + ch +" xGravity=" +
                                    xGravity + " yGravity=" + yGravity);

        if (needToUpdatePosition) {
            if (!xSet) {
                //no explicit request to change x, get default
                x = getX();
            }

            if (!ySet) {
                //no explicit request to change y, get default
                y = getY();
            }
        }

        if (w > 0) {
            //window width surpass window content width (cw)
            width = w;
            needResize = true;
        } else if (cw > 0) {
            //content width changed
            width = cw;
            isContentSize = true;
            needResize = true;
        } else {
            //no explicit request to change width, get default
            width = getWidth();
        }

        if (h > 0) {
            //window height surpass window content height(ch)
            height = h;
            needResize = true;
        } else if (cw > 0) {
            //content height changed
            height = ch;
            isContentSize = true;
            needResize = true;
        } else {
            //no explicit request to change height, get default
            height = getHeight();
        }

        if (restrictWindowToScreen) {
            int screenWidth = this.getScreen().getWidth();
            int screenHeight = this.getScreen().getHeight();

            LensLogger.getLogger().fine("Restricting window size to screen" +
                                        " Requested " + width + "X" + height +
                                        " Screen size " + screenWidth + "X" +
                                        screenHeight);

            width = (width > screenWidth) ? screenWidth : width;
            height = (height > screenHeight) ? screenHeight : height;
            x = Math.max(x, 0);
            y = Math.max(y, 0);
            x = Math.min(screenWidth - width, x);
            y = Math.min(screenHeight - height, y);

            LensLogger.getLogger().fine("Setting bounds to "+ x + "," + y +
                                        "+" + width + "x" + height);
        }

        setBoundsImpl(nativeWindowPointer, x, y, width, height,
                      needToUpdatePosition, needResize, isContentSize);


    }

    @Override
    native protected long _createWindow(long NativeWindow, long NativeScreen,
                                        int mask);

    @Override
    native protected long _createChildWindow(long parent);

    @Override
    native protected boolean _close(long nativeWindowPointer);

    native private boolean attachViewToWindow(long nativeWindowPointer,
                                              long nativeViewPointer);


    @Override
    protected boolean _setView(long nativeWindowPointer, View view) {
        boolean result = false;
        LensLogger.getLogger().info(
            "set view " + view + ", visible=" + isVisible());
        long nativeViewPtr = (view == null) ? 0L : view.getNativeView();
        result = attachViewToWindow(nativeWindowPointer, nativeViewPtr);
        if (view != null && result) {
            // the system assumes a resize notification to set the View
            // sizes and to get the Scene to layout correctly.
            ((LensView)view)._notifyResize(getWidth(), getHeight());
        }
        return result;
    }

    /**
     * Returns the handle used to create a rendering context in Prism
     */
    @Override
    public long getNativeWindow() {
        return _getNativeWindowImpl(super.getNativeWindow());
    }

    native private long _getNativeWindowImpl(long ptr);

    @Override
    //native protected boolean _setMenubar(long ptr, long menubarPtr);
    protected boolean _setMenubar(long ptr, long menubarPtr) {
        return true;
    }

    @Override
    native protected boolean _minimize(long nativeWindowPointer, boolean minimize);

    @Override
    native protected boolean _maximize(long nativeWindowPointer, boolean maximize,
                                       boolean wasMaximized);

    /**
     * Change the window size and/or position
     * Change in size may be the window content size w/o decorations
     * or the total window size
     *
     * @param nativeWindowPointer as was created by createWindow()
     * @param x the position of the window X
     * @param y the position of the window Y
     * @param width of window/content
     * @param height of window/content
     * @param needToUpdatePostion is x&y are valid, if not resize only
     * @param needToUpdateSize is width&height are valid, if not only
     *                     move
     * @param isContentSize does width&height refer to the content
     *                      size or the whole window
     */
    native private void setBoundsImpl(long nativeWindowPointer,
                                      int x, int y, int width, int height,
                                      boolean needToUpdatePostion,
                                      boolean needToUpdateSize,
                                      boolean isContentSize);

    @Override
    native protected boolean _setVisible(long ptr, boolean visible);

    @Override
    native protected boolean _setResizable(long ptr, boolean resizable);

    @Override
    native protected boolean _requestFocus(long ptr, int event);

    @Override
    native protected void _setFocusable(long ptr, boolean isFocusable);

    @Override
    native protected boolean _setTitle(long ptr, String title);

    @Override
    native protected void _setLevel(long ptr, int level);

    @Override
    native protected void _setAlpha(long ptr, float alpha);

    @Override
    native protected boolean _setBackground(long ptr, float r, float g, float b);

    @Override
    native protected void _setEnabled(long ptr, boolean enabled);

    @Override
    native protected boolean _setMinimumSize(long ptr, int width, int height);

    @Override
    native protected boolean _setMaximumSize(long ptr, int width, int height);

    @Override
    native protected void _setIcon(long ptr, Pixels pixels);

    @Override
    native protected void _toFront(long ptr);

    @Override
    native protected void _toBack(long ptr);

    @Override
    native protected boolean _grabFocus(long ptr);

    @Override
    native protected void _ungrabFocus(long ptr);

    /**
     * The functions below are used when the platform support modality natively.
     * Currently only GTK is using it. This functionality is disabled by
     * default. In order to enable it this class need to override Window::
     * supportsPlatformModality() to return true.
     *
     */
    @Override
    protected void _enterModal(long ptr) {
        //should not get here
        LensLogger.getLogger().severe("Platform modality not supported");
    }

    @Override
    protected void _enterModalWithWindow(long dialog, long window) {
        //should not get here
        LensLogger.getLogger().severe("Platform modality not supported");
    }

    @Override
    protected void _exitModal(long ptr) {
        //should not get here
        LensLogger.getLogger().severe("Platform modality not supported");
    }

    //**************************************************************
    // wrappers so Application run loop can get where it needs to go
    protected void _notifyClose() {
        //This event is called by LensWindowManager when a window needs to be
        //closed, so this is a synthetic way to emulate platform window manager
        //window close event
        notifyClose();
        close();
    }

    protected void _notifyDestroy() {
        notifyDestroy();
    }

    protected void _notifyFocus(int event) {
        notifyFocus(event);
    }

    protected void _notifyMove(final int x, final int y) {
        notifyMove(x, y);
        // Note! we don't notify the view of a move
        // as the view is only supposed to have decoration
        // offsets for X and Y. If we have those, call
        // view._notifyMove(x,y) directly with the offsets
    }

    protected void _notifyResize(final int type, final int width,
                                 final int height) {
        notifyResize(type, width, height);
        LensView view = (LensView) getView();
        if (view != null) {
            view._notifyResize(width, height);
        }
    }

    protected void _notifyExpose(final int x, final int y, final int width,
                                 final int height) {
        LensView view = (LensView) getView();
        if (view != null) {
            view._notifyRepaint(x, y, width, height);
        }
    }

    protected void _notifyFocusUngrab() {
        notifyFocusUngrab();
    }

    //**************************************************************
    // upcalls from native, need to forward to event loop

    //it seems the functions in this section
    //are not been used
    protected void nativeExpose(int x, int y, int width, int height) {
        if (Application.isEventThread()) {
            _notifyExpose(x, y, width, height);
        } else {
            ((LensApplication)(Application.GetApplication())).windowExpose(this, x,
                                                                           y, width,
                                                                           height);
        }
    }

    protected void nativeConfigure(int type, int x, int y, int width, int height) {
        LensApplication app = (LensApplication) Application.GetApplication();
        if (Application.isEventThread()) {
            if (x != getX() || y != getY()) {
                _notifyMove(x, y);
            }
            if (width != getWidth() || height != getHeight()) {
                _notifyResize(type, width, height);
            }
        } else {
            if (x != getX() || y != getY()) {
                app.notifyWindowMove(this, x, y);
            }
            if (width != getWidth() || height != getHeight()) {
                app.notifyWindowResize(this, type, width, height);
            }
        }
    }

    protected void nativeClose() {
        LensApplication lensApp = ((LensApplication)(Application.GetApplication()));
        lensApp.notifyWindowEvent(this, WindowEvent.CLOSE);
    }
    //**************************************************************

    @Override protected void _setCursor(long ptr, Cursor cursor) {
        ((LensCursor)cursor).set();
    }

    @Override protected int _getEmbeddedX(long ptr) {
        return 0;
    }
    @Override protected int _getEmbeddedY(long ptr) {
        return 0;
    }

    @Override
    protected void _requestInput(long ptr, String text, int type, double width, double height,
                                 double Mxx, double Mxy, double Mxz, double Mxt,
                                 double Myx, double Myy, double Myz, double Myt,
                                 double Mzx, double Mzy, double Mzz, double Mzt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void _releaseInput(long ptr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
