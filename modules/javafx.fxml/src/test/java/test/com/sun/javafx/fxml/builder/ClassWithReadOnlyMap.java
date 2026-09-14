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

package test.com.sun.javafx.fxml.builder;

import javafx.beans.NamedArg;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Fixture class for testing ProxyBuilder with a read-only Map property alongside
 * a {@code @NamedArg}-annotated constructor. Models the real-world pattern of
 * {@code Node.getProperties()} — an immutable-reference, mutable-content Map that
 * has no corresponding setter.
 */
public class ClassWithReadOnlyMap {

    public final String label;
    private final Map<String, Object> properties = new HashMap<>();
    private final ObservableMap<String, Object> observableProperties = FXCollections.observableHashMap();

    public ClassWithReadOnlyMap() {
        this.label = null;
    }

    public ClassWithReadOnlyMap(@NamedArg("label") String label) {
        this.label = label;
    }

    /** Read-only map property: getter only, no setter. */
    public Map<String, Object> getProperties() {
        return properties;
    }

    public ObservableMap<String, Object> getObservableProperties() {
        return observableProperties;
    }
}
