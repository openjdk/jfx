/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package test.robot.javafx.scene.layout;

import javafx.geometry.BoundingBox;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Ellipse;
import javafx.stage.Stage;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import test.robot.testharness.VisualTestBase;

/**
 * Test layout bounds update of node in the scene graph.
 */
public class LayoutTest extends VisualTestBase {

    private Stage testStage;
    private Scene testScene;

    @Test(timeout = 5000)
    public void testLayout() {
        final int WIDTH = 800;
        final int HEIGHT = 400;
        final Label l1 = new Label("1 2");
        final Ellipse e1 = new Ellipse(20, 30);
        final StackPane sp = new StackPane();

        runAndWait(() -> {
            e1.setOpacity(0.5);
            sp.getChildren().addAll(l1, e1);

            e1.radiusXProperty().bind(l1.widthProperty());
            e1.radiusYProperty().bind(l1.heightProperty());

            Pane topPane = new Pane();
            topPane.getChildren().addAll(sp);
            sp.relocate(200, 100);

            sp.setStyle("-fx-border-color: RED;");
            testStage = getStage();
            testStage.setTitle("Test bounds update of invisible node");
            testScene = new Scene(topPane, WIDTH, HEIGHT);
            testStage.setScene(testScene);
            testStage.show();
        });
        waitFirstFrame();
        runAndWait(() -> {
            BoundingBox e1Bounds = (BoundingBox) e1.getLayoutBounds();
            BoundingBox l1Bounds = (BoundingBox) l1.getLayoutBounds();

            // Each child is aligned to Pos.CENTER by default.
            BoundingBox testbb = new BoundingBox(0, 0, 0,
                    Math.max(e1Bounds.getWidth(), l1Bounds.getWidth()),
                    Math.max(e1Bounds.getHeight(), l1Bounds.getHeight()),
                    Math.max(e1Bounds.getDepth(), l1Bounds.getDepth()));

            BoundingBox bb = (BoundingBox) sp.getLayoutBounds();
            assertTrue(bb.contains(testbb));

            // Preparing for next Frame
            l1.setText("A B C D E F G");
        });
        waitNextFrame();
        runAndWait(() -> {
            BoundingBox e1Bounds = (BoundingBox) e1.getLayoutBounds();
            BoundingBox l1Bounds = (BoundingBox) l1.getLayoutBounds();
            BoundingBox testbb = new BoundingBox(0, 0, 0,
                    Math.max(e1Bounds.getWidth(), l1Bounds.getWidth()),
                    Math.max(e1Bounds.getHeight(), l1Bounds.getHeight()),
                    Math.max(e1Bounds.getDepth(), l1Bounds.getDepth()));

            BoundingBox bb = (BoundingBox) sp.getLayoutBounds();
            assertTrue(bb.contains(testbb));

            // Preparing for next Frame
            l1.setText("1 2 3");
        });
        waitNextFrame();
        runAndWait(() -> {
            BoundingBox e1Bounds = (BoundingBox) e1.getLayoutBounds();
            BoundingBox l1Bounds = (BoundingBox) l1.getLayoutBounds();
            BoundingBox testbb = new BoundingBox(0, 0, 0,
                    Math.max(e1Bounds.getWidth(), l1Bounds.getWidth()),
                    Math.max(e1Bounds.getHeight(), l1Bounds.getHeight()),
                    Math.max(e1Bounds.getDepth(), l1Bounds.getDepth()));

            BoundingBox bb = (BoundingBox) sp.getLayoutBounds();
            assertTrue(bb.contains(testbb));
        });

    }

}
