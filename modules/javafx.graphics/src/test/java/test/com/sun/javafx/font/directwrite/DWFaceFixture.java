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

import com.sun.javafx.font.directwrite.DWNativeShim;
import com.sun.javafx.font.directwrite.DWNativeShim.Api;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What {@link DWFontFaceTest} needs: getting an
 * {@code IDWriteFontFace} for a named family, reading an sfnt table out of the font file without
 * asking DirectWrite, and writing a {@code Path2D} out as text so two of them can be compared exactly.
 * <p>
 * The sfnt reader is the independent oracle for the three font-face calls that were never JNI natives
 * and therefore have no parity partner: {@code TryGetFontTable}, {@code GetGlyphCount} and
 * {@code GetMetrics}. It parses the file the way the OpenType specification describes - a table
 * directory of 16-byte records after a 12-byte header - and shares no code with the thing it checks.
 * <p>
 * Line numbers into {@code dwrite.h} refer to the Windows SDK 10.0.26100.0 ({@code Include/10.0.26100.0/um}).
 * <p>
 * Not named {@code *Test}, so surefire does not try to run it.
 */
final class DWFaceFixture {

    /** {@code dwrite.h}: {@code DWRITE_FONT_WEIGHT_NORMAL}, {@code _STRETCH_NORMAL}, {@code _STYLE_NORMAL}. */
    static final int WEIGHT_NORMAL = 400;
    static final int STRETCH_NORMAL = 5;
    static final int STYLE_NORMAL = 0;

    /** {@code dwrite.h:92}: {@code DWRITE_FONT_FACE_TYPE_TRUETYPE} is the second constant, so 1. */
    static final int FONT_FACE_TYPE_TRUETYPE = 1;

    /**
     * A code point of every kind the tests need, as numbers rather than as a string literal so
     * that this file stays pure ASCII: ASCII letters, digits and punctuation; Cyrillic and Greek, two
     * non-Latin scripts Arial covers; and two supplementary code points, each of which is a surrogate
     * pair in UTF-16 and a single {@code UINT32} to {@code GetGlyphIndices}.
     */
    static final int[] CODE_POINTS = {
            0x0020, 0x0041, 0x0048, 0x0069, 0x0030, 0x0039, 0x002E, 0x007E,
            0x041F, 0x0440, 0x0438, 0x0432, 0x0435, 0x0442,
            0x0391, 0x03A9,
            0x1F600, 0x1F30D };

    private DWFaceFixture() {
    }

    /* ---------------------------------------------------------------------------------------------
     * Font faces
     * ------------------------------------------------------------------------------------------- */

    /**
     * The regular face of {@code family}, created the way {@code DWFontFile.createFontFace} creates
     * it: find the family, take its first matching font, ask that font for a face. The returned
     * pointer carries one reference for the caller.
     */
    static long fontFace(Api api, long collection, String family) {
        return fontFace(api, collection, family, WEIGHT_NORMAL, STRETCH_NORMAL, STYLE_NORMAL);
    }

    static long fontFace(Api api, long collection, String family, int weight, int stretch, int style) {
        int index = api.findFamilyName(collection, wide(family));
        assertTrue(index >= 0, () -> family + " must be installed");
        long fontFamily = api.getFontFamily(collection, index);
        assertNotEquals(0L, fontFamily, "IDWriteFontCollection::GetFontFamily");
        try {
            long font = api.getFirstMatchingFont(fontFamily, weight, stretch, style);
            assertNotEquals(0L, font, "IDWriteFontFamily::GetFirstMatchingFont");
            try {
                long face = api.createFontFace(font);
                assertNotEquals(0L, face, "IDWriteFont::CreateFontFace");
                return face;
            } finally {
                api.release(font);
            }
        } finally {
            api.release(fontFamily);
        }
    }

    /** What the peers pass: UTF-16 with the terminator the C never appended itself. */
    static char[] wide(String text) {
        return (text + '\0').toCharArray();
    }

    static Path windowsFont(String fileName) {
        String root = System.getenv("SystemRoot");
        Path path = Path.of(root == null ? "C:\\Windows" : root, "Fonts", fileName);
        assertTrue(Files.exists(path), () -> "missing system font " + path);
        return path;
    }

    /* ---------------------------------------------------------------------------------------------
     * The sfnt file, read directly
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code DWRITE_MAKE_OPENTYPE_TAG}: the four characters in file order packed little-endian, so
     * "cmap" is {@code 0x70616D63}.
     */
    static int openTypeTag(String tag) {
        byte[] ascii = tag.getBytes(StandardCharsets.US_ASCII);
        assertTrue(ascii.length == 4, "an OpenType tag is four characters");
        return (ascii[0] & 0xFF) | ((ascii[1] & 0xFF) << 8) | ((ascii[2] & 0xFF) << 16)
                | ((ascii[3] & 0xFF) << 24);
    }

    /** The bytes of one table of a single-face {@code .ttf}, straight from the file, or {@code null}. */
    static byte[] tableFromFile(Path file, String tag) {
        byte[] data = read(file);
        int numTables = ushort(data, 4);
        for (int i = 0; i < numTables; i++) {
            int record = 12 + i * 16;
            String name = new String(data, record, 4, StandardCharsets.US_ASCII);
            if (name.equals(tag)) {
                int offset = int32(data, record + 8);
                int length = int32(data, record + 12);
                assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length,
                        () -> "table " + tag + " does not lie inside " + file);
                return Arrays.copyOfRange(data, offset, offset + length);
            }
        }
        return null;
    }

    /** {@code head.unitsPerEm}, at offset 18 of the head table. */
    static int unitsPerEmFromFile(Path file) {
        byte[] head = tableFromFile(file, "head");
        assertTrue(head != null && head.length >= 20, () -> "no head table in " + file);
        return ushort(head, 18);
    }

    /** {@code maxp.numGlyphs}, at offset 4 of the maxp table. */
    static int numGlyphsFromFile(Path file) {
        byte[] maxp = tableFromFile(file, "maxp");
        assertTrue(maxp != null && maxp.length >= 6, () -> "no maxp table in " + file);
        return ushort(maxp, 4);
    }

    private static byte[] read(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int ushort(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private static int int32(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
    }

    /* ---------------------------------------------------------------------------------------------
     * Outlines
     * ------------------------------------------------------------------------------------------- */

    /**
     * A shape as the exact text of its segment stream - the same form
     * {@code DirectWriteMetricsGoldenTest} hashes, so a difference here is a difference there. Floats
     * go through {@link Float#toString}, which round-trips every value including {@code -0.0} and
     * {@code NaN}, so this comparison is exact and not a tolerance.
     */
    static String outlineText(Shape shape) {
        if (shape == null) {
            return "#null";
        }
        PathIterator path = shape.getPathIterator(BaseTransform.IDENTITY_TRANSFORM);
        StringBuilder text = new StringBuilder();
        text.append("wind ").append(path.getWindingRule()).append('\n');
        float[] coords = new float[6];
        while (!path.isDone()) {
            int type = path.currentSegment(coords);
            int count = switch (type) {
                case PathIterator.SEG_MOVETO, PathIterator.SEG_LINETO -> 2;
                case PathIterator.SEG_QUADTO -> 4;
                case PathIterator.SEG_CUBICTO -> 6;
                default -> 0;
            };
            text.append(type);
            for (int i = 0; i < count; i++) {
                text.append(' ').append(Float.toString(coords[i]));
            }
            text.append('\n');
            path.next();
        }
        return text.toString();
    }

    static String describe(String font, int glyph, float size, boolean sideways) {
        return String.format(Locale.ROOT, "%s glyph %d at %s%s", font, glyph, Float.toString(size),
                sideways ? " sideways" : "");
    }

    /** Runs the facade class initializer and states the one precondition every test here has. */
    static void requireDirectWrite() {
        DWNativeShim.ensureLoaded();
        assertTrue(DWNativeShim.isAvailable(),
                "dwrite.dll must load and export DWriteCreateFactory on Windows");
    }
}
