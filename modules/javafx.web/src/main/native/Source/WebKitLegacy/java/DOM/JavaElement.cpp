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


#include <WebCore/DOMException.h>
#include <WebCore/Attr.h>
#include <WebCore/CSSStyleProperties.h>
#include <WebCore/Element.h>
#include <WebCore/ElementInlines.h>
#include <WebCore/EventListener.h>
#include <WebCore/EventNames.h>
#include <WebCore/HTMLCollection.h>
#include <WebCore/HTMLNames.h>
#include <WebCore/JSExecState.h>
#include <WebCore/NamedNodeMap.h>
#include <WebCore/NodeList.h>
#include <WebCore/ScrollIntoViewOptions.h>
#include <WebCore/StyledElement.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<Element*>(wkj_to_ptr(peer)))

WKJ_EXPORT int32_t wkj_dom_Element_isHTMLElement(int64_t peer) {
    WKJCallScope wkjScope;
    return IMPL->isHTMLElement();
}


// Attributes
WKJ_EXPORT int32_t wkj_dom_Element_getTagName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->tagName());
}

WKJ_EXPORT int64_t wkj_dom_Element_getAttributes(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<NamedNodeMap>(WTF::getPtr(IMPL->attributesMap()));
}

WKJ_EXPORT int64_t wkj_dom_Element_getStyle(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    auto ret = is<WebCore::StyledElement>(IMPL) ? WTF::getPtr(&downcast<WebCore::StyledElement>(IMPL)->cssomStyle()) : nullptr;
    return WKJReturnPeer<CSSStyleProperties>(ret);
}

WKJ_EXPORT int32_t wkj_dom_Element_getId(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getIdAttribute());
}

WKJ_EXPORT void wkj_dom_Element_setId(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::idAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT double wkj_dom_Element_getOffsetLeft(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->offsetLeftForBindings();
}

WKJ_EXPORT double wkj_dom_Element_getOffsetTop(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->offsetTopForBindings();
}

WKJ_EXPORT double wkj_dom_Element_getOffsetWidth(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->offsetWidth();
}

WKJ_EXPORT double wkj_dom_Element_getOffsetHeight(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->offsetHeight();
}

WKJ_EXPORT double wkj_dom_Element_getClientLeft(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->clientLeft();
}

WKJ_EXPORT double wkj_dom_Element_getClientTop(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->clientTop();
}

WKJ_EXPORT double wkj_dom_Element_getClientWidth(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->clientWidth();
}

WKJ_EXPORT double wkj_dom_Element_getClientHeight(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->clientHeight();
}

WKJ_EXPORT int32_t wkj_dom_Element_getScrollLeft(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->scrollLeft();
}

WKJ_EXPORT void wkj_dom_Element_setScrollLeft(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setScrollLeft(value);
}

WKJ_EXPORT int32_t wkj_dom_Element_getScrollTop(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->scrollTop();
}

WKJ_EXPORT void wkj_dom_Element_setScrollTop(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setScrollTop(value);
}

WKJ_EXPORT int32_t wkj_dom_Element_getScrollWidth(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->scrollWidth();
}

WKJ_EXPORT int32_t wkj_dom_Element_getScrollHeight(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->scrollHeight();
}

WKJ_EXPORT int64_t wkj_dom_Element_getOffsetParent(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(IMPL->offsetParentForBindings()));
}

WKJ_EXPORT int32_t wkj_dom_Element_getInnerHTML(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->innerHTML());
}

WKJ_EXPORT void wkj_dom_Element_setInnerHTML(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setInnerHTML(AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_Element_getOuterHTML(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->outerHTML());
}

WKJ_EXPORT void wkj_dom_Element_setOuterHTML(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setOuterHTML(AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_Element_getClassName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::classAttr));
}

WKJ_EXPORT void wkj_dom_Element_setClassName(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::classAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnbeforecopy(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().beforecopyEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnbeforecopy(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforecopyEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnbeforecut(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().beforecutEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnbeforecut(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforecutEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnbeforepaste(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().beforepasteEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnbeforepaste(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforepasteEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOncopy(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().copyEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOncopy(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().copyEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOncut(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().cutEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOncut(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().cutEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnpaste(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().pasteEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnpaste(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pasteEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnselectstart(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().selectstartEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnselectstart(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().selectstartEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnanimationend(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().animationendEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnanimationend(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().animationendEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnanimationiteration(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().animationiterationEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnanimationiteration(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().animationiterationEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnanimationstart(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().animationstartEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnanimationstart(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().animationstartEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOntransitionend(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().transitionendEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOntransitionend(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().transitionendEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnwebkitanimationend(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().webkitAnimationEndEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnwebkitanimationend(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().webkitAnimationEndEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnwebkitanimationiteration(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().webkitAnimationIterationEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnwebkitanimationiteration(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().webkitAnimationIterationEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnwebkitanimationstart(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().webkitAnimationStartEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnwebkitanimationstart(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().webkitAnimationStartEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnwebkittransitionend(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().webkitTransitionEndEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnwebkittransitionend(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().webkitTransitionEndEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnfocusin(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().focusinEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnfocusin(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusinEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnfocusout(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().focusoutEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnfocusout(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusoutEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnbeforeload(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().beforeloadEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnbeforeload(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforeloadEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnabort(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().abortEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnabort(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().abortEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnblur(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().blurEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnblur(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().blurEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOncanplay(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().canplayEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOncanplay(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().canplayEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOncanplaythrough(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().canplaythroughEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOncanplaythrough(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().canplaythroughEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnchange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().changeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnchange(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().changeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnclick(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().clickEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnclick(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().clickEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOncontextmenu(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().contextmenuEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOncontextmenu(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().contextmenuEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOndblclick(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dblclickEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOndblclick(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dblclickEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOndrag(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dragEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOndrag(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOndragend(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dragendEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOndragend(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragendEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOndragenter(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dragenterEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOndragenter(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragenterEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOndragleave(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dragleaveEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOndragleave(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragleaveEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOndragover(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dragoverEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOndragover(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragoverEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOndragstart(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dragstartEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOndragstart(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragstartEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOndrop(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dropEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOndrop(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dropEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOndurationchange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().durationchangeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOndurationchange(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().durationchangeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnemptied(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().emptiedEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnemptied(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().emptiedEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnended(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().endedEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnended(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().endedEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnerror(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().errorEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnerror(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().errorEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnfocus(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().focusEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnfocus(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOninput(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().inputEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOninput(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().inputEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOninvalid(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().invalidEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOninvalid(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().invalidEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnkeydown(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().keydownEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnkeydown(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().keydownEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnkeypress(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().keypressEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnkeypress(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().keypressEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnkeyup(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().keyupEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnkeyup(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().keyupEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnload(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().loadEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnload(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnloadeddata(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().loadeddataEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnloadeddata(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadeddataEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnloadedmetadata(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().loadedmetadataEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnloadedmetadata(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadedmetadataEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnloadstart(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().loadstartEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnloadstart(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadstartEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnmousedown(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mousedownEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnmousedown(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mousedownEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnmouseenter(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseenterEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnmouseenter(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseenterEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnmouseleave(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseleaveEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnmouseleave(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseleaveEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnmousemove(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mousemoveEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnmousemove(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mousemoveEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnmouseout(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseoutEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnmouseout(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseoutEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnmouseover(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseoverEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnmouseover(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseoverEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnmouseup(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseupEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnmouseup(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseupEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnmousewheel(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mousewheelEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnmousewheel(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mousewheelEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnpause(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().pauseEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnpause(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pauseEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnplay(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().playEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnplay(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().playEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnplaying(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().playingEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnplaying(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().playingEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnprogress(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().progressEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnprogress(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().progressEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnratechange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().ratechangeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnratechange(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().ratechangeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnreset(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().resetEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnreset(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().resetEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnresize(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().resizeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnresize(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().resizeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnscroll(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().scrollEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnscroll(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().scrollEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnseeked(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().seekedEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnseeked(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().seekedEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnseeking(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().seekingEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnseeking(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().seekingEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnselect(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().selectEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnselect(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().selectEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnstalled(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().stalledEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnstalled(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().stalledEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnsubmit(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().submitEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnsubmit(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().submitEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnsuspend(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().suspendEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnsuspend(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().suspendEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOntimeupdate(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().timeupdateEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOntimeupdate(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().timeupdateEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnvolumechange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().volumechangeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnvolumechange(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().volumechangeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnwaiting(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().waitingEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnwaiting(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().waitingEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getOnwheel(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().wheelEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Element_setOnwheel(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().wheelEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Element_getPreviousElementSibling(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(IMPL->previousElementSibling()));
}

WKJ_EXPORT int64_t wkj_dom_Element_getNextElementSibling(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(IMPL->nextElementSibling()));
}

WKJ_EXPORT int64_t wkj_dom_Element_getChildren(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLCollection>(WTF::getPtr(IMPL->children()));
}

WKJ_EXPORT int64_t wkj_dom_Element_getFirstElementChild(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(IMPL->firstElementChild()));
}

WKJ_EXPORT int64_t wkj_dom_Element_getLastElementChild(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(IMPL->lastElementChild()));
}

WKJ_EXPORT int32_t wkj_dom_Element_getChildElementCount(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->childElementCount();
}


// Functions
WKJ_EXPORT int32_t wkj_dom_Element_getAttribute(int64_t peer, const uint16_t* name, int32_t name_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(AtomString {WKJString(name, name_length)}));
}


WKJ_EXPORT void wkj_dom_Element_setAttribute(int64_t peer, const uint16_t* name, int32_t name_length, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->setAttribute(AtomString {WKJString(name, name_length)}
            , AtomString {WKJString(value, value_length)}));
}


WKJ_EXPORT void wkj_dom_Element_removeAttribute(int64_t peer, const uint16_t* name, int32_t name_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->removeAttribute(AtomString {WKJString(name, name_length)});
}


WKJ_EXPORT int64_t wkj_dom_Element_getAttributeNode(int64_t peer, const uint16_t* name, int32_t name_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Attr>(WTF::getPtr(IMPL->getAttributeNode(AtomString {WKJString(name, name_length)})));
}


WKJ_EXPORT int64_t wkj_dom_Element_setAttributeNode(int64_t peer, int64_t newAttr)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!newAttr) {
        raiseTypeErrorException();
        return 0;
    }

    return WKJReturnPeer<Attr>(WTF::getPtr(raiseOnDOMError(IMPL->setAttributeNode(*static_cast<Attr*>(wkj_to_ptr(newAttr))))));
}


WKJ_EXPORT int64_t wkj_dom_Element_removeAttributeNode(int64_t peer, int64_t oldAttr)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!oldAttr) {
        raiseTypeErrorException();
        return 0;
    }

    return WKJReturnPeer<Attr>(WTF::getPtr(raiseOnDOMError(IMPL->removeAttributeNode(*static_cast<Attr*>(wkj_to_ptr(oldAttr))))));
}


WKJ_EXPORT int64_t wkj_dom_Element_getElementsByTagName(int64_t peer, const uint16_t* name, int32_t name_length)
{
    WKJCallScope wkjScope;
    if (!name)
        return 0;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<NodeList>(WTF::getPtr(IMPL->getElementsByTagName(AtomString {WKJString(name, name_length)})));
}


WKJ_EXPORT int32_t wkj_dom_Element_hasAttributes(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttributes();
}


WKJ_EXPORT int32_t wkj_dom_Element_getAttributeNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* localName, int32_t localName_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttributeNS(AtomString {WKJString(namespaceURI, namespaceURI_length)}
            , AtomString {WKJString(localName, localName_length)}));
}


WKJ_EXPORT void wkj_dom_Element_setAttributeNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* qualifiedName, int32_t qualifiedName_length, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->setAttributeNS(AtomString {WKJString(namespaceURI, namespaceURI_length)}
            , AtomString {WKJString(qualifiedName, qualifiedName_length)}
            , AtomString {WKJString(value, value_length)}));
}


WKJ_EXPORT void wkj_dom_Element_removeAttributeNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* localName, int32_t localName_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->removeAttributeNS(AtomString {WKJString(namespaceURI, namespaceURI_length)}
            , AtomString {WKJString(localName, localName_length)});
}


WKJ_EXPORT int64_t wkj_dom_Element_getElementsByTagNameNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* localName, int32_t localName_length)
{
    WKJCallScope wkjScope;
    if (!localName)
        return 0;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<NodeList>(WTF::getPtr(IMPL->getElementsByTagNameNS(AtomString {WKJString(namespaceURI, namespaceURI_length)}
            , AtomString {WKJString(localName, localName_length)})));
}


WKJ_EXPORT int64_t wkj_dom_Element_getAttributeNodeNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* localName, int32_t localName_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Attr>(WTF::getPtr(IMPL->getAttributeNodeNS(AtomString {WKJString(namespaceURI, namespaceURI_length)}
            , AtomString {WKJString(localName, localName_length)})));
}


WKJ_EXPORT int64_t wkj_dom_Element_setAttributeNodeNS(int64_t peer, int64_t newAttr)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!newAttr) {
        raiseTypeErrorException();
        return 0;
    }
    return WKJReturnPeer<Attr>(WTF::getPtr(raiseOnDOMError(IMPL->setAttributeNodeNS(*static_cast<Attr*>(wkj_to_ptr(newAttr))))));
}


WKJ_EXPORT int32_t wkj_dom_Element_hasAttribute(int64_t peer, const uint16_t* name, int32_t name_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(AtomString {WKJString(name, name_length)});
}


WKJ_EXPORT int32_t wkj_dom_Element_hasAttributeNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* localName, int32_t localName_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttributeNS(AtomString {WKJString(namespaceURI, namespaceURI_length)}
            , AtomString {WKJString(localName, localName_length)});
}


WKJ_EXPORT void wkj_dom_Element_focus(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->focus();
}


WKJ_EXPORT void wkj_dom_Element_blur(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->blur();
}


WKJ_EXPORT void wkj_dom_Element_scrollIntoView(int64_t peer, int32_t alignWithTop)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->scrollIntoView(alignWithTop);
}


WKJ_EXPORT void wkj_dom_Element_scrollIntoViewIfNeeded(int64_t peer, int32_t centerIfNeeded)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->scrollIntoViewIfNeeded(centerIfNeeded);
}



WKJ_EXPORT int64_t wkj_dom_Element_getElementsByClassName(int64_t peer, const uint16_t* name, int32_t name_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLCollection>(WTF::getPtr(IMPL->getElementsByClassName(AtomString {WKJString(name, name_length)})));
}


WKJ_EXPORT int32_t wkj_dom_Element_matches(int64_t peer, const uint16_t* selectors, int32_t selectors_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return raiseOnDOMError(IMPL->matches(AtomString {WKJString(selectors, selectors_length)}));
}


WKJ_EXPORT int64_t wkj_dom_Element_closest(int64_t peer, const uint16_t* selectors, int32_t selectors_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(raiseOnDOMError(IMPL->closest(AtomString {WKJString(selectors, selectors_length)}))));
}


WKJ_EXPORT int32_t wkj_dom_Element_webkitMatchesSelector(int64_t peer, const uint16_t* selectors, int32_t selectors_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return raiseOnDOMError(IMPL->matches(AtomString {WKJString(selectors, selectors_length)}));
}


WKJ_EXPORT void wkj_dom_Element_webkitRequestFullScreen(int64_t peer, int16_t arg0)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->webkitRequestFullscreen();
}


WKJ_EXPORT void wkj_dom_Element_webkitRequestFullscreen(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->webkitRequestFullscreen();
}


WKJ_EXPORT void wkj_dom_Element_remove(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->remove());
}


WKJ_EXPORT int64_t wkj_dom_Element_querySelector(int64_t peer, const uint16_t* selectors, int32_t selectors_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(raiseOnDOMError(IMPL->querySelector(AtomString {WKJString(selectors, selectors_length)}))));
}


WKJ_EXPORT int64_t wkj_dom_Element_querySelectorAll(int64_t peer, const uint16_t* selectors, int32_t selectors_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<NodeList>(WTF::getPtr(raiseOnDOMError(IMPL->querySelectorAll(AtomString {WKJString(selectors, selectors_length)}))));
}


}
