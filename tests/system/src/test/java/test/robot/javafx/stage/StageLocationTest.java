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

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import test.robot.testharness.VisualTestBase;
import test.util.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static test.util.Util.*;
import static test.util.Util.PARAMETERIZED_TEST_DISPLAY;
import static test.util.Util.TIMEOUT;

@Timeout(value = TIMEOUT, unit= TimeUnit.MILLISECONDS)
class StageLocationTest extends VisualTestBase {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 300;
    private static final int X = 100;
    private static final int Y = 100;
    private static final int TO_X = 500;
    private static final int TO_Y = 500;
    private static final Color COLOR = Color.RED;
    private static final double TOLERANCE = 0.07;

    private Stage createStage(StageStyle stageStyle) {
        Stage s = getStage(true);
        s.initStyle(stageStyle);
        VBox vBox = new VBox(createLabel("X: ", s.xProperty()),
                createLabel("Y: ", s.yProperty()));
        vBox.setBackground(Background.EMPTY);
        Scene scene = new Scene(vBox, WIDTH, HEIGHT);
        scene.setFill(COLOR);
        s.setScene(scene);
        s.setWidth(WIDTH);
        s.setHeight(HEIGHT);
        return s;
    }

    protected Label createLabel(String prefix, ReadOnlyDoubleProperty property) {
        Label label = new Label();
        label.textProperty().bind(Bindings.concat(prefix, Bindings.convert(property)));
        return label;
    }

    private void assertColorEquals(Color expected, int x, int y) {
        Color color = getColor(x, y);
        assertColorEquals(expected, color, TOLERANCE);
    }

    private Stage stage;

    private void showStageAndWait(Stage stage) {
        CountDownLatch latch = new CountDownLatch(1);
        runAndWait(() -> {
            stage.setOnShown(e -> Platform.runLater(latch::countDown));
            stage.show();
        });
        try {
            assertTrue(latch.await(TIMEOUT, TimeUnit.MILLISECONDS),
                    "Timeout waiting for stage to be shown");
        } catch (InterruptedException e) {
            fail(e);
        }

        Util.sleep(FOCUS_DELAY);
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED",  "EXTENDED", "UTILITY"})
    void moveXY(StageStyle stageStyle) {
        runAndWait(() -> {
            stage = createStage(stageStyle);
            stage.setX(X);
            stage.setY(Y);
        });
        showStageAndWait(stage);
        Util.sleep(GEOMETRY_DELAY);

        runAndWait(() -> assertColorEquals(COLOR, X + 100, Y + 100));

        runAndWait(() -> {
            stage.setX(TO_X);
            stage.setY(TO_Y);
        });
        Util.sleep(GEOMETRY_DELAY);

        runAndWait(() -> assertColorEquals(COLOR, TO_X + 100, TO_Y + 100));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED", "UTILITY"})
    void moveX(StageStyle stageStyle) {
        runAndWait(() -> {
            stage = createStage(stageStyle);
            stage.setX(X);
            stage.setY(Y);
        });
        showStageAndWait(stage);
        Util.sleep(GEOMETRY_DELAY);

        runAndWait(() -> assertColorEquals(COLOR, X + 100, Y + 100));

        runAndWait(() -> stage.setX(TO_X));
        Util.sleep(GEOMETRY_DELAY);

        runAndWait(() -> assertColorEquals(COLOR, TO_X + 100, Y + 100));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED", "UTILITY"})
    void moveY(StageStyle stageStyle) {
        runAndWait(() -> {
            stage = createStage(stageStyle);
            stage.setX(X);
            stage.setY(Y);
        });
        showStageAndWait(stage);
        Util.sleep(GEOMETRY_DELAY);

        runAndWait(() -> assertColorEquals(COLOR, X + 100, Y + 100));

        runAndWait(() -> stage.setY(TO_Y));
        Util.sleep(GEOMETRY_DELAY);

        runAndWait(() -> assertColorEquals(COLOR, X + 100, TO_Y + 100));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED", "UTILITY"})
    void moveAfterShow(StageStyle stageStyle) {
        runAndWait(() -> {
            stage = createStage(stageStyle);
            stage.setX(X);
            stage.setY(Y);
        });
        showStageAndWait(stage);
        Util.sleep(GEOMETRY_DELAY);

        runAndWait(() -> {
            stage.setX(TO_X);
            stage.setY(TO_Y);
        });
        Util.sleep(GEOMETRY_DELAY);

        runAndWait(() -> assertColorEquals(COLOR, TO_X + 100, TO_Y + 100));
    }
}
