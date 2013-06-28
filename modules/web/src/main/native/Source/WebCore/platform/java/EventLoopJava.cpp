/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "EventLoop.h"

#include "JavaEnv.h"

static JGClass eventLoopClass;
static jmethodID cycleMethod;

static void initRefs(JNIEnv* env)
{
    if (!eventLoopClass) {
        eventLoopClass = JLClass(env->FindClass(
                "com/sun/webkit/EventLoop"));
        ASSERT(eventLoopClass);

        cycleMethod = env->GetStaticMethodID(eventLoopClass, "fwkCycle", "()V");
        ASSERT(cycleMethod);
    }
}

namespace WebCore {

void EventLoop::cycle()
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    env->CallStaticVoidMethod(eventLoopClass, cycleMethod);
    CheckAndClearException(env);
}

} // namespace WebCore
