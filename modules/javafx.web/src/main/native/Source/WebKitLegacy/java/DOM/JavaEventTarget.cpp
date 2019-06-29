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


#include <WebCore/DOMException.h>
#include <WebCore/Event.h>
#include <WebCore/EventListener.h>
#include <WebCore/EventTarget.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<EventTarget*>(jlong_to_ptr(peer)))

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_EventTargetImpl_dispose(JNIEnv*, jclass, jlong peer)
{
    IMPL->deref();
}


// Functions
JNIEXPORT void JNICALL Java_com_sun_webkit_dom_EventTargetImpl_addEventListenerImpl(JNIEnv* env, jclass, jlong peer
    , jstring type
    , jlong listener
    , jboolean useCapture)
{
    WebCore::JSMainThreadNullState state;
    IMPL->addEventListenerForBindings(String(env, type)
            , static_cast<EventListener*>(jlong_to_ptr(listener))
            , static_cast<bool>(useCapture));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_EventTargetImpl_removeEventListenerImpl(JNIEnv* env, jclass, jlong peer
    , jstring type
    , jlong listener
    , jboolean useCapture)
{
    WebCore::JSMainThreadNullState state;
    IMPL->removeEventListenerForBindings(String(env, type)
            , static_cast<EventListener*>(jlong_to_ptr(listener))
            , static_cast<bool>(useCapture));
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_EventTargetImpl_dispatchEventImpl(JNIEnv* env, jclass, jlong peer
    , jlong event)
{
    WebCore::JSMainThreadNullState state;
    if (!event) {
        raiseTypeErrorException(env);
        return JNI_FALSE;
    }
    return raiseOnDOMError(env, IMPL->dispatchEventForBindings(*static_cast<Event*>(jlong_to_ptr(event))));
}


}
