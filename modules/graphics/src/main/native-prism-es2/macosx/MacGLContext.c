/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include <dlfcn.h>
#include <stdlib.h>
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

#include "../PrismES2Defs.h"
#include "com_sun_prism_es2_MacGLContext.h"

extern void printAndReleaseResources(jlong pf, jlong ctx, const char *message);

/*
 * Class:     com_sun_prism_es2_MacGLContext
 * Method:    nInitialize
 * Signature: (JJJ)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_MacGLContext_nInitialize
(JNIEnv *env, jclass class, jlong nativeDInfo, jlong nativePFInfo,
        jlong nativeShareCtxHandle, jboolean vSyncRequested) {
    const char *glVersion;
    const char *glVendor;
    const char *glRenderer;
    char *tmpVersionStr;
    int versionNumbers[2];
    const char *glExtensions;

    jlong pixelFormat = 0;
    jlong win = 0;
    jlong context = 0;
    int viewNotReady;
    jboolean result;
    ContextInfo *ctxInfo = NULL;
    DrawableInfo *dInfo = (DrawableInfo *) jlong_to_ptr(nativeDInfo);
    PixelFormatInfo *pfInfo = (PixelFormatInfo *) jlong_to_ptr(nativePFInfo);

    if ((dInfo == NULL) || (pfInfo == NULL)) {
        return 0;
    }

    pixelFormat = pfInfo->pixelFormat;
    win = dInfo->win;

    context = (jlong) (intptr_t) createContext((void *) (intptr_t) nativeShareCtxHandle,
            (void *) (intptr_t) win,
            (void *) (intptr_t) pixelFormat, &viewNotReady);

    if (context == 0) {
        fprintf(stderr, "Fail in createContext");
        return 0;
    }

    result = makeCurrentContext((void *) (intptr_t) context);
    if (!result) {
        printAndReleaseResources(0, context,
                "Fail in makeCurrentContext");
        return 0;
    }

    /* Get the OpenGL version */
    glVersion = (char *) glGetString(GL_VERSION);
    if (glVersion == NULL) {
        printAndReleaseResources(0, context, "glVersion == null");
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
     * Supported Cards: Intel HD Graphics, Intel HD Graphics 2000/3000,
     * Radeon HD 2350, GeForce FX (with newer drivers), GeForce 6 series or higher
     *
     * Check for OpenGL 2.0 or later.
     */
    if (versionNumbers[0] < 2) {
        printAndReleaseResources(0, context, NULL);
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
        printAndReleaseResources(0, context, "glExtensions == null");
        return 0;
    }

    // We use GL 2.0 and GL_ARB_pixel_buffer_object as an guide to
    // determine PS 3.0 capable.
    if (!isExtensionSupported(glExtensions, "GL_ARB_pixel_buffer_object")) {
        printAndReleaseResources(0, context, "GL profile isn't PS 3.0 capable");
        return 0;
    }

    /* allocate the structure */
    ctxInfo = (ContextInfo *) malloc(sizeof (ContextInfo));
    if (ctxInfo == NULL) {
        fprintf(stderr, "nInitialize: Failed in malloc\n");
        return 0;
    }

    /* initialize the structure */
    initializeCtxInfo(ctxInfo);
    ctxInfo->versionStr = strdup(glVersion);
    ctxInfo->vendorStr = strdup(glVendor);
    ctxInfo->rendererStr = strdup(glRenderer);
    ctxInfo->glExtensionStr = strdup(glExtensions);
    ctxInfo->versionNumbers[0] = versionNumbers[0];
    ctxInfo->versionNumbers[1] = versionNumbers[1];
    ctxInfo->context = context;

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
    setSwapInterval((void *) jlong_to_ptr(ctxInfo->context), 0);
    ctxInfo->state.vSyncEnabled = JNI_FALSE;
    ctxInfo->vSyncRequested = vSyncRequested;

    initState(ctxInfo);

    // Release context once we are all done
    makeCurrentContext(NULL);

    return ptr_to_jlong(ctxInfo);
}

/*
 * Class:     com_sun_prism_es2_MacGLContext
 * Method:    nGetNativeHandle
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_MacGLContext_nGetNativeHandle
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return 0;
    }
    return ctxInfo->context;
}

/*
 * Class:     com_sun_prism_es2_MacGLContext
 * Method:    nMakeCurrent
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_MacGLContext_nMakeCurrent
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativeDInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    DrawableInfo *dInfo = (DrawableInfo *) jlong_to_ptr(nativeDInfo);
    int interval;
    jboolean vSyncNeeded;

    if ((ctxInfo == NULL) || (dInfo == NULL)) {
        return;
    }

    if (!makeCurrentContext((void *) (intptr_t) ctxInfo->context)) {
        fprintf(stderr, "Failed in makeCurrentContext\n");
    }
    vSyncNeeded = ctxInfo->vSyncRequested && dInfo->onScreen;
    if (vSyncNeeded == ctxInfo->state.vSyncEnabled) {
        return;
    }
    interval = (vSyncNeeded) ? 1 : 0;
    ctxInfo->state.vSyncEnabled = vSyncNeeded;
    setSwapInterval((void *) jlong_to_ptr(ctxInfo->context), interval);
}
