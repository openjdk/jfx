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

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import test.robot.testharness.VisualTestBase;
import test.util.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static test.util.Util.PARAMETERIZED_TEST_DISPLAY;

class StageMixedSizeTest extends VisualTestBase {
    private static final Color BACKGROUND_COLOR = Color.YELLOW;
    private static final double TOLERANCE = 0.07;
    private static final int WAIT = 300;
    private Stage testStage;

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED"})
    void testSetWidthOnlyAfterShownOnContentSizeWindow(StageStyle stageStyle) {
        final int finalWidth = 200;
        final int initialContentSize = 300;

        Util.doTimeLine(WAIT,
                () -> setupContentSizeTestStage(stageStyle, initialContentSize, initialContentSize),
                () -> testStage.setWidth(finalWidth),
                () -> assertColorDoesNotEqual(BACKGROUND_COLOR,
                        getColor(initialContentSize - 10, initialContentSize / 2), TOLERANCE),
                () -> assertEquals(finalWidth, testStage.getWidth(), "Window width should be " + finalWidth));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED"})
    void testSetHeightOnlyAfterShownOnContentSizeWindow(StageStyle stageStyle) {
        final int finalHeight = 200;
        final int initialContentSize = 300;

        Util.doTimeLine(WAIT,
                () -> setupContentSizeTestStage(stageStyle, initialContentSize, initialContentSize),
                () -> testStage.setHeight(finalHeight),
                () -> assertColorDoesNotEqual(BACKGROUND_COLOR,
                        getColor(initialContentSize / 2, initialContentSize - 10), TOLERANCE),
                () -> assertEquals(finalHeight, testStage.getHeight(), "Window height should be " + finalHeight));
    }

    private void setupContentSizeTestStage(StageStyle stageStyle, int width, int height) {
        testStage = getStage(true);
        testStage.initStyle(stageStyle);
        Scene scene = new Scene(new StackPane(), width, height, BACKGROUND_COLOR);
        testStage.setScene(scene);
        testStage.setX(0);
        testStage.setY(0);
        testStage.show();
    }
}
