/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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
#include <WebCore/KeyboardEvent.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include "AbstractViewInternal.h"
#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<KeyboardEvent*>(wkj_to_ptr(peer)))


// Attributes
WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getKeyIdentifier(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->keyIdentifier());
}

WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getLocation(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->location();
}

WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getKeyLocation(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->location();
}

WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getCtrlKey(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->ctrlKey();
}

WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getShiftKey(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->shiftKey();
}

WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getAltKey(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->altKey();
}

WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getMetaKey(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->metaKey();
}

WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getKeyCode(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->keyCode();
}

WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getCharCode(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->charCode();
}


// Functions
WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getModifierState(int64_t peer, const uint16_t* keyIdentifierArg, int32_t keyIdentifierArg_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->getModifierState(AtomString {WKJString(keyIdentifierArg, keyIdentifierArg_length)});
}


WKJ_EXPORT void wkj_dom_KeyboardEvent_initKeyboardEvent(int64_t peer, const uint16_t* type, int32_t type_length, int32_t canBubble, int32_t cancelable, int64_t view, const uint16_t* keyIdentifier, int32_t keyIdentifier_length, int32_t location, int32_t ctrlKey, int32_t altKey, int32_t shiftKey, int32_t metaKey)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->initKeyboardEvent(AtomString {WKJString(type, type_length)}
            , canBubble
            , cancelable
            , toWindowProxy(static_cast<DOMWindow*>(wkj_to_ptr(view)))
            , AtomString {WKJString(keyIdentifier, keyIdentifier_length)}
            , location
            , ctrlKey
            , altKey
            , shiftKey
            , metaKey);
}


WKJ_EXPORT void wkj_dom_KeyboardEvent_initKeyboardEventEx(int64_t peer, const uint16_t* type, int32_t type_length, int32_t canBubble, int32_t cancelable, int64_t view, const uint16_t* keyIdentifier, int32_t keyIdentifier_length, int32_t location, int32_t ctrlKey, int32_t altKey, int32_t shiftKey, int32_t metaKey)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->initKeyboardEvent(AtomString {WKJString(type, type_length)}
            , canBubble
            , cancelable
            , toWindowProxy(static_cast<DOMWindow*>(wkj_to_ptr(view)))
            , AtomString{WKJString(keyIdentifier, keyIdentifier_length)}
            , location
            , ctrlKey
            , altKey
            , shiftKey
            , metaKey);
}


}
