/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font.freetype;

import com.sun.javafx.font.DisposerRecord;
import com.sun.javafx.font.FontStrikeDesc;
import com.sun.javafx.font.Glyph;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.font.PrismFontStrike;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.transform.BaseTransform;

class FTFontStrike extends PrismFontStrike<FTFontFile> {
    FT_Matrix matrix;

    protected FTFontStrike(FTFontFile fontResource, float size,
                              BaseTransform tx, int aaMode,
                              FontStrikeDesc desc) {
        super(fontResource, size, tx, aaMode, desc);
        float maxDim = PrismFontFactory.getFontSizeLimit();
        if (tx.isTranslateOrIdentity()) {
            drawShapes = size > maxDim;
        } else {
            BaseTransform tx2d = getTransform();
            matrix = new FT_Matrix();
            /* Fixed 16.16 to int */
            matrix.xx = (int)( tx2d.getMxx() * 65536.0f);
            matrix.yx = (int)(-tx2d.getMyx() * 65536.0f); /*Inverted coordinates system */
            matrix.xy = (int)(-tx2d.getMxy() * 65536.0f); /*Inverted coordinates system */
            matrix.yy = (int)( tx2d.getMyy() * 65536.0f);

            if (Math.abs(tx2d.getMxx() * size) > maxDim ||
                Math.abs(tx2d.getMyx() * size) > maxDim ||
                Math.abs(tx2d.getMxy() * size) > maxDim ||
                Math.abs(tx2d.getMyy() * size) > maxDim)
            {
                drawShapes = true;
            }
        }
    }

    @Override
    protected DisposerRecord createDisposer(FontStrikeDesc desc) {
        return null;
    }

    @Override
    protected Glyph createGlyph(int glyphCode) {
        return new FTGlyph(this, glyphCode, drawShapes);
    }

    @Override
    protected Path2D createGlyphOutline(int glyphCode) {
        FTFontFile fontResource = getFontResource();
        return fontResource.createGlyphOutline(glyphCode, getSize());
    }

    void initGlyph(FTGlyph glyph) {
        FTFontFile fontResource = getFontResource();
        fontResource.initGlyph(glyph, this);
    }

}
