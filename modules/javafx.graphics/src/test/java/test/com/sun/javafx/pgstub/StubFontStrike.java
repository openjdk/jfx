/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.Glyph;
import com.sun.javafx.font.Metrics;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.text.GlyphList;

/**
 *
 */
public class StubFontStrike implements FontStrike {
    private final FontResource fontResource;
    private final float size;
    private final BaseTransform transform;

    public StubFontStrike(FontResource r, float size, BaseTransform t) {
        this.fontResource = r;
        this.size = size;
        this.transform = t;
    }

    @Override
    public FontResource getFontResource() {
        return fontResource;
    }

    @Override
    public float getSize() {
        return size;
    }

    @Override
    public BaseTransform getTransform() {
        return transform;
    }

    @Override
    public boolean drawAsShapes() {
        return false;
    }

    @Override
    public int getQuantizedPosition(Point2D point) {
        point.x = Math.round(point.x);
        point.y = Math.round(point.y);
        return 0;
    }

    @Override
    public Metrics getMetrics() {
        return new StubFontMetrics(size);
    }

    @Override
    public Glyph getGlyph(char symbol) {
        return null;
    }

    @Override
    public Glyph getGlyph(int glyphCode) {
        return null;
    }

    @Override
    public void clearDesc() {
    }

    @Override
    public int getAAMode() {
        return 0;
    }

    @Override
    public float getCharAdvance(char ch) {
        int glyphCode = fontResource.getGlyphMapper().charToGlyph((int)ch);
        return fontResource.getAdvance(glyphCode, size);
    }

    @Override
    public Shape getOutline(GlyphList gl, BaseTransform transform) {
        return null;
    }
}
