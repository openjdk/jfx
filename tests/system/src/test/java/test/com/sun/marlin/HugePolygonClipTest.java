/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Translate;
import javafx.stage.Screen;
import javafx.stage.Stage;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

/**
 * Clip rendering test
 *
 * @test
 * @summary verify that huge polygon is properly rasterized
 * @bug 8274066
 */
public class HugePolygonClipTest {

    private static final Double LARGE_X_COORDINATE = 4194304.250;
    private static final int SCENE_WIDTH = 600;
    private static final int SCENE_HEIGHT = 400;

    private static final double WIDTH = 2.73;

    private static final int G_MASK = 0x0000ff00;
    private static final int R_MASK = 0x00ff0000;
    private static final int RGB_MASK = 0x00ffffff;

    private static final boolean SAVE_IMAGE = false;

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    static {
        Locale.setDefault(Locale.US);
        /*
            System.out.println("BLUE_PIXEL: " + BLUE_PIXEL);
            System.out.println("RED_PIXEL:  " + RED_PIXEL);
         */
        // enable Marlin logging:
        System.setProperty("prism.marlin.log", "true");

        System.setProperty("prism.marlin.clip", "true");

        System.setProperty("prism.marlin.subPixel_log2_X", "8");
    }

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
            HugePolygonClipTest.myApp = this;
        }

        @Override
        public void start(Stage primaryStage) throws Exception {
            this.stage = primaryStage;

            stage.setScene(new Scene(new Group()));
            stage.setTitle("HugePolygonClipTest");
            stage.show();

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

    @Test(timeout = 10000)
    public void TestHugePolygonCoords() throws InterruptedException {
        Util.runAndWait(() -> {

            double dpi = Screen.getPrimary().getDpi();
            System.out.println("dpi: " + dpi);

            double dpiScale = Screen.getPrimary().getOutputScaleX();
            System.out.println("dpiScale: " + dpiScale);

            // original test case => large moveTo in Filler but no bug in Stroker:
            Double longWidth = LARGE_X_COORDINATE + SCENE_WIDTH + 0.001;

            final Polygon veryWidePolygon;

            veryWidePolygon = new Polygon(
                    longWidth, 50.0,
                    longWidth, 100.0,
                    0.0, 100.0,
                    0.0, 0.0
            );

            veryWidePolygon.setFill(Color.RED);
            veryWidePolygon.setStroke(Color.GREEN);
            veryWidePolygon.setStrokeWidth(WIDTH);

            Group group = new Group(veryWidePolygon);
            group.getTransforms().add(new Translate(-longWidth + SCENE_WIDTH, 100.0));

            Scene scene = new Scene(group, SCENE_WIDTH, SCENE_HEIGHT, Color.BLACK);
            myApp.stage.setScene(scene);

            final SnapshotParameters sp = new SnapshotParameters();
            sp.setFill(Color.BLACK);
            sp.setViewport(new Rectangle2D(0, 0, SCENE_WIDTH, SCENE_HEIGHT));

            final WritableImage img = scene.getRoot().snapshot(sp, new WritableImage(SCENE_WIDTH, SCENE_HEIGHT));

            if (SAVE_IMAGE) {
                try {
                    saveImage(img, OUTPUT_DIR, "TestHugePolygonCoords.png");
                } catch (IOException ex) {
                    // ignore
                }
            }

            // Check image on few pixels:
            final PixelReader pr = img.getPixelReader();

            final int x = SCENE_WIDTH / 2;
            checkColumn(pr, x, SCENE_HEIGHT);
        });
    }

    private static void checkColumn(final PixelReader pr, final int x, final int maxY) {
        boolean trigger = false;
        boolean inside = false;

        for (int y = 0; y < maxY; y++) {
            final int rgb = pr.getArgb(x, y);
            // System.out.println("pixel at (" + x + ", " + y + ") = " + rgb);

            if ((rgb & G_MASK) != 0) {
                if (!trigger) {
                    trigger = true;
                    inside = !inside;
                    // System.out.println("inside: "+inside);
                }
            } else {
                trigger = false;

                final int mask = (inside) ? R_MASK : RGB_MASK;

                final int expected = (rgb & mask);

                // System.out.println("pix[" + y + "] = " + expected + " inside: " + inside);
                if ((inside && (expected == 0))
                        || (!inside && (expected != 0))) {
                    fail("bad pixel at (" + x + ", " + y
                            + ") = " + expected + " inside: " + inside);
                }
            }
        }
    }

    static final File OUTPUT_DIR = new File(".");

    static void saveImage(final WritableImage image, final File resDirectory, final String imageFileName) throws IOException {
        saveImage(SwingFXUtils.fromFXImage(image, null), resDirectory, imageFileName);
    }

    static void saveImage(final BufferedImage image, final File resDirectory, final String imageFileName) throws IOException {
        final Iterator<ImageWriter> itWriters = ImageIO.getImageWritersByFormatName("PNG");
        if (itWriters.hasNext()) {
            final ImageWriter writer = itWriters.next();

            final ImageWriteParam writerParams = writer.getDefaultWriteParam();
            writerParams.setProgressiveMode(ImageWriteParam.MODE_DISABLED);

            final File imgFile = new File(resDirectory, imageFileName);

            if (!imgFile.exists() || imgFile.canWrite()) {
                System.out.println("saveImage: saving image as PNG [" + imgFile + "]...");
                imgFile.delete();

                // disable cache in temporary files:
                ImageIO.setUseCache(false);

                final long start = System.nanoTime();

                // PNG uses already buffering:
                final ImageOutputStream imgOutStream = ImageIO.createImageOutputStream(new FileOutputStream(imgFile));

                writer.setOutput(imgOutStream);
                try {
                    writer.write(null, new IIOImage(image, null, null), writerParams);
                } finally {
                    imgOutStream.close();

                    final long time = System.nanoTime() - start;
                    System.out.println("saveImage: duration= " + (time / 1000000l) + " ms.");
                }
            }
        }
    }
}
