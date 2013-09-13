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

extern pthread_key_t GlassThreadDataKey;

extern CVDisplayLinkRef GlassDisplayLink;

extern JavaVM *jVM;
extern JNIEnv *jEnv;

extern jclass jApplicationClass;
extern jclass jWindowClass;
extern jclass jViewClass;

extern jclass jScreenClass;
extern jclass jMenuBarDelegateClass;
extern jclass jIntegerClass;
extern jclass jLongClass;
extern jclass jMapClass;
extern jclass jBooleanClass;

extern jmethodID jRunnableRun;

extern jmethodID jWindowNotifyMove;
extern jmethodID jWindowNotifyResize;
extern jmethodID jWindowNotifyClose;
extern jmethodID jWindowNotifyMoveToAnotherScreen;
extern jmethodID jWindowNotifyFocus;
extern jmethodID jWindowNotifyFocusUngrab;
extern jmethodID jWindowNotifyFocusDisabled;
extern jmethodID jWindowNotifyDestroy;
extern jmethodID jWindowNotifyDelegatePtr;
extern jmethodID jWindowNotifyInitAccessibilityPtr;

extern jmethodID jViewNotifyEvent;
extern jmethodID jViewNotifyRepaint;
extern jmethodID jViewNotifyResize;
extern jmethodID jViewNotifyKey;
extern jmethodID jViewNotifyMouse;
extern jmethodID jViewNotifyMenu;
extern jmethodID jViewNotifyInputMethod;
extern jmethodID jViewNotifyInputMethodMac;
extern jmethodID jViewNotifyInputMethodCandidatePosRequest;
extern jmethodID jViewNotifyDragEnter;
extern jmethodID jViewNotifyDragOver;
extern jmethodID jViewNotifyDragLeave;
extern jmethodID jViewNotifyDragDrop;
extern jmethodID jViewNotifyDragEnd;

extern jmethodID jScreenNotifySettingsChanged;

extern jmethodID jMapGetMethod;
extern jmethodID jBooleanValueMethod;
extern jmethodID jIntegerInitMethod;
extern jmethodID jIntegerValueMethod;
extern jmethodID jLongValueMethod;

extern jmethodID jSizeInit;

extern jmethodID jPixelsAttachData;

typedef struct _tagJavaIDs {
    struct {
        jmethodID init;
        jmethodID enter;
        jmethodID leave;
    } EventLoop;
    struct {
        jmethodID add;
    } List;
    struct {
        jmethodID init;
    } ArrayList;
    struct {
        jmethodID init;
    } MacFileNSURL;
    struct {
        jmethodID isFileNSURLEnabled;
    } MacCommonDialogs;
    struct {
        jmethodID init;
    } File;
    struct {
        jmethodID createPixels;
        jmethodID getScaleFactor;
        jmethodID reportException;
    } Application;
    struct {
        jmethodID rotateGesturePerformed;
        jmethodID scrollGesturePerformed;
        jmethodID swipeGesturePerformed;
        jmethodID magnifyGesturePerformed;
        jmethodID gestureFinished;
        jmethodID notifyBeginTouchEvent;
        jmethodID notifyNextTouchEvent;
        jmethodID notifyEndTouchEvent;
    } GestureSupport;
    struct {
        jmethodID getDescription;
        jmethodID extensionsToArray;
    } ExtensionFilter;
    struct {
        jmethodID init;
    } FileChooserResult;
} JavaIDs;
extern JavaIDs javaIDs;

void initJavaIDsList(JNIEnv* env);
void initJavaIDsArrayList(JNIEnv* env);
void initJavaIDsFile(JNIEnv* env);


