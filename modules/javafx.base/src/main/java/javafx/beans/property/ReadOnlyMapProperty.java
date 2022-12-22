/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.property;

import java.util.Map;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.MapExpression;
import javafx.collections.ObservableMap;

/**
 * Superclass for all readonly properties wrapping an {@link javafx.collections.ObservableMap}.
 *
 * @see javafx.collections.ObservableMap
 * @see javafx.beans.value.ObservableMapValue
 * @see javafx.beans.binding.MapExpression
 * @see ReadOnlyProperty
 *
 * @param <K> the type of the key elements of the map
 * @param <V> the type of the value elements of the map
 * @since JavaFX 2.1
 */
public abstract class ReadOnlyMapProperty<K, V> extends MapExpression<K, V> implements ReadOnlyProperty<ObservableMap<K, V>>  {

    /**
     * The constructor of {@code ReadOnlyMapProperty}.
     */
    public ReadOnlyMapProperty() {
    }

    /**
     * Creates a bidirectional content binding of the {@link javafx.collections.ObservableMap}, that is
     * wrapped in this {@code ReadOnlyMapProperty}, and another {@code ObservableMap}.
     * <p>
     * A bidirectional content binding ensures that the content of two {@code ObservableMaps} is the
     * same. If the content of one of the maps changes, the other one will be updated automatically.
     *
     * @param map the {@code ObservableMap} this property should be bound to
     * @throws NullPointerException if {@code map} is {@code null}
     * @throws IllegalArgumentException if {@code map} is the same map that this {@code ReadOnlyMapProperty} points to
     */
    public void bindContentBidirectional(ObservableMap<K, V> map) {
        Bindings.bindContentBidirectional(this, map);
    }

    /**
     * Deletes a bidirectional content binding between the {@link javafx.collections.ObservableMap}, that is
     * wrapped in this {@code ReadOnlyMapProperty}, and another {@code Object}.
     *
     * @param object the {@code Object} to which the bidirectional binding should be removed
     * @throws NullPointerException if {@code object} is {@code null}
     * @throws IllegalArgumentException if {@code object} is the same map that this {@code ReadOnlyMapProperty} points to
     */
    public void unbindContentBidirectional(Object object) {
        Bindings.unbindContentBidirectional(this, object);
    }

    /**
     * Creates a content binding between the {@link javafx.collections.ObservableMap}, that is
     * wrapped in this {@code ReadOnlyMapProperty}, and another {@code ObservableMap}.
     * <p>
     * A content binding ensures that the content of the wrapped {@code ObservableMaps} is the
     * same as that of the other map. If the content of the other map changes, the wrapped map will be updated
     * automatically. Once the wrapped list is bound to another map, you must not change it directly.
     *
     * @param map the {@code ObservableMap} this property should be bound to
     * @throws NullPointerException if {@code map} is {@code null}
     * @throws IllegalArgumentException if {@code map} is the same map that this {@code ReadOnlyMapProperty} points to
     */
    public void bindContent(ObservableMap<K, V> map) {
        Bindings.bindContent(this, map);
    }

    /**
     * Deletes a content binding between the {@link javafx.collections.ObservableMap}, that is
     * wrapped in this {@code ReadOnlyMapProperty}, and another {@code Object}.
     *
     * @param object the {@code Object} to which the binding should be removed
     * @throws NullPointerException if {@code object} is {@code null}
     * @throws IllegalArgumentException if {@code object} is the same map that this {@code ReadOnlyMapProperty} points to
     */
    public void unbindContent(Object object) {
        Bindings.unbindContent(this, object);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Map<?, ?> otherMap) || otherMap.size() != size()) {
            return false;
        }

        try {
            for (Entry<K, V> e : entrySet()) {
                K key = e.getKey();
                V value = e.getValue();
                if (value == null) {
                    if (otherMap.get(key) != null || !otherMap.containsKey(key)) {
                        return false;
                    }
                } else if (!value.equals(otherMap.get(key))) {
                    return false;
                }
            }

            return true;
        } catch (ClassCastException | NullPointerException unused) {
            return false;
        }
    }

    /**
     * Returns a hash code for this {@code ReadOnlyMapProperty} object.
     * @return a hash code for this {@code ReadOnlyMapProperty} object.
     */
    @Override
    public int hashCode() {
        int h = 0;
        for (Entry<K,V> e : entrySet()) {
            h += e.hashCode();
        }
        return h;
    }

    /**
     * Returns a string representation of this {@code ReadOnlyMapProperty} object.
     * @return a string representation of this {@code ReadOnlyMapProperty} object.
     */
    @Override
    public String toString() {
        final Object bean = getBean();
        final String name = getName();
        final StringBuilder result = new StringBuilder(
                "ReadOnlyMapProperty [");
        if (bean != null) {
            result.append("bean: ").append(bean).append(", ");
        }
        if ((name != null) && !name.equals("")) {
            result.append("name: ").append(name).append(", ");
        }
        result.append("value: ").append(get()).append("]");
        return result.toString();
    }

}
