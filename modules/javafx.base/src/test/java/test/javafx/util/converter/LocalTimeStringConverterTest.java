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

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javafx.util.converter.BaseTemporalStringConverterShim;
import javafx.util.converter.LocalTimeStringConverter;

public class LocalTimeStringConverterTest {

    private static final LocalTime VALID_TIME_WITH_SECONDS = LocalTime.of(12, 34, 56);
    private static final LocalTime VALID_TIME_WITHOUT_SECONDS = LocalTime.of(12, 34, 0);

    private static Locale oldLocale;
    private static DateTimeFormatter aFormatter;
    private static DateTimeFormatter aParser;

    @BeforeAll
    public static void setupBeforeAll() {
        oldLocale = Locale.getDefault();
        // Tests require that default locale is en_US
        Locale.setDefault(Locale.US);
        // DateTimeFormatter uses default locale, so we can init this after updating locale
        aFormatter = DateTimeFormatter.ofPattern("HH mm ss");
        aParser = DateTimeFormatter.ofPattern("hh mm ss a");
    }

    @AfterAll
    public static void teardownAfterAll() {
        // Restore VM's old locale
        Locale.setDefault(oldLocale);
    }

    private enum LocalTimeStringConverterVariant {
        NO_PARAM,
        WITH_FORMATTER_PARSER,
        WITH_FORMAT_STYLES;
    }

    private record TestCase(LocalTimeStringConverterVariant variant, LocalTime validTime) {}

    private static Stream<TestCase> provideTestParameters() {
        return Stream.of(
                new TestCase(LocalTimeStringConverterVariant.NO_PARAM, VALID_TIME_WITHOUT_SECONDS),
                new TestCase(LocalTimeStringConverterVariant.WITH_FORMATTER_PARSER, VALID_TIME_WITH_SECONDS),
                new TestCase(LocalTimeStringConverterVariant.WITH_FORMAT_STYLES, VALID_TIME_WITHOUT_SECONDS)
        );
    }

    private record ConverterSetup(LocalTimeStringConverter converter, DateTimeFormatter formatter, DateTimeFormatter parser) {}

    private ConverterSetup setupConverter(LocalTimeStringConverterVariant converterVariant) {
        return switch (converterVariant) {
            case NO_PARAM -> {
                var converter = new LocalTimeStringConverter();
                yield new ConverterSetup(converter, null, null);
            }
            case WITH_FORMATTER_PARSER -> {
                var converter = new LocalTimeStringConverter(aFormatter, aParser);
                yield new ConverterSetup(converter, aFormatter, aParser);
            }
            case WITH_FORMAT_STYLES -> {
                var converter = new LocalTimeStringConverter(FormatStyle.SHORT, Locale.UK);
                yield new ConverterSetup(converter, null, null);
            }
        };
    }

    @ParameterizedTest
    @MethodSource("provideTestParameters")
    void testConstructor(TestCase testCase) {
        ConverterSetup setup = setupConverter(testCase.variant());
        LocalTimeStringConverter converter = setup.converter();
        DateTimeFormatter formatter = setup.formatter();
        DateTimeFormatter parser = setup.parser();

        if (formatter != null) {
            assertEquals(formatter, BaseTemporalStringConverterShim.getFormatter(converter));
        }
        if (parser != null) {
            assertEquals(parser, BaseTemporalStringConverterShim.getParser(converter));
        } else if (formatter != null) {
            assertEquals(formatter, BaseTemporalStringConverterShim.getParser(converter));
        }
    }

    @ParameterizedTest
    @MethodSource("provideTestParameters")
    void toString_to_fromString_testRoundtrip(TestCase testCase) {
        ConverterSetup setup = setupConverter(testCase.variant());
        LocalTimeStringConverter converter = setup.converter();
        DateTimeFormatter formatter = setup.formatter();

        if (formatter == null) {
            assertEquals(testCase.validTime(), converter.fromString(converter.toString(testCase.validTime())));
        }
    }

    @ParameterizedTest
    @MethodSource("provideTestParameters")
    void fromString_testInvalidInput(TestCase testCase) {
        ConverterSetup setup = setupConverter(testCase.variant());
        LocalTimeStringConverter converter = setup.converter();

        assertThrows(RuntimeException.class, () -> converter.fromString("abcdefg"));
    }
}
