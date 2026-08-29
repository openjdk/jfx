/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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

#undef IMPL


#include <WebCore/Event.h>
#include <WebCore/EventTarget.h>
#include <WebCore/KeyboardEvent.h>
#include <WebCore/MouseEvent.h>
#include <WebCore/MutationEvent.h>
#include <WebCore/UIEvent.h>
#include <WebCore/WheelEvent.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<Event*>(wkj_to_ptr(peer)))

WKJ_EXPORT void wkj_dom_Event_dispose(int64_t peer)
{
    WKJCallScope wkjScope;
    IMPL->deref();
}

WKJ_EXPORT int32_t wkj_dom_Event_getCPPType(int64_t peer)
{
    WKJCallScope wkjScope;
    if (is<WheelEvent>(*IMPL))
        return 1;
    if (is<MouseEvent>(*IMPL))
        return 2;
    if (is<KeyboardEvent>(*IMPL))
        return 3;
    if (IMPL->isUIEvent())
        return 4;
    if (IMPL->isMutationEvent())
        return 5;
    return 0;
}


// Attributes
WKJ_EXPORT int32_t wkj_dom_Event_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->type());
}

WKJ_EXPORT int64_t wkj_dom_Event_getTarget(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventTarget>(WTF::getPtr(IMPL->target()));
}

WKJ_EXPORT int64_t wkj_dom_Event_getCurrentTarget(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventTarget>(WTF::getPtr(IMPL->currentTarget()));
}

WKJ_EXPORT int16_t wkj_dom_Event_getEventPhase(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->eventPhase();
}

WKJ_EXPORT int32_t wkj_dom_Event_getBubbles(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->bubbles();
}

WKJ_EXPORT int32_t wkj_dom_Event_getCancelable(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->cancelable();
}

WKJ_EXPORT int64_t wkj_dom_Event_getTimeStamp(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->timeStamp().approximateWallTime().secondsSinceEpoch().milliseconds();
}

WKJ_EXPORT int32_t wkj_dom_Event_getDefaultPrevented(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->defaultPrevented();
}

WKJ_EXPORT int32_t wkj_dom_Event_getIsTrusted(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->isTrusted();
}

WKJ_EXPORT int64_t wkj_dom_Event_getSrcElement(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventTarget>(WTF::getPtr(IMPL->target()));
}

WKJ_EXPORT int32_t wkj_dom_Event_getReturnValue(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->legacyReturnValue();
}

WKJ_EXPORT void wkj_dom_Event_setReturnValue(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setLegacyReturnValue(value);
}

WKJ_EXPORT int32_t wkj_dom_Event_getCancelBubble(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->cancelBubble();
}

WKJ_EXPORT void wkj_dom_Event_setCancelBubble(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setCancelBubble(value);
}


// Functions
WKJ_EXPORT void wkj_dom_Event_stopPropagation(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->stopPropagation();
}


WKJ_EXPORT void wkj_dom_Event_preventDefault(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->preventDefault();
}


WKJ_EXPORT void wkj_dom_Event_initEvent(int64_t peer, const uint16_t* eventTypeArg, int32_t eventTypeArg_length, int32_t canBubbleArg, int32_t cancelableArg)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->initEvent(AtomString{ WKJString(eventTypeArg, eventTypeArg_length) }
            , canBubbleArg
            , cancelableArg);
}


WKJ_EXPORT void wkj_dom_Event_stopImmediatePropagation(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->stopImmediatePropagation();
}


}
