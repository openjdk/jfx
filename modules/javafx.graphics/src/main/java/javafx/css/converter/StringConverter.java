/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.util.Utils;
import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.scene.text.Font;

/**
 * String type converts embedded unicode characters
 *
 * @since 9
 */
public final class StringConverter extends StyleConverter<String, String> {

    // lazy, thread-safe instatiation
    private static class Holder {
        static final StringConverter INSTANCE = new StringConverter();
        static final SequenceConverter SEQUENCE_INSTANCE = new SequenceConverter();
    }

    public static StyleConverter<String, String> getInstance() {
        return Holder.INSTANCE;
    }

    private StringConverter() {
        super();
    }

    @Override
    public String convert(ParsedValue<String, String> value, Font font) {
        String string = value.getValue();
        if (string == null) {
            return null;
        } // escaping for those
        return Utils.convertUnicode(string);
    }

    @Override
    public String toString() {
        return "StringConverter";
    }

    public static final class SequenceConverter extends StyleConverter<ParsedValue<String, String>[], String[]> {


        public static SequenceConverter getInstance() {
            return Holder.SEQUENCE_INSTANCE;
        }

        private SequenceConverter() {
            super();
        }

        @Override
        public String[] convert(ParsedValue<ParsedValue<String, String>[], String[]> value, Font font) {
            ParsedValue<String, String>[] layers = value.getValue();
            String[] strings = new String[layers.length];
            for (int layer = 0; layer < layers.length; layer++) {
                strings[layer] = StringConverter.getInstance().convert(layers[layer], font);
            }
            return strings;
        }

        @Override
        public String toString() {
            return "String.SequenceConverter";
        }
    }

}
