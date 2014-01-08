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

package com.sun.glass.ui.monocle;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Pixels;

final class MonocleCursor extends Cursor {
    byte[] image;
    int hotspotX;
    int hotspotY;

    protected MonocleCursor(int type) {
        super(type);
        image = getImage(type);
        hotspotX = 0;
        hotspotY = 0;
    }

    protected MonocleCursor(int x, int y, Pixels pixels) {
        super(x, y, pixels);
    }

    void applyCursor() {
        int type = getType();
        if (type == CURSOR_NONE) {
            // CURSOR_NONE is mapped to setVisible(false) and will be registered
            // in MonocleApplication as a preference to not show the cursor.
            ((MonocleApplication) Application.GetApplication())
                    .staticCursor_setVisible(false);
        } else {
            NativeCursor cursor = NativePlatformFactory.getNativePlatform().getCursor();
            cursor.setImage(image);
            ((MonocleApplication) Application.GetApplication())
                    .staticCursor_setVisible(true);
        }
    }

    @Override
    protected long _createCursor(int x, int y, Pixels pixels) {
        hotspotX = x;
        hotspotY = y;
        image = pixels.asByteBuffer().array();
        return 1l;
    }

    private static byte[] getImage(int cursorType) {
        // TODO: use the correct cursor image and load it more efficiently
        // TODO: this way of storing the cursor doesn't even work because it
        // mixes up UTF-16 and UTF-8 encodings
        String s =
            "\0\0\0\324\0\0\0\25\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\377\377\377\0"
            + "\377\377\377\0\377\377\377\0\377\377\377\0\0\0\0\377\0\0\0\324\0\0\0\25\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0"
            + "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\377\377\377\0\377\377\377\0\377\377\377\0\377\377\377\0\0\0\0\377"
            + "\314\314\314\377\0\0\0\324\0\0\0\25\377\377\377\0\377\377\377\0\377\377\377\0\377\377\377\0\0\0\0\0\0\0\0\0"
            + "\0\0\0\0\0\0\0\0\377\377\377\0\377\377\377\0\377\377\377\0\377\377\377\0\0\0\0\377\377\377\377\377\314\314\314\377"
            + "\0\0\0\324\0\0\0\25\377\377\377\0\377\377\377\0\377\377\377\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\377\377\377\0"
            + "\377\377\377\0\377\377\377\0\377\377\377\0\0\0\0\377\377\377\377\377\377\377\377\377\314\314\314\377\0\0\0\324"
            + "\0\0\0\25\377\377\377\0\377\377\377\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\377\377\377\0\377\377\377\0\377\377\377\0"
            + "\377\377\377\0\0\0\0\377\377\377\377\377\377\377\377\377\377\377\377\377\314\314\314\377\0\0\0\324\0\0\0\25"
            + "\377\377\377\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\377\377\377\0\377\377\377\0\377\377\377\0\377\377\377\0"
            + "\0\0\0\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\314\314\314\377\0\0\0\324\0\0\0\25"
            + "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\377\377\377\0\377\377\377\0\377\377\377\0\377\377\377\0\0\0\0\377\377\377\377\377"
            + "\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\314\314\314\377\0\0\0\324\0\0\0\25\0\0\0\0"
            + "\0\0\0\0\0\0\0\0\377\377\377\0\377\377\377\0\377\377\377\0\377\377\377\0\0\0\0\377\377\377\377\377\377\377\377\377"
            + "\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\314\314\314\377\0\0\0\324\0\0\0\25\0\0\0\0"
            + "\0\0\0\0\377\377\377\0\377\377\377\0\377\377\377\0\377\377\377\0\0\0\0\377\377\377\377\377\376\376\376\377"
            + "\377\377\377\377\377\377\377\377\0\0\0\377\0\0\0\377\0\0\0\377\0\0\0\377\0\0\0\323\0\0\0\25\0\0\0\0\377\377\377\0"
            + "\377\377\377\0\377\377\377\0\377\377\377\0\0\0\0\377\377\377\377\377\311\311\311\377\237\237\237\377\376\376\376\377"
            + "\52\52\52\377\0\0\0\100\377\377\377\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\377\377\377\0\377\377\377\0\377\377\377\0"
            + "\377\377\377\0\0\0\0\377\321\321\321\377\0\0\0\324\0\0\0\346\352\352\352\377\315\315\315\377\0\0\0\260"
            + "\377\377\377\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\377\377\377\0\377\377\377\0\377\377\377\0\377\377\377\0"
            + "\0\0\0\377\1\1\1\342\0\0\0\34\0\0\0\201\266\266\266\377\366\366\366\377\25\25\25\376\0\0\0\52\0\0\0\0"
            + "\0\0\0\0\0\0\0\0\0\0\0\0\377\377\377\0\377\377\377\0\377\377\377\0\377\377\377\0\0\0\0\352\0\0\0\44\0\0\0\0"
            + "\0\0\0\34\20\20\20\373\364\364\364\377\301\301\301\377\0\0\0\232\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\377\377\377\0"
            + "\377\377\377\0\377\377\377\0\377\377\377\0\0\0\0\62\0\0\0\0\0\0\0\0\377\377\377\0\0\0\0\257\312\312\312\377"
            + "\305\305\305\377\0\0\0\366\0\0\0\20\0\0\0\0\0\0\0\0\0\0\0\0\377\377\377\0\377\377\377\0\377\377\377\0"
            + "\377\377\377\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\110\1\1\1\377\0\0\0\266\0\0\0\72\0\0\0\0\0\0\0\0"
            + "\0\0\0\0\0\0\0\0\377\377\377\0\377\377\377\0\377\377\377\0\377\377\377\0";
        return s.getBytes();
    }

}
