/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.binding;

import com.sun.javafx.binding.BidirectionalBinding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;
import javafx.beans.value.ObservableValue;

import static org.junit.jupiter.api.Assertions.*;

public class BidirectionalBindingTest<T> {

    @FunctionalInterface
    public static interface PropertyFactory<T> {
        Property<T> createProperty();
    }

    public static class Factory<T> {

        private PropertyFactory<T> propertyFactory;
        private T[] values;
        public Factory(PropertyFactory<T> propertyFactory, T[] values) {
            this.propertyFactory = propertyFactory;
            this.values = values;
        }
        public Property<T> createProperty() {
            return propertyFactory.createProperty();
        }
        public T[] getValues() {
            return values;
        }
    }

    private Factory<T> factory;
    private Property<T> op1;
    private Property<T> op2;
    private Property<T> op3;
    private Property<T> op4;
    private T[] v;

    private void setUp(Factory<T> factory) {
        this.factory = factory;
        op1 = factory.createProperty();
        op2 = factory.createProperty();
        op3 = factory.createProperty();
        op4 = factory.createProperty();
        v = factory.getValues();
        op1.setValue(v[0]);
        op2.setValue(v[1]);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testBind(Factory<T> factory) {
        setUp(factory);
        Bindings.bindBidirectional(op1, op2);
        Bindings.bindBidirectional(op1, op2);
        System.gc(); // making sure we did not not overdo weak references
        assertEquals(v[1], op1.getValue());
        assertEquals(v[1], op2.getValue());

        op1.setValue(v[2]);
        assertEquals(v[2], op1.getValue());
        assertEquals(v[2], op2.getValue());

        op2.setValue(v[3]);
        assertEquals(v[3], op1.getValue());
        assertEquals(v[3], op2.getValue());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testUnbind(Factory<T> factory) {
        setUp(factory);
        // unbind non-existing binding => no-op
        Bindings.unbindBidirectional(op1, op2);

        // unbind properties of different beans
        Bindings.bindBidirectional(op1, op2);
        System.gc(); // making sure we did not not overdo weak references
        assertEquals(v[1], op1.getValue());
        assertEquals(v[1], op2.getValue());

        Bindings.unbindBidirectional(op1, op2);
        System.gc();
        assertEquals(v[1], op1.getValue());
        assertEquals(v[1], op2.getValue());

        op1.setValue(v[2]);
        assertEquals(v[2], op1.getValue());
        assertEquals(v[1], op2.getValue());

        op2.setValue(v[3]);
        assertEquals(v[2], op1.getValue());
        assertEquals(v[3], op2.getValue());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChaining(Factory<T> factory) {
        setUp(factory);
        op3.setValue(v[2]);
        Bindings.bindBidirectional(op1, op2);
        Bindings.bindBidirectional(op2, op3);
        System.gc(); // making sure we did not not overdo weak references
        assertEquals(v[2], op1.getValue());
        assertEquals(v[2], op2.getValue());
        assertEquals(v[2], op3.getValue());

        op1.setValue(v[3]);
        assertEquals(v[3], op1.getValue());
        assertEquals(v[3], op2.getValue());
        assertEquals(v[3], op3.getValue());

        op2.setValue(v[0]);
        assertEquals(v[0], op1.getValue());
        assertEquals(v[0], op2.getValue());
        assertEquals(v[0], op3.getValue());

        op3.setValue(v[1]);
        assertEquals(v[1], op1.getValue());
        assertEquals(v[1], op2.getValue());
        assertEquals(v[1], op3.getValue());

        // now unbind
        Bindings.unbindBidirectional(op1, op2);
        System.gc(); // making sure we did not not overdo weak references
        assertEquals(v[1], op1.getValue());
        assertEquals(v[1], op2.getValue());
        assertEquals(v[1], op3.getValue());

        op1.setValue(v[2]);
        assertEquals(v[2], op1.getValue());
        assertEquals(v[1], op2.getValue());
        assertEquals(v[1], op3.getValue());

        op2.setValue(v[3]);
        assertEquals(v[2], op1.getValue());
        assertEquals(v[3], op2.getValue());
        assertEquals(v[3], op3.getValue());

        op3.setValue(v[0]);
        assertEquals(v[2], op1.getValue());
        assertEquals(v[0], op2.getValue());
        assertEquals(v[0], op3.getValue());
    }

    private int getListenerCount(ObservableValue<T> v) {
        return ExpressionHelperUtility.getInvalidationListeners(v).size();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testWeakReferencing(Factory<T> factory) {
        setUp(factory);
        Bindings.bindBidirectional(op1, op2);

        assertEquals(1, getListenerCount(op1));
        assertEquals(1, getListenerCount(op2));

        op1 = null;
        System.gc();
        op2.setValue(v[2]);
        assertEquals(0, getListenerCount(op2));

        Bindings.bindBidirectional(op2, op3);
        assertEquals(1, getListenerCount(op2));
        assertEquals(1, getListenerCount(op3));

        op3 = null;
        System.gc();
        op2.setValue(v[0]);
        assertEquals(0, getListenerCount(op2));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testHashCode(Factory<T> factory) {
        setUp(factory);
        final int hc1 = BidirectionalBinding.bind(op1, op2).hashCode();
        final int hc2 = BidirectionalBinding.bind(op2, op1).hashCode();
        assertEquals(hc1, hc2);
    }

    @SuppressWarnings("unlikely-arg-type")
    @ParameterizedTest
    @MethodSource("parameters")
    public void testEquals(Factory<T> factory) {
        setUp(factory);
        final BidirectionalBinding golden = BidirectionalBinding.bind(op1, op2);

        assertTrue(golden.equals(golden));
        assertFalse(golden.equals(null));
        assertFalse(golden.equals(op1));
        assertTrue(golden.equals(BidirectionalBinding.bind(op1, op2)));
        assertTrue(golden.equals(BidirectionalBinding.bind(op2, op1)));
        assertFalse(golden.equals(BidirectionalBinding.bind(op1, op3)));
        assertFalse(golden.equals(BidirectionalBinding.bind(op3, op1)));
        assertFalse(golden.equals(BidirectionalBinding.bind(op3, op2)));
        assertFalse(golden.equals(BidirectionalBinding.bind(op2, op3)));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testEqualsWithGCedProperty(Factory<T> factory) {
        setUp(factory);
        final BidirectionalBinding binding1 = BidirectionalBinding.bind(op1, op2);
        final BidirectionalBinding binding2 = BidirectionalBinding.bind(op1, op2);
        final BidirectionalBinding binding3 = BidirectionalBinding.bind(op2, op1);
        final BidirectionalBinding binding4 = BidirectionalBinding.bind(op2, op1);
        op1 = null;
        System.gc();

        assertTrue(binding1.equals(binding1));
        assertFalse(binding1.equals(binding2));
        assertFalse(binding1.equals(binding3));

        assertTrue(binding3.equals(binding3));
        assertFalse(binding3.equals(binding1));
        assertFalse(binding3.equals(binding4));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testBind_Null_X(Factory<T> factory) {
        setUp(factory);
        assertThrows(NullPointerException.class, () -> Bindings.bindBidirectional(null, op2));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testBind_X_Null(Factory<T> factory) {
        setUp(factory);
        assertThrows(NullPointerException.class, () -> Bindings.bindBidirectional(op1, null));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testBind_X_Self(Factory<T> factory) {
        setUp(factory);
        assertThrows(IllegalArgumentException.class, () -> Bindings.bindBidirectional(op1, op1));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testUnbind_Null_X(Factory<T> factory) {
        setUp(factory);
        assertThrows(NullPointerException.class, () -> Bindings.unbindBidirectional(null, op2));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testUnbind_X_Null(Factory<T> factory) {
        setUp(factory);
        assertThrows(NullPointerException.class, () -> Bindings.unbindBidirectional(op1, null));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testUnbind_X_Self(Factory<T> factory) {
        setUp(factory);
        assertThrows(IllegalArgumentException.class, () -> Bindings.unbindBidirectional(op1, op1));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testBrokenBind(Factory<T> factory) {
        setUp(factory);
        Bindings.bindBidirectional(op1, op2);
        op1.bind(op3);
        assertEquals(op3.getValue(), op1.getValue());
        assertEquals(op2.getValue(), op1.getValue());

        op2.setValue(v[2]);
        assertEquals(op3.getValue(), op1.getValue());
        assertEquals(op2.getValue(), op1.getValue());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testDoubleBrokenBind(Factory<T> factory) {
        setUp(factory);
        Bindings.bindBidirectional(op1, op2);
        op1.bind(op3);
        op4.setValue(v[0]);

        op2.bind(op4);
        assertEquals(op4.getValue(), op2.getValue());
        assertEquals(op3.getValue(), op1.getValue());
        // Test that bidirectional binding was unbound in this case
        op3.setValue(v[0]);
        op4.setValue(v[1]);
        assertEquals(op4.getValue(), op2.getValue());
        assertEquals(op3.getValue(), op1.getValue());
        assertEquals(v[0], op1.getValue());
        assertEquals(v[1], op2.getValue());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSetValueWithoutIntermediateValidation(Factory<T> factory) {
        setUp(factory);
        BidirectionalBinding.bind(op1, op2);
        op1.setValue(v[0]);
        op2.setValue(v[1]);
        assertEquals(v[1], op1.getValue());
        assertEquals(v[1], op2.getValue());
    }

    public static Collection<Object[]> parameters() {
        final Boolean[] booleanData = new Boolean[] {true, false, true, false};
        final Double[] doubleData = new Double[] {2348.2345, -92.214, -214.0214, -908.214};
        final Float[] floatData = new Float[] {-3592.9f, 234872.8347f, 3897.274f, 3958.938745f};
        final Integer[] integerData = new Integer[] {248, -9384, -234, -34};
        final Long[] longData = new Long[] {9823984L, 2908934L, -234234L, 9089234L};
        final Object[] objectData = new Object[] {new Object(), new Object(), new Object(), new Object()};
        final String[] stringData = new String[] {"A", "B", "C", "D"};

        return Arrays.asList(new Object[][] {
            { new Factory(() -> new SimpleBooleanProperty(), booleanData) },
            { new Factory(() -> new SimpleDoubleProperty(), doubleData) },
            { new Factory(() -> new SimpleFloatProperty(), floatData) },
            { new Factory(() -> new SimpleIntegerProperty(), integerData) },
            { new Factory(() -> new SimpleLongProperty(), longData) },
            { new Factory(() -> new SimpleObjectProperty<>(), objectData) },
            { new Factory(() -> new SimpleStringProperty(), stringData) },
            { new Factory(() -> new ReadOnlyBooleanWrapper(), booleanData) },
            { new Factory(() -> new ReadOnlyDoubleWrapper(), doubleData) },
            { new Factory(() -> new ReadOnlyFloatWrapper(), floatData) },
            { new Factory(() -> new ReadOnlyIntegerWrapper(), integerData) },
            { new Factory(() -> new ReadOnlyLongWrapper(), longData) },
            { new Factory(() -> new ReadOnlyObjectWrapper<>(), objectData) },
            { new Factory(() -> new ReadOnlyStringWrapper(), stringData) },
        });
    }
}
