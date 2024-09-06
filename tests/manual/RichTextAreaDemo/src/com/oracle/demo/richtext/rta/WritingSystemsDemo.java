/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
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

package com.oracle.demo.richtext.rta;

/**
 * Sample text for testing writing systems support.
 * See https://en.wikipedia.org/wiki/List_of_writing_systems
 *
 * @author Andy Goryachev
 */
public class WritingSystemsDemo {
    public static final String[] PAIRS = {
        "Arabic", "العربية",
        "Aramaic", "Classical Syriac: ܐܪܡܝܐ, Old Aramaic: 𐤀𐤓𐤌𐤉𐤀; Imperial Aramaic: 𐡀𐡓𐡌𐡉𐡀; Jewish Babylonian Aramaic: אֲרָמִית‎",
        "Akkadian", "𒀝𒅗𒁺𒌑",
        "Armenian", "հայերէն/հայերեն",
        "Assamese", "অসমীয়া",
        "Awadhi", "अवधी/औधी",
        "Azerbaijanis", "آذربایجانلیلار",
        "Bagheli", "बघेली",
        "Bagri", "बागड़ी, باگڑی",
        "Bengali", "বাংলা",
        "Bhojpuri", "𑂦𑂷𑂔𑂣𑂳𑂩𑂲",
        "Braille", "⠃⠗⠇",
        "Bundeli", "बुन्देली",
        "Burmese", "မြန်မာ",
        "Cherokee", "ᏣᎳᎩ ᎦᏬᏂᎯᏍᏗ",
        "Chhattisgarhi", "छत्तीसगढ़ी, ଛତିଶଗଡ଼ି, ଲରିଆ",
        "Chinese", "中文",
        "Czech", "Čeština",
        "Devanagari", "देवनागरी",
        "Dhivehi", "ދިވެހި",
        "Dhundhari", "ढूण्ढाड़ी/ઢૂણ્ઢાડ઼ી",
        "Farsi", "فارسی",
        "Garhwali", "गढ़वळि",
        "Geʽez", "ግዕዝ",
        "Greek", "Ελληνικά",
        "Georgian", "ქართული",
        "Gujarati", "ગુજરાતી",
        "Harauti", "हाड़ौती, हाड़ोती",
        "Haryanvi", "हरयाणवी",
        "Hausa", "هَرْشٜن هَوْسَ",
        "Hebrew", "עברית",
        "Hindi", "हिन्दी",
        "Inuktitut", "ᐃᓄᒃᑎᑐᑦ",
        "Japanese", "日本語 かな カナ",
        "Kangri", "कांगड़ी",
        "Kannada", "ಕನ್ನಡ",
        "Kashmiri", "كٲشُرकॉशुर𑆑𑆳𑆯𑆶𑆫𑇀",
        "Khmer", "ខ្មែរ",
        "Khortha", "खोरठा",
        "Khowar", "کھووار زبان",
        "Korean", "한국어",
        "Kumaoni", "कुमाऊँनी",
        "Kurdish", "Kurdî / کوردی",
        "Magahi", "𑂧𑂏𑂯𑂲/𑂧𑂏𑂡𑂲",
        "Maithili", "मैथिली",
        "Malayalam", "മലയാളം",
        "Malvi", "माळवी भाषा / માળવી ભાષા",
        "Marathi", "मराठी",
        "Marwari,", "मारवाड़ी",
        "Meitei", "ꯃꯩꯇꯩꯂꯣꯟ",
        "Mewari", "मेवाड़ी/મેવ઼ાડ઼ી",
        "Mongolian", "ᠨᠢᠷᠤᠭᠤ",
        "Nimadi", "निमाड़ी",
        "Odia", "ଓଡ଼ିଆ",
        "Pahari", "पहाड़ी پہاڑی ",
        "Pashto", "پښتو",
        "Punjabi", "ਪੰਜਾਬੀپن٘جابی",
        "Rajasthani", "राजस्थानी",
        "Russian", "Русский",
        "Sanskrit", "संस्कृत-, संस्कृतम्",
        "Santali", "ᱥᱟᱱᱛᱟᱲᱤ",
        "Sindhi", "سِنڌِي‎ • सिन्धी",
        "Suret", "ܣܘܪܝܬ",
        "Surgujia", "सरगुजिया",
        "Surjapuri", "सुरजापुरी, সুরজাপুরী",
        "Tamil", "தமிழ்",
        "Telugu", "తెలుగు",
        "Thaana", "ދިވެހި",
        "Thai", "ไทย",
        "Tibetan", "བོད་",
        "Tulu", "ತುಳು",
        "Turoyo", "ܛܘܪܝܐ",
        "Ukrainian", "Українська",
        "Urdu", "اردو",
        "Vietnamese", "Tiếng Việt",
        "Yiddish", "ייִדיש יידיש  אידיש"
    };

    public static String getText() {
        return getText(false);
    }

    public static String getText(boolean showUnicode) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < PAIRS.length;) {
            String a = PAIRS[i++];
            String b = PAIRS[i++];
            t(sb, a, b, showUnicode);
        }
        return sb.toString();
    }

    private static void t(StringBuilder sb, String name, String text, boolean showUnicode) {
        sb.append(name);
        sb.append(": ");
        sb.append(text);
        if (showUnicode) {
            sb.append(" (");
            native2ascii(sb, text);
            sb.append(")");
        }
        sb.append("\n");
    }

    private static void native2ascii(StringBuilder sb, String text) {
        for (char c: text.toCharArray()) {
            if (c < 0x20) {
                escape(sb, c);
            } else if (c > 0x7f) {
                escape(sb, c);
            } else {
                sb.append(c);
            }
        }
    }

    private static void escape(StringBuilder sb, char c) {
        sb.append("\\u");
        sb.append(h(c >> 12));
        sb.append(h(c >> 8));
        sb.append(h(c >> 4));
        sb.append(h(c));
    }

    private static char h(int d) {
        return "0123456789abcdef".charAt(d & 0x000f);
    }
}
