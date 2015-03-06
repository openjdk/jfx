/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.Observable;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;

public class Person implements Comparable<Person> {
    public StringProperty name = new StringPropertyBase("foo") {

        @Override
        public Object getBean() {
            return Person.this;
        }

        @Override
        public String getName() {
            return "name";
        }
    };

    public Person(String name) {
        this.name.set(name);
    }
    
    public Person() {
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Person other = (Person) obj;
        if (this.name.get() != other.name.get() && (this.name.get() == null || !this.name.get().equals(other.name.get()))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.name.get() != null ? this.name.get().hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "Person[" + name.get() + "]";
    }

    @Override
    public int compareTo(Person o) {
        return this.name.get().compareTo(o.name.get());
    }
    
    public static ObservableList<Person> createPersonsList(Person... persons) {
        ObservableList<Person> list = FXCollections.observableArrayList(
                (Person p) -> new Observable[]{p.name});
        list.addAll(persons);
        return list;
    }

    public static List<Person> createPersonsFromNames(String... names) {
        return Arrays.asList(names).stream().
                map(name -> new Person(name)).collect(Collectors.toList());
    }

    public static ObservableList<Person> createPersonsList(String... names) {
        ObservableList<Person> list = FXCollections.observableArrayList(
                (Person p) -> new Observable[]{p.name});
        list.addAll(createPersonsFromNames(names));
        return list;
    }
}
