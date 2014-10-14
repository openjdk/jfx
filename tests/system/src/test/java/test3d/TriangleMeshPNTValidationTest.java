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
package test3d;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.stage.Stage;
import org.junit.Test;
import testharness.VisualTestBase;

/**
 * Basic TriangleMesh validation tests.
 */
public class TriangleMeshPNTValidationTest extends VisualTestBase {

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

    @Test(timeout = 5000)
    public void testInvalidNormalsLength() {
        runAndWait(() -> {
            testStage = getStage();
            testStage.setTitle("TriangleMesh PNT Validation Test");

            // Intentionally set depth buffer to false to reduce test complexity
            testScene = new Scene(buildScene(), WIDTH, HEIGHT, true);
            testScene.setFill(bgColor);
            addCamera(testScene);
            buildSquare();
            // set invalid points
            triMesh.getNormals().setAll(0f, 0.0f /*, -1.0f */);
            testStage.setScene(testScene);
            testStage.show();
        });
        waitFirstFrame();
        runAndWait(() -> {

            if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
                System.out.println("*************************************************************");
                System.out.println("*      Platform isn't SCENE3D capable, skipping 3D test.    *");
                System.out.println("*************************************************************");
                return;
            }
            // Rendering nothing. Should receive warning from validatePoints
            Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
            assertColorEquals(bgColor, color, TOLERANCE);
        });
    }

    @Test(timeout = 5000)
    public void testNormalsLengthChange() {
        runAndWait(() -> {
            testStage = getStage();
            testStage.setTitle("TriangleMesh PNT Validation Test");

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

            if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
                System.out.println("*************************************************************");
                System.out.println("*      Platform isn't SCENE3D capable, skipping 3D test.    *");
                System.out.println("*************************************************************");
                return;
            }
            Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
            assertColorEquals(Color.RED, color, TOLERANCE);

            // Valid change of normal
            triMesh.getPoints().setAll(0, 0, 1);
        });
        waitFirstFrame();
        runAndWait(() -> {
            if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
                System.out.println("*************************************************************");
                System.out.println("*      Platform isn't SCENE3D capable, skipping 3D test.    *");
                System.out.println("*************************************************************");
                return;
            }
            // Rendering nothing because faces is invalid.
            // Should receive warning from validateFaces
            Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
            assertColorEquals(bgColor, color, TOLERANCE);
        });
    }

    @Test(timeout = 5000)
    public void testDegeneratedMeshUpdateNormals() {
        runAndWait(() -> {
            testStage = getStage();
            testStage.setTitle("TriangleMesh PNT Validation Test");

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

            if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
                System.out.println("*************************************************************");
                System.out.println("*      Platform isn't SCENE3D capable, skipping 3D test.    *");
                System.out.println("*************************************************************");
                return;
            }
            // Rendering 2 Triangles that form a square
            Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
            assertColorEquals(Color.RED, color, TOLERANCE);

            color = getColor(testScene, WIDTH / 2 + 10, WIDTH / 2 + 10);
            assertColorEquals(Color.RED, color, TOLERANCE);
            // set normal with degenerated triangle
            triMesh.getNormals().setAll(0, 0, 0);
        });
        waitFirstFrame();
        runAndWait(() -> {
            if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
                System.out.println("*************************************************************");
                System.out.println("*      Platform isn't SCENE3D capable, skipping 3D test.    *");
                System.out.println("*************************************************************");
                return;
            }

            Color color = getColor(testScene, WIDTH / 2 + 10, WIDTH / 2 + 10);
            assertColorEquals(Color.BLACK, color, TOLERANCE);

        });
    }

    @Test(timeout = 5000)
    public void testDegeneratedMeshUpdatePoints() {
        runAndWait(() -> {
            testStage = getStage();
            testStage.setTitle("TriangleMesh Validation Test");

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

            if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
                System.out.println("*************************************************************");
                System.out.println("*      Platform isn't SCENE3D capable, skipping 3D test.    *");
                System.out.println("*************************************************************");
                return;
            }
            // Rendering 2 Triangles that form a square
            Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
            assertColorEquals(Color.RED, color, TOLERANCE);

            color = getColor(testScene, WIDTH / 2 + 10, WIDTH / 2 + 10);
            assertColorEquals(Color.RED, color, TOLERANCE);
            // set points that casuses a degenerated triangle
            triMesh.getPoints().setAll(
                    0.5f, -1.5f, 0f,
                    0.5f, -1.5f, 0f,
                    0.5f, 1.5f, 0f,
                    0.5f, -1.5f, 0f);
        });
        waitFirstFrame();
        runAndWait(() -> {
            if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
                System.out.println("*************************************************************");
                System.out.println("*      Platform isn't SCENE3D capable, skipping 3D test.    *");
                System.out.println("*************************************************************");
                return;
            }

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

        float normals[] = {
            0f, 0f, -1f, 0f, 0f, 1f // Only the first normal is refered.
        };

        float texCoords[] = {0, 0
        };

        int faces[] = {
            2, 0, 0, 1, 0, 0, 3, 0, 0,
            2, 0, 0, 0, 0, 0, 1, 0, 0,};

        triMesh.getPoints().setAll(points);
        triMesh.getNormals().setAll(normals);
        triMesh.getTexCoords().setAll(texCoords);
        triMesh.getFaces().setAll(faces);
    }

    private Group buildScene() {
        triMesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        material = new PhongMaterial();
        material.setDiffuseColor(Color.RED);
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
