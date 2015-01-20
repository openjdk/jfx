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
import com.sun.javafx.image.IntPixelAccessor;
import com.sun.javafx.image.IntPixelGetter;
import com.sun.javafx.image.IntPixelSetter;
import com.sun.javafx.image.IntToBytePixelConverter;
import com.sun.javafx.image.IntToIntPixelConverter;
import com.sun.javafx.image.PixelUtils;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class IntArgbPre {
    public static final IntPixelGetter     getter = Accessor.instance;
    public static final IntPixelSetter     setter = Accessor.instance;
    public static final IntPixelAccessor accessor = Accessor.instance;

    public static IntToBytePixelConverter ToByteBgraConverter() {
        return IntArgbPre.ToByteBgraConv.instance;
    }

    private static IntToBytePixelConverter ToByteBgraPreObj;
    public  static IntToBytePixelConverter ToByteBgraPreConverter() {
        if (ToByteBgraPreObj == null) {
            ToByteBgraPreObj =
                    new IntTo4ByteSameConverter(IntArgbPre.getter, ByteBgraPre.setter);
        }
        return ToByteBgraPreObj;
    }

    public static IntToIntPixelConverter ToIntArgbConverter() {
        return IntArgbPre.ToIntArgbConv.instance;
    }

    private static IntToIntPixelConverter ToIntArgbPreObj;
    public  static IntToIntPixelConverter ToIntArgbPreConverter() {
        if (ToIntArgbPreObj == null) {
            ToIntArgbPreObj = BaseIntToIntConverter.create(accessor);
        }
        return ToIntArgbPreObj;
    }

    static class Accessor implements IntPixelAccessor {
        static final IntPixelAccessor instance = new Accessor();
        private Accessor() {}

        @Override
        public AlphaType getAlphaType() {
            return AlphaType.PREMULTIPLIED;
        }

        @Override
        public int getNumElements() {
            return 1;
        }

        @Override
        public int getArgb(int arr[], int offset) {
            return PixelUtils.PretoNonPre(arr[offset]);
        }

        @Override
        public int getArgbPre(int arr[], int offset) {
            return arr[offset];
        }

        @Override
        public int getArgb(IntBuffer buffer, int offset) {
            return PixelUtils.PretoNonPre(buffer.get(offset));
        }

        @Override
        public int getArgbPre(IntBuffer buffer, int offset) {
            return buffer.get(offset);
        }

        @Override
        public void setArgb(int arr[], int offset, int argb) {
            arr[offset] = PixelUtils.NonPretoPre(argb);
        }

        @Override
        public void setArgbPre(int arr[], int offset, int argbpre) {
            arr[offset] = argbpre;
        }

        @Override
        public void setArgb(IntBuffer buffer, int offset, int argb) {
            buffer.put(offset, PixelUtils.NonPretoPre(argb));
        }

        @Override
        public void setArgbPre(IntBuffer buffer, int offset, int argbpre) {
            buffer.put(offset, argbpre);
        }
    }

    public static class ToIntArgbConv extends BaseIntToIntConverter {
        public static final IntToIntPixelConverter instance =
            new ToIntArgbConv();

        private ToIntArgbConv() {
            super(IntArgbPre.getter, IntArgb.setter);
        }

        @Override
        void doConvert(int srcarr[], int srcoff, int srcscanints,
                       int dstarr[], int dstoff, int dstscanints,
                       int w, int h)
        {
            srcscanints -= w;
            dstscanints -= w;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int pixel = srcarr[srcoff++];
                    int a = pixel >>> 24;
                    if (a > 0 && a < 0xff) {
                        int halfa = a >> 1;
                        int r = (((pixel >> 16) & 0xff) * 0xff + halfa) / a;
                        int g = (((pixel >>  8) & 0xff) * 0xff + halfa) / a;
                        int b = (((pixel      ) & 0xff) * 0xff + halfa) / a;
                        pixel = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                    dstarr[dstoff++] = pixel;
                }
                srcoff += srcscanints;
                dstoff += dstscanints;
            }
        }

        @Override
        void doConvert(IntBuffer srcbuf, int srcoff, int srcscanints,
                       IntBuffer dstbuf, int dstoff, int dstscanints,
                       int w, int h)
        {
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int pixel = srcbuf.get(srcoff + x);
                    int a = pixel >>> 24;
                    if (a > 0 && a < 0xff) {
                        int halfa = a >> 1;
                        int r = (((pixel >> 16) & 0xff) * 0xff + halfa) / a;
                        int g = (((pixel >>  8) & 0xff) * 0xff + halfa) / a;
                        int b = (((pixel      ) & 0xff) * 0xff + halfa) / a;
                        pixel = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                    dstbuf.put(dstoff + x, pixel);
                }
                srcoff += srcscanints;
                dstoff += dstscanints;
            }
        }
    }

    static class ToByteBgraConv extends BaseIntToByteConverter {
        public static final IntToBytePixelConverter instance =
            new ToByteBgraConv();

        private ToByteBgraConv() {
            super(IntArgbPre.getter, ByteBgra.setter);
        }

        @Override
        void doConvert(int  srcarr[], int srcoff, int srcscanints,
                       byte dstarr[], int dstoff, int dstscanbytes,
                       int w, int h)
        {
            srcscanints -= w;
            dstscanbytes -= w * 4;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int pixel = srcarr[srcoff++];
                    int a = pixel >>> 24;
                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >>  8) & 0xff;
                    int b = (pixel      ) & 0xff;
                    if (a > 0 && a < 0xff) {
                        int halfa = a >> 1;
                        r = (r * 0xff + halfa) / a;
                        g = (g * 0xff + halfa) / a;
                        b = (b * 0xff + halfa) / a;
                    }
                    dstarr[dstoff++] = (byte) b;
                    dstarr[dstoff++] = (byte) g;
                    dstarr[dstoff++] = (byte) r;
                    dstarr[dstoff++] = (byte) a;
                }
                srcoff += srcscanints;
                dstoff += dstscanbytes;
            }
        }

        @Override
        void doConvert(IntBuffer  srcbuf, int srcoff, int srcscanints,
                       ByteBuffer dstbuf, int dstoff, int dstscanbytes,
                       int w, int h)
        {
            dstscanbytes -= w * 4;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int pixel = srcbuf.get(srcoff + x);
                    int a = pixel >>> 24;
                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >>  8) & 0xff;
                    int b = (pixel      ) & 0xff;
                    if (a > 0 && a < 0xff) {
                        int halfa = a >> 1;
                        r = (r * 0xff + halfa) / a;
                        g = (g * 0xff + halfa) / a;
                        b = (b * 0xff + halfa) / a;
                    }
                    dstbuf.put(dstoff    , (byte) b);
                    dstbuf.put(dstoff + 1, (byte) g);
                    dstbuf.put(dstoff + 2, (byte) r);
                    dstbuf.put(dstoff + 3, (byte) a);
                    dstoff += 4;
                }
                srcoff += srcscanints;
                dstoff += dstscanbytes;
            }
        }
    }
}
