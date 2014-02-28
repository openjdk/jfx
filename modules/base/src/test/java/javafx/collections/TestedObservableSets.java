/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.TreeSet;
import javafx.beans.property.SimpleSetProperty;

public interface TestedObservableSets {

    Callable<ObservableSet<String>> HASH_SET = () -> FXCollections.observableSet(new HashSet<String>());

    Callable<ObservableSet<String>> TREE_SET = new CallableTreeSetImpl();

    Callable<ObservableSet<String>> LINKED_HASH_SET = () -> FXCollections.observableSet(new LinkedHashSet<String>());

    Callable<ObservableSet<String>> CHECKED_OBSERVABLE_HASH_SET = () -> FXCollections.checkedObservableSet(FXCollections.observableSet(new HashSet()), String.class);

    Callable<ObservableSet<String>> SYNCHRONIZED_OBSERVABLE_HASH_SET = () -> FXCollections.synchronizedObservableSet(FXCollections.observableSet(new HashSet<String>()));
    
    Callable<ObservableSet<String>> OBSERVABLE_SET_PROPERTY = () -> new SimpleSetProperty<>(FXCollections.observableSet(new HashSet<String>()));

    static class CallableTreeSetImpl implements Callable<ObservableSet<String>> {
        public CallableTreeSetImpl() {
        }

        @Override
        public ObservableSet<String> call() throws Exception {
            return FXCollections.observableSet(new TreeSet<String>());
        }
    }
}
