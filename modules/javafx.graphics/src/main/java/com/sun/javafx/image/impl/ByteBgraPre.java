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

public class ByteBgraPre {
    public static final BytePixelGetter     getter = Accessor.instance;
    public static final BytePixelSetter     setter = Accessor.instance;
    public static final BytePixelAccessor accessor = Accessor.instance;

    public static ByteToBytePixelConverter ToByteBgraConverter() {
        return ByteBgraPre.ToByteBgraConv.instance;
    }

    private static ByteToBytePixelConverter ToByteBgraPreObj;
    public  static ByteToBytePixelConverter ToByteBgraPreConverter() {
        if (ToByteBgraPreObj == null) {
            ToByteBgraPreObj = BaseByteToByteConverter.create(accessor);
        }
        return ToByteBgraPreObj;
    }

    public static ByteToIntPixelConverter ToIntArgbConverter() {
        return ByteBgraPre.ToIntArgbConv.instance;
    }

    public static ByteToIntPixelConverter ToIntArgbPreConverter() {
        return ByteBgra.ToIntArgbSameConv.premul;
    }

    static class Accessor implements BytePixelAccessor {
        static final BytePixelAccessor instance = new Accessor();
        private Accessor() {}

        @Override
        public AlphaType getAlphaType() {
            return AlphaType.PREMULTIPLIED;
        }

        @Override
        public int getNumElements() {
            return 4;
        }

        @Override
        public int getArgb(byte arr[], int offset) {
            return PixelUtils.PretoNonPre(getArgbPre(arr, offset));
        }

        @Override
        public int getArgbPre(byte arr[], int offset) {
            return (((arr[offset    ] & 0xff)      ) |
                    ((arr[offset + 1] & 0xff) <<  8) |
                    ((arr[offset + 2] & 0xff) << 16) |
                    ((arr[offset + 3]       ) << 24));
        }

        @Override
        public int getArgb(ByteBuffer buffer, int offset) {
            return PixelUtils.PretoNonPre(getArgbPre(buffer, offset));
        }

        @Override
        public int getArgbPre(ByteBuffer buffer, int offset) {
            return (((buffer.get(offset    ) & 0xff)      ) |
                    ((buffer.get(offset + 1) & 0xff) <<  8) |
                    ((buffer.get(offset + 2) & 0xff) << 16) |
                    ((buffer.get(offset + 3)       ) << 24));
        }

        @Override
        public void setArgb(byte arr[], int offset, int argb) {
            setArgbPre(arr, offset, PixelUtils.NonPretoPre(argb));
        }

        @Override
        public void setArgbPre(byte arr[], int offset, int argbpre) {
            arr[offset    ] = (byte) (argbpre      );
            arr[offset + 1] = (byte) (argbpre >>  8);
            arr[offset + 2] = (byte) (argbpre >> 16);
            arr[offset + 3] = (byte) (argbpre >> 24);
        }

        @Override
        public void setArgb(ByteBuffer buffer, int offset, int argb) {
            setArgbPre(buffer, offset, PixelUtils.NonPretoPre(argb));
        }

        @Override
        public void setArgbPre(ByteBuffer buffer, int offset, int argbpre) {
            buffer.put(offset    , (byte) (argbpre      ));
            buffer.put(offset + 1, (byte) (argbpre >>  8));
            buffer.put(offset + 2, (byte) (argbpre >> 16));
            buffer.put(offset + 3, (byte) (argbpre >> 24));
        }
    }

    public static class ToByteBgraConv extends BaseByteToByteConverter {
        public static final ByteToBytePixelConverter instance =
            new ToByteBgraConv();

        private ToByteBgraConv() {
            super(ByteBgraPre.getter, ByteBgra.setter);
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
                    byte b = srcarr[srcoff++];
                    byte g = srcarr[srcoff++];
                    byte r = srcarr[srcoff++];
                    int  a = srcarr[srcoff++] & 0xff;
                    if (a > 0 && a < 0xff) {
                        int halfa = a >> 1;
                        b = (byte) (((b & 0xff) * 0xff + halfa) / a);
                        g = (byte) (((g & 0xff) * 0xff + halfa) / a);
                        r = (byte) (((r & 0xff) * 0xff + halfa) / a);
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
                    byte b = srcbuf.get(srcoff    );
                    byte g = srcbuf.get(srcoff + 1);
                    byte r = srcbuf.get(srcoff + 2);
                    int  a = srcbuf.get(srcoff + 3) & 0xff;
                    srcoff += 4;
                    if (a > 0 && a < 0xff) {
                        int halfa = a >> 1;
                        b = (byte) (((b & 0xff) * 0xff + halfa) / a);
                        g = (byte) (((g & 0xff) * 0xff + halfa) / a);
                        r = (byte) (((r & 0xff) * 0xff + halfa) / a);
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

    public static class ToIntArgbConv extends BaseByteToIntConverter {
        public static final ByteToIntPixelConverter instance =
            new ToIntArgbConv();

        private ToIntArgbConv() {
            super(ByteBgraPre.getter, IntArgb.setter);
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
                    if (a > 0 && a < 0xff) {
                        int halfa = a >> 1;
                        r = (r * 0xff + halfa) / a;
                        g = (g * 0xff + halfa) / a;
                        b = (b * 0xff + halfa) / a;
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
                    if (a > 0 && a < 0xff) {
                        int halfa = a >> 1;
                        r = (r * 0xff + halfa) / a;
                        g = (g * 0xff + halfa) / a;
                        b = (b * 0xff + halfa) / a;
                    }
                    dstbuf.put(dstoff + x, (a << 24) | (r << 16) | (g << 8) | b);
                }
                dstoff += dstscanints;
                srcoff += srcscanbytes;
            }
        }
    }
}
