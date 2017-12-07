/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "PlatformStrategiesJava.h"

#include "NotImplemented.h"
#include "PlatformCookieJar.h"
#include "NetworkStorageSession.h"

#include "WebKitLegacy/WebCoreSupport/WebResourceLoadScheduler.h"
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

} // namespace WebCore
