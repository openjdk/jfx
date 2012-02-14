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

import java.util.Collections;
import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.text.Font;

import com.sun.javafx.css.Size;
import com.sun.javafx.css.SizeUnits;
import com.sun.javafx.css.StyleConverter;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.ParsedValue;
import com.sun.javafx.css.converters.InsetsConverter;
import com.sun.javafx.css.converters.URLConverter;
import java.util.ArrayList;
import javafx.beans.value.WritableValue;
import javafx.scene.Node;

/**
 * A Border that uses a image as fill. The image is sliced into 9 parts which
 * are used to fill the top-left,top,top-right,right,center,left,bottom-left,
 * bottom and bottom-right sections of the border.
 *
 */
public class BorderImage extends Border {

    /* lazy, thread-safe */
    private static class Holder {

        private static final RepeatConverter BORDER_IMAGE_REPEAT_CONVERTER =
                new RepeatConverter();
        private static final SliceConverter BORDER_IMAGE_SLICE_CONVERTER =
                new SliceConverter();
        private static final SliceSequenceConverter BORDER_IMAGE_SLICE_SEQUENCE_CONVERTER =
                new SliceSequenceConverter();
    }

    /**
     * Super-lazy instantiation pattern from Bill Pugh.
     * @treatAsPrivate implementation detail
     */
    private static class StyleableProperties {

        private static final StyleableProperty<Node,String[]> BORDER_IMAGE_SOURCE =
            new StyleableProperty<Node,String[]>("-fx-border-image-source", 
                URLConverter.SequenceConverter.getInstance()) {

            @Override
            public boolean isSettable(Node node) {
                return false;
            }

            @Override
            public WritableValue<String[]> getWritableValue(Node node) {
                return null;
            }
        };
        
        private static final StyleableProperty<Node,BorderImageRepeat[]> BORDER_IMAGE_REPEAT =
            new StyleableProperty<Node,BorderImageRepeat[]>("-fx-border-image-repeat",
                RepeatConverter.getInstance(), 
                new BorderImageRepeat[] { new BorderImageRepeat(Repeat.REPEAT, Repeat.REPEAT) }){

            @Override
            public boolean isSettable(Node node) {
                return false;
            }

            @Override
            public WritableValue<BorderImageRepeat[]> getWritableValue(Node node) {
                return null;
            }
        };
        
        private static final StyleableProperty<Node,BorderImageSlice[]> BORDER_IMAGE_SLICE = 
            new StyleableProperty<Node,BorderImageSlice[]> ("-fx-border-image-slice",
                SliceSequenceConverter.getInstance(), 
                new BorderImageSlice[] { new BorderImageSlice(1,1,1,1,true,false) }) {

            @Override
            public boolean isSettable(Node node) {
                return false;
            }

            @Override
            public WritableValue<BorderImageSlice[]> getWritableValue(Node node) {
                return null;
            }
        };
        
        private static final StyleableProperty<Node,Margins[]> BORDER_IMAGE_WIDTH =
            new StyleableProperty<Node,Margins[]>("-fx-border-image-width", 
                Margins.SequenceConverter.getInstance(), 
                new Margins[] { new Margins(1.0,1.0,1.0,1.0,true) }){

            @Override
            public boolean isSettable(Node node) {
                return false;
            }

            @Override
            public WritableValue<Margins[]> getWritableValue(Node node) {
                return null;
            }
        };
        
        private static final StyleableProperty<Node,Insets[]> BORDER_IMAGE_INSETS =
            new StyleableProperty<Node,Insets[]>("-fx-border-image-insets", 
                InsetsConverter.SequenceConverter.getInstance(), 
                new Insets[] {Insets.EMPTY}) {

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
                BORDER_IMAGE_SOURCE,
                BORDER_IMAGE_REPEAT,
                BORDER_IMAGE_SLICE,
                BORDER_IMAGE_WIDTH,
                BORDER_IMAGE_INSETS
            );
            STYLEABLES = Collections.unmodifiableList(subProperties);
        }
    }

    /**
     * Super-lazy instantiation pattern from Bill Pugh.
     * @treatAsPrivate implementation detail
     */
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return BorderImage.StyleableProperties.STYLEABLES;
    }

    public static class Builder extends Border.Builder {

        private Image image;
        private boolean fillCenter;
        private boolean proportionalSlice = true;
        private double topSlice = 1.0;
        private double rightSlice = 1.0;
        private double bottomSlice = 1.0;
        private double leftSlice = 1.0;
        private Repeat repeatX = Repeat.NO_REPEAT;
        private Repeat repeatY = Repeat.NO_REPEAT;

        public Builder setImage(Image i) {
            image = i;
            return this;
        }

        public Builder setFillCenter(boolean b) {
            fillCenter = b;
            return this;
        }

        public Builder setProportionalSlice(boolean b) {
            proportionalSlice = b;
            return this;
        }

        public Builder setTopSlice(double f) {
            topSlice = f;
            return this;
        }

        public Builder setRightSlice(double f) {
            rightSlice = f;
            return this;
        }

        public Builder setBottomSlice(double f) {
            bottomSlice = f;
            return this;
        }

        public Builder setLeftSlice(double f) {
            leftSlice = f;
            return this;
        }

        public Builder setRepeatX(Repeat r) {
            repeatX = r;
            return this;
        }

        public Builder setRepeatY(Repeat r) {
            repeatY = r;
            return this;
        }

        @Override
        public Builder setLeftWidth(double f) {
            super.setLeftWidth(f);
            return this;
        }

        @Override
        public Builder setTopWidth(double f) {
            super.setTopWidth(f);
            return this;
        }

        @Override
        public Builder setRightWidth(double f) {
            super.setRightWidth(f);
            return this;
        }

        @Override
        public Builder setBottomWidth(double f) {
            super.setBottomWidth(f);
            return this;
        }

        @Override
        public Builder setProportionalWidth(boolean b) {
            super.setProportionalWidth(b);
            return this;
        }

        @Override
        public Builder setOffsets(Insets i) {
            super.setOffsets(i);
            return this;
        }

        public BorderImage build() {
            return new BorderImage(image, fillCenter,
                    topSlice, rightSlice, bottomSlice, leftSlice,
                    repeatX, repeatY, proportionalSlice, proportionalWidth,
                    topWidth, rightWidth, bottomWidth, leftWidth, offsets);
        }
    }

    /**
     * The image used to paint this border
     *
     * @defaultValue null
     */
    public Image getImage() {
        return image;
    }
    final private Image image;

    /**
     * If true then the center slice is painted as a additional background after
     * all background fills and background images. If false the center slice is
     * just ignored and never painted.
     *
     * @defaultValue false
     */
    public boolean isFillCenter() {
        return fillCenter;
    }
    final private boolean fillCenter;

    /**
     * The position of the left slice from the left hand side of the image. Size
     * units are image pixels or percentage of image width.
     * @see #proportionalSlice
     * @defaultValue 100%
     */
    public double getLeftSlice() {
        return leftSlice;
    }
    final private double leftSlice;

    /**
     * The position of the top slice from the top side of the image. Size
     * units are image pixels or percentage of image width.
     * @see #proportionalSlice
     * @defaultValue 100%
     */
    public double getTopSlice() {
        return topSlice;
    }
    final private double topSlice;

    /**
     * The position of the right slice from the right hand side of the image. Size
     * units are image pixels or percentage of image width.
     * @see #proportionalSlice
     * @defaultValue 100%
     */
    public double getRightSlice() {
        return rightSlice;
    }
    final private double rightSlice;

    /**
     * The position of the bottom slice from the bottom side of the image. Size
     * units are image pixels or percentage of image width.
     * @see #proportionalSlice
     * @defaultValue 100%
     */
    public double getBottomSlice() {
        return bottomSlice;
    }
    final private double bottomSlice;

    /**
     * Indicates whether the slice units are proportional or absolute.
     * If this flag is true, slice units are defined in a [0..1] space and
     * represent a percentage of the image width. If this flag is false,
     * then slice units are image pixels.
     * @default true
     */
    public boolean isProportionalSlice() {
        return proportionalSlice;
    }
    final private boolean proportionalSlice;

    /**
     * Should the image be repeated along the X axis. The value {@code NO_REPEAT}
     * has same meaning as the css spec value {@code stretch} meaning the
     * image is stretched to fill each slice. The effects the top, center and
     * bottom slices.
     *
     * @default Repeat.NO_REPEAT
     */
    public Repeat getRepeatX() {
        return repeatX;
    }
    final private Repeat repeatX;

    /**
     * Should the image be repeated along the Y axis.The value {@code NO_REPEAT}
     * has same meaning as the css spec value {@code stretch} meaning the
     * image is stretched to fill each slice. The effects the left, center and
     * right slices.
     *
     * @default Repeat.NO_REPEAT
     */
    public Repeat getRepeatY() {
        return repeatY;
    }
    final private Repeat repeatY;

    private BorderImage(Image image, boolean fillCenter, double topSlice,
            double rightSlice, double bottomSlice, double leftSlice,
            Repeat repeatX, Repeat repeatY,
            boolean proportionalSlice, boolean proportionalWidth,
            double topWidth, double rightWidth, double bottomWidth, double leftWidth,
            Insets offsets) {
        super(topWidth, rightWidth, bottomWidth, leftWidth, proportionalWidth, offsets);
        this.image = image;
        this.fillCenter = fillCenter;
        this.topSlice = topSlice;
        this.rightSlice = rightSlice;
        this.bottomSlice = bottomSlice;
        this.leftSlice = leftSlice;
        this.repeatX = repeatX;
        this.repeatY = repeatY;
        this.proportionalSlice = proportionalSlice;
    }

    final static public class BorderImageRepeat {

        final private Repeat repeatX;
        final private Repeat repeatY;

        public BorderImageRepeat(Repeat repeatX, Repeat repeatY) {
            this.repeatX = repeatX;
            this.repeatY = repeatY;
        }

        public Repeat getRepeatX() {
            return repeatX;
        }

        public Repeat getRepeatY() {
            return repeatY;
        }
    }

    /**
     * border-image-repeat  <repeat-style> [ , <repeat-style> ]*
     * where <repeat-style> = repeat-x | repeat-y | [repeat | space | round | stretch | no-repeat]{1,2}
     */
    final static public class RepeatConverter extends StyleConverter<ParsedValue<Repeat, Repeat>[][], BorderImageRepeat[]> {

        public static RepeatConverter getInstance() {
            return Holder.BORDER_IMAGE_REPEAT_CONVERTER;
        }

        private RepeatConverter() {
            super();
        }

        @Override
        public BorderImageRepeat[] convert(ParsedValue<ParsedValue<Repeat, Repeat>[][], BorderImageRepeat[]> value, Font font) {
            ParsedValue<Repeat, Repeat>[][] layers = value.getValue();
            BorderImageRepeat[] borderImageRepeats = new BorderImageRepeat[layers.length];
            for (int l = 0; l < layers.length; l++) {
                ParsedValue<Repeat,Repeat>[] repeats = layers[l];
                Repeat horizontal = repeats[0].getValue();
                Repeat vertical = repeats[1].getValue();
                borderImageRepeats[l] = new BorderImageRepeat(horizontal, vertical);
            }
            return borderImageRepeats;
        }

        @Override
        public String toString() {
            return "BorderImageRepeatConverter";
        }
    }

    final static public class BorderImageSlice {

        final private double top;
        final private double right;
        final private double bottom;
        final private double left;
        final private boolean proportional;
        final private boolean fill;

        public BorderImageSlice(double top, double right, double bottom, double left,
                boolean proportional, boolean fill) {
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.left = left;
            this.proportional = proportional;
            this.fill = fill;
        }

        public double getTop() {
            return top;
        }

        public double getRight() {
            return right;
        }

        public double getBottom() {
            return bottom;
        }

        public double getLeft() {
            return left;
        }

        public boolean isProportional() {
            return proportional;
        }

        public boolean isFill() {
            return fill;
        }
    }

    /**
     * [<size> | <size> <size> <size> <size>] <fill>? [ , [ <size> | <size> <size> <size> <size>] <fill>? ]*
     */
    final static public class SliceSequenceConverter extends StyleConverter<ParsedValue<ParsedValue[], BorderImageSlice>[], BorderImageSlice[]> {

        public static SliceSequenceConverter getInstance() {
            return Holder.BORDER_IMAGE_SLICE_SEQUENCE_CONVERTER;
        }

        @Override
        public BorderImageSlice[] convert(ParsedValue<ParsedValue<ParsedValue[], BorderImageSlice>[], BorderImageSlice[]> value, Font font) {
            // For 'border-image-slice: 10% fill, 20% 30%', the value arg will be
            // ParsedValue { values: [
            //     ParsedValue { values: [ ParsedValue {parsed: 10%}, ParsedValue {parsed: fill}] } ,
            //     ParsedValue { values: [ ParsedValue {parsed: 20%}, ParsedValue {parsed: 30%}] }
            // ]}
            //
            // For 'border-image-slice: 10% fill', the value arg will be
            // ParsedValue { values: [ ParsedValue {parsed: 10%}, ParsedValue {parsed: fill}] }
            //
            // For 'border-image-slice: 10%', the value arg will be
            // ParsedValue {parsed: 10%}
            //
            // where the sizes are actually Size objects.
            //
            // If the value arg contains multiple layers, unwind the nested
            // values by one level.
            ParsedValue<ParsedValue[], BorderImageSlice>[] layers = value.getValue();
            BorderImageSlice[] borderImageSlices = new BorderImageSlice[layers.length];
            for (int l = 0; l < layers.length; l++) {
                borderImageSlices[l] = SliceConverter.getInstance().convert(layers[l], font);
            }
            return borderImageSlices;
        }

        @Override
        public String toString() {
            return "BorderImageSliceSequenceConverter";
        }
    }

    final static public class SliceConverter extends StyleConverter<ParsedValue[], BorderImageSlice> {

        public static SliceConverter getInstance() {
            return Holder.BORDER_IMAGE_SLICE_CONVERTER;
        }

        private SliceConverter() {
            super();
        }

        @Override
        public BorderImageSlice convert(ParsedValue<ParsedValue[], BorderImageSlice> layer, Font font) {
            // Parser sends insets and boolean fill

            ParsedValue[] values = layer.getValue();

            // value[0] is ParsedValue<Value<?,Size>[],Insets>
            ParsedValue<?, Size>[] sizes = (ParsedValue<?, Size>[]) values[0].getValue();
            Size topSz = (Size) sizes[0].convert(font);
            Size rightSz = (Size) sizes[1].convert(font);
            Size bottomSz = (Size) sizes[2].convert(font);
            Size leftSz = (Size) sizes[3].convert(font);

            boolean proportional = false;
            if (topSz.getUnits() == rightSz.getUnits()
                    && topSz.getUnits() == bottomSz.getUnits()
                    && rightSz.getUnits() == leftSz.getUnits()) {
                proportional = topSz.getUnits() == SizeUnits.PERCENT;
            } else {
                System.err.println("border-image-slice size units do not match");
            }

            Double top = topSz.pixels(font);
            Double right = rightSz.pixels(font);
            Double bottom = bottomSz.pixels(font);
            Double left = leftSz.pixels(font);

            Boolean fill = (Boolean) values[1].getValue();

            return new BorderImageSlice(top, right, bottom, left, proportional, fill);
        }

        @Override
        public String toString() {
            return "BorderImageSliceType";
        }
    }
}
