package javafx.fxml;
/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.shape.Rectangle;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FXMLLoaderTest_FieldInjectionTest {

    public static class SuperController {
        @FXML private Parent root;

        public final Parent getRoot() {
            return root;
        }

    }

    public static class SubController_1 extends SuperController {
        @FXML private Rectangle rectangle;

        public Rectangle getRectangle() {
            return rectangle;
        }
    }
    public static class SubController_2 extends SuperController {

        @FXML private Node root;
        @FXML private Rectangle rectangle;

        public Rectangle getRectangle() {
            return rectangle;
        }

        public Node getRootFromSub() {
            return root;
        }
    }

    @Test
    public void testFieldInjectionInSuperClass() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("simple.fxml"));
        SubController_1 controller = new SubController_1();
        fxmlLoader.setController(controller);

        fxmlLoader.load();

        assertNotNull(controller.getRectangle());
        assertNotNull(controller.getRoot());
    }

    @Test
    public void testFieldInjectionInSuperClassNotSuppressed() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("simple.fxml"));
        SubController_2 controller = new SubController_2();
        fxmlLoader.setController(controller);

        fxmlLoader.load();

        assertNotNull(controller.getRectangle());
        assertNotNull(controller.getRoot());
        assertNotNull(controller.getRootFromSub());
        assertEquals(controller.getRoot(), controller.getRootFromSub());
    }
}
