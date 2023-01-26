/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.DirectionalLight;
import javafx.scene.Group;
import javafx.scene.LightBase;
import javafx.scene.Node;
import javafx.scene.PointLight;
import javafx.scene.SpotLight;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;

class Environment extends CameraScene3D {

    private final static double LIGHT_REP_RADIUS = 2;
    private final static double LIGHT_Z_DIST = 50;
    private final static double LIGHT_X_DIST = 50;

    private final static double SPHERE_RADIUS = 50;

    private final AmbientLight ambientLight1 = new AmbientLight(Color.WHITE);
    private final AmbientLight ambientLight2 = new AmbientLight(Color.RED);
    private final AmbientLight ambientLight3 = new AmbientLight(Color.BLACK);
    final List<AmbientLight> ambientLights = List.of(ambientLight1, ambientLight2, ambientLight3);

    private final DirectionalLight directionalLight1 = new DirectionalLight(Color.RED);
    private final DirectionalLight directionalLight2 = new DirectionalLight(Color.BLUE);
    private final DirectionalLight directionalLight3 = new DirectionalLight(Color.MAGENTA);
    final List<DirectionalLight> directionalLights = List.of(directionalLight1, directionalLight2, directionalLight3);

    private final PointLight pointLight1 = new PointLight(Color.RED);
    private final PointLight pointLight2 = new PointLight(Color.BLUE);
    private final PointLight pointLight3 = new PointLight(Color.MAGENTA);
    final List<PointLight> pointLights = List.of(pointLight1, pointLight2, pointLight3);

    private final SpotLight spotLight1 = new SpotLight(Color.RED);
    private final SpotLight spotLight2 = new SpotLight(Color.BLUE);
    private final SpotLight spotLight3 = new SpotLight(Color.MAGENTA);
    final List<SpotLight> spotLights = List.of(spotLight1, spotLight2, spotLight3);

    private Group shapeGroup = new Group();
    private Group lightsGroup = new Group();

    Environment() {
        setStyle("-fx-background-color: teal");

        farClip.set(1000);
        zoom.set(-350);

        ambientLights.forEach(this::addLight);
        directionalLights.forEach(this::addLight);
        pointLights.forEach(this::setupLight);
        spotLights.forEach(this::setupLight);

        pointLight1.setTranslateX(LIGHT_X_DIST);
        spotLight1.setTranslateX(LIGHT_X_DIST);
        pointLight2.setTranslateX(-LIGHT_X_DIST);
        spotLight2.setTranslateX(-LIGHT_X_DIST);

        directionalLight1.setDirection(new Point3D(-LIGHT_X_DIST, 0, LIGHT_Z_DIST));
        directionalLight2.setDirection(new Point3D(LIGHT_X_DIST, 0, LIGHT_Z_DIST));

        rootGroup.getChildren().addAll(lightsGroup, shapeGroup);
        rootGroup.setMouseTransparent(true);
    }

    private void setupLight(PointLight light) {
        light.setTranslateZ(-LIGHT_Z_DIST);
        addLight(light);

        var lightRep = new Sphere(LIGHT_REP_RADIUS);
        var lightRepMat = new PhongMaterial();
        lightRepMat.setSelfIlluminationMap(Boxes.createMapImage(light.colorProperty()));
        lightRep.setMaterial(lightRepMat);
        lightRep.translateXProperty().bind(light.translateXProperty());
        lightRep.translateYProperty().bind(light.translateYProperty());
        lightRep.translateZProperty().bind(light.translateZProperty());
        lightRep.visibleProperty().bind(light.lightOnProperty());
        rootGroup.getChildren().add(lightRep);
    }

    private void addLight(LightBase light) {
        light.getScope().add(shapeGroup);
        lightsGroup.getChildren().add(light);
    }

    void forceDefaultLight(boolean force) {
        if (force) {
            rootGroup.getChildren().remove(lightsGroup);
        } else {
            rootGroup.getChildren().add(lightsGroup);
        }
    }

    Group createBoxes() {
        return new Boxes(LIGHT_Z_DIST);
    }

    Sphere createSphere(int subdivisions) {
        return new Sphere(SPHERE_RADIUS, subdivisions);
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
        shapeGroup.getChildren().setAll(node);
    }
}
