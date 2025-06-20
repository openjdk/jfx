/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.test3d;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.util.concurrent.TimeUnit;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import test.robot.testharness.VisualTestBase;

/**
 * This test verifies the fix for JDK-8255015.
 * It ensures that a sphere is illuminated correctly by a PointLight in a SubScene.
 * The bug occurred on MacBooks with retina displays (maybe also on other systems with HiDPI displays).
 * It was caused by not using the pixel scale factor in SubScenes at the moment, when the light sources
 * were located and so they got wrong positions. This test does not check the light source positions or
 * the pixel scale factor, but it simply creates a test scene containing a sphere and a perspective
 * camera. Some pixels of the scene will be tested if they have got the expected color and brightness.
 */
@Timeout(value=15000, unit=TimeUnit.MILLISECONDS)
public class PointLightIlluminationTest extends VisualTestBase {

    private static final int    SCENE_WIDTH_HEIGHT = 100;
    private static final int    BACKGROUND_PIXEL_X = 1;
    private static final int    BACKGROUND_PIXEL_Y = 1;
    private static final double CORNER_FACTOR      = 0.218;
    private static final int    LEFT_CORNER_X      = (int) (SCENE_WIDTH_HEIGHT * CORNER_FACTOR);
    private static final int    RIGHT_CORNER_X     = (int) (SCENE_WIDTH_HEIGHT * (1 - CORNER_FACTOR));
    private static final int    UPPER_CORNER_Y     = (int) (SCENE_WIDTH_HEIGHT * CORNER_FACTOR);
    private static final int    LOWER_CORNER_Y     = (int) (SCENE_WIDTH_HEIGHT * (1 - CORNER_FACTOR));
    private static final double COLOR_TOLERANCE    = 0.07;
    private static volatile Scene testScene = null;

    @BeforeEach
    public void setupEach() {
        assumeTrue(Platform.isSupported(ConditionalFeature.SCENE3D));

        // Use the same test scene for all tests
        if (testScene == null) {
            runAndWait(() -> {
                Stage testStage = getStage();
                testScene = createTestScene();
                testStage.setScene(testScene);
                testStage.show();
            });
            // Ensure that the scene is really displayed, before the tests begin
            waitFirstFrame();
        }
    }

    @Test
    public void sceneBackgroundColorShouldBeBlue() {
        runAndWait(() -> {
            assertColorEquals(
                    Color.BLUE,
                    getColor(testScene, BACKGROUND_PIXEL_X, BACKGROUND_PIXEL_Y),
                    COLOR_TOLERANCE);
        });
    }

    @Test
    public void sphereUpperLeftPixelColorShouldBeDarkRed() {
        runAndWait(() -> {
            Color color = getColor(testScene, LEFT_CORNER_X, UPPER_CORNER_Y);
            assertColorEquals(Color.DARKRED, color, COLOR_TOLERANCE);
        });
    }

    @Test
    public void sphereUpperRightPixelColorShouldBeDarkRed() {
        runAndWait(() -> {
            Color color = getColor(testScene, RIGHT_CORNER_X, UPPER_CORNER_Y);
            assertColorEquals(Color.DARKRED, color, COLOR_TOLERANCE);
        });
    }

    @Test
    public void sphereLowerRightPixelColorShouldBeDarkRed() {
        runAndWait(() -> {
            Color color = getColor(testScene, RIGHT_CORNER_X, LOWER_CORNER_Y);
            assertColorEquals(Color.DARKRED, color, COLOR_TOLERANCE);
        });
    }

    @Test
    public void sphereLowerLeftPixelColorShouldBeDarkRed() {
        runAndWait(() -> {
            Color color = getColor(testScene, LEFT_CORNER_X, LOWER_CORNER_Y);
            assertColorEquals(Color.DARKRED, color, COLOR_TOLERANCE);
        });
    }

    @Test
    public void sphereCenterPixelColorShouldBeRed() {
        runAndWait(() -> {
            Color color = getColor(testScene, SCENE_WIDTH_HEIGHT / 2, SCENE_WIDTH_HEIGHT / 2);
            assertColorEquals(Color.RED, color, COLOR_TOLERANCE);
        });
    }

    /**
     * This method is overridden and doing nothing, so that the test stage and scene
     * will not be hidden (which is the default behavior in the super class). The same
     * scene can be used for all the illumination tests by this class.
     */
    @Override
    @AfterEach
    public void doTeardown() {
    }

    /**
     * Creates a new scene with a subscene which contains a perspective camera and a sphere
     * Although this test class checks the point light illumination, there is no explicit point light
     * in the scene. For the test, it is sufficient to use the default point light which is created
     * by the perspective camera and located at the same position like the camera.
     * @return Scene
     */
    private Scene createTestScene() {
        Sphere sphere = new Sphere(SCENE_WIDTH_HEIGHT / 2);
        // By moving the sphere and the camera away from point 0, the effect of the bug JDK-8255015
        // becomes more visible, because all point light coordinates would be wrong, if the pixel scale
        // factor was not set correctly. If it was not moved away from point 0, only the Z coordinate
        // of the point light would be wrong.
        sphere.setTranslateX(SCENE_WIDTH_HEIGHT);
        sphere.setTranslateY(SCENE_WIDTH_HEIGHT);
        sphere.setMaterial(new PhongMaterial(Color.RED));

        SubScene subScene = new SubScene(
                new Group(sphere),
                SCENE_WIDTH_HEIGHT,
                SCENE_WIDTH_HEIGHT,
                true,
                SceneAntialiasing.DISABLED);
        subScene.setFill(Color.BLUE);

        PerspectiveCamera perspectiveCamera = new PerspectiveCamera(true);
        // Move the camera, so that it points directly to the center of the sphere.
        perspectiveCamera.setTranslateX(SCENE_WIDTH_HEIGHT);
        perspectiveCamera.setTranslateY(SCENE_WIDTH_HEIGHT);
        perspectiveCamera.setTranslateZ(-2 * SCENE_WIDTH_HEIGHT);
        perspectiveCamera.setFarClip(4 * SCENE_WIDTH_HEIGHT);
        subScene.setCamera(perspectiveCamera);

        return new Scene(new Group(subScene), SCENE_WIDTH_HEIGHT, SCENE_WIDTH_HEIGHT);
    }
}
