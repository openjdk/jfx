/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

public class ByteBgr {
    public static final BytePixelGetter     getter = Accessor.instance;
    public static final BytePixelSetter     setter = Accessor.instance;
    public static final BytePixelAccessor accessor = Accessor.instance;

    private static ByteToBytePixelConverter ToByteBgrObj;
    public  static ByteToBytePixelConverter ToByteBgrConverter() {
        if (ToByteBgrObj == null) {
            ToByteBgrObj = BaseByteToByteConverter.create(accessor);
        }
        return ToByteBgrObj;
    }

    public static ByteToBytePixelConverter ToByteBgraConverter() {
        return ByteBgr.ToByteBgrfConv.nonpremult;
    }

    public static ByteToBytePixelConverter ToByteBgraPreConverter() {
        return ByteBgr.ToByteBgrfConv.premult;
    }

    public static ByteToIntPixelConverter ToIntArgbConverter() {
        return ByteBgr.ToIntFrgbConv.nonpremult;
    }

    public static ByteToIntPixelConverter ToIntArgbPreConverter() {
        return ByteBgr.ToIntFrgbConv.premult;
    }

    public static ByteToBytePixelConverter ToByteArgbConverter() {
        return ByteBgr.ToByteFrgbConv.nonpremult;
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
            return (((arr[offset    ] & 0xff)      ) |
                    ((arr[offset + 1] & 0xff) <<  8) |
                    ((arr[offset + 2] & 0xff) << 16) |
                    ((                  0xff) << 24));
        }

        @Override
        public int getArgbPre(byte arr[], int offset) {
            return (((arr[offset    ] & 0xff)      ) |
                    ((arr[offset + 1] & 0xff) <<  8) |
                    ((arr[offset + 2] & 0xff) << 16) |
                    ((                  0xff) << 24));
        }

        @Override
        public int getArgb(ByteBuffer buffer, int offset) {
            return (((buffer.get(offset    ) & 0xff)      ) |
                    ((buffer.get(offset + 1) & 0xff) <<  8) |
                    ((buffer.get(offset + 2) & 0xff) << 16) |
                    ((                         0xff) << 24));
        }

        @Override
        public int getArgbPre(ByteBuffer buffer, int offset) {
            return (((buffer.get(offset    ) & 0xff)      ) |
                    ((buffer.get(offset + 1) & 0xff) <<  8) |
                    ((buffer.get(offset + 2) & 0xff) << 16) |
                    ((                         0xff) << 24));
        }

        @Override
        public void setArgb(byte[] arr, int offset, int argb) {
            arr[offset    ] = (byte) (argb      );
            arr[offset + 1] = (byte) (argb >>  8);
            arr[offset + 2] = (byte) (argb >> 16);
        }

        @Override
        public void setArgbPre(byte[] arr, int offset, int argbpre) {
            setArgb(arr, offset, PixelUtils.PretoNonPre(argbpre));
        }

        @Override
        public void setArgb(ByteBuffer buf, int offset, int argb) {
            buf.put(offset    , (byte) (argb      ));
            buf.put(offset + 1, (byte) (argb >>  8));
            buf.put(offset + 2, (byte) (argb >> 16));
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
            super(ByteBgr.getter, setter);
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
                    dstarr[dstoff++] = srcarr[srcoff++];
                    dstarr[dstoff++] = srcarr[srcoff++];
                    dstarr[dstoff++] = srcarr[srcoff++];
                    dstarr[dstoff++] = (byte) 0xff;
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
                    dstbuf.put(dstoff++, srcbuf.get(srcoff++));
                    dstbuf.put(dstoff++, srcbuf.get(srcoff++));
                    dstbuf.put(dstoff++, srcbuf.get(srcoff++));
                    dstbuf.put(dstoff++, (byte) 0xff);
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
            super(ByteBgr.getter, setter);
        }

        @Override
        void doConvert(byte srcarr[], int srcoff, int srcscanbytes,
                       int  dstarr[], int dstoff, int dstscanints,
                       int w, int h)
        {
            srcscanbytes -= w * 3;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int b = srcarr[srcoff++] & 0xff;
                    int g = srcarr[srcoff++] & 0xff;
                    int r = srcarr[srcoff++] & 0xff;
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
                    int b = srcbuf.get(srcoff++) & 0xff;
                    int g = srcbuf.get(srcoff++) & 0xff;
                    int r = srcbuf.get(srcoff++) & 0xff;
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
            super(ByteBgr.getter, setter);
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
                    dstarr[dstoff++] = srcarr[srcoff + 2];
                    dstarr[dstoff++] = srcarr[srcoff + 1];
                    dstarr[dstoff++] = srcarr[srcoff    ];
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
            srcscanbytes -= w * 4;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    dstbuf.put(dstoff++, (byte) 0xff);
                    dstbuf.put(dstoff++, srcbuf.get(srcoff + 2));
                    dstbuf.put(dstoff++, srcbuf.get(srcoff + 1));
                    dstbuf.put(dstoff++, srcbuf.get(srcoff    ));
                    srcoff += 3;
                }
                srcoff += srcscanbytes;
                dstoff += dstscanbytes;
            }
        }
    }
}
