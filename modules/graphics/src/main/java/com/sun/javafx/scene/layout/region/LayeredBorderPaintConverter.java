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
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

/*
 * border-color <paint> | <paint> <paint> <paint> <paint> [ , [<paint> | <paint> <paint> <paint> <paint>] ]*    null
 */
public final class LayeredBorderPaintConverter extends StyleConverter<ParsedValue<ParsedValue<?,Paint>[],Paint[]>[], Paint[][]> {
    /**
     * Convert layers of border paint values to an array of Paint[], where
     * each layer contains one Paint element per border.
     */
    private static final LayeredBorderPaintConverter LAYERED_BORDER_PAINT_CONVERTER =
            new LayeredBorderPaintConverter();

    public static LayeredBorderPaintConverter getInstance() {
        return LAYERED_BORDER_PAINT_CONVERTER;
    }

    private LayeredBorderPaintConverter() {
        super();
    }

    @Override
    public Paint[][] convert(ParsedValue<ParsedValue<ParsedValue<?,Paint>[],Paint[]>[], Paint[][]> value, Font font) {
        ParsedValue<ParsedValue<?,Paint>[],Paint[]>[] layers = value.getValue();
        Paint[][] paints = new Paint[layers.length][0];
        for(int layer=0; layer<layers.length; layer++) {
            paints[layer] = StrokeBorderPaintConverter.getInstance().convert(layers[layer],font);
        }
        return paints;
    }

    @Override
    public String toString() {
        return "LayeredBorderPaintConverter";
    }
}

