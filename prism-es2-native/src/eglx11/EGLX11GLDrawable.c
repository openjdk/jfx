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
#include "com_sun_prism_es2_EGLX11GLContext.h"

/*
 * Class:     com_sun_prism_es2_EGLX11GLDrawable
 * Method:    nCreateDrawable
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_EGLX11GLDrawable_nCreateDrawable
(JNIEnv *env, jclass jeglx11Drawable, jlong nativeWindow, jlong nativePFInfo) {
    return 0;
}

/*
 * Class:     com_sun_prism_es2_EGLX11GLDrawable
 * Method:    nGetDummyDrawable
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_EGLX11GLDrawable_nGetDummyDrawable
(JNIEnv *env, jclass jeglx11Drawable, jlong nativePFInfo) {
    return 0;
}

/*
 * Class:     com_sun_prism_es2_EGLX11GLDrawable
 * Method:    nSwapBuffers
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_es2_EGLX11GLDrawable_nSwapBuffers
(JNIEnv *env, jclass jeglx11Drawable, jlong nativeDInfo) {
    return 0;
}
