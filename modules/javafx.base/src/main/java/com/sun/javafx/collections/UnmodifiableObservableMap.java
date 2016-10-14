/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javafx.beans.InvalidationListener;
import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableMap;
import javafx.collections.WeakMapChangeListener;


/**
 * ObservableMap wrapper that does not allow changes to the underlying container.
 */
public class UnmodifiableObservableMap<K, V> extends AbstractMap<K, V>
        implements javafx.collections.ObservableMap<K, V>
{
    private MapListenerHelper<K, V> listenerHelper;
    private final ObservableMap<K, V> backingMap;
    private final MapChangeListener<K, V> listener;

    private Set<K> keyset;
    private Collection<V> values;
    private Set<Entry<K, V>> entryset;

    public UnmodifiableObservableMap(ObservableMap<K, V> map) {
        this.backingMap = map;
        listener = c -> {
            callObservers(new MapAdapterChange<K, V>(UnmodifiableObservableMap.this, c));
        };
        this.backingMap.addListener(new WeakMapChangeListener<K, V>(listener));
    }

    private void callObservers(Change<? extends K,? extends V> c) {
        MapListenerHelper.fireValueChangedEvent(listenerHelper, c);
    }

    @Override
    public void addListener(InvalidationListener listener) {
        listenerHelper = MapListenerHelper.addListener(listenerHelper, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        listenerHelper = MapListenerHelper.removeListener(listenerHelper, listener);
    }

    @Override
    public void addListener(MapChangeListener<? super K, ? super V> observer) {
        listenerHelper = MapListenerHelper.addListener(listenerHelper, observer);
    }

    @Override
    public void removeListener(MapChangeListener<? super K, ? super V> observer) {
        listenerHelper = MapListenerHelper.removeListener(listenerHelper, observer);
    }

    @Override
    public int size() {
        return backingMap.size();
    }

    @Override
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return backingMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return backingMap.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return backingMap.get(key);
    }

    public Set<K> keySet() {
        if (keyset == null) {
            keyset = Collections.unmodifiableSet(backingMap.keySet());
        }
        return keyset;
    }

    public Collection<V> values() {
        if (values == null) {
            values = Collections.unmodifiableCollection(backingMap.values());
        }
        return values;
    }

    public Set<Entry<K,V>> entrySet() {
        if (entryset == null) {
            entryset = Collections.unmodifiableMap(backingMap).entrySet();
        }
        return entryset;
    }
}
