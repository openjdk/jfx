/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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
package test.javafx.scene.control.skin;

import com.sun.javafx.scene.control.skin.Utils;
import com.sun.javafx.scene.text.FontHelper;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.text.GlyphLayout;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.pgstub.StubTextLayout;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This class tests the contract for fonts, text, and computed sizes.
 * While we only use the stub font and text framework here, we can still verify some rules
 * that apply for all headless tests.
 * Most rules are derived from the real font loading and text rendering in JavaFX.
 * <p>
 * See the Javadoc for every method to get more details.
 * @see test.com.sun.javafx.pgstub.StubFontLoader
 * @see test.com.sun.javafx.pgstub.StubTextLayout
 */
class TextLayoutUtilsContractTest {

    /**
     * An unknown font will fall back to the system font. Note that this is the same behavior in JavaFX.
     */
    @Test
    public void testUnknownFont() {
        Font font = new Font("bla", 10);

        assertEquals("bla", font.getName());
        assertEquals("System", font.getFamily());
        assertEquals("Regular", font.getStyle());
        assertEquals(10, font.getSize());

        assertEquals(100, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(10, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * Even if the font name is equal with a family name, we do not know the font by name.
     */
    @Test
    public void testFamilyAsFontName() {
        Font font = new Font("System", 10);

        assertEquals("System", font.getName());
        assertEquals("System", font.getFamily());
        assertEquals("Regular", font.getStyle());
        assertEquals(10, font.getSize());

        assertEquals(100, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(10, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * System Regular is the default font for headless testing but also for JavaFX itself.
     * 12 is the default font size for headless testing.
     * This is different from JavaFX, where the default size of the OS will be used.
     */
    @Test
    public void testDefaultFont() {
        Label lbl = new Label();
        Font defaultFont = lbl.getFont();

        assertEquals("System Regular", defaultFont.getName());
        assertEquals("System", defaultFont.getFamily());
        assertEquals("Regular", defaultFont.getStyle());
        assertEquals(12, defaultFont.getSize());

        assertEquals(120, Utils.computeTextWidth(defaultFont, "ABCDEFGHIJ", -1));
        assertEquals(12, Utils.computeTextHeight(defaultFont, "ABCDEFGHIJ", 0, null));
    }

    @Test
    public void testDefaultFontSet() {
        Label lbl = new Label();
        Font font = lbl.getFont();

        assertEquals("System Regular", font.getName());
        assertEquals(12, font.getSize());

        lbl.setFont(Font.font("system", FontWeight.BOLD, 20));

        font = lbl.getFont();
        assertEquals("System Bold", font.getName());
        assertEquals(20, font.getSize());

        assertEquals(210, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(20, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    @Test
    public void testDefaultFontCssSet() {
        Label lbl = new Label();
        Font font = lbl.getFont();

        assertEquals("System Regular", font.getName());
        assertEquals(12, font.getSize());

        StageLoader stageLoader = new StageLoader(lbl);

        lbl.setStyle("-fx-font: bold 20px System;");
        lbl.applyCss();

        stageLoader.dispose();

        font = lbl.getFont();
        assertEquals("System Bold", font.getName());
        assertEquals(20, font.getSize());

        assertEquals(210, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(20, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * System Regular is a font that is available for testing.
     */
    @Test
    public void testFontByName() {
        Font font = new Font("System Regular", 11);

        assertEquals(110, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(11, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * The System family is available for testing.
     */
    @Test
    public void testFontByFamily() {
        Font font = Font.font("System", 11);

        assertEquals("Regular", font.getStyle());
        assertEquals(110, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(11, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * The system font can be loaded with a normal weight.
     */
    @Test
    public void testFontByFamilyNormal() {
        Font font = Font.font("System", FontWeight.NORMAL, 11);

        assertEquals("Regular", font.getStyle());
        assertEquals(110, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(11, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * The system font can also be bold. In that case it is a bit wider than the normal one.
     */
    @Test
    public void testFontByFamilyBold() {
        Font font = Font.font("System", FontWeight.BOLD, 13);

        assertEquals("Bold", font.getStyle());
        assertEquals(140, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(13, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * The system font can also be italic.
     */
    @Test
    public void testFontByFamilyItalic() {
        Font font = Font.font("System", FontPosture.ITALIC, 11);

        assertEquals("Italic", font.getStyle());
        assertEquals(110, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(11, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * The system font can also be bold and Italic. In that case it is a bit wider than the normal one.
     */
    @Test
    public void testFontByFamilyBoldItalic() {
        Font font = Font.font("System", FontWeight.BOLD, FontPosture.ITALIC, 13);

        assertEquals("Bold Italic", font.getStyle());
        assertEquals(140, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(13, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * Amble is the other font we support for headless testing.
     */
    @Test
    public void testAmbleFont() {
        Font font = Font.font("Amble", 11);

        assertEquals(110, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(11, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * Tests that the text layout will cache layout results when the input parameters are the same.
     * We closely simulate the measurement calls here that are usually done by {@link Utils}.
     * <br>
     * As the stub text layout is very close to that of the actual JavaFX text layout toolchain,
     * we can test the actual caching logic by verifying that no glyph layout is created when there is a cache hit.
     */
    @Test
    public void testTextLayoutIsCached() {
        Font font = Font.font("Amble", 11);
        Object nativeFont = FontHelper.getNativeFont(font);

        AtomicInteger glyphLayoutCreationCounter = new AtomicInteger();

        StubTextLayout textLayout = new StubTextLayout(256) {
            @Override
            protected GlyphLayout glyphLayout() {
                glyphLayoutCreationCounter.addAndGet(1);
                return super.glyphLayout();
            }
        };
        setTextLayout(textLayout, "TEXT", nativeFont, TextLayout.BOUNDS_CENTER);
        assertEquals(1, glyphLayoutCreationCounter.get());

        setTextLayout(textLayout, "ANOTHER", nativeFont, TextLayout.BOUNDS_CENTER);
        assertEquals(2, glyphLayoutCreationCounter.get());

        // We set the same text again, so we expect a cache hit and no glyph layout creation.
        setTextLayout(textLayout, "TEXT", nativeFont, TextLayout.BOUNDS_CENTER);
        assertEquals(2, glyphLayoutCreationCounter.get());
    }

    /**
     * Tests that the text layout will not cache layout results when the bounds-type is not BOUNDS_CENTER.
     * We closely simulate the measurement calls here that are usually done by {@link Utils}.
     * <br>
     * As the stub text layout is very close to that of the actual JavaFX text layout toolchain,
     * we can test the actual caching logic by verifying that no glyph layout is created when there is a cache hit.
     */
    @Test
    public void testTextLayoutWithWrongBoundsTypeIsNotCached() {
        Font font = Font.font("Amble", 11);
        Object nativeFont = FontHelper.getNativeFont(font);

        AtomicInteger glyphLayoutCreationCounter = new AtomicInteger();

        StubTextLayout textLayout = new StubTextLayout(256) {
            @Override
            protected GlyphLayout glyphLayout() {
                glyphLayoutCreationCounter.addAndGet(1);
                return super.glyphLayout();
            }
        };
        setTextLayout(textLayout, "TEXT", nativeFont, 0);
        assertEquals(1, glyphLayoutCreationCounter.get());

        setTextLayout(textLayout, "ANOTHER", nativeFont, 0);
        assertEquals(2, glyphLayoutCreationCounter.get());

        // We set the same text again, but still expect a cache miss due to the set bounds-type.
        setTextLayout(textLayout, "TEXT", nativeFont, 0);
        assertEquals(3, glyphLayoutCreationCounter.get());
    }

    /**
     * Tests that the text layout will be cached and reused when changing the bounds-type from BOUNDS_CENTER to 0.
     * In this case, the runs will be reused.
     * We closely simulate the measurement calls here that are usually done by {@link Utils}.
     * <br>
     * As the stub text layout is very close to that of the actual JavaFX text layout toolchain,
     * we can test the actual caching logic by verifying that no glyph layout is created when there is a cache hit.
     */
    @Test
    public void testTextLayoutBoundsTypeChangeFromCenter() {
        Font font = Font.font("Amble", 11);
        Object nativeFont = FontHelper.getNativeFont(font);

        AtomicInteger glyphLayoutCreationCounter = new AtomicInteger();

        StubTextLayout textLayout = new StubTextLayout(256) {
            @Override
            protected GlyphLayout glyphLayout() {
                glyphLayoutCreationCounter.addAndGet(1);
                return super.glyphLayout();
            }
        };
        setTextLayout(textLayout, "TEXT", nativeFont, TextLayout.BOUNDS_CENTER);
        assertEquals(1, glyphLayoutCreationCounter.get());

        setTextLayout(textLayout, "TEXT", nativeFont, 0);
        assertEquals(1, glyphLayoutCreationCounter.get());

        setTextLayout(textLayout, "TEXT", nativeFont, TextLayout.BOUNDS_CENTER);
        assertEquals(1, glyphLayoutCreationCounter.get());
    }

    /**
     * Tests that the text layout will not be cached and reused when changing the bounds-type from 0 to BOUNDS_CENTER.
     * In this case, there will be no cache entry.
     * We closely simulate the measurement calls here that are usually done by {@link Utils}.
     * <br>
     * As the stub text layout is very close to that of the actual JavaFX text layout toolchain,
     * we can test the actual caching logic by verifying that no glyph layout is created when there is a cache hit.
     */
    @Test
    public void testTextLayoutBoundsTypeChangeFrom0() {
        Font font = Font.font("Amble", 11);
        Object nativeFont = FontHelper.getNativeFont(font);

        AtomicInteger glyphLayoutCreationCounter = new AtomicInteger();

        StubTextLayout textLayout = new StubTextLayout(256) {
            @Override
            protected GlyphLayout glyphLayout() {
                glyphLayoutCreationCounter.addAndGet(1);
                return super.glyphLayout();
            }
        };
        setTextLayout(textLayout, "TEXT", nativeFont, 0);
        assertEquals(1, glyphLayoutCreationCounter.get());

        setTextLayout(textLayout, "TEXT", nativeFont, TextLayout.BOUNDS_CENTER);
        assertEquals(2, glyphLayoutCreationCounter.get());

        setTextLayout(textLayout, "TEXT", nativeFont, 0);
        assertEquals(2, glyphLayoutCreationCounter.get());
    }

    private void setTextLayout(StubTextLayout textLayout, String text, Object nativeFont, int boundsType) {
        textLayout.setContent(text, nativeFont);
        textLayout.setWrapWidth(0);
        textLayout.setLineSpacing(0);
        textLayout.setBoundsType(boundsType);
        // We need to call this to ensure the layout run.
        assertNotNull(textLayout.getBounds());
    }

}
