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

import javafx.scene.layout.BorderWidths;
import javafx.scene.text.Font;
import javafx.css.ParsedValue;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.css.StyleConverter;

/**
 * User: richardbair
 * Date: 8/10/12
 * Time: 8:07 PM
 */
public class BorderImageWidthConverter extends StyleConverter<ParsedValue[], BorderWidths> {
    private static final BorderImageWidthConverter CONVERTER_INSTANCE = new BorderImageWidthConverter();

    public static BorderImageWidthConverter getInstance() {
        return CONVERTER_INSTANCE;
    }

    private BorderImageWidthConverter() { }

    @Override
    public BorderWidths convert(ParsedValue<ParsedValue[], BorderWidths> value, Font font) {
        ParsedValue[] sides = value.getValue();
        assert sides.length == 4;

        double top = 1, right = 1, bottom = 1, left = 1;
        boolean topPercent = false, rightPercent = false, bottomPercent = false, leftPercent = false;
        ParsedValue val = sides[0];
        if ("auto".equals(val.getValue())) {
            top = BorderWidths.AUTO;
        } else {
            Size size = (Size)val.convert(font);
            top = size.pixels(font);
            topPercent = size.getUnits() == SizeUnits.PERCENT;
        }

        val = sides[1];
        if ("auto".equals(val.getValue())) {
            right = BorderWidths.AUTO;
        } else {
            Size size = (Size)val.convert(font);
            right = size.pixels(font);
            rightPercent = size.getUnits() == SizeUnits.PERCENT;
        }

        val = sides[2];
        if ("auto".equals(val.getValue())) {
            bottom = BorderWidths.AUTO;
        } else {
            Size size = (Size)val.convert(font);
            bottom = size.pixels(font);
            bottomPercent = size.getUnits() == SizeUnits.PERCENT;
        }

        val = sides[3];
        if ("auto".equals(val.getValue())) {
            left = BorderWidths.AUTO;
        } else {
            Size size = (Size)val.convert(font);
            left = size.pixels(font);
            leftPercent = size.getUnits() == SizeUnits.PERCENT;
        }

        return new BorderWidths(top, right, bottom, left, topPercent, rightPercent, bottomPercent, leftPercent);
    }

    @Override
    public String toString() {
        return "BorderImageWidthConverter";
    }
}
