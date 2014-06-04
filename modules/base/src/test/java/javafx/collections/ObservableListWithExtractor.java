/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.collections.ElementObservableListDecorator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import javafx.beans.Observable;
import javafx.util.Callback;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ObservableListWithExtractor {
    private final Mode mode;

    public static enum Mode {
        OBSERVABLE_LIST_WRAPPER,
        DECORATOR
    }

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{{Mode.OBSERVABLE_LIST_WRAPPER}, {Mode.DECORATOR}});
    }

    private ObservableList<Person> modifiedList;
    private ObservableList<Person> observedList;
    private MockListObserver obs;
    private Person p0;

    public ObservableListWithExtractor(Mode mode) {
        this.mode = mode;
    }
    
    private void updateP0() {
        p0.name.set("bar");
    }

    
    @Before
    public void setUp() {
        p0 = new Person();
        obs = new MockListObserver();
        Callback<Person, Observable[]> callback = param -> new Observable[]{param.name};
        if (mode == Mode.OBSERVABLE_LIST_WRAPPER) {
            observedList = modifiedList = FXCollections.observableArrayList(callback);
        } else {
            modifiedList = FXCollections.observableArrayList();
            observedList = new ElementObservableListDecorator<Person>(modifiedList, callback);
        }
        
        modifiedList.add(p0);
        observedList.addListener(obs);
    }
    
    @Test
    public void testUpdate_add() {
        updateP0();
        obs.check1Update(modifiedList, 0, 1);
    }
    
    @Test
    public void testUpdate_add1() {
        modifiedList.clear();
        modifiedList.add(0, p0);
        obs.clear();
        updateP0();
        obs.check1Update(observedList, 0, 1);
    }
    
    @Test
    public void testUpdate_addAll() {
        modifiedList.clear();
        modifiedList.addAll(Arrays.asList(p0, p0));
        obs.clear();
        updateP0();
        obs.check1Update(observedList, 0, 2);
    }
    
    @Test
    public void testUpdate_addAll1() {
        modifiedList.clear();
        modifiedList.addAll(0, Arrays.asList(p0, p0));
        obs.clear();
        updateP0();
        obs.check1Update(observedList, 0, 2);
    }
    
    @Test
    public void testUpdate_addAll2() {
        modifiedList.clear();
        modifiedList.addAll(p0, p0);
        obs.clear();
        updateP0();
        obs.check1Update(observedList, 0, 2);
    }
    
    @Test
    public void testUpdate_set() {
        Person p1 = new Person();
        modifiedList.set(0, p1);
        obs.clear();
        updateP0();
        obs.check0();
        p1.name.set("bar");
        obs.check1Update(observedList, 0, 1);
    }
    
    @Test
    public void testUpdate_setAll() {
        Person p1 = new Person();
        modifiedList.setAll(p1);
        obs.clear();
        updateP0();
        obs.check0();
        p1.name.set("bar");
        obs.check1Update(observedList, 0, 1);
    }
    
    @Test
    public void testUpdate_remove() {
        modifiedList.remove(p0);
        obs.clear();
        updateP0();
        obs.check0();
    }
    
    @Test
    public void testUpdate_remove1() {
        modifiedList.remove(0);
        obs.clear();
        updateP0();
        obs.check0();
    }
    
    @Test
    public void testUpdate_removeAll() {
        modifiedList.removeAll(p0);
        obs.clear();
        updateP0();
        obs.check0();
    }
    
    @Test
    public void testUpdate_retainAll() {
        modifiedList.retainAll();
        obs.clear();
        updateP0();
        obs.check0();
    }
    
    @Test
    public void testUpdate_iterator_add() {
        modifiedList.clear();
        modifiedList.listIterator().add(p0);
        obs.clear();
        updateP0();
        obs.check1Update(observedList, 0, 1);
    }
    
    @Test
    public void testUpdate_iterator_set() {
        Person p1 = new Person();
        ListIterator<Person> listIterator = modifiedList.listIterator();
        listIterator.next();
        listIterator.set(p1);
        obs.clear();
        updateP0();
        obs.check0();
        p1.name.set("bar");
        obs.check1Update(observedList, 0, 1);
    }
    
    @Test
    public void testUpdate_sublist_add() {
        List<Person> sublist = modifiedList.subList(0, 1);
        sublist.add(p0);
        obs.clear();
        updateP0();
        obs.check1Update(observedList, 0, 2);
    }
    
    @Test
    public void testUpdate_sublist_add1() {
        List<Person> sublist = modifiedList.subList(0, 1);
        sublist.clear();
        sublist.add(0, p0);
        obs.clear();
        updateP0();
        obs.check1Update(observedList, 0, 1);
    }
    
    @Test
    public void testUpdate_sublist_addAll() {
        List<Person> sublist = modifiedList.subList(0, 1);
        sublist.clear();
        sublist.addAll(Arrays.asList(p0, p0));
        obs.clear();
        updateP0();
        obs.check1Update(observedList, 0, 2);
    }
    
    @Test
    public void testUpdate_sublist_addAll1() {
        List<Person> sublist = modifiedList.subList(0, 1);
        sublist.clear();
        sublist.addAll(0, Arrays.asList(p0, p0));
        obs.clear();
        updateP0();
        obs.check1Update(observedList, 0, 2);
    }
    
    @Test
    public void testUpdate_sublist_set() {
        List<Person> sublist = modifiedList.subList(0, 1);
        Person p1 = new Person();
        sublist.set(0, p1);
        obs.clear();
        updateP0();
        obs.check0();
        p1.name.set("bar");
        obs.check1Update(observedList, 0, 1);
    }
    
    @Test
    public void testUpdate_sublist_remove() {
        List<Person> sublist = modifiedList.subList(0, 1);
        sublist.remove(p0);
        obs.clear();
        updateP0();
        obs.check0();
    }
    
    @Test
    public void testUpdate_sublist_remove1() {
        List<Person> sublist = modifiedList.subList(0, 1);
        sublist.remove(0);
        obs.clear();
        updateP0();
        obs.check0();
    }
    
    @Test
    public void testUpdate_sublist_removeAll() {
        List<Person> sublist = modifiedList.subList(0, 1);
        sublist.removeAll(Arrays.asList(p0));
        obs.clear();
        updateP0();
        obs.check0();
    }
    
    @Test
    public void testUpdate_sublist_retainAll() {
        List<Person> sublist = modifiedList.subList(0, 1);
        sublist.retainAll(Collections.<Person>emptyList());
        obs.clear();
        updateP0();
        obs.check0();
    }
    
    @Test
    public void testUpdate_iterator_sublist_add() {
        List<Person> sublist = modifiedList.subList(0, 1);
        sublist.clear();
        sublist.listIterator().add(p0);
        obs.clear();
        updateP0();
        obs.check1Update(observedList, 0, 1);
    }
    
    @Test
    public void testUpdate_iterator_sublist_set() {
        List<Person> sublist = modifiedList.subList(0, 1);
        Person p1 = new Person();
        ListIterator<Person> listIterator = sublist.listIterator();
        listIterator.next();
        listIterator.set(p1);
        obs.clear();
        updateP0();
        obs.check0();
        p1.name.set("bar");
        obs.check1Update(observedList, 0, 1);
    }

    @Test
    public void testMultipleUpdate() {

        modifiedList.add(new Person());
        modifiedList.addAll(p0, p0);

        obs.clear();

        updateP0();

        obs.checkUpdate(0, modifiedList, 0, 1);
        obs.checkUpdate(1, modifiedList, 2, 4);
        assertEquals(2, obs.calls.size());

    }

    @Test
    public void testPreFilledList() {
        ArrayList<Person> arrayList = new ArrayList<Person>();
        arrayList.add(p0);
        obs = new MockListObserver();
        Callback<Person, Observable[]> callback = param -> new Observable[]{param.name};
        if (mode == Mode.OBSERVABLE_LIST_WRAPPER) {
            observedList = modifiedList = FXCollections.observableList(arrayList, callback);
        } else {
            modifiedList = FXCollections.observableArrayList(arrayList);
            observedList = new ElementObservableListDecorator<Person>(modifiedList, callback);
        }

        observedList.addListener(obs);

        updateP0();

        obs.checkUpdate(0, observedList, 0, 1);
        assertEquals(1, obs.calls.size());
    }

    
}
