/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
#include <com_sun_glass_ui_gtk_GtkDnDClipboard.h>
#include "glass_general.h"
#include "glass_dnd.h"

extern gboolean is_dnd_owner;
extern "C" {

/*
 * Class:     com_sun_glass_ui_gtk_GtkDnDClipboard
 * Method:    isOwner
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_gtk_GtkDnDClipboard_isOwner
  (JNIEnv *env , jobject obj)
{
    (void)env;
    (void)obj;

    return (is_dnd_owner) ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkDnDClipboard
 * Method:    pushToSystemImpl
 * Signature: (Ljava/util/HashMap;I)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_glass_ui_gtk_GtkDnDClipboard_pushToSystemImpl
  (JNIEnv * env, jobject obj, jobject data, jint supported)
{
    (void)obj;

    return execute_dnd(env, data, supported);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkDnDClipboard
 * Method:    pushTargetActionToSystem
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkDnDClipboard_pushTargetActionToSystem
  (JNIEnv * env, jobject obj, jint action)
{
    (void)env;
    (void)obj;
    (void)action;

    // Never called.
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkDnDClipboard
 * Method:    popFromSystem
 * Signature: (Ljava/lang/String;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_gtk_GtkDnDClipboard_popFromSystem
  (JNIEnv * env, jobject obj, jstring mime)
{
    (void)obj;

    return dnd_target_get_data(env, mime);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkDnDClipboard
 * Method:    supportedSourceActionsFromSystem
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkDnDClipboard_supportedSourceActionsFromSystem
  (JNIEnv *env, jobject obj)
{
    (void)obj;

    return dnd_target_get_supported_actions(env);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkDnDClipboard
 * Method:    mimesFromSystem
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_com_sun_glass_ui_gtk_GtkDnDClipboard_mimesFromSystem
  (JNIEnv * env, jobject obj)
{
    (void)obj;

    return dnd_target_get_mimes(env);
}

} // extern "C"
