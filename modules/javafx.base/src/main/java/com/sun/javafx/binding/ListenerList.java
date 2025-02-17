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
 * Extension of {@link ListenerListBase} which given an {@link ObservableValue}
 * and its old value provides the means to notify all contained listeners
 * with a depth first approach.
 *
 * @param <T> the type of the values the observable provides
 */
public class ListenerList<T> extends ListenerListBase {

    /**
     * This field is only used during notifications, and only relevant
     * when nested notifications occur. It is used for communicating
     * information between the different nesting levels. To deeper
     * nesting levels it contains the number of listeners that have
     * been notified in higher level loops, while deeper nesting levels
     * communicate to higher level loops whether a nested notification
     * actually occurred.<p>
     *
     * When its value is zero or positive, it indicates the number of
     * listeners notified in a higher level loop (minus one), while
     * when its negative (-1) it indicates to a higher level loop that
     * a nested notification occurred, requiring, for example, a refresh
     * of the current value and a new equals check.
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
        int maxInvalidations = wasLocked ? Math.min(initialProgress + 1, invalidationListenersSize) : invalidationListenersSize;

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
        int maxChanges = wasLocked ? Math.min(initialProgress + 1 - invalidationListenersSize, changeListenersSize) : changeListenersSize;

        T newValue = null;

        for (int i = 0; i < maxChanges; i++) {
            // only get the latest value if this is the first loop or a nested notification occurred;
            // do this before skipping listeners as this will fail if the first listener was skipped otherwise
            if (progress < 0 || i == 0) {
                newValue = observableValue.getValue();

                valueObtained(newValue);

                if (Objects.equals(newValue, oldValue)) {
                    break;
                }
            }

            ChangeListener<T> listener = getChangeListener(i);

            // skip if this listener was removed during a notification:
            if (listener == null) {
                continue;
            }

            // communicate to a lower level loop (if triggered) how many listeners were notified so far:
            progress = i + invalidationListenersSize;

            // call change listener (and perhaps a nested notification):
            callChangeListener(observableValue, listener, oldValue, newValue);
        }

        // communicate to a higher level loop that a nested notification completed (if
        // there is a higher loop):
        progress = -1;

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
