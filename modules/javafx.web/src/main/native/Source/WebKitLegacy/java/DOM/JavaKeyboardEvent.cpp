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
#include <WebCore/KeyboardEvent.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include "AbstractViewInternal.h"
#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<KeyboardEvent*>(jlong_to_ptr(peer)))


// Attributes
JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_KeyboardEventImpl_getKeyIdentifierImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->keyIdentifier());
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_KeyboardEventImpl_getLocationImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->location();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_KeyboardEventImpl_getKeyLocationImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->location();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_KeyboardEventImpl_getCtrlKeyImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->ctrlKey();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_KeyboardEventImpl_getShiftKeyImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->shiftKey();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_KeyboardEventImpl_getAltKeyImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->altKey();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_KeyboardEventImpl_getMetaKeyImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->metaKey();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_KeyboardEventImpl_getKeyCodeImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->keyCode();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_KeyboardEventImpl_getCharCodeImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->charCode();
}


// Functions
JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_KeyboardEventImpl_getModifierStateImpl(JNIEnv* env, jclass, jlong peer
    , jstring keyIdentifierArg)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->getModifierState(AtomString {String(env, keyIdentifierArg)});
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_KeyboardEventImpl_initKeyboardEventImpl(JNIEnv* env, jclass, jlong peer
    , jstring type
    , jboolean canBubble
    , jboolean cancelable
    , jlong view
    , jstring keyIdentifier
    , jint location
    , jboolean ctrlKey
    , jboolean altKey
    , jboolean shiftKey
    , jboolean metaKey)
{
    WebCore::JSMainThreadNullState state;
    IMPL->initKeyboardEvent(AtomString {String(env, type)}
            , canBubble
            , cancelable
            , toWindowProxy(static_cast<DOMWindow*>(jlong_to_ptr(view)))
            , AtomString {String(env, keyIdentifier)}
            , location
            , ctrlKey
            , altKey
            , shiftKey
            , metaKey);
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_KeyboardEventImpl_initKeyboardEventExImpl(JNIEnv* env, jclass, jlong peer
    , jstring type
    , jboolean canBubble
    , jboolean cancelable
    , jlong view
    , jstring keyIdentifier
    , jint location
    , jboolean ctrlKey
    , jboolean altKey
    , jboolean shiftKey
    , jboolean metaKey)
{
    WebCore::JSMainThreadNullState state;
    IMPL->initKeyboardEvent(AtomString {String(env, type)}
            , canBubble
            , cancelable
            , toWindowProxy(static_cast<DOMWindow*>(jlong_to_ptr(view)))
            , AtomString{String(env, keyIdentifier)}
            , location
            , ctrlKey
            , altKey
            , shiftKey
            , metaKey);
}


}
