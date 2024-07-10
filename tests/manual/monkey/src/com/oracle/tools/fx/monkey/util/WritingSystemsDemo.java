/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.util;

/**
 * Sample text for testing writing systems support.
 * See https://en.wikipedia.org/wiki/List_of_writing_systems
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
