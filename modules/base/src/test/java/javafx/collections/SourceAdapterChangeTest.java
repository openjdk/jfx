/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javafx.beans.Observable;
import javafx.collections.transformation.SortedList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SourceAdapterChangeTest {

    @FunctionalInterface
    public interface ListFactory<E> {
        public ObservableList<E> createList(ObservableList<E> items);
    }

    private static final ListFactory unmodifiableObservableList =
            items -> FXCollections.unmodifiableObservableList(items);
    private static final ListFactory checkedObservableList =
            items -> FXCollections.checkedObservableList(items, Person.class);
    private static final ListFactory synchronizedObservableList =
            items -> FXCollections.synchronizedObservableList(items);
    private static final ListFactory vetoableListDecorator =
            items -> new VetoableListDecorator<Person>(items) {

                @Override
                protected void onProposedChange(List<Person> added, int[] removed) {
                }
            };

    @Parameterized.Parameters
    public static Collection createParameters() {
        Object[][] data = new Object[][] {
            { unmodifiableObservableList },
            { checkedObservableList },
            { synchronizedObservableList },
            { vetoableListDecorator },
         };
        return Arrays.asList(data);
    }

    final ListFactory listFactory;
    ObservableList<Person> items;
    ObservableList<Person> list;
    MockListObserver<Person> mlo;

    public SourceAdapterChangeTest(ListFactory listFactory) {
        this.listFactory = listFactory;
    }

    @Before
    public void setUp() throws Exception {
        items = FXCollections.observableArrayList(
                (Person p) -> new Observable[]{p.name});
        items.addAll(
                new Person("one"), new Person("two"), new Person("three"),
                new Person("four"), new Person("five"));
        list = listFactory.createList(items);
        mlo = new MockListObserver<>();
        list.addListener(mlo);
    }

    @Test
    public void testUpdate() {
        items.get(3).name.set("zero"); // four -> zero
        ObservableList<Person> expected = FXCollections.observableArrayList(
                new Person("one"), new Person("two"), new Person("three"),
                new Person("zero"), new Person("five"));
        mlo.check1Update(expected, 3, 4);
    }

    @Test
    public void testPermutation() {
        FXCollections.sort(items);
        ObservableList<Person> expected = FXCollections.observableArrayList(
                new Person("five"), new Person("four"), new Person("one"),
                new Person("three"), new Person("two"));
        mlo.check1Permutation(expected, new int[]{2, 4, 3, 1, 0});
    }

    @Test
    public void testPermutationUpdate() {
        SortedList<Person> sorted = items.sorted((o1, o2) -> o1.compareTo(o2));
        list.removeListener(mlo);
        list = listFactory.createList(sorted);
        list.addListener(mlo);
        items.get(3).name.set("zero"); // four -> zero
        ObservableList<Person> expected = FXCollections.observableArrayList(
                new Person("five"), new Person("one"), new Person("three"),
                new Person("two"), new Person("zero"));
        mlo.checkPermutation(0, expected, 0, expected.size(), new int[] {0, 4, 1, 2, 3});
        mlo.checkUpdate(1, expected, 4, 5);
    }
}
