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
 * than one listener, the field will hold an {@link OldValueCachingListenerList}. It
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
public non-sealed abstract class OldValueCachingListenerManager<T, I extends ObservableValue<? extends T>> extends ListenerManagerBase<T, I> {

    @Override
    protected final void addInvalidationListener(I instance, InvalidationListener listener) {
        Objects.requireNonNull(listener);

        instance.getValue();  // always trigger validation when adding an invalidation listener (required by tests)

        switch (getData(instance)) {
            case null -> setData(instance, listener);
            case OldValueCachingListenerList<?> list -> list.add(listener);
            case ChangeListenerWrapper<?> wrapper -> {
                OldValueCachingListenerList<Object> list = createListenerList(instance, wrapper.listener, listener);

                list.putLatestValue(wrapper.latestValue);
                setData(instance, list);
            }
            case Object data -> setData(instance, createListenerList(instance, data, listener));
        }
    }

    @Override
    protected final void addChangeListener(I instance, ChangeListener<? super T> listener) {
        Objects.requireNonNull(listener);

        switch (getData(instance)) {
            case null -> setData(instance, new ChangeListenerWrapper<>(listener, instance.getValue()));
            case OldValueCachingListenerList<?> genericList -> {
                @SuppressWarnings("unchecked")
                OldValueCachingListenerList<T> list = (OldValueCachingListenerList<T>) genericList;

                if (!list.hasChangeListeners()) {
                    list.putLatestValue(instance.getValue());
                }

                list.add(listener);
            }
            case ChangeListenerWrapper<?> wrapper -> {
                OldValueCachingListenerList<Object> list = createListenerList(instance, wrapper.listener, listener);

                list.putLatestValue(wrapper.latestValue);

                setData(instance, list);
            }
            case Object data -> {
                OldValueCachingListenerList<T> list = createListenerList(instance, data, listener);

                list.putLatestValue(instance.getValue());

                setData(instance, list);
            }
        }
    }

    @Override
    protected final boolean removeInvalidationListener(I instance, InvalidationListener listener) {
        Objects.requireNonNull(listener);

        Object data = getData(instance);

        if (data == null || data.equals(listener)) {
            setData(instance, null);

            return true;
        }

        if (data instanceof OldValueCachingListenerList) {
            @SuppressWarnings("unchecked")
            OldValueCachingListenerList<T> list = (OldValueCachingListenerList<T>) data;

            list.remove(listener);

            updateAfterRemoval(instance, list);

            return list.totalListeners() == 0;
        }

        return false;
    }

    @Override
    protected final boolean removeChangeListener(I instance, ChangeListener<? super T> listener) {
        Objects.requireNonNull(listener);

        Object data = getData(instance);

        if (data == null || data.equals(listener) || (data instanceof ChangeListenerWrapper<?> wrapper && wrapper.listener.equals(listener))) {
            setData(instance, null);

            return true;
        }

        if (data instanceof OldValueCachingListenerList) {
            @SuppressWarnings("unchecked")
            OldValueCachingListenerList<T> list = (OldValueCachingListenerList<T>) data;

            list.remove(listener);

            updateAfterRemoval(instance, list);

            return list.totalListeners() == 0;
        }

        return false;
    }

    /*
     * Creates a new listener list. If a new listener was added while a notification on this instance
     * is in progress, then this is detected and the new listener list is created locked. This ensures that
     * if the listener that was invoked already triggers a nested change, that we correctly first notify
     * this listener again and possibly never invoke the newly added listener (if the first listener
     * changed the value back to the original value).
     */
    private <U> OldValueCachingListenerList<U> createListenerList(I instance, Object existingListener, Object newListener) {
        if (isNotifying(instance)) {
            OldValueCachingListenerList<U> list = new OldValueCachingListenerList<>(existingListener);

            list.lock();
            list.add(newListener);

            return list;
        }

        return new OldValueCachingListenerList<>(existingListener, newListener);
    }

    /**
     * Notifies the listeners managed in the given instance.<p>
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param listenerData the listener data associated with the instance,
     *   can be {@code null} which means there are no listeners to notify
     * @throws NullPointerException when {@code instance} is {@code null}
     */
    public final void fireValueChanged(I instance, Object listenerData) {
        if (listenerData instanceof OldValueCachingListenerList) {
            @SuppressWarnings("unchecked")
            OldValueCachingListenerList<T> list = (OldValueCachingListenerList<T>) listenerData;

            callMultipleListeners(instance, list);
        }
        else if (listenerData instanceof InvalidationListener il) {
            notifyInvalidationListener(instance, il);
            unlockIfDataStorageTypeBecameList(instance);
        }
        else if (listenerData instanceof ChangeListenerWrapper) {
            @SuppressWarnings("unchecked")
            ChangeListenerWrapper<T> clw = (ChangeListenerWrapper<T>) listenerData;

            callWrappedChangeListener(instance, clw);
            unlockIfDataStorageTypeBecameList(instance);
        }
    }

    private void unlockIfDataStorageTypeBecameList(I instance) {

        /*
         * If during notification, the managed data field changed from a single listener to a list, then this
         * list was locked upon creation, and then must be unlocked here; if this was the top level unlock, then
         * we clean up any stale data that must occur after unlock as usual:
         */

        if (getData(instance) instanceof OldValueCachingListenerList<?> list && list.unlock()) {
            @SuppressWarnings("unchecked")
            OldValueCachingListenerList<T> typedList = (OldValueCachingListenerList<T>) list;

            if (typedList.hasChangeListeners()) {

                /*
                 * The list was locked for its entire existence so far, so its change listener loop may
                 * never have run (for example when an invalidation listener was the one that triggered
                 * the nested change); ensure the cached latest value reflects reality before it is relied
                 * upon again, otherwise a subsequent change may incorrectly be seen as a no-op:
                 */

                typedList.putLatestValue(instance.getValue());
            }

            updateAfterRemoval(instance, typedList);
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
            notifyChangeListener(instance, changeListenerWrapper, oldValue, newValue);
        }
    }

    private static class ChangeListenerWrapper<T> implements ChangeListener<T> {

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
            list.putLatestValue(null);  // clear old value cache to avoid references
        }
    }
}
