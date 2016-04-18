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

package test.robot.scenegraph;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.Test;
import test.robot.testharness.VisualTestBase;

/**
 * Test view order of Node in the scene graph.
 */
public class ViewOrderTest extends VisualTestBase {

    private Stage testStage;
    private Scene testScene;

    private static final double TOLERANCE = 0.07;

    @Test(timeout=5000)
    public void testViewOrder() {
        final int WIDTH = 300;
        final int HEIGHT = 300;

        final Pane rectsPane = new Pane();
        final Rectangle redRect = new Rectangle(150, 150, Color.RED);
        redRect.setViewOrder(0);
        redRect.relocate(20, 10);
        final Rectangle greenRect = new Rectangle(150, 150, Color.GREEN);
        greenRect.setViewOrder(-1);
        greenRect.relocate(100, 50);
        final Rectangle blueRect = new Rectangle(150, 150, Color.BLUE);
        blueRect.setViewOrder(1);
        blueRect.relocate(60, 100);

        rectsPane.getChildren().addAll(redRect, greenRect, blueRect);

        runAndWait(() -> {
            Group root = new Group(rectsPane);

            testStage = getStage();
            testStage.setTitle("Test View Order");
            testScene = new Scene(root, WIDTH, HEIGHT);
            testScene.setFill(Color.WHITE);
            testStage.setScene(testScene);
            testStage.show();
        });
        waitFirstFrame();
        runAndWait(() -> {
            Color color = getColor(testScene, 30, 20);
            assertColorEquals(Color.RED, color, TOLERANCE);
            color = getColor(testScene, 120, 100);
            assertColorEquals(Color.GREEN, color, TOLERANCE);
            color = getColor(testScene, 120, 240);
            assertColorEquals(Color.BLUE, color, TOLERANCE);

            // redRect should now be on top in the next frame
            redRect.setViewOrder(-1.5);
        });
        waitNextFrame();
        runAndWait(() -> {
            // This used to be GREEN
            Color color = getColor(testScene, 120, 100);
            assertColorEquals(Color.RED, color, TOLERANCE);

            // blueRect should now be on top in the next frame
            redRect.setViewOrder(1.5);
            greenRect.setViewOrder(1);
        });
        waitNextFrame();
        runAndWait(() -> {
            // This used to be RED
            Color color = getColor(testScene, 120, 100);
            assertColorEquals(Color.BLUE, color, TOLERANCE);
        });
    }

    @Test(timeout=5000)
    public void testViewOrderHBox() {
        final int WIDTH = 500;
        final int HEIGHT = 200;

        final HBox rectsPane = new HBox();
        final Rectangle redRect = new Rectangle(150, 150, Color.RED);
        redRect.setViewOrder(0);
        final Rectangle greenRect = new Rectangle(150, 150, Color.GREEN);
        greenRect.setViewOrder(-1);
        final Rectangle blueRect = new Rectangle(150, 150, Color.BLUE);
        blueRect.setViewOrder(1);

        rectsPane.getChildren().addAll(redRect, greenRect, blueRect);

        runAndWait(() -> {
            Group root = new Group(rectsPane);

            testStage = getStage();
            testStage.setTitle("Test View Order in HBox");
            testScene = new Scene(root, WIDTH, HEIGHT);
            testScene.setFill(Color.WHITE);
            testStage.setScene(testScene);
            testStage.show();
        });
        waitFirstFrame();
        runAndWait(() -> {
            // viewOrder has no impact on layout order
            Color color = getColor(testScene, 75, 75);
            assertColorEquals(Color.RED, color, TOLERANCE);
            color = getColor(testScene, 225, 75);
            assertColorEquals(Color.GREEN, color, TOLERANCE);
            color = getColor(testScene, 380, 75);
            assertColorEquals(Color.BLUE, color, TOLERANCE);

            // Change in viewOrder shouldn't affect layout order
            redRect.setViewOrder(-1.5);
        });
        waitNextFrame();
        runAndWait(() -> {
            Color color = getColor(testScene, 75, 75);
            assertColorEquals(Color.RED, color, TOLERANCE);
            color = getColor(testScene, 225, 75);
            assertColorEquals(Color.GREEN, color, TOLERANCE);
            color = getColor(testScene, 380, 75);
            assertColorEquals(Color.BLUE, color, TOLERANCE);
        });
    }
}
