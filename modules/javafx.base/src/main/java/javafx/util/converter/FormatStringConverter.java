/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.text.*;
import javafx.beans.NamedArg;
import javafx.util.StringConverter;

/**
 * <p>{@link StringConverter} implementation that can use a {@link Format}
 * instance.</p>
 *
 * @since JavaFX 2.2
 */
public class FormatStringConverter<T> extends StringConverter<T> {

    // ------------------------------------------------------ Private properties

    final Format format;

    // ------------------------------------------------------------ Constructors

    public FormatStringConverter(@NamedArg("format") Format format) {
        this.format = format;
    }

    // ------------------------------------------------------- Converter Methods

    /** {@inheritDoc} */
    @Override public T fromString(String value) {
        // If the specified value is null or zero-length, return null
        if (value == null) {
            return null;
        }

        value = value.trim();

        if (value.length() < 1) {
            return null;
        }

        // Create and configure the parser to be used
        Format _format = getFormat();

        // Perform the requested parsing, and attempt to conver the output
        // back to T
        final ParsePosition pos = new ParsePosition(0);
        T result = (T) _format.parseObject(value, pos);
        if (pos.getIndex() != value.length()) {
            throw new RuntimeException("Parsed string not according to the format");
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override public String toString(T value) {
        // If the specified value is null, return a zero-length String
        if (value == null) {
            return "";
        }

        // Create and configure the formatter to be used
        Format _format = getFormat();

        // Perform the requested formatting
        return _format.format(value);
    }

    /**
     * <p>Return a <code>Format</code> instance to use for formatting
     * and parsing in this {@link StringConverter}.</p>
     *
     * @return a {@code Format} instance for formatting and parsing in this {@link StringConverter}
     */
    protected Format getFormat() {
        return format;
    }
}
