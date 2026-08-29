/*
 * Copyright (c) 2013, 2025, Oracle and/or its affiliates. All rights reserved.
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


#include <WebCore/EventListener.h>
#include <WebCore/HTMLFrameSetElement.h>
#include <WebCore/HTMLNames.h>
#include <WebCore/EventNames.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLFrameSetElement*>(wkj_to_ptr(peer)))


// Attributes
WKJ_EXPORT int32_t wkj_dom_HTMLFrameSetElement_getCols(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::colsAttr));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setCols(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::colsAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLFrameSetElement_getRows(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::rowsAttr));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setRows(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::rowsAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnblur(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().blurEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnblur(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().blurEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnerror(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().errorEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnerror(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().errorEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnfocus(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().focusEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnfocus(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnfocusin(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().focusinEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnfocusin(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusinEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnfocusout(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().focusoutEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnfocusout(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusoutEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnload(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().loadEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnload(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnresize(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().resizeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnresize(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().resizeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnscroll(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().scrollEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnscroll(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().scrollEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnbeforeunload(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().beforeunloadEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnbeforeunload(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforeunloadEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnhashchange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().hashchangeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnhashchange(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().hashchangeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnmessage(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().messageEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnmessage(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().messageEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnoffline(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().offlineEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnoffline(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().offlineEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnonline(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().onlineEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnonline(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().onlineEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnpagehide(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().pagehideEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnpagehide(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pagehideEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnpageshow(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().pageshowEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnpageshow(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pageshowEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnpopstate(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().popstateEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnpopstate(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().popstateEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnstorage(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().storageEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnstorage(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().storageEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnunload(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().unloadEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnunload(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().unloadEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

}
