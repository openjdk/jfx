/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "wtf/Assertions.h"
#include "JavaEnv.h"
#include "Sound.h"

namespace WebCore {

void systemBeep()
{
    JNIEnv* env = WebCore_GetJavaEnv();

    JLClass cls( env->FindClass("java/awt/Toolkit") );
    ASSERT(cls);

    static jmethodID getDefaultToolkitMID = env->GetStaticMethodID(cls,
                                                            "getDefaultToolkit",
                                                            "()Ljava/awt/Toolkit;");
    ASSERT(cls);

    JLObject toolkit(env->CallStaticObjectMethod(cls, getDefaultToolkitMID));
    CheckAndClearException(env);
    ASSERT(toolkit);

    static jmethodID beepMID = env->GetMethodID(cls, "beep", "()V");
    ASSERT(beepMID);

    env->CallVoidMethod(toolkit, beepMID);
    CheckAndClearException(env);
}

} // namespace WebCore


