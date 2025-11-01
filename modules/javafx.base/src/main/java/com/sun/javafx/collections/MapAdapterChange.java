/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.collections;

import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableMap;

public final class MapAdapterChange<K, V> extends MapChangeListener.Change<K, V> {

    private Change<? extends K, ? extends V> change;

    public MapAdapterChange(ObservableMap<K, V> map, Change<? extends K, ? extends V> change) {
        super(map);
        this.change = change;
    }

    @Override
    public boolean wasAdded() {
        return change.wasAdded();
    }

    @Override
    public boolean wasRemoved() {
        return change.wasRemoved();
    }

    @Override
    public K getKey() {
        return change.getKey();
    }

    @Override
    public V getValueAdded() {
        return change.getValueAdded();
    }

    @Override
    public V getValueRemoved() {
        return change.getValueRemoved();
    }

    @Override
    public Change<K, V> next() {
        Change<? extends K, ? extends V> nextChange = change.next();
        if (nextChange != null) {
            change = nextChange;
            return this;
        }

        return null;
    }

    @Override
    public String toString() {
        return change.toString();
    }
}
