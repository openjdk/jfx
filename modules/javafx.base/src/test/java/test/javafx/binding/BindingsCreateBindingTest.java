/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.binding;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import javafx.beans.Observable;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import test.com.sun.javafx.binding.ErrorLoggingUtiltity;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 */
public class BindingsCreateBindingTest<T> {

    private static final float EPSILON_FLOAT = 1e-5f;
    private static final double EPSILON_DOUBLE = 1e-10;

    private static interface Functions<S> {
        public Binding<S> create(Callable<S> func, Observable... dependencies);
        public void check(S value0, S value1);
    }

    private  Property<T> p0;
    private  Property<T> p1;
    private  Functions<T> f;
    private  T value0;
    private  T value1;
    private  T defaultValue;

    @BeforeAll
    public static void setUpClass() {
        ErrorLoggingUtiltity.reset();
    }

    private void setup(Property<T> p0, Property<T> p1, Functions<T> f, T value0, T value1, T defaultValue) {
        this.p0 = p0;
        this.p1 = p1;
        this.f = f;
        this.value0 = value0;
        this.value1 = value1;
        this.defaultValue = defaultValue;
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testNoDependencies(Property<T> p0, Property<T> p1, Functions<T> f, T value0, T value1, T defaultValue) {
        // func returns value0, no dependencies specified
        setup(p0, p1, f, value0, value1, defaultValue);
        final Callable<T> func0 = () -> value0;
        final Binding<T> binding0 = f.create(func0);

        f.check(value0, binding0.getValue());
        assertTrue(binding0.getDependencies().isEmpty());
        binding0.dispose();

        // func returns value1, dependencies set to null
        final Callable<T> func1 = () -> value1;
        final Binding<T> binding1 = f.create(func1, (Observable[])null);

        f.check(value1, binding1.getValue());
        assertTrue(binding1.getDependencies().isEmpty());
        binding1.dispose();

        // func throws exception, dependencies set to empty array
        final Callable<T> func2 = () -> {
            throw new Exception();
        };
        final Binding<T> binding2 = f.create(func2, new Observable [0]);

        f.check(defaultValue, binding2.getValue());
        ErrorLoggingUtiltity.checkWarning(Exception.class);
        assertTrue(binding2.getDependencies().isEmpty());
        binding2.dispose();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testOneDependency(Property<T> p0, Property<T> p1, Functions<T> f, T value0, T value1, T defaultValue) {
        setup(p0, p1, f, value0, value1, defaultValue);
        final Callable<T> func = () -> p0.getValue();
        final Binding<T> binding = f.create(func, p0);

        f.check(p0.getValue(), binding.getValue());
        assertEquals(binding.getDependencies(), Arrays.asList(p0));
        p0.setValue(value1);
        f.check(p0.getValue(), binding.getValue());
        binding.dispose();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testCreateBoolean_TwoDependencies(Property<T> p0, Property<T> p1, Functions<T> f, T value0, T value1, T defaultValue) {
        setup(p0, p1, f, value0, value1, defaultValue);
        final Callable<T> func = () -> p0.getValue();
        final Binding<T> binding = f.create(func, p0, p1);

        f.check(p0.getValue(), binding.getValue());
        assertTrue(binding.getDependencies().equals(Arrays.asList(p0, p1)) || binding.getDependencies().equals((Arrays.asList(p1, p0))));
        p0.setValue(value1);
        f.check(p0.getValue(), binding.getValue());
        binding.dispose();
    }

    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
                {
                    new SimpleBooleanProperty(), new SimpleBooleanProperty(),
                    new Functions<Boolean>() {
                        @Override
                        public Binding<Boolean> create(Callable<Boolean> func, Observable... dependencies) {
                            return Bindings.createBooleanBinding(func, dependencies);
                        }

                        @Override
                        public void check(Boolean value0, Boolean value1) {
                            assertEquals(value0, value1);
                        }
                    },
                    true, false, false
                },
                {
                    new SimpleDoubleProperty(), new SimpleDoubleProperty(),
                    new Functions<Number>() {
                        @Override
                        public Binding<Number> create(Callable func, Observable... dependencies) {
                            return Bindings.createDoubleBinding(func, dependencies);
                        }

                        @Override
                        public void check(Number value0, Number value1) {
                            assertEquals(value0.doubleValue(), value1.doubleValue(), EPSILON_DOUBLE);
                        }
                    },
                    Math.PI, -Math.E, 0.0
                },
                {
                    new SimpleFloatProperty(), new SimpleFloatProperty(),
                    new Functions<Number>() {
                        @Override
                        public Binding<Number> create(Callable func, Observable... dependencies) {
                            return Bindings.createFloatBinding(func, dependencies);
                        }

                        @Override
                        public void check(Number value0, Number value1) {
                            assertEquals(value0.floatValue(), value1.floatValue(), EPSILON_FLOAT);
                        }
                    },
                    (float)Math.PI, (float)-Math.E, 0.0f
                },
                {
                    new SimpleIntegerProperty(), new SimpleIntegerProperty(),
                    new Functions<Number>() {
                        @Override
                        public Binding<Number> create(Callable func, Observable... dependencies) {
                            return Bindings.createIntegerBinding(func, dependencies);
                        }

                        @Override
                        public void check(Number value0, Number value1) {
                            assertEquals(value0.intValue(), value1.intValue());
                        }
                    },
                    Integer.MAX_VALUE, Integer.MIN_VALUE, 0
                },
                {
                    new SimpleLongProperty(), new SimpleLongProperty(),
                    new Functions<Number>() {
                        @Override
                        public Binding<Number> create(Callable func, Observable... dependencies) {
                            return Bindings.createLongBinding(func, dependencies);
                        }

                        @Override
                        public void check(Number value0, Number value1) {
                            assertEquals(value0.longValue(), value1.longValue());
                        }
                    },
                    Long.MAX_VALUE, Long.MIN_VALUE, 0L
                },
                {
                    new SimpleObjectProperty(), new SimpleObjectProperty(),
                    new Functions<Object>() {
                        @Override
                        public Binding<Object> create(Callable<Object> func, Observable... dependencies) {
                            return Bindings.createObjectBinding(func, dependencies);
                        }

                        @Override
                        public void check(Object value0, Object value1) {
                            assertEquals(value0, value1);
                        }
                    },
                    new Object(), new Object(), null
                },
                {
                    new SimpleStringProperty(), new SimpleStringProperty(),
                    new Functions<String>() {
                        @Override
                        public Binding<String> create(Callable<String> func, Observable... dependencies) {
                            return Bindings.createStringBinding(func, dependencies);
                        }

                        @Override
                        public void check(String value0, String value1) {
                            assertEquals(value0, value1);
                        }
                    },
                    "Hello World", "Goodbye World", ""
                },
        });
    }
}
