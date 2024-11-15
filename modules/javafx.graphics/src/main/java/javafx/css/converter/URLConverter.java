/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.text.Font;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.util.DataURI;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Converter to convert a parsed value representing URL to a URL string that is
 * resolved relative to the location of the stylesheet.
 * The input value is in the form: {@code url("<path>")}.
 *
 * @since 9
 */
public final class URLConverter extends StyleConverter<ParsedValue[], String> {

    // lazy, thread-safe instatiation
    private static class Holder {
        static final URLConverter INSTANCE = new URLConverter();
        static final SequenceConverter SEQUENCE_INSTANCE = new SequenceConverter();
    }

    /**
     * Gets the {@code URLConverter} instance.
     * @return the {@code URLConverter} instance
     */
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
        resource = resource != null ? resource.trim() : null;

        if (resource != null && !resource.isEmpty()) {
            if (resource.startsWith("url(")) {
                resource = com.sun.javafx.util.Utils.stripQuotes(resource.substring(4, resource.length() - 1));
            } else {
                resource = com.sun.javafx.util.Utils.stripQuotes(resource);
            }

            if (DataURI.matchScheme(resource)) {
                url = resource;
            } else if (!resource.isEmpty()) {
                String stylesheetURL = values.length > 1 && values[1] != null ? (String) values[1].getValue() : null;
                URL resolvedURL = resolve(stylesheetURL, resource);

                if (resolvedURL != null) url = resolvedURL.toExternalForm();
            }
        }

        return url;
    }

    private URL resolve(String stylesheetUrl, String resource) {
        try {
            // Note: the same code (pretty much) also appears in StyleManager

            // if stylesheetUri is null, then we're dealing with an in-line style.
            // If there is no scheme part, then the url is interpreted as being relative to the application's class-loader.
            URI resourceUri = new URI(resource);

            if (resourceUri.isAbsolute()) {
                return resourceUri.toURL();
            }

            URL rtJarUrl = resolveRuntimeImport(resourceUri);
            if (rtJarUrl != null) {
                return rtJarUrl;
            }

            final String path = resourceUri.getPath();
            if (path.startsWith("/")) {
                final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                // FIXME: JIGSAW -- use Class.getResource if resource is in a module
                return contextClassLoader.getResource(path.substring(1));
            }

            final String stylesheetPath = (stylesheetUrl != null) ?  stylesheetUrl.trim() : null;

            if (stylesheetPath != null && stylesheetPath.isEmpty() == false) {

                URI stylesheetUri = new URI(stylesheetPath);

                if (stylesheetUri.isOpaque() == false) {

                    URI resolved = stylesheetUri.resolve(resourceUri);
                    return resolved.toURL();

                } else {

                    // stylesheet URI is something like jar:file:
                    URL url = stylesheetUri.toURL();
                    return new URL(url, resourceUri.getPath());
                }
            }


            // URL doesn't have scheme or stylesheetUrl is null
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            // FIXME: JIGSAW -- use Class.getResource if resource is in a module
            return contextClassLoader.getResource(path);


        } catch (final MalformedURLException|URISyntaxException e) {
            PlatformLogger cssLogger = com.sun.javafx.util.Logging.getCSSLogger();
            if (cssLogger.isLoggable(PlatformLogger.Level.WARNING)) {
                cssLogger.warning(e.getLocalizedMessage());
            }

            return null;
        }

    }


    //
    // Resolve a path from an @import that implies jfxrt.jar,
    // e.g., @import "com/sun/javafx/scene/control/skin/modena/modena.css".
    //
    // See also StyleSheet#loadStylesheet(String)
    //
    private URL resolveRuntimeImport(final URI resourceUri) {

        // FIXME: JIGSAW -- this method needs to be rewritten for Jigsaw.
        // There is no jfxrt.jar any more, and resource encapsulation will
        // prevent it from being resolved anyway.

        final String path = resourceUri.getPath();
        final String resourcePath = path.startsWith("/") ? path.substring(1) : path;

        if ((resourcePath.startsWith("com/sun/javafx/scene/control/skin/modena/") ||
             resourcePath.startsWith("com/sun/javafx/scene/control/skin/caspian/")) &&
            (resourcePath.endsWith(".css") || resourcePath.endsWith(".bss"))) {

            System.err.println("WARNING: resolveRuntimeImport cannot resolve: " + resourcePath);

            // Look up the resource on the class-path.
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            // FIXME: JIGSAW -- use Class.getResource if resource is in a module
            final URL resolved = contextClassLoader.getResource(resourcePath);
            return resolved;
        }
        return null;
    }

    @Override
    public String toString() {
        return "URLType";
    }

    /**
     * Converter to convert a sequence of URLs to an array of {@code String}s.
     * @since 9
     */
    public static final class SequenceConverter extends StyleConverter<ParsedValue<ParsedValue[], String>[], String[]> {

        /**
         * Gets the {@code SequenceConverter} instance.
         * @return the {@code SequenceConverter} instance
         */
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
