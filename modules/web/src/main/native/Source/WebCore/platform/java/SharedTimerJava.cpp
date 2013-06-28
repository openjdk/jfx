/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "JavaEnv.h"
#include "SharedTimer.h"
#include "SystemTime.h"

#include <wtf/Assertions.h>

namespace WebCore {

static void (*sharedTimerFiredFunction)();

void setSharedTimerFiredFunction(void (*f)())
{
    sharedTimerFiredFunction = f;
}

// The fire time is relative to the classic POSIX epoch of January 1, 1970,
// as the result of currentTime() is.
#define MINIMAL_INTERVAL 1e-9 //1ns
void setSharedTimerFireInterval(double fireTime)
{
    if (fireTime < MINIMAL_INTERVAL) {
        fireTime = MINIMAL_INTERVAL;
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(getTimerClass(env),
                                                  "fwkSetFireTime", "(D)V");
    ASSERT(mid);

    env->CallStaticVoidMethod(getTimerClass(env), mid, fireTime);
    CheckAndClearException(env);
}

void stopSharedTimer()
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(getTimerClass(env),
                                                  "fwkStopTimer", "()V");
    ASSERT(mid);

    env->CallStaticVoidMethod(getTimerClass(env), mid);
    CheckAndClearException(env);
}

} // namespace WebCore

using namespace WebCore;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_sun_webkit_Timer_twkFireTimerEvent
    (JNIEnv *env, jclass clazz)
{
    ASSERT(sharedTimerFiredFunction);
    sharedTimerFiredFunction();
}

#ifdef __cplusplus
}
#endif
