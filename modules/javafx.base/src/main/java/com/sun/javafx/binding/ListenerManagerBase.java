/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.binding;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * Base class for managing a single data field of type {@link Object} to store zero,
 * one or more {@link InvalidationListener}s and {@link ChangeListener}s. This
 * helps to minimize the storage requirements for keeping track of these
 * listeners.
 *
 * @param <T> the type of the values
 * @param <I> the type of the instance providing listener data
 */
public sealed abstract class ListenerManagerBase<T, I extends ObservableValue<? extends T>>
    permits ListenerManager, OldValueCachingListenerManager {

    /**
     * Gets the listener data under management.
     *
     * @param instance the instance it is located in, cannot be {@code null}
     * @return the listener data, can be {@code null}
     * @throws NullPointerException when {@code instance} is {@code null}
     */
    protected abstract Object getData(I instance);

    /**
     * Sets the listener data under management.
     *
     * @param instance the instance it is located in, cannot be {@code null}
     * @param data the data to set, can be {@code null}
     * @throws NullPointerException when {@code instance} is {@code null}
     */
    protected abstract void setData(I instance, Object data);

    /**
     * Adds an invalidation listener.
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param listener a listener to add, cannot be {@code null}
     * @throws NullPointerException when any argument is {@code null}
     */
    protected abstract void addInvalidationListener(I instance, InvalidationListener listener);

    /**
     * Adds a change listener.
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param listener a listener to add, cannot be {@code null}
     * @throws NullPointerException when any argument is {@code null}
     */
    protected abstract void addChangeListener(I instance, ChangeListener<? super T> listener);

    /**
     * Removes an invalidation listener.
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param listener a listener to remove, cannot be {@code null}
     * @return {@code true} if there are no more listeners registered after this call completes, otherwise {@code false}
     * @throws NullPointerException when any argument is {@code null}
     */
    protected abstract boolean removeInvalidationListener(I instance, InvalidationListener listener);

    /**
     * Removes a change listener.
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param listener a listener to remove, cannot be {@code null}
     * @return {@code true} if there are no more listeners registered after this call completes, otherwise {@code false}
     * @throws NullPointerException when any argument is {@code null}
     */
    protected abstract boolean removeChangeListener(I instance, ChangeListener<? super T> listener);

    /**
     * Adds an invalidation listener.
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param listener a listener to add, cannot be {@code null}
     * @throws NullPointerException when any argument is {@code null}
     */
    public final void addListener(I instance, InvalidationListener listener) {
        addInvalidationListener(instance, wrapIfDualPurpose(listener));
    }

    /**
     * Adds a change listener.
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param listener a listener to add, cannot be {@code null}
     * @throws NullPointerException when any argument is {@code null}
     */
    public final void addListener(I instance, ChangeListener<? super T> listener) {
        addChangeListener(instance, wrapIfDualPurpose(listener));
    }

    /**
     * Removes an invalidation listener.
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param listener a listener to remove, cannot be {@code null}
     * @return {@code true} if there are no more listeners registered after this call completes, otherwise {@code false}
     * @throws NullPointerException when any argument is {@code null}
     */
    public final boolean removeListener(I instance, InvalidationListener listener) {
        return removeInvalidationListener(instance, wrapIfDualPurpose(listener));
    }

    /**
     * Removes a change listener.
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param listener a listener to remove, cannot be {@code null}
     * @return {@code true} if there are no more listeners registered after this call completes, otherwise {@code false}
     * @throws NullPointerException when any argument is {@code null}
     */
    public final boolean removeListener(I instance, ChangeListener<? super T> listener) {
        return removeChangeListener(instance, wrapIfDualPurpose(listener));
    }

    /*
     * These two functions wrap listeners that implement both ChangeListener and
     * InvalidationListener. Since the permitted classes and the underlying ListenerList use
     * instanceof checks to distinguish the exact types, wrapping is necessary to
     * ensure the dual purpose listener is not mistaken for the incorrect type.
     *
     * The wrappers delegate equals/hashCode to the delegate to ensure a wrapped
     * listener is also removable again.
     */

    private static <T> ChangeListener<? super T> wrapIfDualPurpose(ChangeListener<? super T> changeListener) {
        if (changeListener instanceof InvalidationListener) {
            return new WrappedChangeListener<>(changeListener);
        }

        return changeListener;
    }

    private static InvalidationListener wrapIfDualPurpose(InvalidationListener invalidationListener) {
        if (invalidationListener instanceof ChangeListener) {
            return new WrappedInvalidationListener(invalidationListener);
        }

        return invalidationListener;
    }

    private static final class WrappedChangeListener<T> implements ChangeListener<T> {
        final ChangeListener<? super T> delegate;

        WrappedChangeListener(ChangeListener<? super T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
            delegate.changed(observable, oldValue, newValue);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof WrappedChangeListener<?> w && delegate.equals(w.delegate);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }

    private static final class WrappedInvalidationListener implements InvalidationListener {
        final InvalidationListener delegate;

        WrappedInvalidationListener(InvalidationListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void invalidated(Observable observable) {
            delegate.invalidated(observable);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof WrappedInvalidationListener w && delegate.equals(w.delegate);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }
}
