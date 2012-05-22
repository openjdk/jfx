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

/**
 */
public class TimeStringConverterTest {
    private TimeStringConverter converter;
    
    private static final Date VALID_TIME;
    private static final String VALID_TIME_STRING = "12:34:56 PM";
    
    static {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("GMT"));
        c.clear();
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 34);
        c.set(Calendar.SECOND, 56);
        VALID_TIME = c.getTime();
    }
    
    @Before public void setup() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        converter = new TimeStringConverter();
    }
    
    /*********************************************************************
     * Test constructors
     ********************************************************************/ 
    
    @Test public void testDefaultConstructor() {
        TimeStringConverter c = new TimeStringConverter();
        assertEquals(Locale.getDefault(), c.locale);
        assertNull(c.pattern);
        assertNull(c.dateFormat);
    }
    
    @Test public void testConstructor_locale() {
        TimeStringConverter c = new TimeStringConverter(Locale.CANADA);
        assertEquals(Locale.CANADA, c.locale);
        assertNull(c.pattern);
        assertNull(c.dateFormat);
    }
    
    @Test public void testConstructor_pattern() {
        TimeStringConverter c = new TimeStringConverter("HH:mm:ss");
        assertEquals(Locale.getDefault(), c.locale);
        assertEquals("HH:mm:ss", c.pattern);
        assertNull(c.dateFormat);
    }
    
    @Test public void testConstructor_locale_pattern() {
        TimeStringConverter c = new TimeStringConverter(Locale.CANADA, "HH:mm:ss");
        assertEquals(Locale.CANADA, c.locale);
        assertEquals("HH:mm:ss", c.pattern);
        assertNull(c.dateFormat);
    }
    
    @Test public void testConstructor_numberFormat() {
        DateFormat format = DateFormat.getTimeInstance();
        TimeStringConverter c = new TimeStringConverter(format);
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
        converter = new TimeStringConverter("yyyy");
        assertTrue(converter.getDateFormat() instanceof SimpleDateFormat);
    }
    
    @Test public void getDateFormat_nonNullNumberFormat() {
        DateFormat format = DateFormat.getTimeInstance();
        converter = new TimeStringConverter(format);
        assertEquals(format, converter.getDateFormat());
    }
    
    
    /*********************************************************************
     * Test toString / fromString methods
     ********************************************************************/    
    
    @Test public void fromString_testValidInput() {
        Locale.setDefault(Locale.US);
        assertEquals(VALID_TIME, converter.fromString(VALID_TIME_STRING));
    }
    
    @Test public void fromString_testValidInputWithWhiteSpace() {
        Locale.setDefault(Locale.US);
        assertEquals(VALID_TIME, converter.fromString("      " + VALID_TIME_STRING + "      "));
    }
    
    @Test public void toString_validInput() {
        Locale.setDefault(Locale.US);
        assertEquals(VALID_TIME_STRING, converter.toString(VALID_TIME));
    }
}
