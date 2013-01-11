/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.css.StyleConverterImpl;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.text.Font;

/**
 */
public final class RepeatStructConverter extends StyleConverterImpl<ParsedValue<BackgroundRepeat, BackgroundRepeat>[][], RepeatStruct[]> {

    private static final RepeatStructConverter REPEAT_STRUCT_CONVERTER =
            new RepeatStructConverter();

    public static RepeatStructConverter getInstance() {
        return REPEAT_STRUCT_CONVERTER;
    }

    private RepeatStructConverter() { }

    @Override
    public RepeatStruct[] convert(ParsedValue<ParsedValue<BackgroundRepeat, BackgroundRepeat>[][], RepeatStruct[]> value, Font font) {
        final ParsedValue<BackgroundRepeat, BackgroundRepeat>[][] layers = value.getValue();
        final RepeatStruct[] backgroundRepeat = new RepeatStruct[layers.length];
        for (int l = 0; l < layers.length; l++) {
            final ParsedValue<BackgroundRepeat, BackgroundRepeat>[] repeats = layers[l];
            final BackgroundRepeat horizontal = repeats[0].getValue();
            final BackgroundRepeat vertical = repeats[1].getValue();
            backgroundRepeat[l] = new RepeatStruct(horizontal, vertical);
        }
        return backgroundRepeat;
    }

    /**
     * @inheritDoc
     */
    @Override public String toString() {
        return "RepeatStructConverter";
    }
}
