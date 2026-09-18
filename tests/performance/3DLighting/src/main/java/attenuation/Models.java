/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

/// Utility class for creating 3D shapes.
final class Models {

    private static final double SPHERE_RADIUS = 50;
    private static final int DEFAULT_SPHERE_SUBDIVISIONS = 50;
    private static final double CYLINDER_RADIUS = 30;
    private static final double CYLINDER_HEIGHT = 150;

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

    /// Creates a box-like structure with 3 edges.
    ///
    /// @param size distance from the center of the box to an edge
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


    // 4 points in a quad with 3 coordinates each
    private static final int COORDS_PER_QUAD = 12;

    // 2 triangles in a face with 6 coordinates each
    private static final int COORDS_PER_FACE = 12;

    // 1 - 4
    // |   |
    // 2 - 3
    private static final float[] TEX_COORDS = {
        0, 0,
        0, 1,
        1, 1,
        1, 0
    };

    private static final float GRID_SIZE = 150f;
    private static final float GAP_RATIO = 0.1f;

    /// Creates a mesh of stacked quads from a single quad so that all quads are overdrawn.
    static MeshView createStackedQuads(int quadNum) {
        // Points and texCoords array defining a single quad that will
        // be referenced by all pairs of triangles in the faces array
        final float[] points = {
            -75.0f,  75.0f, 0.0f,
             75.0f,  75.0f, 0.0f,
             75.0f, -75.0f, 0.0f,
            -75.0f, -75.0f, 0.0f
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
        mesh.getTexCoords().setAll(TEX_COORDS);
        int[] array = faces.stream().mapToInt(i -> i).toArray();
        mesh.getFaces().setAll(array);

        return new MeshView(mesh);
    }

    /// Creates a mesh of co-planar quads in a grid from a single quad so that all quads are visible.
    static MeshView createSpreadQuads(int quadNum) {
        int gridCols = (int) Math.sqrt(quadNum);
        // the larger the grid, the smaller each cell needs to be
        float cellSize = GRID_SIZE / gridCols;
        float gap = cellSize * GAP_RATIO;

        float[] points = new float[quadNum * COORDS_PER_QUAD];
        int[] faces = new int[quadNum * COORDS_PER_FACE];

        // create the grid starting from (0, 0) and center it in front of the camera at the end
        for (int i = 0; i < quadNum; i++) {
            int col = i % gridCols;
            int row = i / gridCols;
            float centerX = col * (cellSize + gap);
            float centerY = row * (cellSize + gap);

            // 1 - 4
            // |   |
            // 2 - 3
            int p = i * COORDS_PER_QUAD;
            points[p + 0] = centerX - cellSize / 2;
            points[p + 1] = centerY - cellSize / 2;
            points[p + 2] = 0;

            points[p + 3] = centerX - cellSize / 2;
            points[p + 4] = centerY + cellSize / 2;
            points[p + 5] = 0;

            points[p + 6] = centerX + cellSize / 2;
            points[p + 7] = centerY + cellSize / 2;
            points[p + 8] = 0;

            points[p + 9] = centerX + cellSize / 2;
            points[p + 10] = centerY - cellSize / 2;
            points[p + 11] = 0;

            int f = i * COORDS_PER_FACE;
            int pointShift = i * 4;
            faces[f + 0] = pointShift;
            faces[f + 1] = 0;

            faces[f + 2] = pointShift + 1;
            faces[f + 3] = 1;

            faces[f + 4] = pointShift + 2;
            faces[f + 5] = 2;

            faces[f + 6] = pointShift;
            faces[f + 7] = 0;

            faces[f + 8] = pointShift + 2;
            faces[f + 9] = 2;

            faces[f + 10] = pointShift + 3;
            faces[f + 11] = 3;
        }

        var mesh = new TriangleMesh();
        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(TEX_COORDS);
        mesh.getFaces().setAll(faces);

        var meshView = new MeshView(mesh);
        double width = meshView.getBoundsInLocal().getWidth();
        double height = meshView.getBoundsInLocal().getHeight();
        meshView.setTranslateX(-width / 2 + cellSize / 2);
        meshView.setTranslateY(-height / 2 + cellSize / 2);
        return meshView;
    }

    ///Creates a grid of co-planar meshes from a single quad so that all meshes are visible.
    static Group createSpreadMeshes(int meshViewNum) {
        int gridCols = (int) Math.sqrt(meshViewNum);
        float cellSize = GRID_SIZE / gridCols;
        float gap = cellSize * GAP_RATIO;

        // 1 - 4
        // |   |
        // 2 - 3
        float[] points = new float[COORDS_PER_QUAD];
        points[0] = -cellSize / 2;
        points[1] = -cellSize / 2;
        points[2] = 0;

        points[3] = -cellSize / 2;
        points[4] = cellSize / 2;
        points[5] = 0;

        points[6] = cellSize / 2;
        points[7] = cellSize / 2;
        points[8] = 0;

        points[9] = cellSize / 2;
        points[10] = -cellSize / 2;
        points[11] = 0;

        var face = List.of(
            0, 0, 1, 1, 2, 2,
            0, 0, 2, 2, 3, 3
        );

        var mesh = new TriangleMesh();
        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(TEX_COORDS);
        mesh.getFaces().setAll(face.stream().mapToInt(i -> i).toArray());

        var group = new Group();
        for (int i = 0; i < meshViewNum; i++) {
            var meshView = new MeshView(mesh);
            int col = i % gridCols;
            int row = i / gridCols;
            float centerX = col * (cellSize + gap);
            float centerY = row * (cellSize + gap);
            meshView.setTranslateX(centerX);
            meshView.setTranslateY(centerY);
            group.getChildren().add(meshView);
        }

        double width = group.getBoundsInLocal().getWidth();
        double height = group.getBoundsInLocal().getHeight();
        group.setTranslateX(-width / 2 + cellSize / 2);
        group.setTranslateY(-height / 2 + cellSize / 2);
        return group;
    }
}
