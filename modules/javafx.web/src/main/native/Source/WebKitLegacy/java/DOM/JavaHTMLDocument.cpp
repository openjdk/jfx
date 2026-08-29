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


#include <WebCore/HTMLCollection.h>
#include <WebCore/HTMLDocument.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLDocument*>(wkj_to_ptr(peer)))


// Attributes
WKJ_EXPORT int64_t wkj_dom_HTMLDocument_getEmbeds(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLCollection>(WTF::getPtr(IMPL->embeds()));
}

WKJ_EXPORT int64_t wkj_dom_HTMLDocument_getPlugins(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLCollection>(WTF::getPtr(IMPL->embeds()));
}

WKJ_EXPORT int64_t wkj_dom_HTMLDocument_getScripts(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLCollection>(WTF::getPtr(IMPL->scripts()));
}

WKJ_EXPORT int32_t wkj_dom_HTMLDocument_getDir(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->dir());
}

WKJ_EXPORT void wkj_dom_HTMLDocument_setDir(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setDir(AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLDocument_getDesignMode(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->designMode());
}

WKJ_EXPORT void wkj_dom_HTMLDocument_setDesignMode(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setDesignMode(AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLDocument_getCompatMode(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->compatMode());
}

WKJ_EXPORT int32_t wkj_dom_HTMLDocument_getBgColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->bgColor());
}

WKJ_EXPORT void wkj_dom_HTMLDocument_setBgColor(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBgColor(AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLDocument_getFgColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->fgColor());
}

WKJ_EXPORT void wkj_dom_HTMLDocument_setFgColor(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setFgColor(AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLDocument_getAlinkColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->alinkColor());
}

WKJ_EXPORT void wkj_dom_HTMLDocument_setAlinkColor(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAlinkColor(AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLDocument_getLinkColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->linkColorForBindings());
}

WKJ_EXPORT void wkj_dom_HTMLDocument_setLinkColor(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setLinkColorForBindings(AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLDocument_getVlinkColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->vlinkColor());
}

WKJ_EXPORT void wkj_dom_HTMLDocument_setVlinkColor(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setVlinkColor(AtomString {WKJString(value, value_length)});
}


// Functions
WKJ_EXPORT void wkj_dom_HTMLDocument_open(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->open();
}


WKJ_EXPORT void wkj_dom_HTMLDocument_close(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->close();
}


WKJ_EXPORT void wkj_dom_HTMLDocument_write(int64_t peer, const uint16_t* text, int32_t text_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    WTF::FixedVector<String> textVector { WKJString(text, text_length) };
    IMPL->write(nullptr, WTF::move(textVector));
}


WKJ_EXPORT void wkj_dom_HTMLDocument_writeln(int64_t peer, const uint16_t* text, int32_t text_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    WTF::FixedVector<String> textVector { WKJString(text, text_length) };
    IMPL->writeln(nullptr, WTF::move(textVector));
}


WKJ_EXPORT void wkj_dom_HTMLDocument_clear(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->clear();
}


WKJ_EXPORT void wkj_dom_HTMLDocument_captureEvents(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->captureEvents();
}


WKJ_EXPORT void wkj_dom_HTMLDocument_releaseEvents(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->releaseEvents();
}


}
