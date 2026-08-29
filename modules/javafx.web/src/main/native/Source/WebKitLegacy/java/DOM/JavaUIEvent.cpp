/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates. All rights reserved.
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
#include <WebCore/JSExecState.h>
#include <WebCore/KeyboardEvent.h>
#include <WebCore/ThreadCheck.h>
#include <WebCore/UIEvent.h>

#include <wtf/GetPtr.h>
#include <wtf/URL.h>

#include "AbstractViewInternal.h"
#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<UIEvent*>(wkj_to_ptr(peer)))


// Attributes
WKJ_EXPORT int64_t wkj_dom_UIEvent_getView(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<DOMWindow>(WTF::getPtr(toDOMWindow(IMPL->view())));
}

WKJ_EXPORT int32_t wkj_dom_UIEvent_getDetail(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->detail();
}

WKJ_EXPORT int32_t wkj_dom_UIEvent_getKeyCode(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (is<WebCore::KeyboardEvent>(*IMPL))
        return downcast<WebCore::KeyboardEvent>(*IMPL).keyCode();
    return 0;
}

WKJ_EXPORT int32_t wkj_dom_UIEvent_getCharCode(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (is<WebCore::KeyboardEvent>(*IMPL))
        return downcast<WebCore::KeyboardEvent>(*IMPL).charCode();
    return 0;
}

WKJ_EXPORT int32_t wkj_dom_UIEvent_getLayerX(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->layerX();
}

WKJ_EXPORT int32_t wkj_dom_UIEvent_getLayerY(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->layerY();
}

WKJ_EXPORT int32_t wkj_dom_UIEvent_getPageX(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->pageX();
}

WKJ_EXPORT int32_t wkj_dom_UIEvent_getPageY(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->pageY();
}

WKJ_EXPORT int32_t wkj_dom_UIEvent_getWhich(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->which();
}


// Functions
WKJ_EXPORT void wkj_dom_UIEvent_initUIEvent(int64_t peer, const uint16_t* type, int32_t type_length, int32_t canBubble, int32_t cancelable, int64_t view, int32_t detail)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->initUIEvent(AtomString{WKJString(type, type_length)}
            , canBubble
            , cancelable
            , toWindowProxy(static_cast<DOMWindow*>(wkj_to_ptr(view)))
            , detail);
}


}
