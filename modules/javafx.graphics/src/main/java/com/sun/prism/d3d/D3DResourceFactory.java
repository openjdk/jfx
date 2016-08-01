/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

import com.sun.glass.ui.Screen;
import com.sun.prism.Image;
import com.sun.prism.MediaFrame;
import com.sun.prism.Mesh;
import com.sun.prism.MeshView;
import com.sun.prism.MultiTexture;
import com.sun.prism.PhongMaterial;
import com.sun.prism.PixelFormat;
import com.sun.prism.Presentable;
import com.sun.prism.PresentableState;
import com.sun.prism.Texture;
import com.sun.prism.Texture.Usage;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.d3d.D3DResource.D3DRecord;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.ps.BaseShaderFactory;
import com.sun.prism.impl.TextureResourcePool;
import com.sun.prism.ps.Shader;
import com.sun.prism.ps.ShaderFactory;
import java.util.WeakHashMap;

class D3DResourceFactory extends BaseShaderFactory {
    private static final Map<Image,Texture> clampTexCache = new WeakHashMap<>();
    private static final Map<Image,Texture> repeatTexCache = new WeakHashMap<>();
    private static final Map<Image,Texture> mipmapTexCache = new WeakHashMap<>();

    private final D3DContext context;
    private final int maxTextureSize;

    /**
     * List of disposer records for d3d resources created by the pipeline.
     * @see D3DResource
     */
    private final LinkedList<D3DResource.D3DRecord> records =
        new LinkedList<D3DResource.D3DRecord>();

    D3DResourceFactory(long pContext, Screen screen) {
        super(clampTexCache, repeatTexCache, mipmapTexCache);
        context = new D3DContext(pContext, screen, this);
        context.initState();
        maxTextureSize = computeMaxTextureSize();

        if (PrismSettings.noClampToZero && PrismSettings.verbose) {
            System.out.println("prism.noclamptozero not supported by D3D");
        }
    }

    D3DContext getContext() {
        return context;
    }

    public TextureResourcePool getTextureResourcePool() {
        return D3DVramPool.instance;
    }

    static final int STATS_FREQUENCY = PrismSettings.prismStatFrequency;
    private int nFrame = -1;
    private D3DFrameStats frameStats;

    private void displayPrismStatistics() {
        if (STATS_FREQUENCY > 0) {
            if (++nFrame == STATS_FREQUENCY) {
                nFrame = 0;
                frameStats = context.getFrameStats(true, frameStats);
                if (frameStats != null) {
                    System.err.println(frameStats.toDebugString(STATS_FREQUENCY));
                }
            }
        }
    }

    @Override
    public boolean isDeviceReady() {
        displayPrismStatistics();
        return context.testLostStateAndReset();
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
    public boolean isCompatibleTexture(Texture tex) {
        return tex instanceof D3DTexture;
    }

    @Override
    public D3DTexture createTexture(PixelFormat format, Usage usagehint,
            WrapMode wrapMode, int w, int h) {
        return createTexture(format, usagehint, wrapMode, w, h, false);
    }

    @Override
    public D3DTexture createTexture(PixelFormat format, Usage usagehint,
            WrapMode wrapMode, int w, int h, boolean useMipmap) {
        if (!isFormatSupported(format)) {
            throw new UnsupportedOperationException(
                "Pixel format " + format +
                " not supported on this device");
        }

        if (format == PixelFormat.MULTI_YCbCr_420) {
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
        D3DVramPool pool = D3DVramPool.instance;
        long size = pool.estimateTextureSize(allocw, alloch, format);
        if (!pool.prepareForAllocation(size)) {
            return null;
        }
        long pResource = nCreateTexture(context.getContextHandle(),
                                        format.ordinal(), usagehint.ordinal(),
                                        false /*isRTT*/, allocw, alloch, 0, useMipmap);
        if (pResource == 0L) {
            return null;
        }

        int texw = nGetTextureWidth(pResource);
        int texh = nGetTextureHeight(pResource);
        if (wrapMode != WrapMode.CLAMP_NOT_NEEDED && (w < texw || h < texh)) {
            wrapMode = wrapMode.simulatedVersion();
        }
        return new D3DTexture(context, format, wrapMode, pResource, texw, texh, w, h, useMipmap);
    }

    @Override
    public Texture createTexture(MediaFrame frame) {
        frame.holdFrame();

        int width = frame.getWidth();
        int height = frame.getHeight();
        int texWidth = frame.getEncodedWidth();
        int texHeight = frame.getEncodedHeight();
        PixelFormat texFormat = frame.getPixelFormat();

        if (texFormat == PixelFormat.MULTI_YCbCr_420) {
            // Create a MultiTexture instead
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

                D3DTexture subTex = createTexture(PixelFormat.BYTE_ALPHA, Usage.DYNAMIC, WrapMode.CLAMP_TO_EDGE,
                                                  subWidth, subHeight);
                if (subTex == null) {
                    tex.dispose();
                    return null;
                }
                tex.setTexture(subTex, index);
            }

            frame.releaseFrame();
            return tex;
        } else {
            D3DVramPool pool = D3DVramPool.instance;
            long size = pool.estimateTextureSize(texWidth, texHeight, texFormat);
            if (!pool.prepareForAllocation(size)) {
                return null;
            }
            long pResource = nCreateTexture(context.getContextHandle(),
                    texFormat.ordinal(), Usage.DYNAMIC.ordinal(),
                    false, texWidth, texHeight, 0, false);
            if (0 == pResource) {
                return null;
            }

            int physWidth = nGetTextureWidth(pResource);
            int physHeight = nGetTextureHeight(pResource);
            WrapMode wrapMode = (texWidth < physWidth || texHeight < physHeight)
                    ? WrapMode.CLAMP_TO_EDGE_SIMULATED : WrapMode.CLAMP_TO_EDGE;
            D3DTexture tex = new D3DTexture(context, texFormat, wrapMode, pResource,
                                            physWidth, physHeight, width, height, false);
            frame.releaseFrame();
            return tex;
        }
    }

    public int getRTTWidth(int w, WrapMode wrapMode) {
        // D3DRTTexture returns the requested dimension as the content dimension
        // so the answer here is just "w" despite the fact that a pow2 adjustment
        // is made for the actual allocation.  Typically, D3D supports non-pow2
        // textures on every implementation so the pow2 code below is not really
        // encountered in practice anyway (it's only supported for "debugging").
//        if (PrismSettings.forcePow2) {
//            w = nextPowerOfTwo(w, Integer.MAX_VALUE);
//        }
        return w;
    }

    public int getRTTHeight(int h, WrapMode wrapMode) {
        // D3DRTTexture returns the requested dimension as the content dimension
        // so the answer here is just "h" despite the fact that a pow2 adjustment
        // is made for the actual allocation.  Typically, D3D supports non-pow2
        // textures on every implementation so the pow2 code below is not really
        // encountered in practice anyway (it's only supported for "debugging").
//        if (PrismSettings.forcePow2) {
//            h = nextPowerOfTwo(h, Integer.MAX_VALUE);
//        }
        return h;
    }

    @Override
    public D3DRTTexture createRTTexture(int width, int height, WrapMode wrapMode) {
        return createRTTexture(width, height, wrapMode, false);
    }

    @Override
    public D3DRTTexture createRTTexture(int width, int height, WrapMode wrapMode, boolean msaa) {
        if (PrismSettings.verbose && context.isLost()) {
            System.err.println("RT Texture allocation while the device is lost");
        }

        int createw = width;
        int createh = height;
        int cx = 0;
        int cy = 0;
        if (PrismSettings.forcePow2) {
            createw = nextPowerOfTwo(createw, Integer.MAX_VALUE);
            createh = nextPowerOfTwo(createh, Integer.MAX_VALUE);
        }
        D3DVramPool pool = D3DVramPool.instance;
        int aaSamples;
        if (msaa) {
            int maxSamples = D3DPipeline.getInstance().getMaxSamples();
            aaSamples =  maxSamples < 2 ? 0 : (maxSamples < 4 ? 2 : 4);
        } else {
            aaSamples = 0;
        }
        // TODO: 3D - Improve estimate to include if multisample rtt
        long size = pool.estimateRTTextureSize(width, height, false);
        if (!pool.prepareForAllocation(size)) {
            return null;
        }

        long pResource = nCreateTexture(context.getContextHandle(),
                                        PixelFormat.INT_ARGB_PRE.ordinal(),
                                        Usage.DEFAULT.ordinal(),
                                        true /*isRTT*/, createw, createh, aaSamples, false);
        if (pResource == 0L) {
            return null;
        }

        int texw = nGetTextureWidth(pResource);
        int texh = nGetTextureHeight(pResource);
        D3DRTTexture rtt = new D3DRTTexture(context, wrapMode, pResource, texw, texh,
                                            cx, cy, width, height, aaSamples);
        // ensure the RTTexture is cleared to all zeros before returning
        // (Decora relies on the Java2D behavior, where an image is expected
        // to be fully transparent after initialization)
        rtt.createGraphics().clear();
        return rtt;
    }

    public Presentable createPresentable(PresentableState pState) {
        if (PrismSettings.verbose && context.isLost()) {
            System.err.println("SwapChain allocation while the device is lost");
        }

        long pResource = nCreateSwapChain(context.getContextHandle(),
                                          pState.getNativeView(),
                                          PrismSettings.isVsyncEnabled);

        if (pResource != 0L) {
            int width = pState.getRenderWidth();
            int height = pState.getRenderHeight();
            D3DRTTexture rtt = createRTTexture(width, height, WrapMode.CLAMP_NOT_NEEDED, pState.isMSAA());
            if (PrismSettings.dirtyOptsEnabled) {
                rtt.contentsUseful();
            }

            if (rtt != null) {
                return new D3DSwapChain(context, pResource, rtt, pState.getRenderScaleX(), pState.getRenderScaleY());
            }

            D3DResourceFactory.nReleaseResource(context.getContextHandle(), pResource);
        }
        return null;

    }

    private static ByteBuffer getBuffer(InputStream is) {
        if (is == null) {
           throw new RuntimeException("InputStream must be non-null");
        }
        try {
            int len = 4096;
            byte[] data = new byte[len];
            BufferedInputStream bis = new BufferedInputStream(is, len);
            int offset = 0;
            int readBytes = -1;
            while ((readBytes = bis.read(data, offset, len - offset)) != -1) {
                offset += readBytes;
                if (len - offset == 0) {
                    // grow the array
                    len *= 2;
                    // was
                    // data = Arrays.copyOf(data, len);
                    //
                    byte[] newdata = new byte[len];
                    System.arraycopy(data, 0, newdata, 0, data.length);
                    data = newdata;
                }
            }
            bis.close();
            // NOTE: for now the D3DShader native code only knows how to
            // deal with direct ByteBuffers, so we have to dump the byte[]
            // into a newly allocated direct buffer...
            ByteBuffer buf = ByteBuffer.allocateDirect(offset);
            buf.put(data, 0, offset);
            return buf;
        } catch (IOException e) {
            throw new RuntimeException("Error loading D3D shader object", e);
        }
    }

    public Shader createShader(InputStream pixelShaderCode,
                               Map<String, Integer> samplers,
                               Map<String, Integer> params,
                               int maxTexCoordIndex,
                               boolean isPixcoordUsed,
                               boolean isPerVertexColorUsed)
    {
        long shaderHandle = D3DShader.init(
                context.getContextHandle(), getBuffer(pixelShaderCode),
                maxTexCoordIndex, isPixcoordUsed, isPerVertexColorUsed);

        return new D3DShader(context, shaderHandle, params);
    }

    public Shader createStockShader(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("Shader name must be non-null");
        }
        try {
            InputStream stream = AccessController.doPrivileged(
                    (PrivilegedAction<InputStream>) () -> D3DResourceFactory.class.
                           getResourceAsStream("hlsl/" + name + ".obj")
            );
            Class klass = Class.forName("com.sun.prism.shader." + name + "_Loader");
            Method m = klass.getMethod("loadShader",
                new Class[] { ShaderFactory.class, InputStream.class });
            return (Shader)m.invoke(null, new Object[] { this, stream });
        } catch (Throwable e) {
            e.printStackTrace();
            throw new InternalError("Error loading stock shader " + name);
        }
    }

    @Override
    public void dispose() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isFormatSupported(PixelFormat format) {
        return true;
    }

    private int computeMaxTextureSize() {
        int size = nGetMaximumTextureSize(context.getContextHandle());
        if (PrismSettings.verbose) {
            System.err.println("Maximum supported texture size: " + size);
        }
        if (size > PrismSettings.maxTextureSize) {
            size = PrismSettings.maxTextureSize;
            if (PrismSettings.verbose) {
                System.err.println("Maximum texture size clamped to " + size);
            }
        }
        return size;
    }

    public int getMaximumTextureSize() {
        return maxTextureSize;
    }

    @Override
    protected void notifyReset() {
        for (ListIterator<D3DRecord> it = records.listIterator(); it.hasNext();) {
            D3DRecord r = it.next();
            if (r.isDefaultPool()) {
                r.markDisposed();
                it.remove();
            }
        }
        super.notifyReset();
    }

    @Override
    protected void notifyReleased() {
        for (ListIterator<D3DRecord> it = records.listIterator(); it.hasNext();) {
            D3DRecord r = it.next();
            r.markDisposed();
        }
        records.clear();
        super.notifyReleased();
    }

    void addRecord(D3DRecord record) {
        records.add(record);
    }

    void removeRecord(D3DRecord record) {
        records.remove(record);
    }

    public PhongMaterial createPhongMaterial() {
        return D3DPhongMaterial.create(context);
    }

    public MeshView createMeshView(Mesh mesh) {
        return D3DMeshView.create(context, (D3DMesh) mesh);

    }

    public Mesh createMesh() {
        return D3DMesh.create(context);
    }

    static native long nGetContext(int adapterOrdinal);
    static native boolean nIsDefaultPool(long pResource);
    static native int nTestCooperativeLevel(long pContext);
    static native int nResetDevice(long pContext);
    static native long nCreateTexture(long pContext,
                                      int format, int hint,
                                      boolean isRTT,
                                      int width, int height, int samples,
                                      boolean useMipmap);
    static native long nCreateSwapChain(long pContext, long hwnd,
                                        boolean isVsyncEnabled);
    static native int nReleaseResource(long pContext, long resource);
    static native int nGetMaximumTextureSize(long pContext);
    static native int nGetTextureWidth(long pResource);
    static native int nGetTextureHeight(long pResource);
    static native int nReadPixelsI(long pContext, long pResource,
                                    long length,
                                    Buffer pixels, int[] arr,
                                    int contentWidth, int contentHeight);
    static native int nReadPixelsB(long pContext, long pResource,
                                    long length,
                                    Buffer pixels, byte[] arr,
                                    int contentWidth, int contentHeight);
    static native int nUpdateTextureI(long contextHandle, long pResource,
                                      IntBuffer buf, int[] pixels,
                                      int dstx, int dsty,
                                      int srcx, int srcy,
                                      int srcw, int srch, int srcscan);
    static native int nUpdateTextureF(long contextHandle, long pResource,
                                      FloatBuffer buf, float[] pixels,
                                      int dstx, int dsty,
                                      int srcx, int srcy,
                                      int srcw, int srch, int srcscan);
    static native int nUpdateTextureB(long contextHandle, long pResource,
                                      ByteBuffer buf, byte[] pixels,
                                      int formatHint,
                                      int dstx, int dsty,
                                      int srcx, int srcy,
                                      int srcw, int srch, int srcscan);

    static native long nGetDevice(long pContext);
    static native long nGetNativeTextureObject(long pResource);
}
