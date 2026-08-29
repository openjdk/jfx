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


#include <WebCore/HTMLNames.h>
#include <WebCore/HTMLOListElement.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLOListElement*>(wkj_to_ptr(peer)))


// Attributes
WKJ_EXPORT int32_t wkj_dom_HTMLOListElement_getCompact(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::compactAttr);
}

WKJ_EXPORT void wkj_dom_HTMLOListElement_setCompact(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::compactAttr, value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLOListElement_getStart(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->start();
}

WKJ_EXPORT int32_t wkj_dom_HTMLOListElement_getReversed(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::reversedAttr);
}

WKJ_EXPORT void wkj_dom_HTMLOListElement_setReversed(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::reversedAttr, value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLOListElement_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::typeAttr));
}

WKJ_EXPORT void wkj_dom_HTMLOListElement_setType(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::typeAttr, AtomString{WKJString(value, value_length)});
}

}
