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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalQuery;
import java.util.Locale;
import java.util.Objects;

/// A `StringConverter` implementation for [LocalTime] values.
///
/// @see LocalDateStringConverter
/// @see LocalDateTimeStringConverter
/// @since JavaFX 8u40
public class LocalTimeStringConverter extends BaseTemporalStringConverter<LocalTime> {

    /// Creates a `LocalTimeStringConverter` that uses a formatter and parser based on [FormatStyle#SHORT], and the
    /// user's [Locale].
    public LocalTimeStringConverter() {
        this(null);
    }

    /// Creates a `LocalTimeStringConverter` that uses a formatter and parser based on the specified `FormatStyle` and
    /// the user's [Locale].
    ///
    /// @param timeStyle the `FormatStyle` that will be used by the formatter and parser. If `null`, [FormatStyle#SHORT]
    ///        will be used.
    public LocalTimeStringConverter(FormatStyle timeStyle) {
        this(timeStyle, null);
    }

    /// Creates a `LocalTimeStringConverter` that uses a formatter and parser based on the specified `FormatStyle` and
    /// `Locale`.
    ///
    /// @param timeStyle The `FormatStyle` that will be used by the formatter and parser. If `null`, [FormatStyle#SHORT]
    ///        will be used.
    /// @param locale the `Locale` that will be used by the formatter and parser. If `null`, the user's locale will be
    ///        used.
    public LocalTimeStringConverter(FormatStyle timeStyle, Locale locale) {
        super(null, Objects.requireNonNullElse(timeStyle, FormatStyle.SHORT), locale, null);
    }

    /// Creates a `LocalTimeStringConverter` that uses the given formatter and parser.
    ///
    /// For example, a fixed pattern can be used for converting both ways:
    /// {@snippet :
    /// String pattern = "HH:mm:ss";
    /// DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
    /// StringConverter<LocalTime> converter = DateTimeStringConverter.getLocalTimeConverter(formatter, null);
    /// }
    ///
    /// @param formatter the formatter that will be used by the `toString()` method. If `null`, a default formatter will
    ///        be used.
    /// @param parser the parser that will be used by the `fromString()` method. This can be identical to formatter. If
    ///        `null`, `formatter` will be used, and if that is also `null`, a default parser will be used.
    public LocalTimeStringConverter(DateTimeFormatter formatter, DateTimeFormatter parser) {
        super(formatter, parser, null, FormatStyle.SHORT);
    }

    @Override
    DateTimeFormatter getLocalizedFormatter(FormatStyle dateStyle, FormatStyle timeStyle) {
        return DateTimeFormatter.ofLocalizedTime(timeStyle);
    }

    @Override
    TemporalQuery<LocalTime> getTemporalQuery() {
        return LocalTime::from;
    }
}
