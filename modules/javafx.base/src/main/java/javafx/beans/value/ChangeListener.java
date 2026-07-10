/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.value;

/**
 * A {@code ChangeListener} is notified whenever the value of an
 * {@link ObservableValue} changes. It can be registered and unregistered with
 * {@link ObservableValue#addListener(ChangeListener)} respectively
 * {@link ObservableValue#removeListener(ChangeListener)}
 * <p>
 * For an in-depth explanation of change events and how they differ from
 * invalidation events, see the documentation of {@code ObservableValue}.
 * <p>
 * The same instance of {@code ChangeListener} can be registered to listen to
 * multiple {@code ObservableValues}.
 *
 * @param <T> the observable value type
 * @see ObservableValue
 * @since JavaFX 2.0
 */
@FunctionalInterface
public interface ChangeListener<T> {

    /**
     * Called when the value of an {@link ObservableValue} changes.
     * <p>
     * When this method is invoked, {@code newValue} represents the current value of the {@code observable};
     * that is, it is equal to {@code observable.getValue()} at the time of invocation. The {@code oldValue}
     * is the value that was reported as {@code newValue} in the previous notification delivered to the same
     * listener.
     * <p>
     * If a change listener modifies the observable value in its callback, other registered change listeners
     * that have not been notified at that point will receive the newly-modified value. This ensures that a
     * change listener will always observe the effective change from its last-observed {@code oldValue} to
     * the current value of the {@code observable} at the time the listener is invoked.
     * <p>
     * However, it is usually considered bad practice to modify the observable value from a listener callback
     * because the order of listener registrations is often not strictly enforceable. This gives earlier
     * listeners an order-dependent veto over the values that later listeners will observe.
     *
     * @param observable the changed {@code ObservableValue}
     * @param oldValue the last value that was observed by this listener
     * @param newValue the current value of the {@code observable}
     */
    void changed(ObservableValue<? extends T> observable, T oldValue, T newValue);
}
