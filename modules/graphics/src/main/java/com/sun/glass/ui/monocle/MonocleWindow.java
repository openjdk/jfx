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

package com.sun.glass.ui.monocle;

import com.sun.glass.events.WindowEvent;
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;

public final class MonocleWindow extends Window {

    private int id;

    protected MonocleWindow(Window owner, Screen screen, int styleMask) {
        super(owner, screen, styleMask);
    }

    protected MonocleWindow(long parent) {
        super(parent);
    }

    @Override
    protected void _toFront(long ptr) {
        MonocleWindowManager.getInstance().toFront(this);
    }

    @Override
    protected void _toBack(long ptr) {
        MonocleWindowManager.getInstance().toBack(this);

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

        //if false, only move window
        boolean needResize = false;

        if (w < 0 && h < 0 && cw < 0 && ch < 0) {
            //nothing to do, return
            return;
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


        //perform actions
        boolean windowHasBeenUpdated = false;
        needResize |= isContentSize;

        //handle resize if needed
        if (needResize &&
            (getWidth() != width || getHeight() != height)) {

            notifyResize(WindowEvent.RESIZE, width, height);

            windowHasBeenUpdated = true;

        }

        //handle move if needed
        if (getX() != x || getY() != y) {
            notifyMove(x, y);       

            windowHasBeenUpdated = true;

            //TODO: do we need repaints?
            //lens_wm_repaint(env, window);
        }

    }

    //creates the native window
    @Override
    protected long _createWindow(long NativeWindow, long NativeScreen,
                                 int mask) {
        id = MonocleWindowManager.getInstance().addWindow(this);
        return id;
    }

    @Override
    protected long _createChildWindow(long parent) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean _close(long nativeWindowPointer) {
        return MonocleWindowManager.getInstance().closeWindow(this);
    }

    @Override
    protected boolean _setView(long nativeWindowPointer, View view) {
        boolean result = true;
        if (view != null) {
            // the system assumes a resize notification to set the View
            // sizes and to get the Scene to layout correctly.
            ((MonocleView)view)._notifyResize(getWidth(), getHeight());
        }
        return result;
    }

    /**
     * Returns the handle used to create a rendering context in Prism
     */
    @Override
    public long getNativeWindow() {
        return NativePlatformFactory.getNativePlatform().getScreen().getNativeHandle();
    }

    @Override
    protected boolean _setMenubar(long ptr, long menubarPtr) {
        return true;
    }

    @Override
    protected boolean _minimize(long nativeWindowPointer, boolean minimize) {
        return MonocleWindowManager.getInstance().minimizeWindow(this);
    }

    @Override
    protected boolean _maximize(long nativeWindowPointer, boolean maximize,
                                boolean wasMaximized) {
        return MonocleWindowManager.getInstance().maximizeWindow(this);
    }

    private float cachedAlpha = 1;
    @Override
    protected boolean _setVisible(long ptr, boolean visible) {
        if (visible) {
            setAlpha(cachedAlpha);
        } else {
            cachedAlpha = getAlpha();
            setAlpha(0);
        }

        return true;
    }

    @Override
    protected boolean _setResizable(long ptr, boolean resizable){
        return true;
    }

    @Override
    protected boolean _requestFocus(long ptr, int event) {
        return MonocleWindowManager.getInstance().requestFocus(this);
    }

    @Override
    protected void _setFocusable(long ptr, boolean isFocusable){}

    @Override
    protected boolean _setTitle(long ptr, String title) {
        return true;
    }

    @Override
    protected void _setLevel(long ptr, int level) {}

    @Override
    protected void _setAlpha(long ptr, float alpha) {}

    @Override
    protected boolean _setBackground(long ptr, float r, float g, float b) {
        return true;
    }

    @Override
    protected void _setEnabled(long ptr, boolean enabled){}

    @Override
    protected boolean _setMinimumSize(long ptr, int width, int height) {
        return true;
    }

    @Override
    protected boolean _setMaximumSize(long ptr, int width, int height) {
        return true;
    }

    @Override
    protected void _setIcon(long ptr, Pixels pixels){}

    @Override
    protected boolean _grabFocus(long ptr) {
        return MonocleWindowManager.getInstance().grabFocus(this);
    }

    @Override
    protected void _ungrabFocus(long ptr) {
        MonocleWindowManager.getInstance().ungrabFocus(this);
    }

    /**
     * The functions below are used when the platform support modality natively.
     * Currently only GTK is using it. This functionality is disabled by
     * default. In order to enable it this class need to override Window::
     * supportsPlatformModality() to return true.
     *
     */
    @Override
    protected void _enterModal(long ptr) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void _enterModalWithWindow(long dialog, long window) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void _exitModal(long ptr) {
        throw new UnsupportedOperationException();
    }

    //**************************************************************
    // wrappers so Application run loop can get where it needs to go
    protected void _notifyClose() {
        //This event is called by MonocleWindowManager when a window needs to be
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
        MonocleView view = (MonocleView) getView();
        if (view != null) {
            view._notifyResize(width, height);
        }
    }

    protected void _notifyExpose(final int x, final int y, final int width,
                                 final int height) {
        MonocleView view = (MonocleView) getView();
        if (view != null) {
            view._notifyRepaint(x, y, width, height);
        }
    }

    protected void _notifyFocusUngrab() {
        notifyFocusUngrab();
    }

    protected void _notifyFocusDisabled() {
        notifyFocusDisabled();
    }

    //**************************************************************

    @Override protected void _setCursor(long ptr, Cursor cursor) {
        ((MonocleCursor) cursor).applyCursor();
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
