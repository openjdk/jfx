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
 * prism_es2_api_x11.c - the Linux (X11 / GLX) lifecycle half of the flat C ABI in prism_es2_api.h.
 *
 * The bodies are Java_com_sun_prism_es2_X11GLFactory_nInitialize / nGetDefaultScreen / nGetDisplay /
 * nGetVisualID, X11GLPixelFormat_nCreatePixelFormat, X11GLDrawable_nCreateDrawable /
 * nGetDummyDrawable / nSwapBuffers / nReleaseDrawable and X11GLContext_nInitialize /
 * nGetNativeHandle / nMakeCurrent with the JNI marshalling removed. The helpers (setGLXAttrs,
 * printAndReleaseResources, queryGLX13) stay in X11GLFactory.c and are reached through the same
 * extern declarations X11GLPixelFormat.c uses; setGLXAttrs takes the Es2PixelFormatAttrs struct
 * directly. The X11 error trap (x11errorhit / x11errorDetector) is file-static here.
 */

#include "../prism_es2_api.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <X11/Xutil.h>

#include "../PrismES2Defs.h"

extern void setGLXAttrs(const Es2PixelFormatAttrs *attrs, int *glxAttrs);
extern void printAndReleaseResources(Display *display, GLXFBConfig *fbConfigList,
        XVisualInfo *visualInfo, Window win, GLXContext ctx, Colormap cmap,
        const char *message);
extern GLboolean queryGLX13(Display *display);

#define ES2_BOOL(x) ((GLboolean) ((x) != 0))

/* The handler is installed only around glXCreateNewContext in es2_factory_init; Xlib ignores
 * its return value. */
static int x11errorhit = 0;

static int x11errorDetector(Display *dpy, XErrorEvent *error) {
    (void) dpy;
    (void) error;
    x11errorhit = 1;
    return 0;
}

/* ------------------------------------------------------------------------------------------------
 * Factory
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT void *
es2_factory_init(const Es2PixelFormatAttrs *attrs) {

    int glxAttrs[MAX_GLX_ATTRS_LENGTH]; /* value, attr pair plus a None */
    ContextInfo *ctxInfo = NULL;

    const char *glVersion;
    const char *glVendor;
    const char *glRenderer;
    char *tmpVersionStr;
    int versionNumbers[2];
    const char *glExtensions;
    const char *glxExtensions;

    GLXFBConfig *fbConfigList = NULL;
    GLXContext ctx = NULL;
    XVisualInfo *visualInfo = NULL;
    int numFBConfigs;
    Display *display = NULL;
    int screen;
    Window root;
    Window win = None;
    XSetWindowAttributes win_attrs;
    Colormap cmap = None;
    unsigned long win_mask;
    int (*old_error_handler) (Display *, XErrorEvent *);

    if (attrs == NULL) {
        return NULL;
    }
    setGLXAttrs(attrs, glxAttrs);

    display = XOpenDisplay(0);
    if (display == NULL) {
        return NULL;
    }

    screen = DefaultScreen(display);

    if (!queryGLX13(display)) {
        return NULL;
    }

    fbConfigList = glXChooseFBConfig(display, screen, glxAttrs, &numFBConfigs);

    if (fbConfigList == NULL) {
        fprintf(stderr, "Prism ES2 Error - nInitialize: glXChooseFBConfig failed\n");
        return NULL;
    }

    visualInfo = glXGetVisualFromFBConfig(display, fbConfigList[0]);
    if (visualInfo == NULL) {
        printAndReleaseResources(display, fbConfigList, visualInfo,
                win, ctx, cmap,
                "Failed in  glXGetVisualFromFBConfig");
        return NULL;
    }

    root = RootWindow(display, visualInfo->screen);

    /* Create a colormap */
    cmap = XCreateColormap(display, root, visualInfo->visual, AllocNone);

    /* Create a 1x1 window */
    win_attrs.colormap = cmap;
    win_attrs.border_pixel = 0;
    win_attrs.event_mask = KeyPressMask | ExposureMask | StructureNotifyMask;
    win_mask = CWColormap | CWBorderPixel | CWEventMask;
    win = XCreateWindow(display, root, 0, 0, 1, 1, 0,
            visualInfo->depth, InputOutput, visualInfo->visual, win_mask, &win_attrs);

    if (win == None) {
        printAndReleaseResources(display, fbConfigList, visualInfo, win, ctx, cmap,
                "Failed in XCreateWindow");
        return NULL;
    }

    old_error_handler = XSetErrorHandler(x11errorDetector);

    ctx = glXCreateNewContext(display, fbConfigList[0], GLX_RGBA_TYPE, NULL, True);

    XSync(display, 0); // sync needed for the GLX error detection.


    if (x11errorhit) {
        // An X11 Error was hit along the way. This would happen if GLX is
        // disabled which recently became the X11 default for remote connections
        printAndReleaseResources(display, fbConfigList, visualInfo, win, ctx, cmap,
                "Error in glXCreateNewContext, remote GLX is likely disabled");
        XSync(display, 0); // sync needed for the GLX error detection.
        XSetErrorHandler(old_error_handler);
        return NULL;
    }

    XSetErrorHandler(old_error_handler);

    if (ctx == NULL) {
        printAndReleaseResources(display, fbConfigList, visualInfo, win, ctx, cmap,
                "Failed in glXCreateNewContext");
        return NULL;
    }

    if (!glXMakeCurrent(display, win, ctx)) {
        printAndReleaseResources(display, fbConfigList, visualInfo, win, ctx, cmap,
                "Failed in glXMakeCurrent");
        return NULL;
    }

    /* Get the OpenGL version */
    glVersion = (char *) glGetString(GL_VERSION);
    if (glVersion == NULL) {
        printAndReleaseResources(display, fbConfigList, visualInfo, win, ctx, cmap,
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
        fprintf(stderr, "Prism-ES2 Error : GL_VERSION (major.minor) = %d.%d\n",
                versionNumbers[0], versionNumbers[1]);
        printAndReleaseResources(display, fbConfigList, visualInfo, win, ctx, cmap, NULL);
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
        printAndReleaseResources(display, fbConfigList, visualInfo, win, ctx, cmap,
                "Prism-ES2 Error : glExtensions == null");
        return NULL;
    }

    // We use GL_ARB_pixel_buffer_object as an guide to
    // determine PS 3.0 capable.
    if (!isExtensionSupported(glExtensions, "GL_ARB_pixel_buffer_object")) {
        printAndReleaseResources(display, fbConfigList, visualInfo,
                win, ctx, cmap, "GL profile isn't PS 3.0 capable");
        return NULL;
    }

    glxExtensions = (const char *) glXGetClientString(display, GLX_EXTENSIONS);
    if (glxExtensions == NULL) {
        printAndReleaseResources(display, fbConfigList, visualInfo, win, ctx, cmap,
                "glxExtensions == null");
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
    ctxInfo->versionStr = strdup(glVersion);
    ctxInfo->vendorStr = strdup(glVendor);
    ctxInfo->rendererStr = strdup(glRenderer);
    ctxInfo->glExtensionStr = strdup(glExtensions);
    ctxInfo->glxExtensionStr = strdup(glxExtensions);
    ctxInfo->versionNumbers[0] = versionNumbers[0];
    ctxInfo->versionNumbers[1] = versionNumbers[1];
    ctxInfo->gl2 = GL_TRUE;

    /* Information required by GLass at startup */
    ctxInfo->display = display;
    ctxInfo->screen = screen;
    ctxInfo->visualID = (int) visualInfo->visualid;

    /* Releasing native resources */
    printAndReleaseResources(display, fbConfigList, visualInfo, win, ctx, cmap, NULL);

    return ctxInfo;
}

PRISM_ES2_EXPORT int32_t
es2_factory_get_x11_info(void *ctx, int64_t info[3]) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if ((ctxInfo == NULL) || (info == NULL)) {
        return ES2_ERR_INVALID_ARG;
    }
    info[0] = (int64_t) ctxInfo->screen;
    info[1] = (int64_t) (intptr_t) ctxInfo->display;
    info[2] = (int64_t) ctxInfo->visualID;
    return ES2_OK;
}

/* ------------------------------------------------------------------------------------------------
 * Pixel format
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT void *
es2_pixel_format_create(int64_t native_screen, const Es2PixelFormatAttrs *attrs) {
    int glxAttrs[MAX_GLX_ATTRS_LENGTH]; /* value, attr pair plus a None */
    PixelFormatInfo *pfInfo = NULL;

    GLXFBConfig *fbConfigList = NULL;
    XVisualInfo *visualInfo = NULL;
    int numFBConfigs;
    Display *display;
    int screen;
    Window root;
    Window win = None;
    XSetWindowAttributes win_attrs;
    Colormap cmap;
    unsigned long win_mask;

    (void) native_screen; /* JDK-8091981: unused, single-monitor assumption, as before */
    if (attrs == NULL) {
        return NULL;
    }
    setGLXAttrs(attrs, glxAttrs);

    // JDK-8091981
    // TODO: Need to use nativeScreen to create this requested pixelformat
    // currently hack to work on a single monitor system
    display = XOpenDisplay(0);
    if (display == NULL) {
        fprintf(stderr, "Failed in XOpenDisplay\n");
        return NULL;
    }

    screen = DefaultScreen(display);

    fbConfigList = glXChooseFBConfig(display, screen, glxAttrs, &numFBConfigs);

    if (fbConfigList == NULL) {
        fprintf(stderr, "Failed in glXChooseFBConfig\n");
        return NULL;
    }

    visualInfo = glXGetVisualFromFBConfig(display, fbConfigList[0]);
    if (visualInfo == NULL) {
        printAndReleaseResources(display, fbConfigList, NULL,
                None, NULL, None,
                "Failed in glXGetVisualFromFBConfig");
        return NULL;
    }

    root = RootWindow(display, visualInfo->screen);

    /* Create a colormap */
    cmap = XCreateColormap(display, root, visualInfo->visual, AllocNone);

    /* Create a 1x1 window */
    win_attrs.colormap = cmap;
    win_attrs.border_pixel = 0;
    win_attrs.event_mask = KeyPressMask | ExposureMask | StructureNotifyMask;
    win_mask = CWColormap | CWBorderPixel | CWEventMask;
    win = XCreateWindow(display, root, 0, 0, 1, 1, 0,
            visualInfo->depth, InputOutput, visualInfo->visual, win_mask, &win_attrs);

    if (win == None) {
        printAndReleaseResources(display, fbConfigList, visualInfo,
                win, NULL, cmap,
                "Failed in XCreateWindow");
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
    pfInfo->display = display;
    pfInfo->fbConfig = fbConfigList[0];
    pfInfo->dummyWin = win;
    pfInfo->dummyCmap = cmap;

    XFree(visualInfo);
    XFree(fbConfigList);

    return pfInfo;
}

/* ------------------------------------------------------------------------------------------------
 * Drawable
 * ---------------------------------------------------------------------------------------------- */

PRISM_ES2_EXPORT void *
es2_drawable_create(void *pf, int64_t native_window) {
    DrawableInfo *dInfo = NULL;
    PixelFormatInfo *pfInfo = (PixelFormatInfo *) pf;
    if (pfInfo == NULL) {
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

    dInfo->display = pfInfo->display;
    dInfo->win = (Window) native_window;
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

    // Use the dummyWin that was already created in the pfInfo
    // since this is an non-onscreen drawable.
    dInfo->display = pfInfo->display;
    dInfo->win = pfInfo->dummyWin;
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
    DrawableInfo *dInfo = (DrawableInfo *) drawable;
    (void) ctx; /* used on macOS only */
    if (dInfo == NULL) {
        return 0;
    }
    glXSwapBuffers(dInfo->display, dInfo->win);
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
    const char *glxExtensions;

    Window win = None;
    GLXFBConfig fbConfig = NULL;
    GLXContext ctx = NULL;
    Display *display = NULL;
    ContextInfo *ctxInfo = NULL;
    DrawableInfo *dInfo = (DrawableInfo *) drawable;
    PixelFormatInfo *pfInfo = (PixelFormatInfo *) pf;

    (void) share_ctx_handle; /* GLX contexts are not shared, as before (X11GLFactory.java) */
    if ((dInfo == NULL) || (pfInfo == NULL)) {
        return NULL;
    }
    display = pfInfo->display;
    fbConfig = pfInfo->fbConfig;
    win = dInfo->win;

    ctx = glXCreateNewContext(display, fbConfig, GLX_RGBA_TYPE, NULL, True);

    if (ctx == NULL) {
        fprintf(stderr, "Failed in glXCreateNewContext");
        return NULL;
    }

    if (!glXMakeCurrent(display, win, ctx)) {
        glXDestroyContext(display, ctx);
        fprintf(stderr, "Failed in glXMakeCurrent");
        return NULL;
    }

    /* Get the OpenGL version */
    glVersion = (char *) glGetString(GL_VERSION);
    if (glVersion == NULL) {
        glXDestroyContext(display, ctx);
        fprintf(stderr, "glVersion == null");
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
        glXDestroyContext(display, ctx);
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
        glXDestroyContext(display, ctx);
        fprintf(stderr, "glExtensions == null");
        return NULL;
    }

    // We use GL_ARB_pixel_buffer_object as an guide to
    // determine PS 3.0 capable.
    if (!isExtensionSupported(glExtensions, "GL_ARB_pixel_buffer_object")) {
        glXDestroyContext(display, ctx);
        fprintf(stderr, "GL profile isn't PS 3.0 capable");
        return NULL;
    }

    glxExtensions = (const char *) glXGetClientString(display, GLX_EXTENSIONS);
    if (glxExtensions == NULL) {
        glXDestroyContext(display, ctx);
        fprintf(stderr, "glxExtensions == null");
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
    ctxInfo->glxExtensionStr = strdup(glxExtensions);
    ctxInfo->versionNumbers[0] = versionNumbers[0];
    ctxInfo->versionNumbers[1] = versionNumbers[1];
    ctxInfo->display = display;
    ctxInfo->context = ctx;

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
            dlsym(RTLD_DEFAULT,"glTexImage2DMultisample");
    ctxInfo->glRenderbufferStorageMultisample = (PFNGLRENDERBUFFERSTORAGEMULTISAMPLEPROC)
            dlsym(RTLD_DEFAULT,"glRenderbufferStorageMultisample");
    ctxInfo->glBlitFramebuffer = (PFNGLBLITFRAMEBUFFERPROC)
            dlsym(RTLD_DEFAULT,"glBlitFramebuffer");

    if (isExtensionSupported(ctxInfo->glxExtensionStr,
            "GLX_SGI_swap_control")) {
        ctxInfo->glXSwapIntervalSGI = (PFNGLXSWAPINTERVALSGIPROC)
                dlsym(RTLD_DEFAULT, "glXSwapIntervalSGI");

        if (ctxInfo->glXSwapIntervalSGI == NULL) {
            ctxInfo->glXSwapIntervalSGI = (PFNGLXSWAPINTERVALSGIPROC)
                glXGetProcAddress((const GLubyte *)"glXSwapIntervalSGI");
        }

    }

    // initialize platform states and properties to match
    // cached states and properties
    if (ctxInfo->glXSwapIntervalSGI != NULL) {
        ctxInfo->glXSwapIntervalSGI(0);
    }
    ctxInfo->state.vSyncEnabled = GL_FALSE;
    ctxInfo->vSyncRequested = ES2_BOOL(vsync_requested);

    initState(ctxInfo);

    // Release context once we are all done
    glXMakeCurrent(display, None, NULL);

    return ctxInfo;
}

PRISM_ES2_EXPORT int64_t
es2_context_get_native_handle(void *ctx) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    if (ctxInfo == NULL) {
        return 0;
    }
    return (int64_t) (intptr_t) ctxInfo->context;
}

PRISM_ES2_EXPORT void
es2_context_make_current(void *ctx, void *drawable) {
    ContextInfo *ctxInfo = (ContextInfo *) ctx;
    DrawableInfo *dInfo = (DrawableInfo *) drawable;
    int interval;
    GLboolean vSyncNeeded;

    /* New guard: X11GLContext.c:326 at commit 868c4801ec dereferenced both without a check; the Windows
     * and macOS ports had this check, and the ABI documents NULL as a no-op on every platform. */
    if ((ctxInfo == NULL) || (dInfo == NULL)) {
        return;
    }

    if (!glXMakeCurrent(ctxInfo->display, dInfo->win, ctxInfo->context)) {
        fprintf(stderr, "Failed in glXMakeCurrent");
    }

    vSyncNeeded = ES2_BOOL(ctxInfo->vSyncRequested && dInfo->onScreen);
    if (vSyncNeeded == ctxInfo->state.vSyncEnabled) {
        return;
    }
    interval = (vSyncNeeded) ? 1 : 0;
    ctxInfo->state.vSyncEnabled = vSyncNeeded;
    if (ctxInfo->glXSwapIntervalSGI != NULL) {
        ctxInfo->glXSwapIntervalSGI(interval);
    }
}
