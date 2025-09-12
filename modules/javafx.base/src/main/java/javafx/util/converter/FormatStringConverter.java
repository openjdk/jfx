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

import java.text.Format;
import java.text.ParsePosition;
import java.util.Objects;

import javafx.beans.NamedArg;

/// A `StringConverter` implementation that can use a [Format] instance to convert arbitrary types to and from a string.
///
/// @param <T> the type converted to/from a string
/// @since JavaFX 2.2
public class FormatStringConverter<T> extends BaseStringConverter<T> {

    private final Format format;

    /// Creates a `FormatStringConverter` for arbitrary types that uses the given `Format`.
    ///
    /// @param format the formatter/parser that will be used by the `toString()` and `fromString()` methods. Must not be
    ///        `null`.
    public FormatStringConverter(@NamedArg("format") Format format) {
        Objects.requireNonNull(format);
        this.format = format;
    }

    @Override
    T fromNonEmptyString(String string) {
        var pos = new ParsePosition(0);
        @SuppressWarnings("unchecked")
        T result = (T) format.parseObject(string, pos);
        if (pos.getIndex() != string.length()) {
            throw new IllegalArgumentException("Parsed string not according to the format");
        }
        return result;
    }

    @Override
    String toStringFromNonNull(T object) {
        return format.format(object);
    }

    /// {@return the `Format` instance for formatting and parsing in this `FormatStringConverter`}
    protected Format getFormat() {
        return format;
    }
}
