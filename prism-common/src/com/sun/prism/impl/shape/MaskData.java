/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl.shape;

import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import java.nio.ByteBuffer;

public class MaskData {

    private ByteBuffer maskBuffer;
    private int originX;
    private int originY;
    private int width;
    private int height;

    public MaskData() {
    }

    public ByteBuffer getMaskBuffer() {
        return maskBuffer;
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginY() {
        return originY;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void uploadToTexture(Texture tex, int dstx, int dsty,
                                boolean skipFlush)
    {
        int scan = width * tex.getPixelFormat().getBytesPerPixelUnit();
        tex.update(maskBuffer, tex.getPixelFormat(),
                   dstx, dsty, 0, 0, width, height,
                   scan, skipFlush);
    }

    public void update(ByteBuffer maskBuffer,
                       int originX, int originY, int width, int height)
    {
        this.maskBuffer = maskBuffer;
        this.originX = originX;
        this.originY = originY;
        this.width = width;
        this.height = height;
    }

    public static MaskData create(byte[] pixels,
                                  int originX, int originY,
                                  int width, int height)
    {
        MaskData maskData = new MaskData();
        maskData.update(ByteBuffer.wrap(pixels), originX, originY, width, height);
        return maskData;
    }
}
