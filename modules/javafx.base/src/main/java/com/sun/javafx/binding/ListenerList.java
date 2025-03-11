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
 * Extension of {@link ListenerListBase}, which allows an {@link ObservableValue}
 * and its old value to notify all contained listeners with a depth first approach.
 *
 * @param <T> the type of the values the observable provides
 */
public class ListenerList<T> extends ListenerListBase {

    /**
     * Indicates a nested notification was aborted early because the new value
     * was modified during the loop to equal the old value. This means that not
     * all intended listeners (up to the maximum set by the higher level loop)
     * were called and so there may be confusion as to the current value is now
     * for some listeners.
     *
     * An abort may indicate a problem with multiple listeners changing values
     * that are not converging to a mutually agreed value.
     */
    private static final int NESTED_NOTIFICATION_ABORTED = -2;

    /**
     * Indicates a nested notification completed normally. After a nested
     * notification (aborted or not), the current value is re-read so listeners
     * not notified yet at the current level receive the newly updated current
     * value.
     */
    private static final int NESTED_NOTIFICATION_COMPLETED = -1;

    /**
     * This field is only used during notifications, and only relevant
     * when nested notifications occur. It is used for communicating
     * information between the different nesting levels. To deeper
     * nesting levels it contains the number of listeners that have
     * been notified in higher level loops, while deeper nesting levels
     * communicate to higher level loops whether a nested notification
     * actually occurred if it completed normally or was aborted early.<p>
     *
     * When its value is zero or positive, it indicates the number of
     * listeners notified in a higher level loop (minus one), while the constants
     * {@link #NESTED_NOTIFICATION_ABORTED} and {@link #NESTED_NOTIFICATION_COMPLETED}
     * indicate to a higher level loop that a nested notification occurred,
     * requiring, for example, a refresh of the current value and a new equals
     * check.
     */
    private int progress;

    /**
     * Creates a new instance with two listeners.
     *
     * @param listener1 a listener, cannot be {@code null}
     * @param listener2 a listener, cannot be {@code null}
     * @throws NullPointerException when any parameter is {@code null}
     */
    public ListenerList(Object listener1, Object listener2) {
        super(listener1, listener2);
    }

    /**
     * Notifies all listeners using the given observable value as source.
     *
     * @param observableValue an {@link ObservableValue}, cannot be {@code null}
     * @param oldValue the value held by the observable before it was changed, can be {@code null}
     * @return {@code true} if the listener list is not locked, and it was modified during
     *     notification otherwise {@code false}
     */
    public boolean notifyListeners(ObservableValue<? extends T> observableValue, T oldValue) {
        boolean wasLocked = isLocked();

        if (!wasLocked) {
            lock();
        }

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

        T newValue = null;

        progress = NESTED_NOTIFICATION_COMPLETED;  // reset progress to ensure latest value is queried at least once

        for (int i = 0; i < maxChanges; i++) {
            ChangeListener<T> listener = getChangeListener(i);

            // skip if this listener was removed during a notification:
            if (listener == null) {
                continue;
            }

            // only get the latest value if this is the first loop or a nested notification occurred
            if (progress < 0) {
                newValue = observableValue.getValue();

                valueObtained(newValue);

                if (Objects.equals(newValue, oldValue)) {
                    progress = NESTED_NOTIFICATION_ABORTED;  // Indicate an early exit before notifying all listeners intended at this level

                    return wasLocked ? false : unlock();
                }
            }

            // communicate to a lower level loop (if triggered) how many listeners were notified so far:
            progress = i + invalidationListenersSize;

            // call change listener (and perhaps a nested notification updating progress field):
            callChangeListener(observableValue, listener, oldValue, newValue);

            if (progress == NESTED_NOTIFICATION_ABORTED) {

                /*
                 * Non-convergence detected: The listener just notified above of value X
                 * triggered a change to Y. The nested notification loop informing earlier
                 * listeners of Y was aborted because another listener changed the value
                 * back to X.
                 *
                 * Since listeners are only called when the old value is the last provided new
                 * value, and not when old == new, the listener that forced Y may incorrectly
                 * assume the value is still Y, leading to potential inconsistencies.
                 * Repeated changes between X and Y would normally cause a StackOverflowError.
                 * This conflicting listener behavior will be reported to the user:
                 */

                throw new StackOverflowError("non-converging value detected in value modifying listeners on " + observableValue + "; original value was: " + oldValue);
            }
        }

        // communicate to a higher level loop that a nested notification completed (if
        // there is a higher loop):
        progress = NESTED_NOTIFICATION_COMPLETED;

        return wasLocked ? false : unlock();
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
