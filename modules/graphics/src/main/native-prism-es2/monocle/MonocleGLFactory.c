/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

#include "com_sun_prism_es2_EGLFBGLContext.h"

/*
 * Class:     com_sun_prism_es2_EGLFBGLFactory
 * Method:    nInitialize
 * Signature: ([I)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_MonocleGLFactory_nInitialize
(JNIEnv *env, jclass jeglfbGLFactory, jintArray attrArr) {
    int eglAttrs[MAX_GLX_ATTRS_LENGTH]; /* value, attr pair plus a None */
    jint *attrs;

    if (attrArr == NULL) {
        return 0;
    }
    return 0;
/*
    attrs = (*env)->GetIntArrayElements(env, attrArr, NULL);
    setEGLAttrs(attrs, eglAttrs);
    (*env)->ReleaseIntArrayElements(env, attrArr, attrs, JNI_ABORT);

    EGLint surfaceType;
    EGLConfig config = 0;
    EGLint numconfigs = 0;
    EGLint configId = 0;

    EGLDisplay egldisplay = eglGetDisplay(getNativeDisplayType());
    if (EGL_NO_DISPLAY == egldisplay) {
        fprintf(stderr, "eglGetDisplay returned EGL_NO_DISPLAY");
        // cleanup
        return 0;
    }
    EGLint egl_major, egl_minor;
    if (!eglInitialize(egldisplay, &egl_major, &egl_minor)) {
        fprintf(stderr, "eglInitialize failed!");
        // cleanup
        return 0;
    }

    if (!eglBindAPI(EGL_OPENGL_ES_API)) {
        fprintf(stderr, "eglBindAPI failed!");
        return 0;
    }

#ifdef DEBUG
    // This is the client side
    const char *eglVendor  = eglQueryString(egldisplay, EGL_VENDOR);
    const char *eglVersion = eglQueryString(egldisplay, EGL_VERSION);
    printf("EGL_VENDOR  is %s\n", eglVendor);
    printf("EGL_VERSION version is %s\n", eglVersion);
    printf("Requested EGL attributes:\n");
    printConfigAttrs(eglAttrs);
#endif

    if (!eglChooseConfig(egldisplay, eglAttrs, &config, 1, &numconfigs)) {
        fprintf(stderr, "Failed to get a FBconfig with requested attrs\n");
        //cleanup
        return 0;
    }

#ifdef DEBUG
    printf("eglChooseConfig return %d configs\n", numconfigs);
#endif

    if (!eglGetConfigAttrib(egldisplay, config, EGL_CONFIG_ID, &configId)) {
        fprintf(stderr, "eglGetConfigAttrib failed!");
        return 0;
    }

#ifdef DEBUG
    printf("EGL: Using config #%d\n", configId);
    printConfig(egldisplay, config);
#endif

    ContextInfo *ctxInfo = eglContextFromConfig(egldisplay, config);
    if (!ctxInfo) {
        fprintf(stderr, "Failed to create EGLContext");
        return 0; // cleanup
    }
    // Information required by GLass at startup
    ctxInfo->display = getNativeDisplayType();
    ctxInfo->gl2 = JNI_FALSE;
    eglDestroyContext(ctxInfo->egldisplay, ctxInfo->context);
    return ptr_to_jlong(ctxInfo);
    */
}

/*
 * Class:     com_sun_prism_es2_EGLFBGLFactory
 * Method:    nGetAdapterOrdinal
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_EGLFBGLFactory_nGetAdapterOrdinal
(JNIEnv *env, jclass jeglfbGLFactory, jlong nativeScreen) {
    return 0;
}

/*
 * Class:     com_sun_prism_es2_EGLFBGLFactory
 * Method:    nGetAdapterCount
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_EGLFBGLFactory_nGetAdapterCount
(JNIEnv *env, jclass jeglfbGLFactory) {
    return 1;
}

/*
 * Class:     com_sun_prism_es2_EGLFBGLFactory
 * Method:    nGetDefaultScreen
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_EGLFBGLFactory_nGetDefaultScreen
(JNIEnv *env, jclass jeglfbGLFactory, jlong nativeCtxInfo) {
    return 0;
}

/*
 * Class:     com_sun_prism_es2_EGLFBGLFactory
 * Method:    nGetDisplay
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_EGLFBGLFactory_nGetDisplay
(JNIEnv *env, jclass jeglfbGLFactory, jlong nativeCtxInfo) {
    return 0;
}

/*
 * Class:     com_sun_prism_es2_EGLFBGLFactory
 * Method:    nGetVisualID
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_EGLFBGLFactory_nGetVisualID
(JNIEnv *env, jclass jeglfbGLFactory, jlong nativeCtxInfo) {
    return 0;
}

/*
 * Class:     com_sun_prism_es2_EGLFBGLFactory
 * Method:    nGetIsGL2
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_es2_EGLFBGLFactory_nGetIsGL2
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    return ((ContextInfo *)jlong_to_ptr(nativeCtxInfo))->gl2;
}
