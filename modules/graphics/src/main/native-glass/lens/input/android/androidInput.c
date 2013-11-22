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
#ifdef ANDROID_NDK

#include <dlfcn.h>

#include "androidInput.h"
#include "com_sun_glass_ui_android_SoftwareKeyboard.h"
#include "com_sun_glass_ui_android_Activity.h"

static ANativeWindow* (*_ANDROID_getNativeWindow)();
static void (*_ANDROID_showIME)();
static void (*_ANDROID_hideIME)();
static void (*_ANDROID_shutdown)();
static const char *(*_ANDROID_getDataDir)();


void init_functions(JNIEnv *env) {
    void *libglass = dlopen("libglass_lens_android.so", RTLD_LAZY | RTLD_GLOBAL);
    if (!libglass) {
        THROW_RUNTIME_EXCEPTION(env, "dlopen failed with error: ", dlerror());
    }
    _ANDROID_getNativeWindow = GET_SYMBOL(env, libglass, "ANDROID_getNativeWindow");
    _ANDROID_showIME = GET_SYMBOL(env, libglass, "ANDROID_showIME");
    _ANDROID_hideIME = GET_SYMBOL(env, libglass, "ANDROID_hideIME");
    _ANDROID_shutdown = GET_SYMBOL(env, libglass, "ANDROID_shutdown");
    _ANDROID_getDataDir = GET_SYMBOL(env, libglass, "ANDROID_getDataDir");
}

ANativeWindow *getAndroidNativeWindow() {
    if (!_ANDROID_getNativeWindow) {
        init_functions(0);
    }
    return (*_ANDROID_getNativeWindow)();
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_android_SoftwareKeyboard__1show
(JNIEnv *env, jclass clazz) {
    if (!_ANDROID_showIME) {
        init_functions(env);
    }
    GLASS_LOG_FINE("Show SoftwareKeyboard");
    (*_ANDROID_showIME)();
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_android_SoftwareKeyboard__1hide
(JNIEnv *env, jclass clazz) {
    if (!_ANDROID_hideIME) {
        init_functions(env);
    }
    GLASS_LOG_FINE("Hide SoftwareKeyboard");
    (*_ANDROID_hideIME)();
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_android_Activity__1shutdown
(JNIEnv *env, jclass clazz) {
    android_shutdown();
}

void android_shutdown() {
    if (!_ANDROID_shutdown) {
        init_functions(NULL);
    }
    GLASS_LOG_FINE("Send shutdown");
    (*_ANDROID_shutdown)();
}

const char *android_getDataDir() {
    if (!_ANDROID_getDataDir) {
        init_functions(NULL);
    }
    GLASS_LOG_FINE("Ask for application data dir.");
    return (*_ANDROID_getDataDir)();
}

#endif /* ANDROID_NDK */
