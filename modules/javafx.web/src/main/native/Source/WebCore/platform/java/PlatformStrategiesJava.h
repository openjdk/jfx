/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
 */

#pragma once

#include "CookiesStrategy.h"
#include "LoaderStrategy.h"
#include "PasteboardStrategy.h"
#include "PlatformStrategies.h"

namespace WebCore {

class PlatformStrategiesJava final : public PlatformStrategies,
                                     private CookiesStrategy
 {
public:
    static void initialize();

private:
    PlatformStrategiesJava();

    // PlatformStrategies
    CookiesStrategy* createCookiesStrategy() override;
    LoaderStrategy* createLoaderStrategy() override;
    PasteboardStrategy* createPasteboardStrategy() override;

    // CookiesStrategy
    String cookiesForDOM(const NetworkStorageSession&, const URL& firstParty, const URL&) override;
    void setCookiesFromDOM(const NetworkStorageSession&, const URL& firstParty, const URL&, const String&) override;
    bool cookiesEnabled(const NetworkStorageSession&, const URL& firstParty, const URL&) override;
    String cookieRequestHeaderFieldValue(const NetworkStorageSession&, const URL& firstParty, const URL&) override;
    bool getRawCookies(const NetworkStorageSession&, const URL& firstParty, const URL&, Vector<Cookie>&) override;
    void deleteCookie(const NetworkStorageSession&, const URL&, const String&) override;
    String cookieRequestHeaderFieldValue(SessionID, const URL& firstParty, const URL&) override;
    void addCookie(const NetworkStorageSession&, const URL&, const Cookie&) override;

    BlobRegistry* createBlobRegistry() override;
};
} // namespace WebCore
