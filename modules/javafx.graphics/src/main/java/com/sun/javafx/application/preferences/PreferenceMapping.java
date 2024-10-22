/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.application.preferences;

import com.sun.javafx.util.Logging;
import java.util.Objects;
import java.util.function.Function;

/**
 * A mapping from platform-specific keys to platform-independent keys defined by JavaFX, including a
 * function that maps the platform-specific value to the platform-independent value.
 */
public record PreferenceMapping<T>(String keyName, Class<T> valueType, Function<T, T> valueMapper) {

    public PreferenceMapping {
        Objects.requireNonNull(keyName, "keyName cannot be null");
        Objects.requireNonNull(valueType, "valueType cannot be null");
        Objects.requireNonNull(valueMapper, "valueMapper cannot be null");
    }

    public PreferenceMapping(String keyName, Class<T> valueType) {
        this(keyName, valueType, Function.identity());
    }

    @SuppressWarnings("unchecked")
    public T map(Object value) {
        if (valueType.isInstance(value)) {
            return valueMapper.apply((T)value);
        }

        if (value != null) {
            Logging.getJavaFXLogger().warning(
                "Unexpected value of " + keyName + " platform preference, " +
                "using default value instead (expected = " + valueType.getName() +
                ", actual = " + value.getClass().getName() + ")");
        }

        return null;
    }
}
