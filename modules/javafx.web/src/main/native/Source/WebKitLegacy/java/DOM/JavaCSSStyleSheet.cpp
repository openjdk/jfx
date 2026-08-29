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


#include <WebCore/CSSImportRule.h>
#include <WebCore/CSSRule.h>
#include <WebCore/CSSRuleList.h>
#include <WebCore/CSSStyleSheet.h>
#include <WebCore/DOMException.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<CSSStyleSheet*>(wkj_to_ptr(peer)))


// Attributes
WKJ_EXPORT int64_t wkj_dom_CSSStyleSheet_getOwnerRule(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<CSSRule>(WTF::getPtr(IMPL->ownerRule()));
}

WKJ_EXPORT int64_t wkj_dom_CSSStyleSheet_getCssRules(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<CSSRuleList>(WTF::getPtr(IMPL->cssRules()));
}

WKJ_EXPORT int64_t wkj_dom_CSSStyleSheet_getRules(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<CSSRuleList>(WTF::getPtr(IMPL->cssRules()));
}


// Functions
WKJ_EXPORT int32_t wkj_dom_CSSStyleSheet_insertRule(int64_t peer, const uint16_t* rule, int32_t rule_length, int32_t index)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return raiseOnDOMError(IMPL->insertRule(WKJString(rule, rule_length)
            , index));
}


WKJ_EXPORT void wkj_dom_CSSStyleSheet_deleteRule(int64_t peer, int32_t index)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->deleteRule(index));
}


WKJ_EXPORT int32_t wkj_dom_CSSStyleSheet_addRule(int64_t peer, const uint16_t* selector, int32_t selector_length, const uint16_t* style, int32_t style_length, int32_t index)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return raiseOnDOMError(IMPL->addRule(WKJString(selector, selector_length)
            , AtomString{WKJString(style, style_length)}
            , index));
}


WKJ_EXPORT void wkj_dom_CSSStyleSheet_removeRule(int64_t peer, int32_t index)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->removeRule(index));
}


}
