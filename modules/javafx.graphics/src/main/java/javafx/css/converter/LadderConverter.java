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
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;

/**
 * Converter to convert a parsed representation of color ladder values to a {@code Color}.
 * @since 9
 */
public final class LadderConverter extends StyleConverter<ParsedValue[], Color> {

    // lazy, thread-safe instatiation
    private static class Holder {
        static final LadderConverter INSTANCE = new LadderConverter();
    }

    /**
     * Gets the {@code LadderConverter} instance.
     * @return the {@code LadderConverter} instance
     */
    public static LadderConverter getInstance() {
        return Holder.INSTANCE;
    }

    private LadderConverter() {
        super();
    }

    @Override
    public Color convert(ParsedValue<ParsedValue[], Color> value, Font font) {
        final ParsedValue[] values = value.getValue();
        final Color color = (Color) values[0].convert(font);
        Stop[] stops = new Stop[values.length - 1];
        for (int v = 1; v < values.length; v++) {
            stops[v - 1] = (Stop) values[v].convert(font);
        }
        return com.sun.javafx.util.Utils.ladder(color, stops);
    }

    @Override
    public String toString() {
        return "LadderConverter";
    }
}
