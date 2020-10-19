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

import com.sun.javafx.collections.ObservableListWrapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListWrapperShim;
import javafx.collections.transformation.FilteredList;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class FilteredListTest {

    private ObservableList<String> list;
    private MockListObserver<String> mlo;
    private FilteredList<String> filteredList;

    @Before
    public void setUp() {
        list = FXCollections.observableArrayList();
        list.addAll("a", "c", "d", "c");
        Predicate<String> predicate = (String e) -> !e.equals("c");
        mlo = new MockListObserver<String>();
        filteredList = new FilteredList<>(list, predicate);
        filteredList.addListener(mlo);
    }

    @Test
    public void test_rt35857_removeFiltered() {
        ObservableList<String> copyList = FXCollections.observableArrayList(list);
        // no relation, but use a different method to remove just to be on the super safe side
        filteredList.forEach(e -> copyList.remove(e));
        // list has duplicates!
        list.removeAll(filteredList);
        assertEquals(copyList, list);
    }

    @Test
    public void test_rt35857_retainFiltered() {
        ObservableList<String> copyFiltered = FXCollections.observableArrayList(filteredList);
        list.retainAll(filteredList);
        assertEquals("sanity: filteredList unchanged", copyFiltered, filteredList);
        assertEquals(filteredList, list);
    }

    private <E> void compareIndices(FilteredList<E> filtered) {
        ObservableList<? extends E> source = filtered.getSource();
        for (int i = 0; i < filtered.size(); i++) {
            // i as a view index
            int sourceIndex = filtered.getSourceIndex(i);
            assertEquals(i, filtered.getViewIndex(sourceIndex));
            assertSame(filtered.get(i), source.get(sourceIndex));
        }
        for (int i = 0; i < source.size(); i++) {
            // i as a source index
            int viewIndex = filtered.getViewIndex(i);
            if (viewIndex >= 0) {
                assertEquals(i, filtered.getSourceIndex(viewIndex));
                assertSame(source.get(i), filtered.get(viewIndex));
            }
        }
    }

    private void compareIndices() {
        compareIndices(filteredList);
    }

    @Test
    public void testLiveMode() {
        assertEquals(Arrays.asList("a", "d"), filteredList);
        mlo.check0();
    }

    @Test
    public void testLiveMode_Add() {
        list.clear();
        mlo.clear();
        assertEquals(Collections.emptyList(), filteredList);
        list.addAll("a", "c", "d", "c");
        assertEquals(Arrays.asList("a", "d"), filteredList);
        mlo.check1AddRemove(filteredList, Collections.<String>emptyList(), 0, 2);
        compareIndices();

        mlo.clear();
        list.add("c");
        mlo.check0();
        list.add(1, "b");
        assertEquals(Arrays.asList("a", "b", "d"), filteredList);
        mlo.check1AddRemove(filteredList, Collections.<String>emptyList(), 1, 2);
        compareIndices();
    }

    @Test
    public void testLiveMode_Remove() {
        list.removeAll(Arrays.asList("c"));
        assertEquals(Arrays.asList("a", "d"), filteredList);
        mlo.check0();
        compareIndices();

        mlo.clear();
        list.remove("a");
        assertEquals(Arrays.asList("d"), filteredList);
        mlo.check1AddRemove(filteredList, Arrays.asList("a"), 0, 0);
        compareIndices();
    }

    @Test
    public void testLiveMode_Permutation() {
        FXCollections.sort(list, (o1, o2) -> -o1.compareTo(o2));
        mlo.check1Permutation(filteredList, new int[] {1, 0});
        assertEquals(Arrays.asList("d", "a"), filteredList);
        compareIndices();
    }

    @Test
    public void testLiveMode_changeMatcher() {
        ObjectProperty<Predicate<String>> pProperty = new SimpleObjectProperty<>();
        pProperty.set((String e) -> !e.equals("c"));
        filteredList = new FilteredList<>(list);
        filteredList.predicateProperty().bind(pProperty);
        filteredList.addListener(mlo);
        assertEquals(Arrays.asList("a", "d"), filteredList);
        mlo.check0();
        pProperty.set((String s) -> !s.equals("d"));
        mlo.check1AddRemove(filteredList, Arrays.asList("a", "d"), 0, 3);
        compareIndices();
    }

    @Test
    public void testLiveMode_mutableElement() {
        ObservableList<Person> list = Person.createPersonsList("A", "BB", "C");

        FilteredList<Person> filtered = new FilteredList<>(list,
                (Person p) -> p.name.get().length() > 1);
        MockListObserver<Person> lo = new MockListObserver<>();
        filtered.addListener(lo);

        assertEquals(Arrays.asList(new Person("BB")), filtered);
        compareIndices(filtered);

        list.get(0).name.set("AA");
        lo.check1AddRemove(filtered, Collections.EMPTY_LIST, 0, 1);
        assertEquals(Person.createPersonsList("AA", "BB"), filtered);
        compareIndices(filtered);

        lo.clear();
        list.get(1).name.set("BBB");
        lo.check1Update(filtered, 1, 2);
        assertEquals(Person.createPersonsList("AA", "BBB"), filtered);
        compareIndices(filtered);

        lo.clear();
        list.get(1).name.set("B");
        lo.check1AddRemove(filtered, Person.createPersonsList("B"), 1, 1);
        assertEquals(Person.createPersonsList("AA"), filtered);
        compareIndices(filtered);
    }

    @Test
    public void testLiveMode_mutableElementEmptyList() {
        ObservableList<Person> list = Person.createPersonsList("A", "B", "C");

        FilteredList<Person> filtered = new FilteredList<>(list,
                (Person p) -> p.name.get().length() > 1);
        MockListObserver<Person> lo = new MockListObserver<>();
        filtered.addListener(lo);

        assertEquals(Collections.EMPTY_LIST, filtered);
        compareIndices(filtered);

        list.get(0).name.set("AA");
        lo.check1AddRemove(filtered, Collections.EMPTY_LIST, 0, 1);
        assertEquals(Person.createPersonsList("AA"), filtered);
        compareIndices(filtered);
    }

    @Test
    public void testLiveMode_mutableElements() {
        Person p1 = new Person("A");
        ObservableList<Person> list = Person.createPersonsList(
                p1, p1, new Person("BB"), new Person("B"), p1, p1, new Person("BC"), p1, new Person("C"));

        FilteredList<Person> filtered = new FilteredList<>(list,
                (Person p) -> p.name.get().length() > 1);
        MockListObserver<Person> lo = new MockListObserver<>();
        filtered.addListener(lo);

        assertEquals(Person.createPersonsList("BB", "BC"), filtered);

        p1.name.set("AA");
        lo.checkAddRemove(0, filtered, Collections.EMPTY_LIST, 0, 2);
        lo.checkAddRemove(1, filtered, Collections.EMPTY_LIST, 3, 5);
        lo.checkAddRemove(2, filtered, Collections.EMPTY_LIST, 6, 7);
        assertEquals(Person.createPersonsList("AA", "AA", "BB", "AA", "AA", "BC", "AA"), filtered);
        compareIndices(filtered);

        lo.clear();
        p1.name.set("AAA");
        lo.checkUpdate(0, filtered, 0, 2);
        lo.checkUpdate(1, filtered, 3, 5);
        lo.checkUpdate(2, filtered, 6, 7);
        assertEquals(Person.createPersonsList("AAA", "AAA", "BB", "AAA", "AAA", "BC", "AAA"), filtered);
        compareIndices(filtered);

        lo.clear();
        p1.name.set("A");
        lo.checkAddRemove(0, filtered, Person.createPersonsList("A", "A"), 0, 0);
        lo.checkAddRemove(1, filtered, Person.createPersonsList("A", "A"), 1, 1);
        lo.checkAddRemove(2, filtered, Person.createPersonsList("A"), 2, 2);
        assertEquals(Person.createPersonsList( "BB", "BC"), filtered);
        compareIndices(filtered);
    }

    private static class Updater<E> extends ObservableListWrapper<E> {
        public Updater(List<E> list) {
            super(list);
        }

        public void update(int from, int to) {
            ObservableListWrapperShim.beginChange(this);
            for (int i = from; i < to; ++i) {
               ObservableListWrapperShim.nextUpdate(this, i);
            }
            ObservableListWrapperShim.endChange(this);
        }

        public void updateAll() {
            update(0, size());
        }
    }

    @Test
    public void testCustomMutableElements() {
        Updater<Person> list = new Updater<>(Person.createPersonsFromNames(
                "A0", "A1", "BB2", "B3", "A4", "A5", "BC6", "A7", "C8"));

        FilteredList<Person> filtered = new FilteredList<>(list,
                (Person p) -> p.name.get().length() > 2);
        MockListObserver<Person> lo = new MockListObserver<>();
        filtered.addListener(lo);

        assertEquals(Person.createPersonsList("BB2", "BC6"), filtered);

        list.updateAll();
        lo.checkUpdate(0, filtered, 0, filtered.size());
        compareIndices(filtered);

        lo.clear();
        list.get(0).name.set("AA0");
        list.get(3).name.set("BB3");
        list.get(5).name.set("AA5");
        list.get(6).name.set("B6");
        list.get(7).name.set("AA7");
        list.updateAll();
        assertEquals(Person.createPersonsList("AA0", "BB2", "BB3", "AA5", "AA7"), filtered);
        lo.checkAddRemove(0, filtered, Collections.EMPTY_LIST, 0, 1);
        lo.checkAddRemove(1, filtered, Person.createPersonsList("B6"), 2, 5);
        lo.checkUpdate(2, filtered, 1, 2);
        compareIndices(filtered);
    }

    @Test
    public void testNullPredicate() {
        filteredList.setPredicate(null);
        assertEquals(list.size(), filteredList.size());
        assertEquals(list, filteredList);
        mlo.check1AddRemove(filteredList, Arrays.asList("a", "d"), 0, 4);
        compareIndices();
    }

    @Test
    public void testSingleArgConstructor() {
        filteredList = new FilteredList<>(list);
        assertEquals(list.size(), filteredList.size());
        assertEquals(list, filteredList);
        compareIndices();
    }
}
