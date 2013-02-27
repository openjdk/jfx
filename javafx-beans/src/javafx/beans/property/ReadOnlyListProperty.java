/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListExpression;
import javafx.collections.ObservableList;

/**
 * Super class for all readonly properties wrapping an {@link javafx.collections.ObservableList}.
 *
 * @see javafx.collections.ObservableList
 * @see javafx.beans.value.ObservableListValue
 * @see javafx.beans.binding.ListExpression
 * @see ReadOnlyProperty
 *
 * @param <E> the type of the {@code List} elements
 */
public abstract class ReadOnlyListProperty<E> extends ListExpression<E>
        implements ReadOnlyProperty<ObservableList<E>>  {

    /**
     * The constructor of {@code ReadOnlyListProperty}.
     */
    public ReadOnlyListProperty() {
    }

    /**
     * Creates a bidirectional content binding of the {@link javafx.collections.ObservableList}, that is
     * wrapped in this {@code ReadOnlyListProperty}, and another {@code ObservableList}.
     * <p>
     * A bidirectional content binding ensures that the content of two {@code ObservableLists} is the
     * same. If the content of one of the lists changes, the other one will be updated automatically.
     *
     * @param list the {@code ObservableList} this property should be bound to
     * @throws NullPointerException if {@code list} is {@code null}
     * @throws IllegalArgumentException if {@code list} is the same list that this {@code ReadOnlyListProperty} points to
     */
    public void bindContentBidirectional(ObservableList<E> list) {
        Bindings.bindContentBidirectional(this, list);
    }

    /**
     * Deletes a bidirectional content binding between the {@link javafx.collections.ObservableList}, that is
     * wrapped in this {@code ReadOnlyListProperty}, and another {@code Object}.
     *
     * @param object the {@code Object} to which the bidirectional binding should be removed
     * @throws NullPointerException if {@code object} is {@code null}
     * @throws IllegalArgumentException if {@code object} is the same list that this {@code ReadOnlyListProperty} points to
     */
    public void unbindContentBidirectional(Object object) {
        Bindings.unbindContentBidirectional(this, object);
    }

    /**
     * Creates a content binding between the {@link javafx.collections.ObservableList}, that is
     * wrapped in this {@code ReadOnlyListProperty}, and another {@code ObservableList}.
     * <p>
     * A content binding ensures that the content of the wrapped {@code ObservableLists} is the
     * same as that of the other list. If the content of the other list changes, the wrapped list will be updated
     * automatically. Once the wrapped list is bound to another list, you must not change it directly.
     *
     * @param list the {@code ObservableList} this property should be bound to
     * @throws NullPointerException if {@code list} is {@code null}
     * @throws IllegalArgumentException if {@code list} is the same list that this {@code ReadOnlyListProperty} points to
     */
    public void bindContent(ObservableList<E> list) {
        Bindings.bindContent(this, list);
    }

    /**
     * Deletes a content binding between the {@link javafx.collections.ObservableList}, that is
     * wrapped in this {@code ReadOnlyListProperty}, and another {@code Object}.
     *
     * @param object the {@code Object} to which the binding should be removed
     * @throws NullPointerException if {@code object} is {@code null}
     * @throws IllegalArgumentException if {@code object} is the same list that this {@code ReadOnlyListProperty} points to
     */
    public void unbindContent(Object object) {
        Bindings.unbindContent(this, object);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        final Object bean1 = getBean();
        final String name1 = getName();
        if ((bean1 == null) || (name1 == null) || name1.equals("")) {
            return false;
        }
        if (obj instanceof ReadOnlyListProperty) {
            final ReadOnlyListProperty other = (ReadOnlyListProperty) obj;
            final Object bean2 = other.getBean();
            final String name2 = other.getName();
            return (bean1 == bean2) && name1.equals(name2);
        }
        return false;
    }

    /**
     * Returns a hash code for this {@code ReadOnlyListProperty} object.
     * @return a hash code for this {@code ReadOnlyListProperty} object.
     */
    @Override
    public int hashCode() {
        final Object bean = getBean();
        final String name = getName();
        if ((bean == null) && ((name == null) || name.equals(""))) {
            return super.hashCode();
        } else {
            int result = 17;
            result = 31 * result + ((bean == null)? 0 : bean.hashCode());
            result = 31 * result + ((name == null)? 0 : name.hashCode());
            return result;
        }
    }

    /**
     * Returns a string representation of this {@code ReadOnlyListProperty} object.
     * @return a string representation of this {@code ReadOnlyListProperty} object.
     */
    @Override
    public String toString() {
        final Object bean = getBean();
        final String name = getName();
        final StringBuilder result = new StringBuilder(
                "ReadOnlyListProperty [");
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
