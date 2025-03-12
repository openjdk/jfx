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

import javafx.beans.value.ObservableValue;

/**
 * Extension of {@link ListenerList} which allows an {@link ObservableValue}
 * to notify all contained listeners with a depth first approach.<p>
 *
 * Note: this listener list stores the latest value for use as the old value when
 * notifying change listeners. It is recommended to use {@link ListenerList} if
 * the old value can be provided in some other way as it is more efficient.
 *
 * @param <T> the type of the values the observable provides
 */
public class OldValueCachingListenerList<T> extends ListenerList<T> {

    private T latestValue;

    /**
     * Creates a new instance with two listeners.
     *
     * @param listener1 a listener, cannot be {@code null}
     * @param listener2 a listener, cannot be {@code null}
     * @throws NullPointerException when any parameter is {@code null}
     */
    public OldValueCachingListenerList(Object listener1, Object listener2) {
        super(listener1, listener2);
    }

    /**
     * Returns the latest value stored.
     *
     * @return the latest value stored, can be {@code null}
     */
    public final T getLatestValue() {
        return latestValue;
    }

    /**
     * Stores the latest value.
     *
     * @param value a value to store, can be {@code null}
     */
    public final void putLatestValue(T value) {
        this.latestValue = value;
    }

    /**
     * Notifies all listeners using the given observable value as source.
     *
     * @param observableValue an {@link ObservableValue}, cannot be {@code null}
     * @return {@code true} if the listener list is not locked, and it was modified during
     *     notification otherwise {@code false}
     */
    public boolean notifyListeners(ObservableValue<? extends T> observableValue) {
        return notifyListeners(observableValue, getLatestValue());
    }

    @Override
    protected final void valueObtained(T value) {
        putLatestValue(value);
    }
}
