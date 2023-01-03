/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <stdio.h>
#include <stdlib.h>
#include <jni.h>

#define jlong_to_ptr(value) (intptr_t)value
#define ptr_to_jlong(value) (jlong)((intptr_t)value)

extern JavaVM *jVM;
extern JNIEnv *jEnv;

// Misc utils - hidden in GlassApplication
extern char * mat_RuntimeException;
void mat_JNU_ThrowByName(JNIEnv *env, const char *name, const char *msg);

// JNI handles
extern jmethodID mat_jRunnableRun;

extern jclass mat_jIntegerClass;
extern jclass mat_jMapClass;
extern jclass mat_jBooleanClass;
extern jclass mat_jLongClass;
extern jmethodID mat_jVectorAddElement;

extern jclass mat_jScreenClass;

extern jclass mat_jViewClass;

extern jmethodID mat_jViewNotifyResize;
extern jmethodID mat_jViewNotifyRepaint;
extern jmethodID mat_jViewNotifyKey;
extern jmethodID mat_jViewNotifyMouse;
extern jmethodID mat_jViewNotifyMenu;
extern jmethodID mat_jViewNotifyInputMethod;
extern jmethodID mat_jViewNotifyView;

extern jmethodID mat_jMapGetMethod;
extern jmethodID mat_jBooleanValueMethod;
extern jmethodID mat_jLongValueMethod;
extern jmethodID mat_jIntegerValueMethod;
extern jmethodID mat_jListAddElement;

extern jfieldID mat_jViewWindow;
extern jfieldID mat_jViewWidth;
extern jfieldID mat_jViewHeight;

extern jfieldID mat_jViewPtr;

extern jclass mat_jWindowClass;

extern jfieldID mat_jWindowX;
extern jfieldID mat_jWindowY;
extern jfieldID mat_jWindowWidth;
extern jfieldID mat_jWindowHeight;
extern jfieldID mat_jWindowView;
extern jfieldID mat_jWindowPtr;

extern jmethodID mat_jWindowNotifyClose;
extern jmethodID mat_jWindowNotifyDestroy;
extern jmethodID mat_jWindowNotifyFocus;
extern jmethodID mat_jWindowNotifyFocusDisabled;
extern jmethodID jWindowNotifyFocusUngrab;
extern jmethodID mat_jWindowNotifyMove;
extern jmethodID mat_jWindowNotifyMoveToAnotherScreen;
extern jmethodID mat_jWindowNotifyResize;

extern jclass mat_jPixelsClass;

extern jfieldID mat_jPixelsWidth;
extern jfieldID mat_jPixelsHeight;
extern jfieldID mat_jPixelsBytes;
extern jfieldID mat_jPixelsInts;

extern jmethodID mat_jPixelsAttachData;

extern jclass mat_jCursorClass;

extern jclass jApplicationClass;
extern jmethodID jApplicationReportException;

