/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.jfx.incubator.scene.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * There should be a common place for module-agnostic test utilities.
 */
public class TUtil {
    private static final Boolean[] BOOLEAN_VALUES = { null, Boolean.TRUE, Boolean.FALSE };

    private TUtil() {
    }

    /** Sets the uncaught exception handler to forward to the thread group */
    public static void setUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                // needed for junit framework to fail the test
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
    }

    /** Sets the uncaught exception handler to null. */
    public static void removeUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    /**
     * Verifies that the boolean property can be set to {@code null}, {@code Boolean.TRUE}, and {@code Boolean.FALSE}
     * using the property's direct {@code setValue()} method, as well as the provided setter method (when not null).
     * The property value is read back using the property's direct {@code getValue()} method as well as the provided
     * getter (when not null).
     * This method restores the initial value afterward.
     *
     * @param prop the boolean property
     * @param getter the getter (or null)
     * @param setter the setter (or null)
     */
    public static void testBooleanProperty(BooleanProperty prop, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        Boolean initialValue = prop.get();

        // directly using the property
        try {
            for (Boolean val : BOOLEAN_VALUES) {
                prop.setValue(val);
                Boolean v = prop.get();
                if (val == null) {
                    val = Boolean.FALSE;
                }
                assertEquals(val, v);
            }
        } finally {
            prop.setValue(initialValue);
        }

        // using the getter
        if (getter != null) {
            try {
                for (Boolean val : BOOLEAN_VALUES) {
                    prop.setValue(val);
                    Boolean v = getter.get();
                    if (val == null) {
                        val = Boolean.FALSE;
                    }
                    assertEquals(val, v);
                }
            } finally {
                prop.setValue(initialValue);
            }
        }

        // using the setter
        if (setter != null) {
            try {
                // BooleanProperty operates with a primitive
                for (Boolean val : new Boolean[] { Boolean.FALSE, Boolean.TRUE }) {
                    setter.accept(val);
                    Boolean v = prop.get();
                    assertEquals(val, v);
                }
            } finally {
                prop.setValue(initialValue);
            }
        }
    }

    /**
     * Verifies that the specified property can be set to the specified values
     * using the property's direct {@code setValue()} method, as well as the provided setter method (when not null).
     * The property value is read back using the property's direct {@code getValue()} method as well as the provided
     * getter (when not null).
     * This method restores the initial value afterward.
     * @param <T> the value type
     * @param prop the property
     * @param getter the getter (or null)
     * @param setter the setter (or null)
     * @param values the values to be used
     */
    public static <T> void testProperty(Property<T> prop, Supplier<T> getter, Consumer<T> setter, T... values) {
        T initialValue = prop.getValue();

        // directly using the property
        try {
            for (T val : values) {
                prop.setValue(val);
                T v = prop.getValue();
                assertEquals(val, v);
            }
        } finally {
            prop.setValue(initialValue);
        }

        // using the getter
        if (getter != null) {
            try {
                for (T val : values) {
                    prop.setValue(val);
                    T v = getter.get();
                    assertEquals(val, v);
                }
            } finally {
                prop.setValue(initialValue);
            }
        }

        // using the setter
        if (setter != null) {
            try {
                for (T val : values) {
                    setter.accept(val);
                    T v = prop.getValue();
                    assertEquals(val, v);
                }
            } finally {
                prop.setValue(initialValue);
            }
        }
    }

    /**
     * Tests that the specified property is non-nullable by setting a null value and verifyting that
     * a {@code NullPointerException} is thrown, using {@code setValue()} method of the property, and the provided
     * {@code setter} if the latter is not {@code null}.
     *
     * @param <T> the value type
     * @param prop the property
     */
    public static <T> void testNonNullable(Property<T> prop, Consumer<T> setter) {
        T initialValue = prop.getValue();

        try {
            assertThrows(NullPointerException.class, () -> {
                prop.setValue(null);
            });
        } finally {
            prop.setValue(initialValue);
        }

        if (setter != null) {
            try {
                assertThrows(NullPointerException.class, () -> {
                    setter.accept(null);
                });
            } finally {
                prop.setValue(initialValue);
            }
        }
    }

    /**
     * Tests whether the current value of the property equals to the expected value, using both
     * {@code getValue()} method of the property and the provided getter (when the latter is not {@code null}).
     *
     * @param <T> the value type
     * @param prop the property
     * @param getter the value getter, can be null
     * @param expected the expected value
     */
    public static <T> void testDefaultValue(Property<T> prop, Supplier<T> getter, T expected) {
        T v = prop.getValue();
        assertEquals(expected, v);

        if (getter != null) {
            T v2 = getter.get();
            assertEquals(expected, v2);
        }
    }

    /**
     * Tests whether the current value of the property satisfies some criteria supplied by the {@code checker},
     * using both
     * {@code getValue()} method of the property and the provided getter (when the latter is not {@code null}).
     * @param <T>
     * @param prop
     * @param getter
     * @param checker
     */
    public static <T> void checkDefaultValue(Property<T> prop, Supplier<T> getter, Predicate<T> checker) {
        T v = prop.getValue();
        assertTrue(checker.test(v));

        if (getter != null) {
            T v2 = getter.get();
            assertTrue(checker.test(v2));
        }
    }

    /**
     * Verifies that the specified property can be bound and the corresponding getter
     * reflects the source property values.
     *
     * @param <T> the value type
     * @param prop the property to be tested
     * @param getter the getter for the property value, cannot be null
     * @param values the values to test with
     */
    public static <T> void testBinding(Property<T> prop, Supplier<T> getter, T... values) {
        T initialValue = prop.getValue();
        try {
            Property<T> p = new SimpleObjectProperty<T>();
            prop.bind(p);

            // this code does not check for the initial value of the bound property
            for (T value : values) {
                p.setValue(value);
                T v = getter.get();
                assertEquals(value, v);
            }
        } finally {
            prop.unbind();
            prop.setValue(initialValue);
        }
    }

    /**
     * Verifies that the specified boolean property can be bound and the corresponding getter
     * reflects the source property values.  The property is tested with the values
     * {@code null, Boolean.TRUE, Boolean.FALSE}.
     *
     * @param prop the property to be tested
     * @param getter the getter for the property value, cannot be null
     */
    public static void testBinding(BooleanProperty prop, Supplier<Boolean> getter) {
        Boolean initialValue = prop.getValue();
        try {
            SimpleBooleanProperty p = new SimpleBooleanProperty();
            prop.bind(p);

            // this code does not check for the initial value of the bound property
            for (Boolean value : BOOLEAN_VALUES) {
                p.setValue(value);
                Boolean v = getter.get();
                if (value == null && v != null) {
                    value = Boolean.FALSE;
                }
                assertEquals(value, v);
            }
        } finally {
            prop.unbind();
            prop.setValue(initialValue);
        }
    }
}
