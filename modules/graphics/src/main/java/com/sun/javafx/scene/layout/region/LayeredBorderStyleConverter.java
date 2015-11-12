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

import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.text.Font;
import javafx.css.ParsedValue;
import javafx.css.StyleConverter;

/**
 * User: richardbair
 * Date: 8/9/12
 * Time: 4:53 PM
 */
/*
* border-style: <border-style> [, <border-style>]*
* where <border-style> = <dash-style> [phase(<number>)]? [centered | inside | outside]?
*                        [line-join [miter <number> | bevel | round]]?
*                        [line-cap [square | butt | round]]?
* where <dash-style> = none | solid | dotted | dashed | segments(<size>[, <size>]+) ]
*/
public final class LayeredBorderStyleConverter
        extends StyleConverter<ParsedValue<ParsedValue<ParsedValue[],BorderStrokeStyle>[], BorderStrokeStyle[]>[], BorderStrokeStyle[][]> {

    /**
     * Convert layers of border style values to an array of BorderStyle[], where
     * each layer contains one BorderStyle element per border.
     */
    private static final LayeredBorderStyleConverter LAYERED_BORDER_STYLE_CONVERTER =
            new LayeredBorderStyleConverter();

    public static LayeredBorderStyleConverter getInstance() {
        return LAYERED_BORDER_STYLE_CONVERTER;
    }

    private LayeredBorderStyleConverter() {
        super();
    }

    @Override
    public BorderStrokeStyle[][]
    convert(ParsedValue<ParsedValue<ParsedValue<ParsedValue[], BorderStrokeStyle>[],BorderStrokeStyle[]>[], BorderStrokeStyle[][]> value, Font font) {

        ParsedValue<ParsedValue<ParsedValue[], BorderStrokeStyle>[],BorderStrokeStyle[]>[] layers = value.getValue();
        BorderStrokeStyle[][] styles = new BorderStrokeStyle[layers.length][0];

        for (int layer=0; layer<layers.length; layer++) {
            styles[layer] = layers[layer].convert(font);
        }
        return styles;
    }

    @Override
    public String toString() {
        return "LayeredBorderStyleConverter";
    }
}
