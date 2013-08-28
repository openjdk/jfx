/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 */

#include "config.h"
#include "Watchdog.h"

#include "com_sun_webkit_WatchdogTimer.h"

namespace JSC {

static jclass GetWatchdogTimerClass(JNIEnv* env)
{
    static JGClass clazz(env->FindClass(
            "com/sun/webkit/WatchdogTimer"));
    ASSERT(clazz);
    return clazz;
}

void Watchdog::initTimer()
{
    JNIEnv* env = JavaScriptCore_GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(
            GetWatchdogTimerClass(env),
            "fwkCreate",
            "(J)Lcom/sun/webkit/WatchdogTimer;");
    ASSERT(mid);

    m_timer = JLObject(env->CallStaticObjectMethod(
            GetWatchdogTimerClass(env),
            mid,
            ptr_to_jlong(timerDidFireAddress())));
    CheckAndClearException(env);
}

void Watchdog::destroyTimer()
{
    JNIEnv* env = JavaScriptCore_GetJavaEnv();

    static jmethodID mid = env->GetMethodID(
            GetWatchdogTimerClass(env),
            "fwkDestroy",
            "()V");
    ASSERT(mid);

    env->CallVoidMethod(m_timer, mid);
    CheckAndClearException(env);

    m_timer.clear();
}

void Watchdog::startTimer(double limit)
{
    JNIEnv* env = JavaScriptCore_GetJavaEnv();

    static jmethodID mid = env->GetMethodID(
            GetWatchdogTimerClass(env),
            "fwkStart",
            "(D)V");
    ASSERT(mid);

    env->CallVoidMethod(m_timer, mid, (jdouble) limit);
    CheckAndClearException(env);
}

void Watchdog::stopTimer()
{
    JNIEnv* env = JavaScriptCore_GetJavaEnv();

    static jmethodID mid = env->GetMethodID(
            GetWatchdogTimerClass(env),
            "fwkStop",
            "()V");
    ASSERT(mid);

    env->CallVoidMethod(m_timer, mid);
    CheckAndClearException(env);
}

} // namespace JSC

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_sun_webkit_WatchdogTimer_twkFire
  (JNIEnv*, jobject, jlong nativePointer)
{
    bool* timerDidFireAddress = static_cast<bool*>(jlong_to_ptr(nativePointer));
    ASSERT(timerDidFireAddress);
    *timerDidFireAddress = true;
}

#ifdef __cplusplus
}
#endif
