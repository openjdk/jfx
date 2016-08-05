/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.image.impl;

import com.sun.javafx.image.AlphaType;
import com.sun.javafx.image.BytePixelAccessor;
import com.sun.javafx.image.BytePixelGetter;
import com.sun.javafx.image.BytePixelSetter;
import com.sun.javafx.image.PixelUtils;
import java.nio.ByteBuffer;

public class ByteArgb {
    public static final BytePixelGetter     getter = Accessor.instance;
    public static final BytePixelSetter     setter = Accessor.instance;
    public static final BytePixelAccessor accessor = Accessor.instance;

    static class Accessor implements BytePixelAccessor {
        static final BytePixelAccessor instance = new Accessor();
        private Accessor() {}

        @Override
        public AlphaType getAlphaType() {
            return AlphaType.NONPREMULTIPLIED;
        }

        @Override
        public int getNumElements() {
            return 4;
        }

        @Override
        public int getArgb(byte arr[], int offset) {
            return (((arr[offset    ]       ) << 24) |
                    ((arr[offset + 1] & 0xff) << 16) |
                    ((arr[offset + 2] & 0xff) <<  8) |
                    ((arr[offset + 3] & 0xff)      ));
        }

        @Override
        public int getArgbPre(byte arr[], int offset) {
            return PixelUtils.NonPretoPre(getArgb(arr, offset));
        }

        @Override
        public int getArgb(ByteBuffer buf, int offset) {
            return (((buf.get(offset    )       ) << 24) |
                    ((buf.get(offset + 1) & 0xff) << 16) |
                    ((buf.get(offset + 2) & 0xff) <<  8) |
                    ((buf.get(offset + 3) & 0xff)      ));
        }

        @Override
        public int getArgbPre(ByteBuffer buf, int offset) {
            return PixelUtils.NonPretoPre(getArgb(buf, offset));
        }

        @Override
        public void setArgb(byte arr[], int offset, int argb) {
            arr[offset    ] = (byte) (argb >> 24);
            arr[offset + 1] = (byte) (argb >> 16);
            arr[offset + 2] = (byte) (argb >>  8);
            arr[offset + 3] = (byte) (argb      );
        }

        @Override
        public void setArgbPre(byte arr[], int offset, int argbpre) {
            setArgb(arr, offset, PixelUtils.PretoNonPre(argbpre));
        }

        @Override
        public void setArgb(ByteBuffer buf, int offset, int argb) {
            buf.put(offset    , (byte) (argb >> 24));
            buf.put(offset + 1, (byte) (argb >> 16));
            buf.put(offset + 2, (byte) (argb >>  8));
            buf.put(offset + 3, (byte) (argb      ));
        }

        @Override
        public void setArgbPre(ByteBuffer buf, int offset, int argbpre) {
            setArgb(buf, offset, PixelUtils.PretoNonPre(argbpre));
        }
    }
}
