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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Predicate;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.TransformationList;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
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
        FXCollections.sort(list, (o1, o2) -> -o1.compareTo(o2));
        mlo.check1Permutation(filteredList, new int[] {1, 0});
        assertEquals(Arrays.asList("d", "a"), filteredList);
    }

    @Test
    @Ignore
    public void testLiveMode_changeMatcher() {
        assertEquals(Arrays.asList("a", "c", "c"), filteredList);
        ObjectProperty<Predicate<String>> pProperty = new SimpleObjectProperty<>();
        pProperty.set((String e) -> !e.equals("c"));
        filteredList = new FilteredList<>(list);
        filteredList.predicateProperty().bind(pProperty);
        filteredList.addListener(mlo);
        assertEquals(Arrays.asList("a", "d"), filteredList);
        mlo.check0();
        pProperty.set((String s) -> !s.equals("d"));
        mlo.check1AddRemove(filteredList, Arrays.asList("a", "d"), 0, 3);
    }

    @Test
    public void testLiveMode_mutableElement() {
        ObservableList<Person> list = FXCollections.observableArrayList(
                (Person p) -> new Observable[] { p.name });

        list.addAll(createPerson("A"), createPerson("BB"), createPerson("C"));

        FilteredList<Person> filtered = new FilteredList<Person>(list,
                (Person p) -> p.name.get().length() > 1);
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
