/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLAppletElement*>(wkj_to_ptr(peer)))


// Attributes
WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    return nullptr;
}

WKJ_EXPORT void wkj_dom_HTMLAppletElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
}

WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getAlt(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    return nullptr;
}

WKJ_EXPORT void wkj_dom_HTMLAppletElement_setAlt(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
}

WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getArchive(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    return nullptr;
}

WKJ_EXPORT void wkj_dom_HTMLAppletElement_setArchive(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
}

WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getCode(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    return nullptr;
}

WKJ_EXPORT void wkj_dom_HTMLAppletElement_setCode(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
}

WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getCodeBase(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    return nullptr;
}

WKJ_EXPORT void wkj_dom_HTMLAppletElement_setCodeBase(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
}

WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getHeight(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    return nullptr;
}

WKJ_EXPORT void wkj_dom_HTMLAppletElement_setHeight(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
}

WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getHspace(int64_t peer)
{
    WKJCallScope wkjScope;
    return 0;
}

WKJ_EXPORT void wkj_dom_HTMLAppletElement_setHspace(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
}

WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    return nullptr;
}

WKJ_EXPORT void wkj_dom_HTMLAppletElement_setName(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
}

WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getObject(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    return nullptr;
}

WKJ_EXPORT void wkj_dom_HTMLAppletElement_setObject(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
}

WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getVspace(int64_t peer)
{
    WKJCallScope wkjScope;
    return 0;
}

WKJ_EXPORT void wkj_dom_HTMLAppletElement_setVspace(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
}

WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getWidth(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    return nullptr;
}

WKJ_EXPORT void wkj_dom_HTMLAppletElement_setWidth(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
}

}
