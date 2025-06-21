/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.gtk;

import com.sun.glass.ui.Cursor;
import com.sun.glass.events.WindowEvent;
import com.sun.glass.ui.HeaderButtonMetrics;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.HeaderButtonOverlay;
import com.sun.javafx.tk.HeaderAreaType;

class GtkWindow extends Window {

    public GtkWindow(Window owner, Screen screen, int styleMask) {
        super(owner, screen, styleMask);

        if (isExtendedWindow()) {
            prefHeaderButtonHeightProperty().subscribe(this::onPrefHeaderButtonHeightChanged);
        }
    }

    @Override
    protected native long _createWindow(long ownerPtr, long screenPtr, int mask);

    @Override
    protected native boolean _close(long ptr);

    @Override
    protected native boolean _setView(long ptr, View view);

    @Override
    protected native void _updateViewSize(long ptr);

    @Override
    protected boolean _setMenubar(long ptr, long menubarPtr) {
        //TODO is it needed?
        return true;
    }

    private native void minimizeImpl(long ptr, boolean minimize);

    private native void maximizeImpl(long ptr, boolean maximize, boolean wasMaximized);

    private native void setVisibleImpl(long ptr, boolean visible);

    @Override
    protected native boolean _setResizable(long ptr, boolean resizable);

    @Override
    protected native boolean _requestFocus(long ptr, int event);

    @Override
    protected native void _setFocusable(long ptr, boolean isFocusable);

    @Override
    protected native boolean _grabFocus(long ptr);

    @Override
    protected native void _ungrabFocus(long ptr);

    @Override
    protected native boolean _setTitle(long ptr, String title);

    @Override
    protected native void _setLevel(long ptr, int level);

    @Override
    protected native void _setAlpha(long ptr, float alpha);

    @Override
    protected native boolean _setBackground(long ptr, float r, float g, float b);

    @Override
    protected native void _setEnabled(long ptr, boolean enabled);

    private native boolean _setSystemMinimumSize(long ptr, int width, int height);

    @Override
    protected native boolean _setMinimumSize(long ptr, int width, int height);

    @Override
    protected native boolean _setMaximumSize(long ptr, int width, int height);

    @Override
    protected native void _setIcon(long ptr, Pixels pixels);

    @Override
    protected native void _toFront(long ptr);

    @Override
    protected native void _toBack(long ptr);

    protected native long _getNativeWindowImpl(long ptr);

    private native void _showSystemMenu(long ptr, int x, int y);

    private native boolean isVisible(long ptr);

    @Override
    protected boolean _setVisible(long ptr, boolean visible) {
        setVisibleImpl(ptr, visible);
        return isVisible(ptr);
    }

    @Override
    protected boolean _minimize(long ptr, boolean minimize) {
        minimizeImpl(ptr, minimize);
        notifyStateChanged(WindowEvent.MINIMIZE);
        return minimize;
    }

    @Override
    protected boolean _maximize(long ptr, boolean maximize,
                                boolean wasMaximized) {
        maximizeImpl(ptr, maximize, wasMaximized);
        notifyStateChanged(WindowEvent.MAXIMIZE);
        return maximize;
    }

    protected void notifyStateChanged(final int state) {
        switch (state) {
            case WindowEvent.MINIMIZE:
            case WindowEvent.MAXIMIZE:
            case WindowEvent.RESTORE:
                notifyResize(state, getWidth(), getHeight());
                break;
            default:
                System.err.println("Unknown window state: " + state);
                break;
        }
    }

    @Override
    protected void _setCursor(long ptr, Cursor cursor) {
        if (cursor.getType() == Cursor.CURSOR_CUSTOM) {
            _setCustomCursor(ptr, cursor);
        } else {
            _setCursorType(ptr, cursor.getType());
        }
    }

    private native void _setCursorType(long ptr, int type);
    private native void _setCustomCursor(long ptr, Cursor cursor);

    /**
     * The lowest level (X11) window handle.
     * (Used in prism to create GLContext)
     * @return X11 Window handle is returned.
     */
    @Override
    public long getNativeWindow() {
        return _getNativeWindowImpl(super.getNativeWindow());
    }

    @Override
    protected native void _setBounds(long ptr, int x, int y, boolean xSet, boolean ySet,
                                       int w, int h, int cw, int ch,
                                       float xGravity, float yGravity);

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

    @Override
    public long getRawHandle() {
        long ptr = super.getRawHandle();
        return ptr == 0L ? 0L : _getNativeWindowImpl(ptr);
    }

    /**
     * Opens a system menu at the specified coordinates.
     *
     * @param x the X coordinate in physical pixels
     * @param y the Y coordinate in physical pixels
     */
    public void showSystemMenu(int x, int y) {
        _showSystemMenu(super.getRawHandle(), x, y);
    }

    /**
     * Creates or disposes the {@link HeaderButtonOverlay} when the preferred header button height has changed.
     * <p>
     * If the preferred height is zero, the overlay is disposed; if the preferred height is non-zero, the
     * {@link #headerButtonOverlay} and {@link #headerButtonMetrics} properties will hold the overlay and
     * its metrics.
     *
     * @param height the preferred header button height
     */
    private void onPrefHeaderButtonHeightChanged(Number height) {
        // Return early if we can keep the existing overlay instance.
        if (height.doubleValue() != 0 && headerButtonOverlay.get() != null) {
            return;
        }

        if (headerButtonOverlay.get() instanceof HeaderButtonOverlay overlay) {
            overlay.dispose();
        }

        if (height.doubleValue() == 0) {
            headerButtonOverlay.set(null);
            headerButtonMetrics.set(HeaderButtonMetrics.EMPTY);
        } else {
            HeaderButtonOverlay overlay = createHeaderButtonOverlay();
            overlay.metricsProperty().subscribe(headerButtonMetrics::set);
            headerButtonOverlay.set(overlay);
        }
    }

    /**
     * Creates a new {@code HeaderButtonOverlay} instance.
     */
    private HeaderButtonOverlay createHeaderButtonOverlay() {
        var overlay = new HeaderButtonOverlay(
            PlatformThemeObserver.getInstance().stylesheetProperty(),
            isModal() || getOwner() != null, isUtilityWindow(),
            (getStyleMask() & RIGHT_TO_LEFT) != 0);

        // Set the system-defined absolute minimum size to the size of the window buttons area,
        // regardless of whether the application has specified a smaller minimum size.
        overlay.metricsProperty().subscribe(metrics -> {
            int w = (int)(metrics.totalInsetWidth() * platformScaleX);
            int h = (int)(metrics.maxInsetHeight() * platformScaleY);
            _setSystemMinimumSize(super.getRawHandle(), w, h);
        });

        overlay.prefButtonHeightProperty().bind(prefHeaderButtonHeightProperty());
        return overlay;
    }

    /**
     * Returns whether the window is draggable at the specified coordinate.
     * <p>
     * This method is called from native code.
     *
     * @param x the X coordinate in physical pixels
     * @param y the Y coordinate in physical pixels
     */
    @SuppressWarnings("unused")
    private boolean dragAreaHitTest(int x, int y) {
        // A full-screen window has no draggable area.
        if (view == null || view.isInFullscreen() || !isExtendedWindow()) {
            return false;
        }

        double wx = x / platformScaleX;
        double wy = y / platformScaleY;

        if (headerButtonOverlay.get() instanceof HeaderButtonOverlay overlay && overlay.buttonAt(wx, wy) != null) {
            return false;
        }

        View.EventHandler eventHandler = view.getEventHandler();
        if (eventHandler == null) {
            return false;
        }

        return eventHandler.pickHeaderArea(wx, wy) == HeaderAreaType.DRAGBAR;
    }
}
