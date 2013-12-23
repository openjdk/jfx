/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
        return LeftButton;
    } else if (javaButton == com_sun_webkit_event_WCMouseEvent_BUTTON2) {
        return MiddleButton;
    } else if (javaButton == com_sun_webkit_event_WCMouseEvent_BUTTON3) {
        return RightButton;
    } else {
        return NoButton;
    }
}

PlatformEvent::Type getWebCoreMouseEventType(jint eventID)
{
    switch (eventID) {
    case com_sun_webkit_event_WCMouseEvent_MOUSE_PRESSED:
        return PlatformEvent::MousePressed;
    case com_sun_webkit_event_WCMouseEvent_MOUSE_RELEASED:
        return PlatformEvent::MouseReleased;
    case com_sun_webkit_event_WCMouseEvent_MOUSE_MOVED:
    case com_sun_webkit_event_WCMouseEvent_MOUSE_DRAGGED:
        return PlatformEvent::MouseMoved;
    case com_sun_webkit_event_WCMouseEvent_MOUSE_WHEEL:
        return PlatformEvent::MouseScroll;
    default:
        return PlatformEvent::MouseMoved;
    }
}    

} // namespace WebCore
