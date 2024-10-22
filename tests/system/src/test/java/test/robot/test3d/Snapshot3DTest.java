/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import test.robot.testharness.VisualTestBase;

/**
 * 3D Snapshot validation tests.
 */
@Timeout(value=15000, unit=TimeUnit.MILLISECONDS)
public final class Snapshot3DTest extends VisualTestBase {

    private static final Object[] pUseSphere = { Boolean.FALSE, Boolean.TRUE };
    private static final Object[] pNumLights = { 0, 1, 2, 3 };

    private static List<Arguments> parameters() {
        List<Arguments> params = new ArrayList();
        for (Object o0 : pUseSphere) {
            for (Object o1 : pNumLights) {
                params.add(Arguments.of(o0, o1));
            }
        }
        return params;
    }

    private static final double TOLERANCE = 0.07;
    private static final int WIDTH = 400;
    private static final int HEIGHT = 400;
    private static final int SAMPLE_X1 = 100;
    private static final int SAMPLE_Y1 = 100;
    private static final int SAMPLE_X2 = 200;
    private static final int SAMPLE_Y2 = 200;
    private static final int SAMPLE_X3 = 300;
    private static final int SAMPLE_Y3 = 300;
    private static final Color bgColor = Color.rgb(10, 10, 40);

    private Stage testStage;
    private Scene testScene;
    private WritableImage wImage;

//    private boolean createSphere;
//    private int numLights;
//
//    public Snapshot3DTest(boolean createSphere, int numLights) {
//        this.createSphere = createSphere;
//        this.numLights = numLights;
//    }

    private Scene buildScene(boolean createSphere, int numLights) {
        Group root = new Group();

        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.WHITE);
        material.setSpecularColor(null);
        Shape3D shape;
        if (createSphere) {
            shape = new Sphere(200);
        } else {
            shape = new Box(300, 300, 300);
        }
        shape.setTranslateX(200);
        shape.setTranslateY(200);
        shape.setTranslateZ(10);
        shape.setMaterial(material);
        root.getChildren().add(shape);

        if (numLights >= 1) {
            AmbientLight ambLight = new AmbientLight(Color.LIMEGREEN);
            root.getChildren().add(ambLight);
        }

        if (numLights >= 2) {
            PointLight pointLight = new PointLight(Color.RED);
            pointLight.setTranslateX(75);
            pointLight.setTranslateY(-50);
            pointLight.setTranslateZ(-200);
            root.getChildren().add(pointLight);
        }

        if (numLights >= 3) {
            PointLight pointLight = new PointLight(Color.BLUE);
            pointLight.setTranslateX(225);
            pointLight.setTranslateY(50);
            pointLight.setTranslateZ(-300);
            root.getChildren().add(pointLight);
        }

        PerspectiveCamera camera = new PerspectiveCamera();

        Scene scene = new Scene(root, WIDTH, HEIGHT, false);
        scene.setFill(bgColor);
        scene.setCamera(camera);

        return scene;
    }

    private void compareColors(Scene scene, WritableImage wImage, int x, int y) {
        Color exColor = getColor(scene, x, y);
        Color sColor = wImage.getPixelReader().getColor(x, y);
        assertColorEquals(exColor, sColor, TOLERANCE);
    }

    @BeforeEach
    public void setupEach() {
        assumeTrue(Platform.isSupported(ConditionalFeature.SCENE3D));
    }

    // -------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSnapshot3D(boolean createSphere, int numLights) {

        runAndWait(() -> {
            testStage = getStage();
            testStage.setTitle("Snapshot 3D Test");

            testScene = buildScene(createSphere, numLights);

            // Take snapshot
            wImage = testScene.snapshot(null);

            testStage.setScene(testScene);
            testStage.show();
        });
        waitFirstFrame();
        runAndWait(() -> {
            // Compare the colors in the snapshot image with those rendered to the scene
            compareColors(testScene, wImage, SAMPLE_X1, SAMPLE_Y1);
            compareColors(testScene, wImage, SAMPLE_X2, SAMPLE_Y2);
            compareColors(testScene, wImage, SAMPLE_X3, SAMPLE_Y3);
        });
    }
}
