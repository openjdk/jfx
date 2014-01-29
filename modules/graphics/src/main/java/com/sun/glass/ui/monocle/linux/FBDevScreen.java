/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.glass.ui.monocle.Framebuffer;
import com.sun.glass.ui.monocle.NativeScreen;
import com.sun.glass.ui.monocle.AcceleratedScreen;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FBDevScreen implements NativeScreen {

    private int nativeFormat;
    private long nativeHandle;
    private FileChannel fbdev;
    private ByteBuffer mappedFB;
    private boolean isShutdown;
    private int consoleCursorBlink;
    private Framebuffer fb;
    private LinuxFrameBuffer linuxFB;

    public FBDevScreen() {
        try {
            linuxFB = new LinuxFrameBuffer("/dev/fb0");
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
        return linuxFB.getDepth();
    }

    @Override
    public int getNativeFormat() {
        return nativeFormat;
    }

    @Override
    public int getWidth() {
        return linuxFB.getWidth();
    }

    @Override
    public int getHeight() {
        return linuxFB.getHeight();
    }

    @Override
    public long getNativeHandle() {
        return nativeHandle;
    }

    @Override
    public int getDPI() {
        return 96; // no way to read DPI from sysfs and ioctl returns junk values
    }

    private boolean isFBDevOpen() {
        return mappedFB != null || fbdev != null;
    }

    private void openFBDev() throws IOException {
        if (mappedFB == null) {
            Path fbdevPath = FileSystems.getDefault().getPath("/dev/fb0");
            fbdev = FileChannel.open(fbdevPath, StandardOpenOption.WRITE);
        }
    }

    private void closeFBDev() {
        if (mappedFB != null) {
            linuxFB.releaseMappedBuffer(mappedFB);
            mappedFB = null;
        } else if (fbdev != null) {
            try {
                fbdev.close();
            } catch (IOException e) { }
            fbdev = null;
        }
        linuxFB.close();
    }

    private Framebuffer getFramebuffer() {
        // The Framebuffer obect must be created lazily. If we are running with
        // the ES2 pipeline then we won't need the framebuffer until shutdown time.
        if (fb == null) {
            ByteBuffer bb;
            mappedFB = linuxFB.getMappedBuffer();
            if (mappedFB != null) {
                bb = mappedFB;
            } else {
                bb = ByteBuffer.allocateDirect(getWidth() * getHeight() * 4);
            }
            bb.order(ByteOrder.nativeOrder());
            fb = new Framebuffer(bb, getWidth(), getHeight(), getDepth(), true);
            fb.setStartAddress(linuxFB.getNextAddress());
        }
        return fb;
    }

    @Override
    public synchronized void shutdown() {
        getFramebuffer().clearBufferContents();
        try {
            if (isFBDevOpen()) {
                writeBuffer();
                closeFBDev();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
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
    public synchronized void uploadPixels(Buffer b,
                             int pX, int pY, int pWidth, int pHeight,
                             float alpha) {
        getFramebuffer().composePixels(b, pX, pY, pWidth, pHeight, alpha);
    }

    @Override
    public synchronized void swapBuffers() {
        try {
            if (isShutdown || fb == null || !getFramebuffer().hasReceivedData()) {
                return;
            }
            writeBuffer();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            getFramebuffer().reset();
        }
    }

    private synchronized void writeBuffer() throws IOException {
        if (!linuxFB.isDoubleBuffer()) {
            linuxFB.vSync();
        }
        if (mappedFB == null) {
            if (!isFBDevOpen()) {
                openFBDev();
            }
            fbdev.position(linuxFB.getNextAddress());
            getFramebuffer().write(fbdev);
        } else if (linuxFB.isDoubleBuffer()) {
            linuxFB.next();
            linuxFB.vSync();
            getFramebuffer().setStartAddress(linuxFB.getNextAddress());
        }
    }

    @Override
    public synchronized IntBuffer getScreenCapture() {
        getFramebuffer().getBuffer().clear();
        return getFramebuffer().getBuffer().asIntBuffer();
    }

    @Override
    public AcceleratedScreen getAcceleratedScreen(int[] attributes) {
        return null;
    }
}
