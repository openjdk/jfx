/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.LinkedList;
import java.util.List;

public interface TestedObservableLists {

    Callable<ObservableList<String>> ARRAY_LIST = new Callable<ObservableList<String>>() {
        @Override
        public ObservableList<String> call() throws Exception {
            return FXCollections.observableList(new ArrayList<String>());
        }
    };

    Callable<ObservableList<String>> LINKED_LIST = new Callable<ObservableList<String>>() {
        @Override
        public ObservableList<String> call() throws Exception {
            return FXCollections.observableList(new LinkedList<String>());
        }
    };

    Callable<ObservableList<String>> VETOABLE_LIST = new Callable<ObservableList<String>>() {
        @Override
        public ObservableList<String> call() throws Exception {
            return new VetoableListDecorator<String>(FXCollections.<String>observableArrayList()) {

                @Override
                protected void onProposedChange(List list, int[] idx) { }
            };
        }
    };

    Callable<ObservableList<String>> CHECKED_OBSERVABLE_ARRAY_LIST = new Callable<ObservableList<String>>() {
        @Override
        public ObservableList<String> call() throws Exception {
            return FXCollections.checkedObservableList(FXCollections.observableList(new ArrayList()), String.class);
        }
    };

    Callable<ObservableList<String>> SYNCHRONIZED_OBSERVABLE_ARRAY_LIST = new Callable<ObservableList<String>>() {
        @Override
        public ObservableList<String> call() throws Exception {
            return FXCollections.synchronizedObservableList(FXCollections.observableList(new ArrayList<String>()));
        }
    };
    
}
