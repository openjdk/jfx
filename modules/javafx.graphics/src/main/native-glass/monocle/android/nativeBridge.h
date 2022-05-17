/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <android/native_window_jni.h>
#include <android/log.h>

// the following functions can be invoked by native code invoked by Android Activity code
void androidJfx_setNativeWindow(ANativeWindow* nativeWindow);
void androidJfx_setDensity(float nativeDensity);
void androidJfx_gotTouchEvent (int count, int* actions, int* ids, int* xs, int* ys, int primary);
void androidJfx_requestGlassToRedraw();

#define GLASS_LOG_INFO(...)  ((void)__android_log_print(ANDROID_LOG_INFO,"GLASS", __VA_ARGS__))
#define GLASS_LOG_FINE(...)  ((void)__android_log_print(ANDROID_LOG_INFO,"GLASS", __VA_ARGS__))
#define GLASS_LOG_FINEST(...)  ((void)__android_log_print(ANDROID_LOG_INFO,"GLASS", __VA_ARGS__))
#define GLASS_LOG_WARNING(...)  ((void)__android_log_print(ANDROID_LOG_INFO,"GLASS", __VA_ARGS__))

static const jint SCREEN_DPI = 100;

ANativeWindow* android_getNativeWindow(JNIEnv *env);
jfloat android_getDensity(JNIEnv *env);


