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

public class D3D12NativeMeshView extends D3D12NativeObject {

    D3D12NativeMeshView(long ptr) {
        super(ptr);
    }

    @Override protected native void nReleaseNativeObject(long ptr);
    private native void nSetCullingMode(long ptr, int mode);
    private native void nSetMaterial(long ptr, long phongMaterialPtr);
    private native void nSetWireframe(long ptr, boolean wireframe);
    private native void nSetAmbientLight(long ptr, float r, float g, float b);
    private native void nSetLight(long ptr, int index, float x, float y, float z, float r, float g, float b,
                                  float enabled, float ca, float la, float qa, float isAttenuated, float maxRange,
                                  float dirX, float dirY, float dirZ, float innerAngle, float outerAngle, float falloff);
    private native void nRender(long ptr);

    public void setCullingMode(int mode)
    {
        nSetCullingMode(ptr, mode);
    }

    public void setMaterial(D3D12NativePhongMaterial material)
    {
        nSetMaterial(ptr, material.getPtr());
    }

    public void setWireframe(boolean wireframe)
    {
        nSetWireframe(ptr, wireframe);
    }

    public void setAmbientLight(float r, float g, float b)
    {
        nSetAmbientLight(ptr, r, g, b);
    }

    public void setLight(int index, float x, float y, float z, float r, float g, float b,
                         float enabled, float ca, float la, float qa, float isAttenuated, float maxRange,
                         float dirX, float dirY, float dirZ, float innerAngle, float outerAngle, float falloff)
    {
        nSetLight(ptr, index, x, y, z, r, g, b, enabled, ca, la, qa, isAttenuated, maxRange,
                  dirX, dirY, dirZ, innerAngle, outerAngle, falloff);
    }
}
