/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "JavaDOMUtils.h"
#include "JavaEnv.h"
#include "JavaEventListener.h"

namespace WebCore {

//DOM Document implements ScriptExecutionContext!
//FIXME: it need to be per-thread object then [WORKERS] would be introduced!
Vector<ScriptExecutionContext*> JavaEventListener::sm_vScriptExecutionContexts;

ScriptExecutionContext* JavaEventListener::scriptExecutionContext()
{
    return sm_vScriptExecutionContexts.size() == 0
        ? NULL
        : sm_vScriptExecutionContexts.last();
}

bool JavaEventListener::operator==(const EventListener& other)
{
    const JavaEventListener* jother = dynamic_cast<const JavaEventListener*>(&other);
    return jother && isJavaEquals(m_joListener, jother->m_joListener);
}

void JavaEventListener::handleEvent(ScriptExecutionContext* context, Event* event)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    //we need to store context for cascade JS EL execution.
    sm_vScriptExecutionContexts.append(context);

    static jmethodID midFwkHandleEvent(env->GetMethodID(
        JLClass(env->FindClass("com/sun/webkit/dom/EventListenerImpl")),
        "fwkHandleEvent",
        "(J)V"));
    ASSERT(midFwkHandleEvent);

    event->ref();
    env->CallVoidMethod(
        m_joListener,
        midFwkHandleEvent,
        ptr_to_jlong(event));

    sm_vScriptExecutionContexts.removeLast();
    CheckAndClearException(env);
}

JavaEventListener::~JavaEventListener()
{
    JNIEnv* env = WebCore_GetJavaEnv();

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

}; // namespace WebCore

using namespace WebCore;

extern "C" {

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_EventListenerImpl_twkCreatePeer
    (JNIEnv* env, jobject self)
{
    return ptr_to_jlong(new JavaEventListener(JLObject(self, true)));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_EventListenerImpl_twkDisposeJSPeer
    (JNIEnv* env, jclass clazz, jlong peer)
{
    EventListener* pEventListener = static_cast<EventListener *>(jlong_to_ptr(peer));
    if (pEventListener)
        pEventListener->deref();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_EventListenerImpl_twkDispatchEvent
    (JNIEnv* env, jclass clazz, jlong peer, jlong eventPeer)
{
    static_cast<EventListener *>(jlong_to_ptr(peer))->handleEvent(
        JavaEventListener::scriptExecutionContext(),
        static_cast<Event*>(jlong_to_ptr(eventPeer)));
}

}



