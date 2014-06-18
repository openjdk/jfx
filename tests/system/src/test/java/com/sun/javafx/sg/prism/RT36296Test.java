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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.javafx.sg.prism;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import junit.framework.AssertionFailedError;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;
import static util.Util.TIMEOUT;

public class RT36296Test {
    CountDownLatch latch = new CountDownLatch(1);

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Singleton Application instance
    static MyApp myApp;

    // Application class. An instance is created and initialized before running
    // the first test, and it lives through the execution of all tests.
    public static class MyApp extends Application {
        @Override public void init() {
            RT36296Test.myApp = this;
        }

        @Override public void start(Stage primaryStage) throws Exception {
            launchLatch.countDown();
        }
    }

    @BeforeClass
    public static void setupOnce() {
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

    @Test(timeout = 5000)
    public void TestBug() {
        Label label = new Label();
        label.setStyle(" -fx-border-style:dashed; -fx-border-width:0; ");
        label.setText("test");

        SnapshotParameters params = new SnapshotParameters();
        params.setViewport(new Rectangle2D(0, 0, 100, 100));
        Platform.runLater(() -> {
            Scene scene = new Scene(new Group(label));
            label.snapshot(p -> done(), params, new WritableImage(100, 100));
        });
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Logger.getLogger(RT36296Test.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Void done() {
        latch.countDown();
        return null;
    }
}
