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

import javafx.scene.image.Image;
import javafx.scene.text.Font;

import com.sun.javafx.css.Size;
import com.sun.javafx.css.SizeUnits;
import com.sun.javafx.css.StyleConverter;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.ParsedValue;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.URLConverter;
import java.util.ArrayList;
import javafx.beans.value.WritableValue;
import javafx.scene.Node;

/**
 * A background image that can be applied to a Region. It is based in the CSS 3
 * specification http://www.w3.org/TR/css3-background/
 *
 */
public class BackgroundImage {

    private static class Holder {

        private static final BackgroundRepeatConverter BACKGROUND_REPEAT_CONVERTER =
                new BackgroundRepeatConverter();

        private static final BackgroundPositionConverter BACKGROUND_POSITION_CONVERTER =
                new BackgroundPositionConverter();

        private static final LayeredBackgroundPositionConverter LAYERED_BACKGROUND_POSITION_CONVERTER =
                new LayeredBackgroundPositionConverter();


        private static final BackgroundSizeConverter BACKGROUND_SIZE_CONVERTER =
                new BackgroundSizeConverter();

        private static final LayeredBackgroundSizeConverter LAYERED_BACKGROUND_SIZE_CONVERTER =
                new LayeredBackgroundSizeConverter();
    }
    /**
     * Super-lazy instantiation pattern from Bill Pugh.
     * @treatasprivate implementation detail
     */
    private static class StyleableProperties {
        
        private static final StyleableProperty<Node,Image[]> BACKGROUND_IMAGE =
            new StyleableProperty<Node,Image[]>("-fx-background-image",
                URLConverter.SequenceConverter.getInstance()) {

            @Override
            public boolean isSettable(Node node) {
                return false;
            }

            @Override
            public WritableValue<Image[]> getWritableValue(Node node) {
                return null;
            }
        };
        
        private static final StyleableProperty<Node,BackgroundRepeat[]> BACKGROUND_REPEAT =
            new StyleableProperty<Node,BackgroundRepeat[]>("-fx-background-repeat", 
                BackgroundRepeatConverter.getInstance(),
                new BackgroundRepeat[] { new BackgroundRepeat(Repeat.REPEAT, Repeat.REPEAT) }){

            @Override
            public boolean isSettable(Node node) {
                return false;
            }

            @Override
            public WritableValue<BackgroundRepeat[]> getWritableValue(Node node) {
                return null;
            }
        };                
                
        private static final StyleableProperty<Node,BackgroundPosition[]> BACKGROUND_POSITION =
            new StyleableProperty<Node,BackgroundPosition[]>("-fx-background-position", 
                LayeredBackgroundPositionConverter.getInstance(),
                new BackgroundPosition[] { new BackgroundPosition() }) {
                    

            @Override
            public boolean isSettable(Node node) {
                return false;
            }

            @Override
            public WritableValue<BackgroundPosition[]> getWritableValue(Node node) {
                return null;
            }
        };
                
        private static final StyleableProperty<Node,BackgroundSize[]> BACKGROUND_SIZE =
            new StyleableProperty<Node,BackgroundSize[]>("-fx-background-size", 
                LayeredBackgroundSizeConverter.getInstance(), 
                new BackgroundSize[] { new BackgroundSize() } ) {

            @Override
            public boolean isSettable(Node node) {
                return false;
            }

            @Override
            public WritableValue<BackgroundSize[]> getWritableValue(Node node) {
                return null;
            }
        };

        private static final List<StyleableProperty> STYLEABLES;

        static {
             final List<StyleableProperty> subProperties = 
                 new ArrayList<StyleableProperty>();
            Collections.addAll(subProperties,
                BACKGROUND_IMAGE,
                BACKGROUND_REPEAT,
                BACKGROUND_POSITION,
                BACKGROUND_SIZE
            );
            STYLEABLES = Collections.unmodifiableList(subProperties);
        }
    }

    /**
     * Super-lazy instantiation pattern from Bill Pugh.
     * @treatasprivate implementation detail
     */
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return StyleableProperties.STYLEABLES;
    }

    public static class Builder {

        private Image image;
        private Repeat repeatX = Repeat.REPEAT;
        private Repeat repeatY = Repeat.REPEAT;
        private double top;
        private double right;
        private double bottom;
        private double left;
        private double width;
        private double height;
        private boolean proportionalHPos = true;
        private boolean proportionalVPos = true;
        private boolean proportionalWidth = true;
        private boolean proportionalHeight = true;
        private boolean contain = false;
        private boolean cover = false;

        public Builder setImage(Image i) {
            image = i;
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

        public Builder setTop(double f) {
            top = f;
            return this;
        }

        public Builder setRight(double f) {
            right = f;
            return this;
        }

        public Builder setBottom(double f) {
            bottom = f;
            return this;
        }

        public Builder setLeft(double f) {
            left = f;
            return this;
        }

        public Builder setWidth(double f) {
            width = f;
            return this;
        }

        public Builder setHeight(double f) {
            height = f;
            return this;
        }

        public Builder setProportionalHPos(boolean b) {
            proportionalHPos = b;
            return this;
        }

        public Builder setProportionalVPos(boolean b) {
            proportionalVPos = b;
            return this;
        }

        public Builder setProportionalWidth(boolean b) {
            proportionalWidth = b;
            return this;
        }

        public Builder setProportionalHeight(boolean b) {
            proportionalHeight = b;
            return this;
        }

        public Builder setContain(boolean b) {
            contain = b;
            return this;
        }

        public Builder setCover(boolean b) {
            cover = b;
            return this;
        }

        public BackgroundImage build() {
            return new BackgroundImage(image, repeatX, repeatY,
                    top, right, bottom, left, width, height,
                    proportionalHPos, proportionalVPos,
                    proportionalWidth, proportionalHeight,
                    contain, cover);
        }
    }

    /**
     * The image for this background image
     *
     * @default null
     * @css background-image
     */
    public Image getImage() {
        return image;
    }
    final private Image image;

    /**
     * Should the image be repeated along the X axis
     *
     * @default Repeat.REPEAT
     * @css background-repeat
     */
    public Repeat getRepeatX() {
        return repeatX;
    }
    final private Repeat repeatX;

    /**
     * Should the image be repeated along the Y axis
     *
     * @default Repeat.REPEAT
     * @css background-repeat
     */
    public Repeat getRepeatY() {
        return repeatY;
    }
    final private Repeat repeatY;

    /**
     * The position of the left of the image from left of the Region.
     *
     * @default 0
     * @css background-position (Percentages are a percentage of the
     *                          {@code (region width - image width)}
     *                          this means 50% is centered and 100%
     *                          means right align image)
     */
    public double getLeft() {
        return left;
    }
    final private double left;

    /**
     * The position of the top of the image from top of the Region.
     *
     * @default 0
     * @css background-position (Percentages are a percentage of the
     *                          {@code (region height - image height)}
     *                          this means 50% is centered and 100%
     *                          means bottom align image)
     */
    public double getTop() {
        return top;
    }
    final private double top;

    /**
     * The position of the right of the image from right of the Region.
     * If this and {@code left} are !=0 then {@code left} has
     * precedence
     *
     * @default 0
     * @css background-position (Percentages are a percentage of the
     *                          {@code (region width - image width)}
     *                          this means 50% is centered and 100%
     *                          means right align image)
     */
    public double getRight() {
        return right;
    }
    final private double right;

    /**
     * The position of the bottom of the image from bottom of the Region.
     * If this and {@code top} are !=0 then {@code top} has
     * precedence
     *
     * @default 0
     * @css background-position (Percentages are a percentage of the
     *                          {@code (region height - image height)}
     *                          this means 50% is centered and 100%
     *                          means bottom align image)
     */
    public double getBottom() {
        return bottom;
    }
    final private double bottom;

    /**
     * Indicates whether the horizontal position units are proportional or absolute.
     * If this flag is true, position units are defined in a [0..1] space and
     * refer to the size of background positioning area minus the size of
     * background image. If this flag is false, then position units are pixels.
     * @default true
     */
    public boolean isProportionalHPos() {
        return proportionalHPos;
    }
    final private boolean proportionalHPos;

    /**
     * Indicates whether the vertical position units are proportional or absolute.
     * If this flag is true, position units are defined in a [0..1] space and
     * refer to the size of background positioning area minus the size of
     * background image. If this flag is false, then position units are pixels.
     * @default true
     */
    public boolean isProportionalVPos() {
        return proportionalVPos;
    }
    final private boolean proportionalVPos;

    /**
     * The width of the the image. If <=0 then width is auto sized. If both
     * width and height are auto then image is used at its intrinsic(loaded)
     * size. If height is specified and this is auto then the width is
     * calculated based on maintaining the aspect ratio while matching the
     * chosen height.
     *
     * @default 0 (auto)
     * @css background-size (Percentages are a percentage of the Region width)
     */
    public double getWidth() {
        return width;
    }
    final private double width;

    /**
     * The height of the image. If <= 0 then width is auto sized. If both
     * width and height are auto then image is used at its intrinsic(loaded)
     * size. If width is specified and this is auto then the height is
     * calculated based on maintaining the aspect ratio while matching the
     * chosen width.
     *
     * @default 0 (auto)
     * @css background-size (Percentages are a percentage of the Region height)
     */
    public double getHeight() {
        return height;
    }
    final private double height;

    /**
     * Indicates whether the size units are proportional or absolute.
     * If this flag is true, size units are defined in a [0..1] space and
     * represent a percentage of the region size. If this flag is false,
     * then size units are pixels.
     * @default true
     */
    public boolean isProportionalWidth() {
        return proportionalWidth;
    }
    final private boolean proportionalWidth;

    public boolean isProportionalHeight() {
        return proportionalHeight;
    }
    final private boolean proportionalHeight;

    /**
     * If true the image is scaled to be completely contained within the Region
     * with its aspect ratio maintained.
     *
     * This has precedence over width and height.
     *
     * @default false
     * @css background-size
     */
    public boolean isContain() {
        return contain;
    }
    final private boolean contain;

    /**
     * If true the image is scaled to be the smallest size that completely covers
     * the Region with its aspect ratio maintained.
     *
     * This has precedence over width, height and contain.
     *
     * @default false
     * @css background-size
     */
    public boolean isCover() {
        return cover;
    }
    final private boolean cover;

    private BackgroundImage(Image image, Repeat repeatX, Repeat repeatY,
            double top, double right, double bottom, double left, double width, double height,
            boolean proportionalHPos, boolean proportionalVPos,
            boolean proportionalWidth, boolean proportionalHeight,
            boolean contain, boolean cover) {
        this.image = image;
        this.repeatX = repeatX;
        this.repeatY = repeatY;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.width = width;
        this.height = height;
        this.proportionalHPos = proportionalHPos;
        this.proportionalVPos = proportionalVPos;
        this.proportionalWidth = proportionalWidth;
        this.proportionalHeight = proportionalHeight;
        this.contain = contain;
        this.cover = cover;
    }

    /**
     * background-repeat    <repeat-style> [ , <repeat-style> ]*
     * where <repeat-style> = repeat-x | repeat-y | [repeat | space | round | no-repeat]{1,2}
     */
    public static final class BackgroundRepeatConverter extends StyleConverter<ParsedValue<Repeat, Repeat>[][], BackgroundRepeat[]> {

        public static BackgroundRepeatConverter getInstance() {
            return Holder.BACKGROUND_REPEAT_CONVERTER;
        }

        private BackgroundRepeatConverter() {
            super();
        }

        @Override
        public BackgroundRepeat[] convert(ParsedValue<ParsedValue<Repeat, Repeat>[][], BackgroundRepeat[]> value, Font font) {
            ParsedValue<Repeat, Repeat>[][] layers = value.getValue();
            BackgroundRepeat[] backgroundRepeat = new BackgroundRepeat[layers.length];
            for (int l = 0; l < layers.length; l++) {
                ParsedValue<Repeat,Repeat>[] repeats = layers[l];
                Repeat horizontal = repeats[0].getValue();
                Repeat vertical = repeats[1].getValue();
                backgroundRepeat[l] = new BackgroundRepeat(horizontal,vertical);
            }
            return backgroundRepeat;
        }

        @Override
        public String toString() {
            return "BackgroundRepeatType";
        }
    }

    // TODO: same as ImageBorder BorderImageRepeat
    final static public class BackgroundRepeat {

        final private Repeat repeatX;
        final private Repeat repeatY;

        public BackgroundRepeat(Repeat repeatX, Repeat repeatY) {
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

// container for background-position
    // TOOD: eliminate this
    final static public class BackgroundPosition {

        public static final Size ZERO_PERCENT = new Size(0, SizeUnits.PERCENT);
        public static final Size FIFTY_PERCENT = new Size(50, SizeUnits.PERCENT);
        public static final Size ONE_HUNDRED_PERCENT = new Size(100, SizeUnits.PERCENT);
        final private double top;
        final private double right;
        final private double bottom;
        final private double left;
        private boolean proportionalHPos;
        private boolean proportionalVPos;

        private BackgroundPosition() {
            this(0.0f, 0.0f, 0.0f, 0.0f, false, false);
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

        public boolean isProportionalHPos() {
            return proportionalHPos;
        }

        public boolean isProportionalVPos() {
            return proportionalVPos;
        }

        // Public for testing
        public BackgroundPosition(double top, double right, double bottom, double left, 
                                  boolean proportionalHPos, boolean proportionalVPos) {
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.left = left;
            this.proportionalHPos = proportionalHPos;
            this.proportionalVPos = proportionalVPos;
        }
    }

    /**
     * background-position: <bg-position>
     * where <bg-position> = [
     *   [ [ <size> | left | center | right ] [ <size> | top | center | bottom ]? ]
     *   | [ [ center | [ left | right ] <size>? ] || [ center | [ top | bottom ] <size>? ]
     * ]
     * @see <a href="http://www.w3.org/TR/css3-background/#the-background-position">background-position</a>
     */
    public static final class LayeredBackgroundPositionConverter extends StyleConverter<ParsedValue<ParsedValue<?, Size>[], BackgroundPosition>[], BackgroundPosition[]> {

        public static LayeredBackgroundPositionConverter getInstance() {
            return Holder.LAYERED_BACKGROUND_POSITION_CONVERTER;
        }

        private LayeredBackgroundPositionConverter() {
            super();
        }

        @Override
        public BackgroundPosition[] convert(ParsedValue<ParsedValue<ParsedValue<?, Size>[], BackgroundPosition>[], BackgroundPosition[]> value, Font font) {
            ParsedValue<ParsedValue<?, Size>[], BackgroundPosition>[] layers = value.getValue();
            BackgroundPosition[] positions = new BackgroundPosition[layers.length];
            for (int l = 0; l < layers.length; l++) {
                positions[l] = layers[l].convert(font);
            }
            return positions;
        }

        @Override
        public String toString() {
            return "LayeredBackgroundPositionConverter";
        }
    }

    public static final class BackgroundPositionConverter extends StyleConverter<ParsedValue<?, Size>[], BackgroundPosition> {

        public static BackgroundPositionConverter getInstance() {
            return Holder.BACKGROUND_POSITION_CONVERTER;
        }


        private BackgroundPositionConverter() {
            super();
        }

        @Override
        public BackgroundPosition convert(ParsedValue<ParsedValue<?, Size>[], BackgroundPosition> value, Font font) {

            ParsedValue<?, Size>[] positions = value.getValue();

            // The parser gives us 4 values, none of them null
            Size top = positions[0].convert(font);

            Size right = positions[1].convert(font);

            Size bottom = positions[2].convert(font);

            Size left = positions[3].convert(font);

            boolean horizontalEdgeProportional =
                    (bottom.getValue() > 0 && bottom.getUnits() == SizeUnits.PERCENT)
                    || top.getUnits() == SizeUnits.PERCENT;

            // either left or right will be set, not both
            boolean verticalEdgeProportional =
                    (right.getValue() > 0 && right.getUnits() == SizeUnits.PERCENT)
                    || left.getUnits() == SizeUnits.PERCENT;

            double t = (top != null) ? top.pixels(font) : 0.0;

            double r = (right != null) ? right.pixels(font) : 0.0;

            double b = (bottom != null) ? bottom.pixels(font) : 0.0;

            double l = (left != null) ? left.pixels(font) : 0.0;

            return new BackgroundPosition(t, r, b, l,
                    verticalEdgeProportional, horizontalEdgeProportional);
        }

        @Override
        public String toString() {
            return "BackgroundPositionConverter";
        }
    }

    final static public class BackgroundSize {

        public static final BackgroundSize AUTO_SIZE =
                new BackgroundSize(0.0f, 0.0f, true, true, false, false);
        public static final BackgroundSize COVER =
                new BackgroundSize(0.0f, 0.0f, true, true, true, false);
        public static final BackgroundSize CONTAIN =
                new BackgroundSize(0.0f, 0.0f, true, true, false, true);
        final private boolean proportionalWidth;
        final private boolean proportionalHeight;
        final private double width;
        final private double height;
        final private boolean cover;
        final private boolean contain;

        public BackgroundSize(double width, double height,
                boolean proportionalWidth, boolean proportionalHeight,
                boolean cover, boolean contain) {
            this.width = width;
            this.height = height;
            this.proportionalWidth = proportionalWidth;
            this.proportionalHeight = proportionalHeight;
            this.contain = contain;
            this.cover = cover;
        }

        private BackgroundSize() {
            this(0.0f, 0.0f, false, false, false, false);
        }

        public boolean isProportionalWidth() {
            return proportionalWidth;
        }

        public boolean isProportionalHeight() {
            return proportionalHeight;
        }

        public double getWidth() {
            return width;
        }

        public double getHeight() {
            return height;
        }

        public boolean isCover() {
            return cover;
        }

        public boolean isContain() {
            return contain;
        }
    }

    /**
     * background-size      <bg-size> [ , <bg-size> ]*
     * <bg-size> = [ <size> | auto ]{1,2} | cover | contain
     * @see <a href="http://www.w3.org/TR/css3-background/#the-background-size">background-size</a>
     */
    final public static class LayeredBackgroundSizeConverter extends StyleConverter<ParsedValue<ParsedValue[], BackgroundSize>[], BackgroundSize[]> {

        public static LayeredBackgroundSizeConverter getInstance() {
            return Holder.LAYERED_BACKGROUND_SIZE_CONVERTER;
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

    final public static class BackgroundSizeConverter extends StyleConverter<ParsedValue[], BackgroundSize> {

        public static BackgroundSizeConverter getInstance() {
            return Holder.BACKGROUND_SIZE_CONVERTER;
        }

        private BackgroundSizeConverter() {
            super();
        }

        @Override
        public BackgroundSize convert(ParsedValue<ParsedValue[], BackgroundSize> value, Font font) {
            ParsedValue[] values = value.getValue();

            Size wSize = (values[0] != null)
                    ? ((ParsedValue<?, Size>) values[0]).convert(font) : null;
            Size hSize = (values[1] != null)
                    ? ((ParsedValue<?, Size>) values[1]).convert(font) : null;

            boolean proportionalWidth = false;
            boolean proportionalHeight = false;

            if (wSize != null) {
                proportionalWidth = wSize.getUnits() == SizeUnits.PERCENT;
            }
            if (hSize != null) {
                // wSize will be null if wSize is AUTO                
                proportionalHeight = hSize.getUnits() == SizeUnits.PERCENT;
            }

            double w = (wSize != null) ? wSize.pixels(font) : 0.0;
            double h = (hSize != null) ? hSize.pixels(font) : 0.0;

            boolean cover = (values[2] != null)
                    ? BooleanConverter.getInstance().convert(values[2], font) : false;

            boolean contain = (values[3] != null)
                    ? BooleanConverter.getInstance().convert(values[3], font) : false;

            return new BackgroundSize(w, h, proportionalWidth, proportionalHeight, cover, contain);
        }

        @Override
        public String toString() {
            return "BackgroundSizeConverter";
        }
    }
}
