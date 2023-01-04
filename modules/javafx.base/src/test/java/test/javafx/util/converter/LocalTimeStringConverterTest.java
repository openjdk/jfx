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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import javafx.util.converter.LocalTimeStringConverterShim;
import javafx.util.converter.LocalTimeStringConverter;

import static org.junit.Assert.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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

    private static Locale oldLocale = null;
    private static DateTimeFormatter aFormatter = null;
    private static DateTimeFormatter aParser = null;

    // We can only create LocalTimeStringConverter object after Locale is set.
    // Unfortunately, due to unpredictability of @Parameterized.Parameters methods
    // in JUnit, we have to allocate it after @BeforeClass sets up Locale and
    // necessary static fields. Otherwise, the test may collide with other
    // Local*StringConverter tests and cause unpredictable results.
    private enum LocalTimeStringConverterVariant {
        NO_PARAM,
        WITH_FORMATTER_PARSER,
        WITH_FORMAT_STYLES,
    };

    @Parameterized.Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][] {
            { LocalTimeStringConverterVariant.NO_PARAM,
              FormatStyle.SHORT, VALID_TIME_WITHOUT_SECONDS },

            { LocalTimeStringConverterVariant.WITH_FORMATTER_PARSER,
              null, VALID_TIME_WITH_SECONDS },

            { LocalTimeStringConverterVariant.WITH_FORMAT_STYLES,
              FormatStyle.SHORT, VALID_TIME_WITHOUT_SECONDS },
        });
    }

    private LocalTimeStringConverterVariant converterVariant;
    private FormatStyle timeStyle;
    private LocalTime validTime;

    private LocalTimeStringConverter converter;
    private Locale locale;
    private DateTimeFormatter formatter, parser;

    public LocalTimeStringConverterTest(LocalTimeStringConverterVariant converterVariant, FormatStyle timeStyle, LocalTime validTime) {
        this.converterVariant = converterVariant;
        this.timeStyle = timeStyle;
        this.validTime = validTime;

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
        aFormatter = DateTimeFormatter.ofPattern("HH mm ss");
        aParser = DateTimeFormatter.ofPattern("hh mm ss a");
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
            this.converter = new LocalTimeStringConverter();
            this.locale = Locale.getDefault(Locale.Category.FORMAT);
            this.formatter = null;
            this.parser = null;
            break;
        case WITH_FORMATTER_PARSER:
            this.converter = new LocalTimeStringConverter(aFormatter, aParser);
            this.locale = Locale.getDefault(Locale.Category.FORMAT);
            this.formatter = aFormatter;
            this.parser = aParser;
            break;
        case WITH_FORMAT_STYLES:
            this.converter = new LocalTimeStringConverter(FormatStyle.SHORT, Locale.UK);
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
                LocalTimeStringConverterShim.getldtConverterLocale(converter));
        assertNull(LocalTimeStringConverterShim.getldtConverterDateStyle(converter));
        assertEquals((timeStyle != null) ? timeStyle : FormatStyle.SHORT,
                LocalTimeStringConverterShim.getldtConverterTimeStyle(converter));
        if (formatter != null) {
            assertEquals(formatter,
                LocalTimeStringConverterShim.getldtConverterFormatter(converter));
        }
        if (parser != null) {
            assertEquals(parser,
                LocalTimeStringConverterShim.getldtConverterParser(converter));
        } else if (formatter != null) {
            assertEquals(formatter,
                LocalTimeStringConverterShim.getldtConverterFormatter(converter));
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
