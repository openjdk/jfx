/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
 * than one listeners, the field will hold a {@link OldValueCachingListenerList}. It
 * is recommended to never inspect this field directly but always use this manager to
 * interact with it.<p>
 *
 * This is a variant of {@link ListenerManager} which caches the latest value. This
 * means that a single {@link ChangeListener} will require a wrapper and that
 * an extra field is needed within listener list. If possible use {@link ListenerManager},
 * as it has less storage requirements and is faster.
 *
 * @param <T> the type of the values
 * @param <I> the type of the instance providing listener data
 */
public abstract class OldValueCachingListenerManager<T, I extends ObservableValue<T>> {

    /**
     * Gets the listener data under management.
     *
     * @param instance the instance it is located in, never {@code null}
     * @return the listener data, can be {@code null}
     */
    protected abstract Object getData(I instance);

    /**
     * Sets the listener data under management.
     *
     * @param instance the instance it is located in, never {@code null}
     * @param data the data to set, can be {@code null}
     */
    protected abstract void setData(I instance, Object data);

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
        else if (data instanceof OldValueCachingListenerList<?> list) {
            list.remove(listener);

            if (list.size() == 1) {
                Object leftOverListener = list.get(0);

                if (leftOverListener instanceof ChangeListener<?> cl) {
                    setData(instance, new ChangeListenerWrapper<>(cl, list.getLatestValue()));
                }
                else {
                    setData(instance, leftOverListener);
                }
            }
            else if (!list.hasChangeListeners()) {
                list.putLatestValue(null);  // clear to avoid references
            }
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
            callInvalidationListener(instance, il);
        }
        else if (data instanceof ChangeListenerWrapper) {
            @SuppressWarnings("unchecked")
            ChangeListenerWrapper<T> clw = (ChangeListenerWrapper<T>) data;

            callWrappedChangeListener(instance, clw);
        }
    }

    private void callMultipleListeners(I instance, OldValueCachingListenerList<T> list) {
        boolean topLevel = !list.isLocked();

        // when nested, only notify as many listeners as were already notified:
        int max = topLevel ? list.size() : list.getProgress() + 1;

        int count = list.invalidationListenersSize();
        int maxInvalidations = Math.min(count, max);

        for (int i = 0; i < maxInvalidations; i++) {
            InvalidationListener listener = list.getInvalidationListener(i);

            if (listener == null) {
                continue;
            }

            list.setProgress(i);

            callInvalidationListener(instance, listener);
        }

        if (count < max) {
            max -= count;

            T oldValue = list.getLatestValue();

            // Latest value must be store before calling the listener, as nested loop will need to know what the old value was
            // Latest value should even be stored if it was "equals", as it may be a different reference
            list.putLatestValue(instance.getValue());

            for (int i = 0; i < max; i++) {
                ChangeListener<T> listener = list.getChangeListener(i);

                if (listener == null) {
                    continue;
                }

                T newValue = list.getLatestValue();

                if (Objects.equals(newValue, oldValue)) {
                    break;
                }

                list.setProgress(i + count);

                try {
                    listener.changed(instance, oldValue, newValue);  // Old value must be the same for all listeners in this loop
                }
                catch (Exception e) {
                    Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                }
            }
        }

        if (topLevel && list.isLocked()) {
            unlock(instance);
        }
    }

    private void callWrappedChangeListener(I instance, ChangeListenerWrapper<T> changeListenerWrapper) {
        T oldValue = changeListenerWrapper.getLatestValue();
        T value = instance.getValue();  // Required as an earlier listener may have changed the value, and current value is always needed

        // Latest value must be store before calling the listener, as nested loop will need to know what the old value was
        // Latest value should even be stored if it was "equals", as it may be a different reference
        changeListenerWrapper.putLatestValue(value);

        if (!Objects.equals(value, oldValue)) {
            try {
                changeListenerWrapper.changed(instance, oldValue, value);
            }
            catch (Exception e) {
                Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
            }
        }
    }

    private void callInvalidationListener(I instance, InvalidationListener listener) {
        try {
            listener.invalidated(instance);
        }
        catch (Exception e) {
            Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }
    }

    private void unlock(I instance) {
        Object data = getData(instance);

        if (data instanceof OldValueCachingListenerList<?> list) {
            list.unlock();

            if (list.size() == 1) {
                setData(instance, list.get(0));
            }
            else if(list.size() == 0) {
                setData(instance, null);
            }
        }
    }

    static class ChangeListenerWrapper<T> implements ChangeListener<T> {
        private final ChangeListener<T> listener;

        private T latestValue;

        ChangeListenerWrapper(ChangeListener<T> listener, Object latestValue) {
            this.listener = listener;
            this.latestValue = (T) latestValue;
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
}