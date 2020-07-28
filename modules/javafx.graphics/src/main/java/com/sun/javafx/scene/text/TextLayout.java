/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.text;

import javafx.scene.shape.PathElement;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Shape;

public interface TextLayout {

    /* Internal flags Flags */
    static final int FLAGS_LINES_VALID      = 1 << 0; /* unused */
    static final int FLAGS_ANALYSIS_VALID   = 1 << 1;
    static final int FLAGS_HAS_TABS         = 1 << 2;
    static final int FLAGS_HAS_BIDI         = 1 << 3;
    static final int FLAGS_HAS_COMPLEX      = 1 << 4;
    static final int FLAGS_HAS_EMBEDDED     = 1 << 5;
    static final int FLAGS_HAS_CJK          = 1 << 6;
    static final int FLAGS_WRAPPED          = 1 << 7;
    static final int FLAGS_RTL_BASE         = 1 << 8;
    static final int FLAGS_CACHED_UNDERLINE      = 1 << 9;
    static final int FLAGS_CACHED_STRIKETHROUGH  = 1 << 10;
    static final int FLAGS_LAST             = 1 << 11;

    static final int ANALYSIS_MASK = FLAGS_LAST - 1;

    /* Text Layout compact internal representation */
    static final int ALIGN_LEFT     = 1 << 18;
    static final int ALIGN_CENTER   = 1 << 19;
    static final int ALIGN_RIGHT    = 1 << 20;
    static final int ALIGN_JUSTIFY  = 1 << 21;

    static final int ALIGN_MASK = ALIGN_LEFT | ALIGN_CENTER |
                                  ALIGN_RIGHT | ALIGN_JUSTIFY;

    public static final int DIRECTION_LTR          = 1 << 10;
    public static final int DIRECTION_RTL          = 1 << 11;
    public static final int DIRECTION_DEFAULT_LTR  = 1 << 12;
    public static final int DIRECTION_DEFAULT_RTL  = 1 << 13;

    static final int DIRECTION_MASK = DIRECTION_LTR | DIRECTION_RTL |
                                      DIRECTION_DEFAULT_LTR |
                                      DIRECTION_DEFAULT_RTL;

    public static final int BOUNDS_CENTER       = 1 << 14;
    public static final int BOUNDS_MASK = BOUNDS_CENTER;

    public static final int TYPE_TEXT           = 1 << 0;
    public static final int TYPE_UNDERLINE      = 1 << 1;
    public static final int TYPE_STRIKETHROUGH  = 1 << 2;
    public static final int TYPE_BASELINE       = 1 << 3;
    public static final int TYPE_TOP            = 1 << 4;
    public static final int TYPE_BEARINGS       = 1 << 5;

    public static final int DEFAULT_TAB_SIZE = 8;

    public static class Hit {
        int charIndex;
        int insertionIndex;
        boolean leading;

        public Hit(int charIndex, int insertionIndex, boolean leading) {
            this.charIndex = charIndex;
            this.insertionIndex = insertionIndex;
            this.leading = leading;
        }

        public int getCharIndex() { return charIndex; }
        public int getInsertionIndex() { return insertionIndex; }
        public boolean isLeading() { return leading; }
    }

    /**
     * Sets the content for the TextLayout. Supports multiple spans (rich text).
     *
     * @return returns true is the call modifies the layout internal state.
     */
    public boolean setContent(TextSpan[] spans);

    /**
     * Sets the content for the TextLayout. Shorthand for single span text
     * (no rich text).
     *
     * @return returns true is the call modifies the layout internal state.
     */
    public boolean setContent(String string, Object font);

    /**
     * Sets the alignment for the TextLayout.
     *
     * @return returns true is the call modifies the layout internal state.
     */
    public boolean setAlignment(/*TextAlignment*/ int alignment);

    /**
     * Sets the wrap width for the TextLayout.
     *
     * @return returns true is the call modifies the layout internal state.
     */
    public boolean setWrapWidth(float wrapWidth);

    /**
     * Sets the line spacing for the TextLayout.
     *
     * @return returns true is the call modifies the layout internal state.
     */
    public boolean setLineSpacing(float spacing);

    /**
     * Sets the direction (bidi algorithm's) for the TextLayout.
     *
     * @return returns true is the call modifies the layout internal state.
     */
    public boolean setDirection(int direction);

    /**
     * Sets the bounds type for the TextLayout.
     *
     * @return returns true is the call modifies the layout internal state.
     */
    public boolean setBoundsType(int type);

    /**
     * Returns the (logical) bounds of the layout
     * minX is always zero
     * minY is the ascent of the first line (negative)
     * width the width of the widest line
     * height the sum of all lines height
     *
     * Note that this width is different the wrapping width!
     *
     * @return the layout bounds
     */
    public BaseBounds getBounds();

    public BaseBounds getBounds(TextSpan filter, BaseBounds bounds);

    /**
     * Returns the visual bounds of the layout using glyph bounding box
     *
     * @return the visual bounds
     */
    public BaseBounds getVisualBounds(int type);

    /**
     * Returns the lines of text layout.
     *
     * @return the text lines
     */
    public TextLine[] getLines();

    /**
     * Returns the GlyphList of text layout.
     * The runs are returned order visually (rendering order), starting
     * from the first line.
     *
     * @return the runs
     */
    public GlyphList[] getRuns();

    /**
     * Returns the shape of the entire text layout relative to the baseline
     * of the first line.
     *
     * @param type the type of the shapes to include
     * @return the shape
     */
    public Shape getShape(int type, TextSpan filter);

    /**
     * Sets the tab size for the TextLayout.
     *
     * @param spaces the number of spaces represented by a tab. Default is 8.
     * Minimum is 1, lower values will be clamped to 1.
     * @return returns true if the call modifies the layout internal state.
     */
    public boolean setTabSize(int spaces);

    public Hit getHitInfo(float x, float y);

    public PathElement[] getCaretShape(int offset, boolean isLeading,
                                       float x, float y);
    public PathElement[] getRange(int start, int end, int type,
                                  float x, float y);
}
