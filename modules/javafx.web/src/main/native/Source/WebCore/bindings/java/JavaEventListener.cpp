/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"

#include "JavaDOMUtils.h"
#include <wtf/java/JavaEnv.h>
#include "JavaEventListener.h"

namespace WebCore {

// DOM Document implements ScriptExecutionContext!
// FIXME: it need to be per-thread object then [WORKERS] would be introduced!
Vector<ScriptExecutionContext*> JavaEventListener::sm_vScriptExecutionContexts;

ScriptExecutionContext* JavaEventListener::scriptExecutionContext()
{
    return sm_vScriptExecutionContexts.size() == 0
        ? nullptr
        : sm_vScriptExecutionContexts.last();
}

bool JavaEventListener::operator==(const EventListener& other) const
{
    const JavaEventListener* jother = other.isJavaEventListener()
                                        ? static_cast<const JavaEventListener*>(&other)
                                        : nullptr;
    return jother && isJavaEquals(m_joListener, jother->m_joListener);
}

void JavaEventListener::handleEvent(ScriptExecutionContext& context, Event& event)
{
    JNIEnv* env = WTF::GetJavaEnv();

    //we need to store context for cascade JS EL execution.
    sm_vScriptExecutionContexts.append(&context);

    static jmethodID midFwkHandleEvent(env->GetMethodID(
        JLClass(env->FindClass("com/sun/webkit/dom/EventListenerImpl")),
        "fwkHandleEvent",
        "(J)V"));
    ASSERT(midFwkHandleEvent);

    event.ref();
    env->CallVoidMethod(
        m_joListener,
        midFwkHandleEvent,
        ptr_to_jlong(&event));

    sm_vScriptExecutionContexts.removeLast();
    WTF::CheckAndClearException(env);
}

JavaEventListener::~JavaEventListener()
{
    WC_GETJAVAENV_CHKRET(env);

    JGClass eli(env->FindClass("com/sun/webkit/dom/EventListenerImpl"));
    static jmethodID midDispose(env->GetStaticMethodID(
        eli,
        "dispose",
        "(J)V"));
    ASSERT(midDispose);

    env->CallStaticVoidMethod(
        eli,
        midDispose,
        ptr_to_jlong(this));
}

extern "C" {

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_EventListenerImpl_twkCreatePeer
    (JNIEnv*, jobject self)
{
    return ptr_to_jlong(new JavaEventListener(JLObject(self, true)));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_EventListenerImpl_twkDisposeJSPeer
    (JNIEnv*, jclass, jlong peer)
{
    EventListener* pEventListener = static_cast<EventListener *>(jlong_to_ptr(peer));
    if (pEventListener)
        pEventListener->deref();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_EventListenerImpl_twkDispatchEvent
    (JNIEnv*, jclass, jlong peer, jlong eventPeer)
{
    if (!peer || !eventPeer || !JavaEventListener::scriptExecutionContext())
        return;

    static_cast<EventListener *>(jlong_to_ptr(peer))->handleEvent(
        *JavaEventListener::scriptExecutionContext(),
        *static_cast<Event*>(jlong_to_ptr(eventPeer)));
}

}
}

