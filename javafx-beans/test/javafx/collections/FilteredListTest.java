/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.collections.transformation.FilterableList;
import com.sun.javafx.collections.transformation.FilterableList.FilterMode;
import com.sun.javafx.collections.transformation.FilteredList;
import com.sun.javafx.collections.transformation.Matcher;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import javafx.beans.Observable;
import javafx.collections.ListChangeListener.Change;
import javafx.util.Callback;
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
        Matcher<String> matcher = new Matcher<String>() {

            @Override
            public boolean matches(String e) {
                return !e.equals("c");
            }
        };
        mlo = new MockListObserver<String>();
        filteredList = new FilteredList<String>(list, matcher);
        filteredList.addListener(mlo);
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
        mlo.clear();
        list.add("c");
        mlo.check0();
        list.add(1, "b");
        assertEquals(Arrays.asList("a", "b", "d"), filteredList);
        mlo.check1AddRemove(filteredList, Collections.<String>emptyList(), 1, 2);
    }

    @Test
    public void testLiveMode_Remove() {
        list.removeAll(Arrays.asList("c"));
        assertEquals(Arrays.asList("a", "d"), filteredList);
        mlo.check0();
        mlo.clear();
        list.remove("a");
        assertEquals(Arrays.asList("d"), filteredList);
        mlo.check1AddRemove(filteredList, Arrays.asList("a"), 0, 0);
    }

    @Test
    public void testLiveMode_Permutation() {
        FXCollections.sort(list, new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return -o1.compareTo(o2);
            }

        });
        mlo.check1Permutation(filteredList, new int[] {1, 0});
        assertEquals(Arrays.asList("d", "a"), filteredList);
    }

    @Test
    public void testLiveMode_changeMatcher() {
        filteredList.setMatcher(new Matcher<String>() {

            @Override
            public boolean matches(String e) {
                return !e.equals("d");
            }
        });
        assertEquals(Arrays.asList("a", "c", "c"), filteredList);
        mlo.check1AddRemove(filteredList, Arrays.asList("a", "d"), 0, 3);
    }

    @Test
    public void testBatchMode_add() {
        filteredList.setMode(FilterableList.FilterMode.BATCH);
        list.clear();
        mlo.clear();
        list.addAll("a", "c", "d", "c");
        assertEquals(Arrays.asList("a", "c", "d", "c"), filteredList);
        mlo.check1AddRemove(filteredList, Collections.<String>emptyList(), 0, 4);
        mlo.clear();
        filteredList.filter();
        mlo.checkAddRemove(0, filteredList, Arrays.asList("c"), 1, 1);
        mlo.checkAddRemove(1, filteredList, Arrays.asList("c"), 2, 2);
        assertEquals(Arrays.asList("a", "d"), filteredList);
        mlo.clear();
        list.add("c");
        mlo.check1AddRemove(filteredList, Collections.<String>emptyList(), 2, 3);
        assertEquals(Arrays.asList("a", "d", "c"), filteredList);
        mlo.clear();
        filteredList.filter();
        assertEquals(Arrays.asList("a", "d"), filteredList);
        mlo.check1AddRemove(filteredList, Arrays.asList("c"), 2, 2);
        mlo.clear();
        list.add(0, "x");
        mlo.check1AddRemove(filteredList, Collections.<String>emptyList(), 0, 1);
        assertEquals(Arrays.asList("x", "a", "d"), filteredList);
    }

    @Test
    public void testBatchMode_remove() {
        filteredList.setMode(FilterableList.FilterMode.BATCH);
        list.removeAll(Collections.singletonList("c"));
        assertEquals(Arrays.asList("a", "d"), filteredList);
        mlo.check0();
        mlo.clear();
        filteredList.filter();
        mlo.check0();
        assertEquals(Arrays.asList("a", "d"), filteredList);
    }
    
    @Test
    public void testBatchMode_constructor() {
        filteredList = new FilteredList<String>(list, new Matcher<String>() {

            @Override
            public boolean matches(String e) {
                return false;
            }
            
        }, FilterableList.FilterMode.BATCH);
        assertEquals(Arrays.asList("a", "c", "d", "c"), filteredList);
        filteredList.filter();
        assertEquals(Collections.<String>emptyList(), filteredList);
    }
    
    @Test
    public void testBatchMode_mutableElement() {
        List<Date> list = FXCollections.observableArrayList(new Date(5000),
                new Date(10000),
                new Date(1200),
                new Date(12),
                new Date(12000));
        
        FilteredList<Date> filtered = new FilteredList<Date>(list, new Matcher<Date>() {

            @Override
            public boolean matches(Date e) {
                return e.getTime() < 5000;
            }
            
        }, FilterMode.BATCH);
        filtered.filter();
        assertEquals(Arrays.asList(
                new Date(1200),
                new Date(12)), filtered);
        list.get(1).setTime(4000);
        list.get(2).setTime(5001);
        assertEquals(Arrays.asList(
                new Date(5001),
                new Date(12)), filtered);
        ListChangeListener<Date> listener = new ListChangeListener<Date>() {

            @Override
            public void onChanged(Change<? extends Date> change) {
                change.next();
                assertTrue(change.wasAdded());
                assertTrue(change.wasRemoved());
                assertEquals(1, change.getAddedSize());
                assertEquals(1, change.getRemovedSize());
                assertFalse(change.next());
            }
        };
        filtered.addListener(listener);
        filtered.filter();
        assertEquals(Arrays.asList(
                new Date(4000),
                new Date(12)), filtered);
    }

    @Test
    public void testLiveMode_mutableElement() {
        ObservableList<Person> list = FXCollections.observableArrayList(new Callback<Person, Observable[]> () {

            @Override
            public Observable[] call(Person p) {
                return new Observable[] { p.name };
            }
            
        });

        list.addAll(createPerson("A"), createPerson("BB"), createPerson("C"));

        FilteredList<Person> filtered = new FilteredList<Person>(list, new Matcher<Person>() {

            @Override
            public boolean matches(Person p) {
                return p.name.get().length() > 1;
            }

        }, FilterMode.LIVE);
        MockListObserver<Person> lo = new MockListObserver<Person>();
        assertEquals(Arrays.asList(createPerson("BB")), filtered);

        filtered.addListener(lo);

        list.get(0).name.set("AA");
        lo.check1AddRemove(filtered, Collections.EMPTY_LIST, 0, 1);
        assertEquals(Arrays.asList(createPerson("AA"),createPerson("BB")), filtered);

        lo.clear();
        list.get(1).name.set("B");
        assertEquals(1, lo.calls.size());
        lo.checkAddRemove(0, filtered, Arrays.asList(createPerson("B")), 1, 1);

        assertEquals(Arrays.asList(createPerson("AA")), filtered);
    }

    private Person createPerson(String name) {
        Person p =  new Person();
        p.name.set(name);
        return p;
    }

}
