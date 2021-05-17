/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import javafx.beans.value.ObservableValue;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
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

    public BidirectionalBindingTest(Factory<T> factory) {
        this.factory = factory;
    }

    @Before
    public void setUp() {
        op1 = factory.createProperty();
        op2 = factory.createProperty();
        op3 = factory.createProperty();
        op4 = factory.createProperty();
        v = factory.getValues();
        op1.setValue(v[0]);
        op2.setValue(v[1]);
    }

    @Test
    public void testBind() {
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

    @Test
    public void testUnbind() {
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

    @Test
    public void testChaining() {
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

    @Test
    public void testWeakReferencing() {
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

    @Test
    public void testHashCode() {
        final int hc1 = BidirectionalBinding.bind(op1, op2).hashCode();
        final int hc2 = BidirectionalBinding.bind(op2, op1).hashCode();
        assertEquals(hc1, hc2);
    }

    @Test
    public void testEquals() {
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

    @Test
    public void testEqualsWithGCedProperty() {
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

    @Test(expected=NullPointerException.class)
    public void testBind_Null_X() {
        Bindings.bindBidirectional(null, op2);
    }

    @Test(expected=NullPointerException.class)
    public void testBind_X_Null() {
        Bindings.bindBidirectional(op1, null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testBind_X_Self() {
        Bindings.bindBidirectional(op1, op1);
    }

    @Test(expected=NullPointerException.class)
    public void testUnbind_Null_X() {
        Bindings.unbindBidirectional(null, op2);
    }

    @Test(expected=NullPointerException.class)
    public void testUnbind_X_Null() {
        Bindings.unbindBidirectional(op1, null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testUnbind_X_Self() {
        Bindings.unbindBidirectional(op1, op1);
    }

    @Test
    public void testBrokenBind() {
        Bindings.bindBidirectional(op1, op2);
        op1.bind(op3);
        assertEquals(op3.getValue(), op1.getValue());
        assertEquals(op2.getValue(), op1.getValue());

        op2.setValue(v[2]);
        assertEquals(op3.getValue(), op1.getValue());
        assertEquals(op2.getValue(), op1.getValue());
    }

    @Test
    public void testDoubleBrokenBind() {
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

    @Parameterized.Parameters
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
