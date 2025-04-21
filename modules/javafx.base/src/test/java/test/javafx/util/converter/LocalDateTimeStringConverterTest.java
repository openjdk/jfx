/*
 * Copyright (c) 2014, 2025, Oracle and/or its affiliates. All rights reserved.
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

// Imports remain the same, except JUnit 4 imports are replaced with JUnit 5
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.chrono.IsoChronology;
import java.time.chrono.JapaneseChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.stream.Stream;

import javafx.util.StringConverter;
import javafx.util.converter.LocalDateTimeStringConverter;
import javafx.util.converter.LocalDateTimeStringConverterShim;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class LocalDateTimeStringConverterTest {

    private static final String JAPANESE_DATE_STRING = "Saturday, January 12, 60 Shōwa, 12:34:56\u202fPM";;
    private static final LocalDateTime VALID_LDT_WITH_SECONDS = LocalDateTime.of(1985, 1, 12, 12, 34, 56);
    private static final LocalDateTime VALID_LDT_WITHOUT_SECONDS = LocalDateTime.of(1985, 1, 12, 12, 34, 0);

    private static DateTimeFormatter aFormatter;
    private static DateTimeFormatter aParser;
    private static Locale oldLocale;

    public enum LocalDateTimeStringConverterVariant {
        NO_PARAM,
        WITH_FORMATTER_PARSER,
        WITH_FORMAT_STYLES,
    }

    // Parameter source method
    public static Stream<Arguments> implementations() {
        return Stream.of(
                arguments(LocalDateTimeStringConverterVariant.NO_PARAM,
                        FormatStyle.SHORT, FormatStyle.SHORT, VALID_LDT_WITHOUT_SECONDS),
                arguments(LocalDateTimeStringConverterVariant.WITH_FORMATTER_PARSER,
                        null, null, VALID_LDT_WITH_SECONDS),
                arguments(LocalDateTimeStringConverterVariant.WITH_FORMAT_STYLES,
                        FormatStyle.SHORT, FormatStyle.SHORT, VALID_LDT_WITHOUT_SECONDS)
        );
    }

    @BeforeAll
    public static void setupBeforeAll() {
        oldLocale = Locale.getDefault();
        // Tests require that default locale is en_US
        Locale.setDefault(Locale.US);
        // DateTimeFormatter uses default locale, so we can init this after updating locale
        aFormatter = DateTimeFormatter.ofPattern("dd MM yyyy HH mm ss");
        aParser = DateTimeFormatter.ofPattern("yyyy MM dd hh mm ss a");
    }

    @AfterAll
    public static void teardownAfterAll() {
        // Restore VM's old locale
        Locale.setDefault(oldLocale);
    }

    // Parameterized test methods
    @ParameterizedTest
    @MethodSource("implementations")
    void testConstructor(LocalDateTimeStringConverterVariant converterVariant,
                         FormatStyle dateStyle,
                         FormatStyle timeStyle,
                         LocalDateTime validDateTime) {
        LocalDateTimeStringConverter converter = createConverter(converterVariant);
        Locale locale = getLocale(converterVariant);
        DateTimeFormatter formatter = getFormatter(converterVariant);
        DateTimeFormatter parser = getParser(converterVariant);

        assertEquals(locale, LocalDateTimeStringConverterShim.getldtConverterLocale(converter));
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

    @ParameterizedTest
    @MethodSource("implementations")
    void toString_to_fromString_testRoundtrip(LocalDateTimeStringConverterVariant converterVariant,
                                              FormatStyle dateStyle,
                                              FormatStyle timeStyle,
                                              LocalDateTime validDateTime) {
        LocalDateTimeStringConverter converter = createConverter(converterVariant);
        DateTimeFormatter formatter = getFormatter(converterVariant);

        if (formatter == null) {
            assertEquals(validDateTime, converter.fromString(converter.toString(validDateTime)));
        }
    }

    @ParameterizedTest
    @MethodSource("implementations")
    void fromString_testInvalidInput(LocalDateTimeStringConverterVariant converterVariant,
                                     FormatStyle dateStyle,
                                     FormatStyle timeStyle,
                                     LocalDateTime validDateTime) {
        LocalDateTimeStringConverter converter = createConverter(converterVariant);
        assertThrows(RuntimeException.class, () -> converter.fromString("abcdefg"));
    }

    // Helper methods for setup
    private LocalDateTimeStringConverter createConverter(LocalDateTimeStringConverterVariant variant) {
        switch (variant) {
            case NO_PARAM:
                return new LocalDateTimeStringConverter();
            case WITH_FORMATTER_PARSER:
                return new LocalDateTimeStringConverter(aFormatter, aParser);
            case WITH_FORMAT_STYLES:
                return new LocalDateTimeStringConverter(FormatStyle.SHORT, FormatStyle.SHORT,
                        Locale.UK, IsoChronology.INSTANCE);
            default:
                fail("Invalid converter variant: " + variant);
                return null;
        }
    }

    private Locale getLocale(LocalDateTimeStringConverterVariant variant) {
        return switch (variant) {
            case NO_PARAM, WITH_FORMATTER_PARSER -> Locale.getDefault(Locale.Category.FORMAT);
            case WITH_FORMAT_STYLES -> Locale.UK;
            default -> {
                fail("Invalid converter variant: " + variant);
                yield null;
            }
        };
    }

    private DateTimeFormatter getFormatter(LocalDateTimeStringConverterVariant variant) {
        return switch (variant) {
            case WITH_FORMATTER_PARSER -> aFormatter;
            default -> null;
        };
    }

    private DateTimeFormatter getParser(LocalDateTimeStringConverterVariant variant) {
        return switch (variant) {
            case WITH_FORMATTER_PARSER -> aParser;
            default -> null;
        };
    }

    @Test
    void converter_with_specified_formatter_and_parser() {
        String formatPattern = "dd MMMM yyyy, HH:mm:ss";
        String parsePattern = "MMMM dd, yyyy, HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern);
        DateTimeFormatter parser = DateTimeFormatter.ofPattern(parsePattern);
        StringConverter<LocalDateTime> converter = new LocalDateTimeStringConverter(formatter, parser);
        assertEquals("12 January 1985, 12:34:56", converter.toString(VALID_LDT_WITH_SECONDS));
        assertEquals(VALID_LDT_WITH_SECONDS, converter.fromString("January 12, 1985, 12:34:56"));
    }

    @Test
    void converter_with_specified_formatter_and_null_parser() {
        String pattern = "dd MMMM yyyy, HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        StringConverter<LocalDateTime> converter = new LocalDateTimeStringConverter(formatter, null);
        assertEquals("12 January 1985, 12:34:56", converter.toString(VALID_LDT_WITH_SECONDS));
        assertEquals(VALID_LDT_WITH_SECONDS, converter.fromString("12 January 1985, 12:34:56"));
    }

    @Test
    void testChronologyConsistency() {
        var converter = new LocalDateTimeStringConverter(FormatStyle.FULL, FormatStyle.MEDIUM,
                null, JapaneseChronology.INSTANCE);
        assertEquals(JAPANESE_DATE_STRING, converter.toString(VALID_LDT_WITH_SECONDS));
        try {
            converter.toString(LocalDateTime.of(1, 1, 1, 1, 1, 1));
        } catch (DateTimeException e) {}
        assertEquals(VALID_LDT_WITH_SECONDS, converter.fromString(JAPANESE_DATE_STRING));
    }
}
