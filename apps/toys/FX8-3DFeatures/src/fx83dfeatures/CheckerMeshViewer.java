/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

public class CheckerMeshViewer extends Application {

    Group root;
    PointLight pointLight;
    MeshView meshView;
    TriangleMesh triMesh;
    PhongMaterial material;
    RotateTransition rotTrans;

    int divX = 100;
    int divY = 100;
    boolean smooth = false;
    int smoothGroups[];
    float translateValue = 0.0f;
    float rotateAngle = 0.0f;
    float funcValue = 0.0f;
    float resolution = 0.1f;
    boolean texture = true;
    boolean textureSwitch = false;
    WritableImage diffuseMap;
    
    final static float meshScale = 20;
    final static float minX = -10;
    final static float minY = -10;
    final static float maxX = 10;
    final static float maxY = 10;

    double getSinDivX(double x, double y) {
        double r = Math.sqrt(x*x + y*y);
        return funcValue * (r == 0 ? 1 : Math.sin(r) / r);
    }

    void computeMesh(int subDivX, int subDivY, float scale) {
         final int pointSize = 3;
        int numDivX = subDivX + 1;
        float[] points = triMesh.getPoints().toArray(null);

        // Initial points and texCoords
        for (int y = 0; y <= subDivY; y++) {
            float dy = (float) y / subDivY;
            double fy = (1 - dy) * minY + dy * maxY;
//            System.err.println("dy = " + dy + ", fy = " + fy);
            for (int x = 0; x <= subDivX; x++) {
                float dx = (float) x / subDivX;
                double fx = (1 - dx) * minX + dx * maxX;
//                System.err.println("dx = " + dx + ", fx = " + fx);
                int index = y * numDivX * pointSize + (x * pointSize);
//                System.err.println("point index = " + index);
                points[index] = (float) fx * scale;
                points[index + 1] = (float) fy * scale;
                points[index + 2] = (float) getSinDivX(fx, fy) * scale;
//                System.err.println("points[" + (index + 2) + " = " + points[index + 2]);
            }
        }

        triMesh.getPoints().set(0, points, 0, points.length);
    }

    TriangleMesh buildTriangleMesh(int subDivX, int subDivY,
            float scale) {

//        System.err.println("subDivX = " + subDivX + ", subDivY = " + subDivY);

        final int pointSize = 3;
        final int texCoordSize = 2;
        final int faceSize = 6; // 3 point indices and 3 texCoord indices per triangle
        int numDivX = subDivX + 1;
        int numVerts = (subDivY + 1) * numDivX;
        float points[] = new float[numVerts * pointSize];
        float texCoords[] = new float[numVerts * texCoordSize];
        int faceCount = subDivX * subDivY * 2;
        int faces[] = new int[ faceCount * faceSize]; 

        // Create points and texCoords
        for (int y = 0; y <= subDivY; y++) {
            float dy = (float) y / subDivY;
            double fy = (1 - dy) * minY + dy * maxY;
//            System.err.println("dy = " + dy + ", fy = " + fy);
            for (int x = 0; x <= subDivX; x++) {
                float dx = (float) x / subDivX;
                double fx = (1 - dx) * minX + dx * maxX;
//                System.err.println("dx = " + dx + ", fx = " + fx);
                int index = y * numDivX * pointSize + (x * pointSize);
//                System.err.println("point index = " + index);
                points[index] = (float) fx * scale;
                points[index + 1] = (float) fy * scale;
                points[index + 2] = (float) getSinDivX(fx, fy) * scale;
                index = y * numDivX * texCoordSize + (x * texCoordSize);
//                System.err.println("texCoord index = " + index);
                texCoords[index] = dx * subDivX/8;
                texCoords[index + 1] = dy * subDivY/8;
            }
        }

        // Create faces
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

                int index = (y * subDivX * faceSize + (x * faceSize)) * 2;
//                System.err.println("face  0 index = " + index);
                faces[index + 0] = p00;
                faces[index + 1] = tc00;
                faces[index + 2] = p10;
                faces[index + 3] = tc10;
                faces[index + 4] = p11;
                faces[index + 5] = tc11;
                index += faceSize;
//                System.err.println("face  1 index = " + index);
                faces[index + 0] = p11;
                faces[index + 1] = tc11;
                faces[index + 2] = p01;
                faces[index + 3] = tc01;
                faces[index + 4] = p00;
                faces[index + 5] = tc00;
            }
        }
//        for(int i = 0; i < points.length; i++) {
//            System.err.println("points[" + i + "] = " + points[i]);
//        }
//        for(int i = 0; i < texCoords.length; i++) {
//            System.err.println("texCoords[" + i + "] = " + texCoords[i]);
//        }
//        for(int i = 0; i < faces.length; i++) {
//            System.err.println("faces[" + i + "] = " + faces[i]);
//        }
        TriangleMesh triangleMesh = new TriangleMesh();
        triangleMesh.getPoints().setAll(points);
        triangleMesh.getTexCoords().setAll(texCoords);
        triangleMesh.getFaces().setAll(faces);
        smooth = false;
        smoothGroups = new int[divX * divY * 2];
        triangleMesh.getFaceSmoothingGroups().setAll(smoothGroups);
        return triangleMesh;
    }

    private Scene buildScene(int width, int height, boolean depthBuffer) {

        triMesh = buildTriangleMesh(divX, divY, meshScale);
        material = new PhongMaterial();
        material.setDiffuseMap(diffuseMap);
        material.setDiffuseColor(Color.WHITE);
        meshView = new MeshView(triMesh);
        meshView.setMaterial(material);

        //Set Wireframe mode
        meshView.setDrawMode(DrawMode.FILL);
        meshView.setCullFace(CullFace.BACK);

        final Group grp1 = new Group(meshView);
        grp1.setRotate(0);
        grp1.setRotationAxis(Rotate.X_AXIS);
        Group grp2 = new Group(grp1);
        grp2.setRotate(0);
        grp2.setRotationAxis(Rotate.X_AXIS);
        Group grp3 = new Group(grp2);
        grp3.setTranslateX(400);
        grp3.setTranslateY(400);
        grp3.setTranslateZ(10);

        pointLight = new PointLight(Color.ANTIQUEWHITE);
        pointLight.setTranslateX(300);
        pointLight.setTranslateY(-50);
        pointLight.setTranslateZ(-1000);

        // Animate meshview
        rotTrans = new RotateTransition(Duration.seconds(30), meshView);
        rotTrans.setAutoReverse(true);
        rotTrans.setAxis(new Point3D(0, 1, 0).normalize());
        rotTrans.setInterpolator(Interpolator.EASE_BOTH);
        rotTrans.setCycleCount(Timeline.INDEFINITE);
        rotTrans.setByAngle(90);
        rotTrans.play();
        root = new Group(grp3, pointLight);
        Scene scene = new Scene(root, width, height, depthBuffer);
        scene.setOnKeyTyped(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) {
                switch (e.getCharacter()) {
                    case "[":
                        funcValue -= resolution;
                        if (funcValue < -20.0f) {
                            funcValue = -20.0f;
                        }
                        computeMesh(divX, divY, meshScale);
                        break;
                    case "]":
                        funcValue += resolution;
                        if (funcValue > 20.0f) {
                            funcValue = 20.0f;
                        }
                        computeMesh(divX, divY, meshScale);
                        break;
                    case "p":
                        funcValue = 0;
                        computeMesh(divX, divY, meshScale);
                        break;
                    case "i":
                        System.err.print("i ");
                        if (!textureSwitch) {
                            texture = texture ? false : true;
                        } else {
                            textureSwitch = false;
                        }
                        if (texture) {
                            material.setDiffuseMap(diffuseMap);
                            material.setDiffuseColor(Color.WHITE);
                        } else {
                            material.setDiffuseMap(null);
                            material.setBumpMap(null);
                            material.setDiffuseColor(Color.LIGHTGRAY);
                        }
                        break;
                    case "s":
                        smooth = !smooth;
                        if (!smooth) {
                            Arrays.fill(smoothGroups, 0);
                        } else {
                            for (int i = 0; i < smoothGroups.length; i++) {
                                smoothGroups[i] = i % 32;
//                                System.err.println("XXX smoothGroups[" + i + "] = " + smoothGroups[i]);
                            }
                        }
                        triMesh.getFaceSmoothingGroups().setAll(smoothGroups);
                        break;
                    case "k":
                         System.err.print("k ");
                         if ((texture) || (!textureSwitch)) {
                            material.setDiffuseMap(diffuseMap);
                            material.setBumpMap(null);
                            material.setDiffuseColor(Color.WHITE);
                            texture = true;
                            textureSwitch = true;
                        } else {
                            material.setDiffuseMap(null);
                            material.setBumpMap(null);
                            material.setDiffuseColor(Color.LIGHTGRAY);
                        }
                        break;
                    case "l":
                        System.err.print("l ");
                        boolean wireframe = meshView.getDrawMode() == DrawMode.LINE;
                        meshView.setDrawMode(wireframe ? DrawMode.FILL : DrawMode.LINE);
                        break;
                    case "<":
                        grp1.setRotate(rotateAngle -= (resolution * 5));
                        break;
                    case ">":
                        grp1.setRotate(rotateAngle += (resolution * 5));
                        break;
                    case "/":
                        grp3.setTranslateZ(translateValue += 5);
                        break;
                    case "?":
                        grp3.setTranslateZ(translateValue -= 5);
                        break;
                    case "X":
                        grp1.setRotationAxis(Rotate.X_AXIS);
                        break;
                    case "Y":
                        grp1.setRotationAxis(Rotate.Y_AXIS);
                        break;
                    case "Z":
                        grp1.setRotationAxis(Rotate.Z_AXIS);
                        break;
                    case "P":
                        rotateAngle = 0;
                        grp1.setRotate(rotateAngle);
                    case "1":
                        System.err.print("1 ");
                        divX = 5;
                        divY = 5;
                        smooth = true;
                        smoothGroups = new int[divX * divY * 2];
                        rotateAngle = 0.0f;
                        funcValue = 0.0f;
                        triMesh = buildTriangleMesh(divX, divY, meshScale);
                        meshView.setMesh(triMesh);
                        break;
                    case "2":
                        System.err.print("2 ");
                        divX = 70;
                        divY = 70;
                        smooth = true;
                        smoothGroups = new int[divX * divY * 2];
                        rotateAngle = 0.0f;
                        funcValue = 0.0f;
                        triMesh = buildTriangleMesh(divX, divY, meshScale);
                        meshView.setMesh(triMesh);
                        break;
                    case " ":
                        root.getChildren().add(new Button("Button"));
                        break;
                    case "\\":
                        if (rotTrans.getStatus() == Timeline.Status.RUNNING) {
                            rotTrans.pause();
                        } else {
                            rotTrans.play();
                        }
                        break;
                }
            }
        });
        return scene;
    }

    private PerspectiveCamera addCamera(Scene scene) {
        PerspectiveCamera perspectiveCamera = new PerspectiveCamera();
        scene.setCamera(perspectiveCamera);
        return perspectiveCamera;
    }

    private void setArgb(PixelWriter pWriter,
            int startX, int endX, int startY, int endY, int value) {
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                pWriter.setArgb(x, y, value);
            }
        }
    }

    @Override
    public void start(Stage primaryStage) {
        diffuseMap = new WritableImage(16,16);
        PixelWriter pWriter = diffuseMap.getPixelWriter();
        setArgb(pWriter, 0, 8, 0, 8, 0Xff000000);
        setArgb(pWriter, 8, 16, 0, 8, 0Xffffffff);
        setArgb(pWriter, 0, 8, 8, 16, 0Xffffffff);
        setArgb(pWriter, 8, 16, 8, 16, 0Xff000000);

        Scene scene = buildScene(800, 800, true);
        scene.setFill(Color.rgb(10, 10, 40));
        addCamera(scene);
        primaryStage.setTitle("Checker Mesh Viewer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
