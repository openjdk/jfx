/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Size;

/**
 * MacOSX platform implementation class for Cursor.
 */
final class MacCursor extends Cursor {

    private native static void _initIDs();
    static {
        _initIDs();
    }

    protected MacCursor(int type) {
        super(type);
    }

    protected MacCursor(int x, int y, Pixels pixels) {
        super(x, y, pixels);
    }

    @Override native protected long _createCursor(int x, int y, Pixels pixels);

    void set() {
        int type = getType();
        isCursorNONE = type == CURSOR_NONE;
        // Re-apply the last requested cursor visibility, and take into account
        // the isCursorNONE flag (see the setter below).
        setVisible(isVisible);

        switch (type) {
            case CURSOR_NONE:
                break;
            case CURSOR_CUSTOM:
                _setCustom(getNativeCursor());
                break;
            default:
                _set(type);
                break;
        }
    }

    native private void _set(int type);
    native private void _setCustom(long ptr);

    native private static void _setVisible(boolean visible);
    native private static Size _getBestSize(int width, int height);

    // [NSCursor (un)hide] method calls must be balanced,
    // so we keep the state in the boolean field to avoid
    // multiple native calls.
    private static boolean isNSCursorVisible = true;

    // These two flags help us handle the NONE cursor which
    // we emulate by hidding the cursor.
    private static boolean isCursorNONE = false;
    private static boolean isVisible = true; // as requested by client code

    static void setVisible_impl(boolean visible) {
        isVisible = visible;
        final boolean effectiveVisible = visible && !isCursorNONE;
        if (isNSCursorVisible == effectiveVisible) {
            return;
        } else {
            isNSCursorVisible = effectiveVisible;
            _setVisible(effectiveVisible);
        }
    }

    static Size getBestSize_impl(int width, int height) {
        return _getBestSize(width, height);
    }
}

