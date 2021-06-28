/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.collections.MappingChange;
import com.sun.javafx.collections.NonIterableChange;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class MappingChangeTest {

    ObservableList<Integer> originalList;
    ObservableList<String> list;

    @Before
    public void setUp() {
        originalList = FXCollections.observableArrayList(1, 2, 3, 4, 5);
        list = FXCollections.observableArrayList("1", "2", "3", "4", "5");
    }

    @Test
    public void testAddRemove() {
        Change<Integer> change = new NonIterableChange.SimpleRemovedChange<Integer>(0, 1, Integer.valueOf(5), originalList);
        MappingChange<Integer, String> mapChange = new MappingChange<Integer, String>(change,
                e -> e.toString(), list);

        assertTrue(mapChange.next());
        assertEquals(0, mapChange.getFrom());
        assertEquals(1, mapChange.getTo());
        assertEquals(Arrays.asList("5"), mapChange.getRemoved());
        assertNotNull(mapChange.toString());

        assertFalse(mapChange.next());
    }

    @Test
    public void testUpdate() {
        Change<Integer> change = new NonIterableChange.SimpleUpdateChange<Integer>(0, 1, originalList);
        MappingChange<Integer, String> mapChange = new MappingChange<Integer, String>(change,
                e -> e.toString(), list);

        assertTrue(mapChange.next());
        assertEquals(0, mapChange.getFrom());
        assertEquals(1, mapChange.getTo());
        assertTrue(mapChange.wasUpdated());
        assertNotNull(mapChange.toString());

        assertFalse(mapChange.next());
    }

    @Test
    public void testPermutation() {
        Change<Integer> change = new NonIterableChange.SimplePermutationChange<Integer>(0, 2, new int[] {1, 0}, originalList);
        MappingChange<Integer, String> mapChange = new MappingChange<Integer, String>(change,
                e -> e.toString(), list);

        assertTrue(mapChange.next());
        assertEquals(0, mapChange.getFrom());
        assertEquals(2, mapChange.getTo());
        assertTrue(mapChange.wasPermutated());
        assertNotNull(mapChange.toString());

        assertFalse(mapChange.next());
    }

    @Test
    public void testComplex() {
        Change<Integer> change = new Change(originalList) {
            int[][] added= new int[][]{ new int[] {0, 1}, new int[] {2, 3}, new int[] {4, 5}};

            int pointer = -1;

            @Override
            public boolean next() {
                if (pointer == added.length - 1) {
                    return false;
                }
                ++pointer;
                return true;
            }

            @Override
            public void reset() {
                pointer = -1;
            }

            @Override
            public int getFrom() {
                return added[pointer][0];
            }

            @Override
            public int getTo() {
                return added[pointer][1];
            }

            @Override
            public List getRemoved() {
                return Collections.EMPTY_LIST;
            }

            @Override
            protected int[] getPermutation() {
                return new int[0];
            }
        };
        MappingChange<Integer, String> mapChange = new MappingChange<Integer, String>(change,
                e -> e.toString(), list);

        assertTrue(mapChange.next());
        assertEquals(0, mapChange.getFrom());
        assertEquals(1, mapChange.getTo());
        assertTrue(mapChange.wasAdded());
        assertNotNull(mapChange.toString());

        assertTrue(mapChange.next());
        assertEquals(2, mapChange.getFrom());
        assertEquals(3, mapChange.getTo());
        assertTrue(mapChange.wasAdded());
        assertNotNull(mapChange.toString());

        assertTrue(mapChange.next());
        assertEquals(4, mapChange.getFrom());
        assertEquals(5, mapChange.getTo());
        assertTrue(mapChange.wasAdded());
        assertNotNull(mapChange.toString());

        assertFalse(mapChange.next());
    }
}
