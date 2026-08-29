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
#include <WebCore/HTMLBodyElement.h>
#include <WebCore/HTMLNames.h>
#include <WebCore/EventNames.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLBodyElement*>(wkj_to_ptr(peer)))


// Attributes
WKJ_EXPORT int32_t wkj_dom_HTMLBodyElement_getALink(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::alinkAttr));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setALink(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::alinkAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLBodyElement_getBackground(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::backgroundAttr));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setBackground(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::backgroundAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLBodyElement_getBgColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::bgcolorAttr));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setBgColor(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::bgcolorAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLBodyElement_getLink(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::linkAttr));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setLink(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::linkAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLBodyElement_getText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::textAttr));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setText(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::textAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLBodyElement_getVLink(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::vlinkAttr));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setVLink(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::vlinkAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnblur(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().blurEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnblur(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().blurEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnerror(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().errorEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnerror(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().errorEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnfocus(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().focusEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnfocus(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnfocusin(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().focusinEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnfocusin(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusinEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnfocusout(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().focusoutEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnfocusout(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusoutEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnload(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().loadEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnload(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnresize(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().resizeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnresize(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().resizeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnscroll(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().scrollEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnscroll(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().scrollEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnselectionchange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().selectionchangeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnselectionchange(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().selectionchangeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnbeforeunload(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().beforeunloadEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnbeforeunload(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforeunloadEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnhashchange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().hashchangeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnhashchange(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().hashchangeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnmessage(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().messageEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnmessage(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().messageEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnoffline(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().offlineEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnoffline(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().offlineEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnonline(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().onlineEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnonline(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().onlineEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnpagehide(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().pagehideEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnpagehide(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pagehideEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnpageshow(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().pageshowEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnpageshow(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pageshowEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnpopstate(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().popstateEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnpopstate(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().popstateEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnstorage(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().storageEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnstorage(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().storageEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnunload(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().unloadEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnunload(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().unloadEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

}
