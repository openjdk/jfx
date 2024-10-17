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
#include "com_sun_prism_es2_X11GLDrawable.h"

/*
 * Class:     com_sun_prism_es2_X11GLDrawable
 * Method:    nCreateDrawable
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_X11GLDrawable_nCreateDrawable
(JNIEnv *env, jclass class, jlong nativeWindow, jlong nativePFInfo) {
    DrawableInfo *dInfo = NULL;
    PixelFormatInfo *pfInfo = (PixelFormatInfo *) jlong_to_ptr(nativePFInfo);
    if (pfInfo == NULL) {
        return 0;
    }

    /* allocate the structure */
    dInfo = (DrawableInfo *) malloc(sizeof (DrawableInfo));
    if (dInfo == NULL) {
        fprintf(stderr, "nCreateDrawable: Failed in malloc\n");
        return 0;
    }

    /* initialize the structure */
    memset(dInfo, 0, sizeof(DrawableInfo));

    // Use the dummyWin that was already created in the pfInfo
    // since this is an non-onscreen drawable.
    dInfo->display = pfInfo->display;
    dInfo->win = (Window) jlong_to_ptr(nativeWindow);
    dInfo->onScreen = JNI_TRUE;

    return ptr_to_jlong(dInfo);
}

/*
 * Class:     com_sun_prism_es2_X11GLDrawable
 * Method:    nGetDummyDrawable
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_X11GLDrawable_nGetDummyDrawable
(JNIEnv *env, jclass class, jlong nativePFInfo) {
    DrawableInfo *dInfo = NULL;
    PixelFormatInfo *pfInfo = (PixelFormatInfo *) jlong_to_ptr(nativePFInfo);
    if (pfInfo == NULL) {
        return 0;
    }

    /* allocate the structure */
    dInfo = (DrawableInfo *) malloc(sizeof (DrawableInfo));
    if (dInfo == NULL) {
        fprintf(stderr, "nGetDummyDrawable: Failed in malloc\n");
        return 0;
    }

    /* initialize the structure */
    memset(dInfo, 0, sizeof(DrawableInfo));

    // Use the dummyWin that was already created in the pfInfo
    // since this is an non-onscreen drawable.
    dInfo->display = pfInfo->display;
    dInfo->win = pfInfo->dummyWin;
    dInfo->onScreen = JNI_FALSE;

    return ptr_to_jlong(dInfo);
}

/*
 * Class:     com_sun_prism_es2_X11GLDrawable
 * Method:    nSwapBuffers
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_es2_X11GLDrawable_nSwapBuffers
(JNIEnv *env, jclass class, jlong nativeDInfo) {
    DrawableInfo *dInfo = (DrawableInfo *) jlong_to_ptr(nativeDInfo);
    if (dInfo == NULL) {
        return JNI_FALSE;
    }
    glXSwapBuffers(dInfo->display, dInfo->win);
    return JNI_TRUE;
}

/*
 * Class:     com_sun_prism_es2_X11GLDrawable
 * Method:    nReleaseDrawable
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_X11GLDrawable_nReleaseDrawable
(JNIEnv *env, jclass class, jlong nativeDInfo) {
    DrawableInfo *dInfo = (DrawableInfo *) jlong_to_ptr(nativeDInfo);
    if (dInfo == NULL) {
        return;
    }

    free(dInfo);
}
