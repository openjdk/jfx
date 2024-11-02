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

package com.sun.javafx.image;

import java.nio.Buffer;

public interface PixelConverter<T extends Buffer, U extends Buffer> {
    /**
     * Copies a rectangular region of data from the source buffer to the
     * destination buffer using the following relationship:
     * <pre>
     * for each xy : 0 <= x,y < w,h {
     *     int srcpos = y * srcscanelems + x * srcelemsperpixel + srcoff;
     *     int dstpos = y * dstscanelems + x * dstelemsperpixel + dstoff;
     *     for each j : 0 <= j < srcelemsperpixel {
     *         load data from srcbuf.get(srcpos + j);
     *     }
     *     convert data to destination pixel format
     *     for each k : 0 <= k < dstelemsperpixel {
     *         store data into dstbuf.put(dstpos + k, pixel data);
     *     }
     * }
     * </pre>
     *
     * @param srcbuf the nio buffer containing the source data
     * @param srcoff the absolute location in the buffer of the first source pixel data
     * @param srcscanelems number of buffer elements between rows of data in the source
     * @param dstbuf the nio buffer containing the destination data
     * @param dstoff the absolute location in the buffer of the first destination pixel data
     * @param dstscanelems number of buffer elements between rows of data in the destination
     * @param w the number of pixels to process across before moving to the next row
     * @param h the number of rows of pixels to process
     */
    public void convert(T srcbuf, int srcoff, int srcscanelems,
                        U dstbuf, int dstoff, int dstscanelems,
                        int w, int h);

    public PixelGetter<T> getGetter();
    public PixelSetter<U> getSetter();
}
