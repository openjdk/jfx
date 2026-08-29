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


#include <WebCore/DOMException.h>
#include <WebCore/Node.h>
#include <WebCore/NodeFilter.h>
#include <WebCore/TreeWalker.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<TreeWalker*>(wkj_to_ptr(peer)))

WKJ_EXPORT void wkj_dom_TreeWalker_dispose(int64_t peer)
{
    WKJCallScope wkjScope;
    IMPL->deref();
}


// Attributes
WKJ_EXPORT int64_t wkj_dom_TreeWalker_getRoot(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->root()));
}

WKJ_EXPORT int32_t wkj_dom_TreeWalker_getWhatToShow(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->whatToShow();
}

WKJ_EXPORT int64_t wkj_dom_TreeWalker_getFilter(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<NodeFilter>(WTF::getPtr(IMPL->filter()));
}

WKJ_EXPORT int32_t wkj_dom_TreeWalker_getExpandEntityReferences(int64_t arg0)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return 0;
}

WKJ_EXPORT int64_t wkj_dom_TreeWalker_getCurrentNode(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->currentNode()));
}

WKJ_EXPORT void wkj_dom_TreeWalker_setCurrentNode(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!value) {
        raiseTypeErrorException();
        return;
    }
    IMPL->setCurrentNode(*static_cast<Node*>(wkj_to_ptr(value)));
}


// Functions
WKJ_EXPORT int64_t wkj_dom_TreeWalker_parentNode(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;

    auto result = IMPL->parentNode();
    if (result.hasException()) {
        return {};
    }
    return WKJReturnPeer<Node>(WTF::getPtr(result.releaseReturnValue()));
}


WKJ_EXPORT int64_t wkj_dom_TreeWalker_firstChild(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;

    auto result = IMPL->firstChild();
    if (result.hasException()) {
        return {};
    }
    return WKJReturnPeer<Node>(WTF::getPtr(result.releaseReturnValue()));
}


WKJ_EXPORT int64_t wkj_dom_TreeWalker_lastChild(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;

    auto result = IMPL->lastChild();
    if (result.hasException()) {
        return {};
    }
    return WKJReturnPeer<Node>(WTF::getPtr(result.releaseReturnValue()));
}


WKJ_EXPORT int64_t wkj_dom_TreeWalker_previousSibling(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;

    auto result = IMPL->previousSibling();
    if (result.hasException()) {
        return {};
    }
    return WKJReturnPeer<Node>(WTF::getPtr(result.releaseReturnValue()));
}


WKJ_EXPORT int64_t wkj_dom_TreeWalker_nextSibling(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;

    auto result = IMPL->nextSibling();
    if (result.hasException()) {
        return {};
    }
    return WKJReturnPeer<Node>(WTF::getPtr(result.releaseReturnValue()));
}


WKJ_EXPORT int64_t wkj_dom_TreeWalker_previousNode(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;

    auto result = IMPL->previousNode();
    if (result.hasException()) {
        return {};
    }
    return WKJReturnPeer<Node>(WTF::getPtr(result.releaseReturnValue()));
}


WKJ_EXPORT int64_t wkj_dom_TreeWalker_nextNode(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;

    auto result = IMPL->nextNode();
    if (result.hasException()) {
        return {};
    }
    return WKJReturnPeer<Node>(WTF::getPtr(result.releaseReturnValue()));
}


}
