/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
 */

#include "config.h"
#include "PlatformStrategiesJava.h"

#include "NotImplemented.h"
#include "PlatformCookieJar.h"
#include "NetworkStorageSession.h"

#include "WebKit/WebCoreSupport/WebResourceLoadScheduler.h"
#include <WebCore/BlobRegistryImpl.h>
#include <WebCore/Page.h>
#include <WebCore/PageGroup.h>
#include <WebCore/LinkHash.h>

namespace WebCore {
void PlatformStrategiesJava::initialize()
{
    DEPRECATED_DEFINE_STATIC_LOCAL(PlatformStrategiesJava, platformStrategies, ());
    setPlatformStrategies(&platformStrategies);
}

PlatformStrategiesJava::PlatformStrategiesJava()
{
}

CookiesStrategy* PlatformStrategiesJava::createCookiesStrategy()
{
    return this;
}

LoaderStrategy* PlatformStrategiesJava::createLoaderStrategy()
{
    return new WebResourceLoadScheduler;
}

PasteboardStrategy* PlatformStrategiesJava::createPasteboardStrategy()
{
    // This is currently used only by Mac code.
    notImplemented();
    return 0;
}

WebCore::BlobRegistry* PlatformStrategiesJava::createBlobRegistry()
{
    return new WebCore::BlobRegistryImpl;
}

// CookiesStrategy
String PlatformStrategiesJava::cookiesForDOM(const NetworkStorageSession& session, const URL& firstParty, const URL& url)
{
    return WebCore::cookiesForDOM(session, firstParty, url);
}

void PlatformStrategiesJava::setCookiesFromDOM(const NetworkStorageSession& session, const URL& firstParty, const URL& url, const String& cookieString)
{
    WebCore::setCookiesFromDOM(session, firstParty, url, cookieString);
}

bool PlatformStrategiesJava::cookiesEnabled(const NetworkStorageSession& session, const URL& firstParty, const URL& url)
{
    return WebCore::cookiesEnabled(session, firstParty, url);
}

String PlatformStrategiesJava::cookieRequestHeaderFieldValue(const NetworkStorageSession& session, const URL& firstParty, const URL& url)
{
    return WebCore::cookieRequestHeaderFieldValue(session, firstParty, url);
}

bool PlatformStrategiesJava::getRawCookies(const NetworkStorageSession& session, const URL& firstParty, const URL& url, Vector<Cookie>& rawCookies)
{
    return WebCore::getRawCookies(session, firstParty, url, rawCookies);
}

void PlatformStrategiesJava::deleteCookie(const NetworkStorageSession& session, const URL& url, const String& cookieName)
{
    WebCore::deleteCookie(session, url, cookieName);
}

String PlatformStrategiesJava::cookieRequestHeaderFieldValue(SessionID session, const URL& firstParty, const URL& url)
{
    return PlatformStrategiesJava::cookieRequestHeaderFieldValue(*NetworkStorageSession::storageSession(session), firstParty, url);
}

void PlatformStrategiesJava::addCookie(const NetworkStorageSession&, const URL&, const Cookie&)
{
    notImplemented();
}

} // namespace WebCore
