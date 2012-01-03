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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 */
public class CurrencyStringConverterTest {
    private CurrencyStringConverter converter;
    
    @Before public void setup() {
        converter = new CurrencyStringConverter();
    }
    
    /*********************************************************************
     * Test constructors
     ********************************************************************/ 
    
    @Test public void testDefaultConstructor() {
        CurrencyStringConverter c = new CurrencyStringConverter();
        assertEquals(Locale.getDefault(), c.locale);
        assertNull(c.pattern);
        assertNull(c.numberFormat);
    }
    
    @Test public void testConstructor_locale() {
        CurrencyStringConverter c = new CurrencyStringConverter(Locale.CANADA);
        assertEquals(Locale.CANADA, c.locale);
        assertNull(c.pattern);
        assertNull(c.numberFormat);
    }
    
    @Test public void testConstructor_pattern() {
        CurrencyStringConverter c = new CurrencyStringConverter("#,##,###,####");
        assertEquals(Locale.getDefault(), c.locale);
        assertEquals("#,##,###,####", c.pattern);
        assertNull(c.numberFormat);
    }
    
    @Test public void testConstructor_locale_pattern() {
        CurrencyStringConverter c = new CurrencyStringConverter(Locale.CANADA, "#,##,###,####");
        assertEquals(Locale.CANADA, c.locale);
        assertEquals("#,##,###,####", c.pattern);
        assertNull(c.numberFormat);
    }
    
    @Test public void testConstructor_numberFormat() {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.JAPAN);
        CurrencyStringConverter c = new CurrencyStringConverter(format);
        assertNull(c.locale);
        assertNull(c.pattern);
        assertEquals(format, c.numberFormat);
    }
    
    
    /*********************************************************************
     * Test methods
     ********************************************************************/   
    
    @Test public void getNumberFormat_default() {
        assertNotNull(converter.getNumberFormat());
    }
    
    @Test public void getNumberFormat_nonNullPattern() {
        converter = new CurrencyStringConverter("#,##,###,####");
        assertTrue(converter.getNumberFormat() instanceof DecimalFormat);
    }
    
    @Test public void getNumberFormat_nonNullNumberFormat() {
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        converter = new CurrencyStringConverter(nf);
        assertEquals(nf, converter.getNumberFormat());
    }
    
    
    /*********************************************************************
     * Test toString / fromString methods
     ********************************************************************/    
    
    @Test public void fromString_testValidStringInput() {
        assertEquals(10.32, converter.fromString("$10.32"));
    }
    
    @Test public void fromString_testValidStringInputWithWhiteSpace() {
        assertEquals(10.32, converter.fromString("      $10.32      "));
    }
    
    @Test public void toString_validInput() {
        assertEquals("$10.32", converter.toString(10.32));
    }
}
