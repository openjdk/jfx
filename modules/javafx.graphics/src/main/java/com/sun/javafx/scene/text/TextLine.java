/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.RectBounds;

public interface TextLine {
    /**
     * Returns the list of GlyphList in the line. The list is visually orderded.
     */
    public GlyphList[] getRuns();

    /**
     * Returns metrics information about the line as follow:
     *
     * bounds().getWidth() - the width of the line.
     * The width for the line is sum of all run's width in the line, it is not
     * affect by any wrapping width but it will include any changes caused by
     * justification.
     *
     * bounds().getHeight() - the height of the line.
     * The height of the line is sum of the max ascent, max descent, and
     * max line gap of all the fonts in the line.
     *
     * bounds.().getMinY() - the ascent of the line (negative).
     * The ascent of the line is the max ascent of all fonts in the line.
     *
     * bounds().getMinX() - the x origin of the line (relative to the layout).
     * The x origin is defined by TextAlignment of the text layout, always zero
     * for left-aligned text.
     */
    public RectBounds getBounds();

    /**
     * Returns the left side bearing of the line (negative).
     */
    public float getLeftSideBearing();

    /**
     * Returns the right side bearing of the line (positive).
     */
    public float getRightSideBearing();

    /**
     * Returns the line start offset.
     */
    public int getStart();

    /**
     * Returns the line length in character.
     */
    public int getLength();
}
