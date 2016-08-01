/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;

final class MonocleCursor extends Cursor {
    private byte[] image;
    private int hotspotX;
    private int hotspotY;

    MonocleCursor(int type) {
        super(type);
        image = getImage(type);
        hotspotX = 0;
        hotspotY = 0;
    }

    MonocleCursor(int x, int y, Pixels pixels) {
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

    private static String cursorResourceName(int cursorType) {
        switch (cursorType) {
            case CURSOR_CLOSED_HAND: return "ClosedHand";
            case CURSOR_CROSSHAIR: return "Crosshair";
            case CURSOR_DISAPPEAR: return "Disappear";
            case CURSOR_MOVE: return "Move";
            case CURSOR_OPEN_HAND: return "OpenHand";
            case CURSOR_POINTING_HAND: return "PointingHand";
            case CURSOR_RESIZE_DOWN: return "ResizeDown";
            case CURSOR_RESIZE_LEFT: return "ResizeLeft";
            case CURSOR_RESIZE_LEFTRIGHT: return "ResizeLeftRight";
            case CURSOR_RESIZE_NORTHEAST: return "ResizeNorthEast";
            case CURSOR_RESIZE_NORTHWEST: return "ResizeNorthWest";
            case CURSOR_RESIZE_RIGHT: return "ResizeRight";
            case CURSOR_RESIZE_SOUTHEAST: return "ResizeSouthEast";
            case CURSOR_RESIZE_SOUTHWEST: return "ResizeSouthWest";
            case CURSOR_RESIZE_UP: return "ResizeUp";
            case CURSOR_RESIZE_UPDOWN: return "ResizeUpDown";
            case CURSOR_TEXT: return "Text";
            case CURSOR_WAIT: return "Wait";
            default: return "Default";
        }
    }

    private static byte[] getImage(int cursorType) {
        InputStream in = null;
        try {
            in = MonocleCursor.class.getResourceAsStream(
                    "Cursor"
                    + cursorResourceName(cursorType)
                    + "Translucent.raw");
            byte[] b = new byte[1024];
            int bytesRead = 0;
            while (bytesRead < 1024) {
                int read = in.read(b, bytesRead, 1024 - bytesRead);
                if (read >= 0) {
                    bytesRead += read;
                } else {
                    throw new IOException("Incomplete cursor resource");
                }
            }
            return b;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) { }
            }
        }
    }

}
