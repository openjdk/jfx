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
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import com.sun.javafx.geom.transform.BaseTransform;


/*
 * Wraps a physical font and adds an appropriate fallback resource.
 */

class PrismCompositeFontResource implements CompositeFontResource {

    private FontResource primaryResource;
    private FallbackResource fallbackResource; // is a composite too.

    PrismCompositeFontResource(FontResource primaryResource,
                             String lookupName) {
        // remind go through and make the typing better.
        if (!(primaryResource instanceof PrismFontFile)) {
            Thread.dumpStack();
            throw new IllegalStateException("wrong resource type");
        }
        if (lookupName != null) {
            PrismFontFactory factory = PrismFontFactory.getFontFactory();
            factory.compResourceMap.put(lookupName, this);
        }
        this.primaryResource = primaryResource;
        fallbackResource = FallbackResource.getFallbackResource(primaryResource);
    }

    @Override
    public int getNumSlots() {
        return fallbackResource.getNumSlots()+1;
    }

    @Override
    public int getSlotForFont(String fontName) {
        if (primaryResource.getFullName().equalsIgnoreCase(fontName)) {
            return 0;
        }
        return fallbackResource.getSlotForFont(fontName) + 1;
    }

    @Override
    public FontResource getSlotResource(int slot) {
        if (slot == 0) {
            return primaryResource;
        } else {
            FontResource fb = fallbackResource.getSlotResource(slot-1);
            if (fb != null) {
                return fb;
            } else {
                 return primaryResource;
            }
        }
    }

    @Override
    public String getFullName() {
        return primaryResource.getFullName();
    }

    @Override
    public String getPSName() {
        return primaryResource.getPSName();
    }

    @Override
    public String getFamilyName() {
        return primaryResource.getFamilyName();
    }

    @Override
    public String getStyleName() {
        return primaryResource.getStyleName();
    }

    @Override
    public String getLocaleFullName() {
        return primaryResource.getLocaleFullName();
    }

    @Override
    public String getLocaleFamilyName() {
        return primaryResource.getLocaleFamilyName();
    }

    @Override
    public String getLocaleStyleName() {
        return primaryResource.getLocaleStyleName();
    }

    @Override
    public String getFileName() {
        return primaryResource.getFileName();
    }

    @Override
    public int getFeatures() {
        return primaryResource.getFeatures();
    }

    @Override
    public Object getPeer() {
        return primaryResource.getPeer();
    }

    @Override
    public void setPeer(Object peer) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean isEmbeddedFont() {
        return primaryResource.isEmbeddedFont();
    }

    @Override
    public boolean isBold() {
        return primaryResource.isBold();
    }

    @Override
    public boolean isItalic() {
        return primaryResource.isItalic();
    }

    CompositeGlyphMapper mapper;
    @Override
    public CharToGlyphMapper getGlyphMapper() {
        if (mapper == null) {
            mapper = new CompositeGlyphMapper(this);
        }
        return mapper;
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

    Map<FontStrikeDesc, WeakReference<FontStrike>> strikeMap = new ConcurrentHashMap<>();

    @Override
    public Map<FontStrikeDesc, WeakReference<FontStrike>> getStrikeMap() {
        return strikeMap;
    }

    @Override
    public int getDefaultAAMode() {
        return getSlotResource(0).getDefaultAAMode();
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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PrismCompositeFontResource)) {
            return false;
        }
        final PrismCompositeFontResource other = (PrismCompositeFontResource)obj;
        return primaryResource.equals(other.primaryResource);
    }

    @Override
    public int hashCode() {
        return primaryResource.hashCode();
    }
}
