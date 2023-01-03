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

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.LongExpression;

/**
 * Superclass for all readonly properties wrapping a {@code long}.
 *
 * @see javafx.beans.value.ObservableLongValue
 * @see javafx.beans.binding.LongExpression
 * @see ReadOnlyProperty
 *
 * @since JavaFX 2.0
 */
public abstract class ReadOnlyLongProperty extends LongExpression implements
        ReadOnlyProperty<Number> {

    /**
     * The constructor of {@code ReadOnlyLongProperty}.
     */
    public ReadOnlyLongProperty() {
    }

    /**
     * Returns a string representation of this {@code ReadOnlyLongProperty} object.
     * @return a string representation of this {@code ReadOnlyLongProperty} object.
     */
    @Override
    public String toString() {
        final Object bean = getBean();
        final String name = getName();
        final StringBuilder result = new StringBuilder("ReadOnlyLongProperty [");
        if (bean != null) {
            result.append("bean: ").append(bean).append(", ");
        }
        if ((name != null) && !name.equals("")) {
            result.append("name: ").append(name).append(", ");
        }
        result.append("value: ").append(get()).append("]");
        return result.toString();
    }

    /**
     * Returns a {@code ReadOnlyLongProperty} that wraps a
     * {@link javafx.beans.property.ReadOnlyProperty}. If the
     * {@code ReadOnlyProperty} is already a {@code ReadOnlyLongProperty}, it
     * will be returned. Otherwise a new
     * {@code ReadOnlyLongProperty} is created that is bound to
     * the {@code ReadOnlyProperty}.
     *
     * Note: null values will be interpreted as 0L
     *
     * @param <T> The type of Number to be wrapped
     * @param property
     *            The source {@code ReadOnlyProperty}
     * @return A {@code ReadOnlyLongProperty} that wraps the
     *         {@code ReadOnlyProperty} if necessary
     * @throws NullPointerException
     *             if {@code property} is {@code null}
     * @since JavaFX 8.0
     */
    public static <T extends Number> ReadOnlyLongProperty readOnlyLongProperty(final ReadOnlyProperty<T> property) {
        if (property == null) {
            throw new NullPointerException("Property cannot be null");
        }

        return property instanceof ReadOnlyLongProperty ? (ReadOnlyLongProperty) property:
           new ReadOnlyLongPropertyBase() {
            private boolean valid = true;
            private final InvalidationListener listener = observable -> {
                if (valid) {
                    valid = false;
                    fireValueChangedEvent();
                }
            };

            {
                property.addListener(new WeakInvalidationListener(listener));
            }

            @Override
            public long get() {
                valid = true;
                final T value = property.getValue();
                return value == null ? 0L : value.longValue();
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
     * Creates a {@link javafx.beans.property.ReadOnlyObjectProperty} that holds the value
     * of this {@code ReadOnlyLongProperty}. If the
     * value of this {@code ReadOnlyLongProperty} changes, the value of the
     * {@code ReadOnlyObjectProperty} will be updated automatically.
     *
     * @return the new {@code ReadOnlyObjectProperty}
     * @since JavaFX 8.0
     */
    @Override
    public ReadOnlyObjectProperty<Long> asObject() {
        return new ReadOnlyObjectPropertyBase<>() {

            private boolean valid = true;
            private final InvalidationListener listener = observable -> {
                if (valid) {
                    valid = false;
                    fireValueChangedEvent();
                }
            };

            {
                ReadOnlyLongProperty.this.addListener(new WeakInvalidationListener(listener));
            }

            @Override
            public Object getBean() {
                return null; // Virtual property, does not exist on a bean
            }

            @Override
            public String getName() {
                return ReadOnlyLongProperty.this.getName();
            }

            @Override
            public Long get() {
                valid = true;
                return ReadOnlyLongProperty.this.getValue();
            }
        };
    }

}
