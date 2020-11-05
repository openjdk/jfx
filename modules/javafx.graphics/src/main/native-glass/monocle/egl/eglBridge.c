/* Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "com_sun_glass_ui_monocle_EGLAcceleratedScreen.h"
#include "Monocle.h"
#include "egl_ext.h"

JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_monocle_EGLAcceleratedScreen_nPlatformGetNativeWindow
    (JNIEnv *env, jobject obj, jstring cardId) {
    const char *ccid = (*env)->GetStringUTFChars(env, cardId, NULL);
    long answer = getNativeWindowHandle(ccid);
    (*env)->ReleaseStringUTFChars(env, cardId, ccid);
    return (jlong)answer;
}

JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_monocle_EGLAcceleratedScreen_nGetEglDisplayHandle
    (JNIEnv *env, jobject obj) {
    long answer = getEglDisplayHandle();
    return (jlong)answer;
}

JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_monocle_EGLAcceleratedScreen_nEglInitialize
    (JNIEnv *env, jclass clazz, jlong eglDisplay) {
    jboolean answer = doEglInitialize(asPtr(eglDisplay));
    return answer;
}

JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_monocle_EGLAcceleratedScreen_nEglBindApi
    (JNIEnv *env, jclass clazz, jint api) {
    jboolean answer = doEglBindApi((int)api);
    return answer;
}

JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_monocle_EGLAcceleratedScreen_nEglChooseConfig
    (JNIEnv *env, jclass clazz, jlong eglDisplay, jintArray attribs) {
    jint *attrArray = (*env)->GetIntArrayElements(env, attribs, JNI_FALSE);
    if (attrArray == 0) {
        fprintf(stderr, "Fatal error getting int* from int[]\n");
        return -1;
    }
    jlong answer = doEglChooseConfig(eglDisplay, attrArray);
    (*env)->ReleaseIntArrayElements(env, attribs, attrArray, JNI_ABORT);
    return answer;
}

JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_monocle_EGLAcceleratedScreen_nEglCreateWindowSurface
    (JNIEnv *env, jclass clazz, jlong eglDisplay, jlong config, jlong nativeWindow) {
    jlong answer = doEglCreateWindowSurface(eglDisplay, config, nativeWindow);
    return answer;
}

JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_monocle_EGLAcceleratedScreen_nEglCreateContext
 (JNIEnv *UNUSED(env), jclass UNUSED(clazz), jlong eglDisplay, jlong config) {
    jlong answer = doEglCreateContext(eglDisplay, config);
    return answer;
}

JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_monocle_EGLAcceleratedScreen_nEglMakeCurrent
   (JNIEnv *UNUSED(env), jclass UNUSED(clazz), jlong eglDisplay, jlong drawSurface,
    jlong readSurface, jlong eglContext) {
    jlong answer = doEglMakeCurrent(eglDisplay, drawSurface, readSurface, eglContext);
    return answer;
}

JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_monocle_EGLAcceleratedScreen_nEglSwapBuffers
    (JNIEnv *UNUSED(env), jclass UNUSED(clazz), jlong eglDisplay, jlong eglSurface)  {
    jlong answer = doEglSwapBuffers(eglDisplay, eglSurface);
    return answer;
}

