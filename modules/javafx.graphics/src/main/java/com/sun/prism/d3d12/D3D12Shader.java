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

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;

import com.sun.prism.d3d12.ni.D3D12NativeShader;
import com.sun.prism.impl.BaseGraphicsResource;
import com.sun.prism.impl.BufferUtil;
import com.sun.prism.ps.Shader;


public class D3D12Shader extends BaseGraphicsResource implements Shader {
    private final IntBuffer mIntBuffer = BufferUtil.newIntBuffer(4);
    private final FloatBuffer mFloatBuffer = BufferUtil.newFloatBuffer(4);
    private final D3D12Context mContext;
    private final D3D12ShaderData mResource;

    D3D12Shader(D3D12ShaderData resource, D3D12Context context) {
        super(resource);
        mContext = context;
        mResource = resource;
    }

    public static D3D12Shader create(D3D12Context context, String name, InputStream shaderCode,
                                     Map<String, Integer> samplers, Map<String, Integer> params) {
        D3D12NativeShader shader = context.getDevice().createShader(name, D3D12Utils.getShaderCodeBuffer(shaderCode));
        return new D3D12Shader(new D3D12ShaderData(shader), context);
    }

    @Override
    public void enable() {
        mContext.getDevice().setPixelShader(mResource.getShader());
    }

    @Override
    public void disable() {
        mContext.getDevice().setPixelShader(null);
    }

    @Override
    public boolean isValid() {
        return mResource.isValid();
    }

    @Override
    public void setConstant(String name, int i0) {
        mIntBuffer.clear();
        mIntBuffer.put(i0);
        setConstants(name, mIntBuffer, 0, 1);
    }

    @Override
    public void setConstant(String name, int i0, int i1) {
        mIntBuffer.clear();
        mIntBuffer.put(i0);
        mIntBuffer.put(i1);
        setConstants(name, mIntBuffer, 0, 1);
    }

    @Override
    public void setConstant(String name, int i0, int i1, int i2) {
        mIntBuffer.clear();
        mIntBuffer.put(i0);
        mIntBuffer.put(i1);
        mIntBuffer.put(i2);
        setConstants(name, mIntBuffer, 0, 1);
    }

    @Override
    public void setConstant(String name, int i0, int i1, int i2, int i3) {
        mIntBuffer.clear();
        mIntBuffer.put(i0);
        mIntBuffer.put(i1);
        mIntBuffer.put(i2);
        mIntBuffer.put(i3);
        setConstants(name, mIntBuffer, 0, 1);
    }

    @Override
    public void setConstant(String name, float f0) {
        mFloatBuffer.clear();
        mFloatBuffer.put(f0);
        setConstants(name, mFloatBuffer, 0, 1);
    }

    @Override
    public void setConstant(String name, float f0, float f1) {
        mFloatBuffer.clear();
        mFloatBuffer.put(f0);
        mFloatBuffer.put(f1);
        setConstants(name, mFloatBuffer, 0, 1);
    }

    @Override
    public void setConstant(String name, float f0, float f1, float f2) {
        mFloatBuffer.clear();
        mFloatBuffer.put(f0);
        mFloatBuffer.put(f1);
        mFloatBuffer.put(f2);
        setConstants(name, mFloatBuffer, 0, 1);
    }

    @Override
    public void setConstant(String name, float f0, float f1, float f2, float f3) {
        mFloatBuffer.clear();
        mFloatBuffer.put(f0);
        mFloatBuffer.put(f1);
        mFloatBuffer.put(f2);
        mFloatBuffer.put(f3);
        setConstants(name, mFloatBuffer, 0, 1);
    }

    @Override
    public void setConstants(String name, IntBuffer buf, int off, int count) {
        // NOTE: count means amount of HLSL int4's (so count == 1 expects buf to have 4 ints)
        // D3D12 native side expects this to be actual element count, so we'll multiply it by 4 here
        mContext.getDevice().setShaderConstants(mResource.getShader(), name, buf, off, count * 4);
    }

    @Override
    public void setConstants(String name, FloatBuffer buf, int off, int count) {
        // NOTE: count means count of HLSL float4's (so count == 1 expects buf to have 4 floats)
        // D3D12 native side expects this to be actual element count, so we'll multiply it by 4 here
        mContext.getDevice().setShaderConstants(mResource.getShader(), name, buf, off, count * 4);
    }

    @Override
    public void dispose() {
        mResource.dispose();
    }
}
