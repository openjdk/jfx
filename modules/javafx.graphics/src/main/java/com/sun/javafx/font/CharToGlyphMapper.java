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

/*
 * NB the versions that take a char as an int are used by the opentype
 * layout engine. If that remains in native these methods may not be
 * needed in the Java class.
 */
public abstract class CharToGlyphMapper {

    public static final int HI_SURROGATE_SHIFT = 10;
    public static final int HI_SURROGATE_START = 0xD800;
    public static final int HI_SURROGATE_END = 0xDBFF;
    public static final int LO_SURROGATE_START = 0xDC00;
    public static final int LO_SURROGATE_END = 0xDFFF;
    public static final int SURROGATES_START = 0x10000;

    public static final int MISSING_GLYPH = 0;
    public static final int INVISIBLE_GLYPH_ID = 0xffff;

    protected int missingGlyph = MISSING_GLYPH;

    public boolean canDisplay(char cp) {
        int glyph = charToGlyph(cp);
        return glyph != missingGlyph;
    }

    public int getMissingGlyphCode() {
        return missingGlyph;
    }

    public abstract int getGlyphCode(int charCode);

    public int charToGlyph(char unicode) {
        return getGlyphCode(unicode);
    }

    public int charToGlyph(int unicode) {
        return getGlyphCode(unicode);
    }

    public void charsToGlyphs(int start, int count, char[] unicodes,
                              int[] glyphs, int glyphStart) {
        for (int i=0; i<count; i++) {
            int code = unicodes[start + i]; // char is unsigned.
            if (code >= HI_SURROGATE_START &&
                code <= HI_SURROGATE_END && i + 1 < count) {
                char low = unicodes[start + i + 1];

                if (low >= LO_SURROGATE_START &&
                    low <= LO_SURROGATE_END) {
                    code = ((code - HI_SURROGATE_START) << HI_SURROGATE_SHIFT) +
                        low - LO_SURROGATE_START + SURROGATES_START;
                    glyphs[glyphStart + i] = getGlyphCode(code);
                    i += 1; // Empty glyph slot after surrogate
                    glyphs[glyphStart + i] = INVISIBLE_GLYPH_ID;
                    continue;
                }
            }
            glyphs[glyphStart + i] = getGlyphCode(code);
        }
    }

    public void charsToGlyphs(int start, int count, char[] unicodes, int[] glyphs) {
        charsToGlyphs(start, count, unicodes, glyphs, 0);
    }

    public void charsToGlyphs(int count, char[] unicodes, int[] glyphs) {
        charsToGlyphs(0, count, unicodes, glyphs, 0);
    }

}
