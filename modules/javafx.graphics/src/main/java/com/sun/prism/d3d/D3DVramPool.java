/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.prism.impl.BaseResourcePool;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.TextureResourcePool;

class D3DVramPool extends BaseResourcePool<D3DTextureData>
    implements TextureResourcePool<D3DTextureData>
{
    public static final D3DVramPool instance = new D3DVramPool();

    private D3DVramPool() {
        super(PrismSettings.targetVram, PrismSettings.maxVram);
    }

    @Override
    public long size(D3DTextureData resource) {
        return resource.getSize();
    }

    @Override
    public long estimateTextureSize(int width, int height,
                                    PixelFormat format)
    {
        return (long) width * height * format.getBytesPerPixelUnit();
    }

    @Override
    public long estimateRTTextureSize(int width, int height,
                                      boolean hasDepth)
    {
        // REMIND: need to deal with size of depth buffer, etc.
        return 4L * width * height;
    }

    @Override
    public String toString() {
        return "D3D Vram Pool";
    }
}
