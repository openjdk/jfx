/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.chrono.IsoChronology;
import java.time.chrono.JapaneseChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import static org.junit.Assert.*;

import javafx.util.StringConverter;
import javafx.util.converter.LocalDateTimeStringConverter;
import javafx.util.converter.LocalDateTimeStringConverterShim;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 */
@RunWith(Parameterized.class)
public class LocalDateTimeStringConverterTest {

    private static final String JAPANESE_DATE_STRING = "Saturday, January 12, 60 Shōwa, 12:34:56 PM";
    private static final LocalDateTime VALID_LDT_WITH_SECONDS    = LocalDateTime.of(1985, 1, 12, 12, 34, 56);
    private static final LocalDateTime VALID_LDT_WITHOUT_SECONDS = LocalDateTime.of(1985, 1, 12, 12, 34, 0);

    private static DateTimeFormatter aFormatter;
    private static DateTimeFormatter aParser;
    private static Locale oldLocale;

    // We can only create LocalDateTimeStringConverter object after Locale is set.
    // Unfortunately, due to unpredictability of @Parameterized.Parameters methods
    // in JUnit, we have to allocate it after @BeforeClass sets up Locale and
    // necessary static fields. Otherwise, the test may collide with other
    // Local*StringConverter tests and cause unpredictable results.
    private enum LocalDateTimeStringConverterVariant {
        NO_PARAM,
        WITH_FORMATTER_PARSER,
        WITH_FORMAT_STYLES,
    };

    @Parameterized.Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][] {
            { LocalDateTimeStringConverterVariant.NO_PARAM,
              FormatStyle.SHORT, FormatStyle.SHORT, VALID_LDT_WITHOUT_SECONDS},

            { LocalDateTimeStringConverterVariant.WITH_FORMATTER_PARSER,
              null, null, VALID_LDT_WITH_SECONDS},

            { LocalDateTimeStringConverterVariant.WITH_FORMAT_STYLES,
              FormatStyle.SHORT, FormatStyle.SHORT, VALID_LDT_WITHOUT_SECONDS},
        });
    }

    private LocalDateTimeStringConverterVariant converterVariant;
    private FormatStyle dateStyle;
    private FormatStyle timeStyle;
    private LocalDateTime validDateTime;

    private LocalDateTimeStringConverter converter;
    private Locale locale;
    private DateTimeFormatter formatter, parser;


    public LocalDateTimeStringConverterTest(LocalDateTimeStringConverterVariant converterVariant, FormatStyle dateStyle, FormatStyle timeStyle, LocalDateTime validDateTime) {
        this.converterVariant = converterVariant;
        this.dateStyle = dateStyle;
        this.timeStyle = timeStyle;
        this.validDateTime = validDateTime;

        this.converter = null;
        this.locale = null;
        this.formatter = null;
        this.parser = null;
    }

    @BeforeClass
    public static void setupBeforeAll() {
        // Tests require that default locale is en_US
        oldLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);

        // DateTimeFormatter uses default locale, so we can init this after updating locale
        aFormatter = DateTimeFormatter.ofPattern("dd MM yyyy HH mm ss");
        aParser = DateTimeFormatter.ofPattern("yyyy MM dd hh mm ss a");
    }

    @AfterClass
    public static void teardownAfterAll() {
        // Restore VM's old locale
        Locale.setDefault(oldLocale);
    }

    @Before
    public void setup() {
        // Locale is established now, so we can allocate objects depending on it
        switch (this.converterVariant) {
        case NO_PARAM:
            this.converter = new LocalDateTimeStringConverter();
            this.locale = Locale.getDefault(Locale.Category.FORMAT);
            this.formatter = null;
            this.parser = null;
            break;
        case WITH_FORMATTER_PARSER:
            this.converter = new LocalDateTimeStringConverter(aFormatter, aParser);
            this.locale = Locale.getDefault(Locale.Category.FORMAT);
            this.formatter = aFormatter;
            this.parser = aParser;
            break;
        case WITH_FORMAT_STYLES:
            this.converter = new LocalDateTimeStringConverter(FormatStyle.SHORT, FormatStyle.SHORT, Locale.UK, IsoChronology.INSTANCE);
            this.locale = Locale.UK;
            this.formatter = null;
            this.parser = null;
            break;
        default:
            fail("Invalid converter variant: " + this.converterVariant.toString());
        }
    }

    /*********************************************************************
     * Test constructors
     ********************************************************************/

    @Test public void testConstructor() {
        assertEquals(locale,
                LocalDateTimeStringConverterShim.getldtConverterLocale(converter));
        assertEquals((dateStyle != null) ? dateStyle : FormatStyle.SHORT,
                LocalDateTimeStringConverterShim.getldtConverterDateStyle(converter));
        assertEquals((timeStyle != null) ? timeStyle : FormatStyle.SHORT,
                LocalDateTimeStringConverterShim.getldtConverterTimeStyle(converter));
        if (formatter != null) {
            assertEquals(formatter,
                LocalDateTimeStringConverterShim.getldtConverterFormatter(converter));
        }
        if (parser != null) {
            assertEquals(parser,
                LocalDateTimeStringConverterShim.getldtConverterParser(converter));
        } else if (formatter != null) {
            assertEquals(formatter,
                LocalDateTimeStringConverterShim.getldtConverterFormatter(converter));
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

    @Test
    public void testChronologyConsistency() {
        var converter = new LocalDateTimeStringConverter(FormatStyle.FULL, FormatStyle.MEDIUM, null, JapaneseChronology.INSTANCE);
        assertEquals(JAPANESE_DATE_STRING, converter.toString(VALID_LDT_WITH_SECONDS));
        // force a chronology change with an invalid Japanese date
        try {
            converter.toString(LocalDateTime.of(1, 1, 1, 1, 1, 1));
        } catch (DateTimeException e) {}
        assertEquals(VALID_LDT_WITH_SECONDS, converter.fromString(JAPANESE_DATE_STRING));
    }
}
