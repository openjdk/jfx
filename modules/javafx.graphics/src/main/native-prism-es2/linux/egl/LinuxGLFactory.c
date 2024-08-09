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
#include <X11/Xutil.h>

#define ERROR_CODE_TO_STR(value) case value: return #value;

#include "../PrismES2Defs.h"
#include "com_sun_prism_es2_LinuxGLFactory.h"
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL
JNI_OnLoad_prism_es2(JavaVM *vm, void * reserved) {
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
#endif


void setEGLAttrs(jint *attrs, int *eglAttrs) {
    int index = 0;

    eglAttrs[index++] = EGL_RENDERABLE_TYPE;
    eglAttrs[index++] = EGL_OPENGL_BIT;

    eglAttrs[index++] = EGL_SURFACE_TYPE;
    eglAttrs[index++] = EGL_WINDOW_BIT;

    eglAttrs[index++] = EGL_RED_SIZE;
    eglAttrs[index++] = attrs[RED_SIZE];
    eglAttrs[index++] = EGL_GREEN_SIZE;
    eglAttrs[index++] = attrs[GREEN_SIZE];
    eglAttrs[index++] = EGL_BLUE_SIZE;
    eglAttrs[index++] = attrs[BLUE_SIZE];
    eglAttrs[index++] = EGL_ALPHA_SIZE;
    eglAttrs[index++] = attrs[ALPHA_SIZE];

    eglAttrs[index++] = EGL_DEPTH_SIZE;
    eglAttrs[index++] = attrs[DEPTH_SIZE];

    eglAttrs[index] = EGL_NONE;
}

void printAndReleaseResources(EGLDisplay eglDisplay, EGLSurface eglSurface, EGLContext eglContext,
                              const char *message) {
    if (message != NULL) {
        fprintf(stderr, "%s\n", message);
    }

    if (eglDisplay == EGL_NO_DISPLAY) {
        return;
    }

    eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);

    if (eglContext != EGL_NO_CONTEXT) {
        eglDestroyContext(eglDisplay, eglContext);
    }

    if (eglSurface != EGL_NO_SURFACE) {
        eglDestroySurface(eglDisplay, eglSurface);
    }
}

const char* eglGetErrorString(EGLint error) {
    switch(error) {
        ERROR_CODE_TO_STR(EGL_SUCCESS)
        ERROR_CODE_TO_STR(EGL_NOT_INITIALIZED)
        ERROR_CODE_TO_STR(EGL_BAD_ACCESS)
        ERROR_CODE_TO_STR(EGL_BAD_ALLOC)
        ERROR_CODE_TO_STR(EGL_BAD_ATTRIBUTE)
        ERROR_CODE_TO_STR(EGL_BAD_CONTEXT)
        ERROR_CODE_TO_STR(EGL_BAD_CONFIG)
        ERROR_CODE_TO_STR(EGL_BAD_CURRENT_SURFACE)
        ERROR_CODE_TO_STR(EGL_BAD_DISPLAY)
        ERROR_CODE_TO_STR(EGL_BAD_SURFACE)
        ERROR_CODE_TO_STR(EGL_BAD_MATCH)
        ERROR_CODE_TO_STR(EGL_BAD_PARAMETER)
        ERROR_CODE_TO_STR(EGL_BAD_NATIVE_PIXMAP)
        ERROR_CODE_TO_STR(EGL_BAD_NATIVE_WINDOW)
        ERROR_CODE_TO_STR(EGL_CONTEXT_LOST)
        default:
            return "UNKNOWN EGL ERROR";
    }
}


/*
 * Class:     com_sun_prism_es2_LinuxGLFactory
 * Method:    nInitialize
 * Signature: ([I[J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_LinuxGLFactory_nInitialize
(JNIEnv *env, jclass class, jintArray attrArr) {

    EGLint eglAttrs[MAX_GL_ATTRS_LENGTH];
    jint *attrs;
    ContextInfo *ctxInfo = NULL;

    const char *glVersion;
    const char *glVendor;
    const char *glRenderer;
    char *tmpVersionStr;
    int versionNumbers[2];
    const char *glExtensions;
    const char *eglExtensions;
    EGLint majorVersion;
    EGLint minorVersion;
    Display *display = NULL;
    EGLDisplay eglDisplay;
    EGLConfig eglConfig;
    int num_configs;

    if (attrArr == NULL) {
        return 0;
    }

    attrs = (*env)->GetIntArrayElements(env, attrArr, NULL);
    setEGLAttrs(attrs, eglAttrs);
    (*env)->ReleaseIntArrayElements(env, attrArr, attrs, JNI_ABORT);

    display = XOpenDisplay(0);
    if (display == NULL) {
        return 0;
    }

    eglDisplay = eglGetDisplay(display);

    if (eglDisplay == EGL_NO_DISPLAY) {
        fprintf(stderr, "Prism ES2 Error: Initialize - EGL_NO_DISPLAY\n");
        return 0;
    }

    if (!eglBindAPI(EGL_OPENGL_API)) {
        fprintf(stderr, "Prism ES2 Error: Initialize - cannot bind EGL_OPENGL_API.\n");
        return 0;
    }

    if (!eglInitialize(eglDisplay, &majorVersion, &minorVersion)) {
        fprintf(stderr, "Prism ES2 Error: Initialize - eglInitialize failed. Version: %d.%d\n",
                majorVersion, minorVersion);
        return 0;
    }

    if ((eglGetConfigs(eglDisplay, NULL, 0, &num_configs) != EGL_TRUE) || (num_configs == 0)) {
        fprintf(stderr, "Prism ES2 Error: Initialize - no EGL configuration available\n");
        return 0;
    }

    if (eglChooseConfig(eglDisplay, eglAttrs, &eglConfig, 1, &num_configs) != EGL_TRUE) {
        fprintf(stderr, "Prism ES2 Error: Initialize - eglChooseConfig failed\n");
        return 0;
    }

    EGLContext eglContext = eglCreateContext(eglDisplay, eglConfig, EGL_NO_CONTEXT, NULL);

    if (!eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, eglContext)) {
        printAndReleaseResources(eglDisplay, EGL_NO_SURFACE, eglContext,
                "Prism ES2 Error: eglMakeCurrent failed");
        return 0;
    }

    /* Get the OpenGL version */
    glVersion = (char *) glGetString(GL_VERSION);
    if (glVersion == NULL) {
        printAndReleaseResources(eglDisplay, EGL_NO_SURFACE, eglContext,
                "glVersion == null");
        return 0;
    }

    /* find out the version, major and minor version number */
    tmpVersionStr = strdup(glVersion);
    extractVersionInfo(tmpVersionStr, versionNumbers);
    free(tmpVersionStr);

//    fprintf(stderr, "GL_VERSION string = %s\n", glVersion);
//    fprintf(stderr, "GL_VERSION (major.minor) = %d.%d\n",
//            versionNumbers[0], versionNumbers[1]);

    /*
     * Targeted Cards: Intel HD Graphics, Intel HD Graphics 2000/3000,
     * Radeon HD 2350, GeForce FX (with newer drivers), GeForce 7 series or higher
     *
     * Check for OpenGL 2.1 or later.
     */
    if ((versionNumbers[0] < 2) || ((versionNumbers[0] == 2) && (versionNumbers[1] < 1))) {
        fprintf(stderr, "Prism-ES2 Error: GL_VERSION (major.minor) = %d.%d\n",
                versionNumbers[0], versionNumbers[1]);
        printAndReleaseResources(eglDisplay, EGL_NO_SURFACE, eglContext, NULL);
        return 0;
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
        printAndReleaseResources(eglDisplay, EGL_NO_SURFACE, eglContext,
                "Prism-ES2 Error : glExtensions == null");
        return 0;
    }

    // We use GL_ARB_pixel_buffer_object as an guide to
    // determine PS 3.0 capable.
    if (!isExtensionSupported(glExtensions, "GL_ARB_pixel_buffer_object")) {
            printAndReleaseResources(eglDisplay, EGL_NO_SURFACE, eglContext,
             "GL profile isn't PS 3.0 capable");
        return 0;
    }

    eglExtensions = (const char *) eglQueryString(eglDisplay, EGL_EXTENSIONS);
    if (eglExtensions == NULL) {
            printAndReleaseResources(eglDisplay, EGL_NO_SURFACE, eglContext,
                "eglExtensions == null");
        return 0;
    }

    /* Note: We are only storing the string information of a driver.
     Assuming a system with a single or homogeneous GPUs. For the case
     of heterogeneous GPUs system the string information will need to move to
     GLContext class. */
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
    ctxInfo->gl2 = JNI_TRUE;
    ctxInfo->eglDisplay = eglDisplay;

    return ptr_to_jlong(ctxInfo);
}

/*
 * Class:     com_sun_prism_es2_LinuxGLFactory
 * Method:    nGetAdapterOrdinal
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_LinuxGLFactory_nGetAdapterOrdinal
(JNIEnv *env, jclass class, jlong screen) {
    //TODO: Needs implementation to handle multi-monitors (RT-27437)
    return 0;
}

/*
 * Class:     com_sun_prism_es2_LinuxGLFactory
 * Method:    nGetAdapterCount
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_LinuxGLFactory_nGetAdapterCount
(JNIEnv *env, jclass class) {
    //TODO: Needs implementation to handle multi-monitors (RT-27437)
    return 1;
}

/*
 * Class:     com_sun_prism_es2_LinuxGLFactory
 * Method:    nGetDefaultScreen
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_LinuxGLFactory_nGetDefaultScreen
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return 0;
    }
    return (jint) ctxInfo->screen;
}

/*
 * Class:     com_sun_prism_es2_LinuxGLFactory
 * Method:    nGetDisplay
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_LinuxGLFactory_nGetDisplay
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return 0;
    }

    return (jlong) ptr_to_jlong(ctxInfo->display);
}

/*
 * Class:     com_sun_prism_es2_LinuxGLFactory
 * Method:    nGetVisualID
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_LinuxGLFactory_nGetVisualID
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return 0;
    }
    return (jlong) ctxInfo->visualID;
}

/*
 * Class:     com_sun_prism_es2_X11_LinuxGLFactory
 * Method:    nGetIsGL2
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_es2_LinuxGLFactory_nGetIsGL2
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    return ((ContextInfo *)jlong_to_ptr(nativeCtxInfo))->gl2;
}
