/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.Test;
import testharness.VisualTestBase;

/**
 * Basic visual near and far clipping tests using glass Robot to sample pixels.
 */
public class NearAndFarClipTest extends VisualTestBase {

    private Stage testStage;
    private Scene testScene;

    private static final double TOLERANCE = 0.01;
    private static final double OFFSET = 0.05;

    @Test(timeout=5000)
    public void testNearAndFarClips() {
        final int WIDTH = 500;
        final int HEIGHT = 500;
        final double FOV = 30.0;
        final double NEAR = 0.1;
        final double FAR = 10.0;

        runAndWait(new Runnable() {
            @Override
            public void run() {
                testStage = getStage();
                testStage.setTitle("Near and Far Clip Test");

                final double tanOfHalfFOV = Math.tan(Math.toRadians(FOV) / 2.0);
                final double halfHeight = HEIGHT / 2;
                final double focalLenght = halfHeight / tanOfHalfFOV;
                final double eyePositionZ = -1.0 * focalLenght;
                final double nearClipDistance = focalLenght * NEAR + eyePositionZ;
                final double farClipDistance =  focalLenght * FAR + eyePositionZ;

//                System.err.println("In scene coordinate: focalLenght = " + focalLenght
//                        + ", nearClipDistance = " + nearClipDistance
//                        + ", farClipDistance = " + farClipDistance);


                Rectangle insideRect = new Rectangle(220, 220, Color.GREEN);
                insideRect.setLayoutX(140);
                insideRect.setLayoutY(140);

                Rectangle insideNearClip = new Rectangle(16, 16, Color.BLUE);
                insideNearClip.setLayoutX(242);
                insideNearClip.setLayoutY(242);
                insideNearClip.setTranslateZ(nearClipDistance + OFFSET);

                Rectangle outsideNearClip = new Rectangle(16, 16, Color.YELLOW);
                outsideNearClip.setLayoutX(242);
                outsideNearClip.setLayoutY(242);
                outsideNearClip.setTranslateZ(nearClipDistance - OFFSET);

                Rectangle insideFarClip = new Rectangle(3000, 3000, Color.RED);
                insideFarClip.setTranslateX(-1250);
                insideFarClip.setTranslateY(-1250);
                insideFarClip.setTranslateZ(farClipDistance - OFFSET);

                Rectangle outsideFarClip = new Rectangle(4000, 4000, Color.CYAN);
                outsideFarClip.setTranslateX(-1750);
                outsideFarClip.setTranslateY(-1750);
                outsideFarClip.setTranslateZ(farClipDistance + OFFSET);

                Group root = new Group();

                // Render in painter order (far to near)
                root.getChildren().addAll(outsideFarClip, insideFarClip, insideRect, insideNearClip, outsideNearClip);

                // Intentionally set depth buffer to false to reduce test complexity
                testScene = new Scene(root, WIDTH, HEIGHT, false);

                PerspectiveCamera camera = new PerspectiveCamera();
                camera.setFieldOfView(FOV);
                camera.setNearClip(NEAR);
                camera.setFarClip(FAR);
                testScene.setCamera(camera);

                testStage.setScene(testScene);
                testStage.show();
            }
        });
        waitFirstFrame();
        runAndWait(new Runnable() {
            @Override public void run() {
                
                if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
                    System.out.println("*************************************************************");
                    System.out.println("*      Platform isn't SCENE3D capable, skipping 3D test.    *");
                    System.out.println("*************************************************************");
                    return;
                }

                Color color;
                // Enable Near Clip test once RT-32880 is fixed.
                // D3D: Near clip appears to clip further (away from viewer) than the specified near value 
//                // Verify Near Clip
//                Color color = getColor(testScene, WIDTH / 2, HEIGHT / 2);
//                assertColorEquals(Color.BLUE, color, TOLERANCE);

                // Verify Inside Rect
                color = getColor(testScene, (WIDTH / 3), HEIGHT / 2);
                assertColorEquals(Color.GREEN, color, TOLERANCE);

                // Verify Far Clip
                color = getColor(testScene, WIDTH / 5, HEIGHT / 2);
                assertColorEquals(Color.RED, color, TOLERANCE);

                // Verify Fill
                color = getColor(testScene, WIDTH / 8, HEIGHT / 2);
                assertColorEquals(Color.WHITE, color, TOLERANCE);
            }
        });
    }

}
