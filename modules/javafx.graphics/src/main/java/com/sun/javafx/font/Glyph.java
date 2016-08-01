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

package com.sun.javafx.font;

import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Shape;

public interface Glyph {
    public int getGlyphCode();
    /* These 3 are user space values */
    public RectBounds getBBox();
    public float getAdvance();
    public Shape getShape();
    /* The rest are in device space */
    public byte[] getPixelData();

    /**
     * Returns the glyph mask at the subpixel position specified by subPixel.
     *
     * @see FontStrike#getQuantizedPosition(com.sun.javafx.geom.Point2D)
     */
    public byte[] getPixelData(int subPixel);
    public float getPixelXAdvance();
    public float getPixelYAdvance();
    public boolean isLCDGlyph();

    /* These 4 methods should only be called after either getPixelData() or
     * getPixelData(int subPixel) is invoked. This ensures the returned value
     * is correct for the requested subpixel position. */
    public int getWidth();
    public int getHeight();
    public int getOriginX();
    public int getOriginY();
}
