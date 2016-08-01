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


package com.sun.glass.ui.lens;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Size;
import com.sun.glass.ui.Application;

final class LensCursor extends Cursor {

    // LensCursor doesn't use ptr from superclass
    private long ptr = 0;

    protected LensCursor(int type) {
        super(type);

        if (type != CURSOR_NONE) {
            ptr = _createNativeCursorByType(type);
        }
    }

    protected LensCursor(int x, int y, Pixels pixels) {
        super(x, y, pixels);
        ptr = getNativeCursor();
    }


    protected void finalize() throws Throwable {
        try {
            if (ptr != 0) {
                _releaseNativeCursor(ptr);
            }
        } finally {
            super.finalize();
        }
    }

    @Override
    protected long _createCursor(int x, int y, Pixels pixels) {

        long res = 0;
        Buffer data = pixels.getPixels();
        int width = pixels.getWidth();
        int height = pixels.getHeight();

        if (data != null) {
            if (!data.isDirect()) {
                if (pixels.getBytesPerComponent() == 4) {
                    IntBuffer ints = (IntBuffer)(data.rewind());
                    int[] intArray = ints.array();
                    res =  _createNativeCursorInts(x, y, intArray, width, height);
                } else if (pixels.getBytesPerComponent() == 1) {
                    ByteBuffer bytes = (ByteBuffer)(data.rewind());
                    byte[] byteArray = bytes.array();
                    res =  _createNativeCursorBytes(x, y, byteArray, width, height);
                }
            } else {
                data.rewind();
                res =  _createNativeCursorDirect(x, y, data, data.capacity(), width , height);

            }
        }
        return res;
    }

    void set() {
        if (ptr != 0) {
            _setNativeCursor(ptr);
        }

        int type = getType();
        if (type == CURSOR_NONE) {
            // CURSOR_NONE is mapped to setVisible(false) and will be registered
            // in LensApplication as a user preference to not show the cursor.
            ((LensApplication)Application.GetApplication()).staticCursor_setVisible(false);
        } else {
            ((LensApplication)Application.GetApplication()).staticCursor_setVisible(true);
        }
    }



    static  void setVisible_impl(boolean visible) {
        _setVisible(visible);
    }

    static Size getBestSize_impl(int width, int height) {
        return new Size(16, 16);
    }


    native private void _setNativeCursor(long ptr);
    native private void _releaseNativeCursor(long ptr);

    native private long _createNativeCursorByType(int type);

    native private long _createNativeCursorInts(int x, int y, int[] array, int width, int height);
    native private long _createNativeCursorBytes(int x, int y, byte[] array, int width, int height);
    native private long _createNativeCursorDirect(int x, int y, Buffer array, int capacity, int width, int height);

    native static private void _setVisible(boolean isVisible);
}
