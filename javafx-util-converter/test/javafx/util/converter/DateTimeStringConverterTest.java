/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.text.*;
import java.util.Calendar;
import java.util.Date;

import java.util.Locale;
import java.util.TimeZone;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 */
public class DateTimeStringConverterTest {
    private DateTimeStringConverter converter;
    
    private static final Date VALID_DATE;
    private static final String VALID_DATE_STRING_MDY = "12/01/1985 12:34:56 PM"; 
    
    static {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("GMT"));
        c.set(Calendar.YEAR, 1985);
        c.set(Calendar.MONTH, Calendar.JANUARY);
        c.set(Calendar.DAY_OF_MONTH, 12);
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 34);
        c.set(Calendar.SECOND, 56);
        c.set(Calendar.MILLISECOND, 0);
        VALID_DATE = c.getTime();
    }
    
    @Before public void setup() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        converter = new DateTimeStringConverter();
    }
    
    /*********************************************************************
     * Test constructors
     ********************************************************************/ 
    
    @Test public void testDefaultConstructor() {
        DateTimeStringConverter c = new DateTimeStringConverter();
        assertEquals(Locale.getDefault(), c.locale);
        assertNull(c.pattern);
        assertNull(c.dateFormat);
    }
    
    @Test public void testConstructor_locale() {
        DateTimeStringConverter c = new DateTimeStringConverter(Locale.CANADA);
        assertEquals(Locale.CANADA, c.locale);
        assertNull(c.pattern);
        assertNull(c.dateFormat);
    }
    
    @Test public void testConstructor_pattern() {
        DateTimeStringConverter c = new DateTimeStringConverter("yyyy/MM/dd HH:mm:ss");
        assertEquals(Locale.getDefault(), c.locale);
        assertEquals("yyyy/MM/dd HH:mm:ss", c.pattern);
        assertNull(c.dateFormat);
    }
    
    @Test public void testConstructor_locale_pattern() {
        DateTimeStringConverter c = new DateTimeStringConverter(Locale.CANADA, "yyyy/MM/dd HH:mm:ss");
        assertEquals(Locale.CANADA, c.locale);
        assertEquals("yyyy/MM/dd HH:mm:ss", c.pattern);
        assertNull(c.dateFormat);
    }
    
    @Test public void testConstructor_numberFormat() {
        DateFormat format = DateFormat.getDateTimeInstance();
        DateTimeStringConverter c = new DateTimeStringConverter(format);
        assertNull(c.locale);
        assertNull(c.pattern);
        assertEquals(format, c.dateFormat);
    }
    

    /*********************************************************************
     * Test methods
     ********************************************************************/   
    
    @Test public void getNumberFormat_default() {
        assertNotNull(converter.getDateFormat());
    }
    
    @Test public void getNumberFormat_nonNullPattern() {
        converter = new DateTimeStringConverter("yyyy/MM/dd HH:mm:ss");
        assertTrue(converter.getDateFormat() instanceof SimpleDateFormat);
    }
    
    @Test public void getNumberFormat_nonNullNumberFormat() {
        DateFormat format = DateFormat.getDateTimeInstance();
        converter = new DateTimeStringConverter(format);
        assertEquals(format, converter.getDateFormat());
    }
    
    
    /*********************************************************************
     * Test toString / fromString methods
     ********************************************************************/    
    
    @Test public void fromString_testValidInput() {
        assertEquals(VALID_DATE, converter.fromString(VALID_DATE_STRING_MDY));
    }
    
    @Test public void fromString_testValidInputWithWhiteSpace() {
        assertEquals(VALID_DATE, converter.fromString("      " + VALID_DATE_STRING_MDY + "      "));
    }
    
    @Test(expected=RuntimeException.class)
    public void fromString_testInvalidInput() {
        converter.fromString("abcdefg");
    }
    
    @Test public void toString_validInput() {
        assertEquals(VALID_DATE_STRING_MDY, converter.toString(VALID_DATE));
    }    
}
