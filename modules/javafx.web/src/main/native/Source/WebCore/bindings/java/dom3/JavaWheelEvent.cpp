/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"

#include <WebCore/DOMWindow.h>
#include <WebCore/WheelEvent.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include "JavaDOMUtils.h"
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<WheelEvent*>(jlong_to_ptr(peer)))


// Attributes
JNIEXPORT jdouble JNICALL Java_com_sun_webkit_dom_WheelEventImpl_getDeltaXImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->deltaX();
}

JNIEXPORT jdouble JNICALL Java_com_sun_webkit_dom_WheelEventImpl_getDeltaYImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->deltaY();
}

JNIEXPORT jdouble JNICALL Java_com_sun_webkit_dom_WheelEventImpl_getDeltaZImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->deltaZ();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_WheelEventImpl_getDeltaModeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->deltaMode();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_WheelEventImpl_getWheelDeltaXImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->wheelDeltaX();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_WheelEventImpl_getWheelDeltaYImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->wheelDeltaY();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_WheelEventImpl_getWheelDeltaImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->wheelDelta();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_WheelEventImpl_getWebkitDirectionInvertedFromDeviceImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->webkitDirectionInvertedFromDevice();
}


// Functions
JNIEXPORT void JNICALL Java_com_sun_webkit_dom_WheelEventImpl_initWheelEventImpl(JNIEnv* env, jclass, jlong peer
    , jint wheelDeltaX
    , jint wheelDeltaY
    , jlong view
    , jint screenX
    , jint screenY
    , jint clientX
    , jint clientY
    , jboolean ctrlKey
    , jboolean altKey
    , jboolean shiftKey
    , jboolean metaKey)
{
    WebCore::JSMainThreadNullState state;
    IMPL->initWheelEvent(wheelDeltaX
            , wheelDeltaY
            , static_cast<DOMWindow*>(jlong_to_ptr(view))
            , screenX
            , screenY
            , clientX
            , clientY
            , ctrlKey
            , altKey
            , shiftKey
            , metaKey);
}


}
