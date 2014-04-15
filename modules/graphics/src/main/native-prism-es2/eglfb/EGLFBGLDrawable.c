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

#include <stdlib.h>
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

#include <EGL/egl.h>
#include <GL/gl.h>
#include "eglUtils.h"

#include "com_sun_prism_es2_EGLFBGLContext.h"

extern void initializeDrawableInfo(DrawableInfo *dInfo);
extern void deleteDrawableInfo(DrawableInfo *dInfo);

/*
 * Class:     com_sun_prism_es2_EGLFBGLDrawable
 * Method:    nCreateDrawable
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_EGLFBGLDrawable_nCreateDrawable
(JNIEnv *env, jclass jeglfbDrawable, jlong nativeWindow, jlong nativePFInfo) {
    DrawableInfo *dInfo = NULL;
    PixelFormatInfo *pfInfo = (PixelFormatInfo *) jlong_to_ptr(nativePFInfo);
    if (pfInfo == NULL) {
        fprintf(stderr, "nCreateDrawable: PixelFormatInfo null\n");
        return 0;
    }
    /* allocate the structure */
    dInfo = (DrawableInfo *) malloc(sizeof(DrawableInfo));
    if (dInfo == NULL) {
        fprintf(stderr, "nCreateDrawable: Failed in malloc\n");
        return 0;
    }

    /* initialize the structure */
    initializeDrawableInfo(dInfo);
    EGLNativeDisplayType disptype = getNativeDisplayType();
    if (disptype == (EGLNativeDisplayType)0xBAD) {
        fprintf(stderr, "nCreateDrawable: Failed in getNativeDisplayType\n");
        return 0;
    }
    dInfo->egldisplay = eglGetDisplay(disptype);
    dInfo->eglsurface = getSharedWindowSurface(dInfo->egldisplay,
                                               pfInfo->fbConfig,
                                               jlong_to_ptr(nativeWindow));
    dInfo->onScreen = JNI_TRUE;

    return ptr_to_jlong(dInfo);
}

/*
 * Class:     com_sun_prism_es2_EGLFBGLDrawable
 * Method:    nGetDummyDrawable
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_EGLFBGLDrawable_nGetDummyDrawable
(JNIEnv *env, jclass jeglfbDrawable, jlong nativePFInfo) {
    DrawableInfo *dInfo = NULL;
    PixelFormatInfo *pfInfo = (PixelFormatInfo *) jlong_to_ptr(nativePFInfo);
    if (pfInfo == NULL) {
        fprintf(stderr, " GetDummyDrawable, PixelFormatInfo is null\n");
        return 0;
    }

    /* allocate the structure */
    dInfo = (DrawableInfo *) malloc(sizeof(DrawableInfo));
    if (dInfo == NULL) {
        fprintf(stderr, "nGetDummyDrawable: Failed in malloc\n");
        return 0;
    }

    /* initialize the structure */
    initializeDrawableInfo(dInfo);
    EGLNativeDisplayType disptype = getNativeDisplayType();
    if (disptype == (EGLNativeDisplayType)0xBAD) {
        fprintf(stderr, "nGetDummyDrawable: Failed in getNativeDisplayType\n");
        free(dInfo);
        return 0;
    }
    dInfo->egldisplay =
        eglGetDisplay(disptype);
    dInfo->onScreen = JNI_FALSE;
    dInfo->eglsurface = getDummyWindowSurface(pfInfo->display,
                                              pfInfo->fbConfig);

    return ptr_to_jlong(dInfo);
}

/*
 * Class:     com_sun_prism_es2_EGLFBGLDrawable
 * Method:    nSwapBuffers
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_es2_EGLFBGLDrawable_nSwapBuffers
(JNIEnv *env, jclass jeglfbDrawable, jlong nativeDInfo) {
    int value;

    DrawableInfo *dInfo = (DrawableInfo *) jlong_to_ptr(nativeDInfo);
    if (dInfo == NULL) {
        return JNI_FALSE;
    }
    if (!eglSwapBuffers(dInfo->egldisplay, dInfo->eglsurface)) {
        fprintf(stderr, "eglSwapBuffers failed; eglGetError %d\n", eglGetError());
    }
    return JNI_TRUE;
}

