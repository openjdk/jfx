/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

class EGLCursor extends NativeCursor {

    private static final int CURSOR_WIDTH = 16;
    private static final int CURSOR_HEIGHT = 16;


    private native void _initEGLCursor(int cursorWidth, int cursorHeight);
    private native void _setVisible(boolean visible);
    private native void _setLocation(int x, int y);
    private native void _setImage(byte[] cursorImage);

    EGLCursor() {
        _initEGLCursor(CURSOR_WIDTH, CURSOR_HEIGHT);
    }

    @Override
    Size getBestSize() {
        return new Size(CURSOR_WIDTH, CURSOR_HEIGHT);
    }

    @Override
    void setVisibility(boolean visibility) {
        isVisible = visibility;
        _setVisible(visibility);
    }

    private void updateImage(boolean always) {
        System.out.println("EGLCursor.updateImage: not implemented");
    }

    @Override
    void setImage(byte[] cursorImage) {
        _setImage(cursorImage);
    }

    @Override
    void setLocation(int x, int y) {
        _setLocation(x, y);
    }

    @Override
    void setHotSpot(int hotspotX, int hotspotY) {
    }

    @Override
    void shutdown() {
    }
}
