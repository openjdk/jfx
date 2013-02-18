/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

/**
 */
public class DateStringConverterTest {
    private DateStringConverter converter;
    
    private static final Date VALID_DATE;
    private static final String VALID_DATE_STRING_MDY = "12/01/1985"; // 12th January 1985
//    private static final String VALID_DATE_STRING_YMD = "1985/01/12"; // 12th January 1985
    
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
//        
//        c.set(Calendar.HOUR_OF_DAY, 12);
//        c.set(Calendar.MINUTE, 34);
//        c.set(Calendar.SECOND, 56);
//        VALID_DATE_FULL = c.getTime();
    }
    
    @Before public void setup() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        converter = new DateStringConverter();
    }
    
    /*********************************************************************
     * Test constructors
     ********************************************************************/ 
    
    @Test public void testDefaultConstructor() {
        DateStringConverter c = new DateStringConverter();
        assertEquals(Locale.getDefault(), c.locale);
        assertNull(c.pattern);
        assertNull(c.dateFormat);
    }
    
    @Test public void testConstructor_locale() {
        DateStringConverter c = new DateStringConverter(Locale.CANADA);
        assertEquals(Locale.CANADA, c.locale);
        assertNull(c.pattern);
        assertNull(c.dateFormat);
    }
    
    @Test public void testConstructor_pattern() {
        DateStringConverter c = new DateStringConverter("yyyy/MM/dd");
        assertEquals(Locale.getDefault(), c.locale);
        assertEquals("yyyy/MM/dd", c.pattern);
        assertNull(c.dateFormat);
    }
    
    @Test public void testConstructor_locale_pattern() {
        DateStringConverter c = new DateStringConverter(Locale.CANADA, "yyyy/MM/dd");
        assertEquals(Locale.CANADA, c.locale);
        assertEquals("yyyy/MM/dd", c.pattern);
        assertNull(c.dateFormat);
    }
    
    @Test public void testConstructor_numberFormat() {
        DateFormat format = DateFormat.getDateInstance();
        DateStringConverter c = new DateStringConverter(format);
        assertNull(c.locale);
        assertNull(c.pattern);
        assertEquals(format, c.dateFormat);
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
    
    @Test public void getDateFormat_nonNullNumberFormat() {
        DateFormat format = DateFormat.getDateInstance();
        converter = new DateStringConverter(format);
        assertEquals(format, converter.getDateFormat());
    }
    
    
    /*********************************************************************
     * Test toString / fromString methods
     ********************************************************************/    
    
    @Ignore
    @Test public void fromString_testValidInput() {
        assertEquals(VALID_DATE, converter.fromString(VALID_DATE_STRING_MDY));
    }
    
    @Ignore
    @Test public void fromString_testValidInputWithWhiteSpace() {
        assertEquals(VALID_DATE, converter.fromString("      " + VALID_DATE_STRING_MDY + "      "));
    }
    
    @Ignore
    @Test public void toString_validInput() {
        assertEquals(VALID_DATE_STRING_MDY, converter.toString(VALID_DATE));
    }
}
