/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 
#include "LensCommon.h"

static jobject createJavaScreen(JNIEnv *env, NativeScreen screen) {
    // already allocated globally, leaving here only for reference
    //jclass jScreenClass = (*env)->FindClass(env, "com/sun/glass/ui/Screen");

    jmethodID screenInit = (*env)->GetMethodID(env, jScreenClass, 
        "<init>", 
        "(JIIIIIIIIIIIF)V");
    GLASS_CHECK_EXCEPTION(env);

    if (!screenInit) {
        glass_throw_exception_by_name(env, glass_RuntimeException,"missing Screen()");
        return NULL ;
    }

    jobject newScreen = (jobject)(*env)->NewObject(env, jScreenClass, screenInit,
        ptr_to_jlong(screen),

        screen->depth,

        screen->x,
        screen->y,
        screen->width,
        screen->height,

        screen->visibleX,
        screen->visibleY,
        screen->visibleWidth,
        screen->visibleHeight,

        screen->resolutionX,
        screen->resolutionY,

        1.0f);
    GLASS_CHECK_EXCEPTION(env);

    return newScreen;
}

jobjectArray createJavaScreens(JNIEnv *env) {
    // Update the Java notion of our Screens[]


    // create our one Screen object
    jobject defScreen = createJavaScreen(env, glass_screen_getMainScreen());
    if (defScreen == NULL) {
        glass_throw_exception_by_name(env, glass_RuntimeException,"failed to create default Screen");
        return NULL;
    }

    // create our Screen[]
    // with only one element because we know that is all we currently support
    int screenCount = 1;
    jobjectArray screenArray = (*env)->NewObjectArray(env, 
            screenCount, 
            jScreenClass, 
            NULL);
    (*env)->SetObjectArrayElement(env, screenArray, 0, defScreen);

    return screenArray;
}

