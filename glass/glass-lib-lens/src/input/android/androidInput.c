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
#ifdef ANDROID_NDK

#include "android/Main.h"
#include "androidInput.h"
#include "input/LensInput.h"
#include "com_sun_glass_events_TouchEvent.h"
#include "com_sun_glass_events_MouseEvent.h"
#include "com_sun_glass_events_KeyEvent.h"
#include "com_sun_glass_ui_lens_LensApplication.h"

#define round(x) ((x)>=0?(int)((x)+0.5):(int)((x)-0.5))

#define WAIT_FOR_EVENTS -1

struct _TouchPoint {
   int x;
   int y;
   int32_t action;
};

typedef struct _TouchPoint *TouchPoint;

JNIEnv *glass_env;

void handle_cmd(struct android_app* app, int32_t cmd) {
    switch (cmd) {
        case APP_CMD_START:
           GLASS_LOG_FINE("APP_CMD_START");
           break;
        case APP_CMD_STOP:
           GLASS_LOG_FINE("APP_CMD_STOP");
            break;
        case APP_CMD_PAUSE:
           GLASS_LOG_FINE("APP_CMD_PAUSE");
            break;
        case APP_CMD_SAVE_STATE:
           GLASS_LOG_FINE("APP_CMD_SAVE_STATE");
            break;
        case APP_CMD_INIT_WINDOW:
           GLASS_LOG_FINE("APP_CMD_INIT_WINDOW");
            break;
        case APP_CMD_TERM_WINDOW:
           GLASS_LOG_FINE("APP_CMD_TERM_WINDOW");
            break;
        case APP_CMD_GAINED_FOCUS:
           GLASS_LOG_FINE("APP_CMD_GAINED_FOCUS");
            break;
        case APP_CMD_LOST_FOCUS:
            GLASS_LOG_FINE("APP_CMD_LOST_FOCUS");
            break;
    }
}

jboolean lens_input_initialize(JNIEnv *env){
   uint32_t flags = 0;
   flags |= 1 << com_sun_glass_ui_lens_LensApplication_DEVICE_MULTITOUCH;
   glass_application_notifyDeviceEvent(env, flags, TRUE);
    return JNI_TRUE;
}

void lens_input_shutdown() {
   if (glass_env) {
       JavaVM *glass_vm = glass_application_GetVM();
       (*glass_vm)->DetachCurrentThread(glass_vm);
   }
}

int32_t translateToLinuxKeyCode(int32_t androidKeyCode) {
    for (int i = 0 ; i < sizeof(keyMap) ; ++i) {
        if (keyMap[i].androidKC == androidKeyCode) {
            return keyMap[i].linuxKC;
        }
    }
    return KEY_RESERVED;
}

TouchPoint getTouchPoint(TouchPoint tp, AInputEvent *event, int pindex) {
   tp->x = round(AMotionEvent_getX(event, pindex));
   tp->y = round(AMotionEvent_getY(event, pindex));
   tp->action = AMotionEvent_getAction(event) & AMOTION_EVENT_ACTION_MASK;
   GLASS_LOG_FINE("TouchPoint [action:%i x:%i y:%i]", tp->action, tp->x, tp->y);
   return tp;
}

JNIEnv *getJNIEnv() {
   if (!glass_env) {
       JavaVM *glass_vm = glass_application_GetVM();
       if (glass_vm == NULL) {
           return NULL;
       }
       (*glass_vm)->AttachCurrentThread(glass_vm, (void **) &glass_env, NULL);
       if (glass_env == NULL) {
           GLASS_LOG_WARNING("Cannot attach native event thread to VM!");
           return NULL;
       }
       GLASS_LOG_FINE("Native event thread attached to VM.");
   }
   return glass_env;
}

void handle_motion_event(AInputEvent* event) {

   JNIEnv *env;
   if (NULL == (env = getJNIEnv())) {
       GLASS_LOG_WARNING("Ignoring event");
       return;
   }

   int32_t pointerIndex = 0;
   size_t  pcount = 1;
   int32_t deviceID = AInputEvent_getDeviceId(event);
   int32_t source = AInputEvent_getSource(event);
   int32_t action = AMotionEvent_getAction(event);
   int32_t actionCode = action & AMOTION_EVENT_ACTION_MASK;
    TouchPoint tp = malloc(sizeof(struct _TouchPoint));

   switch(actionCode) {
       case AMOTION_EVENT_ACTION_CANCEL:
           GLASS_LOG_FINE("Motion Event: Cancel");
           break;
       case AMOTION_EVENT_ACTION_DOWN:
           tp = getTouchPoint(tp, event, 0);
           lens_wm_notifyTouchEvent(env, com_sun_glass_events_TouchEvent_TOUCH_PRESSED, 0, tp->x, tp->y);
           lens_wm_notifyButtonEvent(env, JNI_TRUE, com_sun_glass_events_MouseEvent_BUTTON_LEFT, tp->x, tp->y);
           break;
       case AMOTION_EVENT_ACTION_MOVE:
           tp = getTouchPoint(tp, event, 0);
           lens_wm_notifyMotionEvent(env, tp->x, tp->y, TRUE, 0);
           break;
       case AMOTION_EVENT_ACTION_UP:
           tp = getTouchPoint(tp, event, 0);
           lens_wm_notifyTouchEvent(env, com_sun_glass_events_TouchEvent_TOUCH_RELEASED, 0, tp->x, tp->y);
           lens_wm_notifyButtonEvent(env, JNI_FALSE, com_sun_glass_events_MouseEvent_BUTTON_LEFT, tp->x, tp->y);
           break;
       case AMOTION_EVENT_ACTION_POINTER_DOWN:
       case AMOTION_EVENT_ACTION_POINTER_UP:
           pointerIndex = action & AMOTION_EVENT_ACTION_POINTER_INDEX_MASK;
           pcount = AMotionEvent_getPointerCount(event);
           for(int i = 0; i < pcount; i++) {
               tp = getTouchPoint(tp, event, i);
               if (i == pointerIndex) {
                   /* TODOEnable when there is a support for multitouch in lens.
                   lens_wm_notifyTouchEvent(env,
                           actionCode == AMOTION_EVENT_ACTION_POINTER_DOWN ?
                           com_sun_glass_events_TouchEvent_TOUCH_PRESSED :
                           com_sun_glass_events_TouchEvent_TOUCH_RELEASED,
                           i, tp->x, tp->y);
                   */
               } else {
                   /*
                   lens_wm_notifyTouchEvent(env, com_sun_glass_events_TouchEvent_TOUCH_STILL, i, tp->x, tp->y);
                   */
               }
           }//for
           break;
   }//switch

   free(tp);
}

void handle_key_event(AInputEvent *event) {

   JNIEnv *env;
   if (NULL == (env = getJNIEnv())) {
       GLASS_LOG_WARNING("Ignoring event");
       return;
   }

    int32_t deviceID = AInputEvent_getDeviceId(event);
   int32_t source = AInputEvent_getSource(event);
   int32_t action = AKeyEvent_getAction(event);
   int32_t jfxEventType;
   int32_t jfxKeyCode;
   int32_t keyCode;
   int32_t kcount = 1;
   jboolean isRepeatEvent = JNI_FALSE;

   NativeWindow window = glass_window_getFocusedWindow();

   if (window == NULL) {
       GLASS_LOG_FINE("Skipping event, no focused window");
       return;
   }

   switch (action) {
       case AKEY_EVENT_ACTION_DOWN:
           jfxEventType = com_sun_glass_events_KeyEvent_PRESS;
           keyCode = AKeyEvent_getKeyCode(event);
           GLASS_LOG_FINE("AKEY_EVENT_ACTION_DOWN:[%i]", keyCode);
           jfxKeyCode = glass_inputEvents_getJavaKeycodeFromPlatformKeyCode(
                            translateToLinuxKeyCode(keyCode));
           break;

       case AKEY_EVENT_ACTION_UP:
           jfxEventType = com_sun_glass_events_KeyEvent_RELEASE;
           keyCode = AKeyEvent_getKeyCode(event);
           GLASS_LOG_FINE("AKEY_EVENT_ACTION_UP:[%i]", keyCode);
           jfxKeyCode = glass_inputEvents_getJavaKeycodeFromPlatformKeyCode(
                            translateToLinuxKeyCode(keyCode));
           break;

       case AKEY_EVENT_ACTION_MULTIPLE:
           GLASS_LOG_FINE("AKEY_EVENT_ACTION_MULTIPLE:");
           jfxEventType = com_sun_glass_events_KeyEvent_PRESS;
           isRepeatEvent = JNI_TRUE;
           keyCode = AKeyEvent_getKeyCode(event);
           jfxKeyCode = glass_inputEvents_getJavaKeycodeFromPlatformKeyCode(
                           translateToLinuxKeyCode(keyCode));
           kcount = AKeyEvent_getRepeatCount(event);
           break;

       default:
           GLASS_LOG_FINE("Skipping event, unsupported event[%d]", action);
           break;
   }

   GLASS_LOG_FINEST("Notifying key event on windows %d[%p] - "
                            "event type %d, key code %d, is repeat?%s",
                            window->id, window, jfxEventType, jfxKeyCode,
                            (isRepeatEvent ? "yes" : "no"));

   if (action != AKEY_EVENT_ACTION_MULTIPLE) {
       glass_application_notifyKeyEvent(env, window, jfxEventType, jfxKeyCode, isRepeatEvent);
   }
}

int32_t handle_input(struct android_app* app, AInputEvent* event) {

   if (AINPUT_EVENT_TYPE_MOTION == AInputEvent_getType(event)) {
       GLASS_LOG_FINE("Got motion input event.");
       handle_motion_event(event);

   } else if (AINPUT_EVENT_TYPE_KEY == AInputEvent_getType(event)) {
       GLASS_LOG_FINE("Got key input event.");
       handle_key_event(event);

   } else {
       GLASS_LOG_WARNING("Unknown event type!");
   }

    return 0;
}

void dvkEventLoop(DvkContext context) {
   int ident;
   int events;
   struct android_poll_source* source;

   context->app->onAppCmd = handle_cmd;
   context->app->onInputEvent = handle_input;

   while(1) {
       GLASS_LOG_FINE( "Native event loop start.");

       while((ident=ALooper_pollAll(WAIT_FOR_EVENTS, NULL, &events,
               (void**)&source)) >= 0) {

           // Process this event.
           if (source != NULL) {
               source->process(context->app, source);
           }

           // Check if we are exiting.
           if (context->app->destroyRequested != 0) {
               GLASS_LOG_FINE("Native event loop end.");
               return;
           }
       }
   }//event loop

void showIme(int32_t flags) {
   DvkContext context = getDvkContext();
   ANativeActivity_showSoftInput(context->app->activity, flags);
}

void hideIme(int32_t flags) {
   DvkContext context = getDvkContext();
   ANativeActivity_hideSoftInput(context->app->activity, flags);
}

}

#endif /* ANDROID_NDK */

