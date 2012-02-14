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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;

import com.sun.javafx.css.Size;
import com.sun.javafx.css.StyleConverter;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.ParsedValue;
import com.sun.javafx.css.converters.InsetsConverter;
import java.util.ArrayList;
import javafx.beans.value.WritableValue;
import javafx.scene.Node;


public class StrokeBorder extends Border {

    // lazy, thread-safe
    private static class Holder {

        /**
         * Convert layers of border paint values to an array of Paint[], where
         * each layer contains one Paint element per border.
         */
        private static final LayeredBorderPaintConverter LAYERED_BORDER_PAINT_CONVERTER =
                new LayeredBorderPaintConverter();

        /**
         * Convert an array of border paint values to an array of Paint which
         * contains one Paint element per border (top, right, bottom, left).
         */
        private static final BorderPaintConverter BORDER_PAINT_CONVERTER =
                new BorderPaintConverter();

        /**
         * Convert layers of border style values to an array of BorderStyle[], where
         * each layer contains one BorderStyle element per border.
         */
        private static final LayeredBorderStyleConverter LAYERED_BORDER_STYLE_CONVERTER =
                new LayeredBorderStyleConverter();
        /**
         * Convert an array of border style values to an array of BorderStyle which
         * contains one BorderStyle element per border (top, right, bottom, left).
         */
        private static final BorderStyleSequenceConverter BORDER_STYLE_SEQUENCE_CONVERTER =
                new BorderStyleSequenceConverter();

        /**
         * Convert a sequence of values to a BorderStyle.
         */
        private static final BorderStyleConverter BORDER_STYLE_CONVERTER =
                new BorderStyleConverter();

    }

     /**
      * Super-lazy instantiation pattern from Bill Pugh.
      * @treatAsPrivate implementation detail
      */
     private static class StyleableProperties {
         
        private static final StyleableProperty<Node,Paint[]> BORDER_COLOR =
            new StyleableProperty<Node,Paint[]>("-fx-border-color", 
                LayeredBorderPaintConverter.getInstance()) {

            @Override
            public boolean isSettable(Node node) {
                return false;
            }

            @Override
            public WritableValue<Paint[]> getWritableValue(Node node) {
                return null;
            }
        };
        
        private static final StyleableProperty<Node,BorderStyle[][]> BORDER_STYLE =
            new StyleableProperty<Node,BorderStyle[][]>("-fx-border-style",
                LayeredBorderStyleConverter.getInstance()){

            @Override
            public boolean isSettable(Node node) {
                return false;
            }

            @Override
            public WritableValue<BorderStyle[][]> getWritableValue(Node node) {
                return null;
            }
        };
        
        private static final StyleableProperty<Node,Margins[]> BORDER_WIDTH = 
            new StyleableProperty<Node,Margins[]> ("-fx-border-width",
                Margins.SequenceConverter.getInstance()) {

            @Override
            public boolean isSettable(Node node) {
                return false;
            }

            @Override
            public WritableValue<Margins[]> getWritableValue(Node node) {
                return null;
            }
        };
        
        private static final StyleableProperty<Node,Margins[]> BORDER_RADIUS =
            new StyleableProperty<Node,Margins[]>("-fx-border-radius", 
                Margins.SequenceConverter.getInstance()){

            @Override
            public boolean isSettable(Node node) {
                return false;
            }

            @Override
            public WritableValue<Margins[]> getWritableValue(Node node) {
                return null;
            }
        };
        
        private static final StyleableProperty<Node,Insets[]> BORDER_INSETS =
            new StyleableProperty<Node,Insets[]>("-fx-border-insets", 
                InsetsConverter.SequenceConverter.getInstance()) {

            @Override
            public boolean isSettable(Node node) {
                return false;
            }

            @Override
            public WritableValue<Insets[]> getWritableValue(Node node) {
                return null;
            }
        };

        private static final List<StyleableProperty> STYLEABLES;
        static {
             final List<StyleableProperty> subProperties = 
                 new ArrayList<StyleableProperty>();
            Collections.addAll(subProperties,
                BORDER_COLOR,
                BORDER_INSETS,
                BORDER_RADIUS,
                BORDER_STYLE,
                BORDER_WIDTH
            );
            STYLEABLES = Collections.unmodifiableList(subProperties);
        }
        
    }

     /**
      * Super-lazy instantiation pattern from Bill Pugh.
      * @treatAsPrivate implementation detail
      */
     public static List<StyleableProperty> impl_CSS_STYLEABLES() {
         return StrokeBorder.StyleableProperties.STYLEABLES;
     }

    /**
     * Defines the radius of the top left corner of the region this border is
     * being applied to. It only has effect if the region is a rectangular region.
     *
     * @defaultValue 0.0
     */
    public double getTopLeftCornerRadius() { return topLeftCornerRadius; }
    final private double topLeftCornerRadius;

    /**
     * Defines the radius of the top right corner of the region this border is
     * being applied to. It only has effect if the region is a rectangular region.
     *
     * @defaultValue 0.0
     */
    public double getTopRightCornerRadius() { return topRightCornerRadius; }
    final private double topRightCornerRadius;

    /**
     * Define the radius of the bottom left corner of the region this border is
     * being applied to. It only has effect if the region is a rectangular region.
     *
     * @defaultValue 0.0
     */
    public double getBottomLeftCornerRadius() { return bottomLeftCornerRadius; }
    final private double bottomLeftCornerRadius;

    /**
     * Defines the radius of the bottom right corner of the region this border is
     * being applied to. It only has effect if the region is a rectangular region.
     *
     * @defaultValue 0.0
     */
    public double getBottomRightCornerRadius() { return bottomRightCornerRadius; }
    final private double bottomRightCornerRadius;

    /**
     * Defines the fill of left side of this border
     *
     * @css border-top-color
     * @defaultValue BLACK
     */
    public Paint getLeftFill() { return leftFill; }
    final private Paint leftFill;

    /**
     * Defines the fill of top side of this border. If {@code null} then the
     * leftFill is used.
     *
     * @css border-top-color
     * @defaultValue null = same as leftFill
     */
    public Paint getTopFill() { return topFill; }
    final private Paint topFill;

    /**
     * Defines the fill of right side of this border. If {@code null} then the
     * leftFill is used.
     *
     * @css border-top-color
     * @defaultValue null = same as leftFill
     */
    public Paint getRightFill() { return rightFill; }
    final private Paint rightFill;

    /**
     * Defines the fill of bottom side of this border. If {@code null} then the
     * leftFill is used.
     *
     * @css border-top-color
     * @defaultValue null = same as leftFill
     */
    public Paint getBottomFill() { return bottomFill; }
    final private Paint bottomFill;

    /**
     * Defines the style of top side of this border.
     *
     * @css border-top-style
     * @defaultValue solid
     */
    public BorderStyle getTopStyle() { return topStyle; }
    final private BorderStyle topStyle;

    /**
     * Defines the style of top side of this border. If {@code null} then
     * topStyle is used;
     *
     * @css border-right-style
     * @defaultValue null = same as topStyle
     */
    public BorderStyle getRightStyle() { return rightStyle; }
    final private BorderStyle rightStyle;

    /**
     * Defines the style of bottom side of this border. If {@code null} then
     * topStyle is used;  Use BorderStyle.NONE to set the border to
     * have no border style.
     *
     * @css border-bottom-style
     * @defaultValue null = same as topStyle
     */
    public BorderStyle getBottomStyle() { return bottomStyle; }
    final private BorderStyle bottomStyle;

    /**
     * Defines the style of left side of this border. If {@code null} then
     * rightStyle is used. Use BorderStyle.NONE to set the border to
     * have no border style.
     *
     * @css border-left-style
     * @defaultValue null = same as rightStyle
     */
    public BorderStyle getLeftStyle() { return leftStyle; }
    final private BorderStyle leftStyle;

    public static class Builder extends Border.Builder {
        private double topLeftCornerRadius = 0.0;
        private double topRightCornerRadius = 0.0;
        private double bottomLeftCornerRadius = 0.0;
        private double bottomRightCornerRadius = 0.0;
        private Paint topFill = Color.BLACK;
        private Paint rightFill = Color.BLACK;
        private Paint bottomFill = Color.BLACK;
        private Paint leftFill = Color.BLACK;
        private BorderStyle topStyle = BorderStyle.SOLID;
        private BorderStyle rightStyle = BorderStyle.SOLID;
        private BorderStyle bottomStyle = BorderStyle.SOLID;
        private BorderStyle leftStyle = BorderStyle.SOLID;

        public Builder() {}

        public Builder setTopLeftCornerRadius(double f) {
            topLeftCornerRadius = f;
            return this;
        }
        public Builder setTopRightCornerRadius(double f) {
            topRightCornerRadius = f;
            return this;
    }
        public Builder setBottomRightCornerRadius(double f) {
            bottomRightCornerRadius = f;
            return this;
        }
        public Builder setBottomLeftCornerRadius(double f) {
            bottomLeftCornerRadius = f;
            return this;
        }
        public Builder setTopFill(Paint f) {
            topFill = f;
            return this;
        }
        public Builder setRightFill(Paint f) {
            rightFill = f;
            return this;
        }
        public Builder setBottomFill(Paint f) {
            bottomFill = f;
            return this;
        }
        public Builder setLeftFill(Paint f) {
            leftFill = f;
            return this;
        }
        public Builder setTopStyle(BorderStyle f) {
            topStyle = f;
            return this;
        }
        public Builder setRightStyle(BorderStyle f) {
            rightStyle = f;
            return this;
        }
        public Builder setBottomStyle(BorderStyle f) {
            bottomStyle = f;
            return this;
        }
        public Builder setLeftStyle(BorderStyle f) {
            leftStyle = f;
            return this;
        }
        public StrokeBorder build() {

            if (topStyle != null) topStyle.adjustForStrokeWidth(topWidth);
            if (rightStyle != null) rightStyle.adjustForStrokeWidth(rightWidth);
            if (bottomStyle != null) bottomStyle.adjustForStrokeWidth(bottomWidth);
            if (leftStyle != null) leftStyle.adjustForStrokeWidth(leftWidth);

            return new StrokeBorder(
                topWidth,
                rightWidth,
                bottomWidth,
                leftWidth,
                proportionalWidth,
                offsets,
                topLeftCornerRadius,
                topRightCornerRadius,
                bottomLeftCornerRadius,
                bottomRightCornerRadius,
                topFill,
                rightFill,
                bottomFill,
                leftFill,
                topStyle,
                rightStyle,
                bottomStyle,
                leftStyle
            );
        }
    }

    StrokeBorder(
        double topWidth,
        double rightWidth,
        double bottomWidth,
        double leftWidth,
        boolean proportionalWidth,
        Insets offsets,
        double topLeftCornerRadius,
        double topRightCornerRadius,
        double bottomLeftCornerRadius,
        double bottomRightCornerRadius,
        Paint topFill,
        Paint rightFill,
        Paint bottomFill,
        Paint leftFill,
        BorderStyle topStyle,
        BorderStyle rightStyle,
        BorderStyle bottomStyle,
        BorderStyle leftStyle
    ) {
        super (
            topWidth,
            rightWidth,
            bottomWidth,
            leftWidth,
            proportionalWidth,
            offsets
        );
        this.topLeftCornerRadius = topLeftCornerRadius;
        this.topRightCornerRadius = topRightCornerRadius;
        this.bottomLeftCornerRadius = bottomLeftCornerRadius;
        this.bottomRightCornerRadius = bottomRightCornerRadius;
        this.topFill = topFill;
        this.rightFill = rightFill;
        this.bottomFill = bottomFill;
        this.leftFill = leftFill;
        this.topStyle = topStyle;
        this.rightStyle = rightStyle;
        this.bottomStyle = bottomStyle;
        this.leftStyle = leftStyle;
    }

    /*
     * border-style: <border-style> [, <border-style>]*
     * where <border-style> = <dash-style> [phase(<number>)]? [centered | inside | outside]?
     *                        [line-join [miter <number> | bevel | round]]?
     *                        [line-cap [square | butt | round]]?
     * where <dash-style> = none | solid | dotted | dashed | segments(<size>[, <size>]+) ]
     */
    static final public class LayeredBorderStyleConverter
            extends StyleConverter<ParsedValue<ParsedValue<ParsedValue[],BorderStyle>[], BorderStyle[]>[], BorderStyle[][]> {

        public static LayeredBorderStyleConverter getInstance() {
            return Holder.LAYERED_BORDER_STYLE_CONVERTER;
        }

        private LayeredBorderStyleConverter() {
            super();
        }

        @Override
        public BorderStyle[][]
                convert(ParsedValue<ParsedValue<ParsedValue<ParsedValue[], BorderStyle>[],BorderStyle[]>[], BorderStyle[][]> value, Font font) {

            ParsedValue<ParsedValue<ParsedValue[], BorderStyle>[],BorderStyle[]>[] layers = value.getValue();
            BorderStyle[][] styles = new BorderStyle[layers.length][0];

            for (int layer=0; layer<layers.length; layer++) {
                styles[layer] = layers[layer].convert(font);
    }
            return styles;
        }

        @Override
        public String toString() {
            return "LayeredBorderStyleType";
    }

    }

    static final public class BorderStyleSequenceConverter extends StyleConverter<ParsedValue<ParsedValue[],BorderStyle>[],BorderStyle[]> {

        public static BorderStyleSequenceConverter getInstance() {
            return Holder.BORDER_STYLE_SEQUENCE_CONVERTER;
        }

        private BorderStyleSequenceConverter() {
            super();
        }

        @Override
        public BorderStyle[] convert(ParsedValue<ParsedValue<ParsedValue[],BorderStyle>[], BorderStyle[]> value, Font font) {

            ParsedValue<ParsedValue[],BorderStyle>[] borders = value.getValue();
            BorderStyle[] styles = new BorderStyle[4];

            styles[0] = (borders.length > 0) ?
                borders[0].convert(font) : BorderStyle.SOLID;

            styles[1] = (borders.length > 1) ?
                borders[1].convert(font) : styles[0];

            styles[2] = (borders.length > 2) ?
                borders[2].convert(font) : styles[0];

            styles[3] = (borders.length > 3) ?
                borders[3].convert(font) : styles[1];

            return styles;
    }

        @Override
        public String toString() {
            return "BorderStyleType";
    }

    }


    static final public class BorderStyleConverter extends StyleConverter<ParsedValue[],BorderStyle> {

        public static BorderStyleConverter getInstance() {
            return Holder.BORDER_STYLE_CONVERTER;
        }

        private BorderStyleConverter() {
            super();
        }

        @Override
        public BorderStyle convert(ParsedValue<ParsedValue[],BorderStyle> value, Font font) {

            ParsedValue[] values = value.getValue();

            // TODO: does the parser do this, or will the dash segments just be null?
            if (values.length == 1 && BorderStyle.NONE == values[0].getValue()) {
                return BorderStyle.NONE;
            }

            ParsedValue<?,Size>[] dash_vals =
                    ((ParsedValue<ParsedValue<?,Size>[],Double[]>)values[0]).getValue();

            if (dash_vals == null) {
                return BorderStyle.NONE;
            }

            double[] dashes = new double[dash_vals.length];
            for(int dash=0; dash<dash_vals.length; dash++) {
                Size size = dash_vals[dash].convert(font);
                dashes[dash] = size.pixels(font);
            }

            Double dash_phase =
                    (values[1] != null) ? (Double)values[1].convert(font) : 0;

            StrokeType stroke_type =
                    (values[2] != null) ? (StrokeType)values[2].convert(font) : StrokeType.CENTERED;

            StrokeLineJoin line_join =
                    (values[3] != null) ? (StrokeLineJoin)values[3].convert(font) : StrokeLineJoin.MITER;

            Double miter_limit =
                    (values[4] != null) ? (Double)values[4].convert(font) : 10;

            StrokeLineCap line_cap =
                    (values[5] != null) ? (StrokeLineCap)values[5].convert(font) : StrokeLineCap.BUTT;

            BorderStyle borderStyle = new BorderStyle(stroke_type, line_join, line_cap,
                    miter_limit, dash_phase, dashes);
            if (BorderStyle.SOLID.equals(borderStyle)) {
                return BorderStyle.SOLID;
            } else {
                return borderStyle;
            }
        }

        @Override
        public String toString() {
            return "StyleType";
        }

    }

    /*
     * border-color	<paint> | <paint> <paint> <paint> <paint> [ , [<paint> | <paint> <paint> <paint> <paint>] ]*	null
     */
    static final public class LayeredBorderPaintConverter extends StyleConverter<ParsedValue<ParsedValue<?,Paint>[],Paint[]>[], Paint[][]> {

        public static LayeredBorderPaintConverter getInstance() {
            return Holder.LAYERED_BORDER_PAINT_CONVERTER;
        }

        private LayeredBorderPaintConverter() {
            super();
        }

        @Override
        public Paint[][] convert(ParsedValue<ParsedValue<ParsedValue<?,Paint>[],Paint[]>[], Paint[][]> value, Font font) {
            ParsedValue<ParsedValue<?,Paint>[],Paint[]>[] layers = value.getValue();
            Paint[][] paints = new Paint[layers.length][0];
            for(int layer=0; layer<layers.length; layer++) {
                paints[layer] = BorderPaintConverter.getInstance().convert(layers[layer],font);
    }
            return paints;
        }

        @Override
        public String toString() {
            return "LayeredBorderPaintType";
}
    }

    static final public class BorderPaintConverter extends StyleConverter<ParsedValue<?,Paint>[], Paint[]> {

        public static BorderPaintConverter getInstance() {
            return Holder.BORDER_PAINT_CONVERTER;
        }

        private BorderPaintConverter() {
            super();
        }

        @Override
        public Paint[] convert(ParsedValue<ParsedValue<?,Paint>[], Paint[]> value, Font font) {
            ParsedValue<?,Paint>[] borders = value.getValue();
            Paint[] paints = new Paint[4];

            paints[0] = (borders.length > 0) ?
                borders[0].convert(font) : Color.BLACK;

            paints[1] = (borders.length > 1) ?
                borders[1].convert(font) : paints[0];

            paints[2] = (borders.length > 2) ?
                borders[2].convert(font) : paints[0];

            paints[3] = (borders.length > 3) ?
                borders[3].convert(font) : paints[1];

            return paints;
        }

        @Override
        public String toString() {
            return "BorderPaintType";
        }
    }

}
