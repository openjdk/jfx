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

package com.sun.prism.sw;

import com.sun.prism.PixelFormat;
import com.sun.prism.impl.BaseResourcePool;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.TextureResourcePool;

class SWTexturePool extends BaseResourcePool<SWTexture>
    implements TextureResourcePool<SWTexture>
{
    static final SWTexturePool instance = new SWTexturePool();

    private static long maxVram() {
        long heapmax = Runtime.getRuntime().maxMemory();
        long setmax = PrismSettings.maxVram;
        return Math.min(heapmax / 4, setmax);
    }

    private static long targetVram() {
        long max = maxVram();
        return Math.min(max / 2, PrismSettings.targetVram);
    }

    private SWTexturePool() {
        super(null, targetVram(), maxVram());
    }

    @Override
    public long used() {
//        long heapfree = Runtime.getRuntime().freeMemory();
//        long managedfree = max() - managed();
//        return max() - Math.min(heapfree, managedfree);
        return 0;
    }

    @Override
    public long size(SWTexture resource) {
        long size = resource.getPhysicalWidth();
        size *= resource.getPhysicalHeight();
        if (resource instanceof SWArgbPreTexture) {
            size *= 4L;
        }
        return size;
    }

    @Override
    public long estimateTextureSize(int width, int height, PixelFormat format) {
        switch (format) {
            case BYTE_ALPHA:
                return ((long) width) * ((long) height);
            default:
                return ((long) width) * ((long) height) * 4L;
        }
    }

    @Override
    public long estimateRTTextureSize(int width, int height, boolean hasDepth) {
        return ((long) width) * ((long) height) * 4L;
    }
}
