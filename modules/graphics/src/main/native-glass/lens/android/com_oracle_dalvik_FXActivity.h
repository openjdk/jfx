/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

#ifndef _Included_com_oracle_dalvik_FXActivity
#define _Included_com_oracle_dalvik_FXActivity
#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     com_oracle_dalvik_FXActivity
 * Method:    initContext
 * Signature: (Lcom/oracle/dalvik/FXActivity;)V
 */
JNIEXPORT void JNICALL Java_com_oracle_dalvik_FXActivity_initContext
  (JNIEnv *, jobject, jobject);

/*
 * Class:     com_oracle_dalvik_FXActivity
 * Method:    initGlassSymbols
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_oracle_dalvik_FXActivity_initGlassSymbols
  (JNIEnv *, jobject, jstring);

/*
 * Class:     com_oracle_dalvik_FXActivity
 * Method:    _surfaceChanged
 * Signature: (Landroid/view/Surface;)V
 */
JNIEXPORT void JNICALL Java_com_oracle_dalvik_FXActivity__1surfaceChanged__Landroid_view_Surface_2
  (JNIEnv *, jobject, jobject);

/*
 * Class:     com_oracle_dalvik_FXActivity
 * Method:    _surfaceChanged
 * Signature: (Landroid/view/Surface;III)V
 */
JNIEXPORT void JNICALL Java_com_oracle_dalvik_FXActivity__1surfaceChanged__Landroid_view_Surface_2III
  (JNIEnv *, jobject, jobject, jint, jint, jint);

/*
 * Class:     com_oracle_dalvik_FXActivity
 * Method:    _surfaceRedrawNeeded
 * Signature: (Landroid/view/Surface;)V
 */
JNIEXPORT void JNICALL Java_com_oracle_dalvik_FXActivity__1surfaceRedrawNeeded
  (JNIEnv *, jobject, jobject);

#ifdef __cplusplus
}
#endif
#endif
