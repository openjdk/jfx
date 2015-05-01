/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.ios;

import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.View;
import java.util.Map;

/**
 * iOS View platform implementation.
 */
final class IosView extends View {

    private long nativePtr;

    public IosView() {
        super();
    }

    // Constants /reusing Mac OS X values
    private static final long multiClickTime =  300;
    private static final int multiClickMaxX = 2;
    private static final int multiClickMaxY = 2;

    static long _getMultiClickTime() {
        return multiClickTime;
    }

    static int _getMultiClickMaxX() {
        return multiClickMaxX;
    }

    static int _getMultiClickMaxY() {
        return multiClickMaxY;
    }

    // See View
    @Override protected void _enableInputMethodEvents(long ptr, boolean enable) { }

    @Override native protected int _getNativeFrameBuffer(long ptr);
    @Override native protected long _create(Map caps);

    @Override native protected long _getNativeView(long ptr);

    @Override native protected int _getX(long ptr);
    @Override protected native int _getY(long ptr);

    @Override native protected boolean _close(long ptr);

    @Override native protected void _scheduleRepaint(long ptr);

    @Override native protected void _begin(long ptr);
    @Override native protected void _end(long ptr);

    @Override native protected boolean _enterFullscreen(long ptr, boolean animate, boolean keepRatio, boolean hideCursor);
    @Override native protected void _exitFullscreen(long ptr, boolean animate);

    @Override native protected void _setParent(long ptr, long parentPtr);
    @Override protected void _uploadPixels(long ptr, Pixels pixels) {
        throw new RuntimeException("IosView._uploadPixels() UNIMPLEMENTED.");
    }
}

