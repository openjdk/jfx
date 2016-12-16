/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#if COMPILER(GCC)
#pragma GCC diagnostic ignored "-Wunused-parameter"
#endif

#include "NotImplemented.h"
#include <wtf/text/AtomicString.h>
#include "Font.h"
#include "FontCache.h"
#include "FontRanges.h"
#include "FontPlatformData.h"
#include "Font.h"

namespace WebCore {

void FontCache::platformInit()
{
}

RefPtr<Font> FontCache::systemFallbackForCharacters(const FontDescription& fontDescription, const Font*, bool, const UChar* characters, unsigned length)
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

}


