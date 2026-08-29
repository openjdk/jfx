/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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


#include <WebCore/DOMWindow.h>
#include <WebCore/EventTarget.h>
#include <WebCore/MouseEvent.h>
#include <WebCore/Node.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include "AbstractViewInternal.h"
#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<MouseEvent*>(wkj_to_ptr(peer)))


// Attributes
WKJ_EXPORT int32_t wkj_dom_MouseEvent_getScreenX(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->screenX();
}

WKJ_EXPORT int32_t wkj_dom_MouseEvent_getScreenY(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->screenY();
}

WKJ_EXPORT int32_t wkj_dom_MouseEvent_getClientX(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->clientX();
}

WKJ_EXPORT int32_t wkj_dom_MouseEvent_getClientY(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->clientY();
}

WKJ_EXPORT int32_t wkj_dom_MouseEvent_getCtrlKey(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->ctrlKey();
}

WKJ_EXPORT int32_t wkj_dom_MouseEvent_getShiftKey(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->shiftKey();
}

WKJ_EXPORT int32_t wkj_dom_MouseEvent_getAltKey(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->altKey();
}

WKJ_EXPORT int32_t wkj_dom_MouseEvent_getMetaKey(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->metaKey();
}

WKJ_EXPORT int32_t wkj_dom_MouseEvent_getButton(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
        int16_t button = enumToUnderlyingType(IMPL->button());
    return button;
}

WKJ_EXPORT int64_t wkj_dom_MouseEvent_getRelatedTarget(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventTarget>(WTF::getPtr(IMPL->relatedTarget()));
}

WKJ_EXPORT int32_t wkj_dom_MouseEvent_getOffsetX(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->offsetX();
}

WKJ_EXPORT int32_t wkj_dom_MouseEvent_getOffsetY(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->offsetY();
}

WKJ_EXPORT int64_t wkj_dom_MouseEvent_getFromElement(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->fromElement()));
}

WKJ_EXPORT int64_t wkj_dom_MouseEvent_getToElement(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->toElement()));
}


// Functions
WKJ_EXPORT void wkj_dom_MouseEvent_initMouseEvent(int64_t peer, const uint16_t* type, int32_t type_length, int32_t canBubble, int32_t cancelable, int64_t view, int32_t detail, int32_t screenX, int32_t screenY, int32_t clientX, int32_t clientY, int32_t ctrlKey, int32_t altKey, int32_t shiftKey, int32_t metaKey, int16_t button, int64_t relatedTarget)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->initMouseEvent(AtomString {WKJString(type, type_length)}
            , canBubble
            , cancelable
            , toWindowProxy(static_cast<DOMWindow*>(wkj_to_ptr(view)))
            , detail
            , screenX
            , screenY
            , clientX
            , clientY
            , ctrlKey
            , altKey
            , shiftKey
            , metaKey
            , button
            , static_cast<EventTarget*>(wkj_to_ptr(relatedTarget)));
}


}
