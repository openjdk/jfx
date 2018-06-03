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
package test.com.sun.marlin;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.VLineTo;
import javafx.stage.Stage;

import junit.framework.AssertionFailedError;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;
import static test.util.Util.TIMEOUT;

/**
 * Simple Dashed Rect rendering test
 *
 * @test
 * @summary verify that dashed rectangle is properly rasterized
 * @bug 8202743
 */
public class DashedRectTest {

    static final int BLUE_PIXEL = 0xff0000ff;

    final static double DASH_LEN = 3.0;
    final static double DASH_PH = 5000.0;

    final static int MAX = 100;

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Singleton Application instance
    static MyApp myApp;

    // Application class. An instance is created and initialized before running
    // the first test, and it lives through the execution of all tests.
    public static class MyApp extends Application {

        Stage stage = null;

        public MyApp() {
            super();
        }

        @Override
        public void init() {
            DashedRectTest.myApp = this;
        }

        @Override
        public void start(Stage primaryStage) throws Exception {
            this.stage = primaryStage;

            stage.setScene(new Scene(new Group()));
            stage.setTitle("DashedRectTest");
            stage.show();

            launchLatch.countDown();
        }
    }

    @BeforeClass
    public static void setupOnce() {
        // Start the Application
        new Thread(() -> Application.launch(MyApp.class, (String[]) null)).start();

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

    @Test(timeout = 10000)
    public void TestDashedPath() throws InterruptedException {

        final int size = MAX * 2;

        Util.runAndWait(() -> {

            // Corrupt Marlin Dasher.dash cached array:
            final Path path1 = new Path();
            path1.getElements().addAll(
                    new MoveTo(20, 20),
                    new HLineTo(70),
                    new VLineTo(70),
                    new HLineTo(20),
                    new ClosePath()
            );
            path1.setStroke(Color.RED);
            path1.setFill(null);
            path1.setStrokeWidth(2);
            path1.setStrokeDashOffset(DASH_PH);

            final ObservableList<Double> pDashes = path1.getStrokeDashArray();
            pDashes.clear();
            for (int i = 0; i < 100; i++) {
                pDashes.add(19.333);
            }

            // Create 2nd path shape
            final Path path2 = new Path();
            path2.getElements().addAll(
                    new MoveTo(5, 5),
                    new HLineTo(MAX),
                    new VLineTo(MAX),
                    new HLineTo(5),
                    new ClosePath()
            );
            path2.setFill(null);
            path2.setStroke(Color.BLUE);
            path2.setStrokeWidth(2);
            path2.setStrokeDashOffset(DASH_PH);
            path2.getStrokeDashArray().setAll(DASH_LEN);

            Scene scene = new Scene(new Group(path1, path2));

            myApp.stage.setScene(scene);

            final SnapshotParameters sp = new SnapshotParameters();
            sp.setViewport(new Rectangle2D(0, 0, size, size));

            final WritableImage img = scene.getRoot().snapshot(sp, new WritableImage(size, size));

            // Check image on few pixels:
            final PixelReader pr = img.getPixelReader();

            // 10, 5 = blue
            checkPixel(pr, 10, 5, BLUE_PIXEL);
        });
    }

    private static void checkPixel(final PixelReader pr,
                                   final int x, final int y,
                                   final int expected) {

        final int rgb = pr.getArgb(x, y);
        if (rgb != expected) {
            fail("bad pixel at (" + x + ", " + y
                    + ") = " + rgb + " expected: " + expected);
        }
    }
}
