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
 * listener (change listeners are wrapped to track old value). When there is more
 * than one listener, the field will hold a {@link OldValueCachingListenerList}. It
 * is recommended to never inspect this field directly but always use this manager to
 * interact with it.<p>
 *
 * This is a variant of {@link ListenerManager} which caches the latest value for
 * cases where the latest value prior to the change (ie. the old value) cannot be
 * provided by the caller itself. This means that a single {@link ChangeListener}
 * will require a wrapper to track this value, and that an extra field is needed
 * within listener list. If possible use {@link ListenerManager}, as it has less
 * storage requirements and is faster.
 *
 * @param <T> the type of the values
 * @param <I> the type of the instance providing listener data
 */
public abstract class OldValueCachingListenerManager<T, I extends ObservableValue<? extends T>> extends ListenerManagerBase<T, I> {

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
        else if (data instanceof OldValueCachingListenerList<?> list) {
            list.add(listener);
        }
        else if (data instanceof ChangeListenerWrapper) {
            @SuppressWarnings("unchecked")
            ChangeListenerWrapper<T> wrapper = (ChangeListenerWrapper<T>) data;
            OldValueCachingListenerList<T> list = new OldValueCachingListenerList<>(wrapper.listener, listener);

            list.putLatestValue(wrapper.latestValue);

            setData(instance, list);
        }
        else {
            setData(instance, new OldValueCachingListenerList<>(data, listener));
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

        Object data = getData(instance);

        if (data == null) {
            setData(instance, new ChangeListenerWrapper<>(listener, instance.getValue()));
        }
        else if (data instanceof OldValueCachingListenerList) {
            @SuppressWarnings("unchecked")
            OldValueCachingListenerList<T> list = (OldValueCachingListenerList<T>) data;

            if (!list.hasChangeListeners()) {
                list.putLatestValue(instance.getValue());
            }

            list.add(listener);
        }
        else if (data instanceof ChangeListenerWrapper) {
            @SuppressWarnings("unchecked")
            ChangeListenerWrapper<T> wrapper = (ChangeListenerWrapper<T>) data;
            OldValueCachingListenerList<T> list = new OldValueCachingListenerList<>(wrapper.listener, listener);

            list.putLatestValue(wrapper.latestValue);

            setData(instance, list);
        }
        else {
            OldValueCachingListenerList<T> list = new OldValueCachingListenerList<>(data, listener);

            list.putLatestValue(instance.getValue());

            setData(instance, list);
        }
    }

    /**
     * Removes a listener.
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param listener a listener to remove, cannot be {@code null}
     * @throws NullPointerException when listener is {@code null}
     */
    public void removeListener(I instance, Object listener) {
        Objects.requireNonNull(listener);

        Object data = getData(instance);

        if (data == null || data.equals(listener) || (data instanceof ChangeListenerWrapper<?> wrapper && wrapper.listener.equals(listener))) {
            setData(instance, null);  // TODO not needed when already null
        }
        else if (data instanceof OldValueCachingListenerList) {
            @SuppressWarnings("unchecked")
            OldValueCachingListenerList<T> list = (OldValueCachingListenerList<T>) data;

            list.remove(listener);

            updateAfterRemoval(instance, list);
        }
    }

    /**
     * Notifies the listeners managed in the given instance.<p>
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     */
    public void fireValueChanged(I instance) {
        Object data = getData(instance);

        if (data instanceof OldValueCachingListenerList) {
            @SuppressWarnings("unchecked")
            OldValueCachingListenerList<T> list = (OldValueCachingListenerList<T>) data;

            callMultipleListeners(instance, list);
        }
        else if (data instanceof InvalidationListener il) {
            ListenerListBase.callInvalidationListener(instance, il);
        }
        else if (data instanceof ChangeListenerWrapper) {
            @SuppressWarnings("unchecked")
            ChangeListenerWrapper<T> clw = (ChangeListenerWrapper<T>) data;

            callWrappedChangeListener(instance, clw);
        }
    }

    private void callMultipleListeners(I instance, OldValueCachingListenerList<T> list) {
        boolean modified = list.notifyListeners(instance);

        if (modified) {  // if modified, compact the data field if possible
            updateAfterRemoval(instance, list);
        }
    }

    private void callWrappedChangeListener(I instance, ChangeListenerWrapper<T> changeListenerWrapper) {
        T oldValue = changeListenerWrapper.getLatestValue();
        T newValue = instance.getValue();

        // Latest value must be stored even if it was "equals", as it may be a different reference
        changeListenerWrapper.putLatestValue(newValue);

        if (!Objects.equals(newValue, oldValue)) {
            ListenerListBase.callChangeListener(instance, changeListenerWrapper, oldValue, newValue);
        }
    }

    static class ChangeListenerWrapper<T> implements ChangeListener<T> {
        private final ChangeListener<T> listener;

        private T latestValue;

        ChangeListenerWrapper(ChangeListener<T> listener, T latestValue) {
            this.listener = listener;
            this.latestValue = latestValue;
        }

        T getLatestValue() {
            return latestValue;
        }

        void putLatestValue(T value) {
            this.latestValue = value;
        }

        @Override
        public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
            listener.changed(observable, oldValue, newValue);
        }
    }

    private void updateAfterRemoval(I instance, OldValueCachingListenerList<T> list) {
        int invalidationListenersSize = list.invalidationListenersSize();
        int changeListenersSize = list.changeListenersSize();

        if (invalidationListenersSize + changeListenersSize <= 1) {
            if (invalidationListenersSize == 1) {
                setData(instance, list.getInvalidationListener(0));
            }
            else if (changeListenersSize == 1) {
                setData(instance, new ChangeListenerWrapper<>(list.getChangeListener(0), list.getLatestValue()));
            }
            else {
                setData(instance, null);
            }
        }
        else if (!list.hasChangeListeners()) {
            list.putLatestValue(null);  // clear to avoid references
        }
    }
}