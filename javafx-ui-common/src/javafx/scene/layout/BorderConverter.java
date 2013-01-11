/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javafx.scene.layout;

import java.util.Map;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.paint.Paint;
import javafx.css.ParsedValue;
import com.sun.javafx.css.StyleConverterImpl;
import javafx.css.CssMetaData;
import com.sun.javafx.scene.layout.region.BorderImageSlices;
import com.sun.javafx.scene.layout.region.Margins;
import com.sun.javafx.scene.layout.region.RepeatStruct;

/**
 */
class BorderConverter extends StyleConverterImpl<ParsedValue[], Border> {

    private static final BorderConverter BORDER_IMAGE_CONVERTER =
            new BorderConverter();

    public static BorderConverter getInstance() {
        return BORDER_IMAGE_CONVERTER;
    }

    // Disallow instantiation
    private BorderConverter() { }

    @Override
    public Border convert(Map<CssMetaData, Object> convertedValues) {
        final Paint[][] strokeFills = (Paint[][])convertedValues.get(Border.BORDER_COLOR);
        final String[] imageUrls = (String[]) convertedValues.get(Border.BORDER_IMAGE_SOURCE);
        final boolean hasStrokes = strokeFills != null && strokeFills.length > 0;
        final boolean hasImages = imageUrls != null && imageUrls.length > 0;

        // If there are neither background fills nor images, then there is nothing for us to construct.
        if (!hasStrokes && !hasImages) return null;

        BorderStroke[] borderStrokes = null;
        if (hasStrokes) {
            Object tmp = convertedValues.get(Border.BORDER_STYLE);
            final BorderStrokeStyle[][] strokeStyles = tmp == null ? new BorderStrokeStyle[0][0] : (BorderStrokeStyle[][]) tmp;
            final int lastStrokeStyle = strokeStyles.length - 1;

            tmp = convertedValues.get(Border.BORDER_WIDTH);
            final Margins[] borderWidths = tmp == null ? new Margins[0] : (Margins[]) tmp;
            final int lastMarginIndex = borderWidths.length - 1;

            tmp = convertedValues.get(Border.BORDER_RADIUS);
            final Margins[] borderRadii = tmp == null ? new Margins[0] : (Margins[]) tmp;
            final int lastRadiusIndex = borderRadii.length - 1;

            tmp = convertedValues.get(Border.BORDER_INSETS);
            final Insets[] borderInsets = tmp == null ? new Insets[0] : (Insets[]) tmp;
            final int lastInsetsIndex = borderInsets.length - 1;

            for (int i=0; i<strokeFills.length; i++) {
                if (strokeFills[i] == null) continue;

                BorderStrokeStyle[] styles;
                if (strokeStyles.length == 0) {
                    styles = new BorderStrokeStyle[4];
                    styles[0] = styles[1] = styles[2] = styles[3] = BorderStrokeStyle.SOLID;
                } else {
                    styles = strokeStyles[i <= lastStrokeStyle ? i : lastStrokeStyle];
                }

                if (styles[0] == BorderStrokeStyle.NONE &&
                        styles[1] == BorderStrokeStyle.NONE &&
                        styles[2] == BorderStrokeStyle.NONE &&
                        styles[3] == BorderStrokeStyle.NONE) continue;

                if (borderStrokes == null) borderStrokes = new BorderStroke[strokeFills.length];
                final Paint[] strokes = strokeFills[i];
                final Margins margins = borderWidths.length == 0 ?
                        null :
                        borderWidths[i <= lastMarginIndex ? i : lastMarginIndex];
                final Margins radii = borderRadii.length == 0 ?
                        null :
                        borderRadii[i <= lastRadiusIndex ? i : lastRadiusIndex];
                final Insets insets = borderInsets.length == 0 ?
                        null :
                        borderInsets[i <= lastInsetsIndex ? i : lastInsetsIndex];

                borderStrokes[i] = new BorderStroke(
                        strokes[0], strokes[1], strokes[2], strokes[3],
                        styles[0], styles[1], styles[2], styles[3],
                        radii == null ?
                                CornerRadii.EMPTY :
                                new CornerRadii(
                                    radii.getTop(), radii.getRight(), radii.getBottom(), radii.getLeft(),
                                    radii.isProportional()),
                        margins == null ?
                                BorderStroke.DEFAULT_WIDTHS :
                                new BorderWidths(margins.getTop(), margins.getRight(), margins.getBottom(), margins.getLeft()),
                        insets);
            }
        }

        BorderImage[] borderImages = null;
        if (hasImages) {
            borderImages = new BorderImage[imageUrls.length];
            Object tmp = convertedValues.get(Border.BORDER_IMAGE_REPEAT);
            final RepeatStruct[] repeats = tmp == null ? new RepeatStruct[0] : (RepeatStruct[]) tmp;
            final int lastRepeatIndex = repeats.length - 1;

            tmp = convertedValues.get(Border.BORDER_IMAGE_SLICE);
            final BorderImageSlices[] slices = tmp == null ? new BorderImageSlices[0] : (BorderImageSlices[]) tmp;
            final int lastSlicesIndex = slices.length - 1;

            tmp = convertedValues.get(Border.BORDER_IMAGE_WIDTH);
            final BorderWidths[] widths = tmp == null ? new BorderWidths[0] : (BorderWidths[]) tmp;
            final int lastWidthsIndex = widths.length - 1;

            tmp = convertedValues.get(Border.BORDER_IMAGE_INSETS);
            final Insets[] insets = tmp == null ? new Insets[0] : (Insets[]) tmp;
            final int lastInsetsIndex = insets.length - 1;

            for (int i=0; i<imageUrls.length; i++) {
                if (imageUrls[i] == null) continue;
                BorderRepeat repeatX = BorderRepeat.STRETCH, repeatY = BorderRepeat.STRETCH;
                if (repeats.length > 0) {
                    final RepeatStruct repeat = repeats[i <= lastRepeatIndex ? i : lastRepeatIndex];
                    switch (repeat.repeatX) {
                        case SPACE: repeatX = BorderRepeat.SPACE; break;
                        case ROUND: repeatX = BorderRepeat.ROUND; break;
                        case REPEAT: repeatX = BorderRepeat.REPEAT; break;
                        case NO_REPEAT: repeatX = BorderRepeat.STRETCH; break;
                    }
                    switch (repeat.repeatY) {
                        case SPACE: repeatY = BorderRepeat.SPACE; break;
                        case ROUND: repeatY = BorderRepeat.ROUND; break;
                        case REPEAT: repeatY = BorderRepeat.REPEAT; break;
                        case NO_REPEAT: repeatY = BorderRepeat.STRETCH; break;
                    }
                }

                final BorderImageSlices slice = slices.length > 0 ? slices[i <= lastSlicesIndex ? i : lastSlicesIndex] : BorderImageSlices.EMPTY;
                final Insets inset = insets.length > 0 ? insets[i <= lastInsetsIndex ? i : lastInsetsIndex] : Insets.EMPTY;
                final BorderWidths width = widths.length > 0 ? widths[i <= lastWidthsIndex ? i : lastWidthsIndex] : BorderWidths.DEFAULT;
                borderImages[i] = new BorderImage(new Image(imageUrls[i]), width, inset, slice.widths, slice.filled, repeatX, repeatY);
            }
        }

        return borderStrokes == null && borderImages == null ? null : new Border(borderStrokes, borderImages);
    }

    /**
     * @inheritDoc
     */
    @Override public String toString() {
        return "BorderConverter";
    }
}
