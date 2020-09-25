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

package javafx.beans.binding;

import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;

import com.sun.javafx.binding.StringFormatter;

/**
 * {@code StringExpression} is an
 * {@link javafx.beans.value.ObservableStringValue} plus additional convenience
 * methods to generate bindings in a fluent style.
 * <p>
 * A concrete sub-class of {@code StringExpression} has to implement the method
 * {@link javafx.beans.value.ObservableStringValue#get()}, which provides the
 * actual value of this expression.
 * <p>
 * Note: all implementation of {@link javafx.beans.binding.BooleanBinding}
 * returned by the comparisons in this class consider a {@code String} that is
 * {@code null} equal to an empty {@code String}.
 * @since JavaFX 2.0
 */
public abstract class StringExpression implements ObservableStringValue {

    /**
     * Creates a default {@code StringExpression}.
     */
    public StringExpression() {
    }

    @Override
    public String getValue() {
        return get();
    }

    /**
     * Returns usually the value of this {@code StringExpression}. Only if the
     * value is {@code null} an empty {@code String} is returned instead.
     *
     * @return the value of this {@code StringExpression} or the empty
     *         {@code String}
     */
    public final String getValueSafe() {
        final String value = get();
        return value == null ? "" : value;
    }

    /**
     * Returns a {@code StringExpression} that wraps a
     * {@link javafx.beans.value.ObservableValue}. If the
     * {@code ObservableValue} is already a {@code StringExpression}, it will be
     * returned. Otherwise a new {@link javafx.beans.binding.StringBinding} is
     * created that holds the value of the {@code ObservableValue} converted to
     * a {@code String}.
     *
     * @param value
     *            The source {@code ObservableValue}
     * @return A {@code StringExpression} that wraps the {@code ObservableValue}
     *         if necessary
     * @throws NullPointerException
     *             if {@code value} is {@code null}
     */
    public static StringExpression stringExpression(
            final ObservableValue<?> value) {
        if (value == null) {
            throw new NullPointerException("Value must be specified.");
        }
        return StringFormatter.convert(value);
    }

    /**
     * Returns a {@code StringExpression} that holds the value of this
     * {@code StringExpression} concatenated with another {@code Object}.
     * <p>
     * If the value of this {@code StringExpression} changes, the value of the
     * resulting {@code StringExpression} is updated automatically. Also if the
     * other {@code Object} is an implementation of
     * {@link javafx.beans.value.ObservableValue}, changes in the other
     * {@code Object} are reflected automatically in the resulting
     * {@code StringExpression}.
     *
     * @param other
     *            the other {@code Object}
     * @return the new {@code StringExpression}
     */
    public StringExpression concat(Object other) {
        return Bindings.concat(this, other);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this and another {@link javafx.beans.value.ObservableStringValue} are
     * equal.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding isEqualTo(final ObservableStringValue other) {
        return Bindings.equal(this, other);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code StringExpression} is equal to a constant value.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding isEqualTo(final String other) {
        return Bindings.equal(this, other);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this and another {@link javafx.beans.value.ObservableStringValue} are
     * not equal.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding isNotEqualTo(final ObservableStringValue other) {
        return Bindings.notEqual(this, other);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code StringExpression} is not equal to a constant value.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding isNotEqualTo(final String other) {
        return Bindings.notEqual(this, other);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this and another {@link javafx.beans.value.ObservableStringValue} are
     * equal ignoring case.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param other
     *            the second {@code ObservableStringValue}
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding isEqualToIgnoreCase(final ObservableStringValue other) {
        return Bindings.equalIgnoreCase(this, other);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code StringExpression} is equal to a constant value ignoring
     * case.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding isEqualToIgnoreCase(final String other) {
        return Bindings.equalIgnoreCase(this, other);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this and another {@link javafx.beans.value.ObservableStringValue} are
     * not equal ignoring case.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param other
     *            the second {@code ObservableStringValue}
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding isNotEqualToIgnoreCase(
            final ObservableStringValue other) {
        return Bindings.notEqualIgnoreCase(this, other);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code StringExpression} is not equal to a constant value
     * ignoring case.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding isNotEqualToIgnoreCase(final String other) {
        return Bindings.notEqualIgnoreCase(this, other);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code StringExpression} is greater than another
     * {@link javafx.beans.value.ObservableStringValue}.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param other
     *            the second {@code ObservableStringValue}
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding greaterThan(final ObservableStringValue other) {
        return Bindings.greaterThan(this, other);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code StringExpression} is greater than a constant value.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding greaterThan(final String other) {
        return Bindings.greaterThan(this, other);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code StringExpression} is less than another
     * {@link javafx.beans.value.ObservableStringValue}.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param other
     *            the second {@code ObservableStringValue}
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding lessThan(final ObservableStringValue other) {
        return Bindings.lessThan(this, other);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code StringExpression} is less than a constant value.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding lessThan(final String other) {
        return Bindings.lessThan(this, other);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code StringExpression} is greater than or equal to another
     * {@link javafx.beans.value.ObservableStringValue}.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param other
     *            the second {@code ObservableStringValue}
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding greaterThanOrEqualTo(final ObservableStringValue other) {
        return Bindings.greaterThanOrEqual(this, other);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code StringExpression} is greater than or equal to a constant
     * value.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding greaterThanOrEqualTo(final String other) {
        return Bindings.greaterThanOrEqual(this, other);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code StringExpression} is less than or equal to another
     * {@link javafx.beans.value.ObservableStringValue}.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param other
     *            the second {@code ObservableStringValue}
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding lessThanOrEqualTo(final ObservableStringValue other) {
        return Bindings.lessThanOrEqual(this, other);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code StringExpression} is less than or equal to a constant
     * value.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding lessThanOrEqualTo(final String other) {
        return Bindings.lessThanOrEqual(this, other);
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if this
     * {@code StringExpression} is {@code null}.
     *
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding isNull() {
        return Bindings.isNull(this);
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if this
     * {@code StringExpression} is not {@code null}.
     *
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding isNotNull() {
        return Bindings.isNotNull(this);
    }

    /**
     * Creates a new {@link IntegerBinding} that holds the length of this
     * {@code StringExpression}.
     * <p>
     * Note: If the value of this {@code StringExpression} is {@code null},
     * the length is considered to be {@code 0}.
     *
     * @return the new {@code IntegerBinding}
     * @since JavaFX 8.0
     */
    public IntegerBinding length() {
        return Bindings.length(this);
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if this
     * {@code StringExpression} is empty.
     * <p>
     * Note: If the value of this {@code StringExpression} is {@code null},
     * it is considered to be empty.
     *
     * @return the new {@code BooleanBinding}
     * @since JavaFX 8.0
     */
    public BooleanBinding isEmpty() {
        return Bindings.isEmpty(this);
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if this
     * {@code StringExpression} is not empty.
     * <p>
     * Note: If the value of this {@code StringExpression} is {@code null},
     * it is considered to be empty.
     *
     * @return the new {@code BooleanBinding}
     * @since JavaFX 8.0
     */
    public BooleanBinding isNotEmpty() {
        return Bindings.isNotEmpty(this);
    }
}
