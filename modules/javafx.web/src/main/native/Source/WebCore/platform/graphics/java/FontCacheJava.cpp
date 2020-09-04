/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "NotImplemented.h"
#include <wtf/text/AtomString.h>
#include "FontCache.h"
#include "FontRanges.h"
#include "FontPlatformData.h"
#include "Font.h"
#include "FontSelector.h"

namespace WebCore {

void FontCache::platformInit()
{
}

RefPtr<Font> FontCache::systemFallbackForCharacters(const FontDescription&, const Font*, IsForPlatformFont, PreferColoredFont, const UChar*, unsigned)
{
    return nullptr;
}


std::unique_ptr<FontPlatformData> FontCache::createFontPlatformData(const FontDescription& fontDescription, const AtomString& family, const FontFeatureSettings*, FontSelectionSpecifiedCapabilities) {

    return FontPlatformData::create(fontDescription, family);
}

Ref<Font> FontCache::lastResortFallbackFont(const FontDescription& fontDescription)
{
    // We want to return a fallback font here, otherwise the logic preventing FontConfig
    // matches for non-fallback fonts might return 0. See isFallbackFontAllowed.
    static AtomString timesStr("serif");
    return *fontForFamily(fontDescription, timesStr);
}

Vector<String> FontCache::systemFontFamilies()
{
    // FIXME: <https://webkit.org/b/147018> Web Inspector: [Freetype] Allow inspector to retrieve a list of system fonts
    // FIXME: JDK-8146864
    notImplemented();
    return Vector<String>();
}

bool FontCache::isSystemFontForbiddenForEditing(const String&)
{
    return false;
}

const AtomString& FontCache::platformAlternateFamilyName(const AtomString&)
{
    notImplemented();
    return nullAtom();
}

Vector<FontSelectionCapabilities> FontCache::getFontSelectionCapabilitiesInFamily(const AtomString&, AllowUserInstalledFonts)
{
    notImplemented();
    return { };
}

}

