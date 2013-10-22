/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.cubeGame;


import javafx.geometry.Point3D;
import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.HashMap;
import java.util.Map;


/**
 * Creates 3D box of given size with beveled edges.
 */
public class FancyBox3D extends MeshView {
//    public static final Image DIFFUSE_MAP = new Image(MagicCube.class.getResourceAsStream("/images/RubixCubeTexture.png"));
    public static final Image DIFFUSE_MAP = new Image(MagicCube.class.getResourceAsStream("/images/RubixCubeTexture-color.png"));
    public static final Image BUMP_MAP = new Image(MagicCube.class.getResourceAsStream("/images/RubixCubeTexture-normal-map.png"));
    private TriangleMesh mesh;

    private static final float ONE_PIXEL = 1 / 1024f;
    private static final float T0 = 0;
    private static final float T1 = 1 / 3f;
    private static final float SIDE_SIZE = T1 - ONE_PIXEL * 2;
    private static final float PART_SIZE = SIDE_SIZE / 3;
    private static final float T2 = 2 * T1;
    private static final float T3 = 1;
    
    /**
     * Creates 3D box of given size with beveled edges. Box bounds are (0, 0, 0)
     * and (xSize, ySize, zSize).
     * @param xSize size of box along X-axis
     * @param ySize size of box along Y-axis
     * @param zSize size of box along Z-axis
     * @param edge size occupied by bevel on each side
     */
    public FancyBox3D(float xSize, float ySize, float zSize, float edge) {
        final Point3D[] op = new Point3D[] {
            new Point3D(edge, edge, 0),
            new Point3D(xSize - edge, edge, 0),
            new Point3D(xSize, edge, edge),
            new Point3D(xSize, edge, zSize - edge),
            new Point3D(xSize - edge, edge, zSize),
            new Point3D(edge, edge, zSize),
            new Point3D(0, edge, zSize - edge),
            new Point3D(0, edge, edge),
        };
        Point3D[] ip = new Point3D[] {
            new Point3D(edge, 0, edge),
            new Point3D(xSize - edge, 0, edge),
            new Point3D(xSize - edge, 0, zSize - edge),
            new Point3D(edge, 0, zSize - edge),
        };
        double mid = ySize - edge;

        mesh = new TriangleMesh();
        mesh.getPoints().ensureCapacity(72);
        mesh.getTexCoords().ensureCapacity(32);
        mesh.getFaces().ensureCapacity(264);
        mesh.getFaceSmoothingGroups().resize(264 / mesh.getFaceElementSize());
        mesh.getTexCoords().addAll(
                T2 + ONE_PIXEL, T2 + ONE_PIXEL,
                T3 - ONE_PIXEL, T2 + ONE_PIXEL,
                T3 - ONE_PIXEL, T3 - ONE_PIXEL,
                T2 + ONE_PIXEL, T3 - ONE_PIXEL
                );
        mesh.getFaces().addAll(newRectangleFace(ip[0], ip[1], ip[2], ip[3]));
        mesh.getFaces().addAll(newRectangleFace(
                atY(ip[0], ySize), atY(ip[3], ySize), atY(ip[2], ySize), atY(ip[1], ySize)));
        for(int i = 0; i < 8; i++) {
            mesh.getFaces().addAll(newRectangleFace(
                    op[i], atY(op[i], mid), atY(op[(i + 1) % 8], mid), op[(i + 1) % 8]));
            if (i % 2 == 0) {
                mesh.getFaces().addAll(newRectangleFace(
                        atY(op[i], mid),
                        atY(ip[i / 2], ySize),
                        atY(ip[(i / 2 + 1) % 4], ySize),
                        atY(op[(i + 1) % 8], mid)));
                mesh.getFaces().addAll(newRectangleFace(
                        op[i], op[(i + 1) % 8], ip[(i / 2 + 1) % 4], ip[i / 2]));
            } else {
                mesh.getFaces().addAll(newTriangleFace(
                        atY(op[i], mid),
                        atY(ip[(i / 2 + 1) % 4], ySize),
                        atY(op[(i + 1) % 8], mid)));
                mesh.getFaces().addAll(newTriangleFace(
                        op[i], op[(i + 1) % 8], ip[(i / 2 + 1) % 4]));
            }
        }
        setMesh(mesh);
        PhongMaterial phongMaterial = new PhongMaterial();
        phongMaterial.setDiffuseMap(DIFFUSE_MAP);
//        phongMaterial.setDiffuseMap(new Image(MagicCube.class.getResourceAsStream("RubixCubeTexture-color.png")));
        phongMaterial.setBumpMap(BUMP_MAP);
        setMaterial(phongMaterial);
        this.setPickOnBounds(true);
    }

    public static Point3D atY(Point3D p, double y) {
        return new Point3D(
                p.getX(),
                y,
                p.getZ());
    }

    Map<Point3D, Integer> pIndexes = new HashMap<>();

    private int[] newRectangleFace(Point3D p0, Point3D p1, Point3D p2, Point3D p3) {
        int p0i = getPointIndex(p0);
        int p1i = getPointIndex(p1);
        int p2i = getPointIndex(p2);
        int p3i = getPointIndex(p3);
        return new int[] {
            p0i, 0, p1i, 1, p3i, 3,
            p1i, 1, p2i, 2, p3i, 3,
        };
    }

    private int[] newTriangleFace(Point3D p0, Point3D p1, Point3D p2) {
        int p0i = getPointIndex(p0);
        int p1i = getPointIndex(p1);
        int p2i = getPointIndex(p2);
        return new int[] {
            p0i, 0, p1i, 0, p2i, 0,
        };
    }

    private int getPointIndex(Point3D point) {
        Integer index = pIndexes.get(point);
        if (index == null) {
            index = mesh.getPoints().size() / mesh.getPointElementSize();
            mesh.getPoints().addAll((float) point.getX(), (float) point.getY(), (float) point.getZ());
            pIndexes.put(point, index);
        }
        return index;
    }

    public void setFace(Face face, int x, int y, int px, int py) {
        int faceIndex = 0;
        if (face.ordinal() <= Face.BACK.ordinal()) {
            faceIndex = face.ordinal() * 2 * mesh.getFaceElementSize();
        } else {
            faceIndex = ((face.ordinal() - 2) / 6 * 10 + 4) * mesh.getFaceElementSize();
        }
        float leftX = T1 * x + ONE_PIXEL + (2 - py) * PART_SIZE;
        float topY = T1 * y + ONE_PIXEL + px * PART_SIZE;
        int baseTC = mesh.getTexCoords().size() / mesh.getTexCoordElementSize();
        mesh.getTexCoords().addAll(
                leftX + PART_SIZE, topY,
                leftX, topY,
                leftX, topY + PART_SIZE,
                leftX + PART_SIZE, topY + PART_SIZE
                );
        mesh.getFaces().set(faceIndex +  1, baseTC + 0);
        mesh.getFaces().set(faceIndex +  3, baseTC + 1);
        mesh.getFaces().set(faceIndex +  5, baseTC + 3);
        mesh.getFaces().set(faceIndex +  7, baseTC + 1);
        mesh.getFaces().set(faceIndex +  9, baseTC + 2);
        mesh.getFaces().set(faceIndex + 11, baseTC + 3);
    }
    
    /**
     * FancyBox3D faces
     */
    public static enum Face { 
            BOTTOM, TOP, 
            BACK, EDGE_BACK_TOP, EDGE_BACK_BOTTOM, 
            EDGE_RIGHT_BACK, EDGE_RIGHT_BACK_TOP, EDGE_RIGHT_BACK_BOTTOM,
            RIGHT, EDGE_RIGHT_TOP, EDGE_RIGHT_BOTTOM, 
            EDGE_FRONT_RIGHT, EDGE_FRONT_RIGHT_TOP, EDGE_FRONT_RIGHT_BOTTOM,
            FRONT, EDGE_FRONT_TOP, EDGE_FRONT_BOTTOM, 
            EDGE_LEFT_FRONT, EDGE_LEFT_FRONT_TOP, EDGE_LEFT_FRONT_BOTTOM,
            LEFT, EDGE_LEFT_TOP, EDGE_LEFT_BOTTOM, 
            EDGE_BACK_LEFT, EDGE_BACK_LEFT_TOP, EDGE_BACK_LEFT_BOTTOM
    }
}
