/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/// A `StringConverter` implementation for [Date] values that represent dates.
///
/// Note that using `Date` is not recommended in JDK versions where [java.time.LocalDate] is available, in which case
/// [LocalDateStringConverter] should be used.
///
/// @since JavaFX 2.1
public class DateStringConverter extends DateTimeStringConverter {

    /// Creates a `DateStringConverter` that uses a formatter/parser based on [DateFormat#DEFAULT] for the date style,
    /// and the user's [Locale].
    public DateStringConverter() {
        super();
    }

    /// Creates a `DateStringConverter` that uses a formatter/parser based on the given date style, and the user's
    /// [Locale].
    ///
    /// @param dateStyle the formatting style for dates. For example, [DateFormat#SHORT] for "M/d/yy" in the US locale.
    /// @since JavaFX 8u40
    public DateStringConverter(int dateStyle) {
        super(dateStyle, DateFormat.DEFAULT);
    }

    /// Creates a `DateStringConverter` that uses a formatter/parser based on [DateFormat#DEFAULT] for the date style,
    /// and the given `Locale`.
    ///
    /// @param locale the `Locale` that will be used by the formatter/parser. If `null`, the user's locale will be used.
    public DateStringConverter(Locale locale) {
        super(locale);
    }

    /// Creates a `DateStringConverter` that uses a formatter/parser based on the given date style and `Locale`.
    ///
    /// @param locale the `Locale` that will be used by the formatter/parser. If `null`, the user's locale will be used.
    /// @param dateStyle the formatting style for dates. For example, [DateFormat#SHORT] for "M/d/yy" in the US locale.
    ///
    /// @since JavaFX 8u40
    public DateStringConverter(Locale locale, int dateStyle) {
        super(locale, dateStyle, DateFormat.DEFAULT);
    }

    /// Creates a `DateStringConverter` that uses a formatter/parser based on the given pattern, and the user's [Locale].
    ///
    /// @param pattern the pattern describing the date format. If `null`, [DateFormat#DEFAULT] will be used for the date
    ///        style.
    public DateStringConverter(String pattern) {
        super(pattern);
    }

    /// Creates a `DateStringConverter` that uses a formatter/parser based on the given pattern and `Locale`.
    ///
    /// @param locale the `Locale` that will be used by the formatter/parser. If `null`, the user's locale will be used.
    /// @param pattern the pattern describing the date format. If `null`, [DateFormat#DEFAULT] will be used for the date
    ///        style.
    public DateStringConverter(Locale locale, String pattern) {
        super(locale, pattern);
    }

    /// Creates a `DateStringConverter` that uses the given formatter/parser.
    ///
    /// @param dateFormat the formatter/parser that will be used by the `toString()` and `fromString()` methods. If
    ///        `null`, a default formatter/parser will be used.
    public DateStringConverter(DateFormat dateFormat) {
        super(dateFormat);
    }

    @Override
    DateFormat getSpecialziedDateFormat(int dateStyle, int timeStyle, Locale locale) {
        return DateFormat.getDateInstance(dateStyle, locale);
    }
}
