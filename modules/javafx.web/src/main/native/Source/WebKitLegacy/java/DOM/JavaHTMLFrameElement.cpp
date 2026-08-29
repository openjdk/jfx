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


#include <WebCore/DOMWindow.h>
#include <WebCore/Document.h>
#include <WebCore/HTMLFrameElement.h>
#include <WebCore/ElementInlines.h>
#include <WebCore/HTMLNames.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include "AbstractViewInternal.h"
#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLFrameElement*>(wkj_to_ptr(peer)))


// Attributes
WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getFrameBorder(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::frameborderAttr));
}

WKJ_EXPORT void wkj_dom_HTMLFrameElement_setFrameBorder(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::frameborderAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getLongDesc(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::longdescAttr));
}

WKJ_EXPORT void wkj_dom_HTMLFrameElement_setLongDesc(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::longdescAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getMarginHeight(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::marginheightAttr));
}

WKJ_EXPORT void wkj_dom_HTMLFrameElement_setMarginHeight(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::marginheightAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getMarginWidth(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::marginwidthAttr));
}

WKJ_EXPORT void wkj_dom_HTMLFrameElement_setMarginWidth(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::marginwidthAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getNameAttribute());
}

WKJ_EXPORT void wkj_dom_HTMLFrameElement_setName(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::nameAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getNoResize(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::noresizeAttr);
}

WKJ_EXPORT void wkj_dom_HTMLFrameElement_setNoResize(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::noresizeAttr, value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getScrolling(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::scrollingAttr));
}

WKJ_EXPORT void wkj_dom_HTMLFrameElement_setScrolling(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::scrollingAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getSrc(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getURLAttribute(WebCore::HTMLNames::srcAttr).string());
}

WKJ_EXPORT void wkj_dom_HTMLFrameElement_setSrc(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::srcAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameElement_getContentDocument(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Document>(WTF::getPtr(IMPL->contentDocument()));
}

WKJ_EXPORT int64_t wkj_dom_HTMLFrameElement_getContentWindow(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<DOMWindow>(WTF::getPtr(toDOMWindow(IMPL->contentWindow())));
}

WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getLocation(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return nullptr;
}

WKJ_EXPORT void wkj_dom_HTMLFrameElement_setLocation(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    return;
}

WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getWidth(int64_t peer)
{
    WKJCallScope wkjScope;
    return 0;
}

WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getHeight(int64_t peer)
{
    WKJCallScope wkjScope;
    return 0;
}

}
