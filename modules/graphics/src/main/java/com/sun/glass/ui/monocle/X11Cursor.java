/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.ByteBuffer;

public class X11Cursor extends NativeCursor {

    private static final int CURSOR_WIDTH = 16;
    private static final int CURSOR_HEIGHT = 16;
    protected long xdisplay;
    protected long xwindow;
    private ByteBuffer transparentCursorBuffer;
    private long transparentCursor;
    private long pixmap;

    X11Cursor() {
        xdisplay =
            NativePlatformFactory.getNativePlatform().accScreen.platformGetNativeDisplay();
        xwindow = NativePlatformFactory.getNativePlatform().accScreen.platformGetNativeWindow();
        transparentCursorBuffer = ByteBuffer.allocateDirect(4);
        pixmap = X.XCreateBitmapFromData(xdisplay, xwindow,
                transparentCursorBuffer, 1, 1);
        X.XColor black = new X.XColor();
        black.setRed(black.p, 0);
        black.setGreen(black.p, 0);
        black.setBlue(black.p, 0);
        transparentCursor = X.XCreatePixmapCursor(xdisplay, pixmap,
                pixmap, black.p, black.p, 0, 0);
    }

    @Override
    Size getBestSize() {
        return new Size(CURSOR_WIDTH, CURSOR_HEIGHT);
    }

    @Override
    void setVisibility(boolean visibility) {
        if (isVisible && !visibility) {
            // make the X cursor invisible
            X.XDefineCursor(xdisplay, xwindow, transparentCursor);
            MonocleWindowManager.getInstance().repaintAll();
        } else if (!isVisible && visibility) {
            // make the cursor visible
            X.XUndefineCursor(xdisplay, xwindow);
            MonocleWindowManager.getInstance().repaintAll();
        }
        isVisible = visibility;
    }

    @Override
    void setImage(byte[] cursorImage) {
    }

    @Override
    void setLocation(int x, int y) {
    }

    @Override
    void setHotSpot(int hotspotX, int hotspotY) {

    }

    @Override
    void shutdown() {
        X.XFreePixmap(xdisplay, pixmap);
    }
}
