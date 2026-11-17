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
import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;

/**
 * Fixture class for testing ProxyBuilder with read-only ObservableIntegerArray and
 * ObservableFloatArray properties alongside a {@code @NamedArg}-annotated constructor.
 * Models the pattern of a class that exposes mutable primitive observable arrays via
 * getter-only properties (no corresponding setter).
 */
public class ClassWithReadOnlyObservableArrays {

    public final String label;
    private final ObservableIntegerArray intArray = FXCollections.observableIntegerArray();
    private final ObservableFloatArray floatArray = FXCollections.observableFloatArray();

    public ClassWithReadOnlyObservableArrays() {
        this.label = null;
    }

    public ClassWithReadOnlyObservableArrays(@NamedArg("label") String label) {
        this.label = label;
    }

    /** Read-only integer array property: getter only, no setter. */
    public ObservableIntegerArray getIntArray() {
        return intArray;
    }

    /** Read-only float array property: getter only, no setter. */
    public ObservableFloatArray getFloatArray() {
        return floatArray;
    }
}
