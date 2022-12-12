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

package test.javafx.scene.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.IntBuffer;
import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

/**
 * This test verifies the fix for JDK-8229890.
 * This test ensures that the callback provided to the {@link javafx.scene.image.PixelBuffer#updateBuffer()}
 * method may return {@link javafx.geometry.Rectangle2D.Empty} in order to
 * indicate that no update is necessary and no exception is thrown.
 */
public class WritableImageFromBufferTest {

    static CountDownLatch startupLatch = new CountDownLatch(1);

    private static final int IMG_WIDTH = 600;
    private static final int IMG_HEIGHT = 400;

    private PixelBuffer<IntBuffer> pixelBuffer;
    private Graphics2D g2d;
    private WritableImage fxImage;

    private static Scene scene;

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            scene = new Scene(new StackPane(), IMG_WIDTH, IMG_HEIGHT);
            primaryStage.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> startupLatch.countDown());
            primaryStage.setScene(scene);
            primaryStage.show();
        }
    }

    @Before
    public void setUp() throws Exception {
        BufferedImage awtImage = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_ARGB_PRE);
        g2d = (Graphics2D) awtImage.getGraphics();

        DataBuffer db = awtImage.getRaster().getDataBuffer();
        DataBufferInt dbi = (DataBufferInt) db;
        int[] rawInts = dbi.getData();
        IntBuffer ib = IntBuffer.wrap(rawInts);

        PixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbPreInstance();
        pixelBuffer = new PixelBuffer<>(IMG_WIDTH, IMG_HEIGHT, ib, pixelFormat);
        fxImage = new WritableImage(pixelBuffer);
    }

    @Test
    public void test() throws InterruptedException {
        PrintStream defaultErrorStream = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setErr(new PrintStream(out, true));

        Thread.sleep(1000);
        Util.runAndWait(() -> {
            StackPane root = (StackPane)scene.getRoot();
            root.getChildren().add(new ImageView(fxImage));
            requestFullUpdate();
        });
        Thread.sleep(100);
        Util.runAndWait(() -> {
            requestEmptyUpdate(); // This will fail without the fix.
        });
        Thread.sleep(100);
        Util.runAndWait(() -> {
            requestPartialUpdate();
        });
        Thread.sleep(100);

        System.setErr(defaultErrorStream);
        Assert.assertEquals("No error should be thrown", "", out.toString());
    }

    private void requestFullUpdate() {
        // This call should work before and after the fix.
        pixelBuffer.updateBuffer(pb -> {
            g2d.setBackground(Color.decode("#FF0000"));
            g2d.clearRect(0, 0, IMG_WIDTH, IMG_HEIGHT);
            return null;
        });
    }

    private void requestEmptyUpdate() {
        // This call should fail without the fix and pass after the fix.
        pixelBuffer.updateBuffer(pb -> {
            // Nothing to do.
            return Rectangle2D.EMPTY;
        });
    }

    private void requestPartialUpdate() {
        // This call should work before and after the fix.
        pixelBuffer.updateBuffer(pb -> {
            g2d.setBackground(Color.decode("#0000FF"));
            g2d.clearRect(0, 0, IMG_WIDTH / 2, IMG_HEIGHT);
            return new Rectangle2D(0, 0, IMG_WIDTH / 2, IMG_HEIGHT);
        });
    }

    @BeforeClass
    public static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void tearDown() {
        Util.shutdown();
    }
}
