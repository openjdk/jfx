/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.mac;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Robot;

/**
 * MacOSX platform implementation class for Robot.
 */
final class MacRobot extends Robot {

    // TODO: get rid of native Robot object
    private long ptr = 0;

    private native long _init();
    @Override protected void _create() {
        ptr = _init();
    }

    private native void _destroy(long ptr);
    @Override protected void _destroy() {
        if (ptr == 0) {
            return;
        }
        _destroy(ptr);
    }

    @Override native protected void _keyPress(int code);
    @Override native protected void _keyRelease(int code);

    private native void _mouseMove(long ptr, int x, int y);
    @Override protected void _mouseMove(int x, int y) {
        if (ptr == 0) {
            return;
        }
        _mouseMove(ptr, x, y);
    }

    private native void _mousePress(long ptr, int buttons);
    @Override protected void _mousePress(int buttons) {
        if (ptr == 0) {
            return;
        }
        _mousePress(ptr, buttons);
    }

    private native void _mouseRelease(long ptr, int buttons);
    @Override protected void _mouseRelease(int buttons) {
        if (ptr == 0) {
            return;
        }
        _mouseRelease(ptr, buttons);
    }

    @Override native protected void _mouseWheel(int wheelAmt);

    private native int _getMouseX(long ptr);
    @Override protected int _getMouseX() {
        if (ptr == 0) {
            return 0;
        }
        return _getMouseX(ptr);
    }

    private native int _getMouseY(long ptr);
    @Override protected int _getMouseY() {
        if (ptr == 0) {
            return 0;
        }
        return _getMouseY(ptr);
    }

    @Override native protected int _getPixelColor(int x, int y);
    @Override native protected void _getScreenCapture(int x, int y, int width, int height, int[] data);

}

