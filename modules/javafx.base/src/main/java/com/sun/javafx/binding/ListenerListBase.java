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
import javafx.beans.WeakListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * Manages a mix of invalidation and change listeners, which can be locked in
 * place for iteration while allowing modifications to take place.<p>
 *
 * While locked, listeners that were removed are set to {@code null}, and listeners
 * that were added are tracked separately. While locked, the size methods do not reflect
 * any modifications made, and so care must be taken to skip {@code null}s when getting a
 * listener by index.<p>
 *
 * While unlocked, the size methods always accurately reflect the actual number of
 * listeners in the list, and {@code null} is never returned when getting a listener by
 * index.<p>
 *
 * This class supports {@link WeakListener}s, and removes any garbage collected
 * listeners at a time of its choosing.
 */
public abstract class ListenerListBase {

    /**
     * This a variant of {@link ArrayManager} with an implemented {@link #compact(ListenerListBase, T[])}
     * method. The compaction is only allowed while its associated {@link ListenerListBase} is unlocked.<p>
     *
     * This handles compacting elements that are {@code null}, as well as elements that implement
     * {@link WeakListener} that were garbage collected in the mean time.
     *
     * @param <T> the element type
     */
    private static abstract class CompactingArrayManager<T> extends ArrayManager<ListenerListBase, T> {

        CompactingArrayManager(Class<T> elementType) {
            super(elementType);
        }

        @Override
        protected int compact(ListenerListBase instance, T[] array) {
            if (instance.isLocked()) {
                return 0;
            }

            int shift = 0;

            for (int i = 0; i < array.length; i++) {
                T element = array[i];

                if (element == null || (element instanceof WeakListener wl && wl.wasGarbageCollected())) {
                    shift++;
                    array[i] = null;
                    continue;
                }

                if (shift > 0) {
                    array[i - shift] = element;
                    array[i] = null;
                }
            }

            return shift;
        }
    }

    private static final ArrayManager<ListenerListBase, InvalidationListener> INVALIDATION_LISTENERS = new CompactingArrayManager<>(InvalidationListener.class) {

        @Override
        protected InvalidationListener[] getArray(ListenerListBase instance) {
            return instance.invalidationListeners;
        }

        @Override
        protected void setArray(ListenerListBase instance, InvalidationListener[] array) {
            instance.invalidationListeners = array;
        }

        @Override
        protected int getOccupiedSlots(ListenerListBase instance) {
            return instance.invalidationListenersCount;
        }

        @Override
        protected void setOccupiedSlots(ListenerListBase instance, int occupiedSlots) {
            instance.invalidationListenersCount = occupiedSlots;
        }
    };

    private static final ArrayManager<ListenerListBase, Object> CHANGE_LISTENERS = new CompactingArrayManager<>(Object.class) {

        @Override
        protected Object[] getArray(ListenerListBase instance) {
            return instance.changeListeners;
        }

        @Override
        protected void setArray(ListenerListBase instance, Object[] array) {
            instance.changeListeners = array;
        }

        @Override
        protected int getOccupiedSlots(ListenerListBase instance) {
            return instance.changeListenersCount;
        }

        @Override
        protected void setOccupiedSlots(ListenerListBase instance, int occupiedSlots) {
            instance.changeListenersCount = occupiedSlots;
        }
    };

    /*
     * The following four fields are used for tracking invalidation and change listeners. When the
     * list is unlocked, these arrays hold what you'd expect (InvalidationListeners and ChangeListeners
     * respectively).
     *
     * While the list is locked, any new listeners (regardless what kind) are always added to the
     * change listener array first, in order to not disturb the indices of any listeners that existed
     * before (the indices remain stable). Any added listeners are invisible, and cannot be obtained
     * via the public methods of this class (the sizes of the lists remain the same while locked, and
     * the result of accessing an index of an added listener which would exceed the size returned is
     * undefined).
     *
     * Similarly, while the list is locked, any removed listeners do not alter the indices of this
     * list; instead removed listeners are set to null.
     *
     * Only after unlocking are any InvalidationListeners that do not belong in the change listener
     * array moved to the invalidation listener array, and nulled elements are removed.
     */

    private InvalidationListener[] invalidationListeners;
    private int invalidationListenersCount;
    private Object[] changeListeners;
    private int changeListenersCount;

    /**
     * Indicates whether the list is locked, and if so, what the total size
     * of the two lists was at the time of locking. A non-negative value
     * indicates the list is locked, while -1 indicates the list is
     * not locked.
     */
    private int lockedSize = -1;

    /**
     * Indicates whether a list currently contains any {@code null}s, and
     * how many. This is always zero when unlocked.
     */
    private int nulledListenerCount;

    /**
     * Creates a new instance with two listeners.
     *
     * @param listener1 a listener, cannot be {@code null}
     * @param listener2 a listener, cannot be {@code null}
     * @throws NullPointerException when any parameter is {@code null}
     */
    public ListenerListBase(Object listener1, Object listener2) {
        Objects.requireNonNull(listener1);
        Objects.requireNonNull(listener2);

        if (listener1 instanceof InvalidationListener il) {
            INVALIDATION_LISTENERS.add(this, il);
        }
        else {
            CHANGE_LISTENERS.add(this, listener1);
        }

        if (listener2 instanceof InvalidationListener il) {
            INVALIDATION_LISTENERS.add(this, il);
        }
        else {
            CHANGE_LISTENERS.add(this, listener2);
        }
    }

    /**
     * Returns the total number of listeners in this list. This accurately
     * reflects added and removed listeners even while the list is locked.
     *
     * @return the total number of listeners in this list, never negative
     */
    public final int totalListeners() {
        return invalidationListenersCount + changeListenersCount - nulledListenerCount;
    }

    /**
     * Returns the number of invalidation listeners. While the list is
     * locked this may include some listeners that were set to {@code null}.
     *
     * @return the number of invalidation listeners, never negative
     */
    public final int invalidationListenersSize() {
        return invalidationListenersCount;
    }

    /**
     * Returns the number of change listeners. While the list is
     * locked this may include some listeners that were set to {@code null}.
     *
     * @return the number of change listeners, never negative
     */
    public final int changeListenersSize() {
        return isLocked() ? lockedSize - invalidationListenersCount : changeListenersCount;
    }

    /**
     * Gets the {@link InvalidationListener} at the given index. This can be {@code null} if the
     * list is locked and the listener was removed in the mean time.<p>
     *
     * Note: the behavior when calling this method with an index outside the valid range is
     * <b>undefined</b>!
     *
     * @param index an index, cannot be negative and must be less than {@link #invalidationListenersSize()}
     * @return the listener at the given index, or {@code null}
     */
    public final InvalidationListener getInvalidationListener(int index) {
        assertInvalidationListenerIndex(index);

        return invalidationListeners[index];
    }

    /**
     * Gets the {@link ChangeListener} at the given index. This can be {@code null} if the
     * list is locked and the listener was removed in the mean time.<p>
     *
     * Note: the behavior when calling this method with an index outside the valid range is
     * <b>undefined</b>!
     *
     * @param <T> the change listener type
     * @param index an index, cannot be negative and must be less than {@link #changeListenersSize()}
     * @return the listener at the given index, or {@code null}
     */
    public final <T> ChangeListener<T> getChangeListener(int index) {
        assertChangeListenerIndex(index);

        @SuppressWarnings("unchecked")
        ChangeListener<T> cl = (ChangeListener<T>) changeListeners[index];

        return cl;
    }

    /**
     * Adds a listener to this list. If the list is locked, this listener won't show
     * up until unlocked, nor will the lists size change.
     *
     * @param listener a listener, cannot be {@code null}
     * @throws NullPointerException when any argument is {@code null}
     */
    public final void add(Object listener) {
        Objects.requireNonNull(listener);

        if (isLocked() || listener instanceof ChangeListener) {
            CHANGE_LISTENERS.add(this, listener);  // even invalidation listeners go into this list when locked!
        }
        else {
            INVALIDATION_LISTENERS.add(this, (InvalidationListener) listener);
        }
    }

    /**
     * Removes a listener from this list. If the list is locked, the removed
     * listener is set to {@code null}. When iterating, {@code null}s must be
     * skipped.
     *
     * @param listener a listener to remove, cannot be {@code null}
     * @throws NullPointerException when any argument is {@code null}
     */
    public final void remove(Object listener) {
        Objects.requireNonNull(listener);

        int index = listener instanceof InvalidationListener il ? INVALIDATION_LISTENERS.indexOf(this, il) : -1;

        if (index >= 0) {
            if (isLocked()) {
                INVALIDATION_LISTENERS.set(this, index, null);

                nulledListenerCount++;
            }
            else {
                INVALIDATION_LISTENERS.remove(this, index);
            }
        }
        else {
            index = CHANGE_LISTENERS.indexOf(this, listener);

            if (index >= 0) {
                if (!isLocked() || index >= lockedSize - invalidationListenersCount) {
                    CHANGE_LISTENERS.remove(this, index);  // not locked, or was added during lock, so can just remove directly
                }
                else {
                    CHANGE_LISTENERS.set(this, index, null);

                    nulledListenerCount++;
                }
            }
        }
    }

    /**
     * Unlocks this listener list, making any listeners available that were added
     * while locked, and removes any empty slots from listeners that were removed while
     * locked.
     *
     * @return {@code true} if the list was modified while it was locked, otherwise {@code false}
     */
    protected final boolean unlock() {
        assertLocked();

        boolean containsNulls = nulledListenerCount > 0;

        // if there were no nulls and no additions...
        if (!containsNulls && invalidationListenersCount + changeListenersCount <= lockedSize) {
            lockedSize = -1;

            return false;
        }

        for (int i = lockedSize - invalidationListenersCount; i < changeListenersCount; i++) {
            Object listener = CHANGE_LISTENERS.get(this, i);

            if (listener instanceof InvalidationListener il) {
                INVALIDATION_LISTENERS.add(this, il);
                CHANGE_LISTENERS.set(this, i, null);

                containsNulls = true;
            }
        }

        if (containsNulls) {

            /*
             * Note: only nulls are removed here. Expired weak listeners are not removed
             * as this would mean the listener count could change unexpectedly at the end
             * of a notification without going through an addListener/removeListener call
             * which may be overridden to track listeners. A scenario that would be troubling
             * is:
             *
             * - A notification starts
             * - An unrelated listener removes itself while list is locked (which will trigger this
             *   clean-up code later); this goes through the normal add/removeListener channel
             * - The notification ends
             * - While unlocking, a weak listener is determined to be expired
             *   - Removing this weak listener would not go through proper channels and thus
             *     code that overrides add/removeListener would be unaware of the change
             *
             * Therefore, weak listeners will only be actively removed when another listener
             * is being added or removed as the caller will then be expecting a change in the
             * listener count, albeit higher than just the one listener being added or removed.
             * Callers already should be aware that removing a listener may not change the count
             * (if the listener didn't exist), so manually checking the new count after such a
             * call must be done already.
             */

            INVALIDATION_LISTENERS.removeIf(this, Objects::isNull);
            CHANGE_LISTENERS.removeIf(this, Objects::isNull);

            nulledListenerCount = 0;
        }

        lockedSize = -1;

        return true;
    }

    /**
     * Locks this list.
     */
    protected final void lock() {
        assertNotLocked();

        this.lockedSize = invalidationListenersCount + changeListenersCount;
    }

    /**
     * Checks whether this list is locked.
     *
     * @return {@code true} if locked, otherwise {@code false}
     */
    protected final boolean isLocked() {
        return lockedSize >= 0;
    }

    /**
     * Checks whether this list has any change listeners. Note: this does not
     * update while locked.
     *
     * @return {@code true} if there were change listeners, otherwise {@code false}
     */
    public final boolean hasChangeListeners() {
        return totalSize() > invalidationListenersCount;
    }

    private int totalSize() {
        return isLocked() ? lockedSize : invalidationListenersCount + changeListenersCount;
    }

    private void assertInvalidationListenerIndex(int index) {
        assert index < invalidationListenersCount : index + " >= " + invalidationListenersCount + ", results would be undefined";
    }

    private void assertChangeListenerIndex(int index) {
        assert index < (isLocked() ? lockedSize - invalidationListenersCount : changeListenersCount)
            : index + " >= " + (isLocked() ? lockedSize - invalidationListenersCount : changeListenersCount) + ", results would be undefined";
    }

    private void assertLocked() {
        assert isLocked() : "wasn't locked";
    }

    private void assertNotLocked() {
        assert !isLocked() : "already locked";
        assert nulledListenerCount == 0 : "nulledListenerCount must be zero when not locked";
    }

    static final void callInvalidationListener(ObservableValue<?> instance, InvalidationListener listener) {
        try {
            listener.invalidated(instance);
        }
        catch (Exception e) {
            Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }
    }

    static final <T> void callChangeListener(ObservableValue<? extends T> instance, ChangeListener<T> changeListener, T oldValue, T newValue) {
        try {
            changeListener.changed(instance, oldValue, newValue);
        }
        catch (Exception e) {
            Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }
    }
}
