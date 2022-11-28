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
package test.robot.javafx.scene;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ColorPicker;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.fail;

import test.util.Util;

public class ColorPickerTest {

    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Robot robot;
    static volatile Stage stage;
    static volatile Scene scene;
    static final int SCENE_WIDTH = 250;
    static final int SCENE_HEIGHT = SCENE_WIDTH;
    static VBox root;
    int onShownCount = 0;
    int onActionCount = 0;
    ColorPicker colorPicker;
    CountDownLatch onShownLatch;
    CountDownLatch onActionLatch;

    private void mouseClick(double x, double y) {
        Util.runAndWait(() -> {
            robot.mouseMove((int) (scene.getWindow().getX() + scene.getX() + x),
                    (int) (scene.getWindow().getY() + scene.getY() + y));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
    }

    private void showColorPickerPalette() throws Exception {
        onShownLatch = new CountDownLatch(1);
        mouseClick(colorPicker.getLayoutX() + colorPicker.getWidth() - 15,
                    colorPicker.getLayoutY() + colorPicker.getHeight() / 2);
        Thread.sleep(400); // ColorPicker takes some time to display the color palette.
        Util.waitForLatch(onShownLatch, 10, "Failed to show color palette.");
    }

    private void clickColorPickerPalette(int yFactor) throws Exception {
        onActionLatch = new CountDownLatch(1);
        mouseClick(colorPicker.getLayoutX() + colorPicker.getWidth() / 2,
                    colorPicker.getLayoutY() + colorPicker.getHeight() * yFactor);
        Thread.sleep(400);
        Util.waitForLatch(onActionLatch, 10, "Failed to receive onAction callback.");
    }

    // This test is for verifying a specific behavior.
    // 1. Remove ColorPicker, call ColorPicker.show() & add back ColorPicker.
    // 2. Verify that the ColorPicker palette is shown using onShown,
    // 2.1 Confirm that color palette is shown, using listener onAction.
    // 3. Click on ColorPicker, color palette should get shown.
    // 4. Repeat step 2.
    // 4.1 Repeat step 2.1
    // 5. Click on ColorPicker, color palette should get shown.
    // 6. Remove & add back ColorPicker, color palette should get shown.
    // 7. Confirm that color palette is shown, using listener onAction.

    @Test
    public void testColorPickerSceneChange() throws Exception {
        Thread.sleep(1000); // Wait for stage to layout

        // 1.
        onShownLatch = new CountDownLatch(1);
        Util.runAndWait(() -> {
            root.getChildren().clear();
            colorPicker.show();
            root.getChildren().add(colorPicker);
        });
        Util.waitForLatch(onShownLatch, 10, "Failed to show color palette.");
        Thread.sleep(400); // ColorPicker takes some time to display the color palette.
        // 2.
        Assert.assertEquals("ColorPicker palette should be shown once.", 1, onShownCount);

        // 2.1
        clickColorPickerPalette(5);
        Assert.assertEquals("ColorPicker palette should be clicked once.", 1, onActionCount);

        // 3.
        showColorPickerPalette();
        // 4.
        Assert.assertEquals("ColorPicker palette should have been shown two times.", 2, onShownCount);

        // 4.1
        clickColorPickerPalette(6);
        Assert.assertEquals("ColorPicker palette have been clicked two times.", 2, onActionCount);

        // 5.
        showColorPickerPalette();
        Assert.assertEquals("ColorPicker palette should have been shown three times.", 3, onShownCount);

        // 6.
        Util.runAndWait(() -> {
            root.getChildren().clear();
            root.getChildren().add(colorPicker);
        });
        Thread.sleep(400); // ColorPicker takes some time to display the color palette.

        // 7.
        clickColorPickerPalette(5);
        Assert.assertEquals("ColorPicker palette should have been clicked three times.", 3, onActionCount);
    }

    @After
    public void resetUI() {
        Platform.runLater(() -> {
            colorPicker.setOnShown(null);
            colorPicker.setOnAction(null);
            root.getChildren().clear();
        });
    }

    @Before
    public void setupUI() {
        Platform.runLater(() -> {
            colorPicker = new ColorPicker();
            colorPicker.setOnShown(event -> {
                onShownCount++;
                onShownLatch.countDown();
            });
            colorPicker.setOnAction(event -> {
                onActionCount++;
                onActionLatch.countDown();
            });
            root.getChildren().add(colorPicker);
        });
    }

    @BeforeClass
    public static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void exit() {
        Util.shutdown(stage);
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;
            root = new VBox();
            scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e ->
                    Platform.runLater(startupLatch::countDown));
            stage.setAlwaysOnTop(true);
            stage.show();
        }
    }
}
