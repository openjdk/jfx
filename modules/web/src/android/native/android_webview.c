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

#include <jni.h>
#include <string.h>
#include "symbol.h"
#include "android_log.h"
#include "android_webview.h"

static jclass jFXActivityClass;
static jclass jInternalWebViewClass;
static jfieldID jInternalWebView_internalID;
static jmethodID jInternalWebView_init;
static jmethodID jInternalWebView_moveAndResize;
static jmethodID jInternalWebView_getInternalID;
static jmethodID jInternalWebView_loadUrl;
static jmethodID jInternalWebView_loadContent;
static jmethodID jInternalWebView_setVisible;
static jmethodID jInternalWebView_dispose;
static jmethodID jInternalWebView_setEncoding;
static jmethodID jFXActivity_getInstance;
static jmethodID jFXActivity_getDataDir;
static JavaVM *jvm;

static void (*_VM_fire_load_event)(int id, int frameId, int state,
        char *url, char *content_type, int progress, int error_code);


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

void init_ids(JNIEnv *env) {
    jInternalWebViewClass = (*env)->NewGlobalRef(env,
            (*env)->FindClass(env, "com/oracle/dalvik/InternalWebView"));
    CHECK_EXCEPTION(env);

    jInternalWebView_init = (*env)->GetMethodID(env, jInternalWebViewClass, "<init>", "()V");
    CHECK_EXCEPTION(env);

    jInternalWebView_internalID = (*env)->GetFieldID(env, jInternalWebViewClass,
            "internalID", "I");
    CHECK_EXCEPTION(env);

    jInternalWebView_getInternalID = (*env)->GetMethodID(env, jInternalWebViewClass,
            "getInternalID", "()I");
    CHECK_EXCEPTION(env);

    jInternalWebView_moveAndResize = (*env)->GetStaticMethodID(env, jInternalWebViewClass,
            "moveAndResize", "(IIIII)V");
    CHECK_EXCEPTION(env);

    jInternalWebView_loadUrl = (*env)->GetStaticMethodID(env, jInternalWebViewClass,
            "loadUrl", "(ILjava/lang/String;)V");
    CHECK_EXCEPTION(env);

    jInternalWebView_loadContent = (*env)->GetStaticMethodID(env, jInternalWebViewClass,
            "loadContent", "(ILjava/lang/String;Ljava/lang/String;)V");
    CHECK_EXCEPTION(env);

    jInternalWebView_setVisible = (*env)->GetStaticMethodID(env, jInternalWebViewClass,
            "setVisible", "(IZ)V");
    CHECK_EXCEPTION(env);
    
    jInternalWebView_dispose = (*env)->GetStaticMethodID(env, jInternalWebViewClass,
            "dispose", "(I)V");
    CHECK_EXCEPTION(env);

    jInternalWebView_setEncoding = (*env)->GetStaticMethodID(env, jInternalWebViewClass,
            "setEncoding", "(ILjava/lang/String;)V");
    CHECK_EXCEPTION(env);

    jFXActivityClass = (*env)->NewGlobalRef(env,
            (*env)->FindClass(env, "com/oracle/dalvik/FXActivity"));
    CHECK_EXCEPTION(env);

    jFXActivity_getInstance = (*env)->GetStaticMethodID(env, jFXActivityClass,
            "getInstance", "()Lcom/oracle/dalvik/FXActivity;");
    CHECK_EXCEPTION(env);

    jFXActivity_getDataDir = (*env)->GetMethodID(env, jFXActivityClass,
            "getDataDir", "()Ljava/lang/String;");
    CHECK_EXCEPTION(env);
}

#define LIBWEBVIEW_SO "libwebview.so"
#define LIB_DIR       "lib"
#define PATH_SEP      "/"

void init_functions(JNIEnv *env) {
    jobject fxactivity = (*env)->CallStaticObjectMethod(env, jFXActivityClass, jFXActivity_getInstance);
    jstring jdatadir = (*env)->CallObjectMethod(env, fxactivity, jFXActivity_getDataDir);
    char *cdatadir = (char*)(*env)->GetStringUTFChars(env, jdatadir, 0);
    int cdatadir_len = (*env)->GetStringUTFLength(env, jdatadir);

    char *fullpath = (char *) calloc(cdatadir_len + strlen(LIBWEBVIEW_SO) + 
                                2 * strlen(PATH_SEP) + strlen(LIB_DIR) + 1, 1);
    strcpy(fullpath, cdatadir);
    strcat(fullpath, PATH_SEP);    
    strcat(fullpath, LIB_DIR);
    strcat(fullpath, PATH_SEP);
    strcat(fullpath, LIBWEBVIEW_SO);

    void *libwebview = dlopen(fullpath, RTLD_LAZY | RTLD_GLOBAL);
    if (!libwebview) {
        THROW_RUNTIME_EXCEPTION(env, "dlopen error: %s", dlerror());
    }

    _VM_fire_load_event = GET_SYMBOL(env, libwebview, "fire_load_event");

    free(fullpath);
}

int create_android_webview() {
    JNIEnv *env;
    int internalID = -1;
    (*jvm)->AttachCurrentThread(jvm, &env, 0);
    jobject jInternalWebView = (*env)->NewGlobalRef(env,
            (*env)->NewObject(env, jInternalWebViewClass, jInternalWebView_init));
    CHECK_EXCEPTION(env);

    internalID = (*env)->CallIntMethod(env, jInternalWebView, jInternalWebView_getInternalID);
    CHECK_EXCEPTION(env);
    
    return internalID;
}

void move_and_resize(int id, int x, int y, int w, int h) {
    JNIEnv *env;
    (*jvm)->AttachCurrentThread(jvm, &env, 0);
    (*env)->CallStaticVoidMethod(env, jInternalWebViewClass,
            jInternalWebView_moveAndResize, id, x, y, w, h);
}

void set_visible(int id, int visible) {
    JNIEnv *env;
    (*jvm)->AttachCurrentThread(jvm, &env, 0);
    (*env)->CallStaticVoidMethod(env, jInternalWebViewClass,
            jInternalWebView_setVisible,
            id, visible == 1 ? JNI_TRUE : JNI_FALSE);
}

void move_to_top(int id) {
    JNIEnv *env;
    (*jvm)->AttachCurrentThread(jvm, &env, 0);
}

void load_url(int id, const char *curl) {
    JNIEnv *env;
    if (!curl) {
        return;
    }
    (*jvm)->AttachCurrentThread(jvm, &env, 0);
    jstring jurl = (*env)->NewStringUTF(env, curl);
    (*env)->CallStaticVoidMethod(env, jInternalWebViewClass, jInternalWebView_loadUrl, id, jurl);
    CHECK_EXCEPTION(env);
}

void load_content(int id, const char *content, const char *content_type) {
    JNIEnv *env;
    if (!content || !content_type) {
        return;
    }
    (*jvm)->AttachCurrentThread(jvm, &env, 0);

    jstring jcontent = (*env)->NewStringUTF(env, content);
    jstring jcontent_type = (*env)->NewStringUTF(env, content_type);
    (*env)->CallStaticVoidMethod(env, jInternalWebViewClass, jInternalWebView_loadContent,
            id, jcontent, jcontent_type);
    CHECK_EXCEPTION(env);
}

void set_encoding(int id, char *encoding) {
    JNIEnv *env;
    (*jvm)->AttachCurrentThread(jvm, &env, 0);
    (*env)->CallStaticVoidMethod(env, jInternalWebViewClass, jInternalWebView_setEncoding,
            id, encoding);
    CHECK_EXCEPTION(env);
}

void dispose(int id) {
    JNIEnv *env;
    (*jvm)->AttachCurrentThread(jvm, &env, 0);
    (*env)->CallStaticVoidMethod(env, jInternalWebViewClass, jInternalWebView_dispose, id);
    CHECK_EXCEPTION(env);
}

//******************************************************************************
//  Android -> VM
//******************************************************************************

/*
 * Class:     com_oracle_dalvik_InternalWebView
 * Method:    _fireLoadEvent
 * Signature: (IIILjava/lang/String;Ljava/lang/String;DI)V
 */
JNIEXPORT void JNICALL Java_com_oracle_dalvik_InternalWebView__1fireLoadEvent
(JNIEnv *env, jobject view, jint id, jint frameID, jint state, jstring url,
        jstring contentType, jint progress, jint errorCode) {
    if (!url || !contentType) {
        return;
    }
    char *curl = (char *)(*env)->GetStringUTFChars(env, url, 0);
    char *ccontent_type = (char *)(*env)->GetStringUTFChars(env, contentType, 0);    

    (*_VM_fire_load_event)(id, frameID, state, curl, ccontent_type, progress, errorCode);
}
