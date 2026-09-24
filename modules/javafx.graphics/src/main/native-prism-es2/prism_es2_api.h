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

/*
 * prism_es2_api.h - the flat C ABI of the prism_es2 library.
 *
 * This is the only surface com.sun.prism.es2.ES2Native binds. The library knows nothing about the
 * JVM: every function takes and returns <stdint.h> scalars, opaque void* handles (the C owns the
 * memory behind them) or caller-provided buffers, and nothing here ever calls back into Java. No
 * exception crosses this boundary. Failures are reported exactly as the JNI entry points reported
 * them: a NULL handle where the JNI returned 0L, 0 where it returned a GL object name of 0 or
 * false, and a diagnostic on stderr. The symbol set is identical on Windows, Linux (X11/GLX),
 * macOS (NSOpenGL) and Linux Monocle (prism_es2_monocle: EGL / OpenGL ES 2, where Java owns the
 * context and hands it over with es2_context_adopt); a query that only one platform can answer
 * returns ES2_ERR_NOT_IMPLEMENTED elsewhere (the jfxmedia_api.h convention).
 *
 * Handles. ctx is a ContextInfo*, pf a PixelFormatInfo*, drawable a DrawableInfo*, mesh a
 * MeshInfo* (all PrismES2Defs.h). Two kinds of ContextInfo exist, as before: the one returned by
 * es2_factory_init (owned by GLFactory, carries the driver strings and, on X11/macOS, what Glass
 * needs) and the one returned by es2_context_create (owned by GLContext, carries the GL 2.0+ entry
 * points and the state cache). Every es2_* function below the lifecycle section takes the latter.
 *
 * Threading. Every function runs on the Prism render thread with the context current, except the
 * factory / pixel-format functions, which run on the thread initialising ES2Pipeline (also the
 * render thread under Quantum). There are no locks.
 *
 * Booleans are int32_t: 0 is false, anything else is true. Never bind them as JAVA_BOOLEAN.
 *
 * GL enums. Every parameter documented below as "GL enum" takes the real
 * OpenGL value (GL_SRC_ALPHA = 0x0302, GL_RGBA = 0x1908, ...) and is passed to GL unchanged; the
 * JNI-era translation of the small GLContext.GL_* indices is not applied on this path. For the
 * Java-side table test, the values the JNI translation tables produced were:
 *   blend factors (translateScaleFactor): GL_ZERO 0, GL_ONE 1, GL_SRC_COLOR 0x0300,
 *     GL_ONE_MINUS_SRC_COLOR 0x0301, GL_SRC_ALPHA 0x0302, GL_ONE_MINUS_SRC_ALPHA 0x0303,
 *     GL_DST_ALPHA 0x0304, GL_ONE_MINUS_DST_ALPHA 0x0305, GL_DST_COLOR 0x0306,
 *     GL_ONE_MINUS_DST_COLOR 0x0307, GL_SRC_ALPHA_SATURATE 0x0308, GL_CONSTANT_COLOR 0x8001,
 *     GL_ONE_MINUS_CONSTANT_COLOR 0x8002, GL_CONSTANT_ALPHA 0x8003, GL_ONE_MINUS_CONSTANT_ALPHA 0x8004
 *   pixel types: GL_FLOAT 0x1406, GL_UNSIGNED_BYTE 0x1401, GL_UNSIGNED_INT_8_8_8_8_REV 0x8367,
 *     GL_UNSIGNED_INT_8_8_8_8 0x8035, GL_UNSIGNED_SHORT_8_8_APPLE 0x85BA
 *   pixel formats: GL_RGBA 0x1908, GL_BGRA 0x80E1, GL_RGB 0x1907, GL_LUMINANCE 0x1909,
 *     GL_ALPHA 0x1906, GL_RGBA32F 0x8814, GL_YCBCR_422_APPLE 0x85B9
 *   textures: GL_TEXTURE_2D 0x0DE1, GL_TEXTURE_BINDING_2D 0x8069, GL_NEAREST 0x2600,
 *     GL_LINEAR 0x2601, GL_NEAREST_MIPMAP_NEAREST 0x2700, GL_LINEAR_MIPMAP_LINEAR 0x2703
 *   wrap modes (WRAPMODE_*): GL_REPEAT 0x2901, GL_CLAMP_TO_EDGE 0x812F, GL_CLAMP_TO_BORDER 0x812D
 *   pixel store (translatePixelStore): GL_UNPACK_ALIGNMENT 0x0CF5, GL_UNPACK_ROW_LENGTH 0x0CF2,
 *     GL_UNPACK_SKIP_PIXELS 0x0CF4, GL_UNPACK_SKIP_ROWS 0x0CF3
 *   glGetIntegerv names: GL_MAX_FRAGMENT_UNIFORM_COMPONENTS 0x8B49,
 *     GL_MAX_FRAGMENT_UNIFORM_VECTORS 0x8DFD, GL_MAX_TEXTURE_IMAGE_UNITS 0x8872,
 *     GL_MAX_TEXTURE_SIZE 0x0D33, GL_MAX_VARYING_COMPONENTS 0x8B4B, GL_MAX_VARYING_VECTORS 0x8DFC,
 *     GL_MAX_VERTEX_ATTRIBS 0x8869, GL_MAX_VERTEX_UNIFORM_COMPONENTS 0x8B4A,
 *     GL_MAX_VERTEX_UNIFORM_VECTORS 0x8DFB, GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS 0x8B4C
 *   culling (nSetCullingMode): GLContext.GL_BACK -> (cull_enable 1, GL_BACK 0x0405),
 *     GL_FRONT -> (1, GL_FRONT 0x0404), GL_NONE -> (0, GL_BACK 0x0405);
 *     wireframe (nSetWireframe): true -> GL_LINE 0x1B01, false -> GL_FILL 0x1B02
 *
 * Strings. Shader sources, attribute names, uniform names arrive as NUL-terminated UTF-8 (every
 * shader input in the tree is ASCII, so this is byte-identical to the modified UTF-8 the JNI used).
 * es2_context_get_string copies into a caller buffer; see its comment for the sizing protocol.
 *
 * Linker.Option.critical(true). Java may pin heap arrays for: es2_uniform4fv,
 * es2_uniform4iv, es2_uniform_matrix4fv, es2_index_buffer16_create, es2_tex_image_2d,
 * es2_tex_sub_image_2d, es2_draw_indexed_quads, es2_mesh_build_geometry_short,
 * es2_mesh_build_geometry_int; each of them is one short GL call around the pinned memory. Every
 * other function must take off-heap memory only, and these BLOCK on the driver or the GPU and must
 * never be critical: es2_drawable_swap_buffers (vsync), es2_finish, es2_context_make_current,
 * es2_context_create, es2_factory_init, es2_pixel_format_create, es2_shader_compile,
 * es2_program_create, es2_texture_create, es2_fbo_create, es2_depth_buffer_create,
 * es2_render_buffer_create, es2_mesh_create. es2_read_pixels is a judgement call: the
 * JNI pinned the array around a glReadPixels that drains the GPU; whichever option Java chooses,
 * the C side is the same.
 *
 * Behaviour-neutral ports. Every body is the JNI body with the marshalling removed, stderr text
 * included. In particular: functions whose JNI predecessor took no context (or ignored it) still
 * ignore ctx - es2_bind_texture, es2_texture_delete, es2_update_filter_state,
 * es2_update_wrap_state, es2_blend_func, es2_finish, es2_gen_and_bind_texture, es2_get_fbo,
 * es2_get_int_param, es2_get_max_sample_size, es2_pixel_storei, es2_tex_params_min_max,
 * es2_tex_image_2d, es2_tex_sub_image_2d - so passing NULL there is neither checked nor an error.
 * A NULL ctx (or mesh / pf / drawable) everywhere else is a no-op, NULL or 0, as the JNI's
 * null-handle checks made it; the one guard the JNI lacked and this ABI adds is
 * es2_context_make_current on X11 (X11GLContext.c:326 at commit 868c4801ec dereferenced unchecked).
 */

#ifndef PRISM_ES2_API_H
#define PRISM_ES2_API_H

#include <stdint.h>

#if defined(_WIN32)
#  define PRISM_ES2_EXPORT __declspec(dllexport)
#else
#  define PRISM_ES2_EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Bumped whenever a symbol, a prototype, a struct layout or an enum value changes. Java binds
 * es2_abi_version first and every other symbol eagerly, so an old library fails with a version
 * mismatch rather than a "missing native symbol" on the first new function. 3: es2_context_adopt
 * added (ES2Native.ABI_VERSION moves to 3 with its binding).
 */
#define ES2_ABI_VERSION 3u

/* Return codes of the int32_t status functions. GL object names and lengths are separate. */
enum {
    ES2_OK                  =  0,
    ES2_ERR_NOT_IMPLEMENTED = -1,
    ES2_ERR_INVALID_ARG     = -2,
    ES2_ERR_FAILED          = -3
};

/*
 * GLPixelFormat.Attributes, formerly an int[7] indexed by the generated-header constants
 * RED_SIZE .. ONSCREEN. Seven consecutive int32_t, 4-byte aligned, sizeof 28, no padding: offsets
 * are red_size 0, green_size 4, blue_size 8, alpha_size 12, depth_size 16, double_buffer 20,
 * on_screen 24 (checked at compile time in prism_es2_api.c and at run time through
 * es2_sizeof_pixel_format_attrs). Java: StructLayout of seven JAVA_INT in this order.
 */
#define ES2_PIXEL_FORMAT_ATTR_COUNT 7

typedef struct Es2PixelFormatAttrs {
    int32_t red_size;
    int32_t green_size;
    int32_t blue_size;
    int32_t alpha_size;
    int32_t depth_size;
    int32_t double_buffer;   /* 0/1 */
    int32_t on_screen;       /* 0/1 */
} Es2PixelFormatAttrs;

/* kind argument of es2_context_get_string. */
enum {
    ES2_STR_VENDOR     = 0,   /* GL_VENDOR captured at factory / context creation */
    ES2_STR_RENDERER   = 1,   /* GL_RENDERER */
    ES2_STR_VERSION    = 2,   /* GL_VERSION */
    ES2_STR_EXTENSIONS = 3    /* GL_EXTENSIONS, space-separated tokens */
};

/* ------------------------------------------------------------------------------------------------
 * ABI guard / layout checks
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT uint32_t es2_abi_version(void);                      /* ES2_ABI_VERSION */
PRISM_ES2_EXPORT int64_t  es2_sizeof_pixel_format_attrs(void);        /* (int64_t) sizeof(Es2PixelFormatAttrs) */

/*
 * The 50 GL enum values of the "GL enums" comment at the top of this file, resolved through the GL
 * headers this library compiles against on each platform, in EXACTLY that order: 15 blend factors,
 * 5 pixel types, 7 pixel formats, 6 texture values, 3 wrap modes, 4 pixel-store names and 10
 * glGetIntegerv names. Java pins its own copies of the same values against these
 * (ES2GLEnumTableTest), so a header whose macro disagrees with the comment fails a test instead of
 * reaching GL. es2_gl_enum_count() returns 50; es2_gl_enum(index) returns the value at index, or -1
 * when index < 0 or >= the count. Added after ES2_ABI_VERSION 1 shipped and bound by ES2Native since
 * ABI 2; ABI 3 then added es2_context_adopt.
 */
PRISM_ES2_EXPORT int32_t  es2_gl_enum_count(void);
PRISM_ES2_EXPORT int32_t  es2_gl_enum(int32_t index);

/* ------------------------------------------------------------------------------------------------
 * Factory. Replaces {Win,X11,Mac}GLFactory.nInitialize and the GLFactory string natives.
 * ---------------------------------------------------------------------------------------------- */

/*
 * Probes the system for a usable GL 2.1 (Windows/X11/macOS factory rule) context with the given
 * attributes and returns the factory ContextInfo* carrying the driver strings, or NULL when the
 * system is incapable (the JNI printed the reason on stderr and returned 0L; so does this). On
 * X11 the returned ContextInfo also holds the Display*, screen and visual ID that
 * es2_factory_get_x11_info reports; on macOS it keeps the NSOpenGLContext alive as the share
 * context Glass uses (es2_context_get_native_handle on it). BLOCKING: creates a window and a
 * context. attrs NULL -> NULL.
 */
PRISM_ES2_EXPORT void*   es2_factory_init(const Es2PixelFormatAttrs* attrs);

/*
 * X11 only: info[0] = default screen, info[1] = (int64_t) Display*, info[2] = X visual ID, all
 * captured by es2_factory_init; returns ES2_OK. Every platform returns ES2_ERR_INVALID_ARG for a
 * NULL ctx or info first; Windows, macOS and Monocle then return ES2_ERR_NOT_IMPLEMENTED without
 * touching info. The values keep flowing to Glass GTK as the XVisualID / XDisplay / XScreenID map
 * entries.
 */
PRISM_ES2_EXPORT int32_t es2_factory_get_x11_info(void* ctx, int64_t info[3]);

/*
 * Copies the NUL-terminated string of the given ES2_STR_* kind into buf and returns its length in
 * bytes excluding the NUL, or -1 when ctx is NULL, kind is unknown or the string was never captured
 * (the JNI returned a Java null for that). cap == 0 only sizes the buffer (buf may be NULL). When
 * cap > 0 at most cap - 1 bytes are copied and buf is always NUL-terminated; a return value >= cap
 * means the copy was truncated. Works on both the factory and the render ContextInfo.
 *
 * ES2_STR_EXTENSIONS replaces GLFactory.nIsGLExtensionSupported, which has no symbol: the C test
 * (GLFactory.c isExtensionSupported) is an exact token match - the name must be non-empty and
 * contain no space, and it matches only where it is bounded by the string start / end or by a
 * space on both sides, so "GL_ARB_texture" does not match "GL_ARB_texture_float".
 */
PRISM_ES2_EXPORT int32_t es2_context_get_string(void* ctx, int32_t kind, char* buf, int32_t cap);

/* ------------------------------------------------------------------------------------------------
 * Pixel format / drawable / context lifecycle
 * ---------------------------------------------------------------------------------------------- */

/*
 * Replaces {Win,X11,Mac}GLPixelFormat.nCreatePixelFormat. Returns a PixelFormatInfo* or NULL.
 * native_screen is accepted and ignored on every platform, exactly as before (JDK-8090498 /
 * JDK-8091981). BLOCKING on Windows (dummy window + ChoosePixelFormat) and X11 (XOpenDisplay +
 * glXChooseFBConfig). attrs NULL -> NULL.
 */
PRISM_ES2_EXPORT void*   es2_pixel_format_create(int64_t native_screen, const Es2PixelFormatAttrs* attrs);

/*
 * New: the JNI never freed a PixelFormatInfo. Runs deletePixelFormatInfo (releases the Windows
 * dummy window / DC and the X11 dummy window / colormap; on macOS the NSOpenGLPixelFormat itself
 * is not released by that helper, as before) and frees the struct. Java wires this into a dispose
 * path only in a later commit (behaviour-neutral rule). NULL -> no-op.
 */
PRISM_ES2_EXPORT void    es2_pixel_format_release(void* pf);

/*
 * On-screen drawable for a native window (HWND / X Window / NSView*). Windows: GetDC +
 * SetPixelFormat, NULL on failure; X11 and macOS: struct only. Windows and X11 return NULL for a
 * NULL pf; macOS ignores pf, as its JNI did. Windows also returns NULL for a 0 native_window.
 */
PRISM_ES2_EXPORT void*   es2_drawable_create(void* pf, int64_t native_window);

/*
 * Off-screen drawable sharing the pixel format's dummy window (Windows / X11) or an empty
 * struct (macOS). Windows and X11 return NULL for a NULL pf; macOS ignores pf.
 */
PRISM_ES2_EXPORT void*   es2_drawable_create_dummy(void* pf);

/* Windows: ReleaseDC when the drawable owns one; every platform then frees the struct. NULL: no-op. */
PRISM_ES2_EXPORT void    es2_drawable_release(void* drawable);

/*
 * SwapBuffers / glXSwapBuffers / -flushBuffer. Returns 1 on success, 0 on failure or a NULL
 * handle. ctx is used on macOS only (flushBuffer needs the NSOpenGLContext); Windows and X11 use
 * only drawable and return 0 for a NULL drawable, macOS returns 0 for a NULL ctx. BLOCKING on
 * vsync: never critical.
 */
PRISM_ES2_EXPORT int32_t es2_drawable_swap_buffers(void* ctx, void* drawable);

/*
 * Replaces {Win,X11,Mac}GLContext.nInitialize. Creates the render context on drawable with the
 * pixel format, requires GL >= 2.0 and GL_ARB_pixel_buffer_object, resolves the GL 2.0+ / FBO
 * entry points (wglGetProcAddress / dlsym), runs initState and releases the context again
 * (Java calls es2_context_make_current before drawing). share_ctx_handle is the
 * es2_context_get_native_handle of the factory context on macOS and is ignored on Windows and
 * X11, as before. vsync_requested is cached and applied by es2_context_make_current. Returns the
 * ContextInfo* or NULL (drawable / pf NULL, or any failure, reported on stderr). BLOCKING.
 */
PRISM_ES2_EXPORT void*   es2_context_create(void* drawable, void* pf, int64_t share_ctx_handle,
                                            int32_t vsync_requested);

/*
 * Monocle (Linux EGL / OpenGL ES 2): adopts a context the CALLER created and made current on the
 * calling thread - Java (com.sun.glass.ui.monocle.AcceleratedScreen) owns the EGL display, surface
 * and context and keeps owning them. Replaces MonocleGLFactory.nPopulateNativeCtxInfo of
 * MonocleGLFactory.c at commit 21d5a654f6. Returns a render ContextInfo* whose driver strings are
 * captured from the current context (four glGetString calls, copied; extractVersionInfo run on
 * GL_VERSION) and whose GL 2.0+ / FBO entry points are loader(user, name) for every name
 * es2_context_get_proc_address knows (the es2_proc_table rows), a NULL entry allowed exactly as the
 * JNI's warning-free dlsym left it; initState is run. The native context / display members stay
 * NULL, the vsync state stays 0 (Monocle applies no swap interval) and gl2 stays 0 (the GLES 2
 * profile). Returns NULL with one stderr line when loader is NULL, malloc fails or
 * glGetString(GL_VERSION) / glGetString(GL_EXTENSIONS) returns NULL (no current context) - the JNI
 * called strdup on the NULL and crashed; a NULL GL_VENDOR / GL_RENDERER becomes "<UNKNOWN>", as in
 * es2_factory_init. es2_context_release on the result frees the strings and the struct and destroys
 * nothing native; es2_context_get_string and es2_context_get_proc_address work on it unchanged.
 * Exported by every platform's library (only glGetString and the generic table are involved) so
 * ES2Native's eager bind sees one symbol set everywhere. Calls back into the loader, which is not
 * retained after the return: never critical.
 */
typedef void* (*Es2ProcLoader)(void* user, const char* name);
PRISM_ES2_EXPORT void*   es2_context_adopt(Es2ProcLoader loader, void* user);

/*
 * New: the JNI never freed a ContextInfo. Runs deleteCtxInfo (frees the captured strings and
 * deletes the HGLRC / GLXContext; the macOS NSOpenGLContext is not deleted by that helper, as
 * before, and the Monocle EGL context is Java's) and frees the struct. The context must not be
 * current on another thread. Java wires this into a dispose path only in a later commit.
 * NULL -> no-op.
 */
PRISM_ES2_EXPORT void    es2_context_release(void* ctx);

/* HGLRC / GLXContext / NSOpenGLContext* as an integer for Glass (still JNI), 0 for a NULL ctx. */
PRISM_ES2_EXPORT int64_t es2_context_get_native_handle(void* ctx);

/*
 * wglMakeCurrent / glXMakeCurrent / -makeCurrentContext, then applies the swap interval when
 * (vsync_requested && drawable->onScreen) differs from the cached value. A failure to make
 * current is reported on stderr, not returned, as before. NULL ctx or drawable: no-op on every
 * platform (the X11 guard is new, see the file comment). BLOCKING: never critical.
 */
PRISM_ES2_EXPORT void    es2_context_make_current(void* ctx, void* drawable);

/*
 * Inspection hook: the GL 2.0+ / FBO entry point es2_context_create (or es2_context_adopt)
 * resolved and stored in this ContextInfo for the given name ("glUniform4fv", "glBindFramebuffer",
 * ...; also "wglSwapIntervalEXT" on Windows and "glXSwapIntervalSGI" on X11 - not on Monocle), or
 * NULL when ctx or name is NULL, the name is not one the C resolves, or the driver did not provide
 * it. The value is the stored pointer, not a fresh wglGetProcAddress / dlsym / loader call. Only
 * meaningful for a render ContextInfo (the factory one stores no entry points).
 */
PRISM_ES2_EXPORT void*   es2_context_get_proc_address(void* ctx, const char* name);

/* ------------------------------------------------------------------------------------------------
 * State setters. All take the render ContextInfo*; see the file comment for the ones that
 * ignore it.
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT void    es2_active_texture(void* ctx, int32_t tex_unit);          /* GL_TEXTURE0 + tex_unit */
PRISM_ES2_EXPORT void    es2_bind_fbo(void* ctx, int32_t fbo_id);                  /* also caches state.fbo */
PRISM_ES2_EXPORT void    es2_bind_texture(void* ctx, int32_t tex_id);              /* GL_TEXTURE_2D; ctx ignored */
/* s_factor / d_factor: GL enum blend factors, passed straight to glBlendFunc. ctx ignored. */
PRISM_ES2_EXPORT void    es2_blend_func(void* ctx, int32_t s_factor, int32_t d_factor);
PRISM_ES2_EXPORT void    es2_clear_buffers(void* ctx, float r, float g, float b, float a,
                                           int32_t clear_color, int32_t clear_depth, int32_t ignore_scissor);
PRISM_ES2_EXPORT void    es2_scissor_test(void* ctx, int32_t enable, int32_t x, int32_t y, int32_t w, int32_t h);
PRISM_ES2_EXPORT void    es2_set_depth_test(void* ctx, int32_t enable);
PRISM_ES2_EXPORT void    es2_set_msaa(void* ctx, int32_t enable);
PRISM_ES2_EXPORT void    es2_update_viewport(void* ctx, int32_t x, int32_t y, int32_t w, int32_t h);
/* min_filter / mag_filter: GL enum filters (GL_NEAREST, GL_LINEAR, GL_*_MIPMAP_*); mag is set first. ctx ignored. */
PRISM_ES2_EXPORT void    es2_tex_params_min_max(void* ctx, int32_t min_filter, int32_t mag_filter);
/* linear: boolean -> GL_LINEAR / GL_NEAREST for MIN then MAG of the bound GL_TEXTURE_2D. ctx and tex_id ignored. */
PRISM_ES2_EXPORT void    es2_update_filter_state(void* ctx, int32_t tex_id, int32_t linear);
/* wrap_mode: GL enum (GL_REPEAT, GL_CLAMP_TO_EDGE, GL_CLAMP_TO_BORDER) for WRAP_S and WRAP_T.
 * ctx and tex_id ignored. */
PRISM_ES2_EXPORT void    es2_update_wrap_state(void* ctx, int32_t tex_id, int32_t wrap_mode);
/* pname: GL enum (GL_UNPACK_ALIGNMENT, GL_UNPACK_ROW_LENGTH, GL_UNPACK_SKIP_PIXELS, GL_UNPACK_SKIP_ROWS).
 * ctx ignored. */
PRISM_ES2_EXPORT void    es2_pixel_storei(void* ctx, int32_t pname, int32_t value);
PRISM_ES2_EXPORT void    es2_use_program(void* ctx, int32_t program_id);
PRISM_ES2_EXPORT void    es2_enable_vertex_attributes(void* ctx);                  /* attributes 0..3 */
PRISM_ES2_EXPORT void    es2_disable_vertex_attributes(void* ctx);                 /* attributes 0..3 */
PRISM_ES2_EXPORT void    es2_set_index_buffer(void* ctx, int32_t buffer_id);       /* GL_ELEMENT_ARRAY_BUFFER */
PRISM_ES2_EXPORT void    es2_set_device_parameters_2d(void* ctx);
PRISM_ES2_EXPORT void    es2_set_device_parameters_3d(void* ctx);
PRISM_ES2_EXPORT void    es2_finish(void* ctx);                                    /* glFinish; BLOCKING; ctx ignored */
/* pname: GL enum for glGetIntegerv (GL_MAX_TEXTURE_SIZE, GL_MAX_VERTEX_ATTRIBS, ...). ctx ignored. */
PRISM_ES2_EXPORT int32_t es2_get_int_param(void* ctx, int32_t pname);
PRISM_ES2_EXPORT int32_t es2_get_max_sample_size(void* ctx);                       /* GL_MAX_SAMPLES; ctx ignored */
PRISM_ES2_EXPORT int32_t es2_get_fbo(void* ctx);               /* GL_FRAMEBUFFER_BINDING; ctx ignored */
PRISM_ES2_EXPORT int32_t es2_gen_and_bind_texture(void* ctx);  /* new GL_TEXTURE_2D name; ctx ignored */
PRISM_ES2_EXPORT void    es2_uniform1f(void* ctx, int32_t loc, float v0);
PRISM_ES2_EXPORT void    es2_uniform2f(void* ctx, int32_t loc, float v0, float v1);
PRISM_ES2_EXPORT void    es2_uniform3f(void* ctx, int32_t loc, float v0, float v1, float v2);
PRISM_ES2_EXPORT void    es2_uniform4f(void* ctx, int32_t loc, float v0, float v1, float v2, float v3);
PRISM_ES2_EXPORT void    es2_uniform1i(void* ctx, int32_t loc, int32_t v0);
PRISM_ES2_EXPORT void    es2_uniform2i(void* ctx, int32_t loc, int32_t v0, int32_t v1);
PRISM_ES2_EXPORT void    es2_uniform3i(void* ctx, int32_t loc, int32_t v0, int32_t v1, int32_t v2);
PRISM_ES2_EXPORT void    es2_uniform4i(void* ctx, int32_t loc, int32_t v0, int32_t v1, int32_t v2, int32_t v3);
/* values: count vec4s (Java applies any buffer offset by slicing); may be NULL. critical ok. */
PRISM_ES2_EXPORT void    es2_uniform4fv(void* ctx, int32_t loc, int32_t count, const float* values);
PRISM_ES2_EXPORT void    es2_uniform4iv(void* ctx, int32_t loc, int32_t count, const int32_t* values);
/* m16: one 4x4 matrix (16 floats); transpose is a boolean. critical ok. */
PRISM_ES2_EXPORT void    es2_uniform_matrix4fv(void* ctx, int32_t loc, int32_t transpose, const float* m16);

/* ------------------------------------------------------------------------------------------------
 * Resource creation / deletion. Creation BLOCKS (GPU allocation) and is never critical.
 * ---------------------------------------------------------------------------------------------- */

/* GL_RGBA / GL_UNSIGNED_BYTE GL_TEXTURE_2D of width x height with GL_LINEAR filters; 0 on any GL error. */
PRISM_ES2_EXPORT int32_t es2_texture_create(void* ctx, int32_t width, int32_t height);
PRISM_ES2_EXPORT void    es2_texture_delete(void* ctx, int32_t tex_id);            /* ctx ignored; 0 is a no-op */
/* New FBO, left bound, with tex_id attached as GL_COLOR_ATTACHMENT0 when tex_id != 0; 0 (and the FBO
 * deleted) when the FBO is incomplete. */
PRISM_ES2_EXPORT int32_t es2_fbo_create(void* ctx, int32_t tex_id);
PRISM_ES2_EXPORT void    es2_fbo_delete(void* ctx, int32_t fbo_id);
/* Renderbuffer attached to the bound FBO as GL_DEPTH_ATTACHMENT / GL_COLOR_ATTACHMENT0; msaa samples or 0;
 * 0 on failure. */
PRISM_ES2_EXPORT int32_t es2_depth_buffer_create(void* ctx, int32_t w, int32_t h, int32_t msaa);
PRISM_ES2_EXPORT int32_t es2_render_buffer_create(void* ctx, int32_t w, int32_t h, int32_t msaa);
PRISM_ES2_EXPORT void    es2_render_buffer_delete(void* ctx, int32_t rb_id);
/* glBlitFramebuffer(GL_COLOR_BUFFER_BIT, GL_LINEAR) from src_fbo to dst_fbo (0 = the cached state.fbo). */
PRISM_ES2_EXPORT void    es2_blit(void* ctx, int32_t src_fbo, int32_t dst_fbo,
                                  int32_t sx0, int32_t sy0, int32_t sx1, int32_t sy1,
                                  int32_t dx0, int32_t dy0, int32_t dx1, int32_t dy1);
/* GL_ELEMENT_ARRAY_BUFFER of n unsigned shorts (GL_STATIC_DRAW), left bound; 0 on failure / NULL data. critical ok. */
PRISM_ES2_EXPORT int32_t es2_index_buffer16_create(void* ctx, const int16_t* data, int32_t n);

/* ------------------------------------------------------------------------------------------------
 * Texture upload / readback. pixels may be NULL (GL allocates only); Java applies the byte
 * offset by slicing the segment. target, internal_format, format and type are GL enums.
 * ---------------------------------------------------------------------------------------------- */

/* Replaces nTexImage2D0/1. Sets GL_GENERATE_MIPMAP first when use_mipmap; returns 1 when glGetError is
 * clean. critical ok. */
PRISM_ES2_EXPORT int32_t es2_tex_image_2d(void* ctx, int32_t target, int32_t level, int32_t internal_format,
                                          int32_t w, int32_t h, int32_t border, int32_t format, int32_t type,
                                          const void* pixels, int32_t use_mipmap);
/* Replaces nTexSubImage2D0/1. critical ok. */
PRISM_ES2_EXPORT void    es2_tex_sub_image_2d(void* ctx, int32_t target, int32_t level, int32_t xoff, int32_t yoff,
                                              int32_t w, int32_t h, int32_t format, int32_t type,
                                              const void* pixels);
/*
 * Replaces nReadPixelsByte / nReadPixelsInt. dst receives w x h BGRA pixels as 32-bit
 * GL_UNSIGNED_INT_8_8_8_8_REV values; dst_len_bytes is checked as (len / 4 / w) < h -> 0. Returns 1
 * on success, 0 (with the JNI's stderr text) for a NULL ctx, w or h <= 0, a too-small buffer or a
 * NULL dst. BLOCKING (drains the GPU) - see critical(true) in the file comment.
 */
PRISM_ES2_EXPORT int32_t es2_read_pixels(void* ctx, void* dst, int32_t dst_len_bytes,
                                         int32_t x, int32_t y, int32_t w, int32_t h);

/* ------------------------------------------------------------------------------------------------
 * Shaders. UTF-8 strings. Compile / link BLOCK in the driver and are never critical.
 * ---------------------------------------------------------------------------------------------- */

/* glCreateShader(is_vertex ? GL_VERTEX_SHADER : GL_FRAGMENT_SHADER) + source + compile; 0 (shader deleted,
 * log on stderr) on failure. */
PRISM_ES2_EXPORT int32_t es2_shader_compile(void* ctx, const char* source, int32_t is_vertex);
/*
 * Program from vert_id plus n_frag fragment shaders, with attr_names[i] bound to location
 * attr_indices[i] for i < n_attrs, then linked. frag_ids, attr_names and attr_indices must be
 * non-NULL (empty arrays are fine); NULL -> 0. On link failure the log is printed, the caller's
 * shader objects are detached and DELETED, the program is deleted and 0 is returned, as before.
 */
PRISM_ES2_EXPORT int32_t es2_program_create(void* ctx, int32_t vert_id, const int32_t* frag_ids, int32_t n_frag,
                                            const char* const* attr_names, const int32_t* attr_indices,
                                            int32_t n_attrs);
PRISM_ES2_EXPORT void    es2_shader_delete(void* ctx, int32_t shader_id);          /* 0 is a no-op */
/* Detaches + deletes vert_id (if != 0) and every non-zero frag id, then deletes the program; a NULL
 * frag_ids returns before the program is deleted, as before. */
PRISM_ES2_EXPORT void    es2_shaders_dispose(void* ctx, int32_t program_id, int32_t vert_id,
                                             const int32_t* frag_ids, int32_t n_frag);
/* glGetUniformLocation; returns 0 (not -1) for a NULL ctx or name, as before. */
PRISM_ES2_EXPORT int32_t es2_get_uniform_location(void* ctx, int32_t program_id, const char* name);

/* ------------------------------------------------------------------------------------------------
 * 2D draw (hot)
 * ---------------------------------------------------------------------------------------------- */

/*
 * Client-side arrays: coords holds num_vertices * 7 floats (xyz, uv0, uv1), colors num_vertices * 4
 * bytes; glVertexAttribPointer is re-issued only when an array's address differs from the cached
 * one, then glDrawElements(GL_TRIANGLES, num_vertices / 4 * 6, GL_UNSIGNED_SHORT). Either NULL:
 * no-op. critical ok (the address cache is a hint, exactly as under JNI).
 */
PRISM_ES2_EXPORT void    es2_draw_indexed_quads(void* ctx, int32_t num_vertices,
                                                const float* coords, const uint8_t* colors);

/* ------------------------------------------------------------------------------------------------
 * 3D mesh. Replaces nCreateES2Mesh, nReleaseES2Mesh, nBuildNativeGeometry{Short,Int} and,
 * folded into es2_mesh_render, nCreateES2MeshView / nReleaseES2MeshView / nSetCullingMode /
 * nSetWireframe / nSetMaterial / nRenderMeshView. The phong-material and light setters
 * were dead stores and have no symbol.
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT void*   es2_mesh_create(void* ctx);     /* MeshInfo* with 2 VBO names; NULL on failure */
PRISM_ES2_EXPORT void    es2_mesh_release(void* ctx, void* mesh);                  /* glDeleteBuffers + free */
/*
 * Uploads vb_len floats (interleaved xyz, uv, nxnynznw = 9 per vertex) and ib_len indices with
 * GL_STATIC_DRAW and records the index count / type. The lengths are ELEMENT counts of the data
 * actually present (the JNI's array-length check has no equivalent; Java guarantees the arrays
 * are at least that long). Returns 1, or 0 for NULL handles / data, negative lengths or missing
 * VBO names. critical ok.
 */
PRISM_ES2_EXPORT int32_t es2_mesh_build_geometry_short(void* ctx, void* mesh, const float* vb, int32_t vb_len,
                                                       const int16_t* ib, int32_t ib_len);
PRISM_ES2_EXPORT int32_t es2_mesh_build_geometry_int(void* ctx, void* mesh, const float* vb, int32_t vb_len,
                                                     const int32_t* ib, int32_t ib_len);
/*
 * Draws the mesh with the three values MeshViewInfo used to carry: cull_enable (boolean),
 * cull_mode_gl (GL enum GL_BACK / GL_FRONT) and fill_mode_gl (GL enum GL_FILL / GL_LINE), applied
 * through the same state cache as before. Java calls this only when a material is set, which is
 * what the NULL phongMaterialInfo check in nRenderMeshView did. NULL ctx or mesh: no-op.
 */
PRISM_ES2_EXPORT void    es2_mesh_render(void* ctx, void* mesh, int32_t cull_enable, int32_t cull_mode_gl,
                                         int32_t fill_mode_gl);

#ifdef __cplusplus
}
#endif

#endif /* PRISM_ES2_API_H */
