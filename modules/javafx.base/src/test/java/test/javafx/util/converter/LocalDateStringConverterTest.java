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

import java.util.Arrays;
import java.time.LocalDate;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collection;
import java.util.Locale;

import javafx.util.converter.LocalDateStringConverter;
import javafx.util.converter.LocalDateStringConverterShim;

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
public class LocalDateStringConverterTest {
    private static final LocalDate VALID_DATE = LocalDate.of(1985, 1, 12);

    private static Locale oldLocale = null;
    private static DateTimeFormatter aFormatter = null;
    private static DateTimeFormatter aParser = null;

    // We can only create LocalDateStringConverter object after Locale is set.
    // Unfortunately, due to unpredictability of @Parameterized.Parameters methods
    // in JUnit, we have to allocate it after @BeforeClass sets up Locale and
    // necessary static fields. Otherwise, the test may collide with other
    // Local*StringConverter tests and cause unpredictable results.
    private enum LocalDateStringConverterVariant {
        NO_PARAM,
        WITH_FORMATTER_PARSER,
        WITH_FORMAT_STYLES,
    };

    @Parameterized.Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][] {
            { LocalDateStringConverterVariant.NO_PARAM,
              FormatStyle.SHORT, VALID_DATE },

            { LocalDateStringConverterVariant.WITH_FORMATTER_PARSER,
              null, VALID_DATE },

            { LocalDateStringConverterVariant.WITH_FORMAT_STYLES,
              FormatStyle.SHORT, VALID_DATE },
        });
    }

    private LocalDateStringConverterVariant converterVariant;
    private FormatStyle dateStyle;
    private LocalDate validDate;

    private LocalDateStringConverter converter;
    private Locale locale;
    private DateTimeFormatter formatter, parser;

    public LocalDateStringConverterTest(LocalDateStringConverterVariant converterVariant, FormatStyle dateStyle, LocalDate validDate) {
        this.converterVariant = converterVariant;
        this.dateStyle = dateStyle;
        this.validDate = validDate;

        // initialized after Locale is established
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
        aFormatter = DateTimeFormatter.ofPattern("dd MM yyyy");
        aParser = DateTimeFormatter.ofPattern("yyyy MM dd");
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
            this.converter = new LocalDateStringConverter();
            this.locale = Locale.getDefault(Locale.Category.FORMAT);
            this.formatter = null;
            this.parser = null;
            break;
        case WITH_FORMATTER_PARSER:
            this.converter = new LocalDateStringConverter(aFormatter, aParser);
            this.locale = Locale.getDefault(Locale.Category.FORMAT);
            this.formatter = aFormatter;
            this.parser = aParser;
            break;
        case WITH_FORMAT_STYLES:
            this.converter = new LocalDateStringConverter(FormatStyle.SHORT, Locale.UK, IsoChronology.INSTANCE);
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
        assertEquals(locale, LocalDateStringConverterShim.getldtConverterLocale(converter));
        assertEquals((dateStyle != null) ? dateStyle : FormatStyle.SHORT,
                LocalDateStringConverterShim.getldtConverterDateStyle(converter));
        assertNull(LocalDateStringConverterShim.getldtConverterTimeStyle(converter));
        if (formatter != null) {
            assertEquals(formatter,
                    LocalDateStringConverterShim.getldtConverterFormatter(converter));
        }
        if (parser != null) {
            assertEquals(parser,
                    LocalDateStringConverterShim.getldtConverterParser(converter));
        } else if (formatter != null) {
            assertEquals(formatter,
                LocalDateStringConverterShim.getldtConverterParser(converter));
        }
    }


    /*********************************************************************
     * Test toString / fromString methods
     ********************************************************************/

    @Test public void toString_to_fromString_testRoundtrip() {
        if (formatter == null) {
            // Only the default formatter/parser can guarantee roundtrip symmetry
            assertEquals(validDate, converter.fromString(converter.toString(validDate)));
        }
    }

    @Test(expected=RuntimeException.class)
    public void fromString_testInvalidInput() {
        converter.fromString("abcdefg");
    }
}
