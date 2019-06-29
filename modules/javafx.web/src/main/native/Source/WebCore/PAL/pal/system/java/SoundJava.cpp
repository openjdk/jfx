/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "wtf/Assertions.h"
#include <wtf/java/JavaEnv.h>
#include "Sound.h"

namespace PAL {

void systemBeep()
{
    WC_GETJAVAENV_CHKRET(env);

    JLClass cls( env->FindClass("java/awt/Toolkit") );
    ASSERT(cls);

    static jmethodID getDefaultToolkitMID = env->GetStaticMethodID(cls,
                                                            "getDefaultToolkit",
                                                            "()Ljava/awt/Toolkit;");
    ASSERT(cls);

    JLObject toolkit(env->CallStaticObjectMethod(cls, getDefaultToolkitMID));
    WTF::CheckAndClearException(env);
    ASSERT(toolkit);

    static jmethodID beepMID = env->GetMethodID(cls, "beep", "()V");
    ASSERT(beepMID);

    env->CallVoidMethod(toolkit, beepMID);
    WTF::CheckAndClearException(env);
}

} // namespace WebCore


