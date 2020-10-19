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
import javafx.scene.PointLight;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;

class Environment extends CameraScene3D {

    private final PointLight light1 = new PointLight(Color.RED);
    private final PointLight light2 = new PointLight(Color.BLUE);
    private final PointLight light3 = new PointLight(Color.MAGENTA);
    final PointLight[] lights = {light1, light2, light3};

    private Shape3D currentShape;

    private final AmbientLight worldLight = new AmbientLight();

    Environment() {
        farClip.set(1000);
        zoom.set(-350);

        for (var light : lights) {
            light.setTranslateZ(-50);
            var lightRep = new Sphere(2);
            lightRep.setMaterial(new PhongMaterial(light.getColor()));
            lightRep.translateXProperty().bind(light.translateXProperty());
            lightRep.translateYProperty().bind(light.translateYProperty());
            lightRep.translateZProperty().bind(light.translateZProperty());
            rootGroup.getChildren().addAll(light, lightRep);
        }
        light1.setTranslateX(40);
        light2.setTranslateX(-40);
        light1.setUserData("RED");
        light2.setUserData("BLUE");
        light3.setUserData("MAGENTA");

        rootGroup.getChildren().add(worldLight);
        rootGroup.setMouseTransparent(true);
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

    void switchTo(Shape3D node) {
        worldLight.getExclusionScope().remove(currentShape);
        worldLight.getExclusionScope().add(node);
        rootGroup.getChildren().remove(currentShape);
        rootGroup.getChildren().add(node);
        currentShape = node;
    }
}
