/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.value.ObservableSetValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import java.util.Collection;
import java.util.Iterator;

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

    /**
     * Creates a default {@code SetExpression}.
     */
    public SetExpression() {
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
                : new SetBinding<>() {
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
        return getNonNull().size();
    }

    @Override
    public boolean isEmpty() {
        return getNonNull().isEmpty();
    }

    @Override
    public boolean contains(Object obj) {
        return getNonNull().contains(obj);
    }

    @Override
    public Iterator<E> iterator() {
        return getNonNull().iterator();
    }

    @Override
    public Object[] toArray() {
        return getNonNull().toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        return getNonNull().toArray(array);
     }

    @Override
    public boolean add(E element) {
        return getNonNull().add(element);
    }

    @Override
    public boolean remove(Object obj) {
        return getNonNull().remove(obj);
    }

    @Override
    public boolean containsAll(Collection<?> objects) {
        return getNonNull().containsAll(objects);
    }

    @Override
    public boolean addAll(Collection<? extends E> elements) {
        return getNonNull().addAll(elements);
    }

    @Override
    public boolean removeAll(Collection<?> objects) {
        return getNonNull().removeAll(objects);
    }

    @Override
    public boolean retainAll(Collection<?> objects) {
        return getNonNull().retainAll(objects);
    }

    @Override
    public void clear() {
        getNonNull().clear();
    }

    private ObservableSet<E> getNonNull() {
        ObservableSet<E> set = get();

        return set == null ? FXCollections.emptyObservableSet() : set;
    }
}
