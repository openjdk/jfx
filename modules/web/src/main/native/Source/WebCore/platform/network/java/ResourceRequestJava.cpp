/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "ResourceRequest.h"

#include "JavaEnv.h"

static JGClass networkContextClass;
static jmethodID getMaximumHTTPConnectionCountPerHostMethod;

static void initRefs(JNIEnv* env)
{
    if (!networkContextClass) {
        networkContextClass = JLClass(env->FindClass(
                "com/sun/webkit/network/NetworkContext"));
        ASSERT(networkContextClass);

        getMaximumHTTPConnectionCountPerHostMethod = env->GetStaticMethodID(
                networkContextClass,
                "fwkGetMaximumHTTPConnectionCountPerHost",
                "()I");
        ASSERT(getMaximumHTTPConnectionCountPerHostMethod);
    }
}

namespace WebCore {

unsigned initializeMaximumHTTPConnectionCountPerHost()
{
    // This is used by the loader to control the number of parallel load
    // requests. Our java framework employs HttpURLConnection for all
    // HTTP exchanges, so we delegate this call to java to return
    // the value of the "http.maxConnections" system property.
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    jint result = env->CallStaticIntMethod(
            networkContextClass,
            getMaximumHTTPConnectionCountPerHostMethod);
    CheckAndClearException(env);

    ASSERT(result >= 0);
    return result;
}

} // namespace WebCore
