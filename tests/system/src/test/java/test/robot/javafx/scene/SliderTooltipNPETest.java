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
package test.robot.javafx.scene;

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

/*
 * Unit test for verifying Slider NPE error.
 *
 * There is 1 tests in this file.
 * Steps for testSliderTooltipNPE()
 * 1. Create a slider and tooltip.
 * 2. Make setAutoHide of tooltip as true and add tooltip to slider.
 * 3. Hover over slider thumb and wait for tooltip. Drag the thumb.
 * 4. Verify that NullPointerException is not thrown.
 */
public class SliderTooltipNPETest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static volatile Throwable exception;
    static volatile Stage stage;
    static volatile Scene scene;
    static Robot robot;
    static Slider slider;
    static final int SCENE_WIDTH = 250;
    static final int SCENE_HEIGHT = SCENE_WIDTH;
    final int DRAG_DISTANCE = 10;

    public static void main(String[] args) {
        initFX();
        try {
            SliderTooltipNPETest test = new SliderTooltipNPETest();
            test.testSliderTooltipNPE();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            exit();
        }
    }

    @Test
    public void testSliderTooltipNPE() throws Throwable {
        dragSliderAfterTooltipDisplayed(DRAG_DISTANCE, true);
    }

    private void dragSliderAfterTooltipDisplayed(int dragDistance, boolean xIncr) throws Throwable {
        Thread.sleep(1000); // Wait for slider to layout

        Util.runAndWait(() -> {
            robot.mouseMove((int)(scene.getWindow().getX() + scene.getX() + SCENE_WIDTH/2),
                (int)(scene.getWindow().getY() + scene.getY() + SCENE_HEIGHT/2));
        });

        Thread.sleep(3000); // Wait for tooltip to be displayed

        Util.runAndWait(() -> {
            robot.mousePress(MouseButton.PRIMARY);
        });

        for (int i = 0; i < dragDistance; i++) {
            final int c = i;
            Util.runAndWait(() -> {
                if (xIncr) {
                robot.mouseMove((int)(scene.getWindow().getX() + scene.getX() + SCENE_WIDTH/2) + c,
                    (int)(scene.getWindow().getY() + scene.getY() + SCENE_HEIGHT/2));
                } else {
                    robot.mouseMove((int)(scene.getWindow().getX() + scene.getX() + SCENE_WIDTH/2),
                        (int)(scene.getWindow().getY() + scene.getY() + SCENE_HEIGHT/2) + c);
                }
            });
        }

        Util.runAndWait(() -> {
            robot.mouseRelease(MouseButton.PRIMARY);
        });

        if (exception != null) {
            throw exception;
        }
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;
            slider = new Slider(0, 100, 50);
            Tooltip tooltip = new Tooltip("Autohide tooltip");
            tooltip.setAutoHide(true);
            slider.setTooltip(tooltip);
            scene = new Scene(slider, SCENE_WIDTH, SCENE_HEIGHT);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e ->
                    Platform.runLater(startupLatch::countDown));
            stage.setAlwaysOnTop(true);
            stage.show();
            Thread.currentThread().setUncaughtExceptionHandler((t2, e) -> {
                exception = e;
            });
        }
    }

    @BeforeClass
    public static void initFX() {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void exit() {
        Util.shutdown(stage);
    }
}
