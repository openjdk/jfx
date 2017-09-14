/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "NotImplemented.h"
#include <wtf/text/AtomicString.h>
#include "FontCache.h"
#include "FontRanges.h"
#include "FontPlatformData.h"
#include "Font.h"
#include "FontSelector.h"

namespace WebCore {

void FontCache::platformInit()
{
}

RefPtr<Font> FontCache::systemFallbackForCharacters(const FontDescription&, const Font*, bool, const UChar*, unsigned)
{
    return nullptr;
}


std::unique_ptr<FontPlatformData> FontCache::createFontPlatformData(const FontDescription& fontDescription, const AtomicString& family, const FontFeatureSettings*, const FontVariantSettings*) {

    return FontPlatformData::create(fontDescription, family);
}

Vector<FontTraitsMask> FontCache::getTraitsInFamily(const AtomicString&)
{
    notImplemented();
    return Vector<FontTraitsMask>();
}

Ref<Font> FontCache::lastResortFallbackFont(const FontDescription& fontDescription)
{
    // We want to return a fallback font here, otherwise the logic preventing FontConfig
    // matches for non-fallback fonts might return 0. See isFallbackFontAllowed.
    static AtomicString timesStr("serif");
    return *fontForFamily(fontDescription, timesStr);
}

Vector<String> FontCache::systemFontFamilies()
{
    // FIXME: <https://webkit.org/b/147018> Web Inspector: [Freetype] Allow inspector to retrieve a list of system fonts
    // FIXME: JDK-8146864
    notImplemented();
    return Vector<String>();
}

Ref<Font> FontCache::lastResortFallbackFontForEveryCharacter(const FontDescription& fontDescription)
{
    return lastResortFallbackFont(fontDescription);
}

const AtomicString& FontCache::platformAlternateFamilyName(const AtomicString&)
{
    notImplemented();
    return nullAtom;
}

}

