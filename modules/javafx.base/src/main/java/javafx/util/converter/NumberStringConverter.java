/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

package javafx.util.converter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import javafx.util.StringConverter;

/**
 * <p>{@link StringConverter} implementation for {@link Number} values.</p>
 * @since JavaFX 2.1
 */
public class NumberStringConverter extends StringConverter<Number> {

    // ------------------------------------------------------ Private properties

    final Locale locale;
    final String pattern;
    final NumberFormat numberFormat;

    // ------------------------------------------------------------ Constructors
    public NumberStringConverter() {
        this(Locale.getDefault());
    }

    public NumberStringConverter(Locale locale) {
        this(locale, null);
    }

    public NumberStringConverter(String pattern) {
        this(Locale.getDefault(), pattern);
    }

    public NumberStringConverter(Locale locale, String pattern) {
        this(locale, pattern, null);
    }

    public NumberStringConverter(NumberFormat numberFormat) {
        this(null, null, numberFormat);
    }

    NumberStringConverter(Locale locale, String pattern, NumberFormat numberFormat) {
        this.locale = locale;
        this.pattern = pattern;
        this.numberFormat = numberFormat;
    }

    // ------------------------------------------------------- Converter Methods

    /** {@inheritDoc} */
    @Override public Number fromString(String value) {
        try {
            // If the specified value is null or zero-length, return null
            if (value == null) {
                return null;
            }

            value = value.trim();

            if (value.length() < 1) {
                return null;
            }

            // Create and configure the parser to be used
            NumberFormat parser = getNumberFormat();

            // Perform the requested parsing
            return parser.parse(value);
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    @Override public String toString(Number value) {
        // If the specified value is null, return a zero-length String
        if (value == null) {
            return "";
        }

        // Create and configure the formatter to be used
        NumberFormat formatter = getNumberFormat();

        // Perform the requested formatting
        return formatter.format(value);
    }

    /**
     * <p>Return a <code>NumberFormat</code> instance to use for formatting
     * and parsing in this {@link StringConverter}.</p>
     *
     * @return a {@code NumberFormat} instance for formatting and parsing in this
     * {@link StringConverter}
     */
    protected NumberFormat getNumberFormat() {
        Locale _locale = locale == null ? Locale.getDefault() : locale;

        if (numberFormat != null) {
            return numberFormat;
        } else if (pattern != null) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(_locale);
            return new DecimalFormat(pattern, symbols);
        } else {
            return NumberFormat.getNumberInstance(_locale);
        }
    }
}
