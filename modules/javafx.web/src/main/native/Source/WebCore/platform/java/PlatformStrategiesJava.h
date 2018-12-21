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

#pragma once

#include <WebCore/CookiesStrategy.h>
#include <WebCore/LoaderStrategy.h>
#include <WebCore/PlatformStrategies.h>
#include <wtf/Forward.h>

namespace WebCore {

class PlatformStrategiesJava final : public PlatformStrategies,
                                     private CookiesStrategy
 {
public:
    static void initialize();
    PlatformStrategiesJava();

private:

    // PlatformStrategies
    CookiesStrategy* createCookiesStrategy() override;
    LoaderStrategy* createLoaderStrategy() override;
    PasteboardStrategy* createPasteboardStrategy() override;

    // CookiesStrategy
    std::pair<String, bool> cookiesForDOM(const NetworkStorageSession&, const URL& firstParty, const SameSiteInfo&, const URL&, std::optional<uint64_t> frameID, std::optional<uint64_t> pageID, IncludeSecureCookies) override;
    void setCookiesFromDOM(const NetworkStorageSession&, const URL& firstParty, const SameSiteInfo&, const URL&, std::optional<uint64_t> frameID, std::optional<uint64_t> pageID, const String& cookieString) override;
    bool cookiesEnabled(const WebCore::NetworkStorageSession&) override;
    std::pair<String, bool> cookieRequestHeaderFieldValue(const NetworkStorageSession&, const URL& firstParty, const SameSiteInfo&, const URL&, std::optional<uint64_t> frameID, std::optional<uint64_t> pageID, IncludeSecureCookies) override;
    std::pair<String, bool> cookieRequestHeaderFieldValue(PAL::SessionID, const URL& firstParty, const SameSiteInfo&, const URL&, std::optional<uint64_t> frameID, std::optional<uint64_t> pageID, IncludeSecureCookies) override;
    bool getRawCookies(const NetworkStorageSession&, const URL& firstParty, const SameSiteInfo&, const URL&, std::optional<uint64_t> frameID, std::optional<uint64_t> pageID, Vector<Cookie>&) override;
    void deleteCookie(const WebCore::NetworkStorageSession&, const WebCore::URL&, const String&) override;

    BlobRegistry* createBlobRegistry() override;
};
} // namespace WebCore
