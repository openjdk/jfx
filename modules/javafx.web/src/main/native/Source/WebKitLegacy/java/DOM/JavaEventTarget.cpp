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

#include <WebCore/AddEventListenerOptions.h>
#include <WebCore/DOMException.h>
#include <WebCore/Event.h>
#include <WebCore/EventListener.h>
#include <WebCore/EventTarget.h>
#include "EventTargetInlines.h"
#include "AddEventListenerOptionsInlines.h"
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<EventTarget*>(wkj_to_ptr(peer)))

WKJ_EXPORT void wkj_dom_EventTarget_dispose(int64_t peer)
{
    WKJCallScope wkjScope;
    IMPL->deref();
}


// Functions
WKJ_EXPORT void wkj_dom_EventTarget_addEventListener(int64_t peer, const uint16_t* type, int32_t type_length, int64_t listener, int32_t useCapture)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->addEventListenerForBindings(AtomString{WKJString(type, type_length)}
            , static_cast<EventListener*>(wkj_to_ptr(listener))
            , static_cast<bool>(useCapture));
}


WKJ_EXPORT void wkj_dom_EventTarget_removeEventListener(int64_t peer, const uint16_t* type, int32_t type_length, int64_t listener, int32_t useCapture)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->removeEventListenerForBindings(AtomString{WKJString(type, type_length)}
            , static_cast<EventListener*>(wkj_to_ptr(listener))
            , static_cast<bool>(useCapture));
}


WKJ_EXPORT int32_t wkj_dom_EventTarget_dispatchEvent(int64_t peer, int64_t event)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!event) {
        raiseTypeErrorException();
        return 0;
    }
    return raiseOnDOMError(IMPL->dispatchEventForBindings(*static_cast<Event*>(wkj_to_ptr(event))));
}


}
