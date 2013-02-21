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
 
#include "input/LensInput.h"
#include "wm/LensWindowManager.h"
#include "com_sun_glass_events_WindowEvent.h"
#include "com_sun_glass_events_KeyEvent.h"
#include "com_sun_glass_events_MouseEvent.h"
#include "com_sun_glass_ui_Robot.h"

jboolean glass_robot_postKeyEvent(JNIEnv *env,
                                  jint keyEventType,
                                  jint jfxKeyCode) {

    NativeWindow window;

    window = glass_window_getFocusedWindow();

    if (window == NULL) {
        GLASS_LOG_WARNING("Can't post event (window is NULL)");
        return JNI_FALSE;
    }

    GLASS_LOG_FINE("Sending keyEvent %d, keyCode %d", keyEventType, jfxKeyCode);
    glass_application_notifyKeyEvent(env, window,
                                     keyEventType,
                                     jfxKeyCode,
                                     JNI_FALSE/*not a repeat event*/);


    return JNI_TRUE;
}


jboolean glass_robot_postScrollEvent(JNIEnv *env,
                                     jint wheelAmt ){

    jboolean result = JNI_FALSE;
    int x, y;

    lens_wm_getPointerPosition(&x, &y);
    lens_wm_notifyScrollEvent(env, x, y, wheelAmt);

    result = JNI_TRUE;

    return result;
}



jboolean glass_robot_postMouseEvent(JNIEnv *env,
                                    jint mouseEventType, jint x, jint y,
                                    jint buttons) {

    NativeWindow window = NULL;
    jboolean result = JNI_FALSE;
    jint glassMouseButton;

    switch (buttons) {
        case com_sun_glass_ui_Robot_MOUSE_LEFT_BTN : 
            glassMouseButton = com_sun_glass_events_MouseEvent_BUTTON_LEFT;
            break;
        case com_sun_glass_ui_Robot_MOUSE_RIGHT_BTN : 
            glassMouseButton = com_sun_glass_events_MouseEvent_BUTTON_RIGHT;
            break;
        case com_sun_glass_ui_Robot_MOUSE_MIDDLE_BTN : 
            glassMouseButton = com_sun_glass_events_MouseEvent_BUTTON_OTHER;
            break;
        default: 
            glassMouseButton = com_sun_glass_events_MouseEvent_BUTTON_NONE;
            break;
    }

    switch (mouseEventType) {
        case com_sun_glass_events_MouseEvent_DOWN:
        case com_sun_glass_events_MouseEvent_UP: {
            if (mouseEventType == com_sun_glass_events_MouseEvent_DOWN) {
                GLASS_LOG_FINE("Posting mouse event: press");
            } else {
                GLASS_LOG_FINE("Posting mouse event: release");
            }
            int mousePosX, mousePosY;
            lens_wm_getPointerPosition(&mousePosX, &mousePosY);
            jboolean isPressed =
                mouseEventType == com_sun_glass_events_MouseEvent_DOWN;
            lens_wm_notifyButtonEvent(env, isPressed, glassMouseButton,
                                            mousePosX, mousePosY);
            result = JNI_TRUE;
        }
        break;
        case com_sun_glass_events_MouseEvent_MOVE:

            GLASS_LOG_FINER("Posting mouse event: Move");
            lens_wm_setPointerPosition(x, y);
            lens_wm_notifyMotionEvent(env, x, y, 0,0);
            result = JNI_TRUE;
            break;
        case com_sun_glass_events_MouseEvent_DRAG:
        case com_sun_glass_events_MouseEvent_ENTER:
        case com_sun_glass_events_MouseEvent_EXIT:
        case com_sun_glass_events_MouseEvent_CLICK:
        case com_sun_glass_events_MouseEvent_WHEEL:
        default:
            break;
    }

    return result;
}

jboolean glass_robot_getMouseLocation(jint *pX, jint *pY) {

    lens_wm_getPointerPosition(pX, pY);
    return JNI_TRUE;
}

