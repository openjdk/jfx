/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import javafx.beans.binding.BooleanBinding;
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
import org.junit.Test;

import static org.junit.Assert.*;

public class ListExpressionTest {

    private static final Integer data1_0 = 7;
    private static final Integer data2_0 = 42;
    private static final Integer data2_1 = -3;
    private static final Integer datax = Integer.MAX_VALUE;
    
    private static final ErrorLoggingUtiltity log = new ErrorLoggingUtiltity();
    
    private ListProperty<Integer> opNull;
    private ListProperty<Integer> opEmpty;
    private ListProperty<Integer> op1;
    private ListProperty<Integer> op2;

    @Before
    public void setUp() {
        opNull = new SimpleListProperty<Integer>();
        opEmpty = new SimpleListProperty<Integer>(FXCollections.<Integer>observableArrayList());
        op1 = new SimpleListProperty<Integer>(FXCollections.observableArrayList(data1_0));
        op2 = new SimpleListProperty<Integer>(FXCollections.observableArrayList(data2_0, data2_1));
    }
    
    @BeforeClass
    public static void setUpClass() {
        log.start();
    }

    @AfterClass
    public static void tearDownClass() {
        log.stop();
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
        log.checkFine(IndexOutOfBoundsException.class);
        assertNull(opEmpty.valueAt(0).get());
        log.checkFine(IndexOutOfBoundsException.class);

        assertEquals(data1_0, op1.valueAt(0).get());
        assertNull(op1.valueAt(1).get());
        log.checkFine(IndexOutOfBoundsException.class);

        assertEquals(data2_0, op2.valueAt(0).get());
        assertEquals(data2_1, op2.valueAt(1).get());
        assertNull(op2.valueAt(2).get());
        log.checkFine(IndexOutOfBoundsException.class);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testValueAt_Constant_NegativeIndex() {
        op1.valueAt(-1);
    }

    @Test
    public void testValueAt_Variable() {
        final IntegerProperty index = new SimpleIntegerProperty(-1);

        assertNull(opNull.valueAt(index).get());
        log.checkFine(IndexOutOfBoundsException.class);
        assertNull(opEmpty.valueAt(index).get());
        log.checkFine(ArrayIndexOutOfBoundsException.class);
        assertNull(op1.valueAt(index).get());
        log.checkFine(ArrayIndexOutOfBoundsException.class);
        assertNull(op2.valueAt(index).get());
        log.checkFine(ArrayIndexOutOfBoundsException.class);

        index.set(0);
        assertNull(opNull.valueAt(index).get());
        log.checkFine(IndexOutOfBoundsException.class);
        assertNull(opEmpty.valueAt(index).get());
        log.checkFine(IndexOutOfBoundsException.class);
        assertEquals(data1_0, op1.valueAt(index).get());
        assertEquals(data2_0, op2.valueAt(index).get());

        index.set(1);
        assertNull(opNull.valueAt(index).get());
        log.checkFine(IndexOutOfBoundsException.class);
        assertNull(opEmpty.valueAt(index).get());
        log.checkFine(IndexOutOfBoundsException.class);
        assertNull(op1.valueAt(index).get());
        log.checkFine(IndexOutOfBoundsException.class);
        assertEquals(data2_1, op2.valueAt(index).get());

        index.set(2);
        assertNull(opNull.valueAt(index).get());
        log.checkFine(IndexOutOfBoundsException.class);
        assertNull(opEmpty.valueAt(index).get());
        log.checkFine(IndexOutOfBoundsException.class);
        assertNull(op1.valueAt(index).get());
        log.checkFine(IndexOutOfBoundsException.class);
        assertNull(op2.valueAt(index).get());
        log.checkFine(IndexOutOfBoundsException.class);
    }

    @Test
    public void testIsEqualTo() {
        final ObservableList<Integer> emptyList = FXCollections.emptyObservableList();
        final ObservableList<Integer> list1 = FXCollections.observableArrayList(data1_0);
        final ObservableList<Integer> list2 = FXCollections.observableArrayList(data2_0, data2_1);

        BooleanBinding binding = opNull.isEqualTo(emptyList);
        assertEquals(false, binding.get());
        binding = opNull.isEqualTo(list1);
        assertEquals(false, binding.get());
        binding = opNull.isEqualTo(list2);
        assertEquals(false, binding.get());

        binding = opEmpty.isEqualTo(emptyList);
        assertEquals(true, binding.get());
        binding = opEmpty.isEqualTo(list1);
        assertEquals(false, binding.get());
        binding = opEmpty.isEqualTo(list2);
        assertEquals(false, binding.get());

        binding = op1.isEqualTo(emptyList);
        assertEquals(false, binding.get());
        binding = op1.isEqualTo(list1);
        assertEquals(true, binding.get());
        binding = op1.isEqualTo(list2);
        assertEquals(false, binding.get());

        binding = op2.isEqualTo(emptyList);
        assertEquals(false, binding.get());
        binding = op2.isEqualTo(list1);
        assertEquals(false, binding.get());
        binding = op2.isEqualTo(list2);
        assertEquals(true, binding.get());
    }


    @Test
    public void testIsNotEqualTo() {
        final ObservableList<Integer> emptyList = FXCollections.emptyObservableList();
        final ObservableList<Integer> list1 = FXCollections.observableArrayList(data1_0);
        final ObservableList<Integer> list2 = FXCollections.observableArrayList(data2_0, data2_1);

        BooleanBinding binding = opNull.isNotEqualTo(emptyList);
        assertEquals(true, binding.get());
        binding = opNull.isNotEqualTo(list1);
        assertEquals(true, binding.get());
        binding = opNull.isNotEqualTo(list2);
        assertEquals(true, binding.get());

        binding = opEmpty.isNotEqualTo(emptyList);
        assertEquals(false, binding.get());
        binding = opEmpty.isNotEqualTo(list1);
        assertEquals(true, binding.get());
        binding = opEmpty.isNotEqualTo(list2);
        assertEquals(true, binding.get());

        binding = op1.isNotEqualTo(emptyList);
        assertEquals(true, binding.get());
        binding = op1.isNotEqualTo(list1);
        assertEquals(false, binding.get());
        binding = op1.isNotEqualTo(list2);
        assertEquals(true, binding.get());

        binding = op2.isNotEqualTo(emptyList);
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
        assertEquals(Collections.emptyList().toString(), opEmpty.asString().get());
        assertEquals(Arrays.asList(data1_0).toString(), op1.asString().get());
        assertEquals(Arrays.asList(data2_0, data2_1).toString(), op2.asString().get());
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
        assertEquals(data2_0, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(data2_1, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testToArray_NoArg() {
        assertArrayEquals(new Object[0], opNull.toArray());
        assertArrayEquals(new Object[0], opEmpty.toArray());
        assertArrayEquals(new Object[] {data1_0}, op1.toArray());
        assertArrayEquals(new Object[] {data2_0, data2_1}, op2.toArray());
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
        assertArrayEquals(new Integer[] {data2_0, data2_1}, arrayOut);
    }
}
