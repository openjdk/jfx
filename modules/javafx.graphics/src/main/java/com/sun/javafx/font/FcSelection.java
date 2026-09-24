/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font;

import java.util.Arrays;
import java.util.List;

/**
 * The font selection of {@code Java_com_sun_javafx_font_FontConfigManager_getFontConfig} in
 * {@code fontpath_linux.c} of commit {@code 7b43255b30} (lines 403-525), as plain Java over an abstract view of an
 * {@code FcFontSort} result: which of the sorted patterns become {@code FontConfigFont}s, in which order, how the
 * {@code FcCompFont} is filled, and when the C returned {@code JNI_FALSE}. {@link FontConfigNative} supplies the
 * view over the real fontconfig objects; {@code test.com.sun.javafx.font.FcSelectionTest} drives it with fakes on
 * every operating system, which is how the branches no fontconfig installation at hand can reach (a non-TrueType
 * font at sort index 10, 254 accepted fonts, a counted font without a family name) are pinned.
 *
 * <p>The rules, in the C's order for each sorted pattern {@code j}:
 * <ol>
 * <li>a pattern whose {@code fontformat} is present and is neither {@code TrueType} nor {@code CFF} is skipped, a
 * pattern without a format passes; the comparison is {@code strcmp}, byte for byte;</li>
 * <li>a pattern without a {@code charset} ends the call with {@code JNI_FALSE}, the target untouched;</li>
 * <li>at {@code j == 10} the threshold rises from 20 to 50 glyphs, but only if index 10 itself gets this far: a
 * non-TrueType font at sort index 10 leaves the threshold at 20 for the rest of the sort;</li>
 * <li>the first pattern to get here is taken unconditionally and its charset becomes the union; every later one is
 * taken only when {@code FcCharSetSubtractCount(charset, union)}, an unsigned count, exceeds the threshold, and
 * the union then grows by {@code FcCharSetUnion(union, charset)};</li>
 * <li>a taken pattern's {@code file}, {@code family}, {@code style} and {@code fullname} are read; without
 * fallbacks the loop ends after the first taken pattern, with fallbacks after the 254th.</li>
 * </ol>
 * Then, with fallbacks, {@code allFonts} becomes a new array of exactly the number of taken patterns (zero length
 * when none was taken); a {@code FontConfigFont} is made for every taken pattern that has a family, in sort order,
 * the first one also becoming {@code firstFont} (so {@code firstFont == allFonts[0]} by identity), a missing
 * style, full name or file leaving that field {@code null}. A taken pattern without a family makes no object, so
 * the array keeps a trailing {@code null}. Without fallbacks {@code allFonts} is not assigned and only the first
 * object is made, as {@code firstFont}.
 */
public final class FcSelection {

    /** {@code minGlyphs} before sort index 10 (fontpath_linux.c:404). */
    public static final int MIN_GLYPHS = 20;

    /** {@code minGlyphs} once sort index 10 has reached the threshold check (fontpath_linux.c:442-444). */
    public static final int MIN_GLYPHS_FROM_INDEX_TEN = 50;

    /** The sort index at which the threshold rises. */
    public static final int RAISE_INDEX = 10;

    /** The most fonts taken with fallbacks: "Upstream Java code currently stores this in a byte" (:464-470). */
    public static final int MAX_FONTS = 254;

    private static final byte[] TRUE_TYPE = {'T', 'r', 'u', 'e', 'T', 'y', 'p', 'e'};

    private static final byte[] CFF = {'C', 'F', 'F'};

    private FcSelection() {
    }

    /**
     * One pattern of the {@code FcFontSort} result, read as the C reads it: each method is one
     * {@code FcPatternGetString(pattern, object, 0, &s)} (or {@code FcPatternGetCharSet}) whose value is
     * {@code null} when the result is not {@code FcResultMatch}. Nothing is read before the selection asks for it.
     *
     * @param <C> the charset handle type
     */
    public interface SortedFont<C> {

        /** The bytes of {@code fontformat} before its terminator, or {@code null} when the pattern has none. */
        byte[] fontFormat();

        /** The {@code charset}, or {@code null} when the pattern has none. */
        C charSet();

        /** {@code file} as {@code NewStringUTF} gave it to Java, or {@code null}. */
        String file();

        /** {@code family}, or {@code null}. */
        String family();

        /** {@code style}, or {@code null}. */
        String style();

        /** {@code fullname}, or {@code null}. */
        String fullName();
    }

    /**
     * The two charset operations the selection uses.
     *
     * @param <C> the charset handle type
     */
    public interface CharSets<C> {

        /**
         * {@code FcCharSetSubtractCount(a, b)}: how many code points {@code a} has that {@code b} lacks, an unsigned
         * {@code FcChar32}.
         */
        int subtractCount(C a, C b);

        /** {@code FcCharSetUnion(a, b)}: a new charset. */
        C union(C a, C b);
    }

    /**
     * Fills {@code target} from the sorted {@code fonts} as described above.
     *
     * @return {@code false} when a pattern that passed the format filter has no charset, the C's
     *         {@code JNI_FALSE}; {@code target} is then untouched
     */
    public static <C> boolean select(List<? extends SortedFont<C>> fonts, CharSets<C> charSets,
                                     boolean includeFallbacks, FontConfigManager.FcCompFont target) {
        int nfonts = fonts.size();
        String[] file = new String[nfonts];
        String[] family = new String[nfonts];
        String[] style = new String[nfonts];
        String[] fullName = new String[nfonts];
        int fontCount = 0;
        int minGlyphs = MIN_GLYPHS;
        C unionCharset = null;
        for (int j = 0; j < nfonts; j++) {
            SortedFont<C> font = fonts.get(j);
            byte[] format = font.fontFormat();
            if (format != null && !Arrays.equals(format, TRUE_TYPE) && !Arrays.equals(format, CFF)) {
                continue;
            }
            C charset = font.charSet();
            if (charset == null) {
                return false;
            }
            if (j == RAISE_INDEX) {
                minGlyphs = MIN_GLYPHS_FROM_INDEX_TEN;
            }
            if (unionCharset == null) {
                unionCharset = charset;
            } else if (Integer.compareUnsigned(charSets.subtractCount(charset, unionCharset), minGlyphs) > 0) {
                unionCharset = charSets.union(unionCharset, charset);
            } else {
                continue;
            }
            fontCount++;
            file[j] = font.file();
            family[j] = font.family();
            style[j] = font.style();
            fullName[j] = font.fullName();
            if (!includeFallbacks) {
                break;
            }
            if (fontCount == MAX_FONTS) {
                break;
            }
        }
        FontConfigManager.FontConfigFont[] all = null;
        if (includeFallbacks) {
            all = new FontConfigManager.FontConfigFont[fontCount];
            target.allFonts = all;
        }
        int fn = 0;
        for (int j = 0; j < nfonts; j++) {
            if (family[j] == null) {
                continue;
            }
            FontConfigManager.FontConfigFont fcFont = new FontConfigManager.FontConfigFont();
            fcFont.familyName = family[j];
            if (file[j] != null) {
                fcFont.fontFile = file[j];
            }
            if (style[j] != null) {
                fcFont.styleStr = style[j];
            }
            if (fullName[j] != null) {
                fcFont.fullName = fullName[j];
            }
            if (fn == 0) {
                target.firstFont = fcFont;
            }
            if (includeFallbacks && all != null) {
                all[fn++] = fcFont;
            } else {
                break;
            }
        }
        return true;
    }
}
