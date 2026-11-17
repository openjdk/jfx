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

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static test.util.Util.TIMEOUT;

import test.robot.testharness.VisualTestBase;

/**
 * Test that scene changes made while a stage is iconified get drawn after the
 * stage is de-iconified.
 *
 * Note: on macOS you should run these tests with the Desktop & Dock
 * "Minimize windows into application icon" setting turned off. When this
 * setting is turned on the OS keeps generating new NSScreen objects and the
 * resulting notifications will mask the original JavaFX bug.
 */
@Timeout(value=15000, unit=TimeUnit.MILLISECONDS)
public class DrawAfterDeiconifyTest extends VisualTestBase {

    private static final int WIDTH = 300;
    private static final int HEIGHT = 300;

    private static final Color FIRST_COLOR = Color.LIME;
    private static final Color SECOND_COLOR = Color.HOTPINK;

    private static final double TOLERANCE = 0.07;

    private Stage stage;
    private int centerX;
    private int centerY;

    public void redrawsAfterDeiconify(StageStyle stageStyle, final boolean maximized) throws Exception {
        final CountDownLatch stageShownLatch = new CountDownLatch(1);

        runAndWait(() -> {
            stage = getStage(false);
            stage.initStyle(stageStyle);
            Scene scene = new Scene(new Pane(), WIDTH, HEIGHT);
            scene.setFill(FIRST_COLOR);
            stage.setScene(scene);
            stage.setOnShown(e -> {
                Platform.runLater(() -> {
                    stage.setMaximized(maximized);
                    centerX = (int)(stage.getX() + stage.getWidth() / 2.0);
                    centerY = (int)(stage.getY() + stage.getHeight() / 2.0);
                    stageShownLatch.countDown();
                });
            });
            stage.show();
        });

        Assertions.assertTrue(stageShownLatch.await(TIMEOUT, TimeUnit.MILLISECONDS), "Timeout waiting for stage to be shown");

        waitFirstFrame();
        runAndWait(() -> {
            Color color = getColor(centerX, centerY);
            assertColorEquals(FIRST_COLOR, color, TOLERANCE);
            stage.setIconified(true);
        });

        // Update the scene and then wait for a pulse to clear the pending
        // paint request.
        runAndWait(() -> stage.getScene().setFill(SECOND_COLOR));
        waitNextFrame();

        // Deiconify and verify that the scene change gets redrawn
        runAndWait(() -> stage.setIconified(false));
        waitNextFrame();
        runAndWait(() -> {
            Color color = getColor(centerX, centerY);
            assertColorEquals(SECOND_COLOR, color, TOLERANCE);
        });
    }

    @ParameterizedTest
    @EnumSource(names = {"DECORATED", "UNDECORATED", "TRANSPARENT"})
    public void stageRedrawsAfterDeiconify(StageStyle stageStyle) throws Exception {
        redrawsAfterDeiconify(stageStyle, false);
    }

    @ParameterizedTest
    @EnumSource(names = {"DECORATED", "UNDECORATED", "TRANSPARENT"})
    public void maximizedStageRedrawsAfterDeiconify(StageStyle stageStyle) throws Exception {
        redrawsAfterDeiconify(stageStyle, true);
    }
}
