/*
 * Copyright (c) 2012, 2023, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sun.javafx.font.CharToGlyphMapper;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.scene.text.FontHelper;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextLine;
import com.sun.javafx.scene.text.TextSpan;
import com.sun.javafx.text.PrismTextLayout;

import javafx.scene.text.Font;

public class TextLayoutTest {
    private static final String J = "\u3041";  // Japanese not complex
    private static final String D = "\u0907";  // Devanagari complex
    private static final String T = "\u0E34";  // Thai complex

    private final PrismTextLayout layout = new PrismTextLayout();
    private final PGFont font = (PGFont) FontHelper.getNativeFont(Font.font("Monaco", 12));
    private final PGFont font2 = (PGFont) FontHelper.getNativeFont(Font.font("Tahoma", 12));

    class TestSpan implements TextSpan {
        String text;
        Object font;
        TestSpan(Object text, Object font) {
            this.text = (String)text;
            this.font = font;
        }
        @Override public String getText() {
            return text;
        }
        @Override public Object getFont() {
            return font;
        }
        @Override public RectBounds getBounds() {
            return null;
        }
    }

    private void setContent(PrismTextLayout layout, Object... content) {
        int count = content.length / 2;
        TextSpan[] spans = new TextSpan[count];
        int i = 0;
        while (i < content.length) {
            spans[i>>1] = new TestSpan(content[i++], content[i++]);
        }
        layout.setContent(spans);
    }

    private void assertLineCount(int lineCount) {
        assertEquals(lineCount, layout.getLines().length, "lineCount");
    }

    private void assertLineBounds(RectBounds... rectBounds) {
        assertLineCount(rectBounds.length);

        TextLine[] lines = layout.getLines();

        for (int i = 0; i < lines.length; i++) {
            assertEquals(rectBounds[i], lines[i].getBounds(), "line " + i);
        }
    }

    private void verifyLayout(int lineCount, int runCount, int... glyphCount) {
        TextLine[] lines = layout.getLines();
        assertEquals(lineCount, lines.length, "lineCount");
        GlyphList[] runs = layout.getRuns();
        assertEquals(runCount, runs.length, "runCount");
        assertEquals(runCount, glyphCount.length, "runCount");
        for (int i = 0; i < runs.length; i++) {
            assertEquals(glyphCount[i], runs[i].getGlyphCount(), "run " + i);
        }
    }

    private void verifyComplex(boolean... complex) {
        GlyphList[] runs = layout.getRuns();
        for (int i = 0; i < runs.length; i++) {
            assertEquals(complex[i], runs[i].isComplex(), "run " + i);
        }
    }

    /**
     * These tests were broken for a long time (as early as 2013-06-28, see JDK-8087615).
     *
     * The reason they break is two fold:
     *
     * - Content that is split into multiple runs will have all runs set to "complex"
     *   if at least one run is set to "complex", while this test was expecting the
     *   runs containing non-complex characters to not become "complex".
     *
     * - The Tahoma font when used with a Thai character generates 2 glyphs per Thai
     *   character, while this test was expecting 1 glyph.
     */
    @Disabled("JDK-8087615")
    @Test
    void complexTestsThatAreBrokenSince2013() {
        layout.setContent("aa" + J + J, font);
        verifyLayout(1, 1, 4);  // no complex (english to japanese)
        verifyComplex(false);

        layout.setContent(D, font);
        verifyLayout(1, 1, 1);  // complex (english to devanagari)
        verifyComplex(true);

        layout.setContent("aa" + D + D, font);
        verifyLayout(1, 2, 2, 2);  // complex (english to devanagari)
        verifyComplex(false, true);

        layout.setContent(D + D + "aa", font);
        verifyLayout(1, 2, 2, 2);  // complex (devanagari to english)
        verifyComplex(true, false);

        layout.setContent("aa" + D + D + J + J, font);
        verifyLayout(1, 3, 2, 2, 2);  // complex (english to devanagari to japanese)
        verifyComplex(false, true, false);

        // Tahoma has Thai but no Hindi, font slot break expected
        layout.setContent(D + D + T + T, font2);
        verifyLayout(1, 2, 2, 2);  // complex (devanagari to thai)
        verifyComplex(true, true);

        layout.setContent(T + T + D + D + T + T, font2);
        verifyLayout(1, 3, 2, 2, 2);
        verifyComplex(true, true, true);

        layout.setContent(T + T + D + D + "aa", font2);
        verifyLayout(1, 3, 2, 2, 2);
        verifyComplex(true, true, false);

        layout.setContent(T + T + "aa" + T + T, font2);
        verifyLayout(1, 3, 2, 2, 2);
        verifyComplex(true, false, true);

        layout.setContent("aa" + D + D + T + T, font2);
        verifyLayout(1, 3, 2, 2, 2);
        verifyComplex(false, true, true);
    }

    /**
     * These are fixed versions of the above tests to avoid
     * further regressions.
     */
    @Test
    void fixedComplexTestsToEnsureNoFurtherRegressions() {
        layout.setContent("aa" + J + J, font);
        verifyLayout(1, 1, 4);  // no complex (english to japanese)
        verifyComplex(false);

        layout.setContent(D, font);
        verifyLayout(1, 1, 1);  // complex (english to devanagari)
        verifyComplex(true);

        layout.setContent("aa" + D + D, font);
        verifyLayout(1, 2, 2, 2);  // complex (english to devanagari)
        verifyComplex(true, true);

        layout.setContent(D + D + "aa", font);
        verifyLayout(1, 2, 2, 2);  // complex (devanagari to english)
        verifyComplex(true, true);

        layout.setContent("aa" + D + D + J + J, font);
        verifyLayout(1, 3, 2, 2, 2);  // complex (english to devanagari to japanese)
        verifyComplex(true, true, true);

        // Tahoma has Thai but no Hindi, font slot break expected
        layout.setContent(D + D + T + T, font2);
        verifyLayout(1, 2, 2, 4);  // complex (devanagari to thai)
        verifyComplex(true, true);

        layout.setContent(T + T + D + D + T + T, font2);
        verifyLayout(1, 3, 4, 2, 4);
        verifyComplex(true, true, true);

        layout.setContent(T + T + D + D + "aa", font2);
        verifyLayout(1, 3, 4, 2, 2);
        verifyComplex(true, true, true);

        layout.setContent(T + T + "aa" + T + T, font2);
        verifyLayout(1, 3, 4, 2, 4);
        verifyComplex(true, true, true);

        layout.setContent("aa" + D + D + T + T, font2);
        verifyLayout(1, 3, 2, 2, 4);
        verifyComplex(true, true, true);
    }

    @Test
    void basicTest() {
        // simple case
        layout.setContent("hello", font);
        verifyLayout(1, 1, 5);

        // simple case, two words
        layout.setContent("hello world", font);
        verifyLayout(1, 1, 11);

        // empty string
        layout.setContent("", font);
        verifyLayout(1, 1, 0);

        // line break
        layout.setContent("\n", font);  // first line has the line break (glyphCount=0)
        verifyLayout(2, 2, 0, 0);
        layout.setContent("\r", font);
        verifyLayout(2, 2, 0, 0);
        layout.setContent("\r\n", font);
        verifyLayout(2, 2, 0, 0);
        layout.setContent("a\nb", font);
        verifyLayout(2, 3, 1, 0, 1);
        layout.setContent("\n\n\r\r\n", font);
        verifyLayout(5, 5, 0, 0, 0, 0, 0);

        // tabs
        layout.setContent("\t", font);
        verifyLayout(1, 1, 0);
        layout.setContent("\t\t", font);
        verifyLayout(1, 2, 0, 0);
        layout.setContent("a\tb", font);
        verifyLayout(1, 3, 1, 0, 1);
    }

    @Test
    void richTextTest() {
        setContent(layout, "hello ", font, "world", font);
        verifyLayout(1, 2, 6, 5);
        verifyComplex(false, false);

        setContent(layout, "aaa", font, J + J + J, font);
        verifyLayout(1, 2, 3, 3);
        verifyComplex(false, false);

        setContent(layout, "aaa", font, D + D + D, font);
        verifyLayout(1, 2, 3, 3);
        verifyComplex(false, true);

        // can't merge \r\n in different spans
        setContent(layout, "aa\r", font, "\nbb", font);
        verifyLayout(3, 4, 2, 0, 0, 2);
        verifyComplex(false, false, false, false);

        setContent(layout, "aa\r\n", font, "bb", font);
        verifyLayout(2, 3, 2, 0, 2);
        verifyComplex(false, false, false);

        // can't merge surrogate pairs in different spans
        setContent(layout, "\uD840\uDC0B", font, "\uD840\uDC89\uD840\uDCA2", font);
        verifyLayout(1, 2, 2, 4);
        GlyphList[] runs = layout.getRuns();
        assertTrue(runs[0].getGlyphCode(0) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[0].getGlyphCode(1) == CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[1].getGlyphCode(0) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[1].getGlyphCode(1) == CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[1].getGlyphCode(2) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[1].getGlyphCode(3) == CharToGlyphMapper.INVISIBLE_GLYPH_ID);

        // Split surrogate pair
        setContent(layout, "\uD840\uDC0B\uD840", font, "\uDC89\uD840\uDCA2", font);
        verifyLayout(1, 2, 3, 3);
        runs = layout.getRuns();
        assertTrue(runs[0].getGlyphCode(0) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[0].getGlyphCode(1) == CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[0].getGlyphCode(2) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);  // broken pair, results in missing glyph
        assertTrue(runs[1].getGlyphCode(0) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);  // broken pair, results in missing glyph
        assertTrue(runs[1].getGlyphCode(1) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[1].getGlyphCode(2) == CharToGlyphMapper.INVISIBLE_GLYPH_ID);
    }

    @Test
    void shouldIgnoreAlignmentWhenWrappingIsDisabled() {
        layout.setContent("The quick brown fox jumps over the lazy dog", font);

        for (int i = 0; i < 3; i++) {
            layout.setAlignment(i);

            assertLineCount(1);
            assertLineBounds(new RectBounds(0, -12, 309.6504f, 4.001953f));
        }
    }

    enum Case {

        /**
         * Checks that alignment variations have no effect when not wrapping.
         */
        NO_WRAP(new Parameters(
            "The quick brown fox jumps over the lazy dog",
            Font.font("Monaco", 12),
            0.0f, List.of(309.6504f), List.of(0.0f),
            12.0f, 4.001953f
        )),

        /**
         * Checks that the individual lines of hard wrapped text are still
         * taking alignment into account (in this specific case, the first
         * line, which is the widest line, will not be aligned, but the 2nd
         * line will be aligned as it is less wide).
         */
        HARD_WRAP(new Parameters(
            "The quick brown fox jumps\nover the lazy dog",
            Font.font("Monaco", 12),
            0.0f, List.of(180.0293f, 122.41992f), List.of(0.0f, 0.0f),
            12.0f, 4.001953f
        )),

        /**
         * Checks that trailing white space is NOT ignored when wrapping
         * is not enabled.
         */
        HARD_WRAP_WITH_EXTRA_TRAILING_SPACE(new Parameters(
            "The quick brown fox jumps           \nover the lazy dog           ",
            Font.font("Monaco", 12),
            0.0f, List.of(180.0293f + 79.2129f, 122.41992f + 79.2129f), List.of(0.0f, 0.0f),
            12.0f, 4.001953f
        )),

        /**
         * Checks that single trailing white spaces are ignored for alignment
         * purposes when wrapping is enabled in simple text.
         */
        SIMPLE(new Parameters(
            "The quick brown fox jumps over the lazy dog",
            Font.font("Monaco", 12),
            200.0f, List.of(180.0293f, 122.41992f), List.of(7.20117f, 0.0f),
            12.0f, 4.001953f
        )),

        /**
         * Checks that multiple trailing white spaces are ignored for alignment
         * purposes when wrapping is enabled in simple text.
         */
        SIMPLE_WITH_EXTRA_TRAILING_SPACE(new Parameters(
            "The quick brown fox jumps           over the lazy dog",
            Font.font("Monaco", 12),
            200.0f, List.of(180.0293f, 122.41992f), List.of(79.2129f, 0.0f),
            12.0f, 4.001953f
        )),

        /**
         * Checks that single trailing white spaces are ignored for alignment
         * purposes when wrapping is enabled in complex text.
         */
        COMPLEX(new Parameters(
            "The quick brown लोमड़ी jumps over the lazy कुत्ता",
            Font.font("Monaco", 12),
            200.0f, List.of(189.89649f, 122.583984f), List.of(7.20117f, 0.0f),
            12.0f, 4.001953f
        )),

        /**
         * Checks that multiple trailing white spaces are ignored for alignment
         * purposes when wrapping is enabled in complex text.
         */
        COMPLEX_WITH_EXTRA_TRAILING_SPACE(new Parameters(
            "The quick brown लोमड़ी jumps           over the lazy कुत्ता",
            Font.font("Monaco", 12),
            200.0f, List.of(189.89649f, 122.583984f), List.of(79.2129f, 0.0f),
            12.0f, 4.001953f
        ));

        Parameters parameters;

        Case(Parameters parameters) {
            this.parameters = parameters;
        }

        record Parameters(String text, Font font, float wrapWidth, List<Float> lineWidths, List<Float> trailingWhiteSpaceWidths, float ascent, float descent) {
            Parameters {
                assert text != null;
                assert font != null;
                assert wrapWidth >= 0;
                assert lineWidths != null;
                assert trailingWhiteSpaceWidths != null;
                assert ascent > 0;
                assert descent > 0;
                assert lineWidths.size() > 0;
                assert lineWidths.size() == trailingWhiteSpaceWidths.size();
            }

            int lineCount() {
                return lineWidths.size();
            }

            float maxWidth() {
                return lineWidths.stream().max(Float::compareTo).orElseThrow();
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Case.class)
    void caseTest(Case c) {
        Case.Parameters p = c.parameters;

        final float ASCENT = p.ascent;
        final float DESCENT = p.descent;
        final float WRAP = p.wrapWidth == 0 ? p.maxWidth() : p.wrapWidth;
        final float CENTER = 0.5f * WRAP;

        // split content on line feeds (without removing the line feeds):
        layout.setContent(Arrays.stream(p.text.split("(?<=\n)")).map(text -> new TestSpan(text, FontHelper.getNativeFont(p.font))).toArray(TextSpan[]::new));
        layout.setWrapWidth(p.wrapWidth);

        // LEFT ALIGNMENT

        layout.setAlignment(0);  // 0 == left

        assertLineCount(p.lineCount());

        for (int i = 0; i < p.lineCount(); i++) {
            TextLine[] lines = layout.getLines();
            String description = "left aligned: line " + i + " for " + c.parameters;
            RectBounds expectedBounds = new RectBounds(0, -ASCENT, p.lineWidths.get(i) + p.trailingWhiteSpaceWidths.get(i), DESCENT);
            Point2D expectedLocation = new Point2D(0, i * (ASCENT + DESCENT));

            assertEquals(expectedBounds, lines[i].getBounds(), description);
            assertEquals(expectedLocation, lines[i].getRuns()[0].getLocation(), description);
        }

        // CENTER ALIGNMENT

        layout.setAlignment(1);  // 1 == center

        assertLineCount(p.lineCount());

        for (int i = 0; i < p.lineCount(); i++) {
            TextLine[] lines = layout.getLines();
            String description = "centered: line " + i + " for " + p;
            RectBounds expectedBounds = new RectBounds(CENTER - 0.5f * p.lineWidths.get(i), -ASCENT, CENTER + 0.5f * p.lineWidths.get(i) + p.trailingWhiteSpaceWidths.get(i), DESCENT);
            Point2D expectedLocation = new Point2D(CENTER - 0.5f * p.lineWidths.get(i), i * (ASCENT + DESCENT));

            assertEquals(expectedBounds, lines[i].getBounds(), description);
            assertEquals(expectedLocation, lines[i].getRuns()[0].getLocation(), description);
        }

        // RIGHT ALIGNMENT

        layout.setAlignment(2);  // 2 == center

        assertLineCount(p.lineCount());

        for (int i = 0; i < p.lineCount(); i++) {
            TextLine[] lines = layout.getLines();
            String description = "right aligned: line " + i + " for " + p;
            RectBounds expectedBounds = new RectBounds(WRAP - p.lineWidths.get(i), -ASCENT, WRAP + p.trailingWhiteSpaceWidths.get(i), DESCENT);
            Point2D expectedLocation = new Point2D(WRAP - p.lineWidths.get(i), i * (ASCENT + DESCENT));

            assertEquals(expectedBounds, lines[i].getBounds(), description);
            assertEquals(expectedLocation, lines[i].getRuns()[0].getLocation(), description);
        }

        // JUSTIFIED ALIGNMENT

        layout.setAlignment(3);  // 3 == justified

        assertLineCount(p.lineCount());

        for (int i = 0; i < p.lineCount(); i++) {
            TextLine[] lines = layout.getLines();
            String description = "justified: line " + i + " for " + p;
            boolean lastLine = i == p.lineCount() - 1;
            RectBounds expectedBounds = new RectBounds(0, -ASCENT, lastLine ? p.lineWidths.get(i) : WRAP, DESCENT);
            Point2D expectedLocation = new Point2D(0, i * (ASCENT + DESCENT));

            assertEquals(expectedBounds, lines[i].getBounds(), description);
            assertEquals(expectedLocation, lines[i].getRuns()[0].getLocation(), description);
        }
    }
}
