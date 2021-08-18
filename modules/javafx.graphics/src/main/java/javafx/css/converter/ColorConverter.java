/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.css.converter;

import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Converter to convert a {@code String} to a {@code Color}.
 * @since 9
 */
public final class ColorConverter extends StyleConverter<String, Color> {

    private static class Holder {
        static final ColorConverter COLOR_INSTANCE = new ColorConverter();
    }

    // lazy, thread-safe instatiation
    /**
     * Gets the {@code ColorConverter} instance.
     * @return the {@code ColorConverter} instance
     */
    public static StyleConverter<String, Color> getInstance() {
        return Holder.COLOR_INSTANCE;
    }

    private ColorConverter() {
        super();
    }

    @Override
    public Color convert(ParsedValue<String, Color> value, Font font) {
        Object val = value.getValue();
        if (val == null) {
            return null;
        }
        if (val instanceof Color) {
            return (Color)val;
        }
        if (val instanceof String) {
            String str = (String)val;
            if (str.isEmpty() || "null".equals(str)) {
                return null;
            }
            try {
                return Color.web((String)val);
            } catch (IllegalArgumentException iae) {
                // fall through pending RT-34551
            }
        }
        // pending RT-34551
        System.err.println("not a color: " + value);
        return Color.BLACK;
    }

    @Override
    public String toString() {
        return "ColorConverter";
    }
}
