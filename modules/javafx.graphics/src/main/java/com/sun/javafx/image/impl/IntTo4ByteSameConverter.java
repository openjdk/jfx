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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import com.sun.javafx.image.BytePixelSetter;
import com.sun.javafx.image.IntPixelGetter;

class IntTo4ByteSameConverter extends BaseIntToByteConverter {

    IntTo4ByteSameConverter(IntPixelGetter getter, BytePixelSetter setter) {
        super(getter, setter);
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
                dstarr[dstoff++] = (byte) (pixel      );
                dstarr[dstoff++] = (byte) (pixel >>  8);
                dstarr[dstoff++] = (byte) (pixel >> 16);
                dstarr[dstoff++] = (byte) (pixel >> 24);
            }
            srcoff += srcscanints;
            dstoff += dstscanbytes;
        }
    }

    @Override
    void doConvert(IntBuffer srcbuf, int srcoff, int srcscanints,
                   ByteBuffer dstbuf, int dstoff, int dstscanbytes,
                   int w, int h)
    {
        dstscanbytes -= w * 4;
        while (--h >= 0) {
            for (int x = 0; x < w; x++) {
                int pixel = srcbuf.get(srcoff + x);
                dstbuf.put(dstoff    , (byte) (pixel      ));
                dstbuf.put(dstoff + 1, (byte) (pixel >>  8));
                dstbuf.put(dstoff + 2, (byte) (pixel >> 16));
                dstbuf.put(dstoff + 3, (byte) (pixel >> 24));
                dstoff += 4;
            }
            srcoff += srcscanints;
            dstoff += dstscanbytes;
        }
    }
}
