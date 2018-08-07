/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#if COMPILER(GCC)
#pragma GCC diagnostic ignored "-Wunused-parameter"
#endif

#include "PlatformCookieJar.h"
#include "URL.h"

#include <wtf/java/JavaEnv.h>
#include "NotImplemented.h"


namespace WebCore {

namespace CookieJarJavaInternal {
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

static String getCookies(const URL& url, bool includeHttpOnlyCookies)
{
    using namespace CookieJarJavaInternal;
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
}

void setCookiesFromDOM(const NetworkStorageSession&, const URL&, const URL& url, std::optional<uint64_t>, std::optional<uint64_t>, const String& value)
{
    using namespace CookieJarJavaInternal;
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    env->CallStaticVoidMethod(
            cookieJarClass,
            putMethod,
            (jstring) url.string().toJavaString(env),
            (jstring) value.toJavaString(env));
    CheckAndClearException(env);
}

std::pair<String, bool> cookiesForDOM(const NetworkStorageSession&, const URL&, const URL& url, std::optional<uint64_t>, std::optional<uint64_t>, IncludeSecureCookies)
{
    using namespace CookieJarJavaInternal;
    // 'HttpOnly' cookies should no be accessible from scripts, so we filter them out here.
    return { getCookies(url, false), false };
}

std::pair<String, bool> cookieRequestHeaderFieldValue(const NetworkStorageSession&, const URL& /*firstParty*/, const URL& url, std::optional<uint64_t>, std::optional<uint64_t>, IncludeSecureCookies)
{
    using namespace CookieJarJavaInternal;
    return { getCookies(url, true), true };
}

bool cookiesEnabled(const NetworkStorageSession&)
{
    return true;
}

bool getRawCookies(const NetworkStorageSession&, const URL&, const URL&, std::optional<uint64_t>, std::optional<uint64_t>, Vector<Cookie>&)
{
    notImplemented();
    return false;
}

void deleteCookie(const NetworkStorageSession&, const URL&, const String&)
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
