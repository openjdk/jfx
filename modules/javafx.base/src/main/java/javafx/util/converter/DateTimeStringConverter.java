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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/// A `StringConverter` implementation for [Date] values that represent dates and times.
///
/// Note that using `Date` is not recommended in JDK versions where [java.time.LocalDateTime] is available, in which
/// case [LocalDateTimeStringConverter] should be used.
///
/// @since JavaFX 2.1
public class DateTimeStringConverter extends BaseStringConverter<Date> {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault(Locale.Category.FORMAT);

    private final DateFormat dateFormat;

    /// Creates a `DateTimeStringConverter` that uses a formatter/parser based on [DateFormat#DEFAULT] for both date and
    /// time styles, and the user's [Locale].
    public DateTimeStringConverter() {
        this(DEFAULT_LOCALE, DateFormat.DEFAULT, DateFormat.DEFAULT);
    }

    /// Creates a `DateTimeStringConverter` that uses a formatter/parser based on the given date and time styles, and
    /// the user's [Locale].
    ///
    /// @param dateStyle the formatting style for dates. For example, [DateFormat#SHORT] for "M/d/yy" in the US locale.
    /// @param timeStyle the formatting style for times. For example, `DateFormat.SHORT` for "h:mm a" in the US locale.
    ///
    /// @since JavaFX 8u40
    public DateTimeStringConverter(int dateStyle, int timeStyle) {
        this(DEFAULT_LOCALE, dateStyle, timeStyle);
    }

    /// Creates a `DateTimeStringConverter` that uses a formatter/parser based on [DateFormat#DEFAULT] for both date and
    /// time styles, and the given `Locale`.
    ///
    /// @param locale the `Locale` that will be used by the formatter/parser. If `null`, the user's locale will be used.
    public DateTimeStringConverter(Locale locale) {
        this(locale, DateFormat.DEFAULT, DateFormat.DEFAULT);
    }

    /// Creates a `DateTimeStringConverter` that uses a formatter/parser based on the given date and time styles, and
    /// `Locale`.
    ///
    /// @param locale the `Locale` that will be used by the formatter/parser. If `null`, the user's locale will be used.
    /// @param dateStyle the formatting style for dates. For example, [DateFormat#SHORT] for "M/d/yy" in the US locale.
    /// @param timeStyle the formatting style for times. For example, `DateFormat.SHORT` for "h:mm a" in the US locale.
    ///
    /// @since JavaFX 8u40
    public DateTimeStringConverter(Locale locale, int dateStyle, int timeStyle) {
        dateFormat = create(locale, dateStyle, timeStyle, null);
    }

    /// Creates a `DateTimeStringConverter` that uses a formatter/parser based on the given pattern, and the user's
    /// [Locale].
    ///
    /// @param pattern the pattern describing the date and time format. If `null`, [DateFormat#DEFAULT] will be used for
    ///        both date and time.
    public DateTimeStringConverter(String pattern) {
        this(DEFAULT_LOCALE, pattern);
    }

    /// Creates a `DateTimeStringConverter` that uses a formatter/parser based on the given pattern and `Locale`.
    ///
    /// @param locale the `Locale` that will be used by the formatter/parser. If `null`, the user's locale will be used.
    /// @param pattern the pattern describing the date and time format. If `null`, [DateFormat#DEFAULT] will be used for
    ///        both date and time styles.
    public DateTimeStringConverter(Locale locale, String pattern) {
        dateFormat = create(locale, DateFormat.DEFAULT, DateFormat.DEFAULT, pattern);
    }

    /// Creates a `DateTimeStringConverter` that uses the given formatter/parser.
    ///
    /// @param dateFormat the formatter/parser that will be used by the `toString()` and `fromString()` methods. If
    ///        `null`, a default formatter/parser will be used.
    public DateTimeStringConverter(DateFormat dateFormat) {
        this.dateFormat = dateFormat != null ? dateFormat :
                create(DEFAULT_LOCALE, DateFormat.DEFAULT, DateFormat.DEFAULT, null);
    }

    private DateFormat create(Locale locale, int dateStyle, int timeStyle, String pattern) {
        locale = locale != null ? locale : DEFAULT_LOCALE;
        DateFormat dateFormat = pattern == null ?
                getSpecialziedDateFormat(dateStyle, timeStyle, locale) :
                new SimpleDateFormat(pattern, locale);
        dateFormat.setLenient(false);
        return dateFormat;
    }

    /// Returns the `DateFormat` to be used without a pattern. Subclasses should override to return their own formatter.
    DateFormat getSpecialziedDateFormat(int dateStyle, int timeStyle, Locale locale) {
        return DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
    }

    @Override
    Date fromNonEmptyString(String string) {
        try {
            return dateFormat.parse(string);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    String toStringFromNonNull(Date data) {
        return dateFormat.format(data);
    }

    /// Used in tests only.
    DateFormat getDateFormat() {
        return dateFormat;
    }
}
