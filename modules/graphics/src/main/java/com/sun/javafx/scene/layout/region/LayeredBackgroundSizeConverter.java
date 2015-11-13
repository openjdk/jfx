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
import javafx.scene.layout.BackgroundSize;
import javafx.scene.text.Font;

/**
 * This class appears to be an artifact of the implementation, such that we need
 * to pass values around as ParsedValues, and so we have a parsed value that just
 * holds an array of background sizes, and the converter just pulls those
 * background sizes back out.
 *
 * background-size      <bg-size> [ , <bg-size> ]*
 * <bg-size> = [ <size> | auto ]{1,2} | cover | contain
 * @see <a href="http://www.w3.org/TR/css3-background/#the-background-size">background-size</a>
 */
public final class LayeredBackgroundSizeConverter extends StyleConverter<ParsedValue<ParsedValue[], BackgroundSize>[], BackgroundSize[]> {
    private static final LayeredBackgroundSizeConverter LAYERED_BACKGROUND_SIZE_CONVERTER =
            new LayeredBackgroundSizeConverter();

    public static LayeredBackgroundSizeConverter getInstance() {
        return LAYERED_BACKGROUND_SIZE_CONVERTER;
    }

    private LayeredBackgroundSizeConverter() {
        super();
    }

    @Override
    public BackgroundSize[] convert(ParsedValue<ParsedValue<ParsedValue[], BackgroundSize>[], BackgroundSize[]> value, Font font) {
        ParsedValue<ParsedValue[], BackgroundSize>[] layers = value.getValue();
        BackgroundSize[] sizes = new BackgroundSize[layers.length];
        for (int l = 0; l < layers.length; l++) {
            sizes[l] = layers[l].convert(font);
        }
        return sizes;
    }
}
