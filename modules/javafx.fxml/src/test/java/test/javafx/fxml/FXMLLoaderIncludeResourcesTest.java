/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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
import javafx.fxml.LoadException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class FXMLLoaderIncludeResourcesTest {

    @Test
    void testIncludeNotFound() {
        final var loader = new FXMLLoader(getClass().getResource("fxmlloader_include_resource_test_not_found.fxml"));
        try {
            loader.load();
            fail("Expected MissingResourceException");
        } catch (final LoadException e) {
            assertTrue(e.getCause() instanceof MissingResourceException);
        } catch (final IOException e) {
            fail("Expected MissingResourceException");
        }
    }

    @Test
    void testIncludeRootNonNullResources() throws IOException {
        final var loader = new FXMLLoader(getClass().getResource("fxmlloader_include_resource_test.fxml"));
        final var innerBundle = ResourceBundle.getBundle("test.javafx.fxml.fxmlloader_include_resource_test_inner");
        loader.setResources(innerBundle);
        final Widget root = loader.load();
        final var inner = root.getChildren().get(0);
        assertEquals(innerBundle.getString("inner"), inner.getName());
        final var child = inner.getChildren().get(0);
        final var nestedBundle = ResourceBundle.getBundle("test.javafx.fxml.fxmlloader_include_resource_test_nested");
        assertEquals(nestedBundle.getString("nested"), child.getName());
    }

    @Test
    void testIncludeRootNullResources() throws IOException {
        final var loader = new FXMLLoader(getClass().getResource("fxmlloader_include_resource_test.fxml"));
        final Widget root = loader.load();
        final var inner = root.getChildren().get(0);
        final var innerBundle = ResourceBundle.getBundle("test.javafx.fxml.fxmlloader_include_resource_test_inner");
        assertEquals(innerBundle.getString("inner"), inner.getName());
        final var child = inner.getChildren().get(0);
        final var nestedBundle = ResourceBundle.getBundle("test.javafx.fxml.fxmlloader_include_resource_test_nested");
        assertEquals(nestedBundle.getString("nested"), child.getName());
    }
}
