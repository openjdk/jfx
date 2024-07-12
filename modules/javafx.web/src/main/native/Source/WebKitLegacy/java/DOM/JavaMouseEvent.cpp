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


#include <WebCore/DOMWindow.h>
#include <WebCore/EventTarget.h>
#include <WebCore/MouseEvent.h>
#include <WebCore/Node.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include "AbstractViewInternal.h"
#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<MouseEvent*>(jlong_to_ptr(peer)))


// Attributes
JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_MouseEventImpl_getScreenXImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->screenX();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_MouseEventImpl_getScreenYImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->screenY();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_MouseEventImpl_getClientXImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->clientX();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_MouseEventImpl_getClientYImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->clientY();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_MouseEventImpl_getCtrlKeyImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->ctrlKey();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_MouseEventImpl_getShiftKeyImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->shiftKey();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_MouseEventImpl_getAltKeyImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->altKey();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_MouseEventImpl_getMetaKeyImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->metaKey();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_MouseEventImpl_getButtonImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
        int16_t button = enumToUnderlyingType(IMPL->button());
    return button;
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_MouseEventImpl_getRelatedTargetImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventTarget>(env, WTF::getPtr(IMPL->relatedTarget()));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_MouseEventImpl_getOffsetXImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->offsetX();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_MouseEventImpl_getOffsetYImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->offsetY();
}
//This code has been commented as the corresponding apis have been removed
/*JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_MouseEventImpl_getXImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->x();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_MouseEventImpl_getYImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->y();
}*/

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_MouseEventImpl_getFromElementImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->fromElement()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_MouseEventImpl_getToElementImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->toElement()));
}


// Functions
JNIEXPORT void JNICALL Java_com_sun_webkit_dom_MouseEventImpl_initMouseEventImpl(JNIEnv* env, jclass, jlong peer
    , jstring type
    , jboolean canBubble
    , jboolean cancelable
    , jlong view
    , jint detail
    , jint screenX
    , jint screenY
    , jint clientX
    , jint clientY
    , jboolean ctrlKey
    , jboolean altKey
    , jboolean shiftKey
    , jboolean metaKey
    , jshort button
    , jlong relatedTarget)
{
    WebCore::JSMainThreadNullState state;
    IMPL->initMouseEvent(AtomString {String(env, type)}
            , canBubble
            , cancelable
            , toWindowProxy(static_cast<DOMWindow*>(jlong_to_ptr(view)))
            , detail
            , screenX
            , screenY
            , clientX
            , clientY
            , ctrlKey
            , altKey
            , shiftKey
            , metaKey
            , button
            , static_cast<EventTarget*>(jlong_to_ptr(relatedTarget)));
}


}
