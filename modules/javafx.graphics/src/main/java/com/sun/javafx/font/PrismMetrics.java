/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font;


public class PrismMetrics implements Metrics {

    PrismFontFile fontResource;
    float ascent, descent, linegap;
    private float[] styleMetrics;
    float size;

    static final int XHEIGHT = 0;
    static final int CAPHEIGHT = 1;
    static final int TYPO_ASCENT = 2;
    static final int TYPO_DESCENT = 3;
    static final int TYPO_LINEGAP = 4;
    static final int STRIKETHROUGH_THICKNESS = 5;
    static final int STRIKETHROUGH_OFFSET = 6;
    static final int UNDERLINE_THICKESS = 7;
    static final int UNDERLINE_OFFSET = 8;
    static final int METRICS_TOTAL = 9;

    PrismMetrics(float ascent, float descent, float linegap,
               PrismFontFile fontResource, float size) {
        this.ascent = ascent;
        this.descent = descent;
        this.linegap = linegap;
        this.fontResource = fontResource;
        this.size = size;
    }

    public float getAscent() {
        return ascent;
    }

    public float getDescent() {
        return descent;
    }

    public float getLineGap() {
        return linegap;
    }

    public float getLineHeight() {
        return -ascent + descent + linegap;
    }

    private void checkStyleMetrics() {
        if (styleMetrics == null) {
            styleMetrics = fontResource.getStyleMetrics(size);
        }
    }

    public float getTypoAscent() {
        checkStyleMetrics();
        return styleMetrics[TYPO_ASCENT];
    }

    public float getTypoDescent() {
        checkStyleMetrics();
        return styleMetrics[TYPO_DESCENT];
    }

    public float getTypoLineGap() {
        checkStyleMetrics();
        return styleMetrics[TYPO_LINEGAP];
    }

    public float getCapHeight() {
        checkStyleMetrics();
        return styleMetrics[CAPHEIGHT];
    }

    public float getXHeight() {
        checkStyleMetrics();
        return styleMetrics[XHEIGHT];
    }

    public float getStrikethroughOffset() {
        checkStyleMetrics();
        return styleMetrics[STRIKETHROUGH_OFFSET];
    }

    public float getStrikethroughThickness() {
        checkStyleMetrics();
        return styleMetrics[STRIKETHROUGH_THICKNESS];
    }

    public float getUnderLineOffset() {
        checkStyleMetrics();
        return styleMetrics[UNDERLINE_OFFSET];
    }

    public float getUnderLineThickness() {
        checkStyleMetrics();
        return styleMetrics[UNDERLINE_THICKESS];
    }

    public String toString() {
        return
            "ascent = " + getAscent() +
            " descent = " + getDescent() +
            " linegap = " + getLineGap() +
            " lineheight = " +getLineHeight();
    }
}
