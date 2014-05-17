/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package test3d;

import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import org.junit.Test;
import testharness.VisualTestBase;

/**
 * Test 2D shapes rendered with a 3D transform where some of the shapes
 * have empty bounds.
 */
public class RT35019Test extends VisualTestBase {

    private Stage testStage;
    private Scene testScene;

    private static final double TOLERANCE = 0.07;

    @Test(timeout=5000)
    public void testEmptyShapes() {
        final int WIDTH = 400;
        final int HEIGHT = 300;

        runAndWait(() -> {
            Circle emptyCircle = new Circle(10, 10, 0);
            Text emptyText = new Text(10, 10, "");
            Circle circle = new Circle(100, 100, 10);
            circle.setFill(Color.DARKBLUE);

            Group root = new Group(emptyCircle, emptyText, circle);
            root.setRotationAxis(Rotate.Y_AXIS);
            root.setRotate(1);

            testStage = getStage();
            testStage.setTitle("Empty Shapes + 3D Transform");
            testScene = new Scene(root, WIDTH, HEIGHT);
            testScene.setCamera(new PerspectiveCamera());
            testScene.setFill(Color.WHITE);
            testStage.setScene(testScene);
            testStage.show();
        });
        waitFirstFrame();
        runAndWait(() -> {
            Color color = getColor(testScene, 10, 10);
            assertColorEquals(Color.WHITE, color, TOLERANCE);
            color = getColor(testScene, 100, 100);
            assertColorEquals(Color.DARKBLUE, color, TOLERANCE);
        });
    }

}
