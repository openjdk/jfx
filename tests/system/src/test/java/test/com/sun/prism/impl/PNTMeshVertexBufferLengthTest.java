/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.prism.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.TriangleMeshShim;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.javafx.sg.prism.NGTriangleMesh;
import com.sun.javafx.sg.prism.NGTriangleMeshShim;
import com.sun.prism.impl.BaseMesh;
import com.sun.prism.impl.BaseMeshShim;

import test.util.Util;

/**
 * @test @bug 8178804
 * @summary Excessive memory consumption in TriangleMesh/MeshView
 */
public class PNTMeshVertexBufferLengthTest {

    // Sleep time showing/hiding window in milliseconds
    private static final int SLEEP_TIME = 1000;

    // Size of a vertex
    private static final int VERTEX_SIZE = 9;

    private static final float meshScale = 15;
    private static final float minX = -10;
    private static final float minY = -10;
    private static final float maxX = 10;
    private static final float maxY = 10;
    private static final float funcValue = -10.0f;

    private static final Vec3f v1 = new Vec3f();
    private static final Vec3f v2 = new Vec3f();

    private static void computeNormal(Vec3f pa, Vec3f pb, Vec3f pc, Vec3f normal) {
        // compute Normal |(v1-v0)X(v2-v0)|
        v1.sub(pb, pa);
        v2.sub(pc, pa);
        normal.cross(v1, v2);
        normal.normalize();
    }

    private static double getSinDivX(double x, double y) {
        double r = Math.sqrt(x * x + y * y);
        return funcValue * (r == 0 ? 1 : Math.sin(r) / r);
    }

    private static void buildTriangleMesh(MeshView meshView,
            int subDivX, int subDivY, float scale) {

        final int pointSize = 3;
        final int normalSize = 3;
        final int texCoordSize = 2;
        final int faceSize = 9; // 3 point indices, 3 normal indices and 3 texCoord indices per triangle
        int numDivX = subDivX + 1;
        int numVerts = (subDivY + 1) * numDivX;
        float points[] = new float[numVerts * pointSize];
        float texCoords[] = new float[numVerts * texCoordSize];
        int faceCount = subDivX * subDivY * 2;
        float normals[] = new float[faceCount * normalSize];
        int faces[] = new int[faceCount * faceSize];

        // Create points and texCoords
        for (int y = 0; y <= subDivY; y++) {
            float dy = (float) y / subDivY;
            double fy = (1 - dy) * minY + dy * maxY;
            for (int x = 0; x <= subDivX; x++) {
                float dx = (float) x / subDivX;
                double fx = (1 - dx) * minX + dx * maxX;
                int index = y * numDivX * pointSize + (x * pointSize);
                points[index] = (float) fx * scale;
                points[index + 1] = (float) fy * scale;
                points[index + 2] = (float) getSinDivX(fx, fy) * scale;
                index = y * numDivX * texCoordSize + (x * texCoordSize);
                texCoords[index] = dx;
                texCoords[index + 1] = dy;
            }
        }

        // Initial faces and normals
        int normalCount = 0;
        Vec3f[] triPoints = new Vec3f[3];
        triPoints[0] = new Vec3f();
        triPoints[1] = new Vec3f();
        triPoints[2] = new Vec3f();
        Vec3f normal = new Vec3f();
        for (int y = 0; y < subDivY; y++) {
            for (int x = 0; x < subDivX; x++) {
                int p00 = y * numDivX + x;
                int p01 = p00 + 1;
                int p10 = p00 + numDivX;
                int p11 = p10 + 1;
                int tc00 = y * numDivX + x;
                int tc01 = tc00 + 1;
                int tc10 = tc00 + numDivX;
                int tc11 = tc10 + 1;

                int ii = p00 * 3;
                triPoints[0].x = points[ii];
                triPoints[0].y = points[ii + 1];
                triPoints[0].z = points[ii + 2];
                ii = p10 * 3;
                triPoints[1].x = points[ii];
                triPoints[1].y = points[ii + 1];
                triPoints[1].z = points[ii + 2];
                ii = p11 * 3;
                triPoints[2].x = points[ii];
                triPoints[2].y = points[ii + 1];
                triPoints[2].z = points[ii + 2];
                computeNormal(triPoints[0], triPoints[1], triPoints[2], normal);

                int normalIndex = normalCount * normalSize;
                normals[normalIndex] = normal.x; //nx
                normals[normalIndex + 1] = normal.y; //ny
                normals[normalIndex + 2] = normal.z; //nz

                int index = (y * subDivX * faceSize + (x * faceSize)) * 2;
                faces[index + 0] = p00;
                faces[index + 1] = normalCount;
                faces[index + 2] = tc00;
                faces[index + 3] = p10;
                faces[index + 4] = normalCount;
                faces[index + 5] = tc10;
                faces[index + 6] = p11;
                faces[index + 7] = normalCount++;
                faces[index + 8] = tc11;
                index += faceSize;

                ii = p11 * 3;
                triPoints[0].x = points[ii];
                triPoints[0].y = points[ii + 1];
                triPoints[0].z = points[ii + 2];
                ii = p01 * 3;
                triPoints[1].x = points[ii];
                triPoints[1].y = points[ii + 1];
                triPoints[1].z = points[ii + 2];
                ii = p00 * 3;
                triPoints[2].x = points[ii];
                triPoints[2].y = points[ii + 1];
                triPoints[2].z = points[ii + 2];
                computeNormal(triPoints[0], triPoints[1], triPoints[2], normal);
                normalIndex = normalCount * normalSize;
                normals[normalIndex] = normal.x; //nx
                normals[normalIndex + 1] = normal.y; //ny
                normals[normalIndex + 2] = normal.z; //nz

                faces[index + 0] = p11;
                faces[index + 1] = normalCount;
                faces[index + 2] = tc11;
                faces[index + 3] = p01;
                faces[index + 4] = normalCount;
                faces[index + 5] = tc01;
                faces[index + 6] = p00;
                faces[index + 7] = normalCount++;
                faces[index + 8] = tc00;
            }
        }


        TriangleMesh triangleMesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        triangleMesh.getPoints().setAll(points);
        triangleMesh.getNormals().setAll(normals);
        triangleMesh.getTexCoords().setAll(texCoords);
        triangleMesh.getFaces().setAll(faces);
        meshView.setMesh(triangleMesh);
    }

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Singleton Application instance
    static MyApp myApp;

    private CountDownLatch latch = new CountDownLatch(1);

    // Application class. An instance is created and initialized before running
    // the first test, and it lives through the execution of all tests.
    public static class MyApp extends Application {

        Stage primaryStage = null;
        MeshView meshView;

        @Override
        public void init() {
            PNTMeshVertexBufferLengthTest.myApp = this;
        }

        @Override
        public void start(Stage primaryStage) throws Exception {
            primaryStage.setTitle("PNTMeshVertexBufferLengthTest");
            TriangleMesh triangleMesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
            meshView = new MeshView(triangleMesh);
            Group rotateGrp = new Group(meshView);
            rotateGrp.setRotate(-30);
            rotateGrp.setRotationAxis(Rotate.X_AXIS);
            Group translateGrp = new Group(rotateGrp);
            translateGrp.setTranslateX(200);
            translateGrp.setTranslateY(200);
            translateGrp.setTranslateZ(100);
            Group root = new Group(translateGrp);

            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setX(0);
            primaryStage.setY(0);
            primaryStage.setWidth(400);
            primaryStage.setHeight(400);
            PerspectiveCamera perspectiveCamera = new PerspectiveCamera();
            scene.setCamera(perspectiveCamera);
            primaryStage.show();
            this.primaryStage = primaryStage;
            launchLatch.countDown();
        }
    }

    @BeforeClass
    public static void setupOnce() {
        Util.launch(launchLatch, MyApp.class);
        assertEquals(0, launchLatch.getCount());
    }

    @AfterClass
    public static void teardownOnce() {
        Util.shutdown();
    }

    @Before
    public void setupEach() {
        assumeTrue(Platform.isSupported(ConditionalFeature.SCENE3D));
    }

    // ========================== TEST CASES ==========================
    @Test(timeout = 15000)
    public void testMeshWithZeroDiv() throws InterruptedException {
        Util.runAndWait(() -> {
            Scene scene = myApp.primaryStage.getScene();
            buildTriangleMesh(myApp.meshView, 0, 0, meshScale);
        });
        Thread.sleep(SLEEP_TIME);

        NGTriangleMesh ngTriMesh = TriangleMeshShim.getNGMesh(myApp.meshView.getMesh());
        assertNotNull(ngTriMesh);
        BaseMesh baseMesh = NGTriangleMeshShim.test_getMesh(ngTriMesh);
        assertNotNull(baseMesh);
        // empty mesh (0 vertices)
        assertEquals(0, BaseMeshShim.test_getNumberOfVertices(baseMesh));
        assertTrue(BaseMeshShim.test_isVertexBufferNull(baseMesh));
    }

    @Test(timeout = 15000)
    public void testMeshWithOneDiv() throws InterruptedException {
        Util.runAndWait(() -> {
            Scene scene = myApp.primaryStage.getScene();
            buildTriangleMesh(myApp.meshView, 1, 1, meshScale);
        });
        Thread.sleep(SLEEP_TIME);

        NGTriangleMesh ngTriMesh = TriangleMeshShim.getNGMesh(myApp.meshView.getMesh());
        assertNotNull(ngTriMesh);
        BaseMesh baseMesh = NGTriangleMeshShim.test_getMesh(ngTriMesh);
        assertNotNull(baseMesh);
        // mesh with 6 vertices (2 triangles)
        assertEquals(6, BaseMeshShim.test_getNumberOfVertices(baseMesh));
        // vertexBuffer started with 4 vertices and grew by 6 (since 12.5% or 1/8th
        // of 4 is  less than 6). Size of vertex is 9 floats (10 * VERTEX_SIZE = 90)
        assertEquals(10 * VERTEX_SIZE, BaseMeshShim.test_getVertexBufferLength(baseMesh));
    }

    @Test(timeout = 15000)
    public void testMeshWithTwoDiv() throws InterruptedException {
        Util.runAndWait(() -> {
            Scene scene = myApp.primaryStage.getScene();
            buildTriangleMesh(myApp.meshView, 2, 2, meshScale);
        });
        Thread.sleep(SLEEP_TIME);

        NGTriangleMesh ngTriMesh = TriangleMeshShim.getNGMesh(myApp.meshView.getMesh());
        assertNotNull(ngTriMesh);
        BaseMesh baseMesh = NGTriangleMeshShim.test_getMesh(ngTriMesh);
        assertNotNull(baseMesh);
        // mesh with 24 vertices (8 triangles)
        assertEquals(24, BaseMeshShim.test_getNumberOfVertices(baseMesh));
        // vertexBuffer started with 9 vertices and grew by 6, 3 times, to a
        // capacity of 27 vertices (27 * VERTEX_SIZE = 243)
        assertEquals(27 * VERTEX_SIZE, BaseMeshShim.test_getVertexBufferLength(baseMesh));
    }

    @Test(timeout = 15000)
    public void testMeshWithThreeDiv() throws InterruptedException {
        Util.runAndWait(() -> {
            Scene scene = myApp.primaryStage.getScene();
            buildTriangleMesh(myApp.meshView, 7, 7, meshScale);
        });
        Thread.sleep(SLEEP_TIME);

        NGTriangleMesh ngTriMesh = TriangleMeshShim.getNGMesh(myApp.meshView.getMesh());
        assertNotNull(ngTriMesh);
        BaseMesh baseMesh = NGTriangleMeshShim.test_getMesh(ngTriMesh);
        assertNotNull(baseMesh);
        // mesh with 294 vertices (98 triangles)
        assertEquals(294, BaseMeshShim.test_getNumberOfVertices(baseMesh));
        // vertexBuffer started with 64 vertices and grew by 6 the first time
        // then crossed over to 12.5% growth rate at each subsequence increase
        // to a capacity of 325 vertices (325 * VERTEX_SIZE = 2925)
        assertEquals(325 * VERTEX_SIZE, BaseMeshShim.test_getVertexBufferLength(baseMesh));
    }

    @Test(timeout = 15000)
    public void testMeshWithFiveDiv() throws InterruptedException {
        Util.runAndWait(() -> {
            Scene scene = myApp.primaryStage.getScene();
            buildTriangleMesh(myApp.meshView, 50, 50, meshScale);
        });
        Thread.sleep(SLEEP_TIME);

        NGTriangleMesh ngTriMesh = TriangleMeshShim.getNGMesh(myApp.meshView.getMesh());
        assertNotNull(ngTriMesh);
        BaseMesh baseMesh = NGTriangleMeshShim.test_getMesh(ngTriMesh);
        assertNotNull(baseMesh);
        // mesh with 15000 vertices (5000 triangles)
        assertEquals(15000, BaseMeshShim.test_getNumberOfVertices(baseMesh));
        // vertexBuffer started with 2601 vertices and grew at 12.5% growth rate
        // at each subsequence increase to a capacity of 15201 vertices
        // (15201 * VERTEX_SIZE = 136809)
        assertEquals(15201 * VERTEX_SIZE, BaseMeshShim.test_getVertexBufferLength(baseMesh));
    }

}
