/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/** Utilities for use by native cursor implementations */
class NativeCursors {

    /** Convert a cursor in 32-bit BYTE_ARGB_PRE format to a 16-bit or 32-bit
     * color-keyed format
     *
     * @param source the cursor pixels
     * @param dest a target ShortBuffer or IntBuffer
     * @param targetDepth the depth of the target format (16 or 32)
     * @param transparentPixel the color key used for transparent pixels
     */
    static void colorKeyCursor(byte[] source,
                                      Buffer dest,
                                      int targetDepth,
                                      int transparentPixel) {
        switch (targetDepth) {
            case 32:
                colorKeyCursor32(source,
                                 (IntBuffer) dest, transparentPixel);
                break;
            case 16:
                colorKeyCursor16(source,
                                 (ShortBuffer) dest, transparentPixel);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    /** Creates a shifted version of the source cursor. Buffers must be
     * ShortBuffers for a bit depth of 16 or IntBuffers for a bit depth of 32.
     *
     * @param sourceBuffer The original cursor to be shifted
     * @param destBuffer A buffer to receive the shifted cursor
     * @param offsetX the number of pixels the cursor is to be shifted right
     * @param offsetY the number of pixels the cursor is to be shifted down
     * @param width the pixel width of the cursor
     * @param height the pixel height of the cursor
     * @param depth the pixel depth of the cursor (16 or 32)
     * @param transparentPixel the color key used for transparent pixels
     */
    static void offsetCursor(Buffer sourceBuffer,
                                    Buffer destBuffer,
                                    int offsetX, int offsetY,
                                    int width, int height,
                                    int depth, int transparentPixel) {
        switch (depth) {
            case 32:
                offsetCursor32((IntBuffer) sourceBuffer,
                               (IntBuffer) destBuffer,
                               offsetX, offsetY,
                               width, height,
                               transparentPixel);
                break;
            case 16:
                offsetCursor16((ShortBuffer) sourceBuffer,
                               (ShortBuffer) destBuffer,
                               offsetX, offsetY,
                               width, height,
                               transparentPixel);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private static void colorKeyCursor32(byte[] source, IntBuffer destBuffer,
                                         int transparentPixel) {
        IntBuffer sourceBuffer = ByteBuffer.wrap(source).asIntBuffer();
        while (sourceBuffer.position() < sourceBuffer.limit()) {
            int i = sourceBuffer.get();
            if ((i & 0xff) == 0) {
                destBuffer.put(transparentPixel);
            } else {
                destBuffer.put(i);
            }
        }
        destBuffer.rewind();
    }

    private static void colorKeyCursor16(byte[] source, ShortBuffer destBuffer,
                                         int transparentPixel) {

        IntBuffer sourceBuffer = ByteBuffer.wrap(source).asIntBuffer();
        while (sourceBuffer.position() < sourceBuffer.limit()) {
            int i = sourceBuffer.get();
            if ((i & 0xff) == 0) {
                destBuffer.put((short) transparentPixel);
            } else {
                int pixel = ((i >> 8) & 0xf800)
                        | ((i >> 5) & 0x7e0)
                        | ( (i >> 3) & 0x1f);
                destBuffer.put((short) pixel);
            }
        }
        destBuffer.rewind();
    }

    private static void offsetCursor32(IntBuffer sourceBuffer,
                                       IntBuffer destBuffer,
                                       int offsetX, int offsetY,
                                       int width, int height,
                                       int transparentPixel) {
        if (offsetX == 0 && offsetY == 0) {
            destBuffer.put(sourceBuffer);
        } else {
            int i;
            for (i = 0; i < offsetY; i++) {
                for (int j = 0; j < width; j++) {
                    destBuffer.put(transparentPixel);
                }
            }
            for (; i < height; i++) {
                int j;
                for (j = 0; j < offsetX; j++) {
                    destBuffer.put(transparentPixel);
                }
                int srcPos = (i - offsetY) * width;
                sourceBuffer.limit(srcPos + width - j);
                sourceBuffer.position(srcPos);
                destBuffer.put(sourceBuffer);
            }
        }
        sourceBuffer.rewind();
        sourceBuffer.limit(sourceBuffer.capacity());
        destBuffer.rewind();
        destBuffer.limit(destBuffer.capacity());
    }

    private static void offsetCursor16(ShortBuffer sourceBuffer,
                                       ShortBuffer destBuffer,
                                       int offsetX, int offsetY,
                                       int width, int height,
                                       int transparentPixel) {
        if (offsetX == 0 && offsetY == 0) {
            destBuffer.put(sourceBuffer);
        } else {
            int i;
            for (i = 0; i < offsetY; i++) {
                for (int j = 0; j < width; j++) {
                    destBuffer.put((short) transparentPixel);
                }
            }
            for (; i < height; i++) {
                int j;
                for (j = 0; j < offsetX; j++) {
                    destBuffer.put((short) transparentPixel);
                }
                int srcPos = (i - offsetY) * width;
                sourceBuffer.limit(srcPos + width - j);
                sourceBuffer.position(srcPos);
                destBuffer.put(sourceBuffer);
            }
        }
        sourceBuffer.rewind();
        sourceBuffer.limit(sourceBuffer.capacity());
        destBuffer.rewind();
        destBuffer.limit(destBuffer.capacity());
    }
}
