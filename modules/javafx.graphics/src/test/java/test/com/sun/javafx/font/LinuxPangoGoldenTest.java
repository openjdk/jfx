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
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontResourceShim;
import com.sun.javafx.font.LinuxFontOracleShim;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.font.freetype.OSPangoShim;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.font.LinuxFontGoldens.GoldenMap;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Parity oracle for the Pango path of the Linux font layer ({@code OSPango}, implemented at commit
 * {@code 7b43255b30} by {@code pango.c} in {@code libjavafx_font_pango.so}), against a golden captured from that
 * JNI build.
 * <p>
 * The golden {@value #GOLDEN} records:
 * <ul>
 * <li>the three string crossings: {@code g_utf16_to_utf8} of raw UTF-16 code units (lone surrogates give NULL,
 * U+0000 truncates), the bytes {@code pango_font_description_set_family} stores for a Java string (modified
 * UTF-8), and the Java string {@code pango_font_description_get_family} makes of stored bytes (the JVM's
 * {@code NewStringUTF}); the stored and planted bytes are observed through {@link LinuxFontOracleShim};</li>
 * <li>{@code pango_itemize} and {@code pango_shape} called exactly as {@code PangoGlyphLayout.layout} calls them,
 * for two families, fallback on and off, both base directions and the whole string corpus: item offsets, levels
 * and scripts, glyphs, widths, clusters and the font Pango chose;</li>
 * <li>{@code PrismTextLayout} runs over composite, plain and logical fonts, which adds the Java cluster
 * arithmetic, glyph masking and fallback slot assignment.</li>
 * </ul>
 * Composite and logical fonts come from {@link FontResourceShim} and are never cached, and every layout is a new
 * {@code PrismTextLayout(0)}, so the fallback slots a case records do not depend on what other test classes
 * shaped first. See {@link LinuxFontGoldens}.
 */
@EnabledOnOs(OS.LINUX)
public class LinuxPangoGoldenTest {

    static final String GOLDEN = "linux-pango-golden.txt";

    private static final ParityGate.Ledger LEDGER = ParityGate.ledger(LinuxPangoGoldenTest.class);

    /** Table 6.1 of the Pango audit: raw UTF-16 inputs for {@code g_utf16_to_utf8}. */
    private static final List<String> UTF16_ROWS = Arrays.asList(
            "abc", "\u05E9\u05DC\u05D5\u05DD", "\uD83D\uDE00", "a\uD800", "\uD800a", "a\uDC00b", "a\u0000b", "",
            "\uFFFF\uFFFE", "\uDBFF\uDFFF", "\uD800\uD800\uDC00", "\u0000a");

    private static final List<String> UTF16_EXTRA_ROWS = Arrays.asList(
            null, "\u0080", "\u07FF", "\u0800", "\uFFFD", "\uFEFF");

    /** Java strings handed to {@code pango_font_description_set_family}. */
    private static final List<String> SET_FAMILY_ROWS = Arrays.asList(
            "DejaVu Sans", "A\u00E9B", "A\u20ACB", "A\uD83D\uDE00B", "A\uD800B", "A\u0000B", "", null, longName());

    /** Bytes stored as a family and read back through {@code pango_font_description_get_family}. */
    private static final List<String> GET_FAMILY_ROWS = List.of(
            "41f09f988042", "41eda0bdedb88042", "41c3a942", "41e282ac42", "41ff42", "41c342", "41c08042",
            "41e080af42", "41c3a9", "");

    private static final int[] LAYOUT_SUBSET = {1, 3, 4, 5, 8, 10, 15};

    @Test
    public void pangoMatchesTheJniGolden() throws Exception {
        LinuxFontGoldens.captureOrVerify(LinuxPangoGoldenTest.class, GOLDEN, header(), true,
                                         LinuxPangoGoldenTest::capture, LinuxPangoGoldenTest::assertCoverage);
    }

    /** The machine gate passed at least once, so at least one body key was compared ({@link ParityGate}). */
    @AfterAll
    static void theOracleRan() {
        LEDGER.assertOracleRan();
    }

    /**
     * Not vacuous, on any Linux with at least one font: GLib converts and rejects UTF-16 as documented, and Pango
     * itemizes and shapes three Latin letters into one left-to-right item with one glyph per letter.
     */
    @Test
    public void pangoShapesLatin() {
        Long fonts = LinuxFontOracleShim.fontconfigFontSetReads().get("list.nfont");
        assumeTrue(fonts != null && fonts > 0, "fontconfig lists no fonts on this machine");
        OSPangoShim.ensureLoaded();
        assertEquals(0L, OSPangoShim.g_utf16_to_utf8("ab\uD800".toCharArray()), "lone surrogate must give NULL");
        long fontmap = OSPangoShim.pango_ft2_font_map_new();
        assertNotEquals(0L, fontmap, "pango_ft2_font_map_new");
        long context = OSPangoShim.pango_font_map_create_context(fontmap);
        long attrs = OSPangoShim.pango_attr_list_new();
        long str = OSPangoShim.g_utf16_to_utf8("abc".toCharArray());
        assertNotEquals(0L, str, "g_utf16_to_utf8");
        assertEquals(3L, OSPangoShim.g_utf8_strlen(str, -1), "g_utf8_strlen");
        assertEquals(str + 3, OSPangoShim.g_utf8_offset_to_pointer(str, 3), "g_utf8_offset_to_pointer");
        long items = OSPangoShim.pango_itemize(context, str, 0, 3, attrs, 0);
        assertNotEquals(0L, items, "pango_itemize");
        assertEquals(1, OSPangoShim.g_list_length(items), "items of abc");
        long item = OSPangoShim.g_list_nth_data(items, 0);
        OSPangoShim.Shaped shaped = OSPangoShim.pango_shape(str, item);
        assertNotNull(shaped, "pango_shape");
        assertEquals(3, shaped.numGlyphs(), "glyphs of abc");
        assertEquals(3, shaped.numChars(), "characters of abc");
        assertArrayEquals(new int[] {0, 1, 2}, shaped.logClusters(), "clusters of abc");
        assertNotEquals(0L, shaped.font(), "font of abc");
        for (int width : shaped.widths()) {
            assertTrue(width > 0, "glyph widths of abc: " + Arrays.toString(shaped.widths()));
        }
        OSPangoShim.pango_item_free(item);
        OSPangoShim.g_list_free(items);
        OSPangoShim.g_free(str);
        OSPangoShim.pango_attr_list_unref(attrs);
        OSPangoShim.g_object_unref(context);
        OSPangoShim.g_object_unref(fontmap);
    }

    // ---------------------------------------------------------------------------------------------
    // Capture
    // ---------------------------------------------------------------------------------------------

    static void capture(GoldenMap out) {
        OSPangoShim.ensureLoaded();
        PrismFontFactory factory = PrismFontFactory.getFontFactory();
        captureCodecs(out);
        long fontmap = OSPangoShim.pango_ft2_font_map_new();
        out.put("pg.raw.fontmap", Boolean.toString(fontmap != 0));
        captureGuards(out, fontmap);
        String[][] families = {{"dejavusans", "DejaVu Sans"}, {"lato", "Lato"}};
        for (String[] family : families) {
            for (boolean fallback : new boolean[] {true, false}) {
                for (boolean rtl : new boolean[] {false, true}) {
                    for (int s = 0; s < LinuxFontGoldens.TEXTS.size(); s++) {
                        rawRun(out, fontmap, family[0], family[1], fallback, rtl, 12f, s);
                    }
                }
            }
        }
        for (int s = 0; s < LinuxFontGoldens.TEXTS.size(); s++) {
            rawRun(out, fontmap, "dejavusans", "DejaVu Sans", true, false, 12.5f, s);
        }
        OSPangoShim.g_object_unref(fontmap);
        captureLayouts(out, factory);
    }

    private static void captureCodecs(GoldenMap out) {
        for (int i = 0; i < UTF16_ROWS.size(); i++) {
            out.put("pg.utf16to8.a" + LinuxFontGoldens.pad(i, 2), utf16to8(UTF16_ROWS.get(i), false));
        }
        for (int i = 0; i < UTF16_EXTRA_ROWS.size(); i++) {
            out.put("pg.utf16to8.x" + LinuxFontGoldens.pad(i, 2), utf16to8(UTF16_EXTRA_ROWS.get(i), false));
        }
        out.put("pg.utf16to8.x" + LinuxFontGoldens.pad(UTF16_EXTRA_ROWS.size(), 2), utf16to8(longText(), true));
        for (int i = 0; i < LinuxFontGoldens.TEXTS.size(); i++) {
            out.put("pg.utf16to8.s" + LinuxFontGoldens.pad(i, 2), utf16to8(LinuxFontGoldens.TEXTS.get(i), false));
        }

        for (int i = 0; i < SET_FAMILY_ROWS.size(); i++) {
            String input = SET_FAMILY_ROWS.get(i);
            long desc = OSPangoShim.pango_font_description_new();
            OSPangoShim.pango_font_description_set_family(desc, input);
            byte[] stored = LinuxFontOracleShim.pangoGetFamilyRaw(desc);
            OSPangoShim.pango_font_description_free(desc);
            String in = input != null && input.length() > 64 ? summary(input) : LinuxFontGoldens.unitsHex(input);
            out.put("pg.setFamily.f" + LinuxFontGoldens.pad(i, 2), in + "|" + LinuxFontGoldens.bytesHex(stored));
        }

        for (int i = 0; i < GET_FAMILY_ROWS.size(); i++) {
            byte[] planted = LinuxFontGoldens.parseBytesHex(GET_FAMILY_ROWS.get(i));
            long desc = OSPangoShim.pango_font_description_new();
            LinuxFontOracleShim.pangoSetFamilyRaw(desc, planted);
            String family = OSPangoShim.pango_font_description_get_family(desc);
            OSPangoShim.pango_font_description_free(desc);
            out.put("pg.getFamily.b" + LinuxFontGoldens.pad(i, 2),
                    LinuxFontGoldens.bytesHex(planted) + "|" + LinuxFontGoldens.unitsHex(family));
        }
        long unset = OSPangoShim.pango_font_description_new();
        String family = OSPangoShim.pango_font_description_get_family(unset);
        OSPangoShim.pango_font_description_free(unset);
        out.put("pg.getFamily.unset", FontGoldens.NULL + "|" + LinuxFontGoldens.unitsHex(family));
    }

    /** {@code input|bytes|strlen|g_utf8_strlen(str,-1)|g_utf8_offset_to_pointer(str,len)-str}. */
    private static String utf16to8(String input, boolean summarize) {
        String in = summarize ? summary(input) : LinuxFontGoldens.unitsHex(input);
        long str = OSPangoShim.g_utf16_to_utf8(input == null ? null : input.toCharArray());
        if (str == 0) {
            return in + "|#null|#null|#null|#null";
        }
        byte[] bytes = LinuxFontOracleShim.cString(str, 1 << 20);
        long length = OSPangoShim.g_utf8_strlen(str, -1);
        long end = OSPangoShim.g_utf8_offset_to_pointer(str, length);
        OSPangoShim.g_free(str);
        String out = summarize ? bytes.length + ":" + FontGoldens.sha256Hex(bytes) : LinuxFontGoldens.bytesHex(bytes);
        return in + "|" + out + "|" + bytes.length + "|" + length + "|" + (end - str);
    }

    private static String summary(String text) {
        return text.length() + ":" + FontGoldens.sha256Hex(text.getBytes(StandardCharsets.UTF_16BE));
    }

    /** 10,000 BMP code units, no surrogate and no NUL. */
    private static String longText() {
        StringBuilder text = new StringBuilder(10_000);
        for (int i = 0; i < 10_000; i++) {
            text.append((char) (0x20 + (i * 7919) % (0xD800 - 0x20)));
        }
        return text.toString();
    }

    private static String longName() {
        StringBuilder name = new StringBuilder();
        while (name.length() < 300) {
            name.append("Long Family Name ");
        }
        return name.substring(0, 300);
    }

    /** The null guards of the natives, exercised with a real item and a real string. */
    private static void captureGuards(GoldenMap out, long fontmap) {
        out.put("pg.guard.strlen0", Long.toString(OSPangoShim.g_utf8_strlen(0, -1)));
        out.put("pg.guard.offsetToPointer0", Long.toString(OSPangoShim.g_utf8_offset_to_pointer(0, 5)));
        long context = OSPangoShim.pango_font_map_create_context(fontmap);
        long attrs = OSPangoShim.pango_attr_list_new();
        long str = OSPangoShim.g_utf16_to_utf8("abc".toCharArray());
        long items = OSPangoShim.pango_itemize(context, str, 0, 3, attrs, 0);
        long item = OSPangoShim.g_list_nth_data(items, 0);
        out.put("pg.guard.shapeNullText", OSPangoShim.pango_shape(0, item) == null ? FontGoldens.NULL : "shaped");
        out.put("pg.guard.shapeNullItem", OSPangoShim.pango_shape(str, 0) == null ? FontGoldens.NULL : "shaped");
        OSPangoShim.pango_item_free(item);
        OSPangoShim.g_list_free(items);
        OSPangoShim.g_free(str);
        OSPangoShim.pango_attr_list_unref(attrs);
        OSPangoShim.g_object_unref(context);
    }

    /**
     * One run exactly as {@code PangoGlyphLayout.layout} makes it: {@code key = utf8Length|items}, and per item
     * {@code offset;length;numChars;level;script|numGlyphs;numChars|glyphs|widths|clusters|family;style;weight|
     * fontIsItemFont}.
     */
    private static void rawRun(GoldenMap out, long fontmap, String familyKey, String family, boolean fallback,
                               boolean rtl, float size, int textIndex) {
        String key = "pg.raw." + familyKey + "." + (fallback ? "on" : "off") + "." + (rtl ? "rtl" : "ltr") + "."
                + size + ".s" + LinuxFontGoldens.pad(textIndex, 2);
        long context = OSPangoShim.pango_font_map_create_context(fontmap);
        if (rtl) {
            OSPangoShim.pango_context_set_base_dir(context, OSPangoShim.constant("PANGO_DIRECTION_RTL"));
        }
        long desc = OSPangoShim.pango_font_description_new();
        OSPangoShim.pango_font_description_set_family(desc, family);
        OSPangoShim.pango_font_description_set_absolute_size(desc, size * OSPangoShim.constant("PANGO_SCALE"));
        OSPangoShim.pango_font_description_set_stretch(desc, OSPangoShim.constant("PANGO_STRETCH_NORMAL"));
        OSPangoShim.pango_font_description_set_style(desc, OSPangoShim.constant("PANGO_STYLE_NORMAL"));
        OSPangoShim.pango_font_description_set_weight(desc, OSPangoShim.constant("PANGO_WEIGHT_NORMAL"));
        long attrs = OSPangoShim.pango_attr_list_new();
        OSPangoShim.pango_attr_list_insert(attrs, OSPangoShim.pango_attr_font_desc_new(desc));
        if (!fallback) {
            OSPangoShim.pango_attr_list_insert(attrs, OSPangoShim.pango_attr_fallback_new(false));
        }
        long str = OSPangoShim.g_utf16_to_utf8(LinuxFontGoldens.TEXTS.get(textIndex).toCharArray());
        if (str == 0) {
            out.put(key, "#null|#null");
        } else {
            long length = OSPangoShim.g_utf8_strlen(str, -1);
            long end = OSPangoShim.g_utf8_offset_to_pointer(str, length);
            long runs = OSPangoShim.pango_itemize(context, str, 0, (int) (end - str), attrs, 0);
            if (runs == 0) {
                out.put(key, (end - str) + "|#null");
            } else {
                int count = OSPangoShim.g_list_length(runs);
                out.put(key, (end - str) + "|" + count);
                int[][] publicFields = new int[count][];
                long[] itemFonts = new long[count];
                OSPangoShim.Shaped[] shaped = new OSPangoShim.Shaped[count];
                for (int i = 0; i < count; i++) {
                    long item = OSPangoShim.g_list_nth_data(runs, i);
                    if (item != 0) {
                        publicFields[i] = LinuxFontOracleShim.pangoItemPublic(item);
                        itemFonts[i] = LinuxFontOracleShim.pangoItemFont(item);
                        shaped[i] = OSPangoShim.pango_shape(str, item);
                        OSPangoShim.pango_item_free(item);
                    }
                }
                OSPangoShim.g_list_free(runs);
                for (int i = 0; i < count; i++) {
                    out.put(key + ".i" + LinuxFontGoldens.pad(i, 2),
                            itemValue(publicFields[i], shaped[i], itemFonts[i]));
                }
            }
            OSPangoShim.g_free(str);
        }
        OSPangoShim.pango_attr_list_unref(attrs);
        OSPangoShim.pango_font_description_free(desc);
        OSPangoShim.g_object_unref(context);
    }

    private static String itemValue(int[] publicFields, OSPangoShim.Shaped shaped, long itemFont) {
        if (publicFields == null) {
            return FontGoldens.NULL;
        }
        StringBuilder value = new StringBuilder();
        value.append(publicFields[0]).append(';').append(publicFields[1]).append(';').append(publicFields[2])
             .append(';').append(publicFields[3]).append(';').append(publicFields[4]).append('|');
        if (shaped == null) {
            return value.append(FontGoldens.NULL).toString();
        }
        List<String> glyphs = new ArrayList<>();
        for (int glyph : shaped.glyphs()) {
            glyphs.add(String.format("%08x", glyph));
        }
        value.append(shaped.numGlyphs()).append(';').append(shaped.numChars())
             .append('|').append(String.join(";", glyphs))
             .append('|').append(LinuxFontGoldens.joinInts(shaped.widths()))
             .append('|').append(LinuxFontGoldens.joinInts(shaped.logClusters()))
             .append('|');
        if (shaped.font() == 0) {
            value.append(FontGoldens.NULL);
        } else {
            long described = OSPangoShim.pango_font_describe(shaped.font());
            value.append(LinuxFontGoldens.safeText(OSPangoShim.pango_font_description_get_family(described)))
                 .append(';').append(OSPangoShim.pango_font_description_get_style(described))
                 .append(';').append(OSPangoShim.pango_font_description_get_weight(described));
            OSPangoShim.pango_font_description_free(described);
        }
        return value.append('|').append(shaped.font() == itemFont).toString();
    }

    private static void captureLayouts(GoldenMap out, PrismFontFactory factory) {
        FontResource dejavu = factory.getFontResource("DejaVu Sans", false, false, false);
        FontResource dejavuBold = factory.getFontResource("DejaVu Sans", true, false, false);
        FontResource dejavuOblique = factory.getFontResource("DejaVu Sans", false, true, false);
        FontResource lato = factory.getFontResource("Lato", false, false, false);
        FontResource ubuntu = factory.getFontResource("Ubuntu", false, false, false);

        int[] all = new int[LinuxFontGoldens.TEXTS.size()];
        for (int i = 0; i < all.length; i++) {
            all[i] = i;
        }
        layoutCase(out, "dvs.comp", composite(dejavu), 12f, all, true);
        layoutCase(out, "lato.comp", composite(lato), 12f, all, false);
        layoutCase(out, "dvs.plain", dejavu, 12f, all, false);
        layoutCase(out, "sys.logical", FontResourceShim.newLogicalFont("System", false, false), 12f, all, false);
        layoutCase(out, "dvs.bold.comp", composite(dejavuBold), 12f, LAYOUT_SUBSET, false);
        layoutCase(out, "dvs.oblique.comp", composite(dejavuOblique), 12f, LAYOUT_SUBSET, false);
        layoutCase(out, "dvs.comp.size0", composite(dejavu), 0f, LAYOUT_SUBSET, false);
        layoutCase(out, "ubuntu.plain", ubuntu, 12f, LAYOUT_SUBSET, false);
    }

    private static FontResource composite(FontResource primary) {
        return primary == null ? null : FontResourceShim.newComposite(primary);
    }

    private static void layoutCase(GoldenMap out, String name, FontResource resource, float size, int[] texts,
                                   boolean bothDirections) {
        String prefix = "pg.layout." + name;
        if (resource == null) {
            out.put(prefix + ".resource", FontGoldens.NULL);
            return;
        }
        out.put(prefix + ".resource", LinuxFontGoldens.safeText(resource.getFullName()) + ";"
                + LinuxFontGoldens.safeText(resource.getFileName()) + ";" + resource.getClass().getSimpleName());
        PGFont font = FontResourceShim.newPrismFont(resource, resource.getFullName(), size);
        for (int s : texts) {
            String text = LinuxFontGoldens.TEXTS.get(s);
            LinuxFontGoldens.putLayout(out, prefix + ".ltr.s" + LinuxFontGoldens.pad(s, 2), font, text, false);
            if (bothDirections) {
                LinuxFontGoldens.putLayout(out, prefix + ".rtl.s" + LinuxFontGoldens.pad(s, 2), font, text, true);
            }
        }
        if (resource instanceof CompositeFontResource composite) {
            int slots = composite.getNumSlots();
            out.put(prefix + ".numSlots", Integer.toString(slots));
            for (int slot = 0; slot < slots; slot++) {
                FontResource slotResource = composite.getSlotResource(slot);
                out.put(prefix + ".slot" + LinuxFontGoldens.pad(slot, 2), slotResource == null ? FontGoldens.NULL
                        : LinuxFontGoldens.safeText(slotResource.getFullName()) + ";"
                        + LinuxFontGoldens.safeText(slotResource.getFileName()));
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Coverage
    // ---------------------------------------------------------------------------------------------

    /**
     * The golden can tell the right Pango binding from the wrong ones the audit named: a charset encoder in place
     * of {@code g_utf16_to_utf8}, standard UTF-8 in place of modified UTF-8, {@code MemorySegment.getString} in
     * place of the {@code NewStringUTF} rules, a missing byte-to-character cluster conversion, and the unknown
     * glyph, fallback slot and lone-surrogate paths.
     */
    static void assertCoverage(Map<String, String> values) {
        List<String> missing = new ArrayList<>();
        boolean nullWhereCharsetEncodes = false;
        boolean truncatedAtNul = false;
        boolean modifiedUtf8 = false;
        boolean jniDecoding = false;
        boolean rtlByteClusters = false;
        boolean unknownGlyph = false;
        boolean fallbackSlot = false;
        boolean unshapedSplitRun = false;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String[] parts = entry.getValue().split("\\|", -1);
            if (key.startsWith("pg.utf16to8.") && !parts[0].contains(":") && !parts[0].equals(FontGoldens.NULL)) {
                String input = LinuxFontGoldens.fromUnitsHex(parts[0]);
                nullWhereCharsetEncodes |= parts[1].equals(FontGoldens.NULL);
                int nul = input.indexOf('\0');
                if (nul >= 0 && !parts[1].equals(FontGoldens.NULL)) {
                    byte[] prefix = input.substring(0, nul).getBytes(StandardCharsets.UTF_8);
                    truncatedAtNul |= parts[1].equals(LinuxFontGoldens.bytesHex(prefix));
                }
            } else if (key.startsWith("pg.setFamily.") && !parts[0].contains(":")
                       && !parts[0].equals(FontGoldens.NULL) && !parts[1].equals(FontGoldens.NULL)) {
                String input = LinuxFontGoldens.fromUnitsHex(parts[0]);
                modifiedUtf8 |= !parts[1].equals(LinuxFontGoldens.bytesHex(input.getBytes(StandardCharsets.UTF_8)));
            } else if (key.startsWith("pg.getFamily.b") && !parts[1].equals(FontGoldens.NULL)) {
                String standard = new String(LinuxFontGoldens.parseBytesHex(parts[0]), StandardCharsets.UTF_8);
                jniDecoding |= !parts[1].equals(LinuxFontGoldens.unitsHex(standard));
            } else if (key.startsWith("pg.raw.") && key.contains(".i") && parts.length >= 6) {
                String[] item = parts[0].split(";");
                String[] counts = parts[1].split(";");
                if (counts.length == 2) {
                    int numGlyphs = Integer.parseInt(counts[0]);
                    boolean odd = (Integer.parseInt(item[3]) & 1) != 0;
                    boolean multiByte = !item[1].equals(item[2]);
                    rtlByteClusters |= odd && multiByte && numGlyphs >= 2 && descending(parts[4]);
                    for (String glyph : parts[2].split(";")) {
                        unknownGlyph |= !glyph.isEmpty() && (Long.parseLong(glyph, 16) & 0xF0000000L) == 0x10000000L;
                    }
                }
            } else if (key.startsWith("pg.layout.") && key.contains(".r")) {
                String[] run = parts[0].split(";");
                if (run.length >= 8) {
                    for (String code : parts[1].split(";")) {
                        fallbackSlot |= !code.isEmpty() && (Long.parseLong(code, 16) >>> 24) > 0;
                    }
                    boolean splitText = key.contains(".s13.") || key.contains(".s14.");
                    unshapedSplitRun |= splitText && run[5].equals("true") && run[7].equals("0");
                }
            }
        }
        LinuxFreetypeGoldenTest.check(missing, nullWhereCharsetEncodes,
                "a g_utf16_to_utf8 row that is NULL where a charset encoder would produce bytes");
        LinuxFreetypeGoldenTest.check(missing, truncatedAtNul, "a g_utf16_to_utf8 row truncated at U+0000");
        LinuxFreetypeGoldenTest.check(missing, modifiedUtf8,
                "a set_family row whose stored bytes differ from standard UTF-8");
        LinuxFreetypeGoldenTest.check(missing, jniDecoding,
                "a get_family row whose Java string differs from a standard UTF-8 decode");
        LinuxFreetypeGoldenTest.check(missing, rtlByteClusters,
                "a right-to-left multi-byte item with descending clusters");
        LinuxFreetypeGoldenTest.check(missing, unknownGlyph, "a raw unknown glyph 0x1000xxxx");
        LinuxFreetypeGoldenTest.check(missing, fallbackSlot, "a layout glyph code in a fallback slot above 0");
        LinuxFreetypeGoldenTest.check(missing, unshapedSplitRun,
                "an empty complex run caused by a surrogate pair split across bidi runs (s13 or s14)");
        if (!missing.isEmpty()) {
            fail(GOLDEN + " is not discriminating; it lacks: " + missing);
        }
    }

    private static boolean descending(String clusters) {
        String[] values = clusters.split(";");
        for (int i = 1; i < values.length; i++) {
            if (Integer.parseInt(values[i]) >= Integer.parseInt(values[i - 1])) {
                return false;
            }
        }
        return true;
    }

    private static List<String> header() {
        return LinuxFontGoldens.header("Linux Pango golden (OSPango / pango.c, PangoGlyphLayout, PrismTextLayout)",
                List.of(
                "  pg.utf16to8.*          g_utf16_to_utf8: input units|bytes|strlen|g_utf8_strlen|end-str",
                "                         (aNN: audit table 6.1, xNN: code point limits, sNN: string corpus)",
                "  pg.setFamily.fNN       set_family: input units|bytes Pango stored",
                "  pg.getFamily.bNN       bytes planted|units of the Java string get_family returned",
                "  pg.guard.*             null guards of g_utf8_strlen, g_utf8_offset_to_pointer, pango_shape",
                "  pg.raw.<family>.<fallback>.<dir>.<size>.sNN  PangoGlyphLayout call sequence: utf8Length|items",
                "  pg.raw...sNN.iII       offset;length;numChars;level;script|numGlyphs;numChars|glyphs|widths|",
                "                         clusters|family;style;weight|shaped font == item font",
                "  pg.layout.<case>.<dir>.sNN  PrismTextLayout: bounds|lines|runs",
                "  pg.layout...sNN.rRR    start;length;level;script;slot;complex;ltr;glyphCount;width;height;",
                "                         ascent;descent;leading|glyph codes (hex)|char offsets|posX|posY",
                "  pg.layout.<case>.slot* fallback slots after the case: full name;file"));
    }
}
