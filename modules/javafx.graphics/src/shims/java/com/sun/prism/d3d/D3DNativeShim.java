/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.List;

/**
 * Test access to the {@code prism_d3d} binding layer of {@code com.sun.prism.d3d}.
 * <p>
 * Compiled into the module by the {@code compile-shims} execution of the javafx.graphics pom, so it
 * may call the package-private {@link D3DNative} and read the package-private constants of
 * {@link D3DContext}; the tests in {@code test.com.sun.prism.d3d} reach it through the
 * {@code --add-exports javafx.graphics/com.sun.prism.d3d=ALL-UNNAMED} line of {@code src/test/addExports}.
 * Every downcall a test makes goes through here, so the restricted {@code java.lang.foreign} calls stay
 * inside the module that {@code --enable-native-access} names. The out-structs the facade expects a
 * caller to own are allocated here per call, in a confined arena, and copied into plain Java values.
 */
public final class D3DNativeShim {

    /** What {@code d3d_texture_create} reported: the HRESULT and the {@code D3dTextureInfo} it filled. */
    public record TextureInfo(int hresult, MemorySegment handle, int width, int height, boolean isDefaultPool) {
    }

    private D3DNativeShim() {
    }

    /**
     * Loads the {@code prism_d3d} library, binds every {@code d3d_*} symbol and checks the ABI version,
     * without going through {@code D3DPipeline}.
     *
     * @throws UnsatisfiedLinkError if the library cannot be loaded, lacks a symbol, was denied native
     *         access or reports another ABI version
     */
    public static void loadLibrary() {
        D3DNative.loadLibrary();
    }

    public static List<String> boundSymbols() {
        return D3DNative.boundSymbols();
    }

    /** Every bound symbol with its {@code FunctionDescriptor}, {@code name + " " + descriptor}, in binding order. */
    public static List<String> descriptors() {
        return D3DNative.descriptors();
    }

    public static List<String> missingSymbols() {
        return D3DNative.missingSymbols();
    }

    public static int expectedAbiVersion() {
        return D3DNative.ABI_VERSION;
    }

    public static int abiVersion() {
        return D3DNative.abiVersion();
    }

    /* ---------------------------------------------------------------------------------------------
     * Layouts
     * ------------------------------------------------------------------------------------------- */

    public static long sizeofDriverInfo() {
        return D3DNative.sizeofDriverInfo();
    }

    public static long sizeofFrameStats() {
        return D3DNative.sizeofFrameStats();
    }

    public static long sizeofTextureInfo() {
        return D3DNative.sizeofTextureInfo();
    }

    public static long driverInfoLayoutByteSize() {
        return D3DNative.DRIVER_INFO_LAYOUT.byteSize();
    }

    public static long frameStatsLayoutByteSize() {
        return D3DNative.FRAME_STATS_LAYOUT.byteSize();
    }

    public static long textureInfoLayoutByteSize() {
        return D3DNative.TEXTURE_INFO_LAYOUT.byteSize();
    }

    public static long driverInfoLayoutOffset(String field) {
        return D3DNative.DRIVER_INFO_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement(field));
    }

    public static long frameStatsLayoutOffset(String field) {
        return D3DNative.FRAME_STATS_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement(field));
    }

    public static long textureInfoLayoutOffset(String field) {
        return D3DNative.TEXTURE_INFO_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement(field));
    }

    public static long matrixLayoutByteSize() {
        return D3DNative.MATRIX_LAYOUT.byteSize();
    }

    public static long lightLayoutByteSize() {
        return D3DNative.LIGHT_LAYOUT.byteSize();
    }

    /* ---------------------------------------------------------------------------------------------
     * Constants shared with the C side
     * ------------------------------------------------------------------------------------------- */

    public static int initFlags(boolean forceGPU, boolean isVsyncEnabled, boolean verbose) {
        return D3DNative.initFlags(forceGPU, isVsyncEnabled, verbose);
    }

    /** {@code D3dInitFlags}: force GPU, vsync, verbose. */
    public static int[] initFlagBits() {
        return new int[] {D3DNative.INIT_FORCE_GPU, D3DNative.INIT_VSYNC, D3DNative.INIT_VERBOSE};
    }

    /** {@code D3dIndexType}: 16-bit, 32-bit. */
    public static int[] indexTypes() {
        return new int[] {D3DNative.INDEX_16, D3DNative.INDEX_32};
    }

    /** The {@code D3dPixelFormat} the facade passes for {@code int[]} pixels. */
    public static int pixelFormatIntArgbPre() {
        return D3DNative.PFORMAT_INT_ARGB_PRE;
    }

    /** The {@code D3dPixelFormat} the facade passes for {@code float[]} pixels. */
    public static int pixelFormatFloatXyzw() {
        return D3DNative.PFORMAT_FLOAT_XYZW;
    }

    /** {@code D3DContext.D3DCOMPMODE_*}: clear, src, src-over, dst-out, add - the {@code D3dCompMode} order. */
    public static int[] contextCompositeModes() {
        return new int[] {D3DContext.D3DCOMPMODE_CLEAR, D3DContext.D3DCOMPMODE_SRC, D3DContext.D3DCOMPMODE_SRCOVER,
                D3DContext.D3DCOMPMODE_DSTOUT, D3DContext.D3DCOMPMODE_ADD};
    }

    /** {@code D3DContext.CULL_*}: back, front, none - the {@code D3dCullMode} order. */
    public static int[] contextCullModes() {
        return new int[] {D3DContext.CULL_BACK, D3DContext.CULL_FRONT, D3DContext.CULL_NONE};
    }

    public static int d3dOk() {
        return D3DContext.D3D_OK;
    }

    public static int eFail() {
        return D3DContext.E_FAIL;
    }

    public static boolean failed(int hr) {
        return D3DContext.FAILED(hr);
    }

    /* ---------------------------------------------------------------------------------------------
     * Pipeline
     * ------------------------------------------------------------------------------------------- */

    public static boolean pipelineInit(int flags) {
        return D3DNative.pipelineInit(flags);
    }

    public static String pipelineGetErrorMessage() {
        return D3DNative.pipelineGetErrorMessage();
    }

    public static void pipelineDispose() {
        D3DNative.pipelineDispose();
    }

    public static int pipelineGetAdapterCount() {
        return D3DNative.pipelineGetAdapterCount();
    }

    public static int pipelineGetMaxSampleSupport(int adapter) {
        return D3DNative.pipelineGetMaxSampleSupport(adapter);
    }

    /**
     * The string fields of the adapter's driver information - device description, device name, driver
     * name, warning message - or {@code null} when the adapter cannot be described.
     */
    public static String[] pipelineGetDriverStrings(int adapter) {
        D3DDriverInformation di = D3DNative.pipelineGetDriverInformation(adapter);
        if (di == null) {
            return null;
        }
        return new String[] {di.deviceDescription, di.deviceName, di.driverName, di.warningMessage};
    }

    /**
     * The 13 integer fields of the adapter's driver information in {@code D3dDriverInfo} order - vendor,
     * device, subsystem, product, version, sub-version, build, PS major, PS minor, max samples, OS major,
     * OS minor, OS build - or {@code null} when the adapter cannot be described.
     */
    public static int[] pipelineGetDriverInts(int adapter) {
        D3DDriverInformation di = D3DNative.pipelineGetDriverInformation(adapter);
        if (di == null) {
            return null;
        }
        return new int[] {di.vendorID, di.deviceID, di.subSysId, di.product, di.version, di.subVersion,
                di.buildID, di.psVersionMajor, di.psVersionMinor, di.maxSamples, di.osMajorVersion,
                di.osMinorVersion, di.osBuildNumber};
    }

    /* ---------------------------------------------------------------------------------------------
     * Context and resources
     * ------------------------------------------------------------------------------------------- */

    public static MemorySegment contextGet(int adapter) {
        return D3DNative.contextGet(adapter);
    }

    public static int contextTestCooperativeLevel(MemorySegment ctx) {
        return D3DNative.contextTestCooperativeLevel(ctx);
    }

    public static int contextGetMaxTextureSize(MemorySegment ctx) {
        return D3DNative.contextGetMaxTextureSize(ctx);
    }

    /** The eight {@code D3dFrameStats} counters in declaration order, or {@code null} when unavailable. */
    public static int[] contextGetFrameStats(MemorySegment ctx, boolean reset) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment statsOut = arena.allocate(D3DNative.FRAME_STATS_LAYOUT);
            D3DFrameStats stats = new D3DFrameStats();
            if (!D3DNative.contextGetFrameStats(ctx, statsOut, reset, stats)) {
                return null;
            }
            return new int[] {stats.numTrianglesDrawn, stats.numDrawCalls, stats.numBufferLocks,
                    stats.numTextureLocks, stats.numTextureTransferBytes, stats.numSetTexture,
                    stats.numSetPixelShader, stats.numRenderTargetSwitch};
        }
    }

    /**
     * @param format the {@code PixelFormat} ordinal
     * @param usage the {@code Texture.Usage} ordinal
     */
    public static TextureInfo textureCreate(MemorySegment ctx, int format, int usage, boolean isRTT, int width,
            int height, int samples, boolean useMipmap) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(D3DNative.TEXTURE_INFO_LAYOUT);
            int hr = D3DNative.textureCreate(ctx, format, usage, isRTT, width, height, samples, useMipmap, info);
            return new TextureInfo(hr, D3DNative.textureInfoHandle(info), D3DNative.textureInfoWidth(info),
                    D3DNative.textureInfoHeight(info), D3DNative.textureInfoIsDefaultPool(info));
        }
    }

    public static int resourceRelease(MemorySegment ctx, MemorySegment resource) {
        return D3DNative.resourceRelease(ctx, resource);
    }

    /** {@code {status, width, height}}; width and height stay 0 when the status is not 0. */
    public static int[] resourceGetSize(MemorySegment resource) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment sizeOut = arena.allocate(D3DNative.SIZE_LAYOUT);
            int status = D3DNative.resourceGetSize(resource, sizeOut);
            return new int[] {status, D3DNative.sizeWidth(sizeOut), D3DNative.sizeHeight(sizeOut)};
        }
    }

    public static boolean resourceIsDefaultPool(MemorySegment resource) {
        return D3DNative.resourceIsDefaultPool(resource);
    }

    /* ---------------------------------------------------------------------------------------------
     * 3D
     * ------------------------------------------------------------------------------------------- */

    public static MemorySegment meshCreate(MemorySegment ctx) {
        return D3DNative.meshCreate(ctx);
    }

    public static void meshRelease(MemorySegment mesh) {
        D3DNative.meshRelease(mesh);
    }

    public static MemorySegment materialCreate(MemorySegment ctx) {
        return D3DNative.materialCreate(ctx);
    }

    public static void materialRelease(MemorySegment material) {
        D3DNative.materialRelease(material);
    }

    public static MemorySegment meshviewCreate(MemorySegment ctx, MemorySegment mesh) {
        return D3DNative.meshviewCreate(ctx, mesh);
    }

    public static void meshviewRelease(MemorySegment meshView) {
        D3DNative.meshviewRelease(meshView);
    }

    public static void meshviewSetMaterial(MemorySegment meshView, MemorySegment material) {
        D3DNative.meshviewSetMaterial(meshView, material);
    }

    /** @param cullMode one of {@link #contextCullModes()} */
    public static void meshviewSetCullingMode(MemorySegment meshView, int cullMode) {
        D3DNative.meshviewSetCullingMode(meshView, cullMode);
    }

    public static void meshviewRender(MemorySegment meshView) {
        D3DNative.meshviewRender(meshView);
    }
}
