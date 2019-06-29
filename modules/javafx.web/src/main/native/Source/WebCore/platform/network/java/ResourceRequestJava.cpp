/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "ResourceRequest.h"

#include "PlatformJavaClasses.h"

namespace ResourceRequestJavaInternal {

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
}

namespace WebCore {

unsigned initializeMaximumHTTPConnectionCountPerHost()
{
    using namespace ResourceRequestJavaInternal;
    // This is used by the loader to control the number of parallel load
    // requests. Our java framework employs HttpURLConnection for all
    // HTTP exchanges, so we delegate this call to java to return
    // the value of the "http.maxConnections" system property.
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    jint result = env->CallStaticIntMethod(
            networkContextClass,
            getMaximumHTTPConnectionCountPerHostMethod);
    WTF::CheckAndClearException(env);

    ASSERT(result >= 0);
    return result;
}

} // namespace WebCore
