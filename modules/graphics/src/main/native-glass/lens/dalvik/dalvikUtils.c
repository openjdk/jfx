/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

#if (defined(ANDROID_NDK) && defined(DALVIK_VM))

#include <android/keycodes.h>
#include "dalvikConst.h"
#include "dalvikUtils.h"
#include "com_sun_glass_events_TouchEvent.h"
#include "com_sun_glass_events_MouseEvent.h"
#include "com_sun_glass_events_KeyEvent.h"

int to_jfx_touch_action(int state) {
    switch (state) {
        case TOUCH_ACTION_DOWN:
        case TOUCH_ACTION_POINTER_DOWN:    
            return com_sun_glass_events_TouchEvent_TOUCH_PRESSED;
        case TOUCH_ACTION_UP:
        case TOUCH_ACTION_POINTER_UP:    
            return com_sun_glass_events_TouchEvent_TOUCH_RELEASED;
        case TOUCH_ACTION_MOVE:
            return com_sun_glass_events_TouchEvent_TOUCH_MOVED;
        case TOUCH_ACTION_CANCEL:
            return com_sun_glass_events_TouchEvent_TOUCH_RELEASED;                    
        case TOUCH_ACTION_STILL:
            return com_sun_glass_events_TouchEvent_TOUCH_STILL;
        default:
            return 0;
    }
}

int to_jfx_key_action(int action) {
    switch (action) {
        case KEY_ACTION_DOWN:
            return com_sun_glass_events_KeyEvent_PRESS;
        case KEY_ACTION_UP:
            return com_sun_glass_events_KeyEvent_RELEASE;
        case KEY_ACTION_MULTIPLE:
            return com_sun_glass_events_KeyEvent_TYPED;
    }
}

int to_linux_keycode(int androidKeyCode) {
    for (int i = 0; i < sizeof (keyMap); ++i) {
        if (keyMap[i].androidKC == androidKeyCode) {
            return keyMap[i].linuxKC;
        }
    }
    return KEY_RESERVED;
}


char *describe_surface_format(int f) {    
    switch (f) {
        case RGBA_8888:
            return "RGBA_8888";
        case RGBX_8888:
            return "RGBX_8888";
        case RGB_888:
            return "RGB_888";
        case RGB_565:
            return "RGB_565";
        default:
            return "UNKNOWN";
    }
}

char *describe_touch_action(int state) {
    switch (state) {
        case TOUCH_ACTION_DOWN:
            return "TOUCH_ACTION_DOWN";
        case TOUCH_ACTION_UP:
            return "TOUCH_ACTION_UP";
        case TOUCH_ACTION_MOVE:
            return "TOUCH_ACTION_MOVE";
        case TOUCH_ACTION_CANCEL:
            return "TOUCH_ACTION_CANCEL";
        case TOUCH_ACTION_OUTSIDE:
            return "TOUCH_ACTION_OUTSIDE";
        case TOUCH_ACTION_POINTER_DOWN:
            return "TOUCH_ACTION_POINTER_DOWN";
        case TOUCH_ACTION_POINTER_UP:
            return "TOUCH_ACTION_POINTER_UP";
        case TOUCH_ACTION_STILL:
            return "TOUCH_ACTION_STILL";
        default:
            return "TOUCH_ACTION_UNKNOWN";
    }
}

char *describe_key_action(int action) {
    switch(action) {
        case KEY_ACTION_DOWN:
            return "KEY_ACTION_DOWN";
        case KEY_ACTION_UP:
            return "KEY_ACTION_UP";
        case KEY_ACTION_MULTIPLE:
            return "KEY_ACTION_MULTIPLE";
    }
}

#endif
