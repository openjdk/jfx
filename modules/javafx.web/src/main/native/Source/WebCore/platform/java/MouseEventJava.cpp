/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"

#include "com_sun_webkit_event_WCMouseEvent.h"
#include "PlatformMouseEvent.h"

namespace WebCore {

MouseButton getWebCoreMouseButton(jint javaButton)
{
    // This code assumes that we have three-buttons mouse
    // otherwise BUTTON2 is a right button.
    if (javaButton == com_sun_webkit_event_WCMouseEvent_BUTTON1) {
        return MouseButton::Left;
    } else if (javaButton == com_sun_webkit_event_WCMouseEvent_BUTTON2) {
        return MouseButton::Middle;
    } else if (javaButton == com_sun_webkit_event_WCMouseEvent_BUTTON3) {
        return MouseButton::Right;
    } else {
        return MouseButton::None;
    }
}

unsigned short getWebCoreMouseButtons(jint javaButton)
{
    unsigned short buttons = NoButtonMask;
    if (javaButton & com_sun_webkit_event_WCMouseEvent_BUTTON1) {
        buttons |= LeftButtonMask;
    }
    if (javaButton & com_sun_webkit_event_WCMouseEvent_BUTTON2) {
        buttons |= MiddleButtonMask;
    }
    if (javaButton & com_sun_webkit_event_WCMouseEvent_BUTTON3) {
        buttons |= RightButtonMask;
    }
    return buttons;
}

PlatformEvent::Type getWebCoreMouseEventType(jint eventID)
{
    switch (eventID) {
    case com_sun_webkit_event_WCMouseEvent_MOUSE_PRESSED:
        return PlatformEvent::Type::MousePressed;
    case com_sun_webkit_event_WCMouseEvent_MOUSE_RELEASED:
        return PlatformEvent::Type::MouseReleased;
    case com_sun_webkit_event_WCMouseEvent_MOUSE_MOVED:
    case com_sun_webkit_event_WCMouseEvent_MOUSE_DRAGGED:
        return PlatformEvent::Type::MouseMoved;
    case com_sun_webkit_event_WCMouseEvent_MOUSE_WHEEL:
        return PlatformEvent::Type::MouseScroll;
    default:
        return PlatformEvent::Type::MouseMoved;
    }
}

} // namespace WebCore
