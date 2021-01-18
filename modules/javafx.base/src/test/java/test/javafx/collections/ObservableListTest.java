/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests for ObservableList.
 *
 */
@RunWith(Parameterized.class)
public class ObservableListTest  {

    static final List<String> EMPTY = Collections.emptyList();
    final Callable<ObservableList<String>> listFactory;
    ObservableList<String> list;
    MockListObserver<String> mlo;


    public ObservableListTest(final Callable<ObservableList<String>> listFactory) {
        this.listFactory = listFactory;
    }

    @Parameterized.Parameters
    public static Collection createParameters() {
        Object[][] data = new Object[][] {
            { TestedObservableLists.ARRAY_LIST },
            { TestedObservableLists.LINKED_LIST },
            { TestedObservableLists.VETOABLE_LIST },
            { TestedObservableLists.CHECKED_OBSERVABLE_ARRAY_LIST },
            { TestedObservableLists.SYNCHRONIZED_OBSERVABLE_ARRAY_LIST },
            { TestedObservableLists.OBSERVABLE_LIST_PROPERTY }
         };
        return Arrays.asList(data);
    }

    @Before
    public void setUp() throws Exception {
        list = listFactory.call();
        mlo = new MockListObserver<String>();
        list.addListener(mlo);

        useListData("one", "two", "three");
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

    // ========== observer add/remove tests ==========

    @Test
    public void testObserverAddRemove() {
        MockListObserver<String> mlo2 = new MockListObserver<String>();
        list.addListener(mlo2);
        list.removeListener(mlo);
        list.add("xyzzy");
        mlo.check0();
        mlo2.check1AddRemove(list, EMPTY, 3, 4);
    }

    @Test
    @Ignore
    public void testObserverAddTwice() {
        list.addListener(mlo); // add it a second time
        list.add("plugh");
        mlo.check1AddRemove(list, EMPTY, 3, 4);
    }

    @Test
    public void testObserverRemoveTwice() {
        list.removeListener(mlo);
        list.removeListener(mlo);
        list.add("plugh");
        mlo.check0();
    }

    // ========== list mutation tests ==========

    @Test
    public void testAddToEmpty() {
        useListData();
        list.add("asdf");
        mlo.check1AddRemove(list, EMPTY, 0, 1);
    }

    @Test
    public void testAddAtEnd() {
        list.add("four");
        mlo.check1AddRemove(list, EMPTY, 3, 4);
    }

    @Test
    public void testAddInMiddle() {
        list.add(1, "xyz");
        mlo.check1AddRemove(list, EMPTY, 1, 2);
    }

    @Test
    public void testAddSeveralToEmpty() {
        useListData();
        list.addAll(Arrays.asList("alpha", "bravo", "charlie"));
        mlo.check1AddRemove(list, EMPTY, 0, 3);
    }

    @Test
    public void testAddSeveralAtEnd() {
        list.addAll(Arrays.asList("four", "five"));
        mlo.check1AddRemove(list, EMPTY, 3, 5);
    }

    @Test
    public void testAddSeveralInMiddle() {
        list.addAll(1, Arrays.asList("a", "b"));
        mlo.check1AddRemove(list, EMPTY, 1, 3);
    }

    @Test
    public void testClearNonempty() {
        list.clear();
        mlo.check1AddRemove(list, Arrays.asList("one", "two", "three"), 0, 0);
    }

    @Test
    public void testRemoveByIndex() {
        String r = list.remove(1);
        mlo.check1AddRemove(list, Arrays.asList("two"), 1, 1);
        assertEquals("two", r);
    }

    @Test
    public void testRemoveObject() {
        useListData("one", "x", "two", "three");
        boolean b = list.remove("two");
        mlo.check1AddRemove(list, Arrays.asList("two"), 2, 2);
        assertTrue(b);
    }

    @Test
    public void testRemoveNull() {
        useListData("one", "two", null, "three");
        boolean b = list.remove(null);
        mlo.check1AddRemove(list, Arrays.asList((String)null), 2, 2);
        assertTrue(b);
    }

    @Test
    public void testRemoveAll() {
        useListData("one", "two", "three", "four", "five");
        list.removeAll(Arrays.asList("one", "two", "four", "six"));
        assertEquals(2, mlo.calls.size());
        mlo.checkAddRemove(0, list, Arrays.asList("one", "two"), 0, 0);
        mlo.checkAddRemove(1, list, Arrays.asList("four"), 1, 1);
    }

    @Test
    public void testRemoveAll_1() {
        useListData("a", "c", "d", "c");
        list.removeAll(Arrays.asList("c"));
        assertEquals(2, mlo.calls.size());
        mlo.checkAddRemove(0, list, Arrays.asList("c"), 1, 1);
        mlo.checkAddRemove(1, list, Arrays.asList("c"), 2, 2);
    }


    @Test
    public void testRemoveAll_2() {
        useListData("one", "two");
        list.removeAll(Arrays.asList("three", "four"));
        mlo.check0();
    }

    @Test
    public void testRemoveAll_3() {
        useListData("a", "c", "d", "c");
        list.removeAll(Arrays.asList("d"));
        assertEquals(1, mlo.calls.size());
        mlo.checkAddRemove(0, list, Arrays.asList("d"), 2, 2);
    }

    @Test
    public void testRemoveAll_4() {
        useListData("a", "c", "d", "c");
        list.removeAll(Arrays.asList("d", "c"));
        assertEquals(1, mlo.calls.size());
        mlo.checkAddRemove(0, list, Arrays.asList("c", "d", "c"), 1, 1);
    }

    @Test
    public void testRetainAll() {
        useListData("one", "two", "three", "four", "five");
        list.retainAll(Arrays.asList("two", "five", "six"));
        assertEquals(2, mlo.calls.size());
        mlo.checkAddRemove(0, list, Arrays.asList("one"), 0, 0);
        mlo.checkAddRemove(1, list, Arrays.asList("three", "four"), 1, 1);
    }

    @Test
    public void testRetainAllEmptySource() {
        // grab default data
        List<String> data = new ArrayList<>(list);
        // retain none == remove all
        list.retainAll();
        assertTrue(list.isEmpty());
        mlo.check1AddRemove(list, data, 0, 0);
    }

    @Test
    public void testRemoveNonexistent() {
        useListData("one", "two", "x", "three");
        boolean b = list.remove("four");
        mlo.check0();
        assertFalse(b);
    }

    @Test
    public void testSet() {
        String r = list.set(1, "fnord");
        mlo.check1AddRemove(list, Arrays.asList("two"), 1, 2);
        assertEquals("two", r);
    }

    @Test
    public void testSetAll() {
        useListData("one", "two", "three");
        boolean r = list.setAll("one");
        assertTrue(r);

        r = list.setAll("one", "four", "five");
        assertTrue(r);

        r = list.setAll();
        assertTrue(r);

        r = list.setAll("one");
        assertTrue(r);
    }

    @Test
    public void testSetAllNoUpdate() {
        useListData();
        boolean r = list.setAll();
        assertFalse(r);
    }

    @Test
    public void testObserverCanRemoveObservers() {
        final ListChangeListener<String> listObserver = change -> {
            change.getList().removeListener(mlo);
        };
        list.addListener(listObserver);
        list.add("x");
        mlo.clear();
        list.add("y");
        mlo.check0();
        list.removeListener(listObserver);


        final StringListChangeListener listener = new StringListChangeListener();
        list.addListener(listener);
        list.add("z");
        assertEquals(listener.counter, 1);
        list.add("zz");
        assertEquals(listener.counter, 1);
    }

    @Test
    public void testEqualsAndHashCode() {
        final List<String> other = Arrays.asList("one", "two", "three");
        assertTrue(list.equals(other));
        assertEquals(list.hashCode(), other.hashCode());
    }


    private static class StringListChangeListener implements ListChangeListener<String> {

        private int counter;

        @Override
        public void onChanged(final Change<? extends String> change) {
            change.getList().removeListener(this);
            ++counter;
        }
    }
}
