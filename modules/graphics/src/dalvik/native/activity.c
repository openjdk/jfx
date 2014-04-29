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
#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <android/native_window_jni.h>
#include "javafxports_android_FXActivity.h"
#include "EventLoop.h"
#include "logging.h"

#define CHECK_EXCEPTION(env) \
    if ((*env)->ExceptionCheck(env) == JNI_TRUE) {                \
        LOGE("Detected outstanding Java exception at %s:%s:%d\n", \
                __FUNCTION__, __FILE__, __LINE__);                \
        (*env)->ExceptionDescribe(env);                           \
        (*env)->ExceptionClear(env);                              \
    }; 

#define GET_WINDOW_FROM_SURFACE(p, s) \
    (!s) ? NULL : ANativeWindow_fromSurface(p, s)

static JavaVM        *jvm;
static char          *appDataDir;
static ANativeWindow *window;

int32_t    width;
int32_t    height;
int32_t    format;

const char      *android_getDataDir();
ANativeWindow   *android_getNativeWindow();


void eventHandler_process(JNIEnv *, Event e);

EventQ      eventq;

jclass      jFXActivityClass;
jmethodID   jFXActivity_notifyGlassShutdown;
jmethodID   jFXActivity_notifyGlassStartup;
jmethodID   jFXActivity_notifyShowIME;
jmethodID   jFXActivity_notifyHideIME;

/* 
 * prism-es2 gets initialized earlier than glass-lens.
 * We need to provide NativeWindow and application data dir where
 * we can load libraries from.
 */

jint JNI_OnLoad(JavaVM *vm, void *reserved) { 
    LOGV(TAG, "Loading library");
    jvm = vm;
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6)) {
        return JNI_ERR; /* JNI version not supported */
    }
    
    jFXActivityClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "javafxports/android/FXActivity"));
    CHECK_EXCEPTION(env);

    jFXActivity_notifyGlassStartup = (*env)->GetStaticMethodID(env, jFXActivityClass, "notify_glassHasStarted", "()V");
    CHECK_EXCEPTION(env);
    
    jFXActivity_notifyGlassShutdown = (*env)->GetStaticMethodID(env, jFXActivityClass, "notify_glassShutdown", "()V");
    CHECK_EXCEPTION(env);
    
    jFXActivity_notifyShowIME = (*env)->GetStaticMethodID(env, jFXActivityClass, "notify_showIME", "()V");
    CHECK_EXCEPTION(env);
    
    jFXActivity_notifyHideIME = (*env)->GetStaticMethodID(env, jFXActivityClass, "notify_hideIME", "()V");
    CHECK_EXCEPTION(env);
    
    return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *reserved) {
    eventq->stop();    
}

JNIEXPORT void JNICALL Java_javafxports_android_FXActivity__1jfxEventsLoop
  (JNIEnv *env, jobject that) {
    eventq = eventq_getInstance();
    eventq->process = &eventHandler_process;
    eventq->start(env);    
}

/*
 * Class:     javafxports_android_FXActivity
 * Method:    _setDataDir
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_javafxports_android_FXActivity__1setDataDir
  (JNIEnv *env, jobject that, jstring jdir) {    
    const char *cdir = (*env)->GetStringUTFChars(env, jdir, 0);
    int len = strlen(cdir);
    appDataDir = (char *)malloc(len + 1);
    strcpy(appDataDir, cdir);            
    LOGV(TAG, "appDataDir: %s", appDataDir);
}

/*
 * Class:     javafxports_android_FXActivity
 * Method:    _setSurface
 * Signature: (Landroid/view/Surface;)V
 */
JNIEXPORT void JNICALL Java_javafxports_android_FXActivity__1setSurface
  (JNIEnv *env, jobject that, jobject jsurface) {
    window = GET_WINDOW_FROM_SURFACE(env, jsurface);
}

ANativeWindow *android_getNativeWindow() {
    return window;
}

const char *android_getDataDir() {
    return appDataDir;
}

void android_notifyGlassStarted() {
    SignalEvent sevent = createSignalEvent(JFX_SIGNAL_STARTUP);
    eventq->push((Event)sevent); 
}

void android_notifyGlassShutdown() {
    SignalEvent sevent = createSignalEvent(JFX_SIGNAL_SHUTDOWN);
    eventq->push((Event)sevent);
    eventq->stop();    
}

void android_notifyShowIME() {
    SignalEvent sevent = createSignalEvent(JFX_SIGNAL_SHOW_IME);
    eventq->push((Event)sevent);
}

void android_notifyHideIME() {
    SignalEvent sevent = createSignalEvent(JFX_SIGNAL_HIDE_IME);
    eventq->push((Event)sevent);
}


void eventHandler_process(JNIEnv *env, Event e) {    
    if (e->event == JFX_SIGNAL_EVENT) {        
        SignalEvent sevent = (SignalEvent)e;
        if (sevent->type == JFX_SIGNAL_STARTUP) {
            (*env)->CallStaticVoidMethod(env,jFXActivityClass, jFXActivity_notifyGlassStartup);
        } else if (sevent->type == JFX_SIGNAL_SHUTDOWN) {
            (*env)->CallStaticVoidMethod(env,jFXActivityClass, jFXActivity_notifyGlassShutdown);
        } else if (sevent->type == JFX_SIGNAL_SHOW_IME) {
            (*env)->CallStaticVoidMethod(env, jFXActivityClass, jFXActivity_notifyShowIME);
        } else if (sevent->type == JFX_SIGNAL_HIDE_IME) {
            (*env)->CallStaticVoidMethod(env, jFXActivityClass, jFXActivity_notifyHideIME);
        }
    }    
    free(e);
}
