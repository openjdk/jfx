/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.shape.meshmanagercacheleaktest;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

import static test.javafx.scene.shape.meshmanagercacheleaktest.Constants.*;

/*
 * Test application launched by MeshManagerCacheLeakTest with -Xmx16m.
 * Test steps:
 * 1. Create scene with a container.
 * 2. Reserve maximum memory of available memory.
 * 3. Add and remove different sized shapes to scene.
 * 4. Verify that, no OOM occurs.
 */
public class MeshManagerCacheLeakApp {

    static String shapeType;
    static int numShapes;
    CountDownLatch latch;
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Group container;
    static volatile Stage stage;

    public static class TestApp extends Application {
        @Override
        public void start(Stage pStage) {
            // 1. Create scene with a container.
            stage = pStage;
            HBox root = new HBox();
            container = new Group();
            root.getChildren().add(container);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e ->
                    Platform.runLater(startupLatch::countDown));
            stage.show();
        }
    }

    public void testOOM() {
        System.gc();
        System.runFinalization();

        try {
            // 2. Reserve maximum of available memory.
            byte[] mem = null;
            if (shapeType.equals("Sphere")) {
                mem = new byte[1024 * 1024 * 8];
            } else if (shapeType.equals("Cylinder")) {
                mem = new byte[1024 * 1024 * 8];
            } else if (shapeType.equals("Box")) {
                mem = new byte[1024 * 1024 * 11];
            }
            float radius = 20.0f;
            float height = 20;
            int sphereDivisions = 70;
            int cylinderDivisions = 512;
            int boxWHD = 300;

            for (int i = 0; i < numShapes; ++i) {
                // 3. Add and remove different sized shapes to scene.
                Shape3D shape = null;
                if (shapeType.equals("Sphere")) {
                    shape = new Sphere(radius++, sphereDivisions);
                } else if (shapeType.equals("Cylinder")) {
                    shape = new Cylinder(radius++, height, cylinderDivisions);
                } else if (shapeType.equals("Box")) {
                    shape = new Box(boxWHD++, boxWHD, boxWHD);
                }

                latch = new CountDownLatch(1);
                Shape3D shp = shape;
                Platform.runLater(() -> {
                    try {
                        container.getChildren().add(shp);
                        latch.countDown();
                    } catch (OutOfMemoryError e) {
                        System.exit(ERROR_OOM);
                    } catch (Exception e) {
                        System.exit(ERROR_OOM);
                    }
                });
                waitForLatch(latch, 5, -1);

                // The TriangleMesh for the added shape
                // is created on next pulse, hence the sleep
                // for approximately 2 pulse duration.
                Thread.sleep(35); // ~((1000 / 60) * 2)

                latch = new CountDownLatch(1);
                Platform.runLater(() -> {
                    container.getChildren().clear();
                    latch.countDown();
                });
                waitForLatch(latch, 5, -1);
            }
        } catch (OutOfMemoryError e) {
            System.exit(ERROR_OOM);
        } catch (Exception e) {
            System.exit(ERROR_OOM);
        }

        // 4. Verify that, no OOM occurs.
        System.exit(ERROR_NONE);
    }

    public void waitForLatch(CountDownLatch cdLatch, int seconds, int error) {
        try {
            if (!cdLatch.await(seconds, TimeUnit.SECONDS)) {
                System.exit(error);
            }
        } catch (Exception ex) {
            System.exit(error);
        }
    }

    public static void main(String[] args) {
        shapeType = args[0];
        numShapes = Integer.parseInt(args[1]);

        MeshManagerCacheLeakApp test = new MeshManagerCacheLeakApp();
        new Thread(() -> Application.launch(TestApp.class, (String[])null)).start();
        test.waitForLatch(startupLatch, 10, ERROR_LAUNCH);
        test.testOOM();
    }
}
