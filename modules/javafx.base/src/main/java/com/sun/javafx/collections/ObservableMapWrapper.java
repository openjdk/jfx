/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.InvalidationListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * A Map wrapper class that implements observability.
 *
 */
public class ObservableMapWrapper<K, V> implements ObservableMap<K, V>{
    private ObservableEntrySet entrySet;
    private ObservableKeySet keySet;
    private ObservableValues values;

    private MapListenerHelper<K, V> listenerHelper;
    private final Map<K, V> backingMap;

    public ObservableMapWrapper(Map<K, V> map) {
        this.backingMap = map;
    }

    protected void callObservers(MapChangeListener.Change<K,V> change) {
        MapListenerHelper.fireValueChangedEvent(listenerHelper, change);
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

    @Override
    public V put(K key, V value) {
        V ret;
        if (backingMap.containsKey(key)) {
            ret = backingMap.put(key, value);
            if (ret == null && value != null || ret != null && !ret.equals(value)) {
                callObservers(new SimpleChange(key, ret, value, true, true));
            }
        } else {
            ret = backingMap.put(key, value);
            callObservers(new SimpleChange(key, ret, value, true, false));
        }
        return ret;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        if (!backingMap.containsKey(key)) {
            return null;
        }
        V ret = backingMap.remove(key);
        callObservers(new SimpleChange((K)key, ret, null, false, true));
        return ret;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        int size = m.size();

        if (size == 1) {
            var entry = m.entrySet().iterator().next();
            put(entry.getKey(), entry.getValue());
        } else if (size > 1) {
            var change = new IterableMapChange.Generic<>(this);

            for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
                K key = e.getKey();
                V newValue = e.getValue();

                if (!backingMap.containsKey(key)) {
                    change.nextAdded(key, newValue);
                } else {
                    V oldValue = backingMap.get(key);

                    if (!Objects.equals(oldValue, newValue)) {
                        change.nextReplaced(key, oldValue, newValue);
                    }
                }
            }

            backingMap.putAll(m);
            callObservers(change);
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        MapChangeListener.Change<K, V> change = null;
        int i = 0;

        for (Map.Entry<K, V> entry : backingMap.entrySet()) {
            K key;
            V oldValue;

            try {
                key = entry.getKey();
                oldValue = entry.getValue();
            } catch (IllegalStateException ex) {
                // This usually means the entry is no longer in the map.
                throw new ConcurrentModificationException(ex);
            }

            // IllegalStateException thrown from function is not a ConcurrentModificationException.
            V newValue = function.apply(key, oldValue);

            try {
                if (!Objects.equals(oldValue, newValue)) {
                    entry.setValue(newValue);

                    if (change instanceof SimpleChange) {
                        var bulkChange = new IterableMapChange.Generic<>(ObservableMapWrapper.this);
                        bulkChange.nextReplaced(change.getKey(), change.getValueRemoved(), change.getValueAdded());
                        bulkChange.nextReplaced(key, oldValue, newValue);
                        change = bulkChange;
                    } else if (change instanceof IterableMapChange.Generic<K, V> bulkChange) {
                        bulkChange.nextReplaced(key, oldValue, newValue);
                    } else {
                        change = new SimpleChange(key, oldValue, newValue, true, true);
                    }
                }
            } catch (IllegalStateException ex) {
                // This usually means the entry is no longer in the map.
                throw new ConcurrentModificationException(ex);
            }

            ++i;
        }

        if (change == null) {
            return;
        }

        callObservers(change);
    }

    @Override
    public void clear() {
        int size = backingMap.size();

        if (size == 1) {
            Iterator<Entry<K, V>> it = backingMap.entrySet().iterator();
            Entry<K, V> entry = it.next();
            K key = entry.getKey();
            V val = entry.getValue();
            it.remove();
            callObservers(new SimpleChange(key, val, null, false, true));
        } else if (size > 1) {
            var change = new IterableMapChange.Remove<>(this, size);

            for (Map.Entry<? extends K, ? extends V> e : backingMap.entrySet()) {
                change.nextRemoved(e.getKey(), e.getValue());
            }

            backingMap.clear();
            callObservers(change);
        }
    }

    private boolean removeRetain(Collection<?> c, ContainsPredicate<K, V> p, boolean remove) {
        MapChangeListener.Change<K, V> change = null;

        for (Iterator<Entry<K, V>> it = backingMap.entrySet().iterator(); it.hasNext();) {
            Entry<K, V> e = it.next();

            if (remove == p.contains(c, e)) {
                K key = e.getKey();
                V value = e.getValue();

                if (change instanceof SimpleChange) {
                    var bulkChange = new IterableMapChange.Remove<>(ObservableMapWrapper.this);
                    bulkChange.nextRemoved(change.getKey(), change.getValueRemoved());
                    bulkChange.nextRemoved(key, value);
                    change = bulkChange;
                } else if (change instanceof IterableMapChange.Remove<K, V> bulkChange) {
                    bulkChange.nextRemoved(key, value);
                } else {
                    change = new SimpleChange(key, value, null, false, true);
                }

                it.remove();
            }
        }

        if (change == null) {
            return false;
        }

        callObservers(change);
        return true;
    }

    @Override
    public Set<K> keySet() {
        if (keySet == null) {
            keySet = new ObservableKeySet();
        }
        return keySet;
    }

    @Override
    public Collection<V> values() {
        if (values == null) {
            values = new ObservableValues();
        }
        return values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        if (entrySet == null) {
            entrySet = new ObservableEntrySet();
        }
        return entrySet;
    }

    @Override
    public String toString() {
        return backingMap.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return backingMap.equals(obj);
    }

    @Override
    public int hashCode() {
        return backingMap.hashCode();
    }

    private class ObservableKeySet implements Set<K>, ContainsPredicate<K, V> {

        @Override
        public int size() {
            return backingMap.size();
        }

        @Override
        public boolean isEmpty() {
            return backingMap.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return backingMap.keySet().contains(o);
        }

        @Override
        public Iterator<K> iterator() {
            return new Iterator<>() {

                private Iterator<Entry<K, V>> entryIt = backingMap.entrySet().iterator();
                private K lastKey;
                private V lastValue;
                @Override
                public boolean hasNext() {
                    return entryIt.hasNext();
                }

                @Override
                public K next() {
                    Entry<K,V> last = entryIt.next();
                    lastKey = last.getKey();
                    lastValue = last.getValue();
                    return last.getKey();
                }

                @Override
                public void remove() {
                    entryIt.remove();
                    callObservers(new SimpleChange(lastKey, lastValue, null, false, true));
                }

            };
        }

        @Override
        public Object[] toArray() {
            return backingMap.keySet().toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return backingMap.keySet().toArray(a);
        }

        @Override
        public boolean add(K e) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public boolean remove(Object o) {
            return ObservableMapWrapper.this.remove(o) != null;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return backingMap.keySet().containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends K> c) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            // implicit check to ensure c != null
            if (c.isEmpty() && !backingMap.isEmpty()) {
                clear();
                return true;
            }

            if (backingMap.isEmpty()) {
                return false;
            }

            return removeRetain(c, this, false);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            // implicit check to ensure c != null
            if (c.isEmpty() || backingMap.isEmpty()) {
                return false;
            }

            return removeRetain(c, this, true);
        }

        @Override
        public void clear() {
            ObservableMapWrapper.this.clear();
        }

        @Override
        public String toString() {
            return backingMap.keySet().toString();
        }

        @Override
        public boolean equals(Object obj) {
            return backingMap.keySet().equals(obj);
        }

        @Override
        public int hashCode() {
            return backingMap.keySet().hashCode();
        }

        @Override
        public boolean contains(Collection<?> c, Entry<K, V> e) {
            return c.contains(e.getKey());
        }
    }

    private class ObservableValues implements Collection<V>, ContainsPredicate<K, V> {

        @Override
        public int size() {
            return backingMap.size();
        }

        @Override
        public boolean isEmpty() {
            return backingMap.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return backingMap.values().contains(o);
        }

        @Override
        public Iterator<V> iterator() {
            return new Iterator<>() {

                private Iterator<Entry<K, V>> entryIt = backingMap.entrySet().iterator();
                private K lastKey;
                private V lastValue;
                @Override
                public boolean hasNext() {
                    return entryIt.hasNext();
                }

                @Override
                public V next() {
                    Entry<K, V> last = entryIt.next();
                    lastKey = last.getKey();
                    lastValue = last.getValue();
                    return lastValue;
                }

                @Override
                public void remove() {
                    entryIt.remove();
                    callObservers(new SimpleChange(lastKey, lastValue, null, false, true));
                }

            };
        }

        @Override
        public Object[] toArray() {
            return backingMap.values().toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return backingMap.values().toArray(a);
        }

        @Override
        public boolean add(V e) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public boolean remove(Object o) {
            for(Iterator<V> i = iterator(); i.hasNext();) {
                if (i.next().equals(o)) {
                    i.remove();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return backingMap.values().containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends V> c) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            // implicit check to ensure c != null
            if (c.isEmpty() || backingMap.isEmpty()) {
                return false;
            }

            return removeRetain(c, this, true);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            // implicit check to ensure c != null
            if (c.isEmpty() && !backingMap.isEmpty()) {
                clear();
                return true;
            }

            if (backingMap.isEmpty()) {
                return false;
            }

            return removeRetain(c, this, false);
        }

        @Override
        public void clear() {
            ObservableMapWrapper.this.clear();
        }

        @Override
        public String toString() {
            return backingMap.values().toString();
        }

        @Override
        public boolean equals(Object obj) {
            return backingMap.values().equals(obj);
        }

        @Override
        public int hashCode() {
            return backingMap.values().hashCode();
        }

        @Override
        public boolean contains(Collection<?> c, Entry<K, V> e) {
            return c.contains(e.getValue());
        }
    }

    private class ObservableEntry implements Entry<K,V> {

        private final Entry<K, V> backingEntry;

        public ObservableEntry(Entry<K, V> backingEntry) {
            this.backingEntry = backingEntry;
        }

        @Override
        public K getKey() {
            return backingEntry.getKey();
        }

        @Override
        public V getValue() {
            return backingEntry.getValue();
        }

        @Override
        public V setValue(V value) {
            V oldValue = backingEntry.setValue(value);

            if (!Objects.equals(oldValue, value)) {
                callObservers(new SimpleChange(getKey(), oldValue, value, true, true));
            }

            return oldValue;
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) {
                return false;
            }
            Object k1 = getKey();
            Object k2 = e.getKey();
            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                Object v1 = getValue();
                Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return (getKey() == null ? 0 : getKey().hashCode())
                    ^ (getValue() == null ? 0 : getValue().hashCode());
        }

        @Override
        public final String toString() {
            return getKey() + "=" + getValue();
        }

    }

    private class ObservableEntrySet implements Set<Entry<K,V>>{

        @Override
        public int size() {
            return backingMap.size();
        }

        @Override
        public boolean isEmpty() {
            return backingMap.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return backingMap.entrySet().contains(o);
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new Iterator<>() {

                private Iterator<Entry<K,V>> backingIt = backingMap.entrySet().iterator();
                private K lastKey;
                private V lastValue;
                @Override
                public boolean hasNext() {
                    return backingIt.hasNext();
                }

                @Override
                public Entry<K, V> next() {
                    Entry<K, V> last = backingIt.next();
                    lastKey = last.getKey();
                    lastValue = last.getValue();
                    return new ObservableEntry(last);
                }

                @Override
                public void remove() {
                    backingIt.remove();
                    callObservers(new SimpleChange(lastKey, lastValue, null, false, true));
                }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object[] toArray() {
            Object[] array = backingMap.entrySet().toArray();
            for (int i = 0; i < array.length; ++i) {
                array[i] = new ObservableEntry((Entry<K, V>)array[i]);
            }
            return array;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            T[] array = backingMap.entrySet().toArray(a);
            for (int i = 0; i < array.length; ++i) {
                array[i] = (T) new ObservableEntry((Entry<K, V>)array[i]);
            }
            return array;
        }

        @Override
        public boolean add(Entry<K, V> e) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean remove(Object o) {
            boolean ret = backingMap.entrySet().remove(o);
            if (ret) {
                Entry<K,V> entry = (Entry<K, V>) o;
                callObservers(new SimpleChange(entry.getKey(), entry.getValue(), null, false, true));
            }
            return ret;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return backingMap.entrySet().containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends Entry<K, V>> c) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            // implicit check to ensure c != null
            if (c.isEmpty() && !backingMap.isEmpty()) {
                clear();
                return true;
            }

            if (backingMap.isEmpty()) {
                return false;
            }

            return removeRetain(c, Collection::contains, false);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            // implicit check to ensure c != null
            if (c.isEmpty() || backingMap.isEmpty()) {
                return false;
            }

            return removeRetain(c, Collection::contains, true);
        }

        @Override
        public void clear() {
            ObservableMapWrapper.this.clear();
        }

        @Override
        public String toString() {
            return backingMap.entrySet().toString();
        }

        @Override
        public boolean equals(Object obj) {
            return backingMap.entrySet().equals(obj);
        }

        @Override
        public int hashCode() {
            return backingMap.entrySet().hashCode();
        }

    }

    private static String changeToString(MapChangeListener.Change<?, ?> change) {
        StringBuilder builder = new StringBuilder();

        if (change.wasAdded()) {
            if (change.wasRemoved()) {
                builder.append(change.getValueRemoved()).append(" replaced by ").append(change.getValueAdded());
            } else {
                builder.append(change.getValueAdded()).append(" added");
            }
        } else {
            builder.append(change.getValueRemoved()).append(" removed");
        }

        return builder.append(" at key ").append(change.getKey()).toString();
    }

    private class SimpleChange extends MapChangeListener.Change<K,V> {

        private final K key;
        private final V old;
        private final V added;
        private final boolean wasAdded;
        private final boolean wasRemoved;

        public SimpleChange(K key, V old, V added, boolean wasAdded, boolean wasRemoved) {
            super(ObservableMapWrapper.this);
            assert(wasAdded || wasRemoved);
            this.key = key;
            this.old = old;
            this.added = added;
            this.wasAdded = wasAdded;
            this.wasRemoved = wasRemoved;
        }

        @Override
        public boolean wasAdded() {
            return wasAdded;
        }

        @Override
        public boolean wasRemoved() {
            return wasRemoved;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValueAdded() {
            return added;
        }

        @Override
        public V getValueRemoved() {
            return old;
        }

        @Override
        public String toString() {
            return changeToString(this);
        }
    }

    private interface ContainsPredicate<K, V> {
        boolean contains(Collection<?> c, Entry<K, V> e);
    }
}
