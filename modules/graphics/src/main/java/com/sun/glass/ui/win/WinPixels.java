/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.win;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Pixels;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * MS Windows platform implementation class for Pixels.
 */
final class WinPixels extends Pixels {

    private native static int _initIDs(); // returns the native format
    static {
        nativeFormat = _initIDs();
    }

    private static final int nativeFormat;

    protected WinPixels(int width, int height, ByteBuffer data) {
        super(width, height, data);
    }

    protected WinPixels(int width, int height, IntBuffer data) {
        super(width, height, data);
    }

    protected WinPixels(int width, int height, IntBuffer data, float scalex, float scaley) {
        super(width, height, data, scalex, scaley);
    }

    static int getNativeFormat_impl() {
        return nativeFormat;
    }

    @Override native protected void _fillDirectByteBuffer(ByteBuffer bb);
    @Override native protected void _attachInt(long ptr, int w, int h, IntBuffer ints, int[] array, int offset);
    @Override native protected void _attachByte(long ptr, int w, int h, ByteBuffer bytes, byte[] array, int offset);
}

