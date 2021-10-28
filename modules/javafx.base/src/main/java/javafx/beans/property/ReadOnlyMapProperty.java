/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.binding.BidirectionalContentBinding;
import javafx.beans.binding.MapExpression;
import javafx.collections.ObservableMap;

import java.util.Map;
import java.util.Objects;

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
     * Creates a bidirectional content binding between the {@link javafx.collections.ObservableMap} that is
     * wrapped in this {@code ReadOnlyMapProperty} and another {@code ObservableMap}.
     * <p>
     * A bidirectional content binding ensures that the content of two maps is the same.
     * If the content of one of the maps changes, the content of the other map will be updated automatically.
     *
     * @param other the {@code ObservableMap} this property should be bound to
     * @throws NullPointerException if {@code other} is {@code null}
     * @throws IllegalArgumentException if {@code other} is the map wrapped in this {@code ReadOnlyMapProperty}
     */
    public void bindContentBidirectional(ObservableMap<K, V> other) {
        BidirectionalContentBinding.bind(this, other);
    }

    /**
     * Removes the bidirectional content binding that was established with
     * {@link #bindContentBidirectional(ObservableMap)}.
     * <p>
     * Bidirectional content bindings can be removed by calling this method on either of the two endpoints:
     * <pre>{@code
     * property1.bindContentBidirectional(property2);
     * property2.unbindContentBidirectional(property1);
     * }</pre>
     * The content of the wrapped map will remain unchanged.
     * If this property is not bidirectionally content-bound, calling this method has no effect.
     *
     * @param other the {@code ObservableMap} to which the bidirectional content binding should be removed
     * @throws NullPointerException if {@code other} is {@code null}
     * @throws IllegalArgumentException if {@code other} is the map wrapped in this {@code ReadOnlyMapProperty}
     * @since 18
     */
    public void unbindContentBidirectional(ObservableMap<K, V> other) {
        BidirectionalContentBinding.unbind(this, other);
    }

    /**
     * Removes the bidirectional content binding that was established with
     * {@link #bindContentBidirectional(ObservableMap)}.
     * <p>
     * Bidirectional content bindings can be removed by calling this method on either of the two endpoints:
     * <pre>{@code
     * property1.bindContentBidirectional(property2);
     * property2.unbindContentBidirectional(property1);
     * }</pre>
     * The content of the wrapped map will remain unchanged.
     * If this property is not bidirectionally content-bound, calling this method has no effect.
     *
     * @param other the {@code Object} to which the bidirectional content binding should be removed
     * @throws NullPointerException if {@code other} is {@code null}
     * @throws IllegalArgumentException if {@code other} is the map wrapped in this {@code ReadOnlyMapProperty}
     * @deprecated use {@link #unbindContentBidirectional(ObservableMap)} instead
     */
    @Deprecated(since = "18", forRemoval = true)
    public void unbindContentBidirectional(Object other) {
        Objects.requireNonNull(other);
        if (other instanceof ObservableMap<?, ?>) {
            BidirectionalContentBinding.unbind(this, (ObservableMap<K, V>)other);
        }
    }

    /**
     * Creates a content binding between the {@link javafx.collections.ObservableMap} that is wrapped
     * in this {@code ReadOnlyMapProperty} (the <em>bound map</em>) and another {@code ObservableMap}
     * (the <em>source map</em>).
     * <p>
     * A content binding ensures that the content of the bound map is the same as that of the source map.
     * If the content of the source map changes, the content of the bound map will be updated automatically.
     * In contrast, a regular binding will replace the bound map instance with the source map instance,
     * which means that only a single map instance exists for both properties.
     * <p>
     * Once a content binding is established, the bound map becomes effectively read-only: any attempt to
     * change the content of the bound map by calling a mutating method of {@link ObservableMap} will cause
     * the content binding to fail. In this case, the content binding is removed because the bound map and
     * the source map may be out-of-sync.
     *
     * @param source the source {@code ObservableMap} this property should be bound to
     * @throws NullPointerException if {@code source} is {@code null}
     * @throws IllegalArgumentException if {@code source} is the map wrapped in this {@code ReadOnlyMapProperty}
     */
    public void bindContent(ObservableMap<K, V> source) {
        throw new UnsupportedOperationException();
    }

    /**
     * Deletes a content binding between the {@link javafx.collections.ObservableMap}, that is
     * wrapped in this {@code ReadOnlyMapProperty}, and another {@code Object}.
     *
     * @since 18
     */
    public void unbindContent() {
        throw new UnsupportedOperationException();
    }

    /**
     * Deletes a content binding between the {@link javafx.collections.ObservableMap}, that is
     * wrapped in this {@code ReadOnlyMapProperty}, and another {@code Object}.
     *
     * @param object the {@code Object} to which the binding should be removed
     * @throws NullPointerException if {@code object} is {@code null}
     * @throws IllegalArgumentException if {@code object} is the same map that this {@code ReadOnlyMapProperty} points to
     * @deprecated use {@link #unbindContent()} instead
     */
    @Deprecated(since = "18", forRemoval = true)
    public void unbindContent(Object object) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns whether this property is bound by a unidirectional content binding that was
     * established by calling {@link #bindContent(ObservableMap)}.
     * <p>
     * Note that this method does not account for bidirectional content bindings that were
     * established by calling {@link #bindContentBidirectional(ObservableMap)}.
     *
     * @return whether this property is bound by a unidirectional content binding
     * @since 18
     */
    public boolean isContentBound() {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (!(obj instanceof Map))
            return false;
        Map<K,V> m = (Map<K,V>) obj;
        if (m.size() != size())
            return false;

        try {
            for (Entry<K,V> e : entrySet()) {
                K key = e.getKey();
                V value = e.getValue();
                if (value == null) {
                    if (!(m.get(key)==null && m.containsKey(key)))
                        return false;
                } else {
                    if (!value.equals(m.get(key)))
                        return false;
                }
            }
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }

        return true;
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
