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
 * prism_es2_api_monocle.c - the Monocle (Linux EGL / OpenGL ES 2) lifecycle half of the flat C ABI
 * in prism_es2_api.h, compiled with -DIS_EGLFB into libprism_es2_monocle.so next to the generic
 * sources (linux.cmake, target prismES2Monocle).
 *
 * On Monocle the pixel format, drawable and context are Java's: com.sun.glass.ui.monocle.
 * AcceleratedScreen creates the EGL display, surface and context and makes them current
 * (MonocleGLContext.makeCurrent -> AcceleratedScreen.enableRendering, MonocleGLDrawable.swapBuffers
 * -> AcceleratedScreen.swapBuffers), and MonocleGLPixelFormat / MonocleGLDrawable never create a
 * native PixelFormatInfo / DrawableInfo. The one way into this library is es2_context_adopt
 * (prism_es2_api.c), which replaced MonocleGLFactory.nPopulateNativeCtxInfo of MonocleGLFactory.c
 * at commit 21d5a654f6. The functions below exist so that the symbol set equals that of the desktop
 * libraries - ES2Native binds every es2_* symbol eagerly and records a missing one as a link
 * failure - and each answers as prism_es2_api.h documents for a platform that cannot implement it:
 * NULL, 0, ES2_ERR_NOT_IMPLEMENTED or a no-op. None of them is reached by the Monocle Java code.
 */

#include "../prism_es2_api.h"

#include <stdlib.h>

#include "../PrismES2Defs.h"

/* ------------------------------------------------------------------------------------------------
 * Factory
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT void *
es2_factory_init(const Es2PixelFormatAttrs *attrs) {
    (void) attrs;
    return NULL;
}

PRISM_ES2_EXPORT int32_t
es2_factory_get_x11_info(void *ctx, int64_t info[3]) {
    if ((ctx == NULL) || (info == NULL)) {
        return ES2_ERR_INVALID_ARG;
    }
    return ES2_ERR_NOT_IMPLEMENTED;
}

/* ------------------------------------------------------------------------------------------------
 * Pixel format
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT void *
es2_pixel_format_create(int64_t native_screen, const Es2PixelFormatAttrs *attrs) {
    (void) native_screen;
    (void) attrs;
    return NULL;
}

/* ------------------------------------------------------------------------------------------------
 * Drawable
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT void *
es2_drawable_create(void *pf, int64_t native_window) {
    (void) pf;
    (void) native_window;
    return NULL;
}

PRISM_ES2_EXPORT void *
es2_drawable_create_dummy(void *pf) {
    (void) pf;
    return NULL;
}

PRISM_ES2_EXPORT void
es2_drawable_release(void *drawable) {
    DrawableInfo *dInfo = (DrawableInfo *) drawable;
    if (dInfo == NULL) {
        return;
    }
    free(dInfo);
}

PRISM_ES2_EXPORT int32_t
es2_drawable_swap_buffers(void *ctx, void *drawable) {
    (void) ctx;
    (void) drawable;
    return 0;
}

/* ------------------------------------------------------------------------------------------------
 * Context
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT void *
es2_context_create(void *drawable, void *pf, int64_t share_ctx_handle, int32_t vsync_requested) {
    (void) drawable;
    (void) pf;
    (void) share_ctx_handle;
    (void) vsync_requested;
    return NULL;
}

PRISM_ES2_EXPORT int64_t
es2_context_get_native_handle(void *ctx) {
    /* The ContextInfo es2_context_adopt builds never carries a native context (Java owns it), and
     * MonocleGLContext.getNativeHandle answers 0 on its own; 0 for a NULL ctx as on every platform. */
    (void) ctx;
    return 0;
}

PRISM_ES2_EXPORT void
es2_context_make_current(void *ctx, void *drawable) {
    (void) ctx;
    (void) drawable;
}
