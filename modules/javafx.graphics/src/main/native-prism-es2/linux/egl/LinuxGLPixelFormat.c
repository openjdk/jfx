/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>
#include <stdlib.h>
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

#include "../PrismES2Defs.h"
#include "com_sun_prism_es2_LinuxGLPixelFormat.h"

extern void setEGLAttrs(jint *attrs, int *eglAttrs);
extern void printAndReleaseResources(EGLDisplay eglDisplay, EGLSurface eglSurface, EGLContext eglContext,
                                    const char *message);

static EGLDisplay getPlatformDisplay(void * display) {
    EGLDisplay eglDisplay = NULL;

    const char* extensions = eglQueryString(NULL, EGL_EXTENSIONS);

    if (strstr(extensions, "EGL_KHR_platform_base") != NULL) {
        PFNEGLGETPLATFORMDISPLAYPROC getPlatformDisplay = (void *) eglGetProcAddress("eglGetPlatformDisplay");

        if (getPlatformDisplay != NULL) {
            eglDisplay = getPlatformDisplay(EGL_PLATFORM_X11_KHR, display, NULL);

            if (eglDisplay != EGL_NO_DISPLAY) {
                goto out;
            }
        }
    }

    if (strstr(extensions, "EGL_EXT_platform_base") != NULL) {
        PFNEGLGETPLATFORMDISPLAYEXTPROC getPlatformDisplay =  (void *) eglGetProcAddress("eglGetPlatformDisplayEXT");

        if (getPlatformDisplay != NULL) {
            eglDisplay = getPlatformDisplay(EGL_PLATFORM_X11_EXT, display, NULL);

            if (eglDisplay != EGL_NO_DISPLAY) {
                goto out;
            }
        }
    }

    eglDisplay = eglGetDisplay((EGLNativeDisplayType) display);

out:
    return eglDisplay;
}
/*
 * Class:     com_sun_prism_es2_LinuxGLPixelFormat
 * Method:    nCreatePixelFormat
 * Signature: (J[I)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_LinuxGLPixelFormat_nCreatePixelFormat
(JNIEnv *env, jclass class, jlong nativeScreen, jintArray attrArr) {
    int eglAttrs[MAX_GL_ATTRS_LENGTH]; /* value, attr pair plus a None */
    jint *attrs;
    PixelFormatInfo *pfInfo = NULL;

    EGLConfig eglConfig;
    int num_configs;
    Display *display;
    int screen;
    Window root;
    Window win = None;

    if (attrArr == NULL) {
        return 0;
    }
    attrs = (*env)->GetIntArrayElements(env, attrArr, NULL);
    setEGLAttrs(attrs, eglAttrs);
    (*env)->ReleaseIntArrayElements(env, attrArr, attrs, JNI_ABORT);

    // RT-27386
    // TODO: Need to use nativeScreen to create this requested pixelformat
    // currently hack to work on a single monitor system
    display = XOpenDisplay(0);
    if (display == NULL) {
        fprintf(stderr, "Prism ES2 Error: XOpenDisplay failed\n");
        return 0;
    }

    screen = DefaultScreen(display);

    EGLDisplay eglDisplay = getPlatformDisplay(display);

    if (eglDisplay == EGL_NO_DISPLAY) {
        fprintf(stderr, "Prism ES2 Error: CreatePixelFormat - no supported display found\n");
        return 0;
    }

    if (!eglInitialize(eglDisplay, NULL, NULL)) {
        fprintf(stderr, "Prism ES2 Error: CreatePixelFormat - eglInitialize failed.\n");
        return 0;
    }

    if (!eglBindAPI(EGL_OPENGL_API)) {
        fprintf(stderr, "Prism ES2 Error: CreatePixelFormat - cannot bind EGL_OPENGL_API.\n");
        return 0;
    }

    if ((eglGetConfigs(eglDisplay, NULL, 0, &num_configs) != EGL_TRUE) || (num_configs == 0)) {
        fprintf(stderr, "Prism ES2 Error: CreatePixelFormat - no EGL configuration available\n");
        return 0;
    }

    if (eglChooseConfig(eglDisplay, eglAttrs, &eglConfig, 1, &num_configs) != EGL_TRUE) {
        fprintf(stderr, "Prism ES2 Error: CreatePixelFormat - eglChooseConfig failed\n");
        return 0;
    }

    root = RootWindow(display, screen);

    win = XCreateSimpleWindow(display, root, 0, 0, 1, 1, 0,
                              WhitePixel(display, screen),
                              WhitePixel(display, screen));

    if (win == None) {
        printAndReleaseResources(eglDisplay, NULL, NULL, "Prism ES2 Error: XCreateWindow failed");
        return 0;
    }

    /* allocate the structure */
    pfInfo = (PixelFormatInfo *) malloc(sizeof (PixelFormatInfo));
    if (pfInfo == NULL) {
        fprintf(stderr, "Prism ES2 Error: CreatePixelFormat - Failed in malloc\n");
        return 0;
    }

    /* initialize the structure */
    initializePixelFormatInfo(pfInfo);
    pfInfo->display = display;
    pfInfo->dummyWin = win;
    pfInfo->eglConfig = eglConfig;
    pfInfo->eglDisplay = eglDisplay;

    return ptr_to_jlong(pfInfo);
}
