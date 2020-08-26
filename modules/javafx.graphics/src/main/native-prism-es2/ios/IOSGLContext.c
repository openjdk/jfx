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

#include "PrismES2Defs.h"
#include "com_sun_prism_es2_IOSGLContext.h"

extern void printAndReleaseResources(jlong pf, jlong ctx, const char *message);
jboolean pulseLoggingRequested;

jboolean isPulseLoggingRequested(JNIEnv *env) {
    jclass loggerCls = (*env)->FindClass(env, "com/sun/javafx/logging/PulseLogger");
    if ((*env)->ExceptionCheck(env) || loggerCls == NULL) {
        (*env)->ExceptionClear(env);
        return JNI_FALSE;
    }
    jmethodID loggerMID = (*env)->GetStaticMethodID(env, loggerCls, "isPulseLoggingRequested", "()Z");
    if ((*env)->ExceptionCheck(env) || loggerMID == NULL) {
        (*env)->ExceptionClear(env);
        return JNI_FALSE;
    }
    jboolean result = (*env)->CallStaticBooleanMethod(env, loggerCls, loggerMID);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        return JNI_FALSE;
    }
    return result;
}

/*
 * Class:     com_sun_prism_es2_IOSGLContext
 * Method:    nInitialize
 * Signature: (JJJ)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_IOSGLContext_nInitialize
(JNIEnv *env, jclass class, jlong nativeDInfo, jlong nativePFInfo, jlong nativeShareCtxHandle,
 jboolean vSyncRequested)
{
    const char *glVersion;
    const char *glVendor;
    const char *glRenderer;
    char *tmpVersionStr;
    int  versionNumbers[2];
    const char *glExtensions;

    pulseLoggingRequested = isPulseLoggingRequested(env);
    jlong pixelFormat = 0;
    jlong win = 0;
    jlong context = 0;
    int viewNotReady;
    jboolean result;
    ContextInfo *ctxInfo = NULL;
    DrawableInfo* dInfo =  (DrawableInfo* )jlong_to_ptr(nativeDInfo);
    PixelFormatInfo* pfInfo =  (PixelFormatInfo* )jlong_to_ptr(nativePFInfo);

    if (dInfo == NULL) {
        return 0;
    }

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
    glVersion = (char *)glGetString(GL_VERSION);
    if (glVersion == NULL) {
        printAndReleaseResources(0, context, "glVersion == null");
        return 0;
    }

    /* find out the version, major and minor version number */
    tmpVersionStr = strdup(glVersion);
    extractVersionInfo(tmpVersionStr, versionNumbers);
    free(tmpVersionStr);


    fprintf(stderr, "GL_VERSION string = %s\n", glVersion);
    fprintf(stderr, "GL_VERSION (major.minor) = %d.%d\n",
            versionNumbers[0], versionNumbers[1]);

    fprintf(stderr, "CTXINFO vendor\n");

    /* Get the OpenGL vendor and renderer */
    glVendor = (const char *)glGetString(GL_VENDOR);
    if (glVendor == NULL) {
        glVendor = "<UNKNOWN>";
    }
    fprintf(stderr, "CTXINFO renderer\n");
    glRenderer = (const char *)glGetString(GL_RENDERER);
    if (glRenderer == NULL) {
        glRenderer = "<UNKNOWN>";
    }
    fprintf(stderr, "CTXINFO glExtensions\n");
    glExtensions = (const char *)glGetString(GL_EXTENSIONS);
    if (glExtensions == NULL) {
        printAndReleaseResources(0, context, "glExtensions == null");
        return 0;
    }
    fprintf(stderr, "CTXINFO GL_ARB_pixel_buffer_object\n");

    fprintf(stderr, "CTXINFO allocate the structure\n");
    /* allocate the structure */
    ctxInfo = (ContextInfo *)malloc(sizeof(ContextInfo));

    /* initialize the structure */
    initializeCtxInfo(ctxInfo);
    ctxInfo->versionStr = strdup(glVersion);
    ctxInfo->vendorStr = strdup(glVendor);
    ctxInfo->rendererStr = strdup(glRenderer);
    ctxInfo->glExtensionStr = strdup(glExtensions);
    ctxInfo->versionNumbers[0] = versionNumbers[0];
    ctxInfo->versionNumbers[1] = versionNumbers[1];
    ctxInfo->context = context;

    fprintf(stderr, "CTXINFO set function pointers\n");
    /* set function pointers */
    ctxInfo->glActiveTexture = (PFNGLACTIVETEXTUREPROC)
            getProcAddress("glActiveTexture");
    ctxInfo->glAttachShader = (PFNGLATTACHSHADERPROC)
            getProcAddress("glAttachShader");
    ctxInfo->glBindAttribLocation = (PFNGLBINDATTRIBLOCATIONPROC)
            getProcAddress("glBindAttribLocation");
    ctxInfo->glBindFramebuffer = (PFNGLBINDFRAMEBUFFERPROC)
            getProcAddress("glBindFramebuffer");
    ctxInfo->glBindRenderbuffer = (PFNGLBINDRENDERBUFFERPROC)
            getProcAddress("glBindRenderbuffer");
    ctxInfo->glCheckFramebufferStatus = (PFNGLCHECKFRAMEBUFFERSTATUSPROC)
            getProcAddress("glCheckFramebufferStatus");
    ctxInfo->glCreateProgram = (PFNGLCREATEPROGRAMPROC)
            getProcAddress("glCreateProgram");
    ctxInfo->glCreateShader = (PFNGLCREATESHADERPROC)
            getProcAddress("glCreateShader");
    ctxInfo->glCompileShader = (PFNGLCOMPILESHADERPROC)
            getProcAddress("glCompileShader");
    ctxInfo->glDeleteBuffers = (PFNGLDELETEBUFFERSPROC)
            getProcAddress("glDeleteBuffers");
    ctxInfo->glDeleteFramebuffers = (PFNGLDELETEFRAMEBUFFERSPROC)
            getProcAddress("glDeleteFramebuffers");
    ctxInfo->glDeleteProgram = (PFNGLDELETEPROGRAMPROC)
            getProcAddress("glDeleteProgram");
    ctxInfo->glDeleteRenderbuffers = (PFNGLDELETERENDERBUFFERSPROC)
            getProcAddress("glDeleteRenderbuffers");
    ctxInfo->glDeleteShader = (PFNGLDELETESHADERPROC)
            getProcAddress("glDeleteShader");
    ctxInfo->glDetachShader = (PFNGLDETACHSHADERPROC)
            getProcAddress("glDetachShader");
    ctxInfo->glDisableVertexAttribArray = (PFNGLDISABLEVERTEXATTRIBARRAYPROC)
            getProcAddress("glDisableVertexAttribArray");
    ctxInfo->glEnableVertexAttribArray = (PFNGLENABLEVERTEXATTRIBARRAYPROC)
            getProcAddress("glEnableVertexAttribArray");
    ctxInfo->glFramebufferRenderbuffer = (PFNGLFRAMEBUFFERRENDERBUFFERPROC)
            getProcAddress("glFramebufferRenderbuffer");
    ctxInfo->glFramebufferTexture2D = (PFNGLFRAMEBUFFERTEXTURE2DPROC)
            getProcAddress("glFramebufferTexture2D");
    ctxInfo->glGenFramebuffers = (PFNGLGENFRAMEBUFFERSPROC)
            getProcAddress("glGenFramebuffers");
    ctxInfo->glGenRenderbuffers = (PFNGLGENRENDERBUFFERSPROC)
            getProcAddress("glGenRenderbuffers");
    ctxInfo->glGetProgramiv = (PFNGLGETPROGRAMIVPROC)
            getProcAddress("glGetProgramiv");
    ctxInfo->glGetShaderiv = (PFNGLGETSHADERIVPROC)
            getProcAddress("glGetShaderiv");
    ctxInfo->glGetUniformLocation = (PFNGLGETUNIFORMLOCATIONPROC)
            getProcAddress("glGetUniformLocation");
    ctxInfo->glLinkProgram = (PFNGLLINKPROGRAMPROC)
            getProcAddress("glLinkProgram");
    ctxInfo->glRenderbufferStorage = (PFNGLRENDERBUFFERSTORAGEPROC)
            getProcAddress("glRenderbufferStorage");
    ctxInfo->glShaderSource = (PFNGLSHADERSOURCEPROC)
            getProcAddress("glShaderSource");
    ctxInfo->glUniform1f = (PFNGLUNIFORM1FPROC)
            getProcAddress("glUniform1f");
    ctxInfo->glUniform2f = (PFNGLUNIFORM2FPROC)
            getProcAddress("glUniform2f");
    ctxInfo->glUniform3f = (PFNGLUNIFORM3FPROC)
            getProcAddress("glUniform3f");
    ctxInfo->glUniform4f = (PFNGLUNIFORM4FPROC)
            getProcAddress("glUniform4f");
    ctxInfo->glUniform4fv = (PFNGLUNIFORM4FVPROC)
            getProcAddress("glUniform4fv");
    ctxInfo->glUniform1i = (PFNGLUNIFORM1IPROC)
            getProcAddress("glUniform1i");
    ctxInfo->glUniform2i = (PFNGLUNIFORM2IPROC)
            getProcAddress("glUniform2i");
    ctxInfo->glUniform3i = (PFNGLUNIFORM3IPROC)
            getProcAddress("glUniform3i");
    ctxInfo->glUniform4i = (PFNGLUNIFORM4IPROC)
            getProcAddress("glUniform4i");
    ctxInfo->glUniform4iv = (PFNGLUNIFORM4IVPROC)
            getProcAddress("glUniform4iv");
    ctxInfo->glUniformMatrix4fv = (PFNGLUNIFORMMATRIX4FVPROC)
            getProcAddress("glUniformMatrix4fv");
    ctxInfo->glUseProgram = (PFNGLUSEPROGRAMPROC)
            getProcAddress("glUseProgram");
    ctxInfo->glValidateProgram = (PFNGLVALIDATEPROGRAMPROC)
            getProcAddress("glValidateProgram");
    ctxInfo->glVertexAttribPointer = (PFNGLVERTEXATTRIBPOINTERPROC)
            getProcAddress("glVertexAttribPointer");
    ctxInfo->glGenBuffers = (PFNGLGENBUFFERSPROC)
            getProcAddress("glGenBuffers");
    ctxInfo->glBindBuffer = (PFNGLBINDBUFFERPROC)
            getProcAddress("glBindBuffer");
    ctxInfo->glBufferData = (PFNGLBUFFERDATAPROC)
            getProcAddress("glBufferData");
    ctxInfo->glBufferSubData = (PFNGLBUFFERSUBDATAPROC)
            getProcAddress("glBufferSubData");
    ctxInfo->glGetShaderInfoLog = (PFNGLGETSHADERINFOLOGPROC)
            getProcAddress("glGetShaderInfoLog");
    ctxInfo->glGetProgramInfoLog = (PFNGLGETPROGRAMINFOLOGPROC)
            getProcAddress("glGetProgramInfoLog");
    ctxInfo->glTexImage2DMultisample = (PFNGLTEXIMAGE2DMULTISAMPLEPROC)
            getProcAddress("glTexImage2DMultisample");
    ctxInfo->glRenderbufferStorageMultisample = (PFNGLRENDERBUFFERSTORAGEMULTISAMPLEPROC)
            getProcAddress("glRenderbufferStorageMultisample");
    ctxInfo->glBlitFramebuffer = (PFNGLBLITFRAMEBUFFERPROC)
            getProcAddress("glBlitFramebuffer");

    // initialize platform states and properties to match
    // cached states and properties
    setSwapInterval((void *) jlong_to_ptr(ctxInfo->context), 0);
    ctxInfo->state.vSyncEnabled = JNI_FALSE;
    ctxInfo->vSyncRequested = vSyncRequested;

    initState(ctxInfo);

    return ptr_to_jlong(ctxInfo);
}

/*
 * Class:     com_sun_prism_es2_IOSGLContext
 * Method:    nGetNativeHandle
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_IOSGLContext_nGetNativeHandle
(JNIEnv *env, jclass class, jlong nativeCtxInfo)
{
    ContextInfo* ctxInfo = (ContextInfo*) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return 0;
    }
    return ctxInfo->context;
}

/*
 * Class:     com_sun_prism_es2_IOSGLContext
 * Method:    nMakeCurrent
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_IOSGLContext_nMakeCurrent
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativeDInfo)
{
    ContextInfo* ctxInfo = (ContextInfo*) jlong_to_ptr(nativeCtxInfo);
    DrawableInfo* dInfo =  (DrawableInfo* )jlong_to_ptr(nativeDInfo);
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
    setSwapInterval(ctxInfo->context, interval);
    if (pulseLoggingRequested) {
        fprintf(stderr, "setSwapInterval(%d)\n", interval);
    }
}
