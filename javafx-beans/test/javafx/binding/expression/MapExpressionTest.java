/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package javafx.binding.expression;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MapExpressionTest {

    private static final Number key1_0 = 4711;
    private static final Number key2_0 = 4711;
    private static final Number key2_1 = 4712;
    private static final Number keyx = 4710;
    private static final Integer data1_0 = 7;
    private static final Integer data2_0 = 42;
    private static final Integer data2_1 = -3;
    private static final Integer datax = Integer.MAX_VALUE;
    
    private MapProperty<Number, Integer> opNull;
    private MapProperty<Number, Integer> opEmpty;
    private MapProperty<Number, Integer> op1;
    private MapProperty<Number, Integer> op2;

    @Before
    public void setUp() {
        opNull = new SimpleMapProperty<Number, Integer>();
        opEmpty = new SimpleMapProperty<Number, Integer>(FXCollections.observableMap(Collections.<Number, Integer>emptyMap()));
        op1 = new SimpleMapProperty<Number, Integer>(FXCollections.observableMap(Collections.singletonMap(key1_0, data1_0)));
        final Map<Number, Integer> map = new HashMap<Number, Integer>();
        map.put(key2_0, data2_0);
        map.put(key2_1, data2_1);
        op2 = new SimpleMapProperty<Number, Integer>(FXCollections.observableMap(map));
    }

    @Test
    public void testGetSize() {
        assertEquals(0, opNull.getSize());
        assertEquals(0, opEmpty.getSize());
        assertEquals(1, op1.getSize());
        assertEquals(2, op2.getSize());
    }

    @Test
    public void testValueAt_Constant() {
        assertNull(opNull.valueAt(0).get());
        assertNull(opEmpty.valueAt(0).get());

        assertEquals(data1_0, op1.valueAt(key1_0).get());
        assertNull(op1.valueAt(keyx).get());

        assertEquals(data2_0, op2.valueAt(key2_0).get());
        assertEquals(data2_1, op2.valueAt(key2_1).get());
        assertNull(op2.valueAt(keyx).get());
    }

    @Test
    public void testValueAt_Variable() {
        final IntegerProperty index = new SimpleIntegerProperty(keyx.intValue());

        assertNull(opNull.valueAt(index).get());
        assertNull(opNull.valueAt(index).get());
        assertNull(opEmpty.valueAt(index).get());
        assertNull(op1.valueAt(index).get());
        assertNull(op2.valueAt(index).get());

        index.set(key1_0.intValue());
        assertNull(opNull.valueAt(index).get());
        assertNull(opEmpty.valueAt(index).get());
        assertEquals(data1_0, op1.valueAt(index).get());
        assertEquals(data2_0, op2.valueAt(index).get());

        index.set(key2_1.intValue());
        assertNull(opNull.valueAt(index).get());
        assertNull(opEmpty.valueAt(index).get());
        assertNull(op1.valueAt(index).get());
        assertEquals(data2_1, op2.valueAt(index).get());
    }

    @Test
    public void testIsEqualTo() {
        final ObservableMap<Number, Integer> emptyMap = FXCollections.observableMap(Collections.<Number, Integer>emptyMap());
        final ObservableMap<Number, Integer> map1 = FXCollections.observableMap(Collections.singletonMap(key1_0, data1_0));
        final Map<Number, Integer> map = new HashMap<Number, Integer>();
        map.put(key2_0, data2_0);
        map.put(key2_1, data2_1);
        final ObservableMap<Number, Integer> map2= FXCollections.observableMap(map);

        BooleanBinding binding = opNull.isEqualTo(emptyMap);
        assertEquals(false, binding.get());
        binding = opNull.isEqualTo(map1);
        assertEquals(false, binding.get());
        binding = opNull.isEqualTo(map2);
        assertEquals(false, binding.get());

        binding = opEmpty.isEqualTo(emptyMap);
        assertEquals(true, binding.get());
        binding = opEmpty.isEqualTo(map1);
        assertEquals(false, binding.get());
        binding = opEmpty.isEqualTo(map2);
        assertEquals(false, binding.get());

        binding = op1.isEqualTo(emptyMap);
        assertEquals(false, binding.get());
        binding = op1.isEqualTo(map1);
        assertEquals(true, binding.get());
        binding = op1.isEqualTo(map2);
        assertEquals(false, binding.get());

        binding = op2.isEqualTo(emptyMap);
        assertEquals(false, binding.get());
        binding = op2.isEqualTo(map1);
        assertEquals(false, binding.get());
        binding = op2.isEqualTo(map2);
        assertEquals(true, binding.get());
    }


    @Test
    public void testIsNotEqualTo() {
        final ObservableMap<Number, Integer> emptyMap = FXCollections.observableMap(Collections.<Number, Integer>emptyMap());
        final ObservableMap<Number, Integer> list1 = FXCollections.observableMap(Collections.singletonMap(key1_0, data1_0));
        final Map<Number, Integer> map = new HashMap<Number, Integer>();
        map.put(key2_0, data2_0);
        map.put(key2_1, data2_1);
        final ObservableMap<Number, Integer> list2 = FXCollections.observableMap(map);

        BooleanBinding binding = opNull.isNotEqualTo(emptyMap);
        assertEquals(true, binding.get());
        binding = opNull.isNotEqualTo(list1);
        assertEquals(true, binding.get());
        binding = opNull.isNotEqualTo(list2);
        assertEquals(true, binding.get());

        binding = opEmpty.isNotEqualTo(emptyMap);
        assertEquals(false, binding.get());
        binding = opEmpty.isNotEqualTo(list1);
        assertEquals(true, binding.get());
        binding = opEmpty.isNotEqualTo(list2);
        assertEquals(true, binding.get());

        binding = op1.isNotEqualTo(emptyMap);
        assertEquals(true, binding.get());
        binding = op1.isNotEqualTo(list1);
        assertEquals(false, binding.get());
        binding = op1.isNotEqualTo(list2);
        assertEquals(true, binding.get());

        binding = op2.isNotEqualTo(emptyMap);
        assertEquals(true, binding.get());
        binding = op2.isNotEqualTo(list1);
        assertEquals(true, binding.get());
        binding = op2.isNotEqualTo(list2);
        assertEquals(false, binding.get());
    }

    @Test
    public void testIsNull() {
        assertTrue(opNull.isNull().get());
        assertFalse(opEmpty.isNull().get());
        assertFalse(op1.isNull().get());
        assertFalse(op2.isNull().get());
    }

    @Test
    public void testIsNotNull() {
        assertFalse(opNull.isNotNull().get());
        assertTrue(opEmpty.isNotNull().get());
        assertTrue(op1.isNotNull().get());
        assertTrue(op2.isNotNull().get());
    }
    
    @Test
    public void testAsString() {
        assertEquals("null", opNull.asString().get());
        assertEquals(Collections.emptyMap().toString(), opEmpty.asString().get());
        assertEquals(Collections.singletonMap(key1_0, data1_0).toString(), op1.asString().get());
    }

    @Test
    public void testSize() {
        assertEquals(0, opNull.size());
        assertEquals(0, opEmpty.size());
        assertEquals(1, op1.size());
        assertEquals(2, op2.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(opNull.isEmpty());
        assertTrue(opEmpty.isEmpty());
        assertFalse(op1.isEmpty());
        assertFalse(op2.isEmpty());
    }

    @Test
    public void testContainsKey() {
        assertFalse(opNull.containsKey(key1_0));
        assertFalse(opNull.containsKey(key2_0));
        assertFalse(opNull.containsKey(key2_1));

        assertFalse(opEmpty.containsKey(key1_0));
        assertFalse(opEmpty.containsKey(key2_0));
        assertFalse(opEmpty.containsKey(key2_1));

        assertTrue(op1.containsKey(key1_0));
        assertFalse(op1.containsKey(key2_1));

        assertTrue(op2.containsKey(key2_0));
        assertTrue(op2.containsKey(key2_1));
    }

    @Test
    public void testContainsValue() {
        assertFalse(opNull.containsValue(data1_0));
        assertFalse(opNull.containsValue(data2_0));
        assertFalse(opNull.containsValue(data2_1));

        assertFalse(opEmpty.containsValue(data1_0));
        assertFalse(opEmpty.containsValue(data2_0));
        assertFalse(opEmpty.containsValue(data2_1));

        assertTrue(op1.containsValue(data1_0));
        assertFalse(op1.containsValue(data2_0));
        assertFalse(op1.containsValue(data2_1));

        assertFalse(op2.containsValue(data1_0));
        assertTrue(op2.containsValue(data2_0));
        assertTrue(op2.containsValue(data2_1));
    }


}