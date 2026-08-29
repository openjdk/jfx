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


#include <WebCore/CSSRule.h>
#include <WebCore/CSSStyleDeclaration.h>
#include <WebCore/DeprecatedCSSOMValue.h>
#include <WebCore/DOMException.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<CSSStyleDeclaration*>(wkj_to_ptr(peer)))

WKJ_EXPORT void wkj_dom_CSSStyleDeclaration_dispose(int64_t peer)
{
    WKJCallScope wkjScope;
    IMPL->deref();
}


// Attributes
WKJ_EXPORT int32_t wkj_dom_CSSStyleDeclaration_getCssText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->cssText());
}

WKJ_EXPORT void wkj_dom_CSSStyleDeclaration_setCssText(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setCssText(AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_CSSStyleDeclaration_getLength(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->length();
}

WKJ_EXPORT int64_t wkj_dom_CSSStyleDeclaration_getParentRule(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<CSSRule>(WTF::getPtr(IMPL->parentRule()));
}


// Functions
WKJ_EXPORT int32_t wkj_dom_CSSStyleDeclaration_getPropertyValue(int64_t peer, const uint16_t* propertyName, int32_t propertyName_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getPropertyValue(AtomString {WKJString(propertyName, propertyName_length)}));
}


WKJ_EXPORT int64_t wkj_dom_CSSStyleDeclaration_getPropertyCSSValue(int64_t peer, const uint16_t* propertyName, int32_t propertyName_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<DeprecatedCSSOMValue>(WTF::getPtr(IMPL->getPropertyCSSValue(AtomString {WKJString(propertyName, propertyName_length)})));
}


WKJ_EXPORT int32_t wkj_dom_CSSStyleDeclaration_removeProperty(int64_t peer, const uint16_t* propertyName, int32_t propertyName_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, raiseOnDOMError(IMPL->removeProperty(AtomString {WKJString(propertyName, propertyName_length)})));
}


WKJ_EXPORT int32_t wkj_dom_CSSStyleDeclaration_getPropertyPriority(int64_t peer, const uint16_t* propertyName, int32_t propertyName_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getPropertyPriority(AtomString {WKJString(propertyName, propertyName_length)}));
}


WKJ_EXPORT void wkj_dom_CSSStyleDeclaration_setProperty(int64_t peer, const uint16_t* propertyName, int32_t propertyName_length, const uint16_t* value, int32_t value_length, const uint16_t* priority, int32_t priority_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->setProperty(AtomString {WKJString(propertyName, propertyName_length)}
            , AtomString{WKJString(value, value_length)}
            , AtomString{WKJString(priority, priority_length)}));
}


WKJ_EXPORT int32_t wkj_dom_CSSStyleDeclaration_item(int64_t peer, int32_t index, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->item(index));
}


WKJ_EXPORT int32_t wkj_dom_CSSStyleDeclaration_getPropertyShorthand(int64_t peer, const uint16_t* propertyName, int32_t propertyName_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getPropertyShorthand(AtomString{WKJString(propertyName, propertyName_length)}));
}


WKJ_EXPORT int32_t wkj_dom_CSSStyleDeclaration_isPropertyImplicit(int64_t peer, const uint16_t* propertyName, int32_t propertyName_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->isPropertyImplicit(AtomString{WKJString(propertyName, propertyName_length)});
}


}
