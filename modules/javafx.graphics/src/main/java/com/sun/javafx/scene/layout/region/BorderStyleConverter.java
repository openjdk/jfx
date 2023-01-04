/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.css.ParsedValue;
import com.sun.javafx.css.ParsedValueImpl;
import javafx.css.Size;
import javafx.css.StyleConverter;

/**
 */
public class BorderStyleConverter  extends StyleConverter<ParsedValue[], BorderStrokeStyle> {
//    private static final ParsedValue<ParsedValue<?,Size>[],Double[]> DASHED =
//            new ParsedValue<ParsedValue<?,Size>[],Double[]>(
//                    new ParsedValue[] {
//                            new ParsedValue<Size,Size>(new Size(5.0f, SizeUnits.PX), null),
//                            new ParsedValue<Size,Size>(new Size(3.0f, SizeUnits.PX), null)
//                    }, SizeConverter.SequenceConverter.getInstance());
//
//    private static final ParsedValue<ParsedValue<?,Size>[],Double[]> DOTTED =
//            new ParsedValue<ParsedValue<?,Size>[],Double[]>(
//                    new ParsedValue[]{
//                            new ParsedValue<Size,Size>(new Size(1.0f, SizeUnits.PX), null),
//                            new ParsedValue<Size,Size>(new Size(3.0f, SizeUnits.PX), null)
//                    }, SizeConverter.SequenceConverter.getInstance());
//
//    private static final ParsedValue<ParsedValue<?,Size>[],Double[]> SOLID =
//            new ParsedValue<ParsedValue<?,Size>[],Double[]>(
//                    new ParsedValue[]{
//                            /* empty array */
//                    }, SizeConverter.SequenceConverter.getInstance());


    public static final ParsedValueImpl<ParsedValue[],Number[]> NONE = new ParsedValueImpl<>(null, null);
    public static final ParsedValueImpl<ParsedValue[],Number[]> HIDDEN = new ParsedValueImpl<>(null, null);
    public static final ParsedValueImpl<ParsedValue[],Number[]> DOTTED = new ParsedValueImpl<>(null, null);
    public static final ParsedValueImpl<ParsedValue[],Number[]> DASHED = new ParsedValueImpl<>(null, null);
    public static final ParsedValueImpl<ParsedValue[],Number[]> SOLID = new ParsedValueImpl<>(null, null);

    /**
     * Convert a sequence of values to a BorderStyle.
     */
    private static final BorderStyleConverter BORDER_STYLE_CONVERTER =
            new BorderStyleConverter();

    public static BorderStyleConverter getInstance() {
        return BORDER_STYLE_CONVERTER;
    }

    // Prevent instantiation
    private BorderStyleConverter() { }

    @Override
    public BorderStrokeStyle convert(ParsedValue<ParsedValue[],BorderStrokeStyle> value, Font font) {

        final ParsedValue[] values = value.getValue();

        // The first value may be some named style, such as DOTTED, DASHED, SOLID, or NONE.
        // However even if named, there might be additional style information such as the
        // round cap to use, etc. But most of the time, people will only define the name and
        // nothing more. So we special case this so that if you use "solid" in CSS and that
        // is all, then we map it to BorderStrokeStyle.SOLID and quite early.
        Object v = values[0];
        final boolean onlyNamed = values[1] == null &&
                values[2] == null &&
                values[3] == null &&
                values[4] == null &&
                values[5] == null;

        if (NONE == v) return BorderStrokeStyle.NONE;
        if (DOTTED == v && onlyNamed) {
            return BorderStrokeStyle.DOTTED;
        } else if (DASHED == v && onlyNamed) {
            return BorderStrokeStyle.DASHED;
        } else if (SOLID == v && onlyNamed) {
            return BorderStrokeStyle.SOLID;
        }

        // We have some custom specified value
        ParsedValue<?,Size>[] dash_vals =
                ((ParsedValue<ParsedValue<?,Size>[],Number[]>)values[0]).getValue();

        final List<Double> dashes;
        if (dash_vals == null) {
            if (DOTTED == v) {
                dashes = BorderStrokeStyle.DOTTED.getDashArray();
            } else if (DASHED == v) {
                dashes = BorderStrokeStyle.DASHED.getDashArray();
            } else if (SOLID == v) {
                dashes = BorderStrokeStyle.SOLID.getDashArray();
            } else {
                dashes = Collections.emptyList();
            }
        } else {
            dashes = new ArrayList<>(dash_vals.length);
            for(int dash=0; dash<dash_vals.length; dash++) {
                final Size size = dash_vals[dash].convert(font);
                dashes.add(size.pixels(font));
            }
        }

        final double dash_phase =
                (values[1] != null) ? (Double)values[1].convert(font) : 0;

        final StrokeType stroke_type =
                (values[2] != null) ? (StrokeType)values[2].convert(font) : StrokeType.INSIDE;

        final StrokeLineJoin line_join =
                (values[3] != null) ? (StrokeLineJoin)values[3].convert(font) : StrokeLineJoin.MITER;

        final double miter_limit =
                (values[4] != null) ? (Double)values[4].convert(font) : 10;

        final StrokeLineCap line_cap =
                (values[5] != null) ? (StrokeLineCap)values[5].convert(font) : DOTTED == v ? StrokeLineCap.ROUND : StrokeLineCap.BUTT;

        final BorderStrokeStyle borderStyle = new BorderStrokeStyle(stroke_type, line_join, line_cap,
                miter_limit, dash_phase, dashes);

        if (BorderStrokeStyle.SOLID.equals(borderStyle)) {
            return BorderStrokeStyle.SOLID;
        } else {
            return borderStyle;
        }
    }

    /**
     * @inheritDoc
     */
    @Override public String toString() {
        return "BorderStyleConverter";
    }

}
