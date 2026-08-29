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


#include <WebCore/Attr.h>
#include <WebCore/CDATASection.h>
#include <WebCore/CSSStyleDeclaration.h>
#include <WebCore/Comment.h>
#include <WebCore/CustomElementRegistry.h>
#include <WebCore/DOMException.h>
#include <WebCore/DOMImplementation.h>
#include <WebCore/DOMWindow.h>
#include <WebCore/Document.h>
#include "DocumentInlines.h"
#include <WebCore/DocumentFragment.h>
#include <WebCore/DocumentType.h>
#include <WebCore/Element.h>
#include <WebCore/Event.h>
#include <WebCore/EventListener.h>
#include <WebCore/HTMLCollection.h>
#include <WebCore/HTMLElement.h>
#include <WebCore/HTMLHeadElement.h>
#include <WebCore/HTMLScriptElement.h>
#include <WebCore/ImportNodeOptions.h>
#include <WebCore/Node.h>
#include <WebCore/NodeFilter.h>
#include <WebCore/NodeIterator.h>
#include <WebCore/NodeList.h>
#include <WebCore/ProcessingInstruction.h>
#include <WebCore/Range.h>
#include <WebCore/SecurityOrigin.h>
#include <WebCore/ScrollIntoViewOptions.h>
#include <WebCore/StyleSheetList.h>
#include <WebCore/Text.h>
#include <WebCore/TreeWalker.h>
#include <WebCore/XPathExpression.h>
#include <WebCore/XPathNSResolver.h>
#include <WebCore/XPathResult.h>
#include <WebCore/EventNames.h>
#include <WebCore/JSExecState.h>
#include <WebCore/VisibilityState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include "AbstractViewInternal.h"
#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

enum class VisibilityState : bool;

extern "C" {

#define IMPL (static_cast<Document*>(wkj_to_ptr(peer)))

WKJ_EXPORT int32_t wkj_dom_Document_isHTMLDocument(int64_t peer)
{
    WKJCallScope wkjScope;
    return IMPL->isHTMLDocument() || IMPL->isXHTMLDocument();
}


// Attributes
WKJ_EXPORT int64_t wkj_dom_Document_getDoctype(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<DocumentType>(WTF::getPtr(IMPL->doctype()));
}

WKJ_EXPORT int64_t wkj_dom_Document_getImplementation(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<DOMImplementation>(WTF::getPtr(IMPL->implementation()));
}

WKJ_EXPORT int64_t wkj_dom_Document_getDocumentElement(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(IMPL->documentElement()));
}

WKJ_EXPORT int32_t wkj_dom_Document_getInputEncoding(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->characterSetWithUTF8Fallback());
}

WKJ_EXPORT int32_t wkj_dom_Document_getXmlEncoding(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->xmlEncoding());
}

WKJ_EXPORT int32_t wkj_dom_Document_getXmlVersion(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->xmlVersion());
}

WKJ_EXPORT void wkj_dom_Document_setXmlVersion(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setXMLVersion(AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_Document_getXmlStandalone(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->xmlStandalone();
}

WKJ_EXPORT void wkj_dom_Document_setXmlStandalone(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setXMLStandalone(value);
}

WKJ_EXPORT int32_t wkj_dom_Document_getDocumentURI(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->documentURI());
}

WKJ_EXPORT void wkj_dom_Document_setDocumentURI(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setDocumentURI(AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int64_t wkj_dom_Document_getDefaultView(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<DOMWindow>(WTF::getPtr(toDOMWindow(IMPL->windowProxy())));
}

WKJ_EXPORT int64_t wkj_dom_Document_getStyleSheets(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<StyleSheetList>(WTF::getPtr(IMPL->styleSheets()));
}

WKJ_EXPORT int32_t wkj_dom_Document_getContentType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->contentType());
}

WKJ_EXPORT int32_t wkj_dom_Document_getTitle(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->title());
}

WKJ_EXPORT void wkj_dom_Document_setTitle(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setTitle(WKJString(value, value_length));
}

WKJ_EXPORT int32_t wkj_dom_Document_getReferrer(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->referrer());
}

WKJ_EXPORT int32_t wkj_dom_Document_getDomain(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->domain());
}

WKJ_EXPORT int32_t wkj_dom_Document_getURL(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->urlForBindings().string());
}

WKJ_EXPORT int32_t wkj_dom_Document_getCookie(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, raiseOnDOMError(IMPL->cookie()));
}

WKJ_EXPORT void wkj_dom_Document_setCookie(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setCookie(AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int64_t wkj_dom_Document_getBody(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLElement>(WTF::getPtr(IMPL->bodyOrFrameset()));
}

WKJ_EXPORT void wkj_dom_Document_setBody(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBodyOrFrameset(static_cast<HTMLElement*>(wkj_to_ptr(value)));
}

WKJ_EXPORT int64_t wkj_dom_Document_getHead(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLHeadElement>(WTF::getPtr(IMPL->head()));
}

WKJ_EXPORT int64_t wkj_dom_Document_getImages(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLCollection>(WTF::getPtr(IMPL->images()));
}

WKJ_EXPORT int64_t wkj_dom_Document_getApplets(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLCollection>(WTF::getPtr(IMPL->applets()));
}

WKJ_EXPORT int64_t wkj_dom_Document_getLinks(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLCollection>(WTF::getPtr(IMPL->links()));
}

WKJ_EXPORT int64_t wkj_dom_Document_getForms(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLCollection>(WTF::getPtr(IMPL->forms()));
}

WKJ_EXPORT int64_t wkj_dom_Document_getAnchors(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLCollection>(WTF::getPtr(IMPL->anchors()));
}

WKJ_EXPORT int32_t wkj_dom_Document_getLastModified(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->lastModified());
}

WKJ_EXPORT int32_t wkj_dom_Document_getCharset(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->characterSetWithUTF8Fallback());
}

WKJ_EXPORT int32_t wkj_dom_Document_getDefaultCharset(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->defaultCharsetForLegacyBindings());
}

WKJ_EXPORT int32_t wkj_dom_Document_getReadyState(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    auto readyState = IMPL->readyState();
    const char* readyStateStr { };
    switch (readyState) {
    case WebCore::Document::ReadyState::Loading:
        readyStateStr = "loading";
        break;
    case WebCore::Document::ReadyState::Interactive:
        readyStateStr = "interactive";
        break;
    case WebCore::Document::ReadyState::Complete:
        readyStateStr = "complete";
        break;
    default:
        ASSERT_NOT_REACHED();
    }
    return WKJReturnString(result_buf, result_cap, result_length, String::fromLatin1(readyStateStr));
}

WKJ_EXPORT int32_t wkj_dom_Document_getCharacterSet(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->characterSetWithUTF8Fallback());
}

WKJ_EXPORT int32_t wkj_dom_Document_getPreferredStylesheetSet(int64_t arg0, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    return nullptr;
}

WKJ_EXPORT int32_t wkj_dom_Document_getSelectedStylesheetSet(int64_t arg0, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    return nullptr;
}

WKJ_EXPORT void wkj_dom_Document_setSelectedStylesheetSet(int64_t arg0, const uint16_t* arg1, int32_t arg1_length)
{
    WKJCallScope wkjScope;
}

WKJ_EXPORT int64_t wkj_dom_Document_getActiveElement(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(IMPL->activeElement()));
}

WKJ_EXPORT int32_t wkj_dom_Document_getCompatMode(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->compatMode());
}

WKJ_EXPORT int32_t wkj_dom_Document_getVisibilityState(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    const char* visibility {};
    switch (IMPL->visibilityState()) {
    case WebCore::VisibilityState::Hidden:
        visibility = "hidden";
        break;
    case WebCore::VisibilityState::Visible:
        visibility = "visible";
        break;
    }
    return WKJReturnString(result_buf, result_cap, result_length, String::fromLatin1(visibility));
}

WKJ_EXPORT int32_t wkj_dom_Document_getHidden(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hidden();
}

WKJ_EXPORT int64_t wkj_dom_Document_getCurrentScript(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    WebCore::Element* element = IMPL->currentScript();
    if (!is<WebCore::HTMLScriptElement>(element))
        return 0;
    return WKJReturnPeer<HTMLScriptElement>(WTF::getPtr(downcast<WebCore::HTMLScriptElement>(element)));
}

WKJ_EXPORT int32_t wkj_dom_Document_getOrigin(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->securityOrigin().toString());
}

WKJ_EXPORT int64_t wkj_dom_Document_getScrollingElement(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(IMPL->scrollingElementForAPI()));
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnbeforecopy(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return 0;
}

WKJ_EXPORT void wkj_dom_Document_setOnbeforecopy(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforecopyEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnbeforecut(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return 0;
}

WKJ_EXPORT void wkj_dom_Document_setOnbeforecut(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforecutEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnbeforepaste(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().beforepasteEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnbeforepaste(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforepasteEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOncopy(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().copyEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOncopy(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().copyEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOncut(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().cutEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOncut(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().cutEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnpaste(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().pasteEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnpaste(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pasteEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnselectstart(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().selectstartEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnselectstart(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().selectstartEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnselectionchange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().selectionchangeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnselectionchange(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().selectionchangeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnreadystatechange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().readystatechangeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnreadystatechange(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().readystatechangeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnabort(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().abortEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnabort(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().abortEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnblur(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().blurEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnblur(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().blurEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOncanplay(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().canplayEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOncanplay(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().canplayEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOncanplaythrough(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().canplaythroughEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOncanplaythrough(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().canplaythroughEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnchange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().changeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnchange(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().changeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnclick(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().clickEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnclick(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().clickEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOncontextmenu(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().contextmenuEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOncontextmenu(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().contextmenuEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOndblclick(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dblclickEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOndblclick(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dblclickEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOndrag(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dragEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOndrag(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOndragend(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dragendEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOndragend(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragendEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOndragenter(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dragenterEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOndragenter(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragenterEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOndragleave(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dragleaveEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOndragleave(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragleaveEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOndragover(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dragoverEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOndragover(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragoverEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOndragstart(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dragstartEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOndragstart(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragstartEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOndrop(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().dropEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOndrop(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dropEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOndurationchange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().durationchangeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOndurationchange(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().durationchangeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnemptied(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().emptiedEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnemptied(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().emptiedEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnended(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().endedEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnended(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().endedEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnerror(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().errorEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnerror(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().errorEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnfocus(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().focusEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnfocus(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOninput(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().inputEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOninput(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().inputEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOninvalid(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().invalidEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOninvalid(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().invalidEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnkeydown(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().keydownEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnkeydown(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().keydownEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnkeypress(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().keypressEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnkeypress(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().keypressEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnkeyup(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().keyupEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnkeyup(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().keyupEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnload(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().loadEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnload(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnloadeddata(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().loadeddataEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnloadeddata(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadeddataEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnloadedmetadata(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().loadedmetadataEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnloadedmetadata(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadedmetadataEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnloadstart(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().loadstartEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnloadstart(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadstartEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnmousedown(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mousedownEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnmousedown(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mousedownEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnmouseenter(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseenterEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnmouseenter(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseenterEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnmouseleave(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseleaveEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnmouseleave(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseleaveEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnmousemove(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mousemoveEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnmousemove(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mousemoveEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnmouseout(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseoutEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnmouseout(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseoutEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnmouseover(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseoverEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnmouseover(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseoverEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnmouseup(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseupEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnmouseup(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseupEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnmousewheel(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().mousewheelEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnmousewheel(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mousewheelEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnpause(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().pauseEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnpause(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pauseEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnplay(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().playEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnplay(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().playEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnplaying(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().playingEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnplaying(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().playingEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnprogress(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().progressEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnprogress(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().progressEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnratechange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().ratechangeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnratechange(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().ratechangeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnreset(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().resetEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnreset(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().resetEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnresize(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().resizeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnresize(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().resizeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnscroll(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().scrollEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnscroll(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().scrollEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnseeked(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().seekedEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnseeked(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().seekedEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnseeking(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().seekingEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnseeking(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().seekingEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnselect(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().selectEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnselect(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().selectEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnstalled(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().stalledEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnstalled(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().stalledEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnsubmit(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().submitEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnsubmit(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().submitEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnsuspend(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().suspendEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnsuspend(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().suspendEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOntimeupdate(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().timeupdateEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOntimeupdate(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().timeupdateEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnvolumechange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().volumechangeEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnvolumechange(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().volumechangeEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnwaiting(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().waitingEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnwaiting(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().waitingEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getOnwheel(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<EventListener>(WTF::getPtr(IMPL->attributeEventListener(eventNames().wheelEvent, mainThreadNormalWorldSingleton())));
}

WKJ_EXPORT void wkj_dom_Document_setOnwheel(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().wheelEvent, static_cast<EventListener*>(wkj_to_ptr(value)), mainThreadNormalWorldSingleton());
}

WKJ_EXPORT int64_t wkj_dom_Document_getChildren(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLCollection>(WTF::getPtr(IMPL->children()));
}

WKJ_EXPORT int64_t wkj_dom_Document_getFirstElementChild(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(IMPL->firstElementChild()));
}

WKJ_EXPORT int64_t wkj_dom_Document_getLastElementChild(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(IMPL->lastElementChild()));
}

WKJ_EXPORT int32_t wkj_dom_Document_getChildElementCount(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->childElementCount();
}


// Functions
WKJ_EXPORT int64_t wkj_dom_Document_createElement(int64_t peer, const uint16_t* tagName, int32_t tagName_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(raiseOnDOMError(IMPL->createElementForBindings(AtomString {WKJString(tagName, tagName_length)}))));
}


WKJ_EXPORT int64_t wkj_dom_Document_createDocumentFragment(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<DocumentFragment>(WTF::getPtr(IMPL->createDocumentFragment()));
}


WKJ_EXPORT int64_t wkj_dom_Document_createTextNode(int64_t peer, const uint16_t* data, int32_t data_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Text>(WTF::getPtr(IMPL->createTextNode(WKJString(data, data_length))));
}


WKJ_EXPORT int64_t wkj_dom_Document_createComment(int64_t peer, const uint16_t* data, int32_t data_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<WebCore::Comment>(WTF::getPtr(IMPL->createComment(WKJString(data, data_length))));
}


WKJ_EXPORT int64_t wkj_dom_Document_createCDATASection(int64_t peer, const uint16_t* data, int32_t data_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<CDATASection>(WTF::getPtr(raiseOnDOMError(IMPL->createCDATASection(WKJString(data, data_length)))));
}


WKJ_EXPORT int64_t wkj_dom_Document_createProcessingInstruction(int64_t peer, const uint16_t* target, int32_t target_length, const uint16_t* data, int32_t data_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<ProcessingInstruction>(WTF::getPtr(raiseOnDOMError(IMPL->createProcessingInstruction(WKJString(target, target_length)
            , WKJString(data, data_length)))));
}


WKJ_EXPORT int64_t wkj_dom_Document_createAttribute(int64_t peer, const uint16_t* name, int32_t name_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Attr>(WTF::getPtr(raiseOnDOMError(IMPL->createAttribute(AtomString {WKJString(name, name_length)}))));
}


WKJ_EXPORT int64_t wkj_dom_Document_createEntityReference(int64_t arg0, const uint16_t* arg1, int32_t arg1_length)
{
    WKJCallScope wkjScope;
    raiseNotSupportedErrorException();
    return {};
}


WKJ_EXPORT int64_t wkj_dom_Document_getElementsByTagName(int64_t peer, const uint16_t* tagname, int32_t tagname_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<NodeList>(WTF::getPtr(IMPL->getElementsByTagName(AtomString {WKJString(tagname, tagname_length)})));
}


WKJ_EXPORT int64_t wkj_dom_Document_importNode(int64_t peer, int64_t importedNode, int32_t deep)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!importedNode) {
        raiseTypeErrorException();
        return 0;
    }

   return WKJReturnPeer<Node>(WTF::getPtr(
       raiseOnDOMError(IMPL->importNode(*static_cast<Node*>(wkj_to_ptr(importedNode)),
                            Variant<bool, ImportNodeOptions>(static_cast<bool>(deep))))));
}


WKJ_EXPORT int64_t wkj_dom_Document_createElementNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* qualifiedName, int32_t qualifiedName_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(raiseOnDOMError(IMPL->createElementNS(AtomString {WKJString(namespaceURI, namespaceURI_length)}
            , AtomString {WKJString(qualifiedName, qualifiedName_length)}))));
}


WKJ_EXPORT int64_t wkj_dom_Document_createAttributeNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* qualifiedName, int32_t qualifiedName_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Attr>(WTF::getPtr(raiseOnDOMError(IMPL->createAttributeNS(AtomString {WKJString(namespaceURI, namespaceURI_length)}
            , AtomString {WKJString(qualifiedName, qualifiedName_length)}))));
}


WKJ_EXPORT int64_t wkj_dom_Document_getElementsByTagNameNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* localName, int32_t localName_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<NodeList>(WTF::getPtr(IMPL->getElementsByTagNameNS(AtomString {WKJString(namespaceURI, namespaceURI_length)}
            , AtomString {WKJString(localName, localName_length)})));
}


WKJ_EXPORT int64_t wkj_dom_Document_adoptNode(int64_t peer, int64_t source)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!source) {
        raiseTypeErrorException();
        return 0;
    }

    return WKJReturnPeer<Node>(WTF::getPtr(raiseOnDOMError(IMPL->adoptNode(*static_cast<Node*>(wkj_to_ptr(source))))));
}


WKJ_EXPORT int64_t wkj_dom_Document_createEvent(int64_t peer, const uint16_t* eventType, int32_t eventType_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Event>(WTF::getPtr(raiseOnDOMError(IMPL->createEvent(AtomString {WKJString(eventType, eventType_length)}))));
}


WKJ_EXPORT int64_t wkj_dom_Document_createRange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Range>(WTF::getPtr(IMPL->createRange()));
}


WKJ_EXPORT int64_t wkj_dom_Document_createNodeIterator(int64_t arg0, int64_t arg1, int32_t arg2, int64_t arg3, int32_t arg4)
{
    WKJCallScope wkjScope;
#if 0
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<NodeIterator>(WTF::getPtr(raiseOnDOMError(IMPL->createNodeIterator(static_cast<Node*>(wkj_to_ptr(root))
            , whatToShow
            , static_cast<NodeFilter*>(wkj_to_ptr(filter))
            , expandEntityReferences))));
#endif
    return 0L;
}


WKJ_EXPORT int64_t wkj_dom_Document_createTreeWalker(int64_t arg0, int64_t arg1, int32_t arg2, int64_t arg3, int32_t arg4)
{
    WKJCallScope wkjScope;
#if 0
    WebCore::JSMainThreadNullState state;
    if (!root) {
        raiseTypeErrorException();
        return 0;
    }

    RefPtr<WebCore::NodeFilter> nativeNodeFilter;
    if (filter)
        nativeNodeFilter = WebCore::NativeNodeFilter::create(WebCore::ObjCNodeFilterCondition::create(filter));
    return WKJReturnPeer<TreeWalker>(WTF::getPtr(raiseOnDOMError(IMPL->createTreeWalker(static_cast<Node*>(wkj_to_ptr(root))
            , whatToShow
            , static_cast<NodeFilter*>(wkj_to_ptr(filter))
            , expandEntityReferences))));
#endif
    return 0L;
}


WKJ_EXPORT int64_t wkj_dom_Document_getOverrideStyle(int64_t arg0, int64_t arg1, const uint16_t* arg2, int32_t arg2_length)
{
    WKJCallScope wkjScope;
#if 0
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<CSSStyleDeclaration>(WTF::getPtr(IMPL->getOverrideStyle(static_cast<Element*>(wkj_to_ptr(element))
            , AtomString {WKJString(pseudoElement, pseudoElement_length)})));
#endif
    return 0L;
}


WKJ_EXPORT int64_t wkj_dom_Document_createExpression(int64_t peer, const uint16_t* expression, int32_t expression_length, int64_t resolver)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<XPathExpression>(WTF::getPtr(raiseOnDOMError(IMPL->createExpression(AtomString {WKJString(expression, expression_length)}
            , static_cast<XPathNSResolver*>(wkj_to_ptr(resolver))))));
}


WKJ_EXPORT int64_t wkj_dom_Document_createNSResolver(int64_t peer, int64_t nodeResolver)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!nodeResolver)
        return 0;

    return WKJReturnPeer<XPathNSResolver>(WTF::getPtr(IMPL->createNSResolver(*static_cast<Node*>(wkj_to_ptr(nodeResolver)))));
}

// - (DOMXPathResult *)evaluate:(NSString *)expression
// contextNode:(DOMNode *)contextNode
// resolver:(id <DOMXPathNSResolver>)resolver
// type:(unsigned short)type
// inResult:(DOMXPathResult *)inResult WEBKIT_AVAILABLE_MAC(10_5);

WKJ_EXPORT int64_t wkj_dom_Document_evaluate(int64_t peer, const uint16_t* expression, int32_t expression_length, int64_t contextNode, int64_t resolver, int16_t type, int64_t inResult)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<XPathResult>(WTF::getPtr(raiseOnDOMError(IMPL->evaluate(AtomString {WKJString(expression, expression_length)}
            , *static_cast<Node*>(wkj_to_ptr(contextNode))
            , static_cast<XPathNSResolver*>(wkj_to_ptr(resolver))
            , type
            , static_cast<XPathResult*>(wkj_to_ptr(inResult))))));
}


WKJ_EXPORT int32_t wkj_dom_Document_execCommand(int64_t peer, const uint16_t* command, int32_t command_length, int32_t userInterface, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->execCommand(AtomString {WKJString(command, command_length)}
            , userInterface
            , AtomString {WKJString(value, value_length)}).returnValue();
}


WKJ_EXPORT int32_t wkj_dom_Document_queryCommandEnabled(int64_t peer, const uint16_t* command, int32_t command_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->queryCommandEnabled(AtomString {WKJString(command, command_length)}).returnValue();
}


WKJ_EXPORT int32_t wkj_dom_Document_queryCommandIndeterm(int64_t peer, const uint16_t* command, int32_t command_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->queryCommandIndeterm(AtomString {WKJString(command, command_length)}).returnValue();
}


WKJ_EXPORT int32_t wkj_dom_Document_queryCommandState(int64_t peer, const uint16_t* command, int32_t command_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->queryCommandState(AtomString {WKJString(command, command_length)}).returnValue();
}


WKJ_EXPORT int32_t wkj_dom_Document_queryCommandSupported(int64_t peer, const uint16_t* command, int32_t command_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->queryCommandSupported(AtomString {WKJString(command, command_length)}).returnValue();
}


WKJ_EXPORT int32_t wkj_dom_Document_queryCommandValue(int64_t peer, const uint16_t* command, int32_t command_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->queryCommandValue(AtomString {WKJString(command, command_length)}).returnValue());
}


WKJ_EXPORT int64_t wkj_dom_Document_getElementsByName(int64_t peer, const uint16_t* elementName, int32_t elementName_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<NodeList>(WTF::getPtr(IMPL->getElementsByName(AtomString {WKJString(elementName, elementName_length)})));
}


WKJ_EXPORT int64_t wkj_dom_Document_elementFromPoint(int64_t peer, int32_t x, int32_t y)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(IMPL->elementFromPoint(x
            , y)));
}


WKJ_EXPORT int64_t wkj_dom_Document_caretRangeFromPoint(int64_t peer, int32_t x, int32_t y)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Range>(WTF::getPtr(IMPL->caretRangeFromPoint(x
            , y)));
}


WKJ_EXPORT int64_t wkj_dom_Document_createCSSStyleDeclaration(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return 0;
}


WKJ_EXPORT int64_t wkj_dom_Document_getElementsByClassName(int64_t peer, const uint16_t* classNames, int32_t classNames_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLCollection>(WTF::getPtr(IMPL->getElementsByClassName(AtomString {WKJString(classNames, classNames_length)})));
}


WKJ_EXPORT int32_t wkj_dom_Document_hasFocus(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasFocus();
}

WKJ_EXPORT int64_t wkj_dom_Document_getElementById(int64_t peer, const uint16_t* elementId, int32_t elementId_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(IMPL->getElementById(AtomString {WKJString(elementId, elementId_length)})));
}


WKJ_EXPORT int64_t wkj_dom_Document_querySelector(int64_t peer, const uint16_t* selectors, int32_t selectors_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(raiseOnDOMError(IMPL->querySelector(AtomString {WKJString(selectors, selectors_length)}))));
}


WKJ_EXPORT int64_t wkj_dom_Document_querySelectorAll(int64_t peer, const uint16_t* selectors, int32_t selectors_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<NodeList>(WTF::getPtr(raiseOnDOMError(IMPL->querySelectorAll(AtomString {WKJString(selectors, selectors_length)}))));
}


}
