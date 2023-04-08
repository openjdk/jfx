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

import com.sun.javafx.binding.ArrayManager.Accessor;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakListener;
import javafx.beans.value.ChangeListener;

/**
 * Manages a mix of invalidation and change listeners as a single list, which
 * can be locked in place for iteration while allowing modifications to take
 * place. Modifications during iteration are visible immediately for removals,
 * and only after unlocking for additions.<p>
 *
 * This list guarantees that, during iteration, invalidation listeners are
 * returned before any change listeners. It is not possible to access newly
 * added listeners while the list is locked for iteration.<p>
 *
 * If removals are allowed during iteration, then care must be taken to skip any
 * returned {@code null} elements.<p>
 *
 * Locking the list is achieved by calling {@link #setProgress(int)}, where the
 * value provided can be any integer. This value serves no purpose within this
 * class, but can be used to record the position of iteration when a nested
 * iteration occurs.<p>
 *
 * When the top-level iteration has finished, the list should be unlocked. This
 * will integrate any additions into the list, as well as removing any empty
 * slots from removals that occurred while locked.
 */
public class ListenerList {
    private static final Accessor<ListenerList, Object> INVALIDATION_LISTENERS_ACCESSOR = new Accessor<>() {
        @Override
        public Object[] getArray(ListenerList instance) {
            return instance.managedInvalidationListeners;
        }

        @Override
        public void setArray(ListenerList instance, Object[] array) {
            instance.managedInvalidationListeners = array;
        }

        @Override
        public int getOccupiedSlots(ListenerList instance) {
            return instance.managedInvalidationListenersCount;
        }

        @Override
        public void setOccupiedSlots(ListenerList instance, int occupiedSlots) {
            instance.managedInvalidationListenersCount = occupiedSlots;
        }
    };

    private static final Accessor<ListenerList, Object> CHANGE_LISTENERS_ACCESSOR = new Accessor<>() {
        @Override
        public Object[] getArray(ListenerList instance) {
            return instance.managedChangeListeners;
        }

        @Override
        public void setArray(ListenerList instance, Object[] array) {
            instance.managedChangeListeners = array;
        }

        @Override
        public int getOccupiedSlots(ListenerList instance) {
            return instance.managedChangeListenersCount;
        }

        @Override
        public void setOccupiedSlots(ListenerList instance, int occupiedSlots) {
            instance.managedChangeListenersCount = occupiedSlots;
        }
    };

    /**
     * This a variant of {@link ArrayManager} with an implemented {@link #compact(ListenerList, Object[])}
     * method. The compaction is only allowed while its associated {@link ListenerList} is unlocked.<p>
     *
     * This handles compacting elements that are {@code null}, as well as elements that implement
     * {@link WeakListener} that were garbage collected in the mean time.
     */
    private static class CompactingArrayManager extends ArrayManager<ListenerList, Object> {

        CompactingArrayManager(Accessor<ListenerList, Object> accessor) {
            super(accessor, Object.class);
        }

        @Override
        protected int compact(ListenerList instance, Object[] array) {
            if (instance.isLocked()) {
                return 0;
            }

            int shift = 0;

            for (int i = 0; i < array.length; i++) {
                Object element = array[i];

                if (element == null || (element instanceof WeakListener wl && wl.wasGarbageCollected())) {
                    shift++;
                    array[i] = null;
                    continue;
                }

                if(shift > 0) {
                    array[i - shift] = element;
                    array[i] = null;
                }
            }

            return shift;
        }
    }

    private static final ArrayManager<ListenerList, Object> INVALIDATION_LISTENERS = new CompactingArrayManager(INVALIDATION_LISTENERS_ACCESSOR);
    private static final ArrayManager<ListenerList, Object> CHANGE_LISTENERS = new CompactingArrayManager(CHANGE_LISTENERS_ACCESSOR);

    /*
     * The following four fields are used for tracking invalidation and change listeners. When the
     * list is unlocked, these arrays hold what you'd expect (InvalidationListeners and ChangeListeners
     * respectively), with the total size returned by this class being the total number of occupied
     * slots in the two lists. Indexing into this list first returns invalidation listeners, while
     * higher indices that exceed the number of invalidation listeners index into the change listener
     * array. This allows to keep track of how many listeners have been notified as a single index
     * value, called progress.
     *
     * While the list is locked, any new listeners (regardless what kind) are always added to the
     * change listener array first, in order to not disturb the indices of any listeners that existed
     * before (the indices remain stable). Any added listeners are invisible, and cannot be obtained
     * via the public methods of this class (the size of the list remains the same while locked, and
     * attempting to access an index of an added listener which would exceed the size returned will
     * result in an IndexOutOfBoundsException).
     *
     * Similarly, while the list is locked, any removed listeners do not alter the indices of this
     * list; instead removed listeners are set to null.
     *
     * Only after unlocked are any InvalidationListeners that do not belong in the change listener
     * array moved to the invalidation listener array, and nulled elements are potentially removed.
     *
     * Note: although providing an Iterator (which automatically handles skipping null elements)
     * would seem like a nice addition, its not currently implemented as it would require an object
     * allocation. The current users of this class want to avoid allocations.
     */

    private Object[] managedInvalidationListeners;
    private int managedInvalidationListenersCount;
    private Object[] managedChangeListeners;
    private int managedChangeListenersCount;

    /**
     * This field is only used during notifications, and only relevant
     * when nested notifications occur. It is used to record how many
     * listeners have been notified already in a higher level loop, so
     * more deeply nested loops will never notify more listeners than
     * its previous nesting level.
     */
    private int progress;

    /**
     * Indicates whether the list is locked, and if so, what the size
     * of the list was at the time of locking. A non-negative value
     * indicates the list is locked, while -1 indicates the list is
     * not locked.
     */
    private int lockedOffset = -1;

    /**
     * Creates a new instance with two listeners.
     *
     * @param listener1 a listener, cannot be {@code null}
     * @param listener2 a listener, cannot be {@code null}
     * @throws NullPointerException when any parameter is {@code null}
     */
    public ListenerList(Object listener1, Object listener2) {
        Objects.requireNonNull(listener1);
        Objects.requireNonNull(listener2);

        if(listener1 instanceof ChangeListener) {
            CHANGE_LISTENERS.add(this, listener1);
        }
        else {
            INVALIDATION_LISTENERS.add(this, listener1);
        }

        if(listener2 instanceof ChangeListener) {
            CHANGE_LISTENERS.add(this, listener2);
        }
        else {
            INVALIDATION_LISTENERS.add(this, listener2);
        }
    }

    /**
     * Gets the listener at the given index. This can be {@code null} if the
     * listener was removed in the mean time.
     *
     * @param index an index, cannot be negative and must be less than {@link #size()}
     * @return the listener at the given position, or {@code null}
     * @throws IndexOutOfBoundsException when the index is out of range
     */
    public Object get(int index) {
        if (isLocked() && index >= lockedOffset) {
            throw new IndexOutOfBoundsException(index);
        }

        return index < managedInvalidationListenersCount ? INVALIDATION_LISTENERS.get(this, index) : CHANGE_LISTENERS.get(this, index - managedInvalidationListenersCount);
    }

    /**
     * Returns the size of this listener list. Note: this is not the number of
     * actual listeners in the list, some elements may be {@code null}.
     *
     * @return the size of this listener list, never negative
     */
    public int size() {
        return isLocked() ? lockedOffset : managedInvalidationListenersCount + managedChangeListenersCount;
    }

    /**
     * Adds a listener to this list. If the list is locked, this listener won't show
     * up until unlocked, nor will the lists size change.
     *
     * @param listener a listener, cannot be {@code null}
     */
    public void add(Object listener) {
        Objects.requireNonNull(listener);

        if (isLocked() || listener instanceof ChangeListener) {
            CHANGE_LISTENERS.add(this, listener);  // even invalidation listeners go into this list when locked!
        }
        else {
            INVALIDATION_LISTENERS.add(this, listener);
        }
    }

    /**
     * Removes a listener from this list. If the list is locked, the removed
     * listener is set to {@code null}. When iterating, {@code null}s must be
     * skipped.
     *
     * @param listener a listener to remove, cannot be {@code null}
     */
    public void remove(Object listener) {
        Objects.requireNonNull(listener);

        int index = INVALIDATION_LISTENERS.indexOf(this, listener);

        if (index >= 0) {
            if (isLocked()) {
                INVALIDATION_LISTENERS.set(this, index, null);
            }
            else {
                INVALIDATION_LISTENERS.remove(this, index);
            }
        }
        else {
            index = CHANGE_LISTENERS.indexOf(this, listener);

            if (index >= 0) {
                if (!isLocked() || index >= lockedOffset - managedInvalidationListenersCount) {
                    CHANGE_LISTENERS.remove(this, index);  // not locked, or was added during lock, so can just remove directly
                }
                else {
                    CHANGE_LISTENERS.set(this, index, null);
                }
            }
        }
    }

    /**
     * Unlocks this listener list, making any listeners available that were added
     * while locked, and possibly removing empty slots from listeners that were removed while
     * locked.
     */
    public void unlock() {
        if (!isLocked()) {
            throw new IllegalStateException("wasn't locked");
        }

        if (managedInvalidationListenersCount + managedChangeListenersCount > lockedOffset) {  // if there were additions...
            for (int i = lockedOffset - managedInvalidationListenersCount; i < managedChangeListenersCount; i++) {
                Object listener = CHANGE_LISTENERS.get(this, i);

                if (listener instanceof InvalidationListener) {
                    INVALIDATION_LISTENERS.add(this, listener);
                    CHANGE_LISTENERS.set(this, i, null);
                }
            }
        }

        lockedOffset = -1;
    }

    /**
     * Returns the value previously set progress value.
     *
     * @return the progress value
     */
    public int getProgress() {
        return progress;
    }

    /**
     * Sets the progress value. If the list was unlocked, also locks the list.
     *
     * @param progress a progress value
     */
    public void setProgress(int progress) {
        this.progress = progress;

        if (!isLocked()) {
            this.lockedOffset = managedInvalidationListenersCount + managedChangeListenersCount;
        }
    }

    /**
     * Checks whether this list is locked.
     *
     * @return {@code true} if locked, otherwise {@code false}
     */
    public boolean isLocked() {
        return lockedOffset >= 0;
    }

    /**
     * Checks whether this list has any change listeners. Note: this does not
     * update while locked.
     *
     * @return {@code true} if there were change listeners, otherwise {@code false}
     */
    public boolean hasChangeListeners() {
        return size() > managedInvalidationListenersCount;
    }
}
