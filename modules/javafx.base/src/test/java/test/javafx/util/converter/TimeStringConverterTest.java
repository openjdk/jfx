/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.util.converter;

import static org.junit.jupiter.api.Assertions.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Stream;

import javafx.util.converter.DateTimeStringConverterShim;
import javafx.util.converter.TimeStringConverter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TimeStringConverterTest {
    private static final Date VALID_TIME_WITH_SECONDS;
    private static final Date VALID_TIME_WITHOUT_SECONDS;

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 34);
        c.set(Calendar.SECOND, 56);
        VALID_TIME_WITH_SECONDS = c.getTime();
        c.set(Calendar.SECOND, 0);
        VALID_TIME_WITHOUT_SECONDS = c.getTime();
    }

    private static Stream<Arguments> provideAllConverters() {
        return Stream.of(
                createTestCase(
                        new TimeStringConverter(),
                        Locale.getDefault(Locale.Category.FORMAT),
                        DateFormat.DEFAULT,
                        VALID_TIME_WITH_SECONDS,
                        null,
                        null
                ),
                createTestCase(
                        new TimeStringConverter(DateFormat.SHORT),
                        Locale.getDefault(Locale.Category.FORMAT),
                        DateFormat.SHORT,
                        VALID_TIME_WITHOUT_SECONDS,
                        null,
                        null
                ),
                createTestCase(
                        new TimeStringConverter(Locale.UK),
                        Locale.UK,
                        DateFormat.DEFAULT,
                        VALID_TIME_WITH_SECONDS,
                        null,
                        null
                ),
                createTestCase(
                        new TimeStringConverter(Locale.UK, DateFormat.SHORT),
                        Locale.UK,
                        DateFormat.SHORT,
                        VALID_TIME_WITHOUT_SECONDS,
                        null,
                        null
                ),
                createTestCase(
                        new TimeStringConverter("HH mm ss"),
                        Locale.getDefault(Locale.Category.FORMAT),
                        DateFormat.DEFAULT,
                        VALID_TIME_WITH_SECONDS,
                        "HH mm ss",
                        null
                ),
                createTestCase(
                        new TimeStringConverter(DateFormat.getTimeInstance(DateFormat.FULL)),
                        Locale.getDefault(Locale.Category.FORMAT),
                        DateFormat.DEFAULT,
                        VALID_TIME_WITH_SECONDS,
                        null,
                        DateFormat.getTimeInstance(DateFormat.FULL)
                )
        );
    }

    private static Arguments createTestCase(TimeStringConverter converter, Locale locale, int timeStyle, Date validDate, String pattern, DateFormat dateFormat) {
        DateFormat validFormatter = computeValidFormatter(pattern, dateFormat, timeStyle, locale);
        return Arguments.of(converter, locale, timeStyle, validDate, pattern, dateFormat, validFormatter);
    }

    private static DateFormat computeValidFormatter(String pattern, DateFormat dateFormat, int timeStyle, Locale locale) {
        if (dateFormat != null) {
            return dateFormat;
        } else if (pattern != null) {
            return new SimpleDateFormat(pattern);
        } else {
            return DateFormat.getTimeInstance(timeStyle, locale);
        }
    }

    private static Stream<Arguments> provideConvertersForConstructor() {
        return provideAllConverters()
                .map(args -> Arguments.of(args.get()[0], args.get()[1], args.get()[2], args.get()[4], args.get()[5]));
    }

    @ParameterizedTest
    @MethodSource("provideConvertersForConstructor")
    void testConstructor(TimeStringConverter converter, Locale locale, int timeStyle, String pattern, DateFormat dateFormat) {
        assertEquals(locale, DateTimeStringConverterShim.getLocale(converter));
        assertEquals(timeStyle, DateTimeStringConverterShim.getTimeStyle(converter));
        assertEquals(pattern, DateTimeStringConverterShim.getPattern(converter));
        assertEquals(dateFormat, DateTimeStringConverterShim.getDateFormatVar(converter));
    }

    private static Stream<Arguments> provideConvertersForGetDateFormat() {
        return provideAllConverters()
                .map(args -> Arguments.of(args.get()[0]));
    }

    @ParameterizedTest
    @MethodSource("provideConvertersForGetDateFormat")
    void getDateFormat(TimeStringConverter converter) {
        assertNotNull(DateTimeStringConverterShim.getDateFormat(converter));
    }

    @Test
    void getDateFormat_nonNullPattern() {
        TimeStringConverter converter = new TimeStringConverter("HH");
        assertTrue(DateTimeStringConverterShim.getDateFormat(converter) instanceof SimpleDateFormat);
    }

    private static Stream<Arguments> provideConvertersForFromString() {
        return provideAllConverters()
                .map(args -> Arguments.of(args.get()[0], args.get()[3], args.get()[6]));
    }

    @ParameterizedTest
    @MethodSource("provideConvertersForFromString")
    void fromString_testValidInput(TimeStringConverter converter, Date validDate, DateFormat validFormatter) {
        String input = validFormatter.format(validDate);
        assertEquals(validDate, converter.fromString(input), "Input = " + input);
    }

    @ParameterizedTest
    @MethodSource("provideConvertersForFromString")
    void fromString_testValidInputWithWhiteSpace(TimeStringConverter converter, Date validDate, DateFormat validFormatter) {
        String input = validFormatter.format(validDate);
        assertEquals(validDate, converter.fromString("      " + input + "      "), "Input = " + input);
    }

    private static Stream<Arguments> provideConvertersForException() {
        return provideAllConverters()
                .map(args -> Arguments.of(args.get()[0]));
    }

    @ParameterizedTest
    @MethodSource("provideConvertersForException")
    void fromString_testInvalidInput(TimeStringConverter converter) {
        assertThrows(RuntimeException.class, () -> converter.fromString("abcdefg"));
    }

    private static Stream<Arguments> provideConvertersForToString() {
        return provideConvertersForFromString();
    }

    @ParameterizedTest
    @MethodSource("provideConvertersForToString")
    void toString_validOutput(TimeStringConverter converter, Date validDate, DateFormat validFormatter) {
        assertEquals(validFormatter.format(validDate), converter.toString(validDate));
    }
}
