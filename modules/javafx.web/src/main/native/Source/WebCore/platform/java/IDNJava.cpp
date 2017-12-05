/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "IDNJava.h"
#include <wtf/java/JavaEnv.h>
#include "com_sun_webkit_network_URLLoader.h"

static JGClass idnClass;
static jmethodID toASCIIMID;

static void initRefs(JNIEnv* env)
{
    if (!idnClass) {
        idnClass = JLClass(env->FindClass("java/net/IDN"));
        ASSERT(idnClass);

        toASCIIMID = env->GetStaticMethodID(
                idnClass,
                "toASCII",
                "(Ljava/lang/String;I)Ljava/lang/String;");
        ASSERT(toASCIIMID);
    }
}

namespace WebCore {

namespace IDNJava {

String toASCII(const String& hostname)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    JLString result = static_cast<jstring>(env->CallStaticObjectMethod(
            idnClass,
            toASCIIMID,
            (jstring)hostname.toJavaString(env),
            com_sun_webkit_network_URLLoader_ALLOW_UNASSIGNED));
    CheckAndClearException(env);

    return String(env, result);
}

} // namespace IDNJava

} // namespace WebCore
