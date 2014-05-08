/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

#import <jni.h>
#import <pthread.h>
#import <QuartzCore/CVDisplayLink.h>
#import "GlassStatics.h"
#import "GlassMacros.h"

pthread_key_t GlassThreadDataKey = 0;

CVDisplayLinkRef GlassDisplayLink = NULL;

JavaVM *jVM = NULL;
JNIEnv *jEnv = NULL;

jclass jApplicationClass = NULL;
jclass jWindowClass = NULL;
jclass jViewClass = NULL;

jclass jScreenClass = NULL;
jclass jMenuBarDelegateClass = NULL;
jclass jIntegerClass = NULL;
jclass jLongClass = NULL;
jclass jMapClass = NULL;
jclass jBooleanClass = NULL;

jmethodID jRunnableRun = NULL;

jmethodID jWindowNotifyMove = NULL;
jmethodID jWindowNotifyResize = NULL;
jmethodID jWindowNotifyClose = NULL;
jmethodID jWindowNotifyMoveToAnotherScreen = NULL;
jmethodID jWindowNotifyFocus = NULL;
jmethodID jWindowNotifyFocusUngrab = NULL;
jmethodID jWindowNotifyFocusDisabled = NULL;
jmethodID jWindowNotifyDestroy = NULL;
jmethodID jWindowNotifyDelegatePtr = NULL;

jmethodID jViewNotifyEvent = NULL;
jmethodID jViewNotifyRepaint = NULL;
jmethodID jViewNotifyResize = NULL;
jmethodID jViewNotifyKey = NULL;
jmethodID jViewNotifyMouse = NULL;
jmethodID jViewNotifyMenu = NULL;
jmethodID jViewNotifyInputMethod = NULL;
jmethodID jViewNotifyInputMethodMac = NULL;
jmethodID jViewNotifyInputMethodCandidatePosRequest = NULL;
jmethodID jViewNotifyDragEnter = NULL;
jmethodID jViewNotifyDragOver = NULL;
jmethodID jViewNotifyDragLeave  = NULL;
jmethodID jViewNotifyDragDrop = NULL;
jmethodID jViewNotifyDragEnd = NULL;
jmethodID jViewGetAccessible = NULL;

jmethodID jScreenNotifySettingsChanged = NULL;

jmethodID jMapGetMethod = NULL;
jmethodID jBooleanValueMethod = NULL;
jmethodID jIntegerInitMethod = NULL;
jmethodID jIntegerValueMethod = NULL;
jmethodID jLongValueMethod = NULL;

jmethodID jSizeInit = NULL;

jmethodID jPixelsAttachData = NULL;

JavaIDs javaIDs;

void initJavaIDsList(JNIEnv* env)
{
    if (!javaIDs.List.add) {
        jclass jcls = (*env)->FindClass(env, "java/util/List");
        GLASS_CHECK_EXCEPTION(env);
        if (jcls) {
            javaIDs.List.add = (*env)->GetMethodID(env, jcls, "add", "(Ljava/lang/Object;)Z");
            GLASS_CHECK_EXCEPTION(env);
        }
    }
}

void initJavaIDsArrayList(JNIEnv* env)
{
    if (!javaIDs.ArrayList.init) {
        jclass jcls = (*env)->FindClass(env, "java/util/ArrayList");
        GLASS_CHECK_EXCEPTION(env);
        if (jcls) {
            javaIDs.ArrayList.init = (*env)->GetMethodID(env, jcls, "<init>", "()V");
            GLASS_CHECK_EXCEPTION(env);
        }
    }
}

void initJavaIDsFile(JNIEnv* env)
{
    if (!javaIDs.File.init) {
        jclass jcls = (*env)->FindClass(env, "java/io/File");
        GLASS_CHECK_EXCEPTION(env);
        if (jcls) {
            javaIDs.File.init = (*env)->GetMethodID(env, jcls, "<init>", "(Ljava/lang/String;)V");
            GLASS_CHECK_EXCEPTION(env);
        }
    }
}

