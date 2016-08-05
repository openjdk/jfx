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

#include "../PrismES2Defs.h"
#include "com_sun_prism_es2_MacGLDrawable.h"

extern void initializeDrawableInfo(DrawableInfo *dInfo);

/*
 * Class:     com_sun_prism_es2_MacGLDrawable
 * Method:    nCreateDrawable
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_MacGLDrawable_nCreateDrawable
(JNIEnv *env, jclass class, jlong nativeWindow, jlong nativePFInfo) {
    DrawableInfo *dInfo = NULL;

    /* allocate the structure */
    dInfo = (DrawableInfo *) malloc(sizeof (DrawableInfo));
    if (dInfo == NULL) {
        fprintf(stderr, "nCreateDrawable: Failed in malloc\n");
        return 0;
    }

    /* initialize the structure */
    initializeDrawableInfo(dInfo);

    dInfo->win = nativeWindow;
    dInfo->onScreen = JNI_TRUE;

    return ptr_to_jlong(dInfo);
}

/*
 * Class:     com_sun_prism_es2_MacGLDrawable
 * Method:    nGetDummyDrawable
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_MacGLDrawable_nGetDummyDrawable
(JNIEnv *env, jclass class, jlong nativePFInfo) {
    jlong win = 0;
    DrawableInfo *dInfo = NULL;

    /*
     * No need to create a dummy window on Mac
     * It only uses RTT for rendering and hand over to the CALayer.
     */

    /* allocate the structure */
    dInfo = (DrawableInfo *) malloc(sizeof (DrawableInfo));
    if (dInfo == NULL) {
        fprintf(stderr, "nGetDummyDrawable: Failed in malloc\n");
        return 0;
    }

    /* initialize the structure */
    initializeDrawableInfo(dInfo);

    dInfo->win = win;
    dInfo->onScreen = JNI_FALSE;

    return ptr_to_jlong(dInfo);
}

/*
 * Class:     com_sun_prism_es2_MacGLDrawable
 * Method:    nSwapBuffers
 * Signature: (JJ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_es2_MacGLDrawable_nSwapBuffers
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativeDInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return JNI_FALSE;
    }

    flushBuffer((void *) (intptr_t) ctxInfo->context);
    return JNI_TRUE;
}
