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
#include <WebCore/HTMLCollection.h>
#include <WebCore/ElementInlines.h>
#include <WebCore/HTMLElement.h>
#include "HTMLTableRowElement.h"
#include <WebCore/HTMLNames.h>
#include <WebCore/HTMLTableCaptionElement.h>
#include <WebCore/HTMLTableElement.h>
#include <WebCore/HTMLTableSectionElement.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLTableElement*>(wkj_to_ptr(peer)))


// Attributes
WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_getCaption(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLTableCaptionElement>(WTF::getPtr(IMPL->caption()));
}

WKJ_EXPORT void wkj_dom_HTMLTableElement_setCaption(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setCaption(static_cast<HTMLTableCaptionElement*>(wkj_to_ptr(value)));
}

WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_getTHead(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLTableSectionElement>(WTF::getPtr(IMPL->tHead()));
}

WKJ_EXPORT void wkj_dom_HTMLTableElement_setTHead(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setTHead(static_cast<HTMLTableSectionElement*>(wkj_to_ptr(value)));
}

WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_getTFoot(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLTableSectionElement>(WTF::getPtr(IMPL->tFoot()));
}

WKJ_EXPORT void wkj_dom_HTMLTableElement_setTFoot(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setTFoot(static_cast<HTMLTableSectionElement*>(wkj_to_ptr(value)));
}

WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_getRows(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLCollection>(WTF::getPtr(IMPL->rows()));
}

WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_getTBodies(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLCollection>(WTF::getPtr(IMPL->tBodies()));
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::alignAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::alignAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableElement_getBgColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::bgcolorAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableElement_setBgColor(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::bgcolorAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableElement_getBorder(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::borderAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableElement_setBorder(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::borderAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableElement_getCellPadding(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::cellpaddingAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableElement_setCellPadding(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::cellpaddingAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableElement_getCellSpacing(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::cellspacingAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableElement_setCellSpacing(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::cellspacingAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableElement_getFrame(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::frameAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableElement_setFrame(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::frameAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableElement_getRules(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::rulesAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableElement_setRules(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::rulesAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableElement_getSummary(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::summaryAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableElement_setSummary(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::summaryAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTableElement_getWidth(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::widthAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTableElement_setWidth(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::widthAttr, AtomString {WKJString(value, value_length)});
}


// Functions
WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_createTHead(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLElement>(WTF::getPtr(IMPL->createTHead()));
}


WKJ_EXPORT void wkj_dom_HTMLTableElement_deleteTHead(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->deleteTHead();
}


WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_createTFoot(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLElement>(WTF::getPtr(IMPL->createTFoot()));
}


WKJ_EXPORT void wkj_dom_HTMLTableElement_deleteTFoot(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->deleteTFoot();
}


WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_createTBody(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLElement>(WTF::getPtr(IMPL->createTBody()));
}


WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_createCaption(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLElement>(WTF::getPtr(IMPL->createCaption()));
}


WKJ_EXPORT void wkj_dom_HTMLTableElement_deleteCaption(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->deleteCaption();
}


WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_insertRow(int64_t peer, int32_t index)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLElement>(WTF::getPtr(raiseOnDOMError(IMPL->insertRow(index))));
}


WKJ_EXPORT void wkj_dom_HTMLTableElement_deleteRow(int64_t peer, int32_t index)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->deleteRow(index));
}


}
