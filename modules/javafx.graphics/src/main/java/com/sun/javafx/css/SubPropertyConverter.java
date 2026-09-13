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

package com.sun.javafx.css;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import java.util.Map;

/**
 * Defines the {@code convert} and {@code convertBack} operations that enable object
 * decomposition and reconstruction. Note that the following invariant must always be
 * satisfied: {@code convert(convertBack(value)).equals(value)}
 *
 * @param <T> the target type
 */
public interface SubPropertyConverter<T> {

    /**
     * Converts a map of CSS values to the target type.
     *
     * @param values the constituent values
     * @throws NullPointerException if {@code values} is {@code null}
     * @return the converted object
     */
    T convert(Map<CssMetaData<? extends Styleable, ?>, Object> values);

    /**
     * Converts an object back to a map of its constituent values (deconstruction).
     * The returned map can be passed into {@link #convert(Map)} to reconstruct the object.
     *
     * @param value the object
     * @throws NullPointerException if {@code value} is {@code null}
     * @return a {@code Map} of the constituent values
     */
    Map<CssMetaData<? extends Styleable, ?>, Object> convertBack(T value);
}
