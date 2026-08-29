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


#include <WebCore/MutationEvent.h>
#include <WebCore/Node.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<MutationEvent*>(wkj_to_ptr(peer)))


// Attributes
WKJ_EXPORT int64_t wkj_dom_MutationEvent_getRelatedNode(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->relatedNode()));
}

WKJ_EXPORT int32_t wkj_dom_MutationEvent_getPrevValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->prevValue());
}

WKJ_EXPORT int32_t wkj_dom_MutationEvent_getNewValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->newValue());
}

WKJ_EXPORT int32_t wkj_dom_MutationEvent_getAttrName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->attrName());
}

WKJ_EXPORT int16_t wkj_dom_MutationEvent_getAttrChange(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->attrChange();
}


// Functions
WKJ_EXPORT void wkj_dom_MutationEvent_initMutationEvent(int64_t peer, const uint16_t* type, int32_t type_length, int32_t canBubble, int32_t cancelable, int64_t relatedNode, const uint16_t* prevValue, int32_t prevValue_length, const uint16_t* newValue, int32_t newValue_length, const uint16_t* attrName, int32_t attrName_length, int16_t attrChange)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->initMutationEvent(AtomString{WKJString(type, type_length)}
            , canBubble
            , cancelable
            , static_cast<Node*>(wkj_to_ptr(relatedNode))
            , AtomString{WKJString(prevValue, prevValue_length)}
            , AtomString{WKJString(newValue, newValue_length)}
            , AtomString{WKJString(attrName, attrName_length)}
            , attrChange);
}


}
