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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.javafx.geom.transform.BaseTransform;

/**
 * This acts as a factory class for the 12 logical composite font
 * resources which are available as well as providing the implementation
 * of the resource.
 */
public class LogicalFont implements CompositeFontResource {

    public static final String SYSTEM     = "System";
    public static final String SERIF      = "Serif";
    public static final String SANS_SERIF = "SansSerif";
    public static final String MONOSPACED = "Monospaced";

    public static final String STYLE_REGULAR     = "Regular";
    public static final String STYLE_BOLD        = "Bold";
    public static final String STYLE_ITALIC      = "Italic";
    public static final String STYLE_BOLD_ITALIC = "Bold Italic";

    static final HashMap<String, String>
        canonicalFamilyMap = new  HashMap<String, String>();
    static {
        canonicalFamilyMap.put("system", SYSTEM);

        canonicalFamilyMap.put("serif", SERIF);

        canonicalFamilyMap.put("sansserif", SANS_SERIF);
        canonicalFamilyMap.put("sans-serif", SANS_SERIF); // css style
        canonicalFamilyMap.put("dialog", SANS_SERIF);
        canonicalFamilyMap.put("default", SANS_SERIF);

        canonicalFamilyMap.put("monospaced", MONOSPACED);
        canonicalFamilyMap.put("monospace", MONOSPACED); // css style
        canonicalFamilyMap.put("dialoginput", MONOSPACED);
    }

    static boolean isLogicalFont(String name) {
        int spaceIndex = name.indexOf(' ');
        if (spaceIndex != -1) name = name.substring(0, spaceIndex);
        return canonicalFamilyMap.get(name) != null;
    }

    private static String getCanonicalFamilyName(String name) {
         if (name == null) {
             return SANS_SERIF;
         }
         String lcName = name.toLowerCase();
         return canonicalFamilyMap.get(lcName);
    }

    static LogicalFont[] logicalFonts = new LogicalFont[16];

    static PGFont getLogicalFont(String familyName, boolean bold,
                               boolean italic, float size) {

        String canonicalFamilyName = getCanonicalFamilyName(familyName);
        if (canonicalFamilyName == null) {
            return null;
        }

        int fontIndex = 0;
        if (canonicalFamilyName.equals(SANS_SERIF)) {
            fontIndex = 0;
        } else if (canonicalFamilyName.equals(SERIF)) {
            fontIndex = 4;
       } else if (canonicalFamilyName.equals(MONOSPACED)) {
            fontIndex = 8;
        } else {
            fontIndex = 12;
        }
        if (bold) {
            fontIndex +=1;
        }
        if (italic) {
            fontIndex +=2;
        }

        LogicalFont font = logicalFonts[fontIndex];
        if (font == null) {
            font = new LogicalFont(canonicalFamilyName, bold, italic);
            logicalFonts[fontIndex] = font;
        }
        return new PrismFont(font, font.getFullName(), size);
    }

    static PGFont getLogicalFont(String fullName, float size) {

        /* Need to parse this to find the family portion, for which
         * we will allow the various spellings, and the style portion
         * which must be exactly one of those we understand. The matching
         * is however case insensitive.
         * Don't allow an absence of style, we want people to be
         * in the habit of distinguishing family and full name usage.
         * None of the family names we understand have a space, so look
         * for a space to delimit the family and style.
         */
        int spaceIndex = fullName.indexOf(' ');
        if (spaceIndex == -1 || spaceIndex == fullName.length()-1) {
            return null;
        }
        String family = fullName.substring(0, spaceIndex);
        String canonicalFamily = getCanonicalFamilyName(family);
        if (canonicalFamily == null) {
            return null;
        }
        String style = fullName.substring(spaceIndex+1).toLowerCase();
        boolean bold=false, italic=false;
        if (style.equals("regular")) {
            // nothing to do
        } else if (style.equals("bold")) {
            bold = true;
        } else if (style.equals("italic")) {
            italic = true;
        } else if (style.equals("bold italic")) {
            bold = true;
            italic = true;
        } else {
            return null;
        }
        return getLogicalFont(canonicalFamily, bold, italic, size);
    }

    boolean isBold, isItalic;
    private String fullName, familyName, styleName;
    private String physicalFamily;
    private String physicalFullName;
    private String physicalFileName;

    private LogicalFont(String family, boolean bold, boolean italic) {

        familyName = family;
        isBold = bold;
        isItalic = italic;

        if (!bold && !italic) {
            styleName = STYLE_REGULAR;
        } else if (bold && !italic) {
            styleName = STYLE_BOLD;
        } else if (!bold && italic) {
            styleName = STYLE_ITALIC;
        } else {
            styleName = STYLE_BOLD_ITALIC;
        }
        fullName = familyName + " " + styleName;
        if (PrismFontFactory.isLinux) {
            FontConfigManager.FcCompFont fcCompFont =
                FontConfigManager.getFontConfigFont(family, bold, italic);
            physicalFullName = fcCompFont.firstFont.fullName;
            physicalFileName = fcCompFont.firstFont.fontFile;
        } else {
            physicalFamily = PrismFontFactory.getSystemFont(familyName);
        }
    }

    private FontResource slot0FontResource;

    private FontResource getSlot0Resource() {
        if (slot0FontResource == null) {
            PrismFontFactory factory = PrismFontFactory.getFontFactory();
            if (physicalFamily != null) {
                slot0FontResource =  factory.getFontResource(physicalFamily,
                                                             isBold,
                                                             isItalic, false);
            } else {
                slot0FontResource = factory.getFontResource(physicalFullName,
                                                            physicalFileName,
                                                            false);
            }
            // Its unlikely but possible that this font isn't installed.
            if (slot0FontResource == null) {
                slot0FontResource = factory.getDefaultFontResource(false);
            }
        }
        return slot0FontResource;
    }

    private ArrayList<String> linkedFontFiles;
    private ArrayList<String> linkedFontNames;
    private FontResource[] fallbacks;
    private FontResource[] nativeFallbacks;

    private void getLinkedFonts() {
        if (fallbacks == null) {
            ArrayList<String>[] linkedFontInfo;
            if (PrismFontFactory.isLinux) {
                FontConfigManager.FcCompFont font =
                    FontConfigManager.getFontConfigFont(familyName,
                                                        isBold, isItalic);
                linkedFontFiles = FontConfigManager.getFileNames(font, true);
                linkedFontNames = FontConfigManager.getFontNames(font, true);
            } else {
                linkedFontInfo = PrismFontFactory.getLinkedFonts("Tahoma", true);
                linkedFontFiles = linkedFontInfo[0];
                linkedFontNames = linkedFontInfo[1];
            }
            fallbacks = new FontResource[linkedFontFiles.size()];
        }
    }

    public int getNumSlots() {
        getLinkedFonts();
        int num = linkedFontFiles.size();
        if (nativeFallbacks != null) {
            num += nativeFallbacks.length;
        }
        return num + 1;
    }

    public int getSlotForFont(String fontName) {
        getLinkedFonts();
        int i = 1;
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

    public FontResource getSlotResource(int slot) {
        if (slot == 0) {
            return getSlot0Resource();
        } else {
            getLinkedFonts();
            slot = slot - 1;
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
                if (fallbacks[slot] == null) {
                    fallbacks[slot] = getSlot0Resource();
                }
            }
            return fallbacks[slot];
        }
    }

    public String getFullName() {
        return fullName;
    }

    public String getPSName() {
        return fullName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getStyleName() {
        return styleName;
    }

    public String getLocaleFullName() {
        return fullName;
    }

    public String getLocaleFamilyName() {
        return familyName;
    }

    public String getLocaleStyleName() {
        return styleName;
    }

    public boolean isBold() {
        return getSlotResource(0).isBold();
    }

    public boolean isItalic() {
        return getSlotResource(0).isItalic();
    }

    public String getFileName() {
        return getSlotResource(0).getFileName();
    }

    public int getFeatures() {
        return getSlotResource(0).getFeatures();
    }

    public Object getPeer() {
        return null;
    }

    public boolean isEmbeddedFont() {
        return getSlotResource(0).isEmbeddedFont();
    }

    public void setPeer(Object peer) {
        throw new UnsupportedOperationException("Not supported");
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

    CompositeGlyphMapper mapper;
    public CharToGlyphMapper getGlyphMapper() {
        //return getSlot0Resource().getGlyphMapper();
        if (mapper == null) {
            mapper = new CompositeGlyphMapper(this);
        }
        return mapper;
    }

    Map<FontStrikeDesc, WeakReference<FontStrike>> strikeMap =
        new ConcurrentHashMap<FontStrikeDesc, WeakReference<FontStrike>>();

    public Map<FontStrikeDesc, WeakReference<FontStrike>> getStrikeMap() {
        return strikeMap;
    }

    public int getDefaultAAMode() {
        return getSlot0Resource().getDefaultAAMode();
    }

    public FontStrike getStrike(float size, BaseTransform transform) {
        return getStrike(size, transform, getDefaultAAMode());
    }

    public FontStrike getStrike(float size, BaseTransform transform,
                                int aaMode) {
        FontStrikeDesc desc= new FontStrikeDesc(size, transform, aaMode);
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

    // Family 0 = SansSerif, 1 = Serif, 2 = Monospaced, 3 = System
    private static final int SANS_SERIF_INDEX = 0;
    private static final int SERIF_INDEX      = 1;
    private static final int MONOSPACED_INDEX = 2;
    private static final int SYSTEM_INDEX = 3;
    // Within a family styles are in the usual order
    static String[][] logFamilies = null;

    private static void buildFamily(String[] fullNames, String family) {
        fullNames[0] = family + " " + STYLE_REGULAR;
        fullNames[1] = family + " " + STYLE_BOLD;
        fullNames[2] = family + " " + STYLE_ITALIC;
        fullNames[3] = family + " " + STYLE_BOLD_ITALIC;
    }

    private static void buildFamilies() {
        if (logFamilies == null) {
            String[][] tmpFamilies = new String[SYSTEM_INDEX+1][4];
            buildFamily(tmpFamilies[SANS_SERIF_INDEX], SANS_SERIF);
            buildFamily(tmpFamilies[SERIF_INDEX], SERIF);
            buildFamily(tmpFamilies[MONOSPACED_INDEX], MONOSPACED);
            buildFamily(tmpFamilies[SYSTEM_INDEX], SYSTEM);
            logFamilies = tmpFamilies;
        }
    }

    static void addFamilies(ArrayList<String> familyList) {
        familyList.add(SANS_SERIF);
        familyList.add(SERIF);
        familyList.add(MONOSPACED);
        familyList.add(SYSTEM);
    }

    static void addFullNames(ArrayList<String> fullNames) {
        buildFamilies();
        for (int f = 0; f < logFamilies.length; f++) {
            for (int n = 0; n < logFamilies[f].length; n++) {
                fullNames.add(logFamilies[f][n]);
            }
        }
    }

    static String[] getFontsInFamily(String family) {
        String canonicalFamily = getCanonicalFamilyName(family);
        if (canonicalFamily == null) {
            return null;
        }
        buildFamilies();
        if (canonicalFamily.equals(SANS_SERIF)) {
            return logFamilies[SANS_SERIF_INDEX];
        } else if (canonicalFamily.equals(SERIF)) {
            return logFamilies[SERIF_INDEX];
        } else if (canonicalFamily.equals(MONOSPACED)) {
            return logFamilies[MONOSPACED_INDEX];
        } else {
            return logFamilies[SYSTEM_INDEX];
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof LogicalFont)) {
            return false;
        }
        final LogicalFont other = (LogicalFont)obj;

        return this.fullName.equals(other.fullName);
    }

    private int hash;
    @Override
    public int hashCode() {
        if (hash != 0) {
            return hash;
        }
        else {
            hash = fullName.hashCode();
            return hash;
        }
    }
}
