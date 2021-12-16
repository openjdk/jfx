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
import javafx.scene.Cursor;
import javafx.scene.text.Font;

/**
 * Converter to convert a {@code String} to a {@code Cursor}.
 * @since 9
 */
public final class CursorConverter extends StyleConverter<String, Cursor> {

    // lazy, thread-safe instatiation
    private static class Holder {
        static final CursorConverter INSTANCE = new CursorConverter();
    }

    /**
     * Gets the {@code CursorConverter} instance.
     * @return the {@code CursorConverter} instance
     */
    public static StyleConverter<String, Cursor> getInstance() {
        return Holder.INSTANCE;
    }

    private CursorConverter() {
        super();
    }

    @Override
    public Cursor convert(ParsedValue<String, Cursor> value, Font not_used) {

        // the parser doesn't covert cusor, so convert it from the raw value
        String string = value.getValue();

        if (string != null) {

            int index = string.indexOf("Cursor.");
            if (index > -1) {
                string = string.substring(index+"Cursor.".length());
            }
            string = string.replace('-','_').toUpperCase();
        }

        try {
            return Cursor.cursor(string);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return Cursor.DEFAULT;
        }
    }

    @Override
    public String toString() {
        return "CursorConverter";
    }
}
