/*
 * Copyright (C) 2011 Google Inc. All rights reserved.
 * Copyright (C) 2021 Apple Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"
#include "LocaleToScriptMapping.h"

#include <wtf/SortedArrayMap.h>
#include <wtf/text/WTFString.h>

namespace WebCore {

UScriptCode scriptNameToCode(StringView scriptName)
{
    // This generally maps an ISO 15924 script code to its UScriptCode, but certain families of script codes are
    // treated as a single script for assigning a per-script font in Settings. For example, "hira" is mapped to
    // USCRIPT_KATAKANA_OR_HIRAGANA instead of USCRIPT_HIRAGANA, since we want all Japanese scripts to be rendered
    // using the same font setting.
    using ScriptName = PackedLettersLiteral<uint32_t>;
    static constexpr std::pair<ScriptName, UScriptCode> scriptNameCodeList[] = {
        { "arab"_s, USCRIPT_ARABIC },
        { "armn"_s, USCRIPT_ARMENIAN },
        { "bali"_s, USCRIPT_BALINESE },
        { "batk"_s, USCRIPT_BATAK },
        { "beng"_s, USCRIPT_BENGALI },
        { "blis"_s, USCRIPT_BLISSYMBOLS },
        { "bopo"_s, USCRIPT_BOPOMOFO },
        { "brah"_s, USCRIPT_BRAHMI },
        { "brai"_s, USCRIPT_BRAILLE },
        { "bugi"_s, USCRIPT_BUGINESE },
        { "buhd"_s, USCRIPT_BUHID },
        { "cans"_s, USCRIPT_CANADIAN_ABORIGINAL },
        { "cham"_s, USCRIPT_CHAM },
        { "cher"_s, USCRIPT_CHEROKEE },
        { "cirt"_s, USCRIPT_CIRTH },
        { "copt"_s, USCRIPT_COPTIC },
        { "cprt"_s, USCRIPT_CYPRIOT },
        { "cyrl"_s, USCRIPT_CYRILLIC },
        { "cyrs"_s, USCRIPT_OLD_CHURCH_SLAVONIC_CYRILLIC },
        { "deva"_s, USCRIPT_DEVANAGARI },
        { "dsrt"_s, USCRIPT_DESERET },
        { "egyd"_s, USCRIPT_DEMOTIC_EGYPTIAN },
        { "egyh"_s, USCRIPT_HIERATIC_EGYPTIAN },
        { "egyp"_s, USCRIPT_EGYPTIAN_HIEROGLYPHS },
        { "ethi"_s, USCRIPT_ETHIOPIC },
        { "geok"_s, USCRIPT_KHUTSURI },
        { "geor"_s, USCRIPT_GEORGIAN },
        { "glag"_s, USCRIPT_GLAGOLITIC },
        { "goth"_s, USCRIPT_GOTHIC },
        { "grek"_s, USCRIPT_GREEK },
        { "gujr"_s, USCRIPT_GUJARATI },
        { "guru"_s, USCRIPT_GURMUKHI },
        { "hang"_s, USCRIPT_HANGUL },
        { "hani"_s, USCRIPT_HAN },
        { "hano"_s, USCRIPT_HANUNOO },
        { "hans"_s, USCRIPT_SIMPLIFIED_HAN },
        { "hant"_s, USCRIPT_TRADITIONAL_HAN },
        { "hebr"_s, USCRIPT_HEBREW },
        { "hira"_s, USCRIPT_KATAKANA_OR_HIRAGANA },
        { "hmng"_s, USCRIPT_PAHAWH_HMONG },
        { "hrkt"_s, USCRIPT_KATAKANA_OR_HIRAGANA },
        { "hung"_s, USCRIPT_OLD_HUNGARIAN },
        { "inds"_s, USCRIPT_HARAPPAN_INDUS },
        { "ital"_s, USCRIPT_OLD_ITALIC },
        { "java"_s, USCRIPT_JAVANESE },
        { "jpan"_s, USCRIPT_KATAKANA_OR_HIRAGANA },
        { "kali"_s, USCRIPT_KAYAH_LI },
        { "kana"_s, USCRIPT_KATAKANA_OR_HIRAGANA },
        { "khar"_s, USCRIPT_KHAROSHTHI },
        { "khmr"_s, USCRIPT_KHMER },
        { "knda"_s, USCRIPT_KANNADA },
        { "kore"_s, USCRIPT_HANGUL },
        { "laoo"_s, USCRIPT_LAO },
        { "latf"_s, USCRIPT_LATIN_FRAKTUR },
        { "latg"_s, USCRIPT_LATIN_GAELIC },
        { "latn"_s, USCRIPT_LATIN },
        { "lepc"_s, USCRIPT_LEPCHA },
        { "limb"_s, USCRIPT_LIMBU },
        { "lina"_s, USCRIPT_LINEAR_A },
        { "linb"_s, USCRIPT_LINEAR_B },
        { "mand"_s, USCRIPT_MANDAEAN },
        { "maya"_s, USCRIPT_MAYAN_HIEROGLYPHS },
        { "mero"_s, USCRIPT_MEROITIC },
        { "mlym"_s, USCRIPT_MALAYALAM },
        { "mong"_s, USCRIPT_MONGOLIAN },
        { "mymr"_s, USCRIPT_MYANMAR },
        { "nkoo"_s, USCRIPT_NKO },
        { "ogam"_s, USCRIPT_OGHAM },
        { "orkh"_s, USCRIPT_ORKHON },
        { "orya"_s, USCRIPT_ORIYA },
        { "osma"_s, USCRIPT_OSMANYA },
        { "perm"_s, USCRIPT_OLD_PERMIC },
        { "phag"_s, USCRIPT_PHAGS_PA },
        { "phnx"_s, USCRIPT_PHOENICIAN },
        { "plrd"_s, USCRIPT_PHONETIC_POLLARD },
        { "qaai"_s, USCRIPT_INHERITED },
        { "roro"_s, USCRIPT_RONGORONGO },
        { "runr"_s, USCRIPT_RUNIC },
        { "sara"_s, USCRIPT_SARATI },
        { "shaw"_s, USCRIPT_SHAVIAN },
        { "sinh"_s, USCRIPT_SINHALA },
        { "sylo"_s, USCRIPT_SYLOTI_NAGRI },
        { "syrc"_s, USCRIPT_SYRIAC },
        { "syre"_s, USCRIPT_ESTRANGELO_SYRIAC },
        { "syrj"_s, USCRIPT_WESTERN_SYRIAC },
        { "syrn"_s, USCRIPT_EASTERN_SYRIAC },
        { "tagb"_s, USCRIPT_TAGBANWA },
        { "tale"_s, USCRIPT_TAI_LE },
        { "talu"_s, USCRIPT_NEW_TAI_LUE },
        { "taml"_s, USCRIPT_TAMIL },
        { "telu"_s, USCRIPT_TELUGU },
        { "teng"_s, USCRIPT_TENGWAR },
        { "tfng"_s, USCRIPT_TIFINAGH },
        { "tglg"_s, USCRIPT_TAGALOG },
        { "thaa"_s, USCRIPT_THAANA },
        { "thai"_s, USCRIPT_THAI },
        { "tibt"_s, USCRIPT_TIBETAN },
        { "ugar"_s, USCRIPT_UGARITIC },
        { "vaii"_s, USCRIPT_VAI },
        { "visp"_s, USCRIPT_VISIBLE_SPEECH },
        { "xpeo"_s, USCRIPT_OLD_PERSIAN },
        { "xsux"_s, USCRIPT_CUNEIFORM },
        { "yiii"_s, USCRIPT_YI },
        { "zxxx"_s, USCRIPT_UNWRITTEN_LANGUAGES },
        { "zyyy"_s, USCRIPT_COMMON },
        { "zzzz"_s, USCRIPT_UNKNOWN },
    };
    static_assert(ScriptName("arab"_s).value() == 0x61726162U);
    static_assert(ScriptName("zzzz"_s).value() == 0x7a7a7a7aU);
    static constexpr SortedArrayMap map { scriptNameCodeList };
    return map.get(scriptName, USCRIPT_INVALID_CODE);
}

UScriptCode localeToScriptCodeForFontSelection(const String& locale)
{
    using LocaleName = PackedASCIILowerCodes<uint64_t>;
    static constexpr std::pair<LocaleName, UScriptCode> localeScriptList[] = {
        { "aa"_s, USCRIPT_LATIN },
        { "ab"_s, USCRIPT_CYRILLIC },
        { "ady"_s, USCRIPT_CYRILLIC },
        { "af"_s, USCRIPT_LATIN },
        { "ak"_s, USCRIPT_LATIN },
        { "am"_s, USCRIPT_ETHIOPIC },
        { "ar"_s, USCRIPT_ARABIC },
        { "as"_s, USCRIPT_BENGALI },
        { "ast"_s, USCRIPT_LATIN },
        { "av"_s, USCRIPT_CYRILLIC },
        { "ay"_s, USCRIPT_LATIN },
        { "az"_s, USCRIPT_LATIN },
        { "ba"_s, USCRIPT_CYRILLIC },
        { "be"_s, USCRIPT_CYRILLIC },
        { "bg"_s, USCRIPT_CYRILLIC },
        { "bi"_s, USCRIPT_LATIN },
        { "bn"_s, USCRIPT_BENGALI },
        { "bo"_s, USCRIPT_TIBETAN },
        { "bs"_s, USCRIPT_LATIN },
        { "ca"_s, USCRIPT_LATIN },
        { "ce"_s, USCRIPT_CYRILLIC },
        { "ceb"_s, USCRIPT_LATIN },
        { "ch"_s, USCRIPT_LATIN },
        { "chk"_s, USCRIPT_LATIN },
        { "cs"_s, USCRIPT_LATIN },
        { "cy"_s, USCRIPT_LATIN },
        { "da"_s, USCRIPT_LATIN },
        { "de"_s, USCRIPT_LATIN },
        { "dv"_s, USCRIPT_THAANA },
        { "dz"_s, USCRIPT_TIBETAN },
        { "ee"_s, USCRIPT_LATIN },
        { "efi"_s, USCRIPT_LATIN },
        { "el"_s, USCRIPT_GREEK },
        { "en"_s, USCRIPT_LATIN },
        { "es"_s, USCRIPT_LATIN },
        { "et"_s, USCRIPT_LATIN },
        { "eu"_s, USCRIPT_LATIN },
        { "fa"_s, USCRIPT_ARABIC },
        { "fi"_s, USCRIPT_LATIN },
        { "fil"_s, USCRIPT_LATIN },
        { "fj"_s, USCRIPT_LATIN },
        { "fo"_s, USCRIPT_LATIN },
        { "fr"_s, USCRIPT_LATIN },
        { "fur"_s, USCRIPT_LATIN },
        { "fy"_s, USCRIPT_LATIN },
        { "ga"_s, USCRIPT_LATIN },
        { "gaa"_s, USCRIPT_LATIN },
        { "gd"_s, USCRIPT_LATIN },
        { "gil"_s, USCRIPT_LATIN },
        { "gl"_s, USCRIPT_LATIN },
        { "gn"_s, USCRIPT_LATIN },
        { "gsw"_s, USCRIPT_LATIN },
        { "gu"_s, USCRIPT_GUJARATI },
        { "ha"_s, USCRIPT_LATIN },
        { "haw"_s, USCRIPT_LATIN },
        { "he"_s, USCRIPT_HEBREW },
        { "hi"_s, USCRIPT_DEVANAGARI },
        { "hil"_s, USCRIPT_LATIN },
        { "ho"_s, USCRIPT_LATIN },
        { "hr"_s, USCRIPT_LATIN },
        { "ht"_s, USCRIPT_LATIN },
        { "hu"_s, USCRIPT_LATIN },
        { "hy"_s, USCRIPT_ARMENIAN },
        { "id"_s, USCRIPT_LATIN },
        { "ig"_s, USCRIPT_LATIN },
        { "ii"_s, USCRIPT_YI },
        { "ilo"_s, USCRIPT_LATIN },
        { "inh"_s, USCRIPT_CYRILLIC },
        { "is"_s, USCRIPT_LATIN },
        { "it"_s, USCRIPT_LATIN },
        { "iu"_s, USCRIPT_CANADIAN_ABORIGINAL },
        { "ja"_s, USCRIPT_KATAKANA_OR_HIRAGANA },
        { "jv"_s, USCRIPT_LATIN },
        { "ka"_s, USCRIPT_GEORGIAN },
        { "kaj"_s, USCRIPT_LATIN },
        { "kam"_s, USCRIPT_LATIN },
        { "kbd"_s, USCRIPT_CYRILLIC },
        { "kha"_s, USCRIPT_LATIN },
        { "kk"_s, USCRIPT_CYRILLIC },
        { "kl"_s, USCRIPT_LATIN },
        { "km"_s, USCRIPT_KHMER },
        { "kn"_s, USCRIPT_KANNADA },
        { "ko"_s, USCRIPT_HANGUL },
        { "kok"_s, USCRIPT_DEVANAGARI },
        { "kos"_s, USCRIPT_LATIN },
        { "kpe"_s, USCRIPT_LATIN },
        { "krc"_s, USCRIPT_CYRILLIC },
        { "ks"_s, USCRIPT_ARABIC },
        { "ku"_s, USCRIPT_ARABIC },
        { "kum"_s, USCRIPT_CYRILLIC },
        { "ky"_s, USCRIPT_CYRILLIC },
        { "la"_s, USCRIPT_LATIN },
        { "lah"_s, USCRIPT_ARABIC },
        { "lb"_s, USCRIPT_LATIN },
        { "lez"_s, USCRIPT_CYRILLIC },
        { "ln"_s, USCRIPT_LATIN },
        { "lo"_s, USCRIPT_LAO },
        { "lt"_s, USCRIPT_LATIN },
        { "lv"_s, USCRIPT_LATIN },
        { "mai"_s, USCRIPT_DEVANAGARI },
        { "mdf"_s, USCRIPT_CYRILLIC },
        { "mg"_s, USCRIPT_LATIN },
        { "mh"_s, USCRIPT_LATIN },
        { "mi"_s, USCRIPT_LATIN },
        { "mk"_s, USCRIPT_CYRILLIC },
        { "ml"_s, USCRIPT_MALAYALAM },
        { "mn"_s, USCRIPT_CYRILLIC },
        { "mr"_s, USCRIPT_DEVANAGARI },
        { "ms"_s, USCRIPT_LATIN },
        { "mt"_s, USCRIPT_LATIN },
        { "my"_s, USCRIPT_MYANMAR },
        { "myv"_s, USCRIPT_CYRILLIC },
        { "na"_s, USCRIPT_LATIN },
        { "nb"_s, USCRIPT_LATIN },
        { "ne"_s, USCRIPT_DEVANAGARI },
        { "niu"_s, USCRIPT_LATIN },
        { "nl"_s, USCRIPT_LATIN },
        { "nn"_s, USCRIPT_LATIN },
        { "nr"_s, USCRIPT_LATIN },
        { "nso"_s, USCRIPT_LATIN },
        { "ny"_s, USCRIPT_LATIN },
        { "oc"_s, USCRIPT_LATIN },
        { "om"_s, USCRIPT_LATIN },
        { "or"_s, USCRIPT_ORIYA },
        { "os"_s, USCRIPT_CYRILLIC },
        { "pa"_s, USCRIPT_GURMUKHI },
        { "pag"_s, USCRIPT_LATIN },
        { "pap"_s, USCRIPT_LATIN },
        { "pau"_s, USCRIPT_LATIN },
        { "pl"_s, USCRIPT_LATIN },
        { "pon"_s, USCRIPT_LATIN },
        { "ps"_s, USCRIPT_ARABIC },
        { "pt"_s, USCRIPT_LATIN },
        { "qu"_s, USCRIPT_LATIN },
        { "rm"_s, USCRIPT_LATIN },
        { "rn"_s, USCRIPT_LATIN },
        { "ro"_s, USCRIPT_LATIN },
        { "ru"_s, USCRIPT_CYRILLIC },
        { "rw"_s, USCRIPT_LATIN },
        { "sa"_s, USCRIPT_DEVANAGARI },
        { "sah"_s, USCRIPT_CYRILLIC },
        { "sat"_s, USCRIPT_LATIN },
        { "sd"_s, USCRIPT_ARABIC },
        { "se"_s, USCRIPT_LATIN },
        { "sg"_s, USCRIPT_LATIN },
        { "si"_s, USCRIPT_SINHALA },
        { "sid"_s, USCRIPT_LATIN },
        { "sk"_s, USCRIPT_LATIN },
        { "sl"_s, USCRIPT_LATIN },
        { "sm"_s, USCRIPT_LATIN },
        { "so"_s, USCRIPT_LATIN },
        { "sq"_s, USCRIPT_LATIN },
        { "sr"_s, USCRIPT_CYRILLIC },
        { "ss"_s, USCRIPT_LATIN },
        { "st"_s, USCRIPT_LATIN },
        { "su"_s, USCRIPT_LATIN },
        { "sv"_s, USCRIPT_LATIN },
        { "sw"_s, USCRIPT_LATIN },
        { "ta"_s, USCRIPT_TAMIL },
        { "te"_s, USCRIPT_TELUGU },
        { "tet"_s, USCRIPT_LATIN },
        { "tg"_s, USCRIPT_CYRILLIC },
        { "th"_s, USCRIPT_THAI },
        { "ti"_s, USCRIPT_ETHIOPIC },
        { "tig"_s, USCRIPT_ETHIOPIC },
        { "tk"_s, USCRIPT_LATIN },
        { "tkl"_s, USCRIPT_LATIN },
        { "tl"_s, USCRIPT_LATIN },
        { "tn"_s, USCRIPT_LATIN },
        { "to"_s, USCRIPT_LATIN },
        { "tpi"_s, USCRIPT_LATIN },
        { "tr"_s, USCRIPT_LATIN },
        { "trv"_s, USCRIPT_LATIN },
        { "ts"_s, USCRIPT_LATIN },
        { "tt"_s, USCRIPT_CYRILLIC },
        { "tvl"_s, USCRIPT_LATIN },
        { "tw"_s, USCRIPT_LATIN },
        { "ty"_s, USCRIPT_LATIN },
        { "tyv"_s, USCRIPT_CYRILLIC },
        { "udm"_s, USCRIPT_CYRILLIC },
        { "ug"_s, USCRIPT_ARABIC },
        { "uk"_s, USCRIPT_CYRILLIC },
        { "und"_s, USCRIPT_LATIN },
        { "ur"_s, USCRIPT_ARABIC },
        { "uz"_s, USCRIPT_CYRILLIC },
        { "ve"_s, USCRIPT_LATIN },
        { "vi"_s, USCRIPT_LATIN },
        { "wal"_s, USCRIPT_ETHIOPIC },
        { "war"_s, USCRIPT_LATIN },
        { "wo"_s, USCRIPT_LATIN },
        { "xh"_s, USCRIPT_LATIN },
        { "yap"_s, USCRIPT_LATIN },
        { "yo"_s, USCRIPT_LATIN },
        { "za"_s, USCRIPT_LATIN },
        { "zh"_s, USCRIPT_HAN },
        { "zh_hk"_s, USCRIPT_TRADITIONAL_HAN },
        { "zh_tw"_s, USCRIPT_TRADITIONAL_HAN },
        { "zu"_s, USCRIPT_LATIN },
    };
    static_assert(LocaleName("aa"_s).value() == 0x6161000000000000ULL);
    static_assert(LocaleName("zh_tw"_s).value() == 0x7a685f7477000000ULL);
    static constexpr SortedArrayMap map { localeScriptList };
    String canonicalLocaleString = makeStringByReplacingAll(locale, '-', '_');
    for (StringView canonicalLocale = canonicalLocaleString; !canonicalLocale.isEmpty(); ) {
        if (auto scriptCode = map.tryGet(canonicalLocale))
            return *scriptCode;
        auto underscorePosition = canonicalLocale.reverseFind('_');
        if (underscorePosition == notFound)
            break;
        UScriptCode code = scriptNameToCode(canonicalLocale.substring(underscorePosition + 1));
        if (code != USCRIPT_INVALID_CODE && code != USCRIPT_UNKNOWN)
            return code;
        canonicalLocale = canonicalLocale.left(underscorePosition);
    }
    return USCRIPT_COMMON;
}

} // namespace WebCore
