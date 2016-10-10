/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk;

import javafx.scene.text.Font;

/**
 * The metrics for a Font, including helper functions for computing string
 * width/height and other metrics based on a specific String of text with
 * this font.
 */
public class FontMetrics {

    public static FontMetrics createFontMetrics(
            float maxAscent, float ascent, float xheight, float descent,
            float maxDescent, float leading, Font font) {
        return new FontMetrics(maxAscent, ascent, xheight, descent, maxDescent, leading, font);
    }

    /**
     * The distance from the baseline to the max character height.
     * This value is always positive
     */
    private float maxAscent;

    public final float getMaxAscent() {
        return maxAscent;
    }

    /**
     * The distance from the baseline to the avg max character height.
     * this value is always positive
     */
    private float ascent;

    public final float getAscent() {
        return ascent;
    }

    /**
     * The distance from the baseline to the top of the avg. lowercase letter.
     */
    private float xheight;

    public final float getXheight() {
        return xheight;
    }

    /**
     * The baseline is the imaginary line upon which letters without descenders
     * (for example, the lowercase letter "a") sits. In terms of the font
     * metrics, all other metrics are derived from this point. This point is
     * implicitly defined as zero.
     */
    private int baseline;

    public final int getBaseline() {
        return baseline;
    }

    /**
     * The distance from the baseline down to the lowest avg. descender. This
     * value is always positive
     */
    private float descent;

    public final float getDescent() {
        return descent;
    }

    /**
     * The distance from the baseline down to the absolute lowest descender.
     * this value is always positive
     */
    private float maxDescent;

    public final float getMaxDescent() {
        return maxDescent;
    }

    /**
     * The amount of space between lines of text in this font. This is the
     * amount of space between he maxDecent of one line and the maxAscent
     * of the next. This number is included in the lineHeight.
     */
    private float leading;

    public final float getLeading() {
        return leading;
    }

    /**
     * The maximum line height for a line of text in this font
     */
    private float lineHeight;

    public final float getLineHeight() {
        return lineHeight;
    }

    /**
     * The font that was used to construct these metrics.
     */
    private Font font;

    public final Font getFont() {
        if (font == null) {
            font = Font.getDefault();
        }
        return font;
    }

    public FontMetrics(
            float maxAscent, float ascent, float xheight, float descent,
            float maxDescent, float leading, Font font) {
        this.maxAscent = maxAscent;
        this.ascent = ascent;
        this.xheight = xheight;
        this.descent = descent;
        this.maxDescent = maxDescent;
        this.leading = leading;
        this.font = font;
        lineHeight = maxAscent + maxDescent + leading;
    }

    /**
     * Computes the width of the char when rendered with the font represented
     * by this FontMetrics instance.
     */
    public float getCharWidth(char ch) {
        return Toolkit.getToolkit().getFontLoader().getCharWidth(ch, getFont());
    }

    @Override public String toString() {
        return "FontMetrics: [maxAscent=" + getMaxAscent()
            + ", ascent=" + getAscent()
            + ", xheight=" + getXheight()
            + ", baseline=" + getBaseline()
            + ", descent=" + getDescent()
            + ", maxDescent=" + getMaxDescent()
            + ", leading=" + getLeading()
            + ", lineHeight=" + getLineHeight()
            + ", font=" + getFont() + "]";
    }
}
