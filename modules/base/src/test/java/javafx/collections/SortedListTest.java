/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.collections.NonIterableChange.SimplePermutationChange;
import com.sun.javafx.collections.ObservableListWrapper;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import javafx.collections.ListChangeListener.Change;
import java.util.Collections;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;

import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.transformation.SortedList;
import javafx.collections.transformation.TransformationList;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.* ;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class SortedListTest {

    private ObservableList<String> list;
    private MockListObserver<String> mockListObserver;
    private SortedList<String> sortedList;

    private static class NaturalElementComparator<E> implements Comparator<E> {

        @Override
        public int compare(E o1, E o2) {
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }

            if (o1 instanceof Comparable) {
                return ((Comparable) o1).compareTo(o2);
            }

            return Collator.getInstance().compare(o1.toString(), o2.toString());
        }
    }

    @Before
    public void setUp() {
        list = FXCollections.observableArrayList();
        list.addAll("a", "c", "d", "c");
        sortedList = new SortedList<String>(list, new NaturalElementComparator<>());
        mockListObserver = new MockListObserver<String>();
        sortedList.addListener(mockListObserver);
    }
    @Test
    public void testNoChange() {
        assertEquals(Arrays.asList("a", "c", "c", "d"), sortedList);
        mockListObserver.check0();
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
    }

    @Test
    public void testRemoveSingle() {
        list.remove("a");
        assertEquals(Arrays.asList("c", "c", "d"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("a"), 0, 0);
        assertEquals(0, sortedList.getSourceIndex(0));
        assertEquals(2, sortedList.getSourceIndex(1));
        assertEquals(1, sortedList.getSourceIndex(2));
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
    }

    @Test
    public void testPureRemove() {
        list.removeAll(Arrays.asList("c", "d"));
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("c", "c", "d"), 1, 1);
        assertEquals(0, sortedList.getSourceIndex(0));
    }

    @Test
    public void testChangeComparator() {
        //ObjectProperty<Comparator<String>> op = new SimpleObjectProperty<>(new NaturalElementComparator<>());
        SimpleObjectProperty<Comparator<String>> op = new SimpleObjectProperty<Comparator<String>>();
        op.set(new NaturalElementComparator<String>());

        sortedList = new SortedList<>(list);
        sortedList.comparatorProperty().bind(op);
        sortedList.addListener(mockListObserver);

        op.set((Comparator<String>) (String o1, String o2) -> -o1.compareTo(o2));
        assertEquals(Arrays.asList("d", "c", "c", "a"), sortedList);
        mockListObserver.check1Permutation(sortedList, new int[] {3, 1, 2, 0}); // could be also 3, 2, 1, 0, but the algorithm goes this way
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
      final SortedList<Double> sorted = new SortedList<Double>(sourceList, new NaturalElementComparator<>());
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
    }

    @Test
    public void testMutableElement() {
        ArrayList<Person> backingList = new ArrayList<>();
        backingList.addAll(Arrays.asList(new Person("c"),
                new Person("f"),
                new Person("d"),
                new Person("k"),
                new Person("b")));

        ObservableList<Person> list = FXCollections.observableList(backingList, (Person p) -> new Observable[] {p.name});

        SortedList<Person> sorted = new SortedList<Person>(list, new NaturalElementComparator<>());
        ListChangeListener<Person> listener = c -> {
            c.next();
            assertTrue(c.wasPermutated());
            assertArrayEquals(new int[] {0, 4, 1, 2, 3}, c.getPermutation());
            assertTrue(c.next());
            assertTrue(c.wasUpdated());
            assertEquals(4, c.getFrom());
            assertEquals(5, c.getTo());
        };
        assertEquals(Arrays.asList(new Person("b"),
                new Person("c"),
                new Person("d"),
                new Person("f"),
                new Person("k")), sorted);
        sorted.addListener(listener);
        sorted.get(1).name.set("z");
        assertEquals(Arrays.asList(new Person("b"),
                new Person("d"),
                new Person("f"),
                new Person("k"),
                new Person("z")), sorted);
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

        TransformationList<Object, Object> sorted = new SortedList<>(list, new NaturalElementComparator<>());
        assertEquals(Arrays.asList(o2, o1, o3), sorted);
    }

    @Test
    public void testCompareNulls() {
        ObservableList<String> list = FXCollections.observableArrayList( "g", "a", null, "z");

        TransformationList<String, String> sorted = new SortedList<>(list, new NaturalElementComparator<>());
        assertEquals(Arrays.asList(null, "a", "g", "z"), sorted);
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
            fireChange(new SimplePermutationChange(0, size(), new int[] {2, 1, 0}, this));
        }

    }
    /**
     * SortedList cant cope with permutations.
     */
    @Test
    public void testPermutate() {
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < 3; i++) {
            list.add(i);
        }
        Permutator<Integer> permutator = new Permutator<Integer>(list);
        SortedList<Integer> sorted = new SortedList<Integer>(permutator);
        permutator.swap();
        assertEquals(0, sorted.getSourceIndex(sorted.size() - 1));
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
    }
    
    @Test
    public void testAddAllOnEmpty() {
        list = FXCollections.observableArrayList();
        SortedList<String> sl = list.sorted(String.CASE_INSENSITIVE_ORDER);
        list.addAll("B", "A");
        
        assertEquals(Arrays.asList("A", "B"), sl);
    }

    @Test
    public void test_rt36353_sortedList() {
        ObservableList<String> data = FXCollections.observableArrayList("2", "1", "3");
        SortedList<String> sortedList = new SortedList<String>(data);

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

        // comparator that will create list of [3,2,1]. Sort indices based on
        // previous order [1,2,3].
        sortedList.setComparator((s1,s2) -> s2.compareTo(s1));
        assertEquals(FXCollections.observableArrayList("3","2","1"), sortedList);
        expected.clear();
        expected.put(0, 2);     // item "1" has moved from index 0 to index 2
        expected.put(1, 1);     // item "2" has remained in index 1
        expected.put(2, 0);     // item "3" has moved from index 2 to index 0
        assertEquals(expected, pMap);

        // null comparator so sort order should return to [2,1,3]. Sort indices based on
        // previous order [3,2,1].
        sortedList.setComparator(null);
        assertEquals(FXCollections.observableArrayList("2","1","3"), sortedList);
        expected.clear();
        expected.put(0, 2);     // item "3" has moved from index 0 to index 2
        expected.put(1, 0);     // item "2" has moved from index 1 to index 0
        expected.put(2, 1);     // item "1" has moved from index 2 to index 1
        assertEquals(expected, pMap);
    }


    @Test
    public void testAddWhenUnsorted() {
        sortedList.setComparator(null);
        mockListObserver.clear();
        list.add(2, "b");
        assertEquals(5, sortedList.size());
        assertEquals(Arrays.asList("a", "c", "b", "d", "c"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Collections.emptyList(), 2, 3);

        mockListObserver.clear();
        sortedList.setComparator(Comparator.<String>naturalOrder());
        mockListObserver.check1Permutation(sortedList, new int[] {0, 2, 1, 4, 3});
        assertEquals(5, sortedList.size());
        assertEquals(Arrays.asList("a", "b", "c", "c", "d"), sortedList);

        mockListObserver.clear();
        sortedList.setComparator(null);
        assertEquals(5, sortedList.size());
        assertEquals(Arrays.asList("a", "c", "b", "d", "c"), sortedList);
        mockListObserver.check1Permutation(sortedList, new int[] {0, 2, 1, 4, 3});

    }

    @Test
    public void testRemoveWhenUnsorted() {
        sortedList.setComparator(null);
        mockListObserver.clear();
        list.remove(1);
        assertEquals(3, sortedList.size());
        assertEquals(Arrays.asList("a", "d", "c"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("c"), 1, 1);

        mockListObserver.clear();
        sortedList.setComparator(Comparator.<String>naturalOrder());
        mockListObserver.check1Permutation(sortedList, new int[] {0, 2, 1});
        assertEquals(3, sortedList.size());
        assertEquals(Arrays.asList("a", "c", "d"), sortedList);

        mockListObserver.clear();
        sortedList.setComparator(null);
        assertEquals(3, sortedList.size());
        assertEquals(Arrays.asList("a", "d", "c"), sortedList);
        mockListObserver.check1Permutation(sortedList, new int[] {0, 2, 1});
    }

    @Test
    public void testSetWhenUnsorted() {
        sortedList.setComparator(null);
        mockListObserver.clear();
        list.set(1, "e");
        assertEquals(4, sortedList.size());
        assertEquals(Arrays.asList("a", "e", "d", "c"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("c"), 1, 2);

        mockListObserver.clear();
        sortedList.setComparator(Comparator.<String>naturalOrder());
        mockListObserver.check1Permutation(sortedList, new int[] {0, 3, 2, 1});
        assertEquals(4, sortedList.size());
        assertEquals(Arrays.asList("a", "c", "d", "e"), sortedList);

        mockListObserver.clear();
        sortedList.setComparator(null);
        assertEquals(4, sortedList.size());
        assertEquals(Arrays.asList("a", "e", "d", "c"), sortedList);
        mockListObserver.check1Permutation(sortedList, new int[] {0, 3, 2, 1});
    }
}
