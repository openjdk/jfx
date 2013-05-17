/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.sg.prism.NGHelper;
import com.sun.javafx.sg.prism.NGPhongMaterial;
import com.sun.javafx.sg.prism.NGShape3D;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.es2.ES2Helper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;
import junit.framework.AssertionFailedError;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
//import util.Util;

import static org.junit.Assert.*;
//import static util.Util.TIMEOUT;

/**
 * Tests for snapshot.
 */
@Ignore("RT-29686")
public class PhongMaterialTest {

    private static int TIMEOUT = 5000;
    private static int SLEEP_TIME = 500;

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Singleton Application instance
    static MyApp myApp;

    private static Class pipelineClass;

    // Application class. An instance is created and initialized before running
    // the first test, and it lives through the execution of all tests.
    public static class MyApp extends Application {
        Stage primaryStage;
        Scene scene;

        @Override public void init() {
            PhongMaterialTest.myApp = this;
        }

        @Override public void start(Stage primaryStage) throws Exception {
            assertTrue(Platform.isFxApplicationThread());

            pipelineClass = GraphicsPipeline.getPipeline().getClass();
            assertNotNull(pipelineClass);
            System.err.println("GraphicsPipline = " + pipelineClass.getName());
            assertTrue(pipelineClass.getName().startsWith("com.sun.prism."));


            primaryStage.setTitle("Primary stage");
            Group root = new Group();
            scene = new Scene(root);
            scene.setFill(Color.LIGHTYELLOW);
            primaryStage.setScene(scene);
            primaryStage.setX(0);
            primaryStage.setY(0);
            primaryStage.setWidth(210);
            primaryStage.setHeight(180);
            assertFalse(primaryStage.isShowing());
            primaryStage.show();
            assertTrue(primaryStage.isShowing());

            this.primaryStage = primaryStage;
            launchLatch.countDown();


        }
    }

    @BeforeClass
    public static void setupOnce() {

        System.setProperty("prism.order", "es2");

        // Start the Application
        new Thread(new Runnable() {
            @Override public void run() {
                Application.launch(MyApp.class, (String[])null);
            }
        }).start();

        try {
            if (!launchLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for Application to launch");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }

        assertEquals(0, launchLatch.getCount());
    }

    @AfterClass
    public static void teardownOnce() {
        Platform.exit();
    }

    private Error error = null;
    private RuntimeException exception = null;

    private void runAndWait(final Runnable r) {
        PlatformImpl.runAndWait(new Runnable() {

            public void run() {
                try {
                    r.run();
                } catch (RuntimeException ex) {
                    exception = ex;
                } catch (Error err) {
                    error = err;
                }
            }

        });

        if (error != null) {
            throw error;
        }
        if (exception != null) {
            throw exception;
        }
    }

    private void sleep(long msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException ex) {
            throw new AssertionFailedError("Unexpected Interupt");
        }

    }

    PhongMaterial testMat;
    Shape3D shape;

    // ========================== TEST CASES ==========================

    // Verify that we cannot construct a Scene on a thread other than
    // the FX Application thread
    @Test
    public void testDefaultPhongMaterial() {


        runAndWait(new Runnable() {
            public void run() {
                testMat = new PhongMaterial();
                shape = new Sphere();
                shape.setMaterial(testMat);
                Group root = (Group) myApp.scene.getRoot();
                root.getChildren().add(shape);
            }
        });

        //TODO: should just wait for pulse
        sleep(SLEEP_TIME);

        runAndWait(new Runnable() {
            public void run() {
                NGShape3D peer = (NGShape3D)shape.impl_getPGNode();
                NGPhongMaterial phongMaterial = NGHelper.getMaterial(peer);
                com.sun.prism.PhongMaterial tmp = NGHelper.createMaterial(phongMaterial);
                if (com.sun.prism.es2.ES2Pipeline.class.equals(pipelineClass)) {
                    ES2Helper.checkMaterial(testMat, tmp);
                }

            }
        });

    }


}
