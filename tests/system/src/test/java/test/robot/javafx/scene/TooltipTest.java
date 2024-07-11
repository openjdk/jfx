/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.util.Util;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TooltipTest {

    private static final int DELTA = 100;

    static CountDownLatch startupLatch = new CountDownLatch(1);
    static CountDownLatch tooltipShownLatch;
    static volatile long tooltipStartTime;
    static volatile long tooltipShownTime;
    static Robot robot;
    static Button button;
    static Tooltip tooltip;

    static volatile Stage stage;
    static volatile Scene scene;

    private static void assertTooltipShowDelay(long tooltipShowTime, long expectedTime) {
        // To avoid any small timing error we rather check if the value is between.
        long maximumTime = expectedTime + DELTA;

        assertTrue(tooltipShowTime >= expectedTime, tooltipShowTime + " >= " + expectedTime);
        assertTrue(tooltipShowTime <= maximumTime, tooltipShowTime + " <= " + maximumTime);
    }

    @BeforeAll
    static void initFX() {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    static void exit() {
        Util.shutdown();
    }

    @Test
    void testDefaultTooltip() {
        assertFalse(tooltip.isShowing());

        long tooltipShowTime = waitForTooltip();

        assertTrue(tooltip.isShowing());
        assertTooltipShowDelay(tooltipShowTime, 1000);
    }

    @Test
    void testCssStylesheetTooltip() {
        scene.getStylesheets().add(getClass().getResource("tooltip.css").toExternalForm());

        assertFalse(tooltip.isShowing());

        long tooltipShowTime = waitForTooltip();

        assertTrue(tooltip.isShowing());

        assertTooltipShowDelay(tooltipShowTime, 30);
    }

    @Test
    void testCssStylesheetChangeTooltip() {
        scene.getStylesheets().add(getClass().getResource("tooltip.css").toExternalForm());

        assertFalse(tooltip.isShowing());

        long tooltipShowTime = waitForTooltip();

        assertTrue(tooltip.isShowing());

        assertTooltipShowDelay(tooltipShowTime, 30);

        scene.getStylesheets().setAll(getClass().getResource("tooltip2.css").toExternalForm());
        reset();
        tooltipShowTime = waitForTooltip();

        assertTrue(tooltip.isShowing());

        assertTooltipShowDelay(tooltipShowTime, 200);
    }

    @Test
    void testSmallShowDelayTooltip() {
        tooltip.setShowDelay(Duration.millis(100));

        assertFalse(tooltip.isShowing());

        long tooltipShowTime = waitForTooltip();

        assertTrue(tooltip.isShowing());
        assertTooltipShowDelay(tooltipShowTime, 100);
    }

    @Test
    void testSmallShowDelayCssTooltip() {
        tooltip.setStyle("-fx-show-delay: 100ms;");

        assertFalse(tooltip.isShowing());

        long tooltipShowTime = waitForTooltip();

        assertTrue(tooltip.isShowing());
        assertTooltipShowDelay(tooltipShowTime, 100);
    }

    @Test
    void testChangeShowDelayTooltip() {
        tooltip.setShowDelay(Duration.millis(100));

        assertFalse(tooltip.isShowing());

        long tooltipShowTime = waitForTooltip();

        assertTrue(tooltip.isShowing());
        assertTooltipShowDelay(tooltipShowTime, 100);

        // Try again with a bigger show delay.
        tooltip.setShowDelay(Duration.millis(2000));
        reset();
        tooltipShowTime = waitForTooltip();

        assertTrue(tooltip.isShowing());
        assertTooltipShowDelay(tooltipShowTime, 2000);
    }

    @Test
    void testChangeShowDelayCssTooltip() throws Throwable {
        tooltip.setStyle("-fx-show-delay: 100ms;");

        assertFalse(tooltip.isShowing());

        long tooltipShowTime = waitForTooltip();

        assertTrue(tooltip.isShowing());
        assertTooltipShowDelay(tooltipShowTime, 100);

        // Try again with a bigger show delay.
        tooltip.setStyle("-fx-show-delay: 2000ms;");
        reset();
        tooltipShowTime = waitForTooltip();

        assertTrue(tooltip.isShowing());
        assertTooltipShowDelay(tooltipShowTime, 2000);
    }

    @Test
    void testShowDelayCssShowTooltipTwice() {
        tooltip.setStyle("-fx-show-delay: 100ms;");

        assertFalse(tooltip.isShowing());

        long tooltipShowTime = waitForTooltip();

        assertTrue(tooltip.isShowing());

        assertTooltipShowDelay(tooltipShowTime, 100);

        // Try again.
        reset();
        tooltipShowTime = waitForTooltip();

        assertTrue(tooltip.isShowing());

        assertTooltipShowDelay(tooltipShowTime, 100);
    }

    private void reset() {
        tooltipShownLatch = new CountDownLatch(1);
        tooltipStartTime = 0;
        tooltipShownTime = 0;
    }

    private long waitForTooltip() {
        Util.runAndWait(() -> {
            // Click somewhere in the Stage to ensure that it is active
            Window window = scene.getWindow();
            robot.mouseMove(window.getX() + scene.getX(), window.getY() + scene.getY());
            robot.mouseClick(MouseButton.PRIMARY);
        });

        // Make sure that a previous tooltip is hidden by now.
        Util.sleep(500);

        assertFalse(tooltip.isShowing());

        Util.runAndWait(() -> {
            Window window = scene.getWindow();
            robot.mouseMove(
                    window.getX() + scene.getX() + button.getLayoutX() + button.getLayoutBounds().getWidth() / 2,
                    window.getY() + scene.getY() + button.getLayoutY() + button.getLayoutBounds().getHeight() / 2);
            tooltipStartTime = System.currentTimeMillis();
        });

        Util.waitForLatch(tooltipShownLatch, 5, "Timeout waiting for tooltip to display");

        long finalTime = tooltipShownTime - tooltipStartTime;

        Util.sleep(250);

        return finalTime;
    }

    @AfterEach
    void resetUI() {
        Util.runAndWait(() -> {
            button.setTooltip(null);
            tooltip = null;

            scene.getStylesheets().clear();
        });
    }

    @BeforeEach
    void setupUI() {
        Util.runAndWait(() -> {
            tooltipShownLatch = new CountDownLatch(1);

            tooltip = new Tooltip("tooltip");
            tooltip.showingProperty().addListener((obs, oldV, isShowing) -> {
                if (isShowing) {
                    tooltipShownTime = System.currentTimeMillis();
                    tooltipShownLatch.countDown();
                }
            });
            button.setTooltip(tooltip);
        });
    }

    public static class TestApp extends Application {

        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;

            button = new Button("Button");

            scene = new Scene(new StackPane(button), 250, 250);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setAlwaysOnTop(true);
            stage.setOnShown(event -> Platform.runLater(startupLatch::countDown));
            stage.show();
        }
    }
}
