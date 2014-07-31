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
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/** Cursor using a framebuffer overlay on Freescale i.MX6. */
class MX6Cursor extends NativeCursor {

    private int hotspotX;
    private int hotspotY;
    private int offsetX;
    private int offsetY;
    private int cursorX;
    private int cursorY;
    private static final int SHORT_KEY = 0xABAB;
    private static final int CURSOR_WIDTH = 16;
    private static final int CURSOR_HEIGHT = 16;
    private Buffer cursorBuffer;
    private Buffer offsetCursorBuffer;
    private ByteBuffer offsetCursorByteBuffer;
    private int screenWidth;
    private int screenHeight;
    private LinuxSystem system;
    private MXCFBPos pos = new MXCFBPos();
    private MXCFBGblAlpha alpha = new MXCFBGblAlpha();
    private long fd = -1;

    private static class MXCFBColorKey extends C.Structure {
        private final IntBuffer data;
        MXCFBColorKey() {
            b.order(ByteOrder.nativeOrder());
            data = b.asIntBuffer();
        }
        @Override
        int sizeof() {
            return 8;
        }
        void setEnable(int enable) {
            data.put(0, enable);
        }
        void setColorKey(int key) {
            data.put(1, key);
        }
    }

    private static class MXCFBGblAlpha extends C.Structure {
        private final IntBuffer data;
        MXCFBGblAlpha() {
            b.order(ByteOrder.nativeOrder());
            data = b.asIntBuffer();
        }
        @Override
        int sizeof() {
            return 8;
        }
        void setEnable(int enable) {
            data.put(0, enable);
        }
        void setAlpha(int alpha) {
            data.put(1, alpha);
        }
    }

    private static class MXCFBPos extends C.Structure {
        private final ShortBuffer data;
        MXCFBPos() {
            b.order(ByteOrder.nativeOrder());
            data = b.asShortBuffer();
        }
        @Override
        int sizeof() {
            return 4;
        }
        void set(int x, int y) {
            data.put(0, (short) x);
            data.put(1, (short) y);
        }
    }

    MX6Cursor() {
        try {
            SysFS.write("/sys/class/graphics/fb1/blank", "0");
            system = LinuxSystem.getLinuxSystem();
            LinuxSystem.FbVarScreenInfo screen = new LinuxSystem.FbVarScreenInfo();
            fd = system.open("/dev/fb1", LinuxSystem.O_RDWR);
            if (fd == -1) {
                throw new IOException(system.getErrorMessage());
            }
            system.ioctl(fd, LinuxSystem.FBIOGET_VSCREENINFO, screen.p);
            screen.setRes(screen.p, CURSOR_WIDTH, CURSOR_HEIGHT);
            screen.setVirtualRes(screen.p, CURSOR_WIDTH, CURSOR_HEIGHT);
            screen.setOffset(screen.p, 0, 0);
            screen.setActivate(screen.p, 0);
            // set up cursor as 16-bit
            screen.setBitsPerPixel(screen.p, 16);
            screen.setRed(screen.p, 5, 11);
            screen.setGreen(screen.p, 6, 5);
            screen.setBlue(screen.p, 5, 0);
            screen.setTransp(screen.p, 0, 0);
            system.ioctl(fd, LinuxSystem.FBIOPUT_VSCREENINFO, screen.p);
            system.ioctl(fd, LinuxSystem.FBIOBLANK, LinuxSystem.FB_BLANK_UNBLANK);

            MXCFBColorKey key = new MXCFBColorKey();
            key.setEnable(1);
            key.setColorKey(((SHORT_KEY & 0xf800)<<8)
                            | ((SHORT_KEY & 0xe000)<<3)
                            | ((SHORT_KEY & 0x07e0)<<5)
                            | ((SHORT_KEY & 0x0600)>>1)
                            | ((SHORT_KEY & 0x001f)<<3)
                            | ((SHORT_KEY & 0x001c)>>2));
            int MXCFB_SET_CLR_KEY = system.IOW('F', 0x22, key.sizeof());
            if (system.ioctl(fd, MXCFB_SET_CLR_KEY, key.p) < 0) {
                throw new IOException(system.strerror(system.errno()));
            }
        } catch (IOException e) {
            if (fd != -1) {
                LinuxSystem.getLinuxSystem().close(fd);
                fd = -1;
            }
            e.printStackTrace();
            System.err.println("Failed to initialize i.MX6 cursor");
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
        alpha.setEnable(1);
        alpha.setAlpha(visibility ? 255 : 0);
        int MXCFB_SET_GBL_ALPHA = system.IOW('F', 0x21, alpha.sizeof());
        system.ioctl(fd, MXCFB_SET_GBL_ALPHA, alpha.p);
        isVisible = visibility;
        updateImage(true);
    }

    private void updateImage(boolean always) {
        if (isVisible && cursorBuffer != null) { //skip until cursor is fully initialized
            int newOffsetX, newOffsetY;
            newOffsetX = Math.max(0, CURSOR_WIDTH + cursorX - screenWidth);
            newOffsetY = Math.max(0, CURSOR_HEIGHT + cursorY - screenHeight);
            if (newOffsetX != offsetX || newOffsetY != offsetY || always) {
                NativeCursors.offsetCursor(cursorBuffer, offsetCursorBuffer,
                                           newOffsetX, newOffsetY,
                                           CURSOR_WIDTH, CURSOR_HEIGHT,
                                           16, SHORT_KEY);
                offsetX = newOffsetX;
                offsetY = newOffsetY;
                system.lseek(fd, 0, LinuxSystem.SEEK_SET);
                if (system.write(fd, offsetCursorByteBuffer,
                                 0, offsetCursorByteBuffer.capacity()) < 0) {
                    System.err.println("Failed to write to i.MX6 cursor: "
                                       + system.getErrorMessage());
                }
            }
        }
    }

    @Override
    void setImage(byte[] cursorImage) {
        // Convert the cursor to the color-keyed format
        ByteBuffer bb = ByteBuffer.allocate(cursorImage.length);
        cursorBuffer = bb.asShortBuffer();
        NativeCursors.colorKeyCursor(cursorImage, cursorBuffer, 16, SHORT_KEY);
        // Create an offset version of the cursor for rendering
        offsetCursorByteBuffer = ByteBuffer.allocateDirect(cursorImage.length);
        offsetCursorByteBuffer.order(ByteOrder.nativeOrder());
        offsetCursorBuffer = offsetCursorByteBuffer.asShortBuffer();
        updateImage(true);
    }

    @Override
    void setLocation(int x, int y) {
        cursorX = x;
        cursorY = y;
        updateImage(false);
        pos.set(x, y);
        int MXCFB_SET_OVERLAY_POS = system.IOWR('F', 0x24, pos.sizeof());
        system.ioctl(fd, MXCFB_SET_OVERLAY_POS, pos.p);
    }

    @Override
    void setHotSpot(int hotspotX, int hotspotY) {
        this.hotspotX = hotspotX;
        this.hotspotY = hotspotY;
    }

    @Override
    void shutdown() {
        setVisibility(false);
    }
}
