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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What {@link DWLayoutTest} needs: the corpus of scripts the shaping
 * calls are exercised over, the DirectWrite constants the shaping bindings use, and the two drains that turn
 * an analysis sink or a text renderer into a comparable string.
 * <p>
 * Every string is built from code points rather than written as a literal, so this file stays pure
 * ASCII while still covering Latin, a right-to-left script, a script that needs reordering and a
 * supplementary code point.
 * <p>
 * Line numbers into {@code directwrite.cpp} refer to it at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-font/directwrite.cpp}).
 * <p>
 * Not named {@code *Test}, so surefire does not try to run it.
 */
final class DWLayoutFixture {

    /** {@code OS.java:130-132}. */
    static final int FACTORY_TYPE_SHARED = 0;
    static final int READING_DIRECTION_LEFT_TO_RIGHT = 0;
    static final int READING_DIRECTION_RIGHT_TO_LEFT = 1;

    /** {@code OS.java:36-37}. */
    static final int S_OK = 0;
    static final long E_NOT_SUFFICIENT_BUFFER = 0x8007007AL;
    static final long E_FAIL = 0x80004005L;

    /** {@code OS.java:88-111}: the values {@code DWGlyphLayout.renderShape} passes. */
    static final int FONT_WEIGHT_NORMAL = 400;
    static final int FONT_WEIGHT_BOLD = 700;
    static final int FONT_STRETCH_NORMAL = 5;
    static final int FONT_STYLE_NORMAL = 0;
    static final int FONT_STYLE_ITALIC = 2;

    /** {@code DWGlyphLayout.LOCALE}. */
    static final String LOCALE = "en-us";

    /** {@code DWRITE_MEASURING_MODE_NATURAL}. */
    static final int MEASURING_MODE_NATURAL = 0;

    /** Latin with digits and a space: one script, no reordering, one glyph per character. */
    static final String LATIN = "Hello 123";

    /** Hebrew, right to left: aleph, bet, gimel, dalet, he. */
    static final String HEBREW = codePoints(0x05D0, 0x05D1, 0x05D2, 0x05D3, 0x05D4);

    /**
     * Arabic, right to left <em>and</em> contextually shaped: the six letters of "arabic" join, so
     * the glyph count differs from the character count and the cluster map is not the identity.
     */
    static final String ARABIC = codePoints(0x0627, 0x0644, 0x0639, 0x0631, 0x0628, 0x064A, 0x0629);

    /**
     * Devanagari: "ki" plus a conjunct. The vowel sign U+093F is written <em>before</em> the
     * consonant it follows in the string, so a correct shaper reorders glyphs and the cluster map
     * maps two characters onto one cluster - the case that fails loudly if {@code GetGlyphs} or the
     * analysis sink is wrong.
     */
    static final String DEVANAGARI = codePoints(0x0915, 0x093F, 0x0939, 0x093F, 0x0928, 0x094D, 0x0926);

    /** U+1D11E MUSICAL SYMBOL G CLEF: one code point, two UTF-16 units, in no Windows text font. */
    static final String SURROGATE_PAIR = codePoints(0x1D11E);

    /** CJK, which no Latin font covers, so it drives the fallback path through Draw. */
    static final String CJK = codePoints(0x4E2D, 0x6587, 0x5B57);

    /** Everything at once, in the order the golden's own string mixes scripts. */
    static final String MIXED = LATIN + HEBREW + ARABIC + DEVANAGARI + CJK + SURROGATE_PAIR;

    private DWLayoutFixture() {
    }

    static String codePoints(int... points) {
        return new String(points, 0, points.length);
    }

    /** What the peers pass across: UTF-16 with the terminator the C never appended itself. */
    static char[] wide(String text) {
        return (text + '\0').toCharArray();
    }

    /* ---------------------------------------------------------------------------------------------
     * Drains
     * ------------------------------------------------------------------------------------------- */

    /**
     * Every run an analysis sink collected, in order, as text: {@code start length script shapes}.
     * The cursor of a sink never resets ({@code directwrite.cpp:1151-1154}), so this may be called
     * once per object - which is exactly the property the comparison needs.
     */
    static List<String> drainAnalysisSink(Api api, long handle) {
        List<String> runs = new ArrayList<>();
        while (api.analysisSinkNext(handle)) {
            int[] analysis = api.analysisSinkGetAnalysis(handle);
            runs.add(api.analysisSinkGetStart(handle) + " " + api.analysisSinkGetLength(handle) + " "
                    + analysis[0] + " " + analysis[1]);
        }
        return runs;
    }

    /**
     * Everything a text renderer collected, in order and in full: the glyph indices, advances,
     * offsets and cluster map of every run, drained exactly as {@code DWGlyphLayout.renderShape}
     * drains them ({@code DWGlyphLayout.java:375-400}) - same buffer sizes, same running
     * {@code glyphStart} and {@code textStart}, same slot merge - so a difference here is a
     * difference there. Font faces are reported as "same as run 0" or "different", never as
     * addresses, so two draws of one layout can be compared.
     */
    static List<String> drainTextRenderer(Api api, long handle, int textLength, int slot) {
        List<String> lines = new ArrayList<>();
        int totalGlyphCount = api.textRendererGetTotalGlyphCount(handle);
        lines.add("totalGlyphCount " + totalGlyphCount);
        int[] glyphs = new int[Math.max(1, totalGlyphCount)];
        float[] advances = new float[Math.max(1, totalGlyphCount)];
        float[] offsets = new float[Math.max(2, totalGlyphCount * 2)];
        short[] clusterMap = new short[Math.max(1, textLength)];
        int glyphStart = 0;
        int textStart = 0;
        long firstFace = 0;
        int index = 0;
        while (api.textRendererNext(handle)) {
            long face = api.textRendererGetFontFace(handle);
            if (index == 0) {
                firstFace = face;
            }
            int copiedIndices = api.textRendererGetGlyphIndices(handle, glyphs, glyphStart, slot << 24);
            int copiedOffsets = api.textRendererGetGlyphOffsets(handle, offsets, glyphStart * 2);
            int copiedAdvances = api.textRendererGetGlyphAdvances(handle, advances, glyphStart);
            int copiedClusters = api.textRendererGetClusterMap(handle, clusterMap, textStart, glyphStart);
            lines.add("run " + index
                    + " start " + api.textRendererGetStart(handle)
                    + " length " + api.textRendererGetLength(handle)
                    + " glyphs " + api.textRendererGetGlyphCount(handle)
                    + " face " + (face == 0 ? "null" : (face == firstFace ? "first" : "other"))
                    + " copied " + copiedIndices + " " + copiedAdvances + " " + copiedOffsets + " "
                    + copiedClusters);
            glyphStart += api.textRendererGetGlyphCount(handle);
            textStart += api.textRendererGetLength(handle);
            index++;
        }
        lines.add("glyphIndices " + join(glyphs, glyphStart));
        lines.add("glyphAdvances " + join(advances, glyphStart));
        lines.add("glyphOffsets " + join(offsets, glyphStart * 2));
        lines.add("clusterMap " + join(clusterMap, Math.min(textStart, clusterMap.length)));
        return lines;
    }

    static String join(int[] values, int count) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < count; i++) {
            text.append(i == 0 ? "" : ",").append(values[i]);
        }
        return text.toString();
    }

    static String join(short[] values, int count) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < count; i++) {
            text.append(i == 0 ? "" : ",").append(values[i]);
        }
        return text.toString();
    }

    /** Floats through {@link Float#toString}, which round-trips every value: an exact comparison. */
    static String join(float[] values, int count) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < count; i++) {
            text.append(i == 0 ? "" : ",").append(Float.toString(values[i]));
        }
        return text.toString();
    }

    /* ---------------------------------------------------------------------------------------------
     * Shaping helpers
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code AnalyzeScript} over the whole of {@code text} with one implementation's analyzer, sink
     * and source, drained. The sink is created, referenced, used, released and disposed here, so
     * the caller cannot leak one.
     */
    static List<String> analyzeScript(Api api, long analyzer, String text, int direction) {
        long handle = api.newAnalysisSink(text.toCharArray(), 0, text.length(), LOCALE, direction);
        assertNotEquals(0L, handle, () -> api.name() + " newAnalysisSink");
        try {
            api.analysisSinkAddRef(handle);
            int hr = api.analyzeScript(analyzer, api.analysisSourcePointer(handle), 0, text.length(),
                    api.analysisSinkPointer(handle));
            assertEquals(S_OK, hr, () -> api.name() + " AnalyzeScript");
            List<String> runs = drainAnalysisSink(api, handle);
            assertTrue(!runs.isEmpty(), () -> api.name() + " produced no runs");
            return runs;
        } finally {
            api.analysisSinkRelease(handle);
            api.analysisSinkDispose(handle);
        }
    }

    /** The first run of an analysis as {@code {start, length, script, shapes}}. */
    static int[] firstRun(List<String> runs) {
        String[] parts = runs.get(0).split(" ");
        return new int[] { Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]), Integer.parseInt(parts[3]) };
    }

    static String describe(Api api, String what) {
        return String.format(Locale.ROOT, "%s: %s", api.name(), what);
    }

    /** Runs the facade class initializer and states the one precondition every test here has. */
    static void requireDirectWrite() {
        DWNativeShim.ensureLoaded();
        assertTrue(DWNativeShim.isAvailable(),
                "dwrite.dll must load and export DWriteCreateFactory on Windows");
    }
}
