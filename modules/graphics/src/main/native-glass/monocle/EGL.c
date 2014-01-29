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

#include <EGL/egl.h>

#include "com_sun_glass_ui_monocle_EGL.h"
#include "Monocle.h"

#include <stdlib.h>


void setEGLAttrs(jint *attrs, int *eglAttrs) {
    int index = 0;

    eglAttrs[index++] = EGL_SURFACE_TYPE;
    if (attrs[6] != 0) {
        eglAttrs[index++] = (EGL_WINDOW_BIT);
    } else {
        eglAttrs[index++] = EGL_PBUFFER_BIT;
    }

    // TODO:  We are depending on the order of attributes defined in
    // GLPixelFormat - we need a better way to manage this

    if (attrs[0] == 5 && attrs[1] == 6
            && attrs[2] == 5 && attrs[3] == 0) {
        // Optimization for Raspberry Pi model B. Even though the result
        // of setting EGL_BUFFER_SIZE to 16 should be the same as setting
        // component sizes separately, we get less per-frame overhead if we
        // only set EGL_BUFFER_SIZE.
        eglAttrs[index++] = EGL_BUFFER_SIZE;
        eglAttrs[index++] = 16;
    } else {
        eglAttrs[index++] = EGL_RED_SIZE;
        eglAttrs[index++] = attrs[0];
        eglAttrs[index++] = EGL_GREEN_SIZE;
        eglAttrs[index++] = attrs[1];
        eglAttrs[index++] = EGL_BLUE_SIZE;
        eglAttrs[index++] = attrs[2];
        eglAttrs[index++] = EGL_ALPHA_SIZE;
        eglAttrs[index++] = attrs[3];
    }

    eglAttrs[index++] = EGL_DEPTH_SIZE;
    eglAttrs[index++] = attrs[4];
    eglAttrs[index++] = EGL_RENDERABLE_TYPE;
    eglAttrs[index++] = EGL_OPENGL_ES2_BIT;
    eglAttrs[index] = EGL_NONE;
}

JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_monocle_EGL_eglGetDisplay
    (JNIEnv *env, jclass clazz, jlong display) {
    EGLDisplay dpy = eglGetDisplay(display);
    return asJLong(dpy);
}

JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_monocle_EGL_eglInitialize
    (JNIEnv *env, jclass clazz, jlong eglDisplay, jintArray majorArray,
     jintArray minorArray){

    EGLint major, minor;
    if (!eglInitialize(asPtr(eglDisplay), &major, &minor)) {
         (*env)->SetIntArrayRegion(env, majorArray, 0, 1, &major);
         (*env)->SetIntArrayRegion(env, minorArray, 0, 1, &minor);
        return JNI_FALSE;
    } else {
        return JNI_TRUE;
    }
}

JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_monocle_EGL_eglBindAPI
    (JNIEnv *env, jclass clazz, jint api) {

    if (eglBindAPI(api)) {
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_monocle_EGL_eglChooseConfig
    (JNIEnv *env, jclass clazz, jlong eglDisplay, jintArray attribs,
     jlongArray configs, jint configSize, jintArray numConfigs) {

    int i=0;

    int eglAttrs[50]; /* value, attr pair plus a None */
    jint *attrArray;

    attrArray = (*env)->GetIntArrayElements(env, attribs, JNI_FALSE);
    setEGLAttrs(attrArray, eglAttrs);
    (*env)->ReleaseIntArrayElements(env, attribs, attrArray, JNI_ABORT);
    EGLConfig *configArray = malloc(sizeof(EGLConfig) * configSize);
    jlong *longConfigArray = malloc(sizeof(long) * configSize);
    EGLint numConfigPtr=5;
    jboolean retval;

    if (!eglChooseConfig(asPtr(eglDisplay), eglAttrs, configArray, configSize,
                               &numConfigPtr)) {
        retval = JNI_FALSE;
    } else {
        retval = JNI_TRUE;
    }


    (*env)->SetIntArrayRegion(env, numConfigs, 0, 1, &numConfigPtr);
    for (i = 0; i < numConfigPtr; i++) {
        longConfigArray[i] = asJLong(configArray[i]);
        //printf("i is %d\n", i);
    }

    (*env)->SetLongArrayRegion(env, configs, 0, configSize, longConfigArray);
    free(configArray);
    free(longConfigArray);
    return retval;
}

JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_monocle_EGL__1eglCreateWindowSurface
    (JNIEnv *env, jclass clazz, jlong eglDisplay, jlong config,
     jlong nativeWindow, jintArray attribs) {

    EGLSurface eglSurface;
    EGLint *attrArray = NULL;

    if (attribs != NULL)
        attrArray = (*env)->GetIntArrayElements(env, attribs, JNI_FALSE);

    eglSurface =  eglCreateWindowSurface(asPtr(eglDisplay), asPtr(config),
                                               asPtr(nativeWindow),
                                               NULL);
    if (attrArray != NULL) {
        (*env)->ReleaseIntArrayElements(env, attribs, attrArray, JNI_ABORT);
    }
    return asJLong(eglSurface);
}

JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_monocle_EGL_eglCreateContext
    (JNIEnv *env, jclass clazz, jlong eglDisplay, jlong config,
      jlong shareContext, jintArray attribs){

    // we don't support any passed-in context attributes presently
    // we don't support any share context presently
    EGLint contextAttrs[] = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE};
    EGLContext context = eglCreateContext(asPtr(eglDisplay), asPtr(config),
                                          NULL, contextAttrs);

    if (context == EGL_NO_CONTEXT) {
        fprintf(stderr, "eglCreateContext() failed - %d\n", eglGetError());
        return 0;
    } else {
        return asJLong(context);
    }
}

JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_monocle_EGL_eglMakeCurrent
   (JNIEnv *env, jclass clazz, jlong eglDisplay, jlong drawSurface,
    jlong readSurface, jlong eglContext) {

    if (eglMakeCurrent(asPtr(eglDisplay), asPtr(drawSurface), asPtr(readSurface),
                   asPtr(eglContext))) {
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_monocle_EGL_eglSwapBuffers
    (JNIEnv *env, jclass clazz, jlong eglDisplay, jlong eglSurface) {
    if (eglSwapBuffers(asPtr(eglDisplay), asPtr(eglSurface))) {
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}













