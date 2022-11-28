/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.stage;

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class NestedEventLoopPlatformExitTest {

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    public static class TestApp extends Application {

        @Override public void start(Stage primaryStage) throws Exception {
            primaryStage.setTitle("Primary stage");
            Group root = new Group();
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setWidth(100);
            primaryStage.setHeight(100);
            primaryStage.addEventHandler(WindowEvent.WINDOW_SHOWN,
                e -> Platform.runLater(launchLatch::countDown));
            primaryStage.show();
        }
    }

    @BeforeClass
    public static void initFX() throws InterruptedException {
        Util.launch(launchLatch, TestApp.class);
    }

    // Verify that Platform.exit can be called while the NestedEventLoop is
    // running
    @Test(timeout = 20000)
    public void testPlatformExitWithNestedEventLoop() {
        Util.runAndWait(
            () -> {
                final long nestedLoopEventKey = 1024L;
                Assert.assertFalse(Platform.isNestedLoopRunning());
                Platform.enterNestedEventLoop(nestedLoopEventKey);
            },
            () -> {
                Assert.assertTrue(Platform.isNestedLoopRunning());
                Platform.exit();
            }
        );
    }
}
