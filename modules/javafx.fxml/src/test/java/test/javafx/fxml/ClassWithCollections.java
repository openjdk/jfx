/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.fxml;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassWithCollections {

    private List<String> list = new ArrayList<>();
    private Set<String> set = new HashSet<>();
    private Map<String, Object> map = new HashMap<>();

    private ObservableList<String> observableList = FXCollections.observableArrayList();
    private ObservableSet<String> observableSet = FXCollections.observableSet();
    private ObservableMap<String, Object> observableMap = FXCollections.observableHashMap();

    private float[] ratios = new float[]{};
    private String[] names = new String[]{};

    public ClassWithCollections() {}

    public List<String> getList() {
        return list;
    }

    public Set<String> getSet() {
        return set;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public ObservableList<String> getObservableList() {
        return observableList;
    }

    public ObservableSet<String> getObservableSet() {
        return observableSet;
    }

    public ObservableMap<String, Object> getObservableMap() {
        return observableMap;
    }

    public float[] getRatios() {
        return Arrays.copyOf(ratios, ratios.length);
    }

    public void setRatios(float[] ratios) {
        this.ratios = Arrays.copyOf(ratios, ratios.length);
    }

    public String[] getNames() {
        return Arrays.copyOf(names, names.length);
    }

    public void setNames(String[] names) {
        this.names = Arrays.copyOf(names, names.length);
    }

}
