/*
 * Copyright (c) 2024 Oracle and/or its affiliates. All rights reserved.
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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import test.robot.testharness.VisualTestBase;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.util.Util.TIMEOUT;

public class StageMixedSizeTest extends VisualTestBase {
    private static final Color BACKGROUND_COLOR = Color.YELLOW;
    private static final double TOLERANCE = 0.07;
    private Stage testStage;

    @Test
    public void testSetWidthOnlyAfterShownOnContentSizeWindow() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final int finalWidth = 200;
        final int initialContentSize = 300;

        setupContentSizeTestStage(initialContentSize, initialContentSize,
                () -> doTimeLine(Map.of(500L, () -> testStage.setWidth(finalWidth),
                                        1000L, latch::countDown)));

        assertTrue(latch.await(TIMEOUT, TimeUnit.MILLISECONDS), "Timeout waiting for test stage to be shown");

        runAndWait(() -> assertColorDoesNotEqual(BACKGROUND_COLOR,
                getColor(initialContentSize - 10, initialContentSize / 2), TOLERANCE));
        Assertions.assertEquals(finalWidth, testStage.getWidth(), "Window width should be " + finalWidth);
    }

    @Test
    public void testSetHeightOnlyAfterShownOnContentSizeWindow() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final int finalHeight = 200;
        final int initialContentSize = 300;

        setupContentSizeTestStage(initialContentSize, initialContentSize,
                () -> doTimeLine(Map.of(500L, () -> testStage.setHeight(finalHeight),
                                        1000L, latch::countDown)));

        assertTrue(latch.await(TIMEOUT, TimeUnit.MILLISECONDS), "Timeout waiting for test stage to be shown");

        runAndWait(() -> assertColorDoesNotEqual(BACKGROUND_COLOR,
                getColor(initialContentSize / 2, initialContentSize - 10), TOLERANCE));
        Assertions.assertEquals(finalHeight, testStage.getHeight(), "Window height should be " + finalHeight);
    }

    private void setupContentSizeTestStage(int width, int height, Runnable onShown) {
        runAndWait(() -> {
            testStage = getStage(true);
            testStage.initStyle(StageStyle.TRANSPARENT);
            Pane pane = new Pane();
            pane.setPrefSize(width, height);
            pane.setBackground(new Background(new BackgroundFill(BACKGROUND_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));
            Scene scene = new Scene(pane);
            testStage.setScene(scene);
            testStage.setX(0);
            testStage.setY(0);
            testStage.setOnShown(e -> onShown.run());
            testStage.show();
        });
    }

    private void doTimeLine(Map<Long, Runnable> keyFrames) {
        Timeline timeline = new Timeline();
        timeline.setCycleCount(1);
        keyFrames.forEach((duration, runnable) ->
                timeline.getKeyFrames().add(new KeyFrame(Duration.millis(duration), e -> runnable.run())));
        timeline.play();
    }
}
