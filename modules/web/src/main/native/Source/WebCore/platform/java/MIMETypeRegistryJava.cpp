/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "MIMETypeRegistry.h"
#include "CString.h"

#include "JavaEnv.h"

namespace WebCore {

String MIMETypeRegistry::getMIMETypeForExtension(const String &ext)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    ASSERT(env);

    static JGClass cls(env->FindClass("com/sun/webkit/Utilities"));
    ASSERT(cls);

    static jmethodID mid = env->GetStaticMethodID(cls, 
        "fwkGetMIMETypeForExtension", 
        "(Ljava/lang/String;)Ljava/lang/String;");
    ASSERT(mid);

    JLString type(static_cast<jstring>(env->CallStaticObjectMethod(cls, mid, 
        (jstring)ext.toJavaString(env))));
    CheckAndClearException(env);

    return String(env, type);
}

bool MIMETypeRegistry::isApplicationPluginMIMEType(const String&)
{
    return false;
}

} // namespace WebCore
