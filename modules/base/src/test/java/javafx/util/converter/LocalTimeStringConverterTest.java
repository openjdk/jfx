/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.time.LocalTime;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import javafx.util.StringConverter;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 */
@RunWith(Parameterized.class)
public class LocalTimeStringConverterTest {
    private static final LocalTime VALID_TIME_WITH_SECONDS;
    private static final LocalTime VALID_TIME_WITHOUT_SECONDS;

    static {
        VALID_TIME_WITH_SECONDS = LocalTime.of(12, 34, 56);
        VALID_TIME_WITHOUT_SECONDS = LocalTime.of(12, 34, 0);
    }
    
    private static final DateTimeFormatter aFormatter = DateTimeFormatter.ofPattern("HH mm ss");
    private static final DateTimeFormatter aParser = DateTimeFormatter.ofPattern("hh mm ss a");


    @Parameterized.Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][] {
            { new LocalTimeStringConverter(),
              Locale.getDefault(Locale.Category.FORMAT), FormatStyle.SHORT,
              VALID_TIME_WITHOUT_SECONDS, null, null },

            { new LocalTimeStringConverter(aFormatter, aParser),
              Locale.getDefault(Locale.Category.FORMAT), null,
              VALID_TIME_WITH_SECONDS, aFormatter, aParser },

            { new LocalTimeStringConverter(FormatStyle.SHORT, Locale.UK),
              Locale.UK, FormatStyle.SHORT,
              VALID_TIME_WITHOUT_SECONDS, null, null },
        });
    }

    private LocalTimeStringConverter converter;
    private Locale locale;
    private FormatStyle timeStyle;
    private DateTimeFormatter formatter, parser;
    private LocalTime validTime;

    public LocalTimeStringConverterTest(LocalTimeStringConverter converter, Locale locale, FormatStyle timeStyle, LocalTime validTime, DateTimeFormatter formatter, DateTimeFormatter parser) {
        this.converter = converter;
        this.locale = locale;
        this.timeStyle = timeStyle;
        this.validTime = validTime;
        this.formatter = formatter;
        this.parser = parser;
    }
    
    @Before public void setup() {
    }
    
    /*********************************************************************
     * Test constructors
     ********************************************************************/ 
    
    @Test public void testConstructor() {
        assertEquals(locale, converter.ldtConverter.locale);
        assertNull(converter.ldtConverter.dateStyle);
        assertEquals((timeStyle != null) ? timeStyle : FormatStyle.SHORT, converter.ldtConverter.timeStyle);
        if (formatter != null) {
            assertEquals(formatter, converter.ldtConverter.formatter);
        }
        if (parser != null) {
            assertEquals(parser, converter.ldtConverter.parser);
        } else if (formatter != null) {
            assertEquals(formatter, converter.ldtConverter.parser);
        }
    }
    
    
    
    /*********************************************************************
     * Test toString / fromString methods
     ********************************************************************/    
    
    @Test public void toString_to_fromString_testRoundtrip() {
        if (formatter == null) {
            // Only the default formatter/parser can guarantee roundtrip symmetry
            assertEquals(validTime, converter.fromString(converter.toString(validTime)));
        }
    }
    
    @Test(expected=RuntimeException.class)
    public void fromString_testInvalidInput() {
        converter.fromString("abcdefg");
    }
}
