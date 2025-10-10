/*
 * Copyright (c) 2015, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.beans.property;

import test.com.sun.javafx.binding.ExpressionHelperUtility;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableNumberValue;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class PropertyBaseTest<T> {

    @FunctionalInterface
    public interface PropertyFactory<T> {
        Property<T> createProperty();
    }

    public static class Factory<T> {
        private final PropertyFactory<T> propertyFactory;
        private final PropertyFactory<T> observableFactory;
        private final T value;

        public Factory(PropertyFactory<T> propertyFactory,
                       PropertyFactory<T> observableFactory, T value) {
            this.propertyFactory = propertyFactory;
            this.observableFactory = observableFactory;
            this.value = value;
        }

        public Property<T> createProperty() {
            return propertyFactory.createProperty();
        }

        public Property<T> createObservable() {
            return observableFactory.createProperty();
        }

        public T getValue() {
            return value;
        }
    }

    private static class NumberPropertyMock extends ObservableValueBase<Number>
            implements ObservableNumberValue, Property<Number> {
        private Number value = 0;

        @Override public int intValue()       { return value.intValue(); }
        @Override public long longValue()     { return value.longValue(); }
        @Override public float floatValue()   { return value.floatValue(); }
        @Override public double doubleValue() { return value.doubleValue(); }
        @Override public Number getValue()    { return value; }
        @Override public void setValue(Number value) {
            this.value = value;
            fireValueChangedEvent();
        }

        @Override public void bind(ObservableValue<? extends Number> observable) {}
        @Override public void unbind() {}
        @Override public boolean isBound() { return false; }
        @Override public void bindBidirectional(Property<Number> other) {}
        @Override public void unbindBidirectional(Property<Number> other) {}

        @Override public Object getBean() { return null; }
        @Override public String getName() { return ""; }
    }

    static Stream<Factory<?>> data() {
        return Stream.of(
                // Primitive bindings
                new Factory<>(SimpleBooleanProperty::new, SimpleBooleanProperty::new, true),
                new Factory<>(SimpleDoubleProperty::new, SimpleDoubleProperty::new, 1.0),
                new Factory<>(SimpleFloatProperty::new, SimpleFloatProperty::new, 1.0f),
                new Factory<>(SimpleIntegerProperty::new, SimpleIntegerProperty::new, 1),
                new Factory<>(SimpleLongProperty::new, SimpleLongProperty::new, 1L),

                // Generic with wrapper
                new Factory<>(SimpleBooleanProperty::new, () -> new SimpleObjectProperty<>(), true),
                new Factory<>(SimpleDoubleProperty::new, () -> new SimpleObjectProperty<>(), 1.0),
                new Factory<>(SimpleDoubleProperty::new, NumberPropertyMock::new, 1.0),
                new Factory<>(SimpleFloatProperty::new, () -> new SimpleObjectProperty<>(), 1.0f),
                new Factory<>(SimpleFloatProperty::new, NumberPropertyMock::new, 1.0f),
                new Factory<>(SimpleIntegerProperty::new, () -> new SimpleObjectProperty<>(), 1),
                new Factory<>(SimpleIntegerProperty::new, NumberPropertyMock::new, 1),
                new Factory<>(SimpleLongProperty::new, () -> new SimpleObjectProperty<>(), 1L),
                new Factory<>(SimpleLongProperty::new, NumberPropertyMock::new, 1L),

                // Generic
                new Factory<>(SimpleObjectProperty::new, () -> new SimpleObjectProperty<>(), new Object()),
                new Factory<>(SimpleStringProperty::new, () -> new SimpleObjectProperty<>(), "1"),
                new Factory<>(SimpleStringProperty::new, SimpleStringProperty::new, "1")
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    void testUnbindAfterInvalidation(Factory<T> factory) {
        Property<T> property = factory.createProperty();
        Property<T> observable = factory.createObservable();
        T value = factory.getValue();

        property.bind(observable);
        assertEquals(1, ExpressionHelperUtility.getInvalidationListeners(observable).size());

        property = null;
        System.gc();

        observable.setValue(value);
        assertEquals(0, ExpressionHelperUtility.getInvalidationListeners(observable).size());
    }

    @ParameterizedTest
    @MethodSource("data")
    void testTrimAfterGC(Factory<T> factory) {
        Property<T> property = factory.createProperty();
        Property<T> observable = factory.createObservable();
        T value = factory.getValue();

        Property<T> p1 = factory.createProperty();
        Property<T> p2 = factory.createProperty();
        p1.bind(observable);
        p2.bind(observable);
        assertEquals(2, ExpressionHelperUtility.getInvalidationListeners(observable).size());

        p1 = null;
        p2 = null;
        System.gc();

        property.bind(observable);
        assertEquals(1, ExpressionHelperUtility.getInvalidationListeners(observable).size());
    }

    @ParameterizedTest
    @MethodSource("data")
    void testUnbindGenericWrapper(Factory<T> factory) {
        Property<T> property = factory.createProperty();
        Property<T> observable = factory.createObservable();

        property.bind(observable);
        assertEquals(1, ExpressionHelperUtility.getInvalidationListeners(observable).size());

        property.unbind();
        assertEquals(0, ExpressionHelperUtility.getInvalidationListeners(observable).size());
    }
}
