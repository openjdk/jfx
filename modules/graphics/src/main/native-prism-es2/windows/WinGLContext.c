/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
#include "com_sun_prism_es2_WinGLContext.h"

extern void printAndReleaseResources(HWND hwnd, HGLRC hglrc,
        HDC hdc, LPCTSTR szAppName, char *message);
extern HWND createDummyWindow(LPCTSTR szAppName);
extern LONG WINAPI WndProc(HWND hWnd, UINT msg,
        WPARAM wParam, LPARAM lParam);
extern PIXELFORMATDESCRIPTOR getPFD(jint *attrArr);

/*
 * Class:     com_sun_prism_es2_WinGLContext
 * Method:    nInitialize
 * Signature: (JJZ)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_WinGLContext_nInitialize
(JNIEnv *env, jclass class, jlong nativeDInfo, jlong nativePFInfo,
        jboolean vSyncRequested) {
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
    PFNWGLSWAPINTERVALEXTPROC wglSwapIntervalEXT = NULL;

    DrawableInfo *dInfo = (DrawableInfo *) jlong_to_ptr(nativeDInfo);
    PixelFormatInfo *pfInfo = (PixelFormatInfo *) jlong_to_ptr(nativePFInfo);

    if ((dInfo == NULL) || (pfInfo == NULL)) {
        return 0;
    }

    hdc = dInfo->hdc;
    pixelFormat = pfInfo->pixelFormat;

    if (!SetPixelFormat(hdc, pixelFormat, NULL)) {
        fprintf(stderr, "Failed in SetPixelFormat");
        return 0;
    }

    hglrc = wglCreateContext(hdc);
    if (hglrc == NULL) {
        printAndReleaseResources(NULL, hglrc, NULL, NULL,
                "Failed in wglCreateContext");
        return 0;
    }

    if (!wglMakeCurrent(hdc, hglrc)) {
        printAndReleaseResources(NULL, hglrc, NULL, NULL,
                "Failed in wglMakeCurrent");
        return 0;
    }

    /* Get the OpenGL version */
    glVersion = (const char *) glGetString(GL_VERSION);
    if (glVersion == NULL) {
        printAndReleaseResources(NULL, hglrc, NULL, NULL,
                "glVersion == null");
        return 0;
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
        printAndReleaseResources(NULL, hglrc, NULL, NULL,
                "glExtensions == null");
        return 0;
    }

    // We use GL 2.0 and GL_ARB_pixel_buffer_object as an guide to
    // determine PS 3.0 capable.
    if (!isExtensionSupported(glExtensions, "GL_ARB_pixel_buffer_object")) {
        printAndReleaseResources(NULL, hglrc, NULL, NULL,
                "GL profile isn't PS 3.0 capable");
        return 0;
    }

    wglGetExtensionsStringARB = (PFNWGLGETEXTENSIONSSTRINGARBPROC)
            wglGetProcAddress("wglGetExtensionsStringARB");
    if (wglGetExtensionsStringARB == NULL) {
        printAndReleaseResources(NULL, hglrc, NULL, NULL,
                "wglGetExtensionsStringARB is not supported!");
        return 0;
    }
    wglExtensions = (char *) wglGetExtensionsStringARB(hdc);
    if (wglExtensions == NULL) {
        printAndReleaseResources(NULL, hglrc, NULL, NULL,
                "wglExtensions == null");
        return 0;
    }

    /*
        fprintf(stderr, "glExtensions: %s\n", glExtensions);
        fprintf(stderr, "wglExtensions: %s\n", wglExtensions);
     */

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

    if (isExtensionSupported(ctxInfo->wglExtensionStr,
            "WGL_EXT_swap_control")) {
        ctxInfo->wglSwapIntervalEXT = (PFNWGLSWAPINTERVALEXTPROC)
                wglGetProcAddress("wglSwapIntervalEXT");
    }

    // initialize platform states and properties to match
    // cached states and properties
    ctxInfo->wglSwapIntervalEXT(0);
    ctxInfo->state.vSyncEnabled = JNI_FALSE;
    ctxInfo->vSyncRequested = vSyncRequested;

    initState(ctxInfo);

    // Release context once we are all done
    wglMakeCurrent(NULL, NULL);

    return ptr_to_jlong(ctxInfo);
}

/*
 * Class:     com_sun_prism_es2_WinGLContext
 * Method:    nGetNativeHandle
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_WinGLContext_nGetNativeHandle
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return 0;
    }
    return ptr_to_jlong(ctxInfo->hglrc);
}

/*
 * Class:     com_sun_prism_es2_WinGLContext
 * Method:    nMakeCurrent
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_WinGLContext_nMakeCurrent
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativeDInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    DrawableInfo *dInfo = (DrawableInfo *) jlong_to_ptr(nativeDInfo);
    int interval;
    jboolean vSyncNeeded;

    if ((ctxInfo == NULL) || (dInfo == NULL)) {
        return;
    }

    if (!wglMakeCurrent(dInfo->hdc, ctxInfo->hglrc)) {
        fprintf(stderr, "Failed in wglMakeCurrent");
    }

    vSyncNeeded = ctxInfo->vSyncRequested && dInfo->onScreen;
    if (vSyncNeeded == ctxInfo->state.vSyncEnabled) {
        return;
    }
    interval = (vSyncNeeded) ? 1 : 0;
    ctxInfo->state.vSyncEnabled = vSyncNeeded;
    ctxInfo->wglSwapIntervalEXT(interval);
}
