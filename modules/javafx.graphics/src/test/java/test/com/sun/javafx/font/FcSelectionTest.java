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

package test.com.sun.javafx.font;

import com.sun.javafx.font.FcSelection;
import com.sun.javafx.font.FontConfigManager;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The selection rules of {@code fontpath_linux.c}'s {@code getFontConfig} (commit {@code 7b43255b30}, lines
 * 403-525) as {@link FcSelection} reproduces them, driven with fake sorted patterns on every operating system:
 * the branches the fontconfig installation of the golden machine cannot reach (a non-TrueType font at sort
 * index 10, the 254-font cap, a counted font without a family name, an empty sort) alongside the ones it can.
 */
public class FcSelectionTest {

    private static final int MANY = 300;

    /**
     * A sorted pattern. {@code adds} is what {@code FcCharSetSubtractCount(charset, union)} reports for it; the
     * pattern is its own charset handle. Every read is counted.
     */
    static final class Fake implements FcSelection.SortedFont<Object> {

        final String label;
        final int adds;
        byte[] format = ascii("TrueType");
        boolean noCharSet;
        String file;
        String family;
        String style = "Regular";
        String fullName;
        int formatReads;
        int charSetReads;
        int stringReads;

        Fake(String label, int adds) {
            this.label = label;
            this.adds = adds;
            this.file = "/fonts/" + label + ".ttf";
            this.family = label + " family";
            this.fullName = label + " full";
        }

        Fake format(String text) {
            format = text == null ? null : ascii(text);
            return this;
        }

        Fake withoutCharSet() {
            noCharSet = true;
            return this;
        }

        Fake family(String text) {
            family = text;
            return this;
        }

        Fake style(String text) {
            style = text;
            return this;
        }

        Fake fullName(String text) {
            fullName = text;
            return this;
        }

        Fake file(String text) {
            file = text;
            return this;
        }

        @Override
        public byte[] fontFormat() {
            formatReads++;
            return format;
        }

        @Override
        public Object charSet() {
            charSetReads++;
            return noCharSet ? null : this;
        }

        @Override
        public String file() {
            stringReads++;
            return file;
        }

        @Override
        public String family() {
            stringReads++;
            return family;
        }

        @Override
        public String style() {
            stringReads++;
            return style;
        }

        @Override
        public String fullName() {
            stringReads++;
            return fullName;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Charset arithmetic that records every operation with its argument order; a union is a new handle. */
    static final class Sets implements FcSelection.CharSets<Object> {

        final List<String> log = new ArrayList<>();

        @Override
        public int subtractCount(Object a, Object b) {
            log.add("count(" + a + "," + b + ")");
            return ((Fake) a).adds;
        }

        @Override
        public Object union(Object a, Object b) {
            String union = "union(" + a + "," + b + ")";
            log.add(union);
            return union;
        }
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.US_ASCII);
    }

    private static Fake font(String label, int adds) {
        return new Fake(label, adds);
    }

    private static List<Fake> fonts(Fake... fonts) {
        return Arrays.asList(fonts);
    }

    private static FontConfigManager.FcCompFont target() {
        FontConfigManager.FcCompFont target = new FontConfigManager.FcCompFont();
        target.fcName = "sans:regular:roman";
        target.fcFamily = "sans";
        return target;
    }

    private static List<String> families(FontConfigManager.FontConfigFont[] fonts) {
        List<String> families = new ArrayList<>();
        for (FontConfigManager.FontConfigFont font : fonts) {
            families.add(font == null ? null : font.familyName);
        }
        return families;
    }

    private static List<Fake> manyFonts(int count) {
        List<Fake> fonts = new ArrayList<>();
        for (int j = 0; j < count; j++) {
            fonts.add(font("f" + j, 100));
        }
        return fonts;
    }

    @Test
    public void emptySortWithFallbacksGivesAZeroLengthArray() {
        FontConfigManager.FcCompFont target = target();
        Sets sets = new Sets();
        assertTrue(FcSelection.select(fonts(), sets, true, target));
        assertNotNull(target.allFonts);
        assertEquals(0, target.allFonts.length);
        assertNull(target.firstFont);
        assertEquals(List.of(), sets.log);
    }

    @Test
    public void emptySortWithoutFallbacksAssignsNothing() {
        FontConfigManager.FcCompFont target = target();
        assertTrue(FcSelection.select(fonts(), new Sets(), false, target));
        assertNull(target.allFonts);
        assertNull(target.firstFont);
    }

    @Test
    public void firstSurvivingFontIsTakenWithoutCounting() {
        Fake f0 = font("f0", 0);
        FontConfigManager.FcCompFont target = target();
        Sets sets = new Sets();
        assertTrue(FcSelection.select(fonts(f0), sets, true, target));
        assertEquals(List.of(), sets.log);
        assertEquals(1, target.allFonts.length);
        assertSame(target.allFonts[0], target.firstFont);
        assertEquals("f0 family", target.firstFont.familyName);
        assertEquals("Regular", target.firstFont.styleStr);
        assertEquals("f0 full", target.firstFont.fullName);
        assertEquals("/fonts/f0.ttf", target.firstFont.fontFile);
        assertEquals(4, f0.stringReads);
    }

    @Test
    public void onlyTrueTypeCffAndFormatlessPatternsPassTheFilter() {
        Fake type1 = font("type1", 100).format("Type 1");
        Fake pcf = font("pcf", 100).format("PCF");
        Fake formatless = font("formatless", 100).format(null);
        Fake cff = font("cff", 100).format("CFF");
        Fake trueType = font("truetype", 100);
        Fake lowerCase = font("lower", 100).format("truetype");
        FontConfigManager.FcCompFont target = target();
        assertTrue(FcSelection.select(fonts(type1, pcf, formatless, cff, trueType, lowerCase), new Sets(), true,
                                      target));
        assertEquals(List.of("formatless family", "cff family", "truetype family"), families(target.allFonts));
        assertEquals(0, type1.charSetReads + pcf.charSetReads + lowerCase.charSetReads);
        assertEquals(0, type1.stringReads + pcf.stringReads + lowerCase.stringReads);
    }

    @Test
    public void laterFontsNeedMoreThanTwentyNewGlyphs() {
        Fake f0 = font("f0", 0);
        Fake f1 = font("f1", FcSelection.MIN_GLYPHS);
        Fake f2 = font("f2", FcSelection.MIN_GLYPHS + 1);
        FontConfigManager.FcCompFont target = target();
        Sets sets = new Sets();
        assertTrue(FcSelection.select(fonts(f0, f1, f2), sets, true, target));
        assertEquals(List.of("f0 family", "f2 family"), families(target.allFonts));
        assertEquals(List.of("count(f1,f0)", "count(f2,f0)", "union(f0,f2)"), sets.log);
        assertEquals(0, f1.stringReads);
    }

    @Test
    public void theGlyphCountIsUnsigned() {
        Fake f0 = font("f0", 0);
        Fake f1 = font("f1", -1);
        Fake f2 = font("f2", Integer.MIN_VALUE);
        FontConfigManager.FcCompFont target = target();
        assertTrue(FcSelection.select(fonts(f0, f1, f2), new Sets(), true, target));
        assertEquals(List.of("f0 family", "f1 family", "f2 family"), families(target.allFonts));
    }

    @Test
    public void sortIndexTenRaisesTheThresholdToFifty() {
        List<Fake> sorted = new ArrayList<>();
        for (int j = 0; j < 12; j++) {
            sorted.add(font("f" + j, j == 11 ? FcSelection.MIN_GLYPHS_FROM_INDEX_TEN + 1 : 30));
        }
        FontConfigManager.FcCompFont target = target();
        Sets sets = new Sets();
        assertTrue(FcSelection.select(sorted, sets, true, target));
        List<String> expected = new ArrayList<>();
        for (int j = 0; j < 10; j++) {
            expected.add("f" + j + " family");
        }
        expected.add("f11 family");
        assertEquals(expected, families(target.allFonts));
        assertTrue(sets.log.stream().anyMatch(entry -> entry.startsWith("count(f10,")),
                   "f10 was compared: " + sets.log);
        assertEquals(0, sorted.get(10).stringReads);
    }

    @Test
    public void aNonTrueTypeFontAtIndexTenLeavesTheThresholdAtTwenty() {
        List<Fake> sorted = new ArrayList<>();
        for (int j = 0; j < 12; j++) {
            sorted.add(font("f" + j, 30));
        }
        sorted.get(10).format("Type 1");
        FontConfigManager.FcCompFont target = target();
        assertTrue(FcSelection.select(sorted, new Sets(), true, target));
        assertEquals(11, target.allFonts.length);
        assertEquals("f11 family", target.allFonts[10].familyName);
        assertEquals(0, sorted.get(10).charSetReads);
    }

    @Test
    public void aRejectedFontAtIndexTenStillRaisesTheThreshold() {
        List<Fake> sorted = new ArrayList<>();
        for (int j = 0; j < 13; j++) {
            sorted.add(font("f" + j, 30));
        }
        sorted.set(10, font("f10", 5));
        sorted.set(12, font("f12", 51));
        FontConfigManager.FcCompFont target = target();
        assertTrue(FcSelection.select(sorted, new Sets(), true, target));
        assertEquals(11, target.allFonts.length);
        assertEquals("f9 family", target.allFonts[9].familyName);
        assertEquals("f12 family", target.allFonts[10].familyName);
    }

    @Test
    public void aPatternWithoutCharsetReturnsFalseAndLeavesTheTargetUntouched() {
        Fake f0 = font("f0", 0);
        Fake f1 = font("f1", 100).withoutCharSet();
        Fake f2 = font("f2", 100);
        FontConfigManager.FcCompFont target = target();
        FontConfigManager.FontConfigFont presetFirst = new FontConfigManager.FontConfigFont();
        FontConfigManager.FontConfigFont[] presetAll = new FontConfigManager.FontConfigFont[0];
        target.firstFont = presetFirst;
        target.allFonts = presetAll;
        Sets sets = new Sets();
        assertFalse(FcSelection.select(fonts(f0, f1, f2), sets, true, target));
        assertSame(presetFirst, target.firstFont);
        assertSame(presetAll, target.allFonts);
        assertEquals(List.of(), sets.log);
        assertEquals(4, f0.stringReads);
        assertEquals(0, f2.formatReads);
    }

    @Test
    public void aFormatSkippedPatternWithoutCharsetIsNotAnError() {
        Fake f0 = font("f0", 0);
        Fake f1 = font("f1", 100).format("Type 1").withoutCharSet();
        Fake f2 = font("f2", 100);
        FontConfigManager.FcCompFont target = target();
        assertTrue(FcSelection.select(fonts(f0, f1, f2), new Sets(), true, target));
        assertEquals(List.of("f0 family", "f2 family"), families(target.allFonts));
    }

    @Test
    public void takesAtMostTwoHundredFiftyFourFonts() {
        List<Fake> sorted = manyFonts(MANY);
        FontConfigManager.FcCompFont target = target();
        assertTrue(FcSelection.select(sorted, new Sets(), true, target));
        assertEquals(FcSelection.MAX_FONTS, target.allFonts.length);
        assertEquals("f253 family", target.allFonts[253].familyName);
        assertEquals(0, sorted.get(254).formatReads);
        assertSame(target.allFonts[0], target.firstFont);
    }

    @Test
    public void twoHundredFiftyThreeFontsAreAllTaken() {
        List<Fake> sorted = manyFonts(FcSelection.MAX_FONTS - 1);
        FontConfigManager.FcCompFont target = target();
        assertTrue(FcSelection.select(sorted, new Sets(), true, target));
        assertEquals(FcSelection.MAX_FONTS - 1, target.allFonts.length);
    }

    @Test
    public void withoutFallbacksOnlyTheFirstTakenFontIsReadAndAllFontsIsNotAssigned() {
        Fake f0 = font("f0", 0);
        Fake f1 = font("f1", 100);
        Fake f2 = font("f2", 100);
        FontConfigManager.FcCompFont target = target();
        FontConfigManager.FontConfigFont[] presetAll = new FontConfigManager.FontConfigFont[0];
        target.allFonts = presetAll;
        Sets sets = new Sets();
        assertTrue(FcSelection.select(fonts(f0, f1, f2), sets, false, target));
        assertSame(presetAll, target.allFonts);
        assertEquals("f0 family", target.firstFont.familyName);
        assertEquals("/fonts/f0.ttf", target.firstFont.fontFile);
        assertEquals(0, f1.formatReads + f2.formatReads);
        assertEquals(List.of(), sets.log);
    }

    @Test
    public void withoutFallbacksTheFirstTakenFontNeedNotBeIndexZero() {
        Fake f0 = font("f0", 0).format("Type 1");
        Fake f1 = font("f1", 0);
        FontConfigManager.FcCompFont target = target();
        assertTrue(FcSelection.select(fonts(f0, f1), new Sets(), false, target));
        assertEquals("f1 family", target.firstFont.familyName);
        assertNull(target.allFonts);
    }

    @Test
    public void aCountedFontWithoutFamilyLeavesATrailingNull() {
        Fake f0 = font("f0", 0);
        Fake f1 = font("f1", 100).family(null);
        Fake f2 = font("f2", 100);
        FontConfigManager.FcCompFont target = target();
        assertTrue(FcSelection.select(fonts(f0, f1, f2), new Sets(), true, target));
        assertEquals(Arrays.asList("f0 family", "f2 family", null), families(target.allFonts));
        assertSame(target.allFonts[0], target.firstFont);
        assertEquals(4, f1.stringReads);
    }

    @Test
    public void aFirstCountedFontWithoutFamilyShiftsFirstFont() {
        Fake f0 = font("f0", 0).family(null);
        Fake f1 = font("f1", 100);
        FontConfigManager.FcCompFont target = target();
        assertTrue(FcSelection.select(fonts(f0, f1), new Sets(), true, target));
        assertEquals(Arrays.asList("f1 family", null), families(target.allFonts));
        assertSame(target.allFonts[0], target.firstFont);
    }

    @Test
    public void noFamilyAnywhereLeavesFirstFontUnset() {
        Fake f0 = font("f0", 0).family(null);
        FontConfigManager.FcCompFont target = target();
        assertTrue(FcSelection.select(fonts(f0), new Sets(), true, target));
        assertEquals(Arrays.asList((String) null), families(target.allFonts));
        assertNull(target.firstFont);
    }

    @Test
    public void withoutFallbacksACountedFontWithoutFamilyMakesNoFirstFont() {
        Fake f0 = font("f0", 0).family(null);
        Fake f1 = font("f1", 100);
        FontConfigManager.FcCompFont target = target();
        assertTrue(FcSelection.select(fonts(f0, f1), new Sets(), false, target));
        assertNull(target.firstFont);
        assertNull(target.allFonts);
        assertEquals(0, f1.stringReads);
    }

    @Test
    public void missingOptionalFieldsStayNull() {
        Fake f0 = font("f0", 0).style(null).fullName(null).file(null);
        FontConfigManager.FcCompFont target = target();
        assertTrue(FcSelection.select(fonts(f0), new Sets(), true, target));
        assertEquals("f0 family", target.firstFont.familyName);
        assertNull(target.firstFont.styleStr);
        assertNull(target.firstFont.fullName);
        assertNull(target.firstFont.fontFile);
    }

    @Test
    public void theUnionGrowsWithEveryTakenFont() {
        Fake f0 = font("f0", 0);
        Fake f1 = font("f1", 100);
        Fake f2 = font("f2", 0);
        Fake f3 = font("f3", 100);
        FontConfigManager.FcCompFont target = target();
        Sets sets = new Sets();
        assertTrue(FcSelection.select(fonts(f0, f1, f2, f3), sets, true, target));
        assertEquals(List.of("count(f1,f0)", "union(f0,f1)", "count(f2,union(f0,f1))", "count(f3,union(f0,f1))",
                             "union(union(f0,f1),f3)"), sets.log);
        assertEquals(List.of("f0 family", "f1 family", "f3 family"), families(target.allFonts));
    }

    @Test
    public void aPresetFirstFontIsKeptWhenNothingIsTaken() {
        Fake f0 = font("f0", 0).format("Type 1");
        FontConfigManager.FcCompFont target = target();
        FontConfigManager.FontConfigFont presetFirst = new FontConfigManager.FontConfigFont();
        target.firstFont = presetFirst;
        assertTrue(FcSelection.select(fonts(f0), new Sets(), true, target));
        assertEquals(0, target.allFonts.length);
        assertSame(presetFirst, target.firstFont);
    }

    @Test
    public void theConstantsAreTheOnesOfTheC() {
        assertEquals(20, FcSelection.MIN_GLYPHS);
        assertEquals(50, FcSelection.MIN_GLYPHS_FROM_INDEX_TEN);
        assertEquals(10, FcSelection.RAISE_INDEX);
        assertEquals(254, FcSelection.MAX_FONTS);
    }
}
