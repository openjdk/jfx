/*
 * Copyright (c) 2015, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.scenegraph;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.Test;
import test.robot.testharness.VisualTestBase;

/**
 * Test bounds update of invisible node in the scene graph.
 */
public class JDK8130122Test extends VisualTestBase {

    private Stage testStage;
    private Scene testScene;

    private static final double TOLERANCE = 0.07;

    @Test(timeout = 20000)
    public void testEmptyShapes() {
        final int WIDTH = 800;
        final int HEIGHT = 400;
        final ObservableList<Rectangle> data = FXCollections.<Rectangle>observableArrayList();

        data.addAll(new Rectangle(100, 100, Color.RED), new Rectangle(100, 100, Color.BLUE),
                new Rectangle(100, 100, Color.RED), new Rectangle(100, 100, Color.BLUE),
                new Rectangle(100, 100, Color.RED), new Rectangle(100, 100, Color.BLUE),
                new Rectangle(100, 100, Color.RED), new Rectangle(100, 100, Color.BLUE));

        final ListView<Rectangle> horizontalListView = new ListView<Rectangle>();

        runAndWait(() -> {
            final GridPane gridPane = new GridPane();
            gridPane.setPrefWidth(WIDTH);
            gridPane.setPrefHeight(HEIGHT);

            horizontalListView.setOrientation(Orientation.HORIZONTAL);
            horizontalListView.setItems(data);

            gridPane.add(horizontalListView, 0, 0);
            horizontalListView.setVisible(false);
            GridPane.setVgrow(horizontalListView, Priority.ALWAYS);
            GridPane.setHgrow(horizontalListView, Priority.ALWAYS);

            Group root = new Group(gridPane);

            testStage = getStage();
            testStage.setTitle("Test bounds update of invisible node");
            testScene = new Scene(root, WIDTH, HEIGHT);
            testScene.setCamera(new PerspectiveCamera());
            testScene.setFill(Color.WHITE);
            testStage.setScene(testScene);
            testStage.show();
        });
        waitFirstFrame();
        runAndWait(() -> {
            Color color = getColor(testScene, 200, 250);
            assertColorEquals(Color.WHITE, color, TOLERANCE);
            data.add(0, new Rectangle(250, 150, Color.GREEN));
        });
        waitNextFrame();
        runAndWait(() -> {
            Color color = getColor(testScene, 200, 250);
            assertColorEquals(Color.WHITE, color, TOLERANCE);
            horizontalListView.setVisible(true);
        });
        // Give more time after setVisible(true) is called for frame update
        waitFirstFrame();
        runAndWait(() -> {
            Color color = getColor(testScene, 200, 200);
            assertColorEquals(Color.GREEN, color, TOLERANCE);
        });

    }

}
