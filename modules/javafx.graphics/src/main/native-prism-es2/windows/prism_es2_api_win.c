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
 * prism_es2_api_win.c - the Windows (WGL) lifecycle half of the flat C ABI in prism_es2_api.h.
 *
 * The bodies are Java_com_sun_prism_es2_WinGLFactory_nInitialize, WinGLPixelFormat_nCreatePixelFormat,
 * WinGLDrawable_nCreateDrawable / nGetDummyDrawable / nSwapBuffers / nReleaseDrawable and
 * WinGLContext_nInitialize / nGetNativeHandle / nMakeCurrent with the JNI marshalling removed. The
 * helpers (getPFD, createDummyWindow, printAndReleaseResources) stay in WinGLFactory.c and are
 * reached through the same extern declarations WinGLContext.c uses; getPFD takes the
 * Es2PixelFormatAttrs struct directly.
 */

#include "../prism_es2_api.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "../PrismES2Defs.h"

extern void printAndReleaseResources(HWND hwnd, HGLRC hglrc,
        HDC hdc, LPCTSTR szAppName, char *message);
extern HWND createDummyWindow(LPCTSTR szAppName);
extern PIXELFORMATDESCRIPTOR getPFD(const Es2PixelFormatAttrs *attrs);

#define ES2_BOOL(x) ((GLboolean) ((x) != 0))

/* ------------------------------------------------------------------------------------------------
 * Factory
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT void *
es2_factory_init(const Es2PixelFormatAttrs *attrs) {
    static LPCTSTR szAppName = L"Choose Pixel Format";
    HWND hwnd = NULL;
    HGLRC hglrc = NULL;
    HDC hdc = NULL;
    int pixelFormat;
    PIXELFORMATDESCRIPTOR pfd;

    ContextInfo *ctxInfo = NULL;
    const char *glVersion;
    const char *glVendor;
    const char *glRenderer;
    char *tmpVersionStr;
    int versionNumbers[2];
    const char *glExtensions;
    const char *wglExtensions;
    PFNWGLGETEXTENSIONSSTRINGARBPROC wglGetExtensionsStringARB = NULL;

    if (attrs == NULL) {
        return NULL;
    }
    pfd = getPFD(attrs);

    /*
     * Select a specified pixel format and bound current context to
     * it so that we can get the wglChoosePixelFormatARB entry point.
     * Otherwise wglxxx entry point will always return null.
     * That's why we need to create a dummy window also.
     */
    hwnd = createDummyWindow(szAppName);

    if (!hwnd) {
        return NULL;
    }

    hdc = GetDC(hwnd);
    if (hdc == NULL) {
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "Failed in GetDC");
        return NULL;
    }

    pixelFormat = ChoosePixelFormat(hdc, &pfd);
    if (pixelFormat < 1) {
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "Failed in ChoosePixelFormat");
        return NULL;
    }

    if (!SetPixelFormat(hdc, pixelFormat, NULL)) {
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "Failed in SetPixelFormat");
        return NULL;
    }

    hglrc = wglCreateContext(hdc);
    if (hglrc == NULL) {
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "Failed in wglCreateContext");
        return NULL;
    }

    if (!wglMakeCurrent(hdc, hglrc)) {
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "Failed in wglMakeCurrent");
        return NULL;
    }

    /* Get the OpenGL version */
    glVersion = (const char *) glGetString(GL_VERSION);
    if (glVersion == NULL) {
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "glVersion == null");
        return NULL;
    }

    /* find out the version, major and minor version number */
    tmpVersionStr = _strdup(glVersion);
    extractVersionInfo(tmpVersionStr, versionNumbers);
    free(tmpVersionStr);

    /*
     * Targeted Cards: Intel HD Graphics, Intel HD Graphics 2000/3000,
     * Radeon HD 2350, GeForce FX (with newer drivers), GeForce 7 series or higher
     *
     * Check for OpenGL 2.1 or later.
     */
    if ((versionNumbers[0] < 2) || ((versionNumbers[0] == 2) && (versionNumbers[1] < 1))) {
        fprintf(stderr, "GL_VERSION (major.minor) = %d.%d",
                versionNumbers[0], versionNumbers[1]);
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName, NULL);
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
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "glExtensions == null");
        return NULL;
    }

    // We use GL_ARB_pixel_buffer_object as an guide to
    // determine PS 3.0 capable.
    if (!isExtensionSupported(glExtensions, "GL_ARB_pixel_buffer_object")) {
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "GL profile isn't PS 3.0 capable");
        return NULL;
    }

    wglGetExtensionsStringARB = (PFNWGLGETEXTENSIONSSTRINGARBPROC)
            wglGetProcAddress("wglGetExtensionsStringARB");
    if (wglGetExtensionsStringARB == NULL) {
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "wglGetExtensionsStringARB is not supported!");
        return NULL;
    }
    wglExtensions = (char *) wglGetExtensionsStringARB(hdc);
    if (wglExtensions == NULL) {
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "wglExtensions == null");
        return NULL;
    }

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
    ctxInfo->versionStr = _strdup(glVersion);
    ctxInfo->vendorStr = _strdup(glVendor);
    ctxInfo->rendererStr = _strdup(glRenderer);
    ctxInfo->glExtensionStr = _strdup(glExtensions);
    ctxInfo->wglExtensionStr = _strdup(wglExtensions);
    ctxInfo->versionNumbers[0] = versionNumbers[0];
    ctxInfo->versionNumbers[1] = versionNumbers[1];
    ctxInfo->gl2 = GL_TRUE;

    printAndReleaseResources(hwnd, hglrc, hdc, szAppName, NULL);
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
    static LPCTSTR szAppName = L"Choose Pixel Format";
    HWND hwnd = NULL;
    HDC hdc = NULL;
    int pixelFormat;
    PIXELFORMATDESCRIPTOR pfd;
    PixelFormatInfo *pfInfo = NULL;

    (void) native_screen; /* JDK-8090498: unused, single-monitor assumption, as before */
    if (attrs == NULL) {
        return NULL;
    }
    pfd = getPFD(attrs);

    // JDK-8090498
    // TODO: Need to use nativeScreen to create this requested pixelformat
    // currently hack to work on a single monitor system
    hwnd = createDummyWindow(szAppName);

    if (!hwnd) {
        return NULL;
    }
    hdc = GetDC(hwnd);
    if (hdc == NULL) {
        printAndReleaseResources(hwnd, NULL, hdc, szAppName,
                "Failed in GetDC");
        return NULL;
    }

    pixelFormat = ChoosePixelFormat(hdc, &pfd);
    if (pixelFormat < 1) {
        printAndReleaseResources(hwnd, NULL, hdc, szAppName,
                "Failed in ChoosePixelFormat");
        return NULL;
    }

    /* allocate the structure */
    pfInfo = (PixelFormatInfo *) malloc(sizeof (PixelFormatInfo));
    if (pfInfo == NULL) {
        fprintf(stderr, "nCreatePixelFormat: Failed in malloc\n");
        return NULL;
    }

    /* initialize the structure */
    initializePixelFormatInfo(pfInfo);
    pfInfo->pixelFormat = pixelFormat;
    pfInfo->dummyHwnd = hwnd;
    pfInfo->dummyHdc = hdc;
    pfInfo->dummySzAppName = szAppName;

    return pfInfo;
}

/* ------------------------------------------------------------------------------------------------
 * Drawable
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT void *
es2_drawable_create(void *pf, int64_t native_window) {
    HDC hdc;
    DrawableInfo *dInfo = NULL;
    HWND hwnd = (HWND) (intptr_t) native_window;
    PixelFormatInfo *pfInfo = (PixelFormatInfo *) pf;
    if (pfInfo == NULL) {
        return NULL;
    }

    if (!hwnd) {
        fprintf(stderr, "nCreateHdc: Invalid hwnd");
        return NULL;
    }
    // TODO: Need to get the screen info in pfInfo to handle multi-monitor case. (JDK-8092267)
    hdc = GetDC(hwnd);

    if (!SetPixelFormat(hdc, pfInfo->pixelFormat, NULL)) {
        printAndReleaseResources(NULL, NULL, hdc, NULL,
                "nCreateHdc: Failed in SetPixelFormat");
        return NULL;
    }

    /* allocate the structure */
    dInfo = (DrawableInfo *) malloc(sizeof (DrawableInfo));
    if (dInfo == NULL) {
        fprintf(stderr, "nCreateDrawable: Failed in malloc\n");
        return NULL;
    }

    /* initialize the structure */
    memset(dInfo, 0, sizeof(DrawableInfo));

    dInfo->hdc = hdc;
    dInfo->hwnd = hwnd;
    dInfo->onScreen = GL_TRUE;

    return dInfo;
}

PRISM_ES2_EXPORT void *
es2_drawable_create_dummy(void *pf) {
    DrawableInfo *dInfo = NULL;
    PixelFormatInfo *pfInfo = (PixelFormatInfo *) pf;
    if (pfInfo == NULL) {
        return NULL;
    }

    /* allocate the structure */
    dInfo = (DrawableInfo *) malloc(sizeof (DrawableInfo));
    if (dInfo == NULL) {
        fprintf(stderr, "nGetDummyDrawable: Failed in malloc\n");
        return NULL;
    }

    /* initialize the structure */
    memset(dInfo, 0, sizeof(DrawableInfo));

    // Use the dummyHdc that was already created in the pfInfo
    // since this is an non-onscreen drawable.
    dInfo->hdc = pfInfo->dummyHdc;
    dInfo->hwnd = pfInfo->dummyHwnd;
    dInfo->onScreen = GL_FALSE;

    return dInfo;
}

PRISM_ES2_EXPORT void
es2_drawable_release(void *drawable) {
    DrawableInfo *dInfo = (DrawableInfo *) drawable;
    if (dInfo == NULL) {
        return;
    }

    if ((dInfo->hdc != NULL) && (dInfo->hwnd != NULL)) {
        ReleaseDC(dInfo->hwnd, dInfo->hdc);
    }

    free(dInfo);
}

PRISM_ES2_EXPORT int32_t
es2_drawable_swap_buffers(void *ctx, void *drawable) {
    DrawableInfo *dInfo = (DrawableInfo *) drawable;
    (void) ctx; /* used on macOS only */
    if (dInfo == NULL) {
        return 0;
    }
    return SwapBuffers(dInfo->hdc) ? 1 : 0;
}

/* ------------------------------------------------------------------------------------------------
 * Context
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT void *
es2_context_create(void *drawable, void *pf, int64_t share_ctx_handle, int32_t vsync_requested) {
    HDC hdc = NULL;
    HGLRC hglrc = NULL;
    ContextInfo *ctxInfo = NULL;
    int pixelFormat;

    const char *glVersion;
    const char *glVendor;
    const char *glRenderer;
    char *tmpVersionStr;
    int versionNumbers[2];
    const char *glExtensions;
    const char *wglExtensions;
    PFNWGLGETEXTENSIONSSTRINGARBPROC wglGetExtensionsStringARB = NULL;

    DrawableInfo *dInfo = (DrawableInfo *) drawable;
    PixelFormatInfo *pfInfo = (PixelFormatInfo *) pf;

    (void) share_ctx_handle; /* WGL contexts are not shared, as before (WinGLFactory.java) */
    if ((dInfo == NULL) || (pfInfo == NULL)) {
        return NULL;
    }

    hdc = dInfo->hdc;
    pixelFormat = pfInfo->pixelFormat;

    if (!SetPixelFormat(hdc, pixelFormat, NULL)) {
        fprintf(stderr, "Failed in SetPixelFormat");
        return NULL;
    }

    hglrc = wglCreateContext(hdc);
    if (hglrc == NULL) {
        printAndReleaseResources(NULL, hglrc, NULL, NULL,
                "Failed in wglCreateContext");
        return NULL;
    }

    if (!wglMakeCurrent(hdc, hglrc)) {
        printAndReleaseResources(NULL, hglrc, NULL, NULL,
                "Failed in wglMakeCurrent");
        return NULL;
    }

    /* Get the OpenGL version */
    glVersion = (const char *) glGetString(GL_VERSION);
    if (glVersion == NULL) {
        printAndReleaseResources(NULL, hglrc, NULL, NULL,
                "glVersion == null");
        return NULL;
    }

    /* find out the version, major and minor version number */
    tmpVersionStr = _strdup(glVersion);
    extractVersionInfo(tmpVersionStr, versionNumbers);
    free(tmpVersionStr);

    /*
     * Supported Cards: Intel HD Graphics, Intel HD Graphics 2000/3000,
     * Radeon HD 2350, GeForce FX (with newer drivers), GeForce 6 series or higher
     *
     * Check for OpenGL 2.0 or later.
     */
    if (versionNumbers[0] < 2) {
        fprintf(stderr, "GL_VERSION (major.minor) = %d.%d",
                versionNumbers[0], versionNumbers[1]);
        printAndReleaseResources(NULL, hglrc, NULL, NULL, NULL);
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
        printAndReleaseResources(NULL, hglrc, NULL, NULL,
                "glExtensions == null");
        return NULL;
    }

    // We use GL 2.0 and GL_ARB_pixel_buffer_object as an guide to
    // determine PS 3.0 capable.
    if (!isExtensionSupported(glExtensions, "GL_ARB_pixel_buffer_object")) {
        printAndReleaseResources(NULL, hglrc, NULL, NULL,
                "GL profile isn't PS 3.0 capable");
        return NULL;
    }

    wglGetExtensionsStringARB = (PFNWGLGETEXTENSIONSSTRINGARBPROC)
            wglGetProcAddress("wglGetExtensionsStringARB");
    if (wglGetExtensionsStringARB == NULL) {
        printAndReleaseResources(NULL, hglrc, NULL, NULL,
                "wglGetExtensionsStringARB is not supported!");
        return NULL;
    }
    wglExtensions = (char *) wglGetExtensionsStringARB(hdc);
    if (wglExtensions == NULL) {
        printAndReleaseResources(NULL, hglrc, NULL, NULL,
                "wglExtensions == null");
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
    ctxInfo->versionStr = _strdup(glVersion);
    ctxInfo->vendorStr = _strdup(glVendor);
    ctxInfo->rendererStr = _strdup(glRenderer);
    ctxInfo->glExtensionStr = _strdup(glExtensions);
    ctxInfo->wglExtensionStr = _strdup(wglExtensions);
    ctxInfo->versionNumbers[0] = versionNumbers[0];
    ctxInfo->versionNumbers[1] = versionNumbers[1];
    ctxInfo->hglrc = hglrc;

    /* set function pointers */
    ctxInfo->glActiveTexture = (PFNGLACTIVETEXTUREPROC)
            wglGetProcAddress("glActiveTexture");
    ctxInfo->glAttachShader = (PFNGLATTACHSHADERPROC)
            wglGetProcAddress("glAttachShader");
    ctxInfo->glBindAttribLocation = (PFNGLBINDATTRIBLOCATIONPROC)
            wglGetProcAddress("glBindAttribLocation");
    ctxInfo->glBindFramebuffer = (PFNGLBINDFRAMEBUFFERPROC)
            wglGetProcAddress("glBindFramebuffer");
    ctxInfo->glBindRenderbuffer = (PFNGLBINDRENDERBUFFERPROC)
            wglGetProcAddress("glBindRenderbuffer");
    ctxInfo->glCheckFramebufferStatus = (PFNGLCHECKFRAMEBUFFERSTATUSPROC)
            wglGetProcAddress("glCheckFramebufferStatus");
    ctxInfo->glCreateProgram = (PFNGLCREATEPROGRAMPROC)
            wglGetProcAddress("glCreateProgram");
    ctxInfo->glCreateShader = (PFNGLCREATESHADERPROC)
            wglGetProcAddress("glCreateShader");
    ctxInfo->glCompileShader = (PFNGLCOMPILESHADERPROC)
            wglGetProcAddress("glCompileShader");
    ctxInfo->glDeleteBuffers = (PFNGLDELETEBUFFERSPROC)
            wglGetProcAddress("glDeleteBuffers");
    ctxInfo->glDeleteFramebuffers = (PFNGLDELETEFRAMEBUFFERSPROC)
            wglGetProcAddress("glDeleteFramebuffers");
    ctxInfo->glDeleteProgram = (PFNGLDELETEPROGRAMPROC)
            wglGetProcAddress("glDeleteProgram");
    ctxInfo->glDeleteRenderbuffers = (PFNGLDELETERENDERBUFFERSPROC)
            wglGetProcAddress("glDeleteRenderbuffers");
    ctxInfo->glDeleteShader = (PFNGLDELETESHADERPROC)
            wglGetProcAddress("glDeleteShader");
    ctxInfo->glDetachShader = (PFNGLDETACHSHADERPROC)
            wglGetProcAddress("glDetachShader");
    ctxInfo->glDisableVertexAttribArray = (PFNGLDISABLEVERTEXATTRIBARRAYPROC)
            wglGetProcAddress("glDisableVertexAttribArray");
    ctxInfo->glEnableVertexAttribArray = (PFNGLENABLEVERTEXATTRIBARRAYPROC)
            wglGetProcAddress("glEnableVertexAttribArray");
    ctxInfo->glFramebufferRenderbuffer = (PFNGLFRAMEBUFFERRENDERBUFFERPROC)
            wglGetProcAddress("glFramebufferRenderbuffer");
    ctxInfo->glFramebufferTexture2D = (PFNGLFRAMEBUFFERTEXTURE2DPROC)
            wglGetProcAddress("glFramebufferTexture2D");
    ctxInfo->glGenFramebuffers = (PFNGLGENFRAMEBUFFERSPROC)
            wglGetProcAddress("glGenFramebuffers");
    ctxInfo->glGenRenderbuffers = (PFNGLGENRENDERBUFFERSPROC)
            wglGetProcAddress("glGenRenderbuffers");
    ctxInfo->glGetProgramiv = (PFNGLGETPROGRAMIVPROC)
            wglGetProcAddress("glGetProgramiv");
    ctxInfo->glGetShaderiv = (PFNGLGETSHADERIVPROC)
            wglGetProcAddress("glGetShaderiv");
    ctxInfo->glGetUniformLocation = (PFNGLGETUNIFORMLOCATIONPROC)
            wglGetProcAddress("glGetUniformLocation");
    ctxInfo->glLinkProgram = (PFNGLLINKPROGRAMPROC)
            wglGetProcAddress("glLinkProgram");
    ctxInfo->glRenderbufferStorage = (PFNGLRENDERBUFFERSTORAGEPROC)
            wglGetProcAddress("glRenderbufferStorage");
    ctxInfo->glShaderSource = (PFNGLSHADERSOURCEPROC)
            wglGetProcAddress("glShaderSource");
    ctxInfo->glUniform1f = (PFNGLUNIFORM1FPROC)
            wglGetProcAddress("glUniform1f");
    ctxInfo->glUniform2f = (PFNGLUNIFORM2FPROC)
            wglGetProcAddress("glUniform2f");
    ctxInfo->glUniform3f = (PFNGLUNIFORM3FPROC)
            wglGetProcAddress("glUniform3f");
    ctxInfo->glUniform4f = (PFNGLUNIFORM4FPROC)
            wglGetProcAddress("glUniform4f");
    ctxInfo->glUniform4fv = (PFNGLUNIFORM4FVPROC)
            wglGetProcAddress("glUniform4fv");
    ctxInfo->glUniform1i = (PFNGLUNIFORM1IPROC)
            wglGetProcAddress("glUniform1i");
    ctxInfo->glUniform2i = (PFNGLUNIFORM2IPROC)
            wglGetProcAddress("glUniform2i");
    ctxInfo->glUniform3i = (PFNGLUNIFORM3IPROC)
            wglGetProcAddress("glUniform3i");
    ctxInfo->glUniform4i = (PFNGLUNIFORM4IPROC)
            wglGetProcAddress("glUniform4i");
    ctxInfo->glUniform4iv = (PFNGLUNIFORM4IVPROC)
            wglGetProcAddress("glUniform4iv");
    ctxInfo->glUniformMatrix4fv = (PFNGLUNIFORMMATRIX4FVPROC)
            wglGetProcAddress("glUniformMatrix4fv");
    ctxInfo->glUseProgram = (PFNGLUSEPROGRAMPROC)
            wglGetProcAddress("glUseProgram");
    ctxInfo->glValidateProgram = (PFNGLVALIDATEPROGRAMPROC)
            wglGetProcAddress("glValidateProgram");
    ctxInfo->glVertexAttribPointer = (PFNGLVERTEXATTRIBPOINTERPROC)
            wglGetProcAddress("glVertexAttribPointer");
    ctxInfo->glGenBuffers = (PFNGLGENBUFFERSPROC)
            wglGetProcAddress("glGenBuffers");
    ctxInfo->glBindBuffer = (PFNGLBINDBUFFERPROC)
            wglGetProcAddress("glBindBuffer");
    ctxInfo->glBufferData = (PFNGLBUFFERDATAPROC)
            wglGetProcAddress("glBufferData");
    ctxInfo->glBufferSubData = (PFNGLBUFFERSUBDATAPROC)
            wglGetProcAddress("glBufferSubData");
    ctxInfo->glGetShaderInfoLog = (PFNGLGETSHADERINFOLOGPROC)
            wglGetProcAddress("glGetShaderInfoLog");
    ctxInfo->glGetProgramInfoLog = (PFNGLGETPROGRAMINFOLOGPROC)
            wglGetProcAddress("glGetProgramInfoLog");
    ctxInfo->glTexImage2DMultisample = (PFNGLTEXIMAGE2DMULTISAMPLEPROC)
            wglGetProcAddress("glTexImage2DMultisample");
    ctxInfo->glRenderbufferStorageMultisample = (PFNGLRENDERBUFFERSTORAGEMULTISAMPLEPROC)
            wglGetProcAddress("glRenderbufferStorageMultisample");
    ctxInfo->glBlitFramebuffer = (PFNGLBLITFRAMEBUFFERPROC)
            wglGetProcAddress("glBlitFramebuffer");

    if (isExtensionSupported(ctxInfo->wglExtensionStr,
            "WGL_EXT_swap_control")) {
        ctxInfo->wglSwapIntervalEXT = (PFNWGLSWAPINTERVALEXTPROC)
                wglGetProcAddress("wglSwapIntervalEXT");
    }

    // initialize platform states and properties to match
    // cached states and properties
    if (ctxInfo->wglSwapIntervalEXT != NULL) {
        ctxInfo->wglSwapIntervalEXT(0);
    }
    ctxInfo->state.vSyncEnabled = GL_FALSE;
    ctxInfo->vSyncRequested = ES2_BOOL(vsync_requested);

    initState(ctxInfo);

    // Release context once we are all done
    wglMakeCurrent(NULL, NULL);

    return ctxInfo;
}

PRISM_ES2_EXPORT int64_t
es2_context_get_native_handle(void *ctx) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if (ctxInfo == NULL) {
        return 0;
    }
    return (int64_t) (intptr_t) ctxInfo->hglrc;
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

    if (!wglMakeCurrent(dInfo->hdc, ctxInfo->hglrc)) {
        fprintf(stderr, "Failed in wglMakeCurrent");
    }

    vSyncNeeded = ES2_BOOL(ctxInfo->vSyncRequested && dInfo->onScreen);
    if (vSyncNeeded == ctxInfo->state.vSyncEnabled) {
        return;
    }
    interval = (vSyncNeeded) ? 1 : 0;
    ctxInfo->state.vSyncEnabled = vSyncNeeded;
    if (ctxInfo->wglSwapIntervalEXT != NULL) {
        ctxInfo->wglSwapIntervalEXT(interval);
    }
}
