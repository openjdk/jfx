/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.scene.layout.CornerRadii;
import javafx.scene.text.Font;

/**
 * Convert parsed value of &lt;size&gt;{1,4} [ '/' &lt;size&gt;{1,4}]? [',' &lt;size&gt;{1,4} [ '/' &lt;size&gt;{1,4}]?]? to CornerRadii
 */
public final class CornerRadiiConverter extends StyleConverter<ParsedValue<ParsedValue<?,Size>[][],CornerRadii>[], CornerRadii[]> {

    private static final CornerRadiiConverter INSTANCE =
            new CornerRadiiConverter();

    public static CornerRadiiConverter getInstance() {
        return INSTANCE;
    }

    // Disallow instantiation
    private CornerRadiiConverter() { }


    @Override
    public CornerRadii[] convert(ParsedValue<ParsedValue<ParsedValue<?,Size>[][], CornerRadii>[], CornerRadii[]> value, Font font) {

        ParsedValue<ParsedValue<?,Size>[][], CornerRadii>[] values = value.getValue();
        CornerRadii[] cornerRadiiValues = new CornerRadii[values.length];

        // parser gives us 2x4 array normalized to the rules in http://www.w3.org/TR/css3-background/#the-border-radius
        for(int n=0; n<values.length; n++) {

            ParsedValue<?,Size>[][] sizes = values[n].getValue();

            Size topLeftHorizontalRadius = sizes[0][0].convert(font);
            Size topRightHorizontalRadius = sizes[0][1].convert(font);
            Size bottomRightHorizontalRadius = sizes[0][2].convert(font);
            Size bottomLeftHorizontalRadius = sizes[0][3].convert(font);
            Size topLeftVerticalRadius = sizes[1][0].convert(font);
            Size topRightVerticalRadius = sizes[1][1].convert(font);
            Size bottomRightVerticalRadius = sizes[1][2].convert(font);
            Size bottomLeftVerticalRadius = sizes[1][3].convert(font);

            cornerRadiiValues[n] = new CornerRadii(
                    topLeftHorizontalRadius.pixels(font),     topLeftVerticalRadius.pixels(font),
                    topRightVerticalRadius.pixels(font),      topRightHorizontalRadius.pixels(font),
                    bottomRightHorizontalRadius.pixels(font), bottomRightVerticalRadius.pixels(font),
                    bottomLeftVerticalRadius.pixels(font),    bottomLeftHorizontalRadius.pixels(font),
                    topLeftHorizontalRadius.getUnits()     == SizeUnits.PERCENT, topLeftVerticalRadius.getUnits()      == SizeUnits.PERCENT,
                    topRightVerticalRadius.getUnits()      == SizeUnits.PERCENT, topRightHorizontalRadius.getUnits()   == SizeUnits.PERCENT,
                    bottomRightHorizontalRadius.getUnits() == SizeUnits.PERCENT, bottomRightVerticalRadius.getUnits()  == SizeUnits.PERCENT,
                    bottomRightVerticalRadius.getUnits()   == SizeUnits.PERCENT, bottomLeftHorizontalRadius.getUnits() == SizeUnits.PERCENT
            );

        }

        return cornerRadiiValues;

    }
}
