/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;

import javafx.beans.Observable;

/**
 * A set that allows observers to track changes when they occur. Implementations can be created using methods in {@link FXCollections}
 * such as {@link FXCollections#observableSet(Object...) observableSet}, or with a
 * {@link javafx.beans.property.SimpleSetProperty SimpleSetProperty}.
 *
 * @see SetChangeListener
 * @see SetChangeListener.Change
 * @param <E> the set element type
 * @since JavaFX 2.1
 */
public interface ObservableSet<E> extends Set<E>, Observable {
    /**
     * Add a listener to this observable set.
     * @param listener the listener for listening to the set changes
     */
    public void addListener(SetChangeListener<? super E> listener);
    /**
     * Tries to removed a listener from this observable set. If the listener is not
     * attached to this list, nothing happens.
     * @param listener a listener to remove
     */
    public void removeListener(SetChangeListener<? super E> listener);
}
