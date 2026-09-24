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

import com.sun.glass.utils.NativeLibLoader;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * The single point of contact between {@code com.sun.prism.d3d} and the {@code d3d_*} C ABI exported by
 * the {@code prism_d3d} library ({@code src/main/native-prism-d3d/prism_d3d_api.h}). It owns the library
 * load, the {@link SymbolLookup}, the {@link Linker}, every downcall handle and the three struct layouts,
 * and it is the only class in this package that uses a restricted {@code java.lang.foreign} method.
 * <p>
 * The peers hold their native handles - {@code D3DContext*}, {@code D3DResource*},
 * {@code D3DPixelShaderResource*}, {@code D3DMesh*}, {@code D3DPhongMaterial*}, {@code D3DMeshView*} -
 * as zero-length {@link MemorySegment}s that the C++ owns; {@link MemorySegment#NULL} stands where the
 * JNI code had {@code 0L}. The two values that come from Glass, the {@code HWND} of a presentable and the
 * {@code HMONITOR} of a screen, stay {@code long} because Glass still hands them out as such.
 * <p>
 * Status handling is unchanged from the JNI version: every function returns the raw {@code HRESULT} as
 * an {@code int} ({@code FAILED} is negative, {@code S_FALSE == 1} is a success), a {@code NULL} handle
 * where the JNI returned {@code 0L}, or {@code 1}/{@code 0} where it returned a {@code jboolean}. The
 * callers keep their {@code validate}/{@code setLost}/device-reset checks exactly as they were.
 * <p>
 * Threading is unchanged as well: every {@code d3d_context_*}, {@code d3d_texture_*}, {@code d3d_shader_*},
 * {@code d3d_mesh*}, {@code d3d_material_*} and {@code d3d_meshview_*} call runs on the Prism render
 * thread; the {@code d3d_pipeline_*} family runs on the thread that initialises {@link D3DPipeline}. Only
 * {@link #contextDrawIndexedQuads} and {@link #textureUpdate} are bound with
 * {@link Linker.Option#critical critical(true)} and pass Java heap arrays straight through, exactly where
 * the JNI code used {@code GetPrimitiveArrayCritical} without copy-back; every other function may block,
 * allocate GPU resources or run driver code, so it only ever receives off-heap memory - a direct buffer,
 * a per-context scratch segment owned by {@link D3DContext}, or a per-call confined arena.
 * <p>
 * Loading and binding follow {@code com.sun.media.jfxmediaimpl.JfxMediaNative}: the library is loaded in
 * the class initializer, {@code d3d_abi_version} is bound and checked first, every other symbol is bound
 * eagerly, and any failure - the library missing, a symbol missing, the module not named in
 * {@code --enable-native-access}, an ABI version mismatch - is recorded once and raised by
 * {@link #loadLibrary()} and by every downcall, so that {@link D3DPipeline} fails its own class
 * initialization with an {@link UnsatisfiedLinkError} just as it did when {@code NativeLibLoader} failed.
 */
final class D3DNative {

    /** The {@code d3d_*} ABI revision this class is written against ({@code PRISM_D3D_ABI_VERSION}). */
    static final int ABI_VERSION = 1;

    static final String LIBRARY_NAME = "prism_d3d";

    /* enum D3dInitFlags */
    static final int INIT_FORCE_GPU = 1;
    static final int INIT_VSYNC = 2;
    static final int INIT_VERBOSE = 4;

    /* enum D3dIndexType */
    static final int INDEX_16 = 16;
    static final int INDEX_32 = 32;

    /* The two enum D3dPixelFormat values the merged texture update needs for int[] and float[] pixels. */
    static final int PFORMAT_INT_ARGB_PRE = 0;
    static final int PFORMAT_FLOAT_XYZW = 7;

    /* HRESULTs this class returns itself, where the JNI code returned them before reaching D3D. */
    private static final int E_FAIL = 0x80004005;
    private static final int E_OUTOFMEMORY = 0x8007000E;

    /** Capacity of the buffer {@link #pipelineGetErrorMessage} passes; the C side keeps a 256-byte message. */
    private static final int ERROR_MESSAGE_CAPACITY = 256;

    /* ---------------------------------------------------------------------------------------------
     * Struct layouts - a test compares each byteSize() with the d3d_sizeof_* export
     * ------------------------------------------------------------------------------------------- */

    /** {@code D3dDriverInfo}: four NUL-terminated ANSI char arrays and 13 int32 fields, 1364 bytes. */
    static final StructLayout DRIVER_INFO_LAYOUT = MemoryLayout.structLayout(
            MemoryLayout.sequenceLayout(512, JAVA_BYTE).withName("device_description"),
            MemoryLayout.sequenceLayout(32, JAVA_BYTE).withName("device_name"),
            MemoryLayout.sequenceLayout(512, JAVA_BYTE).withName("driver_name"),
            MemoryLayout.sequenceLayout(256, JAVA_BYTE).withName("warning_message"),
            JAVA_INT.withName("vendor_id"),
            JAVA_INT.withName("device_id"),
            JAVA_INT.withName("subsys_id"),
            JAVA_INT.withName("product"),
            JAVA_INT.withName("version"),
            JAVA_INT.withName("sub_version"),
            JAVA_INT.withName("build_id"),
            JAVA_INT.withName("ps_version_major"),
            JAVA_INT.withName("ps_version_minor"),
            JAVA_INT.withName("max_samples"),
            JAVA_INT.withName("os_major"),
            JAVA_INT.withName("os_minor"),
            JAVA_INT.withName("os_build"));

    /** {@code D3dFrameStats}: eight int32 counters, 32 bytes. */
    static final StructLayout FRAME_STATS_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("num_triangles_drawn"),
            JAVA_INT.withName("num_draw_calls"),
            JAVA_INT.withName("num_buffer_locks"),
            JAVA_INT.withName("num_texture_locks"),
            JAVA_INT.withName("num_texture_transfer_bytes"),
            JAVA_INT.withName("num_set_texture"),
            JAVA_INT.withName("num_set_pixel_shader"),
            JAVA_INT.withName("num_render_target_switch"));

    /** {@code D3dTextureInfo}: the resource handle and its size, 24 bytes. */
    static final StructLayout TEXTURE_INFO_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("handle"),
            JAVA_INT.withName("width"),
            JAVA_INT.withName("height"),
            JAVA_INT.withName("is_default_pool"),
            JAVA_INT.withName("reserved"));

    /** The 16 doubles of a 4x4 matrix in {@code GeneralTransform3D.get(0..15)} order (row-major). */
    static final SequenceLayout MATRIX_LAYOUT = MemoryLayout.sequenceLayout(16, JAVA_DOUBLE);

    /** The 18 floats of {@code d3d_meshview_set_light}, in the former {@code nSetLight} argument order. */
    static final SequenceLayout LIGHT_LAYOUT = MemoryLayout.sequenceLayout(18, JAVA_FLOAT);

    /** The {@code width, height} out-params of {@code d3d_resource_get_size}. */
    static final SequenceLayout SIZE_LAYOUT = MemoryLayout.sequenceLayout(2, JAVA_INT);

    private static final long OFFSET_DEVICE_DESCRIPTION = driverInfoOffset("device_description");
    private static final long OFFSET_DEVICE_NAME = driverInfoOffset("device_name");
    private static final long OFFSET_DRIVER_NAME = driverInfoOffset("driver_name");
    private static final long OFFSET_WARNING_MESSAGE = driverInfoOffset("warning_message");
    private static final long OFFSET_VENDOR_ID = driverInfoOffset("vendor_id");
    private static final long OFFSET_DEVICE_ID = driverInfoOffset("device_id");
    private static final long OFFSET_SUBSYS_ID = driverInfoOffset("subsys_id");
    private static final long OFFSET_PRODUCT = driverInfoOffset("product");
    private static final long OFFSET_VERSION = driverInfoOffset("version");
    private static final long OFFSET_SUB_VERSION = driverInfoOffset("sub_version");
    private static final long OFFSET_BUILD_ID = driverInfoOffset("build_id");
    private static final long OFFSET_PS_VERSION_MAJOR = driverInfoOffset("ps_version_major");
    private static final long OFFSET_PS_VERSION_MINOR = driverInfoOffset("ps_version_minor");
    private static final long OFFSET_MAX_SAMPLES = driverInfoOffset("max_samples");
    private static final long OFFSET_OS_MAJOR = driverInfoOffset("os_major");
    private static final long OFFSET_OS_MINOR = driverInfoOffset("os_minor");
    private static final long OFFSET_OS_BUILD = driverInfoOffset("os_build");

    private static final long DEVICE_DESCRIPTION_BYTES = 512;
    private static final long DEVICE_NAME_BYTES = 32;
    private static final long DRIVER_NAME_BYTES = 512;
    private static final long WARNING_MESSAGE_BYTES = 256;

    private static final long OFFSET_NUM_TRIANGLES_DRAWN = frameStatsOffset("num_triangles_drawn");
    private static final long OFFSET_NUM_DRAW_CALLS = frameStatsOffset("num_draw_calls");
    private static final long OFFSET_NUM_BUFFER_LOCKS = frameStatsOffset("num_buffer_locks");
    private static final long OFFSET_NUM_TEXTURE_LOCKS = frameStatsOffset("num_texture_locks");
    private static final long OFFSET_NUM_TEXTURE_TRANSFER_BYTES = frameStatsOffset("num_texture_transfer_bytes");
    private static final long OFFSET_NUM_SET_TEXTURE = frameStatsOffset("num_set_texture");
    private static final long OFFSET_NUM_SET_PIXEL_SHADER = frameStatsOffset("num_set_pixel_shader");
    private static final long OFFSET_NUM_RENDER_TARGET_SWITCH = frameStatsOffset("num_render_target_switch");

    private static final long OFFSET_HANDLE = textureInfoOffset("handle");
    private static final long OFFSET_WIDTH = textureInfoOffset("width");
    private static final long OFFSET_HEIGHT = textureInfoOffset("height");
    private static final long OFFSET_IS_DEFAULT_POOL = textureInfoOffset("is_default_pool");

    private static long driverInfoOffset(String field) {
        return DRIVER_INFO_LAYOUT.byteOffset(PathElement.groupElement(field));
    }

    private static long frameStatsOffset(String field) {
        return FRAME_STATS_LAYOUT.byteOffset(PathElement.groupElement(field));
    }

    private static long textureInfoOffset(String field) {
        return TEXTURE_INFO_LAYOUT.byteOffset(PathElement.groupElement(field));
    }

    /* ---------------------------------------------------------------------------------------------
     * Library loading and binding
     * ------------------------------------------------------------------------------------------- */

    private static final Linker LINKER = Linker.nativeLinker();

    /** Names of every symbol this class binds, in binding order; read by the binding tests. */
    private static final List<String> BOUND_SYMBOLS = new ArrayList<>();

    /** Names of the bound symbols the library does not export; read by the binding tests. */
    private static final List<String> MISSING_SYMBOLS = new ArrayList<>();

    /** {@code name + " " + descriptor} of every bound symbol, in binding order; test support only. */
    private static final List<String> DESCRIPTORS = new ArrayList<>();

    /**
     * The failure that loading the library, resolving a symbol, being denied native access or checking
     * the ABI version ended in, or {@code null}. Raised by {@link #loadLibrary()} and by every downcall
     * bound after it happened.
     */
    private static UnsatisfiedLinkError initFailure;

    private static final SymbolLookup LOOKUP;

    static {
        SymbolLookup lookup = null;
        try {
            // The library has to be loaded by this class loader before loaderLookup() can see it.
            NativeLibLoader.loadLibrary(LIBRARY_NAME);
            lookup = SymbolLookup.loaderLookup();
        } catch (UnsatisfiedLinkError e) {
            initFailure = e;
        } catch (RuntimeException e) {
            // System::load is a restricted method too: a module left out of --enable-native-access sees
            // an IllegalCallerException from the loader before this class binds anything.
            initFailure = initializationFailed(e);
        }
        LOOKUP = lookup;
    }

    /* Binding order matters: the ABI guard is bound and checked before any other symbol. */
    private static final MethodHandle D3D_ABI_VERSION = bind("d3d_abi_version",
            FunctionDescriptor.of(JAVA_INT));

    static {
        checkAbiVersion();
    }

    private static final MethodHandle D3D_SIZEOF_DRIVER_INFO = bind("d3d_sizeof_driver_info",
            FunctionDescriptor.of(JAVA_LONG));
    private static final MethodHandle D3D_SIZEOF_FRAME_STATS = bind("d3d_sizeof_frame_stats",
            FunctionDescriptor.of(JAVA_LONG));
    private static final MethodHandle D3D_SIZEOF_TEXTURE_INFO = bind("d3d_sizeof_texture_info",
            FunctionDescriptor.of(JAVA_LONG));

    private static final MethodHandle D3D_PIPELINE_INIT = bind("d3d_pipeline_init",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT));
    private static final MethodHandle D3D_PIPELINE_GET_ERROR_MESSAGE = bind("d3d_pipeline_get_error_message",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle D3D_PIPELINE_DISPOSE = bind("d3d_pipeline_dispose",
            FunctionDescriptor.ofVoid());
    private static final MethodHandle D3D_PIPELINE_GET_ADAPTER_ORDINAL = bind("d3d_pipeline_get_adapter_ordinal",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
    private static final MethodHandle D3D_PIPELINE_GET_ADAPTER_COUNT = bind("d3d_pipeline_get_adapter_count",
            FunctionDescriptor.of(JAVA_INT));
    private static final MethodHandle D3D_PIPELINE_GET_DRIVER_INFORMATION =
            bind("d3d_pipeline_get_driver_information", FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS));
    private static final MethodHandle D3D_PIPELINE_GET_MAX_SAMPLE_SUPPORT =
            bind("d3d_pipeline_get_max_sample_support", FunctionDescriptor.of(JAVA_INT, JAVA_INT));

    private static final MethodHandle D3D_CONTEXT_GET = bind("d3d_context_get",
            FunctionDescriptor.of(ADDRESS, JAVA_INT));
    private static final MethodHandle D3D_CONTEXT_TEST_COOPERATIVE_LEVEL = bind("d3d_context_test_cooperative_level",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private static final MethodHandle D3D_CONTEXT_RESET_DEVICE = bind("d3d_context_reset_device",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private static final MethodHandle D3D_CONTEXT_GET_MAX_TEXTURE_SIZE = bind("d3d_context_get_max_texture_size",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private static final MethodHandle D3D_TEXTURE_CREATE = bind("d3d_texture_create",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, ADDRESS));
    private static final MethodHandle D3D_SWAPCHAIN_CREATE = bind("d3d_swapchain_create",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, JAVA_INT, ADDRESS));
    private static final MethodHandle D3D_RESOURCE_RELEASE = bind("d3d_resource_release",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
    private static final MethodHandle D3D_RESOURCE_GET_SIZE = bind("d3d_resource_get_size",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle D3D_RESOURCE_IS_DEFAULT_POOL = bind("d3d_resource_is_default_pool",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private static final MethodHandle D3D_TEXTURE_UPDATE = bind("d3d_texture_update",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT),
            Linker.Option.critical(true));
    private static final MethodHandle D3D_TEXTURE_READ_PIXELS = bind("d3d_texture_read_pixels",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, JAVA_INT, JAVA_INT));

    private static final MethodHandle D3D_SHADER_CREATE = bind("d3d_shader_create",
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG));
    private static final MethodHandle D3D_SHADER_ENABLE = bind("d3d_shader_enable",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
    private static final MethodHandle D3D_SHADER_DISABLE = bind("d3d_shader_disable",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private static final MethodHandle D3D_SHADER_SET_CONSTANTS_F = bind("d3d_shader_set_constants_f",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));

    private static final MethodHandle D3D_SWAPCHAIN_PRESENT = bind("d3d_swapchain_present",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
    private static final MethodHandle D3D_CONTEXT_GET_FRAME_STATS = bind("d3d_context_get_frame_stats",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
    private static final MethodHandle D3D_CONTEXT_DRAW_INDEXED_QUADS = bind("d3d_context_draw_indexed_quads",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT),
            Linker.Option.critical(true));
    private static final MethodHandle D3D_CONTEXT_CLEAR = bind("d3d_context_clear",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle D3D_CONTEXT_SET_BLEND_MODE = bind("d3d_context_set_blend_mode",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle D3D_CONTEXT_SET_RENDER_TARGET = bind("d3d_context_set_render_target",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT));
    private static final MethodHandle D3D_CONTEXT_SET_TEXTURE = bind("d3d_context_set_texture",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle D3D_CONTEXT_SET_CAMERA_POSITION = bind("d3d_context_set_camera_position",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE));
    private static final MethodHandle D3D_CONTEXT_SET_PROJ_VIEW_MATRIX = bind("d3d_context_set_proj_view_matrix",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle D3D_CONTEXT_SET_TRANSFORM = bind("d3d_context_set_transform",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
    private static final MethodHandle D3D_CONTEXT_RESET_TRANSFORM = bind("d3d_context_reset_transform",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private static final MethodHandle D3D_CONTEXT_SET_WORLD_TRANSFORM = bind("d3d_context_set_world_transform",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
    private static final MethodHandle D3D_CONTEXT_SET_CLIP_RECT = bind("d3d_context_set_clip_rect",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle D3D_CONTEXT_RESET_CLIP_RECT = bind("d3d_context_reset_clip_rect",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private static final MethodHandle D3D_CONTEXT_SET_DEVICE_PARAMETERS_2D =
            bind("d3d_context_set_device_parameters_2d", FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private static final MethodHandle D3D_CONTEXT_SET_DEVICE_PARAMETERS_3D =
            bind("d3d_context_set_device_parameters_3d", FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private static final MethodHandle D3D_CONTEXT_BLIT = bind("d3d_context_blit",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_INT, JAVA_INT));

    private static final MethodHandle D3D_MESH_CREATE = bind("d3d_mesh_create",
            FunctionDescriptor.of(ADDRESS, ADDRESS));
    private static final MethodHandle D3D_MESH_RELEASE = bind("d3d_mesh_release",
            FunctionDescriptor.ofVoid(ADDRESS));
    private static final MethodHandle D3D_MESH_BUILD_GEOMETRY = bind("d3d_mesh_build_geometry",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));
    private static final MethodHandle D3D_MATERIAL_CREATE = bind("d3d_material_create",
            FunctionDescriptor.of(ADDRESS, ADDRESS));
    private static final MethodHandle D3D_MATERIAL_RELEASE = bind("d3d_material_release",
            FunctionDescriptor.ofVoid(ADDRESS));
    private static final MethodHandle D3D_MATERIAL_SET_DIFFUSE_COLOR = bind("d3d_material_set_diffuse_color",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT));
    private static final MethodHandle D3D_MATERIAL_SET_SPECULAR_COLOR = bind("d3d_material_set_specular_color",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT));
    private static final MethodHandle D3D_MATERIAL_SET_MAP = bind("d3d_material_set_map",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle D3D_MESHVIEW_CREATE = bind("d3d_meshview_create",
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle D3D_MESHVIEW_RELEASE = bind("d3d_meshview_release",
            FunctionDescriptor.ofVoid(ADDRESS));
    private static final MethodHandle D3D_MESHVIEW_SET_CULLING_MODE = bind("d3d_meshview_set_culling_mode",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
    private static final MethodHandle D3D_MESHVIEW_SET_MATERIAL = bind("d3d_meshview_set_material",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
    private static final MethodHandle D3D_MESHVIEW_SET_WIREFRAME = bind("d3d_meshview_set_wireframe",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
    private static final MethodHandle D3D_MESHVIEW_SET_AMBIENT_LIGHT = bind("d3d_meshview_set_ambient_light",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT));
    private static final MethodHandle D3D_MESHVIEW_SET_LIGHT = bind("d3d_meshview_set_light",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle D3D_MESHVIEW_RENDER = bind("d3d_meshview_render",
            FunctionDescriptor.ofVoid(ADDRESS));

    private D3DNative() {
    }

    /**
     * Binds an exported {@code d3d_*} symbol. When the library could not be loaded, a symbol is missing,
     * native access was denied or the ABI version does not match, the returned handle has the requested
     * type and throws the recorded {@link UnsatisfiedLinkError} when invoked, so the failure is reported
     * once, by name, from {@link #loadLibrary()} and from the first call.
     */
    @SuppressWarnings("restricted")
    private static MethodHandle bind(String name, FunctionDescriptor descriptor, Linker.Option... options) {
        BOUND_SYMBOLS.add(name);
        DESCRIPTORS.add(name + " " + descriptor);
        if (LOOKUP != null) {
            Optional<MemorySegment> symbol = LOOKUP.find(name);
            if (symbol.isEmpty()) {
                MISSING_SYMBOLS.add(name);
                if (initFailure == null) {
                    initFailure = new UnsatisfiedLinkError("missing native symbol: " + name + " in " + LIBRARY_NAME);
                }
            } else if (initFailure == null) {
                try {
                    return LINKER.downcallHandle(symbol.get(), descriptor, options);
                } catch (IllegalCallerException e) {
                    // Restricted; caught here because this runs from a static field initializer and no
                    // field initializer of this class may be the one that throws.
                    initFailure = initializationFailed(e);
                }
            }
        }
        return failingHandle(descriptor, name);
    }

    private static UnsatisfiedLinkError initializationFailed(RuntimeException e) {
        UnsatisfiedLinkError error = new UnsatisfiedLinkError(e instanceof IllegalCallerException
                ? LIBRARY_NAME + " was not granted native access: add javafx.graphics - or ALL-UNNAMED, when it"
                        + " is on the class path - to --enable-native-access"
                : LIBRARY_NAME + " could not be initialized: " + e);
        error.initCause(e);
        return error;
    }

    private static MethodHandle failingHandle(FunctionDescriptor descriptor, String name) {
        MethodType type = descriptor.toMethodType();
        UnsatisfiedLinkError error = initFailure != null
                ? initFailure : new UnsatisfiedLinkError("missing native symbol: " + name);
        MethodHandle thrower = MethodHandles.insertArguments(
                MethodHandles.throwException(type.returnType(), UnsatisfiedLinkError.class), 0, error);
        return MethodHandles.dropArguments(thrower, 0, type.parameterList());
    }

    private static void checkAbiVersion() {
        if (initFailure != null) {
            return;
        }
        int actual;
        try {
            actual = (int) D3D_ABI_VERSION.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
        if (actual != ABI_VERSION) {
            initFailure = new UnsatisfiedLinkError(LIBRARY_NAME + " ABI version mismatch: expected " + ABI_VERSION
                    + ", found " + actual);
        }
    }

    private static Error unexpected(Throwable t) {
        if (t instanceof Error e) {
            return e;
        }
        if (t instanceof RuntimeException e) {
            throw e;
        }
        // Only linkage failures can land here: C cannot throw.
        return new AssertionError(t);
    }

    private static int flag(boolean value) {
        return value ? 1 : 0;
    }

    /**
     * The address of a direct buffer's whole backing memory, as {@code GetDirectBufferAddress} and
     * {@code GetDirectBufferCapacity} saw it: from the start of the buffer, independent of its position
     * and limit, {@code capacity} elements long. {@link MemorySegment#NULL} for a buffer that is not
     * direct - the JNI got a {@code NULL} address there and reported it, and so does the C side.
     */
    private static MemorySegment directSegment(Buffer buffer) {
        if (!buffer.isDirect()) {
            return MemorySegment.NULL;
        }
        return MemorySegment.ofBuffer(buffer.duplicate().clear());
    }

    /** A NUL-terminated string field of a struct, read within the bounds of its {@code char} array. */
    private static String cString(MemorySegment struct, long offset, long capacity) {
        return struct.asSlice(offset, capacity).getString(0);
    }

    /* ---------------------------------------------------------------------------------------------
     * Loading and test hooks (reached through D3DNativeShim)
     * ------------------------------------------------------------------------------------------- */

    /**
     * Makes sure the library is loaded, every {@code d3d_*} symbol is bound and the ABI version matches.
     * {@link D3DPipeline} calls this before anything else, so its class initialization fails with the
     * same {@link UnsatisfiedLinkError} it used to get from {@code NativeLibLoader}.
     *
     * @throws UnsatisfiedLinkError if the library cannot be loaded, lacks a symbol the facade binds, was
     *         denied native access, or reports another ABI version
     */
    static void loadLibrary() {
        if (initFailure != null) {
            throw initFailure;
        }
    }

    /** The symbols this class bound, in binding order. */
    static List<String> boundSymbols() {
        return Collections.unmodifiableList(new ArrayList<>(BOUND_SYMBOLS));
    }

    /**
     * Every bound symbol with the {@link FunctionDescriptor} it was bound with, as
     * {@code name + " " + descriptor}, in binding order: the Java half of the ABI, which the descriptor
     * snapshot test holds against a hand transcription of {@code prism_d3d_api.h}. Built whether or not the
     * library loaded. Test support only.
     */
    static List<String> descriptors() {
        return Collections.unmodifiableList(new ArrayList<>(DESCRIPTORS));
    }

    /** The bound symbols the loaded library does not export; empty for a correct build. */
    static List<String> missingSymbols() {
        return Collections.unmodifiableList(new ArrayList<>(MISSING_SYMBOLS));
    }

    static int abiVersion() {
        try {
            return (int) D3D_ABI_VERSION.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static long sizeofDriverInfo() {
        try {
            return (long) D3D_SIZEOF_DRIVER_INFO.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static long sizeofFrameStats() {
        try {
            return (long) D3D_SIZEOF_FRAME_STATS.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static long sizeofTextureInfo() {
        try {
            return (long) D3D_SIZEOF_TEXTURE_INFO.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Pipeline (D3DPipeline) - runs on the thread that initialises D3DPipeline
     * ------------------------------------------------------------------------------------------- */

    /** Composes the {@code D3dInitFlags} from the three {@code PrismSettings} the C side used to read. */
    static int initFlags(boolean forceGPU, boolean isVsyncEnabled, boolean verbose) {
        return (forceGPU ? INIT_FORCE_GPU : 0) | (isVsyncEnabled ? INIT_VSYNC : 0) | (verbose ? INIT_VERBOSE : 0);
    }

    /** {@code true} when the pipeline came up; otherwise {@link #pipelineGetErrorMessage} has the reason. */
    static boolean pipelineInit(int flags) {
        try {
            return (int) D3D_PIPELINE_INIT.invokeExact(flags) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** The pipeline's error message, or {@code null} when there is none - as {@code nGetErrorMessage}. */
    static String pipelineGetErrorMessage() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buffer = arena.allocate(ERROR_MESSAGE_CAPACITY);
            int length;
            try {
                length = (int) D3D_PIPELINE_GET_ERROR_MESSAGE.invokeExact(buffer, ERROR_MESSAGE_CAPACITY);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            return length == 0 ? null : buffer.getString(0);
        }
    }

    /** Releases every context and the D3D object; {@link #pipelineInit} may be called again afterwards. */
    static void pipelineDispose() {
        try {
            D3D_PIPELINE_DISPOSE.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** @param hMonitor the {@code HMONITOR} from {@code Screen.getNativeScreen()} */
    static int pipelineGetAdapterOrdinal(long hMonitor) {
        try {
            return (int) D3D_PIPELINE_GET_ADAPTER_ORDINAL.invokeExact(hMonitor);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int pipelineGetAdapterCount() {
        try {
            return (int) D3D_PIPELINE_GET_ADAPTER_COUNT.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * The driver, device and OS information of an adapter, or {@code null} when the adapter does not
     * exist or D3D cannot describe it - the cases in which {@code nGetDriverInformation} returned
     * {@code null}. An empty warning message becomes {@code null}, as the JNI left the field.
     */
    static D3DDriverInformation pipelineGetDriverInformation(int adapter) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(DRIVER_INFO_LAYOUT);
            int filled;
            try {
                filled = (int) D3D_PIPELINE_GET_DRIVER_INFORMATION.invokeExact(adapter, info);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            if (filled == 0) {
                return null;
            }
            D3DDriverInformation di = new D3DDriverInformation();
            di.deviceDescription = cString(info, OFFSET_DEVICE_DESCRIPTION, DEVICE_DESCRIPTION_BYTES);
            di.deviceName = cString(info, OFFSET_DEVICE_NAME, DEVICE_NAME_BYTES);
            di.driverName = cString(info, OFFSET_DRIVER_NAME, DRIVER_NAME_BYTES);
            String warning = cString(info, OFFSET_WARNING_MESSAGE, WARNING_MESSAGE_BYTES);
            di.warningMessage = warning.isEmpty() ? null : warning;
            di.vendorID = info.get(JAVA_INT, OFFSET_VENDOR_ID);
            di.deviceID = info.get(JAVA_INT, OFFSET_DEVICE_ID);
            di.subSysId = info.get(JAVA_INT, OFFSET_SUBSYS_ID);
            di.product = info.get(JAVA_INT, OFFSET_PRODUCT);
            di.version = info.get(JAVA_INT, OFFSET_VERSION);
            di.subVersion = info.get(JAVA_INT, OFFSET_SUB_VERSION);
            di.buildID = info.get(JAVA_INT, OFFSET_BUILD_ID);
            di.psVersionMajor = info.get(JAVA_INT, OFFSET_PS_VERSION_MAJOR);
            di.psVersionMinor = info.get(JAVA_INT, OFFSET_PS_VERSION_MINOR);
            di.maxSamples = info.get(JAVA_INT, OFFSET_MAX_SAMPLES);
            di.osMajorVersion = info.get(JAVA_INT, OFFSET_OS_MAJOR);
            di.osMinorVersion = info.get(JAVA_INT, OFFSET_OS_MINOR);
            di.osBuildNumber = info.get(JAVA_INT, OFFSET_OS_BUILD);
            return di;
        }
    }

    static int pipelineGetMaxSampleSupport(int adapter) {
        try {
            return (int) D3D_PIPELINE_GET_MAX_SAMPLE_SUPPORT.invokeExact(adapter);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Context and resources (D3DResourceFactory, D3DResource, D3DTexture, D3DRTTexture)
     * ------------------------------------------------------------------------------------------- */

    /** The {@code D3DContext*} of an adapter, creating the device on first use; {@code NULL} on failure. */
    static MemorySegment contextGet(int adapter) {
        try {
            return (MemorySegment) D3D_CONTEXT_GET.invokeExact(adapter);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** The raw {@code CheckDeviceState} result that drives the device-lost state machine. */
    static int contextTestCooperativeLevel(MemorySegment ctx) {
        try {
            return (int) D3D_CONTEXT_TEST_COOPERATIVE_LEVEL.invokeExact(ctx);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int contextResetDevice(MemorySegment ctx) {
        try {
            return (int) D3D_CONTEXT_RESET_DEVICE.invokeExact(ctx);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int contextGetMaxTextureSize(MemorySegment ctx) {
        try {
            return (int) D3D_CONTEXT_GET_MAX_TEXTURE_SIZE.invokeExact(ctx);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Creates a texture or render target and describes it in {@code infoOut}, a segment of
     * {@link #TEXTURE_INFO_LAYOUT}. Returns the HRESULT of the D3D create call; callers key on
     * {@link #textureInfoHandle} being non-null, exactly as they keyed on {@code nCreateTexture != 0L}.
     *
     * @param format the {@code PixelFormat} ordinal
     * @param usage the {@code Texture.Usage} ordinal
     */
    static int textureCreate(MemorySegment ctx, int format, int usage, boolean isRTT, int width, int height,
            int samples, boolean useMipmap, MemorySegment infoOut) {
        try {
            return (int) D3D_TEXTURE_CREATE.invokeExact(ctx, format, usage, flag(isRTT), width, height, samples,
                    flag(useMipmap), infoOut);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Creates a swap chain for a window and describes it in {@code infoOut}; see {@link #textureCreate}.
     *
     * @param hwnd the {@code HWND} from {@code PresentableState.getNativeView()}
     */
    static int swapchainCreate(MemorySegment ctx, long hwnd, boolean isVsyncEnabled, MemorySegment infoOut) {
        try {
            return (int) D3D_SWAPCHAIN_CREATE.invokeExact(ctx, hwnd, flag(isVsyncEnabled), infoOut);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** The {@code D3DResource*} a create call wrote, or {@link MemorySegment#NULL} when it failed. */
    static MemorySegment textureInfoHandle(MemorySegment info) {
        return info.get(ADDRESS, OFFSET_HANDLE);
    }

    static int textureInfoWidth(MemorySegment info) {
        return info.get(JAVA_INT, OFFSET_WIDTH);
    }

    static int textureInfoHeight(MemorySegment info) {
        return info.get(JAVA_INT, OFFSET_HEIGHT);
    }

    static boolean textureInfoIsDefaultPool(MemorySegment info) {
        return info.get(JAVA_INT, OFFSET_IS_DEFAULT_POOL) != 0;
    }

    /** {@code D3D_OK} for a {@code NULL} resource, {@code S_FALSE} for a {@code NULL} context. */
    static int resourceRelease(MemorySegment ctx, MemorySegment resource) {
        try {
            return (int) D3D_RESOURCE_RELEASE.invokeExact(ctx, resource);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Writes the resource's surface size into {@code sizeOut}, a segment of {@link #SIZE_LAYOUT}, and
     * returns 0; returns -1 for a {@code NULL} resource without writing, which is what
     * {@code nGetTextureWidth}/{@code nGetTextureHeight} answered for one.
     */
    static int resourceGetSize(MemorySegment resource, MemorySegment sizeOut) {
        try {
            return (int) D3D_RESOURCE_GET_SIZE.invokeExact(resource, sizeOut,
                    sizeOut.asSlice(JAVA_INT.byteSize(), JAVA_INT.byteSize()));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int sizeWidth(MemorySegment sizeOut) {
        return sizeOut.getAtIndex(JAVA_INT, 0);
    }

    static int sizeHeight(MemorySegment sizeOut) {
        return sizeOut.getAtIndex(JAVA_INT, 1);
    }

    static boolean resourceIsDefaultPool(MemorySegment resource) {
        try {
            return (int) D3D_RESOURCE_IS_DEFAULT_POOL.invokeExact(resource) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Uploads {@code INT_ARGB_PRE} pixels - the former {@code nUpdateTextureI}. {@code pixels} is the
     * buffer's backing array when it has one and is pinned for the duration of the call; otherwise the
     * direct buffer's memory is used from its start, position and limit ignored, as the JNI did.
     */
    static int textureUpdate(MemorySegment ctx, MemorySegment resource, IntBuffer buf, int[] pixels,
            int dstx, int dsty, int srcx, int srcy, int srcw, int srch, int srcscan) {
        MemorySegment segment = pixels != null ? MemorySegment.ofArray(pixels) : directSegment(buf);
        return textureUpdate(ctx, resource, segment, PFORMAT_INT_ARGB_PRE, dstx, dsty, srcx, srcy, srcw, srch,
                srcscan);
    }

    /** Uploads {@code FLOAT_XYZW} pixels - the former {@code nUpdateTextureF}; see {@link #textureUpdate}. */
    static int textureUpdate(MemorySegment ctx, MemorySegment resource, FloatBuffer buf, float[] pixels,
            int dstx, int dsty, int srcx, int srcy, int srcw, int srch, int srcscan) {
        MemorySegment segment = pixels != null ? MemorySegment.ofArray(pixels) : directSegment(buf);
        return textureUpdate(ctx, resource, segment, PFORMAT_FLOAT_XYZW, dstx, dsty, srcx, srcy, srcw, srch,
                srcscan);
    }

    /**
     * Uploads byte pixels of {@code format}, a {@code PixelFormat} ordinal - the former
     * {@code nUpdateTextureB}; see {@link #textureUpdate}.
     */
    static int textureUpdate(MemorySegment ctx, MemorySegment resource, ByteBuffer buf, byte[] pixels, int format,
            int dstx, int dsty, int srcx, int srcy, int srcw, int srch, int srcscan) {
        MemorySegment segment = pixels != null ? MemorySegment.ofArray(pixels) : directSegment(buf);
        return textureUpdate(ctx, resource, segment, format, dstx, dsty, srcx, srcy, srcw, srch, srcscan);
    }

    private static int textureUpdate(MemorySegment ctx, MemorySegment resource, MemorySegment pixels, int format,
            int dstx, int dsty, int srcx, int srcy, int srcw, int srch, int srcscan) {
        try {
            return (int) D3D_TEXTURE_UPDATE.invokeExact(ctx, resource, pixels, pixels.byteSize(), format,
                    dstx, dsty, srcx, srcy, srcw, srch, srcscan);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Reads the {@code width x height} content of a render target back into {@code buf} - the former
     * {@code nReadPixelsB}. {@code length} is the byte count the caller derived from the buffer's capacity
     * and is what the C side checks against {@code width * height * 4}. A direct buffer is written in
     * place; a heap array goes through an off-heap copy, prefilled from the array and copied back whole,
     * so the array ends exactly as the JNI copy-back left it. {@code GetRenderTargetData} stalls the GPU,
     * so the array is never pinned.
     */
    static int textureReadPixels(MemorySegment ctx, MemorySegment resource, ByteBuffer buf, byte[] pixels,
            long length, int width, int height) {
        if (pixels == null) {
            return textureReadPixels(ctx, resource, directSegment(buf), length, width, height);
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment scratch = arena.allocate(length);
            int count = (int) Math.min(pixels.length, length);
            MemorySegment.copy(pixels, 0, scratch, JAVA_BYTE, 0, count);
            int res = textureReadPixels(ctx, resource, scratch, length, width, height);
            MemorySegment.copy(scratch, JAVA_BYTE, 0, pixels, 0, count);
            return res;
        }
    }

    /** The {@code int[]} form of {@link #textureReadPixels} - the former {@code nReadPixelsI}. */
    static int textureReadPixels(MemorySegment ctx, MemorySegment resource, IntBuffer buf, int[] pixels,
            long length, int width, int height) {
        if (pixels == null) {
            return textureReadPixels(ctx, resource, directSegment(buf), length, width, height);
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment scratch = arena.allocate(length);
            int count = (int) Math.min(pixels.length, length / JAVA_INT.byteSize());
            MemorySegment.copy(pixels, 0, scratch, JAVA_INT, 0, count);
            int res = textureReadPixels(ctx, resource, scratch, length, width, height);
            MemorySegment.copy(scratch, JAVA_INT, 0, pixels, 0, count);
            return res;
        }
    }

    private static int textureReadPixels(MemorySegment ctx, MemorySegment resource, MemorySegment dst, long length,
            int width, int height) {
        try {
            return (int) D3D_TEXTURE_READ_PIXELS.invokeExact(ctx, resource, dst, length, width, height);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Pixel shaders (D3DShader)
     * ------------------------------------------------------------------------------------------- */

    /**
     * Creates a pixel shader from the first {@code length} bytes of {@code bytecode}, an fxc token stream
     * that D3D parses synchronously and never retains; it is copied into a per-call arena for the
     * duration. {@code NULL} on failure, as {@code D3DShader.init} returned {@code 0L}.
     */
    static MemorySegment shaderCreate(MemorySegment ctx, byte[] bytecode, int length) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(length);
            MemorySegment.copy(bytecode, 0, segment, JAVA_BYTE, 0, length);
            try {
                return (MemorySegment) D3D_SHADER_CREATE.invokeExact(ctx, segment, (long) length);
            } catch (Throwable t) {
                throw unexpected(t);
            }
        }
    }

    static int shaderEnable(MemorySegment ctx, MemorySegment shader) {
        try {
            return (int) D3D_SHADER_ENABLE.invokeExact(ctx, shader);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int shaderDisable(MemorySegment ctx) {
        try {
            return (int) D3D_SHADER_DISABLE.invokeExact(ctx);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Sets {@code count} float4 registers from a direct buffer, starting {@code off} registers into it.
     * The bounds check is the one the JNI made against the buffer's capacity, and the offset is applied
     * the way the JNI applied it ({@code off} registers of four floats); every caller in the tree passes
     * {@code off == 0}. A buffer that is not direct is rejected with {@code E_FAIL}, as the JNI rejected
     * the {@code NULL} address it got for one.
     */
    static int shaderSetConstantsF(MemorySegment ctx, int register, FloatBuffer buf, int off, int count) {
        if (off < 0 || count < 1 || off + count > buf.capacity() / 4) {
            return E_FAIL;
        }
        if (!buf.isDirect()) {
            return E_FAIL;
        }
        MemorySegment values = directSegment(buf).asSlice(off * 4L * JAVA_FLOAT.byteSize());
        try {
            return (int) D3D_SHADER_SET_CONSTANTS_F.invokeExact(ctx, register, values, count);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * 2D context state (D3DContext, D3DGraphics, D3DSwapChain)
     * ------------------------------------------------------------------------------------------- */

    /** Presents a swap chain; blocks on vsync when the pipeline was initialised with it. */
    static int swapchainPresent(MemorySegment ctx, MemorySegment swapChain) {
        try {
            return (int) D3D_SWAPCHAIN_PRESENT.invokeExact(ctx, swapChain);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Copies the context's frame counters into {@code result} through {@code statsOut}, a segment of
     * {@link #FRAME_STATS_LAYOUT}, resetting them when {@code reset} is set. {@code false} when the
     * library was built without {@code PERF_COUNTERS}, as {@code nGetFrameStats} answered.
     */
    static boolean contextGetFrameStats(MemorySegment ctx, MemorySegment statsOut, boolean reset,
            D3DFrameStats result) {
        int filled;
        try {
            filled = (int) D3D_CONTEXT_GET_FRAME_STATS.invokeExact(ctx, statsOut, flag(reset));
        } catch (Throwable t) {
            throw unexpected(t);
        }
        if (filled == 0) {
            return false;
        }
        result.numTrianglesDrawn = statsOut.get(JAVA_INT, OFFSET_NUM_TRIANGLES_DRAWN);
        result.numDrawCalls = statsOut.get(JAVA_INT, OFFSET_NUM_DRAW_CALLS);
        result.numBufferLocks = statsOut.get(JAVA_INT, OFFSET_NUM_BUFFER_LOCKS);
        result.numTextureLocks = statsOut.get(JAVA_INT, OFFSET_NUM_TEXTURE_LOCKS);
        result.numTextureTransferBytes = statsOut.get(JAVA_INT, OFFSET_NUM_TEXTURE_TRANSFER_BYTES);
        result.numSetTexture = statsOut.get(JAVA_INT, OFFSET_NUM_SET_TEXTURE);
        result.numSetPixelShader = statsOut.get(JAVA_INT, OFFSET_NUM_SET_PIXEL_SHADER);
        result.numRenderTargetSwitch = statsOut.get(JAVA_INT, OFFSET_NUM_RENDER_TARGET_SWITCH);
        return true;
    }

    /**
     * Draws the vertex batch: {@code coords} holds {@code numVertices * 7} floats, {@code colors}
     * {@code numVertices * 4} bytes. Both heap arrays are pinned for the call, which only locks the
     * ring vertex buffer with {@code DISCARD}/{@code NOOVERWRITE} and queues the draw - the one hot path
     * the JNI pinned the same way.
     */
    static int contextDrawIndexedQuads(MemorySegment ctx, float[] coords, byte[] colors, int numVertices) {
        try {
            return (int) D3D_CONTEXT_DRAW_INDEXED_QUADS.invokeExact(ctx, MemorySegment.ofArray(coords),
                    MemorySegment.ofArray(colors), numVertices);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int contextClear(MemorySegment ctx, int colorArgbPre, boolean clearDepth, boolean ignoreScissor) {
        try {
            return (int) D3D_CONTEXT_CLEAR.invokeExact(ctx, colorArgbPre, flag(clearDepth), flag(ignoreScissor));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** @param compMode one of {@code D3DContext.D3DCOMPMODE_*} */
    static int contextSetBlendMode(MemorySegment ctx, int compMode) {
        try {
            return (int) D3D_CONTEXT_SET_BLEND_MODE.invokeExact(ctx, compMode);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code S_FALSE} (1) when the target was already current; the caller resets its clip on {@code D3D_OK} only. */
    static int contextSetRenderTarget(MemorySegment ctx, MemorySegment target, boolean depthBuffer, boolean msaa) {
        try {
            return (int) D3D_CONTEXT_SET_RENDER_TARGET.invokeExact(ctx, target, flag(depthBuffer), flag(msaa));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** @param wrapMode one of {@code D3DContext.D3DTADDRESS_*}; {@code NOP} leaves the address mode alone */
    static int contextSetTexture(MemorySegment ctx, MemorySegment texture, int texUnit, boolean linear,
            int wrapMode) {
        try {
            return (int) D3D_CONTEXT_SET_TEXTURE.invokeExact(ctx, texture, texUnit, flag(linear), wrapMode);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int contextSetCameraPosition(MemorySegment ctx, double x, double y, double z) {
        try {
            return (int) D3D_CONTEXT_SET_CAMERA_POSITION.invokeExact(ctx, x, y, z);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** @param m16 a segment of {@link #MATRIX_LAYOUT} in {@code GeneralTransform3D.get(0..15)} order */
    static int contextSetProjViewMatrix(MemorySegment ctx, boolean depthTest, MemorySegment m16) {
        try {
            return (int) D3D_CONTEXT_SET_PROJ_VIEW_MATRIX.invokeExact(ctx, flag(depthTest), m16);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int contextSetTransform(MemorySegment ctx, MemorySegment m16) {
        try {
            return (int) D3D_CONTEXT_SET_TRANSFORM.invokeExact(ctx, m16);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int contextResetTransform(MemorySegment ctx) {
        try {
            return (int) D3D_CONTEXT_RESET_TRANSFORM.invokeExact(ctx);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** @param m16OrNull a matrix segment, or {@link MemorySegment#NULL} for the identity */
    static void contextSetWorldTransform(MemorySegment ctx, MemorySegment m16OrNull) {
        try {
            D3D_CONTEXT_SET_WORLD_TRANSFORM.invokeExact(ctx, m16OrNull);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int contextSetClipRect(MemorySegment ctx, int x1, int y1, int x2, int y2) {
        try {
            return (int) D3D_CONTEXT_SET_CLIP_RECT.invokeExact(ctx, x1, y1, x2, y2);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int contextResetClipRect(MemorySegment ctx) {
        try {
            return (int) D3D_CONTEXT_RESET_CLIP_RECT.invokeExact(ctx);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int contextSetDeviceParameters2D(MemorySegment ctx) {
        try {
            return (int) D3D_CONTEXT_SET_DEVICE_PARAMETERS_2D.invokeExact(ctx);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** Creates the phong shaders on first call, so it may take a while. */
    static int contextSetDeviceParameters3D(MemorySegment ctx) {
        try {
            return (int) D3D_CONTEXT_SET_DEVICE_PARAMETERS_3D.invokeExact(ctx);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** @param dstOrNull the destination resource, or {@link MemorySegment#NULL} for the current render target */
    static void contextBlit(MemorySegment ctx, MemorySegment src, MemorySegment dstOrNull,
            int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1) {
        try {
            D3D_CONTEXT_BLIT.invokeExact(ctx, src, dstOrNull, srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * 3D (D3DMesh, D3DPhongMaterial, D3DMeshView)
     * ------------------------------------------------------------------------------------------- */

    static MemorySegment meshCreate(MemorySegment ctx) {
        try {
            return (MemorySegment) D3D_MESH_CREATE.invokeExact(ctx);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void meshRelease(MemorySegment mesh) {
        try {
            D3D_MESH_RELEASE.invokeExact(mesh);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Uploads {@code vertexBufferLength} floats and {@code indexBufferLength} 16-bit indices - the former
     * {@code nBuildNativeGeometryShort}. The length checks are the JNI's; the arrays are copied off-heap
     * for the call because the vertex buffer {@code Lock} may wait for the GPU.
     */
    static boolean meshBuildGeometry(MemorySegment mesh, float[] vertexBuffer, int vertexBufferLength,
            short[] indexBuffer, int indexBufferLength) {
        if (vertexBufferLength < 0 || indexBufferLength < 0
                || vertexBufferLength > vertexBuffer.length || indexBufferLength > indexBuffer.length) {
            return false;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment vb = arena.allocate(JAVA_FLOAT, vertexBufferLength);
            MemorySegment.copy(vertexBuffer, 0, vb, JAVA_FLOAT, 0, vertexBufferLength);
            MemorySegment ib = arena.allocate(JAVA_SHORT, indexBufferLength);
            MemorySegment.copy(indexBuffer, 0, ib, JAVA_SHORT, 0, indexBufferLength);
            return meshBuildGeometry(mesh, vb, vertexBufferLength, ib, indexBufferLength, INDEX_16);
        }
    }

    /** The 32-bit index form of {@link #meshBuildGeometry} - the former {@code nBuildNativeGeometryInt}. */
    static boolean meshBuildGeometry(MemorySegment mesh, float[] vertexBuffer, int vertexBufferLength,
            int[] indexBuffer, int indexBufferLength) {
        if (vertexBufferLength < 0 || indexBufferLength < 0
                || vertexBufferLength > vertexBuffer.length || indexBufferLength > indexBuffer.length) {
            return false;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment vb = arena.allocate(JAVA_FLOAT, vertexBufferLength);
            MemorySegment.copy(vertexBuffer, 0, vb, JAVA_FLOAT, 0, vertexBufferLength);
            MemorySegment ib = arena.allocate(JAVA_INT, indexBufferLength);
            MemorySegment.copy(indexBuffer, 0, ib, JAVA_INT, 0, indexBufferLength);
            return meshBuildGeometry(mesh, vb, vertexBufferLength, ib, indexBufferLength, INDEX_32);
        }
    }

    private static boolean meshBuildGeometry(MemorySegment mesh, MemorySegment vb, int vbLen, MemorySegment ib,
            int ibLen, int indexType) {
        try {
            return (int) D3D_MESH_BUILD_GEOMETRY.invokeExact(mesh, vb, vbLen, ib, ibLen, indexType) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static MemorySegment materialCreate(MemorySegment ctx) {
        try {
            return (MemorySegment) D3D_MATERIAL_CREATE.invokeExact(ctx);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void materialRelease(MemorySegment material) {
        try {
            D3D_MATERIAL_RELEASE.invokeExact(material);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void materialSetDiffuseColor(MemorySegment material, float r, float g, float b, float a) {
        try {
            D3D_MATERIAL_SET_DIFFUSE_COLOR.invokeExact(material, r, g, b, a);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void materialSetSpecularColor(MemorySegment material, boolean set, float r, float g, float b, float a) {
        try {
            D3D_MATERIAL_SET_SPECULAR_COLOR.invokeExact(material, flag(set), r, g, b, a);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * @param mapType the {@code TextureMap.Type} ordinal
     * @param textureOrNull the {@code D3DResource*} of the map's texture, whose {@code IDirect3DTexture9}
     *        the C side reads (this folds the former {@code nGetNativeTextureObject}), or
     *        {@link MemorySegment#NULL} to clear the map
     */
    static void materialSetMap(MemorySegment material, int mapType, MemorySegment textureOrNull) {
        try {
            D3D_MATERIAL_SET_MAP.invokeExact(material, mapType, textureOrNull);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static MemorySegment meshviewCreate(MemorySegment ctx, MemorySegment mesh) {
        try {
            return (MemorySegment) D3D_MESHVIEW_CREATE.invokeExact(ctx, mesh);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void meshviewRelease(MemorySegment meshView) {
        try {
            D3D_MESHVIEW_RELEASE.invokeExact(meshView);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** @param cullMode one of {@code D3DContext.CULL_*} */
    static void meshviewSetCullingMode(MemorySegment meshView, int cullMode) {
        try {
            D3D_MESHVIEW_SET_CULLING_MODE.invokeExact(meshView, cullMode);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void meshviewSetMaterial(MemorySegment meshView, MemorySegment material) {
        try {
            D3D_MESHVIEW_SET_MATERIAL.invokeExact(meshView, material);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void meshviewSetWireframe(MemorySegment meshView, boolean wireframe) {
        try {
            D3D_MESHVIEW_SET_WIREFRAME.invokeExact(meshView, flag(wireframe));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void meshviewSetAmbientLight(MemorySegment meshView, float r, float g, float b) {
        try {
            D3D_MESHVIEW_SET_AMBIENT_LIGHT.invokeExact(meshView, r, g, b);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** @param params18 a segment of {@link #LIGHT_LAYOUT} holding the 18 light parameters */
    static void meshviewSetLight(MemorySegment meshView, int index, MemorySegment params18) {
        try {
            D3D_MESHVIEW_SET_LIGHT.invokeExact(meshView, index, params18);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void meshviewRender(MemorySegment meshView) {
        try {
            D3D_MESHVIEW_RENDER.invokeExact(meshView);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }
}
