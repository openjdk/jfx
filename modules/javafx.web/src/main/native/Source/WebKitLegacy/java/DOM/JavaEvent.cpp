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


#include <WebCore/Event.h>
#include <WebCore/EventTarget.h>
#include <WebCore/KeyboardEvent.h>
#include <WebCore/MouseEvent.h>
#include <WebCore/MutationEvent.h>
#include <WebCore/UIEvent.h>
#include <WebCore/WheelEvent.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<Event*>(jlong_to_ptr(peer)))

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_EventImpl_dispose(JNIEnv*, jclass, jlong peer)
{
    IMPL->deref();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_EventImpl_getCPPTypeImpl(JNIEnv*, jclass, jlong peer)
{
    if (IMPL->isWheelEvent())
        return 1;
    if (IMPL->isMouseEvent())
        return 2;
    if (IMPL->isKeyboardEvent())
        return 3;
    if (IMPL->isUIEvent())
        return 4;
    if (IMPL->isMutationEvent())
        return 5;
    return 0;
}


// Attributes
JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_EventImpl_getTypeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->type());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_EventImpl_getTargetImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventTarget>(env, WTF::getPtr(IMPL->target()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_EventImpl_getCurrentTargetImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventTarget>(env, WTF::getPtr(IMPL->currentTarget()));
}

JNIEXPORT jshort JNICALL Java_com_sun_webkit_dom_EventImpl_getEventPhaseImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->eventPhase();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_EventImpl_getBubblesImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->bubbles();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_EventImpl_getCancelableImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->cancelable();
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_EventImpl_getTimeStampImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->timeStamp().approximateWallTime().secondsSinceEpoch().milliseconds();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_EventImpl_getDefaultPreventedImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->defaultPrevented();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_EventImpl_getIsTrustedImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->isTrusted();
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_EventImpl_getSrcElementImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventTarget>(env, WTF::getPtr(IMPL->target()));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_EventImpl_getReturnValueImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->legacyReturnValue();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_EventImpl_setReturnValueImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setLegacyReturnValue(value);
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_EventImpl_getCancelBubbleImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->cancelBubble();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_EventImpl_setCancelBubbleImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setCancelBubble(value);
}


// Functions
JNIEXPORT void JNICALL Java_com_sun_webkit_dom_EventImpl_stopPropagationImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->stopPropagation();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_EventImpl_preventDefaultImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->preventDefault();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_EventImpl_initEventImpl(JNIEnv* env, jclass, jlong peer
    , jstring eventTypeArg
    , jboolean canBubbleArg
    , jboolean cancelableArg)
{
    WebCore::JSMainThreadNullState state;
    IMPL->initEvent(String(env, eventTypeArg)
            , canBubbleArg
            , cancelableArg);
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_EventImpl_stopImmediatePropagationImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->stopImmediatePropagation();
}


}
