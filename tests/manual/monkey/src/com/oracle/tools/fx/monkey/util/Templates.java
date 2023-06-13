/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Consumer;

/**
 * Text Templates
 */
public class Templates {
    public static final String TWO_EMOJIS = "😊😇";

    public static TextSelector promptChoice(String id, Consumer<String> client) {
        return TextSelector.fromPairs(
            id,
            client,
            "null", null,
            "Short", "yo",
            "Long", "<beg-0123456789012345678901234567890123456789-|-0123456789012345678901234567890123456789-end>",
            "RTL", "العربية"
        );
    }

    public static Object[] multiLineTextPairs() {
        return new Object[] {
            "Long", "<beg-0123456789012345678901234567890123456789-|-0123456789012345678901234567890123456789-end>",
            "Short", "yo",
            "Empty", "",
            "null", null,
            "Right-to-Left", "العربية" + "העברעאיש (עברית) איז אַ סעמיטישע שפּראַך. מען שרייבט העברעאיש מיט די 22 אותיות פונעם אלף בית לשון קודש. די",
            "Writing Systems", WritingSystemsDemo.getText(),
            "Combining Chars", "Tibetan ཨོཾ་མ་ཎི་པདྨེ་ཧཱུྃ\nDouble diacritics: a\u0360b a\u0361b a\u0362b a\u035cb",
            "Failed Nav Bug", "Arabic: \u0627\u0644\u0639\u0631\u0628\u064a\u0629",
            "Wrap Index Bug", "A regular Arabic verb, كَتَبَ‎ kataba (to write).",
            "Emojis", "[🇺🇦❤️🇺🇸🦋🏁🔥\n😀😃😄😁😆😅🤣😂\n🙂🙃😉😊😇]",
            "Tabs", "0123456789012345678901234567890\n0\n\t1\n\t\t2\n\t\t\t3\n\t\t\t\t4\n0\n",
        };
    }

    public static Object[] singleLineTextPairs() {
        return new Object[] {
            "Long", "<beg-0123456789012345678901234567890123456789-|-0123456789012345678901234567890123456789-end>",
            "Short", "yo",
            "Empty", "",
            "null", null,
            "Right-to-Left", "العربية" + "העברעאיש (עברית) איז אַ סעמיטישע שפּראַך. מען שרייבט העברעאיש מיט די 22 אותיות פונעם אלף בית לשון קודש. די",
            "Tibetan", "Tibetan ཨོཾ་མ་ཎི་པདྨེ་ཧཱུྃ",
            "Double diacritics", "a\u0360b a\u0361b a\u0362b a\u035cb",
            "Failed Nav Bug", "Arabic: \u0627\u0644\u0639\u0631\u0628\u064a\u0629",
            "Wrap Index Bug", "A regular Arabic verb, كَتَبَ‎ kataba (to write).",
            "Newlines and Tabs", "1\t\n2\r3\r\n4",
            "Single Newline", "\n",
            "Single Tab", "\t",
            "Emojis", "[🇺🇦❤️🏁🇺🇸🔥🦋😀😃😄😁😆😅🤣😂🙂🙃😉😊😇]",
        };
    }
}
