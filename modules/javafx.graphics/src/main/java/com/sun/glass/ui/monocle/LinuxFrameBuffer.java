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

import java.io.IOException;
import java.nio.ByteBuffer;

class LinuxFrameBuffer {

    private long fd;
    private LinuxSystem system;
    private LinuxSystem.FbVarScreenInfo screenInfo;
    private int width;
    private int height;
    private int bitDepth;
    private int byteDepth;
    private int offsetX, offsetY;
    private int offsetY1, offsetY2;
    private int offsetX1, offsetX2;
    private int state; // 0 = single buffer, 1 = showing buf 1, 2 = showing buf 2
    private int FBIO_WAITFORVSYNC;


    LinuxFrameBuffer(String devNode) throws IOException {
        system = LinuxSystem.getLinuxSystem();
        FBIO_WAITFORVSYNC = system.IOW('F', 0x20, 4);
        fd = system.open(devNode, LinuxSystem.O_RDWR);
        if (fd == -1) {
            throw new IOException(system.getErrorMessage());
        }
        screenInfo = new LinuxSystem.FbVarScreenInfo();
        if (system.ioctl(fd, LinuxSystem.FBIOGET_VSCREENINFO, screenInfo.p) != 0) {
            system.close(fd);
            throw new IOException(system.getErrorMessage());
        }
        bitDepth = screenInfo.getBitsPerPixel(screenInfo.p);
        byteDepth = bitDepth >>> 3;
        width = screenInfo.getXRes(screenInfo.p);
        height = screenInfo.getYRes(screenInfo.p);
        int virtualWidth = screenInfo.getXResVirtual(screenInfo.p);
        int virtualHeight = screenInfo.getYResVirtual(screenInfo.p);
        offsetX = screenInfo.getOffsetX(screenInfo.p);
        offsetY = screenInfo.getOffsetY(screenInfo.p);
        if (virtualHeight >= height * 2) {
            if (offsetY >= height) {
                offsetY1 = offsetY;
                offsetY2 = 0;
            } else if (virtualHeight - offsetY >= height * 2) {
                offsetY1 = offsetY;
                offsetY2 = offsetY + height;
            } else {
                offsetY1 = 0;
                offsetY2 = width * byteDepth;
            }
            offsetX1 = offsetX2 = offsetX;
            state = 1;
        } else if (virtualWidth >= width * 2) {
            if (offsetX >= width) {
                offsetX1 = offsetX;
                offsetX2 = 0;
            } else if (virtualWidth - offsetX >= width * 2) {
                offsetX1 = offsetX;
                offsetX2 = offsetX + height;
            } else {
                offsetX1 = 0;
                offsetX2 = width * byteDepth;
            }
            offsetY1 = offsetY2 = offsetY;
            state = 1;
        }
    }

    boolean canDoubleBuffer() {
        return state != 0;
    }

    int getNativeOffset() {
        int nativeOffsetX = screenInfo.getOffsetX(screenInfo.p);
        int nativeOffsetY = screenInfo.getOffsetY(screenInfo.p);
        if (system.ioctl(fd, LinuxSystem.FBIOGET_VSCREENINFO, screenInfo.p) == 0) {
            nativeOffsetX = screenInfo.getOffsetX(screenInfo.p);
            nativeOffsetY = screenInfo.getOffsetY(screenInfo.p);
        }
        return (nativeOffsetY * width) * byteDepth;
    }

    int getNextAddress() {
        switch (state) {
            case 1:
                return (offsetX2 + offsetY2 * width) * byteDepth;
            case 2:
                return (offsetX1 + offsetY1 * width) * byteDepth;
            default:
                return (offsetX + offsetY * width) * byteDepth;
        }
    }

    void next() throws IOException {
        if (state != 0) {
            int newOffsetX, newOffsetY;
            if (state == 1) {
                newOffsetX = offsetX2;
                newOffsetY = offsetY2;
            } else {
                newOffsetX = offsetX1;
                newOffsetY = offsetY1;
            }
            screenInfo.setActivate(screenInfo.p, LinuxSystem.FB_ACTIVATE_VBL);
            screenInfo.setOffset(screenInfo.p, newOffsetX, newOffsetY);
            if (system.ioctl(fd, LinuxSystem.FBIOPAN_DISPLAY, screenInfo.p) != 0) {
                // turn off double-buffering and don't try to pan again
                state = 0;
                throw new IOException(system.getErrorMessage());
            } else {
                offsetX = newOffsetX;
                offsetY = newOffsetY;
                state = 3 - state;
            }
        }
    }

    void vSync() {
        system.ioctl(fd, FBIO_WAITFORVSYNC, 0);
    }

    ByteBuffer getMappedBuffer() {
        int mappedFBSize = screenInfo.getXResVirtual(screenInfo.p)
                * screenInfo.getYResVirtual(screenInfo.p)
                * byteDepth;
        long addr = system.mmap(0l, mappedFBSize,
                                LinuxSystem.PROT_WRITE,
                                LinuxSystem.MAP_SHARED, fd, 0);
        if (addr != LinuxSystem.MAP_FAILED) {
            return C.getC().NewDirectByteBuffer(addr, mappedFBSize);
        }
        return null;
    }

    void releaseMappedBuffer(ByteBuffer b) {
        system.munmap(C.getC().GetDirectBufferAddress(b), b.capacity());
    }

    void close() {
        system.close(fd);
    }

    boolean isDoubleBuffer() {
        return state > 0;
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    int getDepth() {
        return bitDepth;
    }

}
