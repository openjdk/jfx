/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

#include "com_sun_webkit_NativeWebView.h" 
#include "symbol.h"

#define LIBANDROID_WEBVIEW_SO "libandroid_webview.so"

static jint(*_ANDROID_create_android_webview)();
static void (*_ANDROID_move_and_resize)(int id, int x, int y, int w, int h);
static void (*_ANDROID_set_visible)(int id, int visible);
static void (*_ANDROID_move_to_top)(int id);
static void (*_ANDROID_load_url)(int id, char *url);
static void (*_ANDROID_dispose)(int id);
static void (*_ANDROID_load_content)(int id, char *content, char *content_type);
static void (*_ANDROID_set_encoding)(int id, char *encoding);

static jclass jNativeWebViewClass;
static jmethodID jNativeWebView_fire_load_event;

static JavaVM *jvm;

void init_functions(JNIEnv *);
void init_ids(JNIEnv *);

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6)) {
        return JNI_ERR; /* JNI version not supported */
    }
    jvm = vm;
    init_ids(env);
    init_functions(env);
    return JNI_VERSION_1_6;
}

void init_functions(JNIEnv *env) {    
    void *libandroid_webview = dlopen(LIBANDROID_WEBVIEW_SO, RTLD_LAZY | RTLD_GLOBAL);
    if (!libandroid_webview) {
        THROW_RUNTIME_EXCEPTION(env, "dlopen error: %s", dlerror());
    }

    _ANDROID_create_android_webview = GET_SYMBOL(env, libandroid_webview, "create_android_webview");
    _ANDROID_move_and_resize = GET_SYMBOL(env, libandroid_webview, "move_and_resize");
    _ANDROID_set_visible = GET_SYMBOL(env, libandroid_webview, "set_visible");
    _ANDROID_move_to_top = GET_SYMBOL(env, libandroid_webview, "move_to_top");
    _ANDROID_load_url = GET_SYMBOL(env, libandroid_webview, "load_url");
    _ANDROID_load_content = GET_SYMBOL(env, libandroid_webview, "load_content");
    _ANDROID_dispose = GET_SYMBOL(env, libandroid_webview, "dispose");
    _ANDROID_set_encoding = GET_SYMBOL(env, libandroid_webview, "set_encoding");
}

void init_ids(JNIEnv *env) {
    jNativeWebViewClass = (*env)->NewGlobalRef(env,
            (*env)->FindClass(env, "com/sun/webkit/NativeWebView"));
    CHECK_EXCEPTION(env);

    jNativeWebView_fire_load_event = (*env)->GetStaticMethodID(env, jNativeWebViewClass,
            "fire_load_event", "(IIILjava/lang/String;Ljava/lang/String;II)V");
    CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_webkit_NativeWebView
 * Method:    _moveAndResize
 * Signature: (IIIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_webkit_NativeWebView__1moveAndResize
(JNIEnv *env, jobject view, jint id, jint x, jint y, jint w, jint h) {
    (*_ANDROID_move_and_resize)(id, x, y, w, h);
}

/*
 * Class:     com_sun_webkit_NativeWebView
 * Method:    _setVisible
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_webkit_NativeWebView__1setVisible
(JNIEnv *env, jobject view, jint id, jboolean visible) {
    (*_ANDROID_set_visible)(id, visible);
}

/*
 * Class:     com_sun_webkit_NativeWebView
 * Method:    _createAndroidWebView
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_webkit_NativeWebView__1createAndroidWebView
(JNIEnv *env, jobject view) {
    return (*_ANDROID_create_android_webview)();
}

/*
 * Class:     com_sun_webkit_NativeWebView
 * Method:    _moveToTop
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_webkit_NativeWebView__1moveToTop
(JNIEnv *env, jobject view, jint id) {
    (*_ANDROID_move_to_top)(id);
}

/*
 * Class:     com_sun_webkit_NativeWebView
 * Method:    _loadUrl
 * Signature: (ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_sun_webkit_NativeWebView__1loadUrl
(JNIEnv *env, jobject view, jint id, jstring jurl) {
    if (!jurl) {
        return;
    }
    char *curl = (char *)(*env)->GetStringUTFChars(env, jurl, JNI_FALSE);
    (*_ANDROID_load_url)(id, curl);
    (*env)->ReleaseStringUTFChars(env, jurl, curl);
}

/*
 * Class:     com_sun_webkit_NativeWebView
 * Method:    _dispose
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_webkit_NativeWebView__1dispose
(JNIEnv *env, jobject view, jint id) {
    (*_ANDROID_dispose)(id);
}

/*
 * Class:     com_sun_webkit_NativeWebView
 * Method:    _loadContent
 * Signature: (ILjava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_sun_webkit_NativeWebView__1loadContent
(JNIEnv *env, jobject view, jint id, jstring jcontent, jstring jcontentType) {
    if (!jcontent || !jcontentType) {
        return;
    }
    char *content = (char *)(*env)->GetStringUTFChars(env, jcontent, JNI_FALSE);
    char *content_type = (char *)(*env)->GetStringUTFChars(env, jcontentType, JNI_FALSE);
    (*_ANDROID_load_content)(id, content, content_type);
    (*env)->ReleaseStringUTFChars(env, jcontent, content);
    (*env)->ReleaseStringUTFChars(env, jcontentType, content_type);
}

/*
 * Class:     com_sun_webkit_NativeWebView
 * Method:    _setEncoding
 * Signature: (ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_sun_webkit_NativeWebView__1setEncoding
(JNIEnv *env, jobject view, jint id, jstring encoding) {
    (*_ANDROID_set_encoding)(id, encoding);
}


//******************************************************************************
//        ANDROID -> VM
//******************************************************************************

void fire_load_event(int id, int frameID, int state, char *url, char *content_type,
        int progress, int error_code) {
    JNIEnv *env;
    (*jvm)->AttachCurrentThread(jvm, &env, 0);
    jstring jurl = (*env)->NewStringUTF(env, url);
    jstring jcontentType = (*env)->NewStringUTF(env, content_type);
    CHECK_EXCEPTION(env);
    (*env)->CallStaticVoidMethod(env, jNativeWebViewClass,
            jNativeWebView_fire_load_event,
            id, frameID, state, jurl, jcontentType, progress, error_code);
    CHECK_EXCEPTION(env);    
}
