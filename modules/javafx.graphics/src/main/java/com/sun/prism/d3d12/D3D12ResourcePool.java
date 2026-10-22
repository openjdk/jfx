/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.d3d12;

import com.sun.prism.PixelFormat;
import com.sun.prism.impl.BaseResourcePool;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.TextureResourcePool;

// TODO: D3D12: this probably should be D3D12TextureResourcePool
//
// TODO: D3D12: it seems this is not explicitly disposed? This means
// we have some Textures remaining which don't get properly freed on close
class D3D12ResourcePool extends BaseResourcePool<D3D12TextureData>
    implements TextureResourcePool<D3D12TextureData> {

    public static final D3D12ResourcePool instance = new D3D12ResourcePool();

    private D3D12ResourcePool() {
        super(PrismSettings.targetVram, PrismSettings.maxVram);
    }

    @Override
    public long size(D3D12TextureData resource) {
        return resource.getSize();
    }

    @Override
    public long estimateTextureSize(int width, int height, PixelFormat format) {
        int bpp = format.getBytesPerPixelUnit();
        if (width <= 0 || height <= 0 ||
            height > (Long.MAX_VALUE / bpp) ||
            width > (Long.MAX_VALUE / ((long)height * bpp))) {
            throw new IllegalArgumentException("estimateTextureSize: invalid width or height ");
        }
        return (long) width * height * bpp;
    }

    @Override
    public long estimateRTTextureSize(int width, int height, boolean hasDepth) {
        if (width <= 0 || height <= 0 ||
            height > (Long.MAX_VALUE / 4L) ||
            width  > (Long.MAX_VALUE / (4L * height))) {
            throw new IllegalArgumentException("estimateRTTextureSize: invalid width or height");
        }

        long size = 4L * width * height;
        if (hasDepth) {
            // RTTs in D3D12 use D32_FLOAT depth format
            if ((4L * width * height) > (Long.MAX_VALUE - size)) {
                throw new IllegalArgumentException("estimateRTTextureSize: invalid width or height");
            }
            size += 4L * width * height;
        }
        return size;
    }

    @Override
    public String toString() {
        return "D3D12 Resource Pool";
    }
}
