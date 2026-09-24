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

package com.sun.prism.es2;

import com.sun.glass.utils.NativeLibLoader;
import com.sun.javafx.PlatformUtil;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
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
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * The single point of contact between {@code com.sun.prism.es2} and the {@code es2_*} C ABI exported by
 * the {@code prism_es2} library ({@code src/main/native-prism-es2/prism_es2_api.h}). It owns the library
 * load, the {@link SymbolLookup}, the {@link Linker}, every downcall handle and the one struct layout,
 * and it is the only class in this package that uses a restricted {@code java.lang.foreign} method.
 * <p>
 * The peers keep their native handles as the {@code long native*Info} fields they always had -
 * {@code ContextInfo*}, {@code PixelFormatInfo*}, {@code DrawableInfo*}, {@code MeshInfo*} - so nothing
 * in the package changes shape; this class converts a handle to a zero-length {@link MemorySegment} with
 * {@link MemorySegment#ofAddress} on the way in and reports a returned pointer with
 * {@link MemorySegment#address()} on the way out. {@code 0L} stands where the JNI used {@code 0L}, which
 * {@link MemorySegment#ofAddress} turns into {@link MemorySegment#NULL}, so the C side's
 * {@code jlong_to_ptr(0)} checks see exactly what they saw before. The {@code HWND} / {@code Window} /
 * {@code NSView*} of a presentable and the macOS share-context handle stay {@code long} because Glass
 * still hands them out as such.
 * <p>
 * Status handling is unchanged from the JNI version: a function returns a {@code 0} handle where the JNI
 * returned {@code 0L}, a GL object name of {@code 0} where it failed, or {@code 1}/{@code 0} where it
 * returned a {@code jboolean}. The callers keep their {@code == 0} and validity checks exactly as they
 * were.
 * <p>
 * Threading is unchanged as well: every render-context call runs on the Prism render thread with the
 * context current; the factory / pixel-format family runs on the thread initialising
 * {@link ES2Pipeline}. Only the nine functions the header marks "critical ok" -
 * {@link #uniform4fv}, {@link #uniform4iv}, {@link #uniformMatrix4fv}, {@link #indexBuffer16Create},
 * {@link #texImage2D}, {@link #texSubImage2D}, {@link #drawIndexedQuads}, {@link #meshBuildGeometryShort}
 * and {@link #meshBuildGeometryInt} - are bound with {@link Linker.Option#critical critical(true)} and
 * pass Java heap arrays straight through, exactly where the JNI code used
 * {@code GetPrimitiveArrayCritical} with {@code JNI_ABORT} (no copy-back). Every other function may
 * block, allocate a GPU resource or drain the pipe, so it only ever receives off-heap memory - a direct
 * buffer, or a per-call confined arena. {@link #readPixels} writes its result back and blocks the GPU
 * ({@code glReadPixels}); like the D3D read-back it copies through an off-heap scratch segment for a heap
 * array rather than pinning it (the rendered pixels are identical either way).
 * <p>
 * Loading and binding follow {@code com.sun.prism.d3d.D3DNative}: the library is loaded in the class
 * initializer, {@code es2_abi_version} is bound and checked first, every other symbol is bound eagerly,
 * and any failure - the library missing, a symbol missing, the module not named in
 * {@code --enable-native-access}, an ABI version mismatch - is recorded once and raised by
 * {@link #loadLibrary()} and by every downcall.
 */
final class ES2Native {

    /**
     * The {@code es2_*} ABI revision this class is written against ({@code ES2_ABI_VERSION}): 3 since
     * {@code es2_context_adopt} was bound (2 had added {@code es2_gl_enum_count} / {@code es2_gl_enum}).
     */
    static final int ABI_VERSION = 3;

    /**
     * The library of this platform: {@code prism_es2_monocle} (EGL / OpenGL ES 2, the context owned by Java)
     * when {@code -Dglass.platform=Monocle} selected the Monocle embedded type, {@code prism_es2} otherwise.
     */
    static final String LIBRARY_NAME =
            "monocle".equals(PlatformUtil.getEmbeddedType()) ? "prism_es2_monocle" : "prism_es2";

    /** {@code kind} argument of {@code es2_context_get_string}. */
    static final int STR_VENDOR = 0;
    static final int STR_RENDERER = 1;
    static final int STR_VERSION = 2;
    static final int STR_EXTENSIONS = 3;

    /*
     * GL enums es2_mesh_render takes for cull_mode_gl and fill_mode_gl (the real
     * OpenGL values are passed straight to GL). ES2MeshView translates GLContext.GL_BACK / GL_FRONT /
     * GL_NONE and the wireframe flag into these, exactly as the JNI nSetCullingMode / nSetWireframe did.
     */
    static final int GL_FRONT = 0x0404;
    static final int GL_BACK = 0x0405;
    static final int GL_LINE = 0x1B01;
    static final int GL_FILL = 0x1B02;

    /** {@code ES2_OK}: the success status of the {@code int32_t} status functions. */
    private static final int ES2_OK = 0;

    /** The number of {@code int32_t} fields in {@code Es2PixelFormatAttrs}. */
    private static final int PIXEL_FORMAT_ATTR_COUNT = 7;

    /**
     * {@code Es2PixelFormatAttrs}: seven int32 (red, green, blue, alpha and depth size; double buffer;
     * on-screen) in that order, 28 bytes, no padding - the former {@code int[7]} of
     * {@code GLPixelFormat.Attributes} indexed RED_SIZE .. ONSCREEN. A test compares {@code byteSize()}
     * with {@code es2_sizeof_pixel_format_attrs()}.
     */
    static final StructLayout PIXEL_FORMAT_ATTRS = MemoryLayout.structLayout(
            JAVA_INT.withName("red_size"),
            JAVA_INT.withName("green_size"),
            JAVA_INT.withName("blue_size"),
            JAVA_INT.withName("alpha_size"),
            JAVA_INT.withName("depth_size"),
            JAVA_INT.withName("double_buffer"),
            JAVA_INT.withName("on_screen"));

    private static final Linker LINKER = Linker.nativeLinker();

    /** Names of every symbol this class binds, in binding order; read by the binding tests. */
    private static final List<String> BOUND_SYMBOLS = new ArrayList<>();

    /** Names of the bound symbols the library does not export; read by the binding tests. */
    private static final List<String> MISSING_SYMBOLS = new ArrayList<>();

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
    private static final MethodHandle ES2_ABI_VERSION = bind("es2_abi_version",
            FunctionDescriptor.of(JAVA_INT));

    static {
        checkAbiVersion();
    }

    private static final MethodHandle ES2_SIZEOF_PIXEL_FORMAT_ATTRS = bind("es2_sizeof_pixel_format_attrs",
            FunctionDescriptor.of(JAVA_LONG));
    /*
     * The fifty-entry GL enum table of the header's "GL enums" comment, resolved through the GL headers
     * the library compiled against; read back by ES2GLEnumTableTest against the Java literals. Test-only
     * readers, bound eagerly like everything else so a build that lacks them fails the binding test.
     */
    private static final MethodHandle ES2_GL_ENUM_COUNT = bind("es2_gl_enum_count",
            FunctionDescriptor.of(JAVA_INT));
    private static final MethodHandle ES2_GL_ENUM = bind("es2_gl_enum",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT));

    /* Factory. */
    private static final MethodHandle ES2_FACTORY_INIT = bind("es2_factory_init",
            FunctionDescriptor.of(ADDRESS, ADDRESS));
    private static final MethodHandle ES2_FACTORY_GET_X11_INFO = bind("es2_factory_get_x11_info",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
    private static final MethodHandle ES2_CONTEXT_GET_STRING = bind("es2_context_get_string",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));

    /* Pixel format / drawable / context lifecycle. */
    private static final MethodHandle ES2_PIXEL_FORMAT_CREATE = bind("es2_pixel_format_create",
            FunctionDescriptor.of(ADDRESS, JAVA_LONG, ADDRESS));
    private static final MethodHandle ES2_PIXEL_FORMAT_RELEASE = bind("es2_pixel_format_release",
            FunctionDescriptor.ofVoid(ADDRESS));
    private static final MethodHandle ES2_DRAWABLE_CREATE = bind("es2_drawable_create",
            FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG));
    private static final MethodHandle ES2_DRAWABLE_CREATE_DUMMY = bind("es2_drawable_create_dummy",
            FunctionDescriptor.of(ADDRESS, ADDRESS));
    private static final MethodHandle ES2_DRAWABLE_RELEASE = bind("es2_drawable_release",
            FunctionDescriptor.ofVoid(ADDRESS));
    private static final MethodHandle ES2_DRAWABLE_SWAP_BUFFERS = bind("es2_drawable_swap_buffers",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
    private static final MethodHandle ES2_CONTEXT_CREATE = bind("es2_context_create",
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, JAVA_INT));
    private static final MethodHandle ES2_CONTEXT_RELEASE = bind("es2_context_release",
            FunctionDescriptor.ofVoid(ADDRESS));
    private static final MethodHandle ES2_CONTEXT_GET_NATIVE_HANDLE = bind("es2_context_get_native_handle",
            FunctionDescriptor.of(JAVA_LONG, ADDRESS));
    private static final MethodHandle ES2_CONTEXT_MAKE_CURRENT = bind("es2_context_make_current",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
    private static final MethodHandle ES2_CONTEXT_GET_PROC_ADDRESS = bind("es2_context_get_proc_address",
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
    /**
     * {@code void *es2_context_adopt(Es2ProcLoader loader, void *user)}: a render context over the GL / GLES
     * context current on the calling thread, its entry points asked of {@code loader(user, name)}.
     */
    private static final MethodHandle ES2_CONTEXT_ADOPT = bind("es2_context_adopt",
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));

    /* State setters. */
    private static final MethodHandle ES2_ACTIVE_TEXTURE = bind("es2_active_texture",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
    private static final MethodHandle ES2_BIND_FBO = bind("es2_bind_fbo",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
    private static final MethodHandle ES2_BIND_TEXTURE = bind("es2_bind_texture",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
    private static final MethodHandle ES2_BLEND_FUNC = bind("es2_blend_func",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT));
    private static final MethodHandle ES2_CLEAR_BUFFERS = bind("es2_clear_buffers",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT,
                    JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle ES2_SCISSOR_TEST = bind("es2_scissor_test",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle ES2_SET_DEPTH_TEST = bind("es2_set_depth_test",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
    private static final MethodHandle ES2_SET_MSAA = bind("es2_set_msaa",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
    private static final MethodHandle ES2_UPDATE_VIEWPORT = bind("es2_update_viewport",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle ES2_TEX_PARAMS_MIN_MAX = bind("es2_tex_params_min_max",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT));
    private static final MethodHandle ES2_UPDATE_FILTER_STATE = bind("es2_update_filter_state",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT));
    private static final MethodHandle ES2_UPDATE_WRAP_STATE = bind("es2_update_wrap_state",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT));
    private static final MethodHandle ES2_PIXEL_STOREI = bind("es2_pixel_storei",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT));
    private static final MethodHandle ES2_USE_PROGRAM = bind("es2_use_program",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
    private static final MethodHandle ES2_ENABLE_VERTEX_ATTRIBUTES = bind("es2_enable_vertex_attributes",
            FunctionDescriptor.ofVoid(ADDRESS));
    private static final MethodHandle ES2_DISABLE_VERTEX_ATTRIBUTES = bind("es2_disable_vertex_attributes",
            FunctionDescriptor.ofVoid(ADDRESS));
    private static final MethodHandle ES2_SET_INDEX_BUFFER = bind("es2_set_index_buffer",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
    private static final MethodHandle ES2_SET_DEVICE_PARAMETERS_2D = bind("es2_set_device_parameters_2d",
            FunctionDescriptor.ofVoid(ADDRESS));
    private static final MethodHandle ES2_SET_DEVICE_PARAMETERS_3D = bind("es2_set_device_parameters_3d",
            FunctionDescriptor.ofVoid(ADDRESS));
    private static final MethodHandle ES2_FINISH = bind("es2_finish",
            FunctionDescriptor.ofVoid(ADDRESS));
    private static final MethodHandle ES2_GET_INT_PARAM = bind("es2_get_int_param",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle ES2_GET_MAX_SAMPLE_SIZE = bind("es2_get_max_sample_size",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private static final MethodHandle ES2_GET_FBO = bind("es2_get_fbo",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private static final MethodHandle ES2_GEN_AND_BIND_TEXTURE = bind("es2_gen_and_bind_texture",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private static final MethodHandle ES2_UNIFORM1F = bind("es2_uniform1f",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_FLOAT));
    private static final MethodHandle ES2_UNIFORM2F = bind("es2_uniform2f",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_FLOAT, JAVA_FLOAT));
    private static final MethodHandle ES2_UNIFORM3F = bind("es2_uniform3f",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT));
    private static final MethodHandle ES2_UNIFORM4F = bind("es2_uniform4f",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT));
    private static final MethodHandle ES2_UNIFORM1I = bind("es2_uniform1i",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT));
    private static final MethodHandle ES2_UNIFORM2I = bind("es2_uniform2i",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle ES2_UNIFORM3I = bind("es2_uniform3i",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle ES2_UNIFORM4I = bind("es2_uniform4i",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle ES2_UNIFORM4FV = bind("es2_uniform4fv",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS), Linker.Option.critical(true));
    private static final MethodHandle ES2_UNIFORM4IV = bind("es2_uniform4iv",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS), Linker.Option.critical(true));
    private static final MethodHandle ES2_UNIFORM_MATRIX4FV = bind("es2_uniform_matrix4fv",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS), Linker.Option.critical(true));

    /* Resource creation / deletion. */
    private static final MethodHandle ES2_TEXTURE_CREATE = bind("es2_texture_create",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));
    private static final MethodHandle ES2_TEXTURE_DELETE = bind("es2_texture_delete",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
    private static final MethodHandle ES2_FBO_CREATE = bind("es2_fbo_create",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle ES2_FBO_DELETE = bind("es2_fbo_delete",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
    private static final MethodHandle ES2_DEPTH_BUFFER_CREATE = bind("es2_depth_buffer_create",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle ES2_RENDER_BUFFER_CREATE = bind("es2_render_buffer_create",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle ES2_RENDER_BUFFER_DELETE = bind("es2_render_buffer_delete",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
    private static final MethodHandle ES2_BLIT = bind("es2_blit",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle ES2_INDEX_BUFFER16_CREATE = bind("es2_index_buffer16_create",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT), Linker.Option.critical(true));

    /* Texture upload / readback. */
    private static final MethodHandle ES2_TEX_IMAGE_2D = bind("es2_tex_image_2d",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT), Linker.Option.critical(true));
    private static final MethodHandle ES2_TEX_SUB_IMAGE_2D = bind("es2_tex_sub_image_2d",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_INT, ADDRESS), Linker.Option.critical(true));
    private static final MethodHandle ES2_READ_PIXELS = bind("es2_read_pixels",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));

    /* Shaders. */
    private static final MethodHandle ES2_SHADER_COMPILE = bind("es2_shader_compile",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
    private static final MethodHandle ES2_PROGRAM_CREATE = bind("es2_program_create",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
    private static final MethodHandle ES2_SHADER_DELETE = bind("es2_shader_delete",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
    private static final MethodHandle ES2_SHADERS_DISPOSE = bind("es2_shaders_dispose",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle ES2_GET_UNIFORM_LOCATION = bind("es2_get_uniform_location",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));

    /* 2D draw. */
    private static final MethodHandle ES2_DRAW_INDEXED_QUADS = bind("es2_draw_indexed_quads",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS, ADDRESS), Linker.Option.critical(true));

    /* 3D mesh. */
    private static final MethodHandle ES2_MESH_CREATE = bind("es2_mesh_create",
            FunctionDescriptor.of(ADDRESS, ADDRESS));
    private static final MethodHandle ES2_MESH_RELEASE = bind("es2_mesh_release",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
    private static final MethodHandle ES2_MESH_BUILD_GEOMETRY_SHORT = bind("es2_mesh_build_geometry_short",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT),
            Linker.Option.critical(true));
    private static final MethodHandle ES2_MESH_BUILD_GEOMETRY_INT = bind("es2_mesh_build_geometry_int",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT),
            Linker.Option.critical(true));
    private static final MethodHandle ES2_MESH_RENDER = bind("es2_mesh_render",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT));

    private ES2Native() {
    }

    /* ---------------------------------------------------------------------------------------------
     * Binding and infrastructure - copied verbatim from D3DNative
     * ------------------------------------------------------------------------------------------- */

    /**
     * Binds an exported {@code es2_*} symbol. When the library could not be loaded, a symbol is missing,
     * native access was denied or the ABI version does not match, the returned handle has the requested
     * type and throws the recorded {@link UnsatisfiedLinkError} when invoked, so the failure is reported
     * once, by name, from {@link #loadLibrary()} and from the first call.
     */
    @SuppressWarnings("restricted")
    private static MethodHandle bind(String name, FunctionDescriptor descriptor, Linker.Option... options) {
        BOUND_SYMBOLS.add(name);
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
            actual = (int) ES2_ABI_VERSION.invokeExact();
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

    /** A handle as the zero-length native {@link MemorySegment} the C side owns; {@code 0L} -> NULL. */
    private static MemorySegment seg(long address) {
        return MemorySegment.ofAddress(address);
    }

    /**
     * The address of a direct buffer's whole backing memory, as {@code GetDirectBufferAddress} and
     * {@code GetDirectBufferCapacity} saw it: from the start of the buffer, independent of its position
     * and limit, {@code capacity} elements long. {@link MemorySegment#NULL} for a buffer that is not
     * direct - the JNI got a {@code NULL} address there and reported it, and so does the C side.
     */
    private static MemorySegment directSegment(Buffer buffer) {
        if (buffer == null || !buffer.isDirect()) {
            return MemorySegment.NULL;
        }
        return MemorySegment.ofBuffer(buffer.duplicate().clear());
    }

    /**
     * The pixels a texture-upload or uniform-array call reads, positioned at the buffer's current
     * position exactly as the JNI's {@code GetDirectBufferAddress + getDirectBufferByteOffset} (direct)
     * or {@code GetPrimitiveArrayCritical + getIndirectBufferByteOffset} (heap) did. A direct buffer
     * becomes a native segment; a heap buffer becomes its backing array sliced to the element the
     * position and array offset select, passed under {@code critical(true)}. {@code NULL} for a
     * {@code null} buffer, where the JNI passed a {@code NULL} pointer (GL allocates only).
     */
    private static MemorySegment uploadSegment(Buffer buffer) {
        if (buffer == null) {
            return MemorySegment.NULL;
        }
        if (buffer.isDirect()) {
            return MemorySegment.ofBuffer(buffer);
        }
        Object array = BufferFactory.getArray(buffer);
        long offset = BufferFactory.getIndirectBufferByteOffset(buffer);
        return heapSegment(array).asSlice(offset);
    }

    private static MemorySegment heapSegment(Object array) {
        return switch (array) {
            case byte[] a -> MemorySegment.ofArray(a);
            case short[] a -> MemorySegment.ofArray(a);
            case int[] a -> MemorySegment.ofArray(a);
            case float[] a -> MemorySegment.ofArray(a);
            default -> throw new IllegalArgumentException("unsupported buffer array: " + array.getClass());
        };
    }

    /* ---------------------------------------------------------------------------------------------
     * Loading and test hooks (reached through ES2NativeShim)
     * ------------------------------------------------------------------------------------------- */

    /**
     * Makes sure the library is loaded, every {@code es2_*} symbol is bound and the ABI version matches.
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

    /** The bound symbols the loaded library does not export; empty for a correct build. */
    static List<String> missingSymbols() {
        return Collections.unmodifiableList(new ArrayList<>(MISSING_SYMBOLS));
    }

    static int abiVersion() {
        try {
            return (int) ES2_ABI_VERSION.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static long sizeofPixelFormatAttrs() {
        try {
            return (long) ES2_SIZEOF_PIXEL_FORMAT_ATTRS.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code es2_gl_enum_count()}: the length of the library's GL enum table, 50. */
    static int glEnumCount() {
        try {
            return (int) ES2_GL_ENUM_COUNT.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code es2_gl_enum(index)}: the GL enum at {@code index} of the library's table, or -1 out of range. */
    static int glEnum(int index) {
        try {
            return (int) ES2_GL_ENUM.invokeExact(index);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Factory - runs on the thread that initialises ES2Pipeline
     * ------------------------------------------------------------------------------------------- */

    /**
     * Probes for a usable factory context with the given attributes and returns its {@code ContextInfo*}
     * as a handle, or {@code 0L} when the system is incapable - the former {@code nInitialize}.
     */
    static long factoryInitialize(int[] attrArr) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment attrs = arena.allocate(PIXEL_FORMAT_ATTRS);
            MemorySegment.copy(attrArr, 0, attrs, JAVA_INT, 0, PIXEL_FORMAT_ATTR_COUNT);
            try {
                MemorySegment result = (MemorySegment) ES2_FACTORY_INIT.invokeExact(attrs);
                return result.address();
            } catch (Throwable t) {
                throw unexpected(t);
            }
        }
    }

    /**
     * The X11 factory context's {@code {default screen, Display*, X visual ID}}, or {@code null} on a
     * platform that does not implement it (Windows / macOS) - replaces the X11-only
     * {@code nGetDefaultScreen} / {@code nGetDisplay} / {@code nGetVisualID}.
     */
    static long[] factoryGetX11Info(long ctx) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(JAVA_LONG, 3);
            int status;
            try {
                status = (int) ES2_FACTORY_GET_X11_INFO.invokeExact(seg(ctx), info);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            if (status != ES2_OK) {
                return null;
            }
            return new long[] {
                info.getAtIndex(JAVA_LONG, 0),
                info.getAtIndex(JAVA_LONG, 1),
                info.getAtIndex(JAVA_LONG, 2)
            };
        }
    }

    /**
     * The captured {@code GL_VENDOR} / {@code GL_RENDERER} / {@code GL_VERSION} / {@code GL_EXTENSIONS}
     * string of the given {@link #STR_VENDOR STR_*} kind, or {@code null} when it was never captured -
     * the two-call sizing protocol of {@code es2_context_get_string}. Replaces {@code nGetGLVendor} /
     * {@code nGetGLRenderer} / {@code nGetGLVersion} and feeds {@link #isExtensionSupported}.
     */
    static String contextGetString(long ctx, int kind) {
        MemorySegment context = seg(ctx);
        int length = contextGetStringLength(context, kind);
        if (length < 0) {
            return null;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(length + 1L);
            contextGetStringInto(context, kind, buf, length + 1);
            return buf.getString(0);
        }
    }

    private static int contextGetStringLength(MemorySegment ctx, int kind) {
        try {
            return (int) ES2_CONTEXT_GET_STRING.invokeExact(ctx, kind, MemorySegment.NULL, 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    private static int contextGetStringInto(MemorySegment ctx, int kind, MemorySegment buf, int cap) {
        try {
            return (int) ES2_CONTEXT_GET_STRING.invokeExact(ctx, kind, buf, cap);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * The exact token match {@code GLFactory.c isExtensionSupported} performed, moved to Java:
     * true only when {@code extension} occurs in the space-separated {@code allExtensions}, is not
     * empty, contains no space, and is bounded by the string start / end or by a space on both sides, so
     * {@code "GL_ARB_texture"} does not match inside {@code "GL_ARB_texture_float"}. Replaces
     * {@code nIsGLExtensionSupported}, which has no symbol.
     */
    static boolean isExtensionSupported(String allExtensions, String extension) {
        if (allExtensions == null || extension == null) {
            return false;
        }
        if (extension.isEmpty() || extension.indexOf(' ') >= 0) {
            return false;
        }
        int length = allExtensions.length();
        int extLength = extension.length();
        int start = 0;
        while (true) {
            int where = allExtensions.indexOf(extension, start);
            if (where < 0) {
                return false;
            }
            int terminator = where + extLength;
            boolean leftBoundary = (where == start) || (allExtensions.charAt(where - 1) == ' ');
            boolean rightBoundary = (terminator == length) || (allExtensions.charAt(terminator) == ' ');
            if (leftBoundary && rightBoundary) {
                return true;
            }
            start = terminator;
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Pixel format / drawable / context lifecycle
     * ------------------------------------------------------------------------------------------- */

    /** {@code PixelFormatInfo*} for the attributes, or {@code 0L} - the former {@code nCreatePixelFormat}. */
    static long pixelFormatCreate(long nativeScreen, int[] attrArr) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment attrs = arena.allocate(PIXEL_FORMAT_ATTRS);
            MemorySegment.copy(attrArr, 0, attrs, JAVA_INT, 0, PIXEL_FORMAT_ATTR_COUNT);
            try {
                MemorySegment result = (MemorySegment) ES2_PIXEL_FORMAT_CREATE.invokeExact(nativeScreen, attrs);
                return result.address();
            } catch (Throwable t) {
                throw unexpected(t);
            }
        }
    }

    static void pixelFormatRelease(long pf) {
        try {
            ES2_PIXEL_FORMAT_RELEASE.invokeExact(seg(pf));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** On-screen {@code DrawableInfo*} for a native window, or {@code 0L} - the former {@code nCreateDrawable}. */
    static long drawableCreate(long pf, long nativeWindow) {
        try {
            MemorySegment result = (MemorySegment) ES2_DRAWABLE_CREATE.invokeExact(seg(pf), nativeWindow);
            return result.address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** Off-screen {@code DrawableInfo*}, or {@code 0L} - the former {@code nGetDummyDrawable}. */
    static long drawableCreateDummy(long pf) {
        try {
            MemorySegment result = (MemorySegment) ES2_DRAWABLE_CREATE_DUMMY.invokeExact(seg(pf));
            return result.address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void drawableRelease(long drawable) {
        try {
            ES2_DRAWABLE_RELEASE.invokeExact(seg(drawable));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code true} when the buffers were swapped - the former {@code nSwapBuffers}; ctx is used on macOS. */
    static boolean drawableSwapBuffers(long ctx, long drawable) {
        try {
            return (int) ES2_DRAWABLE_SWAP_BUFFERS.invokeExact(seg(ctx), seg(drawable)) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * The render {@code ContextInfo*} on {@code drawable} with the pixel format, or {@code 0L} - the
     * former {@code nInitialize}. {@code shareCtxHandle} is the factory context's native handle on macOS
     * and ignored on Windows / X11; {@code vsyncRequested} is cached and applied by
     * {@link #contextMakeCurrent}.
     */
    static long contextCreate(long drawable, long pf, long shareCtxHandle, boolean vsyncRequested) {
        try {
            MemorySegment result = (MemorySegment) ES2_CONTEXT_CREATE.invokeExact(seg(drawable), seg(pf),
                    shareCtxHandle, flag(vsyncRequested));
            return result.address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void contextRelease(long ctx) {
        try {
            ES2_CONTEXT_RELEASE.invokeExact(seg(ctx));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** The {@code HGLRC} / {@code GLXContext} / {@code NSOpenGLContext*} as an integer for Glass. */
    static long contextGetNativeHandle(long ctx) {
        try {
            return (long) ES2_CONTEXT_GET_NATIVE_HANDLE.invokeExact(seg(ctx));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void contextMakeCurrent(long ctx, long drawable) {
        try {
            ES2_CONTEXT_MAKE_CURRENT.invokeExact(seg(ctx), seg(drawable));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** The stored GL / FBO entry point of {@code name}, or {@code 0L} - a test hook for the resolved entry points. */
    static long contextGetProcAddress(long ctx, String name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSeg = arena.allocateFrom(name);
            try {
                MemorySegment result = (MemorySegment) ES2_CONTEXT_GET_PROC_ADDRESS.invokeExact(seg(ctx), nameSeg);
                return result.address();
            } catch (Throwable t) {
                throw unexpected(t);
            }
        }
    }

    /**
     * Resolves a GL entry point for {@link #contextAdopt}: the {@code dlsym(handle, name)} the JNI performed,
     * a handle of 0 meaning the global scope of the process ({@code RTLD_DEFAULT}).
     */
    interface ProcLoader {
        long lookup(long handle, String name);
    }

    /** {@code void *(*Es2ProcLoader)(void *user, const char *name)}. */
    private static final FunctionDescriptor PROC_LOADER = FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS);

    /** {@link #loaderUpcall}, with the {@link ProcLoader} still to be bound as its first argument. */
    private static final MethodHandle LOADER_UPCALL;

    static {
        try {
            LOADER_UPCALL = MethodHandles.lookup().findStatic(ES2Native.class, "loaderUpcall",
                    MethodType.methodType(MemorySegment.class, ProcLoader.class, MemorySegment.class,
                            MemorySegment.class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** The loader stub of the last {@link #contextAdopt}; its scope is closed once that call has returned. */
    static volatile MemorySegment lastLoaderStub;

    /**
     * {@code es2_context_adopt}: a render context over the GL / GLES context that is current on this thread -
     * on Monocle, the EGL context AcceleratedScreen created and made current - with every entry point of the
     * table resolved through {@code loader.lookup(glHandle, name)} during the call, as
     * {@code nPopulateNativeCtxInfo} of MonocleGLFactory.c of commit 21d5a654f6 resolved them with dlsym.
     * The loader stub lives in an arena confined to this call: the C stores the resolved pointers and never
     * the loader. A loader that throws contributes a NULL entry; a null loader, or no current context, is a
     * 0 result (the C prints one line on stderr for it). Not a critical downcall: it upcalls.
     *
     * @return the context handle, or {@code 0L}
     */
    @SuppressWarnings("restricted")
    static long contextAdopt(long glHandle, ProcLoader loader) {
        if (loader == null) {
            return adopt(MemorySegment.NULL, glHandle);
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment stub = LINKER.upcallStub(MethodHandles.insertArguments(LOADER_UPCALL, 0, loader),
                    PROC_LOADER, arena);
            lastLoaderStub = stub;
            return adopt(stub, glHandle);
        }
    }

    private static long adopt(MemorySegment loaderStub, long user) {
        try {
            return ((MemorySegment) ES2_CONTEXT_ADOPT.invokeExact(loaderStub, seg(user))).address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** The upcall target: never throws, and answers NULL for whatever the loader cannot resolve. */
    @SuppressWarnings("restricted")
    private static MemorySegment loaderUpcall(ProcLoader loader, MemorySegment user, MemorySegment name) {
        try {
            String symbol = name.reinterpret(Long.MAX_VALUE).getString(0);
            return MemorySegment.ofAddress(loader.lookup(user.address(), symbol));
        } catch (Throwable t) {
            return MemorySegment.NULL;
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * State setters - all run on the render thread with the context current
     * ------------------------------------------------------------------------------------------- */

    static void activeTexture(long ctx, int texUnit) {
        try {
            ES2_ACTIVE_TEXTURE.invokeExact(seg(ctx), texUnit);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void bindFBO(long ctx, int fboID) {
        try {
            ES2_BIND_FBO.invokeExact(seg(ctx), fboID);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void bindTexture(long ctx, int texID) {
        try {
            ES2_BIND_TEXTURE.invokeExact(seg(ctx), texID);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void blendFunc(long ctx, int sFactor, int dFactor) {
        try {
            ES2_BLEND_FUNC.invokeExact(seg(ctx), sFactor, dFactor);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void clearBuffers(long ctx, float red, float green, float blue, float alpha,
            boolean clearColor, boolean clearDepth, boolean ignoreScissor) {
        try {
            ES2_CLEAR_BUFFERS.invokeExact(seg(ctx), red, green, blue, alpha,
                    flag(clearColor), flag(clearDepth), flag(ignoreScissor));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void scissorTest(long ctx, boolean enable, int x, int y, int w, int h) {
        try {
            ES2_SCISSOR_TEST.invokeExact(seg(ctx), flag(enable), x, y, w, h);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void setDepthTest(long ctx, boolean depthTest) {
        try {
            ES2_SET_DEPTH_TEST.invokeExact(seg(ctx), flag(depthTest));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void setMSAA(long ctx, boolean msaa) {
        try {
            ES2_SET_MSAA.invokeExact(seg(ctx), flag(msaa));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void updateViewport(long ctx, int x, int y, int w, int h) {
        try {
            ES2_UPDATE_VIEWPORT.invokeExact(seg(ctx), x, y, w, h);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void texParamsMinMax(long ctx, int minFilter, int magFilter) {
        try {
            ES2_TEX_PARAMS_MIN_MAX.invokeExact(seg(ctx), minFilter, magFilter);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void updateFilterState(long ctx, int texID, boolean linear) {
        try {
            ES2_UPDATE_FILTER_STATE.invokeExact(seg(ctx), texID, flag(linear));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void updateWrapState(long ctx, int texID, int wrapMode) {
        try {
            ES2_UPDATE_WRAP_STATE.invokeExact(seg(ctx), texID, wrapMode);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void pixelStorei(long ctx, int pname, int param) {
        try {
            ES2_PIXEL_STOREI.invokeExact(seg(ctx), pname, param);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void useProgram(long ctx, int programID) {
        try {
            ES2_USE_PROGRAM.invokeExact(seg(ctx), programID);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void enableVertexAttributes(long ctx) {
        try {
            ES2_ENABLE_VERTEX_ATTRIBUTES.invokeExact(seg(ctx));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void disableVertexAttributes(long ctx) {
        try {
            ES2_DISABLE_VERTEX_ATTRIBUTES.invokeExact(seg(ctx));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void setIndexBuffer(long ctx, int bufferID) {
        try {
            ES2_SET_INDEX_BUFFER.invokeExact(seg(ctx), bufferID);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void setDeviceParametersFor2D(long ctx) {
        try {
            ES2_SET_DEVICE_PARAMETERS_2D.invokeExact(seg(ctx));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void setDeviceParametersFor3D(long ctx) {
        try {
            ES2_SET_DEVICE_PARAMETERS_3D.invokeExact(seg(ctx));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void finish(long ctx) {
        try {
            ES2_FINISH.invokeExact(seg(ctx));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int getIntParam(long ctx, int pname) {
        try {
            return (int) ES2_GET_INT_PARAM.invokeExact(seg(ctx), pname);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int getMaxSampleSize(long ctx) {
        try {
            return (int) ES2_GET_MAX_SAMPLE_SIZE.invokeExact(seg(ctx));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int getFBO(long ctx) {
        try {
            return (int) ES2_GET_FBO.invokeExact(seg(ctx));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int genAndBindTexture(long ctx) {
        try {
            return (int) ES2_GEN_AND_BIND_TEXTURE.invokeExact(seg(ctx));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void uniform1f(long ctx, int location, float v0) {
        try {
            ES2_UNIFORM1F.invokeExact(seg(ctx), location, v0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void uniform2f(long ctx, int location, float v0, float v1) {
        try {
            ES2_UNIFORM2F.invokeExact(seg(ctx), location, v0, v1);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void uniform3f(long ctx, int location, float v0, float v1, float v2) {
        try {
            ES2_UNIFORM3F.invokeExact(seg(ctx), location, v0, v1, v2);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void uniform4f(long ctx, int location, float v0, float v1, float v2, float v3) {
        try {
            ES2_UNIFORM4F.invokeExact(seg(ctx), location, v0, v1, v2, v3);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void uniform1i(long ctx, int location, int v0) {
        try {
            ES2_UNIFORM1I.invokeExact(seg(ctx), location, v0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void uniform2i(long ctx, int location, int v0, int v1) {
        try {
            ES2_UNIFORM2I.invokeExact(seg(ctx), location, v0, v1);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void uniform3i(long ctx, int location, int v0, int v1, int v2) {
        try {
            ES2_UNIFORM3I.invokeExact(seg(ctx), location, v0, v1, v2);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void uniform4i(long ctx, int location, int v0, int v1, int v2, int v3) {
        try {
            ES2_UNIFORM4I.invokeExact(seg(ctx), location, v0, v1, v2, v3);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code count} vec4 floats from {@code values}; the buffer offset is applied by slicing. critical. */
    static void uniform4fv(long ctx, int location, int count, FloatBuffer values) {
        try {
            ES2_UNIFORM4FV.invokeExact(seg(ctx), location, count, uploadSegment(values));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code count} vec4 ints from {@code values}; the buffer offset is applied by slicing. critical. */
    static void uniform4iv(long ctx, int location, int count, IntBuffer values) {
        try {
            ES2_UNIFORM4IV.invokeExact(seg(ctx), location, count, uploadSegment(values));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** One 4x4 matrix (16 floats). critical. */
    static void uniformMatrix4fv(long ctx, int location, boolean transpose, float[] values) {
        try {
            ES2_UNIFORM_MATRIX4FV.invokeExact(seg(ctx), location, flag(transpose), MemorySegment.ofArray(values));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Resource creation / deletion
     * ------------------------------------------------------------------------------------------- */

    static int textureCreate(long ctx, int width, int height) {
        try {
            return (int) ES2_TEXTURE_CREATE.invokeExact(seg(ctx), width, height);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void textureDelete(long ctx, int texID) {
        try {
            ES2_TEXTURE_DELETE.invokeExact(seg(ctx), texID);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int fboCreate(long ctx, int texID) {
        try {
            return (int) ES2_FBO_CREATE.invokeExact(seg(ctx), texID);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void fboDelete(long ctx, int fboID) {
        try {
            ES2_FBO_DELETE.invokeExact(seg(ctx), fboID);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int depthBufferCreate(long ctx, int width, int height, int msaaSamples) {
        try {
            return (int) ES2_DEPTH_BUFFER_CREATE.invokeExact(seg(ctx), width, height, msaaSamples);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int renderBufferCreate(long ctx, int width, int height, int msaaSamples) {
        try {
            return (int) ES2_RENDER_BUFFER_CREATE.invokeExact(seg(ctx), width, height, msaaSamples);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void renderBufferDelete(long ctx, int rbID) {
        try {
            ES2_RENDER_BUFFER_DELETE.invokeExact(seg(ctx), rbID);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void blit(long ctx, int srcFBO, int dstFBO,
            int srcX0, int srcY0, int srcX1, int srcY1,
            int dstX0, int dstY0, int dstX1, int dstY1) {
        try {
            ES2_BLIT.invokeExact(seg(ctx), srcFBO, dstFBO,
                    srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code GL_ELEMENT_ARRAY_BUFFER} of {@code n} unsigned shorts; 0 on failure / NULL data. critical. */
    static int indexBuffer16Create(long ctx, short[] data, int n) {
        MemorySegment segment = data != null ? MemorySegment.ofArray(data) : MemorySegment.NULL;
        try {
            return (int) ES2_INDEX_BUFFER16_CREATE.invokeExact(seg(ctx), segment, n);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Texture upload / readback
     * ------------------------------------------------------------------------------------------- */

    /**
     * Uploads {@code pixels} (or allocates only, when {@code null}) - the former {@code nTexImage2D0/1}
     * folded into one call; {@code true} when {@code glGetError} is clean. critical.
     */
    static boolean texImage2D(long ctx, int target, int level, int internalFormat, int width, int height,
            int border, int format, int type, Buffer pixels, boolean useMipmap) {
        try {
            return (int) ES2_TEX_IMAGE_2D.invokeExact(seg(ctx), target, level, internalFormat, width, height,
                    border, format, type, uploadSegment(pixels), flag(useMipmap)) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** The former {@code nTexSubImage2D0/1} folded into one call. critical. */
    static void texSubImage2D(long ctx, int target, int level, int xoffset, int yoffset, int width, int height,
            int format, int type, Buffer pixels) {
        try {
            ES2_TEX_SUB_IMAGE_2D.invokeExact(seg(ctx), target, level, xoffset, yoffset, width, height,
                    format, type, uploadSegment(pixels));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Reads {@code w x h} BGRA pixels back into a {@link ByteBuffer} - the former {@code nReadPixelsByte}.
     * {@code length} is the byte count the caller derived from the buffer's capacity, which the C checks.
     * A direct buffer is written in place; a heap array goes through an off-heap scratch, prefilled from
     * the array and copied back whole, so the array ends exactly as the JNI's {@code GetPrimitiveArray}
     * copy-back left it. {@code glReadPixels} drains the GPU, so the array is never pinned.
     */
    static boolean readPixels(long ctx, int length, ByteBuffer direct, byte[] pixels, int x, int y, int w, int h) {
        if (pixels == null) {
            return readPixels(ctx, directSegment(direct), length, x, y, w, h);
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment scratch = arena.allocate(length);
            int count = Math.min(pixels.length, length);
            MemorySegment.copy(pixels, 0, scratch, JAVA_BYTE, 0, count);
            boolean result = readPixels(ctx, scratch, length, x, y, w, h);
            MemorySegment.copy(scratch, JAVA_BYTE, 0, pixels, 0, count);
            return result;
        }
    }

    /** The {@code int[]} form of {@link #readPixels} - the former {@code nReadPixelsInt}. */
    static boolean readPixels(long ctx, int length, IntBuffer direct, int[] pixels, int x, int y, int w, int h) {
        if (pixels == null) {
            return readPixels(ctx, directSegment(direct), length, x, y, w, h);
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment scratch = arena.allocate(length);
            int count = Math.min(pixels.length, length / (int) JAVA_INT.byteSize());
            MemorySegment.copy(pixels, 0, scratch, JAVA_INT, 0, count);
            boolean result = readPixels(ctx, scratch, length, x, y, w, h);
            MemorySegment.copy(scratch, JAVA_INT, 0, pixels, 0, count);
            return result;
        }
    }

    private static boolean readPixels(long ctx, MemorySegment dst, int length, int x, int y, int w, int h) {
        try {
            return (int) ES2_READ_PIXELS.invokeExact(seg(ctx), dst, length, x, y, w, h) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Shaders
     * ------------------------------------------------------------------------------------------- */

    static int shaderCompile(long ctx, String source, boolean vertex) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment sourceSeg = arena.allocateFrom(source);
            try {
                return (int) ES2_SHADER_COMPILE.invokeExact(seg(ctx), sourceSeg, flag(vertex));
            } catch (Throwable t) {
                throw unexpected(t);
            }
        }
    }

    /**
     * Links a program from {@code vertexShaderID} and {@code fragmentShaderIDs}, binding
     * {@code attrs[i]} to {@code indices[i]}; 0 on failure - the former {@code nCreateProgram}.
     */
    static int programCreate(long ctx, int vertexShaderID, int[] fragmentShaderIDs,
            String[] attrs, int[] indices) {
        int nFrag = fragmentShaderIDs.length;
        int nAttrs = attrs.length;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment fragIDs = arena.allocate(JAVA_INT, nFrag);
            MemorySegment.copy(fragmentShaderIDs, 0, fragIDs, JAVA_INT, 0, nFrag);
            MemorySegment attrNames = arena.allocate(ADDRESS, nAttrs);
            for (int i = 0; i < nAttrs; i++) {
                attrNames.setAtIndex(ADDRESS, i, arena.allocateFrom(attrs[i]));
            }
            MemorySegment attrIndices = arena.allocate(JAVA_INT, nAttrs);
            MemorySegment.copy(indices, 0, attrIndices, JAVA_INT, 0, nAttrs);
            try {
                return (int) ES2_PROGRAM_CREATE.invokeExact(seg(ctx), vertexShaderID, fragIDs, nFrag,
                        attrNames, attrIndices, nAttrs);
            } catch (Throwable t) {
                throw unexpected(t);
            }
        }
    }

    static void shaderDelete(long ctx, int shaderID) {
        try {
            ES2_SHADER_DELETE.invokeExact(seg(ctx), shaderID);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** Disposes a program and its shaders - the former {@code nDisposeShaders}; NULL {@code frag_ids} guards. */
    static void shadersDispose(long ctx, int programID, int vertexShaderID, int[] fragmentShaderIDs) {
        int nFrag = fragmentShaderIDs == null ? 0 : fragmentShaderIDs.length;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment fragIDs;
            if (fragmentShaderIDs == null) {
                fragIDs = MemorySegment.NULL;
            } else {
                fragIDs = arena.allocate(JAVA_INT, nFrag);
                MemorySegment.copy(fragmentShaderIDs, 0, fragIDs, JAVA_INT, 0, nFrag);
            }
            try {
                ES2_SHADERS_DISPOSE.invokeExact(seg(ctx), programID, vertexShaderID, fragIDs, nFrag);
            } catch (Throwable t) {
                throw unexpected(t);
            }
        }
    }

    static int getUniformLocation(long ctx, int programID, String name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSeg = arena.allocateFrom(name);
            try {
                return (int) ES2_GET_UNIFORM_LOCATION.invokeExact(seg(ctx), programID, nameSeg);
            } catch (Throwable t) {
                throw unexpected(t);
            }
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * 2D draw
     * ------------------------------------------------------------------------------------------- */

    /** {@code num_vertices * 7} floats of coords and {@code * 4} bytes of colors. critical. */
    static void drawIndexedQuads(long ctx, int numVertices, float[] coords, byte[] colors) {
        try {
            ES2_DRAW_INDEXED_QUADS.invokeExact(seg(ctx), numVertices,
                    MemorySegment.ofArray(coords), MemorySegment.ofArray(colors));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * 3D mesh
     * ------------------------------------------------------------------------------------------- */

    static long meshCreate(long ctx) {
        try {
            MemorySegment result = (MemorySegment) ES2_MESH_CREATE.invokeExact(seg(ctx));
            return result.address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void meshRelease(long ctx, long mesh) {
        try {
            ES2_MESH_RELEASE.invokeExact(seg(ctx), seg(mesh));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** Uploads interleaved geometry with 16-bit indices; {@code true} on success. critical. */
    static boolean meshBuildGeometryShort(long ctx, long mesh, float[] vertexBuffer, int vbLength,
            short[] indexBuffer, int ibLength) {
        try {
            return (int) ES2_MESH_BUILD_GEOMETRY_SHORT.invokeExact(seg(ctx), seg(mesh),
                    MemorySegment.ofArray(vertexBuffer), vbLength,
                    MemorySegment.ofArray(indexBuffer), ibLength) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** Uploads interleaved geometry with 32-bit indices; {@code true} on success. critical. */
    static boolean meshBuildGeometryInt(long ctx, long mesh, float[] vertexBuffer, int vbLength,
            int[] indexBuffer, int ibLength) {
        try {
            return (int) ES2_MESH_BUILD_GEOMETRY_INT.invokeExact(seg(ctx), seg(mesh),
                    MemorySegment.ofArray(vertexBuffer), vbLength,
                    MemorySegment.ofArray(indexBuffer), ibLength) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Draws {@code mesh} with the three values {@code MeshViewInfo} used to carry - {@code cullEnable}
     * (boolean), {@code cullModeGL} ({@link #GL_BACK} / {@link #GL_FRONT}) and {@code fillModeGL}
     * ({@link #GL_FILL} / {@link #GL_LINE}). {@link ES2MeshView} calls this only when a material is set,
     * which is what the NULL {@code phongMaterialInfo} check in {@code nRenderMeshView} did.
     */
    static void meshRender(long ctx, long mesh, int cullEnable, int cullModeGL, int fillModeGL) {
        try {
            ES2_MESH_RENDER.invokeExact(seg(ctx), seg(mesh), cullEnable, cullModeGL, fillModeGL);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }
}
