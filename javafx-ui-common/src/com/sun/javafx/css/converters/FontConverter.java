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
package com.sun.javafx.css.converters;

import com.sun.javafx.Utils;
import com.sun.javafx.css.FontUnits;
import com.sun.javafx.css.Size;
import com.sun.javafx.css.StyleConverterImpl;
import java.util.Map;
import java.util.Map.Entry;
import javafx.css.CssMetaData;
import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

public final class FontConverter extends StyleConverterImpl<ParsedValue[], Font> {

    // lazy, thread-safe instatiation
    private static class Holder {
        static FontConverter INSTANCE = new FontConverter();
    }

    public static StyleConverter<ParsedValue[], Font> getInstance() {
        return Holder.INSTANCE;
    }

    private FontConverter() {
        super();
    }

    @Override 
    public Font convert(ParsedValue<ParsedValue[], Font> value, Font font) {
        ParsedValue[] values = value.getValue();
        Font aFont = (font != null) ? font : Font.getDefault();
        String family = (values[0] != null) ? Utils.stripQuotes((String) values[0].convert(aFont)) : aFont.getFamily();
        // if font size is given in terms of percent, then we have to call
        // pixels directly in order to pass the multiplier.
        double fsize = aFont.getSize();
        if (values[1] != null) {
            ParsedValue<?, ?> pv = (ParsedValue<?, ?>) values[1].getValue();
            Size size = (Size) pv.convert(aFont);
            fsize = size.pixels(aFont.getSize(), aFont);
        }
        FontWeight weight = (values[2] != null) ? (FontWeight) values[2].convert(aFont) : FontWeight.NORMAL;
        FontPosture style = (values[3] != null) ? (FontPosture) values[3].convert(aFont) : FontPosture.REGULAR;
        Font f = Font.font(family, weight, style, fsize);
        return f;
    }

    @Override
    public Font convert(Map<CssMetaData<? extends Styleable, ?>, Object> convertedValues) {
        Font font = Font.getDefault();
        double size = font.getSize();
        String family = font.getFamily();
        FontWeight weight = FontWeight.NORMAL;
        FontPosture style = FontPosture.REGULAR;
        
        for (Entry<CssMetaData<? extends Styleable, ?>, Object> entry : convertedValues.entrySet()) {

            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            final String prop = entry.getKey().getProperty();
            if (prop.endsWith("font-size")) {
                size = ((Number) value).doubleValue();
            } else if (prop.endsWith("font-family")) {
                family = Utils.stripQuotes((String) value);
            } else if (prop.endsWith("font-weight")) {
                weight = (FontWeight) value;
            } else if (prop.endsWith("font-style")) {
                style = (FontPosture) value;
            }
        }
        final Font f = Font.font(family, weight, style, size);
        return f;
    }

    @Override
    public String toString() {
        return "FontConverter";
    }

    public static final class FontStyleConverter extends StyleConverterImpl<FontUnits.Style, FontPosture> {

        // lazy, thread-safe instatiation
        private static class Holder {
            static FontStyleConverter INSTANCE = new FontStyleConverter();
        }

        public static FontStyleConverter getInstance() {
            return Holder.INSTANCE;
        }

        private FontStyleConverter() {
            super();
        }

        @Override
        public FontPosture convert(ParsedValue<FontUnits.Style, FontPosture> value, Font font) {

            final FontUnits.Style style = value.getValue();

            if (FontUnits.Style.INHERIT != style) {
                return style.toFontPosture();

            } else if (font != null) { // Inherit
                // TODO get the posture from the inherited font
                // TODO take font.oblique into account? (dekstop only vs common)
                return FontPosture.REGULAR;
            } else {
                return FontPosture.REGULAR;
            }
        }

        @Override
        public String toString() {
            return "FontConverter.StyleConverter";
        }
    }

    public static final class FontWeightConverter extends StyleConverterImpl<FontUnits.Weight, FontWeight> {

        // lazy, thread-safe instatiation
        private static class Holder {
            static FontWeightConverter INSTANCE = new FontWeightConverter();
        }

        public static FontWeightConverter getInstance() {
            return Holder.INSTANCE;
        }

        private FontWeightConverter() {
            super();
        }

        @Override
        public FontWeight convert(ParsedValue<FontUnits.Weight, FontWeight> value, Font font) {

            final FontUnits.Weight weight = value.getValue();

            if (FontUnits.Weight.INHERIT != weight
                    && FontUnits.Weight.BOLDER != weight
                    && FontUnits.Weight.LIGHTER != weight) {
                return weight.toFontWeight();
            } else if (font != null) {
                FontWeight fweight = FontWeight.NORMAL;
                // TODO: take font.embolden into account? (dekstop only vs common)
                if (FontUnits.Weight.INHERIT == weight) {
                    return fweight;
                } else if (FontUnits.Weight.BOLDER == weight) {
                    // TODO - this FontUnits.Weight API sucks.
                    return FontUnits.Weight.toWeight(fweight).bolder().toFontWeight();
                } else if (FontUnits.Weight.LIGHTER == weight) {
                    return FontUnits.Weight.toWeight(fweight).lighter().toFontWeight();
                } else {
                    return weight.toFontWeight();
                }
            } else {
                return weight.toFontWeight();
            }
        }

        @Override
        public String toString() {
            return "FontConverter.WeightConverter";
        }
    }
    
    public static final class FontSizeConverter extends StyleConverterImpl<ParsedValue<?, Size>, Number> {

        // lazy, thread-safe instatiation
        private static class Holder {
            static FontSizeConverter INSTANCE = new FontSizeConverter();
        }

        public static FontSizeConverter getInstance() {
            return Holder.INSTANCE;
        }

        private FontSizeConverter() {
            super();
        }

        @Override
        public Number convert(ParsedValue<ParsedValue<?, Size>, Number> value, Font font) {
            final ParsedValue<?, Size> size = value.getValue();
            return size.convert(font).pixels(font.getSize(), font);
        }

        @Override
        public String toString() {
            return "FontConverter.FontSizeConverter";
        }
    }    

}
