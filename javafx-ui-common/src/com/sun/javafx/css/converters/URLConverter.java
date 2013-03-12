/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.StyleConverterImpl;
import java.net.MalformedURLException;
import java.net.URL;
import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.scene.text.Font;

/**
 * Convert url("<path>") a URL string resolved relative to the location of the stylesheet.
 */
public final class URLConverter extends StyleConverterImpl<ParsedValue[], String> {

    // lazy, thread-safe instatiation
    private static class Holder {
        static URLConverter INSTANCE = new URLConverter();
        static SequenceConverter SEQUENCE_INSTANCE = new SequenceConverter();
    }

    public static StyleConverter<ParsedValue[], String> getInstance() {
        return Holder.INSTANCE;
    }

    private URLConverter() {
        super();
    }

    @Override
    public String convert(ParsedValue<ParsedValue[], String> value, Font font) {
        String url = null;
        try {
            ParsedValue[] values = value.getValue();
            // clean URI String
            String uriStr = StringConverter.getInstance().convert(values[0], font);
            if (uriStr.startsWith("url(")) {
                uriStr = com.sun.javafx.Utils.stripQuotes(uriStr.substring(4, uriStr.length() - 1));
            } else {
                uriStr = com.sun.javafx.Utils.stripQuotes(uriStr);
            }
            URL stylesheetURL = values[1] != null ? (URL)values[1].getValue() : null;
            URL resolvedURL = null;
            if (stylesheetURL == null) {
                try {
                    resolvedURL = new URL(uriStr);
                } catch (MalformedURLException malf) {
                    // This may be a relative URL, so try resolving
                    // it using the application classloader
                    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    resolvedURL = cl.getResource(uriStr);
                }
            } else {
                // resolve doesn't work with opaque URI's, but this does.
                resolvedURL = new URL(stylesheetURL, uriStr);
            }
            if (resolvedURL != null) url = resolvedURL.toExternalForm();
        } catch (MalformedURLException malf) {
            System.err.println("caught " + malf + " in 'URLType.convert'");
        }
        return url;
    }

    @Override
    public String toString() {
        return "URLType";
    }

    public static final class SequenceConverter extends StyleConverterImpl<ParsedValue<ParsedValue[], String>[], String[]> {

        public static SequenceConverter getInstance() {
            return Holder.SEQUENCE_INSTANCE;
        }

        private SequenceConverter() {
            super();
        }

        @Override
        public String[] convert(ParsedValue<ParsedValue<ParsedValue[], String>[], String[]> value, Font font) {
            ParsedValue<ParsedValue[], String>[] layers = value.getValue();
            String[] urls = new String[layers.length];
            for (int layer = 0; layer < layers.length; layer++) {
                urls[layer] = URLConverter.getInstance().convert(layers[layer], font);
            }
            return urls;
        }

        @Override
        public String toString() {
            return "URLSeqType";
        }
    }

}
