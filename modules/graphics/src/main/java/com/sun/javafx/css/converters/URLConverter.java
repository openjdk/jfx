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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.scene.text.Font;
import sun.util.logging.PlatformLogger;

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

        ParsedValue[] values = value.getValue();

        String resource = values.length > 0 ? StringConverter.getInstance().convert(values[0], font) : null;

        if (resource != null && resource.trim().isEmpty() == false) {

            if (resource.startsWith("url(")) {
                resource = com.sun.javafx.Utils.stripQuotes(resource.substring(4, resource.length() - 1));
            } else {
                resource = com.sun.javafx.Utils.stripQuotes(resource);
            }

            String stylesheetURL = values.length > 1 && values[1] != null ? (String)values[1].getValue() : null;
            URL resolvedURL = resolve(stylesheetURL, resource);

            if (resolvedURL != null) url = resolvedURL.toExternalForm();
        }

        return url;
    }

    // package for testing
    URL resolve(String stylesheetUrl, String resource) {

        try {

            // Note: the same code (pretty much) also appears in StyleManager

            if (stylesheetUrl != null && stylesheetUrl.trim().isEmpty() == false) {
                URI stylesheetUri = new URI(stylesheetUrl.trim());
                URI resolved = stylesheetUri.resolve(resource.trim());
                return resolved.toURL();
            }


            // if stylesheetUri is null, then we're dealing with an in-line style.
            // If there is no scheme part, then the url is interpreted as being relative to the application's class-loader.
            URI uri = new URI(resource.trim());

            if (uri.isAbsolute() == false) {
                // URL doesn't have scheme
                final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                URL resolved = null;
                final String path = uri.getPath();

                if (path.startsWith("/")) {
                    resolved = contextClassLoader.getResource(path.substring(1));
                } else {
                    resolved = contextClassLoader.getResource(path);
                }

                return resolved;

            }

            // else, url does have a scheme
            return uri.toURL();

        } catch (final MalformedURLException|URISyntaxException e) {
            PlatformLogger cssLogger = com.sun.javafx.Logging.getCSSLogger();
            if (cssLogger.isLoggable(PlatformLogger.Level.WARNING)) {
                cssLogger.warning(e.getLocalizedMessage());
            }

            return null;
        }

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
