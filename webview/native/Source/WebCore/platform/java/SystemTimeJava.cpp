/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "NotImplemented.h"

#include "JavaEnv.h"
#include "SystemTime.h"

namespace WebCore {

// Return the current system time in seconds, using the classic POSIX epoch of January 1, 1970.
// Like time(0) from <time.h>, except with a wider range of values and higher precision.
double currentTime()
{
    JNIEnv *env = WebCore_GetJavaEnv();

    static JGClass systemCls(env->FindClass("java/lang/System"));
    static jmethodID currentTimeMillisMID = env->GetStaticMethodID(
        systemCls, 
        "currentTimeMillis", 
        "()J");
    ASSERT(currentTimeMillisMID);

    jlong jvm_time = env->CallStaticLongMethod(systemCls, currentTimeMillisMID);
    CheckAndClearException(env);

    return jvm_time / 1000.0;
}

// Return the number of seconds since a user event has been generated
float userIdleTime()
{
    notImplemented();
    return 0;
}

} // namespace WebCore
