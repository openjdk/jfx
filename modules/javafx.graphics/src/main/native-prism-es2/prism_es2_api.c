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
 * prism_es2_api.c - the platform-neutral part of the flat C ABI declared in prism_es2_api.h.
 *
 * Every body here is the corresponding Java_com_sun_prism_es2_GLContext_n* / GLFactory_n* body from
 * GLContext.c and GLFactory.c with the JNI marshalling removed: the handle macros become casts,
 * Get*ArrayCritical / GetDirectBufferAddress become the pointer Java now passes, strJavaToC becomes
 * the UTF-8 pointer, and the Prism-constant translation tables (translatePrismToGL,
 * translateScaleFactor, translatePixelStore) are not called because the ABI takes real GL enums
 * (prism_es2_api.h, "GL enums"). The shared helpers are the ones GLContext.c defines with external linkage
 * (initState, clearBuffers, bindFBO, checkFramebufferStatus, createAndAttachRenderBuffer,
 * setCullMode, setPolyonMode, deleteCtxInfo) and GLPixelFormat.c's deletePixelFormatInfo, reached
 * through the extern declarations below - the idiom windows/WinGLContext.c already uses. The
 * vertex-attribute helper setVertexAttributePointers is file-static here. The platform lifecycle
 * functions (factory, pixel format, drawable, context) live in
 * windows/prism_es2_api_win.c, x11/prism_es2_api_x11.c, macosx/prism_es2_api_mac.m and, as stubs,
 * monocle/prism_es2_api_monocle.c. es2_context_adopt (the Monocle way in: Java owns the EGL
 * context) is here because it needs only glGetString and es2_proc_table, so every platform's
 * library exports the same symbol set.
 */

#include "prism_es2_api.h"

#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "PrismES2Defs.h"

/* ------------------------------------------------------------------------------------------------
 * Compile-time guards
 * ---------------------------------------------------------------------------------------------- */

/* MSVC accepts _Static_assert in C only under /std:c11, which win.cmake does not pass: use the
 * negative-array-size idiom there (and on any other pre-C11 compiler). */
#if defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
#  define ES2_STATIC_ASSERT(cond, tag) _Static_assert(cond, #tag)
#else
#  define ES2_STATIC_ASSERT(cond, tag) typedef char es2_static_assert_##tag[(cond) ? 1 : -1]
#endif

ES2_STATIC_ASSERT(sizeof(Es2PixelFormatAttrs) == 28, Es2PixelFormatAttrs_is_28_bytes);
ES2_STATIC_ASSERT(sizeof(Es2PixelFormatAttrs) == ES2_PIXEL_FORMAT_ATTR_COUNT * sizeof(int32_t),
        Es2PixelFormatAttrs_is_seven_int32);
ES2_STATIC_ASSERT(offsetof(Es2PixelFormatAttrs, red_size) == 0, Es2PixelFormatAttrs_red_size_offset);
ES2_STATIC_ASSERT(offsetof(Es2PixelFormatAttrs, green_size) == 4, Es2PixelFormatAttrs_green_size_offset);
ES2_STATIC_ASSERT(offsetof(Es2PixelFormatAttrs, blue_size) == 8, Es2PixelFormatAttrs_blue_size_offset);
ES2_STATIC_ASSERT(offsetof(Es2PixelFormatAttrs, alpha_size) == 12, Es2PixelFormatAttrs_alpha_size_offset);
ES2_STATIC_ASSERT(offsetof(Es2PixelFormatAttrs, depth_size) == 16, Es2PixelFormatAttrs_depth_size_offset);
ES2_STATIC_ASSERT(offsetof(Es2PixelFormatAttrs, double_buffer) == 20, Es2PixelFormatAttrs_double_buffer_offset);
ES2_STATIC_ASSERT(offsetof(Es2PixelFormatAttrs, on_screen) == 24, Es2PixelFormatAttrs_on_screen_offset);
ES2_STATIC_ASSERT(sizeof(GLboolean) == 1, GLboolean_is_one_byte);
ES2_STATIC_ASSERT(sizeof(GLenum) == sizeof(int32_t), GLenum_is_32_bits);
ES2_STATIC_ASSERT(sizeof(GLint) == sizeof(int32_t), GLint_is_32_bits);
ES2_STATIC_ASSERT(sizeof(GLfloat) == sizeof(float), GLfloat_is_float);

/* ------------------------------------------------------------------------------------------------
 * Helpers defined with external linkage in GLContext.c but not declared in PrismES2Defs.h.
 * ---------------------------------------------------------------------------------------------- */

extern void deleteCtxInfo(ContextInfo *ctxInfo);
extern void clearBuffers(ContextInfo *ctxInfo,
        GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha,
        GLboolean clearColor, GLboolean clearDepth, GLboolean ignoreScissor);
extern void bindFBO(ContextInfo *ctxInfo, GLuint fboId);
extern int checkFramebufferStatus(ContextInfo *ctxInfo);
extern GLuint createAndAttachRenderBuffer(ContextInfo *ctxInfo, GLsizei width, GLsizei height,
        GLsizei msaa, GLenum attachment);
extern void setCullMode(ContextInfo *ctxInfo, MeshViewInfo *mvInfo);
extern void setPolyonMode(ContextInfo *ctxInfo, MeshViewInfo *mvInfo);

#define ES2_BOOL(x) ((GLboolean) ((x) != 0))

/* MSVC spells the POSIX strdup _strdup and flags the POSIX name (C4996); the platform files already
 * use each spelling on its own platform. */
#if defined(_WIN32)
#  define es2_strdup _strdup
#else
#  define es2_strdup strdup
#endif

/* ------------------------------------------------------------------------------------------------
 * ABI guard / layout checks
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT uint32_t
es2_abi_version(void) {
    return ES2_ABI_VERSION;
}

PRISM_ES2_EXPORT int64_t
es2_sizeof_pixel_format_attrs(void) {
    return (int64_t) sizeof(Es2PixelFormatAttrs);
}

/* The GLenum names resolved through the same headers the library compiles against, in EXACTLY the
 * order of the "GL enums" comment in prism_es2_api.h - the order ES2GLEnumTableTest.TABLE holds. */
static const GLenum ES2_GL_ENUMS[] = {
    /* blend factors (translateScaleFactor), 15 */
    GL_ZERO, GL_ONE, GL_SRC_COLOR, GL_ONE_MINUS_SRC_COLOR, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA,
    GL_DST_ALPHA, GL_ONE_MINUS_DST_ALPHA, GL_DST_COLOR, GL_ONE_MINUS_DST_COLOR, GL_SRC_ALPHA_SATURATE,
    GL_CONSTANT_COLOR, GL_ONE_MINUS_CONSTANT_COLOR, GL_CONSTANT_ALPHA, GL_ONE_MINUS_CONSTANT_ALPHA,
    /* pixel types (translatePrismToGL), 5 */
    GL_FLOAT, GL_UNSIGNED_BYTE, GL_UNSIGNED_INT_8_8_8_8_REV, GL_UNSIGNED_INT_8_8_8_8,
    GL_UNSIGNED_SHORT_8_8_APPLE,
    /* pixel formats, 7 */
    GL_RGBA, GL_BGRA, GL_RGB, GL_LUMINANCE, GL_ALPHA, GL_RGBA32F, GL_YCBCR_422_APPLE,
    /* textures, 6 */
    GL_TEXTURE_2D, GL_TEXTURE_BINDING_2D, GL_NEAREST, GL_LINEAR, GL_NEAREST_MIPMAP_NEAREST,
    GL_LINEAR_MIPMAP_LINEAR,
    /* wrap modes (WRAPMODE_*), 3 */
    GL_REPEAT, GL_CLAMP_TO_EDGE, GL_CLAMP_TO_BORDER,
    /* pixel store (translatePixelStore), 4 */
    GL_UNPACK_ALIGNMENT, GL_UNPACK_ROW_LENGTH, GL_UNPACK_SKIP_PIXELS, GL_UNPACK_SKIP_ROWS,
    /* glGetIntegerv names, 10 */
    GL_MAX_FRAGMENT_UNIFORM_COMPONENTS, GL_MAX_FRAGMENT_UNIFORM_VECTORS, GL_MAX_TEXTURE_IMAGE_UNITS,
    GL_MAX_TEXTURE_SIZE, GL_MAX_VARYING_COMPONENTS, GL_MAX_VARYING_VECTORS, GL_MAX_VERTEX_ATTRIBS,
    GL_MAX_VERTEX_UNIFORM_COMPONENTS, GL_MAX_VERTEX_UNIFORM_VECTORS, GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS
};

ES2_STATIC_ASSERT(sizeof(ES2_GL_ENUMS) / sizeof(ES2_GL_ENUMS[0]) == 50, ES2_GL_ENUMS_has_50_entries);

PRISM_ES2_EXPORT int32_t
es2_gl_enum_count(void) {
    return (int32_t) (sizeof(ES2_GL_ENUMS) / sizeof(ES2_GL_ENUMS[0]));
}

PRISM_ES2_EXPORT int32_t
es2_gl_enum(int32_t index) {
    if (index < 0 || index >= es2_gl_enum_count()) {
        return -1;
    }
    return (int32_t) ES2_GL_ENUMS[index];
}

/* ------------------------------------------------------------------------------------------------
 * Strings and entry points (former GLFactory natives)
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT int32_t
es2_context_get_string(void *ctx, int32_t kind, char *buf, int32_t cap) {
    const ContextInfo *ctxInfo = (const ContextInfo *) ctx;
    const char *str;
    size_t len;

    if (ctxInfo == NULL) {
        return -1;
    }
    switch (kind) {
        case ES2_STR_VENDOR:
            str = ctxInfo->vendorStr;
            break;
        case ES2_STR_RENDERER:
            str = ctxInfo->rendererStr;
            break;
        case ES2_STR_VERSION:
            str = ctxInfo->versionStr;
            break;
        case ES2_STR_EXTENSIONS:
            str = ctxInfo->glExtensionStr;
            break;
        default:
            return -1;
    }
    if (str == NULL) {
        return -1;
    }
    len = strlen(str);
    if (len > (size_t) INT32_MAX) {
        return -1;
    }
    if ((cap > 0) && (buf != NULL)) {
        size_t n = (size_t) cap - 1;
        if (n > len) {
            n = len;
        }
        memcpy(buf, str, n);
        buf[n] = '\0';
    }
    return (int32_t) len;
}

/* Name -> ContextInfo member of every entry point es2_context_create resolves (at commit 868c4801ec:
 * WinGLContext.c:183-282, X11GLContext.c:171-270, MacGLContext.c:158-257) plus the platform swap-interval function. */
typedef struct Es2ProcEntry {
    const char *name;
    size_t offset;
} Es2ProcEntry;

#define ES2_PROC(member) { #member, offsetof(ContextInfo, member) }

static const Es2ProcEntry es2_proc_table[] = {
    ES2_PROC(glActiveTexture),
    ES2_PROC(glAttachShader),
    ES2_PROC(glBindAttribLocation),
    ES2_PROC(glBindFramebuffer),
    ES2_PROC(glBindRenderbuffer),
    ES2_PROC(glCheckFramebufferStatus),
    ES2_PROC(glCompileShader),
    ES2_PROC(glCreateProgram),
    ES2_PROC(glCreateShader),
    ES2_PROC(glDeleteBuffers),
    ES2_PROC(glDeleteFramebuffers),
    ES2_PROC(glDeleteProgram),
    ES2_PROC(glDeleteShader),
    ES2_PROC(glDeleteRenderbuffers),
    ES2_PROC(glDetachShader),
    ES2_PROC(glDisableVertexAttribArray),
    ES2_PROC(glEnableVertexAttribArray),
    ES2_PROC(glFramebufferRenderbuffer),
    ES2_PROC(glFramebufferTexture2D),
    ES2_PROC(glGenFramebuffers),
    ES2_PROC(glGenRenderbuffers),
    ES2_PROC(glGetProgramiv),
    ES2_PROC(glGetShaderiv),
    ES2_PROC(glGetUniformLocation),
    ES2_PROC(glLinkProgram),
    ES2_PROC(glRenderbufferStorage),
    ES2_PROC(glShaderSource),
    ES2_PROC(glGetShaderInfoLog),
    ES2_PROC(glGetProgramInfoLog),
    ES2_PROC(glBufferSubData),
    ES2_PROC(glUniform1f),
    ES2_PROC(glUniform2f),
    ES2_PROC(glUniform3f),
    ES2_PROC(glUniform4f),
    ES2_PROC(glUniform4fv),
    ES2_PROC(glUniform1i),
    ES2_PROC(glUniform2i),
    ES2_PROC(glUniform3i),
    ES2_PROC(glUniform4i),
    ES2_PROC(glUniform4iv),
    ES2_PROC(glUniformMatrix4fv),
    ES2_PROC(glUseProgram),
    ES2_PROC(glValidateProgram),
    ES2_PROC(glVertexAttribPointer),
    ES2_PROC(glGenBuffers),
    ES2_PROC(glBindBuffer),
    ES2_PROC(glBufferData),
    ES2_PROC(glTexImage2DMultisample),
    ES2_PROC(glRenderbufferStorageMultisample),
    ES2_PROC(glBlitFramebuffer),
#ifdef WIN32
    ES2_PROC(wglSwapIntervalEXT),
#endif
#if defined(UNIX) && !defined(IS_EGL)
    ES2_PROC(glXSwapIntervalSGI),   /* PrismES2Defs.h has no GLX member in the EGL (Monocle) build */
#endif
};

PRISM_ES2_EXPORT void *
es2_context_get_proc_address(void *ctx, const char *name) {
    const ContextInfo *ctxInfo = (const ContextInfo *) ctx;
    size_t i;

    if ((ctxInfo == NULL) || (name == NULL)) {
        return NULL;
    }
    for (i = 0; i < sizeof(es2_proc_table) / sizeof(es2_proc_table[0]); i++) {
        if (strcmp(es2_proc_table[i].name, name) == 0) {
            /* Every member is a function pointer; read it through a generic one. */
            void (*proc)(void);
            memcpy(&proc, (const char *) ctxInfo + es2_proc_table[i].offset, sizeof(proc));
            return (void *) proc;
        }
    }
    return NULL;
}

/*
 * Java_com_sun_prism_es2_MonocleGLFactory_nPopulateNativeCtxInfo of MonocleGLFactory.c at commit
 * 21d5a654f6 with the JNI removed: its 50 GET_DLSYM(handle, "gl...") lines are the 50 GL rows of
 * es2_proc_table, resolved here through the caller's loader instead of
 * dlsym(handle != 0 ? handle : RTLD_DEFAULT, name), and a NULL result is stored as before (that
 * get_dlsym ran with warn = 0). The only differences are on paths where the JNI crashed: the loader
 * and the GL_VERSION / GL_EXTENSIONS strings are checked before anything is allocated.
 */
PRISM_ES2_EXPORT void *
es2_context_adopt(Es2ProcLoader loader, void *user) {
    ContextInfo *ctxInfo = NULL;
    const char *glVersion;
    const char *glVendor;
    const char *glRenderer;
    const char *glExtensions;
    char *tmpVersionStr;
    int versionNumbers[2];
    size_t i;

    if (loader == NULL) {
        fprintf(stderr, "Prism ES2 Error - es2_context_adopt: loader == NULL\n");
        return NULL;
    }

    /* The driver strings of the context the caller made current on this thread. */
    glVersion = (const char *) glGetString(GL_VERSION);
    if (glVersion == NULL) {
        fprintf(stderr, "Prism ES2 Error - es2_context_adopt: glGetString(GL_VERSION) == NULL"
                " (no context is current on this thread?)\n");
        return NULL;
    }
    glVendor = (const char *) glGetString(GL_VENDOR);
    if (glVendor == NULL) {
        glVendor = "<UNKNOWN>";
    }
    glRenderer = (const char *) glGetString(GL_RENDERER);
    if (glRenderer == NULL) {
        glRenderer = "<UNKNOWN>";
    }
    glExtensions = (const char *) glGetString(GL_EXTENSIONS);
    if (glExtensions == NULL) {
        fprintf(stderr, "Prism ES2 Error - es2_context_adopt: glGetString(GL_EXTENSIONS) == NULL\n");
        return NULL;
    }

    /* find out the version, major and minor version number */
    tmpVersionStr = es2_strdup(glVersion);
    extractVersionInfo(tmpVersionStr, versionNumbers);
    free(tmpVersionStr);

    /* Note: We are only storing the string information of a driver.
     Assuming a system with a single or homogeneous GPUs. For the case
     of heterogeneous GPUs system the string information will need to move to
     GLContext class. */
    /* allocate the structure */
    ctxInfo = (ContextInfo *) malloc(sizeof (ContextInfo));
    if (ctxInfo == NULL) {
        fprintf(stderr, "nInitialize: Failed in malloc\n");
        return NULL;
    }
    /* initialize the structure */
    initializeCtxInfo(ctxInfo);

    ctxInfo->versionStr = es2_strdup(glVersion);
    ctxInfo->vendorStr = es2_strdup(glVendor);
    ctxInfo->rendererStr = es2_strdup(glRenderer);
    ctxInfo->glExtensionStr = es2_strdup(glExtensions);
    ctxInfo->versionNumbers[0] = versionNumbers[0];
    ctxInfo->versionNumbers[1] = versionNumbers[1];
    /* The context / display members, state.vSyncEnabled, vSyncRequested and gl2 stay 0 from
     * initializeCtxInfo: the caller owns the context, and Monocle never applied a swap interval
     * or ran a GL 2 profile. */

    /* set function pointers */
    for (i = 0; i < sizeof(es2_proc_table) / sizeof(es2_proc_table[0]); i++) {
        /* Every member is a function pointer; store it through a generic one. */
        void (*proc)(void) = (void (*)(void)) loader(user, es2_proc_table[i].name);
        memcpy((char *) ctxInfo + es2_proc_table[i].offset, &proc, sizeof(proc));
    }

    initState(ctxInfo);
    return ctxInfo;
}

/* ------------------------------------------------------------------------------------------------
 * Release functions (new: the JNI never freed these)
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT void
es2_context_release(void *ctx) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if (ctxInfo == NULL) {
        return;
    }
    deleteCtxInfo(ctxInfo);
    free(ctxInfo);
}

PRISM_ES2_EXPORT void
es2_pixel_format_release(void *pf) {
    PixelFormatInfo *pfInfo = (PixelFormatInfo *) pf;
    if (pfInfo == NULL) {
        return;
    }
    deletePixelFormatInfo(pfInfo);
    free(pfInfo);
}

/* ------------------------------------------------------------------------------------------------
 * State setters (GLContext.c nActiveTexture .. nUniformMatrix4fv)
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT void
es2_active_texture(void *ctx, int32_t tex_unit) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glActiveTexture == NULL)) {
        return;
    }
    ctxInfo->glActiveTexture(GL_TEXTURE0 + tex_unit);
}

PRISM_ES2_EXPORT void
es2_bind_fbo(void *ctx, int32_t fbo_id) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    bindFBO(ctxInfo, (GLuint) fbo_id);
}

PRISM_ES2_EXPORT void
es2_bind_texture(void *ctx, int32_t tex_id) {
    (void) ctx; /* ignored, as nBindTexture did */
    glBindTexture(GL_TEXTURE_2D, (GLuint) tex_id);
}

PRISM_ES2_EXPORT void
es2_blend_func(void *ctx, int32_t s_factor, int32_t d_factor) {
    (void) ctx; /* nBlendFunc took no context */
    glBlendFunc((GLenum) s_factor, (GLenum) d_factor);
}

PRISM_ES2_EXPORT void
es2_clear_buffers(void *ctx, float r, float g, float b, float a,
        int32_t clear_color, int32_t clear_depth, int32_t ignore_scissor) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if (ctxInfo == NULL) {
        return;
    }

    clearBuffers(ctxInfo,
            (GLclampf) r, (GLclampf) g, (GLclampf) b, (GLclampf) a,
            ES2_BOOL(clear_color), ES2_BOOL(clear_depth), ES2_BOOL(ignore_scissor));
}

PRISM_ES2_EXPORT void
es2_scissor_test(void *ctx, int32_t enable, int32_t x, int32_t y, int32_t w, int32_t h) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if (ctxInfo == NULL) {
        return;
    }

    if (enable) {
        if (!ctxInfo->state.scissorEnabled) {
            glEnable(GL_SCISSOR_TEST);
            ctxInfo->state.scissorEnabled = GL_TRUE;
        }
        glScissor(x, y, w, h);
    } else if (ctxInfo->state.scissorEnabled) {
        glDisable(GL_SCISSOR_TEST);
        ctxInfo->state.scissorEnabled = GL_FALSE;
    }
}

PRISM_ES2_EXPORT void
es2_set_depth_test(void *ctx, int32_t enable) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if (ctxInfo == NULL) {
        return;
    }

    if (enable) {
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glDepthMask(GL_TRUE);
        ctxInfo->state.depthWritesEnabled = GL_TRUE;
    } else {
        glDisable(GL_DEPTH_TEST);
        glDepthMask(GL_FALSE);
        ctxInfo->state.depthWritesEnabled = GL_FALSE;
    }
}

PRISM_ES2_EXPORT void
es2_set_msaa(void *ctx, int32_t enable) {
#ifndef IS_EGL
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if (ctxInfo == NULL) {
        return;
    }

    if (enable) {
        glEnable(GL_MULTISAMPLE);
    } else {
        glDisable(GL_MULTISAMPLE);
    }
#else
    (void) ctx;
    (void) enable;
#endif
}

PRISM_ES2_EXPORT void
es2_update_viewport(void *ctx, int32_t x, int32_t y, int32_t w, int32_t h) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if (ctxInfo == NULL) {
        return;
    }

    glViewport((GLint) x, (GLint) y, (GLsizei) w, (GLsizei) h);
}

PRISM_ES2_EXPORT void
es2_tex_params_min_max(void *ctx, int32_t min_filter, int32_t mag_filter) {
    (void) ctx; /* nTexParamsMinMax took no context */
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, (GLint) mag_filter);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, (GLint) min_filter);
}

PRISM_ES2_EXPORT void
es2_update_filter_state(void *ctx, int32_t tex_id, int32_t linear) {
    int glFilter;

    (void) ctx;    /* ignored, as nUpdateFilterState did */
    (void) tex_id;
    glFilter = linear ? GL_LINEAR : GL_NEAREST;
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, glFilter);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, glFilter);
}

PRISM_ES2_EXPORT void
es2_update_wrap_state(void *ctx, int32_t tex_id, int32_t wrap_mode) {
    (void) ctx;    /* ignored, as nUpdateWrapState did */
    (void) tex_id;
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, (GLint) wrap_mode);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, (GLint) wrap_mode);
}

PRISM_ES2_EXPORT void
es2_pixel_storei(void *ctx, int32_t pname, int32_t value) {
    (void) ctx; /* nPixelStorei took no context */
    glPixelStorei((GLenum) pname, (GLint) value);
}

PRISM_ES2_EXPORT void
es2_use_program(void *ctx, int32_t program_id) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glUseProgram == NULL)) {
        return;
    }
    ctxInfo->glUseProgram((GLuint) program_id);
}

PRISM_ES2_EXPORT void
es2_enable_vertex_attributes(void *ctx) {
    int i;
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glEnableVertexAttribArray == NULL)) {
        return;
    }

    for (i = 0; i != 4; ++i) {
        ctxInfo->glEnableVertexAttribArray(i);
    }
}

PRISM_ES2_EXPORT void
es2_disable_vertex_attributes(void *ctx) {
    int i;
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glDisableVertexAttribArray == NULL)) {
        return;
    }

    for (i = 0; i != 4; ++i) {
        ctxInfo->glDisableVertexAttribArray(i);
    }
}

PRISM_ES2_EXPORT void
es2_set_index_buffer(void *ctx, int32_t buffer_id) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glBindBuffer == NULL)) {
        return;
    }
    ctxInfo->glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, (GLuint) buffer_id);
}

PRISM_ES2_EXPORT void
es2_set_device_parameters_2d(void *ctx) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glBindBuffer == NULL) ||
            (ctxInfo->glBufferData == NULL) ||
            (ctxInfo->glDisableVertexAttribArray == NULL)) {
        return;
    }

    // Disable 3D states
    ctxInfo->glBindBuffer(GL_ARRAY_BUFFER, 0);
    ctxInfo->glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    ctxInfo->glDisableVertexAttribArray(VC_3D_INDEX);
    ctxInfo->glDisableVertexAttribArray(NC_3D_INDEX);
    ctxInfo->glDisableVertexAttribArray(TC_3D_INDEX);

    ctxInfo->vbFloatData = NULL;
    ctxInfo->vbByteData = NULL;

    glEnable(GL_BLEND);
    glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

    if (ctxInfo->state.scissorEnabled) {
        ctxInfo->state.scissorEnabled = GL_FALSE;
        glDisable(GL_SCISSOR_TEST);
    }

    glCullFace(GL_BACK);
    ctxInfo->state.cullMode = GL_BACK;
    glDisable(GL_CULL_FACE);
    ctxInfo->state.cullEnable = GL_FALSE;
#ifndef IS_EGL
    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
#endif
    ctxInfo->state.fillMode = GL_FILL;
}

PRISM_ES2_EXPORT void
es2_set_device_parameters_3d(void *ctx) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if (ctxInfo == NULL) {
        return;
    }
    // Note: projViewTx and camPos are handled above in the Java layer

    // This setting matches 2D ((1,1-alpha); premultiplied alpha case.
    // Will need to evaluate when support proper 3D blending (alpha,1-alpha).
    glEnable(GL_BLEND);
    glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

    if (ctxInfo->state.scissorEnabled) {
        ctxInfo->state.scissorEnabled = GL_FALSE;
        glDisable(GL_SCISSOR_TEST);
    }

    glEnable(GL_CULL_FACE);
    ctxInfo->state.cullEnable = GL_TRUE;
    glCullFace(GL_BACK);
    ctxInfo->state.cullMode = GL_BACK;
    glFrontFace(GL_CW); // set clockwise order as front-facing
#ifndef IS_EGL
    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
#endif
    ctxInfo->state.fillMode = GL_FILL;
}

PRISM_ES2_EXPORT void
es2_finish(void *ctx) {
    (void) ctx; /* nFinish took no context */
    glFinish();
}

PRISM_ES2_EXPORT int32_t
es2_get_int_param(void *ctx, int32_t pname) {
    GLint param;

    (void) ctx; /* nGetIntParam took no context */
    glGetIntegerv((GLenum) pname, &param);
    return (int32_t) param;
}

PRISM_ES2_EXPORT int32_t
es2_get_max_sample_size(void *ctx) {
    GLint samples;

    (void) ctx; /* nGetMaxSampleSize took no context */
    glGetIntegerv(GL_MAX_SAMPLES, &samples);
    return (int32_t) samples;
}

PRISM_ES2_EXPORT int32_t
es2_get_fbo(void *ctx) {
    GLint param;

    (void) ctx; /* nGetFBO took no context */
    /* The caching logic has been done on Java side if platform isn't
     * MAC. On macOS Glass can change the FBO under us. We should be
     * able to simplify the logic in Java and remove this method once
     * Glass stop doing it.
     */
    glGetIntegerv(GL_FRAMEBUFFER_BINDING, &param);
    return (int32_t) param;
}

PRISM_ES2_EXPORT int32_t
es2_gen_and_bind_texture(void *ctx) {
    GLuint texID;

    (void) ctx; /* nGenAndBindTexture took no context */
    glGenTextures(1, &texID);
    glBindTexture(GL_TEXTURE_2D, texID);
    return (int32_t) texID;
}

PRISM_ES2_EXPORT void
es2_uniform1f(void *ctx, int32_t loc, float v0) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if (ctxInfo == NULL) {
        return;
    }
    ctxInfo->glUniform1f(loc, v0);
}

PRISM_ES2_EXPORT void
es2_uniform2f(void *ctx, int32_t loc, float v0, float v1) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if (ctxInfo == NULL) {
        return;
    }
    ctxInfo->glUniform2f(loc, v0, v1);
}

PRISM_ES2_EXPORT void
es2_uniform3f(void *ctx, int32_t loc, float v0, float v1, float v2) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if (ctxInfo == NULL) {
        return;
    }
    ctxInfo->glUniform3f(loc, v0, v1, v2);
}

PRISM_ES2_EXPORT void
es2_uniform4f(void *ctx, int32_t loc, float v0, float v1, float v2, float v3) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if (ctxInfo == NULL) {
        return;
    }
    ctxInfo->glUniform4f(loc, v0, v1, v2, v3);
}

PRISM_ES2_EXPORT void
es2_uniform1i(void *ctx, int32_t loc, int32_t v0) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glUniform1i == NULL)) {
        return;
    }
    ctxInfo->glUniform1i(loc, v0);
}

PRISM_ES2_EXPORT void
es2_uniform2i(void *ctx, int32_t loc, int32_t v0, int32_t v1) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glUniform2i == NULL)) {
        return;
    }
    ctxInfo->glUniform2i(loc, v0, v1);
}

PRISM_ES2_EXPORT void
es2_uniform3i(void *ctx, int32_t loc, int32_t v0, int32_t v1, int32_t v2) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glUniform3i == NULL)) {
        return;
    }
    ctxInfo->glUniform3i(loc, v0, v1, v2);
}

PRISM_ES2_EXPORT void
es2_uniform4i(void *ctx, int32_t loc, int32_t v0, int32_t v1, int32_t v2, int32_t v3) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glUniform4i == NULL)) {
        return;
    }
    ctxInfo->glUniform4i(loc, v0, v1, v2, v3);
}

/* nUniform4fv0 / nUniform4fv1 merged: Java hands over the (already offset) pointer. */
PRISM_ES2_EXPORT void
es2_uniform4fv(void *ctx, int32_t loc, int32_t count, const float *values) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if (ctxInfo == NULL) {
        return;
    }
    ctxInfo->glUniform4fv((GLint) loc, (GLsizei) count, (const GLfloat *) values);
}

/* nUniform4iv0 / nUniform4iv1 merged. */
PRISM_ES2_EXPORT void
es2_uniform4iv(void *ctx, int32_t loc, int32_t count, const int32_t *values) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glUniform4iv == NULL)) {
        return;
    }
    ctxInfo->glUniform4iv((GLint) loc, (GLsizei) count, (const GLint *) values);
}

PRISM_ES2_EXPORT void
es2_uniform_matrix4fv(void *ctx, int32_t loc, int32_t transpose, const float *m16) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glUniformMatrix4fv == NULL)) {
        return;
    }
    ctxInfo->glUniformMatrix4fv((GLint) loc, 1, ES2_BOOL(transpose), (const GLfloat *) m16);
}

/* ------------------------------------------------------------------------------------------------
 * Resource creation / deletion (GLContext.c nCreateTexture, nCreateFBO, nCreateDepthBuffer,
 * nCreateRenderBuffer, nBlit, nDelete*, nCreateIndexBuffer16)
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT int32_t
es2_texture_create(void *ctx, int32_t width, int32_t height) {
    GLuint texID = 0;
    GLenum err;

    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glActiveTexture == NULL)) {
        return 0;
    }

    glGenTextures(1, &texID);
    if (texID == 0) {
        // fprintf(stderr, "nCreateTexture: Failed to generate texture.\n");
        return (int32_t) texID;
    }

    glBindTexture(GL_TEXTURE_2D, texID);

    // Reset Error
    glGetError();
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height,
            0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);

    err  = glGetError();
    // printGLError(err);

    if (err != GL_NO_ERROR) {
        glDeleteTextures(1, &texID);
        texID = 0;
    } else {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    }
    return (int32_t) texID;
}

PRISM_ES2_EXPORT void
es2_texture_delete(void *ctx, int32_t tex_id) {
    GLuint tID = (GLuint) tex_id;
    (void) ctx; /* ignored, as nDeleteTexture did */
    if (tID != 0) {
        glDeleteTextures(1, &tID);
    }
}

PRISM_ES2_EXPORT int32_t
es2_fbo_create(void *ctx, int32_t tex_id) {
    GLuint fboID;

    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glGenFramebuffers == NULL)
            || (ctxInfo->glFramebufferTexture2D == NULL)
            || (ctxInfo->glCheckFramebufferStatus == NULL)
            || (ctxInfo->glDeleteFramebuffers == NULL)) {
        return 0;
    }

    // initialize framebuffer object
    ctxInfo->glGenFramebuffers(1, &fboID);
    bindFBO(ctxInfo, fboID);

    if (tex_id) {
        // Attach color texture to framebuffer object
        ctxInfo->glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                                        GL_TEXTURE_2D, (GLuint) tex_id, 0);
        // Can't check status of FBO until after a buffer is attached to it
        if (checkFramebufferStatus(ctxInfo)) {
            ctxInfo->glDeleteFramebuffers(1, &fboID);
            fprintf(stderr,
                    "Error creating framebuffer object with TexID %d)\n", (int) tex_id);
            return 0;
        }
        // explicitly clear the color buffer, since it may contain garbage
        // after initialization
        clearBuffers(ctxInfo, 0, 0, 0, 0, GL_TRUE, GL_FALSE, GL_TRUE);
    }

    return (int32_t) fboID;
}

PRISM_ES2_EXPORT void
es2_fbo_delete(void *ctx, int32_t fbo_id) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glDeleteFramebuffers == NULL)) {
        return;
    }
    if (fbo_id != 0) {
        ctxInfo->glDeleteFramebuffers(1, (GLuint *) &fbo_id);
    }
}

PRISM_ES2_EXPORT int32_t
es2_depth_buffer_create(void *ctx, int32_t w, int32_t h, int32_t msaa) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    return (int32_t) createAndAttachRenderBuffer(ctxInfo, w, h, msaa, GL_DEPTH_ATTACHMENT);
}

PRISM_ES2_EXPORT int32_t
es2_render_buffer_create(void *ctx, int32_t w, int32_t h, int32_t msaa) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    return (int32_t) createAndAttachRenderBuffer(ctxInfo, w, h, msaa, GL_COLOR_ATTACHMENT0);
}

PRISM_ES2_EXPORT void
es2_render_buffer_delete(void *ctx, int32_t rb_id) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glDeleteRenderbuffers == NULL)) {
        return;
    }
    if (rb_id != 0) {
        ctxInfo->glDeleteRenderbuffers(1, (GLuint *) &rb_id);
    }
}

PRISM_ES2_EXPORT void
es2_blit(void *ctx, int32_t src_fbo, int32_t dst_fbo,
        int32_t sx0, int32_t sy0, int32_t sx1, int32_t sy1,
        int32_t dx0, int32_t dy0, int32_t dx1, int32_t dy1) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glGenFramebuffers == NULL)
            || (ctxInfo->glBindFramebuffer == NULL)
            || (ctxInfo->glBlitFramebuffer == NULL)) {
        return;
    }

    // Temporarily disable scissor to avoid a problem with some GL drivers
    // that honor the scissor test if enabled
    if (ctxInfo->state.scissorEnabled) {
        glDisable(GL_SCISSOR_TEST);
    }

    if (dst_fbo == 0) {
        dst_fbo = (int32_t) ctxInfo->state.fbo;
    }
    //Bind the FBOs
    ctxInfo->glBindFramebuffer(GL_READ_FRAMEBUFFER, (GLuint) src_fbo);
    ctxInfo->glBindFramebuffer(GL_DRAW_FRAMEBUFFER, (GLuint) dst_fbo);
    ctxInfo->glBlitFramebuffer(sx0, sy0, sx1, sy1,
                               dx0, dy0, dx1, dy1,
                               GL_COLOR_BUFFER_BIT, GL_LINEAR);

    // Restore previous FBO
    ctxInfo->glBindFramebuffer(GL_FRAMEBUFFER, ctxInfo->state.fbo);

    // Restore previous scissor
    if (ctxInfo->state.scissorEnabled) {
        glEnable(GL_SCISSOR_TEST);
    }
}

PRISM_ES2_EXPORT int32_t
es2_index_buffer16_create(void *ctx, const int16_t *data, int32_t n) {
    GLuint id = 0;

    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glBindBuffer == NULL) ||
            (ctxInfo->glBufferData == NULL) || (ctxInfo->glGenBuffers == NULL)) {
        return 0;
    }

    if (data) {
        ctxInfo->glGenBuffers(1, &id);
        if (id) {
            ctxInfo->glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, id);
            ctxInfo->glBufferData(GL_ELEMENT_ARRAY_BUFFER, sizeof(short) * n, data, GL_STATIC_DRAW);
        }
    }
    return (int32_t) id;
}

/* ------------------------------------------------------------------------------------------------
 * Texture upload / readback (GLContext.c nTexImage2D0/1, nTexSubImage2D0/1, doReadPixels)
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT int32_t
es2_tex_image_2d(void *ctx, int32_t target, int32_t level, int32_t internal_format,
        int32_t w, int32_t h, int32_t border, int32_t format, int32_t type,
        const void *pixels, int32_t use_mipmap) {
    GLenum err;

    (void) ctx; /* nTexImage2D0/1 took no context */
    glGetError();
    if (use_mipmap) {
        glTexParameteri(GL_TEXTURE_2D, GL_GENERATE_MIPMAP, GL_TRUE);
    }

    // It is okay if pixels is null.
    // In this case, a call to glTexImage2D will cause texture memory to be allocated
    // to accommodate a texture of width and height.
    glTexImage2D((GLenum) target, (GLint) level, (GLint) internal_format,
            (GLsizei) w, (GLsizei) h, (GLint) border,
            (GLenum) format, (GLenum) type, (const GLvoid *) pixels);
    err  = glGetError();

    // printGLError(err);
    return err == GL_NO_ERROR ? 1 : 0;
}

PRISM_ES2_EXPORT void
es2_tex_sub_image_2d(void *ctx, int32_t target, int32_t level, int32_t xoff, int32_t yoff,
        int32_t w, int32_t h, int32_t format, int32_t type, const void *pixels) {
    (void) ctx; /* nTexSubImage2D0/1 took no context */
    glTexSubImage2D((GLenum) target, (GLint) level,
            (GLint) xoff, (GLint) yoff,
            (GLsizei) w, (GLsizei) h, (GLenum) format,
            (GLenum) type, (const GLvoid *) pixels);
}

/*
 * doReadPixels verbatim. Note that ContextInfo.gl2 is set only on the factory ContextInfo
 * ({Win,X11,Mac}GLFactory.c); the render ContextInfo this is called with never sets it, so the
 * GL_RGBA + swizzle branch is the one that runs on every desktop platform today. Both branches are
 * kept so the GL traffic is unchanged.
 */
PRISM_ES2_EXPORT int32_t
es2_read_pixels(void *ctx, void *dst, int32_t dst_len_bytes,
        int32_t x, int32_t y, int32_t w, int32_t h) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if (ctxInfo == NULL) {
        fprintf(stderr, "doReadPixels: ctxInfo is NULL\n");
        return 0;
    }

    if (w <= 0 || h <= 0) {
        fprintf(stderr, "doReadPixels: width or height is <= 0\n");
        return 0;
    }

    // sanity check, do we have enough memory
    // length, width and height are non-negative
    if ((dst_len_bytes / 4 / w) < h) {
        fprintf(stderr, "doReadPixels: pixel buffer too small - length = %d\n",
                (int) dst_len_bytes);
        return 0;
    }

    if (dst == NULL) {
        fprintf(stderr, "doReadPixels: pixel buffer is NULL\n");
        return 0;
    }

    if (ctxInfo->gl2) {
        glReadPixels((GLint) x, (GLint) y, (GLsizei) w, (GLsizei) h,
                    GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, dst);
    } else {
        int32_t i;
        GLubyte* c = (GLubyte*) dst;
        GLubyte temp;
        glReadPixels((GLint) x, (GLint) y, (GLsizei) w, (GLsizei) h,
                GL_RGBA, GL_UNSIGNED_BYTE, dst);

        for (i = 0; i < w * h; i++) {
            temp = c[0];
            c[0] = c[2];
            c[2] = temp;
            c += 4;
        }
    }

    return 1;
}

/* ------------------------------------------------------------------------------------------------
 * Shaders (GLContext.c nCompileShader, nCreateProgram, nDeleteShader, nDisposeShaders,
 * nGetUniformLocation)
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT int32_t
es2_shader_compile(void *ctx, const char *source, int32_t is_vertex) {
    GLenum shaderType;
    GLuint shaderID;
    GLint success;

    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (source == NULL)
            || (ctxInfo->glCreateShader == NULL)
            || (ctxInfo->glShaderSource == NULL)
            || (ctxInfo->glCompileShader == NULL)
            || (ctxInfo->glGetShaderiv == NULL)
            || (ctxInfo->glGetShaderInfoLog == NULL)
            || (ctxInfo->glDeleteShader == NULL)) {
        return 0;
    }

    // create the shader object and compile the shader source code
    shaderType = is_vertex ? GL_VERTEX_SHADER : GL_FRAGMENT_SHADER;
    shaderID = ctxInfo->glCreateShader(shaderType);
    ctxInfo->glShaderSource(shaderID, 1, (const GLchar **) &source, (GLint *) NULL);
    ctxInfo->glCompileShader(shaderID);
    ctxInfo->glGetShaderiv(shaderID, GL_COMPILE_STATUS, &success);

    if (success == GL_FALSE) {
        GLint  length;

        ctxInfo->glGetShaderiv(shaderID, GL_INFO_LOG_LENGTH, &length);
        if (length) {
            char* msg = (char *) malloc(length * sizeof(char));
            ctxInfo->glGetShaderInfoLog(shaderID, length, NULL, msg);
            fprintf(stderr, "Shader compile log: %.*s\n", length, msg);
            free(msg);
        } else {
            fprintf(stderr, "glCompileShader: GL_COMPILE_STATUS returns GL_FALSE but GL_INFO_LOG_LENGTH returns 0\n");
        }

        ctxInfo->glDeleteShader(shaderID);
        return 0;
    }

    return (int32_t) shaderID;
}

PRISM_ES2_EXPORT int32_t
es2_program_create(void *ctx, int32_t vert_id, const int32_t *frag_ids, int32_t n_frag,
        const char *const *attr_names, const int32_t *attr_indices, int32_t n_attrs) {
    GLuint shaderProgram;
    GLint success;
    int32_t i;
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (attr_names == NULL) || (attr_indices == NULL)
            || (ctxInfo->glCreateProgram == NULL)
            || (ctxInfo->glAttachShader == NULL)
            || (ctxInfo->glBindAttribLocation == NULL)
            || (ctxInfo->glLinkProgram == NULL)
            || (ctxInfo->glGetProgramiv == NULL)
            || (ctxInfo->glGetProgramInfoLog == NULL)
            || (ctxInfo->glDetachShader == NULL)
            || (ctxInfo->glDeleteShader == NULL)
            || (ctxInfo->glDeleteProgram == NULL)) {
        return 0;
    }

    if (frag_ids == NULL) {
        return 0;
    }
    // create the program object and attach it to the shader
    shaderProgram = ctxInfo->glCreateProgram();
    ctxInfo->glAttachShader(shaderProgram, (GLuint) vert_id);
    for (i = 0; i < n_frag; i++) {
        ctxInfo->glAttachShader(shaderProgram, (GLuint) frag_ids[i]);
    }

    // bind any user-defined index values to their corresponding names
    for (i = 0; i < n_attrs; i++) {
        ctxInfo->glBindAttribLocation(shaderProgram, (GLuint) attr_indices[i], attr_names[i]);
    }

    // link the program
    ctxInfo->glLinkProgram(shaderProgram);
    ctxInfo->glGetProgramiv(shaderProgram, GL_LINK_STATUS, &success);

    if (success == GL_FALSE) {
        GLint  logLength;

        ctxInfo->glGetProgramiv(shaderProgram, GL_INFO_LOG_LENGTH, &logLength);
        if (logLength) {
            char* msg  =  (char *) malloc(logLength * sizeof(char));
            ctxInfo->glGetProgramInfoLog(shaderProgram, logLength, NULL, msg);
            fprintf(stderr, "Program link log: %.*s\n", logLength, msg);
            free(msg);
        } else {
            fprintf(stderr, "glLinkProgram: GL_LINK_STATUS returns GL_FALSE but GL_INFO_LOG_LENGTH returns 0\n");
        }

        /* GLContext.c:582-602 at commit 868c4801ec shadowed the fragment count with the info-log length here and read
         * fragIDs[] past its end on a failed link; this loop uses the fragment count. */
        ctxInfo->glDetachShader(shaderProgram, (GLuint) vert_id);
        ctxInfo->glDeleteShader((GLuint) vert_id);
        for (i = 0; i < n_frag; i++) {
            ctxInfo->glDetachShader(shaderProgram, (GLuint) frag_ids[i]);
            ctxInfo->glDeleteShader((GLuint) frag_ids[i]);
        }
        ctxInfo->glDeleteProgram(shaderProgram);
        return 0;
    }

    return (int32_t) shaderProgram;
}

PRISM_ES2_EXPORT void
es2_shader_delete(void *ctx, int32_t shader_id) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glDeleteShader == NULL)) {
        return;
    }
    if (shader_id != 0) {
        ctxInfo->glDeleteShader((GLuint) shader_id);
    }
}

PRISM_ES2_EXPORT void
es2_shaders_dispose(void *ctx, int32_t program_id, int32_t vert_id,
        const int32_t *frag_ids, int32_t n_frag) {
    int32_t i;
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glDetachShader == NULL)
            || (ctxInfo->glDeleteShader == NULL)
            || (ctxInfo->glDeleteProgram == NULL)) {
        return;
    }

    if (vert_id != 0) {
        ctxInfo->glDetachShader((GLuint) program_id, (GLuint) vert_id);
        ctxInfo->glDeleteShader((GLuint) vert_id);
    }

    if (frag_ids == NULL) {
        return;
    }

    for (i = 0; i < n_frag; i++) {
        if (frag_ids[i] != 0) {
            ctxInfo->glDetachShader((GLuint) program_id, (GLuint) frag_ids[i]);
            ctxInfo->glDeleteShader((GLuint) frag_ids[i]);
        }
    }

    ctxInfo->glDeleteProgram((GLuint) program_id);
}

PRISM_ES2_EXPORT int32_t
es2_get_uniform_location(void *ctx, int32_t program_id, const char *name) {
    GLint result;
    ContextInfo *ctxInfo = (ContextInfo *) ctx;

    if ((ctxInfo == NULL) || (name == NULL)
            || (ctxInfo->glGetUniformLocation == NULL)) {
        return 0;
    }

    result = ctxInfo->glGetUniformLocation((GLuint) program_id, name);
    return (int32_t) result;
}

/* ------------------------------------------------------------------------------------------------
 * 2D draw (GLContext.c nDrawIndexedQuads)
 * ---------------------------------------------------------------------------------------------- */

#define FLOATS_PER_TC 2
#define FLOATS_PER_VC 3
#define FLOATS_PER_VERT (FLOATS_PER_TC * 2 + FLOATS_PER_VC)

#define coordStride (sizeof(float) * FLOATS_PER_VERT)
#define colorStride 4

/* NOTE: the ctx->vbFloatData and ctx->vbByteData pointers must be updated
 * whenever calling glVertexAttribPointer. Failing to do this could leave
 * the pointers in an inconsistent state.
 */

/* Verbatim copy of the static helper at GLContext.c:1638 (commit 868c4801ec); the cache fields are non-const, so the
 * const pointers Java passes are cast once here. The JNI copy is gone; this is the only one. */
static void setVertexAttributePointers(ContextInfo *ctx, const float *pFloat, const uint8_t *pByte) {
    if (pFloat != ctx->vbFloatData) {
        ctx->glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, coordStride, pFloat);
        ctx->glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, coordStride,
            pFloat + FLOATS_PER_VC);
        ctx->glVertexAttribPointer(3, 2, GL_FLOAT, GL_FALSE, coordStride,
            pFloat + FLOATS_PER_VC + FLOATS_PER_TC);
        ctx->vbFloatData = (float *) pFloat;
    }

    if ((const char *) pByte != ctx->vbByteData) {
        ctx->glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, GL_TRUE, colorStride, pByte);
        ctx->vbByteData = (char *) pByte;
    }
}

PRISM_ES2_EXPORT void
es2_draw_indexed_quads(void *ctx, int32_t num_vertices, const float *coords, const uint8_t *colors) {
    int numQuads = num_vertices / 4;

    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glVertexAttribPointer == NULL)) {
        return;
    }

    if (coords && colors) {
        setVertexAttributePointers(ctxInfo, coords, colors);
        glDrawElements(GL_TRIANGLES, numQuads * 2 * 3, GL_UNSIGNED_SHORT, 0);
    }
}

/* ------------------------------------------------------------------------------------------------
 * 3D mesh (GLContext.c nCreateES2Mesh, nReleaseES2Mesh, nBuildNativeGeometryShort/Int,
 * nRenderMeshView with the MeshViewInfo inputs passed as scalars)
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT void *
es2_mesh_create(void *ctx) {
    MeshInfo *meshInfo = NULL;
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (ctxInfo->glGenBuffers == NULL)) {
        return NULL;
    }

    /* allocate the structure */
    meshInfo = (MeshInfo *) malloc(sizeof (MeshInfo));
    if (meshInfo == NULL) {
        fprintf(stderr, "nCreateES2Mesh: Failed in malloc\n");
        return NULL;
    }

    /* initialize the structure */
    meshInfo->vboIDArray[MESH_VERTEXBUFFER] = 0;
    meshInfo->vboIDArray[MESH_INDEXBUFFER] = 0;
    meshInfo->indexBufferSize = 0;
    meshInfo->indexBufferType = 0;

    /* create vbo ids */
    ctxInfo->glGenBuffers(MESH_MAX_BUFFERS, (meshInfo->vboIDArray));

    return meshInfo;
}

PRISM_ES2_EXPORT void
es2_mesh_release(void *ctx, void *mesh) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    MeshInfo *meshInfo = (MeshInfo *) mesh;
    if ((ctxInfo == NULL) || (meshInfo == NULL) ||
            (ctxInfo->glDeleteBuffers == NULL)) {
        return;
    }

    // TODO: 3D - Native clean up. Need to determine do we have to free what
    //            is held by ES2MeshInfo.
    ctxInfo->glDeleteBuffers(MESH_MAX_BUFFERS, (GLuint *) (meshInfo->vboIDArray));
    free(meshInfo);
}

PRISM_ES2_EXPORT int32_t
es2_mesh_build_geometry_short(void *ctx, void *mesh, const float *vb, int32_t vb_len,
        const int16_t *ib, int32_t ib_len) {
    GLuint uvbSize;
    GLuint uibSize;

    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    MeshInfo *meshInfo = (MeshInfo *) mesh;
    if ((ctxInfo == NULL) || (meshInfo == NULL) ||
            (vb == NULL) || (ib == NULL) ||
            (ctxInfo->glBindBuffer == NULL) ||
            (ctxInfo->glBufferData == NULL) ||
            (meshInfo->vboIDArray[MESH_VERTEXBUFFER] == 0)||
            (meshInfo->vboIDArray[MESH_INDEXBUFFER] == 0) ||
            vb_len < 0 || ib_len < 0) {
        return 0;
    }

    uvbSize = (GLuint) vb_len;
    uibSize = (GLuint) ib_len;

    // Initialize vertex buffer
    ctxInfo->glBindBuffer(GL_ARRAY_BUFFER, meshInfo->vboIDArray[MESH_VERTEXBUFFER]);
    ctxInfo->glBufferData(GL_ARRAY_BUFFER, uvbSize * sizeof (GLfloat),
            vb, GL_STATIC_DRAW);

    // Initialize index buffer
    ctxInfo->glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, meshInfo->vboIDArray[MESH_INDEXBUFFER]);
    ctxInfo->glBufferData(GL_ELEMENT_ARRAY_BUFFER, uibSize * sizeof (GLushort),
            ib, GL_STATIC_DRAW);
    meshInfo->indexBufferSize = uibSize;
    meshInfo->indexBufferType = GL_UNSIGNED_SHORT;

    // Unbind VBOs
    ctxInfo->glBindBuffer(GL_ARRAY_BUFFER, 0);
    ctxInfo->glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

    return 1;
}

PRISM_ES2_EXPORT int32_t
es2_mesh_build_geometry_int(void *ctx, void *mesh, const float *vb, int32_t vb_len,
        const int32_t *ib, int32_t ib_len) {
    GLuint uvbSize;
    GLuint uibSize;

    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    MeshInfo *meshInfo = (MeshInfo *) mesh;
    if ((ctxInfo == NULL) || (meshInfo == NULL) ||
            (vb == NULL) || (ib == NULL) ||
            (ctxInfo->glBindBuffer == NULL) ||
            (ctxInfo->glBufferData == NULL) ||
            (meshInfo->vboIDArray[MESH_VERTEXBUFFER] == 0)||
            (meshInfo->vboIDArray[MESH_INDEXBUFFER] == 0) ||
            vb_len < 0 || ib_len < 0) {
        return 0;
    }

    uvbSize = (GLuint) vb_len;
    uibSize = (GLuint) ib_len;

    // Initialize vertex buffer
    ctxInfo->glBindBuffer(GL_ARRAY_BUFFER, meshInfo->vboIDArray[MESH_VERTEXBUFFER]);
    ctxInfo->glBufferData(GL_ARRAY_BUFFER, uvbSize * sizeof (GLfloat),
            vb, GL_STATIC_DRAW);

    // Initialize index buffer
    ctxInfo->glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, meshInfo->vboIDArray[MESH_INDEXBUFFER]);
    ctxInfo->glBufferData(GL_ELEMENT_ARRAY_BUFFER, uibSize * sizeof (GLuint),
            ib, GL_STATIC_DRAW);
    meshInfo->indexBufferSize = uibSize;
    meshInfo->indexBufferType = GL_UNSIGNED_INT;

    // Unbind VBOs
    ctxInfo->glBindBuffer(GL_ARRAY_BUFFER, 0);
    ctxInfo->glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

    return 1;
}

PRISM_ES2_EXPORT void
es2_mesh_render(void *ctx, void *mesh, int32_t cull_enable, int32_t cull_mode_gl, int32_t fill_mode_gl) {
    GLuint offset = 0;
    MeshViewInfo mvInfo;
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    MeshInfo *mInfo = (MeshInfo *) mesh;
    if ((ctxInfo == NULL) || (mInfo == NULL) ||
            (ctxInfo->glBindBuffer == NULL) ||
            (ctxInfo->glBufferData == NULL) ||
            (ctxInfo->glDisableVertexAttribArray == NULL) ||
            (ctxInfo->glEnableVertexAttribArray == NULL) ||
            (ctxInfo->glVertexAttribPointer == NULL)) {
        return;
    }

    /* The three MeshViewInfo fields setCullMode / setPolyonMode read, as nSetCullingMode and
     * nSetWireframe used to store them; the material and light fields were never read. */
    memset(&mvInfo, 0, sizeof(mvInfo));
    mvInfo.meshInfo = mInfo;
    mvInfo.cullEnable = ES2_BOOL(cull_enable);
    mvInfo.cullMode = (GLenum) cull_mode_gl;
    mvInfo.fillMode = (GLenum) fill_mode_gl;

    setCullMode(ctxInfo, &mvInfo);
    setPolyonMode(ctxInfo, &mvInfo);

    // Draw triangles ...
    ctxInfo->glBindBuffer(GL_ARRAY_BUFFER, mInfo->vboIDArray[MESH_VERTEXBUFFER]);
    ctxInfo->glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, mInfo->vboIDArray[MESH_INDEXBUFFER]);

    ctxInfo->glEnableVertexAttribArray(VC_3D_INDEX);
    ctxInfo->glEnableVertexAttribArray(TC_3D_INDEX);
    ctxInfo->glEnableVertexAttribArray(NC_3D_INDEX);

    ctxInfo->glVertexAttribPointer(VC_3D_INDEX, VC_3D_SIZE, GL_FLOAT, GL_FALSE,
            VERT_3D_STRIDE, (const GLvoid *) (uintptr_t) offset);
    offset += VC_3D_SIZE * sizeof(GLfloat);
    ctxInfo->glVertexAttribPointer(TC_3D_INDEX, TC_3D_SIZE, GL_FLOAT, GL_FALSE,
            VERT_3D_STRIDE, (const GLvoid *) (uintptr_t) offset);
    offset += TC_3D_SIZE * sizeof(GLfloat);
    ctxInfo->glVertexAttribPointer(NC_3D_INDEX, NC_3D_SIZE, GL_FLOAT, GL_FALSE,
            VERT_3D_STRIDE, (const GLvoid *) (uintptr_t) offset);

    glDrawElements(GL_TRIANGLES, mInfo->indexBufferSize,
            mInfo->indexBufferType, 0);

    // Reset states
    ctxInfo->glDisableVertexAttribArray(VC_3D_INDEX);
    ctxInfo->glDisableVertexAttribArray(NC_3D_INDEX);
    ctxInfo->glDisableVertexAttribArray(TC_3D_INDEX);
    ctxInfo->glBindBuffer(GL_ARRAY_BUFFER, 0);
    ctxInfo->glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
}
