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
import java.lang.reflect.Method;
import java.util.Map;

import com.sun.glass.ui.Screen;
import com.sun.prism.MediaFrame;
import com.sun.prism.Mesh;
import com.sun.prism.MeshView;
import com.sun.prism.MultiTexture;
import com.sun.prism.PhongMaterial;
import com.sun.prism.PixelFormat;
import com.sun.prism.Presentable;
import com.sun.prism.PresentableState;
import com.sun.prism.RTTexture;
import com.sun.prism.Texture;
import com.sun.prism.Texture.Usage;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.TextureResourcePool;
import com.sun.prism.impl.ps.BaseShaderFactory;
import com.sun.prism.ps.Shader;
import com.sun.prism.ps.ShaderFactory;
import com.sun.prism.d3d12.ni.D3D12NativeInstance;
import com.sun.prism.d3d12.ni.D3D12NativeTexture;


class D3D12ResourceFactory extends BaseShaderFactory {
    private final D3D12NativeInstance mInstance;
    private final D3D12Context mContext;
    private int mMaximumTextureSize;

    D3D12ResourceFactory(D3D12NativeInstance instance, int adapterOrdinal, Screen screen) {
        super();
        mInstance = instance;
        mContext = new D3D12Context(mInstance.createDevice(adapterOrdinal), screen, this);

        mMaximumTextureSize = mContext.getDevice().getMaximumTextureSize();
        if (mMaximumTextureSize > PrismSettings.maxTextureSize) {
            mMaximumTextureSize = PrismSettings.maxTextureSize;
            if (PrismSettings.verbose) {
                System.err.println("Maximum texture size clamped to " + mMaximumTextureSize);
            }
        }
    }

    D3D12Context getContext() {
        return mContext;
    }

    D3D12NativeInstance getNativeInstance() {
        return mInstance;
    }

    @Override
    public boolean isDeviceReady() {
        return mContext.getDevice().isDeviceReady() && super.isDeviceReady();
    }

    @Override
    public Shader createShader(String pixelShaderName, InputStream pixelShaderCode, Map<String, Integer> samplers, Map<String, Integer> params,
                               int maxTexCoordIndex, boolean isPixcoordUsed, boolean isPerVertexColorUsed) {
        if (checkDisposed()) return null;

        try {
            return D3D12Shader.create(mContext, pixelShaderName, pixelShaderCode, samplers, params);
        } catch (Exception e) {
            throw new InternalError("Failed to create shader: " + e.getMessage());
        }
    }

    @Override
    public Shader createShader(String pixelShaderName, Map<String, Integer> samplers, Map<String, Integer> params,
                               int maxTexCoordIndex, boolean isPixcoordUsed, boolean isPerVertexColorUsed) {
        throw new UnsupportedOperationException("Not supported on D3D12 backend");
    }

    @Override
    public Shader createStockShader(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Shader name must be non-null");
        }
        try {
            Class klass = Class.forName("com.sun.prism.shader." + name + "_Loader");
            InputStream codeStream = D3D12ResourceFactory.class.getResourceAsStream("hlsl6/" + name + ".cso");
            Method m = klass.getMethod("loadShader",
                new Class[] { ShaderFactory.class, String.class, InputStream.class });
            return (Shader)m.invoke(null, new Object[] { this, name, codeStream });
        } catch (Throwable e) {
            e.printStackTrace();
            throw new InternalError("Error loading stock shader " + name);
        }
    }

    @Override
    public TextureResourcePool<D3D12TextureData> getTextureResourcePool() {
        return D3D12ResourcePool.instance;
    }

    static int nextPowerOfTwo(int val, int max) {
        if (val > max) {
            return 0;
        }
        int i = 1;
        while (i < val) {
            i *= 2;
        }
        return i;
    }

    @Override
    public D3D12Texture createTexture(PixelFormat formatHint, Usage usageHint, WrapMode wrapMode, int w, int h) {
        return createTexture(formatHint, usageHint, wrapMode, w, h, false);
    }

    @Override
    public D3D12Texture createTexture(PixelFormat formatHint, Usage usageHint, WrapMode wrapMode, int w, int h,
            boolean useMipmap) {
        if (checkDisposed()) return null;

        if (!isFormatSupported(formatHint)) {
            throw new UnsupportedOperationException(
                "Pixel format " + formatHint + " not supported on this device");
        }

        if (formatHint == PixelFormat.MULTI_YCbCr_420) {
            throw new UnsupportedOperationException("MULTI_YCbCr_420 textures require a MediaFrame");
        }

        int allocw, alloch;
        if (PrismSettings.forcePow2) {
            allocw = nextPowerOfTwo(w, Integer.MAX_VALUE);
            alloch = nextPowerOfTwo(h, Integer.MAX_VALUE);
        } else {
            allocw = w;
            alloch = h;
        }

        if (allocw <= 0 || alloch <= 0) {
            throw new RuntimeException("Illegal texture dimensions (" + allocw + "x" + alloch + ")");
        }

        int bpp = formatHint.getBytesPerPixelUnit();
        if (allocw >= (Integer.MAX_VALUE / alloch / bpp)) {
            throw new RuntimeException("Illegal texture dimensions (" + allocw + "x" + alloch + ")");
        }

        D3D12ResourcePool pool = D3D12ResourcePool.instance;
        long size = pool.estimateTextureSize(allocw, alloch, formatHint);
        if (!pool.prepareForAllocation(size)) {
            return null;
        }

        D3D12NativeTexture texture = mContext.getDevice().createTexture(
            allocw, alloch, formatHint, usageHint, wrapMode, 1, useMipmap, false
        );

        if (!texture.isValid()) {
            throw new RuntimeException("Failed to create D3D12 texture");
        }

        if (wrapMode != WrapMode.CLAMP_NOT_NEEDED && (w < texture.getWidth() || h < texture.getHeight())) {
            wrapMode = wrapMode.simulatedVersion();
        }
        return D3D12Texture.create(texture, mContext, formatHint, wrapMode, w, h);
    }

    @Override
    public Texture createTexture(MediaFrame frame) {
        if (checkDisposed()) return null;

        D3D12Texture texture = null;

        try (D3D12Utils.AutoReleasableMediaFrame mf =
                new D3D12Utils.AutoReleasableMediaFrame(frame)) {
            int width = mf.get().getWidth();
            int height = mf.get().getHeight();
            int texWidth = mf.get().getEncodedWidth();
            int texHeight = mf.get().getEncodedHeight();
            PixelFormat texFormat = mf.get().getPixelFormat();

            if (texFormat == PixelFormat.MULTI_YCbCr_420) {
                // Create a MultiTexture instead
                // TODO: D3D12: This is done "the old way" because of old APIs handling of YUV textures.
                //             D3D12 allows us to combine all planes into one texture, but that would
                //             require rewriting JSL shaders, which breaks backwards compatibility. If
                //             we ever drop older backends, consider switching to single NativeTexture
                //             allocated with one of multi-plane formats (if other backends allow)
                MultiTexture tex = new MultiTexture(texFormat, WrapMode.CLAMP_TO_EDGE, width, height);

                // create/add the subtextures
                // plane indices: 0 = luma, 1 = Cb, 2 = Cr, 3 (optional) = alpha
                for (int index = 0; index < frame.planeCount(); index++) {
                    int subWidth = texWidth;
                    int subHeight =  texHeight; // might not match height if height is odd

                    if (index == PixelFormat.YCBCR_PLANE_CHROMABLUE
                            || index == PixelFormat.YCBCR_PLANE_CHROMARED)
                    {
                        subWidth /= 2;
                        subHeight /= 2;
                    }

                    D3D12Texture subTex = createTexture(PixelFormat.BYTE_ALPHA, Usage.DYNAMIC, WrapMode.CLAMP_TO_EDGE, subWidth, subHeight);
                    if (subTex == null || !subTex.getNativeTexture().isValid()) {
                        tex.dispose();
                        return null;
                    }
                    tex.setTexture(subTex, index);
                }

                return tex;
            } else {
                if (texWidth <= 0 || texHeight <= 0) {
                    throw new RuntimeException("Illegal texture dimensions (" + texWidth + "x" + texHeight + ")");
                }

                int bpp = texFormat.getBytesPerPixelUnit();
                if (texWidth >= (Integer.MAX_VALUE / texHeight / bpp)) {
                    throw new RuntimeException("Illegal texture dimensions (" + texWidth + "x" + texHeight + ")");
                }

                D3D12ResourcePool pool = D3D12ResourcePool.instance;
                long size = pool.estimateTextureSize(texWidth, texHeight, texFormat);
                if (!pool.prepareForAllocation(size)) {
                    return null;
                }

                WrapMode wrapMode = WrapMode.CLAMP_TO_EDGE;
                D3D12NativeTexture nativeTexture = mContext.getDevice().createTexture(
                    texWidth, texHeight, texFormat, Usage.DYNAMIC, wrapMode, 1, false, false
                );

                if (!nativeTexture.isValid()) {
                    throw new RuntimeException("Failed to create D3D12 texture for MediaFrame");
                }

                int physWidth = nativeTexture.getWidth();
                int physHeight = nativeTexture.getHeight();
                wrapMode = (texWidth < physWidth || texHeight < physHeight)
                    ? WrapMode.CLAMP_TO_EDGE_SIMULATED : WrapMode.CLAMP_TO_EDGE;
                texture = D3D12Texture.create(nativeTexture, mContext, texFormat, wrapMode, width, height);
            }
        }

        return texture;
    }

    @Override
    public boolean isFormatSupported(PixelFormat format) {
        return mContext.getDevice().checkFormatSupport(format);
    }

    @Override
    public int getMaximumTextureSize() {
        return mMaximumTextureSize;
    }

    @Override
    public int getRTTWidth(int w, WrapMode wrapMode) {
        // Same as D3D9, D3D12 supports non-power-of-two dimensions for RTTs
        return w;
    }

    @Override
    public int getRTTHeight(int h, WrapMode wrapMode) {
        // Same as D3D9, D3D12 supports non-power-of-two dimensions for RTTs
        return h;
    }

    @Override
    public RTTexture createRTTexture(int width, int height, WrapMode wrapMode) {
        return createRTTexture(width, height, wrapMode, false);
    }

    @Override
    public RTTexture createRTTexture(int width, int height, WrapMode wrapMode, boolean msaa) {
        // Default to creating an RTT with dirty bbox enabled
        // Realistically only SwapChain will want an offscreen RTT with dirty bbox opts disabled, so
        // it can manually call the full overload below. Other RTTs should have this enabled.
        return createRTTexture(width, height, wrapMode, msaa, true);
    }

    public RTTexture createRTTexture(int width, int height, WrapMode wrapMode, boolean msaa, boolean enableDirtyBBox) {
        if (checkDisposed()) return null;

        int createw = width;
        int createh = height;
        if (PrismSettings.forcePow2) {
            createw = nextPowerOfTwo(createw, Integer.MAX_VALUE);
            createh = nextPowerOfTwo(createh, Integer.MAX_VALUE);
        }

        if (createw <= 0 || createh <= 0) {
            throw new RuntimeException("Illegal texture dimensions (" + createw + "x" + createh + ")");
        }

        PixelFormat format = PixelFormat.INT_ARGB_PRE;
        int bpp = format.getBytesPerPixelUnit();
        if (createw >= (Integer.MAX_VALUE / createh / bpp)) {
            throw new RuntimeException("Illegal texture dimensions (" + createw + "x" + createh + ")");
        }

        D3D12ResourcePool pool = D3D12ResourcePool.instance;
        int aaSamples = 1;
        if (msaa) {
            aaSamples = mContext.getMSAASampleSize(format);
        }

        // TODO: D3D12: Add depth texture estimation
        long size = pool.estimateRTTextureSize(createw, createh, false) * aaSamples;
        if (!pool.prepareForAllocation(size)) {
            return null;
        }

        D3D12RTTexture tex = D3D12RTTexture.create(mContext, createw, createh, width, height, format, wrapMode, aaSamples, enableDirtyBBox);
        if (!tex.isValid()) {
            return null;
        }

        return tex;
    }

    @Override
    public boolean isCompatibleTexture(Texture tex) {
        return tex instanceof D3D12Texture;
    }

    @Override
    public Presentable createPresentable(PresentableState pState) {
        if (checkDisposed()) return null;
        return D3D12SwapChain.create(mContext, pState);
    }

    @Override
    public PhongMaterial createPhongMaterial() {
        if (checkDisposed()) return null;
        return D3D12PhongMaterial.create(mContext);
    }

    @Override
    public MeshView createMeshView(Mesh mesh) {
        if (checkDisposed()) return null;
        return D3D12MeshView.create(mContext, (D3D12Mesh)mesh);
    }

    @Override
    public Mesh createMesh() {
        if (checkDisposed()) return null;
        return D3D12Mesh.create(mContext);
    }

    @Override
    public void dispose() {
        super.dispose();
        mContext.dispose();
    }
}
