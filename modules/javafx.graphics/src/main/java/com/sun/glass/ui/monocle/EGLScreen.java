/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.Buffer;
import java.nio.ByteBuffer;

public class EGLScreen implements NativeScreen {

    final int depth;
    final int nativeFormat;
    final int width, height;
    final int offsetX, offsetY;
    final int dpi;
    final long handle;
    final float scale;

    public EGLScreen(int idx) {
        this.handle = nGetHandle(idx);
        this.depth = nGetDepth(idx);
        this.nativeFormat = nGetNativeFormat(idx);
        this.width = nGetWidth(idx);
        this.height = nGetHeight(idx);
        this.offsetX = nGetOffsetX(idx);
        this.offsetY = nGetOffsetY(idx);
        this.dpi = nGetDpi(idx);
        this.scale = nGetScale(idx);
    }

    @Override
    public int getDepth() {
        return this.depth;
    }

    @Override
    public int getNativeFormat() {
        return this.nativeFormat;
    }

    @Override
    public int getWidth() {
         return this.width;
    }

    @Override
    public int getHeight() {
         return this.height;
    }

    @Override
    public int getOffsetX() {
         return this.offsetX;
    }

    @Override
    public int getOffsetY() {
         return this.offsetY;
    }

    @Override
    public int getDPI() {
         return this.dpi;
    }

    @Override
    public long getNativeHandle() {
        return handle;
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void uploadPixels(Buffer b, int x, int y, int width, int height, float alpha) {
    }

    @Override
    public void swapBuffers() {
    }

    @Override
    public ByteBuffer getScreenCapture() {
        throw new UnsupportedOperationException("No screencapture on EGL platforms");
    }

    @Override
    public float getScale() {
        return this.scale;
    }

    private native long nGetHandle(int idx);
    private native int nGetDepth(int idx);
    private native int nGetWidth(int idx);
    private native int nGetHeight(int idx);
    private native int nGetOffsetX(int idx);
    private native int nGetOffsetY(int idx);
    private native int nGetDpi(int idx);
    private native int nGetNativeFormat(int idx);
    private native float nGetScale(int idx);

}
