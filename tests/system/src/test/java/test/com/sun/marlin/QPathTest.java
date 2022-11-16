/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

/**
 * @test
 * @bug 8170140
 * @summary Check the rendering anomaly with MarlinFX renderer
 */
public class QPathTest {

    private final static double SCALE = 2.0;

    private final static long MAX_DURATION = 3000 * 1000 * 1000L; // 3s

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Singleton Application instance
    static MyApp myApp;

    static boolean doChecksFailed = false;

    private static final Logger log;

    static {
        Locale.setDefault(Locale.US);

        // initialize j.u.l Looger:
        log = Logger.getLogger("prism.marlin");
        log.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                final Throwable th = record.getThrown();
                // detect any Throwable:
                if (th != null) {
                    System.out.println("Test failed:\n" + record.getMessage());
                    th.printStackTrace(System.out);

                    doChecksFailed = true;

                    throw new RuntimeException("Test failed: ", th);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });

        // enable Marlin logging & internal checks:
        System.setProperty("prism.marlinrasterizer", "true");
        System.setProperty("prism.marlin.log", "true");
        System.setProperty("prism.marlin.useLogger", "true");
        System.setProperty("prism.marlin.doChecks", "true");
    }

    private CountDownLatch latch = new CountDownLatch(1);

    // Application class. An instance is created and initialized before running
    // the first test, and it lives through the execution of all tests.
    public static class MyApp extends Application {

        Stage stage = null;

        public MyApp() {
            super();
        }

        @Override
        public void init() {
            QPathTest.myApp = this;
        }

        @Override
        public void start(Stage primaryStage) throws Exception {
            this.stage = primaryStage;
            launchLatch.countDown();
        }
    }

    @BeforeClass
    public static void setupOnce() throws Exception {
        Util.launch(launchLatch, MyApp.class);
        assertEquals(0, launchLatch.getCount());
    }

    @AfterClass
    public static void teardownOnce() {
        Util.shutdown();
    }

    @Test(timeout = 15000)
    public void TestBug() {
        Platform.runLater(() -> {
            SVGPath path = new SVGPath();
            String svgpath = readPath();
            path.setContent(svgpath);

            Scene scene = new Scene(new Group(path), 400, 400, Color.WHITE);
            myApp.stage.setScene(scene);
            myApp.stage.show();

            DoubleProperty rscale = new SimpleDoubleProperty(SCALE);
            myApp.stage.renderScaleXProperty().bind(rscale);
            myApp.stage.renderScaleYProperty().bind(rscale);

            double scw = scene.getWidth();
            double sch = scene.getHeight();
            Bounds pathbounds = path.getBoundsInParent();
            double pathXoff = -pathbounds.getMinX();
            double pathYoff = -pathbounds.getMinY();
            double bounceW = scw - pathbounds.getWidth();
            double bounceH = sch - pathbounds.getHeight();
            double Xrate = (1.0 + Math.random()) / 2.0;
            double Yrate = (1.0 + Math.random()) / 2.0;

            DoubleProperty prop = new SimpleDoubleProperty();

            final long start = System.nanoTime();

            prop.addListener((Observable) -> {
                if (doChecksFailed || System.nanoTime() - start > MAX_DURATION) {
                    latch.countDown();
                    myApp.stage.close();
                }
                double v = prop.doubleValue();
                double x = Math.abs((((v * Xrate) % 2.0) - 1.0) * bounceW);
                double y = Math.abs((((v * Yrate) % 2.0) - 1.0) * bounceH);
                path.setTranslateX(pathXoff + x);
                path.setTranslateY(pathYoff + y);
                path.setContent(null);
                path.setContent(svgpath);
            });
            int bignum = 1000000;
            KeyValue kv = new KeyValue(prop, bignum);
            KeyFrame kf = new KeyFrame(Duration.seconds(bignum), kv);
            Timeline t = new Timeline(kf);
            t.setCycleCount(Timeline.INDEFINITE);
            t.play();
        });
        try {
            latch.await();
        } catch (InterruptedException ie) {
            Logger.getLogger(QPathTest.class.getName()).log(Level.SEVERE, "interrupted", ie);
        }
        Assert.assertFalse("DoChecks detected a problem.", doChecksFailed);
    }

    static String readPath() {
        return "M54.589844,86.230469 C27.929688,86.230469 10.546875,107.714844 10.546875,140.722656 C10.546875,173.925781 "
                + "27.734375,195.214844 54.589844,195.214844 C69.433594,195.214844 80.761719,189.062500 87.500000,177.539063 "
                + "L89.062500,177.539063 L89.062500,228.515625 L106.054688,228.515625 L106.054688,88.085938 L89.843750,88.085938 "
                + "L89.843750,105.664063 L88.281250,105.664063 C82.031250,93.847656 68.945313,86.230469 54.589844,86.230469 Z "
                + "M58.398438,180.078125 C39.257813,180.078125 27.929688,165.429688 27.929688,140.722656 C27.929688,116.113281 "
                + "39.355469,101.367188 58.496094,101.367188 C77.539063,101.367188 89.550781,116.601563 89.550781,140.722656 "
                + "C89.550781,164.941406 77.636719,180.078125 58.398438,180.078125 Z ";
    }
}
