/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.image.ByteToBytePixelConverter;
import com.sun.javafx.image.PixelUtils;
import java.nio.ByteBuffer;

public final class ByteAbgr {

    private ByteAbgr() {}

    public static final BytePixelGetter     getter = Accessor.instance;
    public static final BytePixelSetter     setter = Accessor.instance;
    public static final BytePixelAccessor accessor = Accessor.instance;

    private static ByteToBytePixelConverter ToByteBgraObj;
    public  static ByteToBytePixelConverter ToByteBgraConverter() {
        if (ToByteBgraObj == null) {
            ToByteBgraObj = BaseByteToByteConverter.createReorderer(
                getter, ByteBgra.setter, 1, 2, 3, 0);
        }
        return ToByteBgraObj;
    }

    public static ByteToBytePixelConverter ToByteBgraPreConverter() {
        return ToByteBgraPreConv.instance;
    }

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
        public int getArgb(byte[] arr, int offset) {
            return (((arr[offset    ]       ) << 24) |
                    ((arr[offset + 1] & 0xff)      ) |
                    ((arr[offset + 2] & 0xff) <<  8) |
                    ((arr[offset + 3] & 0xff) << 16));
        }

        @Override
        public int getArgbPre(byte[] arr, int offset) {
            return PixelUtils.NonPretoPre(getArgb(arr, offset));
        }

        @Override
        public int getArgb(ByteBuffer buf, int offset) {
            return (((buf.get(offset    )       ) << 24) |
                    ((buf.get(offset + 1) & 0xff)      ) |
                    ((buf.get(offset + 2) & 0xff) <<  8) |
                    ((buf.get(offset + 3) & 0xff) << 16));
        }

        @Override
        public int getArgbPre(ByteBuffer buf, int offset) {
            return PixelUtils.NonPretoPre(getArgb(buf, offset));
        }

        @Override
        public void setArgb(byte[] arr, int offset, int argb) {
            arr[offset    ] = (byte) (argb >> 24);
            arr[offset + 1] = (byte) (argb      );
            arr[offset + 2] = (byte) (argb >>  8);
            arr[offset + 3] = (byte) (argb >> 16);
        }

        @Override
        public void setArgbPre(byte[] arr, int offset, int argbpre) {
            setArgb(arr, offset, PixelUtils.PretoNonPre(argbpre));
        }

        @Override
        public void setArgb(ByteBuffer buf, int offset, int argb) {
            buf.put(offset    , (byte) (argb >> 24));
            buf.put(offset + 1, (byte) (argb      ));
            buf.put(offset + 2, (byte) (argb >>  8));
            buf.put(offset + 3, (byte) (argb >> 16));
        }

        @Override
        public void setArgbPre(ByteBuffer buf, int offset, int argbpre) {
            setArgb(buf, offset, PixelUtils.PretoNonPre(argbpre));
        }
    }

    static class ToByteBgraPreConv extends BaseByteToByteConverter {
        static final ByteToBytePixelConverter instance =
            new ToByteBgraPreConv();

        private ToByteBgraPreConv() {
            super(ByteAbgr.getter, ByteBgraPre.setter);
        }

        @Override
        void doConvert(byte[] srcarr, int srcoff, int srcscanbytes,
                       byte[] dstarr, int dstoff, int dstscanbytes,
                       int w, int h)
        {
            srcscanbytes -= w * 4;
            dstscanbytes -= w * 4;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int  a = srcarr[srcoff] & 0xff;
                    byte b = srcarr[srcoff + 1];
                    byte g = srcarr[srcoff + 2];
                    byte r = srcarr[srcoff + 3];
                    srcoff += 4;
                    if (a < 0xff) {
                        if (a == 0) {
                            b = g = r = 0;
                        } else {
                            b = (byte) (((b & 0xff) * a + 0x7f) / 0xff);
                            g = (byte) (((g & 0xff) * a + 0x7f) / 0xff);
                            r = (byte) (((r & 0xff) * a + 0x7f) / 0xff);
                        }
                    }
                    dstarr[dstoff++] = b;
                    dstarr[dstoff++] = g;
                    dstarr[dstoff++] = r;
                    dstarr[dstoff++] = (byte) a;
                }
                srcoff += srcscanbytes;
                dstoff += dstscanbytes;
            }
        }

        @Override
        void doConvert(ByteBuffer srcbuf, int srcoff, int srcscanbytes,
                       ByteBuffer dstbuf, int dstoff, int dstscanbytes,
                       int w, int h)
        {
            srcscanbytes -= w * 4;
            dstscanbytes -= w * 4;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int  a = srcbuf.get(srcoff) & 0xff;
                    byte b = srcbuf.get(srcoff + 1);
                    byte g = srcbuf.get(srcoff + 2);
                    byte r = srcbuf.get(srcoff + 3);
                    srcoff += 4;
                    if (a < 0xff) {
                        if (a == 0) {
                            b = g = r = 0;
                        } else {
                            b = (byte) (((b & 0xff) * a + 0x7f) / 0xff);
                            g = (byte) (((g & 0xff) * a + 0x7f) / 0xff);
                            r = (byte) (((r & 0xff) * a + 0x7f) / 0xff);
                        }
                    }
                    dstbuf.put(dstoff    , b);
                    dstbuf.put(dstoff + 1, g);
                    dstbuf.put(dstoff + 2, r);
                    dstbuf.put(dstoff + 3, (byte) a);
                    dstoff += 4;
                }
                srcoff += srcscanbytes;
                dstoff += dstscanbytes;
            }
        }
    }

}
