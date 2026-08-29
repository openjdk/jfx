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


#include <WebCore/CharacterData.h>
#include <WebCore/DOMException.h>
#include <WebCore/Element.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<CharacterData*>(wkj_to_ptr(peer)))


// Attributes
WKJ_EXPORT int32_t wkj_dom_CharacterData_getData(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->data());
}

WKJ_EXPORT void wkj_dom_CharacterData_setData(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setData(AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_CharacterData_getLength(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->length();
}

WKJ_EXPORT int64_t wkj_dom_CharacterData_getPreviousElementSibling(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(IMPL->previousElementSibling()));
}

WKJ_EXPORT int64_t wkj_dom_CharacterData_getNextElementSibling(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Element>(WTF::getPtr(IMPL->nextElementSibling()));
}


// Functions
WKJ_EXPORT int32_t wkj_dom_CharacterData_substringData(int64_t peer, int32_t offset, int32_t length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, raiseOnDOMError(IMPL->substringData(offset
            , length)));
}


WKJ_EXPORT void wkj_dom_CharacterData_appendData(int64_t peer, const uint16_t* data, int32_t data_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->appendData(AtomString {WKJString(data, data_length)});
}


WKJ_EXPORT void wkj_dom_CharacterData_insertData(int64_t peer, int32_t offset, const uint16_t* data, int32_t data_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->insertData(offset
            , AtomString {WKJString(data, data_length)}));
}


WKJ_EXPORT void wkj_dom_CharacterData_deleteData(int64_t peer, int32_t offset, int32_t length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->deleteData(offset
            , length));
}


WKJ_EXPORT void wkj_dom_CharacterData_replaceData(int64_t peer, int32_t offset, int32_t length, const uint16_t* data, int32_t data_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->replaceData(offset
            , length
            , AtomString {WKJString(data, data_length)}));
}


WKJ_EXPORT void wkj_dom_CharacterData_remove(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->remove());
}


}
