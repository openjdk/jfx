/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.pisces;

import com.sun.prism.impl.Disposer;
import java.lang.foreign.MemorySegment;

public abstract class AbstractSurface implements Surface {

    private MemorySegment nativePtr = MemorySegment.NULL;
    private int width;
    private int height;

    AbstractSurface(int width, int height) {
        if (width < 0) {
            throw new IllegalArgumentException("WIDTH must be positive");
        }
        if (height < 0) {
            throw new IllegalArgumentException("HEIGHT must be positive");
        }
        final int nbits = 32-Integer.numberOfLeadingZeros(width) + 32-Integer.numberOfLeadingZeros(height);
        if (nbits > 31) {
            throw new IllegalArgumentException("WIDTH * HEIGHT is too large");
        }
        this.width = width;
        this.height = height;
    }

    /**
     * Creates the native surface descriptor. The subclass constructor calls this once, after it has
     * validated its pixel storage, and then {@link #addDisposerRecord()}.
     */
    final void createNativeSurface(int dataType) {
        this.nativePtr = PiscesNative.surfaceCreate(dataType, width, height);
    }

    /** The native surface descriptor {@link PiscesRenderer} binds a renderer to. */
    final MemorySegment nativeSurface() {
        return nativePtr;
    }

    /**
     * The pixel storage of this surface: {@code width * height} premultiplied ARGB ints, row-major,
     * stride {@code width}, offset 0. The C side never retains it; it is passed to every call that
     * reads or writes pixels. {@link JavaSurface} is the only subclass and returns its {@code int[]}.
     */
    abstract int[] pixels();

    protected void addDisposerRecord() {
        Disposer.addRecord(this, new AbstractSurfaceDisposerRecord(nativePtr));
    }

    @Override
    public final void getRGB(int[] argb, int offset, int scanLength, int x, int y, int width, int height) {
        this.rgbCheck(argb.length, offset, scanLength, x, y, width, height);
        PiscesNative.surfaceGetRGB(nativePtr, pixels(), argb, offset, scanLength, x, y, width, height);
    }

    @Override
    public final void setRGB(int[] argb, int offset, int scanLength, int x, int y, int width, int height) {
        this.rgbCheck(argb.length, offset, scanLength, x, y, width, height);
        PiscesNative.surfaceSetRGB(nativePtr, pixels(), argb, offset, scanLength, x, y, width, height);
    }

    private void rgbCheck(int arr_length, int offset, int scanLength, int x, int y, int width, int height) {
        if (x < 0 || x >= this.width) {
            throw new IllegalArgumentException("X is out of surface");
        }
        if (y < 0 || y >= this.height) {
            throw new IllegalArgumentException("Y is out of surface");
        }
        if (width < 0) {
            throw new IllegalArgumentException("WIDTH must be positive");
        }
        if (height < 0) {
            throw new IllegalArgumentException("HEIGHT must be positive");
        }
        if (x + width > this.width) {
            throw new IllegalArgumentException("X+WIDTH is out of surface");
        }
        if (y + height > this.height) {
            throw new IllegalArgumentException("Y+HEIGHT is out of surface");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("OFFSET must be positive");
        }
        if (scanLength < 0) {
            throw new IllegalArgumentException("SCAN-LENGTH must be positive");
        }
        if (scanLength < width) {
            throw new IllegalArgumentException("SCAN-LENGTH must be >= WIDTH");
        }
        final int nbits = 32-Integer.numberOfLeadingZeros(scanLength) + 32-Integer.numberOfLeadingZeros(height);
        if (nbits > 31) {
            throw new IllegalArgumentException("SCAN-LENGTH * HEIGHT is too large");
        }
        if ((offset + scanLength*(height-1) + width) > arr_length) {
            throw new IllegalArgumentException("STRIDE * HEIGHT exceeds length of data");
        }
    }

    private static class AbstractSurfaceDisposerRecord implements Disposer.Record {
        private MemorySegment nativeHandle;

        AbstractSurfaceDisposerRecord(MemorySegment nh) {
            nativeHandle = nh;
        }

        @Override
        public void dispose() {
            if (nativeHandle != null) {
                PiscesNative.surfaceDispose(nativeHandle);
                nativeHandle = null;
            }
        }
    }

    @Override
    public final int getWidth() {
        return width;
    }

    @Override
    public final int getHeight() {
        return height;
    }
}
