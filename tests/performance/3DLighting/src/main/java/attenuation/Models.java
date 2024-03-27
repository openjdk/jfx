/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;

/**
 * Utility class for creating 3D shapes.
 */
final class Models {

    private final static double SPHERE_RADIUS = 50;
    private final static int DEFAULT_SPHERE_SUBDIVISIONS = 50;
    private final static double CYLINDER_RADIUS = 30;
    private final static double CYLINDER_HEIGHT = 150;

    enum Model {
        NONE,
        BOXES,
        BOX,
        CYLINDER,
        SPHERE
    }

    static Node createModel(Model model) {
        Node node = switch (model) {
            case NONE -> new Group();
            case BOXES -> createBoxes(Environment.LIGHT_Z_DIST);
            case BOX -> createBox(Environment.LIGHT_Z_DIST);
            case CYLINDER -> createCylinder(CYLINDER_RADIUS, CYLINDER_HEIGHT);
            case SPHERE -> createSphere(DEFAULT_SPHERE_SUBDIVISIONS);
        };
        node.setTranslateZ(Environment.LIGHT_Z_DIST);
        return node;
    }

    /**
     * Creates a box-like structure with 3 edges.
     *
     * @param size distance from the center of the box to an edge
     */
    private static Group createBoxes(double size) {
        var back = createBox(size);
        var right = createBox(size);
        right.setRotationAxis(Rotate.Y_AXIS);
        right.setRotate(90);
        right.setTranslateX(size * 2);
        right.setTranslateZ(-size * 2);
        var left = createBox(size);
        left.setRotationAxis(Rotate.Y_AXIS);
        left.setRotate(90);
        left.setTranslateX(-size * 2);
        left.setTranslateZ(-size * 2);
        return new Group(left, back , right);
    }

    private static Box createBox(double size) {
        var shape = new Box(size * 4, size * 4, 1);
        shape.setMaterial(MaterialControls.MATERIAL);
        return shape;
    }

    private static Cylinder createCylinder(double radius, double height) {
        var shape = new Cylinder(radius, height);
        shape.setMaterial(MaterialControls.MATERIAL);
        return shape;
    }

    static Sphere createSphere(int subdivisions) {
        var shape = new Sphere(SPHERE_RADIUS, subdivisions);
        shape.setMaterial(MaterialControls.MATERIAL);
        return shape;
    }

    static MeshView createMeshView(int quadNum) {
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

        return new MeshView(mesh);
    }
}
