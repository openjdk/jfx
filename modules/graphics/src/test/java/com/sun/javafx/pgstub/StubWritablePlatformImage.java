/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.pgstub;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import com.sun.javafx.tk.PlatformImage;

/**
 */
public class StubWritablePlatformImage implements PlatformImage {
    private final int w, h;
    private final int[] data;

    public StubWritablePlatformImage(int w, int h) {
        this.w = w;
        this.h = h;
        this.data = new int[w * h];
    }

    @Override
    public float getPixelScale() {
        return 1;
    }

    @Override
    public int getArgb(int x, int y) {
        return data[w * y + x];
    }

    @Override
    public void setArgb(int x, int y, int argb) {
        data[w * y + x] = argb;
    }

    @Override
    public PixelFormat getPlatformPixelFormat() {
        return PixelFormat.getIntArgbInstance();
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public PlatformImage promoteToWritableImage() {
        return this;
    }

    @Override
    public <T extends Buffer> void getPixels(int x, int y, int w, int h, WritablePixelFormat<T> pixelformat, T pixels, int scanlineElems) {
    }

    @Override
    public void getPixels(int x, int y, int w, int h, WritablePixelFormat<ByteBuffer> pixelformat, byte[] pixels, int offset, int scanlineBytes) {
    }

    @Override
    public void getPixels(int x, int y, int w, int h, WritablePixelFormat<IntBuffer> pixelformat, int[] pixels, int offset, int scanlineInts) {
    }

    @Override
    public <T extends Buffer> void setPixels(int x, int y, int w, int h, PixelFormat<T> pixelformat, T pixels, int scanlineBytes) {
    }

    @Override
    public void setPixels(int x, int y, int w, int h, PixelFormat<ByteBuffer> pixelformat, byte[] pixels, int offset, int scanlineBytes) {
    }

    @Override
    public void setPixels(int x, int y, int w, int h, PixelFormat<IntBuffer> pixelformat, int[] pixels, int offset, int scanlineInts) {
    }

    @Override
    public void setPixels(int dstx, int dsty, int w, int h, PixelReader reader, int srcx, int srcy) {
    }
}
