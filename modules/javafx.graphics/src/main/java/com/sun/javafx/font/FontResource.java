/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;
import java.util.Map;

import com.sun.javafx.geom.transform.BaseTransform;

/**
 * Presently always represent a font file.
 * May extend what this exports as needed.
 */
public interface FontResource {

    /* Corresponds to FontSmoothingType enum values */
    public static final int AA_GREYSCALE = 0;
    public static final int AA_LCD       = 1;

    /* Font Features - not public API for now */
      // CSS naming
//    public static final int common_ligatures = 1 << 0;
//    public static final int discretionary_ligatures = 1 << 0;
//    public static final int historical_ligatures = 1 << 0;
//    public static final int contextual = 1 << 0;
//    public static final int small_caps = 1 << 0;
//    public static final int diagonal_fractions = 1 << 0;
//    public static final int stacked_fractions = 1 << 0;
//    public static final int slashed_zero = 1 << 0;

    // OpenType naming
    public static final int KERN = 1 << 0; // Kerning
    public static final int CLIG = 1 << 1; // Contextual Ligatures
    public static final int DLIG = 1 << 2; // Discretionary Ligatures
    public static final int HLIG = 1 << 3; // Historical Ligatures
    public static final int LIGA = 1 << 4; // Standard Ligatures
    public static final int RLIG = 1 << 5; // Required Liagtures
    public static final int LIGATURES = CLIG | DLIG | HLIG | LIGA | RLIG;
    public static final int SMCP = 1 << 6; // Small Capitals
    public static final int FRAC = 1 << 7; // Fractions
    public static final int AFRC = 1 << 8; // Alternative Fractions
    public static final int ZERO = 1 << 9; // Slashed Zero
    public static final int SWSH = 1 << 10; // Swash
    public static final int CSWH = 1 << 11; // Contextual Swash
    public static final int SALT = 1 << 12; // Stylistic Alternates
    public static final int NALT = 1 << 13; // Alternate Annotation Forms
    public static final int RUBY = 1 << 14; // Ruby Notation Forms
    public static final int SS01 = 1 << 15; // Stylistic Set 1
    public static final int SS02 = 1 << 16; // Stylistic Set 2
    public static final int SS03 = 1 << 17; // Stylistic Set 3
    public static final int SS04 = 1 << 18; // Stylistic Set 4
    public static final int SS05 = 1 << 19; // Stylistic Set 5
    public static final int SS06 = 1 << 20; // Stylistic Set 6
    public static final int SS07 = 1 << 21; // Stylistic Set 7
    //Note: the last two bits are reserved for layout. See GlyphLayout.

    public String getFullName();

    public String getPSName();

    public String getFamilyName();

    public String getFileName();

    public String getStyleName();

    public String getLocaleFullName();

    public String getLocaleFamilyName();

    public String getLocaleStyleName();

    public int getFeatures();

    public boolean isBold();

    public boolean isItalic();

    public float getAdvance(int gc, float size);

    public float[] getGlyphBoundingBox(int gc, float size, float[] retArr);

    public int getDefaultAAMode();

    public CharToGlyphMapper getGlyphMapper();

    public Map<FontStrikeDesc, WeakReference<FontStrike>> getStrikeMap();

    public FontStrike getStrike(float size, BaseTransform transform);
    public FontStrike getStrike(float size, BaseTransform transform,
                                int aaMode);
    public Object getPeer();
    public void setPeer(Object peer);

    public boolean isEmbeddedFont();

    public boolean isColorGlyph(int gc);

}
