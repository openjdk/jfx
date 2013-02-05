/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.tk.PlatformImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;

public final class StubPlatformImage implements PlatformImage {
    private final StubImageLoader imageLoader;
    private final int frame;

    public StubPlatformImage(final StubImageLoader imageLoader,
                             final int frame) {
        this.imageLoader = imageLoader;
        this.frame = frame;
    }

    public int getFrame() {
        return frame;
    }

    @Override
    public float getPixelScale() {
        return 1.0f;
    }

    public StubImageLoader getImageLoader() {
        return imageLoader;
    }

    public StubPlatformImageInfo getImageInfo() {
        return imageLoader.getImageInfo();
    }

    public Object getSource() {
        return imageLoader.getSource();
    }

    @Override
    public PixelFormat getPlatformPixelFormat() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isWritable() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PlatformImage promoteToWritableImage() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getArgb(int x, int y) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setArgb(int x, int y, int argb) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends Buffer> void getPixels(int x, int y, int w, int h,
                                             WritablePixelFormat<T> pixelformat,
                                             T pixels, int scanlineBytes)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getPixels(int x, int y, int w, int h,
                          WritablePixelFormat<ByteBuffer> pixelformat,
                          byte[] pixels, int offset, int scanlineBytes)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getPixels(int x, int y, int w, int h,
                          WritablePixelFormat<IntBuffer> pixelformat,
                          int[] pixels, int offset, int scanlineInts)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends Buffer> void setPixels(int x, int y, int w, int h,
                                             PixelFormat<T> pixelformat,
                                             T pixels, int scanlineBytes)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setPixels(int x, int y, int w, int h,
                          PixelFormat<ByteBuffer> pixelformat,
                          byte[] pixels, int offset, int scanlineBytes)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setPixels(int x, int y, int w, int h,
                          PixelFormat<IntBuffer> pixelformat,
                          int[] pixels, int offset, int scanlineInts)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setPixels(int dstx, int dsty, int w, int h,
                          PixelReader reader, int srcx, int srcy)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("StubPlatformImage[source = ")
          .append(imageLoader.getSource())
          .append(", width = ").append(imageLoader.getWidth())
          .append(", height = ").append(imageLoader.getHeight())
          .append(", frame = ").append(frame)
          .append("]");

        return sb.toString();
    }
}
