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
import com.sun.javafx.image.IntPixelAccessor;
import com.sun.javafx.image.IntPixelGetter;
import com.sun.javafx.image.IntPixelSetter;
import com.sun.javafx.image.IntToIntPixelConverter;
import com.sun.javafx.image.PixelUtils;
import java.nio.IntBuffer;

public class IntRgb {
    private IntRgb() {}

    public static final IntPixelGetter     getter = Accessor.instance;
    public static final IntPixelSetter     setter = Accessor.instance;
    public static final IntPixelAccessor accessor = Accessor.instance;

    public static IntToIntPixelConverter ToIntArgbPreConverter() {
        return IntRgb.ToIntArgbPreConv.instance;
    }

    static class Accessor implements IntPixelAccessor {
        static final IntPixelAccessor instance = new Accessor();
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
        public int getArgb(int[] arr, int offset) {
            return arr[offset] | (0xff << 24);
        }

        @Override
        public int getArgbPre(int[] arr, int offset) {
            return PixelUtils.NonPretoPre(arr[offset] | (0xff << 24));
        }

        @Override
        public int getArgb(IntBuffer buffer, int offset) {
            return buffer.get(offset) | (0xff << 24);
        }

        @Override
        public int getArgbPre(IntBuffer buffer, int offset) {
            return PixelUtils.NonPretoPre(buffer.get(offset) | (0xff << 24));
        }

        @Override
        public void setArgb(int[] arr, int offset, int argb) {
            arr[offset] = argb | (0xff << 24);
        }

        @Override
        public void setArgbPre(int[] arr, int offset, int argbpre) {
            arr[offset] = PixelUtils.PretoNonPre(argbpre) | (0xff << 24);
        }

        @Override
        public void setArgb(IntBuffer buffer, int offset, int argb) {
            buffer.put(offset, argb | (0xff << 24));
        }

        @Override
        public void setArgbPre(IntBuffer buffer, int offset, int argbpre) {
            buffer.put(offset, PixelUtils.PretoNonPre(argbpre) | (0xff << 24));
        }
    }

    public static class ToIntArgbPreConv extends BaseIntToIntConverter {
        public static final IntToIntPixelConverter instance =
            new ToIntArgbPreConv();

        private ToIntArgbPreConv() {
            super(IntRgb.getter, IntArgbPre.setter);
        }

        @Override
        void doConvert(int[] srcarr, int srcoff, int srcscanints,
                       int[] dstarr, int dstoff, int dstscanints,
                       int w, int h)
        {
            srcscanints -= w;
            dstscanints -= w;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int pixel = srcarr[srcoff++];
                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >>  8) & 0xff;
                    int b = (pixel      ) & 0xff;
                    dstarr[dstoff++] = (255 << 24) | (r << 16) | (g << 8) | b;
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
                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >>  8) & 0xff;
                    int b = (pixel      ) & 0xff;
                    dstbuf.put(dstoff + x, (255 << 24) | (r << 16) | (g << 8) | b);
                }
                srcoff += srcscanints;
                dstoff += dstscanints;
            }
        }
    }

}
