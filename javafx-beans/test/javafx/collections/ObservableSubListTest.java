/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.collections;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests for sublists of ObservableList.
 * 
 */
@RunWith(Parameterized.class)
public class ObservableSubListTest {
    final Callable<ObservableList<String>> listFactory;
    ObservableList<String> list;
    List<String> sublist;
    private MockListObserver<String> mlo;

    public ObservableSubListTest(final Callable<ObservableList<String>> listFactory) {
        this.listFactory = listFactory;
    }

    @Parameterized.Parameters
    public static Collection createParameters() {
        Object[][] data = new Object[][] {
            { TestedObservableLists.ARRAY_LIST },
            { TestedObservableLists.LINKED_LIST },
            { TestedObservableLists.VETOABLE_LIST },
            { TestedObservableLists.CHECKED_OBSERVABLE_ARRAY_LIST },
            { TestedObservableLists.SYNCHRONIZED_OBSERVABLE_ARRAY_LIST }
        };
        return Arrays.asList(data);
    }

    @Before
    public void setup() throws Exception {
        list = listFactory.call();
        mlo = new MockListObserver<String>();
        list.addListener(mlo);
        useListData("a", "b", "c", "d", "e", "f");
        sublist = list.subList(1, 5);
    }

    /**
     * Modifies the list in the fixture to use the strings passed in instead of
     * the default strings, and re-creates the observable list and the observer.
     * If no strings are passed in, the result is an empty list.
     *
     * @param strings the strings to use for the list in the fixture
     */
    void useListData(String... strings) {
        list.clear();
        list.addAll(Arrays.asList(strings));
        mlo.clear();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadRange() {
        list.subList(3, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRangeTooLow() {
        list.subList(-2, 4);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRangeTooHigh() {
        list.subList(3, 7);
    }

    @Test
    public void testWidestRange() {
        List<String> sub = list.subList(0, 6);
        assertEquals("[a, b, c, d, e, f]", sub.toString());
    }

    @Test
    public void testAdd() {
        sublist.add("X");
        assertEquals("[b, c, d, e, X]", sublist.toString());
        assertEquals("[a, b, c, d, e, X, f]", list.toString());
        assertEquals(5, sublist.size());
        mlo.check1AddRemove(list, null, 5, 6);
    }

    @Test
    public void testAddAll() {
        sublist.addAll(1, Arrays.asList("X", "Y", "Z"));
        assertEquals("[b, X, Y, Z, c, d, e]", sublist.toString());
        assertEquals("[a, b, X, Y, Z, c, d, e, f]", list.toString());
        assertEquals(7, sublist.size());
        mlo.check1AddRemove(list, null, 2, 5);
    }

    @Test
    public void testClear() {
        sublist.clear();
        assertEquals("[]", sublist.toString());
        assertEquals("[a, f]", list.toString());
        assertEquals(0, sublist.size());
        mlo.check1AddRemove(list, Arrays.asList("b", "c", "d", "e"), 1, 1);
    }

    @Test
    public void testContains() {
        assertTrue(sublist.contains("c"));
        assertFalse(sublist.contains("a"));
        assertFalse(sublist.contains(null));
        assertFalse(sublist.contains(Integer.valueOf(7)));
    }

    @Test
    public void testContainsAll() {
        assertTrue(sublist.containsAll(Arrays.asList("b", "c")));
        assertFalse(sublist.containsAll(Arrays.asList("a", "b")));
    }

    @Test
    public void testContainsNull() {
        list.add(3, null);
        sublist = list.subList(1, 5);
        assertTrue(sublist.contains(null));
    }

    @Test
    public void testEqualsOnAnotherType() {
        assertFalse(sublist.equals(Integer.valueOf(7)));
    }

    @Test
    public void testEqualsOnLongerList() {
        List<String> other = Arrays.asList("b", "c", "d", "e", "f");
        assertFalse(sublist.equals(other));
        assertTrue(other.hashCode() != sublist.hashCode());
    }

    @Test
    public void testEqualsOnShorterList() {
        List<String> other = Arrays.asList("b", "c", "d");
        assertFalse(sublist.equals(other));
        assertTrue(other.hashCode() != sublist.hashCode());
    }

    @Test
    public void testEquals() {
        List<String> other = Arrays.asList("b", "c", "d", "e");
        assertTrue(sublist.equals(other));
        assertEquals(other.hashCode(), sublist.hashCode());
    }

    @Test
    public void testEqualsWithNull() {
        sublist.add(2, null);
        List<String> other = Arrays.asList("b", "c", null, "e");
        assertFalse(sublist.equals(other));
        assertTrue(other.hashCode() != sublist.hashCode());
    }

    @Test
    public void testEqualsWithNullOnLongerList() {
        sublist.add(2, null);
        List<String> other = Arrays.asList("b", "c", null, "d", "e");
        assertTrue(sublist.equals(other));
        assertEquals(other.hashCode(), sublist.hashCode());
    }

    @Test
    public void testEqualsWithNullOnShorterList() {
        sublist.add(2, null);
        List<String> other = Arrays.asList("b", "c", null);
        assertFalse(sublist.equals(other));
        assertTrue(other.hashCode() != sublist.hashCode());
    }

    @Test
    public void testIndexOf() {
        assertEquals(2, sublist.indexOf("d"));
        assertEquals(-1, sublist.indexOf("a"));
    }

    @Test
    public void testIndexOfWithNull() {
        sublist.add(2, null);
        assertEquals(3, sublist.indexOf("d"));
        assertEquals(2, sublist.indexOf(null));
        assertEquals(-1, sublist.indexOf("f"));
    }

    @Test
    public void testIsEmpty() {
        assertFalse(sublist.isEmpty());
        List<String> otherSublist = list.subList(2, 2);
        assertTrue(otherSublist.isEmpty());
    }

    @Test
    public void testLastIndexOf() {
        list = FXCollections.observableList(new ArrayList<String>());
        list.addAll(Arrays.asList("a", null, "a", null, "a", null, "a"));
        sublist = list.subList(1, 5);
        
        assertEquals(3, sublist.lastIndexOf("a"));
        assertEquals(2, sublist.lastIndexOf(null));
    }

    @Test
    public void testRemoveAll() {
        list = FXCollections.observableList(new ArrayList<String>());
        list.addAll(Arrays.asList("a", "b", "c", "a", "b", "c"));
        sublist = list.subList(2, 4);
        list.addListener(mlo);
        sublist.removeAll(Arrays.asList("a", "b", "c"));

        assertEquals("[]", sublist.toString());
        assertEquals(0, sublist.size());
        assertEquals("[a, b, b, c]", list.toString());
        mlo.check1AddRemove(list, Arrays.asList("c", "a"), 2, 2);
    }

    @Test
    public void testRemoveIndex() {
        String s = sublist.remove(2);
        assertEquals("d", s);
        assertEquals(3, sublist.size());
        assertEquals("[b, c, e]", sublist.toString());
        assertEquals("[a, b, c, e, f]", list.toString());
        mlo.check1AddRemove(list, Collections.singletonList("d"), 3, 3);
    }

    @Test
    public void testRemoveNull() {
        sublist.add(2, null);
        assertTrue(sublist.remove(null));
        assertEquals(4, sublist.size());
        assertEquals("[b, c, d, e]", sublist.toString());
        assertEquals("[a, b, c, d, e, f]", list.toString());
    }

    @Test
    public void testRemoveObjectExists() {
        assertTrue(sublist.remove("b"));
        assertEquals(3, sublist.size());
        assertEquals("[c, d, e]", sublist.toString());
        assertEquals("[a, c, d, e, f]", list.toString());
        mlo.check1AddRemove(list, Collections.singletonList("b"), 1, 1);
    }

    @Test
    public void testRemoveObjectNotExists() {
        assertFalse(sublist.remove("f"));
        assertEquals(4, sublist.size());
        assertEquals("[b, c, d, e]", sublist.toString());
        assertEquals("[a, b, c, d, e, f]", list.toString());
        mlo.check0();
    }

    @Test
    public void testRetainAll() {
        list = FXCollections.observableList(new ArrayList<String>());
        list.addAll(Arrays.asList("a", "b", "c", "a", "b", "c"));
        list.addListener(mlo);
        sublist = list.subList(2, 4);
        sublist.retainAll(Arrays.asList("c", "b"));

        assertEquals("[c]", sublist.toString());
        assertEquals(1, sublist.size());
        assertEquals("[a, b, c, b, c]", list.toString());
        mlo.check1AddRemove(list, Collections.singletonList("a"), 3, 3);
    }

    @Test
    public void testSet() {
        String s = sublist.set(2, "X");
        assertEquals("d", s);
        assertEquals("[b, c, X, e]", sublist.toString());
        assertEquals("[a, b, c, X, e, f]", list.toString());
        mlo.check1AddRemove(list, Collections.singletonList("d"), 3, 4);
    }

    @Test
    public void testSize() {
        assertEquals(4, sublist.size());
        List<String> otherSublist = list.subList(3, 3);
        assertEquals(0, otherSublist.size());
    }

    @Test
    public void testSubSubList() {
        List<String> subsublist = sublist.subList(1, 3);
        assertEquals(2, subsublist.size());
        assertEquals("[c, d]", subsublist.toString());
    }

    @Test
    public void testSubSubListAdd() {
        List<String> subsublist = sublist.subList(1, 3);
        subsublist.add(1, "X");
        // sublist is now invalid
        assertEquals("[c, X, d]", subsublist.toString());
        assertEquals("[a, b, c, X, d, e, f]", list.toString());
    }

    @Test
    public void testSubSubListRemove() {
        List<String> subsublist = sublist.subList(1, 3);
        assertEquals("c", subsublist.remove(0));
        // sublist is now invalid
        assertEquals("[d]", subsublist.toString());
        assertEquals("[a, b, d, e, f]", list.toString());
    }

    @Test
    public void testSubSubListSet() {
        List<String> subsublist = sublist.subList(1, 3);
        String s = subsublist.set(1, "X");
        assertEquals("d", s);
        assertEquals("[c, X]", subsublist.toString());
        assertEquals("[b, c, X, e]", sublist.toString());
        assertEquals("[a, b, c, X, e, f]", list.toString());
    }

    @Test
    public void testToString() {
        List<String> sub0 = list.subList(3, 3);
        List<String> sub1 = list.subList(3, 4);
        List<String> sub2 = list.subList(2, 5);

        assertEquals("[]", sub0.toString());
        assertEquals("[d]", sub1.toString());
        assertEquals("[c, d, e]", sub2.toString());
    }

    @Test
    public void testConcurrencyGet() {
        list.add("x");
        try { sublist.get(0); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyAdd() {
        list.add("x");
        try { sublist.add("y"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyAddAll() {
        list.add("x");
        try { sublist.addAll(Collections.singleton("y")); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyClear() {
        list.add("x");
        try { sublist.addAll(Collections.singleton("y")); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyContains() {
        list.add("x");
        try { sublist.contains("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyContainsAll() {
        list.add("x");
        try { sublist.containsAll(Collections.singletonList("x")); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    
    @Test
    public void testConcurrencyIsEmpty() {
        list.add("x");
        try { sublist.isEmpty(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyIterator() {
        list.add("x");
        try { sublist.iterator().next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyRemove() {
        list.add("x");
        try { sublist.remove("y"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyRemove0() {
        list.add("x");
        try { sublist.remove(0); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencySet() {
        list.add("x");
        try { sublist.set(0, "y"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyRemoveAll() {
        list.add("x");
        try { sublist.removeAll(Arrays.asList("c", "d")); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    
    @Test
    public void testConcurrencyRetainAll() {
        list.add("x");
        try { sublist.retainAll(Arrays.asList("c", "d")); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyRetainSize() {
        list.add("x");
        try { sublist.size(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
}
