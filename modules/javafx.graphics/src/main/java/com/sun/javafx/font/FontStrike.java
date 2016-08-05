/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;

public interface FontStrike {
    public FontResource getFontResource();
    public float getSize();
    public BaseTransform getTransform();
    public boolean drawAsShapes();

    /**
     * Modifies the point argument to the quantized position suitable for the
     * underlying glyph rasterizer.
     * The return value is the sub pixel index which should be passed to
     * {@link Glyph#getPixelData(int)} in order to obtain the correct glyph mask
     * for the given point.
     */
    public int getQuantizedPosition(Point2D point);
    public Metrics getMetrics();
    public Glyph getGlyph(char symbol);
    public Glyph getGlyph(int glyphCode);
    public void clearDesc(); // for cache management.
    public int getAAMode();

    /* These are all user space values */
    public float getCharAdvance(char ch);
    public Shape getOutline(GlyphList gl,
                            BaseTransform transform);
}
