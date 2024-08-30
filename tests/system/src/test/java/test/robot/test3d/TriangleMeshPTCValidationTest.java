/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.stage.Stage;
import org.junit.Before;
import org.junit.Test;
import test.robot.testharness.VisualTestBase;

import java.util.stream.Stream;

import static org.junit.Assume.assumeTrue;

/**
 * Basic TriangleMesh color validation.
 */
public class TriangleMeshPTCValidationTest extends VisualTestBase {
    private Stage testStage;
    private Scene testScene;
    private MeshView meshView;
    private TriangleMesh triMesh;
    private PhongMaterial material;
    private Group root;

    private static final double TOLERANCE = 0.07;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 800;
    private Color bgColor = Color.rgb(10, 10, 40);

    @Before
    public void setupEach() {
        assumeTrue(Platform.isSupported(ConditionalFeature.SCENE3D));
    }

    @Test(timeout = 15000)
    public void testInvalidColorsLength() {
        runAndWait(() -> {
            testStage = getStage();
            testStage.setTitle("TriangleMesh PTC Validation Test");

            // Intentionally set depth buffer to false to reduce test complexity
            testScene = new Scene(buildScene(), WIDTH, HEIGHT, true);
            testScene.setFill(bgColor);
            addCamera(testScene);
            buildSquare();
            // set invalid colors
            triMesh.getColors().setAll(0f, 0f, 0f /*, 1.0f */);
            testStage.setScene(testScene);
            testStage.show();
        });
        waitFirstFrame();
        runAndWait(() -> {

            // Rendering nothing. Should receive warning from validatePoints
            Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
            assertColorEquals(bgColor, color, TOLERANCE);
        });
    }

    @Test(timeout = 15000)
    public void testColorsLengthChange() {
        runAndWait(() -> {
            testStage = getStage();
            testStage.setTitle("TriangleMesh PTC Validation Test");

            // Intentionally set depth buffer to false to reduce test complexity
            testScene = new Scene(buildScene(), WIDTH, HEIGHT, true);
            testScene.setFill(bgColor);
            addCamera(testScene);
            buildSquare();
            testStage.setScene(testScene);
            testStage.show();
        });
        waitFirstFrame();
        runAndWait(() -> {

            Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
            assertColorEquals(Color.RED, color, TOLERANCE);

            // Valid change of points
            triMesh.getPoints().setAll(0, 0, 1);
        });
        waitFirstFrame();
        runAndWait(() -> {

            // Rendering nothing because faces is invalid.
            // Should receive warning from validateFaces
            Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
            assertColorEquals(bgColor, color, TOLERANCE);
        });
    }

    @Test(timeout = 15000)
    public void testInvisibleMeshUpdateColors() {
        runAndWait(() -> {
            testStage = getStage();
            testStage.setTitle("TriangleMesh PTC Validation Test");

            // Intentionally set depth buffer to false to reduce test complexity
            testScene = new Scene(buildScene(), WIDTH, HEIGHT, true);
            testScene.setFill(bgColor);
            addCamera(testScene);
            buildSquare();
            testStage.setScene(testScene);
            testStage.show();
        });
        waitFirstFrame();
        runAndWait(() -> {

            // Rendering 2 Triangles that form a square (First triangle is red)
            Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
            assertColorEquals(Color.RED, color, TOLERANCE);

            // Second triangle is blue
            color = getColor(testScene, WIDTH / 2 + 10, WIDTH / 2 + 10);
            assertColorEquals(Color.BLUE, color, TOLERANCE);

            // set normal with invisible vertex colors.
            triMesh.getColors().setAll(1F, 1F, 1F, 0F, 1F, 1F, 1F, 0F);
        });
        waitFirstFrame();
        runAndWait(() -> {

            // Rendering nothing. No warning.
            Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
            assertColorEquals(bgColor, color, TOLERANCE);

        });
    }

    @Test(timeout = 15000)
    public void testDegeneratedMeshUpdatePoints() {
        runAndWait(() -> {
            testStage = getStage();
            testStage.setTitle("TriangleMesh PTC Validation Test");

            // Intentionally set depth buffer to false to reduce test complexity
            testScene = new Scene(buildScene(), WIDTH, HEIGHT, true);
            testScene.setFill(bgColor);
            addCamera(testScene);
            buildSquare();
            testStage.setScene(testScene);
            testStage.show();
        });
        waitFirstFrame();
        runAndWait(() -> {

            // Rendering 2 Triangles that form a square (First triangle is red)
            Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
            assertColorEquals(Color.RED, color, TOLERANCE);

            // Second triangle is blue
            color = getColor(testScene, WIDTH / 2 + 10, WIDTH / 2 + 10);
            assertColorEquals(Color.BLUE, color, TOLERANCE);

            // set points that casuses a degenerated triangle
            triMesh.getPoints().setAll(
                    0.5f, -1.5f, 0f,
                    0.5f, -1.5f, 0f,
                    0.5f, 1.5f, 0f,
                    0.5f, -1.5f, 0f);
        });
        waitFirstFrame();
        runAndWait(() -> {

            Color color = getColor(testScene, WIDTH / 2 + 10, WIDTH / 2 + 10);
            assertColorEquals(bgColor, color, TOLERANCE);

        });
    }

    void buildSquare() {

        float points[] = {
                1.5f, 1.5f, 0f,
                1.5f, -1.5f, 0f,
                -1.5f, 1.5f, 0f,
                -1.5f, -1.5f, 0f
        };

        float colors[] = {
                1f, 0f, 0f, 1f, 0f, 0f, 1f, 1f // Only the first normal is refered.
        };

        float texCoords[] = {0, 0
        };

        int faces[] = {
                2, 0, 0, 1, 0, 0, 3, 0, 0,
                2, 0, 1, 0, 0, 1, 1, 0, 1,};

        triMesh.getPoints().setAll(points);
        triMesh.getColors().setAll(colors);
        triMesh.getTexCoords().setAll(texCoords);
        triMesh.getFaces().setAll(faces);
    }

    private Group buildScene() {
        triMesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD_COLOR);
        material = new PhongMaterial();
        material.setDiffuseColor(Color.WHITE);
        meshView = new MeshView(triMesh);
        meshView.setMaterial(material);
        meshView.setScaleX(200);
        meshView.setScaleY(200);
        meshView.setScaleZ(200);
        meshView.setTranslateX(400);
        meshView.setTranslateY(400);
        meshView.setTranslateZ(10);

        root = new Group(meshView);
        return root;
    }

    private PerspectiveCamera addCamera(Scene scene) {
        PerspectiveCamera perspectiveCamera = new PerspectiveCamera(false);
        scene.setCamera(perspectiveCamera);
        return perspectiveCamera;
    }
}
