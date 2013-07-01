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

package com.sun.javafx.binding;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ContentBindingMapTest {
    
    private static final String key1 = "Key1";
    private static final String key2_1 = "Key2_1";
    private static final String key2_2 = "Key2_2";

    private Map<String, Integer> op1;
    private ObservableMap<String, Integer> op2;
    private ObservableMap<String, Integer> op3;
    private Map<String, Integer> map0;
    private Map<String, Integer> map1;
    private Map<String, Integer> map2;

    @Before
    public void setUp() {
        map0 = new HashMap<String, Integer>();
        map1 = new HashMap<String, Integer>();
        map1.put(key1, -1);
        map2 = new HashMap<String, Integer>();
        map2.put(key2_1, 2);
        map2.put(key2_2, 1);

        op1 = new HashMap<String, Integer>(map1);
        op2 = FXCollections.observableMap(map2);
        op3 = FXCollections.observableMap(map0);
    }

    @Test
    public void testBind() {
        Bindings.bindContent(op1, op2);
        System.gc(); // making sure we did not not overdo weak references
        assertEquals(map2, op1);
        assertEquals(map2, op2);

        op2.clear();
        op2.putAll(map1);
        assertEquals(map1, op1);
        assertEquals(map1, op2);

        op2.clear();
        op2.putAll(map0);
        assertEquals(map0, op1);
        assertEquals(map0, op2);

        op2.clear();
        op2.putAll(map2);
        assertEquals(map2, op1);
        assertEquals(map2, op2);
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
        assertEquals(map2, op1);
        assertEquals(map2, op2);

        Bindings.unbindContent(op1, op2);
        System.gc();
        assertEquals(map2, op1);
        assertEquals(map2, op2);

        op1.clear();
        assertEquals(map0, op1);
        assertEquals(map2, op2);

        op2.clear();
        op2.putAll(map1);
        assertEquals(map0, op1);
        assertEquals(map1, op2);
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
        assertEquals(map0, op1);
        assertEquals(map0, op2);
        assertEquals(map0, op3);

        op3.clear();
        op3.putAll(map1);
        assertEquals(map1, op1);
        assertEquals(map1, op2);
        assertEquals(map1, op3);

        // now unbind 
        Bindings.unbindContent(op1, op2);
        System.gc(); // making sure we did not not overdo weak references
        assertEquals(map1, op1);
        assertEquals(map1, op2);
        assertEquals(map1, op3);

        op3.clear();
        op3.putAll(map2);
        assertEquals(map1, op1);
        assertEquals(map2, op2);
        assertEquals(map2, op3);
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
