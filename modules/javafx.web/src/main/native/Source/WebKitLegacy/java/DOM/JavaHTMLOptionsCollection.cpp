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

#include <WebCore/HTMLOptGroupElement.h>
#include <WebCore/HTMLOptionElement.h>
#include <WebCore/HTMLOptionsCollection.h>
#include <WebCore/JSExecState.h>
#include <WebCore/Node.h>
#include <WebCore/ThreadCheck.h>
#include <wtf/GetPtr.h>
#include <wtf/RefPtr.h>
#include <wtf/URL.h>
#include <variant>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLOptionsCollection*>(wkj_to_ptr(peer)))

// Attributes
WKJ_EXPORT int32_t wkj_dom_HTMLOptionsCollection_getSelectedIndex(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->selectedIndex();
}

WKJ_EXPORT void wkj_dom_HTMLOptionsCollection_setSelectedIndex(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setSelectedIndex(value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLOptionsCollection_getLength(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->length();
}

WKJ_EXPORT void wkj_dom_HTMLOptionsCollection_setLength(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setLength(value);
}


// Functions
WKJ_EXPORT int64_t wkj_dom_HTMLOptionsCollection_namedItem(int64_t peer, const uint16_t* name, int32_t name_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->namedItem(AtomString{WKJString(name, name_length)})));
}


WKJ_EXPORT void wkj_dom_HTMLOptionsCollection_add(int64_t peer, int64_t option, int32_t index)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!option) {
        raiseTypeErrorException();
        return;
    }
    raiseOnDOMError(IMPL->add(static_cast<HTMLOptionElement*>(wkj_to_ptr(option)), std::optional<WebCore::HTMLOptionsCollection::HTMLElementOrInt> { static_cast<int>(index) }));
}


WKJ_EXPORT int64_t wkj_dom_HTMLOptionsCollection_item(int64_t peer, int32_t index)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->item(index)));
}


}
