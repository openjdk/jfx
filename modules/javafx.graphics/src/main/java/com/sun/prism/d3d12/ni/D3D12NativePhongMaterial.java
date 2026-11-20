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

package com.sun.prism.d3d12.ni;

import com.sun.prism.PhongMaterial.MapType;

public class D3D12NativePhongMaterial extends D3D12NativeObject {

    D3D12NativePhongMaterial(long ptr) {
        super(ptr);
    }

    @Override protected native void nReleaseNativeObject(long ptr);
    private native void nSetDiffuseColor(long ptr, float r, float g, float b, float a);
    private native void nSetSpecularColor(long ptr, boolean set, float r, float g, float b, float a);
    private native void nSetTextureMap(long ptr, long texturePtr, int mapType);

    public void setDiffuseColor(float r, float g, float b, float a) {
        nSetDiffuseColor(ptr, r, g, b, a);
    }

    public void setSpecularColor(boolean set, float r, float g, float b, float a) {
        nSetSpecularColor(ptr, set, r, g, b, a);
    }

    public void setTextureMap(D3D12NativeTexture texture, MapType type) {
        if (texture == null) {
            nSetTextureMap(ptr, 0, type.ordinal());
        } else {
            nSetTextureMap(ptr, texture.getPtr(), type.ordinal());
        }
    }
}
