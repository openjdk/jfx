/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "NotImplemented.h"

#include "AtomicString.h"
#include "Font.h"
#include "FontCache.h"
#include "FontData.h"
#include "FontPlatformData.h"
#include "SimpleFontData.h"

namespace WebCore {

void FontCache::platformInit()
{
}

PassRefPtr<SimpleFontData> FontCache::systemFallbackForCharacters(const FontDescription& fontDescription, const SimpleFontData*, bool, const UChar* characters, int length)
{
    return 0;
}


PassOwnPtr<FontPlatformData> FontCache::createFontPlatformData(const FontDescription& fontDescription, const AtomicString& family)
{
    return FontPlatformData::create(fontDescription, family);
}

void FontCache::getTraitsInFamily(AtomicString const&, WTF::Vector<unsigned int,0>&)
{
    notImplemented();
}

PassRefPtr<SimpleFontData> FontCache::getLastResortFallbackFont(const FontDescription& description, ShouldRetain shouldRetain)
{
    // FIXME: Would be even better to somehow get the user's default font here.
    // For now we'll pick the default that the user would get without changing any prefs.
    switch (description.genericFamily()) {
        case FontDescription::SansSerifFamily:
            return getCachedFontData(description, AtomicString("sans-serif"));
        case FontDescription::MonospaceFamily:
            return getCachedFontData(description, AtomicString("monospaced"));
        case FontDescription::SerifFamily:
        default:
            return getCachedFontData(description, AtomicString("serif"));
    }
}

}


