/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Stream;

import javafx.util.converter.DateTimeStringConverter;
import javafx.util.converter.DateTimeStringConverterShim;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DateTimeStringConverterTest {
    private static final Date VALID_DATE_WITH_SECONDS;
    private static final Date VALID_DATE_WITHOUT_SECONDS;

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, 1985);
        c.set(Calendar.MONTH, Calendar.JANUARY);
        c.set(Calendar.DAY_OF_MONTH, 12);
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 34);
        c.set(Calendar.SECOND, 56);
        c.set(Calendar.MILLISECOND, 0);
        VALID_DATE_WITH_SECONDS = c.getTime();
        c.set(Calendar.SECOND, 0);
        VALID_DATE_WITHOUT_SECONDS = c.getTime();
    }

    static Stream<Arguments> provideTestParameters() {
        return Stream.of(
                Arguments.of(
                        "no-args",
                        new Object[]{},
                        Locale.getDefault(Locale.Category.FORMAT),
                        DateFormat.DEFAULT,
                        DateFormat.DEFAULT,
                        VALID_DATE_WITH_SECONDS,
                        null,
                        null,
                        null
                ),
                Arguments.of(
                        "styles",
                        new Object[]{DateFormat.SHORT, DateFormat.SHORT},
                        Locale.getDefault(Locale.Category.FORMAT),
                        DateFormat.SHORT,
                        DateFormat.SHORT,
                        VALID_DATE_WITHOUT_SECONDS,
                        null,
                        null,
                        null
                ),
                Arguments.of(
                        "locale",
                        new Object[]{Locale.UK},
                        Locale.UK,
                        DateFormat.DEFAULT,
                        DateFormat.DEFAULT,
                        VALID_DATE_WITH_SECONDS,
                        null,
                        null,
                        null
                ),
                Arguments.of(
                        "localeStyles",
                        new Object[]{Locale.UK, DateFormat.SHORT, DateFormat.SHORT},
                        Locale.UK,
                        DateFormat.SHORT,
                        DateFormat.SHORT,
                        VALID_DATE_WITHOUT_SECONDS,
                        null,
                        null,
                        null
                ),
                Arguments.of(
                        "pattern",
                        new Object[]{"dd MM yyyy HH mm ss"},
                        Locale.getDefault(Locale.Category.FORMAT),
                        DateFormat.DEFAULT,
                        DateFormat.DEFAULT,
                        VALID_DATE_WITH_SECONDS,
                        "dd MM yyyy HH mm ss",
                        null,
                        null
                ),
                Arguments.of(
                        "dateFormatInstance",
                        new Object[]{DateFormat.LONG, DateFormat.FULL},
                        Locale.getDefault(Locale.Category.FORMAT),
                        DateFormat.DEFAULT,
                        DateFormat.DEFAULT,
                        VALID_DATE_WITH_SECONDS,
                        null,
                        DateFormat.LONG,
                        DateFormat.FULL
                )
        );
    }

    @BeforeAll
    static void setup() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    private DateTimeStringConverter createConverter(String constructorType, Object[] constructorArgs) {
        switch (constructorType) {
            case "no-args":
                return new DateTimeStringConverter();
            case "styles":
                return new DateTimeStringConverter(
                        (Integer) constructorArgs[0],
                        (Integer) constructorArgs[1]
                );
            case "locale":
                return new DateTimeStringConverter((Locale) constructorArgs[0]);
            case "localeStyles":
                return new DateTimeStringConverter(
                        (Locale) constructorArgs[0],
                        (Integer) constructorArgs[1],
                        (Integer) constructorArgs[2]
                );
            case "pattern":
                return new DateTimeStringConverter((String) constructorArgs[0]);
            case "dateFormatInstance":
                DateFormat dateFormat = DateFormat.getDateTimeInstance(
                        (Integer) constructorArgs[0],
                        (Integer) constructorArgs[1]
                );
                return new DateTimeStringConverter(dateFormat);
            default:
                fail("Unknown constructor type: " + constructorType);
                return null;
        }
    }

    private DateFormat createValidFormatter(
            Integer dateFormatDateStyle,
            Integer dateFormatTimeStyle,
            String pattern,
            int dateStyle,
            int timeStyle,
            Locale locale
    ) {
        DateFormat formatter;
        if (dateFormatDateStyle != null && dateFormatTimeStyle != null) {
            formatter = DateFormat.getDateTimeInstance(dateFormatDateStyle, dateFormatTimeStyle, locale);
        } else if (pattern != null) {
            formatter = new SimpleDateFormat(pattern);
        } else {
            formatter = DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
        }
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return formatter;
    }

    @ParameterizedTest
    @MethodSource("provideTestParameters")
    void testConstructor(
            String constructorType,
            Object[] constructorArgs,
            Locale expectedLocale,
            int expectedDateStyle,
            int expectedTimeStyle,
            Date validDate,
            String expectedPattern,
            Integer dateFormatDateStyle,
            Integer dateFormatTimeStyle
    ) {
        DateTimeStringConverter converter = createConverter(constructorType, constructorArgs);

        assertEquals(expectedLocale, DateTimeStringConverterShim.getLocale(converter));
        assertEquals(expectedDateStyle, DateTimeStringConverterShim.getDateStyle(converter));
        assertEquals(expectedTimeStyle, DateTimeStringConverterShim.getTimeStyle(converter));
        assertEquals(expectedPattern, DateTimeStringConverterShim.getPattern(converter));

        if (dateFormatDateStyle != null) {
            DateFormat dateFormat = DateTimeStringConverterShim.getDateFormatVar(converter);
            assertNotNull(dateFormat);
        }
    }

    @Test
    void getDateFormat_default() {
        DateTimeStringConverter converter = new DateTimeStringConverter();
        assertNotNull(DateTimeStringConverterShim.getDateFormat(converter));
    }

    @Test
    void getDateFormat_nonNullPattern() {
        DateTimeStringConverter converter = new DateTimeStringConverter("yyyy/MM/dd HH:mm:ss");
        assertTrue(DateTimeStringConverterShim.getDateFormat(converter) instanceof SimpleDateFormat);
    }

    @ParameterizedTest
    @MethodSource("provideTestParameters")
    void fromString_testValidInput(
            String constructorType,
            Object[] constructorArgs,
            Locale expectedLocale,
            int expectedDateStyle,
            int expectedTimeStyle,
            Date validDate,
            String expectedPattern,
            Integer dateFormatDateStyle,
            Integer dateFormatTimeStyle
    ) {
        DateTimeStringConverter converter = createConverter(constructorType, constructorArgs);
        DateFormat validFormatter = createValidFormatter(
                dateFormatDateStyle,
                dateFormatTimeStyle,
                expectedPattern,
                expectedDateStyle,
                expectedTimeStyle,
                expectedLocale
        );
        String input = validFormatter.format(validDate);
        assertEquals(validDate, converter.fromString(input), "Input = " + input);
    }

    @ParameterizedTest
    @MethodSource("provideTestParameters")
    void fromString_testValidInputWithWhiteSpace(
            String constructorType,
            Object[] constructorArgs,
            Locale expectedLocale,
            int expectedDateStyle,
            int expectedTimeStyle,
            Date validDate,
            String expectedPattern,
            Integer dateFormatDateStyle,
            Integer dateFormatTimeStyle
    ) {
        DateTimeStringConverter converter = createConverter(constructorType, constructorArgs);
        DateFormat validFormatter = createValidFormatter(
                dateFormatDateStyle,
                dateFormatTimeStyle,
                expectedPattern,
                expectedDateStyle,
                expectedTimeStyle,
                expectedLocale
        );
        String input = validFormatter.format(validDate);
        assertEquals(validDate, converter.fromString("      " + input + "      "), "Input = " + input);
    }

    @ParameterizedTest
    @MethodSource("provideTestParameters")
    void fromString_testInvalidInput(
            String constructorType,
            Object[] constructorArgs,
            Locale expectedLocale,
            int expectedDateStyle,
            int expectedTimeStyle,
            Date validDate,
            String expectedPattern,
            Integer dateFormatDateStyle,
            Integer dateFormatTimeStyle
    ) {
        DateTimeStringConverter converter = createConverter(constructorType, constructorArgs);
        assertThrows(RuntimeException.class, () -> converter.fromString("abcdefg"));
    }

    @ParameterizedTest
    @MethodSource("provideTestParameters")
    void toString_validOutput(
            String constructorType,
            Object[] constructorArgs,
            Locale expectedLocale,
            int expectedDateStyle,
            int expectedTimeStyle,
            Date validDate,
            String expectedPattern,
            Integer dateFormatDateStyle,
            Integer dateFormatTimeStyle
    ) {
        DateTimeStringConverter converter = createConverter(constructorType, constructorArgs);
        DateFormat validFormatter = createValidFormatter(
                dateFormatDateStyle,
                dateFormatTimeStyle,
                expectedPattern,
                expectedDateStyle,
                expectedTimeStyle,
                expectedLocale
        );
        assertEquals(validFormatter.format(validDate), converter.toString(validDate));
    }
}
