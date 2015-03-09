/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "PlatformCookieJar.h"
#include "URL.h"

#include "JavaEnv.h"
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

static String getCookies(const URL& url, bool includeHttpOnlyCookies)
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

void setCookiesFromDOM(const NetworkStorageSession&, const URL&, const URL& url, const String& value)
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

String cookiesForDOM(const NetworkStorageSession&, const URL&, const URL& url)
{
    // 'HttpOnly' cookies should no be accessible from scripts, so we filter them out here.
    return getCookies(url, false);
}

String cookieRequestHeaderFieldValue(const NetworkStorageSession&, const URL&, const URL& url)
{
    return getCookies(url, true);
}

bool cookiesEnabled(const NetworkStorageSession&, const URL&, const URL&)
{
    return true;
}

bool getRawCookies(const NetworkStorageSession&, const URL&, const URL& url, Vector<Cookie>& rawCookies)
{
    notImplemented();
    return false;
}

void deleteCookie(const NetworkStorageSession&, const URL& url, const String& name)
{
    notImplemented();
}

void getHostnamesWithCookies(const NetworkStorageSession&, HashSet<String>&)
{
    notImplemented();
}

void deleteCookiesForHostname(const NetworkStorageSession&, const String& /*hostname*/)
{
    notImplemented();
}

void deleteAllCookies(const NetworkStorageSession&)
{
    notImplemented();
}

} // namespace WebCore
