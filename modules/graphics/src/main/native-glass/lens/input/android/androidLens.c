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

#include "wm/LensWindowManager.h"
#include "com_sun_glass_ui_lens_LensApplication.h"
#include "LensCommon.h"
#include "com_sun_glass_events_TouchEvent.h"
#include "com_sun_glass_events_MouseEvent.h"
#include "com_sun_glass_events_KeyEvent.h"
#include "androidLens.h"
#include "androidInput.h"

#ifdef DALVIK_VM
    #define ATTACH_JNI_THREAD()
    #define DETACH_JNI_THREAD()
    JNIEnv *env;
#else
    #define ATTACH_JNI_THREAD()  \
        JNIEnv *env;                                                        \
        JavaVM *vm = glass_application_GetVM();                             \
        if (!vm) return;                                                    \
        (*vm)->AttachCurrentThreadAsDaemon(vm, (JNIEnv **) &env, NULL);     

    #define DETACH_JNI_THREAD()  \
        (*vm)->DetachCurrentThread(vm);
#endif

jboolean lens_input_initialize(JNIEnv *_env) {    
    uint32_t flags = 0;
#ifdef DALVIK_VM
    if (!env) {
        env = _env;
    } 
#endif    
    flags |= 1 << com_sun_glass_ui_lens_LensApplication_DEVICE_MULTITOUCH;
    glass_application_notifyDeviceEvent(_env, flags, 1);
    return JNI_TRUE;
}

void lens_input_shutdown() {
    android_shutdown();    
}

void notifyWindowEvent_resize(
        ANativeWindow *window,
        int eventType,
        int width,
        int height) {
   
   ATTACH_JNI_THREAD();
   glass_application_notifyWindowEvent_resize(env,
        window,
        eventType,
        width,
        height);
   DETACH_JNI_THREAD();
}

void notifyTouchEvent(
        int  state,
        int  id,
        int  sendAlsoButtonEvent,
        int  xabs,
        int  yabs) {
    
   ATTACH_JNI_THREAD();
   jlong jlid = id;
   lens_wm_notifyMultiTouchEvent(env,
           1,
           &state,
           &jlid,
           &xabs,
           &yabs);
                   
   if (sendAlsoButtonEvent) {
        lens_wm_notifyButtonEvent(env,
                (state == com_sun_glass_events_TouchEvent_TOUCH_PRESSED),
                com_sun_glass_events_MouseEvent_BUTTON_LEFT,
                xabs,
                yabs);
   }
   DETACH_JNI_THREAD();
}

void notifyMultiTouchEvent(
        int count,
        int *states,
        int *ids,
        int *xs,
        int *ys) {
    
    ATTACH_JNI_THREAD();
    jlong jids[count];
    
    for(int i=0;i<count;i++) jids[i] = ids[i];
    lens_wm_notifyMultiTouchEvent(env,
           count,
           states,
           jids,
           xs,
           ys);
    DETACH_JNI_THREAD();
}

void notifyMotionEvent(
        int mousePosX,
        int mousePosY,
        int isTouch,
        int touchId) {
   
   ATTACH_JNI_THREAD();
   lens_wm_notifyMotionEvent(env,
           mousePosX,
           mousePosY,
           isTouch,
           touchId);
   DETACH_JNI_THREAD();
}

void notifyButtonEvent(
        int pressed,
        int button,
        int xabs, int yabs) {
   
   ATTACH_JNI_THREAD();
   lens_wm_notifyButtonEvent(env,
           pressed,
           button,
           xabs,
           yabs);
   DETACH_JNI_THREAD();
}

void notifyKeyEvent(
        int eventType,
        int platformKeycode,
        int isRepeatEvent) {
    
   ATTACH_JNI_THREAD();
   NativeWindow window = glass_window_getFocusedWindow();
   if (!window) {
       GLASS_LOG_FINE("Doesn't get focused window. Terminate notifying key event.");
       return;
   }
   int jfxKeyCode = glass_inputEvents_getJavaKeycodeFromPlatformKeyCode(platformKeycode);
   glass_application_notifyKeyEvent(env,
           window,
           eventType,
           jfxKeyCode,
           isRepeatEvent);
   DETACH_JNI_THREAD();
}

#endif