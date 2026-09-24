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

package test.com.sun.javafx.font.directwrite;

import com.sun.javafx.font.CharToGlyphMapper;
import com.sun.javafx.font.CompositeFontResource;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.font.directwrite.DWNativeShim;
import com.sun.javafx.font.directwrite.DWNativeShim.Api;
import com.sun.javafx.font.directwrite.DWNativeShim.GlyphRun;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.com.sun.javafx.font.directwrite.DWFaceFixture.CODE_POINTS;
import static test.com.sun.javafx.font.directwrite.DWFaceFixture.FONT_FACE_TYPE_TRUETYPE;
import static test.com.sun.javafx.font.directwrite.DWFaceFixture.fontFace;
import static test.com.sun.javafx.font.directwrite.DWFaceFixture.numGlyphsFromFile;
import static test.com.sun.javafx.font.directwrite.DWFaceFixture.openTypeTag;
import static test.com.sun.javafx.font.directwrite.DWFaceFixture.outlineText;
import static test.com.sun.javafx.font.directwrite.DWFaceFixture.requireDirectWrite;
import static test.com.sun.javafx.font.directwrite.DWFaceFixture.tableFromFile;
import static test.com.sun.javafx.font.directwrite.DWFaceFixture.unitsPerEmFromFile;
import static test.com.sun.javafx.font.directwrite.DWFaceFixture.wide;
import static test.com.sun.javafx.font.directwrite.DWFaceFixture.windowsFont;

/**
 * The runtime proof of the {@code IDWriteFontFace} half of {@code DWNative}: that the twelve struct
 * layouts and slot numbers the font-face bindings add are the ones the Windows SDK describes, and that the
 * geometry sink - a COM object Java synthesizes out of upcall stubs - behaves like the C++ class it
 * replaces.
 * <p>
 * Slots 3 to 14 of one interface are involved and only two of them, 10 and 14, were ever JNI natives,
 * so an off-by-one in the middle of that range would not show up in the parity test at all. Six of the
 * other slots are therefore bound and checked here against values read out of the font file by this
 * test, with no DirectWrite involved: the glyph count against {@code maxp.numGlyphs} (slot 9), the
 * design units against {@code head.unitsPerEm} (slot 8), three whole tables byte for byte (slots 12
 * and 13), the cmap mapping against the Java parser {@code OpenTypeGlyphMapper} (slot 11), the file
 * format (slot 3) and the symbol flag on a font that has it set and one that does not (slot 7). Those
 * are six independent answers, which no permutation of the range can produce by accident.
 * <p>
 * Parity against the {@code OS} natives was a separate test class, deleted in the change set that
 * flipped the peers onto this facade: from that point both sides of the comparison were this code.
 * <p>
 * Line numbers into {@code directwrite.cpp} refer to it at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-font/directwrite.cpp}).
 */
@EnabledOnOs(OS.WINDOWS)
public class DWFontFaceTest {

    private static final Api API = DWNativeShim.ffm();

    private static long factory;
    private static long collection;
    private static long arial;

    @BeforeAll
    static void createFactory() {
        requireDirectWrite();
        factory = API.createFactory(DWNativeShim.factoryTypeShared());
        assertNotEquals(0L, factory, "DWriteCreateFactory(DWRITE_FACTORY_TYPE_SHARED)");
        collection = API.getSystemFontCollection(factory, false);
        assertNotEquals(0L, collection, "IDWriteFactory::GetSystemFontCollection");
        arial = fontFace(API, collection, "Arial");
    }

    @AfterAll
    static void releaseFactory() {
        if (arial != 0) {
            API.release(arial);
            arial = 0;
        }
        if (collection != 0) {
            API.release(collection);
            collection = 0;
        }
        if (factory != 0) {
            API.release(factory);
            factory = 0;
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Struct layouts
     * ------------------------------------------------------------------------------------------- */

    /**
     * The sizes and offsets of every struct that crosses in these bindings, against the Windows SDK
     * headers. There is no way to ask C for a {@code sizeof} here without adding C, which this port
     * refuses to do - so these are pinned against the documented numbers, and then validated
     * behaviourally by every other test in this class and by the parity test, which read real values
     * out of real structs at exactly these offsets.
     */
    @Test
    public void structLayoutsMatchTheWindowsSdk() {
        assertEquals(28, DWNativeShim.layoutByteSize("DWRITE_GLYPH_METRICS"), "dwrite.h:551");
        assertEquals(0, DWNativeShim.layoutOffset("DWRITE_GLYPH_METRICS", "leftSideBearing"));
        assertEquals(4, DWNativeShim.layoutOffset("DWRITE_GLYPH_METRICS", "advanceWidth"));
        assertEquals(8, DWNativeShim.layoutOffset("DWRITE_GLYPH_METRICS", "rightSideBearing"));
        assertEquals(12, DWNativeShim.layoutOffset("DWRITE_GLYPH_METRICS", "topSideBearing"));
        assertEquals(16, DWNativeShim.layoutOffset("DWRITE_GLYPH_METRICS", "advanceHeight"));
        assertEquals(20, DWNativeShim.layoutOffset("DWRITE_GLYPH_METRICS", "bottomSideBearing"));
        assertEquals(24, DWNativeShim.layoutOffset("DWRITE_GLYPH_METRICS", "verticalOriginY"));

        assertEquals(20, DWNativeShim.layoutByteSize("DWRITE_FONT_METRICS"), "dwrite.h:476, ten UINT16");
        assertEquals(0, DWNativeShim.layoutOffset("DWRITE_FONT_METRICS", "designUnitsPerEm"));
        assertEquals(6, DWNativeShim.layoutOffset("DWRITE_FONT_METRICS", "lineGap"));
        assertEquals(18, DWNativeShim.layoutOffset("DWRITE_FONT_METRICS", "strikethroughThickness"));

        assertEquals(8, DWNativeShim.layoutByteSize("DWRITE_GLYPH_OFFSET"), "dwrite.h:604");
        assertEquals(0, DWNativeShim.layoutOffset("DWRITE_GLYPH_OFFSET", "advanceOffset"));
        assertEquals(4, DWNativeShim.layoutOffset("DWRITE_GLYPH_OFFSET", "ascenderOffset"));

        assertEquals(48, DWNativeShim.layoutByteSize("DWRITE_GLYPH_RUN"), "dwrite.h:3038, x64");
        assertEquals(0, DWNativeShim.layoutOffset("DWRITE_GLYPH_RUN", "fontFace"));
        assertEquals(8, DWNativeShim.layoutOffset("DWRITE_GLYPH_RUN", "fontEmSize"));
        assertEquals(12, DWNativeShim.layoutOffset("DWRITE_GLYPH_RUN", "glyphCount"));
        assertEquals(16, DWNativeShim.layoutOffset("DWRITE_GLYPH_RUN", "glyphIndices"));
        assertEquals(24, DWNativeShim.layoutOffset("DWRITE_GLYPH_RUN", "glyphAdvances"));
        assertEquals(32, DWNativeShim.layoutOffset("DWRITE_GLYPH_RUN", "glyphOffsets"));
        assertEquals(40, DWNativeShim.layoutOffset("DWRITE_GLYPH_RUN", "isSideways"));
        assertEquals(44, DWNativeShim.layoutOffset("DWRITE_GLYPH_RUN", "bidiLevel"));

        assertEquals(24, DWNativeShim.layoutByteSize("DWRITE_MATRIX"), "dwrite.h:971");
        assertEquals(20, DWNativeShim.layoutOffset("DWRITE_MATRIX", "dy"));

        assertEquals(16, DWNativeShim.layoutByteSize("RECT"), "windef.h");
        assertEquals(12, DWNativeShim.layoutOffset("RECT", "bottom"));

        assertEquals(8, DWNativeShim.layoutByteSize("D2D1_POINT_2F"), "dcommon.h:183");
        assertEquals(4, DWNativeShim.layoutOffset("D2D1_POINT_2F", "y"));

        assertEquals(24, DWNativeShim.layoutByteSize("D2D1_BEZIER_SEGMENT"), "d2d1.h:563");
        assertEquals(8, DWNativeShim.layoutOffset("D2D1_BEZIER_SEGMENT", "point2"));
        assertEquals(16, DWNativeShim.layoutOffset("D2D1_BEZIER_SEGMENT", "point3"));

        assertEquals(16, DWNativeShim.layoutByteSize("GUID"), "guiddef.h");
    }

    /**
     * {@code D2D1_POINT_2F} is exactly 8 bytes, which is why Microsoft x64 passes it by value in one
     * integer register, and {@code D2D1_BEZIER_SEGMENT} is 24, which is why it never crosses by value
     * at all. That difference is the whole reason {@code BeginFigure} needs the layout in its
     * descriptor and {@code AddBeziers} needs a pointer, so it is worth one assertion of its own.
     */
    @Test
    public void onlyTheEightBytePointCanBePassedInARegister() {
        long point = DWNativeShim.layoutByteSize("D2D1_POINT_2F");
        long bezier = DWNativeShim.layoutByteSize("D2D1_BEZIER_SEGMENT");
        assertTrue(point == 1 || point == 2 || point == 4 || point == 8,
                "an aggregate is passed in a register only at 1, 2, 4 or 8 bytes");
        assertFalse(bezier == 1 || bezier == 2 || bezier == 4 || bezier == 8,
                "D2D1_BEZIER_SEGMENT must not be a register-sized aggregate");
    }

    /* ---------------------------------------------------------------------------------------------
     * The slots, against the font file
     * ------------------------------------------------------------------------------------------- */

    /** {@code IDWriteFontFace::GetType}, slot 3: arial.ttf is a TrueType file, not CFF or a collection. */
    @Test
    public void getTypeSaysTrueType() {
        assertEquals(FONT_FACE_TYPE_TRUETYPE, DWNativeShim.fontFaceType(arial),
                "DWRITE_FONT_FACE_TYPE_TRUETYPE");
    }

    /** {@code IDWriteFontFace::GetIndex}, slot 5, and {@code GetSimulations}, slot 6, on a single-face file. */
    @Test
    public void getIndexAndSimulationsAreZeroForASingleFaceFile() {
        assertEquals(0, DWNativeShim.fontFaceIndex(arial), "arial.ttf holds one face");
        assertEquals(0, DWNativeShim.fontFaceSimulations(arial), "DWRITE_FONT_SIMULATIONS_NONE");
    }

    /**
     * {@code IDWriteFontFace::IsSymbolFont}, slot 7, told apart from its neighbours by a font that
     * answers TRUE and one that answers FALSE. Every other slot in 3..7 returns the same value for
     * both fonts, so only slot 7 can produce this pair.
     */
    @Test
    public void isSymbolFontDistinguishesWingdingsFromArial() {
        assertFalse(DWNativeShim.isSymbolFont(arial), "Arial is not a symbol font");
        long wingdings = fontFace(API, collection, "Wingdings");
        try {
            assertTrue(DWNativeShim.isSymbolFont(wingdings), "Wingdings is a symbol font");
            assertEquals(FONT_FACE_TYPE_TRUETYPE, DWNativeShim.fontFaceType(wingdings),
                    "and slot 3 still says TrueType, so slots 3 and 7 are not the same slot");
        } finally {
            API.release(wingdings);
        }
    }

    /**
     * {@code IDWriteFontFace::GetGlyphCount}, slot 9, against {@code maxp.numGlyphs} read out of
     * arial.ttf by this test. Arial has a few thousand glyphs, so this is a number no neighbouring
     * slot returns by chance - and it also proves the {@code UINT16} return is widened rather than
     * sign-extended.
     */
    @Test
    public void glyphCountMatchesMaxp() {
        int fromFile = numGlyphsFromFile(windowsFont("arial.ttf"));
        assertTrue(fromFile > 1000, "Arial has thousands of glyphs");
        assertEquals(fromFile, DWNativeShim.glyphCount(arial), "IDWriteFontFace::GetGlyphCount");
    }

    /**
     * {@code IDWriteFontFace::GetMetrics}, slot 8, whose {@code designUnitsPerEm} must be
     * {@code head.unitsPerEm}. This is also the one method of these bindings whose name suggests a struct
     * returned by value: it is declared {@code STDMETHOD_(void, GetMetrics)(DWRITE_FONT_METRICS*)},
     * an out-parameter, and reading the right value at offset 0 of a 20-byte block is the proof that
     * it was decoded as one.
     * <p>
     * Only {@code designUnitsPerEm} is asserted exactly. Which sfnt fields DirectWrite draws the other
     * nine from is not contractual, so they are checked for sign and plausibility instead, and for
     * being different between two fonts - a constant or a misread block would fail that.
     */
    @Test
    public void fontMetricsDesignUnitsMatchHead() {
        int unitsPerEm = unitsPerEmFromFile(windowsFont("arial.ttf"));
        int[] metrics = DWNativeShim.fontFaceMetrics(arial);
        assertEquals(10, metrics.length, "DWRITE_FONT_METRICS has ten fields");
        assertEquals(unitsPerEm, metrics[0], "designUnitsPerEm must be head.unitsPerEm");
        assertTrue(metrics[1] > 0 && metrics[1] < 4 * unitsPerEm, "ascent");
        assertTrue(metrics[2] > 0 && metrics[2] < 4 * unitsPerEm, "descent");
        assertTrue(metrics[1] + metrics[2] >= unitsPerEm, "ascent + descent covers the em");
        assertTrue(metrics[4] > 0 && metrics[4] <= unitsPerEm, "capHeight");
        assertTrue(metrics[5] > 0 && metrics[5] <= metrics[4], "xHeight is at most capHeight");
        assertTrue(metrics[6] < 0, "underlinePosition is below the baseline, so it is a signed field");
        assertTrue(metrics[7] > 0, "underlineThickness");

        long consolas = fontFace(API, collection, "Consolas");
        try {
            assertFalse(Arrays.equals(metrics, DWNativeShim.fontFaceMetrics(consolas)),
                    "two different fonts must not report identical metrics");
        } finally {
            API.release(consolas);
        }
    }

    /**
     * {@code IDWriteFontFace::TryGetFontTable} and {@code ReleaseFontTable}, slots 12 and 13, against
     * the bytes of arial.ttf. Three tables of very different sizes are compared in full; a tag no font
     * carries answers {@code null} rather than an empty array.
     */
    @Test
    public void fontTableBytesMatchTheFile() {
        Path file = windowsFont("arial.ttf");
        for (String tag : new String[] { "head", "maxp", "cmap", "hhea" }) {
            byte[] fromFile = tableFromFile(file, tag);
            assertNotNull(fromFile, () -> "arial.ttf must carry " + tag);
            byte[] fromFace = DWNativeShim.fontTable(arial, openTypeTag(tag));
            assertNotNull(fromFace, () -> "TryGetFontTable(" + tag + ")");
            assertEquals(fromFile.length, fromFace.length, () -> "length of " + tag);
            assertArrayEquals(fromFile, fromFace, () -> "bytes of " + tag);
        }
        assertNull(DWNativeShim.fontTable(arial, openTypeTag("zzzz")),
                "a tag the font does not carry must answer null, not an empty table");
    }

    /**
     * The same table twice must come back equal and in two distinct arrays: {@code TryGetFontTable}
     * hands out a pointer into the DirectWrite mapping that is only valid until
     * {@code ReleaseFontTable}, so the facade has to copy, and a second call after a release must
     * still work - which is also the check that the release did not free the mapping.
     */
    @Test
    public void repeatedTableReadsAreIndependentCopies() {
        byte[] first = DWNativeShim.fontTable(arial, openTypeTag("head"));
        byte[] second = DWNativeShim.fontTable(arial, openTypeTag("head"));
        assertNotNull(first);
        assertNotNull(second);
        assertArrayEquals(first, second);
        assertNotSame(first, second, "the two reads must be distinct arrays");
    }

    /**
     * {@code IDWriteFontFace::GetGlyphIndices}, slot 11, against {@code OpenTypeGlyphMapper} - the
     * Java cmap parser already in this tree, which reads the same file and shares no code with
     * DirectWrite. The code points cover ASCII, two non-Latin scripts and two supplementary characters
     * that are surrogate pairs in UTF-16 and single {@code UINT32}s here.
     */
    @Test
    public void glyphIndicesMatchTheJavaCmapParser() {
        CharToGlyphMapper mapper = glyphMapper("Arial");
        short[] indices = DWNativeShim.glyphIndices(arial, CODE_POINTS);
        assertNotNull(indices, "IDWriteFontFace::GetGlyphIndices");
        assertEquals(CODE_POINTS.length, indices.length);
        int mapped = 0;
        for (int i = 0; i < CODE_POINTS.length; i++) {
            int codePoint = CODE_POINTS[i];
            int expected = mapper.charToGlyph(codePoint);
            assertEquals(expected, indices[i] & 0xFFFF,
                    () -> String.format("glyph for U+%04X", codePoint));
            if (expected != 0) {
                mapped++;
            }
        }
        assertTrue(mapped >= 14, "Arial must cover the ASCII, Cyrillic and Greek code points");
        assertEquals(2, Character.charCount(CODE_POINTS[CODE_POINTS.length - 1]),
                "the last two code points really are surrogate pairs in UTF-16");
    }

    /**
     * A supplementary code point a font does actually cover, so that the surrogate-pair case is not
     * only the "both say missing" agreement. Segoe UI Emoji maps U+1F600 through a cmap format 12
     * subtable, which is the only kind that can carry it.
     */
    @Test
    public void glyphIndicesCoverASupplementaryCodePoint() {
        long emoji = fontFace(API, collection, "Segoe UI Emoji");
        try {
            int[] codePoints = { 0x1F600, 0x1F30D };
            short[] indices = DWNativeShim.glyphIndices(emoji, codePoints);
            assertNotNull(indices);
            CharToGlyphMapper mapper = glyphMapper("Segoe UI Emoji");
            for (int i = 0; i < codePoints.length; i++) {
                int codePoint = codePoints[i];
                assertNotEquals(0, indices[i] & 0xFFFF,
                        () -> String.format("Segoe UI Emoji must map U+%04X", codePoint));
                assertEquals(mapper.charToGlyph(codePoint), indices[i] & 0xFFFF,
                        () -> String.format("glyph for U+%04X", codePoint));
            }
        } finally {
            API.release(emoji);
        }
    }

    /**
     * {@code IDWriteFontFace::GetDesignGlyphMetrics}, slot 10, decoded from a 28-byte out-parameter:
     * seven fields that are stable across calls, different between glyphs, and consistent with the
     * design grid the font declares. Reading the struct at a wrong offset would still produce seven
     * numbers, so what pins the layout is that they are the <em>same</em> seven numbers the JNI path
     * read through {@code GetIntField} on the mirror object, which is what the parity test asserted
     * for every glyph of three fonts before the peers were flipped onto this facade.
     * <p>
     * Note what this call does <em>not</em> do, which an earlier version of this test asserted
     * wrongly: a glyph index equal to {@code GetGlyphCount()} is not rejected. DirectWrite answers
     * {@code S_OK} with real metrics for it on this build, so "one past the end" is not an error
     * path, and the JNI native behaves identically. Only the parity assertion is safe to make about
     * out-of-range indices.
     */
    @Test
    public void designGlyphMetricsAreStableAndGlyphSpecific() {
        int unitsPerEm = DWNativeShim.fontFaceMetrics(arial)[0];
        short capitalH = glyphOf('H');
        short lowerG = glyphOf('g');
        assertNotEquals((int) capitalH, (int) lowerG);

        int[] metricsH = API.designGlyphMetrics(arial, capitalH, false);
        assertNotNull(metricsH, "H must have design metrics");
        assertEquals(7, metricsH.length, "DWRITE_GLYPH_METRICS has seven fields");
        assertArrayEquals(metricsH, API.designGlyphMetrics(arial, capitalH, false),
                "the same glyph must give the same metrics twice");
        assertTrue(metricsH[1] > 0 && metricsH[1] <= 4 * unitsPerEm,
                "the advance of H lies on the design grid");
        assertTrue(metricsH[0] >= 0 && metricsH[0] < metricsH[1], "leftSideBearing of H");

        assertFalse(Arrays.equals(metricsH, API.designGlyphMetrics(arial, lowerG, false)),
                "H and g must not have identical metrics");
        // Glyph 1 of Arial is a zero-width control glyph: in range, so it answers, but its advance
        // is 0. Recorded because an earlier version of this test assumed every glyph has a positive
        // advance, and because it shows a successful call is not the same as a drawable glyph.
        int[] metricsOne = API.designGlyphMetrics(arial, (short) 1, false);
        assertNotNull(metricsOne, "glyph 1 is in range");
        assertEquals(0, metricsOne[1], "and has no advance");
    }

    /* ---------------------------------------------------------------------------------------------
     * The geometry sink, called through its own vtable
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code QueryInterface}, {@code AddRef}, {@code Release} and {@code Close} of the synthesized
     * sink, driven through the ten function pointers Java put in its vtable. Reproduces the C exactly,
     * including its two oddities: an unsupported IID answers {@code E_FAIL} rather than
     * {@code E_NOINTERFACE}, and the object is never freed when the count reaches zero.
     */
    @Test
    public void theSynthesizedSinkAnswersIUnknown() {
        long[] result = DWNativeShim.sinkSelfTest();
        assertNotNull(result, "the sink vtable must have been built");
        assertEquals(0L, result[0], "QueryInterface(ID2D1SimplifiedGeometrySink) is S_OK");
        assertEquals(1L, result[1], "and writes the object itself");
        assertEquals(0L, result[2], "QueryInterface(IUnknown) is S_OK");
        assertEquals(1L, result[3], "and writes the same pointer");
        assertEquals(0x80004005L, result[4], "an unsupported IID answers E_FAIL, as directwrite.cpp:1762");
        assertEquals(1L, result[5], "and writes NULL");
        assertEquals(3L, result[6], "two accepted QueryInterfaces AddRef, so this AddRef makes three");
        assertEquals(2L, result[7]);
        assertEquals(1L, result[8]);
        assertEquals(0L, result[9], "the count reaches zero");
        assertEquals(0L, result[10], "Close is always S_OK");
        assertEquals(1L, result[11], "exactly one session was registered during the calls");
        assertEquals(1L, result[12], "and the object is still alive at zero: Release never frees");
        assertEquals(0, DWNativeShim.sinkRegistrySize(), "the registry entry is gone afterwards");
    }

    /**
     * A whole figure through the sink vtable: {@code SetFillMode} and {@code SetSegmentFlags} ignored,
     * one {@code BeginFigure} whose {@code D2D1_POINT_2F} is passed <em>by value</em>, one
     * {@code AddLines} of two points, one {@code AddBeziers} of one cubic and one {@code EndFigure}.
     * <p>
     * Both ends of this call are the same linker, so it cannot prove that the register assignment
     * matches MSVC - that is what the outline parity against the JNI path proves. What it does prove
     * is that the table is well formed, that the by-value point is not silently dropped or reordered,
     * and that the segment types are the ones the C emitted: move-to for {@code BeginFigure} whatever
     * its flag says, line-to per point, a cubic per bezier and a close with no coordinates.
     */
    @Test
    public void theSynthesizedSinkBuildsTheFigureItIsGiven() {
        float[] figure = DWNativeShim.sinkSelfTestFigure();
        assertEquals(12, figure.length);
        Path2D path = DWNativeShim.sinkSelfTestOutline();
        assertNotNull(path, "the sink must have produced a path");
        assertEquals(PathIterator.WIND_EVEN_ODD, path.getWindingRule(),
                "the winding rule is hardcoded to 0, as directwrite.cpp:1804");

        StringBuilder expected = new StringBuilder();
        expected.append("wind ").append(PathIterator.WIND_EVEN_ODD).append('\n');
        expected.append(PathIterator.SEG_MOVETO).append(' ').append(figure[0])
                .append(' ').append(figure[1]).append('\n');
        expected.append(PathIterator.SEG_LINETO).append(' ').append(figure[2])
                .append(' ').append(figure[3]).append('\n');
        expected.append(PathIterator.SEG_LINETO).append(' ').append(figure[4])
                .append(' ').append(figure[5]).append('\n');
        expected.append(PathIterator.SEG_CUBICTO);
        for (int i = 6; i < 12; i++) {
            expected.append(' ').append(figure[i]);
        }
        expected.append('\n');
        expected.append(PathIterator.SEG_CLOSE).append('\n');
        assertEquals(expected.toString(), outlineText(path));
    }

    /**
     * A callback that throws does not throw into DirectWrite: the failure is stashed, every later
     * callback of the same call becomes a no-op so no half-built path survives, and the registry entry
     * still goes away.
     */
    @Test
    public void aFailingCallbackIsStashedAndStopsTheRest() {
        long[] result = DWNativeShim.sinkSelfTestFailure();
        assertNotNull(result);
        assertEquals(1L, result[0], "the failure was recorded rather than thrown");
        assertEquals(0L, result[1], "no segment was appended");
        assertEquals(0L, result[2], "and no coordinate");
        assertEquals(0L, result[3], "the registry entry is gone");
        assertEquals(0, DWNativeShim.sinkRegistrySize());
    }

    /* ---------------------------------------------------------------------------------------------
     * Outlines
     * ------------------------------------------------------------------------------------------- */

    /**
     * A thousand outlines leave nothing behind: no registry entry, and therefore no session, no
     * arena and no upcall stub. The vtable itself is process-wide by design and is not counted.
     */
    @Test
    public void manyOutlinesLeaveTheRegistryEmpty() {
        assertEquals(0, DWNativeShim.sinkRegistrySize(), "nothing may be registered before the loop");
        int glyphs = DWNativeShim.glyphCount(arial);
        for (int i = 0; i < 1000; i++) {
            Path2D path = API.glyphRunOutline(arial, 12f, (short) (i % glyphs), false);
            assertNotNull(path, "every glyph of Arial has an outline, even if it is empty");
        }
        assertEquals(0, DWNativeShim.sinkRegistrySize(), "and nothing after it");
    }

    /**
     * Two outlines fetched before either is walked must be independent. {@code Path2D} stores the
     * arrays it is given <em>by reference</em> and its javadoc says the caller promises to drop all
     * other references, so a session that pooled or reused its buffers would let one glyph report
     * another glyph's coordinates with no exception anywhere - a wrong shape on screen and a green
     * test suite.
     */
    @Test
    public void twoOutlinesDoNotShareTheirArrays() {
        short first = glyphOf('H');
        short second = glyphOf('g');
        assertNotEquals((int) first, (int) second);
        Path2D a = API.glyphRunOutline(arial, 40f, first, false);
        Path2D b = API.glyphRunOutline(arial, 40f, second, false);
        String textA = outlineText(a);
        String textB = outlineText(b);
        assertNotEquals(textA, textB, "H and g are not the same shape");
        assertEquals(textA, outlineText(API.glyphRunOutline(arial, 40f, first, false)),
                "and fetching H again after g must still give H");
    }

    /**
     * A glyph with no contours - the space - is an empty {@code Path2D}, not {@code null}. The C
     * built the object from two zero-length arrays ({@code directwrite.cpp:1798-1806}) and callers
     * such as {@code PrismFontStrike} treat null as "no outline available", so turning this into null
     * would be a behaviour change.
     */
    @Test
    public void aGlyphWithNoContoursIsAnEmptyPathNotNull() {
        Path2D path = API.glyphRunOutline(arial, 24f, glyphOf(' '), false);
        assertNotNull(path, "the space glyph must give an empty path, not null");
        assertEquals("wind " + PathIterator.WIND_EVEN_ODD + "\n", outlineText(path), "no segments");
    }

    /* ---------------------------------------------------------------------------------------------
     * CreateAlphaTexture: the guard ladder before the call
     * ------------------------------------------------------------------------------------------- */

    /**
     * The five guards of {@code directwrite.cpp:2177-2187}, in order: a null rectangle, an empty or
     * inverted one in either axis. Each answers {@code null} without calling DirectWrite at all.
     */
    @Test
    public void createAlphaTextureRejectsDegenerateRectangles() {
        long analysis = analysisFor(glyphOf('H'), 24f);
        try {
            int type = DWNativeShim.textureClearType3x1();
            assertNull(API.alphaTexture(analysis, type, null), "a null RECT");
            assertNull(API.alphaTexture(analysis, type, new int[] { 0, 0, 0, 4 }), "right == left");
            assertNull(API.alphaTexture(analysis, type, new int[] { 4, 0, 0, 4 }), "right < left");
            assertNull(API.alphaTexture(analysis, type, new int[] { 0, 0, 4, 0 }), "bottom == top");
            assertNull(API.alphaTexture(analysis, type, new int[] { 0, 4, 4, 0 }), "bottom < top");
        } finally {
            API.release(analysis);
        }
    }

    /**
     * The bounds of a real glyph are not empty, and the mask that comes back is exactly
     * width * height * 3 bytes for {@code CLEARTYPE_3x1} and width * height for {@code ALIASED_1x1} -
     * which is the arithmetic the guard ladder protects.
     */
    @Test
    public void alphaTextureLengthFollowsTheBoundsAndTheTextureType() {
        long analysis = analysisFor(glyphOf('H'), 24f);
        try {
            int lcd = DWNativeShim.textureClearType3x1();
            int[] bounds = API.alphaTextureBounds(analysis, lcd);
            assertNotNull(bounds, "GetAlphaTextureBounds");
            int width = bounds[2] - bounds[0];
            int height = bounds[3] - bounds[1];
            assertTrue(width > 0 && height > 0, "H at 24px has a non-empty LCD texture");
            byte[] mask = API.alphaTexture(analysis, lcd, bounds);
            assertNotNull(mask);
            assertEquals(width * height * 3, mask.length, "CLEARTYPE_3x1 is three bytes per pixel");
        } finally {
            API.release(analysis);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Helpers
     * ------------------------------------------------------------------------------------------- */

    private static long analysisFor(short glyphIndex, float size) {
        GlyphRun run = GlyphRun.of(arial, size, glyphIndex);
        long analysis = API.createGlyphRunAnalysis(factory, run, 1f, null, 4, 0, 0f, 0f);
        assertNotEquals(0L, analysis, "IDWriteFactory::CreateGlyphRunAnalysis");
        return analysis;
    }

    /** The Arial glyph for one code point, through the cmap call the tests above validated. */
    private static short glyphOf(int codePoint) {
        short[] indices = DWNativeShim.glyphIndices(arial, new int[] { codePoint });
        assertNotNull(indices, "IDWriteFontFace::GetGlyphIndices");
        return indices[0];
    }

    /**
     * The Java cmap parser for the same physical font: {@code OpenTypeGlyphMapper} over the resource
     * {@code PrismFontFactory} resolves, taking slot 0 of a composite so that no fallback slot is
     * encoded into the glyph code. The glyph count is compared as well, which is what ties the
     * resource to the {@code IDWriteFontFace} this test built from the system collection.
     */
    private static CharToGlyphMapper glyphMapper(String family) {
        PrismFontFactory fontFactory = PrismFontFactory.getFontFactory();
        assertNotNull(fontFactory, "PrismFontFactory.getFontFactory()");
        PGFont font = fontFactory.createFont(family, 12f);
        FontResource resource = font.getFontResource();
        if (resource instanceof CompositeFontResource composite) {
            resource = composite.getSlotResource(0);
        }
        assertNotNull(resource, () -> "no font resource for " + family);
        CharToGlyphMapper mapper = resource.getGlyphMapper();
        assertNotNull(mapper, () -> "no glyph mapper for " + family);
        return mapper;
    }

}
