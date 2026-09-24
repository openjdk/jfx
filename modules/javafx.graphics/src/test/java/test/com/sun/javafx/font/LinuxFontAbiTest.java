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

import com.sun.javafx.font.LinuxFontOracleShim;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Self-consistency of the system library record layouts the Linux font layer reads, checked against the running
 * libraries with no C of the project: {@code FT_FaceRec}, {@code FT_GlyphSlotRec} and {@code FT_Bitmap} of
 * FreeType, {@code PangoItem}, {@code PangoAnalysis}, {@code PangoGlyphString}, {@code PangoGlyphInfo} and
 * {@code GList}, and {@code FcFontSet}.
 * <p>
 * Every assertion is an invariant that holds for any correct layout on any architecture and any library version:
 * a record points back at its owner, a count matches what an API function reports, a field read through the
 * offset equals the same value obtained another way (the font file itself, {@code g_list_nth_data},
 * {@code pango_glyph_string_get_width}). A wrong offset reads another field and breaks one of them. The tests are
 * skipped only when a library or the repository's Ahem font is absent; they never depend on the installed fonts'
 * versions, so they run on every Linux architecture where the goldens cannot. All reads go through
 * {@link LinuxFontOracleShim}, which never calls the bindings under test.
 */
@EnabledOnOs(OS.LINUX)
public class LinuxFontAbiTest {

    /** {@code freetype.h}: {@code FT_LOAD_*}, {@code FT_PIXEL_MODE_GRAY}, {@code FT_GLYPH_FORMAT_*}. */
    private static final int FT_LOAD_NO_SCALE = 1;
    private static final int FT_LOAD_NO_HINTING = 1 << 1;
    private static final int FT_LOAD_RENDER = 1 << 2;
    private static final int FT_LOAD_NO_BITMAP = 1 << 3;
    private static final int FT_LOAD_IGNORE_TRANSFORM = 1 << 11;
    private static final int FT_PIXEL_MODE_GRAY = 2;
    private static final long FT_GLYPH_FORMAT_BITMAP = 0x62697473L;
    private static final long FT_GLYPH_FORMAT_OUTLINE = 0x6F75746CL;

    /** GLib {@code GUnicodeScript}, which {@code PangoScript} mirrors. */
    private static final long SCRIPT_HEBREW = 19;
    private static final long SCRIPT_LATIN = 25;

    private static Path ahem() {
        Path ahem = LinuxFontGoldens.pinned("ahem");
        assumeTrue(Files.isRegularFile(ahem), "the repository font " + ahem + " is not present");
        assumeTrue(LinuxFontOracleShim.libraryAvailable("libfreetype.so.6"), "libfreetype.so.6 is not installed");
        return ahem;
    }

    @Test
    public void freetypeFaceRecordMatchesTheFontFile() throws IOException {
        Path ahem = ahem();
        ByteBuffer font = ByteBuffer.wrap(Files.readAllBytes(ahem));
        Map<String, Long> reads = LinuxFontOracleShim.freetypeSlotReads(ahem.toString(), 0, FT_LOAD_NO_SCALE, 0);
        assertEquals(0L, reads.get("init.rc"), "FT_Init_FreeType");
        assertEquals(0L, reads.get("newFace.rc"), "FT_New_Face");
        assertEquals(sfntField(font, "maxp", 4), reads.get("face.num_glyphs"), "FT_FaceRec.num_glyphs");
        assertEquals(sfntField(font, "head", 18), reads.get("face.units_per_EM"), "FT_FaceRec.units_per_EM");
        assertEquals(0L, reads.get("doneFace.rc"), "FT_Done_Face");
        assertEquals(0L, reads.get("done.rc"), "FT_Done_FreeType");
    }

    @Test
    public void freetypeGlyphSlotRecordIsSelfConsistent() {
        Path ahem = ahem();
        int render = FT_LOAD_RENDER | FT_LOAD_NO_HINTING | FT_LOAD_NO_BITMAP | FT_LOAD_IGNORE_TRANSFORM;
        boolean rendered = false;
        for (int glyph = 0; glyph < 64 && !rendered; glyph++) {
            Map<String, Long> reads = LinuxFontOracleShim.freetypeSlotReads(ahem.toString(), glyph, render, 16 * 64);
            assertEquals(0L, reads.get("newFace.rc"), "FT_New_Face");
            assertEquals(0L, reads.get("charSize.rc"), "FT_Set_Char_Size");
            if (reads.get("load.rc") != 0) {
                break;
            }
            String what = " after rendering glyph " + glyph;
            assertNotEquals(0L, reads.get("face.glyph"), "FT_FaceRec.glyph" + what);
            assertEquals(reads.get("library"), reads.get("slot.library"), "FT_GlyphSlotRec.library" + what);
            assertEquals(reads.get("face"), reads.get("slot.face"), "FT_GlyphSlotRec.face" + what);
            assertEquals(FT_GLYPH_FORMAT_BITMAP, reads.get("slot.format"), "FT_GlyphSlotRec.format" + what);
            assertEquals((long) FT_PIXEL_MODE_GRAY, reads.get("slot.bitmap.pixel_mode"), "FT_Bitmap.pixel_mode" + what);
            assertEquals(256L, reads.get("slot.bitmap.num_grays"), "FT_Bitmap.num_grays" + what);
            long rows = reads.get("slot.bitmap.rows");
            long pitch = reads.get("slot.bitmap.pitch");
            if (rows * pitch > 0) {
                assertNotEquals(0L, reads.get("slot.bitmap.buffer"), "FT_Bitmap.buffer" + what);
                assertEquals(reads.get("slot.bitmap.width"), pitch, "FT_Bitmap.pitch of a grey bitmap" + what);
                rendered = true;
            }
        }
        assertTrue(rendered, "no glyph of " + ahem + " rendered to a non-empty bitmap");
    }

    @Test
    public void freetypeOutlineSlotIsSelfConsistent() {
        Path ahem = ahem();
        int outline = FT_LOAD_NO_HINTING | FT_LOAD_NO_BITMAP | FT_LOAD_IGNORE_TRANSFORM;
        boolean checked = false;
        for (int glyph = 0; glyph < 64 && !checked; glyph++) {
            Map<String, Long> reads = LinuxFontOracleShim.freetypeSlotReads(ahem.toString(), glyph, outline, 16 * 64);
            if (reads.get("load.rc") != 0) {
                break;
            }
            String what = " after loading glyph " + glyph;
            assertEquals(FT_GLYPH_FORMAT_OUTLINE, reads.get("slot.format"), "FT_GlyphSlotRec.format" + what);
            assertEquals(reads.get("slot.metrics.horiAdvance"), reads.get("slot.advance.x"),
                         "FT_GlyphSlotRec.advance.x of an unhinted, untransformed load" + what);
            assertEquals(0L, reads.get("slot.advance.y"), "FT_GlyphSlotRec.advance.y" + what);
            if (reads.get("slot.outline.n_points") > 0) {
                assertNotEquals(0L, reads.get("slot.outline.points"), "FT_Outline.points" + what);
                assertNotEquals(0L, reads.get("slot.outline.tags"), "FT_Outline.tags" + what);
                assertTrue(reads.get("slot.outline.n_contours") > 0, "FT_Outline.n_contours" + what);
                assertNotEquals(0L, reads.get("slot.outline.contours"), "FT_Outline.contours" + what);
                assertTrue(reads.get("slot.metrics.width") > 0, "FT_Glyph_Metrics.width" + what);
                checked = true;
            }
        }
        assertTrue(checked, "no glyph of " + ahem + " has outline points");
    }

    @Test
    public void pangoGlyphStringRecordIsSelfConsistent() {
        assumeTrue(LinuxFontOracleShim.libraryAvailable("libpango-1.0.so.0"), "libpango-1.0.so.0 is not installed");
        Map<String, Long> reads = LinuxFontOracleShim.pangoGlyphStringReads(7);
        assertEquals(0L, reads.get("new.num_glyphs"), "PangoGlyphString.num_glyphs of a new string");
        assertEquals(7L, reads.get("sized.num_glyphs"), "PangoGlyphString.num_glyphs after set_size(7)");
        assertNotEquals(0L, reads.get("sized.glyphs"), "PangoGlyphString.glyphs after set_size(7)");
        assertNotEquals(0L, reads.get("sized.log_clusters"), "PangoGlyphString.log_clusters after set_size(7)");
    }

    @Test
    public void pangoItemRecordIsSelfConsistent() {
        assumePangoWithFonts();
        Map<String, Long> reads = LinuxFontOracleShim.pangoItemizeReads("abc");
        assertEquals(1L, reads.get("items"), "items of abc");
        assertEquals(reads.get("list.nthData0"), reads.get("list.data"), "GList.data");
        assertEquals(0L, reads.get("item0.offset"), "PangoItem.offset");
        assertEquals(3L, reads.get("item0.length"), "PangoItem.length");
        assertEquals(3L, reads.get("item0.num_chars"), "PangoItem.num_chars");
        assertNotEquals(0L, reads.get("item0.font"), "PangoAnalysis.font");
        assertEquals(0L, reads.get("item0.level"), "PangoAnalysis.level");
        assertEquals(SCRIPT_LATIN, reads.get("item0.script"), "PangoAnalysis.script");
        assertEquals(3L, reads.get("gs.num_glyphs"), "glyphs of abc shaped with the item's analysis");
        long sum = 0;
        for (int g = 0; g < 3; g++) {
            assertEquals((long) g, reads.get("gs.cluster." + g), "PangoGlyphString.log_clusters[" + g + "]");
            assertTrue(reads.get("gs.width." + g) > 0, "PangoGlyphInfo.geometry.width[" + g + "]");
            sum += reads.get("gs.width." + g);
        }
        assertEquals(reads.get("gs.getWidth"), sum, "PangoGlyphInfo widths against pango_glyph_string_get_width");
        assertEquals(reads.get("gs.getWidth"), reads.get("gs.widthSum"), "width sum");
    }

    @Test
    public void pangoRightToLeftItemRecordIsSelfConsistent() {
        assumePangoWithFonts();
        Map<String, Long> reads = LinuxFontOracleShim.pangoItemizeReads("\u05D0\u05D1");
        assertEquals(1L, reads.get("items"), "items of two Hebrew letters");
        assertEquals(0L, reads.get("item0.offset"), "PangoItem.offset");
        assertEquals(4L, reads.get("item0.length"), "PangoItem.length in UTF-8 bytes");
        assertEquals(2L, reads.get("item0.num_chars"), "PangoItem.num_chars");
        assertEquals(1L, reads.get("item0.level"), "PangoAnalysis.level of right-to-left text");
        assertEquals(SCRIPT_HEBREW, reads.get("item0.script"), "PangoAnalysis.script");
        assertEquals(2L, reads.get("gs.num_glyphs"), "glyphs of two Hebrew letters");
        assertEquals(2L, reads.get("gs.cluster.0"), "byte cluster of the first visual glyph");
        assertEquals(0L, reads.get("gs.cluster.1"), "byte cluster of the second visual glyph");
        assertEquals(reads.get("gs.getWidth"), reads.get("gs.widthSum"), "width sum");
    }

    @Test
    public void fontconfigFontSetRecordIsSelfConsistent() {
        assumeTrue(LinuxFontOracleShim.libraryAvailable("libfontconfig.so.1"), "libfontconfig.so.1 is not installed");
        Map<String, Long> reads = LinuxFontOracleShim.fontconfigFontSetReads();
        assertEquals(1L, reads.get("objectSetAdd.rc"), "FcObjectSetAdd");
        assertNotEquals(0L, reads.get("list"), "FcFontList");
        long nfont = reads.get("list.nfont");
        assertTrue(nfont >= 0 && nfont <= reads.get("list.sfont"), "FcFontSet nfont " + nfont + " <= sfont "
                   + reads.get("list.sfont"));
        if (nfont > 0) {
            assertNotEquals(0L, reads.get("list.fonts"), "FcFontSet.fonts");
            assertNotEquals(0L, reads.get("list.font0"), "FcFontSet.fonts[0]");
            assertEquals(0L, reads.get("list.font0.file.rc"), "FcPatternGetString(fonts[0], file)");
            assertTrue(reads.get("list.font0.file.bytes") > 0, "file of fonts[0]");
        }
        if (reads.containsKey("system.nfont")) {
            assertTrue(reads.get("system.nfont") <= reads.get("system.sfont"), "system FcFontSet nfont <= sfont");
        }
    }

    private static void assumePangoWithFonts() {
        assumeTrue(LinuxFontOracleShim.libraryAvailable("libpango-1.0.so.0")
                   && LinuxFontOracleShim.libraryAvailable("libpangoft2-1.0.so.0")
                   && LinuxFontOracleShim.libraryAvailable("libfontconfig.so.1"),
                   "Pango or fontconfig is not installed");
        Long fonts = LinuxFontOracleShim.fontconfigFontSetReads().get("list.nfont");
        assumeTrue(fonts != null && fonts > 0, "fontconfig lists no fonts");
    }

    /** An unsigned 16-bit field of an sfnt table, read from the font file by Java. */
    private static long sfntField(ByteBuffer font, String tag, int offset) {
        int tables = font.getShort(4) & 0xFFFF;
        int wanted = (tag.charAt(0) << 24) | (tag.charAt(1) << 16) | (tag.charAt(2) << 8) | tag.charAt(3);
        for (int i = 0, record = 12; i < tables; i++, record += 16) {
            if (font.getInt(record) == wanted) {
                return font.getShort(font.getInt(record + 8) + offset) & 0xFFFF;
            }
        }
        throw new IllegalStateException("no " + tag + " table");
    }
}
