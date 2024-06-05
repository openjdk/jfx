/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font.coretext;

import com.sun.javafx.font.DisposerRecord;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrikeDesc;
import com.sun.javafx.font.Glyph;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.font.PrismFontStrike;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.transform.BaseTransform;

class CTFontStrike extends PrismFontStrike<CTFontFile> {

    private long fontRef;
    CGAffineTransform matrix;
    /* CoreText uses different precision for subpixel text according
     * to the font size. By observation, font sizes smaller than 12
     * have 4 subpixel positions. Between 12 and 17 it decreases to 3.
     * Between 18 and 33 it is only 2. Above 33 it rounds all positions
     * to integral values.
     */
    static final float SUBPIXEL4_SIZE = 12;
    static final float SUBPIXEL3_SIZE = 18;
    static final float SUBPIXEL2_SIZE = 34;
    private static final boolean SUBPIXEL;
    static {
        int mode = PrismFontFactory.getFontFactory().getSubPixelMode();
        SUBPIXEL =  (mode & PrismFontFactory.SUB_PIXEL_ON) != 0;
    }

    CTFontStrike(CTFontFile fontResource, float size,
                 BaseTransform graphicsTransform, int aaMode,
                 FontStrikeDesc desc) {
        super(fontResource, size, graphicsTransform, aaMode, desc);
        float maxDim = PrismFontFactory.getFontSizeLimit();
        if (graphicsTransform.isTranslateOrIdentity()) {
            drawShapes = size > maxDim;
        } else {
            BaseTransform tx2d = getTransform();
            matrix = new CGAffineTransform();
            matrix.a = tx2d.getMxx();
            matrix.b = -tx2d.getMyx(); /*Inverted coordinates system */
            matrix.c = -tx2d.getMxy(); /*Inverted coordinates system */
            matrix.d = tx2d.getMyy();

            if (Math.abs(matrix.a * size) > maxDim ||
                Math.abs(matrix.b * size) > maxDim ||
                Math.abs(matrix.c * size) > maxDim ||
                Math.abs(matrix.d * size) > maxDim)
            {
              drawShapes = true;
            }
        }

        fontRef = fontResource.getFontRef(size, matrix);
        if (fontRef == 0) {
            if (PrismFontFactory.debugFonts) {
                System.err.println("Failed to create CTFont for " + this);
            }
        }
    }

    long getFontRef() {
        return fontRef;
    }

    @Override protected DisposerRecord createDisposer(FontStrikeDesc desc) {
        CTFontFile fontResource = getFontResource();
        return new CTStrikeDisposer(fontResource, desc, fontRef);
    }

    @Override protected Glyph createGlyph(int glyphCode) {
        return new CTGlyph(this, glyphCode, drawShapes);
    }

    @Override
    public int getQuantizedPosition(Point2D point) {
        if (SUBPIXEL && matrix == null) {
            /* Prism only produces 3 position, so for sizes smaller than 12 use
             * Coretext for LCD and grayscale text
             */
            if (getSize() < SUBPIXEL4_SIZE) {
                float subPixelX = point.x;
                point.x = (int) point.x;
                subPixelX -= point.x;
                point.y = Math.round(point.y);
                if (subPixelX >= 0.75f) return 3;
                if (subPixelX >= 0.50f) return 2;
                if (subPixelX >= 0.25f) return 1;
                return 0;
            }
            if (getAAMode() == FontResource.AA_GREYSCALE) {
                if (getSize() < SUBPIXEL3_SIZE) {
                    float subPixelX = point.x;
                    point.x = (int) point.x;
                    subPixelX -= point.x;
                    point.y = Math.round(point.y);
                    if (subPixelX >= 0.66f) return 2;
                    if (subPixelX >= 0.33f) return 1;
                    return 0;
                }
                if (getSize() < SUBPIXEL2_SIZE) {
                    float subPixelX = point.x;
                    point.x = (int) point.x;
                    subPixelX -= point.x;
                    point.y = Math.round(point.y);
                    if (subPixelX >= 0.5f) return 1;
                }
                return 0;
            }
        }
        return super.getQuantizedPosition(point);
    }

    float getSubPixelPosition(int index) {
        if (index == 0) return 0;
        float size = getSize();
        if (size < SUBPIXEL4_SIZE) {
            if (index == 3) return 0.75f;
            if (index == 2) return 0.50f;
            if (index == 1) return 0.25f;
            return 0;
        }
        if (getAAMode() == FontResource.AA_LCD) return 0;
        if (size < SUBPIXEL3_SIZE) {
            if (index == 2) return 0.66f;
            if (index == 1) return 0.33f;
            return 0;
        }
        if (size < SUBPIXEL2_SIZE) {
            if (index == 1) return 0.50f;
        }
        return 0;
    }

    boolean isSubPixelGlyph() {
        return SUBPIXEL && matrix == null;
    }

    @Override protected Path2D createGlyphOutline(int glyphCode) {
        CTFontFile fontResource = getFontResource();
        return fontResource.getGlyphOutline(glyphCode, getSize());
    }

    CGRect getBBox(int glyphCode) {
        CTFontFile fontResource = getFontResource();
        return fontResource.getBBox(glyphCode, getSize());
    }
}
