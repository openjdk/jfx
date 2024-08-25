/*
 * Copyright (c) 2024 Oracle and/or its affiliates. All rights reserved.
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
#include "com_sun_prism_es2_LinuxGLContext.h"

extern const char* eglGetErrorString(EGLint error);

/*
 * Class:     com_sun_prism_es2_LinuxGLContext
 * Method:    nInitialize
 * Signature: (JJZ)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_LinuxGLContext_nInitialize
(JNIEnv *env, jclass class, jlong nativeDInfo, jlong nativePFInfo,
        jboolean vSyncRequested) {
    const char *glVersion;
    const char *glVendor;
    const char *glRenderer;
    char *tmpVersionStr;
    int versionNumbers[2];
    const char *glExtensions;
    const char *eglExtensions;

    PixelFormatInfo *pfInfo = (PixelFormatInfo *) jlong_to_ptr(nativePFInfo);
    EGLDisplay eglDisplay = pfInfo->eglDisplay;

    ContextInfo *ctxInfo = NULL;
    EGLContext eglContext = eglCreateContext(eglDisplay, pfInfo->eglConfig, EGL_NO_CONTEXT, NULL);

    if (eglContext == EGL_NO_CONTEXT) {
        fprintf(stderr, "Prism ES2 Error: Initialize - eglCreateContext failed [%s]\n", eglGetErrorString(eglGetError()));
        return 0;
    }

    if (!eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, eglContext)) {
        fprintf(stderr, "Prism ES2 Error: Initialize - eglMakeCurrent failed [%s]\n",  eglGetErrorString(eglGetError()));
        eglDestroyContext(eglDisplay, eglContext);
        return 0;
    }

    /* Get the OpenGL version */
    glVersion = (char *) glGetString(GL_VERSION);
    if (glVersion == NULL) {
        eglDestroyContext(eglDisplay, eglContext);
        fprintf(stderr, "glVersion == null\n");
        return 0;
    }

    /* find out the version, major and minor version number */
    tmpVersionStr = strdup(glVersion);
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
        eglDestroyContext(eglDisplay, eglContext);
        fprintf(stderr, "Prism-ES2 Error : GL_VERSION (major.minor) = %d.%d\n",
                versionNumbers[0], versionNumbers[1]);
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
        eglDestroyContext(eglDisplay, eglContext);
        fprintf(stderr, "glExtensions == null\n");
        return 0;
    }

    // We use GL_ARB_pixel_buffer_object as an guide to
    // determine PS 3.0 capable.
    if (!isExtensionSupported(glExtensions, "GL_ARB_pixel_buffer_object")) {
        eglDestroyContext(eglDisplay, eglContext);
        fprintf(stderr, "GL profile isn't PS 3.0 capable\n");
        return 0;
    }

    eglExtensions = (const char *) eglQueryString(eglDisplay, EGL_EXTENSIONS);
    if (eglExtensions == NULL) {
        eglDestroyContext(eglDisplay, eglContext);
        fprintf(stderr, "eglExtensions == null\n");
        return 0;
    }

    /*
        fprintf(stderr, "glExtensions: %s\n", glExtensions);
        fprintf(stderr, "glxExtensions: %s\n", glxExtensions);
    */

    /* allocate the structure */
    ctxInfo = (ContextInfo *) malloc(sizeof (ContextInfo));
    if (ctxInfo == NULL) {
        fprintf(stderr, "Prism ES2 Error: Initialize - Failed in malloc\n");
        return 0;
    }

    /* initialize the structure */
    initializeCtxInfo(ctxInfo);
    ctxInfo->versionStr = strdup(glVersion);
    ctxInfo->vendorStr = strdup(glVendor);
    ctxInfo->rendererStr = strdup(glRenderer);
    ctxInfo->glExtensionStr = strdup(glExtensions);
    ctxInfo->eglExtensionStr = strdup(eglExtensions);
    ctxInfo->versionNumbers[0] = versionNumbers[0];
    ctxInfo->versionNumbers[1] = versionNumbers[1];
    ctxInfo->context = eglContext;

    /* set function pointers */
    ctxInfo->glActiveTexture = (PFNGLACTIVETEXTUREPROC)
            eglGetProcAddress("glActiveTexture");
    ctxInfo->glAttachShader = (PFNGLATTACHSHADERPROC)
            eglGetProcAddress("glAttachShader");
    ctxInfo->glBindAttribLocation = (PFNGLBINDATTRIBLOCATIONPROC)
            eglGetProcAddress("glBindAttribLocation");
    ctxInfo->glBindFramebuffer = (PFNGLBINDFRAMEBUFFERPROC)
            eglGetProcAddress("glBindFramebuffer");
    ctxInfo->glBindRenderbuffer = (PFNGLBINDRENDERBUFFERPROC)
            eglGetProcAddress("glBindRenderbuffer");
    ctxInfo->glCheckFramebufferStatus = (PFNGLCHECKFRAMEBUFFERSTATUSPROC)
            eglGetProcAddress("glCheckFramebufferStatus");
    ctxInfo->glCreateProgram = (PFNGLCREATEPROGRAMPROC)
            eglGetProcAddress("glCreateProgram");
    ctxInfo->glCreateShader = (PFNGLCREATESHADERPROC)
            eglGetProcAddress("glCreateShader");
    ctxInfo->glCompileShader = (PFNGLCOMPILESHADERPROC)
            eglGetProcAddress("glCompileShader");
    ctxInfo->glDeleteBuffers = (PFNGLDELETEBUFFERSPROC)
            eglGetProcAddress("glDeleteBuffers");
    ctxInfo->glDeleteFramebuffers = (PFNGLDELETEFRAMEBUFFERSPROC)
            eglGetProcAddress("glDeleteFramebuffers");
    ctxInfo->glDeleteProgram = (PFNGLDELETEPROGRAMPROC)
            eglGetProcAddress("glDeleteProgram");
    ctxInfo->glDeleteRenderbuffers = (PFNGLDELETERENDERBUFFERSPROC)
            eglGetProcAddress("glDeleteRenderbuffers");
    ctxInfo->glDeleteShader = (PFNGLDELETESHADERPROC)
            eglGetProcAddress("glDeleteShader");
    ctxInfo->glDetachShader = (PFNGLDETACHSHADERPROC)
            eglGetProcAddress("glDetachShader");
    ctxInfo->glDisableVertexAttribArray = (PFNGLDISABLEVERTEXATTRIBARRAYPROC)
            eglGetProcAddress("glDisableVertexAttribArray");
    ctxInfo->glEnableVertexAttribArray = (PFNGLENABLEVERTEXATTRIBARRAYPROC)
            eglGetProcAddress("glEnableVertexAttribArray");
    ctxInfo->glFramebufferRenderbuffer = (PFNGLFRAMEBUFFERRENDERBUFFERPROC)
            eglGetProcAddress("glFramebufferRenderbuffer");
    ctxInfo->glFramebufferTexture2D = (PFNGLFRAMEBUFFERTEXTURE2DPROC)
            eglGetProcAddress("glFramebufferTexture2D");
    ctxInfo->glGenFramebuffers = (PFNGLGENFRAMEBUFFERSPROC)
            eglGetProcAddress("glGenFramebuffers");
    ctxInfo->glGenRenderbuffers = (PFNGLGENRENDERBUFFERSPROC)
            eglGetProcAddress("glGenRenderbuffers");
    ctxInfo->glGetProgramiv = (PFNGLGETPROGRAMIVPROC)
            eglGetProcAddress("glGetProgramiv");
    ctxInfo->glGetShaderiv = (PFNGLGETSHADERIVPROC)
            eglGetProcAddress("glGetShaderiv");
    ctxInfo->glGetUniformLocation = (PFNGLGETUNIFORMLOCATIONPROC)
            eglGetProcAddress("glGetUniformLocation");
    ctxInfo->glLinkProgram = (PFNGLLINKPROGRAMPROC)
            eglGetProcAddress("glLinkProgram");
    ctxInfo->glRenderbufferStorage = (PFNGLRENDERBUFFERSTORAGEPROC)
            eglGetProcAddress("glRenderbufferStorage");
    ctxInfo->glShaderSource = (PFNGLSHADERSOURCEPROC)
            eglGetProcAddress("glShaderSource");
    ctxInfo->glUniform1f = (PFNGLUNIFORM1FPROC)
            eglGetProcAddress("glUniform1f");
    ctxInfo->glUniform2f = (PFNGLUNIFORM2FPROC)
            eglGetProcAddress("glUniform2f");
    ctxInfo->glUniform3f = (PFNGLUNIFORM3FPROC)
            eglGetProcAddress("glUniform3f");
    ctxInfo->glUniform4f = (PFNGLUNIFORM4FPROC)
            eglGetProcAddress("glUniform4f");
    ctxInfo->glUniform4fv = (PFNGLUNIFORM4FVPROC)
            eglGetProcAddress("glUniform4fv");
    ctxInfo->glUniform1i = (PFNGLUNIFORM1IPROC)
            eglGetProcAddress("glUniform1i");
    ctxInfo->glUniform2i = (PFNGLUNIFORM2IPROC)
            eglGetProcAddress("glUniform2i");
    ctxInfo->glUniform3i = (PFNGLUNIFORM3IPROC)
            eglGetProcAddress("glUniform3i");
    ctxInfo->glUniform4i = (PFNGLUNIFORM4IPROC)
            eglGetProcAddress("glUniform4i");
    ctxInfo->glUniform4iv = (PFNGLUNIFORM4IVPROC)
            eglGetProcAddress("glUniform4iv");
    ctxInfo->glUniformMatrix4fv = (PFNGLUNIFORMMATRIX4FVPROC)
            eglGetProcAddress("glUniformMatrix4fv");
    ctxInfo->glUseProgram = (PFNGLUSEPROGRAMPROC)
            eglGetProcAddress("glUseProgram");
    ctxInfo->glValidateProgram = (PFNGLVALIDATEPROGRAMPROC)
            eglGetProcAddress("glValidateProgram");
    ctxInfo->glVertexAttribPointer = (PFNGLVERTEXATTRIBPOINTERPROC)
            eglGetProcAddress("glVertexAttribPointer");
    ctxInfo->glGenBuffers = (PFNGLGENBUFFERSPROC)
            eglGetProcAddress("glGenBuffers");
    ctxInfo->glBindBuffer = (PFNGLBINDBUFFERPROC)
            eglGetProcAddress("glBindBuffer");
    ctxInfo->glBufferData = (PFNGLBUFFERDATAPROC)
            eglGetProcAddress("glBufferData");
    ctxInfo->glBufferSubData = (PFNGLBUFFERSUBDATAPROC)
            eglGetProcAddress("glBufferSubData");
    ctxInfo->glGetShaderInfoLog = (PFNGLGETSHADERINFOLOGPROC)
            eglGetProcAddress("glGetShaderInfoLog");
    ctxInfo->glGetProgramInfoLog = (PFNGLGETPROGRAMINFOLOGPROC)
            eglGetProcAddress("glGetProgramInfoLog");
    ctxInfo->glTexImage2DMultisample = (PFNGLTEXIMAGE2DMULTISAMPLEPROC)
            eglGetProcAddress("glTexImage2DMultisample");
    ctxInfo->glRenderbufferStorageMultisample = (PFNGLRENDERBUFFERSTORAGEMULTISAMPLEPROC)
            eglGetProcAddress("glRenderbufferStorageMultisample");
    ctxInfo->glBlitFramebuffer = (PFNGLBLITFRAMEBUFFERPROC)
            eglGetProcAddress("glBlitFramebuffer");

    ctxInfo->state.vSyncEnabled = JNI_FALSE;
    ctxInfo->vSyncRequested = vSyncRequested;
    ctxInfo->eglDisplay = eglDisplay;
    ctxInfo->display = pfInfo->display;

    initState(ctxInfo);

    // Release context once we are all done
    if (!eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)) {
        fprintf(stderr, "Prism ES2 Error: Initialize - eglMakeCurrent failed [%s]\n", eglGetErrorString(eglGetError()));
        eglDestroyContext(eglDisplay, eglContext);
        return 0;
    }

    return ptr_to_jlong(ctxInfo);
}

/*
 * Class:     com_sun_prism_es2_LinuxGLContext
 * Method:    nGetNativeHandle
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_LinuxGLContext_nGetNativeHandle
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return 0;
    }

    return ptr_to_jlong(ctxInfo->context);
}

/*
 * Class:     com_sun_prism_es2_LinuxGLContext
 * Method:    nMakeCurrent
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_LinuxGLContext_nMakeCurrent
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativeDInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    DrawableInfo *dInfo = (DrawableInfo *) jlong_to_ptr(nativeDInfo);
    int interval;
    jboolean vSyncNeeded;

    if (dInfo == NULL || ctxInfo == NULL) {
        return;
    }

    if (!eglMakeCurrent(ctxInfo->eglDisplay, dInfo->eglSurface, dInfo->eglSurface, ctxInfo->context)) {
        fprintf(stderr, "Prism ES2 Error: MakeCurrent - eglMakeCurrent failed [%s]\n", eglGetErrorString(eglGetError()));
        return;
    }

    vSyncNeeded = ctxInfo->vSyncRequested && dInfo->onScreen;
    interval = (vSyncNeeded) ? 1 : 0;
    if (vSyncNeeded == ctxInfo->state.vSyncEnabled) {
        return;
    }
    ctxInfo->state.vSyncEnabled = vSyncNeeded;

    if (dInfo->eglSurface != EGL_NO_SURFACE && !eglSwapInterval(ctxInfo->eglDisplay, interval)) {
        fprintf(stderr, "Prism ES2 Error: MakeCurrent - eglSwapInterval failed [%s]\n", eglGetErrorString(eglGetError()));
        return;
    }
}
