/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import javafx.util.converter.DateTimeStringConverterShim;
import javafx.util.converter.TimeStringConverter;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 */
@RunWith(Parameterized.class)
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

    @Parameterized.Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][] {
            { new TimeStringConverter(),
              Locale.getDefault(Locale.Category.FORMAT), DateFormat.DEFAULT,
              VALID_TIME_WITH_SECONDS, null, null },

            { new TimeStringConverter(DateFormat.SHORT),
              Locale.getDefault(Locale.Category.FORMAT), DateFormat.SHORT,
              VALID_TIME_WITHOUT_SECONDS, null, null },

            { new TimeStringConverter(Locale.UK),
              Locale.UK, DateFormat.DEFAULT,
              VALID_TIME_WITH_SECONDS, null, null },

            { new TimeStringConverter(Locale.UK, DateFormat.SHORT),
              Locale.UK, DateFormat.SHORT,
              VALID_TIME_WITHOUT_SECONDS, null, null },

            { new TimeStringConverter("HH mm ss"),
              Locale.getDefault(Locale.Category.FORMAT), DateFormat.DEFAULT,
              VALID_TIME_WITH_SECONDS, "HH mm ss", null },

            { new TimeStringConverter(DateFormat.getTimeInstance(DateFormat.FULL)),
              Locale.getDefault(Locale.Category.FORMAT), DateFormat.DEFAULT,
              VALID_TIME_WITH_SECONDS, null, DateFormat.getTimeInstance(DateFormat.FULL) },
        });
    }

    private TimeStringConverter converter;
    private Locale locale;
    private int timeStyle;
    private String pattern;
    private DateFormat dateFormat;
    private Date validDate;
    private DateFormat validFormatter;

    public TimeStringConverterTest(TimeStringConverter converter, Locale locale, int timeStyle, Date validDate, String pattern, DateFormat dateFormat) {
        this.converter = converter;
        this.locale = locale;
        this.timeStyle = timeStyle;
        this.validDate = validDate;
        this.pattern = pattern;
        this.dateFormat = dateFormat;

        if (dateFormat != null) {
            validFormatter = dateFormat;
        } else if (pattern != null) {
            validFormatter = new SimpleDateFormat(pattern);
        } else {
            validFormatter = DateFormat.getTimeInstance(timeStyle, locale);
        }
    }

    @Before public void setup() {
    }

    /*********************************************************************
     * Test constructors
     ********************************************************************/

    @Test public void testConstructor() {
        assertEquals(locale, DateTimeStringConverterShim.getLocale(converter));
        assertEquals(timeStyle, DateTimeStringConverterShim.getTimeStyle(converter));
        assertEquals(pattern, DateTimeStringConverterShim.getPattern(converter));
        assertEquals(dateFormat, DateTimeStringConverterShim.getDateFormatVar(converter));
    }


    /*********************************************************************
     * Test methods
     ********************************************************************/

    @Test public void getDateFormat() {
        assertNotNull(DateTimeStringConverterShim.getDateFormat(converter));
    }

    @Test public void getDateFormat_nonNullPattern() {
        converter = new TimeStringConverter("HH");
        assertTrue(DateTimeStringConverterShim.getDateFormat(converter)
                instanceof SimpleDateFormat);
    }


    /*********************************************************************
     * Test toString / fromString methods
     ********************************************************************/

    @Test public void fromString_testValidInput() {
        String input = validFormatter.format(validDate);
        assertEquals("Input = "+input, validDate, converter.fromString(input));
    }

    @Test public void fromString_testValidInputWithWhiteSpace() {
        String input = validFormatter.format(validDate);
        assertEquals("Input = "+input, validDate, converter.fromString("      " + input + "      "));
    }

    @Test(expected=RuntimeException.class)
    public void fromString_testInvalidInput() {
        converter.fromString("abcdefg");
    }

    @Test public void toString_validOutput() {
        assertEquals(validFormatter.format(validDate), converter.toString(validDate));
    }
}
