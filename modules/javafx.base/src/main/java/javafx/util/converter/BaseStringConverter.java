/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import javafx.util.StringConverter;

/// A base class containing common implementations for `StringConverter`s as noted in the @implNote of `StringConverter`.
abstract class BaseStringConverter<T> extends StringConverter<T> {

    @Override
    public T fromString(String string) {
        if (string == null) {
            return null;
        }
        string = string.trim();
        if (string.isEmpty()) {
            return null;
        }
        return fromNonEmptyString(string);
    }

    /// Returns an object parsed from a non-`null` non-empty string.
    ///
    /// Treat as protected (implementing classes are public so they can't add a new protected method).
    abstract T fromNonEmptyString(String string);

    @Override
    public String toString(T object) {
        return object == null ? "" : toStringFromNonNull(object);
    }

    /// Returns a string from a non-`null` reference.
    ///
    /// Treat as protected (implementing classes are public so they can't add a new protected method).
    String toStringFromNonNull(T object) {
        return object.toString();
    }
}
