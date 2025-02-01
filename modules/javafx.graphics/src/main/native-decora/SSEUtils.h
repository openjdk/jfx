/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _Included_SSEUtils
#define _Included_SSEUtils

#include <stddef.h>
#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#define FVAL_A   3
#define FVAL_R   0
#define FVAL_G   1
#define FVAL_B   2

#ifndef INT_MAX
#define INT_MAX 2147483647
#endif /* INT_MAX */

void lsample(jint *img,
             jfloat floc_x, jfloat floc_y,
             jint w, jint h, jint scan,
             jfloat *fvals);

void laccumsample(jint *img,
                  jfloat floc_x, jfloat floc_y,
                  jint w, jint h, jint scan,
                  jfloat factor, jfloat *fvals);

void fsample(jfloat *img,
             jfloat floc_x, jfloat floc_y,
             jint w, jint h, jint scan,
             jfloat *fvals);

bool checkRange(JNIEnv *env,
                jintArray dstPixels_arr, jint dstw, jint dsth,
                jintArray srcPixels_arr, jint srcw, jint srch);

#ifdef __cplusplus
};
#endif /* __cplusplus */

#endif /* _Included_SSEUtils */
