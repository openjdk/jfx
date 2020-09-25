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

import javafx.beans.value.ObservableIntegerValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.value.ObservableValue;

/**
 * {@code IntegerExpression} is an
 * {@link javafx.beans.value.ObservableIntegerValue} plus additional convenience
 * methods to generate bindings in a fluent style.
 * <p>
 * A concrete sub-class of {@code IntegerExpression} has to implement the method
 * {@link javafx.beans.value.ObservableIntegerValue#get()}, which provides the
 * actual value of this expression.
 * @since JavaFX 2.0
 */
public abstract class IntegerExpression extends NumberExpressionBase implements
        ObservableIntegerValue {

    /**
     * Creates a default {@code IntegerExpression}.
     */
    public IntegerExpression() {
    }

    @Override
    public int intValue() {
        return get();
    }

    @Override
    public long longValue() {
        return (long) get();
    }

    @Override
    public float floatValue() {
        return (float) get();
    }

    @Override
    public double doubleValue() {
        return (double) get();
    }

    @Override
    public Integer getValue() {
        return get();
    }

    /**
     * Returns a {@code IntegerExpression} that wraps a
     * {@link javafx.beans.value.ObservableIntegerValue}. If the
     * {@code ObservableIntegerValue} is already a {@code IntegerExpression}, it
     * will be returned. Otherwise a new
     * {@link javafx.beans.binding.IntegerBinding} is created that is bound to
     * the {@code ObservableIntegerValue}.
     *
     * @param value
     *            The source {@code ObservableIntegerValue}
     * @return A {@code IntegerExpression} that wraps the
     *         {@code ObservableIntegerValue} if necessary
     * @throws NullPointerException
     *             if {@code value} is {@code null}
     */
    public static IntegerExpression integerExpression(
            final ObservableIntegerValue value) {
        if (value == null) {
            throw new NullPointerException("Value must be specified.");
        }
        return (value instanceof IntegerExpression) ? (IntegerExpression) value
                : new IntegerBinding() {
                    {
                        super.bind(value);
                    }

                    @Override
                    public void dispose() {
                        super.unbind(value);
                    }

                    @Override
                    protected int computeValue() {
                        return value.get();
                    }

                    @Override
                    public ObservableList<ObservableIntegerValue> getDependencies() {
                        return FXCollections.singletonObservableList(value);
                    }
                };
    }

    /**
     * Returns an {@code IntegerExpression} that wraps an
     * {@link javafx.beans.value.ObservableValue}. If the
     * {@code ObservableValue} is already a {@code IntegerExpression}, it
     * will be returned. Otherwise a new
     * {@link javafx.beans.binding.IntegerBinding} is created that is bound to
     * the {@code ObservableValue}.
     *
     * <p>
     * Note: this method can be used to convert an {@link ObjectExpression} or
     * {@link javafx.beans.property.ObjectProperty} of specific number type to IntegerExpression, which
     * is essentially an {@code ObservableValue<Number>}. See sample below.
     *
     * <blockquote><pre>
     *   IntegerProperty integerProperty = new SimpleIntegerProperty(1);
     *   ObjectProperty&lt;Integer&gt; objectProperty = new SimpleObjectProperty&lt;&gt;(2);
     *   BooleanBinding binding = integerProperty.greaterThan(IntegerExpression.integerExpression(objectProperty));
     * </pre></blockquote>
     *
     * Note: null values will be interpreted as 0
     *
     * @param <T> The type of Number to be wrapped
     * @param value
     *            The source {@code ObservableValue}
     * @return A {@code IntegerExpression} that wraps the
     *         {@code ObservableValue} if necessary
     * @throws NullPointerException
     *             if {@code value} is {@code null}
     * @since JavaFX 8.0
     */
    public static <T extends Number> IntegerExpression integerExpression(final ObservableValue<T> value) {
        if (value == null) {
            throw new NullPointerException("Value must be specified.");
        }
        return (value instanceof IntegerExpression) ? (IntegerExpression) value
                : new IntegerBinding() {
            {
                super.bind(value);
            }

            @Override
            public void dispose() {
                super.unbind(value);
            }

            @Override
            protected int computeValue() {
                final T val = value.getValue();
                return val == null ? 0 : val.intValue();
            }

            @Override
            public ObservableList<ObservableValue<T>> getDependencies() {
                return FXCollections.singletonObservableList(value);
            }
        };
    }


    @Override
    public IntegerBinding negate() {
        return (IntegerBinding) Bindings.negate(this);
    }

    @Override
    public DoubleBinding add(final double other) {
        return Bindings.add(this, other);
    }

    @Override
    public FloatBinding add(final float other) {
        return (FloatBinding) Bindings.add(this, other);
    }

    @Override
    public LongBinding add(final long other) {
        return (LongBinding) Bindings.add(this, other);
    }

    @Override
    public IntegerBinding add(final int other) {
        return (IntegerBinding) Bindings.add(this, other);
    }

    @Override
    public DoubleBinding subtract(final double other) {
        return Bindings.subtract(this, other);
    }

    @Override
    public FloatBinding subtract(final float other) {
        return (FloatBinding) Bindings.subtract(this, other);
    }

    @Override
    public LongBinding subtract(final long other) {
        return (LongBinding) Bindings.subtract(this, other);
    }

    @Override
    public IntegerBinding subtract(final int other) {
        return (IntegerBinding) Bindings.subtract(this, other);
    }

    @Override
    public DoubleBinding multiply(final double other) {
        return Bindings.multiply(this, other);
    }

    @Override
    public FloatBinding multiply(final float other) {
        return (FloatBinding) Bindings.multiply(this, other);
    }

    @Override
    public LongBinding multiply(final long other) {
        return (LongBinding) Bindings.multiply(this, other);
    }

    @Override
    public IntegerBinding multiply(final int other) {
        return (IntegerBinding) Bindings.multiply(this, other);
    }

    @Override
    public DoubleBinding divide(final double other) {
        return Bindings.divide(this, other);
    }

    @Override
    public FloatBinding divide(final float other) {
        return (FloatBinding) Bindings.divide(this, other);
    }

    @Override
    public LongBinding divide(final long other) {
        return (LongBinding) Bindings.divide(this, other);
    }

    @Override
    public IntegerBinding divide(final int other) {
        return (IntegerBinding) Bindings.divide(this, other);
    }

    /**
     * Creates an {@link javafx.beans.binding.ObjectExpression} that holds the value
     * of this {@code IntegerExpression}. If the
     * value of this {@code IntegerExpression} changes, the value of the
     * {@code ObjectExpression} will be updated automatically.
     *
     * @return the new {@code ObjectExpression}
     * @since JavaFX 8.0
     */
    public ObjectExpression<Integer> asObject() {
        return new ObjectBinding<Integer>() {
            {
                bind(IntegerExpression.this);
            }

            @Override
            public void dispose() {
                unbind(IntegerExpression.this);
            }

            @Override
            protected Integer computeValue() {
                return IntegerExpression.this.getValue();
            }
        };
    }
}
