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
import javafx.css.Size;
import javafx.css.StyleConverter;
import javafx.scene.layout.BorderWidths;
import javafx.scene.text.Font;

/**
 * User: richardbair
 * Date: 8/10/12
 * Time: 8:27 PM
 */
public class BorderImageWidthsSequenceConverter extends StyleConverter<ParsedValue<ParsedValue[], BorderWidths>[], BorderWidths[]> {
    private static final BorderImageWidthsSequenceConverter CONVERTER =
            new BorderImageWidthsSequenceConverter();

    public static BorderImageWidthsSequenceConverter getInstance() {
        return CONVERTER;
    }

    @Override
    public BorderWidths[] convert(ParsedValue<ParsedValue<ParsedValue[], BorderWidths>[], BorderWidths[]> value, Font font) {
        // For 'border-image-slice: 10% fill, 20% 30%', the value arg will be
        // ParsedValue { values: [
        //     ParsedValue { values: [ ParsedValue {parsed: 10%}, ParsedValue {parsed: fill}] } ,
        //     ParsedValue { values: [ ParsedValue {parsed: 20%}, ParsedValue {parsed: 30%}] }
        // ]}
        //
        // For 'border-image-slice: 10% fill', the value arg will be
        // ParsedValue { values: [ ParsedValue {parsed: 10%}, ParsedValue {parsed: fill}] }
        //
        // For 'border-image-slice: 10%', the value arg will be
        // ParsedValue {parsed: 10%}
        //
        // where the sizes are actually Size objects.
        //
        // If the value arg contains multiple layers, unwind the nested
        // values by one level.
        ParsedValue<ParsedValue[], BorderWidths>[] layers = value.getValue();
        BorderWidths[] widths = new BorderWidths[layers.length];
        for (int l = 0; l < layers.length; l++) {
            widths[l] = BorderImageWidthConverter.getInstance().convert(layers[l], font);
        }
        return widths;
    }

    @Override
    public String toString() {
        return "BorderImageWidthsSequenceConverter";
    }
}
