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
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableListValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * {@code ListExpression} is an
 * {@link javafx.beans.value.ObservableListValue} plus additional convenience
 * methods to generate bindings in a fluent style.
 * <p>
 * A concrete sub-class of {@code ListExpression} has to implement the method
 * {@link javafx.beans.value.ObservableListValue#get()}, which provides the
 * actual value of this expression.
 * <p>
 * If the wrapped list of a {@code ListExpression} is {@code null}, all methods implementing the {@code List}
 * interface will behave as if they were applied to an immutable empty list.
 *
 * @param <E> the type of the {@code List} elements.
 * @since JavaFX 2.1
 */
public abstract class ListExpression<E> implements ObservableListValue<E> {

    private static final ObservableList EMPTY_LIST = FXCollections.emptyObservableList();

    /**
     * Creates a default {@code ListExpression}.
     */
    public ListExpression() {
    }

    @Override
    public ObservableList<E> getValue() {
        return get();
    }

    /**
     * Returns a {@code ListExpression} that wraps a
     * {@link javafx.beans.value.ObservableListValue}. If the
     * {@code ObservableListValue} is already a {@code ListExpression}, it
     * will be returned. Otherwise a new
     * {@link javafx.beans.binding.ListBinding} is created that is bound to
     * the {@code ObservableListValue}.
     *
     * @param <E> the type of the wrapped {@code List}
     * @param value
     *            The source {@code ObservableListValue}
     * @return A {@code ListExpression} that wraps the
     *         {@code ObservableListValue} if necessary
     * @throws NullPointerException
     *             if {@code value} is {@code null}
     */
    public static <E> ListExpression<E> listExpression(final ObservableListValue<E> value) {
        if (value == null) {
            throw new NullPointerException("List must be specified.");
        }
        return value instanceof ListExpression ? (ListExpression<E>) value
                : new ListBinding<E>() {
            {
                super.bind(value);
            }

            @Override
            public void dispose() {
                super.unbind(value);
            }

            @Override
            protected ObservableList<E> computeValue() {
                return value.get();
            }

            @Override
            public ObservableList<ObservableListValue<E>> getDependencies() {
                return FXCollections.singletonObservableList(value);
            }
        };
    }

    /**
     * The size of the list
     * @return the size
     */
    public int getSize() {
        return size();
    }

    /**
     * An integer property that represents the size of the list.
     * @return the property
     */
    public abstract ReadOnlyIntegerProperty sizeProperty();

    /**
     * A boolean property that is {@code true}, if the list is empty.
     * @return the {@code ReadOnlyBooleanProperty}
     *
     */
    public abstract ReadOnlyBooleanProperty emptyProperty();

    /**
     * Creates a new {@link ObjectBinding} that contains the element at the specified position.
     * If {@code index} points behind the list, the {@code ObjectBinding} contains {@code null}.
     *
     * @param index the index of the element
     * @return the {@code ObjectBinding}
     * @throws IllegalArgumentException if {@code index < 0}
     */
    public ObjectBinding<E> valueAt(int index) {
        return Bindings.valueAt(this, index);
    }

    /**
     * Creates a new {@link ObjectBinding} that contains the element at the specified position.
     * If {@code index} points outside of the list, the {@code ObjectBinding} contains {@code null}.
     *
     * @param index the index of the element
     * @return the {@code ObjectBinding}
     * @throws NullPointerException if {@code index} is {@code null}
     */
    public ObjectBinding<E> valueAt(ObservableIntegerValue index) {
        return Bindings.valueAt(this, index);
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if this list is equal to
     * another {@link javafx.collections.ObservableList}.
     *
     * @param other
     *            the other {@code ObservableList}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if {@code other} is {@code null}
     */
    public BooleanBinding isEqualTo(final ObservableList<?> other) {
        return Bindings.equal(this, other);
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if this list is not equal to
     * another {@link javafx.collections.ObservableList}.
     *
     * @param other
     *            the other {@code ObservableList}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if {@code other} is {@code null}
     */
    public BooleanBinding isNotEqualTo(final ObservableList<?> other) {
        return Bindings.notEqual(this, other);
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if the wrapped list is {@code null}.
     *
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding isNull() {
        return Bindings.isNull(this);
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if the wrapped list is not {@code null}.
     *
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding isNotNull() {
        return Bindings.isNotNull(this);
    }

    /**
     * Creates a {@link javafx.beans.binding.StringBinding} that holds the value
     * of the {@code ListExpression} turned into a {@code String}. If the
     * value of this {@code ListExpression} changes, the value of the
     * {@code StringBinding} will be updated automatically.
     *
     * @return the new {@code StringBinding}
     */
    public StringBinding asString() {
        return (StringBinding) StringFormatter.convert(this);
    }

    @Override
    public int size() {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.size() : list.size();
    }

    @Override
    public boolean isEmpty() {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.isEmpty() : list.isEmpty();
    }

    @Override
    public boolean contains(Object obj) {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.contains(obj) : list.contains(obj);
    }

    @Override
    public Iterator<E> iterator() {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.iterator() : list.iterator();
    }

    @Override
    public Object[] toArray() {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.toArray() : list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        final ObservableList<E> list = get();
        return (list == null)? (T[]) EMPTY_LIST.toArray(array) : list.toArray(array);
     }

    @Override
    public boolean add(E element) {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.add(element) : list.add(element);
    }

    @Override
    public boolean remove(Object obj) {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.remove(obj) : list.remove(obj);
    }

    @Override
    public boolean containsAll(Collection<?> objects) {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.contains(objects) : list.containsAll(objects);
    }

    @Override
    public boolean addAll(Collection<? extends E> elements) {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.addAll(elements) : list.addAll(elements);
    }

    @Override
    public boolean addAll(int i, Collection<? extends E> elements) {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.addAll(i, elements) : list.addAll(i, elements);
    }

    @Override
    public boolean removeAll(Collection<?> objects) {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.removeAll(objects) : list.removeAll(objects);
    }

    @Override
    public boolean retainAll(Collection<?> objects) {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.retainAll(objects) : list.retainAll(objects);
    }

    @Override
    public void clear() {
        final ObservableList<E> list = get();
        if (list == null) {
            EMPTY_LIST.clear();
        } else {
            list.clear();
        }
    }

    @Override
    public E get(int i) {
        final ObservableList<E> list = get();
        return (list == null)? (E) EMPTY_LIST.get(i) : list.get(i);
    }

    @Override
    public E set(int i, E element) {
        final ObservableList<E> list = get();
        return (list == null)? (E) EMPTY_LIST.set(i, element) : list.set(i, element);
    }

    @Override
    public void add(int i, E element) {
        final ObservableList<E> list = get();
        if (list == null) {
            EMPTY_LIST.add(i, element);
        } else {
            list.add(i, element);
        }
    }

    @Override
    public E remove(int i) {
        final ObservableList<E> list = get();
        return (list == null)? (E) EMPTY_LIST.remove(i) : list.remove(i);
    }

    @Override
    public int indexOf(Object obj) {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.indexOf(obj) : list.indexOf(obj);
    }

    @Override
    public int lastIndexOf(Object obj) {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.lastIndexOf(obj) : list.lastIndexOf(obj);
    }

    @Override
    public ListIterator<E> listIterator() {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.listIterator() : list.listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int i) {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.listIterator(i) : list.listIterator(i);
    }

    @Override
    public List<E> subList(int from, int to) {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.subList(from, to) : list.subList(from, to);
    }

    @Override
    public boolean addAll(E... elements) {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.addAll(elements) : list.addAll(elements);
    }

    @Override
    public boolean setAll(E... elements) {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.setAll(elements) : list.setAll(elements);
    }

    @Override
    public boolean setAll(Collection<? extends E> elements) {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.setAll(elements) : list.setAll(elements);
    }

    @Override
    public boolean removeAll(E... elements) {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.removeAll(elements) : list.removeAll(elements);
    }

    @Override
    public boolean retainAll(E... elements) {
        final ObservableList<E> list = get();
        return (list == null)? EMPTY_LIST.retainAll(elements) : list.retainAll(elements);
    }

    @Override
    public void remove(int from, int to) {
        final ObservableList<E> list = get();
        if (list == null) {
            EMPTY_LIST.remove(from, to);
        } else {
            list.remove(from, to);
        }
    }

}
