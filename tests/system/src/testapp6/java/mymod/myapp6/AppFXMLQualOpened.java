/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package myapp6;

import java.io.IOException;
import java.net.URL;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import myapp6.pkg5.AnnotatedController;
import myapp6.pkg5.CustomNode;
import myapp6.pkg5.SimpleController;

import static myapp6.Constants.*;

/**
 * Modular test application for testing FXML.
 * This is launched by ModuleLauncherTest.
 */
public class AppFXMLQualOpened extends Application {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            Application.launch(args);
        } catch (Throwable t) {
            System.err.println("ERROR: caught unexpected exception: " + t);
            t.printStackTrace(System.err);
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
    }

    // Load test FXML file -- no controller
    private void doTestNone() throws IOException {
        final URL fxmlURL = Util.getURL(SimpleController.class, "TestNone");

        FXMLLoader loader = new FXMLLoader(fxmlURL);
        Node fxmlRoot = loader.load();

        // Verify that the root node is a StackPane with the expected ID
        Util.assertNotNull(fxmlRoot);
        Util.assertTrue("fxmlRoot is not instance of StackPane", fxmlRoot instanceof StackPane);
        Util.assertEquals("RootTestNone", fxmlRoot.getId());
    }

    // Load test FXML file with reference to CustomNode -- no controller
    private void doTestCustomNode() throws IOException {
        final URL fxmlURL = Util.getURL(SimpleController.class, "TestCustomNode");

        FXMLLoader loader = new FXMLLoader(fxmlURL);
        Node fxmlRoot = loader.load();

        // Verify that the root node is a StackPane with the expected ID
        Util.assertNotNull(fxmlRoot);
        Util.assertTrue("fxmlRoot is not instance of StackPane", fxmlRoot instanceof StackPane);
        Util.assertEquals("RootTestCustomNode", fxmlRoot.getId());

        // Verify that there is at least one child node
        ObservableList<Node> children = ((StackPane) fxmlRoot).getChildren();
        Util.assertNotNull(children);
        Util.assertFalse("fxmlRoot has no children", children.isEmpty());

        // Verify that the first child is our CustomNode
        Node child = children.get(0);
        Util.assertNotNull(child);
        Util.assertTrue("child is not instance of CustomNode", child instanceof CustomNode);
        String name = ((CustomNode) child).getName();
        Util.assertEquals("Duke", name);
    }

    // Load test FXML file -- SimpleController
    private void doTestSimple() throws IOException {
        final URL fxmlURL = Util.getURL(SimpleController.class, "TestSimple");

        FXMLLoader loader = new FXMLLoader(fxmlURL);
        Node fxmlRoot = loader.load();

        // Verify that the controller was constructed and its initialize method called
        SimpleController controller = loader.getController();
        Util.assertNotNull(controller);
        Util.assertTrue("Controller not initialized", controller.isInitialized());

        // Verify that the root node is a StackPane with the expected ID
        Util.assertNotNull(fxmlRoot);
        Util.assertTrue("fxmlRoot is not instance of StackPane", fxmlRoot instanceof StackPane);
        Util.assertEquals("RootTestSimple", fxmlRoot.getId());
    }

    // Load test FXML file -- AnnotatedController
    private void doTestAnnotated() throws IOException {
        final URL fxmlURL = Util.getURL(SimpleController.class, "TestAnnotated");

        FXMLLoader loader = new FXMLLoader(fxmlURL);
        Node fxmlRoot = loader.load();

        // Verify that the controller was constructed and its initialize method called
        AnnotatedController controller = loader.getController();
        Util.assertNotNull(controller);
        Util.assertTrue("Controller not initialized", controller.isInitialized());

        // Verify that the root node is a StackPane with the expected ID
        Util.assertNotNull(fxmlRoot);
        Util.assertTrue("fxmlRoot is not instance of StackPane", fxmlRoot instanceof StackPane);
        Util.assertEquals("RootTestAnnotated", fxmlRoot.getId());

        // Verify that the root node and text node can be retrieved from the controller
        StackPane cRoot = controller.getRoot();
        Util.assertNotNull(cRoot);
        Util.assertSame(fxmlRoot, cRoot);
        Text cText = controller.getTheText();
        Util.assertNotNull(cText);
        Util.assertEquals("theText", cText.getId());
    }

    @Override
    public void start(Stage stage) {
        try {
            doTestNone();
            doTestCustomNode();
            doTestSimple();
            doTestAnnotated();
        } catch (AssertionError ex) {
            ex.printStackTrace(System.err);
            System.exit(ERROR_ASSERTION_FAILURE);
        } catch (Error | Exception ex) {
            System.err.println("ERROR: caught unexpected exception: " + ex);
            ex.printStackTrace(System.err);
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
        Platform.exit();
    }

    @Override public void stop() {
        System.exit(ERROR_NONE);
    }

}
