/*
 * Copyright (c) 2013, 2025, Oracle and/or its affiliates. All rights reserved.
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
#include <WebCore/HTMLTableCellElement.h>
#include <WebCore/ElementInlines.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLTableCellElement*>(wkj_to_ptr(peer)))


// Attributes
WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getCellIndex(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->cellIndex();
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::alignAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::alignAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getAxis(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::axisAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setAxis(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::axisAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getBgColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::bgcolorAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setBgColor(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::bgcolorAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getCh(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::charAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setCh(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::charAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getChOff(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::charoffAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setChOff(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::charoffAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getColSpan(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->colSpan();
}

WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setColSpan(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setColSpan(value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getRowSpan(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->rowSpanForBindings();
}

WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setRowSpan(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setRowSpanForBindings(value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getHeaders(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::headersAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setHeaders(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::headersAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getHeight(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::heightAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setHeight(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::heightAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getNoWrap(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::nowrapAttr);
}

WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setNoWrap(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::nowrapAttr, value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getVAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::valignAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setVAlign(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::valignAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getWidth(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::widthAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setWidth(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::widthAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getAbbr(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::abbrAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setAbbr(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::abbrAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getScope(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::scopeAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setScope(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::scopeAttr, AtomString{WKJString(value, value_length)});
}

}
