/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "ChromiumBridge.h"

namespace WebCore {
#if ENABLE(SKIA)
static bool isFontInstalled(const char *name) {
    static uint32_t defID = SkTypeface::UniqueID(NULL);
    return defID!=SkRefPtr<SkTypeface>(SkTypeface::CreateFromName(name, SkTypeface::kNormal))->uniqueID();
}

static const char* getInstalledFont(const char **ppFonts, size_t length)
{
    for (int i = 0; i < length; ++i) {
        if (isFontInstalled(ppFonts[i])) {
            return ppFonts[i];
        }
    }
    return NULL;
}

static const char* getDefaultCommonFont()
{
    static const char* fontName;
    static bool initialized = false; //used here only - the end if chains
    if (!initialized) {
        static const char* fonts[] = {
            "tahoma",
            "arial unicode ms",
            "lucida sans unicode",
            "microsoft sans serif",
            "palatino linotype",
            // Four fonts below (and code2000 at the end) are not from MS, but
            // once installed, cover a very wide range of characters.
            "freeserif",
            "freesans",
            "gentium",
            "gentiumalt",
            "ms pgothic",
            "simsun",
            "gulim",
            "pmingliu",
            "code2000",
            "code2001",
            "droidsansfallback",
        };
        fontName = getInstalledFont(fonts, sizeof(fonts)/sizeof(*fonts));
        initialized = true;
    }
    return fontName;
}

static const char* getDefaultCJKFont()
{
    static const char* fontName = NULL;
    if (!fontName) {
        static const char* fonts[] = {
            "arial unicode ms",
            "ms pgothic",
            "simsun-extb",
            "simsun-exta",
            "simsun",
            "gulim",
            "pmingliu",
            "wenquanyi zen hei", // partial CJK Ext. A coverage but more
                                  // widely known to Chinese users.
            "ar pl shanheisun uni",
            "ar pl zenkai uni",
            "han nom a",  // Complete CJK Ext. A coverage
            "code2000",   // Complete CJK Ext. A coverage
            "code2001",   // Complete CJK Ext. A coverage
            //android
            "droidsansjapanese",
            // CJK Ext. B fonts are not listed here because it's of no use
            // with our current non-BMP character handling because we use
            // Uniscribe for it and that code path does not go through here.
        };
        fontName = getInstalledFont(fonts, sizeof(fonts)/sizeof(*fonts));
        if (!fontName) {
            fontName = getDefaultCommonFont();
        }
    }
    return fontName;
}

static const char* getDefaultKoreanFont()
{
    static const char* fontName = NULL;
    if (!fontName) {
        static const char* fonts[] = {
            "arial unicode ms",
            "malgun gothic",
            "gulim",
            //android
        };
        fontName = getInstalledFont(fonts, sizeof(fonts)/sizeof(*fonts));
        if (!fontName) {
            fontName = getDefaultCJKFont();
        }
    }
    return fontName;
}

static const char* getDefaultArabicFont()
{
    static const char* fontName = NULL;
    if (!fontName) {
        static const char* fonts[] = {
            //android
            "droidsansarabic",
        };
        fontName = getInstalledFont(fonts, sizeof(fonts)/sizeof(*fonts));
        if (!fontName) {
            fontName = getDefaultCommonFont();
        }
    }
    return fontName;
}

static const char* getDefaultHebrewFont()
{
    static const char* fontName = NULL;
    if (!fontName) {
        static const char* fonts[] = {
            //android
            "droidsanshebrew",
        };
        fontName = getInstalledFont(fonts, sizeof(fonts)/sizeof(*fonts));
        if (!fontName) {
            fontName = getDefaultCommonFont();
        }
    }
    return fontName;
}

static const char* getDefaultThaiFont()
{
    static const char* fontName = NULL;
    if (!fontName) {
        static const char* fonts[] = {
            //android
            "droidsansthai",
        };
        fontName = getInstalledFont(fonts, sizeof(fonts)/sizeof(*fonts));
        if (!fontName) {
            fontName = getDefaultCommonFont();
        }
    }
    return fontName;
}


#if USE(ICU_UNICODE)

// There are a lot of characters in USCRIPT_COMMON that can be covered
// by fonts for scripts closely related to them. See
// http://unicode.org/cldr/utility/list-unicodeset.jsp?a=[:Script=Common:]
// FIXME: make this more efficient with a wider coverage
static UScriptCode getScriptBasedOnUnicodeBlock(int ucs4)
{
    UBlockCode block = ublock_getCode(ucs4);
    switch (block) {
    case UBLOCK_CJK_SYMBOLS_AND_PUNCTUATION:
        return USCRIPT_HAN;
    case UBLOCK_HIRAGANA:
    case UBLOCK_KATAKANA:
        return USCRIPT_HIRAGANA;
    case UBLOCK_ARABIC:
        return USCRIPT_ARABIC;
    case UBLOCK_THAI:
        return USCRIPT_THAI;
    case UBLOCK_GREEK:
        return USCRIPT_GREEK;
    case UBLOCK_DEVANAGARI:
        // For Danda and Double Danda (U+0964, U+0965), use a Devanagari
        // font for now although they're used by other scripts as well.
        // Without a context, we can't do any better.
        return USCRIPT_DEVANAGARI;
    case UBLOCK_ARMENIAN:
        return USCRIPT_ARMENIAN;
    case UBLOCK_GEORGIAN:
        return USCRIPT_GEORGIAN;
    case UBLOCK_KANNADA:
        return USCRIPT_KANNADA;
    default:
        return USCRIPT_COMMON;
    }
}

static UScriptCode getScript(int ucs4)
{
    UErrorCode err = U_ZERO_ERROR;
    UScriptCode script = uscript_getScript(ucs4, &err);
    // If script is invalid, common or inherited or there's an error,
    // infer a script based on the unicode block of a character.
    if (script <= USCRIPT_INHERITED || U_FAILURE(err))
        script = getScriptBasedOnUnicodeBlock(ucs4);
    return script;
}

struct FontMap {
    UScriptCode script;
    const char* family;
};

typedef const char* ScriptToFontMap[USCRIPT_CODE_LIMIT];    
static const char* getFontFamilyForScript(UScriptCode script)
{
    static ScriptToFontMap scriptFontMap;
    static bool initialized = false;
    if (!initialized) {
        const static FontMap fontMap[] = {
            {USCRIPT_LATIN, "times new roman"},
            {USCRIPT_GREEK, "times new roman"},
            {USCRIPT_CYRILLIC, "times new roman"},
            {USCRIPT_HAN, "microsoft yahei"},
            {USCRIPT_SIMPLIFIED_HAN, "microsoft yahei"},
            {USCRIPT_HIRAGANA, "microsoft yahei"},
            {USCRIPT_KATAKANA, "microsoft yahei"},
            {USCRIPT_KATAKANA_OR_HIRAGANA, "microsoft yahei"},
            {USCRIPT_HANGUL, "gulim"},
            {USCRIPT_THAI, "tahoma"},
            {USCRIPT_HEBREW, "david"},
            {USCRIPT_ARABIC, "tahoma"},
            {USCRIPT_DEVANAGARI, "mangal"},
            {USCRIPT_BENGALI, "vrinda"},
            {USCRIPT_GURMUKHI, "raavi"},
            {USCRIPT_GUJARATI, "shruti"},
            {USCRIPT_ORIYA, "kalinga"},
            {USCRIPT_TAMIL, "latha"},
            {USCRIPT_TELUGU, "gautami"},
            {USCRIPT_KANNADA, "tunga"},
            {USCRIPT_MALAYALAM, "kartika"},
            {USCRIPT_LAO, "dokchampa"},
            {USCRIPT_TIBETAN, "microsoft himalaya"},
            {USCRIPT_GEORGIAN, "sylfaen"},
            {USCRIPT_ARMENIAN, "sylfaen"},
            {USCRIPT_ETHIOPIC, "nyala"},
            {USCRIPT_CANADIAN_ABORIGINAL, "euphemia"},
            {USCRIPT_CHEROKEE, "plantagenet cherokee"},
            {USCRIPT_YI, "microsoft yi balti"},
            {USCRIPT_SINHALA, "iskoola pota"},
            {USCRIPT_SYRIAC, "estrangelo edessa"},
            {USCRIPT_KHMER, "daunpenh"},
            {USCRIPT_THAANA, "mv boli"},
            {USCRIPT_MONGOLIAN, "mongolian balti"},
            {USCRIPT_MYANMAR, "padauk"},
            // For USCRIPT_COMMON, we map blocks to scripts when
            // that makes sense.
        };
        
        for (int i = 0; i < sizeof(fontMap) / sizeof(fontMap[0]); ++i) {
            const char *family = fontMap[i].family;
            if (!isFontInstalled(family)) {
                switch(fontMap[i].script) {
                case USCRIPT_HAN://?
                case USCRIPT_SIMPLIFIED_HAN:
                case USCRIPT_HIRAGANA:
                case USCRIPT_KATAKANA:
                case USCRIPT_KATAKANA_OR_HIRAGANA:
                    family = getDefaultCJKFont();
                    break;  
                case USCRIPT_HANGUL:
                    family = getDefaultKoreanFont();
                    break;  
                case USCRIPT_ARABIC:
                    family = getDefaultArabicFont();
                    break;  
                case USCRIPT_HEBREW:
                    family = getDefaultHebrewFont();
                    break;  
                default:
                    family = getDefaultCommonFont();
                    break;  
                }
            }
            scriptFontMap[fontMap[i].script] = family;
        }
/* frozen till 4.0 everywhere
        // Initialize the locale-dependent mapping.
        // Since Chrome synchronizes the ICU default locale with its UI locale,
        // this ICU locale tells the current UI locale of Chrome.
        Locale locale = Locale::getDefault();
        const UChar* localeFamily = 0;
        if (locale == Locale::getJapanese())
            localeFamily = scriptFontMap[USCRIPT_HIRAGANA];
        else if (locale == Locale::getKorean())
            localeFamily = scriptFontMap[USCRIPT_HANGUL];
        else {
            // Use Simplified Chinese font for all other locales including
            // Traditional Chinese because Simsun (SC font) has a wider
            // coverage (covering both SC and TC) than PMingLiu (TC font).
            // Note that |fontMap| does not have a separate entry for
            // USCRIPT_TRADITIONAL_HAN for that reason.
            // This also speeds up the TC version of Chrome when rendering SC
            // pages.
            localeFamily = scriptFontMap[USCRIPT_SIMPLIFIED_HAN];
        }
        if (localeFamily)
            scriptFontMap[USCRIPT_HAN] = localeFamily;
*/
    }
    if (script == USCRIPT_INVALID_CODE)
        return 0;
    ASSERT(script < USCRIPT_CODE_LIMIT);
    printf("Family: %s\n", scriptFontMap[script]);
    return scriptFontMap[script];
}
#endif //USE(ICU_UNICODE)

const char *ChromiumBridge::getSerifFontFamily()
{
    static const char* fontName = NULL;
    if (!fontName) {
        static const char* fonts[] = {
            "times new roman",
            //android
            "droidserif",
            //unix
            "freeserif",
            "serif",
        };
        fontName = getInstalledFont(fonts, sizeof(fonts)/sizeof(*fonts));
        if (!fontName) {
            fontName = getDefaultCommonFont();
        }
    }
    return fontName;
}

const char *ChromiumBridge::setFixedFontFamily()
{
    static const char* fontName = NULL;
    if (!fontName) {
        static const char* fonts[] = {
            "courier new",
            //android
            "droidsansmono",
            //unix
            "freemono",
        };
        fontName = getInstalledFont(fonts, sizeof(fonts)/sizeof(*fonts));
        if (!fontName) {
            fontName = getDefaultCommonFont();
        }
    }
    return fontName;
}

const char *ChromiumBridge::getSansSerifFontFamily()
{
    static const char* fontName = NULL;
    if (!fontName) {
        static const char* fonts[] = {
            "arial",
            "arial unicode ms",
            "lucida sans unicode",
            "microsoft sans serif",
            //android
            "droidsans",
            //unix
            "freesans",
            "sans",
        };
        fontName = getInstalledFont(fonts, sizeof(fonts)/sizeof(*fonts));
        if (!fontName) {
            fontName = getDefaultCommonFont();
        }
    }
    return fontName;
}


const char *ChromiumBridge::getStandardFontFamily()
{
    return getSansSerifFontFamily();
}

const char *ChromiumBridge::getCursiveFontFamily()
{
    static const char* fontName = NULL;
    if (!fontName) {
        static const char* fonts[] = {
            "comic sans ms",
        };
        fontName = getInstalledFont(fonts, sizeof(fonts)/sizeof(*fonts));
        if (!fontName) {
            fontName = getDefaultCommonFont();
        }
    }
    return fontName;
}

const char *ChromiumBridge::getFantasyFontFamily()
{
    return getCursiveFontFamily();    
}

String ChromiumBridge::getFontFamilyForCharacters(const UChar* characters, size_t length) { 
#if !USE(ICU_UNICODE)
    return "lucida sans unicode";
#else //USE(ICU_UNICODE)
    ASSERT(characters && characters[0] && length > 0);
    UScriptCode script = USCRIPT_COMMON;

    // Sometimes characters common to script (e.g. space) is at
    // the beginning of a string so that we need to skip them
    // to get a font required to render the string.
    int i = 0;
    UChar32 ucs4 = 0;
    while (i < length && script == USCRIPT_COMMON) {
        U16_NEXT(characters, i, length, ucs4);
        script = getScript(ucs4);
    }

    // For the full-width ASCII characters (U+FF00 - U+FF5E), use the font for
    // Han (determined in a locale-dependent way above). Full-width ASCII
    // characters are rather widely used in Japanese and Chinese documents and
    // they're fully covered by Chinese, Japanese and Korean fonts.
    if (0xFF00 < ucs4 && ucs4 < 0xFF5F)
        script = USCRIPT_HAN;

    if (script == USCRIPT_COMMON)
        script = getScriptBasedOnUnicodeBlock(ucs4);

    // Another lame work-around to cover non-BMP characters.
    const char* family = getFontFamilyForScript(script);
    if (!family) {
        int plane = ucs4 >> 16;
        switch (plane) {
        case 1:
            family = "code2001";
            break;
        case 2:
            family = "simsun-extb";
            break;
        default:
            family = "lucida sans unicode";
        }
    }
    return family; 
#endif //USE(ICU_UNICODE)
#endif //SKIA
void ChromiumBridge::plugins(bool refresh, Vector<PluginInfo>*)
{

}

} // namespace WebCore
