/*
 * Copyright (c) 2012, 2023, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.image.IntPixelSetter;
import com.sun.javafx.image.PixelUtils;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class ByteRgb {
    public static final BytePixelGetter getter = Accessor.instance;
    public static final BytePixelSetter setter = Accessor.instance;

    public static ByteToBytePixelConverter ToByteBgraConverter() {
        return ByteRgb.ToByteBgrfConv.nonpremult;
    }

    public static ByteToBytePixelConverter ToByteBgraPreConverter() {
        return ByteRgb.ToByteBgrfConv.premult;
    }

    public static ByteToIntPixelConverter ToIntArgbConverter() {
        return ByteRgb.ToIntFrgbConv.nonpremult;
    }

    public static ByteToIntPixelConverter ToIntArgbPreConverter() {
        return ByteRgb.ToIntFrgbConv.premult;
    }

    public static ByteToBytePixelConverter ToByteArgbConverter() {
        return ByteRgb.ToByteFrgbConv.nonpremult;
    }

    private static ByteToBytePixelConverter rgbToBgrInstance;
    public static ByteToBytePixelConverter ToByteBgrConverter() {
        if (rgbToBgrInstance == null) {
            rgbToBgrInstance = new BaseByteToByteConverter.SwapThreeByteConverter(ByteRgb.getter, ByteBgr.setter);
        }
        return rgbToBgrInstance;
    }

    static class Accessor implements BytePixelAccessor {
        static final BytePixelAccessor instance = new Accessor();
        private Accessor() {}

        @Override
        public AlphaType getAlphaType() {
            return AlphaType.OPAQUE;
        }

        @Override
        public int getNumElements() {
            return 3;
        }

        @Override
        public int getArgb(byte arr[], int offset) {
            return (((arr[offset + 2] & 0xff)      ) |
                    ((arr[offset + 1] & 0xff) <<  8) |
                    ((arr[offset    ] & 0xff) << 16) |
                    ((                  0xff) << 24));
        }

        @Override
        public int getArgbPre(byte arr[], int offset) {
            return (((arr[offset + 2] & 0xff)      ) |
                    ((arr[offset + 1] & 0xff) <<  8) |
                    ((arr[offset    ] & 0xff) << 16) |
                    ((                  0xff) << 24));
        }

        @Override
        public int getArgb(ByteBuffer buffer, int offset) {
            return (((buffer.get(offset + 2) & 0xff)      ) |
                    ((buffer.get(offset + 1) & 0xff) <<  8) |
                    ((buffer.get(offset    ) & 0xff) << 16) |
                    ((                         0xff) << 24));
        }

        @Override
        public int getArgbPre(ByteBuffer buffer, int offset) {
            return (((buffer.get(offset + 2) & 0xff)      ) |
                    ((buffer.get(offset + 1) & 0xff) <<  8) |
                    ((buffer.get(offset    ) & 0xff) << 16) |
                    ((                         0xff) << 24));
        }

        @Override
        public void setArgb(byte[] arr, int offset, int argb) {
            arr[offset    ] = (byte)((argb >> 16) & 0xff);
            arr[offset + 1] = (byte)((argb >> 8) & 0xff);
            arr[offset + 2] = (byte)(argb & 0xff);
        }

        @Override
        public void setArgbPre(byte[] arr, int offset, int argbpre) {
            setArgb(arr, offset, PixelUtils.PretoNonPre(argbpre));
        }

        @Override
        public void setArgb(ByteBuffer buf, int offset, int argb) {
            buf.put(offset    , (byte)((argb >> 16) & 0xff));
            buf.put(offset + 1, (byte)((argb >> 8) & 0xff));
            buf.put(offset + 2, (byte)(argb & 0xff));
        }

        @Override
        public void setArgbPre(ByteBuffer buf, int offset, int argbpre) {
            setArgb(buf, offset, PixelUtils.PretoNonPre(argbpre));
        }
    }

    static class ToByteBgrfConv extends BaseByteToByteConverter {
        public static final ByteToBytePixelConverter nonpremult =
            new ToByteBgrfConv(ByteBgra.setter);
        public static final ByteToBytePixelConverter    premult =
            new ToByteBgrfConv(ByteBgraPre.setter);

        private ToByteBgrfConv(BytePixelSetter setter) {
            super(ByteRgb.getter, setter);
        }

        @Override
        void doConvert(byte srcarr[], int srcoff, int srcscanbytes,
                       byte dstarr[], int dstoff, int dstscanbytes,
                       int w, int h)
        {
            srcscanbytes -= w * 3;
            dstscanbytes -= w * 4;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    dstarr[dstoff++] = srcarr[srcoff + 2];
                    dstarr[dstoff++] = srcarr[srcoff + 1];
                    dstarr[dstoff++] = srcarr[srcoff    ];
                    dstarr[dstoff++] = (byte) 0xff;
                    srcoff += 3;
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
            srcscanbytes -= w * 3;
            dstscanbytes -= w * 4;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    dstbuf.put(dstoff    , srcbuf.get(srcoff + 2));
                    dstbuf.put(dstoff + 1, srcbuf.get(srcoff + 1));
                    dstbuf.put(dstoff + 2, srcbuf.get(srcoff    ));
                    dstbuf.put(dstoff + 3, (byte) 0xff);
                    srcoff += 3;
                    dstoff += 4;
                }
                srcoff += srcscanbytes;
                dstoff += dstscanbytes;
            }
        }
    }

    static class ToIntFrgbConv extends BaseByteToIntConverter {
        public static final ByteToIntPixelConverter nonpremult =
            new ToIntFrgbConv(IntArgb.setter);
        public static final ByteToIntPixelConverter    premult =
            new ToIntFrgbConv(IntArgbPre.setter);

        private ToIntFrgbConv(IntPixelSetter setter) {
            super(ByteRgb.getter, setter);
        }

        @Override
        void doConvert(byte srcarr[], int srcoff, int srcscanbytes,
                       int  dstarr[], int dstoff, int dstscanints,
                       int w, int h)
        {
            srcscanbytes -= w * 3;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int r = srcarr[srcoff++] & 0xff;
                    int g = srcarr[srcoff++] & 0xff;
                    int b = srcarr[srcoff++] & 0xff;
                    dstarr[dstoff + x] = 0xff000000 | (r << 16) | (g << 8) | b;
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
            srcscanbytes -= w * 3;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int r = srcbuf.get(srcoff    ) & 0xff;
                    int g = srcbuf.get(srcoff + 1) & 0xff;
                    int b = srcbuf.get(srcoff + 2) & 0xff;
                    srcoff += 3;
                    dstbuf.put(dstoff + x, 0xff000000 | (r << 16) | (g << 8) | b);
                }
                srcoff += srcscanbytes;
                dstoff += dstscanints;
            }
        }
    }

    static class ToByteFrgbConv extends BaseByteToByteConverter {
        static final ByteToBytePixelConverter nonpremult =
            new ToByteFrgbConv(ByteArgb.setter);

        private ToByteFrgbConv(BytePixelSetter setter) {
            super(ByteRgb.getter, setter);
        }

        @Override
        void doConvert(byte srcarr[], int srcoff, int srcscanbytes,
                       byte dstarr[], int dstoff, int dstscanbytes,
                       int w, int h)
        {
            srcscanbytes -= w * 3;
            srcscanbytes -= w * 4;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    dstarr[dstoff++] = (byte) 0xff;
                    dstarr[dstoff++] = srcarr[srcoff++];
                    dstarr[dstoff++] = srcarr[srcoff++];
                    dstarr[dstoff++] = srcarr[srcoff++];
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
            srcscanbytes -= w * 3;
            srcscanbytes -= w * 4;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    dstbuf.put(dstoff++, (byte) 0xff);
                    dstbuf.put(dstoff++, srcbuf.get(srcoff++));
                    dstbuf.put(dstoff++, srcbuf.get(srcoff++));
                    dstbuf.put(dstoff++, srcbuf.get(srcoff++));
                }
                srcoff += srcscanbytes;
                dstoff += dstscanbytes;
            }
        }
    }
}
