/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.win;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.Map;

/**
 * MS Windows platform implementation class for View.
 */
final class WinView extends View {

    private native static void _initIDs();
    static {
        _initIDs();
        multiClickTime = _getMultiClickTime_impl();
        multiClickMaxX = _getMultiClickMaxX_impl();
        multiClickMaxY = _getMultiClickMaxY_impl();
    }

    // Constants
    private static final long multiClickTime;
    private static final int multiClickMaxX, multiClickMaxY;

    protected WinView() {
        super();
    }

    native private static long _getMultiClickTime_impl();
    native private static int _getMultiClickMaxX_impl();
    native private static int _getMultiClickMaxY_impl();

    static long getMultiClickTime_impl() {
        return multiClickTime;
    }

    static int getMultiClickMaxX_impl() {
        return multiClickMaxX;
    }

    static int getMultiClickMaxY_impl() {
        return multiClickMaxY;
    }

    @Override
    protected int _getNativeFrameBuffer(long ptr) {
        return 0;
    }

    @Override native protected void _enableInputMethodEvents(long ptr, boolean enable);
    @Override native protected void _finishInputMethodComposition(long ptr);

    @Override native protected long _create(Map caps);
    @Override native protected long _getNativeView(long ptr);
    @Override native protected int _getX(long ptr);
    @Override native protected int _getY(long ptr);
    @Override native protected void _setParent(long ptr, long parentPtr);
    @Override native protected boolean _close(long ptr);
    @Override native protected void _scheduleRepaint(long ptr);
    @Override native protected void _begin(long ptr);
    @Override native protected void _end(long ptr);
    @Override native protected void _uploadPixels(long ptr, Pixels pixels);
    @Override native protected boolean _enterFullscreen(long ptr, boolean animate, boolean keepRatio, boolean hideCursor);
    @Override native protected void _exitFullscreen(long ptr, boolean animate);

    @Override
    protected void notifyResize(int width, int height) {
        super.notifyResize(width, height);

        // After resizing, do a move notification to force the view relocation.
        // When moving to a screen with different DPI settings, its location needs
        // to be recalculated.
        updateLocation();
    }
}

