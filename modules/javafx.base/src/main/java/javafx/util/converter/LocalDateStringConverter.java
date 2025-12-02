/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.time.LocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalQuery;
import java.util.Locale;
import java.util.Objects;

/// A `StringConverter` implementation for [LocalDate] values.
///
/// @see LocalTimeStringConverter
/// @see LocalDateTimeStringConverter
/// @since JavaFX 8u40
public class LocalDateStringConverter extends BaseTemporalStringConverter<LocalDate> {

    /// Creates a `LocalDateStringConverter` that uses a formatter and parser based on [IsoChronology],
    /// [FormatStyle#SHORT], and the user's [Locale].
    ///
    /// This converter ensures symmetry between the `toString()` and
    /// `fromString()` methods. Many of the default locale based patterns used by
    /// [DateTimeFormatter] will display only two digits for the year when
    /// formatting to a string. This would cause a value like 1955 to be
    /// displayed as 55, which in turn would be parsed back as 2055. This
    /// converter modifies two-digit year patterns to always use four digits. The
    /// input parsing is not affected, so two digit year values can still be
    /// parsed leniently as expected in these locales.
    public LocalDateStringConverter() {
        this(null);
    }

    /// Creates a `LocalDateStringConverter` that uses a formatter and parser based on [IsoChronology], the specified
    /// `FormatStyle`, and the user's [Locale].
    ///
    /// @param dateStyle the `FormatStyle` that will be used by the formatter and parser. If `null`, [FormatStyle#SHORT]
    ///        will be used.
    public LocalDateStringConverter(FormatStyle dateStyle) {
        this(dateStyle, null, null);
    }

    /// Creates a `LocalDateStringConverter` that uses a formatter and parser based on the given `FormatStyle`, `Locale`,
    /// and `Chronology`.
    ///
    /// @param dateStyle the `FormatStyle` that will be used by the formatter and parser. If `null`, [FormatStyle#SHORT]
    ///        will be used.
    /// @param locale the `Locale` that will be used by the formatter and parser. If `null`, the user's locale will be
    ///        used.
    /// @param chronology the `Chronology` that will be used by the formatter and parser. If `null`,
    ///        [IsoChronology#INSTANCE] will be used.
    public LocalDateStringConverter(FormatStyle dateStyle, Locale locale, Chronology chronology) {
        super(Objects.requireNonNullElse(dateStyle, FormatStyle.SHORT), null, locale, chronology);
    }

    /// Creates a `LocalDateStringConverter` that uses the given formatter and parser.
    ///
    /// For example, to use a fixed pattern for converting both ways:
    /// {@snippet :
    /// String pattern = "yyyy-MM-dd";
    /// DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
    /// StringConverter<LocalDate> converter = DateTimeStringConverter.getLocalDateStringConverter(formatter, null);
    /// }
    ///
    /// Note that the formatter and parser can be created to handle non-default [Locale] and [Chronology] as needed.
    ///
    /// @param formatter the formatter that will be used by the `toString()` method. If `null`, a default formatter will
    ///        be used.
    /// @param parser the parser that will be used by the `fromString()` method. This can be identical to formatter. If
    ///        `null`, `formatter` will be used, and if that is also `null`, a default parser will be used.
    public LocalDateStringConverter(DateTimeFormatter formatter, DateTimeFormatter parser) {
        super(formatter, parser, FormatStyle.SHORT, null);
    }

    @Override
    DateTimeFormatter getLocalizedFormatter(FormatStyle dateStyle, FormatStyle timeStyle) {
        return DateTimeFormatter.ofLocalizedDate(dateStyle);
    }

    @Override
    TemporalQuery<LocalDate> getTemporalQuery() {
        return LocalDate::from;
    }
}
