/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.css.SubPropertyConverter;
import com.sun.javafx.scene.layout.region.RepeatStruct;
import java.util.List;
import java.util.Map;
import javafx.css.CssMetaData;
import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.paint.Paint;

/**
 * Converts the CSS for -fx-background items into a Background.
 */
class BackgroundConverter extends StyleConverter<ParsedValue[], Background>
                          implements SubPropertyConverter<Background> {

    static final StyleConverter<ParsedValue[], Background> INSTANCE = new BackgroundConverter();

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code convertedValues} is {@code null}
     */
    @Override
    public Background convert(Map<CssMetaData<? extends Styleable, ?>,Object> convertedValues) {
        final Paint[] fills = (Paint[]) convertedValues.get(Background.BACKGROUND_COLOR);
        final Object[] images = (Object[]) convertedValues.get(Background.BACKGROUND_IMAGE);
        final boolean hasFills = fills != null && fills.length > 0;
        final boolean hasImages = images != null && images.length > 0;

        // If there are neither background fills nor images, then there is nothing for us to construct.
        if (!hasFills && !hasImages) return null;

        // Iterate over all of the fills, and create BackgroundFill objects for each.
        BackgroundFill[] backgroundFills = null;
        if (hasFills) {
            backgroundFills = new BackgroundFill[fills.length];

            Object tmp = convertedValues.get(Background.BACKGROUND_INSETS);
            final Insets[] insets = tmp == null ? new Insets[0] : (Insets[]) tmp;

            tmp = convertedValues.get(Background.BACKGROUND_RADIUS);
            final CornerRadii[] radii = tmp == null ? new CornerRadii[0] : (CornerRadii[]) tmp;

            final int lastInsetsIndex = insets.length - 1;
            final int lastRadiiIndex = radii.length - 1;
            for (int i=0; i<fills.length; i++) {
                Insets in = insets.length > 0 ? insets[i <= lastInsetsIndex ? i : lastInsetsIndex] : Insets.EMPTY;
                CornerRadii ra = radii.length > 0 ? radii[i <= lastRadiiIndex ? i : lastRadiiIndex] : CornerRadii.EMPTY;
                backgroundFills[i] = new BackgroundFill(fills[i], ra, in);
            }
        }

        // Iterate over all of the image, and create BackgroundImage objects for each.
        BackgroundImage[] backgroundImages = null;
        if (hasImages) {
            // TODO convert image urls into image objects!
            backgroundImages = new BackgroundImage[images.length];

            Object tmp = convertedValues.get(Background.BACKGROUND_REPEAT);
            final RepeatStruct[] repeats = tmp == null ? new RepeatStruct[0] : (RepeatStruct[]) tmp;

            tmp = convertedValues.get(Background.BACKGROUND_POSITION);
            final BackgroundPosition[] positions = tmp == null ? new BackgroundPosition[0] : (BackgroundPosition[]) tmp;

            tmp = convertedValues.get(Background.BACKGROUND_SIZE);
            final BackgroundSize[] sizes = tmp == null ? new BackgroundSize[0] : (BackgroundSize[]) tmp;

            final int lastRepeatIndex = repeats.length - 1;
            final int lastPositionIndex = positions.length - 1;
            final int lastSizeIndex = sizes.length - 1;
            for (int i = 0; i < images.length; i++) {
                // JDK-8116322: skip background and border images whose image url is null
                if (images[i] == null) continue;

                final Image image;

                // The images are either URLs (when they come from CSS) or the actual Image
                // instances (when they come from the convertBack operation).
                if (images[i] instanceof String url) {
                    image = StyleManager.getInstance().getCachedImage(url);
                } else if (images[i] instanceof Image img) {
                    image = img;
                } else {
                    throw new IllegalArgumentException("Unexpected type: " + images[i].getClass().getName());
                }

                if (image == null) continue;

                final RepeatStruct repeat = (repeats.length > 0) ?
                        repeats[i <= lastRepeatIndex ? i : lastRepeatIndex] : null; // min
                final BackgroundPosition position = (positions.length > 0) ?
                        positions[i <= lastPositionIndex ? i : lastPositionIndex] : null; // min
                final BackgroundSize size = (sizes.length > 0) ?
                        sizes[i <= lastSizeIndex ? i : lastSizeIndex] : null; // min
                backgroundImages[i] = new BackgroundImage(image,
                        repeat == null ? null : repeat.repeatX,
                        repeat == null ? null : repeat.repeatY,
                        position, size);
            }
        }

        // Give the background fills and background images to a newly constructed BackgroundConverter,
        // and return it.
        return new Background(backgroundFills, backgroundImages);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code value} is {@code null}
     */
    @Override
    public Map<CssMetaData<? extends Styleable, ?>, Object> convertBack(Background value) {
        List<BackgroundFill> fills = value.getFills();
        int fillsCount = fills.size();
        Paint[] backgroundColor = new Paint[fillsCount];
        CornerRadii[] backgroundRadius = new CornerRadii[fillsCount];
        Insets[] backgroundInsets = new Insets[fillsCount];

        for (int i = 0; i < fillsCount; ++i) {
            BackgroundFill fill = fills.get(i);
            backgroundColor[i] = fill.getFill();
            backgroundRadius[i] = fill.getRadii();
            backgroundInsets[i] = fill.getInsets();
        }

        List<BackgroundImage> backgroundImages = value.getImages();
        int imageCount = backgroundImages.size();
        Image[] images = new Image[imageCount];
        BackgroundPosition[] backgroundPosition = new BackgroundPosition[imageCount];
        RepeatStruct[] backgroundRepeat = new RepeatStruct[imageCount];
        BackgroundSize[] backgroundSize = new BackgroundSize[imageCount];

        for (int i = 0; i < imageCount; ++i) {
            BackgroundImage image = backgroundImages.get(i);
            images[i] = image.getImage();
            backgroundPosition[i] = image.getPosition();
            backgroundRepeat[i] = new RepeatStruct(image.getRepeatX(), image.getRepeatY());
            backgroundSize[i] = image.getSize();
        }

        return Map.of(
            Background.BACKGROUND_COLOR, backgroundColor,
            Background.BACKGROUND_IMAGE, images,
            Background.BACKGROUND_INSETS, backgroundInsets,
            Background.BACKGROUND_POSITION, backgroundPosition,
            Background.BACKGROUND_RADIUS, backgroundRadius,
            Background.BACKGROUND_REPEAT, backgroundRepeat,
            Background.BACKGROUND_SIZE, backgroundSize);
    }
}
