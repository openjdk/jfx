/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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

/**
 * A screen of the EGL platform: its nine properties are asked of the vendor library once, at
 * construction, through {@link EglVendorNative}, in the order eglBridge.c of commit 21d5a654f6 served them.
 */
public class EGLScreen implements NativeScreen {

    final int depth;
    final int nativeFormat;
    final int width, height;
    final int offsetX, offsetY;
    final int dpi;
    final long handle;
    final float scale;

    public EGLScreen(int idx) {
        this.handle = EglVendorNative.doGetHandle(idx);
        this.depth = EglVendorNative.doGetDepth(idx);
        this.nativeFormat = EglVendorNative.doGetNativeFormat(idx);
        this.width = EglVendorNative.doGetWidth(idx);
        this.height = EglVendorNative.doGetHeight(idx);
        this.offsetX = EglVendorNative.doGetOffsetX(idx);
        this.offsetY = EglVendorNative.doGetOffsetY(idx);
        this.dpi = EglVendorNative.doGetDpi(idx);
        this.scale = EglVendorNative.doGetScale(idx);
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

}
