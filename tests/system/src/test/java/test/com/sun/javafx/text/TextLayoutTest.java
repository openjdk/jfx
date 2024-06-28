/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assume.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.font.CharToGlyphMapper;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.scene.text.FontHelper;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextLine;
import com.sun.javafx.scene.text.TextSpan;
import com.sun.javafx.text.PrismTextLayout;
import com.sun.javafx.text.TextRun;

import javafx.scene.text.Font;

public class TextLayoutTest {
    private static final String J = "\u3041";  // Japanese not complex
    private static final String D = "\u0907";  // Devanagari complex
    private static final String T = "\u0E34";  // Thai complex

    private final PrismTextLayout layout = new PrismTextLayout();
    private final PGFont arialFont = (PGFont) FontHelper.getNativeFont(Font.font("Arial", 12));
    private final PGFont tahomaFont = (PGFont) FontHelper.getNativeFont(Font.font("Tahoma", 12));

    record TestSpan(String text, Object font) implements TextSpan {
        @Override
        public String getText() {
            return text;
        }

        @Override
        public Object getFont() {
            return font;
        }

        @Override
        public RectBounds getBounds() {
            return null;
        }
    }

    private void setContent(PrismTextLayout layout, Object... content) {
        int count = content.length / 2;
        TextSpan[] spans = new TextSpan[count];
        int i = 0;
        while (i < content.length) {
            spans[i>>1] = new TestSpan((String) content[i++], content[i++]);
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
        layout.setContent("aa" + J + J, arialFont);
        verifyLayout(1, 1, 4);  // no complex (english to japanese)
        verifyComplex(false);

        layout.setContent(D, arialFont);
        verifyLayout(1, 1, 1);  // complex (english to devanagari)
        verifyComplex(true);

        layout.setContent("aa" + D + D, arialFont);
        verifyLayout(1, 2, 2, 2);  // complex (english to devanagari)
        verifyComplex(false, true);

        layout.setContent(D + D + "aa", arialFont);
        verifyLayout(1, 2, 2, 2);  // complex (devanagari to english)
        verifyComplex(true, false);

        layout.setContent("aa" + D + D + J + J, arialFont);
        verifyLayout(1, 3, 2, 2, 2);  // complex (english to devanagari to japanese)
        verifyComplex(false, true, false);

        // Tahoma has Thai but no Hindi, font slot break expected
        layout.setContent(D + D + T + T, tahomaFont);
        verifyLayout(1, 2, 2, 2);  // complex (devanagari to thai)
        verifyComplex(true, true);

        layout.setContent(T + T + D + D + T + T, tahomaFont);
        verifyLayout(1, 3, 2, 2, 2);
        verifyComplex(true, true, true);

        layout.setContent(T + T + D + D + "aa", tahomaFont);
        verifyLayout(1, 3, 2, 2, 2);
        verifyComplex(true, true, false);

        layout.setContent(T + T + "aa" + T + T, tahomaFont);
        verifyLayout(1, 3, 2, 2, 2);
        verifyComplex(true, false, true);

        layout.setContent("aa" + D + D + T + T, tahomaFont);
        verifyLayout(1, 3, 2, 2, 2);
        verifyComplex(false, true, true);
    }

    /**
     * These are fixed versions of the above tests to avoid
     * further regressions.
     */
    @Test
    void fixedComplexTestsToEnsureNoFurtherRegressions() {
        assumeArialFontAvailable();
        assumeTahomaFontAvailable();

        layout.setContent("aa" + J + J, arialFont);
        verifyLayout(1, 1, 4);  // no complex (english to japanese)
        verifyComplex(false);

        layout.setContent(D, arialFont);
        verifyLayout(1, 1, 1);  // complex (english to devanagari)
        verifyComplex(true);

        layout.setContent("aa" + D + D, arialFont);
        verifyLayout(1, 2, 2, 2);  // complex (english to devanagari)
        verifyComplex(true, true);

        layout.setContent(D + D + "aa", arialFont);
        verifyLayout(1, 2, 2, 2);  // complex (devanagari to english)
        verifyComplex(true, true);

        layout.setContent("aa" + D + D + J + J, arialFont);
        verifyLayout(1, 3, 2, 2, 2);  // complex (english to devanagari to japanese)
        verifyComplex(true, true, true);

        // Tahoma has Thai but no Hindi, font slot break expected
        layout.setContent(D + D + T + T, tahomaFont);
        verifyLayout(1, 2, 2, 4);  // complex (devanagari to thai)
        verifyComplex(true, true);

        layout.setContent(T + T + D + D + T + T, tahomaFont);
        verifyLayout(1, 3, 4, 2, 4);
        verifyComplex(true, true, true);

        layout.setContent(T + T + D + D + "aa", tahomaFont);
        verifyLayout(1, 3, 4, 2, 2);
        verifyComplex(true, true, true);

        layout.setContent(T + T + "aa" + T + T, tahomaFont);
        verifyLayout(1, 3, 4, 2, 4);
        verifyComplex(true, true, true);

        layout.setContent("aa" + D + D + T + T, tahomaFont);
        verifyLayout(1, 3, 2, 2, 4);
        verifyComplex(true, true, true);
    }

    @Test
    void basicTest() {
        assumeArialFontAvailable();

        // simple case
        layout.setContent("hello", arialFont);
        verifyLayout(1, 1, 5);

        // simple case, two words
        layout.setContent("hello world", arialFont);
        verifyLayout(1, 1, 11);

        // empty string
        layout.setContent("", arialFont);
        verifyLayout(1, 1, 0);

        // line break
        layout.setContent("\n", arialFont);  // first line has the line break (glyphCount=0)
        verifyLayout(2, 2, 0, 0);
        layout.setContent("\r", arialFont);
        verifyLayout(2, 2, 0, 0);
        layout.setContent("\r\n", arialFont);
        verifyLayout(2, 2, 0, 0);
        layout.setContent("a\nb", arialFont);
        verifyLayout(2, 3, 1, 0, 1);
        layout.setContent("\n\n\r\r\n", arialFont);
        verifyLayout(5, 5, 0, 0, 0, 0, 0);

        // tabs
        layout.setContent("\t", arialFont);
        verifyLayout(1, 1, 0);
        layout.setContent("\t\t", arialFont);
        verifyLayout(1, 2, 0, 0);
        layout.setContent("a\tb", arialFont);
        verifyLayout(1, 3, 1, 0, 1);
    }

    @Test
    void richTextTest() {
        assumeArialFontAvailable();

        setContent(layout, "hello ", arialFont, "world", arialFont);
        verifyLayout(1, 2, 6, 5);
        verifyComplex(false, false);

        setContent(layout, "aaa", arialFont, J + J + J, arialFont);
        verifyLayout(1, 2, 3, 3);
        verifyComplex(false, false);

        setContent(layout, "aaa", arialFont, D + D + D, arialFont);
        verifyLayout(1, 2, 3, 3);
        verifyComplex(false, true);

        // can't merge \r\n in different spans
        setContent(layout, "aa\r", arialFont, "\nbb", arialFont);
        verifyLayout(3, 4, 2, 0, 0, 2);
        verifyComplex(false, false, false, false);

        setContent(layout, "aa\r\n", arialFont, "bb", arialFont);
        verifyLayout(2, 3, 2, 0, 2);
        verifyComplex(false, false, false);

        // can't merge surrogate pairs in different spans
        setContent(layout, "\uD840\uDC0B", arialFont, "\uD840\uDC89\uD840\uDCA2", arialFont);
        verifyLayout(1, 2, 2, 4);
        GlyphList[] runs = layout.getRuns();
        assertTrue(runs[0].getGlyphCode(0) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[0].getGlyphCode(1) == CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[1].getGlyphCode(0) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[1].getGlyphCode(1) == CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[1].getGlyphCode(2) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[1].getGlyphCode(3) == CharToGlyphMapper.INVISIBLE_GLYPH_ID);

        // Split surrogate pair
        setContent(layout, "\uD840\uDC0B\uD840", arialFont, "\uDC89\uD840\uDCA2", arialFont);
        verifyLayout(1, 2, 3, 3);
        runs = layout.getRuns();
        assertTrue(runs[0].getGlyphCode(0) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[0].getGlyphCode(1) == CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[0].getGlyphCode(2) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);  // broken pair, results in missing glyph
        assertTrue(runs[1].getGlyphCode(0) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);  // broken pair, results in missing glyph
        assertTrue(runs[1].getGlyphCode(1) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[1].getGlyphCode(2) == CharToGlyphMapper.INVISIBLE_GLYPH_ID);
    }

    private static final float ARIAL_ASCENT = -10.863281f;
    private static final float ARIAL_DESCENT = 2.9355469f;
    private static final float ARIAL_SPACE_ADVANCE = 3.333984375f;
    private static final float ARIAL_TAB_ADVANCE = ARIAL_SPACE_ADVANCE * 8;

    @Test
    void shouldIgnoreAlignmentWhenWrappingIsDisabled() {
        assumeArialFontAvailable();

        layout.setContent("The quick brown fox jumps over the lazy dog", arialFont);

        for (int i = 0; i < 3; i++) {
            layout.setAlignment(i);

            assertLineCount(1);
            assertLineBounds(new RectBounds(0, ARIAL_ASCENT, 237.45117f, ARIAL_DESCENT));
        }
    }

    enum Case {

        /**
         * Checks that alignment variations have no effect when not wrapping.
         */
        NO_WRAP(
            "The quick brown fox jumps over the lazy dog",
            0.0f, List.of(237.45117f)
        ),

        /**
         * Checks that the individual lines of hard wrapped text are still
         * taking alignment into account (in this specific case, the first
         * line, which is the widest line, will not be aligned, but the 2nd
         * line will be aligned as it is less wide).
         */
        HARD_WRAP(
            "The quick brown fox jumps\nover the lazy dog",
            0.0f, List.of(142.72852f, 91.38867f)
        ),

        /**
         * Checks that trailing white space is NOT ignored when wrapping
         * is not enabled.
         */
        HARD_WRAP_WITH_EXTRA_TRAILING_SPACE(
            "The quick brown fox jumps           \nover the lazy dog           ",
            0.0f, List.of(142.72852f + 11 * ARIAL_SPACE_ADVANCE, 91.38867f + 11 * ARIAL_SPACE_ADVANCE)
        ),

        /**
         * Checks that a character after tabs align, even if there were
         * some preceding characters (that don't exceed the tab advance width)
         */
        HARD_WRAP_WITH_TABS(
            "\tA A\n" + "x\tA A\n" + "xx\tA A",  // expect same width for all three, as the "x" character falls within tab advance width
            0.0f, List.of(46.01367f, 46.01367f, 46.01367f)
        ),

        /**
         * Checks that tabs are a multiple of the tab advance.
         */
        HARD_WRAP_WITH_MULTIPLE_TABS(
            "\t\n" + "\t\t\n" + "\t\t\t",  // expect width ratio 1:2:3
            0.0f, List.of(ARIAL_TAB_ADVANCE, ARIAL_TAB_ADVANCE * 2, ARIAL_TAB_ADVANCE * 3)
        ),

        /**
         * Checks that single white spaces are ignored for alignment
         * purposes when wrapping is enabled in simple text. This soft
         * wraps after "jumps".
         */
        SOFT_WRAP(
            "The quick brown fox jumps over the lazy dog",
            145.0f, List.of(142.72852f, 91.38867f)
        ),

        /**
         * Checks that leading white spaces are NOT stripped even when
         * some lines are soft wrapped. This soft wraps after "fox"
         * and has 4 leading spaces on the 1st line where no soft
         * wrap occurs, and so they should be kept intact.
         */
        SOFT_WRAP_WITH_LEADING_SPACE(
            "    The quick brown fox jumps over the lazy dog",
            145.0f, List.of(107.384762f + 4 * ARIAL_SPACE_ADVANCE, 126.73242f)
        ),

        /**
         * Checks that trailing white spaces are NOT stripped even when
         * some lines are soft wrapped. This soft wraps after "jumps"
         * and has 4 trailing spaces on the 2nd line where no soft
         * wrap occurs, and so they should be kept intact.
         */
        SOFT_WRAP_WITH_TRAILING_SPACE(
            "The quick brown fox jumps over the lazy dog    ",
            145.0f, List.of(142.72852f, 91.38867f + 4 * ARIAL_SPACE_ADVANCE)
        ),

        /**
         * Checks that multiple white spaces are ignored for alignment
         * purposes when wrapping is enabled in simple text. This soft
         * wraps after "jumps".
         */
        SOFT_WRAP_WITH_EXTRA_SPACE(
            "The quick brown fox jumps           over the lazy dog",
            145.0f, List.of(142.72852f, 91.38867f)
        ),

        /**
         * Checks that single white spaces are ignored for alignment
         * purposes when wrapping is enabled in complex text. This soft
         * wraps after "jumps".
         */
        SOFT_WRAP_WITH_COMPLEX_TEXT_ON_WINDOWS(
            TextLayoutTest::assumeWindows,
            "The quick brown लोमड़ी jumps over the lazy कुत्ता",
            160.0f, List.of(158.1914f, 93.134766f)
        ),

        /**
         * Checks that multiple white spaces are ignored for alignment
         * purposes when wrapping is enabled in complex text. This soft
         * wraps after "jumps".
         */
        SOFT_WRAP_WITH_COMPLEX_TEXT_AND_EXTRA_SPACE_ON_WINDOWS(
            TextLayoutTest::assumeWindows,
            "The quick brown लोमड़ी jumps           over the lazy कुत्ता",
            160.0f, List.of(158.1914f, 93.134766f)
        ),

        /**
         * Checks that single white spaces are ignored for alignment
         * purposes when wrapping is enabled in complex text. This soft
         * wraps after "jumps".
         */
        SOFT_WRAP_WITH_COMPLEX_TEXT_ON_MAC(
            TextLayoutTest::assumeMac,
            "The quick brown लोमड़ी jumps over the lazy कुत्ता",
            160.0f, List.of(155.3047f, 91.04719f)
        ),

        /**
         * Checks that multiple white spaces are ignored for alignment
         * purposes when wrapping is enabled in complex text. This soft
         * wraps after "jumps".
         */
        SOFT_WRAP_WITH_COMPLEX_TEXT_AND_EXTRA_SPACE_ON_MAC(
            TextLayoutTest::assumeMac,
            "The quick brown लोमड़ी jumps           over the lazy कुत्ता",
            160.0f, List.of(155.3047f, 91.04719f)
        );

        Runnable assumption;
        String text;
        float wrapWidth;
        List<Float> lineWidths;

        Case(Runnable assumption, String text, float wrapWidth, List<Float> lineWidths) {
            assert text != null;
            assert wrapWidth >= 0;
            assert lineWidths != null;
            assert lineWidths.size() > 0;

            this.assumption = assumption == null ? () -> {} : assumption;
            this.text = text;
            this.wrapWidth = wrapWidth;
            this.lineWidths = List.copyOf(lineWidths);
        }

        Case(String text, float wrapWidth, List<Float> lineWidths) {
            this(null, text, wrapWidth, lineWidths);
        }

        int lineCount() {
            return lineWidths.size();
        }

        float maxWidth() {
            return lineWidths.stream().max(Float::compareTo).orElseThrow();
        }
    }

    @ParameterizedTest
    @EnumSource(Case.class)
    void caseTest(Case c) {
        assumeArialFontAvailable();

        // Check test specific assumption:
        c.assumption.run();

        final float ASCENT = -ARIAL_ASCENT;
        final float DESCENT = ARIAL_DESCENT;
        final float WRAP = c.wrapWidth == 0 ? c.maxWidth() : c.wrapWidth;
        final float CENTER = 0.5f * WRAP;

        for (String contentType : new String[] {"rich text (spans)", "plain text"}) {
            if (contentType.equals("plain text")) {
                layout.setContent(c.text, arialFont);
            }
            else {
                // split content on line feeds (without removing the line feeds):
                layout.setContent(Arrays.stream(c.text.split("(?<=\n)")).map(text -> new TestSpan(text, arialFont)).toArray(TextSpan[]::new));
            }

            layout.setWrapWidth(c.wrapWidth);

            // LEFT ALIGNMENT

            layout.setAlignment(0);  // 0 == left

            assertLineCount(c.lineCount());

            for (int i = 0; i < c.lineCount(); i++) {
                TextLine line = layout.getLines()[i];
                String description = "left aligned " + contentType + ": line " + i + " for " + c;
                RectBounds expectedBounds = new RectBounds(0, -ASCENT, c.lineWidths.get(i), DESCENT);
                Point2D expectedLocation = new Point2D(0, i * (ASCENT + DESCENT));

                assertEquals(expectedBounds, line.getBounds(), description);
                assertEquals(expectedLocation, line.getRuns()[0].getLocation(), description);
            }

            // CENTER ALIGNMENT

            layout.setAlignment(1);  // 1 == center

            assertLineCount(c.lineCount());

            for (int i = 0; i < c.lineCount(); i++) {
                TextLine line = layout.getLines()[i];
                String description = "centered " + contentType + ": line " + i + " for " + c;
                RectBounds expectedBounds = new RectBounds(CENTER - 0.5f * c.lineWidths.get(i), -ASCENT, CENTER + 0.5f * c.lineWidths.get(i), DESCENT);
                Point2D expectedLocation = new Point2D(CENTER - 0.5f * c.lineWidths.get(i), i * (ASCENT + DESCENT));

                assertEquals(expectedBounds, line.getBounds(), description);
                assertEquals(expectedLocation, line.getRuns()[0].getLocation(), description);
            }

            // RIGHT ALIGNMENT

            layout.setAlignment(2);  // 2 == right

            assertLineCount(c.lineCount());

            for (int i = 0; i < c.lineCount(); i++) {
                TextLine line = layout.getLines()[i];
                String description = "right aligned " + contentType + ": line " + i + " for " + c;
                RectBounds expectedBounds = new RectBounds(WRAP - c.lineWidths.get(i), -ASCENT, WRAP, DESCENT);
                Point2D expectedLocation = new Point2D(WRAP - c.lineWidths.get(i), i * (ASCENT + DESCENT));

                assertEquals(expectedBounds, line.getBounds(), description);
                assertEquals(expectedLocation, line.getRuns()[0].getLocation(), description);
            }

            // JUSTIFIED ALIGNMENT

            layout.setAlignment(3);  // 3 == justified

            assertLineCount(c.lineCount());

            for (int i = 0; i < c.lineCount(); i++) {
                TextLine line = layout.getLines()[i];
                String description = "justified " + contentType + ": line " + i + " for " + c;
                GlyphList[] runs = line.getRuns();
                boolean lastLine = i == c.lineCount() - 1 || (runs[runs.length - 1] instanceof TextRun tr && tr.isLinebreak());
                RectBounds expectedBounds = new RectBounds(0, -ASCENT, lastLine ? c.lineWidths.get(i) : WRAP, DESCENT);
                Point2D expectedLocation = new Point2D(0, i * (ASCENT + DESCENT));

                assertEquals(expectedBounds, line.getBounds(), description);
                assertEquals(expectedLocation, line.getRuns()[0].getLocation(), description);
            }
        }
    }

    private void assumeArialFontAvailable() {
        assumeTrue("Arial font missing", arialFont.getName().equals("Arial"));
    }

    private void assumeTahomaFontAvailable() {
        assumeTrue("Tahoma font missing", arialFont.getName().equals("Tahoma"));
    }

    private static void assumeMac() {
        assumeTrue("Platform is not Mac", PlatformUtil.isMac());
    }

    private static void assumeWindows() {
        assumeTrue("Platform is not Windows", PlatformUtil.isWindows());
    }
}
