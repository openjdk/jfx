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
package test.robot.javafx.scene.canvas;

import java.io.FileInputStream;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

// 1. An image, half white half black sized 10 X 10 is used in test.
// 2. The image is drawn on a 20 times scaled up canvas(200 X 200).

public class ImageSmoothingDrawTest {

    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Robot robot;
    static ImageCanvas imageCanvas;
    static volatile Stage stage;
    static volatile Scene scene;
    Color lastWhitePixelColor;
    Color whitePixelColor;
    static final int scaleFactor = 20;

    public void getPixelColors() {
        int lastWhiteX = (int) (stage.getX() + scene.getX() +
            imageCanvas.getLayoutX() + (imageCanvas.getWidth() / 2) - 2);
        int heightCenter = (int) (stage.getY() + scene.getY() +
            imageCanvas.getLayoutY() + (imageCanvas.getHeight() / 2));

        Util.runAndWait(() -> {
            lastWhitePixelColor = robot.getPixelColor(lastWhiteX, heightCenter);
            whitePixelColor = robot.getPixelColor(
                lastWhiteX - (imageCanvas.getWidth() / 4), heightCenter);
        });
    }

    // 3. When image smoothing is enabled, linear filtering is applied
    // at the center where white and black colors meet.
    // 4. A width of few pixels at the center would not be white or black.
    // 5. Color of second last white side pixel, should NOT be white.
    @Test
    public void testImageSmoothingEnabled() {
        imageCanvas.setImageSmoothing(true);
        Util.sleep(1000); // Wait for image to be drawn
        getPixelColors();
        Assert.assertEquals(Color.WHITE, whitePixelColor);
        Assert.assertFalse(whitePixelColor.equals(lastWhitePixelColor));
    }

    // 6. When image smoothing is disabled, filtering is NOT applied
    // where at the center where white and black colors meet.
    // 7. As filtering is NOT applied, all white pixels remain white
    // and black remain black.
    // 8. Color of second last white side pixel, should remain white.
    @Test
    public void testImageSmoothingDisabled() {
        imageCanvas.setImageSmoothing(false);
        Util.sleep(1000); // Wait for image to be drawn
        getPixelColors();
        Assert.assertEquals(Color.WHITE, whitePixelColor);
        Assert.assertEquals(whitePixelColor, lastWhitePixelColor);
    }

    @BeforeClass
    public static void initFX() {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void exit() {
        Util.shutdown(stage);
    }

    @Before
    public void before() {
        Util.parkCursor(robot);
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            robot = new Robot();
            stage = primaryStage;
            // Create 20 times scaled canvas.
            URL resource = this.getClass().getResource("image_smoothing_draw_test.png");
            FileInputStream inFile = new FileInputStream(resource.getFile());
            Image whiteBlack = new Image(inFile);
            inFile.close();
            imageCanvas = new ImageCanvas(whiteBlack,
                whiteBlack.getWidth() * scaleFactor,
                whiteBlack.getHeight() * scaleFactor);

            VBox root = new VBox();
            root.getChildren().add(imageCanvas);

            scene = new Scene(root);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e ->
                    Platform.runLater(startupLatch::countDown));
            stage.setAlwaysOnTop(true);
            stage.show();
        }
    }

    static class ImageCanvas extends Canvas {
        private Image image = null;

        public ImageCanvas(Image img, double width, double height) {
            super(width, height);
            image = img;
        }

        public void render() {
            GraphicsContext gc = getGraphicsContext2D();
            if (image != null) {
                gc.drawImage(image,
                    0, 0, image.getWidth(), image.getHeight(),
                    0, 0, getWidth(), getHeight());
            }
        }

        public void setImageSmoothing(boolean smooth) {
            GraphicsContext gc = getGraphicsContext2D();
            gc.setImageSmoothing(smooth);
            render();
        }
    }
}
