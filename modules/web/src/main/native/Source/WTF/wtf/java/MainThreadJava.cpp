/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "JavaRef.h"
#include "wtf/MainThread.h"

namespace WTF {
void scheduleDispatchFunctionsOnMainThread()
{
    JSC_GETJAVAENV_CHKRET(env);

    static JGClass jMainThreadCls(env->FindClass("com/sun/webkit/MainThread"));

    static jmethodID mid = env->GetStaticMethodID(
            jMainThreadCls,
            "fwkScheduleDispatchFunctions",
            "()V");

    ASSERT(mid);

    env->CallStaticVoidMethod(jMainThreadCls, mid);
    CheckAndClearException(env);
}

void initializeMainThreadPlatform()
{
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
