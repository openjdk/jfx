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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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

    private void assertLineLocations(Point2D... locations) {
        assertLineCount(locations.length);

        TextLine[] lines = layout.getLines();

        for (int i = 0; i < lines.length; i++) {
            assertEquals(locations[i], lines[i].getRuns()[0].getLocation(), "line " + i);
        }
    }

    private void assertGlyphsPerRun(int... glyphCount) {
        GlyphList[] runs = layout.getRuns();

        assertEquals(glyphCount.length, runs.length, "number of glyph counts given does not match number of runs");

        for (int i = 0; i < runs.length; i++) {
            assertEquals(glyphCount[i], runs[i].getGlyphCount(), "run " + i);
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
    void shouldWrapIgnoringTrailingWhiteSpace() {
        layout.setWrapWidth(200);

        setContent(layout, "The quick brown fox jumps over the lazy dog", font);

        layout.setAlignment(0);  // 0 == left

        assertGlyphsPerRun(26, 17);
        assertLineBounds(
            new RectBounds(0, -12, 187.23047f, 4.001953f),
            new RectBounds(0, -12, 122.41992f, 4.001953f)
        );
        assertLineLocations(
            new Point2D(0, 0),
            new Point2D(0, 16.001953f)
        );

        layout.setAlignment(1);  // 1 == center

        assertGlyphsPerRun(26, 17);
        assertLineBounds(
            new RectBounds(9.985352f, -12, 197.21582f, 4.001953f),
            new RectBounds(38.79004f, -12, 161.20996f, 4.001953f)
        );
        assertLineLocations(
            new Point2D(9.985352f, 0),
            new Point2D(38.79004f, 16.001953f)
        );

        layout.setAlignment(2);  // 2 == right

        assertGlyphsPerRun(26, 17);
        assertLineBounds(
            new RectBounds(19.970703f, -12, 207.20117f, 4.001953f),
            new RectBounds(77.58008f, -12, 200.0f, 4.001953f)
        );
        assertLineLocations(
            new Point2D(19.970703f, 0),
            new Point2D(77.58008f, 16.001953f)
        );

        layout.setAlignment(3);  // 3 == justify

        assertGlyphsPerRun(26, 17);
        assertLineBounds(
            new RectBounds(0, -12, 200.0f, 4.001953f),
            new RectBounds(0, -12, 122.41992f, 4.001953f)
        );
        assertLineLocations(
            new Point2D(0, 0),
            new Point2D(0, 16.001953f)
        );

        // Same tests with 10 additional spaces on the break point;
        // note how starting location of each line doesn't change for the same
        // alignment (but the bound width does) despite the different content:

        setContent(layout, "The quick brown fox jumps           over the lazy dog", font);

        layout.setAlignment(0);  // 0 == left

        assertGlyphsPerRun(36, 17);
        assertLineBounds(
            new RectBounds(0, -12, 259.2422f, 4.001953f),
            new RectBounds(0, -12, 122.41992f, 4.001953f)
        );
        assertLineLocations(
            new Point2D(0, 0),
            new Point2D(0, 16.001953f)
        );

        layout.setAlignment(1);  // 1 == center

        assertGlyphsPerRun(36, 17);
        assertLineBounds(
            new RectBounds(9.985352f, -12, 269.22754f, 4.001953f),
            new RectBounds(38.79004f, -12, 161.20996f, 4.001953f)
        );
        assertLineLocations(
            new Point2D(9.985352f, 0),
            new Point2D(38.79004f, 16.001953f)
        );

        layout.setAlignment(2);  // 2 == right

        assertGlyphsPerRun(36, 17);
        assertLineBounds(
            new RectBounds(19.970703f, -12, 279.2129f, 4.001953f),
            new RectBounds(77.58008f, -12, 200.0f, 4.001953f)
        );
        assertLineLocations(
            new Point2D(19.970703f, 0),
            new Point2D(77.58008f, 16.001953f)
        );

        layout.setAlignment(3);  // 3 == justify

        assertGlyphsPerRun(36, 17);
        assertLineBounds(
            new RectBounds(0, -12, 259.2422f, 4.001953f),
            new RectBounds(0, -12, 122.41992f, 4.001953f)
        );
        assertLineLocations(
            new Point2D(0, 0),
            new Point2D(0, 16.001953f)
        );
    }

    @Test
    void shouldWrapIgnoringTrailingWhiteSpaceComplex() {
        layout.setWrapWidth(200);

        setContent(layout, "The quick brown लोमड़ी jumps over the lazy कुत्ता", font);

        layout.setAlignment(0);  // 0 == left

        assertGlyphsPerRun(16, 6, 6, 14, 4);
        assertLineBounds(
            new RectBounds(0, -12, 197.09766f, 4.001953f),
            new RectBounds(0, -12, 122.583984f, 4.001953f)
        );
        assertLineLocations(
            new Point2D(0, 0),
            new Point2D(0, 16.001953f)
        );

        layout.setAlignment(1);  // 1 == center

        assertGlyphsPerRun(16, 6, 6, 14, 4);
        assertLineBounds(
            new RectBounds(5.051758f, -12, 202.14941f, 4.001953f),
            new RectBounds(38.708008f, -12, 161.29199f, 4.001953f)
        );
        assertLineLocations(
            new Point2D(5.051758f, 0),
            new Point2D(38.708008f, 16.001953f)
        );

        layout.setAlignment(2);  // 2 == right

        assertGlyphsPerRun(16, 6, 6, 14, 4);
        assertLineBounds(
            new RectBounds(10.103516f, -12, 207.20117f, 4.001953f),
            new RectBounds(77.416016f, -12, 200.0f, 4.001953f)
        );
        assertLineLocations(
            new Point2D(10.103516f, 0),
            new Point2D(77.416016f, 16.001953f)
        );

        layout.setAlignment(3);  // 3 == justify

        assertGlyphsPerRun(16, 6, 6, 14, 4);
        assertLineBounds(
            new RectBounds(0, -12, 200.0f, 4.001953f),
            new RectBounds(0, -12, 122.583984f, 4.001953f)
        );
        assertLineLocations(
            new Point2D(0, 0),
            new Point2D(0, 16.001953f)
        );

        // Same tests with 10 additional spaces on the break point;
        // note how starting location of each line doesn't change for the same
        // alignment (but the bound width does) despite the different content:

        setContent(layout, "The quick brown लोमड़ी jumps           over the lazy कुत्ता", font);

        layout.setAlignment(0);  // 0 == left

        assertGlyphsPerRun(16, 6, 16, 14, 4);
        assertLineBounds(
            new RectBounds(0, -12, 269.10938f, 4.001953f),
            new RectBounds(0, -12, 122.583984f, 4.001953f)
        );
        assertLineLocations(
            new Point2D(0, 0),
            new Point2D(0, 16.001953f)
        );

        layout.setAlignment(1);  // 1 == center

        assertGlyphsPerRun(16, 6, 16, 14, 4);
        assertLineBounds(
            new RectBounds(5.051758f, -12, 274.16113f, 4.001953f),
            new RectBounds(38.708008f, -12, 161.29199f, 4.001953f)
        );
        assertLineLocations(
            new Point2D(5.051758f, 0),
            new Point2D(38.708008f, 16.001953f)
        );

        layout.setAlignment(2);  // 2 == right

        assertGlyphsPerRun(16, 6, 16, 14, 4);
        assertLineBounds(
            new RectBounds(10.103516f, -12, 279.2129f, 4.001953f),
            new RectBounds(77.416016f, -12, 200.0f, 4.001953f)
        );
        assertLineLocations(
            new Point2D(10.103516f, 0),
            new Point2D(77.416016f, 16.001953f)
        );

        layout.setAlignment(3);  // 3 == justify

        assertGlyphsPerRun(16, 6, 16, 14, 4);
        assertLineBounds(
            new RectBounds(0, -12, 269.10938f, 4.001953f),
            new RectBounds(0, -12, 122.583984f, 4.001953f)
        );
        assertLineLocations(
            new Point2D(0, 0),
            new Point2D(0, 16.001953f)
        );
    }
}
