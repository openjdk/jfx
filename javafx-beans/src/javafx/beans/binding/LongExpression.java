/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.value.ObservableLongValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.sun.javafx.collections.annotations.ReturnsUnmodifiableCollection;

/**
 * A {@code LongExpression} is a {@link javafx.beans.value.ObservableLongValue}
 * plus additional convenience methods to generate bindings in a fluent style.
 * <p>
 * A concrete sub-class of {@code LongExpression} has to implement the method
 * {@link javafx.beans.value.ObservableLongValue#get()}, which provides the
 * actual value of this expression.
 */
public abstract class LongExpression extends NumberExpressionBase implements
        ObservableLongValue {

    @Override
    public int intValue() {
        return (int) get();
    }

    @Override
    public long longValue() {
        return get();
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
    public Long getValue() {
        return get();
    }

    /**
     * Returns a {@code LongExpression} that wraps a
     * {@link javafx.beans.value.ObservableLongValue}. If the
     * {@code ObservableLongValue} is already a {@code LongExpression}, it will
     * be returned. Otherwise a new {@link javafx.beans.binding.LongBinding} is
     * created that is bound to the {@code ObservableLongValue}.
     * 
     * @param value
     *            The source {@code ObservableLongValue}
     * @return A {@code LongExpression} that wraps the
     *         {@code ObservableLongValue} if necessary
     * @throws NullPointerException
     *             if {@code value} is {@code null}
     */
    public static LongExpression longExpression(final ObservableLongValue value) {
        if (value == null) {
            throw new NullPointerException("Value must be specified.");
        }
        return (value instanceof LongExpression) ? (LongExpression) value
                : new LongBinding() {
                    {
                        super.bind(value);
                    }

                    @Override
                    public void dispose() {
                        super.unbind(value);
                    }

                    @Override
                    protected long computeValue() {
                        return value.get();
                    }

                    @Override
                    @ReturnsUnmodifiableCollection
                    public ObservableList<ObservableLongValue> getDependencies() {
                        return FXCollections.singletonObservableList(value);
                    }
                };
    }

    @Override
    public LongBinding negate() {
        return (LongBinding) Bindings.negate(this);
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
    public LongBinding add(final int other) {
        return (LongBinding) Bindings.add(this, other);
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
    public LongBinding subtract(final int other) {
        return (LongBinding) Bindings.subtract(this, other);
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
    public LongBinding multiply(final int other) {
        return (LongBinding) Bindings.multiply(this, other);
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
    public LongBinding divide(final int other) {
        return (LongBinding) Bindings.divide(this, other);
    }
}
