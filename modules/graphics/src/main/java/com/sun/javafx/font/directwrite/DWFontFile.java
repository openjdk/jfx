/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font.directwrite;

import com.sun.javafx.font.Disposer;
import com.sun.javafx.font.FontStrikeDesc;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.font.PrismFontFile;
import com.sun.javafx.font.PrismFontStrike;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;

class DWFontFile extends PrismFontFile {
    private IDWriteFontFace fontFace;
    private DWDisposer disposer;

    DWFontFile(String name, String filename, int fIndex, boolean register,
               boolean embedded, boolean copy, boolean tracked) throws Exception {
        super(name, filename, fIndex, register, embedded, copy, tracked);
        fontFace = createFontFace();

        if (!isRegistered()) {
            disposer = new DWDisposer(fontFace);
            Disposer.addRecord(this, disposer);
        }
    }

    private IDWriteFontFace createFontFace() {
        if (isEmbeddedFont()) {
            /* Note: The font path has already been verified at this point. */
            IDWriteFactory factory = DWFactory.getDWriteFactory();
            IDWriteFontFile fontFile = factory.CreateFontFileReference(getFileName());
            boolean[] isSupportedFontType = new boolean[1];
            int[] fontFileType = new int[1];
            int[] fontFaceType = new int[1];
            int[] numberOfFaces = new int[1];
            fontFile.Analyze(isSupportedFontType, fontFileType, fontFaceType, numberOfFaces);
            IDWriteFontFace face = factory.CreateFontFace(fontFaceType[0], 1, fontFile, 0, OS.DWRITE_FONT_SIMULATIONS_NONE);
            fontFile.Release();
            return face;
        }
        IDWriteFontCollection collection = DWFactory.getFontCollection();
        int index = collection.FindFamilyName(getFamilyName());
        if (index == -1) return null;

        IDWriteFontFamily family = collection.GetFontFamily(index);
        int weight = isBold() ? OS.DWRITE_FONT_WEIGHT_BOLD :
                                OS.DWRITE_FONT_WEIGHT_NORMAL;
        int stretch = OS.DWRITE_FONT_STRETCH_NORMAL;
        int style = isItalic() ? OS.DWRITE_FONT_STYLE_ITALIC :
                                 OS.DWRITE_FONT_STYLE_NORMAL;
        IDWriteFont font = family.GetFirstMatchingFont(weight, stretch, style);
        IDWriteFontFace face = font.CreateFontFace();

        font.Release();
        family.Release();
        return face;
    }

    /* This is called only for fonts where a temp file was created */
    protected synchronized void disposeOnShutdown() {
        if (fontFace != null) {
            fontFace.Release();
            if (PrismFontFactory.debugFonts) {
                System.err.println("fontFace freed: " + fontFace);
            }
            fontFace = null;
        }
        if (disposer != null) {
            disposer.resource = null;
            disposer = null;
        }
    }

    IDWriteFontFace getFontFace() {
        return fontFace;
    }

    Path2D getGlyphOutline(int gc, float size) {
        if (fontFace == null) return null;
        return fontFace.GetGlyphRunOutline(size, (short)gc, false);
    }

    RectBounds getBBox(int glyphCode, float size) {
        /* In coretext and t2k this is the bounds for the path of the glyph */
        float[] bb = new float[4];
        getGlyphBoundingBox(glyphCode, size, bb);
        return new RectBounds(bb[0], bb[1], bb[2], bb[3]);
    }

    @Override protected int[] createGlyphBoundingBox(int gc) {
        if (fontFace == null) return null;
        DWRITE_GLYPH_METRICS metrics = fontFace.GetDesignGlyphMetrics((short)gc, false);
        int[] bb = new int[4];
        bb[0] = metrics.leftSideBearing;
        bb[1] = metrics.topSideBearing;
        bb[2] = metrics.advanceWidth - metrics.rightSideBearing;
        bb[3] = metrics.verticalOriginY;
        return bb;
    }

    @Override
    protected PrismFontStrike createStrike(float size, BaseTransform transform,
                                           int aaMode, FontStrikeDesc desc) {
        return new DWFontStrike(this, size, transform, aaMode, desc);
    }
}
