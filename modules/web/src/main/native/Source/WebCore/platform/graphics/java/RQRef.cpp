/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "RQRef.h"

namespace WebCore {

RQRef::~RQRef()
{
    if (-1 != m_refID) {
        JNIEnv* env = WebCore_GetJavaEnv();

        if (env) {
            //do it if JVM is here. 
            static jmethodID mid = env->GetMethodID(PG_GetRefClass(env), "deref", "()V");
            ASSERT(mid);
            env->CallVoidMethod(m_ref, mid);

            CheckAndClearException(env);
        } 
    }
}

RQRef::operator jint() {
    if (-1 == m_refID) {
        JNIEnv* env = WebCore_GetJavaEnv();

        static jmethodID midGetId = env->GetMethodID(PG_GetRefClass(env), "getID", "()I");
        ASSERT(midGetId);
        m_refID = env->CallIntMethod(m_ref, midGetId);

        static jmethodID midRef = env->GetMethodID(PG_GetRefClass(env), "ref", "()V");
        ASSERT(midRef);
        env->CallVoidMethod(m_ref, midRef);

        CheckAndClearException(env);
    }
    return m_refID;
}

} // namespace WebCore
