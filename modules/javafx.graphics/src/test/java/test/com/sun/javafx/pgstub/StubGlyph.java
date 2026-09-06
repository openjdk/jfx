/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.pgstub;

import java.util.Arrays;

import com.sun.javafx.font.Glyph;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.RoundRectangle2D;
import com.sun.javafx.geom.Shape;

/**
 * A {@code Glyph} that renders as a solid rectangle, so that text drawing can
 * be exercised in the stub toolkit without a real font rasterizer. A regular glyph
 * is as wide as the font size and as tall as the baseline of the font. Italic glyphs
 * are 50% wider.
 */
public class StubGlyph implements Glyph {
    private static final float ITALIC_WIDTH_FACTOR = 1.5f;

    private final int glyphCode;
    private final int width;
    private final int height;
    private final byte[] pixelData;

    public StubGlyph(int glyphCode, float size) {
        this(glyphCode, size, false);
    }

    public StubGlyph(int glyphCode, float size, boolean italic) {
        this.glyphCode = glyphCode;
        this.width = (int) Math.ceil(italic ? size * ITALIC_WIDTH_FACTOR : size);
        this.height = (int) Math.ceil(StubFontMetrics.BASELINE * size);
        this.pixelData = new byte[width * height];

        Arrays.fill(pixelData, (byte) 0xff);
    }

    @Override
    public int getGlyphCode() {
        return glyphCode;
    }

    @Override
    public RectBounds getBBox() {
        return new RectBounds(0, 0, width, height);
    }

    @Override
    public float getAdvance() {
        return width;
    }

    @Override
    public Shape getShape() {
        return new RoundRectangle2D(0, 0, width, height, 0, 0);
    }

    @Override
    public byte[] getPixelData() {
        return pixelData;
    }

    @Override
    public byte[] getPixelData(int subPixel) {
        return pixelData;
    }

    @Override
    public float getPixelXAdvance() {
        return 0;
    }

    @Override
    public float getPixelYAdvance() {
        return 0;
    }

    @Override
    public boolean isLCDGlyph() {
        return false;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getOriginX() {
        return 0;
    }

    @Override
    public int getOriginY() {
        return -height;
    }
}
