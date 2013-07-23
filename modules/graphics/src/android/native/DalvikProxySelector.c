/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
#include <android/log.h>
#include "DalvikProxySelector.h"

static jclass dalvikProxySelectorClass;
static jmethodID getProxyMethodId;

static jclass initDalvikProxySelectorClass(JNIEnv* env) {
    jclass localClass = (*env)->FindClass(env, "com/oracle/dalvik/net/DalvikProxySelector");
    dalvikProxySelectorClass = (jclass) (*env)->NewGlobalRef(env,localClass);
    __android_log_print(3, "DalvikProxySelector", ">>> after FindClass DalvikProxySelectorClass = %x", dalvikProxySelectorClass);
    if ((*env)->ExceptionCheck(env) == JNI_TRUE) {
        __android_log_print(3, "DalvikProxySelector", ">>> initDalvikProxySelectorClass: ExceptionCheck = JNI_TRUE");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
    return dalvikProxySelectorClass;
}

static jmethodID initGetProxyMethod(JNIEnv* env, jclass clazz) {
    getProxyMethodId = (*env)->GetStaticMethodID(env, clazz, "getProxyForURL", "(Ljava/lang/String;)[Ljava/lang/String;");
    __android_log_print(3, "DalvikProxySelector", ">>> getProxyForURL: after GetStaticMethodID");
    if ((*env)->ExceptionCheck(env) == JNI_TRUE) {
        __android_log_print(3, "DalvikProxySelector", ">>> initGetProxyMethod: ExceptionCheck = JNI_TRUE");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
    return getProxyMethodId;
}

void initDalvikProxySelectorData(JNIEnv* env) {
    jclass clazz = initDalvikProxySelectorClass(env);
    initGetProxyMethod(env, clazz);
}

jclass getDalvikProxySelectorClass(void) {
    return dalvikProxySelectorClass;
}

jmethodID getDPSGetProxyMethodID(void) {
    return getProxyMethodId;
}
