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
import com.sun.javafx.image.BytePixelGetter;
import com.sun.javafx.image.BytePixelSetter;
import com.sun.javafx.image.ByteToBytePixelConverter;
import com.sun.javafx.image.PixelUtils;
import java.nio.ByteBuffer;

abstract class BaseIndexedToByteConverter implements ByteToBytePixelConverter {

    static abstract class IndexedGetter implements BytePixelGetter {
        final int[] preColors;
        final int[] nonPreColors;
        private final AlphaType alphaType;

        IndexedGetter(int[] colors, AlphaType alphaType) {
            int[] preColors = null, nonPreColors = null;

            switch (alphaType) {
                case OPAQUE -> {
                    nonPreColors = colors;
                    preColors = colors;
                }

                case PREMULTIPLIED -> {
                    preColors = colors;
                    nonPreColors = new int[colors.length];

                    for (int i = 0; i < colors.length; i++) {
                        nonPreColors[i] = PixelUtils.PretoNonPre(colors[i]);
                    }
                }

                case NONPREMULTIPLIED -> {
                    nonPreColors = colors;
                    preColors = new int[colors.length];

                    for (int i = 0; i < colors.length; i++) {
                        preColors[i] = PixelUtils.NonPretoPre(colors[i]);
                    }
                }
            }

            this.alphaType = alphaType;
            this.preColors = preColors;
            this.nonPreColors = nonPreColors;
        }

        @Override
        public AlphaType getAlphaType() {
            return alphaType;
        }
    }

    protected final IndexedGetter getter;
    protected final BytePixelSetter setter;
    protected final int nSrcElems;
    protected final int nDstElems;

    BaseIndexedToByteConverter(IndexedGetter getter, BytePixelSetter setter) {
        this.getter = getter;
        this.setter = setter;
        this.nSrcElems = getter.getNumElements();
        this.nDstElems = setter.getNumElements();
    }

    @Override
    public final IndexedGetter getGetter() {
        return getter;
    }

    @Override
    public final BytePixelSetter getSetter() {
        return setter;
    }

    abstract void doConvert(byte[] srcarr, int srcoff, int srcscanbytes,
                            byte[] dstarr, int dstoff, int dstscanbytes,
                            int w, int h);

    abstract void doConvert(ByteBuffer srcbuf, int srcoff, int srcscanbytes,
                            ByteBuffer dstbuf, int dstoff, int dstscanbytes,
                            int w, int h);

    @Override
    public final void convert(byte[] srcarr, int srcoff, int srcscanbytes,
                              byte[] dstarr, int dstoff, int dstscanbytes,
                              int w, int h) {
        if (w <= 0 || h <= 0) return;

        if (srcscanbytes == w * nSrcElems && dstscanbytes == w * nDstElems) {
            w *= h;
            h = 1;
        }

        doConvert(srcarr, srcoff, srcscanbytes,
                  dstarr, dstoff, dstscanbytes,
                  w, h);
    }

    @Override
    public final void convert(ByteBuffer srcbuf, int srcoff, int srcscanbytes,
                              ByteBuffer dstbuf, int dstoff, int dstscanbytes,
                              int w, int h) {
        if (w <= 0 || h <= 0) return;

        if (srcscanbytes == w * nSrcElems && dstscanbytes == w * nDstElems) {
            w *= h;
            h = 1;
        }

        if (srcbuf.hasArray() && dstbuf.hasArray()) {
            srcoff += srcbuf.arrayOffset();
            dstoff += dstbuf.arrayOffset();
            doConvert(srcbuf.array(), srcoff, srcscanbytes,
                      dstbuf.array(), dstoff, dstscanbytes,
                      w, h);
        } else {
            doConvert(srcbuf, srcoff, srcscanbytes,
                      dstbuf, dstoff, dstscanbytes,
                      w, h);
        }
    }

    @Override
    public final void convert(ByteBuffer srcbuf, int srcoff, int srcscanbytes,
                              byte[] dstarr, int dstoff, int dstscanbytes,
                              int w, int h) {
        if (w <= 0 || h <= 0) return;

        if (srcscanbytes == w * nSrcElems && dstscanbytes == w * nDstElems) {
            w *= h;
            h = 1;
        }

        if (srcbuf.hasArray()) {
            byte[] srcarr = srcbuf.array();
            srcoff += srcbuf.arrayOffset();
            doConvert(srcarr, srcoff, srcscanbytes,
                      dstarr, dstoff, dstscanbytes,
                      w, h);
        } else {
            ByteBuffer dstbuf = ByteBuffer.wrap(dstarr);
            doConvert(srcbuf, srcoff, srcscanbytes,
                      dstbuf, dstoff, dstscanbytes,
                      w, h);
        }
    }

    @Override
    public final void convert(byte[] srcarr, int srcoff, int srcscanbytes,
                              ByteBuffer dstbuf, int dstoff, int dstscanbytes,
                              int w, int h) {
        if (w <= 0 || h <= 0) return;

        if (srcscanbytes == w * nSrcElems && dstscanbytes == w * nDstElems) {
            w *= h;
            h = 1;
        }

        if (dstbuf.hasArray()) {
            byte[] dstarr = dstbuf.array();
            dstoff += dstbuf.arrayOffset();
            doConvert(srcarr, srcoff, srcscanbytes,
                      dstarr, dstoff, dstscanbytes,
                      w, h);
        } else {
            ByteBuffer srcbuf = ByteBuffer.wrap(srcarr);
            doConvert(srcbuf, srcoff, srcscanbytes,
                      dstbuf, dstoff, dstscanbytes,
                      w, h);
        }
    }
}
