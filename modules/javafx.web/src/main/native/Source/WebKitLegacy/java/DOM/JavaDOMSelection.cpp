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
#include <WebCore/DOMSelection.h>
#include <WebCore/Node.h>
#include <WebCore/Range.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<DOMSelection*>(wkj_to_ptr(peer)))

WKJ_EXPORT void wkj_dom_DOMSelection_dispose(int64_t peer)
{
    WKJCallScope wkjScope;
    IMPL->deref();
}


// Attributes
WKJ_EXPORT int64_t wkj_dom_DOMSelection_getAnchorNode(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->anchorNode()));
}

WKJ_EXPORT int32_t wkj_dom_DOMSelection_getAnchorOffset(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->anchorOffset();
}

WKJ_EXPORT int64_t wkj_dom_DOMSelection_getFocusNode(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->focusNode()));
}

WKJ_EXPORT int32_t wkj_dom_DOMSelection_getFocusOffset(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->focusOffset();
}

WKJ_EXPORT int32_t wkj_dom_DOMSelection_getIsCollapsed(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->isCollapsed();
}

WKJ_EXPORT int32_t wkj_dom_DOMSelection_getRangeCount(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->rangeCount();
}

WKJ_EXPORT int64_t wkj_dom_DOMSelection_getBaseNode(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->baseNode()));
}

WKJ_EXPORT int32_t wkj_dom_DOMSelection_getBaseOffset(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->baseOffset();
}

WKJ_EXPORT int64_t wkj_dom_DOMSelection_getExtentNode(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->extentNode()));
}

WKJ_EXPORT int32_t wkj_dom_DOMSelection_getExtentOffset(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->extentOffset();
}

WKJ_EXPORT int32_t wkj_dom_DOMSelection_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->type());
}


// Functions
WKJ_EXPORT void wkj_dom_DOMSelection_collapse(int64_t peer, int64_t node, int32_t index)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->collapse(static_cast<Node*>(wkj_to_ptr(node))
            , index));
}


WKJ_EXPORT void wkj_dom_DOMSelection_collapseToEnd(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->collapseToEnd());
}


WKJ_EXPORT void wkj_dom_DOMSelection_collapseToStart(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->collapseToStart());
}


WKJ_EXPORT void wkj_dom_DOMSelection_deleteFromDocument(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->deleteFromDocument();
}


WKJ_EXPORT int32_t wkj_dom_DOMSelection_containsNode(int64_t peer, int64_t node, int32_t allowPartial)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->containsNode(static_cast<Node*>(wkj_to_ptr(node))
            , allowPartial);
}


WKJ_EXPORT void wkj_dom_DOMSelection_selectAllChildren(int64_t peer, int64_t node)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->selectAllChildren(static_cast<Node*>(wkj_to_ptr(node))));
}


WKJ_EXPORT void wkj_dom_DOMSelection_extend(int64_t peer, int64_t node, int32_t offset)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->extend(static_cast<Node*>(wkj_to_ptr(node))
            , offset));
}


WKJ_EXPORT int64_t wkj_dom_DOMSelection_getRangeAt(int64_t peer, int32_t index)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Range>(WTF::getPtr(raiseOnDOMError(IMPL->getRangeAt(index))));
}


WKJ_EXPORT void wkj_dom_DOMSelection_removeAllRanges(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->removeAllRanges();
}


WKJ_EXPORT void wkj_dom_DOMSelection_addRange(int64_t peer, int64_t range)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->addRange(static_cast<Range*>(wkj_to_ptr(range)));
}


WKJ_EXPORT void wkj_dom_DOMSelection_modify(int64_t peer, const uint16_t* alter, int32_t alter_length, const uint16_t* direction, int32_t direction_length, const uint16_t* granularity, int32_t granularity_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->modify(AtomString {WKJString(alter, alter_length)}
            , AtomString {WKJString(direction, direction_length)}
            , AtomString {WKJString(granularity, granularity_length)});
}


WKJ_EXPORT void wkj_dom_DOMSelection_setBaseAndExtent(int64_t peer, int64_t baseNode, int32_t baseOffset, int64_t extentNode, int32_t extentOffset)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->setBaseAndExtent(static_cast<Node*>(wkj_to_ptr(baseNode))
            , baseOffset
            , static_cast<Node*>(wkj_to_ptr(extentNode))
            , extentOffset));
}


WKJ_EXPORT void wkj_dom_DOMSelection_setPosition(int64_t peer, int64_t node, int32_t offset)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->setPosition(static_cast<Node*>(wkj_to_ptr(node))
            , offset));
}


WKJ_EXPORT void wkj_dom_DOMSelection_empty(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->empty();
}


}
