/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "com_sun_prism_es2_WinGLFactory.h"

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL JNI_OnLoad_prism_es2(JavaVM *vm, void * reserved) {
#ifdef JNI_VERSION_1_8
    //min. returned JNI_VERSION required by JDK8 for builtin libraries
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_8) != JNI_OK) {
        return JNI_VERSION_1_4;
    }
    return JNI_VERSION_1_8;
#else
    return JNI_VERSION_1_4;
#endif // JNI_VERSION_1_8
}
#endif // STATIC_BUILD

PIXELFORMATDESCRIPTOR getPFD(jint* attrArr) {

    static PIXELFORMATDESCRIPTOR pfd = {
        sizeof (PIXELFORMATDESCRIPTOR),
        1, /* Version number */
        PFD_SUPPORT_OPENGL,
        PFD_TYPE_RGBA,
        24, /* 24 bit color depth */
        0, 0, 0, /* RGB bits and pixel sizes */
        0, 0, 0, /* Do not care about them */
        0, 0, /* no alpha buffer info */
        0, 0, 0, 0, 0, /* no accumulation buffer */
        24, /* 24 bit depth buffer */
        0, /* no stencil buffer */
        0, /* no auxiliary buffers */
        PFD_MAIN_PLANE, /* layer type */
        0, /* reserved, must be 0 */
        0, /* no layer mask */
        0, /* no visible mask */
        0 /* no damage mask */
    };

    if (attrArr[ONSCREEN] != 0) {
        pfd.dwFlags |= PFD_DRAW_TO_WINDOW;
    }
    if (attrArr[DOUBLEBUFFER] != 0) {
        pfd.dwFlags |= PFD_DOUBLEBUFFER;
    }
    pfd.cDepthBits = (BYTE) attrArr[DEPTH_SIZE];
    pfd.cColorBits = (BYTE) (attrArr[RED_SIZE] + attrArr[GREEN_SIZE]
            + attrArr[BLUE_SIZE] + attrArr[ALPHA_SIZE]);
    pfd.cRedBits = (BYTE) attrArr[RED_SIZE];
    pfd.cGreenBits = (BYTE) attrArr[GREEN_SIZE];
    pfd.cBlueBits = (BYTE) attrArr[BLUE_SIZE];
    pfd.cAlphaBits = (BYTE) attrArr[ALPHA_SIZE];

    return pfd;
}

LONG WINAPI WndProc(HWND hWnd, UINT msg,
        WPARAM wParam, LPARAM lParam) {

    /* This function handles any messages that we didn't. */
    /* (Which is most messages) It belongs to the OS. */
    return (LONG) DefWindowProc(hWnd, msg, wParam, lParam);
}

HWND createDummyWindow(LPCTSTR szAppName) {
    static LPCTSTR szTitle = L"Dummy Window";
    WNDCLASS wc; /* windows class structure */

    HWND hWnd;

    /* Fill in window class structure with parameters that */
    /*  describe the main window. */
    wc.style = CS_HREDRAW | CS_VREDRAW; /* Class style(s). */
    wc.lpfnWndProc = (WNDPROC) WndProc; /* Window Procedure */
    wc.cbClsExtra = 0; /* No per-class extra data. */
    wc.cbWndExtra = 0; /* No per-window extra data. */
    wc.hInstance = NULL; /* Owner of this class */
    wc.hIcon = NULL; /* Icon name */
    wc.hCursor = NULL; /* Cursor */
    wc.hbrBackground = (HBRUSH) (COLOR_WINDOW + 1); /* Default color */
    wc.lpszMenuName = NULL; /* Menu from .RC */
    wc.lpszClassName = szAppName; /* Name to register as */

    /* Register the window class */
    if (RegisterClass(&wc) == 0) {
        fprintf(stderr, "createDummyWindow: couldn't register class\n");
        return NULL;
    }

    /* Create a main window for this application instance. */
    hWnd = CreateWindow(
            szAppName, /* app name */
            szTitle, /* Text for window title bar */
            WS_OVERLAPPEDWINDOW/* Window style */
            /* NEED THESE for OpenGL calls to work!*/
            | WS_CLIPCHILDREN | WS_CLIPSIBLINGS,
            0, 0, 1, 1, /* x, y, width, height */
            NULL, /* no parent window */
            NULL, /* Use the window class menu.*/
            NULL, /* This instance owns this window */
            NULL /* We don't use any extra data */
            );

    /* If window could not be created, return zero */
    if (!hWnd) {
        fprintf(stderr, "createDummyWindow: couldn't create window\n");
        UnregisterClass(szAppName, (HINSTANCE) NULL);
        return NULL;
    }
    return hWnd;
}

void printAndReleaseResources(HWND hwnd, HGLRC hglrc, HDC hdc,
        LPCTSTR szAppName, char *message) {
    if (message != NULL) {
        fprintf(stderr, "%s\n", message);
    }
    wglMakeCurrent(NULL, NULL);
    if (hglrc != NULL) {
        wglDeleteContext(hglrc);
    }
    if ((hdc != NULL) && (hwnd != NULL)) {
        ReleaseDC(hwnd, hdc);
    }
    if (hdc != NULL) {
        DeleteObject(hdc);
    }
    if (hwnd != NULL) {
        DestroyWindow(hwnd);
        UnregisterClass(szAppName, (HINSTANCE) NULL);
    }
}

/*
 * Class:     com_sun_prism_es2_WinGLFactory
 * Method:    nInitialize
 * Signature: ([I[J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_WinGLFactory_nInitialize
(JNIEnv *env, jclass class, jintArray attrArr) {
    static LPCTSTR szAppName = L"Choose Pixel Format";
    HWND hwnd = NULL;
    HGLRC hglrc = NULL;
    HDC hdc = NULL;
    int pixelFormat;
    PIXELFORMATDESCRIPTOR pfd;
    jint *attrs;

    ContextInfo *ctxInfo = NULL;
    const char *glVersion;
    const char *glVendor;
    const char *glRenderer;
    char *tmpVersionStr;
    int versionNumbers[2];
    const char *glExtensions;
    const char *wglExtensions;
    PFNWGLGETEXTENSIONSSTRINGARBPROC wglGetExtensionsStringARB = NULL;

    if (attrArr == NULL) {
        return 0;
    }
    attrs = (*env)->GetIntArrayElements(env, attrArr, NULL);
    pfd = getPFD(attrs);
    (*env)->ReleaseIntArrayElements(env, attrArr, attrs, JNI_ABORT);

    /*
     * Select a specified pixel format and bound current context to
     * it so that we can get the wglChoosePixelFormatARB entry point.
     * Otherwise wglxxx entry point will always return null.
     * That's why we need to create a dummy window also.
     */
    hwnd = createDummyWindow(szAppName);

    if (!hwnd) {
        return 0;
    }

    hdc = GetDC(hwnd);
    if (hdc == NULL) {
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "Failed in GetDC");
        return 0;
    }

    pixelFormat = ChoosePixelFormat(hdc, &pfd);
    if (pixelFormat < 1) {
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "Failed in ChoosePixelFormat");
        return 0;
    }

    if (!SetPixelFormat(hdc, pixelFormat, NULL)) {
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "Failed in SetPixelFormat");
        return 0;
    }

    hglrc = wglCreateContext(hdc);
    if (hglrc == NULL) {
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "Failed in wglCreateContext");
        return 0;
    }

    if (!wglMakeCurrent(hdc, hglrc)) {
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "Failed in wglMakeCurrent");
        return 0;
    }

    /* Get the OpenGL version */
    glVersion = (const char *) glGetString(GL_VERSION);
    if (glVersion == NULL) {
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "glVersion == null");
        return 0;
    }

    /* find out the version, major and minor version number */
    tmpVersionStr = _strdup(glVersion);
    extractVersionInfo(tmpVersionStr, versionNumbers);
    free(tmpVersionStr);

    /*
        fprintf(stderr, "GL_VERSION string = %s\n", glVersion);
        fprintf(stderr, "GL_VERSION (major.minor) = %d.%d\n",
                versionNumbers[0], versionNumbers[1]);
     */

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
        return 0;
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
        return 0;
    }

    // We use GL_ARB_pixel_buffer_object as an guide to
    // determine PS 3.0 capable.
    if (!isExtensionSupported(glExtensions, "GL_ARB_pixel_buffer_object")) {
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "GL profile isn't PS 3.0 capable");
        return 0;
    }

    wglGetExtensionsStringARB = (PFNWGLGETEXTENSIONSSTRINGARBPROC)
            wglGetProcAddress("wglGetExtensionsStringARB");
    if (wglGetExtensionsStringARB == NULL) {
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "wglGetExtensionsStringARB is not supported!");
        return 0;
    }
    wglExtensions = (char *) wglGetExtensionsStringARB(hdc);
    if (wglExtensions == NULL) {
        printAndReleaseResources(hwnd, hglrc, hdc, szAppName,
                "wglExtensions == null");
        return 0;
    }

    /* Note: We are only storing the string information of a driver.
     Assuming a system with a single or homogeneous GPUs. For the case
     of heterogeneous GPUs system the string information will need to move to
     GLContext class. */
    /* allocate the structure */
    ctxInfo = (ContextInfo *) malloc(sizeof (ContextInfo));
    if (ctxInfo == NULL) {
        fprintf(stderr, "nInitialize: Failed in malloc\n");
        return 0;
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
    ctxInfo->gl2 = JNI_TRUE;

    printAndReleaseResources(hwnd, hglrc, hdc, szAppName, NULL);
    return ptr_to_jlong(ctxInfo);
}

/*
 * Class:     com_sun_prism_es2_WinGLFactory
 * Method:    nGetAdapterOrdinal
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_WinGLFactory_nGetAdapterOrdinal
(JNIEnv *env, jclass class, jlong hMonitor) {
    //TODO: Needs implementation to handle multi-monitors (RT-27437)
    return 0;
}

/*
 * Class:     com_sun_prism_es2_WinGLFactory
 * Method:    nGetAdapterCount
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_WinGLFactory_nGetAdapterCount
(JNIEnv *env, jclass class) {
    //TODO: Needs implementation to handle multi-monitors (RT-27437)
    return 1;
}

/*
 * Class:     com_sun_prism_es2_WinGLFactory
 * Method:    nGetIsGL2
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_es2_WinGLFactory_nGetIsGL2
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    return ((ContextInfo *)jlong_to_ptr(nativeCtxInfo))->gl2;
}
