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

package com.sun.prism.j2d;

import com.sun.prism.PixelFormat;
import com.sun.prism.impl.BaseResourcePool;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.TextureResourcePool;
import java.awt.image.BufferedImage;

class J2DTexturePool extends BaseResourcePool<BufferedImage>
    implements TextureResourcePool<BufferedImage>
{
    static final J2DTexturePool instance = new J2DTexturePool();

    private static long maxVram() {
        long heapmax = Runtime.getRuntime().maxMemory();
        long setmax = PrismSettings.maxVram;
        return Math.min(heapmax / 4, setmax);
    }

    private static long targetVram() {
        long max = maxVram();
        return Math.min(max / 2, PrismSettings.targetVram);
    }

    private J2DTexturePool() {
        super(null, targetVram(), maxVram());
    }

    @Override
    public long used() {
        Runtime r = Runtime.getRuntime();
        long heapused = r.totalMemory() - r.freeMemory();
        long heapfree = r.maxMemory() - heapused;
//        heapfree = max();
        long managedfree = max() - managed();
        return max() - Math.min(heapfree, managedfree);
    }

    static long size(int w, int h, int type) {
        long size = ((long) w) * ((long) h);
        switch (type) {
            case BufferedImage.TYPE_3BYTE_BGR:
                return size * 3L;
            case BufferedImage.TYPE_BYTE_GRAY:
                return size;
            case BufferedImage.TYPE_INT_ARGB_PRE:
                return size * 4L;
            default:
                throw new InternalError("Unrecognized BufferedImage");
        }
    }

    @Override
    public long size(BufferedImage resource) {
        return size(resource.getWidth(), resource.getHeight(),
                    resource.getType());
    }

    @Override
    public long estimateTextureSize(int width, int height,
                                    PixelFormat format)
    {
        int type;
        switch (format) {
            case BYTE_RGB:
                type = BufferedImage.TYPE_3BYTE_BGR;
                break;
            case BYTE_GRAY:
                type = BufferedImage.TYPE_BYTE_GRAY;
                break;
            case INT_ARGB_PRE:
            case BYTE_BGRA_PRE:
                type = BufferedImage.TYPE_INT_ARGB_PRE;
                break;
            default:
                throw new InternalError("Unrecognized PixelFormat ("+format+")!");
        }
        return size(width, height, type);
    }

    @Override
    public long estimateRTTextureSize(int width, int height,
                                      boolean hasDepth)
    {
        return size(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
    }

    @Override
    public String toString() {
        return "J2D Texture Pool";
    }
}
