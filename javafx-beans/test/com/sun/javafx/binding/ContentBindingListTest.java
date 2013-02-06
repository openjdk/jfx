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
package com.sun.javafx.binding;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ContentBindingListTest {

    private List<Integer> op1;
    private ObservableList<Integer> op2;
    private ObservableList<Integer> op3;
    private List<Integer> list0;
    private List<Integer> list1;
    private List<Integer> list2;

    @Before
    public void setUp() {
        list0 = new ArrayList<Integer>();
        list1 = new ArrayList<Integer>(Arrays.asList(0));
        list2 = new ArrayList<Integer>(Arrays.asList(2, 1));

        op1 = new ArrayList<Integer>(list1);
        op2 = FXCollections.observableArrayList(list2);
        op3 = FXCollections.observableArrayList(list0);
    }

    @Test
    public void testBind() {
        List<Integer> list2_sorted = new ArrayList<Integer>(Arrays.asList(1, 2));

        Bindings.bindContent(op1, op2);
        System.gc(); // making sure we did not not overdo weak references
        assertEquals(list2, op1);
        assertEquals(list2, op2);

        op2.setAll(list1);
        assertEquals(list1, op1);
        assertEquals(list1, op2);

        op2.setAll(list0);
        assertEquals(list0, op1);
        assertEquals(list0, op2);

        op2.setAll(list2);
        assertEquals(list2, op1);
        assertEquals(list2, op2);

        FXCollections.sort(op2);
        assertEquals(list2_sorted, op1);
        assertEquals(list2_sorted, op2);
    }

    @Test(expected = NullPointerException.class)
    public void testBind_Null_X() {
        Bindings.bindContent(null, op2);
    }

    @Test(expected = NullPointerException.class)
    public void testBind_X_Null() {
        Bindings.bindContent(op1, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBind_X_Self() {
        Bindings.bindContent(op2, op2);
    }

    @Test
    public void testUnbind() {
        // unbind non-existing binding => no-op
        Bindings.unbindContent(op1, op2);

        Bindings.bindContent(op1, op2);
        System.gc(); // making sure we did not not overdo weak references
        assertEquals(list2, op1);
        assertEquals(list2, op2);

        Bindings.unbindContent(op1, op2);
        System.gc();
        assertEquals(list2, op1);
        assertEquals(list2, op2);

        op1.clear();
        assertEquals(list0, op1);
        assertEquals(list2, op2);

        op2.setAll(list1);
        assertEquals(list0, op1);
        assertEquals(list1, op2);
    }

    @Test(expected = NullPointerException.class)
    public void testUnbind_Null_X() {
        Bindings.unbindContent(null, op2);
    }

    @Test(expected = NullPointerException.class)
    public void testUnbind_X_Null() {
        Bindings.unbindContent(op1, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnbind_X_Self() {
        Bindings.unbindContent(op2, op2);
    }

    @Test
    public void testChaining() {
        Bindings.bindContent(op1, op2);
        Bindings.bindContent(op2, op3);
        System.gc(); // making sure we did not not overdo weak references
        assertEquals(list0, op1);
        assertEquals(list0, op2);
        assertEquals(list0, op3);

        op3.setAll(list1);
        assertEquals(list1, op1);
        assertEquals(list1, op2);
        assertEquals(list1, op3);

        // now unbind 
        Bindings.unbindContent(op1, op2);
        System.gc(); // making sure we did not not overdo weak references
        assertEquals(list1, op1);
        assertEquals(list1, op2);
        assertEquals(list1, op3);

        op3.setAll(list2);
        assertEquals(list1, op1);
        assertEquals(list2, op2);
        assertEquals(list2, op3);
    }

    @Test
    public void testHashCode() {
        final int hc1 = ContentBinding.bind(op1, op2).hashCode();
        ContentBinding.unbind(op1, op2);
        final int hc2 = ContentBinding.bind(op1, op2).hashCode();
        assertEquals(hc1, hc2);
    }

    @Test
    public void testEquals() {
        final Object golden = ContentBinding.bind(op1, op2);
        ContentBinding.unbind(op1, op2);

        assertTrue(golden.equals(golden));
        assertFalse(golden.equals(null));
        assertFalse(golden.equals(op1));
        assertTrue(golden.equals(ContentBinding.bind(op1, op2)));
        ContentBinding.unbind(op1, op2);
        assertFalse(golden.equals(ContentBinding.bind(op3, op2)));
        ContentBinding.unbind(op2, op3);
        assertFalse(golden.equals(ContentBinding.bind(op2, op3)));
        ContentBinding.unbind(op2, op3);
    }

    @Test
    public void testEqualsWithGCedProperty() {
        final Object binding1 = ContentBinding.bind(op1, op2);
        ContentBinding.unbind(op1, op2);
        final Object binding2 = ContentBinding.bind(op1, op2);
        ContentBinding.unbind(op1, op2);
        op1 = null;
        System.gc();

        assertTrue(binding1.equals(binding1));
        assertFalse(binding1.equals(binding2));
    }
}
