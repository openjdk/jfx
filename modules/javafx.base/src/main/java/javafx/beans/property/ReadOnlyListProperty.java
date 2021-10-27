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

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import com.sun.javafx.binding.BidirectionalContentBinding;
import javafx.beans.binding.ListExpression;
import javafx.collections.ObservableList;

/**
 * Superclass for all readonly properties wrapping an {@link javafx.collections.ObservableList}.
 *
 * @see javafx.collections.ObservableList
 * @see javafx.beans.value.ObservableListValue
 * @see javafx.beans.binding.ListExpression
 * @see ReadOnlyProperty
 *
 * @param <E> the type of the {@code List} elements
 * @since JavaFX 2.1
 */
public abstract class ReadOnlyListProperty<E> extends ListExpression<E>
        implements ReadOnlyProperty<ObservableList<E>>  {

    /**
     * The constructor of {@code ReadOnlyListProperty}.
     */
    public ReadOnlyListProperty() {
    }

    /**
     * Creates a bidirectional content binding between the {@link javafx.collections.ObservableList} that is
     * wrapped in this {@code ReadOnlyListProperty} and another {@code ObservableList}.
     * <p>
     * A bidirectional content binding ensures that the content of the two lists is the same.
     * If the content of one of the lists changes, the content of the other list will be updated automatically.
     *
     * @param other the {@code ObservableList} this property should be bound to
     * @throws NullPointerException if {@code other} is {@code null}
     * @throws IllegalArgumentException if {@code other} is the list wrapped in this {@code ReadOnlyListProperty}
     */
    public void bindContentBidirectional(ObservableList<E> other) {
        BidirectionalContentBinding.bind(this, other);
    }

    /**
     * Removes the bidirectional content binding that was established with
     * {@link #bindContentBidirectional(ObservableList)}.
     * <p>
     * Bidirectional content bindings can be removed by calling this method on either of the two endpoints:
     * <pre>{@code
     * property1.bindContentBidirectional(property2);
     * property2.unbindContentBidirectional(property1);
     * }</pre>
     * The content of the wrapped list will remain unchanged.
     * If this property is not bidirectionally content-bound, calling this method has no effect.
     *
     * @param other the {@code ObservableList} to which the bidirectional content binding should be removed
     * @throws NullPointerException if {@code other} is {@code null}
     * @throws IllegalArgumentException if {@code other} is the list wrapped in this {@code ReadOnlyListProperty}
     * @since 18
     */
    public void unbindContentBidirectional(ObservableList<E> other) {
        BidirectionalContentBinding.unbind(this, other);
    }

    /**
     * Removes the bidirectional content binding that was established with
     * {@link #bindContentBidirectional(ObservableList)}.
     * <p>
     * Bidirectional content bindings can be removed by calling this method on either of the two endpoints:
     * <pre>{@code
     * property1.bindContentBidirectional(property2);
     * property2.unbindContentBidirectional(property1);
     * }</pre>
     * The content of the wrapped list will remain unchanged.
     * If this property is not bidirectionally content-bound, calling this method has no effect.
     *
     * @param other the {@code Object} to which the bidirectional content binding should be removed
     * @throws NullPointerException if {@code other} is {@code null}
     * @throws IllegalArgumentException if {@code other} is the list wrapped in this {@code ReadOnlyListProperty}
     * @deprecated use {@link #unbindContentBidirectional(ObservableList)} instead
     */
    @Deprecated(since = "18", forRemoval = true)
    public void unbindContentBidirectional(Object other) {
        Objects.requireNonNull(other);
        if (other instanceof ObservableList<?>) {
            BidirectionalContentBinding.unbind(this, (ObservableList<E>)other);
        }
    }

    /**
     * Creates a content binding between the {@link javafx.collections.ObservableList} that is wrapped
     * in this {@code ReadOnlyListProperty} (the <em>bound list</em>) and another {@code ObservableList}
     * (the <em>source list</em>).
     * <p>
     * A content binding ensures that the content of the bound list is the same as that of the source list.
     * If the content of the source list changes, the content of the bound list will be updated automatically.
     * In contrast, a regular binding will replace the bound list instance with the source list instance,
     * which means that only a single list instance exists for both properties.
     * <p>
     * Once a content binding is established, the bound list becomes effectively read-only: any attempt to
     * change the content of the bound list by calling a mutating method of {@link ObservableList} will cause
     * the content binding to fail. In this case, the content binding is removed because the bound list and
     * the source list may be out-of-sync.
     *
     * @param source the source {@code ObservableList} this property should be bound to
     * @throws NullPointerException if {@code source} is {@code null}
     * @throws IllegalArgumentException if {@code source} is the list wrapped in this {@code ReadOnlyListProperty}
     */
    public abstract void bindContent(ObservableList<E> source);

    /**
     * Removes the content binding that was established with {@link #bindContent(ObservableList)}.
     * <p>
     * The content of the wrapped list will remain unchanged.
     * If this property is not content-bound, calling this method has no effect.
     *
     * @since 18
     */
    public void unbindContent() {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the content binding that was established with {@link #bindContent(ObservableList)}.
     * <p>
     * The content of the wrapped list will remain unchanged.
     * If this property is not content-bound, calling this method has no effect.
     *
     * @param source the content binding source
     * @deprecated use {@link #unbindContent()}
     */
    @Deprecated(since = "18", forRemoval = true)
    public abstract void unbindContent(Object source);

    /**
     * Returns whether this property is bound by a unidirectional content binding that was
     * established by calling {@link #bindContent(ObservableList)}.
     * <p>
     * Note that this method does not account for bidirectional content bindings that were
     * established by calling {@link #bindContentBidirectional(ObservableList)}.
     *
     * @return whether this property is bound by a unidirectional content binding
     * @since 18
     */
    public boolean isContentBound() {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof List)) {
            return false;
        }
        final List list = (List)obj;

        if (size() != list.size()) {
            return false;
        }

        ListIterator<E> e1 = listIterator();
        ListIterator e2 = list.listIterator();
        while (e1.hasNext() && e2.hasNext()) {
            E o1 = e1.next();
            Object o2 = e2.next();
            if (!(o1==null ? o2==null : o1.equals(o2)))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (E e : this)
            hashCode = 31*hashCode + (e==null ? 0 : e.hashCode());
        return hashCode;
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
