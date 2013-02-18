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

import com.sun.javafx.collections.transformation.SortableList;
import com.sun.javafx.collections.transformation.SortableList.SortMode;
import com.sun.javafx.collections.transformation.SortedList;
import com.sun.javafx.collections.NonIterableChange.SimplePermutationChange;
import com.sun.javafx.collections.ObservableListWrapper;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ListChangeListener.Change;
import java.util.Collections;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.* ;

/**
 *
 */
@Ignore
public class SortedListTest {

    private ObservableList<String> list;
    private MockListObserver<String> mockListObserver;
    private SortedList<String> sortedList;

    @Before
    public void setUp() {
        list = FXCollections.observableArrayList();
        list.addAll("a", "c", "d", "c");
        sortedList = new SortedList<String>(list);
        mockListObserver = new MockListObserver<String>();
        sortedList.addListener(mockListObserver);
    }

    @Test
    public void testBatchMode() {
        list.clear();
        mockListObserver.clear();
        sortedList.setMode(SortMode.BATCH);
        list.addAll("a", "c", "d", "c");
        mockListObserver.check1AddRemove(sortedList, Collections.<String>emptyList(), 0, 4);
        mockListObserver.clear();
        assertEquals(Arrays.asList("a", "c", "d", "c"), sortedList);
        sortedList.sort();
        mockListObserver.check1Permutation(sortedList, new int[] {0, 1, 3, 2});
        assertEquals(Arrays.asList("a", "c", "c", "d"), sortedList);
    }
    
    
    @Test
    public void testBatchMode_add() {
        sortedList.setMode(SortableList.SortMode.BATCH);
        list.add("b");
        assertEquals(Arrays.asList("a", "c", "c", "d", "b"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Collections.<String>emptyList(), 4, 5);
        mockListObserver.clear();
        sortedList.sort();
        assertEquals(Arrays.asList("a", "b", "c", "c", "d"), sortedList);
        mockListObserver.check1Permutation(sortedList, new int[] {0, 2, 3, 4, 1});
        mockListObserver.clear();
        list.remove("b");
        assertEquals(Arrays.asList("a", "c", "c", "d"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Collections.singletonList("b"), 1, 1);
    }
    
    @Test
    public void testBatchMode_add2() {
        sortedList.setMode(SortableList.SortMode.BATCH);
        list.addAll(1, Arrays.asList("b", "b"));
        mockListObserver.check1AddRemove(sortedList, Collections.<String>emptyList(), 4, 6);
        assertEquals(Arrays.asList("a", "c", "c", "d", "b", "b"), sortedList);
        assertEquals(0, sortedList.getSourceIndex(0));
        assertEquals(3, sortedList.getSourceIndex(1));
        assertEquals(5, sortedList.getSourceIndex(2));
        assertEquals(4, sortedList.getSourceIndex(3));
        assertEquals(1, sortedList.getSourceIndex(4));
        assertEquals(2, sortedList.getSourceIndex(5));
    }
    
    @Test
    public void testBatchMode_removeSingle() {
        sortedList.setMode(SortableList.SortMode.BATCH);
        list.addAll(1, Arrays.asList("b", "b"));
        mockListObserver.clear();
        list.remove(3);
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("c"), 1, 1);
        assertEquals(Arrays.asList("a", "c", "d", "b", "b"), sortedList);
        assertEquals(0, sortedList.getSourceIndex(0));
        assertEquals(4, sortedList.getSourceIndex(1));
        assertEquals(3, sortedList.getSourceIndex(2));
        assertEquals(1, sortedList.getSourceIndex(3));
        assertEquals(2, sortedList.getSourceIndex(4));
        mockListObserver.clear();
        list.remove(1);
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("b"), 3, 3);
    }
    
    @Test
    public void testBatchMode_remove() {
        sortedList.setMode(SortMode.BATCH);
        list.removeAll(Arrays.asList("c"));
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("c", "c", "d"), 1, 2);
    }
    
    @Test
    public void testBatchMode_pureRemove() {
        sortedList.setMode(SortMode.BATCH);
        list.removeAll(Arrays.asList("c", "d"));
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("c", "c", "d"), 1, 1);
    }
    
    
    @Test
    public void testLiveMode() {
        assertEquals(Arrays.asList("a", "c", "c", "d"), sortedList);
        mockListObserver.check0();
    }
    
    @Test
    public void testLiveMode_Add() {
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
    public void testLiveMode_AddSingle() {
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
    public void testLiveMode_Remove() {
        list.removeAll(Arrays.asList("c")); // removes "c", "d", "c", adds "d"
        assertEquals(Arrays.asList("a", "d"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("c", "c", "d"), 1, 2);
        assertEquals(0, sortedList.getSourceIndex(0));
        assertEquals(1, sortedList.getSourceIndex(1));
        mockListObserver.clear();
        list.removeAll(Arrays.asList("a", "d"));
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("a", "d"), 0, 0);
    }
    
    @Test
    public void testLiveMode_RemoveSingle() {
        list.remove("a");
        assertEquals(Arrays.asList("c", "c", "d"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("a"), 0, 0);
        assertEquals(0, sortedList.getSourceIndex(0));
        assertEquals(2, sortedList.getSourceIndex(1));
        assertEquals(1, sortedList.getSourceIndex(2));
    }
    
    @Test
    public void testLiveMode_MultipleOperations() {
        list.remove(2);
        assertEquals(Arrays.asList("a", "c", "c"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("d"), 3, 3);
        mockListObserver.clear();
        list.add("b");
        assertEquals(Arrays.asList("a", "b", "c", "c"), sortedList);
        mockListObserver.check1AddRemove(sortedList, Collections.<String>emptyList(), 1, 2);
    }

    @Test
    public void testLiveMode_pureRemove() {
        list.removeAll(Arrays.asList("c", "d"));
        mockListObserver.check1AddRemove(sortedList, Arrays.asList("c", "c", "d"), 1, 1);
        assertEquals(0, sortedList.getSourceIndex(0));
    }
    
    @Test
    public void testLiveMode_changeComparator() {
        sortedList.setComparator(new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return - o1.compareTo(o2);
            }
        });
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
      final SortedList<Double> sorted = new SortedList<Double>(sourceList); 
      ListChangeListener<Double> listener = new ListChangeListener<Double>() { 

          @Override 
          public void onChanged(Change<? extends Double> c) { 
              assertEquals(Arrays.<Double>asList(-300.0, 50.0, 400.0, 600.0, 1300.0, 4000.0), c.getRemoved());
              assertEquals(0, c.getFrom());
              assertEquals(0, c.getFrom());
              assertEquals(3, c.getTo());
              assertEquals(Arrays.<Double>asList(400.0, 600.0, 1300.0), c.getList());
              // grab sourceIndex of last (aka: highest) value in sorted list 
              int sourceIndex = sorted.getSourceIndex(sorted.size() - 1); 
              assertEquals(0, sourceIndex);
          } 
      }; 
      sorted.addListener(listener); 
      sourceList.removeAll(other); 
    } 

    @Test
    public void testMutableElement() {
        List<Date> list = FXCollections.observableArrayList(new Date(5000),
                new Date(10000),
                new Date(1200),
                new Date(12),
                new Date(12000));
        
        SortedList<Date> sorted = new SortedList<Date>(list, null, SortMode.BATCH);
        ListChangeListener<Date> listener = new ListChangeListener<Date>() {

            @Override
            public void onChanged(Change<? extends Date> change) {
                assertTrue(change.wasPermutated());
            }
        };
        sorted.sort();
        assertEquals(Arrays.asList(new Date(12),
                new Date(1200),
                new Date(5000),
                new Date(10000),
                new Date(12000)), sorted);
        list.get(1).setTime(4000);
        assertEquals(Arrays.asList(new Date(12),
                new Date(1200),
                new Date(5000),
                new Date(4000),
                new Date(12000)), sorted);
        sorted.addListener(listener);
        sorted.sort();
        assertEquals(Arrays.asList(new Date(12),
                new Date(1200),
                new Date(4000),
                new Date(5000),
                new Date(12000)), sorted);
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
        List<Object> list = FXCollections.observableArrayList(o1, o2, o3);

        SortedList<Object> sorted = new SortedList<Object>(list);
        assertEquals(Arrays.asList(o2, o1, o3), sorted);
    }
    
    
    @Test(expected=ClassCastException.class)
    public void testNotComparableBroken() {
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
        List<Object> list = FXCollections.observableArrayList( "x", o1, o2, o3);

        SortedList<Object> sorted = new SortedList<Object>(list);
        sorted.get(0);
    }
    
    @Test
    public void testCompareNulls() {
        List<String> list = FXCollections.observableArrayList( "g", "a", null, "z");

        SortedList<String> sorted = new SortedList<String>(list);
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
}
