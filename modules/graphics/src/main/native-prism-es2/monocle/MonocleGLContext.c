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
 * Class:     com_sun_prism_es2_EGLFBGLContext
 * Method:    nInitialize
 * Signature: (JJZ)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_EGLFBGLContext_nInitialize
(JNIEnv *env, jclass jeglfbcontext, jlong nativeDInfo, jlong nativePFInfo, jboolean SyncRequest) {
    DrawableInfo *dInfo = (DrawableInfo *) jlong_to_ptr(nativeDInfo);
    PixelFormatInfo *pfInfo = (PixelFormatInfo *) jlong_to_ptr(nativePFInfo);

    if ((dInfo == NULL) || (pfInfo == NULL)) {
        fprintf(stderr, "EGLFBGLContext_nInitialize: null dInfo pfInfo\n");
        return 0;
    }
    EGLConfig fbConfig = pfInfo->fbConfig;

    ContextInfo *ctxInfo = eglContextFromConfig(dInfo->egldisplay, fbConfig);
    return ptr_to_jlong(ctxInfo);
}

/*
 * Class:     com_sun_prism_es2_EGLFBGLContext
 * Method:    nGetNativeHandle
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_EGLFBGLContext_nGetNativeHandle
(JNIEnv *env, jclass jeglfbcontext, jlong nativeCtxInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        fprintf(stderr, " nGetNativeHandle, ContextInfo is null\n");
        return 0;
    }
    return ptr_to_jlong(ctxInfo->context);
}

/*
 * Class:     com_sun_prism_es2_EGLFBGLContext
 * Method:    nMakeCurrent
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_EGLFBGLContext_nMakeCurrent
(JNIEnv *env, jclass jeglfbcontext, jlong nativeCtxInfo, jlong nativeDInfo) {

    DrawableInfo *dInfo = (DrawableInfo *) jlong_to_ptr(nativeDInfo);
    if (dInfo == NULL) {
        fprintf(stderr, "nMakeCurrent: dIfno is null!!!\n");
        return;
    }

    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        fprintf(stderr, "nMakeCurrent: ctxInfo is null!!!\n");
        return;
    }
    int interval;
    jboolean vSyncNeeded;

    if (!eglMakeCurrent(dInfo->egldisplay, dInfo->eglsurface,
        dInfo->eglsurface, ctxInfo->context)) {
        fprintf(stderr, "Failed in eglMakeCurrent for %p %p %d\n",
            dInfo->eglsurface, ctxInfo->context, eglGetError());
    }
    vSyncNeeded = ctxInfo->vSyncRequested && dInfo->onScreen;
    if (vSyncNeeded == ctxInfo->state.vSyncEnabled) {
        return;
    }

    return;

}

