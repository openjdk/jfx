/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 */
@RunWith(Parameterized.class)
public class DateStringConverterTest {
    private static final Date VALID_DATE;

    static {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("GMT"));
        c.set(Calendar.YEAR, 1985);
        c.set(Calendar.MONTH, Calendar.JANUARY);
        c.set(Calendar.DAY_OF_MONTH, 12);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        VALID_DATE = c.getTime();
    }
    
    @Parameterized.Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][] {
            { new DateStringConverter(),
              Locale.getDefault(Locale.Category.FORMAT), DateFormat.DEFAULT,
              VALID_DATE, null, null },

            { new DateStringConverter(DateFormat.SHORT),
              Locale.getDefault(Locale.Category.FORMAT), DateFormat.SHORT,
              VALID_DATE, null, null },

            { new DateStringConverter(Locale.UK),
              Locale.UK, DateFormat.DEFAULT,
              VALID_DATE, null, null },

            { new DateStringConverter(Locale.UK, DateFormat.SHORT),
              Locale.UK, DateFormat.SHORT,
              VALID_DATE, null, null },

            { new DateStringConverter("dd MM yyyy"),
              Locale.getDefault(Locale.Category.FORMAT), DateFormat.DEFAULT,
              VALID_DATE, "dd MM yyyy", null },

            { new DateStringConverter(DateFormat.getDateInstance(DateFormat.LONG)),
              Locale.getDefault(Locale.Category.FORMAT), DateFormat.DEFAULT,
              VALID_DATE, null, DateFormat.getDateInstance(DateFormat.LONG) },
        });
    }

    private DateStringConverter converter;
    private Locale locale;
    private int dateStyle;
    private String pattern;
    private DateFormat dateFormat;
    private Date validDate;
    private DateFormat validFormatter;
    
    public DateStringConverterTest(DateStringConverter converter, Locale locale, int dateStyle, Date validDate, String pattern, DateFormat dateFormat) {
        this.converter = converter;
        this.locale = locale;
        this.dateStyle = dateStyle;
        this.validDate = validDate;
        this.pattern = pattern;
        this.dateFormat = dateFormat;

        if (dateFormat != null) {
            validFormatter = dateFormat;
        } else if (pattern != null) {
            validFormatter = new SimpleDateFormat(pattern);
        } else {
            validFormatter = DateFormat.getDateInstance(dateStyle, locale);
        }
    }
    
    @Before public void setup() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }
    
    /*********************************************************************
     * Test constructors
     ********************************************************************/ 
    
    @Test public void testConstructor() {
        assertEquals(locale, converter.locale);
        assertEquals(dateStyle, converter.dateStyle);
        assertEquals(pattern, converter.pattern);
        assertEquals(dateFormat, converter.dateFormat);
    }
    
    
    /*********************************************************************
     * Test methods
     ********************************************************************/   
    
    @Test public void getDateFormat() {
        assertNotNull(converter.getDateFormat());
    }
    
    @Test public void getDateFormat_nonNullPattern() {
        converter = new DateStringConverter("yyyy");
        assertTrue(converter.getDateFormat() instanceof SimpleDateFormat);
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
