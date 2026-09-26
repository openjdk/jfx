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
 * only a single invalidation listener or change listener, the field will contain
 * only that listener. When there is more than one listener, the field will hold
 * a {@link ListenerList}. It is recommended to never inspect this field directly
 * but always use this manager to interact with it.
 *
 * @param <T> the type of the values
 * @param <I> the type of the instance providing listener data
 */
public non-sealed abstract class ListenerManager<T, I extends ObservableValue<? extends T>> extends ListenerManagerBase<T, I> {

    @Override
    protected final void addInvalidationListener(I instance, InvalidationListener listener) {
        addAnyListener(instance, listener);
    }

    @Override
    protected final void addChangeListener(I instance, ChangeListener<? super T> listener) {
        addAnyListener(instance, listener);
    }

    @Override
    protected final boolean removeInvalidationListener(I instance, InvalidationListener listener) {
        return removeAnyListener(instance, listener);
    }

    @Override
    protected final boolean removeChangeListener(I instance, ChangeListener<? super T> listener) {
        return removeAnyListener(instance, listener);
    }

    private void addAnyListener(I instance, Object listener) {
        Objects.requireNonNull(listener);

        /*
         * Only trigger validation when adding a listener if no notification is in
         * progress. If validation is deferred, it will be performed when the notification
         * concludes. This ensures that a value that may still be changing (due to nested
         * changes) is only made valid when it is no longer in flux.
         */

        if (!isNotifying(instance)) {
            instance.getValue();  // makes the observable valid
        }

        switch (getData(instance)) {
            case null -> setData(instance, isNotifying(instance) ? createLockedListenerList(listener) : listener);
            case ListenerList<?> list -> list.add(listener);
            case Object data -> setData(instance, createListenerList(instance, data, listener));
        }
    }

    private boolean removeAnyListener(I instance, Object listener) {
        Objects.requireNonNull(listener);

        Object data = getData(instance);

        if (data == null || data.equals(listener)) {
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

    /*
     * Creates a new listener list. If a new listener was added while a notification on this instance
     * is in progress, then this is detected and the new listener list is created locked. This ensures that
     * if the listener that was invoked already triggers a nested change, that we correctly first notify
     * this listener again and possibly never invoke the newly added listener (if the first listener
     * changed the value back to the original value).
     */
    private ListenerList<?> createListenerList(I instance, Object existingListener, Object newListener) {
        ListenerList<?> list = new ListenerList<>();

        list.add(existingListener);

        if (isNotifying(instance)) {
            list.lock();  // will be detected in fireValueChanged and unlocked there
        }

        list.add(newListener);

        return list;
    }

    /*
     * Creates a locked listener list for just a single listener; this is edge case occurs when
     * there is only a single listener being notified, and that listener removes itself and adds
     * itself or another listener; to prevent that new listener from being notified as part of
     * the nested notification a lock must be present which is tracked as a locked listener list with
     * a single entry.
     */
    private static ListenerList<?> createLockedListenerList(Object listener) {
        ListenerList<?> list = new ListenerList<>();

        list.lock();
        list.add(listener);

        return list;
    }

    /**
     * Notifies the listeners managed in the given instance.
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param oldValue the previous value before this change occurred, can be {@code null}
     * @param listenerData the listener data associated with the instance,
     *   can be {@code null} which means there are no listeners to notify
     * @throws NullPointerException when {@code instance} is {@code null}
     */
    public final void fireValueChanged(I instance, T oldValue, Object listenerData) {
        if (listenerData instanceof ListenerList) {
            @SuppressWarnings("unchecked")
            ListenerList<T> list = (ListenerList<T>)listenerData;

            callMultipleListeners(instance, list, oldValue);
        }
        else if (listenerData instanceof InvalidationListener il) {
            notifyInvalidationListener(instance, il);
            unlockIfDataStorageTypeBecameList(instance);
        }
        else if (listenerData instanceof ChangeListener) {
            @SuppressWarnings("unchecked")
            ChangeListener<T> cl = (ChangeListener<T>) listenerData;
            T newValue = instance.getValue();  // Required as an earlier listener may have changed the value, and current value is always needed

            if (!Objects.equals(newValue, oldValue)) {
                notifyChangeListener(instance, cl, oldValue, newValue);
                unlockIfDataStorageTypeBecameList(instance);
            }
        }
    }

    private void unlockIfDataStorageTypeBecameList(I instance) {
        if (isNotifying(instance)) {
            return;  // not top level, so leave list locked
        }

        /*
         * If during notification, the managed data field changed from a single listener to a list, then this
         * list was locked upon creation, and then must be unlocked here; if this was the top level unlock, then
         * we clean up any stale data that must occur after unlock as usual:
         */

        if (getData(instance) instanceof ListenerList<?> list && list.unlock()) {
            if (list.takeListenerAddedWhileLocked()) {
                instance.getValue();  // performs the deferred validation (see add listener)
            }

            updateAfterRemoval(instance, list);
        }
    }

    private void callMultipleListeners(I instance, ListenerList<T> list, T oldValue) {
        boolean modifiedAndUnlocked = list.notifyListeners(instance, oldValue);

        if (modifiedAndUnlocked) {  // if modified, compact the data field if possible
            if (list.takeListenerAddedWhileLocked()) {
                instance.getValue();  // see unlockIfDataStorageTypeBecameList
            }

            updateAfterRemoval(instance, list);
        }
    }

    private void updateAfterRemoval(I instance, ListenerList<?> list) {
        if (list.isLocked()) {
            return;  // while locked, sizes reflect the locked-time shape; defer consolidation until unlock
        }

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
