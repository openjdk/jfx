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

package javafx.beans.value;

/**
 * A common interface of all sub-interfaces of {@link ObservableValue} that wrap
 * a number.
 *
 * @see ObservableValue
 * @see ObservableDoubleValue
 * @see ObservableFloatValue
 * @see ObservableIntegerValue
 * @see ObservableLongValue
 *
 *
 * @since JavaFX 2.0
 */
public interface ObservableNumberValue extends ObservableValue<Number> {

    /**
     * Returns the value of this {@code ObservableNumberValue} as an {@code int}
     * . If the value is not an {@code int}, a standard cast is performed.
     *
     * @return The value of this {@code ObservableNumberValue} as an {@code int}
     */
    int intValue();

    /**
     * Returns the value of this {@code ObservableNumberValue} as a {@code long}
     * . If the value is not a {@code long}, a standard cast is performed.
     *
     * @return The value of this {@code ObservableNumberValue} as a {@code long}
     */
    long longValue();

    /**
     * Returns the value of this {@code ObservableNumberValue} as a
     * {@code float}. If the value is not a {@code float}, a standard cast is
     * performed.
     *
     * @return The value of this {@code ObservableNumberValue} as a
     *         {@code float}
     */
    float floatValue();

    /**
     * Returns the value of this {@code ObservableNumberValue} as a
     * {@code double}. If the value is not a {@code double}, a standard cast is
     * performed.
     *
     * @return The value of this {@code ObservableNumberValue} as a
     *         {@code double}
     */
    double doubleValue();

}
