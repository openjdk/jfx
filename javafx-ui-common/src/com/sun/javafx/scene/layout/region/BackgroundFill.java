/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.paint.Paint;

import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.converters.InsetsConverter;
import com.sun.javafx.css.converters.PaintConverter;
import java.util.ArrayList;
import javafx.beans.value.WritableValue;
import javafx.scene.Node;
import javafx.scene.paint.Color;


public class BackgroundFill {

     /**
      * Super-lazy instantiation pattern from Bill Pugh.
      * @treatAsPrivate implementation detail
      */
     private static class StyleableProperties {
         
         // 
         // These are sub-properties of Region.BACKGROUND_FILLS
         //
         private static final StyleableProperty<Node,Paint[]> BACKGROUND_COLOR =
            new StyleableProperty<Node,Paint[]>("-fx-background-color", 
                PaintConverter.SequenceConverter.getInstance(), 
                new Paint[] {Color.BLACK}) {

            @Override
            public boolean isSettable(Node node) {
                return false;
            }

            @Override
            public WritableValue<Paint[]> getWritableValue(Node node) {
                return null;
            }
        };
         
        private static final StyleableProperty<Node,Insets[]> BACKGROUND_RADIUS =
            new StyleableProperty<Node,Insets[]>("-fx-background-radius", 
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

        private static final StyleableProperty<Node,Insets[]> BACKGROUND_INSETS =
            new StyleableProperty<Node,Insets[]>("-fx-background-insets", 
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
                BACKGROUND_COLOR,
                BACKGROUND_RADIUS,
                BACKGROUND_INSETS
            );
            STYLEABLES = Collections.unmodifiableList(subProperties);
         }
    }

     /**
      * Super-lazy instantiation pattern from Bill Pugh. StyleablePropertyHolder is referenced
      * no earlier (and therefore loaded no earlier by the class loader) than
      * the moment that  impl_CSS_STYLEABLES() is called.
      * @treatAsPrivate implementation detail
      */
     public static List<StyleableProperty> impl_CSS_STYLEABLES() {
         return BackgroundFill.StyleableProperties.STYLEABLES;
     }

    final private Paint fill;
    public Paint getFill() {
        return fill;
    }

    /**
     * Defined the radius of the top left corner of the region this border is
     * being applied to. It only has effect if the region is a rectangular region.
     *
     * @defaultValue 0.0
     */
    final private double topLeftCornerRadius;
    public double getTopLeftCornerRadius() {
        return topLeftCornerRadius;
    }

    /**
     * Defined the radius of the top right corner of the region this border is
     * being applied to. It only has effect if the region is a rectangular region.
     *
     * @defaultValue 0.0
     */
    final private double topRightCornerRadius;
    public double getTopRightCornerRadius() {
        return topRightCornerRadius;
    }

    /**
     * Defined the radius of the bottom left corner of the region this border is
     * being applied to. It only has effect if the region is a rectangular region.
     *
     * @defaultValue 0.0
     */
    final private double bottomLeftCornerRadius;
    public double getBottomLeftCornerRadius() {
        return bottomLeftCornerRadius;
    }

    /**
     * Defined the radius of the bottom right corner of the region this border is
     * being applied to. It only has effect if the region is a rectangular region.
     *
     * @defaultValue 0.0
     */
    final private double bottomRightCornerRadius;
    public double getBottomRightCornerRadius() {
        return bottomRightCornerRadius;
    }

    /**
     * Offsets to use from the region bounds. Units are scene graph units.
     *
     * @defaultValue null
     */
    final private Insets offsets;
    public Insets getOffsets() {
        if (offsets == null) return Insets.EMPTY;
        return offsets;
    }

    /**
     * Since BackgroundFill is an immutable class we only need to compute the
     * hash code once
     */
    private int hash = 0;

    // Constructed only from BackgroundFillsConverter
    BackgroundFill(Paint fill, double topLeftCornerRadius,
            double topRightCornerRadius, double bottomRightCornerRadius,
            double bottomLeftCornerRadius, Insets offsets) {
        this.fill = fill;
        this.topLeftCornerRadius = topLeftCornerRadius;
        this.topRightCornerRadius = topRightCornerRadius;
        this.bottomRightCornerRadius = bottomRightCornerRadius;
        this.bottomLeftCornerRadius = bottomLeftCornerRadius;
        this.offsets = offsets;
    }

    @Override public String toString() {
        return "BackgroundFill [fill=" + fill + ", "
            +  "radii=" + topLeftCornerRadius + ", "
            +  topRightCornerRadius + ","
            +  bottomRightCornerRadius + ","
            +  bottomLeftCornerRadius + ", offets="
            +  offsets + "]";
    }

    /**
     * @inheritDoc
     */
    @Override public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof BackgroundFill) {
            BackgroundFill other = (BackgroundFill) obj;
            return fill.equals(other.fill)
              && topLeftCornerRadius == other.topLeftCornerRadius
              && topRightCornerRadius == other.topRightCornerRadius
              && bottomLeftCornerRadius == other.bottomLeftCornerRadius
              && bottomRightCornerRadius == other.bottomRightCornerRadius
              && (offsets == null ? other.offsets == null : other.offsets.equals(offsets));
        } else return false;
    }

    /**
     * @inheritDoc
     */
    @Override public int hashCode() {
        if (hash == 0) {
            long bits = 17L;
            bits = 37L * bits + (fill == null ? 0L : fill.hashCode());
            bits = 37L * bits + Double.doubleToLongBits(topLeftCornerRadius);
            bits = 37L * bits + Double.doubleToLongBits(topRightCornerRadius);
            bits = 37L * bits + Double.doubleToLongBits(bottomLeftCornerRadius);
            bits = 37L * bits + Double.doubleToLongBits(bottomRightCornerRadius);
            bits = 37L * bits + (offsets == null ? 0L : offsets.hashCode());
            hash = (int) (bits ^ (bits >> 32));
        }
        return hash;
    }
}
