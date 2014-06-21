/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package helloworld;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.Test;
import testharness.VisualTestBase;

/**
 * Basic visual tests using glass Robot to sample pixels.
 */
public class RectangleTest extends VisualTestBase {

    private Stage testStage;
    private Scene testScene;

    private static final double TOLERANCE = 0.07;

    @Test(timeout=5000)
    public void testSceneDefaultFill() {
        final int WIDTH = 400;
        final int HEIGHT = 300;

        runAndWait(() -> {
            testStage = getStage();
            testScene = new Scene(new Group(), WIDTH, HEIGHT);
            testStage.setScene(testScene);
            testStage.show();
        });
        waitFirstFrame();
        runAndWait(() -> {
            Color color = getColor(testScene, WIDTH / 2, HEIGHT / 2);
            assertColorEquals(Color.WHITE, color, TOLERANCE);
        });
    }

    @Test(timeout=5000)
    public void testSceneFillColor() {
        final int WIDTH = 400;
        final int HEIGHT = 300;

        runAndWait(() -> {
            testStage = getStage();
            testScene = new Scene(new Group(), WIDTH, HEIGHT);
            testScene.setFill(Color.CORNFLOWERBLUE);
            testStage.setScene(testScene);
            testStage.show();
        });
        waitFirstFrame();
        runAndWait(() -> {
            Color color = getColor(testScene, WIDTH / 2, HEIGHT / 2);
            assertColorEquals(Color.CORNFLOWERBLUE, color, TOLERANCE);
        });
    }

    @Test(timeout=5000)
    public void testFillRect() {
        final int WIDTH = 400;
        final int HEIGHT = 300;
        final int RECT_X = 200;
        final int RECT_Y = 70;
        final int RECT_W = 30;
        final int RECT_H = 60;
        final int OFFSET = 10;

        runAndWait(() -> {
            Rectangle rect = new Rectangle(RECT_X, RECT_Y, RECT_W, RECT_H);
            rect.setFill(Color.ORANGE);
            Group root = new Group(rect);
            testScene = new Scene(root, WIDTH, HEIGHT);
            testScene.setFill(Color.PALEGREEN);

            testStage = getStage();
            testStage.setScene(testScene);
            testStage.show();
        });
        waitFirstFrame();
        runAndWait(() -> {
            Color color = getColor(testScene, RECT_X - OFFSET, RECT_Y - OFFSET);
            assertColorEquals(Color.PALEGREEN, color, TOLERANCE);
            color = getColor(testScene, RECT_X + RECT_W + OFFSET, RECT_Y + RECT_H + OFFSET);
            assertColorEquals(Color.PALEGREEN, color, TOLERANCE);
            color = getColor(testScene, RECT_X + (RECT_W / 2), RECT_Y + (RECT_H / 2));
            assertColorEquals(Color.ORANGE, color, TOLERANCE);
        });
    }

    @Test(timeout=5000)
    public void testAddFillRect() {
        final int WIDTH = 400;
        final int HEIGHT = 300;
        final int RECT_X = 200;
        final int RECT_Y = 70;
        final int RECT_W = 30;
        final int RECT_H = 60;
        final int OFFSET = 10;

        runAndWait(() -> {
            Group root = new Group();
            testScene = new Scene(root, WIDTH, HEIGHT);
            testScene.setFill(Color.PALEGREEN);

            testStage = getStage();
            testStage.setScene(testScene);
            testStage.show();
        });
        waitFirstFrame();
        runAndWait(() -> {
            Color color = getColor(testScene, RECT_X - OFFSET, RECT_Y - OFFSET);
            assertColorEquals(Color.PALEGREEN, color, TOLERANCE);
            color = getColor(testScene, RECT_X + RECT_W + OFFSET, RECT_Y + RECT_H + OFFSET);
            assertColorEquals(Color.PALEGREEN, color, TOLERANCE);
            color = getColor(testScene, RECT_X + (RECT_W / 2), RECT_Y + (RECT_H / 2));
            assertColorEquals(Color.PALEGREEN, color, TOLERANCE);
        });

        // Now add a rectangle
        runAndWait(() -> {
            Rectangle rect = new Rectangle(RECT_X, RECT_Y, RECT_W, RECT_H);
            rect.setFill(Color.ORANGE);
            Group root = (Group)testScene.getRoot();
            root.getChildren().add(rect);
        });
        waitNextFrame();
        runAndWait(() -> {
            Color color = getColor(testScene, RECT_X - OFFSET, RECT_Y - OFFSET);
            assertColorEquals(Color.PALEGREEN, color, TOLERANCE);
            color = getColor(testScene, RECT_X + RECT_W + OFFSET, RECT_Y + RECT_H + OFFSET);
            assertColorEquals(Color.PALEGREEN, color, TOLERANCE);
            color = getColor(testScene, RECT_X + (RECT_W / 2), RECT_Y + (RECT_H / 2));
            assertColorEquals(Color.ORANGE, color, TOLERANCE);
        });
    }

}
