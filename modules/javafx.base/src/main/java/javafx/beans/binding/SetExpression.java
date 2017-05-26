/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.value.ObservableSetValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * {@code SetExpression} is an
 * {@link javafx.beans.value.ObservableSetValue} plus additional convenience
 * methods to generate bindings in a fluent style.
 * <p>
 * A concrete sub-class of {@code SetExpression} has to implement the method
 * {@link javafx.beans.value.ObservableSetValue#get()}, which provides the
 * actual value of this expression.
 * <p>
 * If the wrapped list of a {@code SetExpression} is {@code null}, all methods implementing the {@code Set}
 * interface will behave as if they were applied to an immutable empty set.
 *
 * @param <E> the type of the {@code Set} elements
 * @since JavaFX 2.1
 */
public abstract class SetExpression<E> implements ObservableSetValue<E> {

    private static final ObservableSet EMPTY_SET = new EmptyObservableSet();

    private static class EmptyObservableSet<E> extends AbstractSet<E> implements ObservableSet<E> {

        private static final Iterator iterator = new Iterator() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Object next() {
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();

            }
        };

        @Override
        public Iterator<E> iterator() {
            return iterator;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public void addListener(SetChangeListener<? super E> setChangeListener) {
            // no-op
        }

        @Override
        public void removeListener(SetChangeListener<? super E> setChangeListener) {
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
    public ObservableSet<E> getValue() {
        return get();
    }

    /**
     * Returns a {@code SetExpression} that wraps a
     * {@link javafx.beans.value.ObservableSetValue}. If the
     * {@code ObservableSetValue} is already a {@code SetExpression}, it
     * will be returned. Otherwise a new
     * {@link javafx.beans.binding.SetBinding} is created that is bound to
     * the {@code ObservableSetValue}.
     *
     * @param <E> the type of the {@code Set} elements
     * @param value
     *            The source {@code ObservableSetValue}
     * @return A {@code SetExpression} that wraps the
     *         {@code ObservableSetValue} if necessary
     * @throws NullPointerException
     *             if {@code value} is {@code null}
     */
    public static <E> SetExpression<E> setExpression(final ObservableSetValue<E> value) {
        if (value == null) {
            throw new NullPointerException("Set must be specified.");
        }
        return value instanceof SetExpression ? (SetExpression<E>) value
                : new SetBinding<E>() {
            {
                super.bind(value);
            }

            @Override
            public void dispose() {
                super.unbind(value);
            }

            @Override
            protected ObservableSet<E> computeValue() {
                return value.get();
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(value);
            }
        };
    }

    /**
     * The size of the set
     * @return the size
     */
    public int getSize() {
        return size();
    }

    /**
     * An integer property that represents the size of the set.
     * @return the property
     */
    public abstract ReadOnlyIntegerProperty sizeProperty();

    /**
     * A boolean property that is {@code true}, if the set is empty.
     * @return the {@code ReadOnlyBooleanProperty}
     */
    public abstract ReadOnlyBooleanProperty emptyProperty();

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if this set is equal to
     * another {@link javafx.collections.ObservableSet}.
     *
     * @param other
     *            the other {@code ObservableSet}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if {@code other} is {@code null}
     */
    public BooleanBinding isEqualTo(final ObservableSet<?> other) {
        return Bindings.equal(this, other);
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if this set is not equal to
     * another {@link javafx.collections.ObservableSet}.
     *
     * @param other
     *            the other {@code ObservableSet}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if {@code other} is {@code null}
     */
    public BooleanBinding isNotEqualTo(final ObservableSet<?> other) {
        return Bindings.notEqual(this, other);
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if the wrapped set is {@code null}.
     *
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding isNull() {
        return Bindings.isNull(this);
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if the wrapped set is not {@code null}.
     *
     * @return the new {@code BooleanBinding}
     */
    public BooleanBinding isNotNull() {
        return Bindings.isNotNull(this);
    }

    /**
     * Creates a {@link javafx.beans.binding.StringBinding} that holds the value
     * of the {@code SetExpression} turned into a {@code String}. If the
     * value of this {@code SetExpression} changes, the value of the
     * {@code StringBinding} will be updated automatically.
     *
     * @return the new {@code StringBinding}
     */
    public StringBinding asString() {
        return (StringBinding) StringFormatter.convert(this);
    }

    @Override
    public int size() {
        final ObservableSet<E> set = get();
        return (set == null)? EMPTY_SET.size() : set.size();
    }

    @Override
    public boolean isEmpty() {
        final ObservableSet<E> set = get();
        return (set == null)? EMPTY_SET.isEmpty() : set.isEmpty();
    }

    @Override
    public boolean contains(Object obj) {
        final ObservableSet<E> set = get();
        return (set == null)? EMPTY_SET.contains(obj) : set.contains(obj);
    }

    @Override
    public Iterator<E> iterator() {
        final ObservableSet<E> set = get();
        return (set == null)? EMPTY_SET.iterator() : set.iterator();
    }

    @Override
    public Object[] toArray() {
        final ObservableSet<E> set = get();
        return (set == null)? EMPTY_SET.toArray() : set.toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        final ObservableSet<E> set = get();
        return (set == null)? (T[]) EMPTY_SET.toArray(array) : set.toArray(array);
     }

    @Override
    public boolean add(E element) {
        final ObservableSet<E> set = get();
        return (set == null)? EMPTY_SET.add(element) : set.add(element);
    }

    @Override
    public boolean remove(Object obj) {
        final ObservableSet<E> set = get();
        return (set == null)? EMPTY_SET.remove(obj) : set.remove(obj);
    }

    @Override
    public boolean containsAll(Collection<?> objects) {
        final ObservableSet<E> set = get();
        return (set == null)? EMPTY_SET.contains(objects) : set.containsAll(objects);
    }

    @Override
    public boolean addAll(Collection<? extends E> elements) {
        final ObservableSet<E> set = get();
        return (set == null)? EMPTY_SET.addAll(elements) : set.addAll(elements);
    }

    @Override
    public boolean removeAll(Collection<?> objects) {
        final ObservableSet<E> set = get();
        return (set == null)? EMPTY_SET.removeAll(objects) : set.removeAll(objects);
    }

    @Override
    public boolean retainAll(Collection<?> objects) {
        final ObservableSet<E> set = get();
        return (set == null)? EMPTY_SET.retainAll(objects) : set.retainAll(objects);
    }

    @Override
    public void clear() {
        final ObservableSet<E> set = get();
        if (set == null) {
            EMPTY_SET.clear();
        } else {
            set.clear();
        }
    }

}
