/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.gtk;

import com.sun.glass.ui.Pixels;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

final class GtkPixels extends Pixels {

    public GtkPixels(int width, int height, ByteBuffer data) {
        super(width, height, data);
    }

    public GtkPixels(int width, int height, ByteBuffer data, float scalex, float scaley) {
        super(width, height, data, scalex, scaley);
    }

    public GtkPixels(int width, int height, IntBuffer data) {
        super(width, height, data);
    }

    public GtkPixels(int width, int height, IntBuffer data, float scalex, float scaley) {
        super(width, height, data, scalex, scaley);
    }

    @Override
    protected void _fillDirectByteBuffer(ByteBuffer bb) {
        // Taken from MacPixels
        if (this.bytes != null) {
            this.bytes.rewind();
            if (this.bytes.isDirect()) {
                _copyPixels(bb, this.bytes, getWidth()*getHeight());
            } else {
                bb.put(this.bytes);
            }
            this.bytes.rewind();
        } else {
            this.ints.rewind();
            if (this.ints.isDirect()) {
                _copyPixels(bb, this.ints, getWidth()*getHeight());
            } else {
                for (int i=0; i<this.ints.capacity(); i++) {
                    int data = this.ints.get();
                    bb.put((byte)((data)&0xff));
                    bb.put((byte)((data>>8)&0xff));
                    bb.put((byte)((data>>16)&0xff));
                    bb.put((byte)((data>>24)&0xff));
                }
            }
            this.ints.rewind();
        }
    }

    protected native void _copyPixels(Buffer dst, Buffer src, int size);

    @Override
    protected native void _attachInt(long ptr, int w, int h, IntBuffer ints, int[] array, int offset);

    @Override
    protected native void _attachByte(long ptr, int w, int h, ByteBuffer bytes, byte[] array, int offset);

}
