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

import com.sun.prism.Texture.WrapMode;

public class D3D12NativeTexture extends D3D12NativeObject {

    D3D12NativeTexture(long ptr) {
        super(ptr);
    }

    @Override protected native void nReleaseNativeObject(long ptr);
    private native long nGetSize(long ptr);
    private native int nGetWidth(long ptr);
    private native int nGetHeight(long ptr);
    private native boolean nResize(long ptr, int width, int height);
    private native void nSetSamplerParameters(long ptr, int wrapMode, boolean isLinear);

    public long getSize() {
        return nGetSize(ptr);
    }

    public int getWidth() {
        return nGetWidth(ptr);
    }

    public int getHeight() {
        return nGetHeight(ptr);
    }

    public boolean resize(int width, int height) {
        return nResize(ptr, width, height);
    }

    public void setSamplerParameters(WrapMode wrapMode, boolean isLinear) {
        nSetSamplerParameters(ptr, wrapMode.ordinal(), isLinear);
    }
}
