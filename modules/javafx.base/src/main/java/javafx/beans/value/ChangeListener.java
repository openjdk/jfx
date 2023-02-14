/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @see ObservableValue
 *
 *
 * @since JavaFX 2.0
 */
@FunctionalInterface
public interface ChangeListener<T> {

    /**
     * Called when the value of an {@link ObservableValue} changes.
     * <p>
     * Changing the observed value in this method will result in all listeners
     * being notified of this latest change after the initial change
     * notification (with the original old and values) has completed.
     * The listeners that still needed to be notified may see a new value that
     * differs from a call to {@link ObservableValue#getValue}. All listeners are
     * then notified again with an old value equal to the initial new value,
     * and a new value with the latest value.
     *
     * @param observable
     *            The {@code ObservableValue} which value changed
     * @param oldValue
     *            The old value
     * @param newValue
     *            The new value
     */
    void changed(ObservableValue<? extends T> observable, T oldValue, T newValue);
}
