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

import java.util.Set;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.SetExpression;
import javafx.collections.ObservableSet;

/**
 * Superclass for all readonly properties wrapping an {@link javafx.collections.ObservableSet}.
 *
 * @see javafx.collections.ObservableSet
 * @see javafx.beans.value.ObservableSetValue
 * @see javafx.beans.binding.SetExpression
 * @see ReadOnlyProperty
 *
 * @param <E> the type of the {@code Set} elements
 * @since JavaFX 2.1
 */
public abstract class ReadOnlySetProperty<E> extends SetExpression<E> implements ReadOnlyProperty<ObservableSet<E>>  {

    /**
     * The constructor of {@code ReadOnlySetProperty}.
     */
    public ReadOnlySetProperty() {
    }

    /**
     * Creates a bidirectional content binding of the {@link javafx.collections.ObservableSet}, that is
     * wrapped in this {@code ReadOnlySetProperty}, and another {@code ObservableSet}.
     * <p>
     * A bidirectional content binding ensures that the content of two {@code ObservableSets} is the
     * same. If the content of one of the sets changes, the other one will be updated automatically.
     *
     * @param set the {@code ObservableSet} this property should be bound to
     * @throws NullPointerException if {@code set} is {@code null}
     * @throws IllegalArgumentException if {@code set} is the same set that this {@code ReadOnlySetProperty} points to
     */
    public void bindContentBidirectional(ObservableSet<E> set) {
        Bindings.bindContentBidirectional(this, set);
    }

    /**
     * Deletes a bidirectional content binding between the {@link javafx.collections.ObservableSet}, that is
     * wrapped in this {@code ReadOnlySetProperty}, and another {@code Object}.
     *
     * @param object the {@code Object} to which the bidirectional binding should be removed
     * @throws NullPointerException if {@code object} is {@code null}
     * @throws IllegalArgumentException if {@code object} is the same set that this {@code ReadOnlySetProperty} points to
     */
    public void unbindContentBidirectional(Object object) {
        Bindings.unbindContentBidirectional(this, object);
    }

    /**
     * Creates a content binding between the {@link javafx.collections.ObservableSet}, that is
     * wrapped in this {@code ReadOnlySetProperty}, and another {@code ObservableSet}.
     * <p>
     * A content binding ensures that the content of the wrapped {@code ObservableSets} is the
     * same as that of the other set. If the content of the other set changes, the wrapped set will be updated
     * automatically. Once the wrapped set is bound to another set, you must not change it directly.
     *
     * @param set the {@code ObservableSet} this property should be bound to
     * @throws NullPointerException if {@code set} is {@code null}
     * @throws IllegalArgumentException if {@code set} is the same set that this {@code ReadOnlySetProperty} points to
     */
    public void bindContent(ObservableSet<E> set) {
        Bindings.bindContent(this, set);
    }

    /**
     * Deletes a content binding between the {@link javafx.collections.ObservableSet}, that is
     * wrapped in this {@code ReadOnlySetProperty}, and another {@code Object}.
     *
     * @param object the {@code Object} to which the binding should be removed
     * @throws NullPointerException if {@code object} is {@code null}
     * @throws IllegalArgumentException if {@code object} is the same set that this {@code ReadOnlySetProperty} points to
     */
    public void unbindContent(Object object) {
        Bindings.unbindContent(this, object);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Set<?> otherSet) || otherSet.size() != size()) {
            return false;
        }

        try {
            return containsAll(otherSet);
        } catch (ClassCastException | NullPointerException unused)   {
            return false;
        }
    }

    /**
     * Returns a hash code for this {@code ReadOnlySetProperty} object.
     * @return a hash code for this {@code ReadOnlySetProperty} object.
     */
    @Override
    public int hashCode() {
        int h = 0;
        for (E e : this) {
            if (e != null)
                h += e.hashCode();
        }
        return h;
    }

    /**
     * Returns a string representation of this {@code ReadOnlySetProperty} object.
     * @return a string representation of this {@code ReadOnlySetProperty} object.
     */
    @Override
    public String toString() {
        final Object bean = getBean();
        final String name = getName();
        final StringBuilder result = new StringBuilder(
                "ReadOnlySetProperty [");
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
