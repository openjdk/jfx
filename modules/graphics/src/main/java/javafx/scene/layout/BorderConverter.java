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

package javafx.scene.layout;

import com.sun.javafx.css.StyleManager;
import com.sun.javafx.scene.layout.region.BorderImageSlices;
import com.sun.javafx.scene.layout.region.Margins;
import com.sun.javafx.scene.layout.region.RepeatStruct;
import java.util.Map;
import javafx.css.CssMetaData;
import javafx.css.ParsedValue;
import javafx.css.Styleable;
import javafx.css.StyleConverter;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/**
 */
class BorderConverter extends StyleConverter<ParsedValue[], Border> {

    private static final BorderConverter BORDER_IMAGE_CONVERTER =
            new BorderConverter();

    public static BorderConverter getInstance() {
        return BORDER_IMAGE_CONVERTER;
    }

    // Disallow instantiation
    private BorderConverter() { }

    @Override
    public Border convert(Map<CssMetaData<? extends Styleable, ?>, Object> convertedValues) {
        final Paint[][] strokeFills = (Paint[][])convertedValues.get(Border.BORDER_COLOR);
        final BorderStrokeStyle[][] strokeStyles = (BorderStrokeStyle[][]) convertedValues.get(Border.BORDER_STYLE);
        final String[] imageUrls = (String[]) convertedValues.get(Border.BORDER_IMAGE_SOURCE);
        //
        // In W3C CSS, border colors and border images are not layered. In javafx, they are. We've taken the position
        // that there is one layer per -fx-border-color or -fx-border-image-source. This is consistent with
        // background-image (see http://www.w3.org/TR/css3-background/#layering). But, in a browser, you can have a
        // border-style with no corresponding border-color - the border-color just defaults to 'currentColor' (which
        // we don't have so we'll call it 'black' for the time being). So the number of stroke-border layers is now
        // determined by the max of strokeFills.length and strokeStyles.length. If there are more styles than fills,
        // the remaining styles will use the last fill value (this is consistent with handling of the other stroke
        // border properties). If there aren't any fills at all, then the fill is 'currentColor' (i.e., black) just
        // as the default stroke is solid.
        //
        final boolean hasStrokes = (strokeFills != null && strokeFills.length > 0) || (strokeStyles != null && strokeStyles.length > 0);
        final boolean hasImages = imageUrls != null && imageUrls.length > 0;

        // If there are neither background fills nor images, then there is nothing for us to construct.
        if (!hasStrokes && !hasImages) return null;

        BorderStroke[] borderStrokes = null;
        if (hasStrokes) {

            final int lastStrokeFill = strokeFills != null ? strokeFills.length - 1 : -1;
            final int lastStrokeStyle = strokeStyles != null ? strokeStyles.length - 1 : -1;
            final int nLayers = (lastStrokeFill >= lastStrokeStyle ? lastStrokeFill : lastStrokeStyle) + 1;

            Object tmp = convertedValues.get(Border.BORDER_WIDTH);
            final Margins[] borderWidths = tmp == null ? new Margins[0] : (Margins[]) tmp;
            final int lastMarginIndex = borderWidths.length - 1;

            tmp = convertedValues.get(Border.BORDER_RADIUS);
            final CornerRadii[] borderRadii = tmp == null ? new CornerRadii[0] : (CornerRadii[]) tmp;
            final int lastRadiusIndex = borderRadii.length - 1;

            tmp = convertedValues.get(Border.BORDER_INSETS);
            final Insets[] borderInsets = tmp == null ? new Insets[0] : (Insets[]) tmp;
            final int lastInsetsIndex = borderInsets.length - 1;

            for (int i=0; i<nLayers; i++) {

                BorderStrokeStyle[] styles;
                // if there are no strokeStyles, then lastStrokeStyle will be < 0
                if (lastStrokeStyle < 0) {
                    styles = new BorderStrokeStyle[4];
                    styles[0] = styles[1] = styles[2] = styles[3] = BorderStrokeStyle.SOLID;
                } else {
                    styles = strokeStyles[i <= lastStrokeStyle ? i : lastStrokeStyle];
                }

                if (styles[0] == BorderStrokeStyle.NONE &&
                        styles[1] == BorderStrokeStyle.NONE &&
                        styles[2] == BorderStrokeStyle.NONE &&
                        styles[3] == BorderStrokeStyle.NONE) continue;

                Paint[] strokes;
                // if there are no strokeFills, then lastStrokeFill will be < 0
                if (lastStrokeFill < 0) {
                    strokes = new Paint[4];
                    // TODO: should be 'currentColor'
                    strokes[0] = strokes[1] = strokes[2] = strokes[3] = Color.BLACK;
                }  else {
                    strokes = strokeFills[i <= lastStrokeFill ? i : lastStrokeFill];
                }

                if (borderStrokes == null) borderStrokes = new BorderStroke[nLayers];

                final Margins margins = borderWidths.length == 0 ?
                        null :
                        borderWidths[i <= lastMarginIndex ? i : lastMarginIndex];
                final CornerRadii radii = borderRadii.length == 0 ?
                        CornerRadii.EMPTY :
                        borderRadii[i <= lastRadiusIndex ? i : lastRadiusIndex];
                final Insets insets = borderInsets.length == 0 ?
                        null :
                        borderInsets[i <= lastInsetsIndex ? i : lastInsetsIndex];

                borderStrokes[i] = new BorderStroke(
                        strokes[0], strokes[1], strokes[2], strokes[3],
                        styles[0], styles[1], styles[2], styles[3],
                        radii,
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

                final BorderImageSlices slice = slices.length > 0 ? slices[i <= lastSlicesIndex ? i : lastSlicesIndex] : BorderImageSlices.DEFAULT;
                final Insets inset = insets.length > 0 ? insets[i <= lastInsetsIndex ? i : lastInsetsIndex] : Insets.EMPTY;
                final BorderWidths width = widths.length > 0 ? widths[i <= lastWidthsIndex ? i : lastWidthsIndex] : BorderWidths.DEFAULT;
                final Image img = StyleManager.getInstance().getCachedImage(imageUrls[i]);
                borderImages[i] = new BorderImage(img, width, inset, slice.widths, slice.filled, repeatX, repeatY);
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
