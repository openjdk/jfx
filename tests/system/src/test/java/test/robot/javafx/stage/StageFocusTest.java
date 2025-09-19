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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sun.javafx.PlatformUtil;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


// NOTE: This test does NOT extend VisualTestBase, as the focus issues mostly happen
// on primaryStage delivered via Application.start()
public class StageFocusTest {
    static CountDownLatch launchLatch = new CountDownLatch(1);

    static final int STAGE_SIZE = 200;

    static final int STAGE_X = 100;
    static final int STAGE_Y = 100;

    static final int TIMEOUT = 2000; // ms
    static final double TOLERANCE = 0.07;

    static final Color SCENE_COLOR = Color.LIGHTGREEN;

    static List<Stage> testStages = new ArrayList<Stage>();
    static Robot robot;

    // NOTE: junit5 (at least the version we use) does not support parameterized class-level tests yet
    // As such, Before/AfterEach need to be hacked this way
    private Stage currentTestStage = null;

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            Platform.setImplicitExit(false);
            assertNotNull(primaryStage);

            testStages.add(primaryStage);
            testStages.add(new Stage());
            launchLatch.countDown();
        }
    }


    @BeforeAll
    public static void setupOnce() throws InterruptedException {
        Util.launch(launchLatch, TestApp.class);
        Util.runAndWait(() -> robot = new Robot());
    }

    @AfterAll
    public static void doTeardownOnce() {
        Util.shutdown();
    }

    // @BeforeEach
    public void setupEach(Stage stage) {
        currentTestStage = stage;
    }

    @AfterEach
    public void doTeardownEach() throws InterruptedException {
        assumeTrue(!PlatformUtil.isLinux()); // JDK-8367893

        hideTestStage(currentTestStage);
        currentTestStage = null;
    }

    private String colorToString(Color c) {
        int r = (int)(c.getRed() * 255.0);
        int g = (int)(c.getGreen() * 255.0);
        int b = (int)(c.getBlue() * 255.0);
        int a = (int)(c.getOpacity() * 255.0);
        return "rgba(" + r + "," + g + "," + b + "," + a + ")";
    }

    private boolean checkColorEquals(Color expected, Color actual, double delta) {
        double deltaRed = Math.abs(expected.getRed() - actual.getRed());
        double deltaGreen = Math.abs(expected.getGreen() - actual.getGreen());
        double deltaBlue = Math.abs(expected.getBlue() - actual.getBlue());
        double deltaOpacity = Math.abs(expected.getOpacity() - actual.getOpacity());
        return deltaRed <= delta && deltaGreen <= delta && deltaBlue <= delta && deltaOpacity <= delta;
    }

    private void initTestStage(Stage stage, CountDownLatch eventReceivedLatch) throws InterruptedException {
        CountDownLatch showLatch = new CountDownLatch(1);

        Util.runAndWait(() -> {
            Group root = new Group();
            Scene scene = new Scene(root, STAGE_SIZE, STAGE_SIZE);
            scene.setFill(SCENE_COLOR);
            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.A) {
                    eventReceivedLatch.countDown();
                }
            });

            stage.initStyle(StageStyle.UNDECORATED);
            stage.setX(STAGE_X);
            stage.setY(STAGE_Y);
            stage.setScene(scene);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> {
                showLatch.countDown();
            });
            stage.show();
        });

        assertTrue(showLatch.await(TIMEOUT, TimeUnit.MILLISECONDS), "Timeout waiting for test stage to be shown");
    }

    private void hideTestStage(Stage stage) throws InterruptedException {
        CountDownLatch hiddenLatch = new CountDownLatch(1);

        Util.runAndWait(() -> {
            stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, e -> {
                hiddenLatch.countDown();
            });
            stage.hide();
        });

        assertTrue(hiddenLatch.await(TIMEOUT, TimeUnit.MILLISECONDS), "Timeout waiting for test stage to hide");
    }

    /**
     * Checks whether Stage is actually shown when calling show()
     *
     * Meant as a "canary" test of sorts to ensure other tests relying on
     * Stage being actually shown and on foreground work fine.
     *
     * This checks both the Stage provided by Application.start() as well as
     * a newly created Stage.
     */
    @ParameterizedTest
    @FieldSource("testStages")
    public void testStageHasFocusAfterShow(Stage stage) throws InterruptedException {
        assumeTrue(!PlatformUtil.isLinux()); // JDK-8367893

        // TODO once we upgrade JUnit5 and have parameterized class-level tests
        //      this can be removed and be an actual @BeforeEach
        setupEach(stage);

        // initialize and show test stage
        CountDownLatch eventReceivedLatch = new CountDownLatch(1);
        initTestStage(stage, eventReceivedLatch);

        // check if isFocused returns true
        assertTrue(
            stage.isFocused(),
            "Stage.isFocused() returned false! Stage does not have focus after showing. " +
            "Some tests might fail because of this. Try re-running the tests with '--no-daemon' flag in Gradle."
        );

        // give UI a bit of time to finish showing transition
        // ex. on Windows above latch is set despite the UI still "animating" the show
        Thread.sleep(500);

        // check if window is on top
        Util.runAndWait(() -> {
            WritableImage capture = robot.getScreenCapture(null, STAGE_X, STAGE_Y, STAGE_SIZE, STAGE_SIZE, false);
            PixelReader captureReader = capture.getPixelReader();
            for (int x = 0; x < STAGE_SIZE; ++x) {
                for (int y = 0; y < STAGE_SIZE; ++y) {
                    Color color = captureReader.getColor(x, y);
                    assertTrue(checkColorEquals(SCENE_COLOR, color, TOLERANCE),
                        "Color " + colorToString(color) + " did not match color " + colorToString(SCENE_COLOR) + ". Stage is not on top after showing! " +
                        "Some tests might fail because of this. Try re-running the tests with '--no-daemon' flag in Gradle."
                    );
                }
            }
        });

        // check if we actually have focus and key presses are registered by the app
        Util.runAndWait(() -> {
            robot.keyPress(KeyCode.A);
        });
        assertTrue(
            eventReceivedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS),
            "Stage did not receive the key stroke generated by Robot! This might happen if the Stage did not receive focus after showing. " +
            "Some tests might fail because of this. Try re-running the tests with '--no-daemon' flag in Gradle."
        );
    }
}
