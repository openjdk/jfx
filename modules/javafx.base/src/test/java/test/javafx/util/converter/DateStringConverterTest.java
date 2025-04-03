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

import static org.junit.jupiter.api.Assertions.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;
import javafx.util.converter.DateStringConverter;
import javafx.util.converter.DateTimeStringConverterShim;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

class DateStringConverterTest {

    private static final Date VALID_DATE;
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        Calendar c = Calendar.getInstance();
        c.set(1985, Calendar.JANUARY, 12, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        VALID_DATE = c.getTime();
    }

    private DateStringConverter converter;
    private DateFormat validFormatter;

    @BeforeEach
    void setup() {}

    static Stream<Arguments> implementations() {
        return Stream.of(
                Arguments.of(new DateStringConverter(), Locale.getDefault(Locale.Category.FORMAT), DateFormat.DEFAULT, VALID_DATE, null, null),
                Arguments.of(new DateStringConverter(DateFormat.SHORT), Locale.getDefault(Locale.Category.FORMAT), DateFormat.SHORT, VALID_DATE, null, null),
                Arguments.of(new DateStringConverter(Locale.UK), Locale.UK, DateFormat.DEFAULT, VALID_DATE, null, null),
                Arguments.of(new DateStringConverter(Locale.UK, DateFormat.SHORT), Locale.UK, DateFormat.SHORT, VALID_DATE, null, null),
                Arguments.of(new DateStringConverter("dd MM yyyy"), Locale.getDefault(Locale.Category.FORMAT), DateFormat.DEFAULT, VALID_DATE, "dd MM yyyy", null),
                Arguments.of(new DateStringConverter(DateFormat.getDateInstance(DateFormat.LONG)), Locale.getDefault(Locale.Category.FORMAT), DateFormat.DEFAULT, VALID_DATE, null, DateFormat.getDateInstance(DateFormat.LONG))
        );
    }

    @ParameterizedTest
    @MethodSource("implementations")
    void testConstructor(DateStringConverter converter, Locale locale, int dateStyle, Date validDate, String pattern, DateFormat dateFormat) {
        assertEquals(locale, DateTimeStringConverterShim.getLocale(converter));
        assertEquals(dateStyle, DateTimeStringConverterShim.getDateStyle(converter));
        assertEquals(pattern, DateTimeStringConverterShim.getPattern(converter));
        assertEquals(dateFormat, DateTimeStringConverterShim.getDateFormatVar(converter));
    }

    @Test
    void fromString_testInvalidInput() {
        assertThrows(RuntimeException.class, () -> converter.fromString("abcdefg"));
    }
}

