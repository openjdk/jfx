/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui;

import java.lang.annotation.Native;

public abstract class Cursor {

    @Native public final static int CURSOR_NONE = -1;
    @Native public final static int CURSOR_CUSTOM = 0;
    @Native public final static int CURSOR_DEFAULT = 1;
    @Native public final static int CURSOR_TEXT = 2;
    @Native public final static int CURSOR_CROSSHAIR = 3;
    @Native public final static int CURSOR_CLOSED_HAND = 4;
    @Native public final static int CURSOR_OPEN_HAND = 5;
    @Native public final static int CURSOR_POINTING_HAND = 6;
    @Native public final static int CURSOR_RESIZE_LEFT = 7;
    @Native public final static int CURSOR_RESIZE_RIGHT = 8;
    @Native public final static int CURSOR_RESIZE_UP = 9;
    @Native public final static int CURSOR_RESIZE_DOWN = 10;
    @Native public final static int CURSOR_RESIZE_LEFTRIGHT = 11;
    @Native public final static int CURSOR_RESIZE_UPDOWN = 12;
    @Native public final static int CURSOR_DISAPPEAR = 13;
    @Native public final static int CURSOR_WAIT = 14;
    @Native public final static int CURSOR_RESIZE_SOUTHWEST = 15;
    @Native public final static int CURSOR_RESIZE_SOUTHEAST = 16;
    @Native public final static int CURSOR_RESIZE_NORTHWEST = 17;
    @Native public final static int CURSOR_RESIZE_NORTHEAST = 18;
    @Native public final static int CURSOR_MOVE = 19;
    private final static int CURSOR_MAX = 19;

    private final int type;

    // Native cursor ptr, for custom cursors
    private long ptr;

    protected Cursor(final int type) {
        Application.checkEventThread();
        this.type = type;
    }

    protected Cursor(final int x, final int y, final Pixels pixels) {
        this(CURSOR_CUSTOM);
        ptr = _createCursor(x, y, pixels);
    }

    public final int getType() {
        Application.checkEventThread();
        return type;
    }

    protected final long getNativeCursor() {
        Application.checkEventThread();
        return ptr;
    }

    /**
     * Shows or hides the cursor.
     * <p>
     * If the cursor is currently hidden with a previous call to {@code
     * setVisible(false)}, setting a new cursor shape (e.g. by means of calling
     * {@code Window.setCursor()}) does not automatically display the cursor on
     * the screen until the client code calls {@code setVisible(true)} to show
     * the cursor again.
     * <p>
     * If the mouse cursor is located over a non-Glass window at the time of
     * calling this method, the call may or may not affect the native cursor's
     * visibility. This behavior is platform-dependent.
     * <p>
     * When the mouse pointer is moved over a non-Glass window, depending on
     * the native platform behavior, the cursor may or may not become visible
     * on the screen, even if it was previously hidden by calling {@code
     * setVisible(false)}. After this occurs, on some platforms the cursor may
     * even remain visible permanently. For example, Mac OS X makes the cursor
     * visible unconditionally when the mouse is moved over the Dock or Menu
     * Bar areas. There's no way to detect that the native cursor became
     * visible, however, from Glass perspective it is still considered hidden,
     * and thus, when the mouse cursor is needed again, the app should call
     * {@code setVisible(true)} in order to continue to operate properly.
     * <p>
     * Calling this method multiple times with the same argument may not have
     * any effect. For example, if the cursor was hidden and the native OS
     * restored its visibility, calling {@code setVisible(false)} again may not
     * hide the cursor. If the app needs to ultimately hide the cursor, it
     * should first show the cursor again, and then proceed with hiding it.
     */
    public static void setVisible(boolean visible) {
        Application.checkEventThread();
        Application.GetApplication().staticCursor_setVisible(visible);
    }

    /**
     * Returns the 'best' cursor size based on the given preferred size.
     */
    public static Size getBestSize(int width, int height) {
        Application.checkEventThread();
        return Application.GetApplication().staticCursor_getBestSize(width, height);
    }

    protected abstract long _createCursor(int x, int y, Pixels pixels);
}
