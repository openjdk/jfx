/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.Buffer;
import java.nio.ByteBuffer;

/** Cursor using a framebuffer overlay on OMAP3.
 * TODO: Can we mmap the image and location files?
 */
class OMAPCursor extends NativeCursor {

    private int hotspotX;
    private int hotspotY;
    private int offsetX;
    private int offsetY;
    private int cursorX;
    private int cursorY;
    private static final int INT_KEY = 0x12121212;
    private static final int SHORT_KEY = 0xABAB;
    private static final int CURSOR_WIDTH = 16;
    private static final int CURSOR_HEIGHT = 16;
    private Buffer cursorBuffer;
    private Buffer offsetCursorBuffer;
    private byte[] offsetCursor;
    private int screenWidth;
    private int screenHeight;
    private int cursorDepth;
    private int transparentPixel;

    OMAPCursor() {
        try {
            SysFS.write("/sys/class/graphics/fb1/virtual_size", "16,16");
            SysFS.write("/sys/devices/platform/omapdss/overlay1/output_size", "16,16");
            SysFS.write("/sys/devices/platform/omapdss/manager0/trans_key_enabled",
                        "1");
            SysFS.write("/sys/devices/platform/omapdss/manager0/trans_key_type",
                        "video-source");
            cursorDepth = SysFS.readInt("/sys/class/graphics/fb1/bits_per_pixel");
            switch (cursorDepth) {
                case 16:
                    transparentPixel = SHORT_KEY;
                    break;
                case 32:
                    transparentPixel = INT_KEY;
                    break;
                default:
                    throw new IOException(
                            "Cannot use an OMAP cursor with a bit depth of "
                            + cursorDepth);
            }
            SysFS.write("/sys/devices/platform/omapdss/manager0/trans_key_value",
                        Long.toString(transparentPixel));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to initialize OMAP cursor");
        }
        NativeScreen screen = NativePlatformFactory.getNativePlatform().getScreen();
        screenWidth = screen.getWidth();
        screenHeight = screen.getHeight();
    }

    @Override
    Size getBestSize() {
        return new Size(CURSOR_WIDTH, CURSOR_HEIGHT);
    }

    @Override
    void setVisibility(boolean visibility) {
        try {
            SysFS.write("/sys/devices/platform/omapdss/overlay1/enabled",
                        visibility ? "1" : "0");
        } catch (IOException e) {
            System.err.format("Failed to %s OMAP cursor\n",
                              (visibility ? "enable" : "disable"));
        }
        isVisible = visibility;
    }

    private void updateImage(boolean always) {
        int newOffsetX, newOffsetY;
        newOffsetX = Math.max(0, CURSOR_WIDTH + cursorX - screenWidth);
        newOffsetY = Math.max(0, CURSOR_HEIGHT + cursorY - screenHeight);
        if (newOffsetX != offsetX || newOffsetY != offsetY || always) {
            NativeCursors.offsetCursor(cursorBuffer, offsetCursorBuffer,
                                       newOffsetX, newOffsetY,
                                       CURSOR_WIDTH, CURSOR_HEIGHT,
                                       cursorDepth, transparentPixel);
            offsetX = newOffsetX;
            offsetY = newOffsetY;
            try {
                SysFS.write("/dev/fb1", offsetCursor);
            } catch (IOException e) {
                System.err.println("Failed to write OMAP cursor image");
            }
        }
    }

    @Override
    void setImage(byte[] cursorImage) {
        // Convert the cursor to the color-keyed format
        ByteBuffer bb = ByteBuffer.allocate(cursorImage.length);
        cursorBuffer = cursorDepth == 32 ? bb.asIntBuffer() : bb.asShortBuffer();
        NativeCursors.colorKeyCursor(cursorImage, cursorBuffer,
                                     cursorDepth, transparentPixel);
        // Create an offset version of the cursor for rendering
        offsetCursor = new byte[cursorImage.length];
        bb = ByteBuffer.wrap(offsetCursor);
        offsetCursorBuffer = cursorDepth == 32 ? bb.asIntBuffer() : bb.asShortBuffer();
        updateImage(true);
    }

    @Override
    void setLocation(int x, int y) {
        cursorX = x;
        cursorY = y;
        updateImage(false);
        try {
            SysFS.write("/sys/devices/platform/omapdss/overlay1/position",
                        (cursorX - hotspotX - offsetX)
                        + "," + (cursorY - hotspotY - offsetY));
        } catch (IOException e) {
            System.err.println("Failed to set OMAP cursor position");
        }
    }

    @Override
    void setHotSpot(int hotspotX, int hotspotY) {
        this.hotspotX = hotspotX;
        this.hotspotY = hotspotY;
    }

    @Override
    void shutdown() {
        try {
            SysFS.write("/sys/devices/platform/omapdss/overlay1/enabled", "0");
        } catch (IOException e) {
            System.err.println("Failed to shut down OMAP cursor");
        }
    }
}
