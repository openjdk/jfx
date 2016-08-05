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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.javafx.geom.transform.BaseTransform;


/*
 * Create a singleton fallback resource per style to be shared across
 * all physical fonts. "Per style" refers to the Bold and Italic styles,
 * thus will (eventually) be 4 in all.
 */

class FallbackResource implements CompositeFontResource {


    private ArrayList<String> linkedFontFiles;
    private ArrayList<String> linkedFontNames;
    private FontResource[] fallbacks;
    private FontResource[] nativeFallbacks;
    private boolean isBold, isItalic;
    private int aaMode;
    private CompositeGlyphMapper mapper;

    Map<FontStrikeDesc, WeakReference<FontStrike>> strikeMap =
       new ConcurrentHashMap<FontStrikeDesc, WeakReference<FontStrike>>();


    public Map<FontStrikeDesc, WeakReference<FontStrike>> getStrikeMap() {
        return strikeMap;
    }

    FallbackResource(boolean bold, boolean italic, int aaMode) {
        this.isBold = bold;
        this.isItalic = italic;
        this.aaMode = aaMode;
    }

    static FallbackResource[] greyFallBackResource = new FallbackResource[4];
    static FallbackResource[] lcdFallBackResource = new FallbackResource[4];

    static FallbackResource
        getFallbackResource(boolean bold, boolean italic, int aaMode) {
        FallbackResource[] arr =
            (aaMode == FontResource.AA_GREYSCALE) ?
            greyFallBackResource : lcdFallBackResource;
        int index = bold ? 1 : 0;
        if (italic) {
            index +=2;
        }
        FallbackResource font = arr[index];
        if (font == null) {
            font = new FallbackResource(bold, italic, aaMode);
            arr[index] = font;
        }
        return font;
    }

    public int getDefaultAAMode() {
        return aaMode;  // for now, has to be same as main font.
    }

    /* Superficially implement the FontResource interface,
     * but for this fall back resource, we should not be asked for
     * the names or number of glyphs as those are reported for the
     * primary font.
     */
    private String throwException() {
         throw new UnsupportedOperationException("Not supported");
    }

    public String getFullName() {
        return throwException();
    }

    public String getPSName() {
        return throwException();
    }

    public String getFamilyName() {
        return throwException();
    }

    public String getStyleName() {
        return throwException();
    }

    public String getLocaleFullName() {
        return throwException();
    }

    public String getLocaleFamilyName() {
        return throwException();
    }

    public String getLocaleStyleName() {
        return throwException();
    }


    public boolean isBold() {
        throw new UnsupportedOperationException("Not supported");
    }

    public boolean isItalic() {
        throw new UnsupportedOperationException("Not supported");

    }

    public int getFeatures() {
        throw new UnsupportedOperationException("Not supported");
    }

    public String getFileName() {
        return throwException();
    }

    public Object getPeer() {
        return null;
    }

    public void setPeer(Object peer) {
        throwException();
    }

    public boolean isEmbeddedFont() {
        return false;
    }

    public CharToGlyphMapper getGlyphMapper() {
        if (mapper == null) {
            mapper = new CompositeGlyphMapper(this);
        }
        return mapper;
    }

    public int getSlotForFont(String fontName) {
        getLinkedFonts();
        int i = 0;
        for (String linkedFontName : linkedFontNames) {
            if (fontName.equalsIgnoreCase(linkedFontName)) {
                return i;
            }
            i++;
        }
        if (nativeFallbacks != null) {
            for (FontResource nativeFallback : nativeFallbacks) {
                if (fontName.equalsIgnoreCase(nativeFallback.getFullName())) {
                    return i;
                }
                i++;
            }
        }

        if (i >= 0x7E) {
            /* There are 8bits (0xFF) reserved in a glyph code to store the slot
             * number. The first bit cannot be set to avoid negative values
             * (leaving 0x7F). The extra -1 (leaving 0x7E) is to account for
             * the primary font resource in PrismCompositeFontResource.
             */
            if (PrismFontFactory.debugFonts) {
                System.err.println("\tToo many font fallbacks!");
            }
            return -1;
        }
        PrismFontFactory factory = PrismFontFactory.getFontFactory();
        FontResource fr = factory.getFontResource(fontName, null, false);
        if (fr == null) {
            if (PrismFontFactory.debugFonts) {
                System.err.println("\t Font name not supported \"" + fontName + "\".");
            }
            return -1;
        }

        /* Add the font to the list of native fallbacks */
        FontResource[] tmp;
        if (nativeFallbacks == null) {
            tmp = new FontResource[1];
        } else {
            tmp = new FontResource[nativeFallbacks.length + 1];
            System.arraycopy(nativeFallbacks, 0, tmp, 0, nativeFallbacks.length);
        }
        tmp[tmp.length - 1] = fr;
        nativeFallbacks = tmp;
        return i;
    }

    /* To start with we will use the exact same fall back list for
     * everything.
     */
    private void getLinkedFonts() {
        if (fallbacks == null) {
            if (PrismFontFactory.isLinux) {
                FontConfigManager.FcCompFont font =
                    FontConfigManager.getFontConfigFont("sans",
                                                        isBold, isItalic);
                linkedFontFiles = FontConfigManager.getFileNames(font, false);
                linkedFontNames = FontConfigManager.getFontNames(font, false);
                fallbacks = new FontResource[linkedFontFiles.size()];
            } else {
                ArrayList<String>[] linkedFontInfo;
                if (PrismFontFactory.isMacOSX) {
                    linkedFontInfo =
                        PrismFontFactory.getLinkedFonts("Arial Unicode MS", true);
                } else {
                    linkedFontInfo =
                        PrismFontFactory.getLinkedFonts("Tahoma", true);
                }
                linkedFontFiles = linkedFontInfo[0];
                linkedFontNames = linkedFontInfo[1];
                fallbacks = new FontResource[linkedFontFiles.size()];
            }
        }
    }

    public int getNumSlots() {
        getLinkedFonts();
        int num = linkedFontFiles.size();
        if (nativeFallbacks != null) {
            num += nativeFallbacks.length;
        }
        return num;
    }

    public float[] getGlyphBoundingBox(int glyphCode,
                                float size, float[] retArr) {
        int slot = (glyphCode >>> 24);
        int slotglyphCode = glyphCode & CompositeGlyphMapper.GLYPHMASK;
        FontResource slotResource = getSlotResource(slot);
        return slotResource.getGlyphBoundingBox(slotglyphCode, size, retArr);
    }

    public float getAdvance(int glyphCode, float size) {
        int slot = (glyphCode >>> 24);
        int slotglyphCode = glyphCode & CompositeGlyphMapper.GLYPHMASK;
        FontResource slotResource = getSlotResource(slot);
        return slotResource.getAdvance(slotglyphCode, size);
    }

    public synchronized FontResource getSlotResource(int slot) {
        getLinkedFonts();
        if (slot >= fallbacks.length) {
            slot = slot - fallbacks.length;
            if (nativeFallbacks == null || slot >= nativeFallbacks.length) {
                return null;
            }
            return nativeFallbacks[slot];
        }
        if (fallbacks[slot] == null) {
            String file = linkedFontFiles.get(slot);
            String name = linkedFontNames.get(slot);
            fallbacks[slot] =
                PrismFontFactory.getFontFactory().
                getFontResource(name, file, false);
        }
        return fallbacks[slot];
    }

    public FontStrike getStrike(float size, BaseTransform transform) {
        return getStrike(size, transform, getDefaultAAMode());
    }

    public FontStrike getStrike(float size, BaseTransform transform,
                                int aaMode) {

        FontStrikeDesc desc = new FontStrikeDesc(size, transform, aaMode);
        WeakReference<FontStrike> ref = strikeMap.get(desc);
        CompositeStrike strike = null;

        if (ref != null) {
            strike = (CompositeStrike)ref.get();
        }
        if (strike == null) {
            strike = new CompositeStrike(this, size, transform, aaMode, desc);
            if (strike.disposer != null) {
                ref = Disposer.addRecord(strike, strike.disposer);
            } else {
                ref = new WeakReference<FontStrike>(strike);
            }
            strikeMap.put(desc, ref);
        }
        return strike;
    }
}
