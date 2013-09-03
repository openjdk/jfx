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

package fx83dfeatures;

import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.stage.Stage;

public class SimpleMeshTexCoordTest extends Application {

    Group root;
    PointLight pointLight;
    MeshView meshView;
    TriangleMesh triMesh;
    PhongMaterial material;
    final Image diffuseMap = new Image("resources/cup_diffuseMap_1024.png");
    
    static TriangleMesh buildTriangleMesh() {

        // Create points
        float points[] = {
            0.0f, 0.0f, 0.0f,
            400.0f, 0.0f, 0.0f,
            0.0f, 400.0f, 0.0f,
            400.0f, 400.0f, 0.0f};

        // Create texCoords
        float texCoords[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        };

        // Create faces
        // Interleaving 3 point indices and 3 texCoord indices per triangle
        int faces[] = {
            // Triangle #1
            0, 0, 2, 2, 3, 3,
            // Triangle #2
            3, 3, 1, 1, 0, 0
        };

        TriangleMesh triangleMesh = new TriangleMesh();
        triangleMesh.getPoints().setAll(points);
        triangleMesh.getTexCoords().setAll(texCoords);
        triangleMesh.getFaces().setAll(faces);

        return triangleMesh;
    }

    private Group buildScene() {
        triMesh = buildTriangleMesh();

        material = new PhongMaterial();
        material.setDiffuseMap(diffuseMap);
        material.setSpecularColor(Color.rgb(30, 30, 30));
        meshView = new MeshView(triMesh);
        meshView.setTranslateX(200);
        meshView.setTranslateY(200);
        meshView.setTranslateZ(20);
        meshView.setMaterial(material);
        meshView.setDrawMode(DrawMode.FILL);
        meshView.setCullFace(CullFace.BACK);

        pointLight = new PointLight(Color.ANTIQUEWHITE);
        pointLight.setTranslateX(150);
        pointLight.setTranslateY(-100);
        pointLight.setTranslateZ(-1000);

        root = new Group(meshView, pointLight);
        return root;
    }

    private PerspectiveCamera addCamera(Scene scene) {
        PerspectiveCamera perspectiveCamera = new PerspectiveCamera();
        scene.setCamera(perspectiveCamera);
        return perspectiveCamera;
    }

    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(buildScene(), 800, 800, true);
        scene.setFill(Color.GRAY);
        addCamera(scene);
        primaryStage.setTitle("SimpleMeshTexCoordTest");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
