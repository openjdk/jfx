/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static test.util.Util.TIMEOUT;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.SnapshotResult;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

import junit.framework.AssertionFailedError;
import test.util.Util;

/**
 * Common base class for testing snapshot.
 */
public class SnapshotCommon {

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Singleton Application instance
    static MyApp myApp;

    // Application class. An instance is created and initialized before running
    // the first test, and it lives through the execution of all tests.
    public static class MyApp extends Application {
        Stage primaryStage;

        @Override public void init() {
            SnapshotCommon.myApp = this;
        }

        @Override public void start(Stage primaryStage) throws Exception {
            assertTrue(Platform.isFxApplicationThread());
            primaryStage.setTitle("Primary stage");
            Group root = new Group();
            Scene scene = new Scene(root);
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

    static class TestStage extends Stage {
        TestStage(Scene scene) {
            this(StageStyle.UNDECORATED, scene);
        }

        TestStage(StageStyle style, Scene scene) {
            this.setTitle("Test stage");
            initStyle(style);

            this.setScene(scene);
            if (scene.getWidth() <= 0) {
                this.setWidth(200);
                this.setHeight(150);
            }
            this.setX(225);
            this.setY(0);
        }
    }

    static void doSetupOnce() {
        Util.launch(launchLatch, MyApp.class);
        assertEquals(0, launchLatch.getCount());
    }

    static void doTeardownOnce() {
        Util.shutdown();
    }

    protected void runDeferredSnapshotWait(final Node node,
            final Callback<SnapshotResult, Void> cb,
            final SnapshotParameters params,
            final WritableImage img,
            final Runnable runAfter) {

        final Throwable[] testError = new Throwable[1];
        final CountDownLatch latch = new CountDownLatch(1);

        Util.runAndWait(() -> {
            node.snapshot(result -> {
                try {
                    cb.call(result);
                } catch (Throwable th) {
                    testError[0] = th;
                } finally {
                    latch.countDown();
                }
                return null;
            }, params, img);
            assertEquals(1, latch.getCount());

            if (runAfter != null) {
                runAfter.run();
            }
        });

        try {
            if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for snapshot callback");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }

        if (testError[0] != null) {
            if (testError[0] instanceof Error) {
                throw (Error)testError[0];
            } else if (testError[0] instanceof RuntimeException) {
                throw (RuntimeException)testError[0];
            } else {
                AssertionFailedError err = new AssertionFailedError("Unknown execution exception");
                err.initCause(testError[0].getCause());
                throw err;
            }
        }
    }

    protected void runDeferredSnapshotWait(final Node node,
            final Callback<SnapshotResult, Void> cb,
            final SnapshotParameters params,
            final WritableImage img) {

        runDeferredSnapshotWait(node, cb, params, img, null);
    }

    protected void runDeferredSnapshotWait(final Scene scene,
            final Callback<SnapshotResult, Void> cb,
            final WritableImage img) {

        final Throwable[] testError = new Throwable[1];
        final CountDownLatch latch = new CountDownLatch(1);

        Util.runAndWait(() -> {
            scene.snapshot(result -> {
                try {
                    cb.call(result);
                } catch (Throwable th) {
                    testError[0] = th;
                } finally {
                    latch.countDown();
                }
                return null;
            }, img);
            assertEquals(1, latch.getCount());
        });

        try {
            if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for snapshot callback");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }

        if (testError[0] != null) {
            if (testError[0] instanceof Error) {
                throw (Error)testError[0];
            } else if (testError[0] instanceof RuntimeException) {
                throw (RuntimeException)testError[0];
            } else {
                AssertionFailedError err = new AssertionFailedError("Unknown execution exception");
                err.initCause(testError[0].getCause());
                throw err;
            }
        }
    }

}
