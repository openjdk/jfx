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

import java.util.Locale;

import javafx.beans.value.ObservableNumberValue;

/**
 * {@code NumberExpression} is an
 * {@link javafx.beans.value.ObservableNumberValue} plus additional convenience
 * methods to generate bindings in a fluent style.
 * <p>
 * This API allows to mix types when defining arithmetic operations. The type of
 * the result is defined by the same rules as in the Java Language.
 * <ol>
 * <li>If one of the operands is a double, the result is a double.</li>
 * <li>If not and one of the operands is a float, the result is a float.</li>
 * <li>If not and one of the operands is a long, the result is a long.</li>
 * <li>The result is an integer otherwise.</li>
 * </ol>
 * <p>
 * To be able to deal with an unspecified return type, two interfaces
 * {@code NumberExpression} and its counterpart
 * {@link javafx.beans.binding.NumberBinding} were introduced. That means if the
 * return type is specified as {@code NumberBinding}, the method will either
 * return a {@link javafx.beans.binding.DoubleBinding},
 * {@link javafx.beans.binding.FloatBinding},
 * {@link javafx.beans.binding.LongBinding} or
 * {@link javafx.beans.binding.IntegerBinding}, depending on the types of the
 * operands.
 * <p>
 * The API tries to do its best in determining the correct return type, e.g.
 * combining a {@link javafx.beans.value.ObservableNumberValue} with a primitive
 * double will always result in a {@link javafx.beans.binding.DoubleBinding}. In
 * cases where the return type is not known by the API, it is the responsibility
 * of the developer to call the correct getter ({@link #intValue()} etc.). If
 * the internal representation does not match the type of the getter, a standard
 * cast is done.
 * @since JavaFX 2.0
 */
public interface NumberExpression extends ObservableNumberValue {

    // ===============================================================
    // Negation

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the negation of {@code NumberExpression}.
     *
     * @return the new {@code NumberBinding}
     */
    NumberBinding negate();

    // ===============================================================
    // Plus

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the sum of this {@code NumberExpression} and another
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param other
     *            the second {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the other {@code ObservableNumberValue} is {@code null}
     */
    NumberBinding add(final ObservableNumberValue other);

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the sum of this {@code NumberExpression} and a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code NumberBinding}
     */
    NumberBinding add(final double other);

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the sum of this {@code NumberExpression} and a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code NumberBinding}
     */
    NumberBinding add(final float other);

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the sum of this {@code NumberExpression} and a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code NumberBinding}
     */
    NumberBinding add(final long other);

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the sum of this {@code NumberExpression} and a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code NumberBinding}
     */
    NumberBinding add(final int other);

    // ===============================================================
    // Minus

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the difference of this {@code NumberExpression} and another
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param other
     *            the second {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the other {@code ObservableNumberValue} is {@code null}
     */
    NumberBinding subtract(final ObservableNumberValue other);

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the difference of this {@code NumberExpression} and a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code NumberBinding}
     */
    NumberBinding subtract(final double other);

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the difference of this {@code NumberExpression} and a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code NumberBinding}
     */
    NumberBinding subtract(final float other);

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the difference of this {@code NumberExpression} and a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code NumberBinding}
     */
    NumberBinding subtract(final long other);

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the difference of this {@code NumberExpression} and a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code NumberBinding}
     */
    NumberBinding subtract(final int other);

    // ===============================================================
    // Times

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the product of this {@code NumberExpression} and another
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param other
     *            the second {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the other {@code ObservableNumberValue} is {@code null}
     */
    NumberBinding multiply(final ObservableNumberValue other);

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the product of this {@code NumberExpression} and a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code NumberBinding}
     */
    NumberBinding multiply(final double other);

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the product of this {@code NumberExpression} and a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code NumberBinding}
     */
    NumberBinding multiply(final float other);

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the product of this {@code NumberExpression} and a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code NumberBinding}
     */
    NumberBinding multiply(final long other);

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the product of this {@code NumberExpression} and a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code NumberBinding}
     */
    NumberBinding multiply(final int other);

    // ===============================================================
    // DividedBy

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the division of this {@code NumberExpression} and another
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param other
     *            the second {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the other {@code ObservableNumberValue} is {@code null}
     */
    NumberBinding divide(final ObservableNumberValue other);

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the division of this {@code NumberExpression} and a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code NumberBinding}
     */
    NumberBinding divide(final double other);

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the division of this {@code NumberExpression} and a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code NumberBinding}
     */
    NumberBinding divide(final float other);

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the division of this {@code NumberExpression} and a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code NumberBinding}
     */
    NumberBinding divide(final long other);

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the division of this {@code NumberExpression} and a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code NumberBinding}
     */
    NumberBinding divide(final int other);

    // ===============================================================
    // IsEqualTo

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this and another {@link javafx.beans.value.ObservableNumberValue} are
     * equal.
     * <p>
     * When comparing floating-point numbers it is recommended to use the
     * {@link #isEqualTo(ObservableNumberValue, double) isEqualTo()} method that
     * allows a small tolerance.
     *
     * @param other
     *            the second {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the other {@code ObservableNumberValue} is {@code null}
     */
    BooleanBinding isEqualTo(final ObservableNumberValue other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this and another {@link javafx.beans.value.ObservableNumberValue} are
     * equal (with a tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param other
     *            the second {@code ObservableNumberValue}
     * @param epsilon
     *            the tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the other {@code ObservableNumberValue} is {@code null}
     */
    BooleanBinding isEqualTo(final ObservableNumberValue other, double epsilon);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is equal to a constant value (with a
     * tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param other
     *            the constant value
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding isEqualTo(final double other, double epsilon);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is equal to a constant value (with a
     * tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param other
     *            the constant value
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding isEqualTo(final float other, double epsilon);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is equal to a constant value.
     * <p>
     * When comparing floating-point numbers it is recommended to use the
     * {@link #isEqualTo(long, double) isEqualTo()} method that allows a small
     * tolerance.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding isEqualTo(final long other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is equal to a constant value (with a
     * tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param other
     *            the constant value
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding isEqualTo(final long other, double epsilon);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is equal to a constant value.
     * <p>
     * When comparing floating-point numbers it is recommended to use the
     * {@link #isEqualTo(int, double) isEqualTo()} method that allows a small
     * tolerance.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding isEqualTo(final int other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is equal to a constant value (with a
     * tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers.
     *
     * @param other
     *            the constant value
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding isEqualTo(final int other, double epsilon);

    // ===============================================================
    // IsNotEqualTo

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this and another {@link javafx.beans.value.ObservableNumberValue} are
     * not equal.
     * <p>
     * When comparing floating-point numbers it is recommended to use the
     * {@link #isNotEqualTo(ObservableNumberValue, double) isNotEqualTo()}
     * method that allows a small tolerance.
     *
     * @param other
     *            the second {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the other {@code ObservableNumberValue} is {@code null}
     */
    BooleanBinding isNotEqualTo(final ObservableNumberValue other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this and another {@link javafx.beans.value.ObservableNumberValue} are
     * not equal (with a tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered not equal if
     * {@code Math.abs(a-b) > epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param other
     *            the second {@code ObservableNumberValue}
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the other {@code ObservableNumberValue} is {@code null}
     */
    BooleanBinding isNotEqualTo(final ObservableNumberValue other,
            double epsilon);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is not equal to a constant value (with a
     * tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered not equal if
     * {@code Math.abs(a-b) > epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers.
     *
     * @param other
     *            the constant value
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding isNotEqualTo(final double other, double epsilon);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is not equal to a constant value (with a
     * tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered not equal if
     * {@code Math.abs(a-b) > epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers.
     *
     * @param other
     *            the constant value
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding isNotEqualTo(final float other, double epsilon);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is not equal to a constant value.
     * <p>
     * When comparing floating-point numbers it is recommended to use the
     * {@link #isNotEqualTo(long, double) isNotEqualTo()} method that allows a
     * small tolerance.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding isNotEqualTo(final long other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is not equal to a constant value (with a
     * tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered not equal if
     * {@code Math.abs(a-b) > epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers.
     *
     * @param other
     *            the constant value
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding isNotEqualTo(final long other, double epsilon);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is not equal to a constant value.
     * <p>
     * When comparing floating-point numbers it is recommended to use the
     * {@link #isNotEqualTo(int, double) isNotEqualTo()} method that allows a
     * small tolerance.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding isNotEqualTo(final int other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is not equal to a constant value (with a
     * tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered not equal if
     * {@code Math.abs(a-b) > epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers.
     *
     * @param other
     *            the constant value
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding isNotEqualTo(final int other, double epsilon);

    // ===============================================================
    // IsGreaterThan

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is greater than another
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param other
     *            the second {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the other {@code ObservableNumberValue} is {@code null}
     */
    BooleanBinding greaterThan(final ObservableNumberValue other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is greater than a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding greaterThan(final double other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is greater than a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding greaterThan(final float other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is greater than a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding greaterThan(final long other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is greater than a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding greaterThan(final int other);

    // ===============================================================
    // IsLesserThan

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is lesser than another
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param other
     *            the second {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the other {@code ObservableNumberValue} is {@code null}
     */
    BooleanBinding lessThan(final ObservableNumberValue other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is lesser than a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding lessThan(final double other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is lesser than a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding lessThan(final float other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is lesser than a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding lessThan(final long other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is lesser than a constant value.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding lessThan(final int other);

    // ===============================================================
    // IsGreaterThanOrEqualTo

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is greater than or equal to another
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param other
     *            the second {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the other {@code ObservableNumberValue} is {@code null}
     */
    BooleanBinding greaterThanOrEqualTo(final ObservableNumberValue other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is greater than or equal to a constant
     * value.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding greaterThanOrEqualTo(final double other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is greater than or equal to a constant
     * value.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding greaterThanOrEqualTo(final float other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is greater than or equal to a constant
     * value.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding greaterThanOrEqualTo(final long other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is greater than or equal to a constant
     * value.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding greaterThanOrEqualTo(final int other);

    // ===============================================================
    // IsLessThanOrEqualTo

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is less than or equal to another
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param other
     *            the second {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the other {@code ObservableNumberValue} is {@code null}
     */
    BooleanBinding lessThanOrEqualTo(final ObservableNumberValue other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is less than or equal to a constant
     * value.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding lessThanOrEqualTo(final double other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is less than or equal to a constant
     * value.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding lessThanOrEqualTo(final float other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is less than or equal to a constant
     * value.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding lessThanOrEqualTo(final long other);

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if this {@code NumberExpression} is less than or equal to a constant
     * value.
     *
     * @param other
     *            the constant value
     * @return the new {@code BooleanBinding}
     */
    BooleanBinding lessThanOrEqualTo(final int other);

    // ===============================================================
    // String conversions

    /**
     * Creates a {@link javafx.beans.binding.StringBinding} that holds the value
     * of the {@code NumberExpression} turned into a {@code String}. If the
     * value of this {@code NumberExpression} changes, the value of the
     * {@code StringBinding} will be updated automatically.
     * <p>
     * The conversion is done without any formatting applied.
     *
     * @return the new {@code StringBinding}
     */
    StringBinding asString();

    /**
     * Creates a {@link javafx.beans.binding.StringBinding} that holds the value
     * of the {@code NumberExpression} turned into a {@code String}. If the
     * value of this {@code NumberExpression} changes, the value of the
     * {@code StringBinding} will be updated automatically.
     * <p>
     * The result is formatted according to the formatting {@code String}. See
     * {@code java.util.Formatter} for formatting rules.
     *
     * @param format
     *            the formatting {@code String}
     * @return the new {@code StringBinding}
     */
    StringBinding asString(String format);

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
     */
    StringBinding asString(Locale locale, String format);
}
