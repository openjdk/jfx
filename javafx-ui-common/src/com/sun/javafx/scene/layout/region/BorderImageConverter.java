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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.scene.image.Image;

import com.sun.javafx.css.StyleConverter;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.ParsedValue;

public final class BorderImageConverter extends StyleConverter<ParsedValue[], List<BorderImage>> {

    private static class Holder {
        private static final BorderImageConverter BORDER_IMAGE_CONVERTER =
                new BorderImageConverter();
    }

    public static BorderImageConverter getInstance() {
        return Holder.BORDER_IMAGE_CONVERTER;
    }

    private BorderImageConverter() {
        super();
    }

    @Override
    public List<BorderImage> convert(Map<StyleableProperty, Object> convertedValues) {
        String[] imageUrls = null;
        BorderImage.BorderImageRepeat[] repeats = null;
        BorderImage.BorderImageSlice[] slices = null;
        Margins[] widths = null;
        Insets[] insets = null;
        List<StyleableProperty> styleables = BorderImage.impl_CSS_STYLEABLES();
        for (int k = 0; k < styleables.size(); k++) {
            StyleableProperty styleable = styleables.get(k);
            Object value = convertedValues.get(styleable);
            if (value == null) {
                continue;
            }
            if ("-fx-border-image-source".equals(styleable.getProperty())) {
                imageUrls = (String[]) value;
            } else if ("-fx-border-image-repeat".equals(styleable.getProperty())) {
                repeats = (BorderImage.BorderImageRepeat[]) value;
            } else if ("-fx-border-image-slice".equals(styleable.getProperty())) {
                slices = (BorderImage.BorderImageSlice[]) value;
            } else if ("-fx-border-image-width".equals(styleable.getProperty())) {
                widths = (Margins[]) value;
            } else if ("-fx-border-image-insets".equals(styleable.getProperty())) {
                insets = (Insets[]) value;
            }
        }
        int nImages = imageUrls != null ? imageUrls.length : 0;
        List<BorderImage> borders = new ArrayList<BorderImage>();
        for (int index = 0; index < nImages; index++) {
            BorderImage.BorderImageRepeat repeat = (repeats != null) ?
                repeats[Math.min(index, repeats.length - 1)] : null;
            BorderImage.BorderImageSlice slice = (slices != null) ?
                slices[Math.min(index, slices.length - 1)] : null;
            Margins width = (widths != null) ?
                widths[Math.min(index, widths.length - 1)] : null;
            Insets inset = (insets != null) ?
                insets[Math.min(index, insets.length - 1)] : null;
            BorderImage.Builder builder = new BorderImage.Builder();
            builder.setImage(new Image(imageUrls[index]));
            if (width != null) {
                builder.setTopWidth(width.getTop())
                       .setRightWidth(width.getRight())
                       .setBottomWidth(width.getBottom())
                       .setLeftWidth(width.getLeft())
                       .setProportionalWidth(width.isProportional());
            }
            if (inset != null) {
                builder.setOffsets(inset);
            }
            if (slice != null) {
                builder.setFillCenter(slice.isFill())
                       .setTopSlice(slice.getTop())
                       .setRightSlice(slice.getRight())
                       .setBottomSlice(slice.getBottom())
                       .setLeftSlice(slice.getLeft())
                       .setProportionalSlice(slice.isProportional());
            }
            if (repeat != null) {
                builder.setRepeatX(repeat.getRepeatX())
                       .setRepeatY(repeat.getRepeatY());
            }
            borders.add(builder.build());
        }
        return borders;
    }
}
