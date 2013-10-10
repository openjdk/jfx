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
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.stage.Stage;
import org.junit.Test;
import testharness.VisualTestBase;

/**
 * Basic TriangleMesh validation tests.
 */
public class TriangleMeshValidationTest extends VisualTestBase {

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

    @Test(timeout=5000)
    public void testEmptyMesh() {
        runAndWait(new Runnable() {
            @Override
            public void run() {
                testStage = getStage();
                testStage.setTitle("TriangleMesh Validation Test");

                // Intentionally set depth buffer to false to reduce test complexity
                testScene = new Scene(buildScene(), WIDTH, HEIGHT, true);
                testScene.setFill(bgColor);
                addCamera(testScene);
                triMesh = new TriangleMesh();
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
                // No warning. Rendering nothing.
                Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
                assertColorEquals(bgColor, color, TOLERANCE);
            }
        });
    }

    @Test(timeout=5000)
    public void testInvalidPointsLength() {
        runAndWait(new Runnable() {
            @Override
            public void run() {
                testStage = getStage();
                testStage.setTitle("TriangleMesh Validation Test");
                
                // Intentionally set depth buffer to false to reduce test complexity
                testScene = new Scene(buildScene(), 800, 800, true);
                testScene.setFill(bgColor);
                addCamera(testScene);
                buildBox();
                // set invalid points
                triMesh.getPoints().setAll(1, 1, 1,
                                1, 1, -1,
                                1, -1, 1,
                                1, -1, -1,
                                -1, 1, 1,
                                -1, 1, -1,
                                -1, //-1, 1,
                                -1, -1, -1);
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
                // Rendering nothing. Should receive warning from validatePoints
                Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
                assertColorEquals(bgColor, color, TOLERANCE);
            }
        });
    }

    @Test(timeout=5000)
    public void testInvalidTexCoordLength() {
        runAndWait(new Runnable() {
            @Override
            public void run() {
                testStage = getStage();
                testStage.setTitle("TriangleMesh Validation Test");

                // Intentionally set depth buffer to false to reduce test complexity
                testScene = new Scene(buildScene(), 800, 800, true);
                testScene.setFill(bgColor);
                addCamera(testScene);
                buildBox();
                // set invalid texcoords
                triMesh.getTexCoords().setAll(0, 0,
                        0, 1,
                        1, //0,
                        1, 1);
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
                // Rendering nothing. Should receive warning from validateTexcoords
                Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
                assertColorEquals(bgColor, color, TOLERANCE);
            }
        });
    }

    @Test(timeout=5000)
    public void testInvalidFacesLength() {
        runAndWait(new Runnable() {
            @Override
            public void run() {
                testStage = getStage();
                testStage.setTitle("TriangleMesh Validation Test");

                // Intentionally set depth buffer to false to reduce test complexity
                testScene = new Scene(buildScene(), 800, 800, true);
                testScene.setFill(bgColor);
                addCamera(testScene);
                buildBox();
                // set invalid faces
                triMesh.getFaces().setAll(0, 0, 2, 2, 1, 1,
                                2, 2, 3, 3, 1, 1,
                                4, 0, 5, 1, 6, 2,
                                6, 2, 5, 1, 7, 3,
                                0, 0, 1, 1, 4, 2,
                                4, 2, 1, 1, 5, //3,
                                2, 0, 6, 2, 3, 1,
                                3, 1, 6, 2, 7, 3,
                                0, 0, 4, 1, 2, 2,
                                2, 2, 4, 1, 6, 3,
                                1, 0, 3, 1, 5, 2,
                                5, 2, 3, 1, 7, 3);
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
                // Rendering nothing. Should receive warning from validateFaces
                Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
                assertColorEquals(bgColor, color, TOLERANCE);
            }
        });
    }

    @Test(timeout=5000)
    public void testInvalidFacesIndex() {
        runAndWait(new Runnable() {
            @Override
            public void run() {
                testStage = getStage();
                testStage.setTitle("TriangleMesh Validation Test");

                // Intentionally set depth buffer to false to reduce test complexity
                testScene = new Scene(buildScene(), 800, 800, true);
                testScene.setFill(bgColor);
                addCamera(testScene);
                buildBox();
                triMesh.getFaces().setAll(0, 0, 2, 2, 1, 1,
                                2, 2, 3, 3, 1, 1,
                                4, 0, 5, 1, 6, 2,
                                6, 2, 5, 1, 7, 3,
                                0, 0, 1, 1, 4, 2,
                                4, 2, 1, 1, 5, 8, // 8 is out of bound
                                2, 0, 6, 2, 3, 1,
                                3, 1, 6, 2, 7, 3,
                                0, 0, 4, 1, 2, 2,
                                2, 2, 4, 1, 6, 3,
                                1, 0, 3, 1, 5, 2,
                                5, 2, 3, 1, 7, 3);
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
                // Rendering nothing. Should receive warning from validateFaces
                Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
                assertColorEquals(bgColor, color, TOLERANCE);
            }
        });
    }

    @Test(timeout=5000)
    public void testInvalidFaceSmoothingGroupsLength() {
        runAndWait(new Runnable() {
            @Override
            public void run() {
                testStage = getStage();
                testStage.setTitle("TriangleMesh Validation Test");

                // Intentionally set depth buffer to false to reduce test complexity
                testScene = new Scene(buildScene(), 800, 800, true);
                testScene.setFill(bgColor);
                addCamera(testScene);
                buildBox();
                // set invalid faceSmoothingGroups
                triMesh.getFaceSmoothingGroups().setAll(1, 1, 1, 1, 2, 2,/* 2, 2,*/ 4, 4, 4, 4);
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
                // Rendering nothing. Should receive warning from validateFacesSmoothingGroups
                Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
                assertColorEquals(bgColor, color, TOLERANCE);
            }
        });
    }

    @Test(timeout=5000)
    public void testPointsLengthChange() {
        runAndWait(new Runnable() {
            @Override
            public void run() {
                testStage = getStage();
                testStage.setTitle("TriangleMesh Validation Test");

                // Intentionally set depth buffer to false to reduce test complexity
                testScene = new Scene(buildScene(), 800, 800, true);
                testScene.setFill(bgColor);
                addCamera(testScene);
                buildBox();
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
                Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
                assertColorEquals(Color.RED, color, TOLERANCE);

                // Valid change of points
                triMesh.getPoints().setAll(1, 1, 1);
            }
        });
        waitFirstFrame();
        runAndWait(new Runnable() {
            @Override public void run() {
                // Rendering nothing because faces is invalid.
                // Should receive warning from validateFaces
                Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
                assertColorEquals(bgColor, color, TOLERANCE);
            }
        });
    }

    @Test(timeout=5000)
    public void testTexCoordsLengthChange() {
        runAndWait(new Runnable() {
            @Override
            public void run() {
                testStage = getStage();
                testStage.setTitle("TriangleMesh Validation Test");

                // Intentionally set depth buffer to false to reduce test complexity
                testScene = new Scene(buildScene(), 800, 800, true);
                testScene.setFill(bgColor);
                addCamera(testScene);
                buildBox();
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
                Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
                assertColorEquals(Color.RED, color, TOLERANCE);

                // Valid change of texcoords
                triMesh.getTexCoords().setAll(0, 0, 1, 1);
            }
        });
        waitFirstFrame();
        runAndWait(new Runnable() {
            @Override public void run() {
                // Rendering nothing because faces is invalid.
                // Should receive warning from validateFaces
                Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
                assertColorEquals(bgColor, color, TOLERANCE);
            }
        });
    }

    @Test(timeout=10000)
    public void testFaceLengthChange() {
        runAndWait(new Runnable() {
            @Override
            public void run() {
                testStage = getStage();
                testStage.setTitle("TriangleMesh Validation Test");

                // Intentionally set depth buffer to false to reduce test complexity
                testScene = new Scene(buildScene(), 800, 800, true);
                testScene.setFill(bgColor);
                addCamera(testScene);
                buildBox();
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
                Color color = getColor(testScene, WIDTH / 4, WIDTH / 4);
                assertColorEquals(Color.RED, color, TOLERANCE);

                // Valid change of faces
                triMesh.getFaces().setAll( 5, 2, 3, 1, 7, 3);

            }
        });
        waitFirstFrame();
        runAndWait(new Runnable() {
            @Override public void run() {
                // Rendering nothing because faceSmoothingGroups is invalid.
                // Should receive warning from validateFacesSmoothingGroups
                Color color = getColor(testScene, WIDTH / 4, WIDTH / 4);
                assertColorEquals(bgColor, color, TOLERANCE);
                // Reset faceSmoothingGroups
                triMesh.getFaceSmoothingGroups().setAll();
            }
        });
        waitFirstFrame();
        runAndWait(new Runnable() {
            @Override public void run() {
                // Rendering a triangle
                Color color = getColor(testScene, WIDTH / 4, WIDTH / 4);
                assertColorEquals(Color.RED, color, TOLERANCE);
            }
        });
    }

    @Test(timeout=5000)
    public void testResetFaceSmoothingGroup() {
        runAndWait(new Runnable() {
            @Override
            public void run() {
                testStage = getStage();
                testStage.setTitle("TriangleMesh Validation Test");

                // Intentionally set depth buffer to false to reduce test complexity
                testScene = new Scene(buildScene(), 800, 800, true);
                testScene.setFill(bgColor);
                addCamera(testScene);
                buildBox();
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
                Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
                assertColorEquals(Color.RED, color, TOLERANCE);

                triMesh.getFaceSmoothingGroups().setAll();
            }
        });
        waitFirstFrame();
        runAndWait(new Runnable() {
            @Override public void run() {
                // Empty Sm should not affect rendering
                Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
                assertColorEquals(Color.RED, color, TOLERANCE);
            }
        });
    }

    @Test(timeout=5000)
    public void testUpdateMesh() {
        runAndWait(new Runnable() {
            @Override
            public void run() {
                testStage = getStage();
                testStage.setTitle("TriangleMesh Validation Test");

                // Intentionally set depth buffer to false to reduce test complexity
                testScene = new Scene(buildScene(), 800, 800, true);
                testScene.setFill(bgColor);
                addCamera(testScene);
                buildBox();
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
                // Rendering nothing. Should receive warning from validatePoints
                Color color = getColor(testScene, WIDTH / 3, WIDTH / 3);
                assertColorEquals(Color.RED, color, TOLERANCE);

                color = getColor(testScene, WIDTH / 5, WIDTH / 5);
                assertColorEquals(bgColor, color, TOLERANCE);
                // set points
                triMesh.getPoints().setAll(1.5f, 1.5f, 1.5f,
                                1.5f, 1.5f, -1.5f,
                                1.5f, -1.5f, 1.5f,
                                1.5f, -1.5f, -1.5f,
                                -1.5f, 1.5f, 1.5f,
                                -1.5f, 1.5f, -1.5f,
                                -1.5f, -1.5f, 1.5f,
                                -1.5f, -1.5f, -1.5f);
                triMesh.getTexCoords().setAll(0, 0,
                                1, 0,
                                0, 1,
                                1, 1);
                triMesh.getFaces().setAll(5, 2, 3, 1, 7, 3);
                triMesh.getFaceSmoothingGroups().setAll(1);
            }
        });
        waitFirstFrame();
        runAndWait(new Runnable() {
            @Override public void run() {
                Color color = getColor(testScene, WIDTH / 5, WIDTH / 5);
                assertColorEquals(Color.RED, color, TOLERANCE);
            }
        });
    }

    void buildBox() {

        float points[] = {
            1, 1, 1,
            1, 1, -1,
            1, -1, 1,
            1, -1, -1,
            -1, 1, 1,
            -1, 1, -1,
            -1, -1, 1,
            -1, -1, -1,};

        float texCoords[] = {0, 0,
            0, 1,
            1, 0,
            1, 1};

        int faceSmoothingGroups[] = {
            1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 4
        };

        int faces[] = {
            0, 0, 2, 2, 1, 1,
            2, 2, 3, 3, 1, 1,
            4, 0, 5, 1, 6, 2,
            6, 2, 5, 1, 7, 3,
            0, 0, 1, 1, 4, 2,
            4, 2, 1, 1, 5, 3,
            2, 0, 6, 2, 3, 1,
            3, 1, 6, 2, 7, 3,
            0, 0, 4, 1, 2, 2,
            2, 2, 4, 1, 6, 3,
            1, 0, 3, 1, 5, 2,
            5, 2, 3, 1, 7, 3,};

        triMesh.getPoints().setAll(points);
        triMesh.getTexCoords().setAll(texCoords);
        triMesh.getFaces().setAll(faces);
        triMesh.getFaceSmoothingGroups().setAll(faceSmoothingGroups);
    }

    private Group buildScene() {
        triMesh = new TriangleMesh();
        material = new PhongMaterial();
        material.setDiffuseColor(Color.RED);
        material.setSpecularColor(Color.rgb(30, 30, 30));
        meshView = new MeshView(triMesh);
        meshView.setMaterial(material);
        meshView.setScaleX(200);
        meshView.setScaleY(200);
        meshView.setScaleZ(200);
        meshView.setTranslateX(400);
        meshView.setTranslateY(400);
        meshView.setTranslateZ(10);

        root = new Group(meshView, new AmbientLight());
        return root;
    }

    private PerspectiveCamera addCamera(Scene scene) {
        PerspectiveCamera perspectiveCamera = new PerspectiveCamera(false);
        scene.setCamera(perspectiveCamera);
        return perspectiveCamera;
    }
}
