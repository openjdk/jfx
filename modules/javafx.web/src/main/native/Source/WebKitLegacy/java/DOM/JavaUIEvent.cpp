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


#include <WebCore/DOMWindow.h>
#include <WebCore/JSExecState.h>
#include <WebCore/KeyboardEvent.h>
#include <WebCore/ThreadCheck.h>
#include <WebCore/UIEvent.h>

#include <wtf/GetPtr.h>
#include <wtf/URL.h>

#include "AbstractViewInternal.h"
#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<UIEvent*>(jlong_to_ptr(peer)))


// Attributes
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_UIEventImpl_getViewImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<DOMWindow>(env, WTF::getPtr(toDOMWindow(IMPL->view())));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_UIEventImpl_getDetailImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->detail();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_UIEventImpl_getKeyCodeImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    if (is<WebCore::KeyboardEvent>(*IMPL))
        return downcast<WebCore::KeyboardEvent>(*IMPL).keyCode();
    return 0;
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_UIEventImpl_getCharCodeImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    if (is<WebCore::KeyboardEvent>(*IMPL))
        return downcast<WebCore::KeyboardEvent>(*IMPL).charCode();
    return 0;
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_UIEventImpl_getLayerXImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->layerX();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_UIEventImpl_getLayerYImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->layerY();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_UIEventImpl_getPageXImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->pageX();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_UIEventImpl_getPageYImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->pageY();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_UIEventImpl_getWhichImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->which();
}


// Functions
JNIEXPORT void JNICALL Java_com_sun_webkit_dom_UIEventImpl_initUIEventImpl(JNIEnv* env, jclass, jlong peer
    , jstring type
    , jboolean canBubble
    , jboolean cancelable
    , jlong view
    , jint detail)
{
    WebCore::JSMainThreadNullState state;
    IMPL->initUIEvent(String(env, type)
            , canBubble
            , cancelable
            , toWindowProxy(static_cast<DOMWindow*>(jlong_to_ptr(view)))
            , detail);
}


}
