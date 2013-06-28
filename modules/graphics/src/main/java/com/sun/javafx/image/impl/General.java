/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.image.ByteToIntPixelConverter;
import com.sun.javafx.image.IntPixelGetter;
import com.sun.javafx.image.IntPixelSetter;
import com.sun.javafx.image.IntToBytePixelConverter;
import com.sun.javafx.image.IntToIntPixelConverter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class General {
    public static ByteToBytePixelConverter create(BytePixelGetter src,
                                                  BytePixelSetter dst)
    {
        return new ByteToByteGeneralConverter(src, dst);
    }

    public static ByteToIntPixelConverter create(BytePixelGetter src,
                                                 IntPixelSetter  dst)
    {
        return new ByteToIntGeneralConverter(src, dst);
    }

    public static IntToBytePixelConverter create(IntPixelGetter  src,
                                                 BytePixelSetter dst)
    {
        return new IntToByteGeneralConverter(src, dst);
    }

    public static IntToIntPixelConverter create(IntPixelGetter src,
                                                IntPixelSetter dst)
    {
        return new IntToIntGeneralConverter(src, dst);
    }

    static class ByteToByteGeneralConverter extends BaseByteToByteConverter {
        boolean usePremult;

        ByteToByteGeneralConverter(BytePixelGetter getter, BytePixelSetter setter) {
            super(getter, setter);
            usePremult = (getter.getAlphaType() != AlphaType.NONPREMULTIPLIED &&
                          setter.getAlphaType() != AlphaType.NONPREMULTIPLIED);
        }

        @Override
        void doConvert(byte srcarr[], int srcoff, int srcscanbytes,
                       byte dstarr[], int dstoff, int dstscanbytes,
                       int w, int h)
        {
            srcscanbytes -= nSrcElems * w;
            dstscanbytes -= nDstElems * w;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    if (usePremult) {
                        setter.setArgbPre(dstarr, dstoff, getter.getArgbPre(srcarr, srcoff));
                    } else {
                        setter.setArgb(dstarr, dstoff, getter.getArgb(srcarr, srcoff));
                    }
                    srcoff += nSrcElems;
                    dstoff += nDstElems;
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
            srcscanbytes -= nSrcElems * w;
            dstscanbytes -= nDstElems * w;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    if (usePremult) {
                        setter.setArgbPre(dstbuf, dstoff, getter.getArgbPre(srcbuf, srcoff));
                    } else {
                        setter.setArgb(dstbuf, dstoff, getter.getArgb(srcbuf, srcoff));
                    }
                    srcoff += nSrcElems;
                    dstoff += nDstElems;
                }
                srcoff += srcscanbytes;
                dstoff += dstscanbytes;
            }
        }
    }

    static class ByteToIntGeneralConverter extends BaseByteToIntConverter {
        boolean usePremult;

        ByteToIntGeneralConverter(BytePixelGetter getter, IntPixelSetter setter) {
            super(getter, setter);
            usePremult = (getter.getAlphaType() != AlphaType.NONPREMULTIPLIED &&
                          setter.getAlphaType() != AlphaType.NONPREMULTIPLIED);
        }

        @Override
        void doConvert(byte srcarr[], int srcoff, int srcscanbytes,
                       int  dstarr[], int dstoff, int dstscanbytes,
                       int w, int h)
        {
            srcscanbytes -= nSrcElems * w;
            dstscanbytes -= w;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    if (usePremult) {
                        setter.setArgbPre(dstarr, dstoff, getter.getArgbPre(srcarr, srcoff));
                    } else {
                        setter.setArgb(dstarr, dstoff, getter.getArgb(srcarr, srcoff));
                    }
                    srcoff += nSrcElems;
                    dstoff++;
                }
                srcoff += srcscanbytes;
                dstoff += dstscanbytes;
            }
        }

        @Override
        void doConvert(ByteBuffer srcbuf, int srcoff, int srcscanbytes,
                       IntBuffer  dstbuf, int dstoff, int dstscanbytes,
                       int w, int h)
        {
            srcscanbytes -= nSrcElems * w;
            dstscanbytes -= w;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    if (usePremult) {
                        setter.setArgbPre(dstbuf, dstoff, getter.getArgbPre(srcbuf, srcoff));
                    } else {
                        setter.setArgb(dstbuf, dstoff, getter.getArgb(srcbuf, srcoff));
                    }
                    srcoff += nSrcElems;
                    dstoff++;
                }
                srcoff += srcscanbytes;
                dstoff += dstscanbytes;
            }
        }
    }
    static class IntToByteGeneralConverter extends BaseIntToByteConverter {
        boolean usePremult;

        public IntToByteGeneralConverter(IntPixelGetter getter, BytePixelSetter setter) {
            super(getter, setter);
            usePremult = (getter.getAlphaType() != AlphaType.NONPREMULTIPLIED &&
                          setter.getAlphaType() != AlphaType.NONPREMULTIPLIED);
        }

        @Override
        void doConvert(int  srcarr[], int srcoff, int srcscanbytes,
                       byte dstarr[], int dstoff, int dstscanbytes,
                       int w, int h)
        {
            srcscanbytes -= w;
            dstscanbytes -= nDstElems * w;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    if (usePremult) {
                        setter.setArgbPre(dstarr, dstoff, getter.getArgbPre(srcarr, srcoff));
                    } else {
                        setter.setArgb(dstarr, dstoff, getter.getArgb(srcarr, srcoff));
                    }
                    srcoff++;
                    dstoff += nDstElems;
                }
                srcoff += srcscanbytes;
                dstoff += dstscanbytes;
            }
        }

        @Override
        void doConvert(IntBuffer  srcbuf, int srcoff, int srcscanbytes,
                       ByteBuffer dstbuf, int dstoff, int dstscanbytes,
                       int w, int h)
        {
            srcscanbytes -= w;
            dstscanbytes -= nDstElems * w;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    if (usePremult) {
                        setter.setArgbPre(dstbuf, dstoff, getter.getArgbPre(srcbuf, srcoff));
                    } else {
                        setter.setArgb(dstbuf, dstoff, getter.getArgb(srcbuf, srcoff));
                    }
                    srcoff++;
                    dstoff += nDstElems;
                }
                srcoff += srcscanbytes;
                dstoff += dstscanbytes;
            }
        }
    }
    static class IntToIntGeneralConverter extends BaseIntToIntConverter {
        boolean usePremult;

        public IntToIntGeneralConverter(IntPixelGetter getter, IntPixelSetter setter) {
            super(getter, setter);
            usePremult = (getter.getAlphaType() != AlphaType.NONPREMULTIPLIED &&
                          setter.getAlphaType() != AlphaType.NONPREMULTIPLIED);
        }

        @Override
        void doConvert(int srcarr[], int srcoff, int srcscanbytes,
                       int dstarr[], int dstoff, int dstscanbytes,
                       int w, int h)
        {
            srcscanbytes -= w;
            dstscanbytes -= w;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    if (usePremult) {
                        setter.setArgbPre(dstarr, dstoff, getter.getArgbPre(srcarr, srcoff));
                    } else {
                        setter.setArgb(dstarr, dstoff, getter.getArgb(srcarr, srcoff));
                    }
                    srcoff++;
                    dstoff++;
                }
                srcoff += srcscanbytes;
                dstoff += dstscanbytes;
            }
        }

        @Override
        void doConvert(IntBuffer srcbuf, int srcoff, int srcscanbytes,
                       IntBuffer dstbuf, int dstoff, int dstscanbytes,
                       int w, int h)
        {
            srcscanbytes -= w;
            dstscanbytes -= w;
            while (--h >= 0) {
                for (int x = 0; x < w; x++) {
                    if (usePremult) {
                        setter.setArgbPre(dstbuf, dstoff, getter.getArgbPre(srcbuf, srcoff));
                    } else {
                        setter.setArgb(dstbuf, dstoff, getter.getArgb(srcbuf, srcoff));
                    }
                    srcoff++;
                    dstoff++;
                }
                srcoff += srcscanbytes;
                dstoff += dstscanbytes;
            }
        }
    }
}
