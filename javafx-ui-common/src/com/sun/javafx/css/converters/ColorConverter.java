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
package com.sun.javafx.css.converters;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import com.sun.javafx.css.StyleConverter;
import com.sun.javafx.css.ParsedValue;

public final class ColorConverter extends StyleConverter<String, Color> {

    private static class Holder {
        static ColorConverter COLOR_INSTANCE = new ColorConverter();
    }

    // lazy, thread-safe instatiation
    public static ColorConverter getInstance() {
        return Holder.COLOR_INSTANCE;
    }

    private ColorConverter() {
        super();
    }

    @Override
    public Color convert(ParsedValue<String, Color> value, Font font) {
        String str = value.getValue();
        if (str == null || str.isEmpty() || "null".equals(str)) {
            return null;
        }
        try {
            return Color.web(str);
        } catch (final IllegalArgumentException e) {
            // TODO: use logger here
            System.err.println("not a color: " + value);
            return Color.BLACK;
        }
    }

    @Override
    public String toString() {
        return "ColorConverter";
    }
}
