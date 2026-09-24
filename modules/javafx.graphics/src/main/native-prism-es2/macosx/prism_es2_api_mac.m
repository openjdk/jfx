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
 * prism_es2_api_mac.m - the macOS (NSOpenGL) lifecycle half of the flat C ABI in prism_es2_api.h.
 *
 * The bodies are Java_com_sun_prism_es2_MacGLFactory_nInitialize, MacGLPixelFormat_nCreatePixelFormat,
 * MacGLDrawable_nCreateDrawable / nGetDummyDrawable / nSwapBuffers / nReleaseDrawable and
 * MacGLContext_nInitialize / nGetNativeHandle / nMakeCurrent with the JNI marshalling removed. Like
 * those files this one is plain C: the NSOpenGL calls go through the C helpers of
 * MacOSXWindowSystemInterface.m declared in macosx-window-system.h (createPixelFormat,
 * createContext, makeCurrentContext, deleteContext, deletePixelFormat, flushBuffer,
 * setSwapInterval); createPixelFormat takes the Es2PixelFormatAttrs struct directly. The
 * release-on-failure helper es2MacReleaseResources below is the former MacGLFactory.c
 * printAndReleaseResources with void* handles.
 *
 * Not compiled on the Windows machine that wrote it; macOS CI is the check.
 */

#include "../prism_es2_api.h"

#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "../PrismES2Defs.h"

#define ES2_BOOL(x) ((GLboolean) ((x) != 0))

/* Former MacGLFactory.c printAndReleaseResources, with void* handles. */
static void es2MacReleaseResources(void *pf, void *ctx, const char *message) {
    if (message != NULL) {
        fprintf(stderr, "%s\n", message);
    }
    makeCurrentContext(NULL);
    if (pf != NULL) {
        deletePixelFormat(pf);
    }
    if (ctx != NULL) {
        deleteContext(ctx);
    }
}

/* ------------------------------------------------------------------------------------------------
 * Factory
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT void *
es2_factory_init(const Es2PixelFormatAttrs *attrs) {

    void *pixelFormat;
    void *context = NULL;
    int viewNotReady;

    ContextInfo *ctxInfo = NULL;

    const char *glVersion;
    const char *glVendor;
    const char *glRenderer;
    char *tmpVersionStr;
    int versionNumbers[2];
    const char *glExtensions;

    if (attrs == NULL) {
        return NULL;
    }

    pixelFormat = createPixelFormat(attrs);

    if (pixelFormat == NULL) {
        // System is incapable of es2 support
        es2MacReleaseResources(NULL, NULL, NULL);
        return NULL;
    }

    context = createContext(NULL, NULL, pixelFormat, &viewNotReady);

    if (context == NULL) {
        es2MacReleaseResources(pixelFormat, NULL,
                "Fail in createContext");
        return NULL;
    }

    if (!makeCurrentContext(context)) {
        es2MacReleaseResources(pixelFormat, context,
                "Fail in CGLSetCurrentContext");
        return NULL;
    }

    /* Get the OpenGL version */
    glVersion = (char *) glGetString(GL_VERSION);
    if (glVersion == NULL) {
        es2MacReleaseResources(pixelFormat, context,
                "glVersion == null");
        return NULL;
    }

    /* find out the version, major and minor version number */
    tmpVersionStr = strdup(glVersion);
    extractVersionInfo(tmpVersionStr, versionNumbers);
    free(tmpVersionStr);

    /*
     * Targeted Cards: Intel HD Graphics, Intel HD Graphics 2000/3000,
     * Radeon HD 2350, GeForce FX (with newer drivers), GeForce 7 series or higher
     *
     * Check for OpenGL 2.1 or later.
     */
    if ((versionNumbers[0] < 2) || ((versionNumbers[0] == 2) && (versionNumbers[1] < 1))) {
        fprintf(stderr,
                "Prism-ES2 Error : GL_VERSION (major.minor) = %d.%d\n",
                versionNumbers[0], versionNumbers[1]);
        es2MacReleaseResources(pixelFormat, context, NULL);
        return NULL;
    }

    /* Get the OpenGL vendor and renderer */
    glVendor = (char *) glGetString(GL_VENDOR);
    if (glVendor == NULL) {
        glVendor = "<UNKNOWN>";
    }
    glRenderer = (char *) glGetString(GL_RENDERER);
    if (glRenderer == NULL) {
        glRenderer = "<UNKNOWN>";
    }

    glExtensions = (char *) glGetString(GL_EXTENSIONS);
    if (glExtensions == NULL) {
        es2MacReleaseResources(pixelFormat, context,
                "Prism-ES2 Error : glExtensions == null");
        return NULL;
    }

    // We use GL_ARB_pixel_buffer_object as an guide to
    // determine PS 3.0 capable.
    if (!isExtensionSupported(glExtensions, "GL_ARB_pixel_buffer_object")) {
        es2MacReleaseResources(pixelFormat, context,
                "GL profile isn't PS 3.0 capable");
        return NULL;
    }

    /* allocate the structure */
    ctxInfo = (ContextInfo *) malloc(sizeof (ContextInfo));
    if (ctxInfo == NULL) {
        fprintf(stderr, "nInitialize: Failed in malloc\n");
        return NULL;
    }
    /* initialize the structure */
    initializeCtxInfo(ctxInfo);

    ctxInfo->versionStr = strdup(glVersion);
    ctxInfo->vendorStr = strdup(glVendor);
    ctxInfo->rendererStr = strdup(glRenderer);
    ctxInfo->glExtensionStr = strdup(glExtensions);
    ctxInfo->versionNumbers[0] = versionNumbers[0];
    ctxInfo->versionNumbers[1] = versionNumbers[1];
    ctxInfo->gl2 = GL_TRUE;

    // Save the context.
    ctxInfo->context = (intptr_t) context;

    /*
     *  Do not free context as we need it for Mac to use as a shareContext for
     * GLass
     */

    return ctxInfo;
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
    void *pixelFormat = NULL;
    PixelFormatInfo *pfInfo = NULL;

    (void) native_screen; /* unused, as before */
    if (attrs == NULL) {
        return NULL;
    }

    pixelFormat = createPixelFormat(attrs);

    /* allocate the structure */
    pfInfo = (PixelFormatInfo *) malloc(sizeof (PixelFormatInfo));
    if (pfInfo == NULL) {
        fprintf(stderr, "nCreatePixelFormat: Failed in malloc\n");
    }

    /* Carried verbatim from MacGLPixelFormat.c:55-62 at commit 868c4801ec: a failed malloc prints and
     * falls through to the dereference below. To be fixed in a later commit, not silently here. */
    /* initialize the structure */
    initializePixelFormatInfo(pfInfo);
    pfInfo->pixelFormat = (intptr_t) pixelFormat;

    return pfInfo;
}

/* ------------------------------------------------------------------------------------------------
 * Drawable
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT void *
es2_drawable_create(void *pf, int64_t native_window) {
    DrawableInfo *dInfo = NULL;

    (void) pf; /* MacGLDrawable_nCreateDrawable never read its pixel format */

    /* allocate the structure */
    dInfo = (DrawableInfo *) malloc(sizeof (DrawableInfo));
    if (dInfo == NULL) {
        fprintf(stderr, "nCreateDrawable: Failed in malloc\n");
        return NULL;
    }

    /* initialize the structure */
    memset(dInfo, 0, sizeof(DrawableInfo));

    dInfo->win = native_window;
    dInfo->onScreen = GL_TRUE;

    return dInfo;
}

PRISM_ES2_EXPORT void *
es2_drawable_create_dummy(void *pf) {
    DrawableInfo *dInfo = NULL;

    (void) pf; /* MacGLDrawable_nGetDummyDrawable never read its pixel format */

    /*
     * No need to create a dummy window on Mac
     * It only uses RTT for rendering and hand over to the CALayer.
     */

    /* allocate the structure */
    dInfo = (DrawableInfo *) malloc(sizeof (DrawableInfo));
    if (dInfo == NULL) {
        fprintf(stderr, "nGetDummyDrawable: Failed in malloc\n");
        return NULL;
    }

    /* initialize the structure */
    memset(dInfo, 0, sizeof(DrawableInfo));

    dInfo->win = 0;
    dInfo->onScreen = GL_FALSE;

    return dInfo;
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
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    (void) drawable; /* MacGLDrawable_nSwapBuffers flushes the context, not the drawable */
    if (ctxInfo == NULL) {
        return 0;
    }

    flushBuffer((void *) (intptr_t) ctxInfo->context);
    return 1;
}

/* ------------------------------------------------------------------------------------------------
 * Context
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT void *
es2_context_create(void *drawable, void *pf, int64_t share_ctx_handle, int32_t vsync_requested) {
    const char *glVersion;
    const char *glVendor;
    const char *glRenderer;
    char *tmpVersionStr;
    int versionNumbers[2];
    const char *glExtensions;

    void *context = NULL;
    int viewNotReady;
    ContextInfo *ctxInfo = NULL;
    DrawableInfo *dInfo = (DrawableInfo *) drawable;
    PixelFormatInfo *pfInfo = (PixelFormatInfo *) pf;

    if ((dInfo == NULL) || (pfInfo == NULL)) {
        return NULL;
    }

    context = createContext((void *) (intptr_t) share_ctx_handle,
            (void *) (intptr_t) dInfo->win,
            (void *) (intptr_t) pfInfo->pixelFormat, &viewNotReady);

    if (context == NULL) {
        fprintf(stderr, "Fail in createContext");
        return NULL;
    }

    if (!makeCurrentContext(context)) {
        es2MacReleaseResources(NULL, context,
                "Fail in makeCurrentContext");
        return NULL;
    }

    /* Get the OpenGL version */
    glVersion = (char *) glGetString(GL_VERSION);
    if (glVersion == NULL) {
        es2MacReleaseResources(NULL, context, "glVersion == null");
        return NULL;
    }

    /* find out the version, major and minor version number */
    tmpVersionStr = strdup(glVersion);
    extractVersionInfo(tmpVersionStr, versionNumbers);
    free(tmpVersionStr);

    /*
     * Supported Cards: Intel HD Graphics, Intel HD Graphics 2000/3000,
     * Radeon HD 2350, GeForce FX (with newer drivers), GeForce 6 series or higher
     *
     * Check for OpenGL 2.0 or later.
     */
    if (versionNumbers[0] < 2) {
        es2MacReleaseResources(NULL, context, NULL);
        fprintf(stderr, "Prism-ES2 Error : GL_VERSION (major.minor) = %d.%d\n",
                versionNumbers[0], versionNumbers[1]);
        return NULL;
    }

    /* Get the OpenGL vendor and renderer */
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
        es2MacReleaseResources(NULL, context, "glExtensions == null");
        return NULL;
    }

    // We use GL 2.0 and GL_ARB_pixel_buffer_object as an guide to
    // determine PS 3.0 capable.
    if (!isExtensionSupported(glExtensions, "GL_ARB_pixel_buffer_object")) {
        es2MacReleaseResources(NULL, context, "GL profile isn't PS 3.0 capable");
        return NULL;
    }

    /* allocate the structure */
    ctxInfo = (ContextInfo *) malloc(sizeof (ContextInfo));
    if (ctxInfo == NULL) {
        fprintf(stderr, "nInitialize: Failed in malloc\n");
        return NULL;
    }

    /* initialize the structure */
    initializeCtxInfo(ctxInfo);
    ctxInfo->versionStr = strdup(glVersion);
    ctxInfo->vendorStr = strdup(glVendor);
    ctxInfo->rendererStr = strdup(glRenderer);
    ctxInfo->glExtensionStr = strdup(glExtensions);
    ctxInfo->versionNumbers[0] = versionNumbers[0];
    ctxInfo->versionNumbers[1] = versionNumbers[1];
    ctxInfo->context = (intptr_t) context;

    /* set function pointers */
    ctxInfo->glActiveTexture = (PFNGLACTIVETEXTUREPROC)
            dlsym(RTLD_DEFAULT, "glActiveTexture");
    ctxInfo->glAttachShader = (PFNGLATTACHSHADERPROC)
            dlsym(RTLD_DEFAULT, "glAttachShader");
    ctxInfo->glBindAttribLocation = (PFNGLBINDATTRIBLOCATIONPROC)
            dlsym(RTLD_DEFAULT, "glBindAttribLocation");
    ctxInfo->glBindFramebuffer = (PFNGLBINDFRAMEBUFFERPROC)
            dlsym(RTLD_DEFAULT, "glBindFramebuffer");
    ctxInfo->glBindRenderbuffer = (PFNGLBINDRENDERBUFFERPROC)
            dlsym(RTLD_DEFAULT, "glBindRenderbuffer");
    ctxInfo->glCheckFramebufferStatus = (PFNGLCHECKFRAMEBUFFERSTATUSPROC)
            dlsym(RTLD_DEFAULT, "glCheckFramebufferStatus");
    ctxInfo->glCreateProgram = (PFNGLCREATEPROGRAMPROC)
            dlsym(RTLD_DEFAULT, "glCreateProgram");
    ctxInfo->glCreateShader = (PFNGLCREATESHADERPROC)
            dlsym(RTLD_DEFAULT, "glCreateShader");
    ctxInfo->glCompileShader = (PFNGLCOMPILESHADERPROC)
            dlsym(RTLD_DEFAULT, "glCompileShader");
    ctxInfo->glDeleteBuffers = (PFNGLDELETEBUFFERSPROC)
            dlsym(RTLD_DEFAULT, "glDeleteBuffers");
    ctxInfo->glDeleteFramebuffers = (PFNGLDELETEFRAMEBUFFERSPROC)
            dlsym(RTLD_DEFAULT, "glDeleteFramebuffers");
    ctxInfo->glDeleteProgram = (PFNGLDELETEPROGRAMPROC)
            dlsym(RTLD_DEFAULT, "glDeleteProgram");
    ctxInfo->glDeleteRenderbuffers = (PFNGLDELETERENDERBUFFERSPROC)
            dlsym(RTLD_DEFAULT, "glDeleteRenderbuffers");
    ctxInfo->glDeleteShader = (PFNGLDELETESHADERPROC)
            dlsym(RTLD_DEFAULT, "glDeleteShader");
    ctxInfo->glDetachShader = (PFNGLDETACHSHADERPROC)
            dlsym(RTLD_DEFAULT, "glDetachShader");
    ctxInfo->glDisableVertexAttribArray = (PFNGLDISABLEVERTEXATTRIBARRAYPROC)
            dlsym(RTLD_DEFAULT, "glDisableVertexAttribArray");
    ctxInfo->glEnableVertexAttribArray = (PFNGLENABLEVERTEXATTRIBARRAYPROC)
            dlsym(RTLD_DEFAULT, "glEnableVertexAttribArray");
    ctxInfo->glFramebufferRenderbuffer = (PFNGLFRAMEBUFFERRENDERBUFFERPROC)
            dlsym(RTLD_DEFAULT, "glFramebufferRenderbuffer");
    ctxInfo->glFramebufferTexture2D = (PFNGLFRAMEBUFFERTEXTURE2DPROC)
            dlsym(RTLD_DEFAULT, "glFramebufferTexture2D");
    ctxInfo->glGenFramebuffers = (PFNGLGENFRAMEBUFFERSPROC)
            dlsym(RTLD_DEFAULT, "glGenFramebuffers");
    ctxInfo->glGenRenderbuffers = (PFNGLGENRENDERBUFFERSPROC)
            dlsym(RTLD_DEFAULT, "glGenRenderbuffers");
    ctxInfo->glGetProgramiv = (PFNGLGETPROGRAMIVPROC)
            dlsym(RTLD_DEFAULT, "glGetProgramiv");
    ctxInfo->glGetShaderiv = (PFNGLGETSHADERIVPROC)
            dlsym(RTLD_DEFAULT, "glGetShaderiv");
    ctxInfo->glGetUniformLocation = (PFNGLGETUNIFORMLOCATIONPROC)
            dlsym(RTLD_DEFAULT, "glGetUniformLocation");
    ctxInfo->glLinkProgram = (PFNGLLINKPROGRAMPROC)
            dlsym(RTLD_DEFAULT, "glLinkProgram");
    ctxInfo->glRenderbufferStorage = (PFNGLRENDERBUFFERSTORAGEPROC)
            dlsym(RTLD_DEFAULT, "glRenderbufferStorage");
    ctxInfo->glShaderSource = (PFNGLSHADERSOURCEPROC)
            dlsym(RTLD_DEFAULT, "glShaderSource");
    ctxInfo->glUniform1f = (PFNGLUNIFORM1FPROC)
            dlsym(RTLD_DEFAULT, "glUniform1f");
    ctxInfo->glUniform2f = (PFNGLUNIFORM2FPROC)
            dlsym(RTLD_DEFAULT, "glUniform2f");
    ctxInfo->glUniform3f = (PFNGLUNIFORM3FPROC)
            dlsym(RTLD_DEFAULT, "glUniform3f");
    ctxInfo->glUniform4f = (PFNGLUNIFORM4FPROC)
            dlsym(RTLD_DEFAULT, "glUniform4f");
    ctxInfo->glUniform4fv = (PFNGLUNIFORM4FVPROC)
            dlsym(RTLD_DEFAULT, "glUniform4fv");
    ctxInfo->glUniform1i = (PFNGLUNIFORM1IPROC)
            dlsym(RTLD_DEFAULT, "glUniform1i");
    ctxInfo->glUniform2i = (PFNGLUNIFORM2IPROC)
            dlsym(RTLD_DEFAULT, "glUniform2i");
    ctxInfo->glUniform3i = (PFNGLUNIFORM3IPROC)
            dlsym(RTLD_DEFAULT, "glUniform3i");
    ctxInfo->glUniform4i = (PFNGLUNIFORM4IPROC)
            dlsym(RTLD_DEFAULT, "glUniform4i");
    ctxInfo->glUniform4iv = (PFNGLUNIFORM4IVPROC)
            dlsym(RTLD_DEFAULT, "glUniform4iv");
    ctxInfo->glUniformMatrix4fv = (PFNGLUNIFORMMATRIX4FVPROC)
            dlsym(RTLD_DEFAULT, "glUniformMatrix4fv");
    ctxInfo->glUseProgram = (PFNGLUSEPROGRAMPROC)
            dlsym(RTLD_DEFAULT, "glUseProgram");
    ctxInfo->glValidateProgram = (PFNGLVALIDATEPROGRAMPROC)
            dlsym(RTLD_DEFAULT, "glValidateProgram");
    ctxInfo->glVertexAttribPointer = (PFNGLVERTEXATTRIBPOINTERPROC)
            dlsym(RTLD_DEFAULT, "glVertexAttribPointer");
    ctxInfo->glGenBuffers = (PFNGLGENBUFFERSPROC)
            dlsym(RTLD_DEFAULT, "glGenBuffers");
    ctxInfo->glBindBuffer = (PFNGLBINDBUFFERPROC)
            dlsym(RTLD_DEFAULT, "glBindBuffer");
    ctxInfo->glBufferData = (PFNGLBUFFERDATAPROC)
            dlsym(RTLD_DEFAULT, "glBufferData");
    ctxInfo->glBufferSubData = (PFNGLBUFFERSUBDATAPROC)
            dlsym(RTLD_DEFAULT, "glBufferSubData");
    ctxInfo->glGetShaderInfoLog = (PFNGLGETSHADERINFOLOGPROC)
            dlsym(RTLD_DEFAULT, "glGetShaderInfoLog");
    ctxInfo->glGetProgramInfoLog = (PFNGLGETPROGRAMINFOLOGPROC)
            dlsym(RTLD_DEFAULT, "glGetProgramInfoLog");
    ctxInfo->glTexImage2DMultisample = (PFNGLTEXIMAGE2DMULTISAMPLEPROC)
            dlsym(RTLD_DEFAULT, "glTexImage2DMultisample");
    ctxInfo->glRenderbufferStorageMultisample = (PFNGLRENDERBUFFERSTORAGEMULTISAMPLEPROC)
            dlsym(RTLD_DEFAULT, "glRenderbufferStorageMultisample");
    ctxInfo->glBlitFramebuffer = (PFNGLBLITFRAMEBUFFERPROC)
            dlsym(RTLD_DEFAULT, "glBlitFramebuffer");

    // initialize platform states and properties to match
    // cached states and properties
    setSwapInterval(context, 0);
    ctxInfo->state.vSyncEnabled = GL_FALSE;
    ctxInfo->vSyncRequested = ES2_BOOL(vsync_requested);

    initState(ctxInfo);

    // Release context once we are all done
    makeCurrentContext(NULL);

    return ctxInfo;
}

PRISM_ES2_EXPORT int64_t
es2_context_get_native_handle(void *ctx) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if (ctxInfo == NULL) {
        return 0;
    }
    return (int64_t) ctxInfo->context;
}

PRISM_ES2_EXPORT void
es2_context_make_current(void *ctx, void *drawable) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    DrawableInfo *dInfo = (DrawableInfo *) drawable;
    int interval;
    GLboolean vSyncNeeded;

    if ((ctxInfo == NULL) || (dInfo == NULL)) {
        return;
    }

    if (!makeCurrentContext((void *) (intptr_t) ctxInfo->context)) {
        fprintf(stderr, "Failed in makeCurrentContext\n");
    }
    vSyncNeeded = ES2_BOOL(ctxInfo->vSyncRequested && dInfo->onScreen);
    if (vSyncNeeded == ctxInfo->state.vSyncEnabled) {
        return;
    }
    interval = (vSyncNeeded) ? 1 : 0;
    ctxInfo->state.vSyncEnabled = vSyncNeeded;
    setSwapInterval((void *) (intptr_t) ctxInfo->context, interval);
}
