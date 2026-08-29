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


#include <WebCore/CSSStyleSheet.h>
#include <WebCore/MediaList.h>
#include <WebCore/Node.h>
#include <WebCore/StyleSheet.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<StyleSheet*>(wkj_to_ptr(peer)))

WKJ_EXPORT void wkj_dom_StyleSheet_dispose(int64_t peer)
{
    WKJCallScope wkjScope;
    IMPL->deref();
}

WKJ_EXPORT int32_t wkj_dom_StyleSheet_getCPPType(int64_t peer)
{
    WKJCallScope wkjScope;
    if (IMPL->isCSSStyleSheet())
        return 1;
    return 0;
}


// Attributes
WKJ_EXPORT int32_t wkj_dom_StyleSheet_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->type());
}

WKJ_EXPORT int32_t wkj_dom_StyleSheet_getDisabled(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->disabled();
}

WKJ_EXPORT void wkj_dom_StyleSheet_setDisabled(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setDisabled(value);
}

WKJ_EXPORT int64_t wkj_dom_StyleSheet_getOwnerNode(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->ownerNode()));
}

WKJ_EXPORT int64_t wkj_dom_StyleSheet_getParentStyleSheet(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<StyleSheet>(WTF::getPtr(IMPL->parentStyleSheet()));
}

WKJ_EXPORT int32_t wkj_dom_StyleSheet_getHref(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->href());
}

WKJ_EXPORT int32_t wkj_dom_StyleSheet_getTitle(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->title());
}

WKJ_EXPORT int64_t wkj_dom_StyleSheet_getMedia(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<MediaList>(WTF::getPtr(IMPL->media()));
}

}
