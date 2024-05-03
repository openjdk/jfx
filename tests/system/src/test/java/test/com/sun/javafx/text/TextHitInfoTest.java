/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.jupiter.api.Test;

import com.sun.javafx.font.PGFont;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.scene.text.FontHelper;
import com.sun.javafx.scene.text.TextLayout.Hit;
import com.sun.javafx.scene.text.TextSpan;
import com.sun.javafx.text.PrismTextLayout;

import javafx.scene.text.Font;

public class TextHitInfoTest {
    private final PrismTextLayout layout = new PrismTextLayout();
    private final PGFont arialFont = (PGFont) FontHelper.getNativeFont(Font.font("Arial", 12));

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

    @Test
    void getHitInfoTest() {
        assumeArialFontAvailable();

        /*
         * Empty line:
         */

        layout.setContent("", arialFont);

        // Checks that hits above the line results in first character:
        assertEquals(new Hit(0, 0, true), layout.getHitInfo(0, -30));

        // Checks before start of line:
        assertEquals(new Hit(0, 0, true), layout.getHitInfo(-50, 0));

        // Checks position of empty string:
        assertEquals(new Hit(0, 0, true), layout.getHitInfo(0, 0));

        // Checks past end of line:
        assertEquals(new Hit(0, 0, true), layout.getHitInfo(250, 0));

        // Checks that hits below the line results in last character + 1:
        assertEquals(new Hit(0, 1, false), layout.getHitInfo(0, 30));

        /*
         * Single line:
         */

        layout.setContent("The quick brown fox jumps over the lazy dog", arialFont);

        // Checks that hits above the line results in first character:
        assertEquals(new Hit(0, 0, true), layout.getHitInfo(0, -30));

        // Checks before start of line:
        assertEquals(new Hit(0, 0, true), layout.getHitInfo(-50, 0));

        // Checks positions of a few characters:
        assertEquals(new Hit(0, 0, true), layout.getHitInfo(0, 0));  // Start of "T"
        assertEquals(new Hit(0, 1, false), layout.getHitInfo(5, 0));  // Past halfway of "T"
        assertEquals(new Hit(1, 1, true), layout.getHitInfo(10, 0));  // Start of "h"

        // Checks past end of line:
        assertEquals(new Hit(42, 43, false), layout.getHitInfo(250, 0));

        // Checks that hits below the line results in last character + 1:
        assertEquals(new Hit(43, 44, false), layout.getHitInfo(0, 30));

        /*
         * Multi line:
         */

        layout.setContent("The\nquick\nbrown\nfox\n", arialFont);

        // Checks that hits above the first line results in first character:
        assertEquals(new Hit(0, 0, true), layout.getHitInfo(0, -30));

        // Checks before start of first line:
        assertEquals(new Hit(0, 0, true), layout.getHitInfo(-50, 0));

        // Checks positions of a few characters on first line:
        assertEquals(new Hit(0, 0, true), layout.getHitInfo(0, 0));  // Start of "T"
        assertEquals(new Hit(0, 1, false), layout.getHitInfo(5, 0));  // Halfway past "T"
        assertEquals(new Hit(1, 1, true), layout.getHitInfo(10, 0));  // Start of "h"

        // Checks past end of first line:
        assertEquals(new Hit(2, 3, false), layout.getHitInfo(250, 0));

        // Checks before start of second line:
        assertEquals(new Hit(4, 4, true), layout.getHitInfo(-50, 15));

        // Check second line:
        assertEquals(new Hit(4, 4, true), layout.getHitInfo(0, 15));  // Start of "q"

        // Checks past end of second line:
        assertEquals(new Hit(8, 9, false), layout.getHitInfo(250, 15));

        /*
         * Test with two spans:
         */

        layout.setContent(new TestSpan[] {new TestSpan("Two", arialFont), new TestSpan("Spans", arialFont)});

        // Checks that hits above the line results in first character:
        assertEquals(new Hit(0, 0, true), layout.getHitInfo(0, -30));

        // Checks before start of line:
        assertEquals(new Hit(0, 0, true), layout.getHitInfo(-50, 0));

        // Checks positions of a few characters:
        assertEquals(new Hit(0, 0, true), layout.getHitInfo(0, 0));  // Start of "T"
        assertEquals(new Hit(0, 1, false), layout.getHitInfo(5, 0));  // Past halfway of "T"
        assertEquals(new Hit(1, 1, true), layout.getHitInfo(10, 0));  // Start of "w"

        assertEquals(new Hit(7, 8, false), layout.getHitInfo(60, 0));  // Past halfway of "s"

        // Checks past end of line:
        assertEquals(new Hit(7, 8, false), layout.getHitInfo(250, 0));

        // Checks that hits below the line results in last character + 1:
        assertEquals(new Hit(8, 9, false), layout.getHitInfo(0, 30));

        /*
         * Test with zero spans:
         */

        layout.setContent(new TestSpan[] {});

        // Checks that hits above the line results in first character:
        assertEquals(new Hit(0, 0, true), layout.getHitInfo(0, -30));

        // Checks before start of line:
        assertEquals(new Hit(0, 1, false), layout.getHitInfo(-50, 0));

        // Checks positions of center:
        assertEquals(new Hit(0, 1, false), layout.getHitInfo(0, 0));  // Start of "T"

        // Checks past end of line:
        assertEquals(new Hit(0, 1, false), layout.getHitInfo(250, 0));

        // Checks that hits below the line results in last character + 1:
        assertEquals(new Hit(0, 1, false), layout.getHitInfo(0, 30));

    }

    private void assumeArialFontAvailable() {
        assumeTrue("Arial font missing", arialFont.getName().equals("Arial"));
    }
}
