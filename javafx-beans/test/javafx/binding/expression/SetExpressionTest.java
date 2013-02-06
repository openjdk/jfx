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
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Iterator;

import static org.junit.Assert.*;

public class SetExpressionTest {

    private static final Integer data1_0 = 7;
    private static final Integer data2_0 = 42;
    private static final Integer data2_1 = -3;
    private static final Integer datax = Integer.MAX_VALUE;
    
    private SetProperty<Integer> opNull;
    private SetProperty<Integer> opEmpty;
    private SetProperty<Integer> op1;
    private SetProperty<Integer> op2;

    @Before
    public void setUp() {
        opNull = new SimpleSetProperty<Integer>();
        opEmpty = new SimpleSetProperty<Integer>(FXCollections.<Integer>observableSet());
        op1 = new SimpleSetProperty<Integer>(FXCollections.observableSet(data1_0));
        op2 = new SimpleSetProperty<Integer>(FXCollections.observableSet(data2_0, data2_1));
    }

    @Test
    public void testGetSize() {
        assertEquals(0, opNull.getSize());
        assertEquals(0, opEmpty.getSize());
        assertEquals(1, op1.getSize());
        assertEquals(2, op2.getSize());
    }

    @Test
    public void testIsEqualTo() {
        final ObservableSet<Integer> emptySet = FXCollections.observableSet(Collections.<Integer>emptySet());
        final ObservableSet<Integer> set1 = FXCollections.observableSet(data1_0);
        final ObservableSet<Integer> set2 = FXCollections.observableSet(data2_0, data2_1);

        BooleanBinding binding = opNull.isEqualTo(emptySet);
        assertEquals(false, binding.get());
        binding = opNull.isEqualTo(set1);
        assertEquals(false, binding.get());
        binding = opNull.isEqualTo(set2);
        assertEquals(false, binding.get());

        binding = opEmpty.isEqualTo(emptySet);
        assertEquals(true, binding.get());
        binding = opEmpty.isEqualTo(set1);
        assertEquals(false, binding.get());
        binding = opEmpty.isEqualTo(set2);
        assertEquals(false, binding.get());

        binding = op1.isEqualTo(emptySet);
        assertEquals(false, binding.get());
        binding = op1.isEqualTo(set1);
        assertEquals(true, binding.get());
        binding = op1.isEqualTo(set2);
        assertEquals(false, binding.get());

        binding = op2.isEqualTo(emptySet);
        assertEquals(false, binding.get());
        binding = op2.isEqualTo(set1);
        assertEquals(false, binding.get());
        binding = op2.isEqualTo(set2);
        assertEquals(true, binding.get());
    }


    @Test
    public void testIsNotEqualTo() {
        final ObservableSet<Integer> emptySet = FXCollections.observableSet(Collections.<Integer>emptySet());
        final ObservableSet<Integer> list1 = FXCollections.observableSet(data1_0);
        final ObservableSet<Integer> list2 = FXCollections.observableSet(data2_0, data2_1);

        BooleanBinding binding = opNull.isNotEqualTo(emptySet);
        assertEquals(true, binding.get());
        binding = opNull.isNotEqualTo(list1);
        assertEquals(true, binding.get());
        binding = opNull.isNotEqualTo(list2);
        assertEquals(true, binding.get());

        binding = opEmpty.isNotEqualTo(emptySet);
        assertEquals(false, binding.get());
        binding = opEmpty.isNotEqualTo(list1);
        assertEquals(true, binding.get());
        binding = opEmpty.isNotEqualTo(list2);
        assertEquals(true, binding.get());

        binding = op1.isNotEqualTo(emptySet);
        assertEquals(true, binding.get());
        binding = op1.isNotEqualTo(list1);
        assertEquals(false, binding.get());
        binding = op1.isNotEqualTo(list2);
        assertEquals(true, binding.get());

        binding = op2.isNotEqualTo(emptySet);
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
        assertEquals(Collections.emptySet().toString(), opEmpty.asString().get());
        assertEquals(Collections.singleton(data1_0).toString(), op1.asString().get());
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
    public void testContains() {
        assertFalse(opNull.contains(data1_0));
        assertFalse(opNull.contains(data2_0));
        assertFalse(opNull.contains(data2_1));

        assertFalse(opEmpty.contains(data1_0));
        assertFalse(opEmpty.contains(data2_0));
        assertFalse(opEmpty.contains(data2_1));

        assertTrue(op1.contains(data1_0));
        assertFalse(op1.contains(data2_0));
        assertFalse(op1.contains(data2_1));

        assertFalse(op2.contains(data1_0));
        assertTrue(op2.contains(data2_0));
        assertTrue(op2.contains(data2_1));
    }
    
    @Test
    public void testIterator() {
        assertFalse(opNull.iterator().hasNext());
        assertFalse(opEmpty.iterator().hasNext());

        Iterator<Integer> iterator = op1.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(data1_0, iterator.next());
        assertFalse(iterator.hasNext());

        iterator = op2.iterator();
        assertTrue(iterator.hasNext());
        final Integer next = iterator.next();
        if (data2_0.equals(next)) {
            assertTrue(iterator.hasNext());
            assertEquals(data2_1, iterator.next());
        } else if (data2_1.equals(next)) {
            assertTrue(iterator.hasNext());
            assertEquals(data2_0, iterator.next());
        } else {
            fail();
        }
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testToArray_NoArg() {
        assertArrayEquals(new Object[0], opNull.toArray());
        assertArrayEquals(new Object[0], opEmpty.toArray());
        assertArrayEquals(new Object[] {data1_0}, op1.toArray());
        final Object[] array = op2.toArray();
        assertEquals(2, array.length);
        if (data2_0.equals(array[0])) {
            assertEquals(data2_1, array[1]);
        } else if (data2_1.equals(array[0])) {
            assertEquals(data2_0, array[1]);
        } else {
            fail();
        }
    }
    
    @Test
    public void testToArray_WithArg() {
        Integer[] arrayIn = new Integer[] {datax};
        Integer[] arrayOut = opNull.toArray(arrayIn);
        assertArrayEquals(new Integer[] {null}, arrayIn);
        assertArrayEquals(new Integer[] {null}, arrayOut);

        arrayIn = new Integer[] {datax};
        arrayOut = new Integer[] {datax};
        arrayOut = opEmpty.toArray(arrayIn);
        assertArrayEquals(new Integer[] {null}, arrayIn);
        assertArrayEquals(new Integer[] {null}, arrayOut);

        arrayIn = new Integer[] {datax};
        arrayOut = new Integer[] {datax};
        arrayOut = op1.toArray(arrayIn);
        assertArrayEquals(new Integer[] {data1_0}, arrayIn);
        assertArrayEquals(new Integer[] {data1_0}, arrayOut);

        arrayIn = new Integer[] {datax};
        arrayOut = new Integer[] {datax};
        arrayOut = op2.toArray(arrayIn);
        assertArrayEquals(new Integer[] {datax}, arrayIn);
        assertEquals(2, arrayOut.length);
        if (data2_0.equals(arrayOut[0])) {
            assertEquals(data2_1, arrayOut[1]);
        } else if (data2_1.equals(arrayOut[0])) {
            assertEquals(data2_0, arrayOut[1]);
        } else {
            fail();
        }
    }
}