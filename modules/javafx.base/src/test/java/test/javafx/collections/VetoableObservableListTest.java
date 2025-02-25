/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.collections.VetoableListDecorator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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

    @BeforeEach
    public void setUp() {
        calls = new ArrayList<>();
        list = new VetoableListDecorator<>(FXCollections.<String>observableArrayList()) {

            @Override
            protected void onProposedChange(List<String> added, int... removed) {
                calls.add(new Call(added, removed));
            }
        };
        list.addAll("foo", "bar", "ham", "eggs");
        calls.clear();
    }


    @Test
    @Disabled
    public void testNull_add() {
        assertThrows(NullPointerException.class, () -> list.add(null));
    }

    @Test
    @Disabled
    public void testNull_add_indexed() {
        assertThrows(NullPointerException.class, () -> list.add(1, null));
    }

    @Test
    @Disabled
    public void testNull_addAll_collection() {
        assertThrows(NullPointerException.class, () -> list.addAll(Arrays.asList("a", null, "b")));
    }

    @Test
    @Disabled
    public void testNull_addAll() {
        assertThrows(NullPointerException.class, () -> list.addAll(Arrays.asList("a", null, "b")));
    }

    @Test
    @Disabled
    public void testNull_addAll_collection_indexed() {
        assertThrows(NullPointerException.class, () -> list.addAll(1, Arrays.asList("a", null, "b")));
    }

    @Test
    @Disabled
    public void testNull_set() {
        assertThrows(NullPointerException.class, () -> list.set(1, null));
    }

    @Test
    @Disabled
    public void testNull_setAll() {
        assertThrows(NullPointerException.class, () -> list.setAll("a", null));
    }

    @Test
    @Disabled
    public void testNull_setAll_collection() {
        assertThrows(NullPointerException.class, () -> list.setAll(Arrays.asList("a", null, "b")));
    }

    @Test
    @Disabled
    public void testNull_listIterator_add() {
        assertThrows(NullPointerException.class, () -> list.listIterator().add(null));
    }

    @Test
    @Disabled
    public void testNull_listIterator_set() {
        ListIterator<String> it = list.listIterator();
        it.next();
        assertThrows(NullPointerException.class, () -> it.set(null));
    }

    @Test
    @Disabled
    public void testNull_subList_add() {
        assertThrows(NullPointerException.class, () -> list.subList(0, 1).add(null));
    }

    @Test
    @Disabled
    public void testNull_subList_add_indexed() {
        assertThrows(NullPointerException.class, () -> list.subList(0, 1).add(0, null));
    }

    @Test
    @Disabled
    public void testNull_subList_addAll() {
        assertThrows(NullPointerException.class, () -> list.subList(0, 1).addAll(Collections.<String>singleton(null)));
    }

    @Test
    @Disabled
    public void testNull_subList_addAll_indexed() {
        assertThrows(NullPointerException.class, () -> list.subList(0, 1).addAll(0, Collections.<String>singleton(null)));
    }

    @Test
    @Disabled
    public void testNull_subList_set() {
        assertThrows(NullPointerException.class, () -> list.subList(0, 1).set(0, null));
    }

    @Test
    @Disabled
    public void testNull_subList_listIterator() {
        assertThrows(NullPointerException.class, () -> list.subList(0, 1).listIterator().add(null));
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
    public void testAddAll_subList() {
        list.addAll(list.subList(0, 2));
        assertEquals(List.of("foo", "bar", "ham", "eggs", "foo", "bar"), list);
        assertSingleCall(new String[] {"foo", "bar"}, new int[] {4, 4});
    }

    @Test
    public void testAddAll_indexed() {
        list.addAll(1, Arrays.asList("a", "b"));
        assertSingleCall(new String[] {"a", "b"}, new int[] {1,1});
    }

    @Test
    public void testAddAll_indexed_subList() {
        list.addAll(1, list.subList(0, 2));
        assertEquals(List.of("foo", "foo", "bar", "bar", "ham", "eggs"), list);
        assertSingleCall(new String[] {"foo", "bar"}, new int[] {1, 1});
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

        list.setAll("a", "b", "c", "d", "e", "f");
        calls.clear();
        list.removeAll(List.of("b", "c", "d"));
        assertEquals(List.of("a", "e", "f"), list);
        assertSingleCall(new String[0], new int[] {1, 4});

        list.setAll("a", "b", "c", "d", "e", "f");
        calls.clear();
        list.removeAll(List.of("a", "b", "d", "e", "f"));
        assertEquals(List.of("c"), list);
        assertSingleCall(new String[0], new int[] {0, 2, 3, 6});
    }

    @Test
    public void testRemoveAll_subList() {
        list.removeAll(list.subList(0, 1));
        assertEquals(List.of("bar", "ham", "eggs"), list);
        assertSingleCall(new String[0], new int[] {0, 1});
    }

    @Test
    public void testRetainAll() {
        list.retainAll(Arrays.asList("foo", "barfoo", "ham"));
        assertSingleCall(new String[0], new int[] {1,2,3,4});

        list.setAll("a", "b", "c", "d", "e", "f");
        calls.clear();
        list.retainAll(List.of("a", "f"));
        assertEquals(List.of("a", "f"), list);
        assertSingleCall(new String[0], new int[] {1, 5});

        list.setAll("a", "b", "c", "d", "e", "f");
        calls.clear();
        list.retainAll(List.of("c"));
        assertEquals(List.of("c"), list);
        assertSingleCall(new String[0], new int[] {0, 2, 3, 6});
    }

    @Test
    public void testRetainAll_subList() {
        list.retainAll(list.subList(0, 2));
        assertEquals(List.of("foo", "bar"), list);
        assertSingleCall(new String[0], new int[] {2, 4});
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
    public void testSetAll_subList() {
        list.setAll(list.subList(0, 2));
        assertEquals(List.of("foo", "bar"), list);
        assertSingleCall(new String[] {"foo", "bar"}, new int[] {0, 4});
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
    public void testSubList_addAll_subList() {
        var subList = list.subList(0, 3);
        subList.addAll(subList.subList(0, 2));
        assertEquals(List.of("foo", "bar", "ham", "foo", "bar"), subList);
        assertSingleCall(new String[] {"foo", "bar"}, new int[] {3, 3});
    }

    @Test
    public void testSubList_addAll_indexed_subList() {
        var subList = list.subList(0, 3);
        subList.addAll(1, subList.subList(0, 2));
        assertEquals(List.of("foo", "foo", "bar", "bar", "ham"), subList);
        assertSingleCall(new String[] {"foo", "bar"}, new int[] {1, 1});
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
    public void testSubList_removeAll_subList() {
        var subList = list.subList(0, 3);
        subList.removeAll(subList.subList(0, 1));
        assertEquals(List.of("bar", "ham"), subList);
        assertSingleCall(new String[0], new int[] {0, 1});
    }

    @Test
    public void testSubList_retainAll() {
        list.subList(0, 1).retainAll(Arrays.asList("foo", "bar"));
        assert(calls.isEmpty());
    }

    @Test
    public void testSubList_retainAll_subList() {
        var subList = list.subList(0, 3);
        subList.retainAll(subList.subList(0, 1));
        assertEquals(List.of("foo"), subList);
        assertSingleCall(new String[0], new int[] {1, 3});
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

    @Test
    public void testConcurrencyIteratorIterator() {
        assertThrows(ConcurrentModificationException.class, () -> {
            ListIterator<String> it1 = list.listIterator();
            ListIterator<String> it2 = list.listIterator();
            it1.next();
            it2.next();
            it1.remove();
            it2.remove();
        });
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
