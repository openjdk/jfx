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


#include <WebCore/DOMException.h>
#include <WebCore/Attr.h>
#include <WebCore/NamedNodeMap.h>
#include <WebCore/Node.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<NamedNodeMap*>(wkj_to_ptr(peer)))

WKJ_EXPORT void wkj_dom_NamedNodeMap_dispose(int64_t peer)
{
    WKJCallScope wkjScope;
    IMPL->deref();
}


// Attributes
WKJ_EXPORT int32_t wkj_dom_NamedNodeMap_getLength(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->length();
}


// Functions
WKJ_EXPORT int64_t wkj_dom_NamedNodeMap_getNamedItem(int64_t peer, const uint16_t* name, int32_t name_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->getNamedItem(AtomString {WKJString(name, name_length)})));
}


WKJ_EXPORT int64_t wkj_dom_NamedNodeMap_setNamedItem(int64_t peer, int64_t node)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!node) {
        raiseTypeErrorException();
        return 0;
    }
    auto& coreNode = *static_cast<Node*>(wkj_to_ptr(node));
    if (!is<WebCore::Attr>(coreNode)) {
        raiseTypeErrorException();
        return 0;
    }
    return WKJReturnPeer<Node>(WTF::getPtr(raiseOnDOMError(IMPL->setNamedItem(downcast<WebCore::Attr>(coreNode)))));
}


WKJ_EXPORT int64_t wkj_dom_NamedNodeMap_removeNamedItem(int64_t peer, const uint16_t* name, int32_t name_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(raiseOnDOMError(IMPL->removeNamedItem(AtomString {WKJString(name, name_length)}))));
}


WKJ_EXPORT int64_t wkj_dom_NamedNodeMap_item(int64_t peer, int32_t index)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->item(index)));
}


WKJ_EXPORT int64_t wkj_dom_NamedNodeMap_getNamedItemNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* localName, int32_t localName_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->getNamedItemNS(AtomString {WKJString(namespaceURI, namespaceURI_length)}
            , AtomString{WKJString(localName, localName_length)})));
}


WKJ_EXPORT int64_t wkj_dom_NamedNodeMap_setNamedItemNS(int64_t peer, int64_t node)
{
    WKJCallScope wkjScope;
    return wkj_dom_NamedNodeMap_setNamedItem(peer, node);
}


WKJ_EXPORT int64_t wkj_dom_NamedNodeMap_removeNamedItemNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* localName, int32_t localName_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(raiseOnDOMError(IMPL->removeNamedItemNS(AtomString {WKJString(namespaceURI, namespaceURI_length)}
            , AtomString{WKJString(localName, localName_length)}))));
}


}
