/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.property;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableStringValue;
import javafx.util.StringConverter;

import java.text.Format;

/**
 * This class provides a full implementation of a {@link Property} wrapping a
 * {@code String} value.
 *
 * The value of a {@code StringProperty} can be get and set with {@link #get()},
 * {@link #getValue()}, {@link #set(Object)}, and {@link #setValue(String)}.
 *
 * A property can be bound and unbound unidirectional with
 * {@link #bind(ObservableValue)} and {@link #unbind()}. Bidirectional bindings
 * can be created and removed with {@link #bindBidirectional(Property)} and
 * {@link #unbindBidirectional(Property)}.
 *
 * The context of a {@code StringProperty} can be read with {@link #getBean()}
 * and {@link #getName()}.
 *
 * @see javafx.beans.value.ObservableStringValue
 * @see javafx.beans.value.WritableStringValue
 * @see ReadOnlyStringProperty
 * @see Property
 *
 * @since JavaFX 2.0
 */
public abstract class StringProperty extends ReadOnlyStringProperty implements
        Property<String>, WritableStringValue {

    /**
     * Creates a default {@code StringProperty}.
     */
    public StringProperty() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(String v) {
        set(v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bindBidirectional(Property<String> other) {
        Bindings.bindBidirectional(this, other);
    }

    /**
     * Create a bidirectional binding between this {@code StringProperty} and another
     * arbitrary property. Relies on an implementation of {@code Format} for conversion.
     *
     * @param other
     *            the other {@code Property}
     * @param format
     *            the {@code Format} used to convert between this {@code StringProperty}
     *            and the other {@code Property}
     * @throws NullPointerException
     *             if {@code other} or {@code format} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code other} is {@code this}
     * @since JavaFX 2.1
     */
    public void bindBidirectional(Property<?> other, Format format) {
        Bindings.bindBidirectional(this, other, format);
    }

    /**
     * Create a bidirectional binding between this {@code StringProperty} and another
     * arbitrary property. Relies on an implementation of {@link StringConverter} for conversion.
     *
     * @param <T> the type of the wrapped {@code Object}
     * @param other
     *            the other {@code Property}
     * @param converter
     *            the {@code StringConverter} used to convert between this {@code StringProperty}
     *            and the other {@code Property}
     * @throws NullPointerException
     *             if {@code other} or {@code converter} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code other} is {@code this}
     * @since JavaFX 2.1
     */
    public <T> void bindBidirectional(Property<T> other, StringConverter<T> converter) {
        Bindings.bindBidirectional(this, other, converter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unbindBidirectional(Property<String> other) {
        Bindings.unbindBidirectional(this, other);
    }

    /**
     * Remove a bidirectional binding between this {@code Property} and another
     * one.
     *
     * If no bidirectional binding between the properties exists, calling this
     * method has no effect.
     *
     * @param other
     *            the other {@code Property}
     * @throws NullPointerException
     *             if {@code other} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code other} is {@code this}
     * @since JavaFX 2.1
     */
    public void unbindBidirectional(Object other) {
        Bindings.unbindBidirectional(this, other);
    }

    /**
     * Returns a string representation of this {@code StringProperty} object.
     * @return a string representation of this {@code StringProperty} object.
     */
    @Override
    public String toString() {
        final Object bean = getBean();
        final String name = getName();
        final StringBuilder result = new StringBuilder(
                "StringProperty [");
        if (bean != null) {
            result.append("bean: ").append(bean).append(", ");
        }
        if ((name != null) && (!name.equals(""))) {
            result.append("name: ").append(name).append(", ");
        }
        result.append("value: ").append(get()).append("]");
        return result.toString();
    }
}
