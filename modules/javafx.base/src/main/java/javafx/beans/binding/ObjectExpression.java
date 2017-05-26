/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.binding;

import com.sun.javafx.binding.StringFormatter;
import javafx.beans.value.ObservableObjectValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Locale;

/**
 * {@code ObjectExpression} is an
 * {@link javafx.beans.value.ObservableObjectValue} plus additional convenience
 * methods to generate bindings in a fluent style.
 * <p>
 * A concrete sub-class of {@code ObjectExpression} has to implement the method
 * {@link javafx.beans.value.ObservableObjectValue#get()}, which provides the
 * actual value of this expression.
 * @since JavaFX 2.0
 */
public abstract class ObjectExpression<T> implements ObservableObjectValue<T> {

    @Override
    public T getValue() {
        return get();
    }

    /**
     * Returns an {@code ObjectExpression} that wraps an
     * {@link javafx.beans.value.ObservableObjectValue}. If the
     * {@code ObservableObjectValue} is already an {@code ObjectExpression}, it
     * will be returned. Otherwise a new
     * {@link javafx.beans.binding.ObjectBinding} is created that is bound to
     * the {@code ObservableObjectValue}.
     *
     * @param <T> the type of the wrapped {@code Object}
     * @param value
     *            The source {@code ObservableObjectValue}
     * @return A {@code ObjectExpression} that wraps the
     *         {@code ObservableObjectValue} if necessary
     * @throws NullPointerException
     *             if {@code value} is {@code null}
     */
    public static <T> ObjectExpression<T> objectExpression(
            final ObservableObjectValue<T> value) {
        if (value == null) {
            throw new NullPointerException("Value must be specified.");
        }
        return value instanceof ObjectExpression ? (ObjectExpression<T>) value
                : new ObjectBinding<T>() {
                    {
                        super.bind(value);
                    }

                    @Override
                    public void dispose() {
                        super.unbind(value);
                    }

                    @Override
                    protected T computeValue() {
                        return value.get();
                    }

                    @Override
                    public ObservableList<ObservableObjectValue<T>> getDependencies() {
                        return FXCollections.singletonObservableList(value);
                    }
                };
    }

    /**
     * Creates a new {@code BooleanExpression} that holds {@code true} if this and
     * another {@link javafx.beans.value.ObservableObjectValue} are equal.
     *
     * @param other
     *            the other {@code ObservableObjectValue}
     * @return the new {@code BooleanExpression}
     * @throws NullPointerException
     *             if {@code other} is {@code null}
     */
    public BooleanBinding isEqualTo(final ObservableObjectValue<?> other) {
        return Bindings.equal(this, other);
    }

    /**
     * Creates a new {@code BooleanExpression} that holds {@code true} if this
     * {@code ObjectExpression} is equal to a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanExpression}
     */
    public BooleanBinding isEqualTo(final Object other) {
        return Bindings.equal(this, other);
    }

    /**
     * Creates a new {@code BooleanExpression} that holds {@code true} if this and
     * another {@link javafx.beans.value.ObservableObjectValue} are not equal.
     *
     * @param other
     *            the other {@code ObservableObjectValue}
     * @return the new {@code BooleanExpression}
     * @throws NullPointerException
     *             if {@code other} is {@code null}
     */
    public BooleanBinding isNotEqualTo(final ObservableObjectValue<?> other) {
        return Bindings.notEqual(this, other);
    }

    /**
     * Creates a new {@code BooleanExpression} that holds {@code true} if this
     * {@code ObjectExpression} is not equal to a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanExpression}
     */
    public BooleanBinding isNotEqualTo(final Object other) {
        return Bindings.notEqual(this, other);
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if this
     * {@code ObjectExpression} is {@code null}.
     *
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding isNull() {
        return Bindings.isNull(this);
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if this
     * {@code ObjectExpression} is not {@code null}.
     *
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding isNotNull() {
        return Bindings.isNotNull(this);
    }

    /**
     * Creates a {@link javafx.beans.binding.StringBinding} that holds the value
     * of this {@code ObjectExpression} turned into a {@code String}. If the
     * value of this {@code ObjectExpression} changes, the value of the
     * {@code StringBinding} will be updated automatically.
     *
     * @return the new {@code StringBinding}
     * @since JavaFX 8.0
     */
    public StringBinding asString() {
        return (StringBinding) StringFormatter.convert(this);
    }

    /**
     * Creates a {@link javafx.beans.binding.StringBinding} that holds the value
     * of the {@code ObjectExpression} turned into a {@code String}. If the
     * value of this {@code ObjectExpression} changes, the value of the
     * {@code StringBinding} will be updated automatically.
     * <p>
     * The result is formatted according to the formatting {@code String}. See
     * {@code java.util.Formatter} for formatting rules.
     *
     * @param format
     *            the formatting {@code String}
     * @return the new {@code StringBinding}
     * @since JavaFX 8.0
     */
    public StringBinding asString(String format) {
        return (StringBinding) Bindings.format(format, this);
    }

    /**
     * Creates a {@link javafx.beans.binding.StringBinding} that holds the value
     * of the {@code NumberExpression} turned into a {@code String}. If the
     * value of this {@code NumberExpression} changes, the value of the
     * {@code StringBinding} will be updated automatically.
     * <p>
     * The result is formatted according to the formatting {@code String} and
     * the passed in {@code Locale}. See {@code java.util.Formatter} for
     * formatting rules. See {@code java.util.Locale} for details on
     * {@code Locale}.
     *
     * @param locale the Locale to be used
     * @param format
     *            the formatting {@code String}
     * @return the new {@code StringBinding}
     * @since JavaFX 8.0
     */
    public StringBinding asString(Locale locale, String format) {
        return (StringBinding) Bindings.format(locale, format, this);
    }
}
