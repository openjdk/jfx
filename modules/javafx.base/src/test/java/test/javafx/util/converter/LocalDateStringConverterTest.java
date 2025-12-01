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

import java.time.LocalDate;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.stream.Stream;

import javafx.util.converter.LocalDateStringConverter;
import javafx.util.converter.LocalDateStringConverterShim;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class LocalDateStringConverterTest {
    private static final LocalDate VALID_DATE = LocalDate.of(1985, 1, 12);

    private static Locale oldLocale = null;
    private static DateTimeFormatter aFormatter = null;
    private static DateTimeFormatter aParser = null;

    private enum LocalDateStringConverterVariant {
        NO_PARAM,
        WITH_FORMATTER_PARSER,
        WITH_FORMAT_STYLES,
    }

    static Stream<Arguments> provideTestParameters() {
        return Stream.of(
                Arguments.of(LocalDateStringConverterVariant.NO_PARAM, FormatStyle.SHORT, VALID_DATE),
                Arguments.of(LocalDateStringConverterVariant.WITH_FORMATTER_PARSER, null, VALID_DATE),
                Arguments.of(LocalDateStringConverterVariant.WITH_FORMAT_STYLES, FormatStyle.SHORT, VALID_DATE)
        );
    }

    @BeforeAll
    public static void setupBeforeAll() {
        oldLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);

        aFormatter = DateTimeFormatter.ofPattern("dd MM yyyy");
        aParser = DateTimeFormatter.ofPattern("yyyy MM dd");
    }

    @AfterAll
    public static void teardownAfterAll() {
        Locale.setDefault(oldLocale);
    }

    private record ConverterSetup(
            LocalDateStringConverter converter,
            Locale locale,
            DateTimeFormatter formatter,
            DateTimeFormatter parser
    ) {}

    private ConverterSetup setupConverter(
            LocalDateStringConverterVariant converterVariant,
            FormatStyle dateStyle,
            LocalDate validDate
    ) {
        return switch (converterVariant) {
            case NO_PARAM -> {
                LocalDateStringConverter converter = new LocalDateStringConverter();
                Locale locale = Locale.getDefault(Locale.Category.FORMAT);
                yield new ConverterSetup(converter, locale, null, null);
            }
            case WITH_FORMATTER_PARSER -> {
                LocalDateStringConverter converter = new LocalDateStringConverter(aFormatter, aParser);
                Locale locale = Locale.getDefault(Locale.Category.FORMAT);
                yield new ConverterSetup(converter, locale, aFormatter, aParser);
            }
            case WITH_FORMAT_STYLES -> {
                LocalDateStringConverter converter = new LocalDateStringConverter(
                        FormatStyle.SHORT, Locale.UK, IsoChronology.INSTANCE
                );
                yield new ConverterSetup(converter, Locale.UK, null, null);
            }
            default -> throw new IllegalArgumentException("Invalid converter variant: " + converterVariant);
        };
    }

    @ParameterizedTest
    @MethodSource("provideTestParameters")
    void testConstructor(
            LocalDateStringConverterVariant converterVariant,
            FormatStyle dateStyle,
            LocalDate validDate
    ) {
        ConverterSetup setup = setupConverter(converterVariant, dateStyle, validDate);
        LocalDateStringConverter converter = setup.converter();
        Locale locale = setup.locale();
        DateTimeFormatter formatter = setup.formatter();
        DateTimeFormatter parser = setup.parser();

        assertEquals(locale, LocalDateStringConverterShim.getldtConverterLocale(converter));
        assertEquals(
                (dateStyle != null) ? dateStyle : FormatStyle.SHORT,
                LocalDateStringConverterShim.getldtConverterDateStyle(converter)
        );
        assertNull(LocalDateStringConverterShim.getldtConverterTimeStyle(converter));
        if (formatter != null) {
            assertEquals(formatter, LocalDateStringConverterShim.getldtConverterFormatter(converter));
        }
        if (parser != null) {
            assertEquals(parser, LocalDateStringConverterShim.getldtConverterParser(converter));
        } else if (formatter != null) {
            assertEquals(formatter, LocalDateStringConverterShim.getldtConverterParser(converter));
        }
    }

    @ParameterizedTest
    @MethodSource("provideTestParameters")
    void toString_to_fromString_testRoundtrip(
            LocalDateStringConverterVariant converterVariant,
            FormatStyle dateStyle,
            LocalDate validDate
    ) {
        ConverterSetup setup = setupConverter(converterVariant, dateStyle, validDate);
        LocalDateStringConverter converter = setup.converter();
        DateTimeFormatter formatter = setup.formatter();

        if (formatter == null) {
            assertEquals(validDate, converter.fromString(converter.toString(validDate)));
        }
    }

    @ParameterizedTest
    @MethodSource("provideTestParameters")
    void fromString_testInvalidInput(
            LocalDateStringConverterVariant converterVariant,
            FormatStyle dateStyle,
            LocalDate validDate
    ) {
        ConverterSetup setup = setupConverter(converterVariant, dateStyle, validDate);
        LocalDateStringConverter converter = setup.converter();

        assertThrows(RuntimeException.class, () -> converter.fromString("abcdefg"));
    }
}
