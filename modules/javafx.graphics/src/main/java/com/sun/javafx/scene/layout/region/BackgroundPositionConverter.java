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

import javafx.geometry.Side;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.text.Font;
import javafx.css.ParsedValue;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.css.StyleConverter;

/**
 * Given four Sizes from the Parser, this converter will produce a BackgroundPosition object.
 */
public final class BackgroundPositionConverter extends StyleConverter<ParsedValue[], BackgroundPosition> {
    private static final BackgroundPositionConverter BACKGROUND_POSITION_CONVERTER =
            new BackgroundPositionConverter();

    public static BackgroundPositionConverter getInstance() {
        return BACKGROUND_POSITION_CONVERTER;
    }

    // Disallow instantiation
    private BackgroundPositionConverter() { }

    @Override
    public BackgroundPosition convert(ParsedValue<ParsedValue[], BackgroundPosition> value, Font font) {
        ParsedValue[] positions = value.getValue();

        // The parser gives us 4 values, none of them null
        final Size top = (Size)positions[0].convert(font);
        final Size right = (Size)positions[1].convert(font);
        final Size bottom = (Size)positions[2].convert(font);
        final Size left = (Size)positions[3].convert(font);

        boolean verticalEdgeProportional =
                (bottom.getValue() > 0 && bottom.getUnits() == SizeUnits.PERCENT)
                        || (top.getValue() > 0 && top.getUnits() == SizeUnits.PERCENT)
                        || (top.getValue() == 0 && bottom.getValue() == 0);

        // either left or right will be set, not both
        boolean horizontalEdgeProportional =
                (right.getValue() > 0 && right.getUnits() == SizeUnits.PERCENT)
                        || ( left.getValue() > 0 && left.getUnits() == SizeUnits.PERCENT)
                        || (left.getValue() == 0 && right.getValue() == 0);

        final double t = top.pixels(font);
        final double r = right.pixels(font);
        final double b = bottom.pixels(font);
        final double l = left.pixels(font);

        return new BackgroundPosition(
                (l == 0 && r != 0) ? Side.RIGHT : Side.LEFT,
                (l == 0 && r != 0) ? r : l,
                horizontalEdgeProportional,
                (t == 0 && b != 0) ? Side.BOTTOM : Side.TOP,
                (t == 0 && b != 0) ? b : t,
                verticalEdgeProportional);
    }

    /**
     * @inheritDoc
     */
    @Override public String toString() {
        return "BackgroundPositionConverter";
    }
}
