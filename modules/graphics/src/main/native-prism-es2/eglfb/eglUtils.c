/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <linux/fb.h>
#include <sys/ioctl.h>

#include "../PrismES2Defs.h"

#include "eglUtils.h"

#include "eglWrapper/eglWrapper.h"

#ifdef EGL_X11_FB_CONTAINER
#include "X11/Xlib.h"
#endif

#define WARN_MISSING_SYMBOLS 0

void *get_dlsym(void *handle, const char *symbol, int warn) {
    void *ret = dlsym(handle, symbol);
    if (!ret && warn) {
        fprintf(stderr, "ERROR: could not find symbol for %s\n", symbol);
    }
    return ret;
}

#define GET_DLSYM(handle,symbol) get_dlsym(handle,symbol, WARN_MISSING_SYMBOLS);

EGLSurface sharedWindowSurface = NULL;
#ifdef ANDROID_NDK
EGLNativeWindowType currentNativeWindow = NULL;
#endif
#ifdef EGL_X11_FB_CONTAINER
EGLSurface dummySurface = NULL;
#endif

EGLSurface getDummyWindowSurface(EGLDisplay dpy, EGLConfig cfg) {
#ifdef EGL_X11_FB_CONTAINER
    if (dummySurface == NULL) {
        Display *display;
        Window window;

        display = XOpenDisplay(0);
        if (display == NULL) {
            fprintf(stderr, "XOpenDisplay failed\n");
            return 0;
        }
        window = XCreateWindow(display,
                               RootWindow(display, DefaultScreen(display)),
                               0, 0, 1, 1, 0,
                               CopyFromParent, InputOutput, CopyFromParent, 0,
                               (XSetWindowAttributes *) 0);
        XSync(display, False);
        dummySurface = eglCreateWindowSurface(dpy, cfg, window, NULL);
        XSync(display, False);
    }
    return dummySurface;
#else
    return getSharedWindowSurface(dpy, cfg, NULL);
#endif
}

EGLSurface getSharedWindowSurface(EGLDisplay dpy,
                                  EGLConfig cfg,
                                  void *nativeWindow) {
    if (sharedWindowSurface == NULL) {
        EGLNativeWindowType window = 0;
#if EGL_X11_FB_CONTAINER
        window = (EGLNativeWindowType)nativeWindow;
#else
        if (nativeWindow == NULL) {
            window = getNativeWindowType();
        }
#endif
        sharedWindowSurface = eglCreateWindowSurface(dpy, cfg, window, NULL);
        if (sharedWindowSurface == EGL_NO_SURFACE) {
            fprintf(stderr, "eglCreateWindowSurface failed! eglGetError %d\n", eglGetError());
            return 0;
        }
#ifdef ANDROID_NDK
        currentNativeWindow = window;
#endif
        return sharedWindowSurface;
    }
#ifdef ANDROID_NDK
    EGLNativeWindowType wnd = getNativeWindowType();
    if (currentNativeWindow != wnd) {
       sharedWindowSurface = eglCreateWindowSurface(dpy, cfg, wnd, NULL);
       if (sharedWindowSurface == EGL_NO_SURFACE) {
           fprintf(stderr, "Recreating eglSurface: eglCreateWindowSurface failed! eglGetError %d\n", eglGetError());
           return 0;
       }
       currentNativeWindow = wnd;
    }
#endif
    return sharedWindowSurface;
}

void setEGLAttrs(jint *attrs, int *eglAttrs) {
    int index = 0;

    eglAttrs[index++] = EGL_SURFACE_TYPE;
    if (attrs[ONSCREEN] != 0) {
        eglAttrs[index++] = (EGL_WINDOW_BIT);
    } else {
        eglAttrs[index++] = EGL_PBUFFER_BIT;
    }

    // NOTE: EGL_TRANSPARENT_TYPE ?

    if (attrs[RED_SIZE] == 5 && attrs[GREEN_SIZE] == 6
            && attrs[BLUE_SIZE] == 5 && attrs[ALPHA_SIZE] == 0) {
        // Optimization for Raspberry Pi model B. Even though the result
        // of setting EGL_BUFFER_SIZE to 16 should be the same as setting
        // component sizes separately, we get less per-frame overhead if we
        // only set EGL_BUFFER_SIZE.
        eglAttrs[index++] = EGL_BUFFER_SIZE;
        eglAttrs[index++] = 16;
    } else {
        eglAttrs[index++] = EGL_RED_SIZE;
        eglAttrs[index++] = attrs[RED_SIZE];
        eglAttrs[index++] = EGL_GREEN_SIZE;
        eglAttrs[index++] = attrs[GREEN_SIZE];
        eglAttrs[index++] = EGL_BLUE_SIZE;
        eglAttrs[index++] = attrs[BLUE_SIZE];
        eglAttrs[index++] = EGL_ALPHA_SIZE;
        eglAttrs[index++] = attrs[ALPHA_SIZE];
    }

    eglAttrs[index++] = EGL_DEPTH_SIZE;
    eglAttrs[index++] = attrs[DEPTH_SIZE];
    eglAttrs[index++] = EGL_RENDERABLE_TYPE;
    eglAttrs[index++] = EGL_OPENGL_ES2_BIT;
    eglAttrs[index] = EGL_NONE;
}

ContextInfo *eglContextFromConfig(EGLDisplay *dpy, EGLConfig config) {

    EGLSurface surface = getDummyWindowSurface(dpy, config);

    EGLint contextAttrs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE
    };

    EGLContext context = eglCreateContext(dpy, config, NULL, contextAttrs);
    if (context == EGL_NO_CONTEXT) {
        fprintf(stderr, "eglCreateContext() failed - %d\n", eglGetError());
        return 0;
    }

    if (!eglMakeCurrent(dpy, surface, surface, context)) {
        fprintf(stderr, "eglMakeCurrent failed - %d\n", eglGetError());
        return 0;
    }
    ContextInfo *ctxInfo = NULL;

    /* Note: We are only storing the string information of a driver.
     Assuming a system with a single or homogeneous GPUs. For the case
     of heterogeneous GPUs system the string information will need to move to
     GLContext class. */
    /* allocate the structure */
    ctxInfo = (ContextInfo *) malloc(sizeof(ContextInfo));
    if (ctxInfo == NULL) {
        fprintf(stderr, "nInitialize: Failed in malloc\n");
        return 0;
    }
    /* initialize the structure */
    initializeCtxInfo(ctxInfo);

    const char *glVersion = (char *)glGetString(GL_VERSION);
    const char *glVendor = (char *)glGetString(GL_VENDOR);
    const char *glRenderer = (char *)glGetString(GL_RENDERER);
    // Make a copy, at least one platform does not preserve the string beyond the call.
    char *glExtensions = strdup((char *)glGetString(GL_EXTENSIONS));
    char *eglExtensions = strdup((char *)eglQueryString(dpy, EGL_EXTENSIONS));

    /* find out the version, major and minor version number */
    char *tmpVersionStr = strdup(glVersion);
    int versionNumbers[2];
    extractVersionInfo(tmpVersionStr, versionNumbers);
    free(tmpVersionStr);

    ctxInfo->versionStr = strdup(glVersion);
    ctxInfo->vendorStr = strdup(glVendor);
    ctxInfo->rendererStr = strdup(glRenderer);
    ctxInfo->glExtensionStr = strdup(glExtensions);
    ctxInfo->glxExtensionStr = strdup(eglExtensions);
    ctxInfo->versionNumbers[0] = versionNumbers[0];
    ctxInfo->versionNumbers[1] = versionNumbers[1];

    ctxInfo->display = getNativeDisplayType();
    ctxInfo->context = context;
    ctxInfo->egldisplay = dpy;

    // cleanup
    free(glExtensions);
    free(eglExtensions);

    // from the eglWrapper.c
    void *handle = getLibGLEShandle();

    /* set function pointers */
    ctxInfo->glActiveTexture = (PFNGLACTIVETEXTUREPROC)
                               GET_DLSYM(handle, "glActiveTexture");
    ctxInfo->glAttachShader = (PFNGLATTACHSHADERPROC)
                              GET_DLSYM(handle, "glAttachShader");
    ctxInfo->glBindAttribLocation = (PFNGLBINDATTRIBLOCATIONPROC)
                                    GET_DLSYM(handle, "glBindAttribLocation");
    ctxInfo->glBindFramebuffer = (PFNGLBINDFRAMEBUFFERPROC)
                                 GET_DLSYM(handle, "glBindFramebuffer");
    ctxInfo->glBindRenderbuffer = (PFNGLBINDRENDERBUFFERPROC)
                                  GET_DLSYM(handle, "glBindRenderbuffer");
    ctxInfo->glCheckFramebufferStatus = (PFNGLCHECKFRAMEBUFFERSTATUSPROC)
                                        GET_DLSYM(handle, "glCheckFramebufferStatus");
    ctxInfo->glCreateProgram = (PFNGLCREATEPROGRAMPROC)
                               GET_DLSYM(handle, "glCreateProgram");
    ctxInfo->glCreateShader = (PFNGLCREATESHADERPROC)
                              GET_DLSYM(handle, "glCreateShader");
    ctxInfo->glCompileShader = (PFNGLCOMPILESHADERPROC)
                               GET_DLSYM(handle, "glCompileShader");
    ctxInfo->glDeleteBuffers = (PFNGLDELETEBUFFERSPROC)
                               GET_DLSYM(handle, "glDeleteBuffers");
    ctxInfo->glDeleteFramebuffers = (PFNGLDELETEFRAMEBUFFERSPROC)
                                    GET_DLSYM(handle, "glDeleteFramebuffers");
    ctxInfo->glDeleteProgram = (PFNGLDELETEPROGRAMPROC)
                               GET_DLSYM(handle, "glDeleteProgram");
    ctxInfo->glDeleteRenderbuffers = (PFNGLDELETERENDERBUFFERSPROC)
                                     GET_DLSYM(handle, "glDeleteRenderbuffers");
    ctxInfo->glDeleteShader = (PFNGLDELETESHADERPROC)
                              GET_DLSYM(handle, "glDeleteShader");
    ctxInfo->glDetachShader = (PFNGLDETACHSHADERPROC)
                              GET_DLSYM(handle, "glDetachShader");
    ctxInfo->glDisableVertexAttribArray = (PFNGLDISABLEVERTEXATTRIBARRAYPROC)
                                         GET_DLSYM(handle, "glDisableVertexAttribArray");
    ctxInfo->glEnableVertexAttribArray = (PFNGLENABLEVERTEXATTRIBARRAYPROC)
                                         GET_DLSYM(handle, "glEnableVertexAttribArray");
    ctxInfo->glFramebufferRenderbuffer = (PFNGLFRAMEBUFFERRENDERBUFFERPROC)
                                         GET_DLSYM(handle, "glFramebufferRenderbuffer");
    ctxInfo->glFramebufferTexture2D = (PFNGLFRAMEBUFFERTEXTURE2DPROC)
                                      GET_DLSYM(handle, "glFramebufferTexture2D");
    ctxInfo->glGenFramebuffers = (PFNGLGENFRAMEBUFFERSPROC)
                                 GET_DLSYM(handle, "glGenFramebuffers");
    ctxInfo->glGenRenderbuffers = (PFNGLGENRENDERBUFFERSPROC)
                                  GET_DLSYM(handle, "glGenRenderbuffers");
    ctxInfo->glGetProgramiv = (PFNGLGETPROGRAMIVPROC)
                              GET_DLSYM(handle, "glGetProgramiv");
    ctxInfo->glGetShaderiv = (PFNGLGETSHADERIVPROC)
                             GET_DLSYM(handle, "glGetShaderiv");
    ctxInfo->glGetUniformLocation = (PFNGLGETUNIFORMLOCATIONPROC)
                                    GET_DLSYM(handle, "glGetUniformLocation");
    ctxInfo->glLinkProgram = (PFNGLLINKPROGRAMPROC)
                             GET_DLSYM(handle, "glLinkProgram");
    ctxInfo->glRenderbufferStorage = (PFNGLRENDERBUFFERSTORAGEPROC)
                                     GET_DLSYM(handle, "glRenderbufferStorage");
    ctxInfo->glShaderSource = (PFNGLSHADERSOURCEPROC)
                              GET_DLSYM(handle, "glShaderSource");
    ctxInfo->glUniform1f = (PFNGLUNIFORM1FPROC)
                           GET_DLSYM(handle, "glUniform1f");
    ctxInfo->glUniform2f = (PFNGLUNIFORM2FPROC)
                           GET_DLSYM(handle, "glUniform2f");
    ctxInfo->glUniform3f = (PFNGLUNIFORM3FPROC)
                           GET_DLSYM(handle, "glUniform3f");
    ctxInfo->glUniform4f = (PFNGLUNIFORM4FPROC)
                           GET_DLSYM(handle, "glUniform4f");
    ctxInfo->glUniform4fv = (PFNGLUNIFORM4FVPROC)
                            GET_DLSYM(handle, "glUniform4fv");
    ctxInfo->glUniform1i = (PFNGLUNIFORM1IPROC)
                           GET_DLSYM(handle, "glUniform1i");
    ctxInfo->glUniform2i = (PFNGLUNIFORM2IPROC)
                           GET_DLSYM(handle, "glUniform2i");
    ctxInfo->glUniform3i = (PFNGLUNIFORM3IPROC)
                           GET_DLSYM(handle, "glUniform3i");
    ctxInfo->glUniform4i = (PFNGLUNIFORM4IPROC)
                           GET_DLSYM(handle, "glUniform4i");
    ctxInfo->glUniform4iv = (PFNGLUNIFORM4IVPROC)
                            GET_DLSYM(handle, "glUniform4iv");
    ctxInfo->glUniformMatrix4fv = (PFNGLUNIFORMMATRIX4FVPROC)
                                  GET_DLSYM(handle, "glUniformMatrix4fv");
    ctxInfo->glUseProgram = (PFNGLUSEPROGRAMPROC)
                            GET_DLSYM(handle, "glUseProgram");
    ctxInfo->glValidateProgram = (PFNGLVALIDATEPROGRAMPROC)
                                 GET_DLSYM(handle, "glValidateProgram");
    ctxInfo->glVertexAttribPointer = (PFNGLVERTEXATTRIBPOINTERPROC)
                                     GET_DLSYM(handle, "glVertexAttribPointer");
    ctxInfo->glGenBuffers = (PFNGLGENBUFFERSPROC)
                            GET_DLSYM(handle, "glGenBuffers");
    ctxInfo->glBindBuffer = (PFNGLBINDBUFFERPROC)
                            GET_DLSYM(handle, "glBindBuffer");
    ctxInfo->glBufferData = (PFNGLBUFFERDATAPROC)
                            GET_DLSYM(handle, "glBufferData");
    ctxInfo->glBufferSubData = (PFNGLBUFFERSUBDATAPROC)
                              GET_DLSYM(handle, "glBufferSubData");
    ctxInfo->glGetShaderInfoLog = (PFNGLGETSHADERINFOLOGPROC)
                                  GET_DLSYM(handle, "glGetShaderInfoLog");
    ctxInfo->glGetProgramInfoLog = (PFNGLGETPROGRAMINFOLOGPROC)
                                   GET_DLSYM(handle, "glGetProgramInfoLog");
    ctxInfo->glTexImage2DMultisample = (PFNGLTEXIMAGE2DMULTISAMPLEPROC)
                            GET_DLSYM(handle, "glTexImage2DMultisample");
    ctxInfo->glRenderbufferStorageMultisample = (PFNGLRENDERBUFFERSTORAGEMULTISAMPLEPROC)
                            GET_DLSYM(handle, "glRenderbufferStorageMultisample");
    ctxInfo->glBlitFramebuffer = (PFNGLBLITFRAMEBUFFERPROC)
                            GET_DLSYM(handle, "glBlitFramebuffer");

    initState(ctxInfo);
    /* Releasing native resources */
    eglMakeCurrent(ctxInfo->egldisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    //eglDestroySurface(ctxInfo->egldisplay, surface);
    return ctxInfo;
}



//#ifdef DEBUG

const char *eglErrorMsg(int err) {
    const char *ret;
    if (err == EGL_SUCCESS) {
        ret = "The last function succeeded without error.";
    } else if (err == EGL_NOT_INITIALIZED) {
        ret = "EGL is not initialized, or could not be initialized, for the specified EGL display connection.";
    } else if (err == EGL_BAD_ACCESS) {
        ret = "EGL cannot access a requested resource (for example a context is bound in another thread).";
    } else if (err == EGL_BAD_ALLOC) {
        ret = "EGL failed to allocate resources for the requested operation.";
    } else if (err == EGL_BAD_ATTRIBUTE) {
        ret = "An unrecognized attribute or attribute value was passed in the attribute list.";
    } else if (err == EGL_BAD_CONTEXT) {
        ret = "An EGLContext argument does not name a valid EGL rendering context.";
    } else if (err == EGL_BAD_CONFIG) {
        ret = "An EGLConfig argument does not name a valid EGL frame buffer configuration.";
    } else if (err == EGL_BAD_CURRENT_SURFACE) {
        ret = "The current surface of the calling thread is a window, pixel buffer or pixmap that is no longer valid.";
    } else if (err == EGL_BAD_DISPLAY) {
        ret = "An EGLDisplay argument does not name a valid EGL display connection.";
    } else if (err == EGL_BAD_SURFACE) {
        ret = "An EGLSurface argument does not name a valid surface (window, pixel buffer or pixmap) configured for GL rendering.";
    } else if (err == EGL_BAD_MATCH) {
        ret = "Arguments are inconsistent (for example, a valid context requires buffers not supplied by a valid surface).";
    } else if (err == EGL_BAD_PARAMETER) {
        ret = "One or more argument values are invalid.";
    } else if (err == EGL_BAD_NATIVE_PIXMAP) {
        ret = "A NativePixmapType argument does not refer to a valid native pixmap.";
    } else if (err == EGL_BAD_NATIVE_WINDOW) {
        ret = "A NativeWindowType argument does not refer to a valid native window.";
    } else {
        ret = "Unknown EGL error";
    }
    return ret;
}

char *printErrorExit(char *message) {
    EGLint err = eglGetError();
    char buffer[80];
    char *ret;
    if (err == EGL_SUCCESS) {
        ret = "The last function succeeded without error.";
    } else if (err == EGL_NOT_INITIALIZED) {
        ret = "EGL is not initialized, or could not be initialized, for the specified EGL display connection.";
    } else if (err == EGL_BAD_ACCESS) {
        ret = "EGL cannot access a requested resource (for example a context is bound in another thread).";
    } else if (err == EGL_BAD_ALLOC) {
        ret = "EGL failed to allocate resources for the requested operation.";
    } else if (err == EGL_BAD_ATTRIBUTE) {
        ret = "An unrecognized attribute or attribute value was passed in the attribute list.";
    } else if (err == EGL_BAD_CONTEXT) {
        ret = "An EGLContext argument does not name a valid EGL rendering context.";
    } else if (err == EGL_BAD_CONFIG) {
        ret = "An EGLConfig argument does not name a valid EGL frame buffer configuration.";
    } else if (err == EGL_BAD_CURRENT_SURFACE) {
        ret = "The current surface of the calling thread is a window, pixel buffer or pixmap that is no longer valid.";
    } else if (err == EGL_BAD_DISPLAY) {
        ret = "An EGLDisplay argument does not name a valid EGL display connection.";
    } else if (err == EGL_BAD_SURFACE) {
        ret = "An EGLSurface argument does not name a valid surface (window, pixel buffer or pixmap) configured for GL rendering.";
    } else if (err == EGL_BAD_MATCH) {
        ret = "Arguments are inconsistent (for example, a valid context requires buffers not supplied by a valid surface).";
    } else if (err == EGL_BAD_PARAMETER) {
        ret = "One or more argument values are invalid.";
    } else if (err == EGL_BAD_NATIVE_PIXMAP) {
        ret = "A NativePixmapType argument does not refer to a valid native pixmap.";
    } else if (err == EGL_BAD_NATIVE_WINDOW) {
        ret = "A NativeWindowType argument does not refer to a valid native window.";
    } else {
        sprintf(buffer, "unknown error code 0x%0x", err);
        ret = buffer;
    }
    if (message) {
        printf("%s\n", message);
    }
    printf("EGL ERROR: %s\n", ret);
    exit(1);
}

int printConfigAttrs(EGLint *config) {
    int cnt = 0;
    while ((*config != EGL_NONE) && (cnt < 25)) {
        EGLint arg = *config++;
        EGLint val = *config++;
        cnt++;
        printf("    ");
        switch (arg) {
            case EGL_SURFACE_TYPE:
                if (val == (EGL_PBUFFER_BIT | EGL_WINDOW_BIT)) {
                    printf("EGL_SURFACE_TYPE, EGL_PBUFFER_BIT | EGL_WINDOW_BIT,\n");
                } else if (val == (EGL_WINDOW_BIT)) {
                    printf("EGL_SURFACE_TYPE: EGL_WINDOW_BIT,\n");
                } else if (val == (EGL_PBUFFER_BIT)) {
                    printf("EGL_SURFACE_TYPE: EGL_PBUFFER_BIT,\n");
                } else {
                    printf("EGL_SURFACE_TYPE, %d,\n", val);
                }
                break;
            case EGL_BUFFER_SIZE:
                printf("EGL_BUFFER_SIZE, %d,\n", val);
                break;
            case EGL_SAMPLE_BUFFERS:
                printf("EGL_SAMPLE_BUFFERS, %d,\n", val);
                break;
            case EGL_SAMPLES:
                printf("EGL_SAMPLES, %d,\n", val);
                break;
            case EGL_DEPTH_SIZE:
                printf("EGL_DEPTH_SIZE, %d,\n", val);
                break;
            case EGL_RED_SIZE:
                printf("EGL_RED_SIZE, %d,\n", val);
                break;
            case EGL_GREEN_SIZE:
                printf("EGL_GREEN_SIZE, %d,\n", val);
                break;
            case EGL_BLUE_SIZE:
                printf("EGL_BLUE_SIZE, %d,\n", val);
                break;
            case EGL_ALPHA_SIZE:
                printf("EGL_ALPHA_SIZE, %d,\n", val);
                break;
            case EGL_LEVEL:
                printf("EGL_LEVEL, %d,\n", val);
                break;
            case EGL_NATIVE_RENDERABLE:
                printf("EGL_NATIVE_RENDERABLE, %d,\n", val);
                break;
            case EGL_STENCIL_SIZE:
                printf("EGL_STENCIL_SIZE, %d,\n", val);
                break;
            case EGL_TRANSPARENT_TYPE:
                if (val == EGL_TRANSPARENT_RGB) {
                    printf("EGL_TRANSPARENT_TYPE, EGL_TRANSPARENT_RGB,\n");
                } else if (val == EGL_NONE) {
                    printf("EGL_TRANSPARENT_TYPE, EGL_NONE,\n");
                } else {
                    printf("EGL_TRANSPARENT_TYPE, bad val %d\n", val);
                }
                break;
            case EGL_TRANSPARENT_RED_VALUE:
                printf("EGL_TRANSPARENT_RED_VALUE, %d,\n", val);
                break;
            case EGL_TRANSPARENT_GREEN_VALUE:
                printf("EGL_TRANSPARENT_GREEN_VALUE, %d,\n", val);
                break;
            case EGL_TRANSPARENT_BLUE_VALUE:
                printf("EGL_TRANSPARENT_BLUE_VALUE, %d,\n", val);
                break;
            case EGL_NATIVE_VISUAL_TYPE:
                printf("EGL_NATIVE_VISUAL_TYPE, %d,\n", val);
                break;
            case EGL_RENDERABLE_TYPE:
                printf("EGL_RENDERABLE_TYPE, %s,\n", val == EGL_OPENGL_ES2_BIT ? "EGL_OPENGL_ES2_BIT," : "EGL_OPENGL_ES_BIT");
                break;
            default:
                printf("UNRECOGNIZED, %d, %d\n", arg, val);
        }
    }
    if (*config == EGL_NONE) {
        printf("    EGL_NONE\n");
    } else {
        printf("    *** ERROR exceeded arg limit *** \n");
    }
    return 1;
}

int printConfig(EGLDisplay display, EGLConfig config) {

    int id;
    eglGetConfigAttrib(display, config, EGL_CONFIG_ID, &id);

    int red, green, blue, alpha, depth;
    eglGetConfigAttrib(display, config, EGL_RED_SIZE, &red);
    eglGetConfigAttrib(display, config, EGL_GREEN_SIZE, &green);
    eglGetConfigAttrib(display, config, EGL_BLUE_SIZE, &blue);
    eglGetConfigAttrib(display, config, EGL_ALPHA_SIZE, &alpha);
    eglGetConfigAttrib(display, config, EGL_BUFFER_SIZE, &depth);

    int pwidth, phgt, psize;
    pwidth = phgt = psize =  0;
    eglGetConfigAttrib(display, config, EGL_MAX_PBUFFER_WIDTH, &pwidth);
    eglGetConfigAttrib(display, config, EGL_MAX_PBUFFER_HEIGHT, &phgt);
    eglGetConfigAttrib(display, config, EGL_MAX_PBUFFER_PIXELS, &psize);

    int sbuffers, samples;
    eglGetConfigAttrib(display, config, EGL_SAMPLE_BUFFERS, &sbuffers);
    eglGetConfigAttrib(display, config, EGL_SAMPLES, &samples);

    int stencil;
    eglGetConfigAttrib(display, config, EGL_STENCIL_SIZE, &stencil);

    int surface;
    eglGetConfigAttrib(display, config, EGL_SURFACE_TYPE, &surface);

    int transparent;
    eglGetConfigAttrib(display, config, EGL_TRANSPARENT_TYPE, &transparent);

    int caveat;
    eglGetConfigAttrib(display, config, EGL_CONFIG_CAVEAT, &caveat);
    char *strcaveat = "Normal";
    if (caveat == EGL_SLOW_CONFIG) {
        strcaveat = "Slow";
    } else if (caveat == EGL_NON_CONFORMANT_CONFIG) {
        strcaveat = "NonConf";
    }

    // humm, not documented as a supported element, but there all the same ?
    int rtype = -1;
    if (!eglGetConfigAttrib(display, config, EGL_RENDERABLE_TYPE, &rtype)) {
        printf("failed to get EGL_RENDERABLE_TYPE\n");
    }
    char rstr[5];
    char *rstrptr = rstr;
    if ((rtype & EGL_OPENGL_ES_BIT) == EGL_OPENGL_ES_BIT) {
        *(rstrptr++) = '1';
    }
    if ((rtype & EGL_OPENGL_ES2_BIT) == EGL_OPENGL_ES2_BIT) {
        *(rstrptr++) = '2';
    }
    if ((rtype & EGL_OPENVG_BIT) == EGL_OPENVG_BIT) {
        *(rstrptr++) = 'V';
    }
    if ((rtype & EGL_OPENGL_BIT) == EGL_OPENGL_BIT) {
        *(rstrptr++) = 'G';
    }
    *rstrptr = 0;

    printf("  %02d: %d%d%d%d %02d %04dx%04d %d %d,%d %d %s%s%s %s %s %s\n", id,
           red, green, blue, alpha, depth,
           pwidth, phgt, psize,
           sbuffers, samples,
           stencil,
           ((surface & EGL_WINDOW_BIT) == EGL_WINDOW_BIT) ? "W" : "_",
           ((surface & EGL_PBUFFER_BIT) == EGL_PBUFFER_BIT) ? "P" : "_",
           ((surface & EGL_PIXMAP_BIT) == EGL_PIXMAP_BIT) ? "X" : "_",
           (transparent == EGL_TRANSPARENT_RGB) ? "Trans" : "Opaqe",
           strcaveat,
           rstr
          );

    return 1;
}

//#endif // DEBUG
