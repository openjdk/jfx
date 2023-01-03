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
import javafx.beans.value.WritableLongValue;
import com.sun.javafx.binding.Logging;

/**
 * This class defines a {@link Property} wrapping a {@code long} value.
 * <p>
 * The value of a {@code LongProperty} can be get and set with {@link #get()},
 * {@link #getValue()}, {@link #set(long)}, and {@link #setValue(Number)}.
 * <p>
 * A property can be bound and unbound unidirectional with
 * {@link #bind(ObservableValue)} and {@link #unbind()}. Bidirectional bindings
 * can be created and removed with {@link #bindBidirectional(Property)} and
 * {@link #unbindBidirectional(Property)}.
 * <p>
 * The context of a {@code LongProperty} can be read with {@link #getBean()}
 * and {@link #getName()}.
 * <p>
 * Note: setting or binding this property to a null value will set the property to "0.0". See {@link #setValue(java.lang.Number) }.
 * @see javafx.beans.value.ObservableLongValue
 * @see javafx.beans.value.WritableLongValue
 * @see ReadOnlyLongProperty
 * @see Property
 *
 * @since JavaFX 2.0
 */
public abstract class LongProperty extends ReadOnlyLongProperty implements
        Property<Number>, WritableLongValue {

    /**
     * Creates a default {@code LongProperty}.
     */
    public LongProperty() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(Number v) {
        if (v == null) {
            Logging.getLogger().fine("Attempt to set long property to null, using default value instead.", new NullPointerException());
            set(0L);
        } else {
            set(v.longValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bindBidirectional(Property<Number> other) {
        Bindings.bindBidirectional(this, other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unbindBidirectional(Property<Number> other) {
        Bindings.unbindBidirectional(this, other);
    }

    /**
     * Returns a string representation of this {@code LongProperty} object.
     * @return a string representation of this {@code LongProperty} object.
     */
    @Override
    public String toString() {
        final Object bean = getBean();
        final String name = getName();
        final StringBuilder result = new StringBuilder("LongProperty [");
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
     * Returns a {@code LongProperty} that wraps a
     * {@link javafx.beans.property.Property} and is
     * bidirectionally bound to it.
     * Changing this property will result in a change of the original property.
     *
     * <p>
     * This is very useful when bidirectionally binding an ObjectProperty&lt;Long&gt; and
     * a LongProperty.
     *
     * <blockquote><pre>
     *   LongProperty longProperty = new SimpleLongProperty(1L);
     *   ObjectProperty&lt;Long&gt; objectProperty = new SimpleObjectProperty&lt;&gt;(2L);
     *
     *   // Need to keep the reference as bidirectional binding uses weak references
     *   LongProperty objectAsLong = LongProperty.longProperty(objectProperty);
     *
     *   longProperty.bindBidirectional(objectAsLong);
     *
     * </pre></blockquote>
     *
     * Another approach is to convert the LongProperty to ObjectProperty using
     * {@link #asObject()} method.
     *
     * <p>
     * Note: null values in the source property will be interpreted as 0L
     *
     * @param property
     *            The source {@code Property}
     * @return A {@code LongProperty} that wraps the
     *         {@code Property}
     * @throws NullPointerException
     *             if {@code property} is {@code null}
     * @see #asObject()
     * @since JavaFX 8.0
     */
    public static LongProperty longProperty(final Property<Long> property) {
        Objects.requireNonNull(property, "Property cannot be null");
        return new LongPropertyBase() {
            {
                BidirectionalBinding.bindNumber(this, property);
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
     * Creates an {@link javafx.beans.property.ObjectProperty}
     * that bidirectionally bound to this {@code LongProperty}. If the
     * value of this {@code LongProperty} changes, the value of the
     * {@code ObjectProperty} will be updated automatically and vice-versa.
     *
     * <p>
     * Can be used for binding an ObjectProperty to LongProperty.
     *
     * <blockquote><pre>
     *   LongProperty longProperty = new SimpleLongProperty(1L);
     *   ObjectProperty&lt;Long&gt; objectProperty = new SimpleObjectProperty&lt;&gt;(2L);
     *
     *   objectProperty.bind(longProperty.asObject());
     * </pre></blockquote>
     *
     * @return the new {@code ObjectProperty}
     * @since JavaFX 8.0
     */
    @Override
    public ObjectProperty<Long> asObject() {
        return new ObjectPropertyBase<> () {
            {
                BidirectionalBinding.bindNumber(this, LongProperty.this);
            }

            @Override
            public Object getBean() {
                return null; // Virtual property, does not exist on a bean
            }

            @Override
            public String getName() {
                return LongProperty.this.getName();
            }
        };
    }
}
