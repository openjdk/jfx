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
#include "com_sun_prism_es2_MacGLPixelFormat.h"

/*
 * Class:     com_sun_prism_es2_MacGLPixelFormat
 * Method:    nCreatePixelFormat
 * Signature: (J[I)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_MacGLPixelFormat_nCreatePixelFormat
(JNIEnv *env, jclass class, jlong nativeScreen, jintArray attrArr) {
    jint *attrs;
    jlong pixelFormat = 0;
    PixelFormatInfo *pfInfo = NULL;

    if (attrArr == NULL) {
        return 0;
    }

    attrs = (*env)->GetIntArrayElements(env, attrArr, NULL);
    pixelFormat = (jlong) (intptr_t) createPixelFormat(attrs);
    (*env)->ReleaseIntArrayElements(env, attrArr, attrs, JNI_ABORT);

    /* allocate the structure */
    pfInfo = (PixelFormatInfo *) malloc(sizeof (PixelFormatInfo));
    if (pfInfo == NULL) {
        fprintf(stderr, "nCreatePixelFormat: Failed in malloc\n");
    }

    /* initialize the structure */
    initializePixelFormatInfo(pfInfo);
    pfInfo->pixelFormat = pixelFormat;

    return ptr_to_jlong(pfInfo);
}
