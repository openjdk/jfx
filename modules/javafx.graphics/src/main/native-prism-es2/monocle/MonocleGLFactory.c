/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <math.h>

#include <EGL/egl.h>
#include "eglUtils.h"

#include "../PrismES2Defs.h"

#include "com_sun_prism_es2_MonocleGLContext.h"
#ifndef ANDROID
#define __USE_GNU
#include <dlfcn.h>
#endif

extern void *get_dlsym(void *handle, const char *symbol, int warn);

#define GET_DLSYM(handle,symbol) get_dlsym(handle,symbol, 0);

#define asPtr(x) ((void *) (unsigned long) (x))
#define asJLong(x) ((jlong) (unsigned long) (x))

//Builtin library entrypoint
JNIEXPORT jint JNICALL
JNI_OnLoad_prism_es2_monocle(JavaVM *vm, void * reserved) {
fprintf(stderr, "In JNI_OnLoad_prism_es2\n");
#ifdef JNI_VERSION_1_8
    //min. returned JNI_VERSION required by JDK8 for builtin libraries
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_8) != JNI_OK) {
        return JNI_VERSION_1_4;
    }
    return JNI_VERSION_1_8;
#else
    return JNI_VERSION_1_4;
#endif
}

JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_MonocleGLFactory_nPopulateNativeCtxInfo
(JNIEnv *env, jclass clazz, jlong libraryHandle) {
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
    //char *eglExtensions = strdup((char *)eglQueryString(asPtr(eglDisplay),
    ///                                                    EGL_EXTENSIONS));

    /* find out the version, major and minor version number */
    char *tmpVersionStr = strdup(glVersion);
    int versionNumbers[2];
    extractVersionInfo(tmpVersionStr, versionNumbers);
    free(tmpVersionStr);

    ctxInfo->versionStr = strdup(glVersion);
    ctxInfo->vendorStr = strdup(glVendor);
    ctxInfo->rendererStr = strdup(glRenderer);
    ctxInfo->glExtensionStr = strdup(glExtensions);
    //ctxInfo->glxExtensionStr = strdup(eglExtensions);
    ctxInfo->versionNumbers[0] = versionNumbers[0];
    ctxInfo->versionNumbers[1] = versionNumbers[1];

    //ctxInfo->display = asPtr(displayType);
    //ctxInfo->context = asPtr(eglContext);
    //ctxInfo->egldisplay = asPtr(eglDisplay);

    // cleanup
    free(glExtensions);
    //free(eglExtensions);

    // from the eglWrapper.c
    void *handle = asPtr(libraryHandle);
    if (libraryHandle == 0) {
         handle = RTLD_DEFAULT;
    }

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
    return ctxInfo;
}

/*
 * Class:     com_sun_prism_es2_MonocleGLFactory
 * Method:    nGetAdapterOrdinal
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_MonocleGLFactory_nGetAdapterOrdinal
(JNIEnv *env, jclass jMonocleGLFactory, jlong nativeScreen) {
    return 0;
}

/*
 * Class:     com_sun_prism_es2_MonocleGLFactory
 * Method:    nGetAdapterCount
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_MonocleGLFactory_nGetAdapterCount
(JNIEnv *env, jclass jMonocleGLFactory) {
    return 1;
}

/*
 * Class:     com_sun_prism_es2_MonocleGLFactory
 * Method:    nGetDefaultScreen
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_MonocleGLFactory_nGetDefaultScreen
(JNIEnv *env, jclass jMonocleGLFactory, jlong nativeCtxInfo) {
    return 0;
}

/*
 * Class:     com_sun_prism_es2_MonocleGLFactory
 * Method:    nGetDisplay
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_MonocleGLFactory_nGetDisplay
(JNIEnv *env, jclass jMonocleGLFactory, jlong nativeCtxInfo) {
    return 0;
}

/*
 * Class:     com_sun_prism_es2_MonocleGLFactory
 * Method:    nGetVisualID
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_MonocleGLFactory_nGetVisualID
(JNIEnv *env, jclass jMonocleGLFactory, jlong nativeCtxInfo) {
    return 0;
}

/*
 * Class:     com_sun_prism_es2_MonocleGLFactory
 * Method:    nGetIsGL2
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_es2_MonocleGLFactory_nGetIsGL2
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    return ((ContextInfo *)jlong_to_ptr(nativeCtxInfo))->gl2;
}
