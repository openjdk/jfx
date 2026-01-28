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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static test.javafx.util.converter.NumberStringConverterTest.numberFormatOf;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import javafx.util.converter.CurrencyStringConverter;

public class CurrencyStringConverterTest {

    private static final String PATTERN = "#,##,###,####";

    private static final CurrencyStringConverter US_LOCALE_CONVERTER = new CurrencyStringConverter(Locale.US);

    @Test
    void testDefaultConstructor() {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

        var converter = new CurrencyStringConverter();

        assertEquals(numberFormat, numberFormatOf(converter));
    }

    @Test
    void testConstructor_locale() {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(Locale.CANADA);

        var converter = new CurrencyStringConverter(Locale.CANADA);

        assertEquals(numberFormat, numberFormatOf(converter));
    }

    @Test
    void testConstructor_pattern() {
        var symbols = new DecimalFormatSymbols(Locale.getDefault());
        var numberFormat = new DecimalFormat(PATTERN, symbols);

        var converter = new CurrencyStringConverter(PATTERN);

        assertEquals(numberFormat, numberFormatOf(converter));
    }

    @Test
    void testConstructor_locale_pattern() {
        var symbols = new DecimalFormatSymbols(Locale.CANADA);
        var numberFormat = new DecimalFormat(PATTERN, symbols);

        var converter = new CurrencyStringConverter(Locale.CANADA, PATTERN);

        assertEquals(numberFormat, numberFormatOf(converter));
    }

    @Test
    void testConstructor_numberFormat() {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(Locale.JAPAN);

        var converter = new CurrencyStringConverter(numberFormat);

        assertEquals(numberFormat, numberFormatOf(converter));
    }

    @Test
    void fromString_testValidStringInput() {
        assertEquals(10.32, US_LOCALE_CONVERTER.fromString("$10.32"));
    }

    @Test
    void fromString_testValidStringInputWithWhiteSpace() {
        assertEquals(10.32, US_LOCALE_CONVERTER.fromString("      $10.32      "));
    }

    @Test
    void toString_validInput() {
        assertEquals("$10.32", US_LOCALE_CONVERTER.toString(10.32));
    }
}
