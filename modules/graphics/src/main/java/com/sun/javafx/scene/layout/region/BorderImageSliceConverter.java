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

import javafx.scene.layout.BorderWidths;
import javafx.scene.text.Font;
import javafx.css.ParsedValue;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.css.StyleConverter;

/**
 */
public final class BorderImageSliceConverter extends StyleConverter<ParsedValue[], BorderImageSlices> {

    private static final BorderImageSliceConverter BORDER_IMAGE_SLICE_CONVERTER =
            new BorderImageSliceConverter();

    public static BorderImageSliceConverter getInstance() {
        return BORDER_IMAGE_SLICE_CONVERTER;
    }

    // Disallow instantiation
    private BorderImageSliceConverter() { }

    @Override
    public BorderImageSlices convert(ParsedValue<ParsedValue[], BorderImageSlices> layer, Font font) {
        // Parser sends insets and boolean fill
        final ParsedValue[] values = layer.getValue();

        // value[0] is ParsedValue<Value<?,Size>[],Insets>
        final ParsedValue<?, Size>[] sizes = (ParsedValue<?, Size>[]) values[0].getValue();
        final Size topSz = sizes[0].convert(font);
        final Size rightSz = sizes[1].convert(font);
        final Size bottomSz = sizes[2].convert(font);
        final Size leftSz = sizes[3].convert(font);

        return new BorderImageSlices(
                new BorderWidths(
                topSz.pixels(font),
                rightSz.pixels(font),
                bottomSz.pixels(font),
                leftSz.pixels(font),
                topSz.getUnits() == SizeUnits.PERCENT,
                rightSz.getUnits() == SizeUnits.PERCENT,
                bottomSz.getUnits() == SizeUnits.PERCENT,
                leftSz.getUnits() == SizeUnits.PERCENT
                ),
                (Boolean) values[1].getValue());
    }

    /**
     * @inheritDoc
     */
    @Override public String toString() {
        return "BorderImageSliceConverter";
    }
}
