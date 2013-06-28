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
    /*if (!FontPlatformData::init()) {
        assert(0);
        fprintf(stderr, "no fonts found exiting\n");
        exit(-1);
    }*/
}

const SimpleFontData* FontCache::getFontDataForCharacters(const Font& font, const UChar* characters, int length)
{
    /* utaTODO: choose the right subst here
    Look at WebCore\platform\graphics\wince\FontCacheWince.cpp
    */
    //SimpleFontData* fontData = 0;
    //fontData = new SimpleFontData(FontPlatformData(font.fontDescription(), font.family().family()));
    //return fontData;
    FontPlatformData fontData(font.fontDescription(), font.family().family());
    if (!fontData.nativeFontData()) {
        return 0; // requested font does not exist
    }
    return getCachedFontData(&fontData);
}

SimpleFontData* FontCache::getSimilarFontPlatformData(const Font& font)
{
    return 0;
}

FontPlatformData* FontCache::createFontPlatformData(const FontDescription& fontDescription, const AtomicString& family)
{
    PassRefPtr<RQRef> font = FontPlatformData::getJavaFont(fontDescription, family);
    return !font ? 0 : new FontPlatformData(font, fontDescription.computedSize());
}

void FontCache::getTraitsInFamily(AtomicString const&, WTF::Vector<unsigned int,0>&)
{
    notImplemented();
}

SimpleFontData* FontCache::getLastResortFallbackFont(const FontDescription& description, ShouldRetain shouldRetain)
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


