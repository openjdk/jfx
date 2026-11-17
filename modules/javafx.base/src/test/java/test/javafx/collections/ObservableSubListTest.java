/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for sublists of ObservableList.
 *
 */
public class ObservableSubListTest {
    Callable<ObservableList<String>> listFactory;
    ObservableList<String> list;
    List<String> sublist;
    private MockListObserver<String> mlo;

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

    private void setup(Callable<ObservableList<String>> listFactory) throws Exception {
        list = listFactory.call();
        mlo = new MockListObserver<>();
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

    @ParameterizedTest
    @MethodSource("createParameters")
    void testBadRange(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        assertThrows(IllegalArgumentException.class, () -> list.subList(3, 2));
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testRangeTooLow(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        assertThrows(IndexOutOfBoundsException.class, () -> list.subList(-2, 4));
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testRangeTooHigh(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        assertThrows(IndexOutOfBoundsException.class, () -> list.subList(3, 7));
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testWidestRange(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        List<String> sub = list.subList(0, 6);
        assertEquals("[a, b, c, d, e, f]", sub.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAdd(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        sublist.add("X");
        assertEquals("[b, c, d, e, X]", sublist.toString());
        assertEquals("[a, b, c, d, e, X, f]", list.toString());
        assertEquals(5, sublist.size());
        mlo.check1AddRemove(list, null, 5, 6);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAll(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        sublist.addAll(1, Arrays.asList("X", "Y", "Z"));
        assertEquals("[b, X, Y, Z, c, d, e]", sublist.toString());
        assertEquals("[a, b, X, Y, Z, c, d, e, f]", list.toString());
        assertEquals(7, sublist.size());
        mlo.check1AddRemove(list, null, 2, 5);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testClear(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        sublist.clear();
        assertEquals("[]", sublist.toString());
        assertEquals("[a, f]", list.toString());
        assertEquals(0, sublist.size());
        mlo.check1AddRemove(list, Arrays.asList("b", "c", "d", "e"), 1, 1);
    }

    @SuppressWarnings("unlikely-arg-type")
    @ParameterizedTest
    @MethodSource("createParameters")
    public void testContains(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        assertTrue(sublist.contains("c"));
        assertFalse(sublist.contains("a"));
        assertFalse(sublist.contains(null));
        assertFalse(sublist.contains(Integer.valueOf(7)));
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testContainsAll(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        assertTrue(sublist.containsAll(Arrays.asList("b", "c")));
        assertFalse(sublist.containsAll(Arrays.asList("a", "b")));
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testContainsNull(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        list.add(3, null);
        sublist = list.subList(1, 5);
        assertTrue(sublist.contains(null));
    }

    @SuppressWarnings("unlikely-arg-type")
    @ParameterizedTest
    @MethodSource("createParameters")
    public void testEqualsOnAnotherType(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        assertFalse(sublist.equals(Integer.valueOf(7)));
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testEqualsOnLongerList(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        List<String> other = Arrays.asList("b", "c", "d", "e", "f");
        assertFalse(sublist.equals(other));
        assertTrue(other.hashCode() != sublist.hashCode());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testEqualsOnShorterList(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        List<String> other = Arrays.asList("b", "c", "d");
        assertFalse(sublist.equals(other));
        assertTrue(other.hashCode() != sublist.hashCode());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testEquals(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        List<String> other = Arrays.asList("b", "c", "d", "e");
        assertTrue(sublist.equals(other));
        assertEquals(other.hashCode(), sublist.hashCode());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testEqualsWithNull(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        sublist.add(2, null);
        List<String> other = Arrays.asList("b", "c", null, "e");
        assertFalse(sublist.equals(other));
        assertTrue(other.hashCode() != sublist.hashCode());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testEqualsWithNullOnLongerList(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        sublist.add(2, null);
        List<String> other = Arrays.asList("b", "c", null, "d", "e");
        assertTrue(sublist.equals(other));
        assertEquals(other.hashCode(), sublist.hashCode());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testEqualsWithNullOnShorterList(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        sublist.add(2, null);
        List<String> other = Arrays.asList("b", "c", null);
        assertFalse(sublist.equals(other));
        assertTrue(other.hashCode() != sublist.hashCode());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testIndexOf(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        assertEquals(2, sublist.indexOf("d"));
        assertEquals(-1, sublist.indexOf("a"));
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testIndexOfWithNull(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        sublist.add(2, null);
        assertEquals(3, sublist.indexOf("d"));
        assertEquals(2, sublist.indexOf(null));
        assertEquals(-1, sublist.indexOf("f"));
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testIsEmpty(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        assertFalse(sublist.isEmpty());
        List<String> otherSublist = list.subList(2, 2);
        assertTrue(otherSublist.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testLastIndexOf(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        list = FXCollections.observableList(new ArrayList<String>());
        list.addAll(Arrays.asList("a", null, "a", null, "a", null, "a"));
        sublist = list.subList(1, 5);

        assertEquals(3, sublist.lastIndexOf("a"));
        assertEquals(2, sublist.lastIndexOf(null));
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testRemoveAll(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
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

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testRemoveIndex(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        String s = sublist.remove(2);
        assertEquals("d", s);
        assertEquals(3, sublist.size());
        assertEquals("[b, c, e]", sublist.toString());
        assertEquals("[a, b, c, e, f]", list.toString());
        mlo.check1AddRemove(list, Collections.singletonList("d"), 3, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testRemoveNull(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        sublist.add(2, null);
        assertTrue(sublist.remove(null));
        assertEquals(4, sublist.size());
        assertEquals("[b, c, d, e]", sublist.toString());
        assertEquals("[a, b, c, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testRemoveObjectExists(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        assertTrue(sublist.remove("b"));
        assertEquals(3, sublist.size());
        assertEquals("[c, d, e]", sublist.toString());
        assertEquals("[a, c, d, e, f]", list.toString());
        mlo.check1AddRemove(list, Collections.singletonList("b"), 1, 1);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testRemoveObjectNotExists(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        assertFalse(sublist.remove("f"));
        assertEquals(4, sublist.size());
        assertEquals("[b, c, d, e]", sublist.toString());
        assertEquals("[a, b, c, d, e, f]", list.toString());
        mlo.check0();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testRetainAll(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
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

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSet(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        String s = sublist.set(2, "X");
        assertEquals("d", s);
        assertEquals("[b, c, X, e]", sublist.toString());
        assertEquals("[a, b, c, X, e, f]", list.toString());
        mlo.check1AddRemove(list, Collections.singletonList("d"), 3, 4);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSize(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        assertEquals(4, sublist.size());
        List<String> otherSublist = list.subList(3, 3);
        assertEquals(0, otherSublist.size());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSubSubList(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        List<String> subsublist = sublist.subList(1, 3);
        assertEquals(2, subsublist.size());
        assertEquals("[c, d]", subsublist.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSubSubListAdd(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        List<String> subsublist = sublist.subList(1, 3);
        subsublist.add(1, "X");
        // sublist is now invalid
        assertEquals("[c, X, d]", subsublist.toString());
        assertEquals("[a, b, c, X, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSubSubListRemove(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        List<String> subsublist = sublist.subList(1, 3);
        assertEquals("c", subsublist.remove(0));
        // sublist is now invalid
        assertEquals("[d]", subsublist.toString());
        assertEquals("[a, b, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSubSubListSet(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        List<String> subsublist = sublist.subList(1, 3);
        String s = subsublist.set(1, "X");
        assertEquals("d", s);
        assertEquals("[c, X]", subsublist.toString());
        assertEquals("[b, c, X, e]", sublist.toString());
        assertEquals("[a, b, c, X, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToString(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        List<String> sub0 = list.subList(3, 3);
        List<String> sub1 = list.subList(3, 4);
        List<String> sub2 = list.subList(2, 5);

        assertEquals("[]", sub0.toString());
        assertEquals("[d]", sub1.toString());
        assertEquals("[c, d, e]", sub2.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyGet(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        list.add("x");
        try { sublist.get(0); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyAdd(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        list.add("x");
        try { sublist.add("y"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyAddAll(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        list.add("x");
        try { sublist.addAll(Collections.singleton("y")); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyClear(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        list.add("x");
        try { sublist.addAll(Collections.singleton("y")); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyContains(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        list.add("x");
        try { sublist.contains("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyContainsAll(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        list.add("x");
        try { sublist.containsAll(Collections.singletonList("x")); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyIsEmpty(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        list.add("x");
        try { sublist.isEmpty(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyIterator(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        list.add("x");
        try { sublist.iterator().next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRemove(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        list.add("x");
        try { sublist.remove("y"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRemove0(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        list.add("x");
        try { sublist.remove(0); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencySet(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        list.add("x");
        try { sublist.set(0, "y"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRemoveAll(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        list.add("x");
        try { sublist.removeAll(Arrays.asList("c", "d")); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRetainAll(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        list.add("x");
        try { sublist.retainAll(Arrays.asList("c", "d")); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRetainSize(Callable<ObservableList<String>> listFactory) throws Exception {
        setup(listFactory);
        list.add("x");
        try { sublist.size(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
}
