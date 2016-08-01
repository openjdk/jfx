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

import java.util.HashMap;

public class CompositeGlyphMapper extends CharToGlyphMapper {

    public static final int SLOTMASK =  0xff000000;
    public static final int GLYPHMASK = 0x00ffffff;

    public static final int NBLOCKS = 216;
    public static final int BLOCKSZ = 256;
    public static final int MAXUNICODE = NBLOCKS*BLOCKSZ;

    private static final int SIMPLE_ASCII_MASK_START =  0x0020;
    private static final int SIMPLE_ASCII_MASK_END =    0x007e;
    private static final int ASCII_COUNT =
            SIMPLE_ASCII_MASK_END - SIMPLE_ASCII_MASK_START + 1;

    private boolean asciiCacheOK;
    private char charToGlyph[]; // Quick lookup

    CompositeFontResource font;
    CharToGlyphMapper slotMappers[];

    /* For now, we'll use a Map to store the char->glyph lookup result.
     * Maybe later I could use arrays for "common" values and
     * perhaps for less common values, just not cache at all if
     * lookup is relatively inexpensive. Or let the slot fonts do
     * the caching ? So a variety of strategies are possible.
     */
    HashMap<Integer, Integer> glyphMap;

    public CompositeGlyphMapper(CompositeFontResource compFont) {
        font = compFont;
        missingGlyph = 0; // TrueType font standard, avoids lookup.
        glyphMap = new HashMap<Integer, Integer>();
        slotMappers = new CharToGlyphMapper[compFont.getNumSlots()];
        asciiCacheOK = true;
    }

    private final CharToGlyphMapper getSlotMapper(int slot) {
        if (slot >= slotMappers.length) {
            CharToGlyphMapper[] tmp = new CharToGlyphMapper[font.getNumSlots()];
            System.arraycopy(slotMappers, 0, tmp, 0, slotMappers.length);
            slotMappers = tmp;
        }
        CharToGlyphMapper mapper = slotMappers[slot];
        if (mapper == null) {
            mapper = font.getSlotResource(slot).getGlyphMapper();
            slotMappers[slot] = mapper;
        }
        return mapper;
    }

    public int getMissingGlyphCode() {
        return missingGlyph;
    }

    /* Making the glyph codes of a composite including the first
     * slot have bits in the top byte set will indicate to the rendering
     * loops that they need to locate the glyphs by dereferencing to
     * the physical font strike.
     */
    public final int compositeGlyphCode(int slot, int glyphCode) {
        return ((slot) << 24 | (glyphCode & GLYPHMASK));
    }

    private final int convertToGlyph(int unicode) {
        for (int slot = 0; slot < font.getNumSlots(); slot++) {
            CharToGlyphMapper mapper = getSlotMapper(slot);
            int glyphCode = mapper.charToGlyph(unicode);
            if (glyphCode != mapper.getMissingGlyphCode()) {
                glyphCode = compositeGlyphCode(slot, glyphCode);
                glyphMap.put(unicode, glyphCode);
                return glyphCode;
            }
        }
        glyphMap.put(unicode, missingGlyph);
        return missingGlyph;
    }

    private int getAsciiGlyphCode(int charCode) {

        // Check if charCode is in ASCII range
        if (!asciiCacheOK ||
            (charCode > SIMPLE_ASCII_MASK_END) ||
            (charCode < SIMPLE_ASCII_MASK_START)) {
            return -1;
        }

        // Construct charToGlyph array of all ASCII characters
        if (charToGlyph == null) {
            char glyphCodes[] = new char[ASCII_COUNT];
            CharToGlyphMapper mapper = getSlotMapper(0);
            int missingGlyphCode = mapper.getMissingGlyphCode();
            for (int i = 0; i < ASCII_COUNT; i++) {
                int glyphCode = mapper.charToGlyph(SIMPLE_ASCII_MASK_START + i);
                if (glyphCode == missingGlyphCode) {
                    // If any glyphCode is missing, then do not use charToGlyph
                    // array.
                    charToGlyph = null;
                    asciiCacheOK = false;
                    return -1;
                }
                // Slot 0 mask is 0, so can use this glyphCode directly
                glyphCodes[i] = (char)glyphCode;
            }
            charToGlyph = glyphCodes;
        }

        int index = charCode - SIMPLE_ASCII_MASK_START;
        return charToGlyph[index];
    }

    public int getGlyphCode(int charCode) {
        // If ASCII then array lookup, else use glyphMap
        int retVal = getAsciiGlyphCode(charCode);
        if (retVal >= 0) {
            return retVal;
        }

        Integer codeInt = glyphMap.get(charCode);
        if (codeInt != null) {
            return codeInt.intValue();
        } else {
            return convertToGlyph(charCode);
        }
    }
}
