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

#include "wm/LensWindowManager.h"
#include "com_sun_glass_events_ViewEvent.h"
#include "com_sun_glass_events_WindowEvent.h"
#include "com_sun_glass_events_MouseEvent.h"

#include <X11/Xlib.h>

void eglfbX11ContainerEventLoop(JNIEnv *env) {
    while (1) {
        XEvent e;
        XNextEvent(eglfbX11ContainerDisplay, &e);
        if (e.xany.window != eglfbX11ContainerWindow) {
            continue;
        }
        switch (e.type) {
            case ButtonPress:
                GLASS_LOG_FINEST("Button %i down", e.xbutton.button);
                if (e.xbutton.button == 1) {
                    lens_wm_notifyButtonEvent(env, JNI_TRUE,
                                              com_sun_glass_events_MouseEvent_BUTTON_LEFT,
                                              e.xbutton.x, e.xbutton.y);
                }
                break;
            case ButtonRelease:
                GLASS_LOG_FINEST("Button %i up", e.xbutton.button);
                if (e.xbutton.button == 1) {
                    lens_wm_notifyButtonEvent(env, JNI_FALSE,
                                              com_sun_glass_events_MouseEvent_BUTTON_LEFT,
                                              e.xbutton.x, e.xbutton.y);
                }
                break;
            case MotionNotify:
                GLASS_LOG_FINEST("Pointer moved to (%i,%i)",
                                 e.xmotion.x, e.xmotion.y);
                lens_wm_notifyMotionEvent(env, e.xmotion.x, e.xmotion.y, 0, 0);
                break;
            default:
                GLASS_LOG_FINEST("XNextEvent returned event of type %i",
                                 e.type);
        }
    }
}
