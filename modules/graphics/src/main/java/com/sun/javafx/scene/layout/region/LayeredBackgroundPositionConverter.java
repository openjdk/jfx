/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.text.Font;

/**
 * background-position: <bg-position>
 * where <bg-position> = [
 *   [ [ <size> | left | center | right ] [ <size> | top | center | bottom ]? ]
 *   | [ [ center | [ left | right ] <size>? ] || [ center | [ top | bottom ] <size>? ]
 * ]
 * @see <a href="http://www.w3.org/TR/css3-background/#the-background-position">background-position</a>
 */
public final class LayeredBackgroundPositionConverter extends StyleConverter<ParsedValue<ParsedValue[], BackgroundPosition>[], BackgroundPosition[]> {
    private static final LayeredBackgroundPositionConverter LAYERED_BACKGROUND_POSITION_CONVERTER =
            new LayeredBackgroundPositionConverter();

    public static LayeredBackgroundPositionConverter getInstance() {
        return LAYERED_BACKGROUND_POSITION_CONVERTER;
    }

    private LayeredBackgroundPositionConverter() {
        super();
    }

    @Override
    public BackgroundPosition[] convert(ParsedValue<ParsedValue<ParsedValue[], BackgroundPosition>[], BackgroundPosition[]> value, Font font) {
        ParsedValue<ParsedValue[], BackgroundPosition>[] layers = value.getValue();
        BackgroundPosition[] positions = new BackgroundPosition[layers.length];
        for (int l = 0; l < layers.length; l++) {
            positions[l] = layers[l].convert(font);
        }
        return positions;
    }

    @Override
    public String toString() {
        return "LayeredBackgroundPositionConverter";
    }
}
