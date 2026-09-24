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

import com.sun.javafx.font.CompositeFontResource;
import com.sun.javafx.font.FontConfigManager;
import com.sun.javafx.font.FontConfigManagerShim;
import com.sun.javafx.font.FontFallbackInfo;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontResourceShim;
import com.sun.javafx.font.PrismFontFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.font.LinuxFontGoldens.FontMaps;
import test.com.sun.javafx.font.LinuxFontGoldens.GoldenMap;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Parity oracle for the fontconfig enumeration of the Linux font layer ({@code FontConfigManager.getFontConfig}
 * and {@code populateMapsNative}, implemented at commit {@code 7b43255b30} by {@code fontpath_linux.c} in
 * {@code libjavafx_font.so}), against a golden captured from that JNI build.
 * <p>
 * The golden {@value #GOLDEN} records the natives <em>raw</em>, with fresh arrays and maps: every font the
 * twelve logical names sort to under six locale strings with and without fallbacks (including the
 * {@code firstFont == allFonts[0]} identity), the partial fill a failing {@code FcNameParse} leaves, the three
 * maps under {@code Locale.ENGLISH} and {@code tr-TR} (dotless i) in insertion order, the {@code get}-then-{@code add}
 * behaviour on pre-seeded maps, and the null-argument returns; then what {@code FontConfigManager},
 * {@code PrismFontFactory}, {@code FTFactory.getFallbacks} and {@code LogicalFont} derive from them.
 * <p>
 * The corpus only reads fontconfig state; the cases that change it run in the child JVMs of
 * {@link LinuxFontProcessGoldenTest}. See {@link LinuxFontGoldens}.
 */
@EnabledOnOs(OS.LINUX)
public class LinuxFontConfigGoldenTest {

    static final String GOLDEN = "linux-fontconfig-golden.txt";

    private static final ParityGate.Ledger LEDGER = ParityGate.ledger(LinuxFontConfigGoldenTest.class);

    /** fcName strings of which the first that {@code FcNameParse} rejects makes the partial-fill call. */
    static final List<String> PARTIAL_FILL_CANDIDATES =
            List.of("sans:weight=notaweight", ":=", "sans:slant=xyz", String.valueOf((char) 92));

    private static final String[] VIEW_FAMILIES = {"System", "SansSerif", "Serif", "Monospaced", "NoSuchFamily"};

    private static final String[] LOGICAL_FAMILIES = {"System", "SansSerif", "Serif", "Monospaced"};

    @Test
    public void fontconfigMatchesTheJniGolden() throws Exception {
        LinuxFontGoldens.captureOrVerify(LinuxFontConfigGoldenTest.class, GOLDEN, header(), true,
                                         LinuxFontConfigGoldenTest::capture, LinuxFontConfigGoldenTest::assertCoverage);
    }

    /** The machine gate passed at least once, so at least one body key was compared ({@link ParityGate}). */
    @AfterAll
    static void theOracleRan() {
        LEDGER.assertOracleRan();
    }

    /**
     * Not vacuous, on any Linux with fontconfig and fonts: the maps are consistent with each other, every file is
     * an absolute path, every logical name resolves with {@code firstFont} as the first of {@code allFonts}, and
     * {@code HOME}, when set, is still what it was in the C environment.
     */
    @Test
    public void fontconfigEnumeratesFonts() {
        FontConfigManagerShim.ensureLoaded();
        FontMaps maps = FontMaps.populate(Locale.ENGLISH);
        assumeTrue(maps.result(), "fontconfig is not available on this machine");
        assumeTrue(!maps.fontToFile().isEmpty(), "fontconfig lists no TrueType or CFF outline fonts");
        for (Map.Entry<String, String> entry : maps.fontToFile().entrySet()) {
            assertTrue(entry.getValue().startsWith("/"), "file of " + entry.getKey() + ": " + entry.getValue());
            assertTrue(maps.fontToFamily().containsKey(entry.getKey()), "family of " + entry.getKey());
        }
        for (Map.Entry<String, ArrayList<String>> entry : maps.familyToFontList().entrySet()) {
            assertFalse(entry.getValue().isEmpty(), "fonts of family " + entry.getKey());
        }
        String[] names = FontConfigManagerShim.fontConfigNames();
        FontConfigManager.FcCompFont[] fonts = FontConfigManagerShim.newLogicalFontArray(names);
        assertTrue(FontConfigManagerShim.getFontConfig(FontConfigManagerShim.fcLocaleStr(), fonts, true),
                   "getFontConfig");
        for (FontConfigManager.FcCompFont font : fonts) {
            assertNotNull(font.firstFont, "firstFont of " + font.fcName);
            assertNotNull(font.allFonts, "allFonts of " + font.fcName);
            assertSame(font.allFonts[0], font.firstFont, "firstFont of " + font.fcName);
        }
        LinuxFontGoldens.assertLibcHomeUnchanged();
    }

    // ---------------------------------------------------------------------------------------------
    // Capture
    // ---------------------------------------------------------------------------------------------

    static void capture(GoldenMap out) {
        FontConfigManagerShim.ensureLoaded();
        String[] names = FontConfigManagerShim.fontConfigNames();
        out.put("fc.raw.names", FontGoldens.joinList(Arrays.asList(names)));
        String defaultLocale = FontConfigManagerShim.fcLocaleStr();
        out.put("fc.raw.gfc.DEFAULT.locale", LinuxFontGoldens.safeText(defaultLocale));
        String[][] locales = {
            {"DEFAULT", defaultLocale}, {"he", "he"}, {"ja-JP", "ja-JP"}, {"zh-CN", "zh-CN"}, {"tr-TR", "tr-TR"},
            {"EMPTY", ""}
        };
        for (String[] locale : locales) {
            for (boolean fallbacks : new boolean[] {true, false}) {
                FontConfigManager.FcCompFont[] fonts = FontConfigManagerShim.newLogicalFontArray(names);
                boolean result = FontConfigManagerShim.getFontConfig(locale[1], fonts, fallbacks);
                LinuxFontGoldens.putFontConfig(out, "fc.raw.gfc." + locale[0] + "." + (fallbacks ? "fb" : "nofb"),
                                               result, fonts, LinuxFontGoldens::safeText);
            }
        }

        String candidate = null;
        for (String c : PARTIAL_FILL_CANDIDATES) {
            FontConfigManager.FcCompFont[] fonts =
                    FontConfigManagerShim.newLogicalFontArray("serif:bold:roman", c, "monospace:regular:roman");
            boolean result = FontConfigManagerShim.getFontConfig(defaultLocale, fonts, true);
            if (!result) {
                candidate = c;
                out.put("fc.raw.gfc.partial.candidate", LinuxFontGoldens.safeText(c));
                LinuxFontGoldens.putFontConfig(out, "fc.raw.gfc.partial", result, fonts, LinuxFontGoldens::safeText);
                break;
            }
        }
        if (candidate == null) {
            fail("none of " + PARTIAL_FILL_CANDIDATES + " made getFontConfig return false; the partial-fill case"
                    + " has no input on this fontconfig and must not be replaced silently");
        }

        String[][] mapLocales = {{"en", "en"}, {"tr-TR", "tr-TR"}};
        for (String[] locale : mapLocales) {
            FontMaps maps = FontMaps.populate(Locale.forLanguageTag(locale[1]));
            String prefix = "fc.raw.pm." + locale[0];
            out.put(prefix + ".return", Boolean.toString(maps.result()));
            out.put(prefix + ".counts", maps.counts());
            maps.entries(LinuxFontGoldens::safeText, LinuxFontGoldens::safeText)
                .forEach((k, v) -> out.put(prefix + "." + k, v));
        }
        out.put("fc.raw.pm.nullMap.return", Boolean.toString(FontConfigManagerShim.populateMapsNative(
                null, new HashMap<>(), new HashMap<>(), Locale.ENGLISH)));
        out.put("fc.raw.pm.nullLocale.return", Boolean.toString(FontConfigManagerShim.populateMapsNative(
                new HashMap<>(), new HashMap<>(), new HashMap<>(), null)));

        HashMap<String, String> seededFiles = new HashMap<>();
        seededFiles.put("zzz seed", "/seed");
        HashMap<String, ArrayList<String>> seededLists = new HashMap<>();
        seededLists.put("dejavu sans", new ArrayList<>(List.of("#seed")));
        boolean seeded = FontConfigManagerShim.populateMapsNative(seededFiles, new HashMap<>(), seededLists,
                                                                  Locale.ENGLISH);
        out.put("fc.raw.pm.seeded.return", Boolean.toString(seeded));
        out.put("fc.raw.pm.seeded.file.zzz seed", LinuxFontGoldens.safeText(seededFiles.get("zzz seed")));
        List<String> seededList = new ArrayList<>();
        seededLists.getOrDefault("dejavu sans", new ArrayList<>()).forEach(n -> seededList.add(n));
        out.put("fc.raw.pm.seeded.list.dejavu sans", FontGoldens.joinList(seededList));

        captureViews(out);
        LinuxFontGoldens.assertLibcHomeUnchanged();
    }

    private static String styleName(boolean bold, boolean italic) {
        return bold ? (italic ? "bolditalic" : "bold") : (italic ? "italic" : "regular");
    }

    private static void captureViews(GoldenMap out) {
        for (String family : VIEW_FAMILIES) {
            for (int style = 0; style < 4; style++) {
                boolean bold = (style & 1) != 0;
                boolean italic = (style & 2) != 0;
                FontConfigManager.FcCompFont font = FontConfigManager.getFontConfigFont(family, bold, italic);
                String key = "fc.view.gfcf." + family + "." + styleName(bold, italic);
                if (font == null) {
                    out.put(key, FontGoldens.NULL);
                    continue;
                }
                out.put(key, LinuxFontGoldens.safeText(font.fcName) + "|" + LinuxFontGoldens.safeText(font.fcFamily)
                        + "|" + font.style + "|"
                        + LinuxFontGoldens.fontFields(font.firstFont, LinuxFontGoldens::safeText));
                out.put(key + ".files", texts(FontConfigManager.getFileNames(font, false)));
                out.put(key + ".names", texts(FontConfigManager.getFontNames(font, false)));
            }
        }
        out.put("fc.view.defaultFontPath", LinuxFontGoldens.safeText(FontConfigManager.getDefaultFontPath()));

        PrismFontFactory factory = PrismFontFactory.getFontFactory();
        String[] families = factory.getFontFamilyNames();
        out.put("fc.api.families", texts(Arrays.asList(families)));
        out.put("fc.api.fullNames", texts(Arrays.asList(factory.getFontFullNames())));
        for (String family : families) {
            out.put("fc.api.family." + LinuxFontGoldens.safeText(family.toLowerCase(Locale.ENGLISH)),
                    texts(Arrays.asList(factory.getFontFullNames(family))));
        }
        for (int style = 0; style < 4; style++) {
            boolean bold = (style & 1) != 0;
            boolean italic = (style & 2) != 0;
            FontResource primary = factory.getFontResource("DejaVu Sans", bold, italic, false);
            String key = "fc.api.fallbacks." + styleName(bold, italic);
            if (primary == null) {
                out.put(key, FontGoldens.NULL);
                continue;
            }
            FontFallbackInfo info = factory.getFallbacks(primary);
            out.put(key, texts(Arrays.asList(info.getFontNames())) + "|" + texts(Arrays.asList(info.getFontFiles())));
        }
        for (String family : LOGICAL_FAMILIES) {
            for (int style = 0; style < 4; style++) {
                boolean bold = (style & 1) != 0;
                boolean italic = (style & 2) != 0;
                CompositeFontResource logical = FontResourceShim.newLogicalFont(family, bold, italic);
                FontResource slot0 = logical.getSlotResource(0);
                out.put("fc.logical." + family + "." + styleName(bold, italic),
                        (slot0 == null ? FontGoldens.NULL : LinuxFontGoldens.safeText(slot0.getFullName()) + "|"
                                + LinuxFontGoldens.safeText(slot0.getFileName())) + "|" + logical.getNumSlots());
            }
        }
    }

    private static String texts(List<String> values) {
        List<String> converted = new ArrayList<>();
        for (String value : values) {
            converted.add(LinuxFontGoldens.safeText(value));
        }
        return FontGoldens.joinList(converted);
    }

    // ---------------------------------------------------------------------------------------------
    // Coverage
    // ---------------------------------------------------------------------------------------------

    /**
     * The golden can tell the right fontconfig binding from the wrong ones the audit named: lower-casing without
     * the passed locale, an all-or-nothing fill, lists replaced instead of appended to, and a copied first font.
     */
    static void assertCoverage(Map<String, String> values) {
        List<String> missing = new ArrayList<>();
        boolean dotlessTurkish = false;
        boolean dotlessEnglish = false;
        boolean firstIsZero = true;
        boolean anyCount = false;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("fc.raw.pm.tr-TR.file.")) {
                dotlessTurkish |= hasDotlessI(key.substring("fc.raw.pm.tr-TR.file.".length()));
            } else if (key.startsWith("fc.raw.pm.en.file.")) {
                dotlessEnglish |= hasDotlessI(key.substring("fc.raw.pm.en.file.".length()));
            } else if (key.startsWith("fc.raw.gfc.") && key.endsWith(".count")
                       && !key.startsWith("fc.raw.gfc.partial")) {
                String count = entry.getValue();
                if (!count.equals(FontGoldens.NULL) && Integer.parseInt(count) > 0) {
                    anyCount = true;
                    String first = values.get(key.substring(0, key.length() - ".count".length()) + ".first");
                    firstIsZero &= "0".equals(first);
                }
            }
        }
        LinuxFreetypeGoldenTest.check(missing, dotlessTurkish && !dotlessEnglish,
                "a tr-TR map key with U+0131 and no such key in the en map");
        LinuxFreetypeGoldenTest.check(missing, anyCount && firstIsZero,
                "getFontConfig results whose firstFont is allFonts[0] by identity");
        LinuxFreetypeGoldenTest.check(missing, "false".equals(values.get("fc.raw.gfc.partial.return"))
                && !FontGoldens.NULL.equals(values.get("fc.raw.gfc.partial.e00.count"))
                && FontGoldens.NULL.equals(values.get("fc.raw.gfc.partial.e01.first"))
                && FontGoldens.NULL.equals(values.get("fc.raw.gfc.partial.e01.count"))
                && FontGoldens.NULL.equals(values.get("fc.raw.gfc.partial.e02.first"))
                && FontGoldens.NULL.equals(values.get("fc.raw.gfc.partial.e02.count")),
                "a getFontConfig call that returns false with element 0 filled and elements 1 and 2 untouched");
        String seeded = values.get("fc.raw.pm.seeded.list.dejavu sans");
        LinuxFreetypeGoldenTest.check(missing, seeded != null && seeded.startsWith("#seed;"),
                "a pre-seeded family list that keeps its entry and gets the fontconfig names appended");
        if (!missing.isEmpty()) {
            fail(GOLDEN + " is not discriminating; it lacks: " + missing);
        }
    }

    /** Whether a key written by {@link LinuxFontGoldens#safeText} holds U+0131 LATIN SMALL LETTER DOTLESS I. */
    private static boolean hasDotlessI(String text) {
        return text.startsWith("u:") && LinuxFontGoldens.fromUnitsHex(text.substring(2)).indexOf(0x0131) >= 0;
    }

    private static List<String> header() {
        return LinuxFontGoldens.header("Linux fontconfig golden (FontConfigManager / fontpath_linux.c)", List.of(
                "  fc.raw.gfc.<locale>.<fb|nofb>  getFontConfig(locale, 12 logical names, includeFallbacks):",
                "                         .return; .eNN.name/.first (index of firstFont in allFonts by identity)/",
                "                         .firstFont/.count; .eNN.fMMM = family;style;fullName;file",
                "  fc.raw.gfc.partial.*   the first candidate FcNameParse rejects, between two good names",
                "  fc.raw.pm.<locale>.*   populateMapsNative into fresh maps: .return; .counts (file;family;list);",
                "                         .file.<key>, .family.<key>, .list.<key> (list in insertion order)",
                "  fc.raw.pm.seeded.*     populateMapsNative into pre-seeded maps",
                "  fc.view.gfcf.<family>.<style>  getFontConfigFont: fcName|fcFamily|style|firstFont;",
                "                         .files/.names = getFileNames/getFontNames",
                "  fc.api.*               PrismFontFactory family and full name lists; getFallbacks names|files",
                "  fc.logical.<family>.<style>  uncached LogicalFont: slot 0 full name|file|number of slots"));
    }
}
