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

    BlobRegistry* createBlobRegistry() override;
};
} // namespace WebCore
