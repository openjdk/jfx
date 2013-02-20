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

package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.scene.control.skin.VirtualFlow.ArrayLinkedList;

public class ArrayLinkedListTest {
    private ArrayLinkedList<String> list;
    private String a = "a";
    private String b = "b";
    private String c = "c";

    @Before public void setUp() {
        list = new ArrayLinkedList<String>();
    }
    
    @Test public void testArrayLinkedList_Empty_GetFirstReturnsNull() {
        assertNull(list.getFirst());
    }

    @Test public void testArrayLinkedList_Empty_GetLastReturnsNull() {
        assertNull(list.getLast());
    }

    @Test public void testArrayLinkedList_Empty_AddFirst() {
        list.addFirst(a);
        assertEquals(1, list.size());
        assertEquals(a, list.getFirst());
        assertEquals(a, list.getLast());
        assertEquals(a, list.get(0));
        assertFalse(list.isEmpty());
    }

    @Test public void testArrayLinkedList_Empty_AddLast() {
        list.addLast(c);
        assertEquals(1, list.size());
        assertEquals(c, list.getFirst());
        assertEquals(c, list.getLast());
        assertEquals(c, list.get(0));
        assertFalse(list.isEmpty());
    }

    @Test public void testArrayLinkedList_Empty_SizeIsZero() {
        assertEquals(0, list.size());
    }

    @Test public void testArrayLinkedList_Empty_IsEmpty() {
        assertTrue(list.isEmpty());
    }

    @Test public void testArrayLinkedList_Empty_ClearHasNoEffect() {
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test public void testArrayLinkedList_Empty_GetResultsInArrayIndexOutOfBounds() {
        try {
            list.get(0);
            assertTrue("get didn't return an ArrayIndexOutOfBoundsException", false);
        } catch (ArrayIndexOutOfBoundsException e) {
            assertTrue(true);
        }
    }

    @Test public void testArrayLinkedList_Empty_RemoveFirstIsNoOp() {
        list.removeFirst();
    }

    @Test public void testArrayLinkedList_Empty_RemoveLastIsNoOp() {
        list.removeLast();
    }

    @Test public void testArrayLinkedList_Empty_RemoveResultsInArrayIndexOutOfBounds() {
        try {
            list.remove(0);
            assertTrue("remove didn't return an ArrayIndexOutOfBoundsException", false);
        } catch (ArrayIndexOutOfBoundsException e) {
            assertTrue(true);
        }
    }

    @Test public void testArrayLinkedList_GetFirst_AfterAddLast() {
        list.addLast(a);
        list.addLast(b);
        list.addLast(c);
        assertEquals(a, list.getFirst());
    }

    @Test public void testArrayLinkedList_GetFirst_AfterAddFirst() {
        list.addFirst(c);
        list.addFirst(b);
        list.addFirst(a);
        assertEquals(a, list.getFirst());
    }

    @Test public void testArrayLinkedList_GetFirst_AfterAddFirstAndAddLast() {
        list.addFirst(b);
        list.addLast(c);
        list.addFirst(a);
        assertEquals(a, list.getFirst());
    }

    @Test public void testArrayLinkedList_GetFirst_AfterRemoveFirst() {
        list.addLast(a);
        list.addLast(b);
        list.addLast(c);
        list.removeFirst();
        assertEquals(b, list.getFirst());
        list.removeFirst();
        assertEquals(c, list.getFirst());
        list.removeFirst();
        assertNull(list.getFirst());
    }

    @Test public void testArrayLinkedList_GetFirst_AfterRemoveLast() {
        list.addLast(a);
        list.addLast(b);
        list.addLast(c);
        list.removeLast();
        assertEquals(a, list.getFirst());
        list.removeLast();
        assertEquals(a, list.getFirst());
        list.removeLast();
        assertNull(list.getFirst());
    }

    @Test public void testArrayLinkedList_GetLast_AfterAddLast() {
        list.addLast(a);
        list.addLast(b);
        list.addLast(c);
        assertEquals(c, list.getLast());
    }

    @Test public void testArrayLinkedList_GetLast_AfterAddFirst() {
        list.addFirst(c);
        list.addFirst(b);
        list.addFirst(a);
        assertEquals(c, list.getLast());
    }

    @Test public void testArrayLinkedList_GetLast_AfterAddFirstAndAddLast() {
        list.addFirst(b);
        list.addLast(c);
        list.addFirst(a);
        assertEquals(c, list.getLast());
    }

    @Test public void testArrayLinkedList_GetLast_AfterRemoveFirst() {
        list.addLast(a);
        list.addLast(b);
        list.addLast(c);
        list.removeFirst();
        assertEquals(c, list.getLast());
        list.removeFirst();
        assertEquals(c, list.getLast());
        list.removeFirst();
        assertNull(list.getLast());
    }

    @Test public void testArrayLinkedList_GetLast_AfterRemoveLast() {
        list.addLast(a);
        list.addLast(b);
        list.addLast(c);
        list.removeLast();
        assertEquals(b, list.getLast());
        list.removeLast();
        assertEquals(a, list.getLast());
        list.removeLast();
        assertNull(list.getLast());
    }

    @Test public void testArrayLinkedList_Get_AfterAddLast() {
        list.addLast(a);
        list.addLast(b);
        list.addLast(c);
        assertEquals(a, list.get(0));
        assertEquals(b, list.get(1));
        assertEquals(c, list.get(2));
    }

    @Test public void testArrayLinkedList_Get_AfterAddFirst() {
        list.addFirst(c);
        list.addFirst(b);
        list.addFirst(a);
        assertEquals(a, list.get(0));
        assertEquals(b, list.get(1));
        assertEquals(c, list.get(2));
    }

    @Test public void testArrayLinkedList_Get_AfterAddFirstAndAddLast() {
        list.addFirst(b);
        list.addLast(c);
        list.addFirst(a);
        assertEquals(a, list.get(0));
        assertEquals(b, list.get(1));
        assertEquals(c, list.get(2));
    }

    @Test public void testArrayLinkedList_Get_AfterRemoveFirst() {
        list.addLast(a);
        list.addLast(b);
        list.addLast(c);
        list.removeFirst();
        assertEquals(b, list.get(0));
        assertEquals(c, list.get(1));
        list.removeFirst();
        assertEquals(c, list.get(0));
    }

    @Test public void testArrayLinkedList_Get_AfterRemoveLast() {
        list.addLast(a);
        list.addLast(b);
        list.addLast(c);
        list.removeLast();
        assertEquals(a, list.get(0));
        assertEquals(b, list.get(1));
        list.removeLast();
        assertEquals(a, list.get(0));
    }

//    // using a combination of addFirst, addLast, removeFirst, removeLast,
//    // and remove calls, check the status of the list and ensure it is
//    // always in the expected state
//    @Test public void testArrayLinkedList_StressTest() {
//        // This is our "control" in the classical scientific testing sense.
//        // If the values / sizes / results in the list match the control, then
//        // we know the VirtualFlow.ArrayLinkedList implementation is working
//        List control = new LinkedList();
//        // Iterate a bunch of times
//        for (int i = 0; i < 5000; i++) {
//            // there are 1 of 6 different possible outcomes here:
//            //  1) addFirst
//            //  2) addLast
//            //  3) removeFirst
//            //  4) removeLast
//            //  5) remove
//            //  6) clear
//            // but since I want to clear very infrequently, I weight everything
//            // between 0 and 60 and the "clear" option has to be an even 59.
//            int choice = (int) (Math.random() * 50);
//            if (choice < 10) {
//                char input = Character.toChars((Math.random() * 26) + 65)[0];
//                IndexedCell cell = new CellStub("input");
//                control.addFirst(cell);
//                list.addFirst(cell);
//                assertMatch(control, list);
//            } else if (choice < 20) {
//                char input = Character.toChars((Math.random() * 26) + 65)[0];
//                IndexedCell cell = new CellStub("input");
//                control.addLast(cell);
//                list.addLast(cell);
//                assertMatch(control, list);
//            } else if (choice < 30) {
//                if (control.size() == 0) continue;
//                IndexedCell cell = (IndexedCell) control.removeFirst();
//                IndexedCell cell2 = list.removeFirst();
//                assertSame(cell, cell2);
//                assertMatch(control, list);
//            } else if (choice < 40) {
//                if (control.size() == 0) continue;
//                IndexedCell cell = control.removeLast();
//                IndexedCell cell2 = list.removeLast();
//                assertSame(cell, cell2);
//                assertMatch(control, list);
//            } else if (choice < 49) {
//                if (control.size() == 0) continue;
//                int index = (Math.random() * control.size());
//                if (index == control.size()) index--;
//                IndexedCell cell = control.remove(index);
//                def cell2 = list.remove(index);
//                assertSame(cell, cell2);
//                assertMatch(control, list);
//            } else if (choice < 50) {
//                control.clear();
//                list.clear();
//                assertMatch(control, list);
//            }
//        }
//    }
}
