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
#include "com_sun_glass_ui_lens_LensScreen.h"

/*
 * Class:     com_sun_glass_ui_lens_LensScreen
 * Method:    _getMainScreen
 * Signature: (Lcom/sun/glass/ui/Screen;)Lcom/sun/glass/ui/Screen;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_lens_LensScreen__1getMainScreen
(JNIEnv *env, jclass clazz, jobject jscreen) {
    NativeScreen screen;
    jclass jScreenClass = (*env)->FindClass(env, "com/sun/glass/ui/Screen");

    screen = glass_screen_getMainScreen();
    GLASS_LOG_FINE("screen=%p", screen);

    if (jscreen != NULL) {
        (*env)->SetLongField(env, jscreen,
                             (*env)->GetFieldID(env, jScreenClass, "ptr", "J"),
                             ptr_to_jlong(screen));
        (*env)->SetIntField(env, jscreen,
                            (*env)->GetFieldID(env, jScreenClass, "depth", "I"),
                            screen->depth);
        (*env)->SetIntField(env, jscreen,
                            (*env)->GetFieldID(env, jScreenClass, "x", "I"),
                            screen->x);
        (*env)->SetIntField(env, jscreen,
                            (*env)->GetFieldID(env, jScreenClass, "y", "I"),
                            screen->y);
        (*env)->SetIntField(env, jscreen,
                            (*env)->GetFieldID(env, jScreenClass, "width", "I"),
                            screen->width);
        (*env)->SetIntField(env, jscreen,
                            (*env)->GetFieldID(env, jScreenClass, "height", "I"),
                            screen->height);

        (*env)->SetIntField(env, jscreen,
                            (*env)->GetFieldID(env, jScreenClass, "visibleX", "I"),
                            screen->visibleX);
        (*env)->SetIntField(env, jscreen,
                            (*env)->GetFieldID(env, jScreenClass, "visibleY", "I"),
                            screen->visibleY);
        (*env)->SetIntField(env, jscreen,
                            (*env)->GetFieldID(env, jScreenClass, "visibleWidth", "I"),
                            screen->visibleWidth);
        (*env)->SetIntField(env, jscreen,
                            (*env)->GetFieldID(env, jScreenClass, "visibleHeight", "I"),
                            screen->visibleHeight);

        (*env)->SetFloatField(env, jscreen,
                              (*env)->GetFieldID(env, jScreenClass, "scale", "F"), 1.0);
        (*env)->SetIntField(env, jscreen,
                            (*env)->GetFieldID(env, jScreenClass, "resolutionX", "I"),
                            screen->resolutionX);
        (*env)->SetIntField(env, jscreen,
                            (*env)->GetFieldID(env, jScreenClass, "resolutionY", "I"),
                            screen->resolutionY);
        GLASS_CHECK_EXCEPTION(env);
    } else {
        GLASS_LOG_SEVERE("Failed to allocate screen");
    }

    return jscreen;
}

