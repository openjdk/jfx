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

import com.sun.javafx.image.BytePixelSetter;
import com.sun.javafx.image.IntPixelGetter;
import com.sun.javafx.image.IntToBytePixelConverter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public abstract class BaseIntToByteConverter
    implements IntToBytePixelConverter
{
    protected final IntPixelGetter  getter;
    protected final BytePixelSetter setter;
    protected final int nDstElems;

    BaseIntToByteConverter(IntPixelGetter getter, BytePixelSetter setter) {
        this.getter = getter;
        this.setter = setter;
        this.nDstElems = setter.getNumElements();
    }

    @Override
    public final IntPixelGetter getGetter() {
        return getter;
    }

    @Override
    public final BytePixelSetter getSetter() {
        return setter;
    }

    abstract void doConvert(int  srcarr[], int srcoff, int srcscanints,
                            byte dstarr[], int dstoff, int dstscanbytes,
                            int w, int h);

    abstract void doConvert(IntBuffer  srcbuf, int srcoff, int srcscanints,
                            ByteBuffer dstbuf, int dstoff, int dstscanbytes,
                            int w, int h);

    @Override
    public final void convert(int  srcarr[], int srcoff, int srcscanints,
                              byte dstarr[], int dstoff, int dstscanbytes,
                              int w, int h)
    {
        if (w <= 0 || h <= 0) return;
        if (srcscanints  == w &&
            dstscanbytes == w * nDstElems)
        {
            w *= h;
            h = 1;
        }
        doConvert(srcarr, srcoff, srcscanints,
                  dstarr, dstoff, dstscanbytes,
                  w, h);
    }

    @Override
    public final void convert(IntBuffer  srcbuf, int srcoff, int srcscanints,
                              ByteBuffer dstbuf, int dstoff, int dstscanbytes,
                              int w, int h)
    {
        if (w <= 0 || h <= 0) return;
        if (srcscanints  == w &&
            dstscanbytes == w * nDstElems)
        {
            w *= h;
            h = 1;
        }
        if (srcbuf.hasArray() && dstbuf.hasArray()) {
            srcoff += srcbuf.arrayOffset();
            dstoff += dstbuf.arrayOffset();
            doConvert(srcbuf.array(), srcoff, srcscanints,
                      dstbuf.array(), dstoff, dstscanbytes,
                      w, h);
        } else {
            doConvert(srcbuf, srcoff, srcscanints,
                      dstbuf, dstoff, dstscanbytes,
                      w, h);
        }
    }

    @Override
    public final void convert(IntBuffer srcbuf,   int srcoff, int srcscanints,
                              byte      dstarr[], int dstoff, int dstscanbytes,
                              int w, int h)
    {
        if (w <= 0 || h <= 0) return;
        if (srcscanints  == w &&
            dstscanbytes == w * nDstElems)
        {
            w *= h;
            h = 1;
        }
        if (srcbuf.hasArray()) {
            int srcarr[] = srcbuf.array();
            srcoff += srcbuf.arrayOffset();
            doConvert(srcarr, srcoff, srcscanints,
                      dstarr, dstoff, dstscanbytes,
                      w, h);
        } else {
            ByteBuffer dstbuf = ByteBuffer.wrap(dstarr);
            doConvert(srcbuf, srcoff, srcscanints,
                      dstbuf, dstoff, dstscanbytes,
                      w, h);
        }
    }

    @Override
    public final void convert(int        srcarr[], int srcoff, int srcscanints,
                              ByteBuffer dstbuf,   int dstoff, int dstscanbytes,
                              int w, int h)
    {
        if (w <= 0 || h <= 0) return;
        if (srcscanints  == w &&
            dstscanbytes == w * nDstElems)
        {
            w *= h;
            h = 1;
        }
        if (dstbuf.hasArray()) {
            byte dstarr[] = dstbuf.array();
            dstoff += dstbuf.arrayOffset();
            doConvert(srcarr, srcoff, srcscanints,
                      dstarr, dstoff, dstscanbytes,
                      w, h);
        } else {
            IntBuffer srcbuf = IntBuffer.wrap(srcarr);
            doConvert(srcbuf, srcoff, srcscanints,
                      dstbuf, dstoff, dstscanbytes,
                      w, h);
        }
    }
}
