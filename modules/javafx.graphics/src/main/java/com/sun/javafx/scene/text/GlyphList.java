/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.text;

import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;


public interface GlyphList {

    /**
     * Returns the number of glyphs in GlyphList.
     */
    public int getGlyphCount();

    /**
     * Returns the glyph code for the given glyphIndex.
     */
    public int getGlyphCode(int glyphIndex);

    /**
     * The x position for the given glyphIndex relative the GlyphList.
     */
    public float getPosX(int glyphIndex);

    /**
     * The y position for the given glyphIndex relative the GlyphList.
     */
    public float getPosY(int glyphIndex);

    /**
     * Returns the width of the GlyphList
     */
    public float getWidth();

    /**
     * Returns the height of the GlyphList
     */
    public float getHeight();

    /**
     * See TextLine#getBounds()
     * (used outside text layout in rendering and span bounds)
     */
    public RectBounds getLineBounds();

    /**
     * The top-left location of the GlyphList relative to
     * the origin of the Text Layout.
     */
    public Point2D getLocation();

    /**
     * Maps the given glyph index to the char offset.
     * (used during rendering (selection))
     */
    public int getCharOffset(int glyphIndex);


    /**
     * Means that this GlyphList was shaped using complex processing (ICU),
     * either because it is complex script or because font features were
     * requested.
     * (used outside text layout in rendering)
     */
    public boolean isComplex();

    /**
     * Used during layout children (for rich text)
     * can be null (for non-rich text) but never null for rich text.
     */
    public TextSpan getTextSpan();
}

