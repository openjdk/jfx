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
import javafx.scene.paint.Paint;

import com.sun.javafx.css.StyleConverter;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.ParsedValue;

public final class StrokeBorderConverter extends StyleConverter<ParsedValue[], List<StrokeBorder>> {

    private static class Holder {
        private static final StrokeBorderConverter STROKE_BORDER_CONVERTER =
            new StrokeBorderConverter();
    }

    public static StrokeBorderConverter getInstance() {
        return Holder.STROKE_BORDER_CONVERTER;
    }

    private StrokeBorderConverter() {
        super();
    }

    @Override
    public List<StrokeBorder> convert(Map<StyleableProperty, Object> convertedValues) {
        Paint[][] borderColor = null;
        BorderStyle[][] borderStyle = null;
        Margins[] borderWidth = null;
        Margins[] borderRadii = null;
        Insets[] borderInsets = null;
        List<StyleableProperty> styleables = StrokeBorder.impl_CSS_STYLEABLES();
        for (int k = 0; k < styleables.size(); k++) {
            StyleableProperty styleable = styleables.get(k);
            Object value = convertedValues.get(styleable);
            if (value == null) {
                continue;
                //System.out.println("StrokeBorderType.convert: styleable = " + styleable + ", value = " + value);
            }
            if ("-fx-border-color".equals(styleable.getProperty())) {
                borderColor = (Paint[][]) value;
            } else if ("-fx-border-style".equals(styleable.getProperty())) {
                borderStyle = (BorderStyle[][]) value;
            } else if ("-fx-border-width".equals(styleable.getProperty())) {
                borderWidth = (Margins[]) value;
            } else if ("-fx-border-radius".equals(styleable.getProperty())) {
                borderRadii = (Margins[]) value;
            } else if ("-fx-border-insets".equals(styleable.getProperty())) {
                borderInsets = (Insets[]) value;
            }
        }
        final int max = (borderColor != null) ? borderColor.length : 0;
        List<StrokeBorder> strokeBorders = new ArrayList<StrokeBorder>();
        for (int index = 0; index < max; index++) {
            BorderStyle[] style = (borderStyle != null) ? borderStyle[Math.min(index, borderStyle.length - 1)] : null;
            Margins widths = (borderWidth != null) ? borderWidth[Math.min(index, borderWidth.length - 1)] : null;
            Margins radius = (borderRadii != null) ? borderRadii[Math.min(index, borderRadii.length - 1)] : null;
            Insets offsets = (borderInsets != null) ? borderInsets[Math.min(index, borderInsets.length - 1)] : null;
            StrokeBorder.Builder builder = new StrokeBorder.Builder();
            builder.setTopFill(borderColor[index][0]).setRightFill(borderColor[index][1]).setBottomFill(borderColor[index][2]).setLeftFill(borderColor[index][3]);
            if (widths != null) {
                builder.setTopWidth(widths.getTop()).setRightWidth(widths.getRight()).setBottomWidth(widths.getBottom()).setLeftWidth(widths.getLeft()).setProportionalWidth(widths.isProportional());
            }
            if (radius != null) {
                builder.setTopLeftCornerRadius(radius.getTop()).setTopRightCornerRadius(radius.getRight()).setBottomRightCornerRadius(radius.getBottom()).setBottomLeftCornerRadius(radius.getLeft()).setProportionalWidth(radius.isProportional());
            }
            if (offsets != null) {
                builder.setOffsets(offsets);
            }
            if (style != null) {
                builder.setTopStyle(style[0]).setRightStyle(style[1]).setBottomStyle(style[2]).setLeftStyle(style[3]);
            }
            strokeBorders.add(builder.build());
        }
        return strokeBorders;
    }

    @Override
    public String toString() {
        return "StrokeBorderType";
    }
}
