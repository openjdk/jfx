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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javafx.util.converter.DateTimeStringConverter;
import javafx.util.converter.DateTimeStringConverterShim;

public class DateTimeStringConverterTest {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault(Locale.Category.FORMAT);
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

    private record TestCase(DateTimeStringConverter converter, Locale locale, int dateStyle, int timetyle, String pattern,
            DateFormat dateFormat, Date validDate) {}

    private static Collection<TestCase> implementations() {
        return List.of(
                new TestCase(new DateTimeStringConverter(),
                        DEFAULT_LOCALE, DateFormat.DEFAULT, DateFormat.DEFAULT,
                        null, null, VALID_DATE_WITH_SECONDS
                ),

                new TestCase(new DateTimeStringConverter(DateFormat.SHORT, DateFormat.SHORT),
                        DEFAULT_LOCALE, DateFormat.SHORT, DateFormat.SHORT,
                        null, null, VALID_DATE_WITHOUT_SECONDS
                ),

                new TestCase(new DateTimeStringConverter(Locale.UK),
                        Locale.UK, DateFormat.DEFAULT, DateFormat.DEFAULT,
                        null, null, VALID_DATE_WITH_SECONDS
                ),

                new TestCase(new DateTimeStringConverter(Locale.UK, DateFormat.SHORT, DateFormat.SHORT),
                        Locale.UK, DateFormat.SHORT, DateFormat.SHORT,
                        null, null, VALID_DATE_WITHOUT_SECONDS
                ),

                new TestCase(new DateTimeStringConverter("dd MM yyyy HH mm ss"),
                        DEFAULT_LOCALE, DateFormat.DEFAULT, DateFormat.DEFAULT,
                        "dd MM yyyy HH mm ss", null, VALID_DATE_WITH_SECONDS
                ),

                new TestCase(new DateTimeStringConverter(DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.FULL)),
                        DEFAULT_LOCALE, DateFormat.DEFAULT, DateFormat.DEFAULT,
                        null, DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.FULL), VALID_DATE_WITH_SECONDS
                )
        );
    }

    private static DateFormat create(TestCase testCase) {
        if (testCase.dateFormat() != null) {
            return testCase.dateFormat();
        }
        DateFormat validFormatter;
        if (testCase.pattern() != null) {
            validFormatter = new SimpleDateFormat(testCase.pattern(), testCase.locale());
        } else {
            validFormatter = DateFormat.getDateTimeInstance(testCase.dateStyle(), testCase.timetyle(), testCase.locale());
        }
        validFormatter.setLenient(false);
        return validFormatter;
    }

    @ParameterizedTest
    @MethodSource("implementations")
    void testConstructor(TestCase testCase) {
        DateFormat validFormatter = create(testCase);
        assertEquals(validFormatter, DateTimeStringConverterShim.getDateFormat(testCase.converter()));
    }

    @Test
    void getDateFormat_nonNullPattern() {
        var converter = new DateTimeStringConverter("yyyy/MM/dd HH:mm:ss");
        assertTrue(DateTimeStringConverterShim.getDateFormat(converter) instanceof SimpleDateFormat);
    }

    @ParameterizedTest
    @MethodSource("implementations")
    void fromString_testValidInput(TestCase testCase) {
        DateFormat validFormatter = create(testCase);
        String input = validFormatter.format(testCase.validDate());
        assertEquals(testCase.validDate(), testCase.converter().fromString(input), "Input = " + input);
    }

    @ParameterizedTest
    @MethodSource("implementations")
    void fromString_testValidInputWithWhiteSpace(TestCase testCase) {
        DateFormat validFormatter = create(testCase);
        String input = validFormatter.format(testCase.validDate());
        assertEquals(testCase.validDate(), testCase.converter().fromString("      " + input + "      "), "Input = " + input);
    }

    @ParameterizedTest
    @MethodSource("implementations")
    void fromString_testInvalidInput(TestCase testCase) {
        assertThrows(RuntimeException.class, () -> testCase.converter().fromString("abcdefg"));
    }

    @ParameterizedTest
    @MethodSource("implementations")
    void toString_validOutput(TestCase testCase) {
        DateFormat validFormatter = create(testCase);
        assertEquals(validFormatter.format(testCase.validDate()), testCase.converter().toString(testCase.validDate()));
    }
}
