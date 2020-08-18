/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package javafx.collections;

import com.sun.javafx.collections.ArrayListenerHelper;
import javafx.beans.InvalidationListener;

/**
 * Abstract class that serves as a base class for {@link ObservableArray} implementations.
 * The base class provides listener handling functionality by implementing
 * {@code addListener} and {@code removeListener} methods.
 * {@link #fireChange(boolean, int, int) } method is provided
 *      for notifying the listeners.
 * @param <T> actual array instance type
 * @see ObservableArray
 * @see ArrayChangeListener
 * @since JavaFX 8.0
 */
public abstract class ObservableArrayBase<T extends ObservableArray<T>> implements ObservableArray<T> {

    private ArrayListenerHelper<T> listenerHelper;

    /**
     * Creates a default {@code ObservableArrayBase}.
     */
    public ObservableArrayBase() {
    }

    @Override public final void addListener(InvalidationListener listener) {
        listenerHelper = ArrayListenerHelper.<T>addListener(listenerHelper, (T) this, listener);
    }

    @Override public final void removeListener(InvalidationListener listener) {
        listenerHelper = ArrayListenerHelper.removeListener(listenerHelper, listener);
    }

    @Override public final void addListener(ArrayChangeListener<T> listener) {
        listenerHelper = ArrayListenerHelper.<T>addListener(listenerHelper, (T) this, listener);
    }

    @Override public final void removeListener(ArrayChangeListener<T> listener) {
        listenerHelper = ArrayListenerHelper.removeListener(listenerHelper, listener);
    }

    /**
     * Notifies all listeners of a change
     * @param sizeChanged indicates size of array changed
     * @param from A beginning (inclusive) of an interval related to the change
     * @param to An end (exclusive) of an interval related to the change.
     */
    protected final void fireChange(boolean sizeChanged, int from, int to) {
        ArrayListenerHelper.fireValueChangedEvent(listenerHelper, sizeChanged, from, to);
    }
}
