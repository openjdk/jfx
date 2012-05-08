/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.beans.annotations.NoBuilder;
import java.text.*;
import java.util.Locale;
import javafx.util.StringConverter;

/**
 * <p>{@link StringConverter} implementation that can use a {@link Format} 
 * instance.</p>
 */
@NoBuilder
public class FormatStringConverter<T> extends StringConverter<T> {
    
    // ------------------------------------------------------ Private properties

    final Format format;
    
    // ------------------------------------------------------------ Constructors
    
    public FormatStringConverter(Format format) {
        this.format = format;
    }

    // ------------------------------------------------------- Converter Methods

    /** {@inheritDoc} */
    @Override public T fromString(String value) {
        try {
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
            return (T) _format.parseObject(value);
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
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
     */
    protected Format getFormat() {
        return format;
    }
}
