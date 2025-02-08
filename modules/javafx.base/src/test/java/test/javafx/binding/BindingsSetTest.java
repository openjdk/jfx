/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.binding;

import javafx.beans.binding.*;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BindingsSetTest {

    private static final Object data1 = new Object();
    private static final Object data2 = new Object();

    private SetProperty<Object> property;
    private ObservableSet<Object> set1;
    private ObservableSet<Object> set2;

    @BeforeEach
    public void setUp() {
        property = new SimpleSetProperty<>();
        set1 = FXCollections.observableSet(data1, data2);
        set2 = FXCollections.observableSet();
    }

    @Test
    public void testSize() {
        final IntegerBinding size = Bindings.size(property);
        DependencyUtils.checkDependencies(size.getDependencies(), property);

        assertEquals(0, size.get());
        property.set(set1);
        assertEquals(2, size.get());
        set1.remove(data2);
        assertEquals(1, size.get());
        property.set(set2);
        assertEquals(0, size.get());
        property.add(data2);
        property.add(data2);
        assertEquals(1, size.get());
        property.set(null);
        assertEquals(0, size.get());
    }

    @Test
    public void testSize_Null() {
        assertThrows(NullPointerException.class, () -> {
            Bindings.size((ObservableSet<Object>) null);
        });
    }


    @Test
    public void testIsEmpty() {
        final BooleanBinding empty = Bindings.isEmpty(property);
        DependencyUtils.checkDependencies(empty.getDependencies(), property);

        assertTrue(empty.get());
        property.set(set1);
        assertFalse(empty.get());
        set1.remove(data2);
        assertFalse(empty.get());
        property.set(set2);
        assertTrue(empty.get());
        property.add(data2);
        property.add(data2);
        assertFalse(empty.get());
        property.set(null);
        assertTrue(empty.get());
    }

    @Test
    public void testIsEmpty_Null() {
        assertThrows(NullPointerException.class, () -> {
            Bindings.isEmpty((ObservableSet<Object>) null);
        });
    }


    @Test
    public void testIsNotEmpty() {
        final BooleanBinding notEmpty = Bindings.isNotEmpty(property);
        DependencyUtils.checkDependencies(notEmpty.getDependencies(), property);

        assertFalse(notEmpty.get());
        property.set(set1);
        assertTrue(notEmpty.get());
        set1.remove(data2);
        assertTrue(notEmpty.get());
        property.set(set2);
        assertFalse(notEmpty.get());
        property.add(data2);
        property.add(data2);
        assertTrue(notEmpty.get());
        property.set(null);
        assertFalse(notEmpty.get());
    }

    @Test
    public void testIsNotEmpty_Null() {
        assertThrows(NullPointerException.class, () -> {
            Bindings.isNotEmpty((ObservableSet<Object>) null);
        });
    }

}
