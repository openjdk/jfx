/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "JavaEnv.h"
#include <JavaScriptCore/JSRetainPtr.h>
#include <JavaScriptCore/JSStringRef.h>
#include <wtf/java/JavaRef.h>

#include "WorkQueueItem.h"

extern jclass getDRTClass(JNIEnv* env);
extern jstring JSStringRef_to_jstring(JSStringRef ref, JNIEnv* env);
extern JSStringRef jstring_to_JSStringRef(jstring str, JNIEnv* env);

bool LoadItem::invoke() const
{
    JNIEnv* env = DumpRenderTree_GetJavaEnv();

    JLString jUrl(JSStringRef_to_jstring(m_url.get(), env));

    static jmethodID loadUrlMID = env->GetStaticMethodID(getDRTClass(env), "loadURL", "(Ljava/lang/String;)V");
    ASSERT(loadUrlMID);
    env->CallStaticObjectMethod(getDRTClass(env), loadUrlMID, (jstring)jUrl);
    CheckAndClearException(env);
    
    return true;
}

bool ReloadItem::invoke() const
{
    // FIXME: implement
    return true;
}

bool ScriptItem::invoke() const
{
    // FIXME: implement
    return true;
}

bool BackForwardItem::invoke() const
{
    JNIEnv* env = DumpRenderTree_GetJavaEnv();

    static jmethodID goMID = env->GetStaticMethodID(getDRTClass(env), "goBackForward", "(I)V");
    ASSERT(goMID);
    env->CallStaticObjectMethod(getDRTClass(env), goMID, m_howFar);
    CheckAndClearException(env);

    return true;
}
