/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.javafx.stage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import test.util.Util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class StageFocusTest {

    static CountDownLatch startupLatch;
    static CountDownLatch eventReceivedLatch;

    static final int STAGE_SIZE = 200;

    static final int STAGE_X = 100;
    static final int STAGE_Y = 100;

    static final int TIMEOUT = 5000; // ms
    static final double TOLERANCE = 0.07;

    static final Color SCENE_COLOR = Color.LIGHTGREEN;

    static Stage theStage;
    static Robot robot;

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            Platform.setImplicitExit(false);

            theStage = primaryStage;

            Group root = new Group();
            Scene scene = new Scene(root, STAGE_SIZE, STAGE_SIZE);
            scene.setFill(SCENE_COLOR);
            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.A) {
                    eventReceivedLatch.countDown();
                }
            });

            theStage.initStyle(StageStyle.UNDECORATED);
            theStage.setX(STAGE_X);
            theStage.setY(STAGE_Y);
            theStage.setScene(scene);
            theStage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> {
                startupLatch.countDown();
            });
            theStage.show();
        }
    }

    @BeforeAll
    public static void setupOnce() throws Exception {
        startupLatch = new CountDownLatch(1);
        eventReceivedLatch = new CountDownLatch(1);

        Util.launch(startupLatch, TestApp.class);
        assertTrue(startupLatch.await(TIMEOUT, TimeUnit.MILLISECONDS), "Timeout waiting for test stage to be shown");

        Util.runAndWait(() -> robot = new Robot());
    }

    @AfterAll
    public static void doTeardownOnce() {
        Util.shutdown();
    }

    private String colorToString(Color c) {
        int r = (int)(c.getRed() * 255.0);
        int g = (int)(c.getGreen() * 255.0);
        int b = (int)(c.getBlue() * 255.0);
        int a = (int)(c.getOpacity() * 255.0);
        return "rgba(" + r + "," + g + "," + b + "," + a + ")";
    }

    private void assertColorEquals(Color expected, Color actual, double delta) {
        double deltaRed = Math.abs(expected.getRed() - actual.getRed());
        double deltaGreen = Math.abs(expected.getGreen() - actual.getGreen());
        double deltaBlue = Math.abs(expected.getBlue() - actual.getBlue());
        double deltaOpacity = Math.abs(expected.getOpacity() - actual.getOpacity());
        assertTrue(deltaRed <= delta && deltaGreen <= delta && deltaBlue <= delta && deltaOpacity <= delta,
            "Color " + colorToString(actual) + " did not match color " + colorToString(expected));
    }

    /**
     * Checks whether Stage is actually shown when calling show()
     *
     * Meant as a "canary" test of sorts to ensure other tests relying on
     * Stage being actually shown and on foreground work fine.
     */
    @Test
    public void testStageHasFocusAfterShow() throws InterruptedException {
        // check if isFocused returns true
        assertTrue(
            theStage.isFocused(),
            "Stage.isFocused() returned false! Stage does not have focus after showing. " +
            "Some tests might fail because of this. Try re-running the tests with '--no-daemon' flag in Gradle."
        );

        // give UI a bit of time to finish showing transition
        // ex. on Windows above latch is set despite the UI still "animating" the show
        Thread.sleep(500);

        // check if window is on top
        Util.runAndWait(() -> {
            System.err.println("Checking if on top");
            WritableImage capture = robot.getScreenCapture(null, STAGE_X, STAGE_Y, STAGE_SIZE, STAGE_SIZE, false);
            PixelReader captureReader = capture.getPixelReader();
            for (int x = 0; x < STAGE_SIZE; ++x) {
                for (int y = 0; y < STAGE_SIZE; ++y) {
                    Color color = captureReader.getColor(x, y);
                    assertColorEquals(SCENE_COLOR, color, TOLERANCE);
                }
            }
            System.err.println("Checking if on top done");
        });

        // check if we actually have focus and key presses are registered by the app
        Util.runAndWait(() -> {
            System.err.println("Pressing A!");
            robot.keyPress(KeyCode.A);
        });
        assertTrue(
            eventReceivedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS),
            "The Stage did not receive the key stroke generated by Robot! This might happen if the Stage did not receive focus after showing. " +
            "Some tests might fail because of this. Try re-running the tests with '--no-daemon' flag in Gradle."
        );
    }
}
