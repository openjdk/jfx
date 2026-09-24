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

import com.sun.javafx.font.CharToGlyphMapper;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontResourceShim;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.Glyph;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.font.PrismFontFile;
import com.sun.javafx.font.freetype.OSFreetypeShim;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.BaseTransform;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.font.LinuxFontGoldens.GoldenMap;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Parity oracle for the FreeType path of the Linux font layer ({@code OSFreetype}, implemented at commit
 * {@code 7b43255b30} by {@code freetype.c} in {@code libjavafx_font_freetype.so}), against a golden captured from
 * that JNI build.
 * <p>
 * The golden {@value #GOLDEN} records, on a test-owned {@code FT_Library}:
 * <ul>
 * <li>the library version, {@code FT_Library_SetLcdFilter} return codes and the two build flags;</li>
 * <li>{@code FT_New_Face} return codes for good, bad, missing and non-ASCII paths and face indices, with a
 * pre-seeded out parameter whose survival on failure is part of the record;</li>
 * <li>{@code FT_Set_Char_Size} codes, and per load flag set and glyph ({@code FT_Load_Glyph}) the glyph slot
 * fields production Java reads and the bitmap ({@code getBitmapData}) as length and sha256, including after a
 * failed load, under three LCD filters and five transforms ({@code FT_Set_Transform});</li>
 * <li>{@code FT_Outline_Decompose} outlines of DejaVu Sans (quadratic) and of the Type 1 font
 * {@code freeeuro.pfa} (cubic), as lengths plus a sha256 of the exact float bits, and nine full dumps.</li>
 * </ul>
 * and, through the Java font API ({@code FTFontFile}, {@code FTFontStrike}, {@code FTGlyph}) for four font files
 * opened outside the factory's caches, three TrueType and one OpenType with CFF outlines ({@link #FONT_WITH_CURVES},
 * whose glyph 52 is cubic): glyph bounding boxes, grey and LCD masks at seven sizes and ten transforms, and
 * outlines at eight sizes. Size {@code 1e7} does not fit {@code FT_Set_Char_Size}, whose return code the Java
 * side ignores, so its outline is taken directly after the size {@code 1000} one and records the face state that
 * call left. Every mask glyph is read again at the end, with its strike still held, and must not have changed.
 * <p>
 * Every value is machine-specific; the comparison runs only where the {@code machine.} header of
 * {@link LinuxFontGoldens#machine()} matches, and the corpus never runs elsewhere. See {@link LinuxFontGoldens}.
 */
@EnabledOnOs(OS.LINUX)
public class LinuxFreetypeGoldenTest {

    static final String GOLDEN = "linux-freetype-golden.txt";

    private static final ParityGate.Ledger LEDGER = ParityGate.ledger(LinuxFreetypeGoldenTest.class);

    private static final long SENTINEL = 0x0123456789ABCDEFL;

    private static final String[] FLAG_SETS = {
        "noScale", "outline", "grey", "lcd", "lcdV", "mono", "hinted", "hintedRender", "autohint", "light", "vertical"
    };

    @Test
    public void freetypeMatchesTheJniGolden() throws Exception {
        LinuxFontGoldens.captureOrVerify(LinuxFreetypeGoldenTest.class, GOLDEN, header(), true,
                                         LinuxFreetypeGoldenTest::capture, LinuxFreetypeGoldenTest::assertCoverage);
    }

    /** The machine gate passed at least once, so at least one body key was compared ({@link ParityGate}). */
    @AfterAll
    static void theOracleRan() {
        LEDGER.assertOracleRan();
    }

    /**
     * Not vacuous, on any Linux: FreeType opens the Ahem font of the repository, renders a grey glyph whose
     * bitmap is {@code pitch * rows} bytes, and decomposes an outline.
     */
    @Test
    public void freetypeLoadsAndRenders() {
        Path ahem = LinuxFontGoldens.pinned("ahem");
        assumeTrue(Files.isRegularFile(ahem), "the repository font " + ahem + " is not present");
        OSFreetypeShim.ensureLoaded();
        long[] library = new long[1];
        assertEquals(0, OSFreetypeShim.ftInitFreeType(library), "FT_Init_FreeType");
        assertNotEquals(0L, library[0], "FT_Library");
        int[] major = {-1};
        int[] minor = {-1};
        int[] patch = {-1};
        OSFreetypeShim.ftLibraryVersion(library[0], major, minor, patch);
        assertEquals(2, major[0], "FreeType major version");
        long[] face = {SENTINEL};
        assertEquals(0, OSFreetypeShim.ftNewFace(library[0], (ahem + "\0").getBytes(), 0, face), "FT_New_Face");
        assertNotEquals(SENTINEL, face[0], "FT_New_Face out parameter");
        assertEquals(0, OSFreetypeShim.ftSetCharSize(face[0], 0, 16 * 64, 72, 72), "FT_Set_Char_Size");
        boolean rendered = false;
        for (int gid = 0; gid < 1000 && !rendered; gid++) {
            if (OSFreetypeShim.ftLoadGlyph(face[0], gid, flags("grey")) != 0) {
                break;
            }
            long[] slot = OSFreetypeShim.glyphSlot(face[0]);
            assertNotNull(slot, "glyph slot of glyph " + gid);
            if (slot[7] > 0 && slot[8] > 0) {
                assertEquals(OSFreetypeShim.constant("FT_PIXEL_MODE_GRAY"), slot[10], "pixel mode of glyph " + gid);
                byte[] bitmap = OSFreetypeShim.bitmapData(face[0]);
                assertNotNull(bitmap, "bitmap of glyph " + gid);
                assertEquals(slot[7] * slot[9], bitmap.length, "bitmap length of glyph " + gid);
                assertEquals(0, OSFreetypeShim.ftLoadGlyph(face[0], gid, flags("outline")), "outline load");
                Path2D outline = OSFreetypeShim.outlineDecompose(face[0]);
                assertNotNull(outline, "outline of glyph " + gid);
                assertTrue(outline.getNumCommands() > 0, "outline of glyph " + gid + " has no segments");
                rendered = true;
            }
        }
        assertTrue(rendered, "no glyph of " + ahem + " rendered to a non-empty grey bitmap");
        assertEquals(0, OSFreetypeShim.ftDoneFace(face[0]), "FT_Done_Face");
        assertEquals(0, OSFreetypeShim.ftDoneFreeType(library[0]), "FT_Done_FreeType");
    }

    // ---------------------------------------------------------------------------------------------
    // Capture
    // ---------------------------------------------------------------------------------------------

    static void capture(GoldenMap out) throws IOException {
        OSFreetypeShim.ensureLoaded();
        PrismFontFactory factory = PrismFontFactory.getFontFactory();

        long[] libraryOut = new long[1];
        out.put("ft.lib.init.rc", Integer.toString(OSFreetypeShim.ftInitFreeType(libraryOut)));
        long library = libraryOut[0];
        int[] major = {-1};
        int[] minor = {-1};
        int[] patch = {-1};
        OSFreetypeShim.ftLibraryVersion(library, major, minor, patch);
        out.put("ft.lib.version", major[0] + "." + minor[0] + "." + patch[0]);
        for (int filter : new int[] {0, 1, 2, 16, 7, -1}) {
            out.put("ft.lib.lcdFilter." + filter + ".rc",
                    Integer.toString(OSFreetypeShim.ftLibrarySetLcdFilter(library, filter)));
        }
        out.put("ft.lib.lcdFilter.restore.rc", Integer.toString(
                OSFreetypeShim.ftLibrarySetLcdFilter(library, OSFreetypeShim.constant("FT_LCD_FILTER_DEFAULT"))));
        out.put("ft.flags.pango", Boolean.toString(OSFreetypeShim.pangoEnabled()));
        out.put("ft.flags.harfbuzz", Boolean.toString(OSFreetypeShim.harfbuzzEnabled()));
        out.put("ft.factory.class", factory.getClass().getName());
        out.put("ft.factory.lcdTextSupported", Boolean.toString(factory.isLCDTextSupported()));

        Map<String, Long> faces = captureNewFace(out, library);
        long dejavu = faces.get("ok");
        long euro = faces.get("pfa");

        FontResource dejavuResource =
                FontResourceShim.newUncachedFontFile(LinuxFontGoldens.pinned("dejavusans").toString());
        if (dejavuResource == null) {
            fail("the font factory could not open " + LinuxFontGoldens.pinned("dejavusans"));
        }
        CharToGlyphMapper mapper = dejavuResource.getGlyphMapper();
        int numGlyphs = FontResourceShim.numGlyphs(dejavuResource);
        int glyphH = mapper.charToGlyph('H');
        int glyphG = mapper.charToGlyph('g');
        int glyphSpace = mapper.charToGlyph(' ');
        int[] rawGlyphs = {glyphH, glyphG, glyphSpace, 0, 65535, -1, numGlyphs};
        out.put("ft.raw.dejavusans.glyphs", LinuxFontGoldens.joinInts(rawGlyphs));

        captureLoads(out, library, dejavu, euro, rawGlyphs, glyphH, glyphG, glyphSpace);

        out.put("ft.raw.doneFace.freeeuro.rc", Integer.toString(OSFreetypeShim.ftDoneFace(euro)));
        out.put("ft.raw.doneFace.dejavusans.rc", Integer.toString(OSFreetypeShim.ftDoneFace(dejavu)));
        out.put("ft.lib.done.rc", Integer.toString(OSFreetypeShim.ftDoneFreeType(library)));

        captureJavaPath(out);
    }

    /** {@code ft.raw.newFace.<case>} = {@code rc;outChanged}, plus {@code ;doneFaceRc} for faces not kept. */
    private static Map<String, Long> captureNewFace(GoldenMap out, long library) throws IOException {
        Path work = LinuxFontGoldens.workDirectory().resolve("freetype");
        LinuxFontGoldens.deleteTree(work);
        Files.createDirectories(work);
        Path notAFont = work.resolve("notafont.ttf");
        Files.write(notAFont, new byte[256]);
        Path freeeuro = LinuxFontGoldens.pinned("freeeuro");
        Path nonAscii = work.resolve("caf\u00E9\uD83D\uDE00.pfa");
        Files.copy(freeeuro, nonAscii, StandardCopyOption.REPLACE_EXISTING);
        String dejavu = LinuxFontGoldens.pinned("dejavusans").toString();

        record FaceCase(String name, String path, long index) {
        }
        List<FaceCase> cases = List.of(
                new FaceCase("ok", dejavu, 0),
                new FaceCase("indexNeg1", dejavu, -1),
                new FaceCase("index5", dejavu, 5),
                new FaceCase("index65536", dejavu, 0x10000),
                new FaceCase("missing", work.resolve("none.ttf").toString(), 0),
                new FaceCase("directory", "/usr/share/fonts", 0),
                new FaceCase("empty", "", 0),
                new FaceCase("notAFont", notAFont.toString(), 0),
                new FaceCase("pfa", freeeuro.toString(), 0),
                new FaceCase("nonAsciiPath", nonAscii.toString(), 0));
        Map<String, Long> kept = new LinkedHashMap<>();
        for (FaceCase c : cases) {
            long[] face = {SENTINEL};
            int rc = OSFreetypeShim.ftNewFace(library, (c.path() + "\0").getBytes(), c.index(), face);
            boolean changed = face[0] != SENTINEL;
            String value = rc + ";" + changed;
            if (rc == 0 && changed) {
                if (c.name().equals("ok") || c.name().equals("pfa")) {
                    kept.put(c.name(), face[0]);
                } else {
                    value += ";" + OSFreetypeShim.ftDoneFace(face[0]);
                }
            }
            out.put("ft.raw.newFace." + c.name(), value);
        }
        if (kept.size() != 2) {
            fail("FT_New_Face did not open both " + dejavu + " and " + freeeuro + ": " + kept.keySet());
        }
        out.put("ft.raw.doneFace.null.rc", Integer.toString(OSFreetypeShim.ftDoneFace(0)));
        out.put("ft.raw.slot.null", LinuxFontGoldens.joinLongs(OSFreetypeShim.glyphSlot(0)));
        out.put("ft.raw.bitmap.null", LinuxFontGoldens.bitmapSummary(OSFreetypeShim.bitmapData(0)));
        out.put("ft.raw.outline.null", LinuxFontGoldens.outlineSummary(OSFreetypeShim.outlineDecompose(0)));
        return kept;
    }

    static int flags(String set) {
        int noHinting = OSFreetypeShim.constant("FT_LOAD_NO_HINTING");
        int noBitmap = OSFreetypeShim.constant("FT_LOAD_NO_BITMAP");
        int ignoreTransform = OSFreetypeShim.constant("FT_LOAD_IGNORE_TRANSFORM");
        int render = OSFreetypeShim.constant("FT_LOAD_RENDER");
        int rendered = render | noHinting | noBitmap | ignoreTransform;
        return switch (set) {
            case "noScale" -> OSFreetypeShim.constant("FT_LOAD_NO_SCALE");
            case "outline" -> noHinting | noBitmap | ignoreTransform;
            case "grey" -> rendered | OSFreetypeShim.constant("FT_LOAD_TARGET_NORMAL");
            case "lcd" -> rendered | OSFreetypeShim.constant("FT_LOAD_TARGET_LCD");
            case "lcdV" -> rendered | OSFreetypeShim.constant("FT_LOAD_TARGET_LCD_V");
            case "mono" -> rendered | OSFreetypeShim.constant("FT_LOAD_TARGET_MONO");
            case "hinted" -> OSFreetypeShim.constant("FT_LOAD_DEFAULT");
            case "hintedRender" -> render;
            case "autohint" -> render | OSFreetypeShim.constant("FT_LOAD_FORCE_AUTOHINT");
            case "light" -> render | OSFreetypeShim.constant("FT_LOAD_TARGET_LIGHT");
            case "vertical" -> render | OSFreetypeShim.constant("FT_LOAD_VERTICAL_LAYOUT") | noHinting;
            case "transformed" -> noHinting | noBitmap;
            case "transformedGrey" -> render | noHinting | noBitmap | OSFreetypeShim.constant("FT_LOAD_TARGET_NORMAL");
            default -> throw new IllegalArgumentException(set);
        };
    }

    /** {@code rc|slot fields|bitmap} after one {@code FT_Load_Glyph}. */
    private static String load(long face, int glyph, int loadFlags) {
        int rc = OSFreetypeShim.ftLoadGlyph(face, glyph, loadFlags);
        return rc + "|" + LinuxFontGoldens.joinLongs(OSFreetypeShim.glyphSlot(face)) + "|"
                + LinuxFontGoldens.bitmapSummary(OSFreetypeShim.bitmapData(face));
    }

    /** {@code rc|outline summary} after an outline load. */
    private static String outline(long face, int glyph, int loadFlags) {
        int rc = OSFreetypeShim.ftLoadGlyph(face, glyph, loadFlags);
        return rc + "|" + LinuxFontGoldens.outlineSummary(OSFreetypeShim.outlineDecompose(face));
    }

    private static void captureLoads(GoldenMap out, long library, long dejavu, long euro, int[] rawGlyphs,
                                     int glyphH, int glyphG, int glyphSpace) {
        long[][] sizes = {{0, 0, 72}, {0, -768, 72}, {0, 768, 72}, {768, 0, 72}, {0, 768, 0}, {0, 2147483647L, 72}};
        for (long[] size : sizes) {
            out.put("ft.raw.charSize." + size[0] + "." + size[1] + "." + size[2] + ".rc", Integer.toString(
                    OSFreetypeShim.ftSetCharSize(dejavu, size[0], size[1], (int) size[2], (int) size[2])));
        }
        out.put("ft.raw.charSize.restore.rc", Integer.toString(OSFreetypeShim.ftSetCharSize(dejavu, 0, 768, 72, 72)));

        for (String set : FLAG_SETS) {
            for (int glyph : rawGlyphs) {
                out.put("ft.raw.load.dejavusans." + set + "." + LinuxFontGoldens.gid(glyph),
                        load(dejavu, glyph, flags(set)));
            }
        }

        int[] filters = {
            OSFreetypeShim.constant("FT_LCD_FILTER_NONE"), OSFreetypeShim.constant("FT_LCD_FILTER_DEFAULT"),
            OSFreetypeShim.constant("FT_LCD_FILTER_LIGHT")
        };
        for (int filter : filters) {
            int rc = OSFreetypeShim.ftLibrarySetLcdFilter(library, filter);
            for (int glyph : new int[] {glyphG, glyphH}) {
                out.put("ft.raw.lcdFilter." + filter + "." + LinuxFontGoldens.gid(glyph),
                        rc + "|" + load(dejavu, glyph, flags("lcd")));
            }
        }
        OSFreetypeShim.ftLibrarySetLcdFilter(library, OSFreetypeShim.constant("FT_LCD_FILTER_DEFAULT"));

        long[] identity = {65536, 0, 0, 65536};
        long[] rot30 = {56756, -32768, 32768, 56756};
        String[] names = {"nullMatrix", "identity", "rot30", "huge", "zero"};
        long[][] matrices = {null, identity, rot30, {2147483647L, 0, 0, 2147483647L}, {0, 0, 0, 0}};
        for (int m = 0; m < names.length; m++) {
            OSFreetypeShim.ftSetTransform(dejavu, matrices[m], 0, 0);
            for (int glyph : new int[] {glyphH, glyphG}) {
                int rc = OSFreetypeShim.ftLoadGlyph(dejavu, glyph, flags("transformed"));
                String slot = LinuxFontGoldens.joinLongs(OSFreetypeShim.glyphSlot(dejavu));
                String path = LinuxFontGoldens.outlineSummary(OSFreetypeShim.outlineDecompose(dejavu));
                out.put("ft.raw.xform." + names[m] + "." + LinuxFontGoldens.gid(glyph), rc + "|" + slot + "|" + path);
            }
        }
        OSFreetypeShim.ftSetTransform(dejavu, rot30, 0, 0);
        for (int glyph : new int[] {glyphH, glyphG}) {
            out.put("ft.raw.xform.rot30.grey." + LinuxFontGoldens.gid(glyph),
                    load(dejavu, glyph, flags("transformedGrey")));
        }
        OSFreetypeShim.ftSetTransform(dejavu, identity, 0, 0);

        int outlineFlags = flags("outline");
        int noScaleOutline = outlineFlags | flags("noScale");
        for (int glyph : rawGlyphs) {
            out.put("ft.raw.outline.dejavusans.noScale." + LinuxFontGoldens.gid(glyph),
                    outline(dejavu, glyph, noScaleOutline));
        }
        out.put("ft.raw.outline.dejavusans.charSize.rc",
                Integer.toString(OSFreetypeShim.ftSetCharSize(dejavu, 0, 768, 72, 72)));
        for (int glyph : rawGlyphs) {
            out.put("ft.raw.outline.dejavusans.12.0." + LinuxFontGoldens.gid(glyph),
                    outline(dejavu, glyph, outlineFlags));
        }
        for (int glyph : new int[] {glyphH, glyphG, glyphSpace, 65535}) {
            OSFreetypeShim.ftLoadGlyph(dejavu, glyph, outlineFlags);
            out.put("ft.raw.outline.dejavusans.12.0." + LinuxFontGoldens.gid(glyph) + ".dump",
                    LinuxFontGoldens.outlineDump(OSFreetypeShim.outlineDecompose(dejavu)));
        }

        for (int glyph = 0; glyph <= 17; glyph++) {
            out.put("ft.raw.outline.freeeuro.noScale." + LinuxFontGoldens.gid(glyph),
                    outline(euro, glyph, noScaleOutline));
        }
        OSFreetypeShim.ftLoadGlyph(euro, 1, noScaleOutline);
        out.put("ft.raw.outline.freeeuro.noScale.g00001.dump",
                LinuxFontGoldens.outlineDump(OSFreetypeShim.outlineDecompose(euro)));
        out.put("ft.raw.outline.freeeuro.charSize.rc",
                Integer.toString(OSFreetypeShim.ftSetCharSize(euro, 0, 768, 72, 72)));
        for (int glyph = 0; glyph <= 17; glyph++) {
            out.put("ft.raw.outline.freeeuro.12.0." + LinuxFontGoldens.gid(glyph), outline(euro, glyph, outlineFlags));
        }
        out.put("ft.raw.load.freeeuro.lcd.g00001", load(euro, 1, flags("lcd")));
        long[] slot = OSFreetypeShim.glyphSlot(euro);
        out.put("ft.raw.load.freeeuro.lcd.g00001.dump",
                LinuxFontGoldens.bitmapDump(OSFreetypeShim.bitmapData(euro), slot == null ? 0 : (int) slot[9]));
    }

    // ---------------------------------------------------------------------------------------------
    // Java path
    // ---------------------------------------------------------------------------------------------

    /**
     * The fonts of the Java path: three pinned TrueType fonts and {@link #FONT_WITH_CURVES}, an OpenType font
     * with CFF outlines whose cubic segments reach {@code FTFontFile.createGlyphOutline} through the
     * {@code cubic_to} callback. Each one is opened with {@link FontResourceShim#newUncachedFontFile}, not looked
     * up through the factory: a lookup would read and fill the factory's name and file maps, which other test
     * classes in the same JVM share, so the resource (and every row made from it) would depend on the order the
     * classes run in.
     */
    private static final List<String> JAVA_FONTS =
            List.of("dejavusans", "latoregular", "ubunturegular", "fontwithcurves");

    /**
     * The Java-path font with cubic outlines, made from the pinned {@code FontWithFeatures.otf} at every run.
     * That font is the only CFF font in the repository and on the capture machine, and all 53 of its charstrings
     * draw lines only (measured: no curve operator in any of them), so the copy rewrites the charstring of its
     * glyph 52 in place and at the same length: the original width and first move, then a move, one
     * {@code rlineto} and four {@code rrcurveto}s, a rounded shape that FreeType's CFF driver hands to
     * {@code FT_Outline_Decompose} as cubic segments. The glyph is addressed by id: the font's two cmap subtables
     * map the letters to different glyphs, and its CFF charset names glyphs by standard-string order. The
     * charstring is the last one of the {@code CharStrings}
     * index and the last bytes of the {@code CFF } table, so no offset and no other table moves; the sfnt
     * checksums are not maintained, and neither FreeType nor {@code PrismFontFile} checks them. The copy is
     * opened by a path relative to the module directory, so no checkout path enters the golden.
     */
    static final String FONT_WITH_CURVES = "fontwithcurves";

    private static final String FONT_WITH_CURVES_FILE =
            LinuxFontGoldens.WORK_DIRECTORY + "/freetype/FontWithCurves.otf";

    /** The charstring of glyph 52 in {@code FontWithFeatures.otf}: its {@code CFF } table is at 172, 8783 bytes. */
    private static final int CURVES_CHARSTRING_OFFSET = 8795;
    private static final int CURVES_CHARSTRING_LENGTH = 160;

    /** The glyph {@link #writeFontWithCurves} rewrites: the last one, whose charstring ends the {@code CFF } table. */
    private static final int CURVES_GLYPH = 52;

    /**
     * One full outline dump per font whose outlines are not those of DejaVu Sans, of a glyph id or, when that is
     * negative, of the glyph the font's mapper gives {@code character}.
     */
    private record JavaDump(String font, char character, int glyph, float size) {

        int glyph(CharToGlyphMapper mapper) {
            return glyph >= 0 ? glyph : mapper.charToGlyph(character);
        }
    }

    private static final List<JavaDump> JAVA_DUMPS = List.of(
            new JavaDump("latoregular", 'S', -1, 12.5f), new JavaDump(FONT_WITH_CURVES, '\0', CURVES_GLYPH, 12f));

    /**
     * Writes {@link #FONT_WITH_CURVES_FILE} and returns that relative path. Fails unless the pinned font still
     * has the charstring the rewrite was made for: the sfnt directory's {@code CFF } table ends exactly where the
     * charstring ends, and the charstring starts with the same width and move and ends with {@code endchar}.
     */
    static String writeFontWithCurves() throws IOException {
        byte[] font = Files.readAllBytes(LinuxFontGoldens.pinned("fontwithfeaturesotf"));
        byte[] charstring = cubicCharstring();
        int end = CURVES_CHARSTRING_OFFSET + CURVES_CHARSTRING_LENGTH;
        boolean expected = font.length > end && (font[end - 1] & 0xFF) == 14 && cffTableEndsAt(font, end)
                && Arrays.equals(font, CURVES_CHARSTRING_OFFSET, CURVES_CHARSTRING_OFFSET + 16, charstring, 0, 16);
        if (!expected) {
            fail(LinuxFontGoldens.pinned("fontwithfeaturesotf") + " does not hold the charstring that "
                    + FONT_WITH_CURVES + " rewrites at " + CURVES_CHARSTRING_OFFSET);
        }
        System.arraycopy(charstring, 0, font, CURVES_CHARSTRING_OFFSET, CURVES_CHARSTRING_LENGTH);
        Path target = LinuxFontGoldens.moduleDirectory().resolve(FONT_WITH_CURVES_FILE);
        Files.createDirectories(target.getParent());
        Files.write(target, font);
        return FONT_WITH_CURVES_FILE;
    }

    /**
     * The 160-byte Type 2 charstring of the rounded glyph: width 1024 with a move to the origin as in the
     * original, a move to (300, 550), a line 400 to the right (the one two-byte number, which makes the length
     * come out), four cubic curves back to the start, {@code endchar}.
     */
    private static byte[] cubicCharstring() {
        ByteArrayOutputStream charstring = new ByteArrayOutputStream();
        fixed(charstring, 1024);
        fixed(charstring, 0);
        fixed(charstring, 0);
        charstring.write(21);
        fixed(charstring, 300);
        fixed(charstring, 550);
        charstring.write(21);
        charstring.write(248);
        charstring.write(36);
        fixed(charstring, 0);
        charstring.write(5);
        int[][] curves = {
            {110, 0, 90, 90, 0, 110}, {0, 110, -90, 90, -110, 0}, {-100, 100, -300, 100, -400, 0},
            {-100, -100, 100, -300, 0, -400}
        };
        for (int[] curve : curves) {
            for (int value : curve) {
                fixed(charstring, value);
            }
            charstring.write(8);
        }
        charstring.write(14);
        if (charstring.size() != CURVES_CHARSTRING_LENGTH) {
            throw new IllegalStateException("cubic charstring of " + charstring.size() + " bytes");
        }
        return charstring.toByteArray();
    }

    /** A Type 2 charstring number in its five-byte 16.16 form. */
    private static void fixed(ByteArrayOutputStream out, int value) {
        int bits = value << 16;
        out.write(255);
        out.write(bits >>> 24);
        out.write(bits >>> 16);
        out.write(bits >>> 8);
        out.write(bits);
    }

    /** Whether the sfnt directory has a {@code CFF } table ending at {@code end}. */
    private static boolean cffTableEndsAt(byte[] font, int end) {
        ByteBuffer directory = ByteBuffer.wrap(font);
        int tables = directory.getShort(4) & 0xFFFF;
        for (int i = 0; i < tables; i++) {
            int entry = 12 + 16 * i;
            boolean cff = directory.getInt(entry) == 0x43464620;
            if (cff && directory.getInt(entry + 8) + directory.getInt(entry + 12) == end) {
                return true;
            }
        }
        return false;
    }

    private static final float[] MASK_SIZES = {0f, 0.01f, 1f, 7.5f, 12f, 12.5f, 48f};

    private static final float[] OUTLINE_SIZES = {0f, 0.5f, 1f, 12f, 12.5f, 100f, 1000f, 1e7f};

    private static final String JAVA_CHARACTERS = "Hg0fi@\u00E9\u03A9\u0416\u05E9\u0645\u65E5";

    private static final String TRANSFORM_CHARACTERS = "Hg \u00E9\u05E9\u0645";

    private static void captureJavaPath(GoldenMap out) throws IOException {
        Map<String, BaseTransform> transforms = new LinkedHashMap<>();
        transforms.put("translate", BaseTransform.getTranslateInstance(0.5, 0.25));
        transforms.put("scale21", BaseTransform.getScaleInstance(2, 1));
        transforms.put("rot30", BaseTransform.getRotateInstance(Math.toRadians(30), 0, 0));
        transforms.put("rot90", new Affine2D(0, 1, -1, 0, 0, 0));
        transforms.put("shear", new Affine2D(1, 0, 0.3, 1, 0, 0));
        transforms.put("flipX", BaseTransform.getScaleInstance(-1, 1));
        transforms.put("scaleTiny", BaseTransform.getScaleInstance(1e-3, 1));
        transforms.put("scaleZero", BaseTransform.getScaleInstance(0, 1));
        transforms.put("scaleNaN", BaseTransform.getScaleInstance(Double.NaN, 1));

        String fontWithCurves = writeFontWithCurves();
        out.put("ft.java.fontwithcurves.sha256",
                FontGoldens.sha256Hex(LinuxFontGoldens.moduleDirectory().resolve(fontWithCurves)));
        RetainedGlyphs retained = new RetainedGlyphs();
        for (String key : JAVA_FONTS) {
            String fontFile = key.equals(FONT_WITH_CURVES) ? fontWithCurves : LinuxFontGoldens.pinned(key).toString();
            FontResource resource = FontResourceShim.newUncachedFontFile(fontFile);
            if (resource == null) {
                out.put("ft.java.resource." + key, FontGoldens.NULL);
                continue;
            }
            int numGlyphs = FontResourceShim.numGlyphs(resource);
            int unitsPerEm = resource instanceof PrismFontFile file ? file.getUnitsPerEm() : -1;
            int fontCount = resource instanceof PrismFontFile file ? file.getFontCount() : -1;
            out.put("ft.java.resource." + key, FontGoldens.joinList(List.of(resource.getClass().getName(),
                    LinuxFontGoldens.safeText(resource.getFullName()),
                    LinuxFontGoldens.safeText(resource.getFamilyName()),
                    LinuxFontGoldens.safeText(resource.getStyleName()),
                    LinuxFontGoldens.safeText(resource.getFileName()),
                    Integer.toString(fontCount), Integer.toString(unitsPerEm), Integer.toString(numGlyphs))));

            CharToGlyphMapper mapper = resource.getGlyphMapper();
            TreeSet<Integer> glyphs = new TreeSet<>();
            JAVA_CHARACTERS.codePoints().forEach(cp -> glyphs.add(mapper.charToGlyph(cp)));
            glyphs.add(0);
            glyphs.add(mapper.charToGlyph(' '));
            glyphs.add(numGlyphs - 1);
            glyphs.add(numGlyphs);

            for (int glyph : glyphs) {
                float[] bbox = resource.getGlyphBoundingBox(glyph, unitsPerEm, new float[4]);
                out.put("ft.java.bbox." + key + "." + LinuxFontGoldens.gid(glyph),
                        LinuxFontGoldens.joinFloats(bbox));
            }
            for (float size : MASK_SIZES) {
                for (int aa : new int[] {FontResource.AA_GREYSCALE, FontResource.AA_LCD}) {
                    putMasks(out, retained, resource, key, size, aa, "identity", BaseTransform.IDENTITY_TRANSFORM,
                             glyphs);
                }
            }
            if (key.equals("dejavusans")) {
                TreeSet<Integer> transformGlyphs = new TreeSet<>();
                TRANSFORM_CHARACTERS.codePoints().forEach(cp -> transformGlyphs.add(mapper.charToGlyph(cp)));
                for (Map.Entry<String, BaseTransform> tx : transforms.entrySet()) {
                    for (int aa : new int[] {FontResource.AA_GREYSCALE, FontResource.AA_LCD}) {
                        putMasks(out, retained, resource, key, 12f, aa, tx.getKey(), tx.getValue(), transformGlyphs);
                    }
                }
                TreeSet<Integer> g = new TreeSet<>(List.of(mapper.charToGlyph('g')));
                putMasks(out, retained, resource, key, 80f, FontResource.AA_GREYSCALE, "identity",
                         BaseTransform.IDENTITY_TRANSFORM, g);
                putMasks(out, retained, resource, key, 81f, FontResource.AA_GREYSCALE, "identity",
                         BaseTransform.IDENTITY_TRANSFORM, g);
                int glyphG = mapper.charToGlyph('g');
                for (int aa : new int[] {FontResource.AA_GREYSCALE, FontResource.AA_LCD}) {
                    Glyph glyph = resource.getStrike(12f, BaseTransform.IDENTITY_TRANSFORM, aa).getGlyph(glyphG);
                    out.put(maskKey(key, 12f, aa, "identity", glyphG) + ".dump",
                            LinuxFontGoldens.bitmapDump(glyph.getPixelData(), glyph.getWidth()));
                }
                FontStrike rotated = resource.getStrike(12f, transforms.get("rot30"), FontResource.AA_GREYSCALE);
                for (int glyph : transformGlyphs) {
                    out.put("ft.java.outline." + key + ".12.0.rot30." + LinuxFontGoldens.gid(glyph),
                            LinuxFontGoldens.shapeSummary(rotated.getGlyph(glyph).getShape()));
                }
            }
            for (int glyph : glyphs) {
                for (float size : OUTLINE_SIZES) {
                    FontStrike strike = resource.getStrike(size, BaseTransform.IDENTITY_TRANSFORM,
                                                           FontResource.AA_GREYSCALE);
                    out.put("ft.java.outline." + key + "." + size + "." + LinuxFontGoldens.gid(glyph),
                            LinuxFontGoldens.shapeSummary(strike.getGlyph(glyph).getShape()));
                }
            }
            for (JavaDump dump : JAVA_DUMPS) {
                if (!dump.font().equals(key)) {
                    continue;
                }
                int glyph = dump.glyph(mapper);
                Shape shape = resource.getStrike(dump.size(), BaseTransform.IDENTITY_TRANSFORM,
                                                 FontResource.AA_GREYSCALE).getGlyph(glyph).getShape();
                out.put("ft.java.outline." + key + "." + dump.size() + "." + LinuxFontGoldens.gid(glyph) + ".dump",
                        shape instanceof Path2D path ? LinuxFontGoldens.outlineDump(path) : FontGoldens.NULL);
            }
        }
        out.put("ft.java.lateReread.masks", Integer.toString(retained.assertUnchanged()));
    }

    /**
     * Every glyph a mask row was recorded from, with its strike held strongly until the end of the Java path.
     * Production keeps what the binding returned inside the glyph ({@code FTGlyph.bitmap}, and the
     * {@code getBitmapData} array itself when the pitch equals the width) and reads it again long after later
     * loads on the same face. A binding that reused one slot mirror or one scratch array per face would give
     * every earlier glyph the size and pixels of the last one loaded, while each row, read right after its own
     * load, still matched. Holding the strikes also keeps the late {@code .dump} rows from depending on when the
     * collector clears the font's weak strike cache.
     */
    private static final class RetainedGlyphs {

        private record Retained(String key, Glyph glyph, String firstRead) {
        }

        private final List<FontStrike> strikes = new ArrayList<>();
        private final List<Retained> glyphs = new ArrayList<>();

        void retain(FontStrike strike) {
            strikes.add(strike);
        }

        void retain(String key, Glyph glyph) {
            glyphs.add(new Retained(key, glyph, sizeAndPixels(glyph)));
        }

        /** Reads every retained glyph again; fails naming each one that changed. Returns the number re-read. */
        int assertUnchanged() {
            List<String> changed = new ArrayList<>();
            for (Retained retained : glyphs) {
                String now = sizeAndPixels(retained.glyph());
                if (!now.equals(retained.firstRead())) {
                    changed.add(retained.key() + ": first=" + retained.firstRead() + " now=" + now);
                }
            }
            if (!changed.isEmpty()) {
                int shown = Math.min(20, changed.size());
                fail(changed.size() + " of " + glyphs.size() + " glyph(s) (" + strikes.size() + " strikes held) read"
                        + " a different width, height or pixel data at the end of the Java path than right after"
                        + " their own load: the binding shares state between glyphs. First " + shown + ": "
                        + changed.subList(0, shown));
            }
            return glyphs.size();
        }

        /** {@code width;height;length;sha256} of the pixel data, or {@code width;height;#null}. */
        static String sizeAndPixels(Glyph glyph) {
            return glyph.getWidth() + ";" + glyph.getHeight() + ";"
                    + LinuxFontGoldens.bitmapSummary(glyph.getPixelData());
        }
    }

    private static String aaName(int aa) {
        return aa == FontResource.AA_LCD ? "lcd" : "grey";
    }

    private static String maskKey(String font, float size, int aa, String tx, int glyph) {
        return "ft.java.mask." + font + "." + size + "." + aaName(aa) + "." + tx + "." + LinuxFontGoldens.gid(glyph);
    }

    private static void putMasks(GoldenMap out, RetainedGlyphs retained, FontResource resource, String font,
                                 float size, int aa, String tx, BaseTransform transform, TreeSet<Integer> glyphs) {
        FontStrike strike = resource.getStrike(size, transform, aa);
        retained.retain(strike);
        out.put("ft.java.strike." + font + "." + size + "." + aaName(aa) + "." + tx,
                strike.getAAMode() + ";" + strike.drawAsShapes());
        for (int code : glyphs) {
            Glyph glyph = strike.getGlyph(code);
            byte[] pixels = glyph.getPixelData();
            String value = glyph.getWidth() + ";" + glyph.getHeight() + ";" + glyph.getOriginX() + ";"
                    + glyph.getOriginY() + ";" + Float.toString(glyph.getPixelXAdvance()) + ";"
                    + Float.toString(glyph.getPixelYAdvance()) + ";" + Float.toString(glyph.getAdvance()) + ";"
                    + glyph.isLCDGlyph() + ";" + LinuxFontGoldens.bitmapSummary(pixels);
            out.put(maskKey(font, size, aa, tx, code), pixels == null ? value + ";" + FontGoldens.NULL : value);
            retained.retain(maskKey(font, size, aa, tx, code), glyph);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Coverage
    // ---------------------------------------------------------------------------------------------

    /**
     * The golden can tell the right FreeType binding from the wrong ones the audit named: a lost {@code -0.0f}, a
     * missing conic or cubic callback (the cubic one on the raw path and through
     * {@code FTFontFile.createGlyphOutline}), a zero-filled out parameter, a bitmap copied as
     * {@code width * rows}, a hard-coded LCD filter, and the whitespace and error mask paths.
     */
    static void assertCoverage(Map<String, String> values) {
        List<String> missing = new ArrayList<>();
        boolean negativeZero = false;
        boolean quad = false;
        boolean cubic = false;
        boolean javaCubic = false;
        boolean failedOutUntouched = false;
        boolean lcdPitch = false;
        boolean monoPitch = false;
        boolean lcdVRows = false;
        boolean javaLcd = false;
        boolean javaEmpty = false;
        boolean javaNull = false;
        int javaMasks = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.startsWith("ft.raw.outline.") && key.endsWith(".dump") && !value.equals(FontGoldens.NULL)) {
                String[] parts = value.split("\\|", -1);
                for (String type : parts[0].split(";")) {
                    quad |= type.equals("2");
                    cubic |= type.equals("3");
                }
                for (String coordinate : parts[1].split(";")) {
                    negativeZero |= coordinate.equals("-0.0");
                }
            } else if (key.startsWith("ft.java.outline.") && key.endsWith(".dump") && !value.equals(FontGoldens.NULL)) {
                for (String type : value.split("\\|", -1)[0].split(";")) {
                    javaCubic |= type.equals("3");
                }
            } else if (key.startsWith("ft.raw.newFace.")) {
                String[] parts = value.split(";");
                failedOutUntouched |= !parts[0].equals("0") && parts[1].equals("false");
            } else if (key.startsWith("ft.raw.load.dejavusans.lcd.")) {
                long[] slot = slot(value);
                lcdPitch |= slot != null && slot[8] > 0 && slot[9] != slot[8];
            } else if (key.startsWith("ft.raw.load.dejavusans.mono.")) {
                long[] slot = slot(value);
                monoPitch |= slot != null && slot[8] > 0 && slot[9] < slot[8];
            } else if (key.startsWith("ft.raw.load.dejavusans.lcdV.")) {
                long[] slot = slot(value);
                long[] grey = slot(values.getOrDefault(key.replace(".lcdV.", ".grey."), FontGoldens.NULL));
                lcdVRows |= slot != null && grey != null && grey[7] > 0 && slot[7] >= 3 * grey[7];
            } else if (key.startsWith("ft.java.mask.") && !key.endsWith(".dump")) {
                String[] parts = value.split(";");
                javaLcd |= parts[7].equals("true");
                javaEmpty |= parts[8].equals("0");
                javaNull |= parts[8].equals(FontGoldens.NULL);
                javaMasks++;
            }
        }
        check(missing, negativeZero, "a raw outline dump with a -0.0 coordinate (y == 0 negated)");
        check(missing, quad, "a raw outline dump with a SEG_QUADTO (conic_to)");
        check(missing, cubic, "a raw outline dump with a SEG_CUBICTO (cubic_to)");
        check(missing, javaCubic, "a Java-path outline dump with a SEG_CUBICTO (cubic_to through"
                + " FTFontFile.createGlyphOutline)");
        check(missing, failedOutUntouched, "an FT_New_Face failure that left the out parameter untouched");
        check(missing, lcdPitch, "a raw LCD bitmap whose pitch differs from its width");
        check(missing, monoPitch, "a raw mono bitmap whose pitch is less than its width");
        check(missing, lcdVRows, "a raw LCD_V bitmap with at least three times the grey rows");
        check(missing, javaLcd, "a Java mask rendered as LCD");
        check(missing, javaEmpty, "a Java mask of length 0 (whitespace)");
        check(missing, javaNull, "a Java mask with null pixel data (failed glyph load)");
        check(missing, !"0".equals(values.get("ft.lib.lcdFilter.16.rc")),
              "FT_Library_SetLcdFilter(FT_LCD_FILTER_LEGACY) failing");
        check(missing, javaMasks > 0 && Integer.toString(javaMasks).equals(values.get("ft.java.lateReread.masks")),
              "a late re-read of every Java mask glyph (" + javaMasks + " mask rows, re-read count "
                      + values.get("ft.java.lateReread.masks") + ")");
        if (!missing.isEmpty()) {
            List<String> resources = new ArrayList<>();
            for (String font : JAVA_FONTS) {
                resources.add(font + "=" + values.get("ft.java.resource." + font));
            }
            List<String> dumps = new ArrayList<>();
            for (Map.Entry<String, String> entry : values.entrySet()) {
                if (entry.getKey().startsWith("ft.java.outline.") && entry.getKey().endsWith(".dump")) {
                    String types = entry.getValue().split("\\|", -1)[0];
                    dumps.add(entry.getKey() + "=" + types.substring(0, Math.min(60, types.length())));
                }
            }
            fail(GOLDEN + " is not discriminating; it lacks: " + missing + "; Java-path resources: " + resources
                    + "; Java-path dumps: " + dumps);
        }
    }

    private static long[] slot(String value) {
        String[] parts = value.split("\\|", -1);
        if (parts.length < 2 || parts[1].equals(FontGoldens.NULL)) {
            return null;
        }
        String[] fields = parts[1].split(";");
        long[] slot = new long[fields.length];
        for (int i = 0; i < fields.length; i++) {
            slot[i] = Long.parseLong(fields[i]);
        }
        return slot;
    }

    static void check(List<String> missing, boolean present, String what) {
        if (!present) {
            missing.add(what);
        }
    }

    private static List<String> header() {
        return LinuxFontGoldens.header("Linux FreeType golden (OSFreetype / freetype.c, FTFontFile, FTGlyph)",
                List.of(
                "  ft.lib.*                   test-owned FT_Library: version, SetLcdFilter codes",
                "  ft.flags.*                 isPangoEnabled / isHarfbuzzEnabled",
                "  ft.raw.newFace.<case>      FT_New_Face rc;outParameterChanged[;FT_Done_Face rc]",
                "  ft.raw.load.<font>.<flags>.g<gid>  FT_Load_Glyph rc|slot: metrics.width;height;horiBearingX;",
                "                             horiBearingY;linearHoriAdvance;advance.x;advance.y;bitmap.rows;width;",
                "                             pitch;pixel_mode;bitmap_left;bitmap_top|bitmap length;sha256",
                "  ft.raw.lcdFilter.<f>.g<gid> SetLcdFilter rc|LCD load as above",
                "  ft.raw.xform.<m>.g<gid>    FT_Set_Transform then load rc|slot|outline",
                "  ft.raw.outline.*           FT_Outline_Decompose rc|numCommands;commandsLength;coordinates;",
                "                             coordsLength;winding;sha256 of '<type> <%08x raw float bits>' lines",
                "  *.dump                     full dump: types|coordinates (Float.toString) or hex rows / by pitch",
                "  ft.java.bbox.*             FontResource.getGlyphBoundingBox at unitsPerEm",
                "  ft.java.strike.*           aaMode;drawAsShapes",
                "  ft.java.mask.<font>.<size>.<aa>.<tx>.g<gid>  width;height;originX;originY;pixelXAdvance;",
                "                             pixelYAdvance;advance;lcd;length;sha256",
                "  ft.java.lateReread.masks   mask glyphs read again, unchanged, after the whole Java path",
                "  ft.java.outline.*          glyph shape: outline summary;minX;minY;maxX;maxY",
                "  ft.java.fontwithcurves.*   the CFF font made from FontWithFeatures.otf with glyph 52 cubic"));
    }
}
