/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
import java.util.List;
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
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class PropertyBaseTest<T> {

    @FunctionalInterface
    public interface PropertyFactory<T> {
        public Property<T> createProperty();
    }

    public static class Factory<T> {

        private PropertyFactory<T> propertyFactory;
        private PropertyFactory<T> observableFactory;
        private T value;

        public Factory(PropertyFactory<T> propertyFactory,
                PropertyFactory<T> observableFactory, T value)
        {
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
            implements ObservableNumberValue, Property<Number>
    {
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

    @Parameterized.Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[][] {
            // primitive binding
            // Property->Listener->Value
            { new Factory(() -> new SimpleBooleanProperty(), () -> new SimpleBooleanProperty(), true) },
            { new Factory(() -> new SimpleDoubleProperty(), () -> new SimpleDoubleProperty(), 1.0) },
            { new Factory(() -> new SimpleFloatProperty(), () -> new SimpleFloatProperty(), 1.0f) },
            { new Factory(() -> new SimpleIntegerProperty(), () -> new SimpleIntegerProperty(), 1) },
            { new Factory(() -> new SimpleLongProperty(), () -> new SimpleLongProperty(), 1L) },
            // generic with wrapper
            // Property->Listener->Binding->BindingHelperObserver->Value
            { new Factory(() -> new SimpleBooleanProperty(), () -> new SimpleObjectProperty<>(), true) },
            { new Factory(() -> new SimpleDoubleProperty(), () -> new SimpleObjectProperty<>(), 1.0) },
            { new Factory(() -> new SimpleDoubleProperty(), () -> new NumberPropertyMock(), 1.0) },
            { new Factory(() -> new SimpleFloatProperty(), () -> new SimpleObjectProperty<>(), 1.0f) },
            { new Factory(() -> new SimpleFloatProperty(), () -> new NumberPropertyMock(), 1.0f) },
            { new Factory(() -> new SimpleIntegerProperty(), () -> new SimpleObjectProperty<>(), 1) },
            { new Factory(() -> new SimpleIntegerProperty(), () -> new NumberPropertyMock(), 1) },
            { new Factory(() -> new SimpleLongProperty(), () -> new SimpleObjectProperty<>(), 1L) },
            { new Factory(() -> new SimpleLongProperty(), () -> new NumberPropertyMock(), 1L) },
            // generic
            // Property->Listener->Value
            { new Factory(() -> new SimpleObjectProperty(), () -> new SimpleObjectProperty<>(), new Object()) },
            { new Factory(() -> new SimpleStringProperty(), () -> new SimpleObjectProperty<>(), "1") },
            // the same as generic
            { new Factory(() -> new SimpleStringProperty(), () -> new SimpleStringProperty(), "1") },
        });
    }

    public PropertyBaseTest(Factory<T> factory) {
        this.factory = factory;
    }

    @Before
    public void setUp() {
        property = factory.createProperty();
        observable = factory.createObservable();
        value = factory.getValue();
    }

    private Factory<T> factory;
    private Property<T> property;
    private Property<T> observable;
    private T value;

    @Test
    public void testUnbindAfterInvalidation() {
        property.bind(observable);
        assertEquals(1, ExpressionHelperUtility.getInvalidationListeners(observable).size());

        property = null;
        System.gc();

        observable.setValue(value);
        assertEquals(0, ExpressionHelperUtility.getInvalidationListeners(observable).size());
    }

    @Test
    public void testTrimAfterGC() {
        Property<T> p1 = factory.createProperty();
        Property<T> p2 = factory.createProperty();
        p1.bind(observable); // creates SingleInvalidation
        p2.bind(observable); // creates Generic with 2 listeners
        assertEquals(2, ExpressionHelperUtility.getInvalidationListeners(observable).size());

        p1 = null;
        p2 = null;
        System.gc();

        property.bind(observable); // calls trim
        assertEquals(1, ExpressionHelperUtility.getInvalidationListeners(observable).size());
    }

    @Test
    public void testUnbindGenericWrapper() {
        property.bind(observable);
        assertEquals(1, ExpressionHelperUtility.getInvalidationListeners(observable).size());

        property.unbind(); // should unbind wrapper from observable
        assertEquals(0, ExpressionHelperUtility.getInvalidationListeners(observable).size());
    }
}
