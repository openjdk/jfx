/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Locale;

import javafx.util.StringConverter;
import javafx.util.converter.LocalDateTimeStringConverter.LdtConverter;

/**
 * <p>{@link StringConverter} implementation for {@link LocalTime} values.</p>
 *
 * @see LocalDateStringConverter
 * @see LocalDateTimeStringConverter
 * @since JavaFX 8u40
 */
public class LocalTimeStringConverter extends StringConverter<LocalTime> {

    LdtConverter<LocalTime> ldtConverter;

    // ------------------------------------------------------------ Constructors

    /**
     * Create a {@link StringConverter} for {@link LocalTime} values, using a
     * default formatter and parser with {@link FormatStyle#SHORT}, and the
     * user's {@link Locale}.
     */
    public LocalTimeStringConverter() {
        ldtConverter = new LdtConverter<LocalTime>(LocalTime.class, null, null,
                                                  null, null, null, null);
    }

    /**
     * Create a {@link StringConverter} for {@link LocalTime} values, using a
     * default formatter and parser with the specified {@link FormatStyle} and
     * based on the user's {@link Locale}.
     *
     * @param timeStyle The {@link FormatStyle} that will be used by the default
     * formatter and parser. If null then {@link FormatStyle#SHORT} will be used.
     */
    public LocalTimeStringConverter(FormatStyle timeStyle) {
        ldtConverter = new LdtConverter<LocalTime>(LocalTime.class, null, null,
                                                  null, timeStyle, null, null);
    }

    /**
     * Create a StringConverter for {@link LocalTime} values, using a
     * default formatter and parser with the specified {@link FormatStyle}
     * and {@link Locale}.
     *
     * @param timeStyle The {@link FormatStyle} that will be used by the default
     * formatter and parser. If null then {@link FormatStyle#SHORT} will be used.
     * @param locale The {@link Locale} that will be used by the default
     * formatter and parser. If null then
     * {@code Locale.getDefault(Locale.Category.FORMAT)} will be used.
     */
    public LocalTimeStringConverter(FormatStyle timeStyle, Locale locale) {
        ldtConverter = new LdtConverter<LocalTime>(LocalTime.class, null, null,
                                                  null, timeStyle, locale, null);
    }

    /**
     * Create a StringConverter for {@link LocalTime} values using the
     * supplied formatter and parser, which are responsible for
     * choosing the desired {@link Locale}.
     *
     * <p>For example, a fixed pattern can be used for converting both ways:</p>
     * <blockquote><pre>
     * String pattern = "HH:mm:ss";
     * DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
     * StringConverter&lt;LocalTime&gt; converter =
     *     DateTimeStringConverter.getLocalTimeConverter(formatter, null);
     * </pre></blockquote>
     *
     * @param formatter An instance of {@link DateTimeFormatter} which
     * will be used for formatting by the toString() method. If null
     * then a default formatter will be used.
     * @param parser An instance of {@link DateTimeFormatter} which
     * will be used for parsing by the fromString() method. This can
     * be identical to formatter. If null, then formatter will be
     * used, and if that is also null, then a default parser will be
     * used.
     */
    public LocalTimeStringConverter(DateTimeFormatter formatter, DateTimeFormatter parser) {
        ldtConverter = new LdtConverter<LocalTime>(LocalTime.class, formatter, parser,
                                                   null, null, null, null);
    }

    // ------------------------------------------------------- Converter Methods

    /** {@inheritDoc} */
    @Override public LocalTime fromString(String value) {
        return ldtConverter.fromString(value);
    }

    /** {@inheritDoc} */
    @Override public String toString(LocalTime value) {
        return ldtConverter.toString(value);
    }
}
