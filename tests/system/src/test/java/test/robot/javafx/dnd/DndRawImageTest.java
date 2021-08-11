/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package test.robot.javafx.dnd;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

public class DndRawImageTest {
    static CountDownLatch startupLatch;
    static int SIZE = 240; //do not exceed 240 because it's the max drag image size
    static Image image = createImage(SIZE, SIZE);
    static Stage stage;
    static Scene scene;
    static Robot robot;

    //will change during the test
    private Color red = Color.BLACK;
    private Color green = Color.BLACK;
    private Color blue = Color.BLACK;

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            robot = new Robot();

            primaryStage.addEventHandler(WindowEvent.WINDOW_SHOWN, e ->
                    Platform.runLater(startupLatch::countDown));

            ImageView imageView = new ImageView(image);
            imageView.setOnDragDetected(event -> {
                ClipboardContent content = new ClipboardContent();
                content.putImage(image);
                Dragboard dragboard = imageView.startDragAndDrop(TransferMode.ANY);
                dragboard.setContent(content);
            });

            Pane pane = new Pane();
            pane.getChildren().add(imageView);
            primaryStage.setTitle("Drag View Image Colors");
            scene = new Scene(pane);
            primaryStage.setScene(scene);
            primaryStage.setAlwaysOnTop(true);
            primaryStage.setX(200);
            primaryStage.setY(200);
            primaryStage.show();
            stage = primaryStage;
        }
    }

    private static Image createImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                if (x < width * 0.33) {
                    image.setRGB(x, y, 0xFF0000);
                } else if (x < width * 0.66) {
                    image.setRGB(x, y, 0x00FF00);
                } else {
                    image.setRGB(x, y, 0x0000FF);
                }
            }
        }

        return SwingFXUtils.toFXImage(image, null);
    }

    @BeforeClass
    public static void initFX() {
        startupLatch = new CountDownLatch(1);
        new Thread(() -> Application.launch(TestApp.class, (String[]) null)).start();
        try {
            if (!startupLatch.await(10, TimeUnit.SECONDS)) {
                fail("Timeout waiting for FX runtime to start");
            }
        } catch (InterruptedException ex) {
            fail("Unexpected exception: " + ex);
        }
    }

    @Test
    public void testDragView() {
        Util.sleep(200);

        int x = (int) (scene.getWindow().getX() + scene.getX());
        int y = (int) (scene.getWindow().getY() + scene.getY());

        Util.runAndWait(() -> {
            robot.mouseMove(x + 1, y + 1);
            robot.mousePress(MouseButton.PRIMARY);
        });

        Util.runAndWait(() -> {
            //activate drag
            robot.mouseMove(x + 300, y + 300);
        });

        Util.sleep(300);

        Util.runAndWait(() -> {
            //may vary according to platform - this considers that the drag image is centered
            int centerPos = (int) robot.getMouseX();

            int redX = centerPos - (SIZE / 3);
            int greenX = centerPos;
            int blueX = centerPos + (SIZE / 3);
            int colorY = (int) robot.getMouseY();

            red = robot.getPixelColor(redX, colorY);
            green = robot.getPixelColor(greenX, colorY);
            blue = robot.getPixelColor(blueX, colorY);
        });

        Util.runAndWait(() -> robot.mouseRelease(MouseButton.PRIMARY));

        Assert.assertEquals("First color must be red", Color.color(1D, 0D, 0D, 1D), red);
        Assert.assertEquals("Second color must be green", Color.color(0D, 1D, 0D, 1D), green);
        Assert.assertEquals("Third color must be blue", Color.color(0D, 0D, 1D, 1D), blue);
    }

    @AfterClass
    public static void teardown() {
        Platform.runLater(stage::hide);
        Platform.exit();
    }
}
