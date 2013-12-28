/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "MainThreadJava.h"
#include "JavaRef.h"

#if USE(PTHREADS)
  #include "../ThreadingPthreads.cpp"
  #include "../ThreadIdentifierDataPthreads.cpp"
#else
  #include "../ThreadingWin.cpp"
  #include "../ThreadSpecificWin.cpp"
#endif

namespace WTF {
    void scheduleDispatchFunctionsOnMainThread()
    {
        JNIEnv* env = JavaScriptCore_GetJavaEnv();

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
        MainThreadJavaScheduler::instance();
    }
} // namespace WTF

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     com_sun_webkit_MainThread
 * Method:    twkScheduleDispatchFunctions
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_webkit_MainThread_twkScheduleDispatchFunctions
  (JNIEnv* env, jobject)
{
    WTF::MainThreadJavaScheduler::scheduleDispatch();
}

#ifdef __cplusplus
}
#endif
