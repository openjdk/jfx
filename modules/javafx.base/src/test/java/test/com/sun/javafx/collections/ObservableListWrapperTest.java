/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.collections;

import javafx.collections.ObservableList;

import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.collections.ObservableListWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ObservableListWrapperTest {

    @Test(expected = NullPointerException.class)
    public void testRemoveAll_Null() {
        ObservableList<Integer> list = new ObservableListWrapper<>(new ArrayList<>());
        list.removeAll((Collection)null);
    }

    @Test(expected = NullPointerException.class)
    public void testRetainAll_Null() {
        ObservableList<Integer> list = new ObservableListWrapper<>(new ArrayList<>());
        list.retainAll((Collection)null);
    }

    @Test
    public void testRemoveAll_Empty() {
        ObservableList<Integer> list = new ObservableListWrapper<>(new ArrayList<>());
        assertFalse(list.removeAll(Collections.EMPTY_LIST));
        assertFalse(list.removeAll(Collections.EMPTY_SET));
    }

    @Test
    public void testRetainAll_Empty() {
        ObservableList<Integer> list = new ObservableListWrapper<>(new ArrayList<>());
        assertFalse(list.retainAll(Collections.EMPTY_LIST));
        assertFalse(list.retainAll(Collections.EMPTY_SET));
    }

    @Test
    public void testRemoveAll_Args() {
        ObservableList<Integer> list = new ObservableListWrapper<>(new ArrayList<>(Arrays.asList(1, 2, 3)));
        assertFalse(list.removeAll(0));
        assertEquals(3, list.size());
        assertTrue(list.removeAll(1));
        assertEquals(2, list.size());
        assertFalse(list.removeAll(1));
        assertEquals(2, list.size());
        assertTrue(list.removeAll(1, 2));
        assertEquals(1, list.size());
        assertTrue(list.removeAll(3));
        assertEquals(0, list.size());
        assertFalse(list.removeAll(Collections.EMPTY_SET));
        assertFalse(list.removeAll(1));
        assertFalse(list.removeAll(1, 2));
    }

    @Test
    public void testRetainAll_Args() {
        ObservableList<Integer> list = new ObservableListWrapper<>(new ArrayList<>(Arrays.asList(1, 2, 3)));
        assertFalse(list.retainAll(0, 1, 2, 3));
        assertEquals(3, list.size());
        assertFalse(list.retainAll(1, 2, 3));
        assertEquals(3, list.size());
        assertTrue(list.retainAll(2, 3));
        assertEquals(2, list.size());
        assertTrue(list.retainAll(3));
        assertEquals(1, list.size());
        assertTrue(list.retainAll(1,2));
        assertEquals(0, list.size());
        assertFalse(list.retainAll(Collections.EMPTY_SET));
        assertFalse(list.retainAll(2,3));
        assertFalse(list.retainAll(3));

        list = new ObservableListWrapper<>(new ArrayList<>(Arrays.asList(1, 2, 3)));
        assertTrue(list.retainAll(Collections.EMPTY_SET));
        assertEquals(0, list.size());
    }
}
