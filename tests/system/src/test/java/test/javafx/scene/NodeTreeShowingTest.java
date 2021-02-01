/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertTrue;

/**
 * Tests for regressions in performance of manipulating Nodes in a very large
 * Scene (see JDK-8252935).
 *
 * Specifically, this test was created for the Tree Showing property which
 * (before this fix) involved registering a listener to Scene and Window by
 * each Node, causing large listeners lists on the Window property in Scene
 * and the Showing property in Window.  The large lists of listeners would
 * cause noticeable performance issues in Scenes with 10-20k+ Nodes (which
 * for example can happen with a TableView with many visible small cells).
 *
 * The goal of this test is *NOT* to measure absolute performance, but to show
 * that adding and removing Nodes (which involves registering and unregistering
 * listeners) in a very large Scene does not take more than a particular
 * threshold of time.
 *
 * The selected threshold is larger than actual observed time.
 * It is not a benchmark value. It is good enough to catch the regression
 * in performance, if any.
 */

public class NodeTreeShowingTest {

    private static CountDownLatch startupLatch;
    private static Stage stage;
    private static BorderPane rootPane;

    public static class TestApp extends Application {

        @Override
        public void start(Stage primaryStage) throws Exception {
            stage = primaryStage;
            rootPane = new BorderPane();
            stage.setScene(new Scene(rootPane));
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> {
                Platform.runLater(() -> startupLatch.countDown());
            });
            stage.show();
        }
    }

    @BeforeClass
    public static void initFX() throws Exception {
        startupLatch = new CountDownLatch(1);
        new Thread(() -> Application.launch(NodeTreeShowingTest.TestApp.class, (String[]) null)).start();

        assertTrue("Timeout waiting for FX runtime to start", startupLatch.await(15, TimeUnit.SECONDS));
    }

    private StackPane createNodesRecursively(int count, int level) {
        StackPane pane = new StackPane();

        for (int i = 0; i < count; i++) {
            pane.getChildren().add(level == 1 ? new StackPane() : createNodesRecursively(count, level - 1));
        }

        return pane;
    }

    /**
     * This tests how quickly Nodes can be added and removed from a very large Scene.  Specifically,
     * this test was created to ensure that not every Node in the Scene creates a listener on its
     * Scene (and/or its associated Window).  With a large amount of Nodes in a Scene the listener
     * lists of the associated Scene and/or Window can become very large and adding or removing
     * a Node takes a performance hit.
     */
    @Test
    public void testAddRemovalSpeedInHugeScene() throws Exception {
        Random rnd = new Random(0);  // seed random to keep it predictable
        int loopCount = 10000;
        int levels = 13;
        int nodesPerLevel = 2;  // total nodes is (nodesPerLevel ^ levels) * 2 - 1
        int leafCount = (int)Math.pow(nodesPerLevel, levels);
        int total = leafCount * 2 - 1;
        StackPane testNode = new StackPane();
        StackPane root = createNodesRecursively(nodesPerLevel, levels);
        AtomicLong bestMillis = new AtomicLong(Long.MAX_VALUE);

        Util.runAndWait(() -> {
            rootPane.setCenter(root);
        });

        for (int j = 0; j < 5; j++) {
            int loopNumber = j + 1;

            Util.runAndWait(() -> {
                // Compute time it takes to add/remove Nodes at random spots
                long startTime = System.currentTimeMillis();

                for (int i = 0; i < loopCount; i++) {
                    // Find a random leaf to remove/re-add a child:
                    int index = rnd.nextInt(leafCount);
                    StackPane current = root;

                    while (index >= nodesPerLevel) {
                        current = (StackPane) current.getChildren().get(index % nodesPerLevel);
                        index /= nodesPerLevel;
                    }

                    current.getChildren().add(current.getChildren().remove(index));
                }

                long endTime = System.currentTimeMillis();

                bestMillis.set(Math.min(endTime - startTime, bestMillis.get()));

                System.out.println("Loop " + loopNumber + ": Time to add/remove " + loopCount + " nodes in "
                        + "a Scene consisting of " + total + " nodes = " + (endTime - startTime) + " mSec");
            });
        }

        // NOTE : 800 mSec is not a benchmark value
        // It is good enough to catch the regression in performance, if any
        assertTrue("Time to add/remove " + loopCount + " nodes in a large Scene is more than 800 mSec (" + bestMillis.get() + ")", bestMillis.get() <= 800);
    }

    @AfterClass
    public static void teardownOnce() {
        Platform.runLater(() -> {
            stage.hide();
            Platform.exit();
        });
    }
}
