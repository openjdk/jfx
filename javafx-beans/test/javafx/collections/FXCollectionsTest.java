/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.InvalidationListener;
import org.junit.Test;

import java.util.*;
import javafx.collections.MockSetObserver.Tuple;

import static org.junit.Assert.*;

public class FXCollectionsTest {

    @Test
    @SuppressWarnings("unchecked")
    public void concatTest() {
        ObservableList<String> seq =
                FXCollections.concat(FXCollections.observableArrayList("foo", "bar"),
                FXCollections.observableArrayList("foobar"));
        assertArrayEquals(new String[] {"foo", "bar", "foobar"}, seq.toArray(new String[0]));
        seq = FXCollections.concat();
        assertTrue(seq.isEmpty());
        seq = FXCollections.concat(FXCollections.observableArrayList("foo"));
        assertArrayEquals(new String[] {"foo"}, seq.toArray(new String[0]));
    }

    @Test
    public void shuffleTest() {
        String[] content = new String[] { "foo", "bar", "ham", "eggs", "spam" };
        ObservableList<String> seq = FXCollections.observableArrayList(content);
        for (int i = 0; i < 10; ++i ) {
            FXCollections.shuffle(seq);
            assertEquals(content.length, seq.size());
            for (String s : content) {
                assertTrue(seq.contains(s));
            }
        }
    }

    @Test
    public void copyTest() {
        ObservableList<String> dest = FXCollections.observableArrayList("a", "b", "c", "d");
        ObservableList<String> src = FXCollections.observableArrayList("foo", "bar");
        final MockListObserver<String> observer = new MockListObserver<String>();

        dest.addListener(observer);

        FXCollections.copy(dest, src);
        assertArrayEquals(new String[] {"foo", "bar", "c", "d" }, dest.toArray(new String[0]));
        observer.check1();

    }

    @Test
    public void fillTest() {
        ObservableList<String> seq = FXCollections.observableArrayList("foo", "bar");
        final MockListObserver<String> observer = new MockListObserver<String>();

        seq.addListener(observer);
        FXCollections.fill(seq, "ham");
        assertArrayEquals(new String[] {"ham", "ham" }, seq.toArray(new String[0]));
        observer.check1();
    }

    @Test
    public void replaceAllTest() {
        ObservableList<String> seq = FXCollections.observableArrayList("eggs", "ham", "spam", "spam", "eggs", "spam");
        final MockListObserver<String> observer = new MockListObserver<String>();

        seq.addListener(observer);
        FXCollections.replaceAll(seq, "spam", "viking");
        assertArrayEquals(new String[] {"eggs", "ham", "viking", "viking", "eggs", "viking"}, seq.toArray(new String[0]));
        observer.check1();
    }

    @Test
    public void reverseTest() {

        ObservableList<String> seq1 = FXCollections.observableArrayList("one", "two", "three", "four");
        final MockListObserver<String> observer1 = new MockListObserver<String>();
        seq1.addListener(observer1);

        ObservableList<String> seq2 = FXCollections.observableArrayList("one", "two", "three", "four", "five");
        final MockListObserver<String> observer2 = new MockListObserver<String>();
        seq2.addListener(observer2);

        FXCollections.reverse(seq1);
        FXCollections.reverse(seq2);

        assertArrayEquals(new String[] { "four", "three", "two", "one"} , seq1.toArray(new String[0]));
        assertArrayEquals(new String[] { "five", "four", "three", "two", "one"} , seq2.toArray(new String[0]));

        observer1.check1();
        observer2.check1();
    }

    @Test
    public void rotateTest() {
        ObservableList<String> seq = FXCollections.observableArrayList("one", "two", "three", "four", "five");
        final MockListObserver<String> observer = new MockListObserver<String>();
        seq.addListener(observer);

        FXCollections.rotate(seq, 2);
        assertArrayEquals(new String[] { "four", "five", "one", "two", "three"} , seq.toArray(new String[0]));
        observer.check1();
        observer.clear();

        FXCollections.rotate(seq, 3);
        assertArrayEquals(new String[] { "one", "two", "three", "four", "five"} , seq.toArray(new String[0]));
        observer.check1();
        observer.clear();

        FXCollections.rotate(seq, 8);
        assertArrayEquals(new String[] { "three", "four", "five", "one", "two"} , seq.toArray(new String[0]));
        observer.check1();
        observer.clear();

        FXCollections.rotate(seq, -3);
        assertArrayEquals(new String[] { "one", "two", "three", "four", "five" } , seq.toArray(new String[0]));
        observer.check1();
        observer.clear();

    }

    @Test
    public void sortTest() {
        String[] content = new String[] {"one", "two", "three", "four", "five" };
        ObservableList<String> seq = FXCollections.observableArrayList(content);
        doSort(seq, true);
        seq = new NonSortableObservableList();
        seq.addAll(content);
        doSort(seq, false);
    }
    
    @Test
    public void sortTest2() {
    //test sort on bigger elements, so that it is sorted with mergesort and not insert sort
        String[] content = new String[] { "q", "w", "e", "r", "t", "y", "u", "i", "o", "p" };
        ObservableList<String> seq = FXCollections.observableArrayList(content);
        final MockListObserver<String> observer = new MockListObserver<String>();
        seq.addListener(observer);
        FXCollections.sort(seq);
        observer.check1Permutation(seq, new int[] {4, 8, 0, 5, 6, 9, 7, 1, 2, 3});
        seq.setAll( "q", "w", "e", "r", "t", "y", "u", "i", "o", "p");
        observer.clear();
        FXCollections.sort(seq, new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return o1.charAt(0) - o2.charAt(0);
            }
            
        });
        observer.check1Permutation(seq, new int[] {4, 8, 0, 5, 6, 9, 7, 1, 2, 3});
    }
    
    @Test
    public void sortTest_empty() {
        ObservableList<String> seq = FXCollections.observableArrayList();
        final MockListObserver<String> observer = new MockListObserver<String>();
        seq.addListener(observer);
        FXCollections.sort(seq);
        observer.check0();
    }

    private void doSort(ObservableList<String> seq, boolean permutation) {
        final MockListObserver<String> observer = new MockListObserver<String>();
        seq.addListener(observer);
        FXCollections.sort(seq);
        assertArrayEquals(new String[]{"five", "four", "one", "three", "two"}, seq.toArray(new String[0]));
        if (permutation) {
            observer.check1Permutation(seq, new int[] {2, 4, 3, 1, 0});
        } else {
            observer.check1();
        }
        observer.clear();
        FXCollections.sort(seq, new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return -o1.compareTo(o2);
            }
        });
        assertArrayEquals(new String[]{"two", "three", "one", "four", "five"}, seq.toArray(new String[0]));
        if (permutation) {
            observer.check1Permutation(seq, new int[] {4, 3, 2, 1, 0});
        } else {
            observer.check1();
        }
    }

    @Test(expected=ClassCastException.class)
    @SuppressWarnings("unchecked")
    public void sortNotComparableTest() {
        ObservableList seq = FXCollections.observableArrayList(new Object(), new Object(), new Object());
        FXCollections.sort(seq);
    }

    @Test
    public void emptyObservableListTest() {
        ObservableList<String> seq = FXCollections.<String>emptyObservableList();
        assertEquals(0, seq.size());
        try {
            seq.get(0);
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
        testIfUnmodifiable(seq);
    }

    @Test
    public void singletonObservableListTest() {
        ObservableList<String> seq = FXCollections.singletonObservableList("foo");
        assertEquals(1, seq.size());
        assertEquals("foo", seq.get(0));
        assertEquals("foo", seq.iterator().next());
        assertTrue(seq.contains("foo"));
        testIfUnmodifiable(seq);
    }

    @Test
    public void unmodifiableObservableListTest() {
        ObservableList<String> seq = FXCollections.unmodifiableObservableList(FXCollections.observableArrayList("foo"));
        testIfUnmodifiable(seq);
    }

    @Test
    public void unmodifiableObservableMapTest() {
        final ObservableMap<String, String> map = FXCollections.observableMap(new HashMap<String, String>());
        ObservableMap<String, String> om = FXCollections.unmodifiableObservableMap(map);
        map.put("foo", "bar");
        testIfUnmodifiable(om);
    }

    @Test
    public void emptyObservableSetTest() {
        ObservableSet set = FXCollections.emptyObservableSet();
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
        assertFalse(set.contains("foo"));
        assertFalse(set.containsAll(Arrays.asList("foo", "foo2")));
        assertTrue(set.containsAll(new LinkedList()));
    }

    @Test
    public void unmodifiableObservableSetTest() {
        ObservableSet<String> set = FXCollections.unmodifiableObservableSet(FXCollections.observableSet("foo", "foo2"));
        try {
            set.add("foo3");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            set.addAll(Arrays.asList("foo3", "foo4"));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            set.remove("foo");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            set.removeAll(Arrays.asList("foo", "foo2"));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            set.retainAll(Arrays.asList("foo"));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            set.clear();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            final Iterator<String> setIterator = set.iterator();
            if (setIterator.hasNext()) {
                setIterator.next();
                setIterator.remove();
                fail("Expected UnsupportedOperationException");
            }
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void checkedListenerObservableSetTest() {
        ObservableSet<String> set = FXCollections.checkedObservableSet(FXCollections.observableSet("foo", "foo2"), String.class);
        final MockSetObserver<String> observer = new MockSetObserver<String>();
        set.addListener(observer);
        try {
            set.add("foo3");
            observer.assertAdded(Tuple.tup("foo3"));
            set.add("foo4");
            observer.assertAdded(1, Tuple.tup("foo4"));
            set.addAll(Arrays.asList("foo5", "foo6"));
            observer.assertAdded(2, Tuple.tup("foo5"));
            observer.assertAdded(3, Tuple.tup("foo6"));
            assertEquals(4, observer.getCallsNumber());
            set.remove("foo2");
            observer.assertRemoved(4, Tuple.tup("foo2"));
            assertEquals(5, observer.getCallsNumber());
        } catch (UnsupportedOperationException e) {
        }
    }

    private void testIfUnmodifiable(Map<String, String> map) {
        try {
            map.put("foo", "bar");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            map.put("fooo", "bar");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            map.remove("foo");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            map.clear();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            Map<String, String> putMap = new HashMap<String, String>();
            putMap.put("bar", "bar");
            map.putAll(putMap);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        testIfUnmodifiable(map.values());
        testIfUnmodifiable(map.keySet());
    }

    /**
     * Note that observableArrayList should contain "foo" on the first position to be fully tested
     * @param seq
     */
    private void testIfUnmodifiable(ObservableList<String> seq) {
        testIfUnmodifiable((List<String>)seq);
        try {
            seq.addAll("foo");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            seq.setAll("foo");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        if (seq.isEmpty() ) {
            testIfUnmodifiable(seq.subList(0, 0));
        } else {
            testIfUnmodifiable(seq.subList(0, 1));
        }
    }

    private void testIfUnmodifiable(List<String> list) {
        testIfUnmodifiable((Collection<String>)list);
        try {
            list.add(0, "foo");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            list.addAll(0, Arrays.asList("foo"));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            final ListIterator<String> listIterator = list.listIterator();
            if (listIterator.hasNext()) {
                listIterator.next();
                listIterator.remove();
                fail("Expected UnsupportedOperationException");
            }
        } catch (UnsupportedOperationException e) {
        }
        try {
            final ListIterator<String> listIterator = list.listIterator();
            if (listIterator.hasNext()) {
                listIterator.next();
                listIterator.set("foo");
                fail("Expected UnsupportedOperationException");
            }
        } catch (UnsupportedOperationException e) {
        }
        try {
            list.listIterator().add("foo");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    private void testIfUnmodifiable(Collection<String> col) {
        try {
            col.add("foo");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            col.addAll(Arrays.asList("foo"));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            if (!col.isEmpty()) {
                col.clear();
                fail("Expected UnsupportedOperationException");
            }
        } catch (UnsupportedOperationException e) {
        }
        try {
            if (!col.isEmpty()) {
                col.remove("foo");
                fail("Expected UnsupportedOperationException");
            }
        } catch (UnsupportedOperationException e) {
        }
        try {
            if (!col.isEmpty()) {
                col.removeAll(Arrays.asList("foo"));
                fail("Expected UnsupportedOperationException");
            }
        } catch (UnsupportedOperationException e) {
        }
        try {
            if (!col.isEmpty()) {
                col.retainAll(Arrays.asList("bar"));
                fail("Expected UnsupportedOperationException");
            }
        } catch (UnsupportedOperationException e) {
        }
        try {
            final Iterator<String> it = col.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
                fail("Expected UnsupportedOperationException");
            }
        } catch (UnsupportedOperationException e) {
        }
    }

    private static class NonSortableObservableList extends AbstractList<String> implements ObservableList<String> {

        private List<String> backingList = new ArrayList<String>();
        private Set<ListChangeListener<? super String>> listeners = new HashSet<ListChangeListener<? super String>>();
        private Set<InvalidationListener> invalidationListeners = new HashSet<InvalidationListener>();

        @Override
        public boolean addAll(String... ts) {
            return addAll(Arrays.asList(ts));
        }

        @Override
        public boolean setAll(String... ts) {
            return setAll(Arrays.asList(ts));
        }

        @Override
        public boolean setAll(Collection<? extends String> clctn) {
            final List<String> copy = new ArrayList<String>(this);
            clear();
            boolean ret = addAll(clctn);
            for (ListChangeListener<? super String> l : listeners) {
                l.onChanged(new ListChangeListener.Change<String>(this) {
                    
                    boolean valid = true;

                    @Override
                    public int getFrom() {
                        return 0;
                    }

                    @Override
                    public int getTo() {
                        return size();
                    }

                    @Override
                    public List<String> getRemoved() {
                        return copy;
                    }

                    @Override
                    public boolean wasPermutated() {
                        return false;
                    }

                    @Override
                    public boolean next() {
                        if (valid) {
                            valid = false;
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public void reset() {
                    }

                    @Override
                    public int[] getPermutation() {
                        return new int[0];
                    }

                });
            }
            for (InvalidationListener listener : invalidationListeners) {
                listener.invalidated(this);
            }
            return ret;
        }

        @Override
        public boolean addAll(Collection<? extends String> c) {
            return backingList.addAll(c);
        }

        @Override
        public void clear() {
            backingList.clear();
        }

        @Override
        public String get(int index) {
            return backingList.get(index);
        }

        @Override
        public int size() {
            return backingList.size();
        }

        @Override
        public void addListener(InvalidationListener listener) {
            invalidationListeners.add(listener);
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            invalidationListeners.remove(listener);
        }

        @Override
        public void addListener(ListChangeListener<? super String> ll) {
            listeners.add(ll);
        }

        @Override
        public void removeListener(ListChangeListener<? super String> ll) {
            listeners.remove(ll);
        }

        @Override
        public boolean removeAll(String... es) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean retainAll(String... es) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void remove(int i, int i1) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }
}
