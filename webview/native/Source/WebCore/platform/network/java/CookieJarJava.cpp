/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "CookieJar.h"

#include "JavaEnv.h"
#include "KURL.h"
#include "NotImplemented.h"

static JGClass cookieJarClass;
static jmethodID getMethod;
static jmethodID putMethod;

static void initRefs(JNIEnv* env)
{
    if (!cookieJarClass) {
        cookieJarClass = JLClass(env->FindClass(
                "com/sun/webkit/network/CookieJar"));
        ASSERT(cookieJarClass);

        getMethod = env->GetStaticMethodID(
                cookieJarClass,
                "fwkGet",
                "(Ljava/lang/String;Z)Ljava/lang/String;");
        ASSERT(getMethod);

        putMethod = env->GetStaticMethodID(
                cookieJarClass,
                "fwkPut",
                "(Ljava/lang/String;Ljava/lang/String;)V");
        ASSERT(putMethod);
    }
}

namespace WebCore {

static String getCookies(const KURL& url, bool includeHttpOnlyCookies)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    JLString result = static_cast<jstring>(env->CallStaticObjectMethod(
            cookieJarClass,
            getMethod,
            (jstring) url.string().toJavaString(env),
            bool_to_jbool(includeHttpOnlyCookies)));
    CheckAndClearException(env);

    return result ? String(env, result) : emptyString();
}

String cookies(const Document*, const KURL& url)
{
    return getCookies(url, false);
}

String cookieRequestHeaderFieldValue(const Document*, const KURL& url)
{
    return getCookies(url, true);
}

void setCookies(Document*, const KURL& url, const String& value)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    env->CallStaticVoidMethod(
            cookieJarClass,
            putMethod,
            (jstring) url.string().toJavaString(env),
            (jstring) value.toJavaString(env));
    CheckAndClearException(env);    
}

bool cookiesEnabled(const Document*)
{
    return true;
}

bool getRawCookies(const Document*, const KURL&, Vector<Cookie>&)
{
    notImplemented();
    return false;
}

void deleteCookie(const Document*, const KURL&, const String&)
{
    notImplemented();
}

} // namespace WebCore
