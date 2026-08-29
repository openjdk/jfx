/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include <WebCore/WheelEvent.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<WheelEvent*>(wkj_to_ptr(peer)))


// Attributes
WKJ_EXPORT double wkj_dom_WheelEvent_getDeltaX(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->deltaX();
}

WKJ_EXPORT double wkj_dom_WheelEvent_getDeltaY(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->deltaY();
}

WKJ_EXPORT double wkj_dom_WheelEvent_getDeltaZ(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->deltaZ();
}

WKJ_EXPORT int32_t wkj_dom_WheelEvent_getDeltaMode(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->deltaMode();
}

WKJ_EXPORT int32_t wkj_dom_WheelEvent_getWheelDeltaX(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->wheelDeltaX();
}

WKJ_EXPORT int32_t wkj_dom_WheelEvent_getWheelDeltaY(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->wheelDeltaY();
}

WKJ_EXPORT int32_t wkj_dom_WheelEvent_getWheelDelta(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->wheelDelta();
}

WKJ_EXPORT int32_t wkj_dom_WheelEvent_getWebkitDirectionInvertedFromDevice(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->webkitDirectionInvertedFromDevice();
}


// Functions
WKJ_EXPORT void wkj_dom_WheelEvent_initWheelEvent(int64_t peer, int32_t wheelDeltaX, int32_t wheelDeltaY, int64_t view, int32_t screenX, int32_t screenY, int32_t clientX, int32_t clientY, int32_t ctrlKey, int32_t altKey, int32_t shiftKey, int32_t metaKey)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->initWheelEvent(wheelDeltaX
            , wheelDeltaY
            , static_cast<DOMWindow*>(wkj_to_ptr(view))
            , screenX
            , screenY
            , clientX
            , clientY
            , ctrlKey
            , altKey
            , shiftKey
            , metaKey);
}


}
