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

#include "com_sun_glass_ui_monocle_util_C.h"
#include "Monocle.h"

JNIEXPORT jobject JNICALL
 Java_com_sun_glass_ui_monocle_util_C_NewDirectByteBuffer
    (JNIEnv *env, jobject UNUSED(obj), jlong ptr, jint size) {
    return (*env)->NewDirectByteBuffer(env,
            (void *) (unsigned long) ptr, (jlong) size);
}

JNIEXPORT jlong JNICALL
 Java_com_sun_glass_ui_monocle_util_C_GetDirectBufferAddress
    (JNIEnv *env, jobject UNUSED(obj), jobject byteBuffer) {
    return byteBuffer
            ? asJLong((*env)->GetDirectBufferAddress(env, byteBuffer))
            : (jlong) 0l;
}

void monocle_returnInt(JNIEnv *env, jintArray buffer, jint value) {
    if (buffer && (*env)->GetArrayLength(env, buffer) > 0) {
        (*env)->SetIntArrayRegion(env, buffer, 0, 1, &value);
    }
}

void monocle_returnLong(JNIEnv *env, jlongArray buffer, jlong value) {
    if (buffer && (*env)->GetArrayLength(env, buffer) > 0) {
        (*env)->SetLongArrayRegion(env, buffer, 0, 1, &value);
    }
}

