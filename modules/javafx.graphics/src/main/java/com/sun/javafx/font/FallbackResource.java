/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * Fallback fonts may differ depending on the primary resource.
 * Additionally it may differ based on style even if it is otherwise
 * the same for multiple fonts.
 */

public class FallbackResource implements CompositeFontResource {


    FontResource primaryResource;
    private String[] linkedFontFiles;
    private String[] linkedFontNames;
    private FontResource[] fallbacks;
    private FontResource[] nativeFallbacks;
    private boolean isBold, isItalic;
    private int aaMode;
    private CompositeGlyphMapper mapper;

    Map<FontStrikeDesc, WeakReference<FontStrike>> strikeMap = new ConcurrentHashMap<>();


    @Override
    public Map<FontStrikeDesc, WeakReference<FontStrike>> getStrikeMap() {
        return strikeMap;
    }

    /*
     * Initially this is used only on macOS where the cascading list for a
     * resource may include font variations and system fonts that cannot
     * be directly instantiated. So we need to install resources which already
     * wrap a native reference to these fonts as we won't be successful
     * in requesting them from native using name+file.
     * I hope we should still be able to share these in the global
     * name->font map so its not too wasteful.
     */
    FallbackResource(FontResource primary) {
        primaryResource = primary;
        aaMode = primaryResource.getDefaultAAMode();
        isBold = primaryResource.isBold();
        isItalic = primaryResource.isItalic();
    }

    static FallbackResource getFallbackResource(FontResource primaryResource) {
        return new FallbackResource(primaryResource);
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

    @Override
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

    @Override
    public String getFullName() {
        return throwException();
    }

    @Override
    public String getPSName() {
        return throwException();
    }

    @Override
    public String getFamilyName() {
        return throwException();
    }

    @Override
    public String getStyleName() {
        return throwException();
    }

    @Override
    public String getLocaleFullName() {
        return throwException();
    }

    @Override
    public String getLocaleFamilyName() {
        return throwException();
    }

    @Override
    public String getLocaleStyleName() {
        return throwException();
    }


    @Override
    public boolean isBold() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean isItalic() {
        throw new UnsupportedOperationException("Not supported");

    }

    @Override
    public int getFeatures() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public String getFileName() {
        return throwException();
    }

    @Override
    public Object getPeer() {
        return null;
    }

    @Override
    public void setPeer(Object peer) {
        throwException();
    }

    @Override
    public boolean isEmbeddedFont() {
        return false;
    }

    @Override
    public CharToGlyphMapper getGlyphMapper() {
        if (mapper == null) {
            mapper = new CompositeGlyphMapper(this);
        }
        return mapper;
    }

    private int getSlotForFontNoCreate(String fontName) {
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
        return -1;
    }

    @Override
    public int getSlotForFont(String fontName) {
      int slot = getSlotForFontNoCreate(fontName);
        if (slot >= 0) {
            return slot;
        }

        PrismFontFactory factory = PrismFontFactory.getFontFactory();
        FontResource fr = factory.getFontResource(fontName, null, false);

        if (fr == null) {
            if (PrismFontFactory.debugFonts) {
                System.err.println("\t Font name not supported \"" + fontName + "\".");
            }
            return -1;
        }
        slot = getSlotForFontNoCreate(fr.getFullName());
        if (slot >= 0) {
            return slot;
        }

        /* Add the font to the list of native fallbacks */
        return addNativeFallback(fr);
    }


    private int addNativeFallback(FontResource fr) {
        int ns = getNumSlots();
        if (ns >= 0x7E) {
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

        return ns+1;
    }

    public int addSlotFont(FontResource fr) {
        int slot = getSlotForFont(fr.getFullName());
        if (slot >= 0) {
            return slot;
        } else {
            return addNativeFallback(fr);
        }
    }

    private void getLinkedFonts() {
        if (fallbacks == null) {
            PrismFontFactory factory = PrismFontFactory.getFontFactory();
            FontFallbackInfo fallbackInfo = factory.getFallbacks(primaryResource);
            linkedFontNames = fallbackInfo.getFontNames();
            linkedFontFiles = fallbackInfo.getFontFiles();
            fallbacks       = fallbackInfo.getFonts();
        }
    }

    @Override
    public int getNumSlots() {
        getLinkedFonts();
        int num = fallbacks.length;
        if (nativeFallbacks != null) {
            num += nativeFallbacks.length;
        }
        return num;
    }

    @Override
    public float[] getGlyphBoundingBox(int glyphCode,
                                float size, float[] retArr) {
        int slot = (glyphCode >>> 24);
        int slotglyphCode = glyphCode & CompositeGlyphMapper.GLYPHMASK;
        FontResource slotResource = getSlotResource(slot);
        return slotResource.getGlyphBoundingBox(slotglyphCode, size, retArr);
    }

    @Override
    public float getAdvance(int glyphCode, float size) {
        int slot = (glyphCode >>> 24);
        int slotglyphCode = glyphCode & CompositeGlyphMapper.GLYPHMASK;
        FontResource slotResource = getSlotResource(slot);
        return slotResource.getAdvance(slotglyphCode, size);
    }

    @Override
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
            String file = linkedFontFiles[slot];
            String name = linkedFontNames[slot];
            fallbacks[slot] =
                PrismFontFactory.getFontFactory().
                getFontResource(name, file, false);
        }
        return fallbacks[slot];
    }

    @Override
    public FontStrike getStrike(float size, BaseTransform transform) {
        return getStrike(size, transform, getDefaultAAMode());
    }

    @Override
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
                ref = new WeakReference<>(strike);
            }
            strikeMap.put(desc, ref);
        }
        return strike;
    }

    public String toString() {
        int ns = getNumSlots();
        String s = "Fallback resource:\n";
        for (int i=0; i<ns; i++) {
            if ((getSlotResource(i) == null)) {
                s += "Slot " + i + "=null\n";
            } else {
                s += "Slot " + i + "=" + getSlotResource(i).getFullName()+"\n";
            }
        }
        s+= "\n";
        return s;
    }
}
