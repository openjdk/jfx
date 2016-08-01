/* Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

#include "com_sun_glass_ui_monocle_MX6AcceleratedScreen.h"
#include <EGL/egl.h>

#include <fcntl.h>
#include "Monocle.h"

//Vivante specials
static EGLNativeDisplayType (*wr_fbGetDisplayByIndex)(int DisplayIndex);
static EGLNativeWindowType (*wr_fbCreateWindow)(EGLNativeDisplayType Display, int X, int Y, int Width, int Height);

JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_monocle_MX6AcceleratedScreen__1platformGetNativeDisplay
    (JNIEnv *env, jobject obj, jlong methodHandle) {
    EGLNativeDisplayType cachedDisplay = NULL;

    if (cachedDisplay == NULL) {
        wr_fbGetDisplayByIndex = asPtr(methodHandle);
        cachedDisplay = wr_fbGetDisplayByIndex(0);
    }
    return asJLong(cachedDisplay);
}

JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_monocle_MX6AcceleratedScreen__1platformGetNativeWindow
    (JNIEnv *env, jobject obj, jlong methodHandle, jlong nativeDisplay) {
    NativeWindowType retval;

    wr_fbCreateWindow = asPtr(methodHandle);
    retval = wr_fbCreateWindow(asPtr(nativeDisplay), 0, 0, 0, 0);
    return asJLong(retval);
}