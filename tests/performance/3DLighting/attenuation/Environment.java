/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package attenuation;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.LightBase;
import javafx.scene.Node;
import javafx.scene.PointLight;
import javafx.scene.SpotLight;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;

class Environment extends CameraScene3D {

    private final static double LIGHT_Z_DIST = 50;
    private final static double LIGHT_X_DIST = 50;

    private final PointLight pointLight1 = new PointLight(Color.RED);
    private final PointLight pointLight2 = new PointLight(Color.BLUE);
    private final PointLight pointLight3 = new PointLight(Color.MAGENTA);
    private final SpotLight spotLight1 = new SpotLight(Color.RED);
    private final SpotLight spotLight2 = new SpotLight(Color.BLUE);
    private final SpotLight spotLight3 = new SpotLight(Color.MAGENTA);
    final LightBase[] lights = {pointLight1, pointLight2, pointLight3, spotLight1, spotLight2, spotLight3};

    private Node currentShape;

    private final AmbientLight worldLight = new AmbientLight();

    Environment() {
        farClip.set(1000);
        zoom.set(-350);

        for (var light : lights) {
            light.setTranslateZ(-LIGHT_Z_DIST);
            var lightRep = new Sphere(2);
            lightRep.setMaterial(new PhongMaterial(light.getColor()));
            lightRep.translateXProperty().bind(light.translateXProperty());
            lightRep.translateYProperty().bind(light.translateYProperty());
            lightRep.translateZProperty().bind(light.translateZProperty());
            rootGroup.getChildren().addAll(light , lightRep);
        }

        pointLight1.setTranslateX(LIGHT_X_DIST);
        spotLight1.setTranslateX(LIGHT_X_DIST);
        pointLight2.setTranslateX(-LIGHT_X_DIST);
        spotLight2.setTranslateX(-LIGHT_X_DIST);

        pointLight1.setUserData("RED");
        pointLight2.setUserData("BLUE");
        pointLight3.setUserData("MAGENTA");
        spotLight1.setUserData("RED");
        spotLight2.setUserData("BLUE");
        spotLight3.setUserData("MAGENTA");

        rootGroup.getChildren().add(worldLight);
        rootGroup.setMouseTransparent(true);
    }

    Group createBoxes() {
        var front = new Box(200, 200, 1);
        var back = new Box(200, 200, 1);
        var side = new Box(200, 200, 1);
        side.setRotationAxis(Rotate.Y_AXIS);
        side.setRotate(90);
        side.setTranslateX(LIGHT_Z_DIST * 2);
        side.setTranslateZ(-LIGHT_Z_DIST);
        front.setTranslateZ(LIGHT_Z_DIST);
        back.setTranslateZ(-LIGHT_Z_DIST * 3);
        return new Group(front, back, side);
    }

    Sphere createSphere(int subdivisions) {
        return new Sphere(50, subdivisions);
    }

    MeshView createMeshView(int quadNum) {
        // Points and texCoords array defining a single quad that will
        // be referenced by all pairs of triangles in the faces array
        final float[] points = {
            -75.0f,  75.0f, 0.0f,
             75.0f,  75.0f, 0.0f,
             75.0f, -75.0f, 0.0f,
            -75.0f, -75.0f, 0.0f
        };
        final float[] texCoords = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
        };
        // List of faces defining a single quad (pair of triangles).
        // This is replicated for the desired number of quads
        var face = List.of(
            0, 0, 1, 1, 2, 2,
            0, 0, 2, 2, 3, 3
        );

        var faces = new ArrayList<Integer>(quadNum * face.size());
        for (int i = 0; i < quadNum; i++) {
            faces.addAll(face);
        }

        var mesh = new TriangleMesh();
        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(texCoords);
        int[] array = faces.stream().mapToInt(i -> i).toArray();
        mesh.getFaces().setAll(array);

        var mv = new MeshView(mesh);
        return mv;
    }

    void switchTo(Node node) {
        worldLight.getExclusionScope().remove(currentShape);
        worldLight.getExclusionScope().add(node);
        rootGroup.getChildren().remove(currentShape);
        rootGroup.getChildren().add(node);
        currentShape = node;
    }
}
