/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.css.converter;

import javafx.css.Size;
import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.geometry.Insets;
import javafx.scene.text.Font;

/**
 * Converts a parsed value array of 1 to 4 size components to an Insets.
 * The size values are interpreted as
 * top, right, bottom, left.
 * If only top is given, that value is used on all sides.
 * If there is only top and right, then bottom is set to top and left is set to right.
 * If top, right and bottom are given, then left is set to right.
 *
 * @since 9
 */
public final class InsetsConverter extends StyleConverter<ParsedValue[], Insets> {

    // lazy, thread-safe instatiation
    private static class Holder {
        static final InsetsConverter INSTANCE = new InsetsConverter();
        static final SequenceConverter SEQUENCE_INSTANCE = new SequenceConverter();
    }

    public static StyleConverter<ParsedValue[], Insets> getInstance() {
        return Holder.INSTANCE;
    }

    private InsetsConverter() {
        super();
    }

    @Override
    public Insets convert(ParsedValue<ParsedValue[], Insets> value, Font font) {
        ParsedValue[] sides = value.getValue();
        double top = ((Size)sides[0].convert(font)).pixels(font);
        double right = (sides.length > 1) ? ((Size)sides[1].convert(font)).pixels(font) : top;
        double bottom = (sides.length > 2) ? ((Size)sides[2].convert(font)).pixels(font) : top;
        double left = (sides.length > 3) ? ((Size)sides[3].convert(font)).pixels(font) : right;
        return new Insets(top, right, bottom, left);
    }

    @Override
    public String toString() {
        return "InsetsConverter";
    }

    /**
     * Converts an array of parsed values, each of which is an array
     * of 1 to 4 size components, to an array of Insets objects.
     */
    public static final class SequenceConverter extends StyleConverter<ParsedValue<ParsedValue[], Insets>[], Insets[]> {

        public static SequenceConverter getInstance() {
            return Holder.SEQUENCE_INSTANCE;
        }

        private SequenceConverter() {
            super();
        }

        @Override
        public Insets[] convert(ParsedValue<ParsedValue<ParsedValue[], Insets>[], Insets[]> value, Font font) {
            ParsedValue<ParsedValue[], Insets>[] layers = value.getValue();
            Insets[] insets = new Insets[layers.length];
            for (int layer = 0; layer < layers.length; layer++) {
                insets[layer] = InsetsConverter.getInstance().convert(layers[layer], font);
            }
            return insets;
        }

        @Override
        public String toString() {
            return "InsetsSequenceConverter";
        }
    }
}

