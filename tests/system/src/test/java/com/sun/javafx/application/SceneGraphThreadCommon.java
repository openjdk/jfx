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

package com.sun.javafx.application;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import junit.framework.AssertionFailedError;
import org.junit.AfterClass;
import util.Util;

import static org.junit.Assert.*;
import static util.Util.TIMEOUT;

/**
 * Unit tests for scene graph construction on a background thread
 */
public class SceneGraphThreadCommon {

    // Sleep time showing/hiding window in milliseconds
    private static final int SLEEP_TIME = 1000;

    // Latch indicating that the start method has been called
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Completion latch indicating that the launch method has returned
    private static final CountDownLatch doneLatch = new CountDownLatch(1);

    // Callable to create the node in question on the init thread
    static volatile Callable<Node> initCallable;

    // Singleton Application instance
    static MyApp myApp;

    // Application class. An instance is created and initialized before running
    // the first test, and it lives through the execution of all tests.
    public static class MyApp extends Application {

        Node content;
        Stage primaryStage;

        public MyApp() {
            Platform.setImplicitExit(false);
            assertTrue(Platform.isFxApplicationThread());
        }

        @Override public void init() throws Exception {
            assertFalse(Platform.isFxApplicationThread());
            SceneGraphThreadCommon.myApp = this;
            content = initCallable.call();
            assertNotNull(content);
        }

        @Override public void start(Stage primaryStage) throws Exception {
            assertTrue(Platform.isFxApplicationThread());
            primaryStage.setTitle("Primary stage");
            StackPane root = new StackPane(content);
            Scene scene = new Scene(root, 300, 200);
            assertFalse(primaryStage.isShowing());
            primaryStage.setScene(scene);
            primaryStage.show();
            assertTrue(primaryStage.isShowing());

            this.primaryStage = primaryStage;
            launchLatch.countDown();
        }
    }


    private void doTest(Callable<Node> callable) {
        // Start the Application using the specified Callable
        initCallable = callable;
        final Thread testThread = Thread.currentThread();
        final AtomicReference<Throwable> launchErr = new AtomicReference<>(null);
        new Thread(() -> {
            try {
                Application.launch(MyApp.class, (String[])null);
                doneLatch.countDown();
            } catch (Throwable t) {
                launchErr.set(t);
                testThread.interrupt();
            }
        }).start();

        try {
            if (!launchLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for Application to launch");
            }
        } catch (InterruptedException ex) {
            Throwable t = launchErr.get();
            if (t instanceof RuntimeException) {
                throw (RuntimeException)t;
            }
            else if (t instanceof Error) {
                throw (Error)t;
            } else {
                throw new RuntimeException(t);
            }
        }
        assertNotNull(myApp);
        assertNotNull(myApp.content);
        assertNotNull(myApp.primaryStage);
        Util.sleep(SLEEP_TIME);
        Util.runAndWait(() -> myApp.primaryStage.hide());
        Util.sleep(SLEEP_TIME);
        Platform.exit();
        try {
            if (!doneLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for Application to finish");
            }
        } catch (InterruptedException ex) {
            Throwable t = launchErr.get();
            if (t instanceof RuntimeException) {
                throw (RuntimeException)t;
            }
            else if (t instanceof Error) {
                throw (Error)t;
            } else {
                throw new RuntimeException(t);
            }
        }
    }

    // ========================== TEST CASES ==========================

    protected void doTestShape() {
       doTest(() -> new Circle(75, 75, 50));
    }

    protected void doTestContextMenu() {
       doTest(() -> {
            Label label = new Label("My Label");

            ContextMenu contextMenu = new ContextMenu();
            label.setContextMenu(contextMenu);
            return label;
       });
    }

    protected void doTestTooltip() {
       doTest(() -> {
            Button button = new Button("My Button");

            Tooltip tooltip = new Tooltip("My Tooltip");
            button.setTooltip(tooltip);
            return button;
       });
    }

    protected void doTestScene() {
       doTest(() -> {
           // Test creating a Scene and modifying the nodes in that scene
           Group root1 = new Group();
           Group root2 = new Group();
           Scene theScene = new Scene(root1);
           Rectangle theNode = new Rectangle(75, 50);
           root1.getChildren().add(theNode);
           root1.getChildren().clear();
           root2.getChildren().add(theNode);
           theScene.setRoot(root2);
           root2.getChildren().clear();
           return theNode;
       });
    }

}
