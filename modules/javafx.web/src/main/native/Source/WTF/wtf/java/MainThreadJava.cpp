/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include <wtf/java/JavaEnv.h>
#include <wtf/java/JavaRef.h>
#include <wtf/MainThread.h>

namespace WTF {
void scheduleDispatchFunctionsOnMainThread()
{
    AttachThreadAsNonDaemonToJavaEnv autoAttach;
    JNIEnv* env = autoAttach.env();
    static JGClass jMainThreadCls(env->FindClass("com/sun/webkit/MainThread"));

    static jmethodID mid = env->GetStaticMethodID(
            jMainThreadCls,
            "fwkScheduleDispatchFunctions",
            "()V");

    ASSERT(mid);

    env->CallStaticVoidMethod(jMainThreadCls, mid);
    WTF::CheckAndClearException(env);
}

void initializeMainThreadPlatform()
{
}

bool isMainThreadIfInitialized()
{
    return isMainThread();
}

bool isMainThread()
{
    AttachThreadAsNonDaemonToJavaEnv autoAttach;
    JNIEnv* env = autoAttach.env();
    static JGClass jMainThreadCls(env->FindClass("com/sun/webkit/MainThread"));

    static jmethodID mid = env->GetStaticMethodID(
            jMainThreadCls,
            "fwkIsMainThread",
            "()Z");

    ASSERT(mid);

    jboolean isMainThread = env->CallStaticBooleanMethod(jMainThreadCls, mid);
    WTF::CheckAndClearException(env);
    return isMainThread == JNI_TRUE;
}

extern "C" {

/*
 * Class:     com_sun_webkit_MainThread
 * Method:    twkScheduleDispatchFunctions
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_webkit_MainThread_twkScheduleDispatchFunctions
  (JNIEnv*, jobject)
{
    dispatchFunctionsFromMainThread();
}
}

} // namespace WTF
