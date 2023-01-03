/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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
import javafx.util.StringConverter;

/**
 * A {@link StringConverter} implementation for {@link Number} values that represent percentages. Instances of this class are
 * immutable.
 *
 * @see CurrencyStringConverter
 * @see NumberStringConverter
 * @see StringConverter
 * @since JavaFX 2.1
 */
public class PercentageStringConverter extends NumberStringConverter {

    /**
     * Constructs a {@code PercentageStringConverter} with the default locale and format.
     */
    public PercentageStringConverter() {
        this(Locale.getDefault());
    }

    /**
     * Constructs a {@code PercentageStringConverter} with the given locale and the default format.
     *
     * @param locale the locale used in determining the number format used to format the string
     */
    public PercentageStringConverter(Locale locale) {
        super(locale, null, null);
    }

    /**
     * Constructs a {@code PercentageStringConverter} with the given number format.
     *
     * @param numberFormat the number format used to format the string
     */
    public PercentageStringConverter(NumberFormat numberFormat) {
        super(null, null, numberFormat);
    }

    /** {@inheritDoc} */
    @Override public NumberFormat getNumberFormat() {
        Locale _locale = locale == null ? Locale.getDefault() : locale;

        if (numberFormat != null) {
            return numberFormat;
        } else {
            return NumberFormat.getPercentInstance(_locale);
        }
    }
}
