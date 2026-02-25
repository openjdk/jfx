/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.mtl;

import com.sun.prism.Texture;
import com.sun.prism.ps.Shader;

import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

class MTLShader implements Shader {

    private long nMetalShaderRef;
    private final MTLContext context;
    private final String fragmentFunctionName;
    private final Map<Integer, String> samplers = new HashMap<>();
    private final Map<String, Integer> uniformNameIdMap;
    private final Map<Integer, WeakReference<Object>> textureIdRefMap = new HashMap<>();

    private static final Map<String, MTLShader> shaderMap = new HashMap<>();
    private static MTLShader currentEnabledShader;

    private MTLShader(MTLContext context, String fragmentFunctionName) {
        this.fragmentFunctionName = fragmentFunctionName;
        this.context = context;

        nMetalShaderRef = nCreateMetalShader(context.getContextHandle(), fragmentFunctionName);
        if (nMetalShaderRef != 0) {
            shaderMap.put(fragmentFunctionName, this);
        } else {
            throw new InternalError("Failed to create the Shader : " + fragmentFunctionName);
        }
        uniformNameIdMap = nGetUniformNameIdMap(nMetalShaderRef);
    }

    public static Shader createShader(MTLContext ctx, String fragFuncName, Map<String, Integer> samplers,
                                      Map<String, Integer> params, int maxTexCoordIndex,
                                      boolean isPixcoordUsed, boolean isPerVertexColorUsed) {
        if (shaderMap.containsKey(fragFuncName)) {
            return shaderMap.get(fragFuncName);
        } else {
            MTLShader shader = new MTLShader(ctx, fragFuncName);
            shader.storeSamplers(samplers);
            return shader;
        }
    }

    public static MTLShader createShader(MTLContext ctx, String fragFuncName) {
        if (shaderMap.containsKey(fragFuncName)) {
            return shaderMap.get(fragFuncName);
        } else {
            return new MTLShader(ctx, fragFuncName);
        }
    }

    private void storeSamplers(Map<String, Integer> samplers) {
        samplers.forEach((name, id) -> this.samplers.put(id, name));
    }

    @Override
    public void enable() {
        currentEnabledShader = this;
        nEnable(nMetalShaderRef);
    }

    @Override
    public void disable() {
        // There are no disable calls coming from BaseShaderContext.
        // So this is a no-op. We can call disable on lastShader in
        // BaseShaderContext.checkState() but that will be a common change for
        // all pipelines.
        nDisable(nMetalShaderRef);
    }

    @Override
    public boolean isValid() {
        return nMetalShaderRef != 0;
    }

    public static void setTexture(int texUnit, Texture tex, boolean isLinear, int wrapMode) {
        if (currentEnabledShader.textureIdRefMap.get(texUnit) != null &&
            currentEnabledShader.textureIdRefMap.get(texUnit).get() == tex) return;

        currentEnabledShader.textureIdRefMap.put(texUnit, new WeakReference<>(tex));
        MTLTexture<?> mtlTex = (MTLTexture<?>)tex;
        nSetTexture(currentEnabledShader.nMetalShaderRef, texUnit,
                currentEnabledShader.uniformNameIdMap.get(currentEnabledShader.samplers.get(texUnit)),
                mtlTex.getNativeHandle(), isLinear, wrapMode);
    }

    @Override
    public void setConstant(String name, int i0) {
        nSetInt(nMetalShaderRef, uniformNameIdMap.get(name), i0);
    }

    @Override
    public void setConstant(String name, int i0, int i1) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void setConstant(String name, int i0, int i1, int i2) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void setConstant(String name, int i0, int i1, int i2, int i3) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void setConstants(String name, IntBuffer buf, int off, int count) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void setConstant(String name, float f0) {
        nSetFloat1(nMetalShaderRef, uniformNameIdMap.get(name), f0);
    }

    @Override
    public void setConstant(String name, float f0, float f1) {
        nSetFloat2(nMetalShaderRef, uniformNameIdMap.get(name), f0, f1);
    }

    @Override
    public void setConstant(String name, float f0, float f1, float f2) {
        nSetFloat3(nMetalShaderRef, uniformNameIdMap.get(name), f0, f1, f2);
    }

    @Override
    public void setConstant(String name, float f0, float f1, float f2, float f3) {
        nSetFloat4(nMetalShaderRef, uniformNameIdMap.get(name), f0, f1, f2, f3);
    }

    @Override
    public void setConstants(String name, FloatBuffer buf, int off, int count) {
        boolean direct = buf.isDirect();
        if (direct) {
            nSetConstantsBuf(nMetalShaderRef, uniformNameIdMap.get(name),
                                buf, buf.position() * 4, count * 4);
        } else {
            count = 4 * count;
            float[] values = new float[count];
            buf.get(off, values, 0, count);
            nSetConstants(nMetalShaderRef, uniformNameIdMap.get(name), values, count);
        }
    }

    @Override
    public void dispose() {
        if (isValid()) {
            context.disposeShader(nMetalShaderRef);
            shaderMap.remove(fragmentFunctionName);
            nMetalShaderRef = 0;
            textureIdRefMap.clear();
            uniformNameIdMap.clear();
            samplers.clear();
        }
    }

    // Native methods

    private static native long nCreateMetalShader(long context, String fragFuncName);
    private static native Map<String, Integer>  nGetUniformNameIdMap(long nMetalShader);
    private static native void nEnable(long nMetalShader);
    private static native void nDisable(long nMetalShader);

    private static native void nSetTexture(long nMetalShader, int texID, int uniformID,
                                           long texPtr, boolean isLinear, int wrapMode);

    private static native void nSetInt(long nMetalShader, int uniformID, int i0);

    private static native void nSetFloat1(long nMetalShader, int uniformID, float f0);
    private static native void nSetFloat2(long nMetalShader, int uniformID,
                                            float f0, float f1);
    private static native void nSetFloat3(long nMetalShader, int uniformID,
                                            float f0, float f1, float f2);
    private static native void nSetFloat4(long nMetalShader, int uniformID,
                                            float f0, float f1, float f2, float f3);

    private static native void nSetConstants(long nMetalShader, int uniformID,
                                            float[] values, int size);
    private static native void nSetConstantsBuf(long nMetalShader, int uniformID,
                                    Object values, int valuesByteOffset, int size);
}
