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
#include <WebCore/Document.h>
#include <WebCore/DocumentFragment.h>
#include <WebCore/Node.h>
#include <WebCore/Range.h>
#include "BoundaryPointInlines.h"
#include <WebCore/SimpleRange.h>
#include <WebCore/TextIterator.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<WebCore::Range*>(wkj_to_ptr(peer)))

WKJ_EXPORT void wkj_dom_Range_dispose(int64_t peer)
{
    WKJCallScope wkjScope;
    IMPL->deref();
}


// Attributes
WKJ_EXPORT int64_t wkj_dom_Range_getStartContainer(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->startContainer()));
}

WKJ_EXPORT int32_t wkj_dom_Range_getStartOffset(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->startOffset();
}

WKJ_EXPORT int64_t wkj_dom_Range_getEndContainer(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->endContainer()));
}

WKJ_EXPORT int32_t wkj_dom_Range_getEndOffset(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->endOffset();
}

WKJ_EXPORT int32_t wkj_dom_Range_getCollapsed(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->collapsed();
}

WKJ_EXPORT int64_t wkj_dom_Range_getCommonAncestorContainer(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->commonAncestorContainer()));
}

WKJ_EXPORT int32_t wkj_dom_Range_getText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;

    auto range = makeSimpleRange(*IMPL);
    range.start.document().updateLayout();
    return WKJReturnString(result_buf, result_cap, result_length, plainText(range));
}


// Functions
WKJ_EXPORT void wkj_dom_Range_setStart(int64_t peer, int64_t refNode, int32_t offset)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException();
        return;
    }
    raiseOnDOMError(IMPL->setStart(*static_cast<Node*>(wkj_to_ptr(refNode)), offset));
}


WKJ_EXPORT void wkj_dom_Range_setEnd(int64_t peer, int64_t refNode, int32_t offset)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException();
        return;
    }
    raiseOnDOMError(IMPL->setEnd(*static_cast<Node*>(wkj_to_ptr(refNode))
            , offset));
}


WKJ_EXPORT void wkj_dom_Range_setStartBefore(int64_t peer, int64_t refNode)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException();
        return;
    }
    raiseOnDOMError(IMPL->setStartBefore(*static_cast<Node*>(wkj_to_ptr(refNode))));
}


WKJ_EXPORT void wkj_dom_Range_setStartAfter(int64_t peer, int64_t refNode)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException();
        return;
    }
    raiseOnDOMError(IMPL->setStartAfter(*static_cast<Node*>(wkj_to_ptr(refNode))));
}


WKJ_EXPORT void wkj_dom_Range_setEndBefore(int64_t peer, int64_t refNode)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException();
        return;
    }
    raiseOnDOMError(IMPL->setEndBefore(*static_cast<Node*>(wkj_to_ptr(refNode))));
}


WKJ_EXPORT void wkj_dom_Range_setEndAfter(int64_t peer, int64_t refNode)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException();
        return;
    }
    raiseOnDOMError(IMPL->setEndAfter(*static_cast<Node*>(wkj_to_ptr(refNode))));
}


WKJ_EXPORT void wkj_dom_Range_collapse(int64_t peer, int32_t toStart)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->collapse(toStart);
}


WKJ_EXPORT void wkj_dom_Range_selectNode(int64_t peer, int64_t refNode)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException();
        return;
    }
    raiseOnDOMError(IMPL->selectNode(*static_cast<Node*>(wkj_to_ptr(refNode))));
}


WKJ_EXPORT void wkj_dom_Range_selectNodeContents(int64_t peer, int64_t refNode)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException();
        return;
    }
    raiseOnDOMError(IMPL->selectNodeContents(*static_cast<Node*>(wkj_to_ptr(refNode))));
}


WKJ_EXPORT int16_t wkj_dom_Range_compareBoundaryPoints(int64_t peer, int16_t how, int64_t sourceRange)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!sourceRange) {
        raiseTypeErrorException();
        return 0;
    }
    return raiseOnDOMError(IMPL->compareBoundaryPoints(static_cast<WebCore::Range::CompareHow>(how), *static_cast<WebCore::Range*>(wkj_to_ptr(sourceRange))));
}


WKJ_EXPORT void wkj_dom_Range_deleteContents(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->deleteContents());
}


WKJ_EXPORT int64_t wkj_dom_Range_extractContents(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<DocumentFragment>(WTF::getPtr(raiseOnDOMError(IMPL->extractContents())));
}


WKJ_EXPORT int64_t wkj_dom_Range_cloneContents(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<DocumentFragment>(WTF::getPtr(raiseOnDOMError(IMPL->cloneContents())));
}


WKJ_EXPORT void wkj_dom_Range_insertNode(int64_t peer, int64_t newNode)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!newNode) {
        raiseTypeErrorException();
        return;
    }
    raiseOnDOMError(IMPL->insertNode(*static_cast<Node*>(wkj_to_ptr(newNode))));
}


WKJ_EXPORT void wkj_dom_Range_surroundContents(int64_t peer, int64_t newParent)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!newParent) {
        raiseTypeErrorException();
        return;
    }
    raiseOnDOMError(IMPL->surroundContents(*static_cast<Node*>(wkj_to_ptr(newParent))));
}


WKJ_EXPORT int64_t wkj_dom_Range_cloneRange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<WebCore::Range>(WTF::getPtr(IMPL->cloneRange()));
}


WKJ_EXPORT int32_t wkj_dom_Range_toString(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->toString());
}


WKJ_EXPORT void wkj_dom_Range_detach(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->detach();
}


WKJ_EXPORT int64_t wkj_dom_Range_createContextualFragment(int64_t peer, const uint16_t* html, int32_t html_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<DocumentFragment>(WTF::getPtr(raiseOnDOMError(IMPL->createContextualFragment(AtomString {WKJString(html, html_length)}))));
}


WKJ_EXPORT int16_t wkj_dom_Range_compareNode(int64_t peer, int64_t refNode)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException();
        return 0;
    }
    return raiseOnDOMError(IMPL->compareNode(*static_cast<Node*>(wkj_to_ptr(refNode))));
}


WKJ_EXPORT int16_t wkj_dom_Range_comparePoint(int64_t peer, int64_t refNode, int32_t offset)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException();
        return 0;
    }
    return raiseOnDOMError(IMPL->comparePoint(*static_cast<Node*>(wkj_to_ptr(refNode))
            , offset));
}


WKJ_EXPORT int32_t wkj_dom_Range_intersectsNode(int64_t peer, int64_t refNode)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException();
        return 0;
    }
    return IMPL->intersectsNode(*static_cast<Node*>(wkj_to_ptr(refNode)));
}


WKJ_EXPORT int32_t wkj_dom_Range_isPointInRange(int64_t peer, int64_t refNode, int32_t offset)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException();
        return 0;
    }
    return raiseOnDOMError(IMPL->isPointInRange(*static_cast<Node*>(wkj_to_ptr(refNode))
            , offset));
}


WKJ_EXPORT void wkj_dom_Range_expand(int64_t peer, const uint16_t* unit, int32_t unit_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->expand(AtomString{WKJString(unit, unit_length)}));
}


}
