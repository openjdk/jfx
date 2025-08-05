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
import com.sun.prism.Texture.Usage;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.Texture;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.TextureResourcePool;
import com.sun.prism.impl.ps.BaseShaderFactory;
import com.sun.prism.ps.Shader;
import com.sun.prism.ps.ShaderFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;


class MTLResourceFactory extends BaseShaderFactory {

    private final MTLContext context;

    MTLResourceFactory(Screen screen) {
        context = new MTLContext(screen, this);
        context.initState();
        if (PrismSettings.noClampToZero && PrismSettings.verbose) {
            System.err.println("prism.noClampToZero not supported by MTL");
        }
    }

    static int nextPowerOfTwo(int val, int max) {
        if (val > max) {
            return 0;
        }

        // check if number is power of 2
        if ((val & (val - 1)) == 0) {
            return val;
        }

        int i = 1;
        while (i < val) {
            i <<= 1; // i *= 2;
        }
        return i;
    }

    public MTLContext getContext() {
        return context;
    }

    private void checkTextureSize(int width, int height) {
        int maxSize = getMaximumTextureSize();
        if (width <= 0 || height <= 0 ||
            width > maxSize || height > maxSize) {
            throw new RuntimeException("Illegal texture dimensions (" + width + "x" + height + ")");
        }
    }

    @Override
    public Shader createShader(String pixelShaderName, InputStream pixelShaderCode, Map<String, Integer> samplers,
                               Map<String, Integer> params, int maxTexCoordIndex,
                               boolean isPixcoordUsed, boolean isPerVertexColorUsed) {
        try {
            return createShader(pixelShaderName, samplers, params, maxTexCoordIndex,
                                isPixcoordUsed, isPerVertexColorUsed);
        } catch (Exception e) {
            e.printStackTrace();
            throw new UnsupportedOperationException("Failed to create a prism shader");
        }
    }

    @Override
    public Shader createShader(String shaderName, Map<String, Integer> samplers,
                               Map<String, Integer> params, int maxTexCoordIndex,
                               boolean isPixcoordUsed, boolean isPerVertexColorUsed) {
        return MTLShader.createShader(getContext(), shaderName, samplers,
                params, maxTexCoordIndex, isPixcoordUsed, isPerVertexColorUsed);
    }

    @Override
    public Shader createStockShader(String shaderName) {
        Objects.requireNonNull(shaderName, "Shader name not be null");
        try {
            if (PrismSettings.verbose) {
                System.err.println("MTLResourceFactory: Prism - createStockShader: " + shaderName);
            }
            Class<?> klass = Class.forName("com.sun.prism.shader." + shaderName + "_Loader");
            Method m = klass.getMethod("loadShader", new Class[] {ShaderFactory.class, String.class, InputStream.class});
            InputStream nameStream = new ByteArrayInputStream(shaderName.getBytes());
            return (Shader) m.invoke(null, new Object[]{this, shaderName, nameStream});
        } catch (Throwable e) {
            e.printStackTrace();
            throw new InternalError("Error loading stock shader " + shaderName);
        }
    }

    @Override
    public TextureResourcePool<?> getTextureResourcePool() {
        return MTLVramPool.getInstance();
    }

    @Override
    public Texture createTexture(PixelFormat formatHint, Texture.Usage usageHint,
                                 Texture.WrapMode wrapMode, int w, int h) {
        return createTexture(formatHint, usageHint, wrapMode, w, h, false);
    }

    @Override
    public Texture createTexture(PixelFormat formatHint, Texture.Usage usageHint,
                                 Texture.WrapMode wrapMode, int w, int h, boolean useMipmap) {

        if (checkDisposed()) return null;

        if (!isFormatSupported(formatHint)) {
            throw new UnsupportedOperationException(
                "Pixel format " + formatHint +
                    " not supported on this device");
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

        checkTextureSize(allocw, alloch);

        int bpp = formatHint.getBytesPerPixelUnit();
        if (allocw >= (Integer.MAX_VALUE / alloch / bpp)) {
            throw new RuntimeException("Illegal texture dimensions (" + allocw + "x" + alloch + ")");
        }

        MTLVramPool pool = MTLVramPool.getInstance();
        long size = pool.estimateTextureSize(allocw, alloch, formatHint);
        if (!pool.prepareForAllocation(size)) {
            return null;
        }

        long pResource = nCreateTexture(context.getContextHandle() ,
                formatHint.ordinal(), usageHint.ordinal(),
                false, allocw, alloch, 0, useMipmap);

        if (pResource == 0L) {
            return null;
        }

        MTLTextureData textData = new MTLTextureData(context, pResource, size);
        MTLTextureResource<MTLTextureData> resource = new MTLTextureResource<>(textData, true);

        // contentX and contentY is set as 0 unlike D3D/ES2.
        // The wrap mode are addressed, can be mapped to D3D/ES2 only if necessary.
        return new MTLTexture<MTLTextureData>(getContext(), resource, formatHint, wrapMode, allocw, alloch, 0, 0, allocw, alloch, useMipmap);
    }

    @Override
    public Texture createTexture(MediaFrame frame) {
        frame.holdFrame();

        try {
            int width = frame.getWidth();
            int height = frame.getHeight();
            int texWidth = frame.getEncodedWidth();
            int texHeight = frame.getEncodedHeight();
            PixelFormat texFormat = frame.getPixelFormat();

            checkTextureSize(texWidth, texHeight);

            int bpp = texFormat.getBytesPerPixelUnit();
            if (texWidth >= (Integer.MAX_VALUE / texHeight / bpp)) {
                throw new RuntimeException("Illegal texture dimensions (" + texWidth + "x" + texHeight + ")");
            }

            if (texFormat == PixelFormat.MULTI_YCbCr_420) {
                // Create a MultiTexture
                MultiTexture tex = new MultiTexture(texFormat, WrapMode.CLAMP_TO_EDGE, width, height);

                // create/add the subtextures
                // Textures: 0 = luma, 1 = Chroma blue, 2 = Chroma red, 3 = alpha
                for (int index = 0; index < frame.planeCount(); index++) {
                    int subWidth = texWidth;
                    int subHeight =  texHeight;

                    if (index == PixelFormat.YCBCR_PLANE_CHROMABLUE
                            || index == PixelFormat.YCBCR_PLANE_CHROMARED)
                    {
                        subWidth /= 2;
                        subHeight /= 2;
                    }

                    Texture subTex = createTexture(PixelFormat.BYTE_ALPHA, Usage.DYNAMIC, WrapMode.CLAMP_TO_EDGE,
                                                      subWidth, subHeight);

                    if (subTex == null) {
                        tex.dispose();
                        return null;
                    }

                    tex.setTexture(subTex, index);
                }

                // Note : Solid_TexuteYV12.metal shader that is used to render this pixel format
                // expects 4 texture parameters
                // Generate alpha texture artificially if it is unavailable in the MediaFrame
                if (frame.planeCount() == 3) {

                    Texture subTex = createTexture(PixelFormat.BYTE_ALPHA, Usage.DYNAMIC, WrapMode.CLAMP_TO_EDGE,
                                                   texWidth, texHeight);

                    if (subTex == null) {
                        tex.dispose();
                        return null;
                    }

                    byte[] arr = new byte[texWidth * texHeight];
                    Arrays.fill(arr, (byte)255);
                    ByteBuffer pixels = ByteBuffer.wrap(arr);
                    subTex.update(pixels, PixelFormat.BYTE_ALPHA, 0, 0, 0, 0,
                                  texWidth, texHeight, texWidth, true);

                    tex.setTexture(subTex, 3);
                }
                return tex;
            } // PixelFormat.MULTI_YCbCr_420

            Texture tex = createTexture(texFormat, Usage.DEFAULT, WrapMode.CLAMP_TO_EDGE, texWidth, texHeight);

            return tex;
        } finally {
            frame.releaseFrame();
        }
    }

    @Override
    public boolean isFormatSupported(PixelFormat format) {
        return switch (format) {
            case BYTE_RGB,
                 BYTE_GRAY,
                 BYTE_ALPHA,
                 BYTE_BGRA_PRE,
                 BYTE_APPLE_422,
                 INT_ARGB_PRE,
                 FLOAT_XYZW -> true;

            case MULTI_YCbCr_420 -> false;
        };
    }

    @Override
    public int getMaximumTextureSize() {
        // This value can be fetched from the MTLDevice.
        // This value comes from Metal feature set tables
        return 16384; // For MTLGPUFamilyApple3 and above
    }

    @Override
    public int getRTTWidth(int w, Texture.WrapMode wrapMode) {
        // Below debugging logic replicates D3DResoureFactory
        // if (PrismSettings.forcePow2) {
        //     w = nextPowerOfTwo(w, Integer.MAX_VALUE);
        // }
        return w;
    }

    @Override
    public int getRTTHeight(int h, Texture.WrapMode wrapMode) {
        // Below debugging logic replicates D3DResoureFactory
        // if (PrismSettings.forcePow2) {
        //     h = nextPowerOfTwo(h, Integer.MAX_VALUE);
        // }
        return h;
    }

    @Override
    public RTTexture createRTTexture(int width, int height, Texture.WrapMode wrapMode) {
        return createRTTexture(width, height, wrapMode, false);
    }

    static int nextPowerOf64(int val, int max) {
        // Using a random value for width or height of texture results in this error:
        // -> validateStrideTextureParameters:1512: failed assertion
        // -> Linear texture: bytesPerRow (XXXX) must be aligned to 256 bytes,
        // This implies that the width and height of a texture must be multiple of 64 pixels.
        if (val > max) {
            return 0;
        }
        int minPixelsRow = 64;
        if (val % minPixelsRow != 0) {
            int times = val / minPixelsRow;
            val = minPixelsRow * (times + 1);
        }
        return val;
    }

    @Override
    public RTTexture createRTTexture(int width, int height, Texture.WrapMode wrapMode, boolean msaa) {
        int createw = width;
        int createh = height;

        if (PrismSettings.forcePow2) {
            createw = nextPowerOfTwo(createw, Integer.MAX_VALUE);
            createh = nextPowerOfTwo(createh, Integer.MAX_VALUE);
        }

        checkTextureSize(createw, createh);

        PixelFormat format = PixelFormat.INT_ARGB_PRE;
        int bpp = format.getBytesPerPixelUnit();
        if (createw >= (Integer.MAX_VALUE / createh / bpp)) {
            throw new RuntimeException("Illegal texture dimensions (" + createw + "x" + createh + ")");
        }
        // We don't create PowerOf64 textures in D3D/OpenGL but
        // earlier implementation of Metal required RT texture of pow64.
        // Removed usage of nextPowerOf64 as part of JDK-8311225 and
        // verified Ensemble8 and demos are running fine.
        // If usage of nextPowerOf64 is not needed we should remove it in future.
        // createw = nextPowerOf64(createw, 8192);
        // createh = nextPowerOf64(createh, 8192);

        MTLVramPool pool = MTLVramPool.getInstance();
        long size = pool.estimateRTTextureSize(createw, createh, false);
        if (!pool.prepareForAllocation(size)) {
            return null;
        }

        MTLRTTexture rtt = MTLRTTexture.create(context, createw, createh, width, height, wrapMode, msaa, size);
        return rtt;
    }

    @Override
    public boolean isCompatibleTexture(Texture tex) {
        return tex instanceof MTLTexture;
    }

    @Override
    public Presentable createPresentable(PresentableState pState) {
        if (checkDisposed()) {
            return null;
        }
        checkTextureSize(pState.getRenderWidth(), pState.getRenderHeight());
        return new MTLSwapChain(getContext(), pState);
    }

    @Override
    public void dispose() {
        context.dispose();
        super.dispose();
    }

    @Override
    public PhongMaterial createPhongMaterial() {
        if (checkDisposed()) return null;
        return MTLPhongMaterial.create(context);
    }

    @Override
    public MeshView createMeshView(Mesh mesh) {
        if (checkDisposed()) return null;
        return MTLMeshView.create(context, (MTLMesh) mesh);
    }

    @Override
    public Mesh createMesh() {
        if (checkDisposed()) return null;
        return MTLMesh.create(context);
    }

    static void releaseTexture(long resource) {
        nReleaseTexture(resource);
    }

    // Native methods

    static native long nCreateTexture(long pContext, int format, int hint, boolean isRTT,
                                      int width, int height, int samples, boolean useMipmap);

    static native void nReleaseTexture(long pTexture);
}
