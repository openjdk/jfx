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
import com.sun.javafx.image.IntPixelSetter;
import com.sun.javafx.image.PixelUtils;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class ByteGray {
    public static final BytePixelGetter     getter = Accessor.instance;
    public static final BytePixelSetter     setter = Accessor.instance;
    public static final BytePixelAccessor accessor = Accessor.instance;

    private static ByteToBytePixelConverter ToByteGrayObj;
    public  static ByteToBytePixelConverter ToByteGrayConverter() {
        if (ToByteGrayObj == null) {
            ToByteGrayObj = BaseByteToByteConverter.create(accessor);
        }
        return ToByteGrayObj;
    }

    public static ByteToBytePixelConverter ToByteBgraConverter() {
        return ByteGray.ToByteBgrfConv.nonpremult;
    }

    public static ByteToBytePixelConverter ToByteBgraPreConverter() {
        return ByteGray.ToByteBgrfConv.premult;
    }

    public static ByteToIntPixelConverter ToIntArgbConverter() {
        return ByteGray.ToIntFrgbConv.nonpremult;
    }

    public static ByteToIntPixelConverter ToIntArgbPreConverter() {
        return ByteGray.ToIntFrgbConv.premult;
    }

    public static ByteToBytePixelConverter ToByteBgrConverter() {
        return ByteGray.ToByteRgbAnyConv.bgr;
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
            return 1;
        }

        @Override
        public int getArgb(byte arr[], int offset) {
            int g = arr[offset] & 0xff;
            return (0xff000000 | (g << 16) | (g << 8) | g);
        }

        @Override
        public int getArgbPre(byte arr[], int offset) {
            int g = arr[offset] & 0xff;
            return (0xff000000 | (g << 16) | (g << 8) | g);
        }

        @Override
        public int getArgb(ByteBuffer buf, int offset) {
            int g = buf.get(offset) & 0xff;
            return (0xff000000 | (g << 16) | (g << 8) | g);
        }

        @Override
        public int getArgbPre(ByteBuffer buf, int offset) {
            int g = buf.get(offset) & 0xff;
            return (0xff000000 | (g << 16) | (g << 8) | g);
        }

        @Override
        public void setArgb(byte arr[], int offset, int argb) {
            arr[offset] = (byte) PixelUtils.RgbToGray(argb);
        }

        @Override
        public void setArgbPre(byte arr[], int offset, int argbpre) {
            setArgb(arr, offset, PixelUtils.PretoNonPre(argbpre));
        }

        @Override
        public void setArgb(ByteBuffer buf, int offset, int argb) {
            buf.put(offset, (byte) PixelUtils.RgbToGray(argb));
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

        ToByteBgrfConv(BytePixelSetter setter) {
            super(ByteGray.getter, setter);
        }

        @Override
        void doConvert(byte srcarr[], int srcoff, int srcscanbytes,
                       byte dstarr[], int dstoff, int dstscanbytes,
                       int w, int h)
        {
            dstscanbytes -= w * 4;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    byte g = srcarr[srcoff + x];
                    dstarr[dstoff++] = g;
                    dstarr[dstoff++] = g;
                    dstarr[dstoff++] = g;
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
            dstscanbytes -= w * 4;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    byte g = srcbuf.get(srcoff + x);
                    dstbuf.put(dstoff    , g);
                    dstbuf.put(dstoff + 1, g);
                    dstbuf.put(dstoff + 2, g);
                    dstbuf.put(dstoff + 3, (byte) 0xff);
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
            super(ByteGray.getter, setter);
        }

        @Override
        void doConvert(byte srcarr[], int srcoff, int srcscanbytes,
                       int  dstarr[], int dstoff, int dstscanints,
                       int w, int h)
        {
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int g = srcarr[srcoff + x] & 0xff;
                    dstarr[dstoff + x] = 0xff000000 | (g << 16) | (g << 8) | g;
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
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int g = srcbuf.get(srcoff + x) & 0xff;
                    dstbuf.put(dstoff + x, 0xff000000 | (g << 16) | (g << 8) | g);
                }
                srcoff += srcscanbytes;
                dstoff += dstscanints;
            }
        }
    }

    static class ToByteRgbAnyConv extends BaseByteToByteConverter {
        static ToByteRgbAnyConv bgr = new ToByteRgbAnyConv(ByteBgr.setter);

        private ToByteRgbAnyConv(BytePixelSetter setter) {
            super(ByteGray.getter, setter);
        }

        @Override
        void doConvert(byte[] srcarr, int srcoff, int srcscanbytes,
                       byte[] dstarr, int dstoff, int dstscanbytes,
                       int w, int h)
        {
            dstscanbytes -= w * 3;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int g = srcarr[srcoff + x] & 0xff;
                    dstarr[dstoff++] = (byte) g;
                    dstarr[dstoff++] = (byte) g;
                    dstarr[dstoff++] = (byte) g;
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
            dstscanbytes -= w * 3;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int g = srcbuf.get(srcoff + x) & 0xff;
                    dstbuf.put(dstoff++, (byte) g);
                    dstbuf.put(dstoff++, (byte) g);
                    dstbuf.put(dstoff++, (byte) g);
                }
                srcoff += srcscanbytes;
                dstoff += dstscanbytes;
            }
        }
    }
}
