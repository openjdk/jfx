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

public final class BackgroundFillConverter extends StyleConverter<ParsedValue[], List<BackgroundFill>> {

    public static class Holder {
        private static final BackgroundFillConverter BACKGROUND_FILLS_CONVERTER
                = new BackgroundFillConverter();
    }

    public static BackgroundFillConverter getInstance() {
        return Holder.BACKGROUND_FILLS_CONVERTER;
    }

    private BackgroundFillConverter() {
        super();
    }

    @Override
    public List<BackgroundFill> convert(Map<StyleableProperty, Object> convertedValues) {
        Paint[] fills = null;
        Insets[] radii = null;
        Insets[] insets = null;
        List<StyleableProperty> styleables = BackgroundFill.impl_CSS_STYLEABLES();
        for (int k = 0; k < styleables.size(); k++) {
            StyleableProperty styleable = styleables.get(k);
            Object value = convertedValues.get(styleable);
            if (value == null) {
                continue;
            }
            if ("-fx-background-color".equals(styleable.getProperty())) {
                fills = (Paint[]) value;
            } else if ("-fx-background-radius".equals(styleable.getProperty())) {
                radii = (Insets[]) value;
            } else if ("-fx-background-insets".equals(styleable.getProperty())) {
                insets = (Insets[]) value;
            }
        }
        int nFills = (fills != null) ? fills.length : 0;
        List<BackgroundFill> results = new ArrayList<BackgroundFill>();
        for (int i = 0; i < nFills; i++) {
            Insets radius = (radii != null) ? radii[Math.min(i, radii.length - 1)] : Insets.EMPTY;
            Insets offsets = (insets != null) ? insets[Math.min(i, insets.length - 1)] : Insets.EMPTY;
            BackgroundFill bf = new BackgroundFill(fills[i], radius.getTop(), radius.getRight(), radius.getBottom(), radius.getLeft(), offsets);
            results.add(bf);
        }
        return results;
    }

    @Override
    public String toString() {
        return "BackgroundFillsType";
    }
}
