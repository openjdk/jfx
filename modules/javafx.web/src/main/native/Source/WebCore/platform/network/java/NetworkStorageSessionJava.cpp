/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include "config.h"
#include "NetworkStorageSession.h"

#include "Cookie.h"
#include "CookieRequestHeaderFieldProxy.h"
#include "HTTPCookieAcceptPolicy.h"
#include "NetworkingContext.h"
#include "NotImplemented.h"
#include "ResourceHandle.h"

#include <wtf/MainThread.h>
#include <wtf/NeverDestroyed.h>
#include <wtf/URL.h>
#include "PlatformJavaClasses.h"

namespace WebCore {

namespace CookieInternalJava {

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
    using namespace CookieInternalJava;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    JLString result = static_cast<jstring>(env->CallStaticObjectMethod(
            cookieJarClass,
            getMethod,
            (jstring) url.string().toJavaString(env),
            bool_to_jbool(includeHttpOnlyCookies)));
    WTF::CheckAndClearException(env);

    return result ? String(env, result) : emptyString();
}
}

NetworkStorageSession::NetworkStorageSession(PAL::SessionID sessionID, const String& alternativeServicesDirectory)
    : m_sessionID(sessionID)
{
}

NetworkStorageSession::~NetworkStorageSession()
{
}

void NetworkStorageSession::setCookiesFromDOM(const URL& /*firstParty*/, const SameSiteInfo&, const URL& url, std::optional<FrameIdentifier>, std::optional<PageIdentifier>, ApplyTrackingPrevention, const String& value, ShouldRelaxThirdPartyCookieBlocking) const
{
    using namespace CookieInternalJava;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    env->CallStaticVoidMethod(
            cookieJarClass,
            putMethod,
            (jstring) url.string().toJavaString(env),
            (jstring) value.toJavaString(env));
    WTF::CheckAndClearException(env);
}

std::pair<String, bool> NetworkStorageSession::cookiesForDOM(const URL&, const SameSiteInfo&, const URL& url, std::optional<FrameIdentifier>, std::optional<PageIdentifier>, IncludeSecureCookies, ApplyTrackingPrevention, ShouldRelaxThirdPartyCookieBlocking) const
{
    // 'HttpOnly' cookies should no be accessible from scripts, so we filter them out here.
    return { CookieInternalJava::getCookies(url, false), false };
}

std::pair<String, bool> NetworkStorageSession::cookieRequestHeaderFieldValue(const URL& /*firstParty*/, const SameSiteInfo&, const URL& url, std::optional<FrameIdentifier>, std::optional<PageIdentifier>, IncludeSecureCookies, ApplyTrackingPrevention, ShouldRelaxThirdPartyCookieBlocking) const
{
    return { CookieInternalJava::getCookies(url, true), true };
}

std::pair<String, bool> NetworkStorageSession::cookieRequestHeaderFieldValue(const CookieRequestHeaderFieldProxy& headerFieldProxy) const
{
    return { CookieInternalJava::getCookies(headerFieldProxy.firstParty, true), true };
}

bool NetworkStorageSession::getRawCookies(const URL& /*firstParty*/, const SameSiteInfo&, const URL&, std::optional<FrameIdentifier>, std::optional<PageIdentifier>, ApplyTrackingPrevention, ShouldRelaxThirdPartyCookieBlocking, Vector<Cookie>&) const
{
    notImplemented();
    return false;
}

HTTPCookieAcceptPolicy NetworkStorageSession::cookieAcceptPolicy() const
{
    return HTTPCookieAcceptPolicy::AlwaysAccept;
}

void NetworkStorageSession::setCookies(const Vector<Cookie>&, const URL&, const URL&)
{
    // FIXME: Implement for WebKit to use.
}

void NetworkStorageSession::deleteCookiesForHostnames(const Vector<String>& hostnames, IncludeHttpOnlyCookies includeHttpOnlyCookies, ScriptWrittenCookiesOnly, CompletionHandler<void()>&& completionHandler)
{
    UNUSED_PARAM(hostnames);
        UNUSED_PARAM(includeHttpOnlyCookies);
     // FIXME: Implement for WebKit to use.
    completionHandler();
}

void NetworkStorageSession::setCookie(const Cookie&)
{
    // FIXME: Implement for WebKit to use.
}

void NetworkStorageSession::deleteCookie(const Cookie&, CompletionHandler<void()>&&)
{
    // FIXME: Implement for WebKit to use.
}

void NetworkStorageSession::deleteCookie(const URL&, const String&,CompletionHandler<void()>&&) const
{
    // FIXME: Implement for WebKit to use.
}

Vector<Cookie> NetworkStorageSession::getAllCookies()
{
    // FIXME: Implement for WebKit to use.
    return { };
}

Vector<Cookie> NetworkStorageSession::getCookies(const URL&)
{
    // FIXME: Implement for WebKit to use.
    return { };
}

/*void NetworkStorageSession::flushCookieStore()
{
    // FIXME: Implement for WebKit to use.
}*/

} // namespace WebCore

