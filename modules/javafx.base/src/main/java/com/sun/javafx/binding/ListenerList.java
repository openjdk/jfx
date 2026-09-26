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
 * Extension of {@link ListenerListBase}, which allows an {@link ObservableValue}
 * and its old value to notify all contained listeners with a depth first approach.
 *
 * @param <T> the type of the values the observable provides
 */
public class ListenerList<T> extends ListenerListBase {

    /**
     * This field is only used during notifications, and only relevant
     * when nested notifications occur. It is used to communicate to
     * a deeper nesting level the index of the listener that is currently
     * being notified in higher level loops.
     */
    private int progress;

    /**
     * Notifies all listeners using the given observable value as source.
     *
     * @param observableValue an {@link ObservableValue}, cannot be {@code null}
     * @param oldValue the value held by the observable before it was changed, can be {@code null}
     * @return {@code true} if the listener list is not locked, and it was modified during
     *     notification otherwise {@code false}
     */
    public final boolean notifyListeners(ObservableValue<? extends T> observableValue, T oldValue) {
        boolean wasLocked = isLocked();
        boolean hadNoChangeListeners = false;

        if (!wasLocked) {
            hadNoChangeListeners = !hasChangeListeners();

            lock();
        }

        boolean modifiedWhileLocked = false;

        try {

            /*
             * Note: even though this block is guarded by try/finally, this call
             * is not allowed nor expected to throw any (non-error) exceptions under any
             * circumstances.
             */

            notifyWhileLocked(observableValue, oldValue, wasLocked);
        }
        finally {
            if (!wasLocked) {
                modifiedWhileLocked = unlock();

                /*
                 * If this notification added the first change listener(s) where none
                 * existed before, then the cached latest value was captured during
                 * notification when the first change listener was added. This cached value
                 * however could have changed if an invalidation listener modified it
                 * and so we must capture it once more here:
                 */

                if (hadNoChangeListeners && hasChangeListeners()) {
                    valueObtained(observableValue.getValue());
                }
            }
        }

        return modifiedWhileLocked;
    }

    private void notifyWhileLocked(ObservableValue<? extends T> observableValue, T oldValue, boolean wasLocked) {
        int initialProgress = progress;  // save as it will be modified soon
        int invalidationListenersSize = invalidationListenersSize();
        int maxInvalidations = wasLocked
            ? Math.min(initialProgress + 1, invalidationListenersSize)
            : invalidationListenersSize;

        for (int i = 0; i < maxInvalidations; i++) {
            InvalidationListener listener = getInvalidationListener(i);

            // skip if this listener was removed during a notification:
            if (listener == null) {
                continue;
            }

            // communicate to a lower level loop (if triggered) how many listeners were notified so far:
            progress = i;

            // call invalidation listener (and perhaps a nested notification):
            callInvalidationListener(observableValue, listener);
        }

        int changeListenersSize = changeListenersSize();
        int maxChanges = wasLocked
            ? Math.min(initialProgress + 1 - invalidationListenersSize, changeListenersSize)
            : changeListenersSize;

        /*
         * The maxChanges local represents the number of change listeners that existed at the
         * start of the notification. A property that had change listeners when invalidated must
         * become valid during notification (by reading its value). The loop below will ensure this,
         * even if all change listeners were removed by an invalidation listener.
         */

        T newValue = null;
        T triggeringValue = null;

        for (int i = 0; i < maxChanges; i++) {
            ChangeListener<T> listener = getChangeListener(i);

            newValue = observableValue.getValue();

            valueObtained(newValue);

            if (Objects.equals(newValue, oldValue)) {
                if (wasLocked) {  // veto-ing at top level is not non-convergence, so only log when nested
                    logNonConvergence(observableValue, initialProgress, invalidationListenersSize, oldValue, triggeringValue);
                }

                return;
            }

            triggeringValue = newValue;

            /*
             * Skip if this listener was removed during a notification; to ensure the property always becomes valid this
             * must be after we called ObservableValue#getValue, regardless of whether any change listeners are actually notified.
             */

            if (listener == null) {
                continue;
            }

            // communicate to a lower level loop (if triggered) how many listeners were notified so far:
            progress = i + invalidationListenersSize;

            // call change listener:
            callChangeListener(observableValue, listener, oldValue, newValue);
        }
    }

    private void logNonConvergence(ObservableValue<? extends T> observableValue, int listenerIndex, int invalidationListenersSize, T oldValue, T newValue) {
        Object listener = listenerIndex < invalidationListenersSize
            ? getInvalidationListener(listenerIndex)
            : getChangeListener(listenerIndex - invalidationListenersSize);

        Logging.getLogger().warning(
            """
            %s was modified during the invocation of multiple listeners, and the values set do not seem to be converging; \
            the listener %s changed the value from %s to %s, which was then reset to %s by another listener; \
            to avoid this warning ensure listeners are not making conflicting updates, or avoid changing the value in listeners
            """.formatted(observableValue, listener, oldValue, newValue, oldValue)
        );
    }

    /**
     * Called during notifications when a new value was obtained from the
     * involved {@link ObservableValue}.<p>
     *
     * This is useful when this value needs to be kept track of.
     *
     * @param value the value that was obtained, can be {@code null}
     */
    protected void valueObtained(T value) {
    }
}
