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
#include <WebCore/DocumentFragment.h>
#include <WebCore/Element.h>
#include <WebCore/HTMLCollection.h>
#include <WebCore/NodeList.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<DocumentFragment*>(wkj_to_ptr(peer)))


// Attributes
WKJ_EXPORT int64_t wkj_dom_DocumentFragment_getChildren(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLCollection>(WTF::getPtr(IMPL->children()));
}

WKJ_EXPORT int64_t wkj_dom_DocumentFragment_getFirstElementChild(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(IMPL->firstElementChild()));
}

WKJ_EXPORT int64_t wkj_dom_DocumentFragment_getLastElementChild(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(IMPL->lastElementChild()));
}

WKJ_EXPORT int32_t wkj_dom_DocumentFragment_getChildElementCount(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->childElementCount();
}


// Functions
WKJ_EXPORT int64_t wkj_dom_DocumentFragment_getElementById(int64_t peer, const uint16_t* elementId, int32_t elementId_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(IMPL->getElementById(AtomString {WKJString(elementId, elementId_length)})));
}


WKJ_EXPORT int64_t wkj_dom_DocumentFragment_querySelector(int64_t peer, const uint16_t* selectors, int32_t selectors_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(raiseOnDOMError(IMPL->querySelector(AtomString {WKJString(selectors, selectors_length)}))));
}


WKJ_EXPORT int64_t wkj_dom_DocumentFragment_querySelectorAll(int64_t peer, const uint16_t* selectors, int32_t selectors_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<NodeList>(WTF::getPtr(raiseOnDOMError(IMPL->querySelectorAll(AtomString {WKJString(selectors, selectors_length)}))));
}


}
