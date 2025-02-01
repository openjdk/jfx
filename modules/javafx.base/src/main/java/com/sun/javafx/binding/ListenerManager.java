/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Objects;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * Manages a single data field of type {@link Object} to store zero,
 * one or more {@link InvalidationListener}s and {@link ChangeListener}s. This
 * helps to minimize the storage requirements for keeping track of these
 * listeners.<p>
 *
 * When there are no listeners, the field will be {@code null}. When there is
 * only a single invalidation listener, the field will contain only that
 * listener (change listeners are wrapped to track old value). When there are more
 * than one listeners, the field will hold a {@link ListenerList}. It is recommended
 * to never inspect this field directly but always use this manager to
 * interact with it.
 *
 * @param <T> the type of the values
 * @param <I> the type of the instance providing listener data
 */
public abstract class ListenerManager<T, I extends ObservableValue<? extends T>> extends ListenerManagerBase<T, I> {

    /**
     * Adds an invalidation listener.
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param listener a listener to add, cannot be {@code null}
     * @throws NullPointerException when listener is {@code null}
     */
    public void addListener(I instance, InvalidationListener listener) {
        Objects.requireNonNull(listener);

        instance.getValue();  // always trigger validation when adding an invalidation listener (required by tests)

        Object data = getData(instance);

        if (data == null) {
            setData(instance, listener);
        }
        else if (data instanceof ListenerList<?> list) {
            list.add(listener);
        }
        else {
            setData(instance, new ListenerList<>(data, listener));
        }
    }

    /**
     * Adds a change listener.
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param listener a listener to add, cannot be {@code null}
     * @throws NullPointerException when listener is {@code null}
     */
    public void addListener(I instance, ChangeListener<? super T> listener) {
        Objects.requireNonNull(listener);

        instance.getValue();  // always trigger validation when adding a change listener (required by tests)

        Object data = getData(instance);

        if (data == null) {
            setData(instance, listener);
        }
        else if (data instanceof ListenerList<?> list) {
            list.add(listener);
        }
        else {
            setData(instance, new ListenerList<>(data, listener));
        }
    }

    /**
     * Removes a listener.
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param listener a listener to remove, cannot be {@code null}
     * @return {@code true} if there are no more listeners registered after this call completes, otherwise {@code false}
     * @throws NullPointerException when listener is {@code null}
     */
    public boolean removeListener(I instance, Object listener) {
        Objects.requireNonNull(listener);

        Object data = getData(instance);

        if (data == null) {
            return true;
        }

        if (data.equals(listener)) {
            setData(instance, null);

            return true;
        }

        if (data instanceof ListenerList<?> list) {
            list.remove(listener);

            updateAfterRemoval(instance, list);

            return list.totalListeners() == 0;
        }

        return false;
    }

    /**
     * Notifies the listeners managed in the given instance.
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param oldValue the previous value before this change occurred, can be {@code null}
     */
    public void fireValueChanged(I instance, T oldValue) {
        Object data = getData(instance);

        if (data instanceof ListenerList) {
            @SuppressWarnings("unchecked")
            ListenerList<T> list = (ListenerList<T>)data;

            callMultipleListeners(instance, list, oldValue);
        }
        else if (data instanceof InvalidationListener il) {
            ListenerListBase.callInvalidationListener(instance, il);
        }
        else if (data instanceof ChangeListener) {
            @SuppressWarnings("unchecked")
            ChangeListener<T> cl = (ChangeListener<T>) data;
            T newValue = instance.getValue();  // Required as an earlier listener may have changed the value, and current value is always needed

            if (!Objects.equals(newValue, oldValue)) {
                ListenerListBase.callChangeListener(instance, cl, oldValue, newValue);
            }
        }
    }

    private void callMultipleListeners(I instance, ListenerList<T> list, T oldValue) {
        boolean modified = list.notifyListeners(instance, oldValue);

        if (modified) {  // if modified, compact the data field if possible
            updateAfterRemoval(instance, list);
        }
    }

    private void updateAfterRemoval(I instance, ListenerList<?> list) {
        int invalidationListenersSize = list.invalidationListenersSize();
        int changeListenersSize = list.changeListenersSize();

        if (invalidationListenersSize + changeListenersSize <= 1) {
            if (invalidationListenersSize == 1) {
                setData(instance, list.getInvalidationListener(0));
            }
            else if (changeListenersSize == 1) {
                setData(instance, list.getChangeListener(0));
            }
            else {
                setData(instance, null);
            }
        }
    }
}