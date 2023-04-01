/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.value.ObservableFloatValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.value.ObservableValue;

/**
 * {@code FloatExpression} is an
 * {@link javafx.beans.value.ObservableFloatValue} plus additional convenience
 * methods to generate bindings in a fluent style.
 * <p>
 * A concrete sub-class of {@code FloatExpression} has to implement the method
 * {@link javafx.beans.value.ObservableFloatValue#get()}, which provides the
 * actual value of this expression.
 * @since JavaFX 2.0
 */
public abstract class FloatExpression extends NumberExpressionBase implements
        ObservableFloatValue {

    /**
     * Creates a default {@code FloatExpression}.
     */
    public FloatExpression() {
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
        return get();
    }

    @Override
    public double doubleValue() {
        return get();
    }

    @Override
    public Float getValue() {
        return get();
    }

    /**
     * Returns a {@code FloatExpression} that wraps a
     * {@link javafx.beans.value.ObservableFloatValue}. If the
     * {@code ObservableFloatValue} is already a {@code FloatExpression}, it
     * will be returned. Otherwise a new
     * {@link javafx.beans.binding.FloatBinding} is created that is bound to the
     * {@code ObservableFloatValue}.
     *
     * @param value
     *            The source {@code ObservableFloatValue}
     * @return A {@code FloatExpression} that wraps the
     *         {@code ObservableFloatValue} if necessary
     * @throws NullPointerException
     *             if {@code value} is {@code null}
     */
    public static FloatExpression floatExpression(
            final ObservableFloatValue value) {
        if (value == null) {
            throw new NullPointerException("Value must be specified.");
        }
        return (value instanceof FloatExpression) ? (FloatExpression) value
                : new FloatBinding() {
                    {
                        super.bind(value);
                    }

                    @Override
                    public void dispose() {
                        super.unbind(value);
                    }

                    @Override
                    protected float computeValue() {
                        return value.get();
                    }

                    @Override
                    public ObservableList<ObservableFloatValue> getDependencies() {
                        return FXCollections.singletonObservableList(value);
                    }
                };
    }

    /**
     * Returns a {@code FloatExpression} that wraps an
     * {@link javafx.beans.value.ObservableValue}. If the
     * {@code ObservableValue} is already a {@code FloatExpression}, it
     * will be returned. Otherwise a new
     * {@link javafx.beans.binding.FloatBinding} is created that is bound to
     * the {@code ObservableValue}.
     *
     * <p>
     * Note: this method can be used to convert an {@link ObjectExpression} or
     * {@link javafx.beans.property.ObjectProperty} of specific number type to FloatExpression, which
     * is essentially an {@code ObservableValue<Number>}. See sample below.
     *
     * <blockquote><pre>
     *   FloatProperty floatProperty = new SimpleFloatProperty(1.0f);
     *   ObjectProperty&lt;Float&gt; objectProperty = new SimpleObjectProperty&lt;&gt;(2.0f);
     *   BooleanBinding binding = floatProperty.greaterThan(FloatExpression.floatExpression(objectProperty));
     * </pre></blockquote>
     *
     *  Note: null values will be interpreted as 0f
     *
     * @param <T> The type of Number to be wrapped
     * @param value
     *            The source {@code ObservableValue}
     * @return A {@code FloatExpression} that wraps the
     *         {@code ObservableValue} if necessary
     * @throws NullPointerException
     *             if {@code value} is {@code null}
     * @since JavaFX 8.0
     */
    public static <T extends Number> FloatExpression floatExpression(final ObservableValue<T> value) {
        if (value == null) {
            throw new NullPointerException("Value must be specified.");
        }
        return (value instanceof FloatExpression) ? (FloatExpression) value
                : new FloatBinding() {
            {
                super.bind(value);
            }

            @Override
            public void dispose() {
                super.unbind(value);
            }

            @Override
            protected float computeValue() {
                final T val = value.getValue();
                return val == null ? 0f :  val.floatValue();
            }

            @Override
            public ObservableList<ObservableValue<T>> getDependencies() {
                return FXCollections.singletonObservableList(value);
            }
        };
    }


    @Override
    public FloatBinding negate() {
        return (FloatBinding) Bindings.negate(this);
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
    public FloatBinding add(final long other) {
        return (FloatBinding) Bindings.add(this, other);
    }

    @Override
    public FloatBinding add(final int other) {
        return (FloatBinding) Bindings.add(this, other);
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
    public FloatBinding subtract(final long other) {
        return (FloatBinding) Bindings.subtract(this, other);
    }

    @Override
    public FloatBinding subtract(final int other) {
        return (FloatBinding) Bindings.subtract(this, other);
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
    public FloatBinding multiply(final long other) {
        return (FloatBinding) Bindings.multiply(this, other);
    }

    @Override
    public FloatBinding multiply(final int other) {
        return (FloatBinding) Bindings.multiply(this, other);
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
    public FloatBinding divide(final long other) {
        return (FloatBinding) Bindings.divide(this, other);
    }

    @Override
    public FloatBinding divide(final int other) {
        return (FloatBinding) Bindings.divide(this, other);
    }

    /**
     * Creates an {@link javafx.beans.binding.ObjectExpression} that holds the value
     * of this {@code FloatExpression}. If the
     * value of this {@code FloatExpression} changes, the value of the
     * {@code ObjectExpression} will be updated automatically.
     *
     * @return the new {@code ObjectExpression}
     * @since JavaFX 8.0
     */
    public ObjectExpression<Float> asObject() {
        return new ObjectBinding<>() {
            {
                bind(FloatExpression.this);
            }

            @Override
            public void dispose() {
                unbind(FloatExpression.this);
            }

            @Override
            protected Float computeValue() {
                return FloatExpression.this.getValue();
            }
        };
    }
}
