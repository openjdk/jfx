/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.collections.VetoableListDecorator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 */
public class VetoableObservableListTest {

    private static class Call {

        public Call(List<String> added, int[] removed) {
            this.added = added;
            this.removed = removed;
        }

        int[] removed;
        List<String> added;
    }

    ObservableList<String> list;
    List<Call> calls;

    private void assertCallCount(int count) {
        assertEquals(count, calls.size());
    }

    private void assertCall(int number, String[] added, int[] removed) {
        Call c = calls.get(number);
        assertArrayEquals(removed, c.removed);
        assertArrayEquals(added, c.added.toArray(new String[0]));
    }

    private void assertSingleCall(String[] added, int[] removed) {
        assertCallCount(1);
        assertCall(0, added, removed);
    }

    @Before
    public void setUp() {
        calls = new ArrayList<Call>();
        list = new VetoableListDecorator<String>(FXCollections.<String>observableArrayList()) {

            @Override
            protected void onProposedChange(List<String> added, int[] removed) {
                calls.add(new Call(added, removed));
            }
        };
        list.addAll("foo", "bar", "ham", "eggs");
        calls.clear();
    }


    @Test(expected=NullPointerException.class)
    @Ignore
    public void testNull_add() {
        list.add(null);

    }

    @Test(expected=NullPointerException.class)
    @Ignore
    public void testNull_add_indexed() {
        list.add( 1, null);

    }

    @Test(expected=NullPointerException.class)
    @Ignore
    public void testNull_addAll_collection() {
        list.addAll(Arrays.asList("a", null, "b"));

    }

    @Test(expected=NullPointerException.class)
    @Ignore
    public void testNull_addAll() {
        list.addAll("a", null, "b");

    }

    @Test(expected=NullPointerException.class)
    @Ignore
    public void testNull_addAll_collection_indexed() {
        list.addAll(1, Arrays.asList("a", null, "b"));

    }

    @Test(expected=NullPointerException.class)
    @Ignore
    public void testNull_set() {
        list.set(1, null);

    }

    @Test(expected=NullPointerException.class)
    @Ignore
    public void testNull_setAll() {
        list.setAll("a", null);

    }

    @Test(expected=NullPointerException.class)
    @Ignore
    public void testNull_setAll_collection() {
        list.setAll(Arrays.asList("a", null, "b"));
    }

    @Test(expected=NullPointerException.class)
    @Ignore
    public void testNull_listIterator_add() {
        list.listIterator().add(null);

    }
    @Test(expected=NullPointerException.class)
    @Ignore
    public void testNull_listIterator_set() {
        ListIterator<String> it = list.listIterator();
        it.next();
        it.set(null);
    }

    @Test(expected=NullPointerException.class)
    @Ignore
    public void testNull_subList_add() {
        list.subList(0, 1).add(null);
    }

    @Test(expected = NullPointerException.class)
    @Ignore
    public void testNull_subList_add_indexed() {
        list.subList(0, 1).add(0, null);
    }

    @Test(expected = NullPointerException.class)
    @Ignore
    public void testNull_subList_addAll() {
        list.subList(0, 1).addAll(Collections.<String>singleton(null));
    }

    @Test(expected = NullPointerException.class)
    @Ignore
    public void testNull_subList_addAll_indexed() {
        list.subList(0, 1).addAll(0, Collections.<String>singleton(null));
    }

    @Test(expected = NullPointerException.class)
    @Ignore
    public void testNull_subList_set() {
        list.subList(0, 1).set(0, null);
    }

    @Test(expected = NullPointerException.class)
    @Ignore
    public void testNull_subList_listIterator() {
        list.subList(0, 1).listIterator().add(null);
    }

    @Test
    public void testAdd() {
        list.add("a");
        assertSingleCall(new String[] {"a"}, new int[] {4,4});
    }

    @Test
    public void testAdd_indexed() {
        list.add(1, "a");
        assertSingleCall(new String[] {"a"}, new int[] {1,1});
    }

    @Test
    public void testAddAll() {
        list.addAll("a", "b");
        assertSingleCall(new String[] {"a", "b"}, new int[] {4,4});
    }

    @Test
    public void testAddAll_indexed() {
        list.addAll(1, Arrays.asList("a", "b"));
        assertSingleCall(new String[] {"a", "b"}, new int[] {1,1});
    }

    @Test
    public void testClear() {
        list.clear();
        assertSingleCall(new String[0], new int[] {0,4});
    }

    @Test
    public void testRemove() {
        list.remove("bar");
        assertSingleCall(new String[0], new int[] {1,2});
    }

    @Test
    public void testRemove_indexed() {
        list.remove(0);
        assertSingleCall(new String[0], new int[] {0,1});
    }

    @Test
    public void testRemoveAll() {
        list.removeAll(Arrays.asList("bar", "eggs", "foobar"));
        assertSingleCall(new String[0], new int[] {1,2,3,4});
    }

    @Test
    public void testRetainAll() {
        list.retainAll(Arrays.asList("foo", "barfoo", "ham"));
        assertSingleCall(new String[0], new int[] {1,2,3,4});
    }

    @Test
    public void testSet() {
        list.set(1, "foobar");
        assertSingleCall(new String[] {"foobar"}, new int[] {1,2});
    }

    @Test
    public void testSetAll() {
        list.setAll("a", "b");
        assertSingleCall(new String[] {"a", "b"}, new int[] {0, 4});
    }

    @Test
    public void testIterator_remove() {
        final Iterator<String> iterator = list.iterator();
        iterator.next();
        iterator.remove();
        assertSingleCall(new String[0], new int[] {0,1});
    }

    @Test
    public void testListIterator_add() {
        list.listIterator().add("a");
        assertSingleCall(new String[] {"a"}, new int[] {0,0});
    }

    @Test
    public void testListIterator_set() {
        final ListIterator<String> listIterator = list.listIterator();
        listIterator.next();
        listIterator.set("a");
        assertSingleCall(new String[] {"a"}, new int[] {0,1});
    }

    @Test
    public void testSubList_add() {
        list.subList(0, 1).add("b");
        assertSingleCall(new String[] {"b"}, new int[] {1,1});
    }

    @Test
    public void testSubList_addAll() {
        list.subList(0, 1).addAll(Arrays.asList("a", "b"));
        assertSingleCall(new String[] {"a", "b"}, new int[] {1,1});
    }

    @Test
    public void testSubList_clear() {
        list.subList(0, 1).clear();
        assertSingleCall(new String[0], new int[] {0, 1});
    }

    @Test
    public void testSubList_remove() {
        list.subList(0, 1).remove(0);
        assertSingleCall(new String[0], new int[] {0, 1});
    }

    @Test
    public void testSubList_removeAll() {
        list.subList(0, 1).removeAll(Arrays.asList("foo", "bar"));
        assertSingleCall(new String[0], new int[] {0, 1});
    }

    @Test
    public void testSubList_retainAll() {
        list.subList(0, 1).retainAll(Arrays.asList("foo", "bar"));
        assert(calls.isEmpty());
    }

    @Test
    public void testSubList_set() {
        list.subList(0, 1).set(0, "a");
        assertSingleCall(new String[] {"a"}, new int[] {0,1});
    }

    @Test
    public void testSubList_iterator_quicktest() {
        final ListIterator<String> iterator = list.subList(0, 1).listIterator();
        iterator.next();
        iterator.remove();
        iterator.add("a");
        iterator.previous();
        iterator.set("b");
        assertCallCount(3);
        assertCall(0, new String[0], new int[] {0, 1});
        assertCall(1, new String[] {"a"}, new int[] {0, 0});
        assertCall(2, new String[] {"b"}, new int[] {0, 1});
    }

    @Test
    public void testConcurrencyAdd() {
        boolean exception = false;
        List<String> sub = list.subList(0, 1);
        list.add("x");
        assertCallCount(1);
        try {
            sub.add("y");
            fail();
        } catch (ConcurrentModificationException e) {
        }
        assertCallCount(1);
    }
    @Test
    public void testConcurrencyAddAll() {
        boolean exception = false;
        List<String> sub = list.subList(0, 1);
        list.add("x");
        assertCallCount(1);
        try {
            sub.addAll(Arrays.asList("y", "z"));
            fail();
        } catch (ConcurrentModificationException e) {
        }
        assertCallCount(1);
    }
    @Test
    public void testConcurrencyClear() {
        boolean exception = false;
        List<String> sub = list.subList(0, 1);
        list.add("x");
        assertCallCount(1);
        try {
            sub.clear();
            fail();
        } catch (ConcurrentModificationException e) {
        }
        assertCallCount(1);
    }
    @Test
    public void testConcurrencyRemove() {
        boolean exception = false;
        List<String> sub = list.subList(0, 1);
        list.add("x");
        assertCallCount(1);
        try {
            sub.remove("foo");
            fail();
        } catch (ConcurrentModificationException e) {
        }
        assertCallCount(1);
    }
    @Test
    public void testConcurrencyRemoveAll() {
        boolean exception = false;
        List<String> sub = list.subList(0, 1);
        list.add("x");
        assertCallCount(1);
        try {
            sub.removeAll(Arrays.asList("x"));
            fail();
        } catch (ConcurrentModificationException e) {
        }
        assertCallCount(1);
    }
    @Test
    public void testConcurrencyRetainAll() {
        boolean exception = false;
        List<String> sub = list.subList(0, 1);
        list.add("x");
        assertCallCount(1);
        try {
            sub.retainAll(Arrays.asList("x"));
            fail();
        } catch (ConcurrentModificationException e) {
        }
        assertCallCount(1);
    }
    @Test
    public void testConcurrencySet() {
        boolean exception = false;
        List<String> sub = list.subList(0, 1);
        list.add("x");
        assertCallCount(1);
        try {
            sub.set(0, "z");
            fail();
        } catch (ConcurrentModificationException e) {
        }
        assertCallCount(1);
    }

    @Test
    public void testConcurrencyIteratorRemove() {
        boolean exception = false;
        ListIterator<String> it = list.listIterator();
        it.next();
        list.add("x");
        assertCallCount(1);
        try {
            it.remove();
        } catch (ConcurrentModificationException e) {
        }
        assertCallCount(1);
    }
    @Test
    public void testConcurrencyIteratorAdd() {
        boolean exception = false;
        ListIterator<String> it = list.listIterator();
        it.next();
        list.add("x");
        assertCallCount(1);
        try {
            it.add("g");
        } catch (ConcurrentModificationException e) {
        }
        assertCallCount(1);
    }
    @Test
    public void testConcurrencyIteratorSet() {
        boolean exception = false;
        ListIterator<String> it = list.listIterator();
        it.next();
        list.add("x");
        assertCallCount(1);
        try {
            it.set("p");
        } catch (ConcurrentModificationException e) {
        }
        assertCallCount(1);
    }

    @Test(expected=ConcurrentModificationException.class)
    public void testConcurrencyIteratorIterator() {
        ListIterator<String> it1 = list.listIterator();
        ListIterator<String> it2 = list.listIterator();
        it1.next();
        it2.next();
        it1.remove();
        it2.remove();
    }


    @Test
    public void testNonConcurrency() {
        ListIterator<String> it = list.listIterator();
        it.next();
        it.remove();
        it.next();
        it.remove();
        it.add("foo");
        it.add("bar");
        it.previous();
        it.set("foobar");
    }

    @Test
    public void testSubListCreatedOnChangeValid() {
        final List<List<? extends String>> subLists = new ArrayList<>();

        list.addListener((ListChangeListener<String>) c -> {
            subLists.add(c.getList().subList(0, 1));
        });

        list.add("abc");
        assertEquals(1, subLists.size());
        subLists.get(0).size(); // Assert not throwing Exception
        subLists.clear();

        list.add(0, "abc");
        assertEquals(1, subLists.size());
        subLists.get(0).size(); // Assert not throwing Exception
        subLists.clear();

        list.addAll(0, Arrays.asList("abc", "bcd"));
        assertEquals(1, subLists.size());
        subLists.get(0).size(); // Assert not throwing Exception
        subLists.clear();

        list.addAll(Arrays.asList("abc", "bcd"));
        assertEquals(1, subLists.size());
        subLists.get(0).size(); // Assert not throwing Exception
        subLists.clear();

        list.remove(0);
        assertEquals(1, subLists.size());
        subLists.get(0).size(); // Assert not throwing Exception
        subLists.clear();

        list.remove("abc");
        assertEquals(1, subLists.size());
        subLists.get(0).size(); // Assert not throwing Exception
        subLists.clear();

        list.removeAll("abc");
        assertEquals(1, subLists.size());
        subLists.get(0).size(); // Assert not throwing Exception
        subLists.clear();

        list.retainAll("bcd");
        assertEquals(1, subLists.size());
        subLists.get(0).size(); // Assert not throwing Exception
        subLists.clear();


        list.setAll("foo", "bar", "ham", "eggs");
        subLists.clear();

        list.subList(0, 2).add("a");
        assertEquals(1, subLists.size());
        subLists.get(0).size(); // Assert not throwing Exception
        subLists.clear();

        list.subList(0, 2).remove(0);
        assertEquals(1, subLists.size());
        subLists.get(0).size(); // Assert not throwing Exception
        subLists.clear();

        Iterator<String> it = list.iterator();
        it.next();
        it.remove();
        assertEquals(1, subLists.size());
        subLists.get(0).size(); // Assert not throwing Exception
        subLists.clear();

        list.listIterator().add("abc");
        assertEquals(1, subLists.size());
        subLists.get(0).size(); // Assert not throwing Exception
        subLists.clear();

    }

}
