/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

/**
 * This test is based on the test case reported in JDK-8209830
 *
 * Redundant CSS Re-application was avoided in JDK-8193445.
 * It results in faster application of CSS on Controls (Nodes). In turn,
 * resulting in improved Node creation/addition time to a Scene.
 *
 * The goal of this test is *NOT* to measure absolute performance, but to show
 * creating and adding 500 Nodes to a scene does not take more than a
 * particular threshold of time.
 *
 * The selected thresold is larger than actual observed time.
 * It is not a benchmark value. It is good enough to catch the regression
 * in performance, if any.
 */

public class QuadraticCssTimeTest {

    private static CountDownLatch startupLatch = new CountDownLatch(1);
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
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void teardownOnce() {
        Util.shutdown(stage);
    }

    @Test
    public void testTimeForAdding500NodesToScene() throws Exception {

        Util.runAndWait(() -> {
            // Compute time for adding 500 Nodes
            long startTime = System.currentTimeMillis();

            HBox hbox = new HBox();
            for (int i = 0; i < 500; i++) {
                hbox = new HBox(new Text("y"), hbox);
                final HBox h = hbox;
                h.setPadding(new Insets(1));
            }
            rootPane.setCenter(hbox);

            long endTime = System.currentTimeMillis();

            System.out.println("Time to create and add 500 nodes to a Scene = " +
                               (endTime - startTime) + " mSec");

            // NOTE : 800 mSec is not a benchmark value
            // It is good enough to catch the regression in performance, if any
            assertTrue("Time to add 500 Nodes is more than 800 mSec", (endTime - startTime) < 800);
        });
    }
}
