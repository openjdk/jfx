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

import com.sun.javafx.font.DisposerRecord;
import com.sun.javafx.font.FontStrikeDesc;
import com.sun.javafx.font.Glyph;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.font.PrismFontStrike;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.text.TextRun;

class DWFontStrike extends PrismFontStrike<DWFontFile> {
    DWRITE_MATRIX matrix;
    static final boolean SUBPIXEL;
    static {
        SUBPIXEL = PrismFontFactory.getFontFactory().isSubPixelEnabled();
    }

    DWFontStrike(DWFontFile fontResource, float size, BaseTransform tx,
                 int aaMode, FontStrikeDesc desc) {
        super(fontResource, size, tx, aaMode, desc);
        float maxDim = 80f;
        if (tx.isTranslateOrIdentity()) {
            drawShapes = size > maxDim;
        } else {
            BaseTransform tx2d = getTransform();
            matrix = new DWRITE_MATRIX();
            matrix.m11 = (float)tx2d.getMxx();
            matrix.m12 = (float)tx2d.getMyx();
            matrix.m21 = (float)tx2d.getMxy();
            matrix.m22 = (float)tx2d.getMyy();

            if (Math.abs(matrix.m11 * size) > maxDim ||
                Math.abs(matrix.m12 * size) > maxDim ||
                Math.abs(matrix.m21 * size) > maxDim ||
                Math.abs(matrix.m22 * size) > maxDim)
            {
              drawShapes = true;
            }
        }
    }

    @Override protected DisposerRecord createDisposer(FontStrikeDesc desc) {
        return null;
    }

    @Override
    public boolean isSubPixelGlyph() {
        /* Disable subpixel for DirectWrite until better support for it
         * is implemented on Prism. DirectWrite support 3 subpixel positions
         * for LCD and Gray text. Prism currently expects it to support 4.
         */
//        return SUBPIXEL && matrix == null;
        return false;
    }

    IDWriteFontFace getFontFace() {
        DWFontFile fontResource = getFontResource();
        return fontResource.getFontFace();
    }

    RectBounds getBBox(int glyphCode) {
        DWFontFile fontResource = getFontResource();
        return fontResource.getBBox(glyphCode, getSize());
    }

    int getUpem() {
        return getFontResource().getUnitsPerEm();
    }

    @Override protected Path2D createGlyphOutline(int glyphCode) {
        DWFontFile fontResource = getFontResource();
        return fontResource.getGlyphOutline(glyphCode, getSize());
    }

    @Override public Glyph createGlyph(GlyphList gl, int gi) {
        Point2D offset = (Point2D)((TextRun)gl).getGlyphData(gi);
        float advanceOffset = offset != null ? offset.x : 0;
        float ascenderOffset = offset != null ? offset.y : 0;
        int gc = gl.getGlyphCode(gi);
        if (PrismFontFactory.debugFonts) {
            if (advanceOffset != 0 || ascenderOffset != 0) {
                System.err.println("Setting glyph[" + Integer.toHexString(gc) +
                                   "] offsets to " + offset);
            }
        }
        return new DWGlyph(this, gc, advanceOffset, ascenderOffset, drawShapes);
    }

    @Override protected Glyph createGlyph(int glyphCode) {
        return new DWGlyph(this, glyphCode, 0, 0, drawShapes);
    }
}
