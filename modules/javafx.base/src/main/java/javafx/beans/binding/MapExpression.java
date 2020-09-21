/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.binding;

import com.sun.javafx.binding.StringFormatter;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.value.ObservableMapValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.util.*;

/**
 * {@code MapExpression} is an
 * {@link javafx.beans.value.ObservableMapValue} plus additional convenience
 * methods to generate bindings in a fluent style.
 * <p>
 * A concrete sub-class of {@code MapExpression} has to implement the method
 * {@link javafx.beans.value.ObservableMapValue#get()}, which provides the
 * actual value of this expression.
 * <p>
 * If the wrapped list of a {@code MapExpression} is {@code null}, all methods implementing the {@code Map}
 * interface will behave as if they were applied to an immutable empty list.
 *
 * @param <K> the type of the key elements
 * @param <V> the type of the value elements
 * @since JavaFX 2.1
 */
public abstract class MapExpression<K, V> implements ObservableMapValue<K, V> {

    private static final ObservableMap EMPTY_MAP = new EmptyObservableMap();

    private static class EmptyObservableMap<K, V> extends AbstractMap<K, V> implements ObservableMap<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }

        @Override
        public void addListener(MapChangeListener<? super K, ? super V> mapChangeListener) {
            // no-op
        }

        @Override
        public void removeListener(MapChangeListener<? super K, ? super V> mapChangeListener) {
            // no-op
        }

        @Override
        public void addListener(InvalidationListener listener) {
            // no-op
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            // no-op
        }
    }

    @Override
    public ObservableMap<K, V> getValue() {
        return get();
    }

    /**
     * Creates a default {@code MapExpression}.
     */
    public MapExpression() {
    }

    /**
     * Returns a {@code MapExpression} that wraps a
     * {@link javafx.beans.value.ObservableMapValue}. If the
     * {@code ObservableMapValue} is already a {@code MapExpression}, it
     * will be returned. Otherwise a new
     * {@link javafx.beans.binding.MapBinding} is created that is bound to
     * the {@code ObservableMapValue}.
     *
     * @param <K> the type of the key elements
     * @param <V> the type of the value elements
     * @param value
     *            The source {@code ObservableMapValue}
     * @return A {@code MapExpression} that wraps the
     *         {@code ObservableMapValue} if necessary
     * @throws NullPointerException
     *             if {@code value} is {@code null}
     */
    public static <K, V> MapExpression<K, V> mapExpression(final ObservableMapValue<K, V> value) {
        if (value == null) {
            throw new NullPointerException("Map must be specified.");
        }
        return value instanceof MapExpression ? (MapExpression<K, V>) value
                : new MapBinding<K, V>() {
            {
                super.bind(value);
            }

            @Override
            public void dispose() {
                super.unbind(value);
            }

            @Override
            protected ObservableMap<K, V> computeValue() {
                return value.get();
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(value);
            }
        };
    }

    /**
     * The size of the map
     * @return the size
     */
    public int getSize() {
        return size();
    }

    /**
     * An integer property that represents the size of the map.
     * @return the property
     */
    public abstract ReadOnlyIntegerProperty sizeProperty();

    /**
     * A boolean property that is {@code true}, if the map is empty.
     * @return the {@code ReadOnlyBooleanProperty}
     */
    public abstract ReadOnlyBooleanProperty emptyProperty();

    /**
     * Creates a new {@link ObjectBinding} that contains the mapping of the specified key.
     *
     * @param key the key of the mapping
     * @return the {@code ObjectBinding}
     */
    public ObjectBinding<V> valueAt(K key) {
        return Bindings.valueAt(this, key);
    }

    /**
     * Creates a new {@link ObjectBinding} that contains the mapping of the specified key.
     *
     * @param key the key of the mapping
     * @return the {@code ObjectBinding}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public ObjectBinding<V> valueAt(ObservableValue<K> key) {
        return Bindings.valueAt(this, key);
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if this map is equal to
     * another {@link javafx.collections.ObservableMap}.
     *
     * @param other
     *            the other {@code ObservableMap}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if {@code other} is {@code null}
     */
    public BooleanBinding isEqualTo(final ObservableMap<?, ?> other) {
        return Bindings.equal(this, other);
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if this map is not equal to
     * another {@link javafx.collections.ObservableMap}.
     *
     * @param other
     *            the other {@code ObservableMap}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if {@code other} is {@code null}
     */
    public BooleanBinding isNotEqualTo(final ObservableMap<?, ?> other) {
        return Bindings.notEqual(this, other);
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if the wrapped map is {@code null}.
     *
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding isNull() {
        return Bindings.isNull(this);
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if the wrapped map is not {@code null}.
     *
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding isNotNull() {
        return Bindings.isNotNull(this);
    }

    /**
     * Creates a {@link javafx.beans.binding.StringBinding} that holds the value
     * of the {@code MapExpression} turned into a {@code String}. If the
     * value of this {@code MapExpression} changes, the value of the
     * {@code StringBinding} will be updated automatically.
     *
     * @return the new {@code StringBinding}
     */
    public StringBinding asString() {
        return (StringBinding) StringFormatter.convert(this);
    }

    @Override
    public int size() {
        final ObservableMap<K, V> map = get();
        return (map == null)? EMPTY_MAP.size() : map.size();
    }

    @Override
    public boolean isEmpty() {
        final ObservableMap<K, V> map = get();
        return (map == null)? EMPTY_MAP.isEmpty() : map.isEmpty();
    }

    @Override
    public boolean containsKey(Object obj) {
        final ObservableMap<K, V> map = get();
        return (map == null)? EMPTY_MAP.containsKey(obj) : map.containsKey(obj);
    }

    @Override
    public boolean containsValue(Object obj) {
        final ObservableMap<K, V> map = get();
        return (map == null)? EMPTY_MAP.containsValue(obj) : map.containsValue(obj);
    }

    @Override
    public V put(K key, V value) {
        final ObservableMap<K, V> map = get();
        return (map == null)? (V) EMPTY_MAP.put(key, value) : map.put(key, value);
    }

    @Override
    public V remove(Object obj) {
        final ObservableMap<K, V> map = get();
        return (map == null)? (V) EMPTY_MAP.remove(obj) : map.remove(obj);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> elements) {
        final ObservableMap<K, V> map = get();
        if (map == null) {
            EMPTY_MAP.putAll(elements);
        } else {
            map.putAll(elements);
        }
    }

    @Override
    public void clear() {
        final ObservableMap<K, V> map = get();
        if (map == null) {
            EMPTY_MAP.clear();
        } else {
            map.clear();
        }
    }

    @Override
    public Set<K> keySet() {
        final ObservableMap<K, V> map = get();
        return (map == null)? EMPTY_MAP.keySet() : map.keySet();
    }

    @Override
    public Collection<V> values() {
        final ObservableMap<K, V> map = get();
        return (map == null)? EMPTY_MAP.values() : map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        final ObservableMap<K, V> map = get();
        return (map == null)? EMPTY_MAP.entrySet() : map.entrySet();
    }

    @Override
    public V get(Object key) {
        final ObservableMap<K, V> map = get();
        return (map == null)? (V) EMPTY_MAP.get(key) : map.get(key);
    }

}
