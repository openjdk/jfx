/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import javafx.util.StringConverter;

/**
 * <p>{@link StringConverter} implementation for {@link Number} values
 * that represent currency.</p>
 *
 * @see PercentageStringConverter
 * @see NumberStringConverter
 * @see StringConverter
 * @since JavaFX 2.1
 */
public class CurrencyStringConverter extends NumberStringConverter {

    // ------------------------------------------------------------ Constructors
    public CurrencyStringConverter() {
        this(Locale.getDefault());
    }

    public CurrencyStringConverter(Locale locale) {
        this(locale, null);
    }

    public CurrencyStringConverter(String pattern) {
        this(Locale.getDefault(), pattern);
    }

    public CurrencyStringConverter(Locale locale, String pattern) {
        super(locale, pattern, null);
    }

    public CurrencyStringConverter(NumberFormat numberFormat) {
        super(null, null, numberFormat);
    }


    // ---------------------------------------------------------------0- Methods

    /** {@inheritDoc} */
    @Override protected NumberFormat getNumberFormat() {
        Locale _locale = locale == null ? Locale.getDefault() : locale;

        if (numberFormat != null) {
            return numberFormat;
        } else if (pattern != null) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(_locale);
            return new DecimalFormat(pattern, symbols);
        } else {
            return NumberFormat.getCurrencyInstance(_locale);
        }
    }
}
