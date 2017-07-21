/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include <wtf/java/JavaEnv.h>
#include "MainThreadSharedTimer.h"

#include <wtf/Assertions.h>
#include <wtf/MainThread.h>

namespace WebCore {

// The fire time is relative to the classic POSIX epoch of January 1, 1970,
// as the result of currentTime() is.
#define MINIMAL_INTERVAL 1e-9 //1ns
void MainThreadSharedTimer::setFireInterval(Seconds timeout)
{
    auto fireTime = timeout.value();
    if (fireTime < MINIMAL_INTERVAL) {
        fireTime = MINIMAL_INTERVAL;
    }
    WC_GETJAVAENV_CHKRET(env);

    static jmethodID mid = env->GetStaticMethodID(getTimerClass(env),
                                                  "fwkSetFireTime", "(D)V");
    ASSERT(mid);

    env->CallStaticVoidMethod(getTimerClass(env), mid, fireTime);
    CheckAndClearException(env);
}

void MainThreadSharedTimer::stop()
{
    WC_GETJAVAENV_CHKRET(env);

    static jmethodID mid = env->GetStaticMethodID(getTimerClass(env),
                                                  "fwkStopTimer", "()V");
    ASSERT(mid);

    env->CallStaticVoidMethod(getTimerClass(env), mid);
    CheckAndClearException(env);
}

// JDK-8146958
void MainThreadSharedTimer::invalidate()
{
}

} // namespace WebCore

extern "C" {

JNIEXPORT void JNICALL Java_com_sun_webkit_Timer_twkFireTimerEvent
    (JNIEnv*, jclass)
{
    WebCore::MainThreadSharedTimer::singleton().fired();
}

}
