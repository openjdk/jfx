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

import static org.junit.jupiter.api.Assertions.*;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import javafx.util.converter.NumberStringConverter;
import javafx.util.converter.NumberStringConverterShim;

public class NumberStringConverterTest {

    private static final String PATTERN = "#,##,###,####";

    private final NumberStringConverter usLocaleConverter = new NumberStringConverter(Locale.US);

    @Test
    void testDefaultConstructor() {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());

        var nsc = new NumberStringConverter();

        assertEquals(numberFormat, numberFormatOf(nsc));
    }

    @Test
    void testConstructor_locale() {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.CANADA);

        var nsc = new NumberStringConverter(Locale.CANADA);

        assertEquals(numberFormat, numberFormatOf(nsc));
    }

    @Test
    void testConstructor_pattern() {
        var symbols = new DecimalFormatSymbols(Locale.getDefault());
        var numberFormat = new DecimalFormat(PATTERN, symbols);

        var nsc = new NumberStringConverter(PATTERN);

        assertEquals(numberFormat, numberFormatOf(nsc));
    }

    @Test
    void testConstructor_locale_pattern() {
        var symbols = new DecimalFormatSymbols(Locale.CANADA);
        var numberFormat = new DecimalFormat(PATTERN, symbols);

        var nsc = new NumberStringConverter(Locale.CANADA, PATTERN);

        assertEquals(numberFormat, numberFormatOf(nsc));
    }

    @Test
    void testConstructor_numberFormat() {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.JAPAN);

        var nsc = new NumberStringConverter(numberFormat);

        assertEquals(numberFormat, numberFormatOf(nsc));
    }

    static NumberFormat numberFormatOf(NumberStringConverter csc) {
        return NumberStringConverterShim.getNumberFormat(csc);
    }

    @Test
    void fromString_testValidInput() {
        assertEquals(10L, usLocaleConverter.fromString("10"));
    }

    @Test
    void fromString_testValidInputWithWhiteSpace() {
        assertEquals(10L, usLocaleConverter.fromString("      10      "));
    }

    @Test
    void fromString_testInvalidInput() {
        assertThrows(RuntimeException.class, () -> usLocaleConverter.fromString("abcdefg"));
    }

    @Test
    void toString_validInput() {
        assertEquals("10", usLocaleConverter.toString(10L));
    }
}
