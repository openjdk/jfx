/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.d3d;

import com.sun.prism.PixelFormat;
import com.sun.prism.impl.PrismTrace;

public class D3DTextureData extends D3DResource.D3DRecord {
    private final long size;
    private final boolean isRTT;
    private final int samples;

    static long estimateSize(int physicalWidth, int physicalHeight,
                             PixelFormat format)
    {
        return ((long) physicalWidth) * ((long) physicalHeight) *
               ((long) format.getBytesPerPixelUnit());
    }

    static long estimateRTSize(int physicalWidth, int physicalHeight,
                               boolean hasDepth)
    {
        return ((long) physicalWidth) * ((long) physicalHeight) * 4L;
    }

    D3DTextureData(D3DContext context,
                   long pResource, boolean isRTT,
                   int physicalWidth, int physicalHeight,
                   PixelFormat format, int numberOfSamples)
    {
        super(context, pResource);
        this.size = isRTT
               ? estimateRTSize(physicalWidth, physicalHeight, false)
               : estimateSize(physicalWidth, physicalHeight, format);
        this.isRTT = isRTT;
        this.samples = numberOfSamples;
        if (isRTT) {
            PrismTrace.rttCreated(pResource, physicalWidth, physicalHeight, size);
        } else {
            PrismTrace.textureCreated(pResource, physicalWidth, physicalHeight, size);
        }
    }

    int getSamples() {
        return samples;
    }

    long getSize() {
        return size;
    }

    @Override
    protected void markDisposed() {
        long pResource = getResource();
        if (pResource != 0L) {
            if (isRTT) {
                PrismTrace.rttDisposed(pResource);
            } else {
                PrismTrace.textureDisposed(pResource);
            }
        }
        super.markDisposed();
    }

    @Override
    public void dispose() {
        long pResource = getResource();
        if (pResource != 0L) {
            if (isRTT) {
                PrismTrace.rttDisposed(pResource);
            } else {
                PrismTrace.textureDisposed(pResource);
            }
        }
        super.dispose();
    }
}
