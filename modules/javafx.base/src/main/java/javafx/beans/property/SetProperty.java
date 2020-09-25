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

package javafx.beans.property;

import javafx.beans.binding.Bindings;
import javafx.beans.value.WritableListValue;
import javafx.beans.value.WritableSetValue;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

/**
 * This class provides a full implementation of a {@link Property} wrapping an
 * {@link javafx.collections.ObservableSet}.
 *
 * The value of a {@code SetProperty} can be get and set with {@link #get()},
 * {@link #getValue()}, {@link #set(Object)}, and {@link #setValue(ObservableSet)}.
 *
 * A property can be bound and unbound unidirectional with
 * {@link #bind(javafx.beans.value.ObservableValue)} and {@link #unbind()}. Bidirectional bindings
 * can be created and removed with {@link #bindBidirectional(Property)} and
 * {@link #unbindBidirectional(Property)}.
 *
 * The context of a {@code SetProperty} can be read with {@link #getBean()}
 * and {@link #getName()}.
 *
 * @see javafx.collections.ObservableSet
 * @see javafx.beans.value.ObservableSetValue
 * @see javafx.beans.value.WritableSetValue
 * @see ReadOnlySetProperty
 * @see Property
 *
 * @param <E> the type of the {@code Set} elements
 * @since JavaFX 2.1
 */
public abstract class SetProperty<E> extends ReadOnlySetProperty<E> implements
        Property<ObservableSet<E>>, WritableSetValue<E> {

    /**
     * Creates a default {@code SetProperty}.
     */
    public SetProperty() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(ObservableSet<E> v) {
        set(v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bindBidirectional(Property<ObservableSet<E>> other) {
        Bindings.bindBidirectional(this, other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unbindBidirectional(Property<ObservableSet<E>> other) {
        Bindings.unbindBidirectional(this, other);
    }

    /**
     * Returns a string representation of this {@code SetProperty} object.
     * @return a string representation of this {@code SetProperty} object.
     */
    @Override
    public String toString() {
        final Object bean = getBean();
        final String name = getName();
        final StringBuilder result = new StringBuilder(
                "SetProperty [");
        if (bean != null) {
            result.append("bean: ").append(bean).append(", ");
        }
        if ((name != null) && (!name.equals(""))) {
            result.append("name: ").append(name).append(", ");
        }
        result.append("value: ").append(get()).append("]");
        return result.toString();
    }
}
