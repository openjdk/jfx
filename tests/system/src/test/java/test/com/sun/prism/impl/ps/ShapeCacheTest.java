/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.prism.impl.ps;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.transform.Affine;
import javafx.stage.Stage;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

/**
 * @test
 * @bug 8180086
 * @summary Check the rendering of a cached shape with a DropShadow when changing cache hints
 */
public class ShapeCacheTest {
    static boolean verbose = false;

    // Used to launch the application before running any test
    static CountDownLatch launchLatch = new CountDownLatch(1);

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
            myApp = this;
        }

        @Override
        public void start(Stage primaryStage) throws Exception {
            this.stage = primaryStage;
            launchLatch.countDown();
        }
    }

    @BeforeClass
    public static void setupOnce() {
        Util.launch(launchLatch, MyApp.class);
        assertEquals(0, launchLatch.getCount());
    }

    @AfterClass
    public static void teardownOnce() {
        Util.shutdown();
    }

    static Image snapshot(Node p, double sx, double sy) {
        if (verbose) {
            System.out.println("scale = "+sx+", "+sy);
        }
        int w = (int) Math.ceil(sx * 100);
        int h = (int) Math.ceil(sy * 100);
        Affine tx = new Affine();
        tx.appendScale(sx, sy);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.WHITE);
        params.setTransform(tx);
        params.setViewport(new Rectangle2D(0, 0, w, h));
        WritableImage wimg = new WritableImage(w, h);
        p.snapshot(params, wimg);

        return wimg;
    }

    @Test
    public void cachedTranslateTest() {
        CountDownLatch latch = new CountDownLatch(1);
        int ret[] = new int[1];
        Platform.runLater(() -> {
            Path p = new Path();
            p.getElements().addAll(new MoveTo(29, 30),
                                   new LineTo(31, 30),
                                   new LineTo(35, 50),
                                   new LineTo(25, 50),
                                   new ClosePath());
            p.setFill(Color.BLUE);
            p.setStroke(Color.YELLOW);
            p.setCache(true);
            p.setCacheHint(CacheHint.SPEED);
            p.setEffect(new DropShadow(10.0, Color.BLACK));
            p.setRotate(180);

            Image img1 = snapshot(p, 1.0, 1.0);
            p.setCacheHint(CacheHint.ROTATE);
            Image img2 = snapshot(p, 1.0, 1.0);

            int y = 40;
            int maxdiff = 0;
            for (int x = 10; x < 50; x++) {
                int pixel1 = img1.getPixelReader().getArgb(x, y);
                int pixel2 = img2.getPixelReader().getArgb(x, y);
                if (verbose) {
                    System.out.printf("pixel at (%d, %d) ", x, y);
                    System.out.printf("in first image = 0x%08x", pixel1);
                    System.out.printf(", second image = 0x%08x", pixel2);
                }
                if (pixel1 != pixel2) {
                    int a1 = (pixel1 >> 24) & 0xff;
                    int r1 = (pixel1 >> 16) & 0xff;
                    int g1 = (pixel1 >>  8) & 0xff;
                    int b1 = (pixel1      ) & 0xff;
                    int a2 = (pixel2 >> 24) & 0xff;
                    int r2 = (pixel2 >> 16) & 0xff;
                    int g2 = (pixel2 >>  8) & 0xff;
                    int b2 = (pixel2      ) & 0xff;
                    int diffar = Math.max(Math.abs(a1 - a2), Math.abs(r1 - r2));
                    int diffgb = Math.max(Math.abs(g1 - g2), Math.abs(b1 - b2));
                    int diff = Math.max(diffar, diffgb);
                    maxdiff = Math.max(diff, maxdiff);
                    if (verbose) {
                        System.out.printf("Pixels differ by %d!!!!", diff);
                    }
                }
                if (verbose) {
                    System.out.println();
                }
            }
            ret[0] = maxdiff;
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException("test timed out");
        }
        int maxdiff = ret[0];
        if (maxdiff > 5) {
            throw new RuntimeException("max diff ("+maxdiff+") > 5");
        } else if (verbose) {
            System.out.printf("worst diff = %d\n", maxdiff);
        }
    }
}
