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

#include <wtf/RefPtr.h>

#include <WebCore/Document.h>
#include <WebCore/Element.h>
#include <WebCore/Event.h>
#include <WebCore/EventListener.h>
#include <WebCore/EventTarget.h>
#include <WebCore/NamedNodeMap.h>
#include <WebCore/Node.h>
#include <WebCore/NodeList.h>
#include <WebCore/JSExecState.h>
#include <WebCore/SVGTests.h>
#include <JavaScriptCore/APICast.h>

#include <WebCore/DOMException.h>
#include "com_sun_webkit_dom_JSObject.h"
#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<Node*>(jlong_to_ptr(peer)))

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_NodeImpl_dispose(JNIEnv*, jclass, jlong peer) {
    IMPL->deref();
}


// Attributes
JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_NodeImpl_getNodeNameImpl(JNIEnv* env, jclass, jlong peer) {
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->nodeName());
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_NodeImpl_getNodeValueImpl(JNIEnv* env, jclass, jlong peer) {
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->nodeValue());
}
JNIEXPORT void JNICALL Java_com_sun_webkit_dom_NodeImpl_setNodeValueImpl(JNIEnv* env, jclass, jlong peer, jstring value) {
    WebCore::JSMainThreadNullState state;
    IMPL->setNodeValue(String(env, value));
}

JNIEXPORT jshort JNICALL Java_com_sun_webkit_dom_NodeImpl_getNodeTypeImpl(JNIEnv*, jclass, jlong peer) {
    WebCore::JSMainThreadNullState state;
    return IMPL->nodeType();
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_NodeImpl_getParentNodeImpl(JNIEnv* env, jclass, jlong peer) {
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->parentNode()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_NodeImpl_getChildNodesImpl(JNIEnv* env, jclass, jlong peer) {
    WebCore::JSMainThreadNullState state;
    return JavaReturn<NodeList>(env, WTF::getPtr(IMPL->childNodes()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_NodeImpl_getFirstChildImpl(JNIEnv* env, jclass, jlong peer) {
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->firstChild()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_NodeImpl_getLastChildImpl(JNIEnv* env, jclass, jlong peer) {
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->lastChild()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_NodeImpl_getPreviousSiblingImpl(JNIEnv* env, jclass, jlong peer) {
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->previousSibling()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_NodeImpl_getNextSiblingImpl(JNIEnv* env, jclass, jlong peer) {
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->nextSibling()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_NodeImpl_getOwnerDocumentImpl(JNIEnv* env, jclass, jlong peer) {
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Document>(env, WTF::getPtr(IMPL->ownerDocument()));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_NodeImpl_getNamespaceURIImpl(JNIEnv* env, jclass, jlong peer) {
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->namespaceURI());
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_NodeImpl_getPrefixImpl(JNIEnv* env, jclass, jlong peer) {
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->prefix());
}
JNIEXPORT void JNICALL Java_com_sun_webkit_dom_NodeImpl_setPrefixImpl(JNIEnv* env, jclass, jlong peer, jstring value) {
    WebCore::JSMainThreadNullState state;
    IMPL->setPrefix(String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_NodeImpl_getLocalNameImpl(JNIEnv* env, jclass, jlong peer) {
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->localName());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_NodeImpl_getAttributesImpl(JNIEnv* env, jclass, jlong peer) {
    WebCore::JSMainThreadNullState state;
    return JavaReturn<NamedNodeMap>(env, WTF::getPtr(IMPL->attributes()));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_NodeImpl_getBaseURIImpl(JNIEnv* env, jclass, jlong peer) {
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->baseURI().string());
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_NodeImpl_getTextContentImpl(JNIEnv* env, jclass, jlong peer) {
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->textContent());
}
JNIEXPORT void JNICALL Java_com_sun_webkit_dom_NodeImpl_setTextContentImpl(JNIEnv* env, jclass, jlong peer, jstring value) {
    WebCore::JSMainThreadNullState state;
    IMPL->setTextContent(String(env, value));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_NodeImpl_getParentElementImpl(JNIEnv* env, jclass, jlong peer) {
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->parentElement()));
}


// Functions
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_NodeImpl_insertBeforeImpl(JNIEnv* env, jclass, jlong peer
    , jlong newChild
    , jlong refChild)
{
    WebCore::JSMainThreadNullState state;
    if (!newChild) {
        raiseTypeErrorException(env);
        return 0;
    }
    Node* pnewChild = static_cast<Node*>(jlong_to_ptr(newChild));
    raiseOnDOMError(env, IMPL->insertBefore(*pnewChild, static_cast<Node*>(jlong_to_ptr(refChild))));
    return JavaReturn<Node>(env, pnewChild );
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_NodeImpl_replaceChildImpl(JNIEnv* env, jclass, jlong peer
    , jlong newChild
    , jlong oldChild)
{
    WebCore::JSMainThreadNullState state;
    if (!newChild) {
        raiseTypeErrorException(env);
        return 0;
    }

    if (!oldChild) {
        raiseTypeErrorException(env);
        return 0;
    }

    Node* poldChild = static_cast<Node*>(jlong_to_ptr(oldChild));
    raiseOnDOMError(env, IMPL->replaceChild(*static_cast<Node*>(jlong_to_ptr(newChild)), *poldChild));
    return JavaReturn<Node>(env, poldChild);
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_NodeImpl_removeChildImpl(JNIEnv* env, jclass, jlong peer
    , jlong oldChild)
{
    WebCore::JSMainThreadNullState state;
    if (!oldChild) {
        raiseTypeErrorException(env);
        return 0;
    }
    Node* poldChild = static_cast<Node*>(jlong_to_ptr(oldChild));
    raiseOnDOMError(env, IMPL->removeChild(*poldChild));
    return JavaReturn<Node>(env, poldChild);
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_NodeImpl_appendChildImpl(JNIEnv* env, jclass, jlong peer
    , jlong newChild)
{
    WebCore::JSMainThreadNullState state;
    if (!newChild) {
        raiseTypeErrorException(env);
        return 0;
    }
    Node* pnewChild = static_cast<Node*>(jlong_to_ptr(newChild));
    raiseOnDOMError(env, IMPL->appendChild(*pnewChild));
    return JavaReturn<Node>(env, pnewChild);
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_NodeImpl_hasChildNodesImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasChildNodes();
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_NodeImpl_cloneNodeImpl(JNIEnv* env, jclass, jlong peer
    , jboolean deep)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->cloneNodeForBindings(deep))));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_NodeImpl_normalizeImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->normalize();
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_NodeImpl_isSupportedImpl(JNIEnv* env, jclass, jlong
    , jstring feature
    , jstring version)
{
    WebCore::JSMainThreadNullState state;
    return SVGTests::hasFeatureForLegacyBindings(String(env, feature), String(env, version));
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_NodeImpl_hasAttributesImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttributes();
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_NodeImpl_isSameNodeImpl(JNIEnv*, jclass, jlong peer
    , jlong other)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->isSameNode(static_cast<Node*>(jlong_to_ptr(other)));
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_NodeImpl_isEqualNodeImpl(JNIEnv*, jclass, jlong peer
    , jlong other)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->isEqualNode(static_cast<Node*>(jlong_to_ptr(other)));
}


JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_NodeImpl_lookupPrefixImpl(JNIEnv* env, jclass, jlong peer
    , jstring namespaceURI)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->lookupPrefix(String(env, namespaceURI)));
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_NodeImpl_isDefaultNamespaceImpl(JNIEnv* env, jclass, jlong peer
    , jstring namespaceURI)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->isDefaultNamespace(String(env, namespaceURI));
}


JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_NodeImpl_lookupNamespaceURIImpl(JNIEnv* env, jclass, jlong peer
    , jstring prefix)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->lookupNamespaceURI(String(env, prefix)));
}


JNIEXPORT jshort JNICALL Java_com_sun_webkit_dom_NodeImpl_compareDocumentPositionImpl(JNIEnv*, jclass, jlong peer
    , jlong other)
{
    WebCore::JSMainThreadNullState state;
    if (!other)
        return Node::DOCUMENT_POSITION_DISCONNECTED;
    return IMPL->compareDocumentPosition(*static_cast<Node*>(jlong_to_ptr(other)));
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_NodeImpl_containsImpl(JNIEnv*, jclass, jlong peer
    , jlong other)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->contains(static_cast<Node*>(jlong_to_ptr(other)));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_NodeImpl_addEventListenerImpl(JNIEnv* env, jclass, jlong peer
    , jstring type
    , jlong listener
    , jboolean useCapture)
{
    WebCore::JSMainThreadNullState state;
    IMPL->addEventListenerForBindings(String(env, type)
            , static_cast<EventListener*>(jlong_to_ptr(listener))
            , static_cast<bool>(useCapture));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_NodeImpl_removeEventListenerImpl(JNIEnv* env, jclass, jlong peer
    , jstring type
    , jlong listener
    , jboolean useCapture)
{
    WebCore::JSMainThreadNullState state;
    IMPL->removeEventListenerForBindings(String(env, type)
            , static_cast<EventListener*>(jlong_to_ptr(listener))
            , static_cast<bool>(useCapture));
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_NodeImpl_dispatchEventImpl(JNIEnv* env, jclass, jlong peer
    , jlong event)
{
    WebCore::JSMainThreadNullState state;
    if (!event) {
        raiseTypeErrorException(env);
        return JNI_FALSE;
    }

    return raiseOnDOMError(env, IMPL->dispatchEventForBindings(*static_cast<Event*>(jlong_to_ptr(event))));
}


}
