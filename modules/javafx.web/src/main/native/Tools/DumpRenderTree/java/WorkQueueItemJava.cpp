/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "JavaEnv.h"
#include <JavaScriptCore/JSRetainPtr.h>
#include <JavaScriptCore/JSStringRef.h>
#include <wtf/java/JavaRef.h>

#include "WorkQueueItem.h"

extern jstring JSStringRef_to_jstring(JSStringRef ref, JNIEnv* env);
extern JSStringRef jstring_to_JSStringRef(jstring str, JNIEnv* env);

bool LoadItem::invoke() const
{
    JNIEnv* env = DumpRenderTree_GetJavaEnv();
    JLString jUrl(JSStringRef_to_jstring(m_url.get(), env));
    env->CallStaticObjectMethod(getDumpRenderTreeClass(), getLoadURLMID(), (jstring)jUrl);
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
    env->CallStaticObjectMethod(getDumpRenderTreeClass(), getGoBackForward(), m_howFar);
    CheckAndClearException(env);

    return true;
}
