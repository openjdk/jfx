/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui;

import java.lang.annotation.Native;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ByteOrder;

/**
 * The object wraps the given raw pixels data.
 *
 * Pixels class is NOT thread safe.
 */
public abstract class Pixels {
    /**
     * The Format specifies the native byte order of the
     * underlying chunk of image data.
     * The data may be either INTs or BYTEs depending on
     * the constructor used.
     * The format ABCD implies the following byte order:
     * BYTE[0] = A
     * BYTE[1] = B
     * BYTE[2] = C
     * BYTE[3] = D
     * BYTE[4] = A
     * ...
     * Calling code should take care of endianness of the platform
     * when passing image data as ints.
     */
    public static class Format {
        @Native public static final int BYTE_BGRA_PRE = 1;
        @Native public static final int BYTE_ARGB = 2;
    }

    public static int getNativeFormat() {
        Application.checkEventThread();
        return Application.GetApplication().staticPixels_getNativeFormat();
    }

    // Need:
    // Clipboard:
    //    public Pixels(final int width, final int height, final byte[] data)
    //
    // Robot:
    //    public Pixels(final int width, final int height, final int[] data)
    //
    // PixelUtils == Prism == GlassToolkit :
    //    public Pixels(final int width, final int height, final ByteBuffer)
    //    public Pixels(final int width, final int height, final IntBuffer)

    // The following fields are safe to be protected, since they are final
    protected final int width;
    protected final int height;
    protected final int bytesPerComponent;

    // The following fields are safe to be protected, since they are final
    protected final ByteBuffer bytes;
    protected final IntBuffer ints;

    private final float scalex;
    private final float scaley;

    protected Pixels(final int width, final int height, final ByteBuffer pixels) {
        this.width = width;
        this.height = height;
        this.bytesPerComponent = 1;
        this.bytes = pixels.slice();
        if ((this.width <= 0) || (this.height <= 0) || ((this.width * this.height * 4) > this.bytes.capacity())) {
            throw new IllegalArgumentException("Too small byte buffer size "+this.width+"x"+this.height+" ["+(this.width*this.height*4)+"] > "+this.bytes.capacity());
        }

        this.ints = null;
        this.scalex = 1.0f;
        this.scaley = 1.0f;
    }

    protected Pixels(final int width, final int height, IntBuffer pixels) {
        this.width = width;
        this.height = height;
        this.bytesPerComponent = 4;
        this.ints = pixels.slice();
        if ((this.width <= 0) || (this.height <= 0) || ((this.width * this.height) > this.ints.capacity())) {
            throw new IllegalArgumentException("Too small int buffer size "+this.width+"x"+this.height+" ["+(this.width*this.height)+"] > "+this.ints.capacity());
        }

        this.bytes = null;
        this.scalex = 1.0f;
        this.scaley = 1.0f;
    }

    protected Pixels(final int width, final int height, IntBuffer pixels, float scalex, float scaley) {
        this.width = width;
        this.height = height;
        this.bytesPerComponent = 4;
        this.ints = pixels.slice();
        if ((this.width <= 0) || (this.height <= 0) || ((this.width * this.height) > this.ints.capacity())) {
            throw new IllegalArgumentException("Too small int buffer size "+this.width+"x"+this.height+" ["+(this.width*this.height)+"] > "+this.ints.capacity());
        }

        this.bytes = null;
        this.scalex = scalex;
        this.scaley = scaley;
    }

    public final float getScaleX() {
        Application.checkEventThread();
        return this.scalex;
    }

    public final float getScaleY() {
        Application.checkEventThread();
        return this.scaley;
    }

    public final float getScaleXUnsafe() {
        return this.scalex;
    }

    public final float getScaleYUnsafe() {
        return this.scaley;
    }

    public final int getWidth() {
        Application.checkEventThread();
        return this.width;
    }

    public final int getWidthUnsafe() {
        return this.width;
    }

    public final int getHeight() {
        Application.checkEventThread();
        return this.height;
    }

    public final int getHeightUnsafe() {
        return this.height;
    }

    public final int getBytesPerComponent() {
        Application.checkEventThread();
        return this.bytesPerComponent;
    }

    /*
     * Return the original pixels buffer.
     */
    public final Buffer getPixels() {
        if (this.bytes != null) {
            this.bytes.rewind();
            return this.bytes;
        } else if (this.ints != null) {
            this.ints.rewind();
            return this.ints;
        } else {
            throw new RuntimeException("Unexpected Pixels state.");
        }
    }

    /*
     * Return a copy of pixels as bytes.
     */
    public final ByteBuffer asByteBuffer() {
        Application.checkEventThread();
        ByteBuffer bb = ByteBuffer.allocateDirect(getWidth()*getHeight()*4);
        bb.order(ByteOrder.nativeOrder());
        bb.rewind();
        asByteBuffer(bb);
        return bb;
    }

    /*
     * Copy pixels into provided byte buffer.
     * The ByteBuffer must be direct.
     */
    public final void asByteBuffer(ByteBuffer bb) {
        Application.checkEventThread();
        if (!bb.isDirect()) {
            throw new RuntimeException("Expected direct buffer.");
        } else if (bb.remaining() < (getWidth()*getHeight()*4)) {
            throw new RuntimeException("Too small buffer.");
        }
        _fillDirectByteBuffer(bb);
    }

    // This method is called from the native code to reduce the number of JNI up-calls.
    private void attachData(long ptr) {
        if (this.ints != null) {
            int[] array = !this.ints.isDirect() ? this.ints.array() : null;
            _attachInt(ptr, this.width, this.height, this.ints, array, array != null ? this.ints.arrayOffset() : 0);
        }
        if (this.bytes != null) {
            byte[] array = !this.bytes.isDirect() ? this.bytes.array() : null;
            _attachByte(ptr, this.width, this.height, this.bytes, array, array != null ? this.bytes.arrayOffset() : 0);
        }
    }

    protected abstract void _fillDirectByteBuffer(ByteBuffer bb);
    protected abstract void _attachInt(long ptr, int w, int h, IntBuffer ints, int[] array, int offset);
    protected abstract void _attachByte(long ptr, int w, int h, ByteBuffer bytes, byte[] array, int offset);

    @Override public final boolean equals(Object object) {
        Application.checkEventThread();
        boolean equals = ((object != null) && (getClass().equals(object.getClass())));
        if (equals) {
            Pixels pixels = (Pixels)object;
            equals = ((getWidth() == pixels.getWidth()) && (getHeight() == pixels.getHeight()));
            if (equals) {
                ByteBuffer b1 = asByteBuffer();
                ByteBuffer b2 = pixels.asByteBuffer();
                equals = (b1.compareTo(b2) == 0);
            }
        }
        return equals;
    }

    @Override public final int hashCode() {
        Application.checkEventThread();
        int val = getWidth();
        val = 31*val + getHeight();
        val = 17*val + asByteBuffer().hashCode();
        return val;
    }
}
