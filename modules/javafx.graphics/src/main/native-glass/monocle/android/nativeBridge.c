/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <sys/types.h>
#include <dlfcn.h>
#include "nativeBridge.h"
#include "Monocle.h"

JNIEnv* javaEnv = NULL;
JavaVM *jVM = NULL;

static jclass jAndroidInputDeviceRegistryClass;
static jclass jMonocleWindowManagerClass;
static jclass jScreenClass;

static jmethodID monocle_gotTouchEventFromNative;
static jmethodID monocle_dispatchKeyEventFromNative;
static jmethodID monocle_repaintAll;
static jmethodID monocle_registerDevice;
static jmethodID screen_init;

ANativeWindow* androidWindow = NULL;
jfloat androidDensity = 0.f;
static int deviceRegistered = 0;

void initializeFromJava (JNIEnv *env) {
    if (jVM != NULL) return; // already have a jVM
    (*env)->GetJavaVM(env, &jVM);
    GLASS_LOG_FINE("Initializing native Android Bridge from Java code");
    jMonocleWindowManagerClass = (*env)->NewGlobalRef(env,
                                                 (*env)->FindClass(env, "com/sun/glass/ui/monocle/MonocleWindowManager"));
    jAndroidInputDeviceRegistryClass = (*env)->NewGlobalRef(env,
                                                 (*env)->FindClass(env, "com/sun/glass/ui/monocle/AndroidInputDeviceRegistry"));
    jScreenClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "com/sun/glass/ui/Screen"));
    monocle_repaintAll = (*env)->GetStaticMethodID(
                                            env, jMonocleWindowManagerClass, "repaintFromNative",
                                            "(Lcom/sun/glass/ui/Screen;)V");
    monocle_gotTouchEventFromNative = (*env)->GetStaticMethodID(
                                            env, jAndroidInputDeviceRegistryClass, "gotTouchEventFromNative",
                                            "(I[I[I[I[II)V");
    monocle_dispatchKeyEventFromNative = (*env)->GetStaticMethodID(
                                            env, jAndroidInputDeviceRegistryClass, "dispatchKeyEventFromNative",
                                            "(II[CI)V");
    monocle_registerDevice = (*env)->GetStaticMethodID(env, jAndroidInputDeviceRegistryClass, "registerDevice","()V");
    screen_init = (*env)->GetMethodID(env, jScreenClass,"<init>", "(JIIIIIIIIIIIIIIIFFFF)V");
    GLASS_LOG_FINE("Initializing native Android Bridge done");
}

void initializeFromNative () {
    if (javaEnv != NULL) return; // already have a JNIEnv
    if (jVM == NULL) {
        GLASS_LOG_FINE("initialize from native can't be done without JVM");
        return; // can't initialize from native before we have a jVM
    }
    GLASS_LOG_FINE("Initializing native Android Bridge from Android/native code");
    jint error = (*jVM)->AttachCurrentThread(jVM, (void **)&javaEnv, NULL);
    if (error != 0) {
        GLASS_LOG_FINE("initializeFromNative failed with error %d\n", error);
    }
}

/* ===== called from native ===== */

void androidJfx_setNativeWindow(ANativeWindow* nativeWindow) {
    initializeFromNative();
    androidWindow = nativeWindow;
    GLASS_LOG_FINE("after androidSetNativeWindow asked, window is %p\n", nativeWindow);
}

void androidJfx_setDensity(float nativeDensity) {
    initializeFromNative();
    androidDensity = nativeDensity;
}

void androidJfx_gotTouchEvent (int count, int* actions, int* ids, int* xs, int* ys, int primary) {
    initializeFromNative();
    GLASS_LOG_FINE("Call InternalSurfaceView_onMultiTouchEventNative");
    if (javaEnv == NULL) {
        GLASS_LOG_FINE("javaEnv still null, not ready to process touch events");
        return;
    }
    if (deviceRegistered == 0) {
        deviceRegistered = 1;
        GLASS_LOG_FINE("This is the first time we have a touch even, register device now");
        (*javaEnv)->CallStaticVoidMethod(javaEnv, jAndroidInputDeviceRegistryClass, monocle_registerDevice);
    }
    jint jcount = (jint)count;
    jlong jlongids[jcount];

    jintArray jactions = (*javaEnv)->NewIntArray(javaEnv, count);
    (*javaEnv)->SetIntArrayRegion(javaEnv, jactions, 0, count, actions);
    jintArray jids = (*javaEnv)->NewIntArray(javaEnv, count);
    (*javaEnv)->SetIntArrayRegion(javaEnv, jids, 0, count, ids);
    jintArray jxs = (*javaEnv)->NewIntArray(javaEnv, count);
    (*javaEnv)->SetIntArrayRegion(javaEnv, jxs, 0, count, xs);
    jintArray jys = (*javaEnv)->NewIntArray(javaEnv, count);
    (*javaEnv)->SetIntArrayRegion(javaEnv, jys, 0, count, ys);

    (*javaEnv)->CallStaticVoidMethod(javaEnv, jAndroidInputDeviceRegistryClass, monocle_gotTouchEventFromNative,
            jcount, jactions, jids, jxs, jys, primary);
}

void androidJfx_gotKeyEvent (int action, int keyCode, jchar* chars, int count, int mods) {
    initializeFromNative();
    if (javaEnv == NULL) {
        GLASS_LOG_FINE("javaEnv still null, not ready to process touch events");
        return;
    }
    if (deviceRegistered == 0) {
        deviceRegistered = 1;
        GLASS_LOG_FINE("This is the first time we have a touch even, register device now");
        (*javaEnv)->CallStaticVoidMethod(javaEnv, jAndroidInputDeviceRegistryClass, monocle_registerDevice);
    }
    jcharArray jchars = (*javaEnv)->NewCharArray(javaEnv, count);
    (*javaEnv)->SetCharArrayRegion(javaEnv, jchars, 0, count, chars);
    (*javaEnv)->CallStaticVoidMethod(javaEnv, jAndroidInputDeviceRegistryClass, monocle_dispatchKeyEventFromNative,
                                     action, keyCode, jchars, mods);
}

void androidJfx_requestGlassToRedraw() {
    GLASS_LOG_FINEST("Native code is notified that surface needs to be redrawn (repaintall)");
    if (jVM == NULL) {
        GLASS_LOG_WARNING("we can't do this yet, no jVM\n");
        return;
    }
    if (javaEnv == NULL) {
        jint error = (*jVM)->AttachCurrentThread(jVM, (void **)&javaEnv, NULL);
        GLASS_LOG_WARNING("result of attach: %d\n",error);
    }
    if (jMonocleWindowManagerClass == NULL) {
        GLASS_LOG_WARNING("we can't do this yet, no jMonocleWindowManagerClass\n");
        return;
    }
    if (monocle_repaintAll == NULL) {
        GLASS_LOG_WARNING("we can't do this yet, no monocle_repaintAll\n");
        return;
    }
    if (androidWindow == NULL) {
        GLASS_LOG_WARNING("we can't do this yet, no androidWindow\n");
        return;
    }
    int32_t width = ANativeWindow_getWidth(androidWindow) / androidDensity;
    int32_t height = ANativeWindow_getHeight(androidWindow) / androidDensity;
    jobject screen = (*javaEnv)->NewObject(javaEnv, jScreenClass, screen_init,
        (jlong) androidWindow, 24,
        0, 0, (jint) width, (jint) height,
        0, 0, (jint) width, (jint) height,
        0, 0, (jint) width, (jint) height,
        SCREEN_DPI, SCREEN_DPI, (jfloat) 1, (jfloat) 1, androidDensity, androidDensity);
    (*javaEnv)->CallStaticVoidMethod(javaEnv, jMonocleWindowManagerClass, monocle_repaintAll, screen);
}

/* ===== called from Java ===== */

ANativeWindow *android_getNativeWindow(JNIEnv *env) {
    initializeFromJava(env);
    return androidWindow;
}

jfloat android_getDensity(JNIEnv *env) {
    initializeFromJava(env);
    return androidDensity;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_android_SoftwareKeyboard__1show
(JNIEnv *env, jclass clazz) {
    initializeFromJava(env);
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_android_SoftwareKeyboard__1hide
(JNIEnv *env, jclass clazz) {
    initializeFromJava(env);
}

JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_monocle_LinuxSystem_dlopen
  (JNIEnv *env, jobject obj, jstring filenameS, jint flag) {
    const char *filename = (*env)->GetStringUTFChars(env, filenameS, NULL);
    GLASS_LOG_FINE("I have to Call dlopen %s\n",filename);
    void *handle = dlopen(filename, RTLD_LAZY | RTLD_GLOBAL);
    GLASS_LOG_FINE("handle = %p\n",handle);
    (*env)->ReleaseStringUTFChars(env, filenameS, filename);
    return asJLong(handle);
}

