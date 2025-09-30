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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.stream.Stream;

import javafx.util.converter.LocalTimeStringConverter;
import javafx.util.converter.LocalTimeStringConverterShim;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

    private enum LocalTimeStringConverterVariant {
        NO_PARAM,
        WITH_FORMATTER_PARSER,
        WITH_FORMAT_STYLES,
    }

    static Stream<Arguments> provideTestParameters() {
        return Stream.of(
                Arguments.of(LocalTimeStringConverterVariant.NO_PARAM, FormatStyle.SHORT, VALID_TIME_WITHOUT_SECONDS),
                Arguments.of(LocalTimeStringConverterVariant.WITH_FORMATTER_PARSER, null, VALID_TIME_WITH_SECONDS),
                Arguments.of(LocalTimeStringConverterVariant.WITH_FORMAT_STYLES, FormatStyle.SHORT, VALID_TIME_WITHOUT_SECONDS)
        );
    }

    @BeforeAll
    public static void setupBeforeAll() {
        oldLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);

        aFormatter = DateTimeFormatter.ofPattern("HH mm ss");
        aParser = DateTimeFormatter.ofPattern("hh mm ss a");
    }

    @AfterAll
    public static void teardownAfterAll() {
        Locale.setDefault(oldLocale);
    }

    private record ConverterSetup(
            LocalTimeStringConverter converter,
            Locale locale,
            DateTimeFormatter formatter,
            DateTimeFormatter parser
    ) {}

    private ConverterSetup setupConverter(
            LocalTimeStringConverterVariant converterVariant,
            FormatStyle timeStyle,
            LocalTime validTime
    ) {
        return switch (converterVariant) {
            case NO_PARAM -> {
                LocalTimeStringConverter converter = new LocalTimeStringConverter();
                Locale locale = Locale.getDefault(Locale.Category.FORMAT);
                yield new ConverterSetup(converter, locale, null, null);
            }
            case WITH_FORMATTER_PARSER -> {
                LocalTimeStringConverter converter = new LocalTimeStringConverter(aFormatter, aParser);
                Locale locale = Locale.getDefault(Locale.Category.FORMAT);
                yield new ConverterSetup(converter, locale, aFormatter, aParser);
            }
            case WITH_FORMAT_STYLES -> {
                LocalTimeStringConverter converter = new LocalTimeStringConverter(FormatStyle.SHORT, Locale.UK);
                yield new ConverterSetup(converter, Locale.UK, null, null);
            }
            default -> throw new IllegalArgumentException("Invalid converter variant: " + converterVariant);
        };
    }

    @ParameterizedTest
    @MethodSource("provideTestParameters")
    void testConstructor(
            LocalTimeStringConverterVariant converterVariant,
            FormatStyle timeStyle,
            LocalTime validTime
    ) {
        ConverterSetup setup = setupConverter(converterVariant, timeStyle, validTime);
        LocalTimeStringConverter converter = setup.converter();
        Locale locale = setup.locale();
        DateTimeFormatter formatter = setup.formatter();
        DateTimeFormatter parser = setup.parser();

        assertEquals(locale, LocalTimeStringConverterShim.getldtConverterLocale(converter));
        assertNull(LocalTimeStringConverterShim.getldtConverterDateStyle(converter));
        assertEquals(
                (timeStyle != null) ? timeStyle : FormatStyle.SHORT,
                LocalTimeStringConverterShim.getldtConverterTimeStyle(converter)
        );
        if (formatter != null) {
            assertEquals(formatter, LocalTimeStringConverterShim.getldtConverterFormatter(converter));
        }
        if (parser != null) {
            assertEquals(parser, LocalTimeStringConverterShim.getldtConverterParser(converter));
        } else if (formatter != null) {
            assertEquals(formatter, LocalTimeStringConverterShim.getldtConverterFormatter(converter));
        }
    }

    @ParameterizedTest
    @MethodSource("provideTestParameters")
    void toString_to_fromString_testRoundtrip(
            LocalTimeStringConverterVariant converterVariant,
            FormatStyle timeStyle,
            LocalTime validTime
    ) {
        ConverterSetup setup = setupConverter(converterVariant, timeStyle, validTime);
        LocalTimeStringConverter converter = setup.converter();
        DateTimeFormatter formatter = setup.formatter();

        if (formatter == null) {
            assertEquals(validTime, converter.fromString(converter.toString(validTime)));
        }
    }

    @ParameterizedTest
    @MethodSource("provideTestParameters")
    void fromString_testInvalidInput(
            LocalTimeStringConverterVariant converterVariant,
            FormatStyle timeStyle,
            LocalTime validTime
    ) {
        ConverterSetup setup = setupConverter(converterVariant, timeStyle, validTime);
        LocalTimeStringConverter converter = setup.converter();

        assertThrows(RuntimeException.class, () -> converter.fromString("abcdefg"));
    }
}
