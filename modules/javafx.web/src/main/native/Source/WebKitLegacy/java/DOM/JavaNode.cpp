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

#include <wtf/RefPtr.h>

#include <WebCore/AddEventListenerOptions.h>
#include <WebCore/Document.h>
#include <WebCore/Element.h>
#include <WebCore/ElementInlines.h>
#include <WebCore/Event.h>
#include <WebCore/EventListener.h>
#include <WebCore/EventTarget.h>
#include <WebCore/NamedNodeMap.h>
#include <WebCore/Node.h>
#include <WebCore/NodeList.h>
#include <WebCore/JSExecState.h>
#include <WebCore/SVGTests.h>
#include <JavaScriptCore/APICast.h>
#include "AddEventListenerOptionsInlines.h"

#include <WebCore/DOMException.h>
#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<Node*>(wkj_to_ptr(peer)))

WKJ_EXPORT void wkj_dom_Node_dispose(int64_t peer) {
    WKJCallScope wkjScope;
    IMPL->deref();
}


// Attributes
WKJ_EXPORT int32_t wkj_dom_Node_getNodeName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->nodeName());
}

WKJ_EXPORT int32_t wkj_dom_Node_getNodeValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->nodeValue());
}
WKJ_EXPORT void wkj_dom_Node_setNodeValue(int64_t peer, const uint16_t* value, int32_t value_length) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setNodeValue(AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int16_t wkj_dom_Node_getNodeType(int64_t peer) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->nodeType();
}

WKJ_EXPORT int64_t wkj_dom_Node_getParentNode(int64_t peer) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->parentNode()));
}

WKJ_EXPORT int64_t wkj_dom_Node_getChildNodes(int64_t peer) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<NodeList>(WTF::getPtr(IMPL->childNodes()));
}

WKJ_EXPORT int64_t wkj_dom_Node_getFirstChild(int64_t peer) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->firstChild()));
}

WKJ_EXPORT int64_t wkj_dom_Node_getLastChild(int64_t peer) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->lastChild()));
}

WKJ_EXPORT int64_t wkj_dom_Node_getPreviousSibling(int64_t peer) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->previousSibling()));
}

WKJ_EXPORT int64_t wkj_dom_Node_getNextSibling(int64_t peer) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->nextSibling()));
}

WKJ_EXPORT int64_t wkj_dom_Node_getOwnerDocument(int64_t peer) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Document>(WTF::getPtr(IMPL->ownerDocument()));
}

WKJ_EXPORT int32_t wkj_dom_Node_getNamespaceURI(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->namespaceURI());
}

WKJ_EXPORT int32_t wkj_dom_Node_getPrefix(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->prefix());
}
WKJ_EXPORT void wkj_dom_Node_setPrefix(int64_t peer, const uint16_t* value, int32_t value_length) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setPrefix(AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_Node_getLocalName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->localName());
}

WKJ_EXPORT int64_t wkj_dom_Node_getAttributes(int64_t peer) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<NamedNodeMap>(WTF::getPtr(IMPL->attributesMap()));
}

WKJ_EXPORT int32_t wkj_dom_Node_getBaseURI(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->baseURI().string());
}

WKJ_EXPORT int32_t wkj_dom_Node_getTextContent(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->textContent());
}
WKJ_EXPORT void wkj_dom_Node_setTextContent(int64_t peer, const uint16_t* value, int32_t value_length) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setTextContent(WKJString(value, value_length));
}

WKJ_EXPORT int64_t wkj_dom_Node_getParentElement(int64_t peer) {
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(IMPL->parentElement()));
}


// Functions
WKJ_EXPORT int64_t wkj_dom_Node_insertBefore(int64_t peer, int64_t newChild, int64_t refChild)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!newChild) {
        raiseTypeErrorException();
        return 0;
    }
    Node* pnewChild = static_cast<Node*>(wkj_to_ptr(newChild));
    raiseOnDOMError(IMPL->insertBefore(*pnewChild, static_cast<Node*>(wkj_to_ptr(refChild))));
    return WKJReturnPeer<Node>(pnewChild );
}


WKJ_EXPORT int64_t wkj_dom_Node_replaceChild(int64_t peer, int64_t newChild, int64_t oldChild)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!newChild) {
        raiseTypeErrorException();
        return 0;
    }

    if (!oldChild) {
        raiseTypeErrorException();
        return 0;
    }

    Node* poldChild = static_cast<Node*>(wkj_to_ptr(oldChild));
    raiseOnDOMError(IMPL->replaceChild(*static_cast<Node*>(wkj_to_ptr(newChild)), *poldChild));
    return WKJReturnPeer<Node>(poldChild);
}


WKJ_EXPORT int64_t wkj_dom_Node_removeChild(int64_t peer, int64_t oldChild)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!oldChild) {
        raiseTypeErrorException();
        return 0;
    }
    Node* poldChild = static_cast<Node*>(wkj_to_ptr(oldChild));
    raiseOnDOMError(IMPL->removeChild(*poldChild));
    return WKJReturnPeer<Node>(poldChild);
}


WKJ_EXPORT int64_t wkj_dom_Node_appendChild(int64_t peer, int64_t newChild)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!newChild) {
        raiseTypeErrorException();
        return 0;
    }
    Node* pnewChild = static_cast<Node*>(wkj_to_ptr(newChild));
    raiseOnDOMError(IMPL->appendChild(*pnewChild));
    return WKJReturnPeer<Node>(pnewChild);
}


WKJ_EXPORT int32_t wkj_dom_Node_hasChildNodes(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasChildNodes();
}


WKJ_EXPORT int64_t wkj_dom_Node_cloneNode(int64_t peer, int32_t deep)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(raiseOnDOMError(IMPL->cloneNodeForBindings(deep))));
}


WKJ_EXPORT void wkj_dom_Node_normalize(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->normalize();
}


WKJ_EXPORT int32_t wkj_dom_Node_hasAttributes(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttributes();
}


WKJ_EXPORT int32_t wkj_dom_Node_isSameNode(int64_t peer, int64_t other)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->isSameNode(static_cast<Node*>(wkj_to_ptr(other)));
}


WKJ_EXPORT int32_t wkj_dom_Node_isEqualNode(int64_t peer, int64_t other)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->isEqualNode(static_cast<Node*>(wkj_to_ptr(other)));
}


WKJ_EXPORT int32_t wkj_dom_Node_lookupPrefix(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->lookupPrefix(AtomString {WKJString(namespaceURI, namespaceURI_length)}));
}


WKJ_EXPORT int32_t wkj_dom_Node_isDefaultNamespace(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->isDefaultNamespace(AtomString {WKJString(namespaceURI, namespaceURI_length)});
}


WKJ_EXPORT int32_t wkj_dom_Node_lookupNamespaceURI(int64_t peer, const uint16_t* prefix, int32_t prefix_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->lookupNamespaceURI(AtomString {WKJString(prefix, prefix_length)}));
}


WKJ_EXPORT int16_t wkj_dom_Node_compareDocumentPosition(int64_t peer, int64_t other)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!other)
        return Node::DOCUMENT_POSITION_DISCONNECTED;
    return IMPL->compareDocumentPosition(*static_cast<Node*>(wkj_to_ptr(other)));
}


WKJ_EXPORT int32_t wkj_dom_Node_contains(int64_t peer, int64_t other)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->contains(static_cast<Node*>(wkj_to_ptr(other)));
}


WKJ_EXPORT void wkj_dom_Node_addEventListener(int64_t peer, const uint16_t* type, int32_t type_length, int64_t listener, int32_t useCapture)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->addEventListenerForBindings(AtomString {WKJString(type, type_length)}
            , static_cast<EventListener*>(wkj_to_ptr(listener))
            , static_cast<bool>(useCapture));
}


WKJ_EXPORT void wkj_dom_Node_removeEventListener(int64_t peer, const uint16_t* type, int32_t type_length, int64_t listener, int32_t useCapture)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->removeEventListenerForBindings(AtomString {WKJString(type, type_length)}
            , static_cast<EventListener*>(wkj_to_ptr(listener))
            , static_cast<bool>(useCapture));
}


WKJ_EXPORT int32_t wkj_dom_Node_dispatchEvent(int64_t peer, int64_t event)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!event) {
        raiseTypeErrorException();
        return 0;
    }

    return raiseOnDOMError(IMPL->dispatchEventForBindings(*static_cast<Event*>(wkj_to_ptr(event))));
}


}
