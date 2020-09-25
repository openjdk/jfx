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

import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableNumberValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.value.ObservableValue;

/**
 * {@code DoubleExpression} is an
 * {@link javafx.beans.value.ObservableDoubleValue} plus additional convenience
 * methods to generate bindings in a fluent style.
 * <p>
 * A concrete sub-class of {@code DoubleExpression} has to implement the method
 * {@link javafx.beans.value.ObservableDoubleValue#get()}, which provides the
 * actual value of this expression.
 * @since JavaFX 2.0
 */
public abstract class DoubleExpression extends NumberExpressionBase implements
        ObservableDoubleValue {

    /**
     * Creates a default {@code DoubleExpression}.
     */
    public DoubleExpression() {
    }

    @Override
    public int intValue() {
        return (int) get();
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
        return get();
    }

    @Override
    public Double getValue() {
        return get();
    }

    /**
     * Returns a {@code DoubleExpression} that wraps a
     * {@link javafx.beans.value.ObservableDoubleValue}. If the
     * {@code ObservableDoubleValue} is already a {@code DoubleExpression}, it
     * will be returned. Otherwise a new
     * {@link javafx.beans.binding.DoubleBinding} is created that is bound to
     * the {@code ObservableDoubleValue}.
     *
     * @param value
     *            The source {@code ObservableDoubleValue}
     * @return A {@code DoubleExpression} that wraps the
     *         {@code ObservableDoubleValue} if necessary
     * @throws NullPointerException
     *             if {@code value} is {@code null}
     */
    public static DoubleExpression doubleExpression(
            final ObservableDoubleValue value) {
        if (value == null) {
            throw new NullPointerException("Value must be specified.");
        }
        return (value instanceof DoubleExpression) ? (DoubleExpression) value
                : new DoubleBinding() {
                    {
                        super.bind(value);
                    }

                    @Override
                    public void dispose() {
                        super.unbind(value);
                    }

                    @Override
                    protected double computeValue() {
                        return value.get();
                    }

                    @Override
                    public ObservableList<ObservableDoubleValue> getDependencies() {
                        return FXCollections.singletonObservableList(value);
                    }
                };
    }

    /**
     * Returns a {@code DoubleExpression} that wraps an
     * {@link javafx.beans.value.ObservableValue}. If the
     * {@code ObservableValue} is already a {@code DoubleExpression}, it
     * will be returned. Otherwise a new
     * {@link javafx.beans.binding.DoubleBinding} is created that is bound to
     * the {@code ObservableValue}.
     *
     * <p>
     * Note: this method can be used to convert an {@link ObjectExpression} or
     * {@link javafx.beans.property.ObjectProperty} of specific number type to DoubleExpression, which
     * is essentially an {@code ObservableValue<Number>}. See sample below.
     *
     * <blockquote><pre>
     *   DoubleProperty doubleProperty = new SimpleDoubleProperty(1.0);
     *   ObjectProperty&lt;Double&gt; objectProperty = new SimpleObjectProperty&lt;&gt;(2.0);
     *   BooleanBinding binding = doubleProperty.greaterThan(DoubleExpression.doubleExpression(objectProperty));
     * </pre></blockquote>
     *
     * Note: null values will be interpreted as 0.0
     *
     * @param <T> The type of Number to be wrapped
     * @param value
     *            The source {@code ObservableValue}
     * @return A {@code DoubleExpression} that wraps the
     *         {@code ObservableValue} if necessary
     * @throws NullPointerException
     *             if {@code value} is {@code null}
     * @since JavaFX 8.0
     */
    public static <T extends Number> DoubleExpression doubleExpression(final ObservableValue<T> value) {
        if (value == null) {
            throw new NullPointerException("Value must be specified.");
        }
        return (value instanceof DoubleExpression) ? (DoubleExpression) value
                : new DoubleBinding() {
            {
                super.bind(value);
            }

            @Override
            public void dispose() {
                super.unbind(value);
            }

            @Override
            protected double computeValue() {
                final T val = value.getValue();
                return val == null ? 0.0 : val.doubleValue();
            }

            @Override
            public ObservableList<ObservableValue<T>> getDependencies() {
                return FXCollections.singletonObservableList(value);
            }
        };
    }

    @Override
    public DoubleBinding negate() {
        return (DoubleBinding) Bindings.negate(this);
    }

    @Override
    public DoubleBinding add(final ObservableNumberValue other) {
        return (DoubleBinding) Bindings.add(this, other);
    }

    @Override
    public DoubleBinding add(final double other) {
        return Bindings.add(this, other);
    }

    @Override
    public DoubleBinding add(final float other) {
        return (DoubleBinding) Bindings.add(this, other);
    }

    @Override
    public DoubleBinding add(final long other) {
        return (DoubleBinding) Bindings.add(this, other);
    }

    @Override
    public DoubleBinding add(final int other) {
        return (DoubleBinding) Bindings.add(this, other);
    }

    @Override
    public DoubleBinding subtract(final ObservableNumberValue other) {
        return (DoubleBinding) Bindings.subtract(this, other);
    }

    @Override
    public DoubleBinding subtract(final double other) {
        return Bindings.subtract(this, other);
    }

    @Override
    public DoubleBinding subtract(final float other) {
        return (DoubleBinding) Bindings.subtract(this, other);
    }

    @Override
    public DoubleBinding subtract(final long other) {
        return (DoubleBinding) Bindings.subtract(this, other);
    }

    @Override
    public DoubleBinding subtract(final int other) {
        return (DoubleBinding) Bindings.subtract(this, other);
    }

    @Override
    public DoubleBinding multiply(final ObservableNumberValue other) {
        return (DoubleBinding) Bindings.multiply(this, other);
    }

    @Override
    public DoubleBinding multiply(final double other) {
        return Bindings.multiply(this, other);
    }

    @Override
    public DoubleBinding multiply(final float other) {
        return (DoubleBinding) Bindings.multiply(this, other);
    }

    @Override
    public DoubleBinding multiply(final long other) {
        return (DoubleBinding) Bindings.multiply(this, other);
    }

    @Override
    public DoubleBinding multiply(final int other) {
        return (DoubleBinding) Bindings.multiply(this, other);
    }

    @Override
    public DoubleBinding divide(final ObservableNumberValue other) {
        return (DoubleBinding) Bindings.divide(this, other);
    }

    @Override
    public DoubleBinding divide(final double other) {
        return Bindings.divide(this, other);
    }

    @Override
    public DoubleBinding divide(final float other) {
        return (DoubleBinding) Bindings.divide(this, other);
    }

    @Override
    public DoubleBinding divide(final long other) {
        return (DoubleBinding) Bindings.divide(this, other);
    }

    @Override
    public DoubleBinding divide(final int other) {
        return (DoubleBinding) Bindings.divide(this, other);
    }

    /**
     * Creates an {@link javafx.beans.binding.ObjectExpression} that holds the value
     * of this {@code DoubleExpression}. If the
     * value of this {@code DoubleExpression} changes, the value of the
     * {@code ObjectExpression} will be updated automatically.
     *
     * @return the new {@code ObjectExpression}
     * @since JavaFX 8.0
     */
    public ObjectExpression<Double> asObject() {
        return new ObjectBinding<Double>() {
            {
                bind(DoubleExpression.this);
            }

            @Override
            public void dispose() {
                unbind(DoubleExpression.this);
            }

            @Override
            protected Double computeValue() {
                return DoubleExpression.this.getValue();
            }
        };
    }
}
