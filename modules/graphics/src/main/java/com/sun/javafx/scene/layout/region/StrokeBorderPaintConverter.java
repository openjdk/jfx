/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.layout.region;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.css.ParsedValue;
import javafx.css.StyleConverter;

/**
 */
public class StrokeBorderPaintConverter extends StyleConverter<ParsedValue<?,Paint>[], Paint[]> {
    /**
     * Convert an array of border paint values to an array of Paint which
     * contains one Paint element per border (top, right, bottom, left).
     */
    private static final StrokeBorderPaintConverter STROKE_BORDER_PAINT_CONVERTER =
            new StrokeBorderPaintConverter();

    public static StrokeBorderPaintConverter getInstance() {
        return STROKE_BORDER_PAINT_CONVERTER;
    }

    // Prevent instantiation
    private StrokeBorderPaintConverter() { }

    @Override
    public Paint[] convert(ParsedValue<ParsedValue<?,Paint>[], Paint[]> value, Font font) {
        final ParsedValue<?,Paint>[] borders = value.getValue();
        final Paint[] paints = new Paint[4];

        paints[0] = (borders.length > 0) ?
                borders[0].convert(font) : Color.BLACK;

        paints[1] = (borders.length > 1) ?
                borders[1].convert(font) : paints[0];

        paints[2] = (borders.length > 2) ?
                borders[2].convert(font) : paints[0];

        paints[3] = (borders.length > 3) ?
                borders[3].convert(font) : paints[1];

        return paints;
    }

    @Override public String toString() {
        return "StrokeBorderPaintConverter";
    }
}
