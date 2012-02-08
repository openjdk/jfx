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

import javafx.scene.image.Image;

import com.sun.javafx.css.StyleConverter;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.ParsedValue;

public final class BackgroundImageConverter extends StyleConverter<ParsedValue[], List<BackgroundImage>> {

    private static class Holder {
        private static final BackgroundImageConverter BACKGROUND_IMAGE_CONVERTER =
            new BackgroundImageConverter();
    }

    public static BackgroundImageConverter getInstance() {
        return Holder.BACKGROUND_IMAGE_CONVERTER;
    }

    private BackgroundImageConverter() {
        super();
    }

    @Override
    public List<BackgroundImage> convert(Map<StyleableProperty, Object> convertedValues) {
        String[] imageUrls = null;
        BackgroundImage.BackgroundRepeat[] repeats = null;
        BackgroundImage.BackgroundPosition[] positions = null;
        BackgroundImage.BackgroundSize[] sizes = null;
        List<StyleableProperty> styleables = BackgroundImage.impl_CSS_STYLEABLES();
        for (int k = 0; k < styleables.size(); k++) {
            StyleableProperty styleable = styleables.get(k);
            Object value = convertedValues.get(styleable);
            if (value == null) {
                continue;
            }
            if ("-fx-background-image".equals(styleable.getProperty())) {
                imageUrls = (String[]) value;
            } else if ("-fx-background-repeat".equals(styleable.getProperty())) {
                repeats = (BackgroundImage.BackgroundRepeat[]) value;
            } else if ("-fx-background-position".equals(styleable.getProperty())) {
                positions = (BackgroundImage.BackgroundPosition[]) value;
            } else if ("-fx-background-size".equals(styleable.getProperty())) {
                sizes = (BackgroundImage.BackgroundSize[]) value;
            }
        }
        List<BackgroundImage> images = new ArrayList<BackgroundImage>();
        int nImages = (imageUrls != null) ? imageUrls.length : 0;
        for (int index = 0; index < nImages; index++) {
            BackgroundImage.BackgroundRepeat repeat = (repeats != null) ? repeats[Math.min(index, repeats.length - 1)] : null;
            BackgroundImage.BackgroundPosition position = (positions != null) ? positions[Math.min(index, positions.length - 1)] : null;
            BackgroundImage.BackgroundSize size = (sizes != null) ? sizes[Math.min(index, sizes.length - 1)] : null;
            BackgroundImage.Builder builder = new BackgroundImage.Builder();
            builder.setImage(new Image(imageUrls[index]));
            if (repeat != null) {
                builder.setRepeatX(repeat.getRepeatX()).setRepeatY(repeat.getRepeatY());
            }
            if (position != null) {
                builder.setTop(position.getTop())
                       .setRight(position.getRight())
                       .setBottom(position.getBottom())
                       .setLeft(position.getLeft())
                       .setProportionalHPos(position.isProportionalHPos())
                       .setProportionalVPos(position.isProportionalVPos());
            }
            if (size != null) {
                builder.setWidth(size.getWidth())
                       .setHeight(size.getHeight())
                       .setContain(size.isContain())
                       .setCover(size.isCover())
                       .setProportionalWidth(size.isProportionalWidth())
                       .setProportionalHeight(size.isProportionalHeight());
            }
            images.add(builder.build());
        }
        return images;
    }
}
