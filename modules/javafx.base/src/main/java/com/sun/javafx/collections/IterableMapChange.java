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

package com.sun.javafx.collections;

import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for map changes that support bulk change iteration.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public sealed abstract class IterableMapChange<K, V> extends MapChangeListener.Change<K, V> {

    private IterableMapChange(ObservableMap<K, V> map) {
        super(map);
    }

    /**
     * Returns {@code this} object instance if there is another change to report, or {@code null} if there
     * are no more changes. If this method returns another change, the implementation must configure this
     * object instance to represent the next change.
     * <p>
     * Note that this narrows down the {@link MapChangeListener.Change#next()} specification, which does
     * not mandate that the same object instance is returned on each call.
     *
     * @return this instance, representing the next change, or {@code null} if there are no more changes
     */
    @Override
    public abstract MapChangeListener.Change<K, V> next();

    /**
     * Resets this {@code IterableMapChange} instance to the first change.
     */
    public abstract void reset();

    @Override
    public final String toString() {
        StringBuilder builder = new StringBuilder();

        if (wasAdded()) {
            if (wasRemoved()) {
                builder.append(getValueRemoved()).append(" replaced by ").append(getValueAdded());
            } else {
                builder.append(getValueAdded()).append(" added");
            }
        } else {
            builder.append(getValueRemoved()).append(" removed");
        }

        return builder.append(" at key ").append(getKey()).toString();
    }

    public final static class Remove<K, V> extends IterableMapChange<K, V> {

        private record Entry<K, V>(K key, V value) {}

        private final List<Entry<K, V>> changes;
        private int index;

        public Remove(ObservableMap<K, V> map) {
            super(map);
            changes = new ArrayList<>();
        }

        public Remove(ObservableMap<K, V> map, int initialCapacity) {
            super(map);
            changes = new ArrayList<>(initialCapacity);
        }

        @Override
        public MapChangeListener.Change<K, V> next() {
            if (index < changes.size() - 1) {
                ++index;
                return this;
            }

            return null;
        }

        @Override
        public void reset() {
            index = 0;
        }

        @Override
        public boolean wasAdded() {
            return false;
        }

        @Override
        public boolean wasRemoved() {
            return true;
        }

        @Override
        public K getKey() {
            return changes.get(index).key;
        }

        @Override
        public V getValueAdded() {
            return null;
        }

        @Override
        public V getValueRemoved() {
            return changes.get(index).value;
        }

        public void nextRemoved(K key, V value) {
            changes.add(new Entry<>(key, value));
        }
    }

    public final static class Generic<K, V> extends IterableMapChange<K, V> {

        private static final Object NO_VALUE = new Object();

        private record Entry<K, V>(K key, V newValue, V oldValue) {
            boolean wasAdded() {
                return newValue != NO_VALUE;
            }

            boolean wasRemoved() {
                return oldValue != NO_VALUE;
            }
        }

        private final List<Entry<K, V>> changes;
        private int index;

        public Generic(ObservableMap<K, V> map) {
            super(map);
            changes = new ArrayList<>();
        }

        @Override
        public MapChangeListener.Change<K, V> next() {
            if (index < changes.size() - 1) {
                ++index;
                return this;
            }

            return null;
        }

        @Override
        public void reset() {
            index = 0;
        }

        @Override
        public boolean wasAdded() {
            return changes.get(index).wasAdded();
        }

        @Override
        public boolean wasRemoved() {
            return changes.get(index).wasRemoved();
        }

        @Override
        public K getKey() {
            return changes.get(index).key;
        }

        @Override
        public V getValueAdded() {
            var change = changes.get(index);
            return change.wasAdded() ? change.newValue : null;
        }

        @Override
        public V getValueRemoved() {
            var change = changes.get(index);
            return change.wasRemoved() ? change.oldValue : null;
        }

        public void nextAdded(K key, V value) {
            @SuppressWarnings("unchecked")
            var entry = new Entry<>(key, value, (V)NO_VALUE);
            changes.add(entry);
        }

        public void nextRemoved(K key, V value) {
            @SuppressWarnings("unchecked")
            var entry = new Entry<>(key, (V)NO_VALUE, value);
            changes.add(entry);
        }

        public void nextReplaced(K key, V oldValue, V newValue) {
            changes.add(new Entry<>(key, newValue, oldValue));
        }
    }
}
