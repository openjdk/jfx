/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.collections.annotations.ReturnsUnmodifiableCollection;

/**
 * A {@code DoubleExpression} is a
 * {@link javafx.beans.value.ObservableDoubleValue} plus additional convenience
 * methods to generate bindings in a fluent style.
 * <p>
 * A concrete sub-class of {@code DoubleExpression} has to implement the method
 * {@link javafx.beans.value.ObservableDoubleValue#get()}, which provides the
 * actual value of this expression.
 */
public abstract class DoubleExpression extends NumberExpressionBase implements
        ObservableDoubleValue {

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
                    @ReturnsUnmodifiableCollection
                    public ObservableList<ObservableDoubleValue> getDependencies() {
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
}
