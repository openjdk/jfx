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

import java.time.LocalDateTime;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import static org.junit.Assert.*;

import javafx.util.StringConverter;

import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 */
@RunWith(Parameterized.class)
public class LocalDateTimeStringConverterTest {
    private static final LocalDateTime VALID_LDT_WITH_SECONDS    = LocalDateTime.of(1985, 1, 12, 12, 34, 56);
    private static final LocalDateTime VALID_LDT_WITHOUT_SECONDS = LocalDateTime.of(1985, 1, 12, 12, 34, 0);

    private static final DateTimeFormatter aFormatter = DateTimeFormatter.ofPattern("dd MM yyyy HH mm ss");
    private static final DateTimeFormatter aParser = DateTimeFormatter.ofPattern("yyyy MM dd hh mm ss a");

    @Parameterized.Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][] {
            { new LocalDateTimeStringConverter(),
              Locale.getDefault(Locale.Category.FORMAT), FormatStyle.SHORT, FormatStyle.SHORT,
              VALID_LDT_WITHOUT_SECONDS, null, null },

            { new LocalDateTimeStringConverter(aFormatter, aParser),
              Locale.getDefault(Locale.Category.FORMAT), null, null,
              VALID_LDT_WITH_SECONDS, aFormatter, aParser },

            { new LocalDateTimeStringConverter(FormatStyle.SHORT, FormatStyle.SHORT, Locale.UK, IsoChronology.INSTANCE),
              Locale.UK, FormatStyle.SHORT, FormatStyle.SHORT,
              VALID_LDT_WITHOUT_SECONDS, null, null },
        });
    }

    private LocalDateTimeStringConverter converter;
    private Locale locale;
    private FormatStyle dateStyle;
    private FormatStyle timeStyle;
    private DateTimeFormatter formatter, parser;

    private LocalDateTime validDateTime;

    public LocalDateTimeStringConverterTest(LocalDateTimeStringConverter converter, Locale locale, FormatStyle dateStyle, FormatStyle timeStyle, LocalDateTime validDateTime, DateTimeFormatter formatter, DateTimeFormatter parser) {
        this.converter = converter;
        this.locale = locale;
        this.dateStyle = dateStyle;
        this.timeStyle = timeStyle;
        this.validDateTime = validDateTime;
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
        assertEquals((dateStyle != null) ? dateStyle : FormatStyle.SHORT, converter.ldtConverter.dateStyle);
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
            assertEquals(validDateTime, converter.fromString(converter.toString(validDateTime)));
        }
    }
    
    
    @Test(expected=RuntimeException.class)
    public void fromString_testInvalidInput() {
        converter.fromString("abcdefg");
    }

    @Test public void converter_with_specified_formatter_and_parser() {
        String formatPattern = "dd MMMM yyyy, HH:mm:ss";
        String parsePattern = "MMMM dd, yyyy, HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern);
        DateTimeFormatter parser = DateTimeFormatter.ofPattern(parsePattern);
        StringConverter<LocalDateTime> converter = new LocalDateTimeStringConverter(formatter, parser);
        assertEquals("12 January 1985, 12:34:56", converter.toString(VALID_LDT_WITH_SECONDS));
        assertEquals(VALID_LDT_WITH_SECONDS, converter.fromString("January 12, 1985, 12:34:56"));
    }

    @Test public void converter_with_specified_formatter_and_null_parser() {
        String pattern = "dd MMMM yyyy, HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        StringConverter<LocalDateTime> converter = new LocalDateTimeStringConverter(formatter, null);
        assertEquals("12 January 1985, 12:34:56", converter.toString(VALID_LDT_WITH_SECONDS));
        assertEquals(VALID_LDT_WITH_SECONDS, converter.fromString("12 January 1985, 12:34:56"));
    }
}
