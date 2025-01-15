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
import com.sun.javafx.image.BytePixelSetter;
import com.sun.javafx.image.ByteToBytePixelConverter;
import java.nio.ByteBuffer;

public final class EightBitIndexed {

    private EightBitIndexed() {}

    public static Getter createGetter(int[] colors, AlphaType alphaType) {
        return new Getter(colors, alphaType);
    }

    public static ByteToBytePixelConverter createToByteRgb(Getter src, BytePixelSetter dst) {
        return new ToByteRgbConverter(src, dst);
    }

    public static ByteToBytePixelConverter createToByteBgraAny(Getter src, BytePixelSetter dst) {
        return new ToByteBgraAnyConverter(src, dst);
    }

    public static class Getter extends BaseIndexedToByteConverter.IndexedGetter {
        Getter(int[] colors, AlphaType alphaType) {
            super(colors, alphaType);
        }

        @Override
        public int getNumElements() {
            return 8;
        }

        @Override
        public int getArgb(byte[] arr, int offset) {
            return preColors[arr[offset]];
        }

        @Override
        public int getArgbPre(byte[] arr, int offset) {
            return nonPreColors[arr[offset]];
        }

        @Override
        public int getArgb(ByteBuffer buf, int offset) {
            return preColors[buf.get(offset)];
        }

        @Override
        public int getArgbPre(ByteBuffer buf, int offset) {
            return nonPreColors[buf.get(offset)];
        }
    }

    public static class ToByteBgraAnyConverter extends BaseIndexedToByteConverter {
        public ToByteBgraAnyConverter(Getter getter, BytePixelSetter setter) {
            super(getter, setter);
        }

        @Override
        void doConvert(byte[] srcarr, int srcoff, int srcscanbytes,
                       byte[] dstarr, int dstoff, int dstscanbytes,
                       int w, int h) {
            int[] colors = switch (setter.getAlphaType()) {
                case OPAQUE, NONPREMULTIPLIED -> getGetter().nonPreColors;
                case PREMULTIPLIED -> getGetter().preColors;
            };

            dstscanbytes -= w * 4;

            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int argb = colors[srcarr[srcoff + x]];
                    dstarr[dstoff++] = (byte) argb;
                    dstarr[dstoff++] = (byte) (argb >> 8);
                    dstarr[dstoff++] = (byte) (argb >> 16);
                    dstarr[dstoff++] = (byte) (argb >> 24);
                }

                srcoff += srcscanbytes;
                dstoff += dstscanbytes;
            }
        }

        @Override
        void doConvert(ByteBuffer srcbuf, int srcoff, int srcscanbytes,
                       ByteBuffer dstbuf, int dstoff, int dstscanbytes,
                       int w, int h) {
            int[] colors = switch (setter.getAlphaType()) {
                case OPAQUE, NONPREMULTIPLIED -> getGetter().nonPreColors;
                case PREMULTIPLIED -> getGetter().preColors;
            };

            dstscanbytes -= w * 4;

            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int argb = colors[srcbuf.get(srcoff + x)];
                    dstbuf.put(dstoff, (byte) argb);
                    dstbuf.put(dstoff + 1, (byte) (argb >> 8));
                    dstbuf.put(dstoff + 2, (byte) (argb >> 16));
                    dstbuf.put(dstoff + 3, (byte) (argb >> 24));
                    dstoff += 4;
                }

                srcoff += srcscanbytes;
                dstoff += dstscanbytes;
            }
        }
    }

    public static class ToByteRgbConverter extends BaseIndexedToByteConverter {
        public ToByteRgbConverter(Getter getter, BytePixelSetter setter) {
            super(getter, setter);
        }

        @Override
        void doConvert(byte[] srcarr, int srcoff, int srcscanbytes,
                       byte[] dstarr, int dstoff, int dstscanbytes,
                       int w, int h) {
            int[] colors = getGetter().nonPreColors;
            dstscanbytes -= w * 3;

            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int argb = colors[srcarr[srcoff + x]];
                    dstarr[dstoff++] = (byte) (argb >> 16);
                    dstarr[dstoff++] = (byte) (argb >> 8);
                    dstarr[dstoff++] = (byte) argb;
                }

                srcoff += srcscanbytes;
                dstoff += dstscanbytes;
            }
        }

        @Override
        void doConvert(ByteBuffer srcbuf, int srcoff, int srcscanbytes,
                       ByteBuffer dstbuf, int dstoff, int dstscanbytes,
                       int w, int h) {
            int[] colors = getGetter().nonPreColors;
            dstscanbytes -= w * 3;

            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    int argb = colors[srcbuf.get(srcoff + x)];
                    dstbuf.put(dstoff, (byte) (argb >> 16));
                    dstbuf.put(dstoff + 1, (byte) (argb >> 8));
                    dstbuf.put(dstoff + 2, (byte) argb);
                    dstoff += 3;
                }

                srcoff += srcscanbytes;
                dstoff += dstscanbytes;
            }
        }
    }
}
