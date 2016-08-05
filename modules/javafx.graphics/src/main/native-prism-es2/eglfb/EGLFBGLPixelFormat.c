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

#include <jni.h>
#include <stdlib.h>
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include "../PrismES2Defs.h"

#include <EGL/egl.h>

#include "eglUtils.h"

#include "com_sun_prism_es2_EGLFBGLPixelFormat.h"

/*
 * Class:     com_sun_prism_es2_EGLFBGLPixelFormat
 * Method:    nCreatePixelFormat
 * Signature: (J[I)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_EGLFBGLPixelFormat_nCreatePixelFormat
(JNIEnv *env, jclass jeglfbPixelFormat, jlong nativeScreen, jintArray attrArr) {
    int eglAttrs[MAX_GLX_ATTRS_LENGTH]; /* value, attr pair plus a None */
    jint *attrs;
    PixelFormatInfo *pfInfo = NULL;

    EGLConfig config;
    int numFBConfigs;

    if (attrArr == NULL) {
        return 0;
    }
    attrs = (*env)->GetIntArrayElements(env, attrArr, NULL);
    setEGLAttrs(attrs, eglAttrs);
    (*env)->ReleaseIntArrayElements(env, attrArr, attrs, JNI_ABORT);

    EGLNativeDisplayType disptype = getNativeDisplayType();
    if (disptype == (EGLNativeDisplayType)0xBAD) {
        fprintf(stderr, "nCreatePixelFormat: Failed in getNativeDisplayType\n");
        return 0;
    }
    EGLDisplay egldisplay = eglGetDisplay(disptype);
    if (EGL_NO_DISPLAY == egldisplay) {
        fprintf(stderr, "eglGetDisplay returned EGL_NO_DISPLAY");
        return 0;
    }

    if (!eglInitialize(egldisplay, NULL, NULL)) {
        fprintf(stderr, "eglInitialize failed!");
        return 0;
    }

#ifdef DEBUG
    printf("Requested EGL attributes:\n");
    printConfigAttrs(eglAttrs);
#endif

    if (!eglChooseConfig(egldisplay, eglAttrs, &config, 1, &numFBConfigs)) {
        fprintf(stderr, "PixelFormat - Failed to get a FBconfig with requested attrs\n");
        //cleanup
        return 0;
    }
#ifdef DEBUG
    printf("EGL: Using config\n");
    printConfig(egldisplay, config);
#endif

    /* allocate the structure */
    pfInfo = (PixelFormatInfo *) malloc(sizeof(PixelFormatInfo));
    if (pfInfo == NULL) {
        fprintf(stderr, "nCreatePixelFormat: Failed in malloc\n");
        return 0;
    }

    /* initialize the structure */
    initializePixelFormatInfo(pfInfo);
    pfInfo->fbConfig = config;

    return ptr_to_jlong(pfInfo);
}

