/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DecimalStyle;
import java.time.format.FormatStyle;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalQuery;
import java.util.Locale;
import java.util.Objects;

/// Base class for the java.time types converters.
abstract class BaseTemporalStringConverter<T extends Temporal> extends BaseStringConverter<T> {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault(Locale.Category.FORMAT);
    private static final Chronology DEFAULT_CHRONO = IsoChronology.INSTANCE;

    private final DateTimeFormatter formatter;
    private final DateTimeFormatter parser;

    protected BaseTemporalStringConverter(FormatStyle dateStyle, FormatStyle timeStyle, Locale locale, Chronology chrono) {
        locale = Objects.requireNonNullElse(locale, DEFAULT_LOCALE);
        chrono = Objects.requireNonNullElse(chrono, DEFAULT_CHRONO);
        formatter = createFormatter(dateStyle, timeStyle, locale, chrono);
        parser = createParser(dateStyle, timeStyle, locale, chrono);
    }

    protected BaseTemporalStringConverter(DateTimeFormatter formatter, DateTimeFormatter parser,
            FormatStyle dateStyle, FormatStyle timeStyle) {
        this.formatter = formatter != null ? formatter :
                createFormatter(dateStyle, timeStyle, DEFAULT_LOCALE, DEFAULT_CHRONO);
        this.parser = parser != null ? parser :
                formatter != null ? formatter :
                    createParser(dateStyle, timeStyle, DEFAULT_LOCALE, DEFAULT_CHRONO);
    }

    private final DateTimeFormatter createParser(FormatStyle dateStyle, FormatStyle timeStyle, Locale locale, Chronology chrono) {
        String pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(dateStyle, timeStyle, chrono, locale);
        return new DateTimeFormatterBuilder().parseLenient()
                .appendPattern(pattern)
                .toFormatter()
                .withChronology(chrono)
                .withDecimalStyle(DecimalStyle.of(locale));
    }

    /// To satisfy the requirements of date formatters as described in, e.g.,
    /// [LocalDateStringConverter#LocalDateStringConverter()], the method checks if there's a need to modify the pattern.
    private final DateTimeFormatter createFormatter(FormatStyle dateStyle, FormatStyle timeStyle, Locale locale, Chronology chrono) {
        if (dateStyle != null) {
            String pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(dateStyle, timeStyle, chrono, locale);
            if (pattern.contains("yy") && !pattern.contains("yyy")) {
                // Modify pattern to show four-digit year, including leading zeros.
                String newPattern = pattern.replace("yy", "yyyy");
                return DateTimeFormatter.ofPattern(newPattern).withDecimalStyle(DecimalStyle.of(locale));
            }
        }
        return getLocalizedFormatter(dateStyle, timeStyle)
                .withLocale(locale)
                .withChronology(chrono)
                .withDecimalStyle(DecimalStyle.of(locale));
    }

    abstract DateTimeFormatter getLocalizedFormatter(FormatStyle dateStyle, FormatStyle timeStyle);

    @Override
    String toStringFromNonNull(T value) {
        return formatter.format(value);
    }

    @Override
    T fromNonEmptyString(String string) {
        return parser.parse(string, getTemporalQuery());
    }

    abstract TemporalQuery<T> getTemporalQuery();

    /// For tests only
    DateTimeFormatter getFormatter() {
        return formatter;
    }

    /// For tests only
    DateTimeFormatter getParser() {
        return parser;
    }
}
