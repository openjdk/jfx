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

import javafx.scene.text.Font;

import com.sun.javafx.css.Size;
import com.sun.javafx.css.SizeUnits;
import com.sun.javafx.css.StyleConverter;
import com.sun.javafx.css.ParsedValue;

/**
 * Similar to Insets but with flag denoting values are proportional.
 * If proportional is true, then the values represent fractions or percentages
 * and are in the range 0..1, although this is not enforced.
 */
public class Margins {

    // lazy, thread-safe instatiation
    private static class Holder {
        static Converter CONVERTER_INSTANCE = new Converter();
        static SequenceConverter SEQUENCE_CONVERTER_INSTANCE = new SequenceConverter();
    }

    final private double top;
    public final double getTop() { return top; }

    final private double right;
    public final double getRight() { return right; }

    final private double bottom;
    public final double getBottom() { return bottom; }

    final private double left;
    public final double getLeft() { return left; }

    final private boolean proportional;
    public final boolean isProportional() { return proportional; }

    public Margins(double top, double right, double bottom, double left, boolean proportional) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
        this.proportional = proportional;
    }

    @Override
    public String toString() {
        return "top: "+top+"\nright: "+right+"\nbottom: "+bottom+"\nleft: "+left;
    }

    /**
     * Convert a sequence of sizes to an Margins
     */
    public static final class Converter extends StyleConverter<ParsedValue<?, Size>[], Margins> {

        public static Converter getInstance() {
            return Holder.CONVERTER_INSTANCE;
        }

        private Converter() {
            super();
        }

        @Override
        public Margins convert(ParsedValue<ParsedValue<?, Size>[], Margins> value, Font font) {
            ParsedValue<?, Size>[] sides = value.getValue();
            Size topSz = (sides.length > 0) ? sides[0].convert(font) : new Size(0.0F, SizeUnits.PX);
            Size rightSz = (sides.length > 1) ? sides[1].convert(font) : topSz;
            Size bottomSz = (sides.length > 2) ? sides[2].convert(font) : topSz;
            Size leftSz = (sides.length > 3) ? sides[3].convert(font) : rightSz;
            boolean proportional = false;
            if (topSz.getUnits() == rightSz.getUnits() && topSz.getUnits() == bottomSz.getUnits() && topSz.getUnits() == leftSz.getUnits()) {
                proportional = topSz.getUnits() == SizeUnits.PERCENT;
            } else {
                System.err.println("units do not match");
            }
            double top = topSz.pixels(font);
            double right = rightSz.pixels(font);
            double bottom = bottomSz.pixels(font);
            double left = leftSz.pixels(font);
            return new Margins(top, right, bottom, left, proportional);
        }

        @Override
        public String toString() {
            return "MarginsConverter";
        }
    }

    /**
     * Convert a sequence of sizes to an Insets
     */
    public static final class SequenceConverter extends StyleConverter<ParsedValue<ParsedValue<?, Size>[], Margins>[], Margins[]> {

        public static SequenceConverter getInstance() {
            return Holder.SEQUENCE_CONVERTER_INSTANCE;
        }

        private SequenceConverter() {
            super();
        }

        @Override
        public Margins[] convert(ParsedValue<ParsedValue<ParsedValue<?, Size>[], Margins>[], Margins[]> value, Font font) {
            ParsedValue<ParsedValue<?, Size>[], Margins>[] layers = value.getValue();
            Margins[] margins = new Margins[layers.length];
            for (int layer = 0; layer < layers.length; layer++) {
                margins[layer] = Converter.getInstance().convert(layers[layer], font);
            }
            return margins;
        }

        @Override
        public String toString() {
            return "MarginsSequenceConverter";
        }
    }

}
