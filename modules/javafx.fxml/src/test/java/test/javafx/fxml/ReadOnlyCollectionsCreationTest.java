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

import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.fxml.builder.ClassWithPlainListAndScalarArg;
import test.com.sun.javafx.fxml.builder.ClassWithPlainSetAndScalarArg;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ReadOnlyCollectionsCreationTest {

    @Test
    public void testReadOnlyCollectionsNoProxyBuilder() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("readonly_collections_creation_a.fxml"));
        ClassWithCollections widget = fxmlLoader.load();

        assertEquals(3, widget.getList().size());
        assertEquals(3, widget.getSet().size());
        assertEquals(2, widget.getMap().size());

        assertEquals(3, widget.getObservableList().size());
        assertEquals(3, widget.getObservableSet().size());
        assertEquals(2, widget.getObservableMap().size());
    }

    @Test
    public void testReadOnlyCollections() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("readonly_collections_creation_b.fxml"));
        ClassWithCollections2 widget = fxmlLoader.load();

        assertEquals(3, widget.getList().size());
        assertEquals(3, widget.getSet().size());
        assertEquals(2, widget.getMap().size());

        assertEquals(3, widget.getObservableList().size());
        assertEquals(3, widget.getObservableSet().size());
        assertEquals(2, widget.getObservableMap().size());
    }

    @Test
    public void testArrayListUnwrapsFirstElementViaFxml() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("array_list_scalar_arg.fxml"));
        ClassWithPlainListAndScalarArg result = fxmlLoader.load();

        assertEquals("hello", result.child,
                "FXMLLoader must unwrap the single ArrayList element and pass it as the scalar @NamedArg constructor argument");
    }

    @Test
    public void testHashSetUnwrapsFirstElementViaFxml() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("hash_set_scalar_arg.fxml"));
        ClassWithPlainSetAndScalarArg result = fxmlLoader.load();

        assertEquals("hello", result.child,
                "FXMLLoader must unwrap the single HashSet element and pass it as the scalar @NamedArg constructor argument");
    }

}
