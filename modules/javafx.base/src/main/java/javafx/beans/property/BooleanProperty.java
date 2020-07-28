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

package javafx.beans.property;

import java.util.Objects;

import com.sun.javafx.binding.BidirectionalBinding;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableBooleanValue;
import com.sun.javafx.binding.Logging;

/**
 * This class provides a full implementation of a {@link Property} wrapping a
 * {@code boolean} value.
 * <p>
 * The value of a {@code BooleanProperty} can be get and set with {@link #get()},
 * {@link #getValue()}, {@link #set(boolean)}, and {@link #setValue(Boolean)}.
 * <p>
 * A property can be bound and unbound unidirectional with
 * {@link #bind(ObservableValue)} and {@link #unbind()}. Bidirectional bindings
 * can be created and removed with {@link #bindBidirectional(Property)} and
 * {@link #unbindBidirectional(Property)}.
 * <p>
 * The context of a {@code BooleanProperty} can be read with {@link #getBean()}
 * and {@link #getName()}.
 *
 * <p>
 * Note: setting or binding this property to a null value will set the property to "false". See {@link #setValue(java.lang.Boolean) }.
 *
 * @see javafx.beans.value.ObservableBooleanValue
 * @see javafx.beans.value.WritableBooleanValue
 * @see ReadOnlyBooleanProperty
 * @see Property
 *
 * @since JavaFX 2.0
 */
public abstract class BooleanProperty extends ReadOnlyBooleanProperty implements
        Property<Boolean>, WritableBooleanValue {

    /**
     * Sole constructor
     */
    public BooleanProperty() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(Boolean v) {
        if (v == null) {
            Logging.getLogger().fine("Attempt to set boolean property to null, using default value instead.", new NullPointerException());
            set(false);
        } else {
            set(v.booleanValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bindBidirectional(Property<Boolean> other) {
        Bindings.bindBidirectional(this, other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unbindBidirectional(Property<Boolean> other) {
        Bindings.unbindBidirectional(this, other);
    }

    /**
     * Returns a string representation of this {@code BooleanProperty} object.
     * @return a string representation of this {@code BooleanProperty} object.
     */
    @Override
    public String toString() {
        final Object bean = getBean();
        final String name = getName();
        final StringBuilder result = new StringBuilder(
                "BooleanProperty [");
        if (bean != null) {
            result.append("bean: ").append(bean).append(", ");
        }
        if ((name != null) && (!name.equals(""))) {
            result.append("name: ").append(name).append(", ");
        }
        result.append("value: ").append(get()).append("]");
        return result.toString();
    }

    /**
     * Returns a {@code BooleanProperty} that wraps a
     * {@link javafx.beans.property.Property}. If the
     * {@code Property} is already a {@code BooleanProperty}, it
     * will be returned. Otherwise a new
     * {@code BooleanProperty} is created that is bound to
     * the {@code Property}.
     *
     * Note: null values in the source property will be interpreted as "false"
     *
     * @param property
     *            The source {@code Property}
     * @return A {@code BooleanProperty} that wraps the
     *         {@code Property} if necessary
     * @throws NullPointerException
     *             if {@code property} is {@code null}
     * @since JavaFX 8.0
     */
    public static BooleanProperty booleanProperty(final Property<Boolean> property) {
        Objects.requireNonNull(property, "Property cannot be null");
        return property instanceof BooleanProperty ? (BooleanProperty)property : new BooleanPropertyBase() {
            {
                BidirectionalBinding.bind(this, property);
            }

            @Override
            public Object getBean() {
                return null; // Virtual property, no bean
            }

            @Override
            public String getName() {
                return property.getName();
            }
        };
    }

    /**
     * Creates an {@link javafx.beans.property.ObjectProperty} that holds the value
     * of this {@code BooleanProperty}. If the
     * value of this {@code BooleanProperty} changes, the value of the
     * {@code ObjectProperty} will be updated automatically.
     *
     * @return the new {@code ObjectProperty}
     * @since JavaFX 8.0
     */
    @Override
    public ObjectProperty<Boolean> asObject() {
        return new ObjectPropertyBase<> () {
            {
                BidirectionalBinding.bind(this, BooleanProperty.this);
            }

            @Override
            public Object getBean() {
                return null; // Virtual property, does not exist on a bean
            }

            @Override
            public String getName() {
                return BooleanProperty.this.getName();
            }
        };
    }
}
