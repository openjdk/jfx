/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import javafx.beans.property.SimpleMapProperty;

public interface TestedObservableMaps {

    Callable<ObservableMap<String, String>> HASH_MAP = () -> FXCollections.observableMap(new HashMap<String, String>());

    Callable<ObservableMap<String, String>> TREE_MAP = new CallableTreeMapImpl();

    Callable<ObservableMap<String, String>> LINKED_HASH_MAP = () -> FXCollections.observableMap(new LinkedHashMap<String, String>());

    Callable<ObservableMap<String, String>> CONCURRENT_HASH_MAP = new CallableConcurrentHashMapImpl();

    Callable<ObservableMap<String, String>> CHECKED_OBSERVABLE_HASH_MAP = () -> FXCollections.checkedObservableMap(FXCollections.observableMap(new HashMap()), String.class, String.class);

    Callable<ObservableMap<String, String>> SYNCHRONIZED_OBSERVABLE_HASH_MAP = () -> FXCollections.synchronizedObservableMap(FXCollections.observableMap(new HashMap<String, String>()));
    
    Callable<ObservableMap<String, String>> OBSERVABLE_MAP_PROPERTY = () -> new SimpleMapProperty<>(FXCollections.observableMap(new HashMap<String, String>()));

    static class CallableTreeMapImpl implements Callable<ObservableMap<String, String>> {
        public CallableTreeMapImpl() {
        }

        @Override
        public ObservableMap<String, String> call() throws Exception {
            return FXCollections.observableMap(new TreeMap<String, String>());
        }
    }

    static class CallableConcurrentHashMapImpl implements Callable<ObservableMap<String, String>> {
        public CallableConcurrentHashMapImpl() {
        }

        @Override
        public ObservableMap<String, String> call() throws Exception {
            return FXCollections.observableMap(new ConcurrentHashMap<String, String>());
        }
    }

}
