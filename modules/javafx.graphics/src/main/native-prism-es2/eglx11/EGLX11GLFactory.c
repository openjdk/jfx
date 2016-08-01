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
#include "com_sun_prism_es2_EGLX11GLFactory.h"

/*
 * Class:     com_sun_prism_es2_EGLX11GLFactory
 * Method:    nInitialize
 * Signature: ([I)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_EGLX11GLFactory_nInitialize
(JNIEnv *env, jclass jeglx11GLFactory, jintArray attrArr) {
    printf("In EGLX11GLFactory_nInitialize\n");
    return 0;
}

/*
 * Class:     com_sun_prism_es2_EGLX11GLFactory
 * Method:    nGetAdapterOrdinal
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_EGLX11GLFactory_nGetAdapterOrdinal
(JNIEnv *env, jclass jeglx11GLFactory, jlong nativeScreen) {
    return 0;
}

/*
 * Class:     com_sun_prism_es2_EGLX11GLFactory
 * Method:    nGetAdapterCount
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_EGLX11GLFactory_nGetAdapterCount
(JNIEnv *env, jclass jeglx11GLFactory) {
    return 1;
}

/*
 * Class:     com_sun_prism_es2_EGLX11GLFactory
 * Method:    nGetDefaultScreen
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_EGLX11GLFactory_nGetDefaultScreen
(JNIEnv *env, jclass jeglx11GLFactory, jlong nativeCtxInfo) {
    return 0;
}

/*
 * Class:     com_sun_prism_es2_EGLX11GLFactory
 * Method:    nGetDisplay
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_EGLX11GLFactory_nGetDisplay
(JNIEnv *env, jclass jeglx11GLFactory, jlong nativeCtxInfo) {
    return 0;
}

/*
 * Class:     com_sun_prism_es2_EGLX11GLFactory
 * Method:    nGetVisualID
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_EGLX11GLFactory_nGetVisualID
(JNIEnv *env, jclass jeglx11GLFactory, jlong nativeCtxInfo) {
    return 0;
}

/*
 * Class:     com_sun_prism_es2_EGLX11GLFactory
 * Method:    nSetDebug
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_EGLX11GLFactory_nSetDebug
(JNIEnv *env, jclass jeglx11GLFactory, jboolean on) {

}

