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


#include <JavaScriptCore/APICast.h>
#include <WebCore/AddEventListenerOptions.h>
#include <WebCore/CSSStyleDeclaration.h>
#include <WebCore/DOMException.h>
#include <WebCore/DOMSelection.h>
#include <WebCore/DOMWindow.h>
#include <WebCore/Document.h>
#if PLATFORM(JAVA)
#include "ShadowRoot.h"
#include "AddEventListenerOptionsInlines.h"
#endif
#include <WebCore/HTMLFrameOwnerElement.h>
#include <WebCore/Event.h>
#include <WebCore/EventListener.h>
#include <WebCore/EventTarget.h>
#include <WebCore/EventNames.h>
#include <WebCore/JSExecState.h>
#include <WebCore/WindowProxy.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (dynamicDowncast<LocalDOMWindow>(static_cast<DOMWindow*>(wkj_to_ptr(peer))))

WKJ_EXPORT void wkj_dom_DOMWindow_dispose(int64_t peer)
{
    WKJCallScope wkjScope;
    IMPL->deref();
}


// Attributes
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getFrameElement(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLFrameOwnerElement>(WTF::getPtr(IMPL->frameElement()));
}

WKJ_EXPORT int32_t wkj_dom_DOMWindow_getOffscreenBuffering(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->offscreenBuffering();
}

WKJ_EXPORT int32_t wkj_dom_DOMWindow_getOuterHeight(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->outerHeight();
}

WKJ_EXPORT int32_t wkj_dom_DOMWindow_getOuterWidth(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->outerWidth();
}

WKJ_EXPORT int32_t wkj_dom_DOMWindow_getInnerHeight(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->innerHeight();
}

WKJ_EXPORT int32_t wkj_dom_DOMWindow_getInnerWidth(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->innerWidth();
}

WKJ_EXPORT int32_t wkj_dom_DOMWindow_getScreenX(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->screenX();
}

WKJ_EXPORT int32_t wkj_dom_DOMWindow_getScreenY(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->screenY();
}

WKJ_EXPORT int32_t wkj_dom_DOMWindow_getScreenLeft(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->screenLeft();
}

WKJ_EXPORT int32_t wkj_dom_DOMWindow_getScreenTop(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->screenTop();
}

WKJ_EXPORT int32_t wkj_dom_DOMWindow_getScrollX(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->scrollX();
}

WKJ_EXPORT int32_t wkj_dom_DOMWindow_getScrollY(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->scrollY();
}

WKJ_EXPORT int32_t wkj_dom_DOMWindow_getPageXOffset(int64_t arg0)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return {};
}

WKJ_EXPORT int32_t wkj_dom_DOMWindow_getPageYOffset(int64_t arg0)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return {};
}

WKJ_EXPORT int32_t wkj_dom_DOMWindow_getClosed(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->closed();
}

WKJ_EXPORT int32_t wkj_dom_DOMWindow_getLength(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->length();
}

WKJ_EXPORT int32_t wkj_dom_DOMWindow_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->name());
}

WKJ_EXPORT void wkj_dom_DOMWindow_setName(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setName(AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_DOMWindow_getStatus(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->status());
}

WKJ_EXPORT void wkj_dom_DOMWindow_setStatus(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setStatus(AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_DOMWindow_getDefaultStatus(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->status());
}

WKJ_EXPORT void wkj_dom_DOMWindow_setDefaultStatus(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setStatus(AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getSelf(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<DOMWindow>(WTF::getPtr(IMPL));
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getWindow(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    // DOMWindow::frames() / DOMWindow::window() methods as they are just aliases for DOMWindow::self()
    return WKJReturnPeer<DOMWindow>(WTF::getPtr(IMPL));
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getFrames(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    // DOMWindow::frames() / DOMWindow::window() methods as they are just aliases for DOMWindow::self()
    return WKJReturnPeer<DOMWindow>(WTF::getPtr(IMPL));
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOpener(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<DOMWindow>(WTF::getPtr(IMPL->opener()->window()));
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getParent(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<DOMWindow>(WTF::getPtr(IMPL->parent()->window()));
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getTop(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<DOMWindow>(WTF::getPtr(IMPL->top()->window()));
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getDocumentEx(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Document>(WTF::getPtr(IMPL->document()));
}

WKJ_EXPORT double wkj_dom_DOMWindow_getDevicePixelRatio(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->devicePixelRatio();
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnanimationend(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().animationendEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnanimationend(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().animationendEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnanimationiteration(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().animationiterationEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnanimationiteration(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().animationiterationEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnanimationstart(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().animationstartEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnanimationstart(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().animationstartEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOntransitionend(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().transitionendEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOntransitionend(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().transitionendEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnwebkitanimationend(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().webkitAnimationEndEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnwebkitanimationend(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().webkitAnimationEndEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnwebkitanimationiteration(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().webkitAnimationIterationEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnwebkitanimationiteration(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().webkitAnimationIterationEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnwebkitanimationstart(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().webkitAnimationStartEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnwebkitanimationstart(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().webkitAnimationStartEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnwebkittransitionend(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().webkitTransitionEndEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnwebkittransitionend(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().webkitTransitionEndEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnabort(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().abortEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnabort(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().abortEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnblur(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().blurEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnblur(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().blurEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOncanplay(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().canplayEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOncanplay(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().canplayEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOncanplaythrough(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().canplaythroughEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOncanplaythrough(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().canplaythroughEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnchange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().changeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnchange(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().changeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnclick(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().clickEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnclick(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().clickEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOncontextmenu(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().contextmenuEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOncontextmenu(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().contextmenuEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOndblclick(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dblclickEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOndblclick(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dblclickEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOndrag(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dragEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOndrag(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOndragend(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dragendEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOndragend(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragendEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOndragenter(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dragenterEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOndragenter(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragenterEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOndragleave(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dragleaveEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOndragleave(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragleaveEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOndragover(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dragoverEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOndragover(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragoverEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOndragstart(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dragstartEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOndragstart(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragstartEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOndrop(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dropEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOndrop(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dropEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOndurationchange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().durationchangeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOndurationchange(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().durationchangeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnemptied(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().emptiedEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnemptied(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().emptiedEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnended(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().endedEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnended(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().endedEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnerror(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().errorEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnerror(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().errorEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnfocus(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().focusEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnfocus(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOninput(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().inputEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOninput(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().inputEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOninvalid(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().invalidEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOninvalid(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().invalidEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnkeydown(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().keydownEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnkeydown(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().keydownEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnkeypress(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().keypressEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnkeypress(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().keypressEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnkeyup(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().keyupEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnkeyup(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().keyupEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnload(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().loadEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnload(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnloadeddata(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().loadeddataEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnloadeddata(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadeddataEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnloadedmetadata(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().loadedmetadataEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnloadedmetadata(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadedmetadataEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnloadstart(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().loadstartEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnloadstart(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadstartEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnmousedown(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mousedownEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnmousedown(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mousedownEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnmouseenter(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseenterEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnmouseenter(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseenterEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnmouseleave(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseleaveEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnmouseleave(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseleaveEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnmousemove(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mousemoveEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnmousemove(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mousemoveEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnmouseout(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseoutEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnmouseout(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseoutEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnmouseover(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseoverEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnmouseover(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseoverEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnmouseup(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseupEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnmouseup(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseupEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnmousewheel(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mousewheelEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnmousewheel(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mousewheelEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnpause(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().pauseEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnpause(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pauseEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnplay(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().playEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnplay(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().playEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnplaying(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().playingEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnplaying(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().playingEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnprogress(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().progressEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnprogress(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().progressEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnratechange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().ratechangeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnratechange(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().ratechangeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnreset(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().resetEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnreset(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().resetEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnresize(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().resizeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnresize(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().resizeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnscroll(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().scrollEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnscroll(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().scrollEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnseeked(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().seekedEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnseeked(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().seekedEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnseeking(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().seekingEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnseeking(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().seekingEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnselect(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().selectEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnselect(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().selectEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnstalled(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().stalledEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnstalled(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().stalledEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnsubmit(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().submitEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnsubmit(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().submitEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnsuspend(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().suspendEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnsuspend(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().suspendEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOntimeupdate(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().timeupdateEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOntimeupdate(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().timeupdateEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnvolumechange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().volumechangeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnvolumechange(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().volumechangeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnwaiting(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().waitingEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnwaiting(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().waitingEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnwheel(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().wheelEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnwheel(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().wheelEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnbeforeunload(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().beforeunloadEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnbeforeunload(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforeunloadEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnhashchange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().hashchangeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnhashchange(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().hashchangeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnmessage(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().messageEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnmessage(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().messageEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnoffline(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().offlineEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnoffline(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().offlineEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnonline(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().onlineEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnonline(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().onlineEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnpagehide(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().pagehideEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnpagehide(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pagehideEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnpageshow(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().pageshowEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnpageshow(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pageshowEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnpopstate(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().popstateEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnpopstate(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().popstateEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnstorage(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().storageEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnstorage(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().storageEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnunload(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().unloadEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_DOMWindow_setOnunload(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().unloadEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}


// Functions
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getSelection(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<DOMSelection>(WTF::getPtr(IMPL->getSelection()));
}


WKJ_EXPORT void wkj_dom_DOMWindow_focus(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->focus();
}


WKJ_EXPORT void wkj_dom_DOMWindow_blur(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->blur();
}


WKJ_EXPORT void wkj_dom_DOMWindow_close(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->close();
}


WKJ_EXPORT void wkj_dom_DOMWindow_print(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->print();
}


WKJ_EXPORT void wkj_dom_DOMWindow_stop(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->stop();
}


WKJ_EXPORT void wkj_dom_DOMWindow_alert(int64_t peer, const uint16_t* message, int32_t message_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->alert(AtomString{WKJString(message, message_length)});
}


WKJ_EXPORT int32_t wkj_dom_DOMWindow_confirm(int64_t peer, const uint16_t* message, int32_t message_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->confirmForBindings(AtomString{WKJString(message, message_length)});
}


WKJ_EXPORT int32_t wkj_dom_DOMWindow_prompt(int64_t peer, const uint16_t* message, int32_t message_length, const uint16_t* defaultValue, int32_t defaultValue_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->prompt(AtomString{WKJString(message, message_length)}
            , AtomString{WKJString(defaultValue, defaultValue_length)}));
}


WKJ_EXPORT int32_t wkj_dom_DOMWindow_find(int64_t peer, const uint16_t* string, int32_t string_length, int32_t caseSensitive, int32_t backwards, int32_t wrap, int32_t wholeWord, int32_t searchInFrames, int32_t showDialog)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->find(AtomString{WKJString(string, string_length)}
            , caseSensitive
            , backwards
            , wrap
            , wholeWord
            , searchInFrames
            , showDialog);
}


WKJ_EXPORT void wkj_dom_DOMWindow_scrollBy(int64_t peer, int32_t x, int32_t y)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->scrollBy(x
            , y);
}


WKJ_EXPORT void wkj_dom_DOMWindow_scrollTo(int64_t peer, int32_t x, int32_t y)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->scrollTo(x
            , y);
}


WKJ_EXPORT void wkj_dom_DOMWindow_scroll(int64_t peer, int32_t x, int32_t y)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->scrollTo(x, y);
}


WKJ_EXPORT void wkj_dom_DOMWindow_moveBy(int64_t peer, float x, float y)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->moveBy(x
            , y);
}


WKJ_EXPORT void wkj_dom_DOMWindow_moveTo(int64_t peer, float x, float y)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->moveTo(x
            , y);
}


WKJ_EXPORT void wkj_dom_DOMWindow_resizeBy(int64_t peer, float x, float y)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->resizeBy(x
            , y);
}


WKJ_EXPORT void wkj_dom_DOMWindow_resizeTo(int64_t peer, float width, float height)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->resizeTo(width
            , height);
}


WKJ_EXPORT int64_t wkj_dom_DOMWindow_getComputedStyle(int64_t peer, int64_t element, const uint16_t* pseudoElement, int32_t pseudoElement_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!element) {
        raiseTypeErrorException();
        return {};
    }
    return WKJReturnPeer<CSSStyleDeclaration>(WTF::getPtr(IMPL->getComputedStyle(*static_cast<Element*>(wkj_to_ptr(element))
            , AtomString{WKJString(pseudoElement, pseudoElement_length)})));
}


WKJ_EXPORT void wkj_dom_DOMWindow_captureEvents(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->captureEvents();
}


WKJ_EXPORT void wkj_dom_DOMWindow_releaseEvents(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->releaseEvents();
}


WKJ_EXPORT void wkj_dom_DOMWindow_addEventListener(int64_t peer, const uint16_t* type, int32_t type_length, int64_t listener, int32_t useCapture)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->addEventListenerForBindings(AtomString{WKJString(type, type_length)}
            , static_cast<EventListener*>(wkj_to_ptr(listener))
            , static_cast<bool>(useCapture));
}


WKJ_EXPORT void wkj_dom_DOMWindow_removeEventListener(int64_t peer, const uint16_t* type, int32_t type_length, int64_t listener, int32_t useCapture)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->removeEventListenerForBindings(AtomString{WKJString(type, type_length)}
            , static_cast<EventListener*>(wkj_to_ptr(listener))
            , static_cast<bool>(useCapture));
}


WKJ_EXPORT int32_t wkj_dom_DOMWindow_dispatchEvent(int64_t peer, int64_t event)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!event) {
        raiseTypeErrorException();
        return 0;
    }
    return raiseOnDOMError(IMPL->dispatchEventForBindings(*static_cast<Event*>(wkj_to_ptr(event))));
}


WKJ_EXPORT int32_t wkj_dom_DOMWindow_atob(int64_t peer, const uint16_t* string, int32_t string_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, raiseOnDOMError(IMPL->DOMWindow::atob(AtomString{WKJString(string, string_length)})));
}


WKJ_EXPORT int32_t wkj_dom_DOMWindow_btoa(int64_t peer, const uint16_t* string, int32_t string_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, raiseOnDOMError(IMPL->DOMWindow::atob(AtomString{WKJString(string, string_length)})));
}


WKJ_EXPORT void wkj_dom_DOMWindow_clearTimeout(int64_t peer, int32_t handle)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->clearTimeout(handle);
}


WKJ_EXPORT void wkj_dom_DOMWindow_clearInterval(int64_t peer, int32_t handle)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->clearInterval(handle);
}


}
