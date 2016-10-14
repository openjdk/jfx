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

package javafx.beans.binding;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.sun.javafx.binding.BindingHelperObserver;
import com.sun.javafx.binding.ExpressionHelper;

/**
 * Base class that provides most of the functionality needed to implement a
 * {@link Binding} of a {@code double} value.
 * <p>
 * {@code DoubleBinding} provides a simple invalidation-scheme. An extending
 * class can register dependencies by calling {@link #bind(Observable...)}.
 * If One of the registered dependencies becomes invalid, this
 * {@code DoubleBinding} is marked as invalid. With
 * {@link #unbind(Observable...)} listening to dependencies can be stopped.
 * <p>
 * To provide a concrete implementation of this class, the method
 * {@link #computeValue()} has to be implemented to calculate the value of this
 * binding based on the current state of the dependencies. It is called when
 * {@link #get()} is called for an invalid binding.
 * <p>
 * Below is a simple example of a {@code DoubleBinding} calculating the square-
 * root of a {@link javafx.beans.value.ObservableNumberValue} {@code moo}.
 *
 * <pre>
 * <code>
 * final ObservableDoubleValue moo = ...;
 *
 * DoubleBinding foo = new DoubleBinding() {
 *
 *     {
 *         super.bind(moo);
 *     }
 *
 *     &#x40;Override
 *     protected double computeValue() {
 *         return Math.sqrt(moo.getValue());
 *     }
 * };
 * </code>
 * </pre>
 *
 * Following is the same example with implementations for the optional methods
 * {@link Binding#getDependencies()} and {@link Binding#dispose()}.
 *
 * <pre>
 * <code>
 * final ObservableDoubleValue moo = ...;
 *
 * DoubleBinding foo = new DoubleBinding() {
 *
 *     {
 *         super.bind(moo);
 *     }
 *
 *     &#x40;Override
 *     protected double computeValue() {
 *         return Math.sqrt(moo.getValue());
 *     }
 *
 *     &#x40;Override
 *     public ObservableList&lt;?&gt; getDependencies() {
 *         return FXCollections.singletonObservableList(moo);
 *     }
 *
 *     &#x40;Override
 *     public void dispose() {
 *         super.unbind(moo);
 *     }
 * };
 * </code>
 * </pre>
 *
 * @see Binding
 * @see NumberBinding
 * @see javafx.beans.binding.DoubleExpression
 *
 *
 * @since JavaFX 2.0
 */
public abstract class DoubleBinding extends DoubleExpression implements
        NumberBinding {

    private double value;
    private boolean valid;
    private BindingHelperObserver observer;
    private ExpressionHelper<Number> helper = null;

    @Override
    public void addListener(InvalidationListener listener) {
        helper = ExpressionHelper.addListener(helper, this, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        helper = ExpressionHelper.removeListener(helper, listener);
    }

    @Override
    public void addListener(ChangeListener<? super Number> listener) {
        helper = ExpressionHelper.addListener(helper, this, listener);
    }

    @Override
    public void removeListener(ChangeListener<? super Number> listener) {
        helper = ExpressionHelper.removeListener(helper, listener);
    }

    /**
     * Start observing the dependencies for changes. If the value of one of the
     * dependencies changes, the binding is marked as invalid.
     *
     * @param dependencies
     *            the dependencies to observe
     */
    protected final void bind(Observable... dependencies) {
        if ((dependencies != null) && (dependencies.length > 0)) {
            if (observer == null) {
                observer = new BindingHelperObserver(this);
            }
            for (final Observable dep : dependencies) {
                dep.addListener(observer);
            }
        }
    }

    /**
     * Stop observing the dependencies for changes.
     *
     * @param dependencies
     *            the dependencies to stop observing
     */
    protected final void unbind(Observable... dependencies) {
        if (observer != null) {
            for (final Observable dep : dependencies) {
                dep.removeListener(observer);
            }
            observer = null;
        }
    }

    /**
     * A default implementation of {@code dispose()} that is empty.
     */
    @Override
    public void dispose() {
    }

    /**
     * A default implementation of {@code getDependencies()} that returns an
     * empty {@link javafx.collections.ObservableList}.
     *
     * @return an empty {@code ObservableList}
     */
    @Override
    public ObservableList<?> getDependencies() {
        return FXCollections.emptyObservableList();
    }

    /**
     * Returns the result of {@link #computeValue()}. The method
     * {@code computeValue()} is only called if the binding is invalid. The
     * result is cached and returned if the binding did not become invalid since
     * the last call of {@code get()}.
     *
     * @return the current value
     */
    @Override
    public final double get() {
        if (!valid) {
            value = computeValue();
            valid = true;
        }
        return value;
    }

    /**
     * The method onInvalidating() can be overridden by extending classes to
     * react, if this binding becomes invalid. The default implementation is
     * empty.
     */
    protected void onInvalidating() {
    }

    @Override
    public final void invalidate() {
        if (valid) {
            valid = false;
            onInvalidating();
            ExpressionHelper.fireValueChangedEvent(helper);
        }
    }

    @Override
    public final boolean isValid() {
        return valid;
    }

    /**
     * Calculates the current value of this binding.
     * <p>
     * Classes extending {@code DoubleBinding} have to provide an implementation
     * of {@code computeValue}.
     *
     * @return the current value
     */
    protected abstract double computeValue();

    /**
     * Returns a string representation of this {@code DoubleBinding} object.
     * @return a string representation of this {@code DoubleBinding} object.
     */
    @Override
    public String toString() {
        return valid ? "DoubleBinding [value: " + get() + "]"
                : "DoubleBinding [invalid]";
    }
}
