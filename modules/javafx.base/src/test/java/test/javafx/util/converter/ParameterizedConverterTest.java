/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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

import javafx.util.StringConverter;
import javafx.util.converter.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ParameterizedConverterTest {

    static Stream<Arguments> converterClasses() {
        return Stream.of(
                Arguments.of(BigDecimalStringConverter.class),
                Arguments.of(BigIntegerStringConverter.class),
                Arguments.of(BooleanStringConverter.class),
                Arguments.of(ByteStringConverter.class),
                Arguments.of(CharacterStringConverter.class),
                Arguments.of(CurrencyStringConverter.class),
                Arguments.of(DateStringConverter.class),
                Arguments.of(DateTimeStringConverter.class),
                Arguments.of(DefaultStringConverter.class),
                Arguments.of(DoubleStringConverter.class),
                Arguments.of(FloatStringConverter.class),
                Arguments.of(IntegerStringConverter.class),
                Arguments.of(LongStringConverter.class),
                Arguments.of(NumberStringConverter.class),
                Arguments.of(PercentageStringConverter.class),
                Arguments.of(ShortStringConverter.class),
                Arguments.of(TimeStringConverter.class)
        );
    }

    @ParameterizedTest
    @MethodSource("converterClasses")
    void toString_testNull(Class<? extends StringConverter<?>> converterClass) throws Exception {
        StringConverter<?> converter = converterClass.getDeclaredConstructor().newInstance();
        assertEquals("", converter.toString(null));
    }

    @ParameterizedTest
    @MethodSource("converterClasses")
    void fromString_testEmptyStringWithWhiteSpace(Class<? extends StringConverter<?>> converterClass) throws Exception {
        StringConverter<?> converter = converterClass.getDeclaredConstructor().newInstance();
        if (converter instanceof DefaultStringConverter) {
            assertEquals("      ", converter.fromString("      "));
        } else {
            assertNull(converter.fromString("      "));
        }
    }

    @ParameterizedTest
    @MethodSource("converterClasses")
    void fromString_testNull(Class<? extends StringConverter<?>> converterClass) throws Exception {
        StringConverter<?> converter = converterClass.getDeclaredConstructor().newInstance();
        assertNull(converter.fromString(null));
    }

    @ParameterizedTest
    @MethodSource("converterClasses")
    void fromString_testEmptyString(Class<? extends StringConverter<?>> converterClass) throws Exception {
        StringConverter<?> converter = converterClass.getDeclaredConstructor().newInstance();
        if (converter instanceof DefaultStringConverter) {
            assertEquals("", converter.fromString(""));
        } else {
            assertNull(converter.fromString(""));
        }
    }
}
