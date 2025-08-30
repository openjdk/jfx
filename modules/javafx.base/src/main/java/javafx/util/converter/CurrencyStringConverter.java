/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package javafx.util.converter;

import java.text.NumberFormat;
import java.util.Locale;

/// A `StringConverter` implementation for `Number` values that represent currency.
///
/// @since JavaFX 2.1
public class CurrencyStringConverter extends NumberStringConverter {

    /// Creates a `CurrencyStringConverter` that uses a formatter/parser based on the user's [Locale].
    public CurrencyStringConverter() {
        super();
    }

    /// Creates a `CurrencyStringConverter` that uses a formatter/parser based on the given locale.
    ///
    /// @param locale the `Locale` that will be used by the formatter/parser. If `null`, the user's locale will be used.
    public CurrencyStringConverter(Locale locale) {
        super(locale);
    }

    /// Creates a `CurrencyStringConverter` that uses a formatter/parser based on the user's [Locale] and the given
    /// decimal format pattern.
    ///
    /// @param pattern the pattern describing the number format. If `null`, a default formatter/parser will be used.
    /// @see java.text.DecimalFormat
    public CurrencyStringConverter(String pattern) {
        super(pattern);
    }

    /// Creates a `CurrencyStringConverter` that uses a formatter/parser based on the given `Locale` and decimal format
    /// pattern.
    ///
    /// @param locale the `Locale` that will be used by the formatter/parser. If `null`, the user's locale will be used.
    /// @param pattern the pattern describing the number format. If `null`, a default formatter/parser will be used.
    /// @see java.text.DecimalFormat
    public CurrencyStringConverter(Locale locale, String pattern) {
        super(locale, pattern);
    }

    /// Creates a `CurrencyStringConverter` that uses the given formatter/parser.
    ///
    /// @param numberFormat the formatter/parser that will be used by the `toString()` and `fromString()` methods. If
    ///        `null`, a default formatter/parser will be used.
    public CurrencyStringConverter(NumberFormat numberFormat) {
        super(numberFormat);
    }

    @Override
    NumberFormat getSpecializedNumberFormat(Locale locale) {
        return NumberFormat.getCurrencyInstance(locale);
    }
}
