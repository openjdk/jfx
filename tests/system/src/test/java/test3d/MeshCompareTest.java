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

import com.sun.javafx.geom.Vec3f;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
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
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.stage.Stage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import testharness.VisualTestBase;

/**
 * 3D Snapshot validation tests.
 */
@RunWith(Parameterized.class)
public class MeshCompareTest extends VisualTestBase {

    private static Collection params = null;

    private static final Object[] pNumLights = { 0, 1, 2, 3 };

    @Parameterized.Parameters
    public static Collection getParams() {
        if (params == null) {
            params = new ArrayList();
            for (Object o1 : pNumLights) {
                params.add(new Object[]{o1});
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
    private MeshView shape;

    private int numLights;

    public MeshCompareTest(int numLights) {
        this.numLights = numLights;
    }

    private TriangleMesh createICOSphere(float scale) {
        final int pointSize = 3; // x, y, z
        final int texCoordSize = 2; // u, v
        final int faceSize = 9; // 3 point indices, 3 normal indices and 3 texCoord indices per triangle
        
        // create 12 vertices of a icosahedron
        int numVerts = 12;
        float t = (float) ((1.0 + Math.sqrt(5.0)) / 2.0);
        float points[] = {
            -1, t, 0,
            1, t, 0,
            -1, -t, 0,
            1, -t, 0,
            0, -1, t,
            0, 1, t,
            0, -1, -t,
            0, 1, -t,
            t, 0, -1,
            t, 0, 1,
            -t, 0, -1,
            -t, 0, 1
        };

        float texCoords[] = new float[numVerts * texCoordSize];

        for(int i = 0; i < numVerts; i++) {
            int pointIndex = i * pointSize;
            points[pointIndex] *= scale;    
            points[pointIndex + 1] *= scale;    
            points[pointIndex + 2] *= scale;
            int texCoordIndex = i * texCoordSize;
            texCoords[texCoordIndex] = 0f;
            texCoords[texCoordIndex + 1] = 0f;
                
        }

        // create 20 triangles of the icosahedron
        int faces[] = {
            0, 0, 11, 0, 5, 0,
            0, 0, 5, 0, 1, 0,            
            0, 0, 1, 0, 7, 0,
            0, 0, 7, 0, 10, 0,
            0, 0, 10, 0, 11, 0,
            1, 0, 5, 0, 9, 0,
            5, 0, 11, 0, 4, 0,
            11, 0, 10, 0, 2, 0,
            10, 0, 7, 0, 6, 0,
            7, 0, 1, 0, 8, 0,
            3, 0, 9, 0, 4, 0,
            3, 0, 4, 0, 2, 0,
            3, 0, 2, 0, 6, 0,
            3, 0, 6, 0, 8, 0,
            3, 0, 8, 0, 9, 0,
            4, 0, 9, 0, 5, 0,
            2, 0, 4, 0, 11, 0,
            6, 0, 2, 0, 10, 0,
            8, 0, 6, 0, 7, 0,
            9, 0, 8, 0, 1, 0
        };

        TriangleMesh triangleMesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);
        triangleMesh.getPoints().setAll(points);
        triangleMesh.getTexCoords().setAll(texCoords);
        triangleMesh.getFaces().setAll(faces);

        return triangleMesh;
    }

    private TriangleMesh createPNTICOSphere(float scale) {
        final int pointSize = 3; // x, y, z
        final int normalSize = 3; // nx, ny, nz
        final int texCoordSize = 2; // u, v
        final int faceSize = 9; // 3 point indices, 3 normal indices and 3 texCoord indices per triangle
        
        // create 12 vertices of a icosahedron
        int numVerts = 12;
        float points[] = new float[numVerts * pointSize];
        float normals[] = new float[numVerts * normalSize];
        float texCoords[] = new float[numVerts * texCoordSize];

        Vec3f[] arrV = new Vec3f[numVerts];
        float t = (float) ((1.0 + Math.sqrt(5.0)) / 2.0);
        arrV[0] = new Vec3f(-1, t, 0);
        arrV[1] = new Vec3f(1, t, 0);
        arrV[2] = new Vec3f(-1, -t, 0);
        arrV[3] = new Vec3f(1, -t, 0);

        arrV[4] = new Vec3f(0, -1, t);
        arrV[5] = new Vec3f(0, 1, t);
        arrV[6] = new Vec3f(0, -1, -t);
        arrV[7] = new Vec3f(0, 1, -t);

        arrV[8] = new Vec3f(t, 0, -1);
        arrV[9] = new Vec3f(t, 0, 1);
        arrV[10] = new Vec3f(-t, 0, -1);
        arrV[11] = new Vec3f(-t, 0, 1);

        for(int i = 0; i < numVerts; i++) {
            int pointIndex = i * pointSize;
            points[pointIndex] = scale * arrV[i].x;    
            points[pointIndex + 1] = scale * arrV[i].y;    
            points[pointIndex + 2] = scale * arrV[i].z;

            int normalIndex = i * normalSize;
            arrV[i].normalize();
            normals[normalIndex] = arrV[i].x;
            normals[normalIndex + 1] = arrV[i].y;
            normals[normalIndex + 2] = arrV[i].z;

            int texCoordIndex = i * texCoordSize;
            texCoords[texCoordIndex] = 0f;
            texCoords[texCoordIndex + 1] = 0f;         
        }

        // create 20 triangles of the icosahedron
        int faces[] = {
            0, 0, 0, 11, 11, 0, 5, 5, 0,
            0, 0, 0, 5, 5, 0, 1, 1, 0,            
            0, 0, 0, 1, 1, 0, 7, 7, 0,
            0, 0, 0, 7, 7, 0, 10, 10, 0,
            0, 0, 0, 10, 10, 0, 11, 11, 0,
            1, 1, 0, 5, 5, 0, 9, 9, 0,
            5, 5, 0, 11, 11, 0, 4, 4, 0,
            11, 11, 0, 10, 10, 0, 2, 2, 0,
            10, 10, 0, 7, 7, 0, 6, 6, 0,
            7, 7, 0, 1, 1, 0, 8, 8, 0,
            3, 3, 0, 9, 9, 0, 4, 4, 0,
            3, 3, 0, 4, 4, 0, 2, 2, 0,
            3, 3, 0, 2, 2, 0, 6, 6, 0,
            3, 3, 0, 6, 6, 0, 8, 8, 0,
            3, 3, 0, 8, 8, 0, 9, 9, 0,
            4, 4, 0, 9, 9, 0, 5, 5, 0,
            2, 2, 0, 4, 4, 0, 11, 11, 0,
            6, 6, 0, 2, 2, 0, 10, 10, 0,
            8, 8, 0, 6, 6, 0, 7, 7, 0,
            9, 9, 0, 8, 8, 0, 1, 1, 0
        };

        TriangleMesh triangleMesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        triangleMesh.getPoints().setAll(points);
        triangleMesh.getNormals().setAll(normals);
        triangleMesh.getTexCoords().setAll(texCoords);
        triangleMesh.getFaces().setAll(faces);

        return triangleMesh;
    }

    private Scene buildScene() {
        Group root = new Group();

        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.WHITE);
        material.setSpecularColor(null);
        shape = new MeshView();
        shape.setMesh(createPNTICOSphere(100));
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

    // -------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------

    @Test(timeout=5000)
    public void testSnapshot3D() {
        final AtomicBoolean scene3dSupported = new AtomicBoolean();
        runAndWait(() -> scene3dSupported.set(Platform.isSupported(ConditionalFeature.SCENE3D)));
        if (!scene3dSupported.get()) {
            System.out.println("*************************************************************");
            System.out.println("*      Platform isn't SCENE3D capable, skipping 3D test.    *");
            System.out.println("*************************************************************");
            return;
        }

        runAndWait(() -> {
            testStage = getStage();
            testStage.setTitle("Mesh VertexFormat Compare Test");

            testScene = buildScene();

            // Take snapshot
            wImage = testScene.snapshot(null);

            shape.setMesh(createICOSphere(100));
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
