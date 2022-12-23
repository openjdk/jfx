/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.Size;

import java.nio.ByteBuffer;

/** Provides a native cursor for use with X11. Since X11 doesn't provide any
 * programmatic way to dynamically hide/show the cursor, we use a workaround of
 * creating a 1x1 transparent pixmap cursor which we activate whenever we want
 * to hide the cursor.
 *
 */
public class X11Cursor extends NativeCursor {

    private static final int CURSOR_WIDTH = 16;
    private static final int CURSOR_HEIGHT = 16;
    protected long xdisplay;
    protected long xwindow;
    private ByteBuffer transparentCursorBuffer;
    private long transparentCursor;
    private long pixmap;
    private static X xLib = X.getX();

    X11Cursor() {
        xdisplay =
            NativePlatformFactory.getNativePlatform().accScreen.platformGetNativeDisplay();
        xwindow = NativePlatformFactory.getNativePlatform().accScreen.platformGetNativeWindow();
        /* Create the transparent cursor - we set this cursor active whenever
         * we need the cursor to appear to be hidden
         */
        transparentCursorBuffer = ByteBuffer.allocateDirect(4);
        pixmap = xLib.XCreateBitmapFromData(xdisplay, xwindow,
                transparentCursorBuffer, 1, 1);
        X.XColor black = new X.XColor();
        black.setRed(black.p, 0);
        black.setGreen(black.p, 0);
        black.setBlue(black.p, 0);
        transparentCursor = xLib.XCreatePixmapCursor(xdisplay, pixmap,
                pixmap, black.p, black.p, 0, 0);
        xLib.XFreePixmap(xdisplay, pixmap);
    }

    @Override
    Size getBestSize() {
        return new Size(CURSOR_WIDTH, CURSOR_HEIGHT);
    }

    /** Set the cursor according to the current visibility and requested
     * visibility
     * @param visibility the new setting for the cursor's visibility.
     */
    @Override
    void setVisibility(boolean visibility) {
        if (isVisible && !visibility) {
            // make the X cursor invisible
            xLib.XDefineCursor(xdisplay, xwindow, transparentCursor);
            MonocleWindowManager.getInstance().repaintAll();
        } else if (!isVisible && visibility) {
            // make the cursor visible
            xLib.XUndefineCursor(xdisplay, xwindow);
            MonocleWindowManager.getInstance().repaintAll();
        }
        isVisible = visibility;
    }

    /** Set the cursor image to the requested bytes
     *
     * @param cursorImage the cursor image, in BYTE_BGRA_PRE format
     */
    @Override
    void setImage(byte[] cursorImage) {
    }

    /** For the X11 cursor, we don't do anything to move the cursor - X handles
     * it for us
     * @param x the new X location on the screen
     * @param y the new Y location on the screen
     */
    @Override
    void setLocation(int x, int y) {
    }

    @Override
    void setHotSpot(int hotspotX, int hotspotY) {

    }

    @Override
    void shutdown() {
    }
}
