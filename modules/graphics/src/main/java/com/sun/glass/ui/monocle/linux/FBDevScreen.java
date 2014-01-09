/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle.linux;

import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.monocle.NativeScreen;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FBDevScreen implements NativeScreen {

    private int depth;
    private int nativeFormat;
    protected int width;
    protected int height;
    private long nativeHandle;
    private FileChannel fbdev;
    private boolean isShutdown;
    private int consoleCursorBlink;

    public FBDevScreen() {
        try {
            depth = SysFS.readInt("/sys/class/graphics/fb0/bits_per_pixel");
            int[] vsize = SysFS.readInts("/sys/class/graphics/fb0/virtual_size", 2);
            width = vsize[0];
            height = vsize[1];
            nativeHandle = 1l;
            nativeFormat = Pixels.Format.BYTE_BGRA_PRE;
            try {
                consoleCursorBlink = SysFS.readInt(SysFS.CURSOR_BLINK);
                if (consoleCursorBlink != 0) {
                    SysFS.write(SysFS.CURSOR_BLINK, "0");
                }
            } catch (IOException e) {
                // We failed to read or set the cursor blink state. So don't
                // try to restore the previous state on exit.
                consoleCursorBlink = 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw (IllegalStateException)
                    new IllegalStateException().initCause(e);
        }
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public int getNativeFormat() {
        return nativeFormat;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public long getNativeHandle() {
        return nativeHandle;
    }

    @Override
    public int getDPI() {
        return 96; // no way to read DPI from sysfs and ioctl returns junk values
    }

    private void openFBDev() throws IOException {
        Path fbdevPath = FileSystems.getDefault().getPath("/dev/fb0");
        fbdev = FileChannel.open(fbdevPath, StandardOpenOption.WRITE);
    }

    private void clearFBDev() {
        ByteBuffer b = ByteBuffer.allocate(width * depth >> 3);
        try {
            for (int i = 0; i < height; i++) {
                b.position(0);
                b.limit(b.capacity());
                fbdev.position(i * width * depth >> 3);
                fbdev.write(b);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void shutdown() {
        try {
            if (fbdev == null) {
                openFBDev();
            }
            if (fbdev != null) {
                clearFBDev();
                fbdev.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fbdev = null;
            isShutdown = true;
        }
        if (consoleCursorBlink != 0) {
            try {
                SysFS.write(SysFS.CURSOR_BLINK, String.valueOf(consoleCursorBlink));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public synchronized void uploadPixels(ByteBuffer b,
                             int pX, int pY, int pWidth, int pHeight) {
        if (isShutdown) {
            return;
        }
        // TODO: Handle 16-bit screens and window composition
        try {
            if (fbdev == null) {
                openFBDev();
                clearFBDev();
            }
            if (width == pWidth) {
                b.limit(pWidth * 4 * pHeight);
                fbdev.position((pY * width + pX) * (depth >> 3));
                fbdev.write(b);
            } else {
                for (int i = 0; i < pHeight; i++) {
                    int position = i * pWidth * (depth >> 3);
                    b.position(position);
                    b.limit(position + pWidth * 4);
                    fbdev.position(((i + pY) * width + pX) * (depth >> 3));
                    fbdev.write(b);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void swapBuffers() {
        // TODO: We could double-buffer here if the virtual screen size is at
        // least twice the visible size
    }

}
