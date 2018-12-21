/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "FrameNetworkingContextJava.h"
#include "NetworkStorageSession.h"

#include "WebKitLegacy/WebCoreSupport/WebResourceLoadScheduler.h"
#include <wtf/NeverDestroyed.h>
#include <WebCore/BlobRegistryImpl.h>
#include <WebCore/NetworkStorageSession.h>
#include <WebCore/Page.h>
#include <WebCore/PageGroup.h>

namespace WebCore {
void PlatformStrategiesJava::initialize()
{
    static NeverDestroyed<PlatformStrategiesJava> platformStrategies;
}

PlatformStrategiesJava::PlatformStrategiesJava()
{
    setPlatformStrategies(this);
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

std::pair<String, bool> PlatformStrategiesJava::cookiesForDOM(const NetworkStorageSession& session, const URL& firstParty, const SameSiteInfo& sameSiteInfo, const URL& url, std::optional<uint64_t> frameID, std::optional<uint64_t> pageID, IncludeSecureCookies includeSecureCookies)
{
    return session.cookiesForDOM(firstParty, sameSiteInfo, url, frameID, pageID, includeSecureCookies);
}

void PlatformStrategiesJava::setCookiesFromDOM(const NetworkStorageSession& session, const URL& firstParty, const SameSiteInfo& sameSiteInfo, const URL& url, std::optional<uint64_t> frameID, std::optional<uint64_t> pageID, const String& cookieString)
{
    session.setCookiesFromDOM(firstParty, sameSiteInfo, url, frameID, pageID, cookieString);
}

bool PlatformStrategiesJava::cookiesEnabled(const NetworkStorageSession& session)
{
    return session.cookiesEnabled();
}

std::pair<String, bool> PlatformStrategiesJava::cookieRequestHeaderFieldValue(const NetworkStorageSession& session, const URL& firstParty, const SameSiteInfo& sameSiteInfo, const URL& url, std::optional<uint64_t> frameID, std::optional<uint64_t> pageID, IncludeSecureCookies includeSecureCookies)
{
    return session.cookieRequestHeaderFieldValue(firstParty, sameSiteInfo, url, frameID, pageID, includeSecureCookies);
}

std::pair<String, bool> PlatformStrategiesJava::cookieRequestHeaderFieldValue(PAL::SessionID sessionID, const URL& firstParty, const SameSiteInfo& sameSiteInfo, const URL& url, std::optional<uint64_t> frameID, std::optional<uint64_t> pageID, IncludeSecureCookies includeSecureCookies)
{
    auto& session = sessionID.isEphemeral() ? FrameNetworkingContextJava::ensurePrivateBrowsingSession() : NetworkStorageSession::defaultStorageSession();
    return session.cookieRequestHeaderFieldValue(firstParty, sameSiteInfo, url, frameID, pageID, includeSecureCookies);
}

bool PlatformStrategiesJava::getRawCookies(const NetworkStorageSession& session, const URL& firstParty, const SameSiteInfo& sameSiteInfo, const URL& url, std::optional<uint64_t> frameID, std::optional<uint64_t> pageID, Vector<Cookie>& rawCookies)
{
    return session.getRawCookies(firstParty, sameSiteInfo, url, frameID, pageID, rawCookies);
}

void PlatformStrategiesJava::deleteCookie(const NetworkStorageSession& session, const URL& url, const String& cookieName)
{
    session.deleteCookie(url, cookieName);
}

} // namespace WebCore
