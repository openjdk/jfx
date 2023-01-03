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

import java.util.Map;

import javafx.beans.Observable;

/**
 * A map that allows observers to track changes when they occur. Implementations can be created using methods in {@link FXCollections}
 * such as {@link FXCollections#observableHashMap() observableHashMap}, or with a
 * {@link javafx.beans.property.SimpleMapProperty SimpleMapProperty}.
 *
 * @see MapChangeListener
 * @see MapChangeListener.Change
 * @param <K> the map key element type
 * @param <V> the map value element type
 * @since JavaFX 2.0
 */
public interface ObservableMap<K, V> extends Map<K, V>, Observable {
    /**
     * Add a listener to this observable map.
     * @param listener the listener for listening to the list changes
     */
    public void addListener(MapChangeListener<? super K, ? super V> listener);
    /**
     * Tries to removed a listener from this observable map. If the listener is not
     * attached to this map, nothing happens.
     * @param listener a listener to remove
     */
    public void removeListener(MapChangeListener<? super K, ? super V> listener);
}
