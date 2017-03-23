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

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.FloatExpression;

/**
 * Superclass for all readonly properties wrapping a {@code float}.
 *
 * @see javafx.beans.value.ObservableFloatValue
 * @see javafx.beans.binding.FloatExpression
 * @see ReadOnlyProperty
 *
 * @since JavaFX 2.0
 */
public abstract class ReadOnlyFloatProperty extends FloatExpression implements
        ReadOnlyProperty<Number> {

    /**
     * The constructor of {@code ReadOnlyFloatProperty}.
     */
    public ReadOnlyFloatProperty() {
    }

    /**
     * Returns a string representation of this {@code ReadOnlyFloatProperty} object.
     * @return a string representation of this {@code ReadOnlyFloatProperty} object.
     */
    @Override
    public String toString() {
        final Object bean = getBean();
        final String name = getName();
        final StringBuilder result = new StringBuilder(
                "ReadOnlyFloatProperty [");
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
     * Returns a {@code ReadOnlyFloatProperty} that wraps a
     * {@link javafx.beans.property.ReadOnlyProperty}. If the
     * {@code ReadOnlyProperty} is already a {@code ReadOnlyFloatProperty}, it
     * will be returned. Otherwise a new
     * {@code ReadOnlyFloatProperty} is created that is bound to
     * the {@code ReadOnlyProperty}.
     *
     * Note: null values will be interpreted as 0f
     *
     * @param <T> The type of Number to be wrapped
     * @param property
     *            The source {@code ReadOnlyProperty}
     * @return A {@code ReadOnlyFloatProperty} that wraps the
     *         {@code ReadOnlyProperty} if necessary
     * @throws NullPointerException
     *             if {@code property} is {@code null}
     * @since JavaFX 8.0
     */
    public static <T extends Number> ReadOnlyFloatProperty readOnlyFloatProperty(final ReadOnlyProperty<T> property) {
        if (property == null) {
            throw new NullPointerException("Property cannot be null");
        }

        return property instanceof ReadOnlyFloatProperty ? (ReadOnlyFloatProperty) property:
           new ReadOnlyFloatPropertyBase() {
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
            public float get() {
                valid = true;
                final T value = property.getValue();
                return value == null ? 0f : value.floatValue();
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
     * of this {@code ReadOnlyFloatProperty}. If the
     * value of this {@code ReadOnlyFloatProperty} changes, the value of the
     * {@code ReadOnlyObjectProperty} will be updated automatically.
     *
     * @return the new {@code ReadOnlyObjectProperty}
     * @since JavaFX 8.0
     */
    @Override
    public ReadOnlyObjectProperty<Float> asObject() {
        return new ReadOnlyObjectPropertyBase<Float>() {

            private boolean valid = true;
            private final InvalidationListener listener = observable -> {
                if (valid) {
                    valid = false;
                    fireValueChangedEvent();
                }
            };

            {
                ReadOnlyFloatProperty.this.addListener(new WeakInvalidationListener(listener));
            }

            @Override
            public Object getBean() {
                return null; // Virtual property, does not exist on a bean
            }

            @Override
            public String getName() {
                return ReadOnlyFloatProperty.this.getName();
            }

            @Override
            public Float get() {
                valid = true;
                return ReadOnlyFloatProperty.this.getValue();
            }
        };
    };

}
