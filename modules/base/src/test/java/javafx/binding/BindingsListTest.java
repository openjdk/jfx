/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.binding;

import javafx.beans.binding.*;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.sun.javafx.binding.ErrorLoggingUtiltity;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 */
public class BindingsListTest {

    private static final double EPSILON_DOUBLE = 1e-12;
    private static final float EPSILON_FLOAT = 1e-5f;

    private static final Object data1 = new Object();
    private static final Object data2 = new Object();

    private static final ErrorLoggingUtiltity log = new ErrorLoggingUtiltity();

    private ListProperty<Object> property;
    private ObservableList<Object> list1;
    private ObservableList<Object> list2;
    private IntegerProperty index;

    @BeforeClass
    public static void setUpClass() {
        log.start();
    }

    @AfterClass
    public static void tearDownClass() {
        log.stop();
    }

    @Before
    public void setUp() {
        property = new SimpleListProperty<Object>();
        list1 = FXCollections.<Object>observableArrayList(data1, data2);
        list2 = FXCollections.<Object>observableArrayList();
        index = new SimpleIntegerProperty();
    }

    @Test
    public void testSize() {
        final IntegerBinding size = Bindings.size(property);
        DependencyUtils.checkDependencies(size.getDependencies(), property);

        assertEquals(0, size.get());
        property.set(list1);
        assertEquals(2, size.get());
        list1.remove(data2);
        assertEquals(1, size.get());
        property.set(list2);
        assertEquals(0, size.get());
        property.addAll(data2, data2);
        assertEquals(2, size.get());
        property.set(null);
        assertEquals(0, size.get());
    }

    @Test(expected = NullPointerException.class)
    public void testSize_Null() {
        Bindings.size((ObservableList<Object>) null);
    }

    @Test
    public void testIsEmpty() {
        final BooleanBinding empty = Bindings.isEmpty(property);
        DependencyUtils.checkDependencies(empty.getDependencies(), property);

        assertTrue(empty.get());
        property.set(list1);
        assertFalse(empty.get());
        list1.remove(data2);
        assertFalse(empty.get());
        property.set(list2);
        assertTrue(empty.get());
        property.addAll(data2, data2);
        assertFalse(empty.get());
        property.set(null);
        assertTrue(empty.get());
    }

    @Test(expected = NullPointerException.class)
    public void testIsEmpty_Null() {
        Bindings.isEmpty((ObservableList<Object>) null);
    }

    @Test
    public void testIsNotEmpty() {
        final BooleanBinding notEmpty = Bindings.isNotEmpty(property);
        DependencyUtils.checkDependencies(notEmpty.getDependencies(), property);

        assertFalse(notEmpty.get());
        property.set(list1);
        assertTrue(notEmpty.get());
        list1.remove(data2);
        assertTrue(notEmpty.get());
        property.set(list2);
        assertFalse(notEmpty.get());
        property.addAll(data2, data2);
        assertTrue(notEmpty.get());
        property.set(null);
        assertFalse(notEmpty.get());
    }

    @Test(expected = NullPointerException.class)
    public void testIsNotEmpty_Null() {
        Bindings.isNotEmpty((ObservableList<Object>) null);
    }

    @Ignore("RT-27128")
    @Test
    public void testValueAt_Constant() {
        synchronized (log) {
            log.reset();

            final ObjectBinding<Object> binding0 = Bindings.valueAt(property, 0);
            final ObjectBinding<Object> binding1 = Bindings.valueAt(property, 1);
            final ObjectBinding<Object> binding2 = Bindings.valueAt(property, 2);
            DependencyUtils.checkDependencies(binding0.getDependencies(), property);
            DependencyUtils.checkDependencies(binding1.getDependencies(), property);
            DependencyUtils.checkDependencies(binding2.getDependencies(), property);
            assertNull(binding0.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertNull(binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertNull(binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            property.set(list1);
            assertEquals(data1, binding0.get());
            assertEquals(data2, binding1.get());
            assertNull(binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            property.remove(data2);
            assertEquals(data1, binding0.get());
            assertNull(binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertNull(binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            property.set(list2);
            assertNull(binding0.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertNull(binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertNull(binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            property.addAll(data2, data2);
            assertEquals(data2, binding0.get());
            assertEquals(data2, binding1.get());
            assertNull(binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            property.set(null);
            assertNull(binding0.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertNull(binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertNull(binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
        }
    }

    @Test(expected = NullPointerException.class)
    public void testValueAt_Constant_Null() {
        Bindings.valueAt(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueAt_Constant_NegativeIndex() {
        Bindings.valueAt(property, -1);
    }

    @Ignore("RT-27128")
    @Test
    public void testValueAt_Variable() {
        synchronized (log) {
            log.reset();

            final ObjectBinding<Object> binding = Bindings.valueAt(property, index);
            DependencyUtils.checkDependencies(binding.getDependencies(), property, index);

            index.set(-1);
            assertNull(binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertNull(binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            property.set(list1);
            index.set(-1);
            assertNull(binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(data1, binding.get());
            index.set(1);
            assertEquals(data2, binding.get());
            index.set(2);
            assertNull(binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            property.remove(data2);
            index.set(-1);
            assertNull(binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(data1, binding.get());
            index.set(1);
            assertNull(binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            property.set(list2);
            index.set(-1);
            assertNull(binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertNull(binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            property.addAll(data2, data2);
            index.set(-1);
            assertNull(binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(data2, binding.get());
            index.set(1);
            assertEquals(data2, binding.get());
            index.set(2);
            assertNull(binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            property.set(null);
            index.set(-1);
            assertNull(binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertNull(binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
        }
    }

    @Test(expected = NullPointerException.class)
    public void testValueAt_Variable_Null() {
        Bindings.valueAt((ObservableList<Object>)null, index);
    }

    @Test(expected = NullPointerException.class)
    public void testValueAt_Variable_NullIndex() {
        Bindings.valueAt(property, null);
    }

    @Ignore("RT-27128")
    @Test
    public void testBooleanValueAt_Constant() {
        synchronized (log) {
            log.reset();

            final boolean defaultData = false;
            final boolean localData1 = false;
            final boolean localData2 = true;
            final ListProperty<Boolean> localProperty = new SimpleListProperty<Boolean>();
            final ObservableList<Boolean> localList1 = FXCollections.observableArrayList(localData1, localData2);
            final ObservableList<Boolean> localList2 = FXCollections.observableArrayList();

            final BooleanBinding binding0 = Bindings.booleanValueAt(localProperty, 0);
            final BooleanBinding binding1 = Bindings.booleanValueAt(localProperty, 1);
            final BooleanBinding binding2 = Bindings.booleanValueAt(localProperty, 2);
            DependencyUtils.checkDependencies(binding0.getDependencies(), localProperty);
            DependencyUtils.checkDependencies(binding1.getDependencies(), localProperty);
            DependencyUtils.checkDependencies(binding2.getDependencies(), localProperty);
            assertEquals(defaultData, binding0.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList1);
            assertEquals(localData1, binding0.get());
            assertEquals(localData2, binding1.get());
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.remove(1);
            assertEquals(localData1, binding0.get());
            assertEquals(defaultData, binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList2);
            assertEquals(defaultData, binding0.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.addAll(localData2, localData2);
            assertEquals(localData2, binding0.get());
            assertEquals(localData2, binding1.get());
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(null);
            assertEquals(defaultData, binding0.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
        }
    }

    @Test(expected = NullPointerException.class)
    public void testBooleanValueAt_Constant_Null() {
        Bindings.booleanValueAt(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBooleanValueAt_Constant_NegativeIndex() {
        final ListProperty<Boolean> localProperty = new SimpleListProperty<Boolean>();
        Bindings.booleanValueAt(localProperty, -1);
    }

    @Ignore("RT-27128")
    @Test
    public void testBooleanValueAt_Variable() {
        synchronized (log) {
            log.reset();

            final boolean defaultData = false;
            final boolean localData1 = false;
            final boolean localData2 = true;
            final ListProperty<Boolean> localProperty = new SimpleListProperty<Boolean>();
            final ObservableList<Boolean> localList1 = FXCollections.observableArrayList(localData1, localData2);
            final ObservableList<Boolean> localList2 = FXCollections.observableArrayList();

            final BooleanBinding binding = Bindings.booleanValueAt(localProperty, index);
            DependencyUtils.checkDependencies(binding.getDependencies(), localProperty, index);

            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList1);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(localData1, binding.get());
            index.set(1);
            assertEquals(localData2, binding.get());
            index.set(2);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.remove(1);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(localData1, binding.get());
            index.set(1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(0, null);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get());
            log.check(0, "INFO", "NullPointerException");
            index.set(1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList2);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.addAll(localData2, localData2);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(localData2, binding.get());
            index.set(1);
            assertEquals(localData2, binding.get());
            index.set(2);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(null);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
        }
    }

    @Test(expected = NullPointerException.class)
    public void testBooleanValueAt_Variable_Null() {
        Bindings.booleanValueAt((ObservableList<Boolean>)null, index);
    }

    @Test(expected = NullPointerException.class)
    public void testBooleanValueAt_Variable_NullIndex() {
        final ListProperty<Boolean> localProperty = new SimpleListProperty<Boolean>();
        Bindings.booleanValueAt(localProperty, null);
    }

    @Ignore("RT-27128")
    @Test
    public void testDoubleValueAt_Constant() {
        synchronized (log) {
            log.reset();

            final double defaultData = 0.0;
            final double localData1 = Math.PI;
            final double localData2 = -Math.E;
            final ListProperty<Double> localProperty = new SimpleListProperty<Double>();
            final ObservableList<Double> localList1 = FXCollections.observableArrayList(localData1, localData2);
            final ObservableList<Double> localList2 = FXCollections.observableArrayList();

            final DoubleBinding binding0 = Bindings.doubleValueAt(localProperty, 0);
            final DoubleBinding binding1 = Bindings.doubleValueAt(localProperty, 1);
            final DoubleBinding binding2 = Bindings.doubleValueAt(localProperty, 2);
            DependencyUtils.checkDependencies(binding0.getDependencies(), localProperty);
            DependencyUtils.checkDependencies(binding1.getDependencies(), localProperty);
            DependencyUtils.checkDependencies(binding2.getDependencies(), localProperty);
            assertEquals(defaultData, binding0.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding1.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList1);
            assertEquals(localData1, binding0.get(), EPSILON_DOUBLE);
            assertEquals(localData2, binding1.get(), EPSILON_DOUBLE);
            assertEquals(defaultData, binding2.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.remove(1);
            assertEquals(localData1, binding0.get(), EPSILON_DOUBLE);
            assertEquals(defaultData, binding1.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList2);
            assertEquals(defaultData, binding0.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding1.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.addAll(localData2, localData2);
            assertEquals(localData2, binding0.get(), EPSILON_DOUBLE);
            assertEquals(localData2, binding1.get(), EPSILON_DOUBLE);
            assertEquals(defaultData, binding2.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(null);
            assertEquals(defaultData, binding0.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding1.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
        }
    }

    @Test(expected = NullPointerException.class)
    public void testDoubleValueAt_Constant_Null() {
        Bindings.doubleValueAt(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDoubleValueAt_Constant_NegativeIndex() {
        final ListProperty<Double> localProperty = new SimpleListProperty<Double>();
        Bindings.doubleValueAt(localProperty, -1);
    }

    @Ignore("RT-27128")
    @Test
    public void testDoubleValueAt_Variable() {
        synchronized (log) {
            log.reset();

            final double defaultData = 0.0;
            final double localData1 = -Math.PI;
            final double localData2 = Math.E;
            final ListProperty<Double> localProperty = new SimpleListProperty<Double>();
            final ObservableList<Double> localList1 = FXCollections.observableArrayList(localData1, localData2);
            final ObservableList<Double> localList2 = FXCollections.observableArrayList();

            final DoubleBinding binding = Bindings.doubleValueAt(localProperty, index);
            DependencyUtils.checkDependencies(binding.getDependencies(), localProperty, index);

            index.set(-1);
            assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList1);
            index.set(-1);
            assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(localData1, binding.get(), EPSILON_DOUBLE);
            index.set(1);
            assertEquals(localData2, binding.get(), EPSILON_DOUBLE);
            index.set(2);
            assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.remove(1);
            index.set(-1);
            assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(localData1, binding.get(), EPSILON_DOUBLE);
            index.set(1);
            assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(0, null);
            index.set(-1);
            assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
            log.check(0, "INFO", "NullPointerException");
            index.set(1);
            assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList2);
            index.set(-1);
            assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.addAll(localData2, localData2);
            index.set(-1);
            assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(localData2, binding.get(), EPSILON_DOUBLE);
            index.set(1);
            assertEquals(localData2, binding.get(), EPSILON_DOUBLE);
            index.set(2);
            assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(null);
            index.set(-1);
            assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
        }
    }

    @Test(expected = NullPointerException.class)
    public void testDoubleValueAt_Variable_Null() {
        Bindings.doubleValueAt((ObservableList<Double>)null, index);
    }

    @Test(expected = NullPointerException.class)
    public void testDoubleValueAt_Variable_NullIndex() {
        final ListProperty<Double> localProperty = new SimpleListProperty<Double>();
        Bindings.doubleValueAt(localProperty, null);
    }

    @Ignore("RT-27128")
    @Test
    public void testFloatValueAt_Constant() {
        synchronized (log) {
            log.reset();

            final float defaultData = 0.0f;
            final float localData1 = (float)Math.PI;
            final float localData2 = (float)-Math.E;
            final ListProperty<Float> localProperty = new SimpleListProperty<Float>();
            final ObservableList<Float> localList1 = FXCollections.observableArrayList(localData1, localData2);
            final ObservableList<Float> localList2 = FXCollections.observableArrayList();

            final FloatBinding binding0 = Bindings.floatValueAt(localProperty, 0);
            final FloatBinding binding1 = Bindings.floatValueAt(localProperty, 1);
            final FloatBinding binding2 = Bindings.floatValueAt(localProperty, 2);
            DependencyUtils.checkDependencies(binding0.getDependencies(), localProperty);
            DependencyUtils.checkDependencies(binding1.getDependencies(), localProperty);
            DependencyUtils.checkDependencies(binding2.getDependencies(), localProperty);
            assertEquals(defaultData, binding0.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding1.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList1);
            assertEquals(localData1, binding0.get(), EPSILON_FLOAT);
            assertEquals(localData2, binding1.get(), EPSILON_FLOAT);
            assertEquals(defaultData, binding2.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.remove(1);
            assertEquals(localData1, binding0.get(), EPSILON_FLOAT);
            assertEquals(defaultData, binding1.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList2);
            assertEquals(defaultData, binding0.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding1.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.addAll(localData2, localData2);
            assertEquals(localData2, binding0.get(), EPSILON_FLOAT);
            assertEquals(localData2, binding1.get(), EPSILON_FLOAT);
            assertEquals(defaultData, binding2.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(null);
            assertEquals(defaultData, binding0.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding1.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
        }
    }

    @Test(expected = NullPointerException.class)
    public void testFloatValueAt_Constant_Null() {
        Bindings.floatValueAt((ObservableList) null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFloatValueAt_Constant_NegativeIndex() {
        final ListProperty<Float> localProperty = new SimpleListProperty<Float>();
        Bindings.floatValueAt(localProperty, -1);
    }

    @Ignore("RT-27128")
    @Test
    public void testFloatValueAt_Variable() {
        synchronized (log) {
            log.reset();

            final float defaultData = 0.0f;
            final float localData1 = (float)-Math.PI;
            final float localData2 = (float)Math.E;
            final ListProperty<Float> localProperty = new SimpleListProperty<Float>();
            final ObservableList<Float> localList1 = FXCollections.observableArrayList(localData1, localData2);
            final ObservableList<Float> localList2 = FXCollections.observableArrayList();

            final FloatBinding binding = Bindings.floatValueAt(localProperty, index);
            DependencyUtils.checkDependencies(binding.getDependencies(), localProperty, index);

            index.set(-1);
            assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList1);
            index.set(-1);
            assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(localData1, binding.get(), EPSILON_FLOAT);
            index.set(1);
            assertEquals(localData2, binding.get(), EPSILON_FLOAT);
            index.set(2);
            assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.remove(1);
            index.set(-1);
            assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(localData1, binding.get(), EPSILON_FLOAT);
            index.set(1);
            assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(0, null);
            index.set(-1);
            assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
            log.check(0, "INFO", "NullPointerException");
            index.set(1);
            assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList2);
            index.set(-1);
            assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.addAll(localData2, localData2);
            index.set(-1);
            assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(localData2, binding.get(), EPSILON_FLOAT);
            index.set(1);
            assertEquals(localData2, binding.get(), EPSILON_FLOAT);
            index.set(2);
            assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(null);
            index.set(-1);
            assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
        }
    }

    @Test(expected = NullPointerException.class)
    public void testFloatValueAt_Variable_Null() {
        Bindings.floatValueAt((ObservableList<Float>)null, index);
    }

    @Test(expected = NullPointerException.class)
    public void testFloatValueAt_Variable_NullIndex() {
        final ListProperty<Float> localProperty = new SimpleListProperty<Float>();
        Bindings.floatValueAt(localProperty, null);
    }

    @Ignore("RT-27128")
    @Test
    public void testIntegerValueAt_Constant() {
        synchronized (log) {
            log.reset();

            final int defaultData = 0;
            final int localData1 = 42;
            final int localData2 = -7;
            final ListProperty<Integer> localProperty = new SimpleListProperty<Integer>();
            final ObservableList<Integer> localList1 = FXCollections.observableArrayList(localData1, localData2);
            final ObservableList<Integer> localList2 = FXCollections.observableArrayList();

            final IntegerBinding binding0 = Bindings.integerValueAt(localProperty, 0);
            final IntegerBinding binding1 = Bindings.integerValueAt(localProperty, 1);
            final IntegerBinding binding2 = Bindings.integerValueAt(localProperty, 2);
            DependencyUtils.checkDependencies(binding0.getDependencies(), localProperty);
            DependencyUtils.checkDependencies(binding1.getDependencies(), localProperty);
            DependencyUtils.checkDependencies(binding2.getDependencies(), localProperty);
            assertEquals(defaultData, binding0.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList1);
            assertEquals(localData1, binding0.get());
            assertEquals(localData2, binding1.get());
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.remove(1);
            assertEquals(localData1, binding0.get());
            assertEquals(defaultData, binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList2);
            assertEquals(defaultData, binding0.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.addAll(localData2, localData2);
            assertEquals(localData2, binding0.get());
            assertEquals(localData2, binding1.get());
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(null);
            assertEquals(defaultData, binding0.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
        }
    }

    @Test(expected = NullPointerException.class)
    public void testIntegerValueAt_Constant_Null() {
        Bindings.integerValueAt((ObservableList) null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIntegerValueAt_Constant_NegativeIndex() {
        final ListProperty<Integer> localProperty = new SimpleListProperty<Integer>();
        Bindings.integerValueAt(localProperty, -1);
    }

    @Ignore("RT-27128")
    @Test
    public void testIntegerValueAt_Variable() {
        synchronized (log) {
            log.reset();

            final int defaultData = 0;
            final int localData1 = 42;
            final int localData2 = -7;
            final ListProperty<Integer> localProperty = new SimpleListProperty<Integer>();
            final ObservableList<Integer> localList1 = FXCollections.observableArrayList(localData1, localData2);
            final ObservableList<Integer> localList2 = FXCollections.observableArrayList();

            final IntegerBinding binding = Bindings.integerValueAt(localProperty, index);
            DependencyUtils.checkDependencies(binding.getDependencies(), localProperty, index);

            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList1);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(localData1, binding.get());
            index.set(1);
            assertEquals(localData2, binding.get());
            index.set(2);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.remove(1);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(localData1, binding.get());
            index.set(1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(0, null);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get());
            log.check(0, "INFO", "NullPointerException");
            index.set(1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList2);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.addAll(localData2, localData2);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(localData2, binding.get());
            index.set(1);
            assertEquals(localData2, binding.get());
            index.set(2);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(null);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
        }
    }

    @Test(expected = NullPointerException.class)
    public void testIntegerValueAt_Variable_Null() {
        Bindings.integerValueAt((ObservableList<Integer>)null, index);
    }

    @Test(expected = NullPointerException.class)
    public void testIntegerValueAt_Variable_NullIndex() {
        final ListProperty<Integer> localProperty = new SimpleListProperty<Integer>();
        Bindings.integerValueAt(localProperty, null);
    }

    @Ignore("RT-27128")
    @Test
    public void testLongValueAt_Constant() {
        synchronized (log) {
            log.reset();

            final long defaultData = 0L;
            final long localData1 = 1234567890987654321L;
            final long localData2 = -987654321987654321L;
            final ListProperty<Long> localProperty = new SimpleListProperty<Long>();
            final ObservableList<Long> localList1 = FXCollections.observableArrayList(localData1, localData2);
            final ObservableList<Long> localList2 = FXCollections.observableArrayList();

            final LongBinding binding0 = Bindings.longValueAt(localProperty, 0);
            final LongBinding binding1 = Bindings.longValueAt(localProperty, 1);
            final LongBinding binding2 = Bindings.longValueAt(localProperty, 2);
            DependencyUtils.checkDependencies(binding0.getDependencies(), localProperty);
            DependencyUtils.checkDependencies(binding1.getDependencies(), localProperty);
            DependencyUtils.checkDependencies(binding2.getDependencies(), localProperty);
            assertEquals(defaultData, binding0.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList1);
            assertEquals(localData1, binding0.get());
            assertEquals(localData2, binding1.get());
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.remove(1);
            assertEquals(localData1, binding0.get());
            assertEquals(defaultData, binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList2);
            assertEquals(defaultData, binding0.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.addAll(localData2, localData2);
            assertEquals(localData2, binding0.get());
            assertEquals(localData2, binding1.get());
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(null);
            assertEquals(defaultData, binding0.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
        }
    }

    @Test(expected = NullPointerException.class)
    public void testLongValueAt_Constant_Null() {
        Bindings.longValueAt(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLongValueAt_Constant_NegativeIndex() {
        final ListProperty<Long> localProperty = new SimpleListProperty<Long>();
        Bindings.longValueAt(localProperty, -1);
    }

    @Ignore("RT-27128")
    @Test
    public void testLongValueAt_Variable() {
        synchronized (log) {
            log.reset();

            final long defaultData = 0;
            final long localData1 = 98765432123456789L;
            final long localData2 = -1234567890123456789L;
            final ListProperty<Long> localProperty = new SimpleListProperty<Long>();
            final ObservableList<Long> localList1 = FXCollections.observableArrayList(localData1, localData2);
            final ObservableList<Long> localList2 = FXCollections.observableArrayList();

            final LongBinding binding = Bindings.longValueAt(localProperty, index);
            DependencyUtils.checkDependencies(binding.getDependencies(), localProperty, index);

            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList1);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(localData1, binding.get());
            index.set(1);
            assertEquals(localData2, binding.get());
            index.set(2);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.remove(1);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(localData1, binding.get());
            index.set(1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(0, null);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get());
            log.check(0, "INFO", "NullPointerException");
            index.set(1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList2);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.addAll(localData2, localData2);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(localData2, binding.get());
            index.set(1);
            assertEquals(localData2, binding.get());
            index.set(2);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(null);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
        }
    }

    @Test(expected = NullPointerException.class)
    public void testLongValueAt_Variable_Null() {
        Bindings.longValueAt((ObservableList<Long>)null, index);
    }

    @Test(expected = NullPointerException.class)
    public void testLongValueAt_Variable_NullIndex() {
        final ListProperty<Long> localProperty = new SimpleListProperty<Long>();
        Bindings.longValueAt(localProperty, null);
    }

    @Ignore("RT-27128")
    @Test
    public void testStringValueAt_Constant() {
        synchronized (log) {
            log.reset();

            final String defaultData = null;
            final String localData1 = "Hello World";
            final String localData2 = "Goodbye World";
            final ListProperty<String> localProperty = new SimpleListProperty<String>();
            final ObservableList<String> localList1 = FXCollections.observableArrayList(localData1, localData2);
            final ObservableList<String> localList2 = FXCollections.observableArrayList();

            final StringBinding binding0 = Bindings.stringValueAt(localProperty, 0);
            final StringBinding binding1 = Bindings.stringValueAt(localProperty, 1);
            final StringBinding binding2 = Bindings.stringValueAt(localProperty, 2);
            DependencyUtils.checkDependencies(binding0.getDependencies(), localProperty);
            DependencyUtils.checkDependencies(binding1.getDependencies(), localProperty);
            DependencyUtils.checkDependencies(binding2.getDependencies(), localProperty);
            assertEquals(defaultData, binding0.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList1);
            assertEquals(localData1, binding0.get());
            assertEquals(localData2, binding1.get());
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.remove(1);
            assertEquals(localData1, binding0.get());
            assertEquals(defaultData, binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList2);
            assertEquals(defaultData, binding0.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.addAll(localData2, localData2);
            assertEquals(localData2, binding0.get());
            assertEquals(localData2, binding1.get());
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(null);
            assertEquals(defaultData, binding0.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding1.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            assertEquals(defaultData, binding2.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
        }
    }

    @Test(expected = NullPointerException.class)
    public void testStringValueAt_Constant_Null() {
        Bindings.stringValueAt(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStringValueAt_Constant_NegativeIndex() {
        final ListProperty<String> localProperty = new SimpleListProperty<String>();
        Bindings.stringValueAt(localProperty, -1);
    }

    @Ignore("RT-27128")
    @Test
    public void testStringValueAt_Variable() {
        synchronized (log) {
            log.reset();

            final String defaultData = null;
            final String localData1 = "Goodbye";
            final String localData2 = "Hello";
            final ListProperty<String> localProperty = new SimpleListProperty<String>();
            final ObservableList<String> localList1 = FXCollections.observableArrayList(localData1, localData2);
            final ObservableList<String> localList2 = FXCollections.observableArrayList();

            final StringBinding binding = Bindings.stringValueAt(localProperty, index);
            DependencyUtils.checkDependencies(binding.getDependencies(), localProperty, index);

            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList1);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(localData1, binding.get());
            index.set(1);
            assertEquals(localData2, binding.get());
            index.set(2);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.remove(1);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(localData1, binding.get());
            index.set(1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(localList2);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.addAll(localData2, localData2);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(localData2, binding.get());
            index.set(1);
            assertEquals(localData2, binding.get());
            index.set(2);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");

            localProperty.set(null);
            index.set(-1);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
            index.set(0);
            assertEquals(defaultData, binding.get());
            log.check(0, "WARNING", 1, "IndexOutOfBoundsException");
        }
    }

    @Test(expected = NullPointerException.class)
    public void testStringValueAt_Variable_Null() {
        Bindings.stringValueAt((ObservableList<String>)null, index);
    }

    @Test(expected = NullPointerException.class)
    public void testStringValueAt_Variable_NullIndex() {
        final ListProperty<String> localProperty = new SimpleListProperty<String>();
        Bindings.stringValueAt(localProperty, null);
    }


}
