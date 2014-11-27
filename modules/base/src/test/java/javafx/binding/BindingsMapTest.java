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
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import com.sun.javafx.binding.ErrorLoggingUtiltity;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class BindingsMapTest {
    
    private static final double EPSILON_DOUBLE = 1e-12;
    private static final float EPSILON_FLOAT = 1e-5f;

    private static final String key1 = "Key1";
    private static final String key2 = "Key2";
    private static final String key3 = "Key3";
    private static final Object data1 = new Object();
    private static final Object data2 = new Object();

    private static final ErrorLoggingUtiltity log = new ErrorLoggingUtiltity();

    private MapProperty<String, Object> property;
    private ObservableMap<String, Object> map1;
    private ObservableMap<String, Object> map2;
    private StringProperty index;

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
        property = new SimpleMapProperty<String, Object>();
        map1 = FXCollections.observableHashMap();
        map1.put(key1, data1);
        map1.put(key2, data2);
        map2 = FXCollections.observableHashMap();
        index = new SimpleStringProperty();
    }

    @Test
    public void testSize() {
        final IntegerBinding size = Bindings.size(property);
        DependencyUtils.checkDependencies(size.getDependencies(), property);
        
        assertEquals(0, size.get());
        property.set(map1);
        assertEquals(2, size.get());
        map1.remove(key2);
        assertEquals(1, size.get());
        property.set(map2);
        assertEquals(0, size.get());
        property.put(key1, data1);
        property.put(key2, data2);
        assertEquals(2, size.get());
        property.set(null);
        assertEquals(0, size.get());
    }

    @Test(expected = NullPointerException.class)
    public void testSize_Null() {
        Bindings.size((ObservableMap<String, Object>) null);
    }

    @Test
    public void testIsEmpty() {
        final BooleanBinding empty = Bindings.isEmpty(property);
        DependencyUtils.checkDependencies(empty.getDependencies(), property);

        assertTrue(empty.get());
        property.set(map1);
        assertFalse(empty.get());
        map1.remove(key2);
        assertFalse(empty.get());
        property.set(map2);
        assertTrue(empty.get());
        property.put(key1, data1);
        property.put(key2, data2);
        assertFalse(empty.get());
        property.set(null);
        assertTrue(empty.get());
    }

    @Test(expected = NullPointerException.class)
    public void testIsEmpty_Null() {
        Bindings.isEmpty((ObservableMap<String, Object>) null);
    }

    @Test
    public void testIsNotEmpty() {
        final BooleanBinding notEmpty = Bindings.isNotEmpty(property);
        DependencyUtils.checkDependencies(notEmpty.getDependencies(), property);

        assertFalse(notEmpty.get());
        property.set(map1);
        assertTrue(notEmpty.get());
        map1.remove(key2);
        assertTrue(notEmpty.get());
        property.set(map2);
        assertFalse(notEmpty.get());
        property.put(key1, data1);
        property.put(key2, data2);
        assertTrue(notEmpty.get());
        property.set(null);
        assertFalse(notEmpty.get());
    }

    @Test(expected = NullPointerException.class)
    public void testIsNotEmpty_Null() {
        Bindings.isNotEmpty((ObservableMap<String, Object>) null);
    }

    @Test
    public void testValueAt_Constant() {
        final ObjectBinding<Object> binding0 = Bindings.valueAt(property, key1);
        final ObjectBinding<Object> binding1 = Bindings.valueAt(property, key2);
        final ObjectBinding<Object> binding2 = Bindings.valueAt(property, key3);
        DependencyUtils.checkDependencies(binding0.getDependencies(), property);
        DependencyUtils.checkDependencies(binding1.getDependencies(), property);
        DependencyUtils.checkDependencies(binding2.getDependencies(), property);
        assertNull(binding0.get());
        assertNull(binding1.get());
        assertNull(binding2.get());

        property.set(map1);
        assertEquals(data1, binding0.get());
        assertEquals(data2, binding1.get());
        assertNull(binding2.get());

        property.remove(key2);
        assertEquals(data1, binding0.get());
        assertNull(binding1.get());
        assertNull(binding2.get());

        property.set(map2);
        assertNull(binding0.get());
        assertNull(binding1.get());
        assertNull(binding2.get());

        property.put(key1, data2);
        property.put(key2, data2);
        assertEquals(data2, binding0.get());
        assertEquals(data2, binding1.get());
        assertNull(binding2.get());

        property.set(null);
        assertNull(binding0.get());
        assertNull(binding1.get());
        assertNull(binding2.get());
    }

    @Test(expected = NullPointerException.class)
    public void testValueAt_Constant_Null() {
        Bindings.valueAt(null, key1);
    }
    
    @Test
    public void testValueAt_Variable() {
        final ObjectBinding<Object> binding = Bindings.valueAt(property, index);
        DependencyUtils.checkDependencies(binding.getDependencies(), property, index);
        
        index.set(null);
        assertNull(binding.get());
        index.set(key1);
        assertNull(binding.get());
        
        property.set(map1);
        index.set(null);
        assertNull(binding.get());
        index.set(key1);
        assertEquals(data1, binding.get());
        index.set(key2);
        assertEquals(data2, binding.get());
        index.set(key3);
        assertNull(binding.get());
        
        property.remove(key2);
        index.set(null);
        assertNull(binding.get());
        index.set(key1);
        assertEquals(data1, binding.get());
        index.set(key2);
        assertNull(binding.get());
        
        property.set(map2);
        index.set(null);
        assertNull(binding.get());
        index.set(key1);
        assertNull(binding.get());

        property.put(key1, data2);
        property.put(key2, data2);
        index.set(null);
        assertNull(binding.get());
        index.set(key1);
        assertEquals(data2, binding.get());
        index.set(key2);
        assertEquals(data2, binding.get());
        index.set(key3);
        assertNull(binding.get());

        property.set(null);
        index.set(null);
        assertNull(binding.get());
        index.set(key1);
        assertNull(binding.get());
    }

    @Test(expected = NullPointerException.class)
    public void testValueAt_Variable_Null() {
        Bindings.valueAt((ObservableMap<String, Object>)null, index);
    }
    
    @Test(expected = NullPointerException.class)
    public void testValueAt_Variable_Null_2() {
        Bindings.valueAt(property, (ObservableValue<String>)null);
    }
    
    @Test
    public void testBooleanValueAt_Constant() {
        final boolean defaultData = false;
        final boolean localData1 = false;
        final boolean localData2 = true;
        final MapProperty<String, Boolean> localProperty = new SimpleMapProperty<String, Boolean>();
        final ObservableMap<String, Boolean> localMap1 = FXCollections.observableHashMap();
        localMap1.put(key1, localData1);
        localMap1.put(key2, localData2);
        final ObservableMap<String, Boolean> localMap2 = FXCollections.observableHashMap();

        final BooleanBinding binding0 = Bindings.booleanValueAt(localProperty, key1);
        final BooleanBinding binding1 = Bindings.booleanValueAt(localProperty, key2);
        final BooleanBinding binding2 = Bindings.booleanValueAt(localProperty, key3);
        DependencyUtils.checkDependencies(binding0.getDependencies(), localProperty);
        DependencyUtils.checkDependencies(binding1.getDependencies(), localProperty);
        DependencyUtils.checkDependencies(binding2.getDependencies(), localProperty);
        assertEquals(defaultData, binding0.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding1.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get());
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap1);
        assertEquals(localData1, binding0.get());
        assertEquals(localData2, binding1.get());
        assertEquals(defaultData, binding2.get());
        log.checkFine(NullPointerException.class);

        localProperty.remove(key2);
        assertEquals(localData1, binding0.get());
        assertEquals(defaultData, binding1.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get());
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap2);
        assertEquals(defaultData, binding0.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding1.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get());
        log.checkFine(NullPointerException.class);

        localProperty.put(key1, localData2);
        localProperty.put(key2, localData2);
        assertEquals(localData2, binding0.get());
        assertEquals(localData2, binding1.get());
        assertEquals(defaultData, binding2.get());
        log.checkFine(NullPointerException.class);

        localProperty.set(null);
        assertEquals(defaultData, binding0.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding1.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get());
        log.checkFine(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void testBooleanValueAt_Constant_Null() {
        Bindings.booleanValueAt(null, key1);
    }
    
    @Test
    public void testBooleanValueAt_Variable() {
        final boolean defaultData = false;
        final boolean localData1 = false;
        final boolean localData2 = true;
        final MapProperty<String, Boolean> localProperty = new SimpleMapProperty<String, Boolean>();
        final ObservableMap<String, Boolean> localMap1 = FXCollections.observableHashMap();
        localMap1.put(key1, localData1);
        localMap1.put(key2, localData2);
        final ObservableMap<String, Boolean> localMap2 = FXCollections.observableHashMap();

        final BooleanBinding binding = Bindings.booleanValueAt(localProperty, index);
        DependencyUtils.checkDependencies(binding.getDependencies(), localProperty, index);

        index.set(null);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap1);
        index.set(null);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(localData1, binding.get());
        index.set(key2);
        assertEquals(localData2, binding.get());
        index.set(key3);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);

        localProperty.remove(key2);
        index.set(null);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(localData1, binding.get());
        index.set(key2);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap2);
        index.set(null);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);

        localProperty.put(key1, localData2);
        localProperty.put(key2, localData2);
        index.set(null);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(localData2, binding.get());
        index.set(key2);
        assertEquals(localData2, binding.get());
        index.set(key3);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);

        localProperty.set(null);
        index.set(null);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void testBooleanValueAt_Variable_Null() {
        Bindings.booleanValueAt((ObservableMap<String, Boolean>)null, index);
    }
    
    @Test(expected = NullPointerException.class)
    public void testBooleanValueAt_Variable_Null_2() {
        final MapProperty<String, Boolean> localProperty = new SimpleMapProperty<String, Boolean>();
        Bindings.booleanValueAt(localProperty, (ObservableValue<String>)null);
    }
    
    @Test
    public void testDoubleValueAt_Constant() {
        final double defaultData = 0.0;
        final double localData1 = Math.PI;
        final double localData2 = -Math.E;
        final MapProperty<String, Double> localProperty = new SimpleMapProperty<String, Double>();
        final ObservableMap<String, Double> localMap1 = FXCollections.observableHashMap();
        localMap1.put(key1, localData1);
        localMap1.put(key2, localData2);
        final ObservableMap<String, Double> localMap2 = FXCollections.observableHashMap();

        final DoubleBinding binding0 = Bindings.doubleValueAt(localProperty, key1);
        final DoubleBinding binding1 = Bindings.doubleValueAt(localProperty, key2);
        final DoubleBinding binding2 = Bindings.doubleValueAt(localProperty, key3);
        DependencyUtils.checkDependencies(binding0.getDependencies(), localProperty);
        DependencyUtils.checkDependencies(binding1.getDependencies(), localProperty);
        DependencyUtils.checkDependencies(binding2.getDependencies(), localProperty);
        assertEquals(defaultData, binding0.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding1.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap1);
        assertEquals(localData1, binding0.get(), EPSILON_DOUBLE);
        assertEquals(localData2, binding1.get(), EPSILON_DOUBLE);
        assertEquals(defaultData, binding2.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);

        localProperty.remove(key2);
        assertEquals(localData1, binding0.get(), EPSILON_DOUBLE);
        assertEquals(defaultData, binding1.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap2);
        assertEquals(defaultData, binding0.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding1.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);

        localProperty.put(key1, localData2);
        localProperty.put(key2, localData2);
        assertEquals(localData2, binding0.get(), EPSILON_DOUBLE);
        assertEquals(localData2, binding1.get(), EPSILON_DOUBLE);
        assertEquals(defaultData, binding2.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);

        localProperty.set(null);
        assertEquals(defaultData, binding0.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding1.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void testDoubleValueAt_Constant_Null() {
        Bindings.doubleValueAt(null, key1);
    }
    
    @Test
    public void testDoubleValueAt_Variable() {
        final double defaultData = 0.0;
        final double localData1 = -Math.PI;
        final double localData2 = Math.E;
        final MapProperty<String, Double> localProperty = new SimpleMapProperty<String, Double>();
        final ObservableMap<String, Double> localMap1 = FXCollections.observableHashMap();
        localMap1.put(key1, localData1);
        localMap1.put(key2, localData2);
        final ObservableMap<String, Double> localMap2 = FXCollections.observableHashMap();

        final DoubleBinding binding = Bindings.doubleValueAt(localProperty, index);
        DependencyUtils.checkDependencies(binding.getDependencies(), localProperty, index);

        index.set(null);
        assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap1);
        index.set(null);
        assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(localData1, binding.get(), EPSILON_DOUBLE);
        index.set(key2);
        assertEquals(localData2, binding.get(), EPSILON_DOUBLE);
        index.set(key3);
        assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);

        localProperty.remove(key2);
        index.set(null);
        assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(localData1, binding.get(), EPSILON_DOUBLE);
        index.set(key2);
        assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap2);
        index.set(null);
        assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);

        localProperty.put(key1, localData2);
        localProperty.put(key2, localData2);
        index.set(null);
        assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(localData2, binding.get(), EPSILON_DOUBLE);
        index.set(key2);
        assertEquals(localData2, binding.get(), EPSILON_DOUBLE);
        index.set(key3);
        assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);

        localProperty.set(null);
        index.set(null);
        assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(defaultData, binding.get(), EPSILON_DOUBLE);
        log.checkFine(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void testDoubleValueAt_Variable_Null_1() {
        Bindings.doubleValueAt((ObservableMap<String, Double>)null, index);
    }
    
    @Test(expected = NullPointerException.class)
    public void testDoubleValueAt_Variable_Null_2() {
        final MapProperty<String, Double> localProperty = new SimpleMapProperty<String, Double>();
        Bindings.doubleValueAt(localProperty, (ObservableValue<String>)null);
    }
    
    @Test
    public void testFloatValueAt_Constant() {
        final float defaultData = 0.0f;
        final float localData1 = (float)Math.PI;
        final float localData2 = (float)-Math.E;
        final MapProperty<String, Float> localProperty = new SimpleMapProperty<String, Float>();
        final ObservableMap<String, Float> localMap1 = FXCollections.observableHashMap();
        localMap1.put(key1, localData1);
        localMap1.put(key2, localData2);
        final ObservableMap<String, Float> localMap2 = FXCollections.observableHashMap();

        final FloatBinding binding0 = Bindings.floatValueAt(localProperty, key1);
        final FloatBinding binding1 = Bindings.floatValueAt(localProperty, key2);
        final FloatBinding binding2 = Bindings.floatValueAt(localProperty, key3);
        DependencyUtils.checkDependencies(binding0.getDependencies(), localProperty);
        DependencyUtils.checkDependencies(binding1.getDependencies(), localProperty);
        DependencyUtils.checkDependencies(binding2.getDependencies(), localProperty);
        assertEquals(defaultData, binding0.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding1.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap1);
        assertEquals(localData1, binding0.get(), EPSILON_FLOAT);
        assertEquals(localData2, binding1.get(), EPSILON_FLOAT);
        assertEquals(defaultData, binding2.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);

        localProperty.remove(key2);
        assertEquals(localData1, binding0.get(), EPSILON_FLOAT);
        assertEquals(defaultData, binding1.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap2);
        assertEquals(defaultData, binding0.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding1.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);

        localProperty.put(key1, localData2);
        localProperty.put(key2, localData2);
        assertEquals(localData2, binding0.get(), EPSILON_FLOAT);
        assertEquals(localData2, binding1.get(), EPSILON_FLOAT);
        assertEquals(defaultData, binding2.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);

        localProperty.set(null);
        assertEquals(defaultData, binding0.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding1.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void testFloatValueAt_Constant_Null() {
        Bindings.floatValueAt(null, key1);
    }
    
    @Test
    public void testFloatValueAt_Variable() {
        final float defaultData = 0.0f;
        final float localData1 = (float)-Math.PI;
        final float localData2 = (float)Math.E;
        final MapProperty<String, Float> localProperty = new SimpleMapProperty<String, Float>();
        final ObservableMap<String, Float> localMap1 = FXCollections.observableHashMap();
        localMap1.put(key1, localData1);
        localMap1.put(key2, localData2);
        final ObservableMap<String, Float> localMap2 = FXCollections.observableHashMap();

        final FloatBinding binding = Bindings.floatValueAt(localProperty, index);
        DependencyUtils.checkDependencies(binding.getDependencies(), localProperty, index);

        index.set(null);
        assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap1);
        index.set(null);
        assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(localData1, binding.get(), EPSILON_FLOAT);
        index.set(key2);
        assertEquals(localData2, binding.get(), EPSILON_FLOAT);
        index.set(key3);
        assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);

        localProperty.remove(key2);
        index.set(null);
        assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(localData1, binding.get(), EPSILON_FLOAT);
        index.set(key2);
        assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap2);
        index.set(null);
        assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);

        localProperty.put(key1, localData2);
        localProperty.put(key2, localData2);
        index.set(null);
        assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(localData2, binding.get(), EPSILON_FLOAT);
        index.set(key2);
        assertEquals(localData2, binding.get(), EPSILON_FLOAT);
        index.set(key3);
        assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);

        localProperty.set(null);
        index.set(null);
        assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(defaultData, binding.get(), EPSILON_FLOAT);
        log.checkFine(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void testFloatValueAt_Variable_Null_1() {
        Bindings.floatValueAt((ObservableMap<String, Float>)null, index);
    }
    
    @Test(expected = NullPointerException.class)
    public void testFloatValueAt_Variable_Null_2() {
        final MapProperty<String, Float> localProperty = new SimpleMapProperty<String, Float>();
        Bindings.floatValueAt(localProperty, (ObservableValue<String>)null);
    }
    
    @Test
    public void testIntegerValueAt_Constant() {
        final int defaultData = 0;
        final int localData1 = 42;
        final int localData2 = -7;
        final MapProperty<String, Integer> localProperty = new SimpleMapProperty<String, Integer>();
        final ObservableMap<String, Integer> localMap1 = FXCollections.observableHashMap();
        localMap1.put(key1, localData1);
        localMap1.put(key2, localData2);
        final ObservableMap<String, Integer> localMap2 = FXCollections.observableHashMap();

        final IntegerBinding binding0 = Bindings.integerValueAt(localProperty, key1);
        final IntegerBinding binding1 = Bindings.integerValueAt(localProperty, key2);
        final IntegerBinding binding2 = Bindings.integerValueAt(localProperty, key3);
        DependencyUtils.checkDependencies(binding0.getDependencies(), localProperty);
        DependencyUtils.checkDependencies(binding1.getDependencies(), localProperty);
        DependencyUtils.checkDependencies(binding2.getDependencies(), localProperty);
        assertEquals(defaultData, binding0.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding1.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get());
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap1);
        assertEquals(localData1, binding0.get());
        assertEquals(localData2, binding1.get());
        assertEquals(defaultData, binding2.get());
        log.checkFine(NullPointerException.class);

        localProperty.remove(key2);
        assertEquals(localData1, binding0.get());
        assertEquals(defaultData, binding1.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get());
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap2);
        assertEquals(defaultData, binding0.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding1.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get());
        log.checkFine(NullPointerException.class);

        localProperty.put(key1, localData2);
        localProperty.put(key2, localData2);
        assertEquals(localData2, binding0.get());
        assertEquals(localData2, binding1.get());
        assertEquals(defaultData, binding2.get());
        log.checkFine(NullPointerException.class);

        localProperty.set(null);
        assertEquals(defaultData, binding0.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding1.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get());
        log.checkFine(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void testIntegerValueAt_Constant_Null() {
        Bindings.integerValueAt(null, key1);
    }
    
    @Test
    public void testIntegerValueAt_Variable() {
        final int defaultData = 0;
        final int localData1 = 42;
        final int localData2 = -7;
        final MapProperty<String, Integer> localProperty = new SimpleMapProperty<String, Integer>();
        final ObservableMap<String, Integer> localMap1 = FXCollections.observableHashMap();
        localMap1.put(key1, localData1);
        localMap1.put(key2, localData2);
        final ObservableMap<String, Integer> localMap2 = FXCollections.observableHashMap();

        final IntegerBinding binding = Bindings.integerValueAt(localProperty, index);
        DependencyUtils.checkDependencies(binding.getDependencies(), localProperty, index);

        index.set(null);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap1);
        index.set(null);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(localData1, binding.get());
        index.set(key2);
        assertEquals(localData2, binding.get());
        index.set(key3);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);

        localProperty.remove(key2);
        index.set(null);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(localData1, binding.get());
        index.set(key2);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap2);
        index.set(null);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);

        localProperty.put(key1, localData2);
        localProperty.put(key2, localData2);
        index.set(null);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(localData2, binding.get());
        index.set(key2);
        assertEquals(localData2, binding.get());
        index.set(key3);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);

        localProperty.set(null);
        index.set(null);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void testIntegerValueAt_Variable_Null_1() {
        Bindings.integerValueAt((ObservableMap<String, Integer>)null, index);
    }
    
    @Test(expected = NullPointerException.class)
    public void testIntegerValueAt_Variable_Null_2() {
        final MapProperty<String, Integer> localProperty = new SimpleMapProperty<String, Integer>();
        Bindings.integerValueAt(localProperty, (ObservableValue<String>)null);
    }
    
    @Test
    public void testLongValueAt_Constant() {
        final long defaultData = 0L;
        final long localData1 = 1234567890987654321L;
        final long localData2 = -987654321987654321L;
        final MapProperty<String, Long> localProperty = new SimpleMapProperty<String, Long>();
        final ObservableMap<String, Long> localMap1 = FXCollections.observableHashMap();
        localMap1.put(key1, localData1);
        localMap1.put(key2, localData2);
        final ObservableMap<String, Long> localMap2 = FXCollections.observableHashMap();

        final LongBinding binding0 = Bindings.longValueAt(localProperty, key1);
        final LongBinding binding1 = Bindings.longValueAt(localProperty, key2);
        final LongBinding binding2 = Bindings.longValueAt(localProperty, key3);
        DependencyUtils.checkDependencies(binding0.getDependencies(), localProperty);
        DependencyUtils.checkDependencies(binding1.getDependencies(), localProperty);
        DependencyUtils.checkDependencies(binding2.getDependencies(), localProperty);
        assertEquals(defaultData, binding0.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding1.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get());
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap1);
        assertEquals(localData1, binding0.get());
        assertEquals(localData2, binding1.get());
        assertEquals(defaultData, binding2.get());
        log.checkFine(NullPointerException.class);

        localProperty.remove(key2);
        assertEquals(localData1, binding0.get());
        assertEquals(defaultData, binding1.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get());
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap2);
        assertEquals(defaultData, binding0.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding1.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get());
        log.checkFine(NullPointerException.class);

        localProperty.put(key1, localData2);
        localProperty.put(key2, localData2);
        assertEquals(localData2, binding0.get());
        assertEquals(localData2, binding1.get());
        assertEquals(defaultData, binding2.get());
        log.checkFine(NullPointerException.class);

        localProperty.set(null);
        assertEquals(defaultData, binding0.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding1.get());
        log.checkFine(NullPointerException.class);
        assertEquals(defaultData, binding2.get());
        log.checkFine(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void testLongValueAt_Constant_Null() {
        Bindings.longValueAt(null, key1);
    }
    
    @Test
    public void testLongValueAt_Variable() {
        final long defaultData = 0;
        final long localData1 = 98765432123456789L;
        final long localData2 = -1234567890123456789L;
        final MapProperty<String, Long> localProperty = new SimpleMapProperty<String, Long>();
        final ObservableMap<String, Long> localMap1 = FXCollections.observableHashMap();
        localMap1.put(key1, localData1);
        localMap1.put(key2, localData2);
        final ObservableMap<String, Long> localMap2 = FXCollections.observableHashMap();

        final LongBinding binding = Bindings.longValueAt(localProperty, index);
        DependencyUtils.checkDependencies(binding.getDependencies(), localProperty, index);

        index.set(null);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap1);
        index.set(null);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(localData1, binding.get());
        index.set(key2);
        assertEquals(localData2, binding.get());
        index.set(key3);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);

        localProperty.remove(key2);
        index.set(null);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(localData1, binding.get());
        index.set(key2);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);

        localProperty.set(localMap2);
        index.set(null);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);

        localProperty.put(key1, localData2);
        localProperty.put(key2, localData2);
        index.set(null);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(localData2, binding.get());
        index.set(key2);
        assertEquals(localData2, binding.get());
        index.set(key3);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);

        localProperty.set(null);
        index.set(null);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
        index.set(key1);
        assertEquals(defaultData, binding.get());
        log.checkFine(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void testLongValueAt_Variable_Null() {
        Bindings.longValueAt((ObservableMap<String, Long>)null, index);
    }
    
    @Test(expected = NullPointerException.class)
    public void testLongValueAt_Variable_Null_2() {
        final MapProperty<String, Long> localProperty = new SimpleMapProperty<String, Long>();
        Bindings.longValueAt(localProperty, (ObservableValue<String>)null);
    }
    
    @Test
    public void testStringValueAt_Constant() {
        final String defaultData = null;
        final String localData1 = "Hello World";
        final String localData2 = "Goodbye World";
        final MapProperty<String, String> localProperty = new SimpleMapProperty<String, String>();
        final ObservableMap<String, String> localMap1 = FXCollections.observableHashMap();
        localMap1.put(key1, localData1);
        localMap1.put(key2, localData2);
        final ObservableMap<String, String> localMap2 = FXCollections.observableHashMap();
        
        final StringBinding binding0 = Bindings.stringValueAt(localProperty, key1);
        final StringBinding binding1 = Bindings.stringValueAt(localProperty, key2);
        final StringBinding binding2 = Bindings.stringValueAt(localProperty, key3);
        DependencyUtils.checkDependencies(binding0.getDependencies(), localProperty);
        DependencyUtils.checkDependencies(binding1.getDependencies(), localProperty);
        DependencyUtils.checkDependencies(binding2.getDependencies(), localProperty);
        assertEquals(defaultData, binding0.get());
        assertEquals(defaultData, binding1.get());
        assertEquals(defaultData, binding2.get());
        
        localProperty.set(localMap1);
        assertEquals(localData1, binding0.get());
        assertEquals(localData2, binding1.get());
        assertEquals(defaultData, binding2.get());
        
        localProperty.remove(key2);
        assertEquals(localData1, binding0.get());
        assertEquals(defaultData, binding1.get());
        assertEquals(defaultData, binding2.get());
        
        localProperty.set(localMap2);
        assertEquals(defaultData, binding0.get());
        assertEquals(defaultData, binding1.get());
        assertEquals(defaultData, binding2.get());

        localProperty.put(key1, localData2);
        localProperty.put(key2, localData2);
        assertEquals(localData2, binding0.get());
        assertEquals(localData2, binding1.get());
        assertEquals(defaultData, binding2.get());

        localProperty.set(null);
        assertEquals(defaultData, binding0.get());
        assertEquals(defaultData, binding1.get());
        assertEquals(defaultData, binding2.get());
    }

    @Test(expected = NullPointerException.class)
    public void testStringValueAt_Constant_Null() {
        Bindings.stringValueAt(null, key1);
    }
    
    @Test
    public void testStringValueAt_Variable() {
        final String defaultData = null;
        final String localData1 = "Goodbye";
        final String localData2 = "Hello";
        final MapProperty<String, String> localProperty = new SimpleMapProperty<String, String>();
        final ObservableMap<String, String> localMap1 = FXCollections.observableHashMap();
        localMap1.put(key1, localData1);
        localMap1.put(key2, localData2);
        final ObservableMap<String, String> localMap2 = FXCollections.observableHashMap();

        final StringBinding binding = Bindings.stringValueAt(localProperty, index);
        DependencyUtils.checkDependencies(binding.getDependencies(), localProperty, index);

        index.set(null);
        assertEquals(defaultData, binding.get());
        index.set(key1);
        assertEquals(defaultData, binding.get());
        
        localProperty.set(localMap1);
        index.set(null);
        assertEquals(defaultData, binding.get());
        index.set(key1);
        assertEquals(localData1, binding.get());
        index.set(key2);
        assertEquals(localData2, binding.get());
        index.set(key3);
        assertEquals(defaultData, binding.get());
        
        localProperty.remove(key2);
        index.set(null);
        assertEquals(defaultData, binding.get());
        index.set(key1);
        assertEquals(localData1, binding.get());
        index.set(key2);
        assertEquals(defaultData, binding.get());
        
        localProperty.set(localMap2);
        index.set(null);
        assertEquals(defaultData, binding.get());
        index.set(key1);
        assertEquals(defaultData, binding.get());

        localProperty.put(key1, localData2);
        localProperty.put(key2, localData2);
        index.set(null);
        assertEquals(defaultData, binding.get());
        index.set(key1);
        assertEquals(localData2, binding.get());
        index.set(key2);
        assertEquals(localData2, binding.get());
        index.set(key3);
        assertEquals(defaultData, binding.get());

        localProperty.set(null);
        index.set(null);
        assertEquals(defaultData, binding.get());
        index.set(key1);
        assertEquals(defaultData, binding.get());
    }

    @Test(expected = NullPointerException.class)
    public void testStringValueAt_Variable_Null() {
        Bindings.stringValueAt((ObservableMap<String, String>)null, index);
    }
    
    @Test(expected = NullPointerException.class)
    public void testStringValueAt_Variable_Null_2() {
        final MapProperty<String, String> localProperty = new SimpleMapProperty<String, String>();
        Bindings.stringValueAt(localProperty, (ObservableValue<String>)null);
    }
    
    
}
