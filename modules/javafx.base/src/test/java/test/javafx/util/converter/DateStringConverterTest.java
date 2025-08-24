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

import javafx.util.converter.DateStringConverter;
import javafx.util.converter.DateTimeStringConverterShim;

public class DateStringConverterTest {

    private static final Locale DEFALUT_LOCALE = Locale.getDefault(Locale.Category.FORMAT);
    private static final Date VALID_DATE;

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, 1985);
        c.set(Calendar.MONTH, Calendar.JANUARY);
        c.set(Calendar.DAY_OF_MONTH, 12);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        VALID_DATE = c.getTime();
    }

    private record TestCase(DateStringConverter converter, Locale locale, int dateStyle, String pattern, DateFormat dateFormat) {}

    private static Collection<TestCase> implementations() {
        return List.of(
                new TestCase(new DateStringConverter(), DEFALUT_LOCALE, DateFormat.DEFAULT, null, null),
                new TestCase(new DateStringConverter(DateFormat.SHORT), DEFALUT_LOCALE, DateFormat.SHORT, null, null),
                new TestCase(new DateStringConverter(Locale.UK), Locale.UK, DateFormat.DEFAULT, null, null),
                new TestCase(new DateStringConverter(Locale.UK, DateFormat.SHORT), Locale.UK, DateFormat.SHORT, null, null),
                new TestCase(new DateStringConverter("dd MM yyyy"), DEFALUT_LOCALE, DateFormat.DEFAULT, "dd MM yyyy", null),
                new TestCase(new DateStringConverter(DateFormat.getDateInstance(DateFormat.LONG)),
                        DEFALUT_LOCALE, DateFormat.DEFAULT, null, DateFormat.getDateInstance(DateFormat.LONG))
                );
    }

    private static DateFormat createFormatter(TestCase testCase) {
        if (testCase.dateFormat() != null) {
            return testCase.dateFormat();
        }
        DateFormat validFormatter;
        if (testCase.pattern() != null) {
            validFormatter = new SimpleDateFormat(testCase.pattern(), testCase.locale());
        } else {
            validFormatter = DateFormat.getDateInstance(testCase.dateStyle(), testCase.locale());
        }
        validFormatter.setLenient(false);
        return validFormatter;
    }

    @ParameterizedTest
    @MethodSource("implementations")
    void testConstructor(TestCase testCase) {
        DateFormat validFormatter = createFormatter(testCase);
        assertEquals(validFormatter, DateTimeStringConverterShim.getDateFormat(testCase.converter()));
    }

    @Test
    void getDateFormat_nonNullPattern() {
        var converter = new DateStringConverter("yyyy");
        assertTrue(DateTimeStringConverterShim.getDateFormat(converter) instanceof SimpleDateFormat);
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void fromString_testValidInput(TestCase testCase) {
        DateFormat validFormatter = createFormatter(testCase);
        String input = validFormatter.format(VALID_DATE);
        assertEquals(VALID_DATE, testCase.converter().fromString(input), "Input = " + input);
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void fromString_testValidInputWithWhiteSpace(TestCase testCase) {
        DateFormat validFormatter = createFormatter(testCase);
        String input = validFormatter.format(VALID_DATE);
        assertEquals(VALID_DATE, testCase.converter().fromString("      " + input + "      "), "Input = " + input);
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void fromString_testInvalidInput(TestCase testCase) {
        assertThrows(RuntimeException.class, () -> testCase.converter().fromString("abcdefg"));
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void toString_validOutput(TestCase testCase) {
        DateFormat validFormatter = createFormatter(testCase);
        assertEquals(validFormatter.format(VALID_DATE), testCase.converter().toString(VALID_DATE));
    }
}
