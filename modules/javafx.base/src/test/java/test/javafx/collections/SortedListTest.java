/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.collections.NonIterableChange.SimplePermutationChange;
import com.sun.javafx.collections.ObservableListWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import javafx.beans.Observable;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListWrapperShim;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.* ;
import static org.junit.Assert.assertEquals;

public class SortedListTest {

    private ObservableList<String> list;
    private MockListObserver<String> mockListObserver;
    private SortedList<String> sortedList;

    @Before
    public void setUp() {
        list = FXCollections.observableArrayList();
        list.addAll("a", "c", "d", "c");
        sortedList = list.sorted();
        mockListObserver = new MockListObserver<>();
        sortedList.addListener(mockListObserver);
    }

    @Test
    public void testNoChange() {
        assertEquals(Arrays.asList("a", "c", "c", "d"), sortedList);
        mockListObserver.check0();

        compareIndices();
    }

    @Test
    public void testAdd() {
        list.clear();
        mockListObserver.clear();
        assertEquals(Collections.emptyList(), sortedList);
        list.addAll("a", "c", "d", "c");
        assertEquals(Arrays.asList("a", "c", "c", "d"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Collections.<String>emptyList(), 0, 4);
        assertEquals(0, sortedList.getSourceIndex(0));
        assertEquals(2, sortedList.getSourceIndex(3));

        compareIndices();
    }

    private <E> void compareIndices(SortedList<E> sorted) {
        ObservableList<? extends E> source = sorted.getSource();
        for (int i = 0; i < sorted.size(); i++) {
            // i as a view index
            int sourceIndex = sorted.getSourceIndex(i);
            assertEquals(i, sorted.getViewIndex(sourceIndex));
            assertSame(sorted.get(i), source.get(sourceIndex));

            // i as a source index
            int viewIndex = sorted.getViewIndex(i);
            assertEquals(i, sorted.getSourceIndex(viewIndex));
            assertSame(source.get(i), sorted.get(viewIndex));
        }
    }

    private void compareIndices() {
        compareIndices(sortedList);
    }

    @Test
    public void testAddSingle() {
        list.add("b");
        assertEquals(Arrays.asList("a", "b", "c", "c", "d"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Collections.<String>emptyList(), 1, 2);
        assertEquals(0, sortedList.getSourceIndex(0));
        assertEquals(4, sortedList.getSourceIndex(1));
        assertEquals(1, sortedList.getSourceIndex(2));
        assertEquals(3, sortedList.getSourceIndex(3));
        assertEquals(2, sortedList.getSourceIndex(4));

        compareIndices();
    }

    @Test
    public void testRemove() {
        list.removeAll(Arrays.asList("c")); // removes "c", "d", "c", adds "d"
        assertEquals(Arrays.asList("a", "d"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("c", "c"), 1, 1);
        assertEquals(0, sortedList.getSourceIndex(0));
        assertEquals(1, sortedList.getSourceIndex(1));
        mockListObserver.clear();
        list.removeAll(Arrays.asList("a", "d"));
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("a", "d"), 0, 0);

        compareIndices();
    }

    @Test
    public void testRemoveSingle() {
        list.remove("a");
        assertEquals(Arrays.asList("c", "c", "d"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("a"), 0, 0);
        assertEquals(0, sortedList.getSourceIndex(0));
        assertEquals(2, sortedList.getSourceIndex(1));
        assertEquals(1, sortedList.getSourceIndex(2));

        compareIndices();
    }

    @Test
    public void testMultipleOperations() {
        list.remove(2);
        assertEquals(Arrays.asList("a", "c", "c"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("d"), 3, 3);
        mockListObserver.clear();
        list.add("b");
        assertEquals(Arrays.asList("a", "b", "c", "c"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Collections.<String>emptyList(), 1, 2);

        compareIndices();
    }

    @Test
    public void testPureRemove() {
        list.removeAll(Arrays.asList("c", "d"));
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("c", "c", "d"), 1, 1);
        assertEquals(0, sortedList.getSourceIndex(0));

        compareIndices();
    }

    @Test
    public void testChangeComparator() {
        SimpleObjectProperty<Comparator<String>> op =
                new SimpleObjectProperty<>(Comparator.naturalOrder());

        sortedList = new SortedList<>(list);
        assertEquals(Arrays.asList("a", "c", "d", "c"), sortedList);
        compareIndices();

        sortedList.comparatorProperty().bind(op);
        assertEquals(Arrays.asList("a", "c", "c", "d"), sortedList);
        compareIndices();

        sortedList.addListener(mockListObserver);

        op.set((Comparator<String>) (String o1, String o2) -> -o1.compareTo(o2));
        assertEquals(Arrays.asList("d", "c", "c", "a"), sortedList);
        mockListObserver.check1Permutation(sortedList, new int[] {3, 1, 2, 0}); // could be also 3, 2, 1, 0, but the algorithm goes this way
        compareIndices();

        mockListObserver.clear();
        op.set(null);
        assertEquals(Arrays.asList("a", "c", "d", "c"), sortedList);
        mockListObserver.check1Permutation(sortedList, new int[] {2, 1, 3, 0});
        compareIndices();
    }


   /**
     * A slightly updated test provided by "Kleopatra" (http://javafx-jira.kenai.com/browse/RT-14400)
     */
    @Test
    public void testSourceIndex() {
        final ObservableList<Double> sourceList = FXCollections.observableArrayList(
                1300., 400., 600.
              );
        // the list to be removed again, note that its highest value is greater
        // then the highest in the base list before adding
        List<Double> other = Arrays.asList(
                50., -300., 4000.
        );
        sourceList.addAll(other);
        // wrap into a sorted list and add a listener to the sorted
        final SortedList<Double> sorted = sourceList.sorted();
        ListChangeListener<Double> listener = c -> {
            assertEquals(Arrays.<Double>asList(400.0, 600.0, 1300.0), c.getList());

            c.next();
            assertEquals(Arrays.<Double>asList(-300.0, 50.0), c.getRemoved());
            assertEquals(0, c.getFrom());
            assertEquals(0, c.getTo());
            assertTrue(c.next());
            assertEquals(Arrays.<Double>asList(4000.), c.getRemoved());
            assertEquals(3, c.getFrom());
            assertEquals(3, c.getTo());
            assertFalse(c.next());


            // grab sourceIndex of last (aka: highest) value in sorted list
            int sourceIndex = sorted.getSourceIndex(sorted.size() - 1);
            assertEquals(0, sourceIndex);
        };
        sorted.addListener(listener);
        sourceList.removeAll(other);

        compareIndices(sorted);
    }

    @Test
    public void testMutableElement() {
        ObservableList<Person> list = createPersonsList();

        SortedList<Person> sorted = list.sorted();
        assertEquals(Arrays.asList(
                new Person("five"), new Person("four"), new Person("one"),
                new Person("three"), new Person("two")),
                sorted);
        MockListObserver<Person> listener = new MockListObserver<>();
        sorted.addListener(listener);
        list.get(3).name.set("zero"); // four -> zero
        ObservableList<Person> expected = FXCollections.observableArrayList(
                new Person("five"), new Person("one"), new Person("three"),
                new Person("two"), new Person("zero"));
        listener.checkPermutation(0, expected, 0, list.size(), new int[]{0, 4, 1, 2, 3});
        listener.checkUpdate(1, expected, 4, 5);
        assertEquals(expected, sorted);

        compareIndices(sorted);
    }

    @Test
    public void testMutableElementUnsorted_rt39541() {
        ObservableList<Person> list = createPersonsList();
        SortedList<Person> unsorted = new SortedList<>(list);
        MockListObserver<Person> listener = new MockListObserver<>();
        unsorted.addListener(listener);
        list.get(3).name.set("zero"); // four -> zero
        ObservableList<Person> expected = FXCollections.observableArrayList(
                new Person("one"), new Person("two"), new Person("three"),
                new Person("zero"), new Person("five"));
        listener.check1Update(expected, 3, 4);

        compareIndices(unsorted);
    }

    @Test
    public void testMutableElementUnsortedChain_rt39541() {
        ObservableList<Person> items = createPersonsList();

        SortedList<Person> sorted = items.sorted();
        SortedList<Person> unsorted = new SortedList<>(sorted);

        assertEquals(sorted, unsorted);

        MockListObserver<Person> listener = new MockListObserver<>();
        unsorted.addListener(listener);
        items.get(3).name.set("zero"); // "four" -> "zero"
        ObservableList<Person> expected = FXCollections.observableArrayList(
                new Person("five"), new Person("one"), new Person("three"),
                new Person("two"), new Person("zero"));
        listener.checkPermutation(0, expected, 0, expected.size(), new int[] {0, 4, 1, 2, 3});
        listener.checkUpdate(1, expected, 4, 5);
        assertEquals(expected, sorted);
        assertEquals(expected, unsorted);

        compareIndices(sorted);
        compareIndices(unsorted);
    }

    @Test
    public void testMutableElementSortedFilteredChain() {
        ObservableList<Person> items = FXCollections.observableArrayList(
                (Person p) -> new Observable[]{p.name});
        items.addAll(
                new Person("b"), new Person("c"), new Person("a"),
                new Person("f"), new Person("e"), new Person("d"));

        FilteredList<Person> filtered = items.filtered(e -> !e.name.get().startsWith("z"));
        MockListObserver<Person> filterListener = new MockListObserver<>();
        filtered.addListener(filterListener);

        SortedList<Person> sorted = filtered.sorted((x, y) -> x.name.get().compareTo(y.name.get()));
        MockListObserver<Person> sortListener = new MockListObserver<>();
        sorted.addListener(sortListener);
        items.get(2).name.set("z"); // "a" -> "z"
        filterListener.check1AddRemove(filtered, Arrays.asList(new Person("z")), 2, 2);
        sortListener.check1AddRemove(sorted, Arrays.asList(new Person("z")), 0, 0);
        ObservableList<Person> expected = FXCollections.observableArrayList(
                new Person("b"), new Person("c"), new Person("d"),
                new Person("e"), new Person("f"));
        assertEquals(expected, sorted);

        compareIndices(sorted);
    }

    private ObservableList<Person> createPersonsList() {
        ObservableList<Person> list = FXCollections.observableArrayList(
                (Person p) -> new Observable[]{p.name});
        list.addAll(
                new Person("one"), new Person("two"), new Person("three"),
                new Person("four"), new Person("five"));
        return list;
    }

    @Test
    public void testNotComparable() {
        final Object o1 = new Object() {

            @Override
            public String toString() {
                return "c";
            }
        };
        final Object o2 = new Object() {

            @Override
            public String toString() {
                return "a";
            }
        };
        final Object o3 = new Object() {

            @Override
            public String toString() {
                return "d";
            }
        };
        ObservableList<Object> list = FXCollections.observableArrayList(o1, o2, o3);

        SortedList<Object> sorted = list.sorted();
        assertEquals(Arrays.asList(o2, o1, o3), sorted);

        compareIndices(sorted);
    }

    @Test
    public void testCompareNulls() {
        ObservableList<String> list = FXCollections.observableArrayList( "g", "a", null, "z");

        SortedList<String> sorted = list.sorted();
        assertEquals(Arrays.asList(null, "a", "g", "z"), sorted);

        compareIndices(sorted);
    }


    private static class Permutator<E> extends ObservableListWrapper<E> {
        private List<E> backingList;
        public Permutator(List<E> list) {
            super(list);
            this.backingList = list;
        }

        public void swap() {
            E first = get(0);
            backingList.set(0, get(size() - 1));
            backingList.set(size() -1, first);
            ObservableListWrapperShim.fireChange(this,
                new SimplePermutationChange(0, size(), new int[] {2, 1, 0}, this));
        }

    }
    /**
     * SortedList cant cope with permutations.
     */
    @Test
    public void testPermutate() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            list.add(i);
        }
        Permutator<Integer> permutator = new Permutator<>(list);
        SortedList<Integer> sorted = new SortedList<>(permutator);
        permutator.swap();

        compareIndices(sorted);
    }

    @Test
    public void testUnsorted() {
        SortedList<String> sorted = new SortedList<>(list);
        assertEquals(sorted, list);
        assertEquals(list, sorted);

        list.removeAll("a", "d");

        assertEquals(sorted, list);

        list.addAll(0, Arrays.asList("a", "b", "c"));

        assertEquals(sorted, list);

        FXCollections.sort(list);

        assertEquals(sorted, list);

        compareIndices(sorted);
    }

    @Test
    public void testUnsorted2() {
        list.setAll("a", "b", "c", "d", "e", "f");
        SortedList<String> sorted = new SortedList<>(list);
        assertEquals(sorted, list);

        list.removeAll("b", "c", "d");

        assertEquals(sorted, list);

        compareIndices(sorted);
    }

    @Test
    public void testSortedNaturalOrder() {
        assertEquals(Arrays.asList("a", "c", "c", "d"), list.sorted());
    }

    @Test
    public void testRemoveFromDuplicates() {
        String toRemove = new String("A");
        String other = new String("A");
        list = FXCollections.observableArrayList(other, toRemove);
        Comparator<String> c = Comparator.naturalOrder();
        SortedList<String> sorted = list.sorted(c);

        list.remove(1);

        assertEquals(1, sorted.size());
        assertTrue(sorted.get(0) == other);

        compareIndices(sorted);
    }

    @Test
    public void testAddAllOnEmpty() {
        list = FXCollections.observableArrayList();
        SortedList<String> sl = list.sorted(String.CASE_INSENSITIVE_ORDER);
        list.addAll("B", "A");

        assertEquals(Arrays.asList("A", "B"), sl);

        compareIndices(sl);
    }

    @Test
    public void test_rt36353_sortedList() {
        ObservableList<String> data = FXCollections.observableArrayList("2", "1", "3");
        SortedList<String> sortedList = new SortedList<>(data);

        HashMap<Integer, Integer> pMap = new HashMap<>();
        sortedList.addListener((ListChangeListener<String>) c -> {
            while (c.next()) {
                if (c.wasPermutated()) {
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        pMap.put(i, c.getPermutation(i));
                    }
                }
            }
        });

        Map<Integer, Integer> expected = new HashMap<>();

        // comparator that will create list of [1,2,3]. Sort indices based on
        // previous order [2,1,3].
        sortedList.setComparator((s1,s2) -> s1.compareTo(s2));
        assertEquals(FXCollections.observableArrayList("1","2","3"), sortedList);
        expected.put(0, 1);     // item "2" has moved from index 0 to index 1
        expected.put(1, 0);     // item "1" has moved from index 1 to index 0
        expected.put(2, 2);     // item "3" has remained in index 2
        assertEquals(expected, pMap);
        compareIndices(sortedList);

        // comparator that will create list of [3,2,1]. Sort indices based on
        // previous order [1,2,3].
        sortedList.setComparator((s1,s2) -> s2.compareTo(s1));
        assertEquals(FXCollections.observableArrayList("3","2","1"), sortedList);
        expected.clear();
        expected.put(0, 2);     // item "1" has moved from index 0 to index 2
        expected.put(1, 1);     // item "2" has remained in index 1
        expected.put(2, 0);     // item "3" has moved from index 2 to index 0
        assertEquals(expected, pMap);
        compareIndices(sortedList);

        // null comparator so sort order should return to [2,1,3]. Sort indices based on
        // previous order [3,2,1].
        sortedList.setComparator(null);
        assertEquals(FXCollections.observableArrayList("2","1","3"), sortedList);
        expected.clear();
        expected.put(0, 2);     // item "3" has moved from index 0 to index 2
        expected.put(1, 0);     // item "2" has moved from index 1 to index 0
        expected.put(2, 1);     // item "1" has moved from index 2 to index 1
        assertEquals(expected, pMap);
        compareIndices(sortedList);
    }

    @Test
    public void testAddWhenUnsorted() {
        sortedList.setComparator(null);
        mockListObserver.clear();
        list.add(2, "b");
        assertEquals(5, sortedList.size());
        assertEquals(Arrays.asList("a", "c", "b", "d", "c"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Collections.emptyList(), 2, 3);
        compareIndices();

        mockListObserver.clear();
        sortedList.setComparator(Comparator.<String>naturalOrder());
        mockListObserver.check1Permutation(sortedList, new int[] {0, 2, 1, 4, 3});
        assertEquals(5, sortedList.size());
        assertEquals(Arrays.asList("a", "b", "c", "c", "d"), sortedList);
        compareIndices();

        mockListObserver.clear();
        sortedList.setComparator(null);
        assertEquals(5, sortedList.size());
        assertEquals(Arrays.asList("a", "c", "b", "d", "c"), sortedList);
        mockListObserver.check1Permutation(sortedList, new int[] {0, 2, 1, 4, 3});
        compareIndices();
    }

    @Test
    public void testRemoveWhenUnsorted() {
        sortedList.setComparator(null);
        mockListObserver.clear();
        list.remove(1);
        assertEquals(3, sortedList.size());
        assertEquals(Arrays.asList("a", "d", "c"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("c"), 1, 1);
        compareIndices();

        mockListObserver.clear();
        sortedList.setComparator(Comparator.<String>naturalOrder());
        mockListObserver.check1Permutation(sortedList, new int[] {0, 2, 1});
        assertEquals(3, sortedList.size());
        assertEquals(Arrays.asList("a", "c", "d"), sortedList);
        compareIndices();

        mockListObserver.clear();
        sortedList.setComparator(null);
        assertEquals(3, sortedList.size());
        assertEquals(Arrays.asList("a", "d", "c"), sortedList);
        mockListObserver.check1Permutation(sortedList, new int[] {0, 2, 1});
        compareIndices();
    }

    @Test
    public void testSetWhenUnsorted() {
        sortedList.setComparator(null);
        mockListObserver.clear();
        list.set(1, "e");
        assertEquals(4, sortedList.size());
        assertEquals(Arrays.asList("a", "e", "d", "c"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("c"), 1, 2);
        compareIndices();

        mockListObserver.clear();
        sortedList.setComparator(Comparator.<String>naturalOrder());
        mockListObserver.check1Permutation(sortedList, new int[] {0, 3, 2, 1});
        assertEquals(4, sortedList.size());
        assertEquals(Arrays.asList("a", "c", "d", "e"), sortedList);
        compareIndices();

        mockListObserver.clear();
        sortedList.setComparator(null);
        assertEquals(4, sortedList.size());
        assertEquals(Arrays.asList("a", "e", "d", "c"), sortedList);
        mockListObserver.check1Permutation(sortedList, new int[] {0, 3, 2, 1});
        compareIndices();
    }
}
