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
import javafx.beans.binding.BooleanExpression;

/**
 * Superclass for all readonly properties wrapping a {@code boolean}.
 *
 * @see javafx.beans.value.ObservableBooleanValue
 * @see javafx.beans.binding.BooleanExpression
 * @see ReadOnlyProperty
 *
 * @since JavaFX 2.0
 */
public abstract class ReadOnlyBooleanProperty extends BooleanExpression
        implements ReadOnlyProperty<Boolean> {

    /**
     * The constructor of {@code ReadOnlyBooleanProperty}.
     */
    public ReadOnlyBooleanProperty() {
    }

    /**
     * Returns a string representation of this {@code ReadOnlyBooleanProperty} object.
     * @return a string representation of this {@code ReadOnlyBooleanProperty} object.
     */
    @Override
    public String toString() {
        final Object bean = getBean();
        final String name = getName();
        final StringBuilder result = new StringBuilder(
                "ReadOnlyBooleanProperty [");
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
     * Returns a {@code ReadOnlyBooleanProperty} that wraps a
     * {@link javafx.beans.property.ReadOnlyProperty}. If the
     * {@code ReadOnlyProperty} is already a {@code ReadOnlyBooleanProperty}, it
     * will be returned. Otherwise a new
     * {@code ReadOnlyBooleanProperty} is created that is bound to
     * the {@code ReadOnlyProperty}.
     *
     * Note: null values will be interpreted as "false"
     *
     * @param property
     *            The source {@code ReadOnlyProperty}
     * @return A {@code ReadOnlyBooleanProperty} that wraps the
     *         {@code ReadOnlyProperty} if necessary
     * @throws NullPointerException
     *             if {@code property} is {@code null}
     * @since JavaFX 8.0
     */
    public static ReadOnlyBooleanProperty readOnlyBooleanProperty(final ReadOnlyProperty<Boolean> property) {
        if (property == null) {
            throw new NullPointerException("Property cannot be null");
        }

        return property instanceof ReadOnlyBooleanProperty ? (ReadOnlyBooleanProperty) property
                : new ReadOnlyBooleanPropertyBase() {
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
            public boolean get() {
                valid = true;
                final Boolean value = property.getValue();
                return value == null ? false : value;
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
     * of this {@code ReadOnlyBooleanProperty}. If the
     * value of this {@code ReadOnlyBooleanProperty} changes, the value of the
     * {@code ReadOnlyObjectProperty} will be updated automatically.
     *
     * @return the new {@code ReadOnlyObjectProperty}
     * @since JavaFX 8.0
     */
    @Override
    public ReadOnlyObjectProperty<Boolean> asObject() {
        return new ReadOnlyObjectPropertyBase<Boolean>() {

            private boolean valid = true;
            private final InvalidationListener listener = observable -> {
                if (valid) {
                    valid = false;
                    fireValueChangedEvent();
                }
            };

            {
                ReadOnlyBooleanProperty.this.addListener(new WeakInvalidationListener(listener));
            }

            @Override
            public Object getBean() {
                return null; // Virtual property, does not exist on a bean
            }

            @Override
            public String getName() {
                return ReadOnlyBooleanProperty.this.getName();
            }

            @Override
            public Boolean get() {
                valid = true;
                return ReadOnlyBooleanProperty.this.getValue();
            }
        };
    };

}
