/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.image.ByteToIntPixelConverter;
import com.sun.javafx.image.PixelUtils;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class ByteRgba {
    public static final BytePixelGetter     getter = Accessor.instance;
    public static final BytePixelSetter     setter = Accessor.instance;
    public static final BytePixelAccessor accessor = Accessor.instance;

    private static ByteToBytePixelConverter ToByteRgbaObj;
    public  static ByteToBytePixelConverter ToByteRgbaConverter() {
        if (ToByteRgbaObj == null) {
            ToByteRgbaObj = BaseByteToByteConverter.create(accessor);
        }
        return ToByteRgbaObj;
    }

    private static ByteToBytePixelConverter ToByteBgraObj;
    public  static ByteToBytePixelConverter ToByteBgraConverter() {
        if (ToByteBgraObj == null) {
            ToByteBgraObj =
                    BaseByteToByteConverter.createReorderer(getter, ByteBgra.setter,
                                                            2, 1, 0, 3);
        }
        return ToByteBgraObj;
    }

//    public static ByteToBytePixelConverter ToByteBgraPreConverter() {
//        return ByteRgba.ToByteBgraPreConv.instance;
//    }

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
            return (((arr[offset + 2] & 0xff)      ) |
                    ((arr[offset + 1] & 0xff) <<  8) |
                    ((arr[offset    ] & 0xff) << 16) |
                    ((arr[offset + 3]       ) << 24));
        }

        @Override
        public int getArgbPre(byte arr[], int offset) {
            return PixelUtils.NonPretoPre(getArgb(arr, offset));
        }

        @Override
        public int getArgb(ByteBuffer buf, int offset) {
            return (((buf.get(offset + 2) & 0xff)      ) |
                    ((buf.get(offset + 1) & 0xff) <<  8) |
                    ((buf.get(offset    ) & 0xff) << 16) |
                    ((buf.get(offset + 3)       ) << 24));
        }

        @Override
        public int getArgbPre(ByteBuffer buf, int offset) {
            return PixelUtils.NonPretoPre(getArgb(buf, offset));
        }

        @Override
        public void setArgb(byte arr[], int offset, int argb) {
            arr[offset    ] = (byte) (argb >> 16);
            arr[offset + 1] = (byte) (argb >>  8);
            arr[offset + 2] = (byte) (argb      );
            arr[offset + 3] = (byte) (argb >> 24);
        }

        @Override
        public void setArgbPre(byte arr[], int offset, int argbpre) {
            setArgb(arr, offset, PixelUtils.PretoNonPre(argbpre));
        }

        @Override
        public void setArgb(ByteBuffer buf, int offset, int argb) {
            buf.put(offset    , (byte) (argb >> 16));
            buf.put(offset + 1, (byte) (argb >>  8));
            buf.put(offset + 2, (byte) (argb      ));
            buf.put(offset + 3, (byte) (argb >> 24));
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
            super(ByteRgba.getter, ByteBgraPre.setter);
        }

        @Override
        void doConvert(byte srcarr[], int srcoff, int srcscanbytes,
                       byte dstarr[], int dstoff, int dstscanbytes,
                       int w, int h)
        {
            srcscanbytes -= w * 4;
            dstscanbytes -= w * 4;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    byte r = srcarr[srcoff++];
                    byte g = srcarr[srcoff++];
                    byte b = srcarr[srcoff++];
                    int  a = srcarr[srcoff++] & 0xff;
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
                    byte r = srcbuf.get(srcoff    );
                    byte g = srcbuf.get(srcoff + 1);
                    byte b = srcbuf.get(srcoff + 2);
                    int  a = srcbuf.get(srcoff + 3) & 0xff;
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

    static class ToIntArgbSameConv extends BaseByteToIntConverter {
        static final ByteToIntPixelConverter nonpremul = new ToIntArgbSameConv(false);
        static final ByteToIntPixelConverter    premul = new ToIntArgbSameConv(true);

        private ToIntArgbSameConv(boolean isPremult) {
            super(isPremult ? ByteBgraPre.getter : ByteRgba.getter,
                  isPremult ?  IntArgbPre.setter :  IntArgb.setter);
        }

        @Override
        void doConvert(byte srcarr[], int srcoff, int srcscanbytes,
                       int  dstarr[], int dstoff, int dstscanints,
                       int w, int h)
        {
            srcscanbytes -= w * 4;
            dstscanints -= w;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    dstarr[dstoff++] =
                        ((srcarr[srcoff++] & 0xff)      ) |
                        ((srcarr[srcoff++] & 0xff) <<  8) |
                        ((srcarr[srcoff++] & 0xff) << 16) |
                        ((srcarr[srcoff++]       ) << 24);
                }
                srcoff += srcscanbytes;
                dstoff += dstscanints;
            }
        }

        @Override
        void doConvert(ByteBuffer srcbuf, int srcoff, int srcscanbytes,
                       IntBuffer  dstbuf, int dstoff, int dstscanints,
                       int w, int h)
        {
            srcscanbytes -= w * 4;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    dstbuf.put(dstoff + x,
                               ((srcbuf.get(srcoff    ) & 0xff)      ) |
                               ((srcbuf.get(srcoff + 1) & 0xff) <<  8) |
                               ((srcbuf.get(srcoff + 2) & 0xff) << 16) |
                               ((srcbuf.get(srcoff + 3)       ) << 24));
                    srcoff += 4;
                }
                srcoff += srcscanbytes;
                dstoff += dstscanints;
            }
        }
    }

    static class ToIntArgbPreConv extends BaseByteToIntConverter {
        public static final ByteToIntPixelConverter instance =
            new ToIntArgbPreConv();

        private ToIntArgbPreConv() {
            super(ByteRgba.getter, IntArgbPre.setter);
        }

        @Override
        void doConvert(byte srcarr[], int srcoff, int srcscanbytes,
                       int  dstarr[], int dstoff, int dstscanints,
                       int w, int h)
        {
            srcscanbytes -= w * 4;
            dstscanints  -= w;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int b = srcarr[srcoff++] & 0xff;
                    int g = srcarr[srcoff++] & 0xff;
                    int r = srcarr[srcoff++] & 0xff;
                    int a = srcarr[srcoff++] & 0xff;
                    if (a < 0xff) {
                        if (a == 0) {
                            b = g = r = 0;
                        } else {
                            b = (b * a + 0x7f) / 0xff;
                            g = (g * a + 0x7f) / 0xff;
                            r = (r * a + 0x7f) / 0xff;
                        }
                    }
                    dstarr[dstoff++] =
                        (a << 24) | (r << 16) | (g << 8) | b;
                }
                dstoff += dstscanints;
                srcoff += srcscanbytes;
            }
        }

        @Override
        void doConvert(ByteBuffer srcbuf, int srcoff, int srcscanbytes,
                       IntBuffer  dstbuf, int dstoff, int dstscanints,
                       int w, int h)
        {
            srcscanbytes -= w * 4;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int b = srcbuf.get(srcoff    ) & 0xff;
                    int g = srcbuf.get(srcoff + 1) & 0xff;
                    int r = srcbuf.get(srcoff + 2) & 0xff;
                    int a = srcbuf.get(srcoff + 3) & 0xff;
                    srcoff += 4;
                    if (a < 0xff) {
                        if (a == 0) {
                            b = g = r = 0;
                        } else {
                            b = (b * a + 0x7f) / 0xff;
                            g = (g * a + 0x7f) / 0xff;
                            r = (r * a + 0x7f) / 0xff;
                        }
                    }
                    dstbuf.put(dstoff + x, (a << 24) | (r << 16) | (g << 8) | b);
                }
                dstoff += dstscanints;
                srcoff += srcscanbytes;
            }
        }
    }
}
